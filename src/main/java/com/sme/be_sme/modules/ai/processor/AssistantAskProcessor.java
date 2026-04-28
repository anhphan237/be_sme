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
import java.util.concurrent.*;
import java.util.concurrent.locks.LockSupport;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
public class AssistantAskProcessor extends BaseBizProcessor<BizContext> {

    private static final String SYSTEM_BLOCK = """
[SYSTEM]
You are an AI assistant for a company knowledge system.
Answer ONLY based on the provided context.
If the answer is not in the context, say "I don't know".
Keep answers concise and factual.
""";

    private static final int TOP_K_CHUNKS = 4;
    private static final int MIN_TOKEN_LENGTH = 3;
    private static final int MAX_HISTORY_MESSAGES = 5;
    private static final int MAX_CHUNK_TOKENS = 420;
    private static final int MIN_CHUNK_TOKENS = 140;
    private static final int CHUNK_TRIM_STEP_TOKENS = 60;
    private static final int MAX_HISTORY_MESSAGE_TOKENS = 120;
    private static final int MAX_PROMPT_TOKENS = 4000;
    private static final int RESERVED_OUTPUT_TOKENS = 700;
    private static final int MAX_AI_RETRIES = 3;
    private static final long RETRY_BASE_MS = 1000L;
    private static final int MAX_DB_CHUNKS = 2000;
    private static final long CACHE_TTL_MS = TimeUnit.MINUTES.toMillis(10);
    private static final double REQUESTS_PER_SECOND_PER_USER = 2.0d;

    private final ObjectMapper objectMapper;
    private final DocumentChunkMapper documentChunkMapper;
    private final DocumentMapper documentMapper;
    private final ChatMessageMapper chatMessageMapper;
    private final ChatSessionMapper chatSessionMapper;
    private final ChatLanguageModel chatModel;
    private final ConcurrentHashMap<String, SimpleRateLimiter> operatorRateLimiters = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, CacheEntry> answerCache = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, CompletableFuture<AnswerBundle>> inFlightRequests = new ConcurrentHashMap<>();

    @Override
    protected Object doProcess(BizContext context, JsonNode payload) {
        AssistantAskRequest request = objectMapper.convertValue(payload, AssistantAskRequest.class);
        validate(context, request);

        String companyId = context.getTenantId();
        String operatorId = context.getOperatorId();
        String question = request.getQuestion().trim();
        String normalizedQuestion = normalizeForCache(question);
        String cacheKey = buildCacheKey(companyId, normalizedQuestion);

        String chatSessionId = StringUtils.hasText(request.getChatSessionId()) ? request.getChatSessionId().trim() : null;
        if (chatSessionId != null) {
            validateSessionOwnership(companyId, operatorId, chatSessionId);
        }

        CacheEntry cached = getCache(cacheKey);
        if (cached != null) {
            if (chatSessionId != null) {
                saveMessages(companyId, chatSessionId, operatorId, question, cached.answer());
            }
            AssistantAskResponse response = new AssistantAskResponse();
            response.setAnswer(cached.answer());
            response.setSourceDocumentNames(cached.sourceDocumentNames());
            response.setChatSessionId(chatSessionId);
            return response;
        }

        AnswerBundle bundle = getOrComputeInFlight(cacheKey, () -> {
            List<DocumentChunkEntity> allChunks = loadChunksWithLimit(companyId, MAX_DB_CHUNKS);
            Map<String, String> documentIdToTitle = loadDocumentTitles(companyId, allChunks);

            List<DocumentChunkEntity> topChunks = selectTopChunksByLexicalScore(allChunks, question, TOP_K_CHUNKS);
            if (topChunks.isEmpty()) {
                return new AnswerBundle(
                        "I don't know based on the available company documents.",
                        new ArrayList<>()
                );
            }

            List<ChunkView> chunkViews = topChunks.stream()
                    .map(c -> new ChunkView(
                            documentIdToTitle.getOrDefault(c.getDocumentId(), "(no title)"),
                            c.getChunkText()
                    ))
                    .collect(Collectors.toList());
            List<MessageView> history = loadConversationHistory(chatSessionId);
            String prompt = buildPrompt(chunkViews, history, question);
            String answer = callGeminiWithRetry(prompt, operatorId);
            List<String> sourceDocumentNames = trimChunks(chunkViews, TOP_K_CHUNKS, MAX_CHUNK_TOKENS).stream()
                    .map(ChunkView::documentName)
                    .collect(Collectors.toCollection(LinkedHashSet::new))
                    .stream()
                    .toList();
            return new AnswerBundle(answer, sourceDocumentNames);
        });

        if (chatSessionId != null) {
            saveMessages(companyId, chatSessionId, operatorId, question, bundle.answer());
        }

        AssistantAskResponse response = new AssistantAskResponse();
        response.setAnswer(bundle.answer());
        response.setSourceDocumentNames(bundle.sourceDocumentNames());
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

    private List<MessageView> loadConversationHistory(String chatSessionId) {
        if (chatSessionId == null) return new ArrayList<>();
        List<ChatMessageEntity> messages = chatMessageMapper.selectBySessionIdOrderByCreatedAt(chatSessionId);
        if (messages == null || messages.isEmpty()) return new ArrayList<>();
        int fromIdx = Math.max(0, messages.size() - MAX_HISTORY_MESSAGES);
        List<MessageView> history = new ArrayList<>();
        for (int i = fromIdx; i < messages.size(); i++) {
            ChatMessageEntity m = messages.get(i);
            String role = "USER".equalsIgnoreCase(m.getSender()) ? "User" : "Assistant";
            history.add(new MessageView(role, m.getContent() != null ? m.getContent() : ""));
        }
        return history;
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

    private String callGeminiWithRetry(String prompt, String operatorId) {
        for (int attempt = 0; attempt <= MAX_AI_RETRIES; attempt++) {
            try {
                acquirePerUserPermit(operatorId);
                return chatModel.generate(prompt);
            } catch (Exception e) {
                if (!isRateLimited(e)) {
                    throw AppException.of(ErrorCodes.INTERNAL_ERROR, "AI assistant failed: " + e.getMessage());
                }
                if (attempt >= MAX_AI_RETRIES) {
                    throw AppException.of(ErrorCodes.LIMIT_EXCEEDED,
                            "AI assistant is busy (rate limit reached). Please try again shortly.");
                }
                long baseDelay = RETRY_BASE_MS * (1L << attempt);
                long jitterMs = ThreadLocalRandom.current().nextLong(0L, 1001L);
                sleepQuietly(baseDelay + jitterMs);
            }
        }
        throw AppException.of(ErrorCodes.INTERNAL_ERROR, "AI assistant failed: unknown retry state");
    }

    private String buildPrompt(List<ChunkView> chunks, List<MessageView> history, String question) {
        String safeQuestion = question == null ? "" : question.trim();

        List<ChunkView> selectedChunks = trimChunks(chunks, TOP_K_CHUNKS, MAX_CHUNK_TOKENS);
        List<MessageView> selectedHistory = trimHistory(history, MAX_HISTORY_MESSAGES, MAX_HISTORY_MESSAGE_TOKENS);

        String contextBlock = formatContext(selectedChunks);
        String historyBlock = formatHistory(selectedHistory);
        String prompt = formatPrompt(contextBlock, historyBlock, safeQuestion);

        int promptBudget = MAX_PROMPT_TOKENS - RESERVED_OUTPUT_TOKENS;
        if (estimateTokens(prompt) <= promptBudget) {
            return prompt;
        }

        List<MessageView> historyAdaptive = new ArrayList<>(selectedHistory);
        while (!historyAdaptive.isEmpty()) {
            historyAdaptive.remove(0); // drop oldest first
            prompt = formatPrompt(contextBlock, formatHistory(historyAdaptive), safeQuestion);
            if (estimateTokens(prompt) <= promptBudget) {
                return prompt;
            }
        }

        int adaptiveChunkTokens = MAX_CHUNK_TOKENS;
        List<ChunkView> chunkAdaptive = new ArrayList<>(selectedChunks);
        while (!chunkAdaptive.isEmpty() && adaptiveChunkTokens > MIN_CHUNK_TOKENS) {
            adaptiveChunkTokens -= CHUNK_TRIM_STEP_TOKENS;
            int perChunkLimit = adaptiveChunkTokens;
            chunkAdaptive = chunkAdaptive.stream()
                    .map(c -> new ChunkView(c.documentName(), truncateByTokens(c.text(), perChunkLimit)))
                    .toList();
            prompt = formatPrompt(formatContext(chunkAdaptive), "", safeQuestion);
            if (estimateTokens(prompt) <= promptBudget) {
                return prompt;
            }
        }

        return formatPrompt("", "", safeQuestion);
    }

    private List<ChunkView> trimChunks(List<ChunkView> chunks, int topK, int maxChunkTokens) {
        if (chunks == null || chunks.isEmpty()) return new ArrayList<>();

        List<ChunkView> normalized = chunks.stream()
                .filter(Objects::nonNull)
                .map(c -> new ChunkView(
                        StringUtils.hasText(c.documentName()) ? c.documentName().trim() : "(no title)",
                        normalizeWhitespace(c.text())
                ))
                .filter(c -> StringUtils.hasText(c.text()))
                .map(c -> new ChunkView(c.documentName(), truncateByTokens(c.text(), maxChunkTokens)))
                .toList();

        List<ChunkView> deduped = new ArrayList<>();
        for (ChunkView c : normalized) {
            boolean nearDuplicate = deduped.stream().anyMatch(d -> similarity(d.text(), c.text()) >= 0.85d);
            if (!nearDuplicate) {
                deduped.add(c);
            }
            if (deduped.size() >= topK) {
                break;
            }
        }
        return deduped;
    }

    private List<MessageView> trimHistory(List<MessageView> history, int maxMessages, int maxMsgTokens) {
        if (history == null || history.isEmpty()) return new ArrayList<>();

        int fromIdx = Math.max(0, history.size() - maxMessages);
        List<MessageView> latest = history.subList(fromIdx, history.size());
        return latest.stream()
                .filter(Objects::nonNull)
                .map(m -> new MessageView(
                        "Assistant".equalsIgnoreCase(m.role()) ? "Assistant" : "User",
                        truncateByTokens(normalizeWhitespace(m.content()), maxMsgTokens)
                ))
                .filter(m -> StringUtils.hasText(m.content()))
                .toList();
    }

    private String formatPrompt(String contextBlock, String historyBlock, String question) {
        return SYSTEM_BLOCK.trim()
                + "\n\n[CONTEXT]\n" + safeBlock(contextBlock)
                + "\n\n[HISTORY]\n" + safeBlock(historyBlock)
                + "\n\n[QUESTION]\n" + safeBlock(question)
                + "\n\n[ANSWER]";
    }

    private String formatContext(List<ChunkView> chunks) {
        if (chunks == null || chunks.isEmpty()) return "";
        StringBuilder sb = new StringBuilder();
        for (ChunkView c : chunks) {
            sb.append("--- Document: ").append(c.documentName()).append("\n")
                    .append(c.text()).append("\n")
                    .append("---\n");
        }
        return sb.toString().trim();
    }

    private String formatHistory(List<MessageView> history) {
        if (history == null || history.isEmpty()) return "";
        return history.stream()
                .map(m -> m.role() + ": " + m.content())
                .collect(Collectors.joining("\n"));
    }

    private static String safeBlock(String block) {
        return StringUtils.hasText(block) ? block.trim() : "";
    }

    private static String normalizeWhitespace(String s) {
        if (!StringUtils.hasText(s)) return "";
        return s.replaceAll("\\s+", " ").trim();
    }

    private static int estimateTokens(String text) {
        if (!StringUtils.hasText(text)) return 0;
        return (int) Math.ceil(text.length() / 3.5d);
    }

    private static String truncateByTokens(String text, int tokenLimit) {
        if (!StringUtils.hasText(text)) return "";
        int charLimit = Math.max(1, (int) Math.ceil(tokenLimit * 3.5d));
        if (text.length() <= charLimit) return text;
        return text.substring(0, charLimit) + "...";
    }

    private static double similarity(String a, String b) {
        Set<String> sa = new HashSet<>(Arrays.asList(normalizeWhitespace(a).toLowerCase(Locale.ROOT).split("\\W+")));
        Set<String> sb = new HashSet<>(Arrays.asList(normalizeWhitespace(b).toLowerCase(Locale.ROOT).split("\\W+")));
        sa.removeIf(token -> token.length() < MIN_TOKEN_LENGTH);
        sb.removeIf(token -> token.length() < MIN_TOKEN_LENGTH);
        if (sa.isEmpty() || sb.isEmpty()) return 0d;

        Set<String> inter = new HashSet<>(sa);
        inter.retainAll(sb);
        Set<String> union = new HashSet<>(sa);
        union.addAll(sb);
        return union.isEmpty() ? 0d : (double) inter.size() / union.size();
    }

    private static boolean isRateLimited(Exception e) {
        String message = e.getMessage();
        if (!StringUtils.hasText(message)) {
            return false;
        }
        String normalized = message.toUpperCase(Locale.ROOT);
        return normalized.contains("RESOURCE_EXHAUSTED")
                || normalized.contains("CODE 429")
                || normalized.contains("429");
    }

    private static void sleepQuietly(long millis) {
        try {
            Thread.sleep(millis);
        } catch (InterruptedException ie) {
            Thread.currentThread().interrupt();
        }
    }

    private void acquirePerUserPermit(String operatorId) {
        String key = StringUtils.hasText(operatorId) ? operatorId.trim() : "anonymous";
        SimpleRateLimiter limiter = operatorRateLimiters.computeIfAbsent(
                key,
                k -> new SimpleRateLimiter(REQUESTS_PER_SECOND_PER_USER)
        );
        limiter.acquire();
    }

    private List<DocumentChunkEntity> loadChunksWithLimit(String companyId, int limit) {
        List<DocumentChunkEntity> all = documentChunkMapper.selectByCompanyId(companyId);
        if (all == null || all.isEmpty()) return new ArrayList<>();
        if (all.size() <= limit) return all;
        return new ArrayList<>(all.subList(0, limit));
    }

    private AnswerBundle getOrComputeInFlight(String cacheKey, Computation computation) {
        CacheEntry cached = getCache(cacheKey);
        if (cached != null) {
            return new AnswerBundle(cached.answer(), cached.sourceDocumentNames());
        }

        CompletableFuture<AnswerBundle> myFuture = new CompletableFuture<>();
        CompletableFuture<AnswerBundle> existing = inFlightRequests.putIfAbsent(cacheKey, myFuture);
        if (existing != null) {
            try {
                return existing.get();
            } catch (InterruptedException ie) {
                Thread.currentThread().interrupt();
                throw AppException.of(ErrorCodes.INTERNAL_ERROR, "AI assistant request interrupted");
            } catch (ExecutionException ee) {
                Throwable cause = ee.getCause();
                if (cause instanceof AppException ae) {
                    throw ae;
                }
                throw AppException.of(ErrorCodes.INTERNAL_ERROR, "AI assistant failed: " + (cause != null ? cause.getMessage() : ee.getMessage()));
            }
        }

        try {
            AnswerBundle result = computation.compute();
            putCache(cacheKey, result);
            myFuture.complete(result);
            return result;
        } catch (RuntimeException ex) {
            myFuture.completeExceptionally(ex);
            throw ex;
        } finally {
            inFlightRequests.remove(cacheKey, myFuture);
        }
    }

    private CacheEntry getCache(String cacheKey) {
        CacheEntry entry = answerCache.get(cacheKey);
        if (entry == null) return null;
        if (entry.expiresAtMs() <= System.currentTimeMillis()) {
            answerCache.remove(cacheKey, entry);
            return null;
        }
        return entry;
    }

    private void putCache(String cacheKey, AnswerBundle result) {
        long expiresAt = System.currentTimeMillis() + CACHE_TTL_MS;
        answerCache.put(cacheKey, new CacheEntry(
                result.answer(),
                result.sourceDocumentNames() != null ? result.sourceDocumentNames() : new ArrayList<>(),
                expiresAt
        ));
    }

    private static String buildCacheKey(String companyId, String normalizedQuestion) {
        return companyId + "::" + normalizedQuestion;
    }

    private static String normalizeForCache(String question) {
        if (!StringUtils.hasText(question)) return "";
        return question.toLowerCase(Locale.ROOT).replaceAll("\\s+", " ").trim();
    }

    private static final class ScoredChunk {
        final DocumentChunkEntity chunk;
        final int score;

        ScoredChunk(DocumentChunkEntity chunk, int score) {
            this.chunk = chunk;
            this.score = score;
        }
    }

    private record ChunkView(String documentName, String text) {}

    private record MessageView(String role, String content) {}

    private record AnswerBundle(String answer, List<String> sourceDocumentNames) {}

    private record CacheEntry(String answer, List<String> sourceDocumentNames, long expiresAtMs) {}

    @FunctionalInterface
    private interface Computation {
        AnswerBundle compute();
    }

    private static final class SimpleRateLimiter {
        private final long intervalNanos;
        private long nextFreeNanos;

        private SimpleRateLimiter(double permitsPerSecond) {
            this.intervalNanos = (long) (1_000_000_000d / permitsPerSecond);
            this.nextFreeNanos = System.nanoTime();
        }

        synchronized void acquire() {
            long now = System.nanoTime();
            if (nextFreeNanos < now) {
                nextFreeNanos = now;
            }
            long waitNanos = nextFreeNanos - now;
            nextFreeNanos += intervalNanos;
            if (waitNanos > 0) {
                LockSupport.parkNanos(waitNanos);
            }
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
