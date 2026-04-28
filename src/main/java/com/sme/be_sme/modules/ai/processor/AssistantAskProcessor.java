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
import com.google.common.util.concurrent.RateLimiter;
import dev.langchain4j.model.chat.ChatLanguageModel;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.util.*;
import java.util.concurrent.*;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
@Slf4j
public class AssistantAskProcessor extends BaseBizProcessor<BizContext> {

    private static final String SYSTEM_BLOCK = """
[SYSTEM]
You are a company knowledge assistant.

Your goal is to explain clearly based on the provided context.

Language rule (VERY IMPORTANT):
- Detect the language of the user's question
- If the user writes in Vietnamese -> respond ONLY in Vietnamese
- If the user writes in English -> respond ONLY in English
- NEVER mix languages in a single response

Answering rules:
- Always try to answer using the available context
- If context is incomplete, infer and complete the meaning logically
- Prefer a reasonable explanation over refusing to answer
- Be clear, specific, and natural

Avoid:
- Saying "I couldn't find exact matching content"
- Giving generic template answers
- Being overly defensive
- Mixing Vietnamese and English

Always produce a complete and natural answer.
Always answer in the same language as the user's question.
""";

    private static final int INITIAL_CANDIDATE_K = 15;
    private static final int OVERVIEW_CANDIDATE_K = 15;
    private static final int FINAL_CONTEXT_K = 6;
    private static final int MIN_TOKEN_LENGTH = 3;
    private static final int MAX_HISTORY_MESSAGES = 5;
    private static final int MAX_CHUNK_TOKENS = 420;
    private static final int MIN_CHUNK_TOKENS = 140;
    private static final int CHUNK_TRIM_STEP_TOKENS = 60;
    private static final int MAX_HISTORY_MESSAGE_TOKENS = 120;
    private static final int MAX_PROMPT_TOKENS = 4000;
    private static final int RESERVED_OUTPUT_TOKENS = 700;
    private static final int MAX_AI_RETRIES = 1;
    private static final long RETRY_BASE_MS = 1000L;
    private static final int MAX_DB_CHUNKS = 2000;
    private static final long CACHE_TTL_MS = TimeUnit.MINUTES.toMillis(10);
    private static final double REQUESTS_PER_SECOND_PER_USER = 2.0d;
    private static final int FOLLOWUP_FALLBACK_K = 5;
    private static final int MERGED_CHUNKS_LIMIT = 8;
    private static final Set<String> FOLLOW_UP_KEYWORDS = Set.of(
            "gì", "nào", "đó", "về", "cái gì", "nền gì", "gi", "nao", "do", "ve",
            "what", "which", "that", "it", "about", "more"
    );
    private static final Set<String> OVERVIEW_KEYWORDS = Set.of(
            "tổng quan", "tong quan", "khái quát", "khai quat", "giới thiệu", "gioi thieu",
            "overview", "summary", "introduce", "introduction", "policy", "guideline"
    );
    private static final Set<String> SPECIFIC_HINT_KEYWORDS = Set.of(
            "là gì", "bao nhiêu", "khi nào", "ở đâu", "như thế nào",
            "what", "where", "when", "how", "which", "who"
    );
    private static final Map<String, List<String>> SYNONYM_MAP = Map.of(
            "tổng quan", List.of("overview", "summary", "policy", "guideline", "process"),
            "tong quan", List.of("overview", "summary", "policy", "guideline", "process"),
            "quy định", List.of("rule", "policy", "regulation"),
            "quy dinh", List.of("rule", "policy", "regulation"),
            "hướng dẫn", List.of("guide", "instruction", "manual"),
            "huong dan", List.of("guide", "instruction", "manual"),
            "chính sách", List.of("policy", "rule", "guideline"),
            "chinh sach", List.of("policy", "rule", "guideline"),
            "onboarding", List.of("orientation", "new hire", "welcome process")
    );

    private final ObjectMapper objectMapper;
    private final DocumentChunkMapper documentChunkMapper;
    private final DocumentMapper documentMapper;
    private final ChatMessageMapper chatMessageMapper;
    private final ChatSessionMapper chatSessionMapper;
    private final ChatLanguageModel chatModel;
    private final RateLimiter globalLimiter = RateLimiter.create(1.0d);
    private volatile long globalCooldownUntil = 0L;
    private final ConcurrentHashMap<String, RateLimiter> operatorRateLimiters = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, CacheEntry> answerCache = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, CompletableFuture<AnswerBundle>> inFlightRequests = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, ConversationState> conversationStateStore = new ConcurrentHashMap<>();

    @Override
    protected Object doProcess(BizContext context, JsonNode payload) {
        AssistantAskRequest request = objectMapper.convertValue(payload, AssistantAskRequest.class);
        validate(context, request);

        String companyId = context.getTenantId();
        String operatorId = context.getOperatorId();
        String question = request.getQuestion().trim();
        String normalizedQuestion = normalizeForCache(question);
        String cacheKey = buildCacheKey(companyId, normalizedQuestion);
        String questionHash = Integer.toHexString(normalizedQuestion.hashCode());
        boolean followUp = isFollowUp(question);
        QuestionIntent intent = detectIntent(question);
        String rewrittenQuery = rewriteQuery(question);
        log.info("Assistant intent: companyId={}, operatorId={}, questionHash={}, intent={}",
                companyId, operatorId, questionHash, intent);
        log.info("Assistant follow-up detection: companyId={}, operatorId={}, questionHash={}, isFollowUp={}",
                companyId, operatorId, questionHash, followUp);
        log.debug("Assistant rewritten query metadata: companyId={}, questionHash={}, originalLen={}, rewrittenLen={}",
                companyId, questionHash, question.length(), rewrittenQuery.length());

        String chatSessionId = StringUtils.hasText(request.getChatSessionId()) ? request.getChatSessionId().trim() : null;
        if (chatSessionId != null) {
            validateSessionOwnership(companyId, operatorId, chatSessionId);
        }
        String conversationKey = buildConversationKey(companyId, operatorId, chatSessionId);

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
            List<MessageView> history = loadConversationHistory(chatSessionId);
            ConversationState previousState = conversationStateStore.get(conversationKey);
            List<ChunkView> chunkViews;
            String previousAnswer = previousState != null ? previousState.lastAnswer() : extractLastAssistantAnswer(history);

            if (followUp) {
                List<ChunkView> previousChunks = previousState != null && previousState.lastChunks() != null
                        ? previousState.lastChunks()
                        : new ArrayList<>();
                chunkViews = new ArrayList<>(previousChunks);
                boolean contextWeak = isContextWeak(previousAnswer, chunkViews);
                boolean fallbackRetrievalTriggered = false;
                if (contextWeak) {
                    List<DocumentChunkEntity> allChunks = loadChunksWithLimit(companyId, MAX_DB_CHUNKS);
                    Map<String, String> documentIdToTitle = loadDocumentTitles(companyId, allChunks);
                    List<DocumentChunkEntity> freshChunks = selectTopChunksByLexicalScore(allChunks, rewrittenQuery, FOLLOWUP_FALLBACK_K);
                    List<ChunkView> freshChunkViews = freshChunks.stream()
                            .map(c -> new ChunkView(
                                    documentIdToTitle.getOrDefault(c.getDocumentId(), "(no title)"),
                                    c.getChunkText()
                            ))
                            .collect(Collectors.toList());
                    chunkViews = mergeChunks(chunkViews, freshChunkViews, MERGED_CHUNKS_LIMIT);
                    fallbackRetrievalTriggered = !freshChunkViews.isEmpty();
                }
                log.info("Assistant follow-up context reuse: companyId={}, questionHash={}, contextWeak={}, fallbackRetrievalTriggered={}, finalChunkCount={}",
                        companyId, questionHash, contextWeak, fallbackRetrievalTriggered, chunkViews.size());
                if (chunkViews.isEmpty()) {
                    return buildFollowUpNoContextResponse(previousAnswer);
                }
            } else {
                List<DocumentChunkEntity> allChunks = loadChunksWithLimit(companyId, MAX_DB_CHUNKS);
                Map<String, String> documentIdToTitle = loadDocumentTitles(companyId, allChunks);

                List<DocumentChunkEntity> topChunks = retrieveChunksByIntent(allChunks, rewrittenQuery, intent);
                log.info("Assistant retrieval: companyId={}, questionHash={}, intent={}, chunksFound={}",
                        companyId, questionHash, intent, topChunks.size());
                if (topChunks.isEmpty()) {
                    log.info("Assistant fallback triggered: companyId={}, questionHash={}, reason=no_chunks", companyId, questionHash);
                    return buildFallbackResponse(question, allChunks, documentIdToTitle);
                }

                chunkViews = topChunks.stream()
                        .map(c -> new ChunkView(
                                documentIdToTitle.getOrDefault(c.getDocumentId(), "(no title)"),
                                c.getChunkText()
                        ))
                        .collect(Collectors.toList());
                previousAnswer = extractLastAssistantAnswer(history);
                log.info("Assistant retrieval path: companyId={}, questionHash={}, mode=new_chunks", companyId, questionHash);
            }

            String promptQuestion = intent == QuestionIntent.OVERVIEW
                    ? "Summarize the company's knowledge base based on the following documents.\nUser request: " + question
                    : question;
            String prompt = buildPrompt(chunkViews, history, promptQuestion, previousAnswer, followUp);
            if (System.currentTimeMillis() < globalCooldownUntil) {
                throw AppException.of(ErrorCodes.LIMIT_EXCEEDED,
                        "AI assistant is cooling down due to high traffic. Please try again shortly.");
            }
            globalLimiter.acquire();
            log.debug("Global limiter acquired before Gemini call");
            getLimiter(operatorId).acquire();
            String rawAnswer = callGeminiWithRetry(prompt, companyId, operatorId, questionHash);
            String answer = ensureAnswerCompleteness(rawAnswer, question, followUp, previousAnswer);
            boolean answerExpanded = !normalizeWhitespace(rawAnswer).equals(normalizeWhitespace(answer));
            List<ChunkView> selectedChunks = trimChunks(chunkViews, FINAL_CONTEXT_K, MAX_CHUNK_TOKENS);
            List<String> sourceDocumentNames = selectedChunks.stream()
                    .map(ChunkView::documentName)
                    .collect(Collectors.toCollection(LinkedHashSet::new))
                    .stream()
                    .toList();
            conversationStateStore.put(conversationKey, new ConversationState(answer, selectedChunks));
            log.info("Assistant answer quality: companyId={}, questionHash={}, answerLength={}, followUp={}, answerExpanded={}",
                    companyId, questionHash, answer.length(), followUp, answerExpanded);
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

    private List<DocumentChunkEntity> retrieveChunksByIntent(
            List<DocumentChunkEntity> allChunks,
            String rewrittenQuery,
            QuestionIntent intent
    ) {
        if (allChunks == null || allChunks.isEmpty()) {
            return new ArrayList<>();
        }
        if (intent == QuestionIntent.OVERVIEW) {
            List<DocumentChunkEntity> overviewChunks = selectTopChunksByLexicalScore(allChunks, rewrittenQuery, OVERVIEW_CANDIDATE_K);
            if (!overviewChunks.isEmpty()) {
                return overviewChunks;
            }
            // For broad overview questions, use a representative sample instead of hard-empty.
            int cap = Math.min(OVERVIEW_CANDIDATE_K, allChunks.size());
            return new ArrayList<>(allChunks.subList(0, cap));
        }
        return selectTopChunksByLexicalScore(allChunks, rewrittenQuery, INITIAL_CANDIDATE_K);
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

    private RateLimiter getLimiter(String operatorId) {
        String key = StringUtils.hasText(operatorId) ? operatorId.trim() : "anonymous";
        return operatorRateLimiters.computeIfAbsent(key, k -> RateLimiter.create(REQUESTS_PER_SECOND_PER_USER));
    }

    private String callGeminiWithRetry(String prompt, String companyId, String operatorId, String questionHash) {
        for (int attempt = 0; attempt <= MAX_AI_RETRIES; attempt++) {
            try {
                log.info("Gemini call: companyId={}, operatorId={}, questionHash={}, attempt={}",
                        companyId, operatorId, questionHash, attempt + 1);
                return chatModel.generate(prompt);
            } catch (Exception e) {
                if (!isRateLimited(e)) {
                    throw AppException.of(ErrorCodes.INTERNAL_ERROR, "AI assistant failed: " + e.getMessage());
                }
                globalCooldownUntil = System.currentTimeMillis() + 3000L;
                log.warn("Global cooldown activated due to 429");
                if (attempt >= MAX_AI_RETRIES) {
                    throw AppException.of(ErrorCodes.LIMIT_EXCEEDED,
                            "AI assistant is busy (rate limit reached). Please try again shortly.");
                }
                long baseDelay = RETRY_BASE_MS * (1L << attempt);
                long jitterMs = ThreadLocalRandom.current().nextLong(0L, 1001L);
                log.warn("Gemini 429 retry: companyId={}, operatorId={}, questionHash={}, retryInMs={}, attempt={}",
                        companyId, operatorId, questionHash, (baseDelay + jitterMs), attempt + 1);
                sleepQuietly(baseDelay + jitterMs);
            }
        }
        throw AppException.of(ErrorCodes.INTERNAL_ERROR, "AI assistant failed: unknown retry state");
    }

    private String buildPrompt(List<ChunkView> chunks, List<MessageView> history, String question, String previousAnswer, boolean followUp) {
        String safeQuestion = question == null ? "" : question.trim();

        List<ChunkView> selectedChunks = trimChunks(chunks, FINAL_CONTEXT_K, MAX_CHUNK_TOKENS);
        List<MessageView> selectedHistory = trimHistory(history, MAX_HISTORY_MESSAGES, MAX_HISTORY_MESSAGE_TOKENS);

        String contextBlock = formatContext(selectedChunks);
        String historyBlock = formatHistory(selectedHistory);
        String followUpBlock = followUp && StringUtils.hasText(previousAnswer)
                ? "Previous answer:\n" + previousAnswer
                + "\n\nContinue the explanation."
                + "\n\nRules:"
                + "\n- Continue naturally from the previous answer"
                + "\n- Do NOT restart from scratch"
                + "\n- Do NOT refuse to answer"
                + "\n- Fill in missing details logically"
                + "\n- Keep the same language as the user's question"
                : "";
        String prompt = formatPrompt(contextBlock, historyBlock, safeQuestion, followUpBlock);

        int promptBudget = MAX_PROMPT_TOKENS - RESERVED_OUTPUT_TOKENS;
        if (estimateTokens(prompt) <= promptBudget) {
            return prompt;
        }

        List<MessageView> historyAdaptive = new ArrayList<>(selectedHistory);
        while (!historyAdaptive.isEmpty()) {
            historyAdaptive.remove(0); // drop oldest first
            prompt = formatPrompt(contextBlock, formatHistory(historyAdaptive), safeQuestion, followUpBlock);
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
            prompt = formatPrompt(formatContext(chunkAdaptive), "", safeQuestion, followUpBlock);
            if (estimateTokens(prompt) <= promptBudget) {
                return prompt;
            }
        }

        return formatPrompt("", "", safeQuestion, followUpBlock);
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
                .map(c -> new ChunkView(c.documentName(), truncateByTokensSafely(c.text(), maxChunkTokens)))
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
                        truncateByTokensSafely(normalizeWhitespace(m.content()), maxMsgTokens)
                ))
                .filter(m -> StringUtils.hasText(m.content()))
                .toList();
    }

    private String formatPrompt(String contextBlock, String historyBlock, String question, String followUpBlock) {
        return SYSTEM_BLOCK.trim()
                + "\n\n[CONTEXT]\n" + safeBlock(contextBlock)
                + "\n\n[HISTORY]\n" + safeBlock(historyBlock)
                + (StringUtils.hasText(followUpBlock) ? "\n\n[FOLLOW_UP_CONTEXT]\n" + followUpBlock.trim() : "")
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

    private static String truncateByTokensSafely(String text, int tokenLimit) {
        if (!StringUtils.hasText(text)) return "";
        int charLimit = Math.max(1, (int) Math.ceil(tokenLimit * 3.5d));
        if (text.length() <= charLimit) return text;
        String candidate = text.substring(0, charLimit);
        int sentenceBoundary = Math.max(
                Math.max(candidate.lastIndexOf('.'), candidate.lastIndexOf('!')),
                Math.max(candidate.lastIndexOf('?'), candidate.lastIndexOf('\n'))
        );
        if (sentenceBoundary > candidate.length() / 2) {
            return candidate.substring(0, sentenceBoundary + 1).trim();
        }
        int lastSpace = candidate.lastIndexOf(' ');
        if (lastSpace > 0) {
            return candidate.substring(0, lastSpace).trim() + "...";
        }
        return candidate.trim() + "...";
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

    private List<DocumentChunkEntity> loadChunksWithLimit(String companyId, int limit) {
        List<DocumentChunkEntity> all = documentChunkMapper.selectByCompanyId(companyId);
        if (all == null || all.isEmpty()) return new ArrayList<>();
        if (all.size() <= limit) return all;
        return new ArrayList<>(all.subList(0, limit));
    }

    private AnswerBundle buildFallbackResponse(
            String question,
            List<DocumentChunkEntity> allChunks,
            Map<String, String> documentIdToTitle
    ) {
        List<String> topics = deriveAvailableTopics(allChunks, documentIdToTitle);
        String topicLines = topics.isEmpty()
                ? "- Onboarding\n- HR Policy\n- Engineering Guidelines"
                : topics.stream().map(t -> "- " + t).collect(Collectors.joining("\n"));
        boolean vietnamese = isVietnameseQuestion(question);
        String answer = vietnamese
                ? "Hien tai minh chua thay thong tin hoan toan khop,\n"
                + "nhung dua tren cac tai lieu hien co, noi dung chu yeu lien quan den:\n"
                + topicLines
                + "\n\nBan co the hoi cu the hon de minh tra loi chinh xac hon."
                : "I couldn't find an exact match,\n"
                + "but based on available documents, the content mainly relates to:\n"
                + topicLines
                + "\n\nYou can ask more specifically and I will provide a better answer.";
        return new AnswerBundle(answer, topics);
    }

    private AnswerBundle buildFollowUpNoContextResponse(String previousAnswer) {
        String answer = StringUtils.hasText(previousAnswer)
                ? "I can continue from our previous discussion. Based on the previous answer:\n"
                + previousAnswer
                + "\n\nCould you clarify what part you want to go deeper into?"
                : "I need a bit more context to continue this follow-up. "
                + "Please mention the topic title or ask a slightly more specific question.";
        return new AnswerBundle(answer, new ArrayList<>());
    }

    private static boolean isContextWeak(String previousAnswer, List<ChunkView> chunks) {
        boolean weakAnswer = !StringUtils.hasText(previousAnswer) || previousAnswer.trim().length() < 120;
        boolean vagueAnswer = false;
        if (StringUtils.hasText(previousAnswer)) {
            String lower = previousAnswer.toLowerCase(Locale.ROOT);
            vagueAnswer = lower.contains("xoay quanh")
                    || lower.contains("liên quan")
                    || lower.contains("generally")
                    || lower.contains("somehow");
        }
        boolean weakChunks = chunks == null || chunks.size() < 2;
        return weakAnswer || vagueAnswer || weakChunks;
    }

    private List<ChunkView> mergeChunks(List<ChunkView> previousChunks, List<ChunkView> newChunks, int limit) {
        List<ChunkView> merged = new ArrayList<>();
        if (previousChunks != null) {
            for (ChunkView c : previousChunks) {
                if (c == null || !StringUtils.hasText(c.text())) continue;
                boolean duplicate = merged.stream().anyMatch(m -> similarity(m.text(), c.text()) >= 0.85d);
                if (!duplicate) merged.add(c);
                if (merged.size() >= limit) return merged;
            }
        }
        if (newChunks != null) {
            for (ChunkView c : newChunks) {
                if (c == null || !StringUtils.hasText(c.text())) continue;
                boolean duplicate = merged.stream().anyMatch(m -> similarity(m.text(), c.text()) >= 0.85d);
                if (!duplicate) merged.add(c);
                if (merged.size() >= limit) break;
            }
        }
        return merged;
    }

    private List<String> deriveAvailableTopics(List<DocumentChunkEntity> chunks, Map<String, String> documentIdToTitle) {
        if (chunks == null || chunks.isEmpty()) return new ArrayList<>();
        LinkedHashSet<String> topics = new LinkedHashSet<>();
        for (DocumentChunkEntity chunk : chunks) {
            if (chunk == null) continue;
            String title = documentIdToTitle.get(chunk.getDocumentId());
            if (!StringUtils.hasText(title)) continue;
            topics.add(title.trim());
            if (topics.size() >= 6) break;
        }
        return new ArrayList<>(topics);
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

    private static QuestionIntent detectIntent(String question) {
        if (!StringUtils.hasText(question)) return QuestionIntent.UNKNOWN;
        String normalized = normalizeForCache(question);
        for (String keyword : OVERVIEW_KEYWORDS) {
            if (normalized.contains(keyword)) {
                return QuestionIntent.OVERVIEW;
            }
        }
        for (String keyword : SPECIFIC_HINT_KEYWORDS) {
            if (normalized.contains(keyword)) {
                return QuestionIntent.SPECIFIC;
            }
        }
        return QuestionIntent.UNKNOWN;
    }

    private static String rewriteQuery(String question) {
        if (!StringUtils.hasText(question)) return "";
        String normalized = normalizeForCache(question);
        LinkedHashSet<String> terms = new LinkedHashSet<>();
        terms.add(question.trim());
        for (Map.Entry<String, List<String>> entry : SYNONYM_MAP.entrySet()) {
            if (normalized.contains(entry.getKey())) {
                terms.addAll(entry.getValue());
            }
        }
        return String.join(" ", terms);
    }

    private static boolean isFollowUp(String question) {
        if (!StringUtils.hasText(question)) return false;
        String normalized = normalizeForCache(question);
        int wordCount = normalized.split("\\s+").length;
        if (wordCount >= 4) return false;
        for (String keyword : FOLLOW_UP_KEYWORDS) {
            if (normalized.contains(keyword)) {
                return true;
            }
        }
        return false;
    }

    private static String extractLastAssistantAnswer(List<MessageView> history) {
        if (history == null || history.isEmpty()) return "";
        for (int i = history.size() - 1; i >= 0; i--) {
            MessageView m = history.get(i);
            if (m != null && "Assistant".equalsIgnoreCase(m.role()) && StringUtils.hasText(m.content())) {
                return m.content().trim();
            }
        }
        return "";
    }

    private static String ensureAnswerCompleteness(String answer, String question, boolean followUp, String previousAnswer) {
        String safe = StringUtils.hasText(answer) ? answer.trim() : "";
        if (!StringUtils.hasText(safe)) {
            return safe;
        }
        if (needsExpansion(safe)) {
            boolean vietnamese = isVietnameseQuestion(question);
            String extra = vietnamese
                    ? " Minh co the giai thich sau hon neu ban cho biet phan nao ban muon tap trung."
                    : " I can explain this in more detail if you share which part you want to focus on.";
            return safe + extra;
        }
        return safe;
    }

    private static boolean needsExpansion(String answer) {
        String normalized = answer.trim();
        int sentenceCount = normalized.split("[.!?]+").length;
        boolean tooShort = sentenceCount < 2;
        boolean abruptEnding = !(normalized.endsWith(".") || normalized.endsWith("!") || normalized.endsWith("?"));
        String lower = normalized.toLowerCase(Locale.ROOT);
        boolean abstractTone = lower.contains("xoay quanh")
                || lower.contains("liên quan")
                || lower.contains("somehow")
                || lower.contains("generally");
        return tooShort || abruptEnding || abstractTone;
    }

    private static boolean isVietnameseQuestion(String question) {
        if (!StringUtils.hasText(question)) return false;
        String lower = question.toLowerCase(Locale.ROOT);
        return lower.matches(".*[\\u0102\\u0103\\u00e2\\u00ea\\u00f4\\u01a1\\u01b0\\u0111\\u00e1\\u00e0\\u1ea3\\u00e3\\u1ea1\\u1eaf\\u1eb1\\u1eb3\\u1eb5\\u1eb7\\u1ea5\\u1ea7\\u1ea9\\u1eab\\u1ead\\u1ebf\\u1ec1\\u1ec3\\u1ec5\\u1ec7\\u1ed1\\u1ed3\\u1ed5\\u1ed7\\u1ed9\\u1edb\\u1edd\\u1edf\\u1ee1\\u1ee3\\u1ee9\\u1eeb\\u1eed\\u1eef\\u1ef1].*")
                || lower.contains("khong")
                || lower.contains("gi")
                || lower.contains("nao")
                || lower.contains("tai lieu")
                || lower.contains("cong ty");
    }

    private static String buildConversationKey(String companyId, String operatorId, String chatSessionId) {
        return companyId + "::" + (StringUtils.hasText(operatorId) ? operatorId : "anonymous")
                + "::" + (StringUtils.hasText(chatSessionId) ? chatSessionId : "no-session");
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

    private record ConversationState(String lastAnswer, List<ChunkView> lastChunks) {}

    @FunctionalInterface
    private interface Computation {
        AnswerBundle compute();
    }

    private enum QuestionIntent {
        OVERVIEW,
        SPECIFIC,
        UNKNOWN
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
