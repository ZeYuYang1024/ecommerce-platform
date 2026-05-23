package com.ecommerce.knowledge.service.impl;

import com.ecommerce.knowledge.chat.KnowledgeLightRoute;
import com.ecommerce.knowledge.chat.KnowledgeQueryRoute;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.Test;

import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;

class ChatExecutionMetricsRecorderTest {

    @Test
    void shouldRecordRouteExecutionAgentAndDownstreamMetrics() {
        SimpleMeterRegistry registry = new SimpleMeterRegistry();
        ChatExecutionMetricsRecorder recorder = new ChatExecutionMetricsRecorder(registry);

        recorder.recordLightRoute(KnowledgeLightRoute.FAST_PATH_CHANNEL);
        recorder.recordRoute(KnowledgeQueryRoute.ORDER_LIST);
        recorder.recordFastPath(KnowledgeQueryRoute.ORDER_LIST, true, true, true);
        recorder.recordExecution(KnowledgeLightRoute.FAST_PATH_CHANNEL, KnowledgeQueryRoute.ORDER_LIST,
                "fastPath", 0, false, Duration.ofMillis(120));
        recorder.recordAgentExecution(KnowledgeQueryRoute.PRODUCT, "toolOnly", Duration.ofMillis(80));
        recorder.recordDownstream("order", KnowledgeQueryRoute.ORDER_LIST, Duration.ofMillis(35), false);
        recorder.recordDownstream("order", "queryCurrentUserOrderSummaries", KnowledgeQueryRoute.ORDER_LIST, Duration.ofMillis(28), false);
        recorder.recordFaqCache("hit", KnowledgeLightRoute.RAG_FAQ_CHANNEL, KnowledgeQueryRoute.RAG_FAQ);

        assertThat(registry.counter("knowledge.chat.light_route.total", "light_route", "FAST_PATH_CHANNEL").count())
                .isEqualTo(1.0);
        assertThat(registry.counter("knowledge.chat.route.total", "route", "ORDER_LIST").count())
                .isEqualTo(1.0);
        assertThat(registry.counter("knowledge.chat.fast_path.total",
                        "route", "ORDER_LIST",
                        "candidate", "true",
                        "available", "true",
                        "executed", "true").count())
                .isEqualTo(1.0);
        assertThat(registry.counter("knowledge.chat.faq_cache.total",
                        "event", "hit",
                        "light_route", "RAG_FAQ_CHANNEL",
                        "route", "RAG_FAQ").count())
                .isEqualTo(1.0);
        assertThat(registry.counter("knowledge.chat.execution.total",
                        "light_route", "FAST_PATH_CHANNEL",
                        "route", "ORDER_LIST",
                        "mode", "fastPath",
                        "agent_calls", "0",
                        "degraded", "false").count())
                .isEqualTo(1.0);
        assertThat(registry.timer("knowledge.chat.execution.duration",
                        "light_route", "FAST_PATH_CHANNEL",
                        "route", "ORDER_LIST",
                        "mode", "fastPath").count())
                .isEqualTo(1);
        assertThat(registry.timer("knowledge.chat.agent.duration",
                        "route", "PRODUCT",
                        "agent_type", "toolOnly").count())
                .isEqualTo(1);
        assertThat(registry.timer("knowledge.chat.downstream.duration",
                        "tool", "order",
                        "operation", "overall",
                        "route", "ORDER_LIST",
                        "timed_out", "false").count())
                .isEqualTo(1);
        assertThat(registry.timer("knowledge.chat.downstream.duration",
                        "tool", "order",
                        "operation", "queryCurrentUserOrderSummaries",
                        "route", "ORDER_LIST",
                        "timed_out", "false").count())
                .isEqualTo(1);
    }
}
