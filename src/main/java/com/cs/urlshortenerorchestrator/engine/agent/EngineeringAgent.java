package com.cs.urlshortenerorchestrator.engine.agent;

import com.cs.urlshortenerorchestrator.engine.domain.Artifact;
import tools.jackson.databind.ObjectMapper;

import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

public class EngineeringAgent {

    private final AgentClient agentClient;
    private final WorkspaceTool workspaceTool;
    private final ObjectMapper objectMapper;

    public EngineeringAgent(
            AgentClient agentClient,
            WorkspaceTool workspaceTool,
            ObjectMapper objectMapper) {

        this.agentClient =
                Objects.requireNonNull(agentClient, "agentClient required");

        this.workspaceTool =
                Objects.requireNonNull(workspaceTool, "workspaceTool required");

        this.objectMapper =
                Objects.requireNonNull(objectMapper, "objectMapper required");
    }

    public CodeGenerationResult execute(EngineeringTask task) {

        Objects.requireNonNull(task, "task required");

        return switch (task.role()) {
            case IMPLEMENTATION ->
                    executeImplementation(task);
        };
    }

    private CodeGenerationResult executeImplementation(
            EngineeringTask task) {

        if (task.upstreamArtifacts() == null
                || task.upstreamArtifacts().isEmpty()) {
            throw new IllegalArgumentException(
                    "Implementation requires upstream artifacts"
            );
        }

        if (task.objective() == null || task.objective().isBlank()) {
            throw new IllegalArgumentException(
                    "Implementation objective required"
            );
        }

        if (task.allowedPackageRoot() == null
                || task.allowedPackageRoot().isBlank()) {
            throw new IllegalArgumentException(
                    "Allowed package root required"
            );
        }

        String artifactContext =
                buildArtifactContext(task.upstreamArtifacts());

        String constraintContext =
                buildConstraintContext(task.constraints());

        String systemPrompt = """
            You are a senior software implementation agent operating inside
            a governed SDLC orchestration workflow.

            Your responsibility is to implement the assigned engineering task
            using the requirements, architecture, test-planning artifacts and
            constraints supplied to you.

            Engineering principles:
            - Produce complete, compilable Java source.
            - Prefer simple, maintainable designs.
            - Do not create placeholder or empty implementations.
            - Do not introduce dependencies or infrastructure unless justified
              by the supplied engineering context.
            - Do not modify anything outside the explicitly permitted workspace.
            - Generate only files required to satisfy the assigned objective.
            - Preserve the architecture and acceptance criteria represented in
              upstream artifacts.

            Return ONLY JSON with this structure:

            {
              "files": [
                {
                  "path": "...",
                  "content": "complete Java source"
                }
              ],
              "summary": "concise implementation summary"
            }

            Do not include markdown fences or explanatory prose outside JSON.
            """;

        String userPrompt = """
            Engineering objective:

            %s

            Permitted source root:

            %s

            Constraints:

            %s

            Upstream SDLC artifacts:

            %s

            Implement the smallest complete engineering solution that satisfies
            the objective and supplied context.
            """.formatted(
                task.objective(),
                task.allowedPackageRoot(),
                constraintContext,
                artifactContext
        );

        AgentResponse response =
                agentClient.execute(
                        systemPrompt,
                        userPrompt
                );

        CodeGenerationResult result;

        try {
            String rawContent = response.content().trim();

            // Some models may wrap otherwise-valid JSON in Markdown code fences.
            if (rawContent.startsWith("```json")) {
                rawContent = rawContent.substring(7);
            } else if (rawContent.startsWith("```")) {
                rawContent = rawContent.substring(3);
            }

            if (rawContent.endsWith("```")) {
                rawContent = rawContent.substring(
                        0,
                        rawContent.length() - 3
                );
            }

            rawContent = rawContent.trim();

            result = objectMapper.readValue(
                    rawContent,
                    CodeGenerationResult.class
            );

        } catch (Exception e) {
            throw new IllegalStateException(
                    "Implementation agent returned invalid code-generation JSON",
                    e
            );
        }

        validateResult(result, task);

        for (GeneratedFile file : result.files()) {
            workspaceTool.writeFile(
                    file.path(),
                    file.content()
            );
        }

        return result;
    }

    private String buildArtifactContext(
            List<Artifact> artifacts) {

        return artifacts.stream()
                .map(artifact -> """
                Artifact:
                  id: %s
                  name: %s
                  type: %s
                  storageLocation: %s
                  metadata: %s
                """.formatted(
                                artifact.id(),
                                artifact.name(),
                                artifact.type(),
                                artifact.storageLocation(),
                                artifact.metadata()
                        )
                )
                .collect(Collectors.joining("\n"));
    }

    private String buildConstraintContext(
            List<String> constraints) {

        if (constraints == null || constraints.isEmpty()) {
            return "No additional constraints.";
        }

        return constraints.stream()
                .map(constraint -> "- " + constraint)
                .collect(Collectors.joining("\n"));
    }

    private void validateResult(
            CodeGenerationResult result,
            EngineeringTask task) {

        if (result == null) {
            throw new IllegalArgumentException(
                    "Implementation agent returned no result"
            );
        }

        if (result.files() == null || result.files().isEmpty()) {
            throw new IllegalArgumentException(
                    "Implementation agent produced no source files"
            );
        }

        for (GeneratedFile file : result.files()) {

            if (file.path() == null || file.path().isBlank()) {
                throw new IllegalArgumentException(
                        "Generated file path required"
                );
            }

            if (file.content() == null || file.content().isBlank()) {
                throw new IllegalArgumentException(
                        "Generated file content required: "
                                + file.path()
                );
            }

            if (!file.path().startsWith(
                    task.allowedPackageRoot())) {

                throw new SecurityException(
                        "Generated file outside permitted source root: "
                                + file.path()
                );
            }

            if (!file.path().endsWith(".java")) {
                throw new IllegalArgumentException(
                        "Implementation agent may only generate Java source files: "
                                + file.path()
                );
            }
        }
    }
}