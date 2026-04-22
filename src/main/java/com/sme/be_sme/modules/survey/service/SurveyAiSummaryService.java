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

        String raw = chatLanguageModel.generate(prompt);
        String json = extractJson(raw);

        try {
            SurveyAiSummaryResponse response =
                    objectMapper.readValue(json, SurveyAiSummaryResponse.class);

            normalize(response);
            return response;
        } catch (Exception e) {
            SurveyAiSummaryResponse fallback = new SurveyAiSummaryResponse();
            fallback.setHealthLevel("WARNING");
            fallback.setSummary(language != null && language.startsWith("en")
                    ? "AI returned an invalid format. Please review the survey dashboard metrics manually."
                    : "AI trả về định dạng chưa hợp lệ. HR vui lòng xem trực tiếp các chỉ số trên dashboard.");
            fallback.setRiskExplanation("");
            fallback.setPositiveSignal("");
            fallback.setKeyFindings(new ArrayList<>());
            fallback.setRecommendedActions(new ArrayList<>());
            return fallback;
        }
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