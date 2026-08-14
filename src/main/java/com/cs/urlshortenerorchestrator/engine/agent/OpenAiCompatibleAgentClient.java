package com.cs.urlshortenerorchestrator.engine.agent;

import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.web.client.RestClient;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import java.util.List;
import java.util.Objects;

/**
 * Real AgentClient backed by an OpenAI-compatible chat-completions API.
 *
 * Works with gateways such as LiteLLM without coupling the orchestration
 * engine to a specific model provider.
 */
public class OpenAiCompatibleAgentClient implements AgentClient {

    private final RestClient restClient;
    private final ObjectMapper objectMapper;
    private final String chatCompletionsUrl;
    private final String model;
    private final int maxTokens;

    public OpenAiCompatibleAgentClient(
            String baseUrl,
            String apiKey,
            String model) {

        this(baseUrl, apiKey, model, 2000);
    }

    public OpenAiCompatibleAgentClient(
            String baseUrl,
            String apiKey,
            String model,
            int maxTokens) {

        Objects.requireNonNull(baseUrl, "baseUrl required");
        Objects.requireNonNull(model, "model required");

        if (maxTokens <= 0) {
            throw new IllegalArgumentException(
                    "maxTokens must be positive"
            );
        }

        this.chatCompletionsUrl =
                stripTrailingSlash(baseUrl) + "/chat/completions";

        this.model = model;
        this.maxTokens = maxTokens;
        this.objectMapper = new ObjectMapper();

        RestClient.Builder builder = RestClient.builder();

        if (apiKey != null && !apiKey.isBlank()) {
            builder.defaultHeader(
                    HttpHeaders.AUTHORIZATION,
                    "Bearer " + apiKey
            );
        }

        this.restClient = builder.build();
    }

    /**
     * Convenience constructor for the live prototype.
     *
     * Required environment variables:
     * LLM_BASE_URL
     * LLM_MODEL
     *
     * LLM_API_KEY is optional so a local LiteLLM proxy can be used
     * without credentials if configured that way.
     */
    public static OpenAiCompatibleAgentClient fromEnvironment() {

        String baseUrl = requireEnvironmentVariable(
                "LLM_BASE_URL"
        );

        String model = requireEnvironmentVariable(
                "LLM_MODEL"
        );

        String apiKey = System.getenv("LLM_API_KEY");

        return new OpenAiCompatibleAgentClient(
                baseUrl,
                apiKey,
                model
        );
    }

    @Override
    public AgentResponse execute(
            String systemPrompt,
            String userPrompt) {

        Objects.requireNonNull(systemPrompt, "systemPrompt required");
        Objects.requireNonNull(userPrompt, "userPrompt required");

        ChatCompletionRequest request =
                new ChatCompletionRequest(
                        model,
                        List.of(
                                new Message("system", systemPrompt),
                                new Message("user", userPrompt)
                        ),
                        0.1,
                        maxTokens
                );

        String responseJson;

        try {
            responseJson = restClient.post()
                    .uri(chatCompletionsUrl)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(request)
                    .retrieve()
                    .body(String.class);

        } catch (Exception e) {
            throw new IllegalStateException(
                    "LLM request failed: " + e.getMessage(),
                    e
            );
        }

        if (responseJson == null || responseJson.isBlank()) {
            throw new IllegalStateException(
                    "LLM returned an empty response"
            );
        }

        try {
            JsonNode root = objectMapper.readTree(responseJson);

            String content = root
                    .path("choices")
                    .path(0)
                    .path("message")
                    .path("content")
                    .asText();

            if (content == null || content.isBlank()) {
                throw new IllegalStateException(
                        "LLM response contained no message content"
                );
            }

            String returnedModel =
                    root.path("model").asText(model);

            int inputTokens =
                    root.path("usage")
                            .path("prompt_tokens")
                            .asInt(0);

            int outputTokens =
                    root.path("usage")
                            .path("completion_tokens")
                            .asInt(0);

            return new AgentResponse(
                    content,
                    returnedModel,
                    inputTokens,
                    outputTokens
            );

        } catch (Exception e) {
            throw new IllegalStateException(
                    "Unable to parse LLM response",
                    e
            );
        }
    }

    private static String requireEnvironmentVariable(
            String name) {

        String value = System.getenv(name);

        if (value == null || value.isBlank()) {
            throw new IllegalStateException(
                    "Required environment variable is missing: " + name
            );
        }

        return value;
    }

    private static String stripTrailingSlash(String value) {
        while (value.endsWith("/")) {
            value = value.substring(0, value.length() - 1);
        }

        return value;
    }

    private record ChatCompletionRequest(
            String model,
            List<Message> messages,
            double temperature,
            int max_tokens
    ) {}

    private record Message(
            String role,
            String content
    ) {}
}