package com.ecommerce.knowledge.agent;

import dev.langchain4j.service.MemoryId;
import dev.langchain4j.service.SystemMessage;
import dev.langchain4j.service.TokenStream;
import dev.langchain4j.service.UserMessage;

public interface StreamingKnowledgeAgent {

    @SystemMessage(KnowledgeAgentSystemPrompt.PROMPT)
    TokenStream chat(@MemoryId String sessionId, @UserMessage String userMessage);
}
