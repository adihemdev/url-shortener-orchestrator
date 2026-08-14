package com.cs.urlshortenerorchestrator.engine.agent;

public record AgentResponse(
        String content,
        String model,
        int inputTokens,
        int outputTokens
) {
}