package com.ecommerce.knowledge.service.impl;

import com.ecommerce.knowledge.chat.KnowledgeLightRoute;
import com.ecommerce.knowledge.chat.KnowledgeQueryRoute;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.Objects;

@Component
public class ChatExecutionMetricsRecorder {

    private static final String LIGHT_ROUTE_TOTAL = "knowledge.chat.light_route.total";
    private static final String ROUTE_TOTAL = "knowledge.chat.route.total";
    private static final String FAST_PATH_TOTAL = "knowledge.chat.fast_path.total";
    private static final String EXECUTION_TOTAL = "knowledge.chat.execution.total";
    private static final String EXECUTION_DURATION = "knowledge.chat.execution.duration";
    private static final String AGENT_DURATION = "knowledge.chat.agent.duration";
    private static final String DOWNSTREAM_DURATION = "knowledge.chat.downstream.duration";
    private static final String FAQ_CACHE_TOTAL = "knowledge.chat.faq_cache.total";

    private final MeterRegistry meterRegistry;

    public ChatExecutionMetricsRecorder(MeterRegistry meterRegistry) {
        this.meterRegistry = Objects.requireNonNull(meterRegistry, "meterRegistry");
    }

    public void recordLightRoute(KnowledgeLightRoute lightRoute) {
        counter(LIGHT_ROUTE_TOTAL, "light_route", lightRoute.name()).increment();
    }

    public void recordRoute(KnowledgeQueryRoute route) {
        counter(ROUTE_TOTAL, "route", route.name()).increment();
    }

    public void recordFastPath(KnowledgeQueryRoute route, boolean candidate, boolean available, boolean executed) {
        counter(FAST_PATH_TOTAL,
                "route", route.name(),
                "candidate", String.valueOf(candidate),
                "available", String.valueOf(available),
                "executed", String.valueOf(executed)).increment();
    }

    public void recordExecution(KnowledgeLightRoute lightRoute,
                                KnowledgeQueryRoute route,
                                String executionMode,
                                int agentCalls,
                                boolean degraded,
                                Duration duration) {
        counter(EXECUTION_TOTAL,
                "light_route", lightRoute.name(),
                "route", route.name(),
                "mode", executionMode,
                "agent_calls", String.valueOf(agentCalls),
                "degraded", String.valueOf(degraded)).increment();
        timer(EXECUTION_DURATION,
                "light_route", lightRoute.name(),
                "route", route.name(),
                "mode", executionMode).record(duration);
    }

    public void recordAgentExecution(KnowledgeQueryRoute route, String agentType, Duration duration) {
        timer(AGENT_DURATION,
                "route", route.name(),
                "agent_type", agentType).record(duration);
    }

    public void recordDownstream(String toolName, KnowledgeQueryRoute route, Duration duration, boolean timedOut) {
        recordDownstream(toolName, "overall", route, duration, timedOut);
    }

    public void recordDownstream(String toolName,
                                 String operation,
                                 KnowledgeQueryRoute route,
                                 Duration duration,
                                 boolean timedOut) {
        timer(DOWNSTREAM_DURATION,
                "tool", toolName,
                "operation", operation,
                "route", route.name(),
                "timed_out", String.valueOf(timedOut)).record(duration);
    }

    public void recordFaqCache(String event, KnowledgeLightRoute lightRoute, KnowledgeQueryRoute route) {
        counter(FAQ_CACHE_TOTAL,
                "event", event,
                "light_route", lightRoute.name(),
                "route", route.name()).increment();
    }

    private Counter counter(String name, String... tags) {
        return Counter.builder(name)
                .tags(tags)
                .register(meterRegistry);
    }

    private Timer timer(String name, String... tags) {
        return Timer.builder(name)
                .tags(tags)
                .register(meterRegistry);
    }
}
