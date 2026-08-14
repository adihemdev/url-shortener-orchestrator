package com.cs.urlshortenerorchestrator.engine.agent;

public interface AgentClient {
    AgentResponse execute(String systemPrompt, String userPrompt);
}