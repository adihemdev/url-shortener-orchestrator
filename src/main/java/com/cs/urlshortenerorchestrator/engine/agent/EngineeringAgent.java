package com.cs.urlshortenerorchestrator.engine.agent;

import com.cs.urlshortenerorchestrator.engine.domain.Artifact;
import com.cs.urlshortenerorchestrator.engine.domain.ArtifactType;
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
            case TESTING ->
                    executeTesting(task);
            case ANALYSIS ->
                    throw new IllegalArgumentException(
                            "Use analyze() for ANALYSIS tasks"
                    );
            case VALIDATION ->
                    throw new IllegalArgumentException(
                            "Use validate() for VALIDATION tasks"
                    );
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
                buildTestingContext(task.upstreamArtifacts());

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
            
            JSON must be strictly valid RFC 8259 JSON.
            Do not include // comments, /* comments */, trailing commas,
            or unescaped newlines inside JSON strings.
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
            String rawContent =
                    normalizeJsonResponse(
                            response.content()
                    );

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

    private CodeGenerationResult executeTesting(
            EngineeringTask task) {

        if (task.upstreamArtifacts() == null
                || task.upstreamArtifacts().isEmpty()) {
            throw new IllegalArgumentException(
                    "Testing requires implementation artifacts"
            );
        }

        String artifactContext =
                buildTestingContext(task.upstreamArtifacts());

        String constraintContext =
                buildConstraintContext(task.constraints());

        String systemPrompt = """
        You are a senior Java testing agent operating inside a governed
        SDLC orchestration workflow.

        Generate tests for the supplied implementation based on the
        engineering objective, source code, requirements/test context,
        and constraints.

        Testing principles:
        - Use JUnit 5.
        - Test observable behavior, not implementation details.
        - Cover important success paths and meaningful edge cases.
        - Tests must compile against the supplied implementation.
        - Do not modify production source code.
        - Do not create placeholder tests.
        - Keep the test suite focused and maintainable.
        - Generate only test files necessary to validate the current
          implementation against the supplied engineering context.
        - Keep the generated test suite focused; prefer 2-4 test classes.
        - Keep the summary under 30 words.

        Every generated path must remain within the permitted test root.

        Return ONLY JSON:

        {
          "files": [
            {
              "path": "...",
              "content": "complete Java test source"
            }
          ],
          "summary": "concise testing summary"
        }
        
        JSON must be strictly valid RFC 8259 JSON.
        Do not include // comments, /* comments */, trailing commas,
        or unescaped newlines inside JSON strings.
        Do not include markdown fences or explanatory prose outside JSON.
        """;

        String userPrompt = """
        Testing objective:

        %s

        Permitted test root:

        %s

        Constraints:

        %s

        Implementation and upstream evidence:

        %s

        Generate tests that evaluate whether the current implementation
        satisfies the supplied engineering objective.
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
            String rawContent =
                    normalizeJsonResponse(
                            response.content()
                    );

            result = objectMapper.readValue(
                    rawContent,
                    CodeGenerationResult.class
            );

        } catch (Exception e) {
            throw new IllegalStateException(
                    "Testing agent returned invalid code-generation JSON",
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

    private String buildTestingContext(List<Artifact> artifacts) {

        return artifacts.stream()
                .map(artifact -> {
                    StringBuilder context = new StringBuilder();

                    context.append("""
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
                    ));

                    String storageLocation =
                            artifact.storageLocation();

                    boolean readableSourceArtifact =
                            storageLocation != null
                                    && (
                                    artifact.type() == ArtifactType.CODE
                                            || (
                                            artifact.type() == ArtifactType.TEST
                                                    && storageLocation.startsWith(
                                                    "src/test/java/"
                                            )
                                    )
                            );

                    if (readableSourceArtifact
                            && workspaceTool.exists(storageLocation)) {

                        context.append("\nSource content:\n");

                        context.append(
                                workspaceTool.readFile(
                                        storageLocation
                                )
                        );

                        context.append("\n");
                    }

                    return context.toString();
                })
                .collect(Collectors.joining("\n"));
    }

    public AmbiguityAssessment assessAmbiguity(
            EngineeringTask task) {

        if (task.role() != AgentRole.ANALYSIS) {
            throw new IllegalArgumentException(
                    "Ambiguity assessment requires ANALYSIS role"
            );
        }

        if (task.upstreamArtifacts() == null
                || task.upstreamArtifacts().isEmpty()) {
            throw new IllegalArgumentException(
                    "Ambiguity assessment requires existing code context"
            );
        }

        if (task.objective() == null
                || task.objective().isBlank()) {
            throw new IllegalArgumentException(
                    "Requirement required for ambiguity assessment"
            );
        }

        String artifactContext =
                buildTestingContext(
                        task.upstreamArtifacts()
                );

        String constraintContext =
                buildConstraintContext(
                        task.constraints()
                );

        String systemPrompt = """
        You are a senior software engineer reviewing an incoming change request
        against an existing application.

        Determine whether the request is sufficiently specified to permit a
        safe implementation.

        Important rules:

        - Inspect the existing code to establish known facts.
        - Distinguish facts from assumptions.
        - Do not invent product requirements.
        - Do not choose among materially different behaviors when stakeholder
          intent is unknown.
        - Identify ambiguities that could materially change API behavior,
          persistence, security, compatibility, or implementation scope.
        - Ask only clarification questions that are necessary to unblock a
          safe implementation.
        - If consequential ambiguity remains, implementationBlocked must be true.
        - Do not generate or modify source code.

        Return ONLY JSON:

        {
          "sufficientlySpecified": true,
          "implementationBlocked": false,
          "knownFacts": [
            "fact established from the existing application"
          ],
          "ambiguities": [
            "material unresolved requirement"
          ],
          "clarificationQuestions": [
            "question requiring stakeholder input"
          ],
          "unsafeAssumptions": [
            "assumption the implementation should not make"
          ],
          "summary": "concise assessment"
        }

        JSON must be strictly valid RFC 8259 JSON.
        Do not include markdown fences or explanatory prose outside JSON.
        """;

        String userPrompt = """
        Requested change:

        %s

        Constraints:

        %s

        Existing application context:

        %s
        """.formatted(
                task.objective(),
                constraintContext,
                artifactContext
        );

        AgentResponse response =
                agentClient.execute(
                        systemPrompt,
                        userPrompt
                );

        try {
            String rawContent =
                    normalizeJsonResponse(
                            response.content()
                    );

            return objectMapper.readValue(
                    rawContent,
                    AmbiguityAssessment.class
            );

        } catch (Exception e) {
            throw new IllegalStateException(
                    "Analysis agent returned invalid ambiguity-assessment JSON",
                    e
            );
        }
    }

    public ImpactAnalysis analyze(
            EngineeringTask task) {

        if (task.role() != AgentRole.ANALYSIS) {
            throw new IllegalArgumentException(
                    "Analysis task requires ANALYSIS role"
            );
        }

        if (task.upstreamArtifacts() == null
                || task.upstreamArtifacts().isEmpty()) {
            throw new IllegalArgumentException(
                    "Analysis requires existing code artifacts"
            );
        }

        if (task.objective() == null
                || task.objective().isBlank()) {
            throw new IllegalArgumentException(
                    "Analysis objective required"
            );
        }

        String artifactContext =
                buildTestingContext(
                        task.upstreamArtifacts()
                );

        String constraintContext =
                buildConstraintContext(
                        task.constraints()
                );

        String systemPrompt = """
        You are a senior software engineer performing brownfield impact analysis.

        Your task is to understand an existing codebase and determine the
        smallest safe change required to satisfy the requested behavior.

        Rules:
        - Do not generate source code.
        - Do not modify files.
        - Identify only files that genuinely need to change.
        - Preserve existing behavior unless the requirement explicitly changes it.
        - Reuse existing framework/repository capabilities where possible.
        - Avoid introducing new abstractions, infrastructure, or dependencies
          unless they are clearly necessary.
        - Identify regression tests that should continue to pass.

        Return ONLY JSON:

        {
          "impactedFiles": [
            "path/to/file"
          ],
          "preservedBehaviors": [
            "existing behavior that must remain unchanged"
          ],
          "implementationSteps": [
            "minimal implementation step"
          ],
          "testChanges": [
            "test that should be added or updated"
          ],
          "risks": [
            "relevant risk or edge case"
          ],
          "summary": "concise impact analysis"
        }

        Do not include markdown fences or explanatory prose outside JSON.
        """;

        String userPrompt = """
        Brownfield change request:

        %s

        Constraints:

        %s

        Existing code and test context:

        %s
        """.formatted(
                task.objective(),
                constraintContext,
                artifactContext
        );

        AgentResponse response =
                agentClient.execute(
                        systemPrompt,
                        userPrompt
                );

        try {
            String rawContent =
                    normalizeJsonResponse(
                            response.content()
                    );

            return objectMapper.readValue(
                    rawContent,
                    ImpactAnalysis.class
            );

        } catch (Exception e) {
            throw new IllegalStateException(
                    "Analysis agent returned invalid impact-analysis JSON",
                    e
            );
        }
    }

    public ValidationAssessment validate(
            EngineeringTask task) {

        if (task.role() != AgentRole.VALIDATION) {
            throw new IllegalArgumentException(
                    "Validation task requires VALIDATION role"
            );
        }

        if (task.upstreamArtifacts() == null
                || task.upstreamArtifacts().isEmpty()) {
            throw new IllegalArgumentException(
                    "Validation requires upstream evidence"
            );
        }

        String artifactContext =
                buildTestingContext(task.upstreamArtifacts());

        String constraintContext =
                buildConstraintContext(task.constraints());

        String systemPrompt = """
        You are a senior engineering validation agent operating inside
        a governed SDLC workflow.

        Evaluate whether the supplied implementation and test evidence
        satisfy the stated acceptance criteria.

        Rules:
        - Do not assume functionality exists without evidence.
        - Passing tests do not automatically mean every requirement is satisfied.
        - Explicitly identify missing or partially implemented criteria.
        - Treat actual test-execution evidence as stronger than proposed tests.
        - Do not generate or modify code.
        - Return PASS only when all required acceptance criteria are supported
          by implementation and validation evidence.

        Return ONLY JSON:

        {
          "status": "PASS|FAIL|ERROR",
          "criteriaResults": {
            "criterion": "PASS|FAIL|PARTIAL - brief reason"
          },
          "gaps": [
            "missing or incomplete capability"
          ],
          "summary": "concise validation summary"
        }
        """;

        String userPrompt = """
        Validation objective:

        %s

        Constraints:

        %s

        Engineering evidence:

        %s
        """.formatted(
                task.objective(),
                constraintContext,
                artifactContext
        );

        AgentResponse response =
                agentClient.execute(
                        systemPrompt,
                        userPrompt
                );

        try {
            String rawContent =
                    normalizeJsonResponse(
                            response.content()
                    );

            return objectMapper.readValue(
                    rawContent,
                    ValidationAssessment.class
            );

        } catch (Exception e) {
            throw new IllegalStateException(
                    "Validation agent returned invalid validation JSON",
                    e
            );
        }
    }


    private String normalizeJsonResponse(
            String content) {

        String rawContent = content.trim();

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

        return rawContent.trim();
    }


}