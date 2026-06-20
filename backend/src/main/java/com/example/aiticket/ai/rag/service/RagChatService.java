package com.example.aiticket.ai.rag.service;

import com.example.aiticket.ai.chat.ChatClient;
import com.example.aiticket.ai.chat.ChatResult;
import com.example.aiticket.ai.rag.domain.AiMessage;
import com.example.aiticket.ai.rag.domain.AiMessageCitation;
import com.example.aiticket.ai.rag.domain.AiMessageRole;
import com.example.aiticket.ai.rag.domain.AiMessageWithCitations;
import com.example.aiticket.ai.rag.domain.AiSession;
import com.example.aiticket.ai.rag.domain.RagAnswer;
import com.example.aiticket.ai.rag.domain.RagCitation;
import com.example.aiticket.ai.rag.domain.RagPolicyDecision;
import com.example.aiticket.ai.rag.domain.RagPrompt;
import com.example.aiticket.ai.rag.mapper.AiChatMapper;
import com.example.aiticket.ai.rag.prompt.RagPromptBuilder;
import com.example.aiticket.config.KnowledgeProperties;
import com.example.aiticket.knowledge.domain.KnowledgeSearchResult;
import com.example.aiticket.knowledge.service.KnowledgeRetrievalService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class RagChatService {
    private static final int SESSION_TITLE_LIMIT = 60;

    private final AiChatMapper mapper;
    private final KnowledgeRetrievalService retrievalService;
    private final ChatClient chatClient;
    private final RagPromptBuilder promptBuilder;
    private final RagAnswerPolicy answerPolicy;
    private final KnowledgeProperties knowledgeProperties;

    public RagChatService(AiChatMapper mapper,
                          KnowledgeRetrievalService retrievalService,
                          ChatClient chatClient,
                          RagPromptBuilder promptBuilder,
                          RagAnswerPolicy answerPolicy,
                          KnowledgeProperties knowledgeProperties) {
        this.mapper = mapper;
        this.retrievalService = retrievalService;
        this.chatClient = chatClient;
        this.promptBuilder = promptBuilder;
        this.answerPolicy = answerPolicy;
        this.knowledgeProperties = knowledgeProperties;
    }

    @Transactional
    public RagAnswer ask(Long userId,
                         Long sessionId,
                         String question,
                         Long categoryId,
                         Integer topK,
                         Double minSimilarity) {
        if (userId == null) {
            throw new IllegalArgumentException("userId must not be null");
        }
        if (question == null || question.isBlank()) {
            throw new IllegalArgumentException("question must not be blank");
        }

        Long resolvedSessionId = resolveSession(userId, sessionId, question);
        Long userMessageId = mapper.nextMessageId();
        mapper.insertMessage(userMessageId, resolvedSessionId, userId, AiMessageRole.USER, question.trim(),
                null, null, null, 0, null);

        List<KnowledgeSearchResult> results = retrievalService.search(question, categoryId, topK, minSimilarity);
        RagPrompt prompt = promptBuilder.build(question, results);
        ChatResult chatResult = chatClient.chat(prompt.prompt());
        double threshold = minSimilarity == null
                ? knowledgeProperties.getRetrieval().getMinSimilarity()
                : minSimilarity;
        RagPolicyDecision decision = answerPolicy.decide(results, chatResult, threshold, question.trim());

        Long assistantMessageId = mapper.nextMessageId();
        mapper.insertMessage(assistantMessageId, resolvedSessionId, userId, AiMessageRole.ASSISTANT,
                chatResult.content(), chatResult.model(), toFlag(decision.canAnswer()), decision.confidence(),
                toFlag(decision.transferSuggested()), decision.transferReason());

        for (RagCitation citation : prompt.citations()) {
            mapper.insertCitation(
                    mapper.nextCitationId(),
                    assistantMessageId,
                    citation.chunkId(),
                    citation.documentId(),
                    citation.citationIndex(),
                    citation.sourceTitle(),
                    citation.snippet(),
                    citation.similarity()
            );
        }
        mapper.updateSessionSummary(resolvedSessionId, userId, question.trim(), toFlag(decision.transferSuggested()));

        return new RagAnswer(
                resolvedSessionId,
                userMessageId,
                assistantMessageId,
                chatResult.content(),
                decision.canAnswer(),
                decision.confidence(),
                decision.transferSuggested(),
                decision.transferReason(),
                prompt.citations()
        );
    }

    public List<AiSession> listSessions(Long userId, int limit) {
        return mapper.listOwnedSessions(userId, limit);
    }

    public List<AiMessageWithCitations> listMessages(Long userId, Long sessionId) {
        List<AiMessage> messages = mapper.listMessages(sessionId, userId);
        return messages.stream()
                .map(message -> new AiMessageWithCitations(message, citationsFor(message)))
                .toList();
    }

    private Long resolveSession(Long userId, Long sessionId, String question) {
        if (sessionId == null) {
            Long newSessionId = mapper.nextSessionId();
            mapper.insertSession(newSessionId, userId, titleFrom(question), question.trim());
            return newSessionId;
        }

        AiSession session = mapper.findOwnedSession(sessionId, userId);
        if (session == null) {
            throw new RagSessionNotFoundException();
        }
        return sessionId;
    }

    private List<AiMessageCitation> citationsFor(AiMessage message) {
        if (message.role() != AiMessageRole.ASSISTANT) {
            return List.of();
        }
        return mapper.listCitations(message.id());
    }

    private String titleFrom(String question) {
        String trimmed = question.trim();
        if (trimmed.length() <= SESSION_TITLE_LIMIT) {
            return trimmed;
        }
        return trimmed.substring(0, SESSION_TITLE_LIMIT);
    }

    private Integer toFlag(boolean value) {
        return value ? 1 : 0;
    }
}
