package com.ecommerce.knowledge.chat;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class KnowledgeLightRouteDeciderTest {

    private final KnowledgeQueryClassifier classifier = new KnowledgeQueryClassifier();
    private final KnowledgeLightRouteDecider decider = new KnowledgeLightRouteDecider();

    @Test
    void decide_shouldRouteRealtimeOrderQuestionToFastPathChannel() {
        KnowledgeQueryFeatures features = classifier.extractFeatures("show my order list");

        KnowledgeLightRoute route = decider.decide(features, 1001L);

        assertThat(route).isEqualTo(KnowledgeLightRoute.FAST_PATH_CHANNEL);
    }

    @Test
    void decide_shouldRouteAddressQuestionToFastPathChannelWhenLoggedIn() {
        KnowledgeQueryFeatures features = classifier.extractFeatures("where is my shipping address");

        KnowledgeLightRoute route = decider.decide(features, 1001L);

        assertThat(route).isEqualTo(KnowledgeLightRoute.FAST_PATH_CHANNEL);
    }

    @Test
    void decide_shouldRouteFaqQuestionToRagFaqChannel() {
        KnowledgeQueryFeatures features = classifier.extractFeatures("what is the return policy");

        KnowledgeLightRoute route = decider.decide(features, 1001L);

        assertThat(route).isEqualTo(KnowledgeLightRoute.RAG_FAQ_CHANNEL);
    }

    @Test
    void decide_shouldRouteProductQuestionToToolOnlyAgentChannel() {
        KnowledgeQueryFeatures features = classifier.extractFeatures("show product iphone 15");

        KnowledgeLightRoute route = decider.decide(features, 1001L);

        assertThat(route).isEqualTo(KnowledgeLightRoute.TOOL_ONLY_AGENT_CHANNEL);
    }

    @Test
    void decide_shouldKeepRealtimeQuestionOnRagPathWhenUserMissing() {
        KnowledgeQueryFeatures features = classifier.extractFeatures("show my order list");

        KnowledgeLightRoute route = decider.decide(features, null);

        assertThat(route).isEqualTo(KnowledgeLightRoute.RAG_FAQ_CHANNEL);
    }
}
