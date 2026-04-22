package com.sme.be_sme.modules.survey.processor;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.sme.be_sme.modules.survey.api.request.SurveyAiSummaryRequest;
import com.sme.be_sme.modules.survey.api.response.SurveyAiSummaryResponse;
import com.sme.be_sme.modules.survey.infrastructure.mapper.SurveyAiSummaryMapper;
import com.sme.be_sme.modules.survey.infrastructure.persistence.entity.SurveyAiSummaryEntity;
import com.sme.be_sme.modules.survey.service.SurveyAiSummaryService;
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
import java.util.Date;
import java.util.Iterator;
import java.util.Map;
import java.util.TreeMap;

@Component
@RequiredArgsConstructor
public class SurveyAnalyticsAiSummaryProcessor extends BaseBizProcessor<BizContext> {

    private static final int MAX_GENERATE_PER_USER_PER_5_MINUTES = 5;

    private final ObjectMapper objectMapper;
    private final SurveyAiSummaryMapper surveyAiSummaryMapper;
    private final SurveyAiSummaryService surveyAiSummaryService;

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

        if (!forceRefresh) {
            SurveyAiSummaryEntity cached = surveyAiSummaryMapper.selectByCacheKey(
                    companyId,
                    trimToNull(request.getTemplateId()),
                    request.getStartDate(),
                    request.getEndDate(),
                    language,
                    inputHash
            );

            if (cached != null) {
                return toResponse(cached, true);
            }
        }

        checkRateLimit(companyId, operatorId);

        JsonNode promptInput = buildPromptInput(
                request,
                language,
                sanitizedSnapshot
        );

        SurveyAiSummaryResponse aiResponse =
                surveyAiSummaryService.generate(promptInput, language);

        Date now = new Date();

        aiResponse.setFromCache(false);
        aiResponse.setGeneratedAt(now);

        boolean shouldCache =
                aiResponse.getSummary() != null
                        && !aiResponse.getSummary().contains("quota Gemini")
                        && !aiResponse.getSummary().contains("Gemini quota")
                        && !aiResponse.getSummary().contains("RESOURCE_EXHAUSTED")
                        && !aiResponse.getSummary().contains("429");

        if (!shouldCache) {
            return aiResponse;
        }

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

        return aiResponse;
    }

    private void checkRateLimit(String companyId, String operatorId) {
        Date since = new Date(System.currentTimeMillis() - 5L * 60L * 1000L);

        int count = surveyAiSummaryMapper.countGeneratedSince(
                companyId,
                operatorId,
                since
        );

        if (count >= MAX_GENERATE_PER_USER_PER_5_MINUTES) {
            throw AppException.of(
                    ErrorCodes.BAD_REQUEST,
                    "AI summary generate limit reached. Please try again later."
            );
        }
    }

    private SurveyAiSummaryResponse toResponse(SurveyAiSummaryEntity entity, boolean fromCache) {
        try {
            SurveyAiSummaryResponse response =
                    objectMapper.readValue(entity.getSummaryJson(), SurveyAiSummaryResponse.class);

            response.setFromCache(fromCache);
            response.setGeneratedAt(entity.getGeneratedAt());

            if (!StringUtils.hasText(response.getHealthLevel())) {
                response.setHealthLevel(entity.getHealthLevel());
            }

            return response;
        } catch (Exception e) {
            throw AppException.of(ErrorCodes.INTERNAL_ERROR, "invalid cached AI summary");
        }
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
            throw AppException.of(ErrorCodes.BAD_REQUEST, "analyticsSnapshot is required");
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

    private String toStableJson(JsonNode node) {
        try {
            Object sorted = sortJson(node);
            return objectMapper
                    .copy()
                    .configure(SerializationFeature.ORDER_MAP_ENTRIES_BY_KEYS, true)
                    .writeValueAsString(sorted);
        } catch (Exception e) {
            throw AppException.of(ErrorCodes.INTERNAL_ERROR, "cannot hash AI summary input");
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
            return objectMapper.convertValue(node, Object.class);
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
            throw AppException.of(ErrorCodes.INTERNAL_ERROR, "cannot create input hash");
        }
    }

    private String writeJson(Object value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (Exception e) {
            throw AppException.of(ErrorCodes.INTERNAL_ERROR, "cannot save AI summary");
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
            throw AppException.of(ErrorCodes.BAD_REQUEST, "tenantId is required");
        }
    }

    private void validateRequest(SurveyAiSummaryRequest request) {
        if (request == null) {
            throw AppException.of(ErrorCodes.BAD_REQUEST, "payload is required");
        }

        if (request.getAnalyticsSnapshot() == null || request.getAnalyticsSnapshot().isNull()) {
            throw AppException.of(ErrorCodes.BAD_REQUEST, "analyticsSnapshot is required");
        }
    }
}