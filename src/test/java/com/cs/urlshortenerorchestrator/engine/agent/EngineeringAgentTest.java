package com.cs.urlshortenerorchestrator.engine.agent;

import com.cs.urlshortenerorchestrator.engine.domain.Artifact;
import com.cs.urlshortenerorchestrator.engine.domain.ArtifactType;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import tools.jackson.databind.ObjectMapper;

import java.nio.file.Path;
import java.time.Instant;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class EngineeringAgentTest {

    @TempDir
    Path projectRoot;

    @Test
    void implementationAgentGeneratesAndWritesFilesFromEngineeringTask() {

        String modelOutput = """
            {
              "files": [
                {
                  "path": "src/main/java/com/cs/urlshortenerorchestrator/analytics/domain/ClickEvent.java",
                  "content": "package com.cs.urlshortenerorchestrator.analytics.domain; public record ClickEvent(String shortCode) {}"
                },
                {
                  "path": "src/main/java/com/cs/urlshortenerorchestrator/analytics/service/EventStore.java",
                  "content": "package com.cs.urlshortenerorchestrator.analytics.service; public class EventStore {}"
                }
              ],
              "summary": "Created a minimal analytics event foundation."
            }
            """;

        AgentClient fakeClient =
                (systemPrompt, userPrompt) ->
                        new AgentResponse(
                                modelOutput,
                                "test-model",
                                100,
                                200
                        );

        BoundedWorkspaceTool workspace =
                new BoundedWorkspaceTool(projectRoot);

        EngineeringAgent agent =
                new EngineeringAgent(
                        fakeClient,
                        workspace,
                        new ObjectMapper()
                );

        Artifact requirements =
                new Artifact(
                        "req-1",
                        ArtifactType.REQUIREMENT_SPEC,
                        "analytics-requirements.md",
                        "requirements",
                        "exec-requirements-1",
                        "artifacts/analytics-requirements.md",
                        Map.of(
                                "feature", "ANALYTICS",
                                "acceptanceCriteria",
                                "event-ingestion,aggregation,spike-detection"
                        ),
                        Instant.now()
                );

        Artifact architecture =
                new Artifact(
                        "arch-1",
                        ArtifactType.ARCHITECTURE_PLAN,
                        "analytics-architecture.md",
                        "architecture",
                        "exec-architecture-1",
                        "artifacts/analytics-architecture.md",
                        Map.of(
                                "storage", "IN_MEMORY",
                                "style", "SPRING_BOOT"
                        ),
                        Instant.now()
                );

        EngineeringTask task =
                new EngineeringTask(
                        AgentRole.IMPLEMENTATION,
                        "Implement the greenfield Analytics capability described by the upstream artifacts.",
                        List.of(requirements, architecture),
                        List.of(
                                "Use Java 21 and Spring Boot",
                                "Use in-memory/local components only",
                                "Do not introduce external infrastructure"
                        ),
                        "src/main/java/com/cs/urlshortenerorchestrator/analytics/"
                );

        CodeGenerationResult result =
                agent.execute(task);

        assertThat(result.files()).hasSize(2);

        assertThat(workspace.exists(
                "src/main/java/com/cs/urlshortenerorchestrator/analytics/domain/ClickEvent.java"
        )).isTrue();

        assertThat(workspace.exists(
                "src/main/java/com/cs/urlshortenerorchestrator/analytics/service/EventStore.java"
        )).isTrue();

        assertThat(workspace.readFile(
                "src/main/java/com/cs/urlshortenerorchestrator/analytics/domain/ClickEvent.java"
        )).contains("ClickEvent");
    }
}