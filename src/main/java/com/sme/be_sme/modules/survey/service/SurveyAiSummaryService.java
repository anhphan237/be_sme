package com.sme.be_sme.modules.survey.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sme.be_sme.modules.survey.api.response.SurveyAiSummaryResponse;
import dev.langchain4j.model.chat.ChatLanguageModel;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.ArrayList;

@Service
@RequiredArgsConstructor
public class SurveyAiSummaryService {

    private final ChatLanguageModel chatLanguageModel;
    private final ObjectMapper objectMapper;

    public SurveyAiSummaryResponse generate(JsonNode summaryInput, String language) {
        String prompt = buildPrompt(summaryInput, language);

        try {
            String raw = chatLanguageModel.generate(prompt);
            String json = extractJson(raw);

            SurveyAiSummaryResponse response =
                    objectMapper.readValue(json, SurveyAiSummaryResponse.class);

            normalize(response);
            return response;

        } catch (Exception e) {
            return buildFallbackResponse(e, language);
        }
    }

    private SurveyAiSummaryResponse buildFallbackResponse(Exception e, String language) {
        boolean english = language != null && language.toLowerCase().startsWith("en");

        String message = e.getMessage() == null ? "" : e.getMessage();
        boolean quotaExceeded =
                message.contains("RESOURCE_EXHAUSTED")
                        || message.contains("429")
                        || message.toLowerCase().contains("quota");

        SurveyAiSummaryResponse fallback = new SurveyAiSummaryResponse();
        fallback.setHealthLevel("WARNING");
        fallback.setFromCache(false);
        fallback.setKeyFindings(new ArrayList<>());
        fallback.setRecommendedActions(new ArrayList<>());

        if (quotaExceeded) {
            fallback.setSummary(english
                    ? "AI summary is temporarily unavailable because the Gemini quota has been exhausted. The dashboard can still be interpreted using the rule-based summary and charts."
                    : "Tóm tắt AI hiện tạm thời chưa khả dụng vì quota Gemini đã hết hoặc đang bị giới hạn. HR vẫn có thể xem phần tóm tắt thường, KPI và biểu đồ hiện có.");

            fallback.getKeyFindings().add(english
                    ? "Gemini returned RESOURCE_EXHAUSTED / 429."
                    : "Gemini trả về lỗi RESOURCE_EXHAUSTED / 429.");

            fallback.getRecommendedActions().add(english
                    ? "Try again later or use cached summary if available."
                    : "Thử lại sau hoặc dùng bản tóm tắt cache nếu đã từng tạo trước đó.");

            fallback.getRecommendedActions().add(english
                    ? "Avoid force refreshing AI summary repeatedly."
                    : "Không nên bấm làm mới AI liên tục vì sẽ tiếp tục tốn quota.");

            fallback.setRiskExplanation("");
            fallback.setPositiveSignal("");
            return fallback;
        }

        fallback.setSummary(english
                ? "AI returned an invalid or unavailable response. Please review the survey dashboard metrics manually."
                : "AI chưa trả về được kết quả hợp lệ. HR vui lòng xem trực tiếp các chỉ số trên dashboard.");

        fallback.getKeyFindings().add(english
                ? "AI summary generation failed."
                : "Quá trình tạo tóm tắt AI bị lỗi.");

        fallback.getRecommendedActions().add(english
                ? "Use the rule-based summary and dashboard charts."
                : "Sử dụng tóm tắt thường và các biểu đồ dashboard để phân tích.");

        fallback.setRiskExplanation("");
        fallback.setPositiveSignal("");
        return fallback;
    }

    private void normalize(SurveyAiSummaryResponse response) {
        if (!StringUtils.hasText(response.getHealthLevel())) {
            response.setHealthLevel("STABLE");
        }

        response.setHealthLevel(response.getHealthLevel().trim().toUpperCase());

        if (!StringUtils.hasText(response.getSummary())) {
            response.setSummary("No summary available.");
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

    private String truncate(String value, int max) {
        if (value == null) {
            return "";
        }

        if (value.length() <= max) {
            return value;
        }

        return value.substring(0, max);
    }
}