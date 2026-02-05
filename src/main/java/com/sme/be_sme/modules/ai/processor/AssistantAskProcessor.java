package com.sme.be_sme.modules.ai.processor;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sme.be_sme.modules.ai.api.request.AssistantAskRequest;
import com.sme.be_sme.modules.ai.api.response.AssistantAskResponse;
import com.sme.be_sme.modules.content.api.request.DocumentListRequest;
import com.sme.be_sme.modules.content.api.response.DocumentListResponse;
import com.sme.be_sme.modules.content.facade.ContentFacade;
import com.sme.be_sme.shared.constant.ErrorCodes;
import com.sme.be_sme.shared.exception.AppException;
import com.sme.be_sme.shared.gateway.core.BaseBizProcessor;
import com.sme.be_sme.shared.gateway.core.BizContext;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
public class AssistantAskProcessor extends BaseBizProcessor<BizContext> {

    private final ObjectMapper objectMapper;
    private final ContentFacade contentFacade;

    @Override
    protected Object doProcess(BizContext context, JsonNode payload) {
        AssistantAskRequest request = objectMapper.convertValue(payload, AssistantAskRequest.class);
        validate(context, request);

        String tenantId = context.getTenantId();
        String userId = StringUtils.hasText(request.getUserId()) ? request.getUserId().trim() : context.getOperatorId();
        String question = request.getQuestion().trim();

        // Query Document Library for this tenant (company-specific)
        DocumentListResponse listResponse = contentFacade.listDocuments(new DocumentListRequest());
        List<DocumentListResponse.DocumentItem> allDocs = listResponse.getItems() != null ? listResponse.getItems() : new ArrayList<>();

        // Mock search: filter documents relevant to the question (by keyword in name/description)
        List<DocumentListResponse.DocumentItem> relevant = searchRelevantDocuments(allDocs, question);

        // Mock LLM: generate answer with personalized context (new employee)
        String answer = generateMockAnswer(question, relevant, userId);

        List<String> sourceNames = relevant.stream()
                .map(DocumentListResponse.DocumentItem::getName)
                .collect(Collectors.toList());

        AssistantAskResponse response = new AssistantAskResponse();
        response.setAnswer(answer);
        response.setSourceDocumentNames(sourceNames);
        return response;
    }

    private List<DocumentListResponse.DocumentItem> searchRelevantDocuments(
            List<DocumentListResponse.DocumentItem> docs, String question) {
        if (docs.isEmpty()) return new ArrayList<>();
        String q = question.toLowerCase(Locale.ROOT);
        List<DocumentListResponse.DocumentItem> matched = docs.stream()
                .filter(d -> {
                    String name = d.getName() != null ? d.getName().toLowerCase(Locale.ROOT) : "";
                    String desc = d.getDescription() != null ? d.getDescription().toLowerCase(Locale.ROOT) : "";
                    return name.contains(q) || desc.contains(q)
                            || matchesTopic(q, name, desc);
                })
                .collect(Collectors.toList());
        if (matched.isEmpty()) return docs;
        return matched;
    }

    private boolean matchesTopic(String question, String name, String desc) {
        String combined = name + " " + desc;
        if (question.contains("wifi") || question.contains("wi-fi")) return combined.contains("wifi") || combined.contains("wi-fi") || combined.contains("network");
        if (question.contains("parking")) return combined.contains("parking") || combined.contains("park");
        if (question.contains("regulation") || question.contains("policy") || question.contains("handbook")) return combined.contains("regulation") || combined.contains("policy") || combined.contains("handbook") || combined.contains("internal");
        return false;
    }

    private String generateMockAnswer(String question, List<DocumentListResponse.DocumentItem> sources, String userId) {
        String q = question.toLowerCase(Locale.ROOT);
        StringBuilder sb = new StringBuilder();
        sb.append("Hello");
        if (StringUtils.hasText(userId)) sb.append(" (as a new employee)");
        sb.append("! ");
        if (sources.isEmpty()) {
            sb.append("I couldn't find company documents matching your question. Please check the Document Library or ask HR. Your question: \"").append(question).append("\"");
            return sb.toString();
        }
        sb.append("Based on your company's documents");
        if (!sources.isEmpty()) {
            sb.append(" (e.g. ");
            sb.append(sources.get(0).getName());
            for (int i = 1; i < Math.min(sources.size(), 3); i++) sb.append(", ").append(sources.get(i).getName());
            sb.append(")");
        }
        sb.append(": ");
        if (q.contains("wifi") || q.contains("wi-fi")) {
            sb.append("WiFi access details are in the internal documents. Connect to the corporate network and refer to the IT or Office Guide document for SSID and password.");
        } else if (q.contains("parking")) {
            sb.append("Parking information (locations, permits) is available in the office or facilities documents. Check the Document Library for your site-specific guide.");
        } else if (q.contains("regulation") || q.contains("policy") || q.contains("handbook")) {
            sb.append("Internal regulations and policies are in the Employee Handbook and related documents. Please read and acknowledge them in the Document Library.");
        } else {
            sb.append("You can find more details in the Document Library. If you need help using the system, go to Onboarding and complete the assigned tasks.");
        }
        sb.append(" You can open these documents from the Document Library.");
        return sb.toString();
    }

    private static void validate(BizContext context, AssistantAskRequest request) {
        if (context == null || !StringUtils.hasText(context.getTenantId())) {
            throw AppException.of(ErrorCodes.BAD_REQUEST, "tenantId is required");
        }
        if (request == null || !StringUtils.hasText(request.getQuestion())) {
            throw AppException.of(ErrorCodes.BAD_REQUEST, "question is required");
        }
    }
}
