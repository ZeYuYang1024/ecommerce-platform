package com.ecommerce.knowledge.chat;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class KnowledgeQueryClassifierTest {

    private final KnowledgeQueryClassifier classifier = new KnowledgeQueryClassifier();

    @Test
    void classify_shouldReturnRagFaqForBlankInput() {
        KnowledgeQueryRoute route = classifier.classify("   ");

        assertThat(route).isEqualTo(KnowledgeQueryRoute.RAG_FAQ);
        assertThat(route.isStructured()).isFalse();
    }

    @Test
    void classify_shouldReturnStructuredRouteForEnglishOrderQuestion() {
        KnowledgeQueryRoute route = classifier.classify("show my order status");

        assertThat(route).isEqualTo(KnowledgeQueryRoute.ORDER_LIST);
        assertThat(route.isStructured()).isTrue();
    }

    @Test
    void classify_shouldReturnStructuredRouteForEnglishCartQuestion() {
        KnowledgeQueryRoute route = classifier.classify("show my cart items");

        assertThat(route).isEqualTo(KnowledgeQueryRoute.CART);
        assertThat(route.isStructured()).isTrue();
    }

    @Test
    void classify_shouldReturnPaymentByOrderNoWhenQuestionMentionsOrderPaymentAndOrderNumber() {
        KnowledgeQueryRoute route = classifier.classify("check payment for order ORD-1001");

        assertThat(route).isEqualTo(KnowledgeQueryRoute.PAYMENT_BY_ORDER_NO);
    }

    @Test
    void classify_shouldCoverAdditionalStructuredRoute() {
        KnowledgeQueryRoute route = classifier.classify("where is my shipping address");

        assertThat(route).isEqualTo(KnowledgeQueryRoute.ADDRESS);
    }

    @Test
    void classify_shouldReturnAfterSaleForStructuredRefundIntent() {
        KnowledgeQueryRoute route = classifier.classify("I want to refund order ORD-1001");

        assertThat(route).isEqualTo(KnowledgeQueryRoute.AFTER_SALE);
        assertThat(route.isStructured()).isTrue();
    }

    @Test
    void classify_shouldReturnAfterSaleForGenericChineseAfterSaleQuestion() {
        KnowledgeQueryRoute route = classifier.classify("如何退换货？");

        assertThat(route).isEqualTo(KnowledgeQueryRoute.AFTER_SALE);
        assertThat(route.isStructured()).isTrue();
    }

    @Test
    void classify_shouldAvoidCouponFalsePositiveForUnrelatedChineseWord() {
        KnowledgeQueryRoute route = classifier.classify("证券账户是什么");

        assertThat(route).isEqualTo(KnowledgeQueryRoute.RAG_FAQ);
    }

    @Test
    void classify_shouldKeepOrderRuleQuestionOnRagPath() {
        KnowledgeQueryRoute route = classifier.classify("帮我查一下订单取消规则");

        assertThat(route).isEqualTo(KnowledgeQueryRoute.RAG_FAQ);
    }

    @Test
    void classify_shouldKeepCouponAvailabilityQuestionOnAgentPath() {
        KnowledgeQueryRoute route = classifier.classify("有哪些优惠券可以领");

        assertThat(route).isEqualTo(KnowledgeQueryRoute.RAG_FAQ);
    }

    @Test
    void classify_shouldReturnRagFaqForGeneralQuestion() {
        KnowledgeQueryRoute route = classifier.classify("what is the return policy");

        assertThat(route).isEqualTo(KnowledgeQueryRoute.RAG_FAQ);
        assertThat(route.isStructured()).isFalse();
    }
}
