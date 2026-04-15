package com.sme.be_sme.modules.ai.processor;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sme.be_sme.modules.ai.api.request.AssistantAskRequest;
import com.sme.be_sme.modules.ai.api.response.AssistantAskResponse;
import com.sme.be_sme.modules.document.infrastructure.mapper.DocumentChunkMapper;
import com.sme.be_sme.modules.document.infrastructure.mapper.DocumentMapper;
import com.sme.be_sme.modules.knowledge.infrastructure.mapper.ChatMessageMapper;
import com.sme.be_sme.modules.knowledge.infrastructure.mapper.ChatSessionMapper;
import com.sme.be_sme.modules.knowledge.infrastructure.persistence.entity.ChatMessageEntity;
import com.sme.be_sme.modules.knowledge.infrastructure.persistence.entity.ChatSessionEntity;
import com.sme.be_sme.modules.document.infrastructure.persistence.entity.DocumentChunkEntity;
import com.sme.be_sme.modules.document.infrastructure.persistence.entity.DocumentEntity;
import com.sme.be_sme.shared.constant.ErrorCodes;
import com.sme.be_sme.shared.exception.AppException;
import com.sme.be_sme.shared.gateway.core.BaseBizProcessor;
import com.sme.be_sme.shared.gateway.core.BizContext;
import com.sme.be_sme.shared.util.UuidGenerator;
import dev.langchain4j.model.chat.ChatLanguageModel;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.util.*;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
public class AssistantAskProcessor extends BaseBizProcessor<BizContext> {

    private static final String SYSTEM_PROMPT =
            "You are an HR assistant for the company. Answer the employee's question strictly based on the provided company documents. "
                    + "If the information is not in the documents, say you don't know. "
                    + "Respond in the same language as the user's question (Vietnamese or English). "
                    + "Be helpful, professional, and explain clearly with appropriate context when useful.";

    private static final int TOP_K_CHUNKS = 6;
    private static final int MIN_TOKEN_LENGTH = 3;
    private static final int MAX_HISTORY_MESSAGES = 10;

    private final ObjectMapper objectMapper;
    private final DocumentChunkMapper documentChunkMapper;
    private final DocumentMapper documentMapper;
    private final ChatMessageMapper chatMessageMapper;
    private final ChatSessionMapper chatSessionMapper;
    private final ChatLanguageModel chatModel;

    @Override
    protected Object doProcess(BizContext context, JsonNode payload) {
        AssistantAskRequest request = objectMapper.convertValue(payload, AssistantAskRequest.class);
        validate(context, request);

        String companyId = context.getTenantId();
        String operatorId = context.getOperatorId();
        String question = request.getQuestion().trim();

        String chatSessionId = StringUtils.hasText(request.getChatSessionId()) ? request.getChatSessionId().trim() : null;
        if (chatSessionId != null) {
            validateSessionOwnership(companyId, operatorId, chatSessionId);
        }

        List<DocumentChunkEntity> allChunks = documentChunkMapper.selectByCompanyId(companyId);
        if (allChunks == null) {
            allChunks = new ArrayList<>();
        }

        Map<String, String> documentIdToTitle = loadDocumentTitles(companyId, allChunks);

        List<DocumentChunkEntity> topChunks = selectTopChunksByLexicalScore(allChunks, question, TOP_K_CHUNKS);
        String answer;
        List<String> sourceDocumentNames;

        if (topChunks.isEmpty()) {
            answer = "I don't know based on the available company documents.";
            sourceDocumentNames = new ArrayList<>();
        } else {
            String documentsExcerpts = buildExcerptsPrompt(topChunks, documentIdToTitle);
            String conversationHistory = buildConversationHistory(chatSessionId);
            String userPrompt = SYSTEM_PROMPT + "\n\nCompany document excerpts (use only this to answer):\n\n"
                    + documentsExcerpts
                    + (conversationHistory.isEmpty() ? "" : "\n\n--- Conversation history ---\n" + conversationHistory + "--- End history ---\n\n")
                    + "Employee question: " + question;
            try {
                answer = chatModel.generate(userPrompt);
            } catch (Exception e) {
                throw AppException.of(ErrorCodes.INTERNAL_ERROR, "AI assistant failed: " + e.getMessage());
            }
            sourceDocumentNames = topChunks.stream()
                    .map(c -> documentIdToTitle.getOrDefault(c.getDocumentId(), "(no title)"))
                    .collect(Collectors.toCollection(LinkedHashSet::new))
                    .stream()
                    .toList();
        }

        if (chatSessionId != null) {
            saveMessages(companyId, chatSessionId, operatorId, question, answer);
        }

        AssistantAskResponse response = new AssistantAskResponse();
        response.setAnswer(answer);
        response.setSourceDocumentNames(sourceDocumentNames);
        response.setChatSessionId(chatSessionId);
        return response;
    }

    private void validateSessionOwnership(String companyId, String userId, String chatSessionId) {
        ChatSessionEntity session = chatSessionMapper.selectByPrimaryKey(chatSessionId);
        if (session == null) {
            throw AppException.of(ErrorCodes.NOT_FOUND, "Chat session not found");
        }
        if (!companyId.equals(session.getCompanyId()) || !userId.equals(session.getUserId())) {
            throw AppException.of(ErrorCodes.FORBIDDEN, "Chat session does not belong to user");
        }
    }

    private String buildConversationHistory(String chatSessionId) {
        if (chatSessionId == null) return "";
        List<ChatMessageEntity> messages = chatMessageMapper.selectBySessionIdOrderByCreatedAt(chatSessionId);
        if (messages == null || messages.isEmpty()) return "";
        int fromIdx = Math.max(0, messages.size() - MAX_HISTORY_MESSAGES);
        StringBuilder sb = new StringBuilder();
        for (int i = fromIdx; i < messages.size(); i++) {
            ChatMessageEntity m = messages.get(i);
            String role = "USER".equals(m.getSender()) ? "User" : "Assistant";
            sb.append(role).append(": ").append(m.getContent() != null ? m.getContent() : "").append("\n");
        }
        return sb.toString();
    }

    private void saveMessages(String companyId, String chatSessionId, String operatorId, String question, String answer) {
        Date now = new Date();
        ChatMessageEntity userMsg = new ChatMessageEntity();
        userMsg.setChatMessageId(UuidGenerator.generate());
        userMsg.setCompanyId(companyId);
        userMsg.setChatSessionId(chatSessionId);
        userMsg.setSender("USER");
        userMsg.setContent(question);
        userMsg.setCreatedAt(now);
        chatMessageMapper.insert(userMsg);

        ChatMessageEntity botMsg = new ChatMessageEntity();
        botMsg.setChatMessageId(UuidGenerator.generate());
        botMsg.setCompanyId(companyId);
        botMsg.setChatSessionId(chatSessionId);
        botMsg.setSender("BOT");
        botMsg.setContent(answer);
        botMsg.setCreatedAt(now);
        chatMessageMapper.insert(botMsg);
    }

    private Map<String, String> loadDocumentTitles(String companyId, List<DocumentChunkEntity> chunks) {
        Set<String> documentIds = chunks.stream()
                .map(DocumentChunkEntity::getDocumentId)
                .filter(StringUtils::hasText)
                .collect(Collectors.toSet());
        return documentIds.stream()
                .map(documentMapper::selectByPrimaryKey)
                .filter(Objects::nonNull)
                .collect(Collectors.toMap(DocumentEntity::getDocumentId, d -> d.getTitle() != null ? d.getTitle() : "(no title)", (a, b) -> a));
    }

    private List<DocumentChunkEntity> selectTopChunksByLexicalScore(List<DocumentChunkEntity> chunks, String question, int topK) {
        if (chunks.isEmpty()) return new ArrayList<>();
        List<String> queryTokens = tokenize(question);
        if (queryTokens.isEmpty()) {
            return new ArrayList<>();
        }

        List<ScoredChunk> scored = chunks.stream()
                .map(c -> new ScoredChunk(c, scoreChunk(c.getChunkText(), queryTokens)))
                .filter(s -> s.score > 0)
                .sorted(Comparator.comparingInt((ScoredChunk s) -> s.score).reversed())
                .limit(topK)
                .toList();

        if (!scored.isEmpty()) {
            return scored.stream().map(s -> s.chunk).toList();
        }
        return new ArrayList<>();
    }

    private static List<String> tokenize(String text) {
        if (text == null || text.isBlank()) return new ArrayList<>();
        List<String> tokens = new ArrayList<>();
        for (String s : text.toLowerCase(Locale.ROOT).split("\\W+")) {
            if (s.length() >= MIN_TOKEN_LENGTH) {
                tokens.add(s);
            }
        }
        return tokens;
    }

    private static int scoreChunk(String chunkText, List<String> queryTokens) {
        if (chunkText == null || queryTokens.isEmpty()) return 0;
        String lower = chunkText.toLowerCase(Locale.ROOT);
        int score = 0;
        for (String token : queryTokens) {
            int idx = 0;
            while ((idx = lower.indexOf(token, idx)) >= 0) {
                score++;
                idx += token.length();
            }
        }
        return score;
    }

    private static String buildExcerptsPrompt(List<DocumentChunkEntity> topChunks, Map<String, String> documentIdToTitle) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < topChunks.size(); i++) {
            DocumentChunkEntity c = topChunks.get(i);
            String title = documentIdToTitle.getOrDefault(c.getDocumentId(), "(no title)");
            sb.append("[").append(i + 1).append("] Source: ").append(title)
                    .append(" (chunk ").append(c.getChunkNo()).append(")\n")
                    .append(c.getChunkText()).append("\n\n");
        }
        return sb.toString();
    }

    private static final class ScoredChunk {
        final DocumentChunkEntity chunk;
        final int score;

        ScoredChunk(DocumentChunkEntity chunk, int score) {
            this.chunk = chunk;
            this.score = score;
        }
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
