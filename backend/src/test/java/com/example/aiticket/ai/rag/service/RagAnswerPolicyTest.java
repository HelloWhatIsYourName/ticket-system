package com.example.aiticket.ai.rag.service;

import com.example.aiticket.ai.chat.ChatResult;
import com.example.aiticket.ai.rag.domain.RagPolicyDecision;
import com.example.aiticket.knowledge.domain.KnowledgeSearchResult;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class RagAnswerPolicyTest {
    @Test
    void suggestsTransferWhenNoKnowledgeIsRetrieved() {
        RagAnswerPolicy policy = new RagAnswerPolicy();

        RagPolicyDecision decision = policy.decide(List.of(), chat(true, 0.9), 0.7);

        assertThat(decision.canAnswer()).isFalse();
        assertThat(decision.confidence()).isEqualTo(0.0);
        assertThat(decision.transferSuggested()).isTrue();
        assertThat(decision.transferReason()).isEqualTo("未检索到相关知识片段");
    }

    @Test
    void suggestsTransferWhenTopSimilarityIsBelowThreshold() {
        RagAnswerPolicy policy = new RagAnswerPolicy();

        RagPolicyDecision decision = policy.decide(List.of(result(0.52)), chat(true, 0.9), 0.7);

        assertThat(decision.canAnswer()).isFalse();
        assertThat(decision.confidence()).isEqualTo(0.52);
        assertThat(decision.transferSuggested()).isTrue();
        assertThat(decision.transferReason()).isEqualTo("知识片段相似度低于阈值");
    }

    @Test
    void acceptsHighSimilarityAnswerAndCombinesConfidenceConservatively() {
        RagAnswerPolicy policy = new RagAnswerPolicy();

        RagPolicyDecision decision = policy.decide(List.of(result(0.86)), chat(true, 0.74), 0.7);

        assertThat(decision.canAnswer()).isTrue();
        assertThat(decision.confidence()).isEqualTo(0.8);
        assertThat(decision.transferSuggested()).isFalse();
        assertThat(decision.transferReason()).isNull();
    }

    @Test
    void modelSelfRefusalSuggestsTransferEvenWhenRetrievalIsStrong() {
        RagAnswerPolicy policy = new RagAnswerPolicy();

        RagPolicyDecision decision = policy.decide(List.of(result(0.9)), chat(false, 0.2), 0.7);

        assertThat(decision.canAnswer()).isFalse();
        assertThat(decision.confidence()).isEqualTo(0.2);
        assertThat(decision.transferSuggested()).isTrue();
        assertThat(decision.transferReason()).isEqualTo("模型自评无法可靠回答");
    }

    @Test
    void sensitivePersonalItineraryQuestionSuggestsTransferWhenBoundaryDocumentIsRetrieved() {
        RagAnswerPolicy policy = new RagAnswerPolicy();

        RagPolicyDecision decision = policy.decide(
                List.of(result(0.88, "系统不得回答个人实时行程、薪酬明细等敏感个人信息，需要人工核查。", "敏感个人信息处理边界")),
                chat(true, 0.86),
                0.7,
                "帮我查一下王经理今天下午在哪里开会。"
        );

        assertThat(decision.canAnswer()).isFalse();
        assertThat(decision.transferSuggested()).isTrue();
        assertThat(decision.transferReason()).isEqualTo("问题涉及敏感或需人工确认的业务边界");
    }

    @Test
    void contractAmountChangeQuestionSuggestsTransferWhenApprovalBoundaryIsRetrieved() {
        RagAnswerPolicy policy = new RagAnswerPolicy();

        RagPolicyDecision decision = policy.decide(
                List.of(result(0.91, "合同金额、折扣等关键条款变更必须经过销售负责人、法务或财务审批。", "合同审批和销售授权制度")),
                chat(true, 0.9),
                0.7,
                "客户说合同金额要临时改成 88 万，我能直接答应吗？"
        );

        assertThat(decision.canAnswer()).isFalse();
        assertThat(decision.transferSuggested()).isTrue();
        assertThat(decision.transferReason()).isEqualTo("问题涉及敏感或需人工确认的业务边界");
    }

    @Test
    void normalPasswordResetQuestionDoesNotSuggestTransferWithQuestionAwarePolicy() {
        RagAnswerPolicy policy = new RagAnswerPolicy();

        RagPolicyDecision decision = policy.decide(
                List.of(result(0.89, "用户可以在登录页点击忘记密码并完成身份验证后重置密码。", "账号登录 FAQ")),
                chat(true, 0.84),
                0.7,
                "忘记密码后应该如何重置？"
        );

        assertThat(decision.canAnswer()).isTrue();
        assertThat(decision.transferSuggested()).isFalse();
        assertThat(decision.transferReason()).isNull();
    }

    private ChatResult chat(boolean canAnswer, double confidence) {
        return new ChatResult("deepseek-chat", "answer", canAnswer, confidence, null);
    }

    private KnowledgeSearchResult result(double similarity) {
        return result(similarity, "content", "title");
    }

    private KnowledgeSearchResult result(double similarity, String content, String sourceTitle) {
        return new KnowledgeSearchResult(1L, 2L, 3L, 0, content, sourceTitle,
                1.0 - similarity, similarity);
    }
}
