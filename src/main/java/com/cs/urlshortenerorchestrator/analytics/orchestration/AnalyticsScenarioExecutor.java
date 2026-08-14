package com.cs.urlshortenerorchestrator.analytics.orchestration;

import com.cs.urlshortenerorchestrator.engine.agent.*;
import com.cs.urlshortenerorchestrator.engine.domain.Artifact;
import com.cs.urlshortenerorchestrator.engine.domain.ArtifactType;
import com.cs.urlshortenerorchestrator.engine.domain.Execution;
import com.cs.urlshortenerorchestrator.engine.domain.ExecutionStatus;
import com.cs.urlshortenerorchestrator.engine.domain.ValidationResult;
import com.cs.urlshortenerorchestrator.engine.domain.ValidationStatus;
import com.cs.urlshortenerorchestrator.engine.domain.WorkflowNode;
import com.cs.urlshortenerorchestrator.engine.execution.ContextAwareNodeExecutor;
import com.cs.urlshortenerorchestrator.engine.execution.ExecutionContext;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Scenario adapter for the greenfield Analytics workflow.
 *
 * This class supplies scenario-specific objectives, constraints and artifact
 * wiring while delegating engineering decisions to EngineeringAgent.
 *
 * The generic WorkflowExecutor remains responsible for lifecycle orchestration,
 * dependencies, retries, gates, approvals and workflow state.
 */
public class AnalyticsScenarioExecutor
        implements ContextAwareNodeExecutor {

    private final String workflowId;
    private final EngineeringAgent engineeringAgent;
    private final TestExecutionTool testExecutionTool;

    public AnalyticsScenarioExecutor(String workflowId) {
        this(workflowId, null, null);
    }

    public AnalyticsScenarioExecutor(
            String workflowId,
            EngineeringAgent engineeringAgent) {

        this(workflowId, engineeringAgent, null);
    }

    public AnalyticsScenarioExecutor(
            String workflowId,
            EngineeringAgent engineeringAgent,
            TestExecutionTool testExecutionTool) {

        this.workflowId = workflowId;
        this.engineeringAgent = engineeringAgent;
        this.testExecutionTool = testExecutionTool;
    }

    @Override
    public Execution execute(
            WorkflowNode node,
            int attemptNumber,
            ExecutionContext context) {

        Instant startedAt = Instant.now();

        String executionId =
                "exec-" + node.getId() + "-" + attemptNumber;

        try {
            return switch (node.getId()) {

                case "requirements" ->
                        executeRequirements(
                                node,
                                attemptNumber,
                                executionId,
                                startedAt,
                                context
                        );

                case "architecture" ->
                        executeArchitecture(
                                node,
                                attemptNumber,
                                executionId,
                                startedAt,
                                context
                        );

                case "test-plan" ->
                        executeTestPlan(
                                node,
                                attemptNumber,
                                executionId,
                                startedAt,
                                context
                        );

                case "sync" ->
                        executeSynchronization(
                                node,
                                attemptNumber,
                                executionId,
                                startedAt,
                                context
                        );

                case "implementation" ->
                        executeImplementation(
                                node,
                                attemptNumber,
                                executionId,
                                startedAt,
                                context
                        );

                case "testing" ->
                        executeTesting(
                                node,
                                attemptNumber,
                                executionId,
                                startedAt,
                                context
                        );

                case "validation" ->
                        executeValidation(
                                node,
                                attemptNumber,
                                executionId,
                                startedAt,
                                context
                        );

                default ->
                        successfulExecution(
                                node,
                                attemptNumber,
                                executionId,
                                startedAt,
                                List.of()
                        );
            };

        } catch (Exception e) {

            return Execution.builder()
                    .id(executionId)
                    .workflowId(workflowId)
                    .nodeId(node.getId())
                    .attemptNumber(attemptNumber)
                    .status(ExecutionStatus.FAILED)
                    .startedAt(startedAt)
                    .endedAt(Instant.now())
                    .errorDetails(
                            e.getClass().getSimpleName()
                                    + ": "
                                    + e.getMessage()
                    )
                    .build();
        }
    }

    private Execution executeRequirements(
            WorkflowNode node,
            int attemptNumber,
            String executionId,
            Instant startedAt,
            ExecutionContext context) {

        Artifact artifact =
                new Artifact(
                        "analytics-requirements-v1",
                        ArtifactType.REQUIREMENT_SPEC,
                        "analytics-requirements.md",
                        node.getId(),
                        executionId,
                        "artifacts/analytics-requirements.md",
                        Map.of(
                                "scenario", "GREENFIELD",
                                "feature", "ANALYTICS",
                                "acceptanceCriteria",
                                "event-ingestion,aggregation,spike-detection,dashboard,validation"
                        ),
                        Instant.now()
                );

        context.publishArtifact(artifact);

        return successfulExecution(
                node,
                attemptNumber,
                executionId,
                startedAt,
                List.of(artifact.id())
        );
    }

    private Execution executeArchitecture(
            WorkflowNode node,
            int attemptNumber,
            String executionId,
            Instant startedAt,
            ExecutionContext context) {

        List<Artifact> inputs =
                context.getArtifactsFromAllPredecessors();

        boolean hasRequirements =
                inputs.stream()
                        .anyMatch(
                                artifact ->
                                        artifact.type()
                                                == ArtifactType.REQUIREMENT_SPEC
                        );

        if (!hasRequirements) {
            throw new IllegalStateException(
                    "Architecture requires requirement artifact"
            );
        }

        Artifact artifact =
                new Artifact(
                        "analytics-architecture-v1",
                        ArtifactType.ARCHITECTURE_PLAN,
                        "analytics-architecture.md",
                        node.getId(),
                        executionId,
                        "artifacts/analytics-architecture.md",
                        Map.of(
                                "components",
                                "event-ingestion,aggregation,anomaly-detection,dashboard-api",
                                "storage",
                                "IN_MEMORY",
                                "deployment",
                                "LOCAL_PROTOTYPE"
                        ),
                        Instant.now()
                );

        context.publishArtifact(artifact);

        return successfulExecution(
                node,
                attemptNumber,
                executionId,
                startedAt,
                List.of(artifact.id())
        );
    }

    private Execution executeTestPlan(
            WorkflowNode node,
            int attemptNumber,
            String executionId,
            Instant startedAt,
            ExecutionContext context) {

        List<Artifact> inputs =
                context.getArtifactsFromAllPredecessors();

        boolean hasRequirements =
                inputs.stream()
                        .anyMatch(
                                artifact ->
                                        artifact.type()
                                                == ArtifactType.REQUIREMENT_SPEC
                        );

        if (!hasRequirements) {
            throw new IllegalStateException(
                    "Test planning requires requirement artifact"
            );
        }

        Artifact artifact =
                new Artifact(
                        "analytics-test-plan-v1",
                        ArtifactType.TEST,
                        "analytics-test-plan.md",
                        node.getId(),
                        executionId,
                        "artifacts/analytics-test-plan.md",
                        Map.of(
                                "unitTests",
                                "event-storage,aggregation,spike-detection",
                                "integrationTests",
                                "analytics-api",
                                "acceptanceTests",
                                "dashboard-valid,spike-detected"
                        ),
                        Instant.now()
                );

        context.publishArtifact(artifact);

        return successfulExecution(
                node,
                attemptNumber,
                executionId,
                startedAt,
                List.of(artifact.id())
        );
    }

    private Execution executeSynchronization(
            WorkflowNode node,
            int attemptNumber,
            String executionId,
            Instant startedAt,
            ExecutionContext context) {

        List<Artifact> predecessorArtifacts =
                context.getArtifactsFromAllPredecessors();

        Artifact architecture =
                predecessorArtifacts.stream()
                        .filter(
                                artifact ->
                                        artifact.type()
                                                == ArtifactType.ARCHITECTURE_PLAN
                        )
                        .findFirst()
                        .orElseThrow(
                                () ->
                                        new IllegalStateException(
                                                "Synchronization requires architecture artifact"
                                        )
                        );

        Artifact testPlan =
                predecessorArtifacts.stream()
                        .filter(
                                artifact ->
                                        artifact.type()
                                                == ArtifactType.TEST
                        )
                        .findFirst()
                        .orElseThrow(
                                () ->
                                        new IllegalStateException(
                                                "Synchronization requires test-plan artifact"
                                        )
                        );

        Artifact syncArtifact =
                new Artifact(
                        "analytics-design-sync-v1",
                        ArtifactType.DOCUMENTATION,
                        "analytics-design-sync.md",
                        node.getId(),
                        executionId,
                        "artifacts/analytics-design-sync.md",
                        Map.of(
                                "architectureArtifactId",
                                architecture.id(),
                                "testPlanArtifactId",
                                testPlan.id(),
                                "status",
                                "ALIGNED"
                        ),
                        Instant.now()
                );

        context.publishArtifact(syncArtifact);

        return successfulExecution(
                node,
                attemptNumber,
                executionId,
                startedAt,
                List.of(syncArtifact.id())
        );
    }

    private Execution executeImplementation(
            WorkflowNode node,
            int attemptNumber,
            String executionId,
            Instant startedAt,
            ExecutionContext context) {

        if (engineeringAgent == null) {
            throw new IllegalStateException(
                    "EngineeringAgent required for implementation execution"
            );
        }

        Artifact syncArtifact =
                context.getArtifactsFromAllPredecessors()
                        .stream()
                        .findFirst()
                        .orElseThrow(
                                () -> new IllegalStateException(
                                        "Implementation requires synchronization artifact"
                                )
                        );

        String architectureArtifactId =
                syncArtifact.metadata()
                        .get("architectureArtifactId");

        String testPlanArtifactId =
                syncArtifact.metadata()
                        .get("testPlanArtifactId");

        Artifact architectureArtifact =
                context.getArtifactById(
                        architectureArtifactId
                );

        Artifact testPlanArtifact =
                context.getArtifactById(
                        testPlanArtifactId
                );

        if (architectureArtifact == null
                || testPlanArtifact == null) {

            throw new IllegalStateException(
                    "Implementation could not resolve synchronized upstream artifacts"
            );
        }

        List<Artifact> upstreamArtifacts =
                new ArrayList<>();

        upstreamArtifacts.add(
                architectureArtifact
        );

        upstreamArtifacts.add(
                testPlanArtifact
        );

        /*
         * On a replan/repair cycle, validation has already produced
         * evidence describing what the previous implementation failed
         * to satisfy.
         *
         * Give the implementation agent:
         *
         * 1. Current implementation source
         * 2. Validation feedback
         *
         * so it can repair the existing solution rather than blindly
         * regenerating the same implementation.
         */
        Artifact previousValidation =
                context.getArtifactById(
                        "analytics-validation-v1"
                );

        if (previousValidation != null) {

            context.getAllArtifacts()
                    .stream()
                    .filter(
                            artifact ->
                                    artifact.type()
                                            == ArtifactType.CODE
                    )
                    .forEach(
                            upstreamArtifacts::add
                    );

            upstreamArtifacts.add(
                    previousValidation
            );
        }

        EngineeringTask task =
                new EngineeringTask(
                        AgentRole.IMPLEMENTATION,
                        """
                        Implement the greenfield Analytics capability described
                        by the upstream SDLC artifacts.
    
                        If validation feedback from a previous implementation
                        attempt is present, repair the existing implementation
                        so that the identified acceptance-criteria gaps are
                        satisfied. Preserve working behavior and avoid unrelated
                        changes.
                        """,
                        upstreamArtifacts,
                        List.of(
                                "Use Java 21 and Spring Boot",
                                "Use in-memory/local components only",
                                "Do not introduce external infrastructure unless justified by upstream artifacts",
                                "Do not modify the existing URL-shortener implementation",
                                "Do not modify the orchestration engine",
                                "Generate only files necessary for a coherent, compilable implementation",
                                "On repair attempts, preserve working behavior and address validation gaps"
                        ),
                        "src/main/java/com/cs/urlshortenerorchestrator/analytics/"
                );

        CodeGenerationResult generationResult =
                engineeringAgent.execute(
                        task
                );

        List<String> artifactIds =
                new ArrayList<>();

        int index = 1;

        for (GeneratedFile generatedFile :
                generationResult.files()) {

            String artifactId =
                    "analytics-code-" + index++;

            Artifact artifact =
                    new Artifact(
                            artifactId,
                            ArtifactType.CODE,
                            generatedFile.path(),
                            node.getId(),
                            executionId,
                            generatedFile.path(),
                            Map.of(
                                    "scenario",
                                    "GREENFIELD",

                                    "feature",
                                    "ANALYTICS",

                                    "generatedBy",
                                    "EngineeringAgent",

                                    "attempt",
                                    String.valueOf(
                                            attemptNumber
                                    )
                            ),
                            Instant.now()
                    );

            context.publishArtifact(
                    artifact
            );

            artifactIds.add(
                    artifactId
            );
        }

        return successfulExecution(
                node,
                attemptNumber,
                executionId,
                startedAt,
                artifactIds
        );
    }

    private Execution executeTesting(
            WorkflowNode node,
            int attemptNumber,
            String executionId,
            Instant startedAt,
            ExecutionContext context) {

        if (engineeringAgent == null) {
            throw new IllegalStateException(
                    "EngineeringAgent required for testing execution"
            );
        }

        if (testExecutionTool == null) {
            throw new IllegalStateException(
                    "TestExecutionTool required for testing execution"
            );
        }

        List<Artifact> upstreamArtifacts =
                new ArrayList<>(
                        context.getArtifactsFromAllPredecessors()
                );

        if (upstreamArtifacts.isEmpty()) {
            throw new IllegalStateException(
                    "Testing requires implementation artifacts"
            );
        }

        /*
         * On a testing replan, give the testing agent its previous
         * generated tests plus the execution failure that caused
         * the replan.
         */
        Artifact previousTestExecution =
                context.getArtifactById(
                        "analytics-test-execution-v1"
                );

        if (previousTestExecution != null) {

            context.getAllArtifacts()
                    .stream()
                    .filter(artifact ->
                            "testing".equals(
                                    artifact.producedByNodeId()
                            ))
                    .filter(artifact ->
                            !"analytics-test-execution-v1"
                                    .equals(artifact.id()))
                    .forEach(upstreamArtifacts::add);

            upstreamArtifacts.add(
                    previousTestExecution
            );
        }

        EngineeringTask task =
                new EngineeringTask(
                        AgentRole.TESTING,
                        """
                        Generate tests that validate the current Analytics
                        implementation against its requirements and acceptance
                        criteria.
    
                        If previous generated tests and test-execution failure
                        evidence are supplied, repair those tests so they compile
                        and correctly validate the current implementation.
    
                        Do not modify production source code merely to make a bad
                        test pass.
                        """,
                        upstreamArtifacts,
                        List.of(
                                "Use JUnit 5",
                                "Do not modify production source code",
                                "Test observable behavior",
                                "Generate only focused tests necessary to validate the implementation",
                                "Generated tests must compile against the current implementation",
                                "Do not invent production APIs that do not exist",
                                "If repairing tests, preserve valid coverage while correcting invalid assertions or mocking"
                        ),
                        "src/test/java/com/cs/urlshortenerorchestrator/analytics/"
                );

        CodeGenerationResult generationResult =
                engineeringAgent.execute(task);

        /*
         * Record generated test source before executing it.
         * Even failed generated tests are useful engineering evidence.
         */
        List<String> artifactIds =
                new ArrayList<>();

        int index = 1;

        for (GeneratedFile generatedFile :
                generationResult.files()) {

            String artifactId =
                    "analytics-test-code-" + index++;

            Artifact artifact =
                    new Artifact(
                            artifactId,
                            ArtifactType.TEST,
                            generatedFile.path(),
                            node.getId(),
                            executionId,
                            generatedFile.path(),
                            Map.of(
                                    "scenario", "GREENFIELD",
                                    "feature", "ANALYTICS",
                                    "generatedBy", "EngineeringAgent",
                                    "role", "TESTING",
                                    "attempt",
                                    String.valueOf(attemptNumber)
                            ),
                            Instant.now()
                    );

            context.publishArtifact(artifact);
            artifactIds.add(artifactId);
        }

        List<String> testClassNames =
                generationResult.files()
                        .stream()
                        .map(GeneratedFile::path)
                        .map(path ->
                                path.substring(
                                        path.lastIndexOf('/') + 1
                                ))
                        .map(fileName ->
                                fileName.replace(
                                        ".java",
                                        ""
                                ))
                        .toList();

        TestExecutionRequest testRequest =
                new TestExecutionRequest(
                        List.of(
                                "./mvnw",
                                "-q",
                                "-Dtest="
                                        + String.join(
                                        ",",
                                        testClassNames
                                ),
                                "test"
                        ),
                        testClassNames
                );

        ValidationResult testResult =
                testExecutionTool.runTests(
                        node.getId(),
                        testRequest
                );

        /*
         * Always publish the result, including failures.
         * The TESTING exit gate decides whether this evidence is good
         * enough to continue or must trigger a testing replan.
         */
        String executionError =
                testResult.error() == null
                        ? ""
                        : testResult.error();

        /*
         * Keep failure evidence bounded before passing it back to an LLM.
         */
        if (executionError.length() > 12000) {
            executionError =
                    executionError.substring(0, 12000);
        }

        java.util.Map<String, String> resultMetadata =
                new java.util.HashMap<>();

        resultMetadata.put(
                "status",
                testResult.status().name()
        );

        resultMetadata.put(
                "totalTests",
                String.valueOf(testResult.totalTests())
        );

        resultMetadata.put(
                "passedTests",
                String.valueOf(testResult.passedTests())
        );

        resultMetadata.put(
                "failedTests",
                String.valueOf(testResult.failedTests())
        );

        resultMetadata.put(
                "error",
                executionError
        );

        resultMetadata.put(
                "attempt",
                String.valueOf(attemptNumber)
        );

        String resultArtifactId =
                "analytics-test-execution-v1";

        Artifact resultArtifact =
                new Artifact(
                        resultArtifactId,
                        ArtifactType.TEST,
                        "analytics-test-execution",
                        node.getId(),
                        executionId,
                        "runtime:test-execution",
                        resultMetadata,
                        Instant.now()
                );

        context.publishArtifact(resultArtifact);
        artifactIds.add(resultArtifactId);

        /*
         * Execution succeeded because the testing stage successfully
         * generated tests and collected test evidence.
         *
         * PASS/FAIL of that evidence is handled by the exit gate.
         */
        return successfulExecution(
                node,
                attemptNumber,
                executionId,
                startedAt,
                artifactIds
        );
    }

    private Execution executeValidation(
            WorkflowNode node,
            int attemptNumber,
            String executionId,
            Instant startedAt,
            ExecutionContext context) {

        if (engineeringAgent == null) {
            throw new IllegalStateException(
                    "EngineeringAgent required for validation"
            );
        }

        /*
         * Validation needs more than just the direct predecessor.
         * It needs requirements + implementation + test evidence.
         */
        List<Artifact> evidence =
                context.getAllArtifacts();

        if (evidence.isEmpty()) {
            throw new IllegalStateException(
                    "Validation requires engineering evidence"
            );
        }

        boolean hasTestExecutionEvidence =
                evidence.stream()
                        .anyMatch(artifact ->
                                "analytics-test-execution-v1"
                                        .equals(artifact.id()));

        if (!hasTestExecutionEvidence) {
            throw new IllegalStateException(
                    "Validation requires executed test evidence"
            );
        }

        EngineeringTask task =
                new EngineeringTask(
                        AgentRole.VALIDATION,
                        """
                        Validate the greenfield Analytics implementation against
                        the original acceptance criteria.
    
                        Required capabilities:
                        - click events can be accepted and stored
                        - click counts can be aggregated
                        - deterministic traffic spikes can be detected
                        - dashboard data can be produced as JSON
                        - generated Analytics tests execute successfully
    
                        Identify every unsupported or partially supported criterion.
                        """,
                        evidence,
                        List.of(
                                "Do not generate code",
                                "Do not infer functionality without implementation evidence",
                                "Passing tests alone do not prove untested requirements",
                                "All required criteria must be satisfied for PASS"
                        ),
                        "src/main/java/com/cs/urlshortenerorchestrator/analytics/"
                );

        ValidationAssessment assessment =
                engineeringAgent.validate(task);

        String artifactId =
                "analytics-validation-v1";

        Artifact validationArtifact =
                new Artifact(
                        artifactId,
                        ArtifactType.DOCUMENTATION,
                        "analytics-validation",
                        node.getId(),
                        executionId,
                        "runtime:validation",
                        Map.of(
                                "status",
                                assessment.status().name(),

                                "summary",
                                assessment.summary(),

                                "gaps",
                                String.join(
                                        " | ",
                                        assessment.gaps()
                                )
                        ),
                        Instant.now()
                );

        context.publishArtifact(validationArtifact);

        return successfulExecution(
                node,
                attemptNumber,
                executionId,
                startedAt,
                List.of(artifactId)
        );
    }

    private Execution successfulExecution(
            WorkflowNode node,
            int attemptNumber,
            String executionId,
            Instant startedAt,
            List<String> artifactIds) {

        return Execution.builder()
                .id(executionId)
                .workflowId(workflowId)
                .nodeId(node.getId())
                .attemptNumber(attemptNumber)
                .status(ExecutionStatus.SUCCESS)
                .startedAt(startedAt)
                .endedAt(Instant.now())
                .producedArtifactIds(artifactIds)
                .build();
    }
}