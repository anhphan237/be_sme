package com.sme.be_sme.modules.survey.processor;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.sme.be_sme.modules.survey.api.request.SurveyAiSummaryRequest;
import com.sme.be_sme.modules.survey.api.response.SurveyAiSummaryResponse;
import com.sme.be_sme.modules.survey.infrastructure.mapper.SurveyAiSummaryMapper;
import com.sme.be_sme.modules.survey.infrastructure.persistence.entity.SurveyAiSummaryEntity;
import com.sme.be_sme.modules.survey.service.SurveyOpenAiSummaryService;
import com.sme.be_sme.shared.constant.ErrorCodes;
import com.sme.be_sme.shared.exception.AppException;
import com.sme.be_sme.shared.gateway.core.BaseBizProcessor;
import com.sme.be_sme.shared.gateway.core.BizContext;
import com.sme.be_sme.shared.util.UuidGenerator;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

@Component
@RequiredArgsConstructor
public class SurveyAnalyticsAiSummaryProcessor extends BaseBizProcessor<BizContext> {

    private static final int MAX_GENERATE_PER_USER_PER_5_MINUTES = 5;

    private final ObjectMapper objectMapper;
    private final SurveyAiSummaryMapper surveyAiSummaryMapper;
    private final SurveyOpenAiSummaryService surveyOpenAiSummaryService;

    @Override
    protected Object doProcess(BizContext context, JsonNode payload) {
        validateContext(context);

        SurveyAiSummaryRequest request =
                objectMapper.convertValue(payload, SurveyAiSummaryRequest.class);

        validateRequest(request);

        String companyId = context.getTenantId().trim();
        String operatorId = StringUtils.hasText(context.getOperatorId())
                ? context.getOperatorId().trim()
                : "system";

        String language = normalizeLanguage(request.getLanguage());
        JsonNode sanitizedSnapshot = sanitizeSnapshot(request.getAnalyticsSnapshot());

        JsonNode inputForHash = buildInputForHash(
                companyId,
                request.getTemplateId(),
                request.getStartDate(),
                request.getEndDate(),
                language,
                sanitizedSnapshot
        );

        String inputHash = sha256(toStableJson(inputForHash));
        boolean forceRefresh = Boolean.TRUE.equals(request.getForceRefresh());

        SurveyAiSummaryEntity cached = surveyAiSummaryMapper.selectByCacheKey(
                companyId,
                trimToNull(request.getTemplateId()),
                request.getStartDate(),
                request.getEndDate(),
                language,
                inputHash
        );

        // Bình thường: có cache thì trả luôn
        if (!forceRefresh && cached != null) {
            return toResponse(
                    cached,
                    true,
                    "CACHE",
                    null
            );
        }

        // Nếu user bấm refresh quá nhiều mà đã có cache, trả cache cũ thay vì chặn cứng
        if (isRateLimited(companyId, operatorId)) {
            if (cached != null) {
                return toResponse(
                        cached,
                        true,
                        "CACHE_STALE",
                        "Đang hiển thị bản tóm tắt gần nhất vì bạn vừa làm mới AI quá nhiều lần trong thời gian ngắn."
                );
            }

            throw AppException.of(
                    ErrorCodes.BAD_REQUEST,
                    "Bạn vừa tạo tóm tắt AI quá nhiều lần trong thời gian ngắn. Vui lòng thử lại sau ít phút."
            );
        }

        JsonNode promptInput = buildPromptInput(
                request,
                language,
                sanitizedSnapshot
        );

        try {
            String prompt = buildPrompt(promptInput, language);
            String raw = surveyOpenAiSummaryService.generateRawJson(prompt);

            SurveyAiSummaryResponse aiResponse = objectMapper.readValue(
                    extractJson(raw),
                    SurveyAiSummaryResponse.class
            );

            normalizeAiResponse(aiResponse);

            aiResponse.setSource("AI");
            aiResponse.setAiAvailable(true);
            aiResponse.setErrorMessage(null);
            aiResponse.setFromCache(false);
            aiResponse.setGeneratedAt(new Date());

            saveCache(
                    companyId,
                    operatorId,
                    request,
                    language,
                    inputHash,
                    aiResponse
            );

            return aiResponse;

        } catch (Exception e) {
            // Nếu AI fail mà đã có cache cũ, trả cache cũ cho mượt
            if (cached != null) {
                return toResponse(
                        cached,
                        true,
                        "CACHE_STALE",
                        buildAiUnavailableMessage(language, e)
                );
            }

            return buildFallbackResponse(language, e);
        }
    }

    private boolean isRateLimited(String companyId, String operatorId) {
        Date since = new Date(System.currentTimeMillis() - 5L * 60L * 1000L);

        int count = surveyAiSummaryMapper.countGeneratedSince(
                companyId,
                operatorId,
                since
        );

        return count >= MAX_GENERATE_PER_USER_PER_5_MINUTES;
    }

    private void saveCache(
            String companyId,
            String operatorId,
            SurveyAiSummaryRequest request,
            String language,
            String inputHash,
            SurveyAiSummaryResponse aiResponse
    ) {
        Date now = new Date();
        aiResponse.setGeneratedAt(now);

        SurveyAiSummaryEntity entity = new SurveyAiSummaryEntity();
        entity.setSummaryId(UuidGenerator.generate());
        entity.setCompanyId(companyId);
        entity.setTemplateId(trimToNull(request.getTemplateId()));
        entity.setStartDate(request.getStartDate());
        entity.setEndDate(request.getEndDate());
        entity.setLanguage(language);
        entity.setInputHash(inputHash);
        entity.setHealthLevel(aiResponse.getHealthLevel());
        entity.setSummaryJson(writeJson(aiResponse));
        entity.setGeneratedAt(now);
        entity.setGeneratedBy(operatorId);

        surveyAiSummaryMapper.insert(entity);
    }

    private SurveyAiSummaryResponse toResponse(
            SurveyAiSummaryEntity entity,
            boolean fromCache,
            String source,
            String errorMessage
    ) {
        try {
            SurveyAiSummaryResponse response =
                    objectMapper.readValue(entity.getSummaryJson(), SurveyAiSummaryResponse.class);

            normalizeAiResponse(response);

            response.setFromCache(fromCache);
            response.setGeneratedAt(entity.getGeneratedAt());
            response.setSource(source);
            response.setAiAvailable(true);
            response.setErrorMessage(errorMessage);

            if (!StringUtils.hasText(response.getHealthLevel())) {
                response.setHealthLevel(entity.getHealthLevel());
            }

            return response;
        } catch (Exception e) {
            throw AppException.of(
                    ErrorCodes.INTERNAL_ERROR,
                    "Không thể đọc dữ liệu tóm tắt AI đã lưu. Vui lòng thử tạo lại tóm tắt mới."
            );
        }
    }

    private SurveyAiSummaryResponse buildFallbackResponse(String language, Exception e) {
        boolean english = language != null && language.toLowerCase().startsWith("en");

        SurveyAiSummaryResponse fallback = new SurveyAiSummaryResponse();
        fallback.setHealthLevel("WARNING");
        fallback.setSource("FALLBACK");
        fallback.setAiAvailable(false);
        fallback.setFromCache(false);
        fallback.setGeneratedAt(new Date());

        fallback.setSummary(english
                ? "AI summary is temporarily unavailable. Please review the survey dashboard metrics directly."
                : "Hiện hệ thống chưa thể tạo tóm tắt AI. HR vẫn có thể theo dõi tình hình qua KPI, biểu đồ và phần tóm tắt mặc định.");

        fallback.setKeyFindings(new ArrayList<>(List.of(
                english
                        ? "The AI provider could not generate a summary at this moment."
                        : "Tóm tắt AI chưa khả dụng tại thời điểm này.",
                english
                        ? "The survey dashboard metrics are still available for review."
                        : "Các chỉ số khảo sát trên dashboard vẫn đầy đủ để HR theo dõi và đánh giá."
        )));

        fallback.setRecommendedActions(new ArrayList<>(List.of(
                english
                        ? "Use the standard summary and dashboard charts for now."
                        : "Tạm thời sử dụng phần tóm tắt mặc định và các biểu đồ trên dashboard.",
                english
                        ? "Try generating the AI summary again later."
                        : "Thử tạo lại tóm tắt AI sau ít phút."
        )));

        fallback.setRiskExplanation("");
        fallback.setPositiveSignal("");
        fallback.setErrorMessage(buildAiUnavailableMessage(language, e));

        return fallback;
    }

    private String buildAiUnavailableMessage(String language, Exception e) {
        boolean english = language != null && language.toLowerCase().startsWith("en");

        String message = e == null || e.getMessage() == null
                ? ""
                : e.getMessage();

        boolean quotaError =
                message.contains("RESOURCE_EXHAUSTED")
                        || message.contains("429")
                        || message.toLowerCase().contains("quota")
                        || message.toLowerCase().contains("rate limit")
                        || message.toLowerCase().contains("resource exhausted");

        if (quotaError) {
            return english
                    ? "The AI service is temporarily busy or has reached its usage limit. Showing the latest available summary."
                    : "Dịch vụ AI đang tạm quá tải hoặc đã chạm giới hạn sử dụng. Hệ thống sẽ ưu tiên hiển thị bản tóm tắt gần nhất nếu có.";
        }

        return english
                ? "The AI summary could not be generated right now."
                : "Hiện chưa thể tạo tóm tắt AI. Vui lòng thử lại sau.";
    }

    private JsonNode buildPromptInput(
            SurveyAiSummaryRequest request,
            String language,
            JsonNode sanitizedSnapshot
    ) {
        ObjectNode root = objectMapper.createObjectNode();

        root.put("language", language);

        ObjectNode filters = root.putObject("filters");
        putNullable(filters, "templateId", request.getTemplateId());
        putNullable(filters, "startDate", request.getStartDate());
        putNullable(filters, "endDate", request.getEndDate());

        root.set("analytics", sanitizedSnapshot);

        return root;
    }

    private JsonNode buildInputForHash(
            String companyId,
            String templateId,
            LocalDate startDate,
            LocalDate endDate,
            String language,
            JsonNode sanitizedSnapshot
    ) {
        ObjectNode root = objectMapper.createObjectNode();

        root.put("companyId", companyId);
        root.put("templateId", trimToNull(templateId));
        root.put("startDate", startDate == null ? null : startDate.toString());
        root.put("endDate", endDate == null ? null : endDate.toString());
        root.put("language", language);
        root.set("analytics", sanitizedSnapshot);

        return root;
    }

    private JsonNode sanitizeSnapshot(JsonNode raw) {
        if (raw == null || raw.isNull()) {
            throw AppException.of(
                    ErrorCodes.BAD_REQUEST,
                    "Chưa có đủ dữ liệu khảo sát để tạo tóm tắt AI."
            );
        }

        ObjectNode root = objectMapper.createObjectNode();

        copyIfExists(raw, root, "sentCount");
        copyIfExists(raw, root, "submittedCount");
        copyIfExists(raw, root, "responseRate");
        copyIfExists(raw, root, "overallSatisfactionScore");
        copyIfExists(raw, root, "dimensionStats");
        copyIfExists(raw, root, "lowScoreDimensions");
        copyIfExists(raw, root, "topPositiveDimensions");
        copyIfExists(raw, root, "stageTrends");
        copyIfExists(raw, root, "timeTrends");
        copyIfExists(raw, root, "questionStats");
        copyIfExists(raw, root, "textFeedbacks");

        return root;
    }

    private void copyIfExists(JsonNode source, ObjectNode target, String field) {
        JsonNode value = source.get(field);
        if (value != null && !value.isNull()) {
            target.set(field, value);
        }
    }

    private String buildPrompt(JsonNode summaryInput, String language) {
        boolean english = language != null && language.toLowerCase().startsWith("en");

        String langInstruction = english
                ? "Write in English."
                : "Viết bằng tiếng Việt, ngắn gọn, thực tế, không sáo rỗng.";

        return """
                You are an HR analytics assistant for an employee onboarding survey dashboard.

                Your job:
                - Analyze ONLY the data provided.
                - Do NOT invent numbers.
                - Do NOT mention data that is not present.
                - If data is insufficient, say it clearly.
                - Focus on what HR should understand and do next.
                - Keep the summary practical and easy to read.
                - %s

                Return ONLY valid JSON. No markdown. No explanation outside JSON.

                JSON schema:
                {
                  "healthLevel": "GOOD | STABLE | WARNING",
                  "summary": "string",
                  "keyFindings": ["string"],
                  "recommendedActions": ["string"],
                  "riskExplanation": "string",
                  "positiveSignal": "string"
                }

                Data:
                %s
                """.formatted(langInstruction, truncate(summaryInput.toString(), 12000));
    }

    private String extractJson(String raw) {
        if (!StringUtils.hasText(raw)) {
            return "{}";
        }

        String text = raw.trim();

        if (text.startsWith("```json")) {
            text = text.replaceFirst("```json", "").trim();
        }

        if (text.startsWith("```")) {
            text = text.replaceFirst("```", "").trim();
        }

        if (text.endsWith("```")) {
            text = text.substring(0, text.length() - 3).trim();
        }

        int start = text.indexOf('{');
        int end = text.lastIndexOf('}');

        if (start >= 0 && end > start) {
            return text.substring(start, end + 1);
        }

        return text;
    }

    private void normalizeAiResponse(SurveyAiSummaryResponse response) {
        if (!StringUtils.hasText(response.getHealthLevel())) {
            response.setHealthLevel("STABLE");
        } else {
            response.setHealthLevel(response.getHealthLevel().trim().toUpperCase());
        }

        if (!StringUtils.hasText(response.getSummary())) {
            response.setSummary("");
        }

        if (response.getKeyFindings() == null) {
            response.setKeyFindings(new ArrayList<>());
        }

        if (response.getRecommendedActions() == null) {
            response.setRecommendedActions(new ArrayList<>());
        }

        if (response.getRiskExplanation() == null) {
            response.setRiskExplanation("");
        }

        if (response.getPositiveSignal() == null) {
            response.setPositiveSignal("");
        }
    }

    private String toStableJson(JsonNode node) {
        try {
            Object sorted = sortJson(node);
            return objectMapper
                    .copy()
                    .configure(SerializationFeature.ORDER_MAP_ENTRIES_BY_KEYS, true)
                    .writeValueAsString(sorted);
        } catch (Exception e) {
            throw AppException.of(
                    ErrorCodes.INTERNAL_ERROR,
                    "Không thể xử lý dữ liệu khảo sát để tạo tóm tắt AI."
            );
        }
    }

    private Object sortJson(JsonNode node) {
        if (node == null || node.isNull()) {
            return null;
        }

        if (node.isObject()) {
            Map<String, Object> map = new TreeMap<>();
            Iterator<String> fields = node.fieldNames();

            while (fields.hasNext()) {
                String field = fields.next();
                map.put(field, sortJson(node.get(field)));
            }

            return map;
        }

        if (node.isArray()) {
            List<Object> list = new ArrayList<>();
            for (JsonNode item : node) {
                list.add(sortJson(item));
            }
            return list;
        }

        if (node.isNumber()) {
            return node.numberValue();
        }

        if (node.isBoolean()) {
            return node.booleanValue();
        }

        return node.asText();
    }

    private String sha256(String value) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(value.getBytes(StandardCharsets.UTF_8));

            StringBuilder hex = new StringBuilder();
            for (byte b : hash) {
                hex.append(String.format("%02x", b));
            }

            return hex.toString();
        } catch (Exception e) {
            throw AppException.of(
                    ErrorCodes.INTERNAL_ERROR,
                    "Không thể kiểm tra dữ liệu tóm tắt AI hiện tại."
            );
        }
    }

    private String writeJson(Object value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (Exception e) {
            throw AppException.of(
                    ErrorCodes.INTERNAL_ERROR,
                    "Đã tạo được tóm tắt AI nhưng không thể lưu lại. Vui lòng thử lại sau."
            );
        }
    }

    private void putNullable(ObjectNode node, String field, Object value) {
        if (value == null) {
            node.putNull(field);
        } else {
            node.put(field, String.valueOf(value));
        }
    }

    private String normalizeLanguage(String language) {
        if (!StringUtils.hasText(language)) {
            return "vi";
        }

        String normalized = language.trim().toLowerCase();

        if (normalized.startsWith("en")) {
            return "en";
        }

        return "vi";
    }

    private String trimToNull(String value) {
        if (!StringUtils.hasText(value)) {
            return null;
        }

        return value.trim();
    }

    private void validateContext(BizContext context) {
        if (context == null || !StringUtils.hasText(context.getTenantId())) {
            throw AppException.of(
                    ErrorCodes.BAD_REQUEST,
                    "Không xác định được công ty hiện tại. Vui lòng tải lại trang và thử lại."
            );
        }
    }

    private void validateRequest(SurveyAiSummaryRequest request) {
        if (request == null) {
            throw AppException.of(
                    ErrorCodes.BAD_REQUEST,
                    "Thiếu dữ liệu đầu vào để tạo tóm tắt AI."
            );
        }

        if (request.getAnalyticsSnapshot() == null || request.getAnalyticsSnapshot().isNull()) {
            throw AppException.of(
                    ErrorCodes.BAD_REQUEST,
                    "Chưa có đủ dữ liệu khảo sát để tạo tóm tắt AI."
            );
        }
    }

    private String truncate(String value, int max) {
        if (value == null) {
            return "";
        }

        return value.length() <= max ? value : value.substring(0, max);
    }
}