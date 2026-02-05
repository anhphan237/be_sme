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
import dev.langchain4j.model.chat.ChatLanguageModel;
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

    private static final String SYSTEM_PROMPT =
            "You are an HR assistant for the company. Answer the employee's question strictly based on the provided company documents. "
                    + "If the information is not in the documents, say you don't know.";

    private final ObjectMapper objectMapper;
    private final ContentFacade contentFacade;
    private final ChatLanguageModel chatModel;

    @Override
    protected Object doProcess(BizContext context, JsonNode payload) {
        AssistantAskRequest request = objectMapper.convertValue(payload, AssistantAskRequest.class);
        validate(context, request);

        String question = request.getQuestion().trim();

        // RAG: Retrieve documents for this tenant (Document Library)
        DocumentListResponse listResponse = contentFacade.listDocuments(new DocumentListRequest());
        List<DocumentListResponse.DocumentItem> allDocs = listResponse.getItems() != null ? listResponse.getItems() : new ArrayList<>();

        // Build context chunks from documents (name + description)
        List<DocumentListResponse.DocumentItem> relevant = retrieveRelevantChunks(allDocs, question);
        String documentsContext = buildDocumentsContext(relevant);
        List<String> sourceNames = relevant.stream()
                .map(DocumentListResponse.DocumentItem::getName)
                .collect(Collectors.toList());

        String answer;
        if (!StringUtils.hasText(documentsContext)) {
            answer = "I don't have access to any company documents for your tenant. Please ask HR to upload relevant documents (e.g. Employee Handbook, WiFi/Parking info) to the Document Library.";
        } else {
            String userPrompt = SYSTEM_PROMPT + "\n\nCompany documents (use only this information to answer):\n\n" + documentsContext + "\n\nEmployee question: " + question;
            try {
                answer = chatModel.generate(userPrompt);
            } catch (Exception e) {
                throw AppException.of(ErrorCodes.INTERNAL_ERROR, "AI assistant failed: " + e.getMessage());
            }
        }

        AssistantAskResponse response = new AssistantAskResponse();
        response.setAnswer(answer);
        response.setSourceDocumentNames(sourceNames);
        return response;
    }

    private List<DocumentListResponse.DocumentItem> retrieveRelevantChunks(
            List<DocumentListResponse.DocumentItem> docs, String question) {
        if (docs.isEmpty()) return new ArrayList<>();
        String q = question.toLowerCase(Locale.ROOT);
        List<DocumentListResponse.DocumentItem> matched = docs.stream()
                .filter(d -> {
                    String name = d.getName() != null ? d.getName().toLowerCase(Locale.ROOT) : "";
                    String desc = d.getDescription() != null ? d.getDescription().toLowerCase(Locale.ROOT) : "";
                    return name.contains(q) || desc.contains(q) || matchesTopic(q, name, desc);
                })
                .collect(Collectors.toList());
        return matched.isEmpty() ? docs : matched;
    }

    private boolean matchesTopic(String question, String name, String desc) {
        String combined = name + " " + desc;
        if (question.contains("wifi") || question.contains("wi-fi")) return combined.contains("wifi") || combined.contains("wi-fi") || combined.contains("network");
        if (question.contains("parking")) return combined.contains("parking") || combined.contains("park");
        if (question.contains("regulation") || question.contains("policy") || question.contains("handbook")) return combined.contains("regulation") || combined.contains("policy") || combined.contains("handbook") || combined.contains("internal");
        return false;
    }

    private String buildDocumentsContext(List<DocumentListResponse.DocumentItem> items) {
        StringBuilder sb = new StringBuilder();
        for (DocumentListResponse.DocumentItem d : items) {
            sb.append("- Document: ").append(d.getName() != null ? d.getName() : "(no title)");
            if (StringUtils.hasText(d.getDescription())) sb.append("\n  Description: ").append(d.getDescription());
            sb.append("\n");
        }
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
