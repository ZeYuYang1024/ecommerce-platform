package com.ecommerce.knowledge.agent;

public final class AgentUserContextHolder {

    private static final ThreadLocal<AgentUserContext> CONTEXT = new ThreadLocal<>();

    private AgentUserContextHolder() {
    }

    public static void set(AgentUserContext context) {
        CONTEXT.set(context);
    }

    public static AgentUserContext get() {
        return CONTEXT.get();
    }

    public static void clear() {
        CONTEXT.remove();
    }
}
