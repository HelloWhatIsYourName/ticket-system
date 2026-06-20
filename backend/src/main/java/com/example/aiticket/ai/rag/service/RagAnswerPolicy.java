package com.example.aiticket.ai.rag.service;

import com.example.aiticket.ai.chat.ChatResult;
import com.example.aiticket.ai.rag.domain.RagPolicyDecision;
import com.example.aiticket.knowledge.domain.KnowledgeSearchResult;
import org.springframework.stereotype.Component;

import java.util.Comparator;
import java.util.List;
import java.util.Locale;

@Component
public class RagAnswerPolicy {
    public RagPolicyDecision decide(List<KnowledgeSearchResult> results,
                                    ChatResult chatResult,
                                    double minSimilarity) {
        return decide(results, chatResult, minSimilarity, null);
    }

    public RagPolicyDecision decide(List<KnowledgeSearchResult> results,
                                    ChatResult chatResult,
                                    double minSimilarity,
                                    String question) {
        List<KnowledgeSearchResult> safeResults = results == null ? List.of() : results;
        if (safeResults.isEmpty()) {
            return new RagPolicyDecision(false, 0.0, true, "未检索到相关知识片段");
        }

        double topSimilarity = safeResults.stream()
                .map(KnowledgeSearchResult::similarity)
                .filter(value -> value != null)
                .max(Comparator.naturalOrder())
                .orElse(0.0);

        if (topSimilarity < minSimilarity) {
            return new RagPolicyDecision(false, round(topSimilarity), true, "知识片段相似度低于阈值");
        }

        if (requiresManualHandling(question, safeResults)) {
            double modelConfidence = chatResult == null ? topSimilarity : chatResult.confidence();
            return new RagPolicyDecision(false, round(Math.min(topSimilarity, modelConfidence)), true,
                    "问题涉及敏感或需人工确认的业务边界");
        }

        if (chatResult != null && !chatResult.canAnswer()) {
            return new RagPolicyDecision(false, round(chatResult.confidence()), true, "模型自评无法可靠回答");
        }

        double modelConfidence = chatResult == null ? 0.7 : chatResult.confidence();
        double confidence = Math.min(topSimilarity, (topSimilarity + modelConfidence) / 2.0);
        return new RagPolicyDecision(true, round(confidence), false, null);
    }

    private boolean requiresManualHandling(String question, List<KnowledgeSearchResult> results) {
        String normalizedQuestion = normalize(question);
        if (normalizedQuestion.isBlank()) {
            return false;
        }

        String evidence = normalize(results.stream()
                .map(result -> result.sourceTitle() + "\n" + result.content())
                .toList()
                .toString());

        return containsAny(normalizedQuestion, "个人行程", "在哪里开会")
                && containsAny(evidence, "敏感个人信息", "个人实时行程")
                || containsAny(normalizedQuestion, "工资", "薪酬", "裁员")
                && containsAny(evidence, "薪酬", "未公开人事", "人工核查")
                || containsAny(normalizedQuestion, "合同金额", "临时改", "直接答应")
                && containsAny(evidence, "合同审批", "销售授权")
                || containsAny(normalizedQuestion, "生产数据库", "误删")
                && containsAny(evidence, "生产事故", "应急预案");
    }

    private boolean containsAny(String value, String... keywords) {
        for (String keyword : keywords) {
            if (value.contains(normalize(keyword))) {
                return true;
            }
        }
        return false;
    }

    private String normalize(String value) {
        if (value == null) {
            return "";
        }
        return value.toLowerCase(Locale.ROOT).replaceAll("\\s+", "");
    }

    private double round(double value) {
        return Math.round(value * 100.0) / 100.0;
    }
}
