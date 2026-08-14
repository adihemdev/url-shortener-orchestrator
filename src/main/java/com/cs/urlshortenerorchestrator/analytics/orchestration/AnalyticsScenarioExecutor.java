package com.cs.urlshortenerorchestrator.analytics.orchestration;

import com.cs.urlshortenerorchestrator.engine.agent.*;
import com.cs.urlshortenerorchestrator.engine.domain.*;
import com.cs.urlshortenerorchestrator.engine.execution.ContextAwareNodeExecutor;
import com.cs.urlshortenerorchestrator.engine.execution.ExecutionContext;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class AnalyticsScenarioExecutor implements ContextAwareNodeExecutor {

    private final String workflowId;
    private final EngineeringAgent engineeringAgent;

    public AnalyticsScenarioExecutor(String workflowId) {
        this(workflowId, null);
    }

    public AnalyticsScenarioExecutor(
            String workflowId,
            EngineeringAgent engineeringAgent) {

        this.workflowId = workflowId;
        this.engineeringAgent = engineeringAgent;
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
                    .errorDetails(e.getClass().getSimpleName()
                            + ": " + e.getMessage())
                    .build();
        }
    }

    private Execution executeRequirements(
            WorkflowNode node,
            int attemptNumber,
            String executionId,
            Instant startedAt,
            ExecutionContext context) {

        Artifact artifact = new Artifact(
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

        boolean hasRequirements = inputs.stream()
                .anyMatch(a ->
                        a.type() == ArtifactType.REQUIREMENT_SPEC);

        if (!hasRequirements) {
            throw new IllegalStateException(
                    "Architecture requires requirement artifact");
        }

        Artifact artifact = new Artifact(
                "analytics-architecture-v1",
                ArtifactType.ARCHITECTURE_PLAN,
                "analytics-architecture.md",
                node.getId(),
                executionId,
                "artifacts/analytics-architecture.md",
                Map.of(
                        "components",
                        "EventStore,Aggregator,AnomalyDetector,Dashboard,REST",
                        "storage", "IN_MEMORY",
                        "deployment", "LOCAL_PROTOTYPE"
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

        boolean hasRequirements = inputs.stream()
                .anyMatch(a ->
                        a.type() == ArtifactType.REQUIREMENT_SPEC);

        if (!hasRequirements) {
            throw new IllegalStateException(
                    "Test planning requires requirement artifact");
        }

        Artifact artifact = new Artifact(
                "analytics-test-plan-v1",
                ArtifactType.TEST,
                "analytics-test-plan.md",
                node.getId(),
                executionId,
                "artifacts/analytics-test-plan.md",
                Map.of(
                        "unitTests",
                        "aggregation,spike-detection,event-validation",
                        "integrationTests",
                        "analytics-api,workflow-e2e",
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
                        .filter(a -> a.type() == ArtifactType.ARCHITECTURE_PLAN)
                        .findFirst()
                        .orElseThrow(() ->
                                new IllegalStateException(
                                        "Synchronization requires architecture artifact"
                                )
                        );

        Artifact testPlan =
                predecessorArtifacts.stream()
                        .filter(a -> a.type() == ArtifactType.TEST)
                        .findFirst()
                        .orElseThrow(() ->
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
                                "architectureArtifactId", architecture.id(),
                                "testPlanArtifactId", testPlan.id(),
                                "status", "ALIGNED"
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

        Artifact syncArtifact =
                context.getArtifactsFromAllPredecessors()
                        .stream()
                        .findFirst()
                        .orElseThrow(() ->
                                new IllegalStateException(
                                        "Implementation requires synchronization artifact"
                                )
                        );

        String architectureArtifactId =
                syncArtifact.metadata().get("architectureArtifactId");

        String testPlanArtifactId =
                syncArtifact.metadata().get("testPlanArtifactId");

        Artifact architectureArtifact =
                context.getArtifactById(architectureArtifactId);

        Artifact testPlanArtifact =
                context.getArtifactById(testPlanArtifactId);

        if (architectureArtifact == null || testPlanArtifact == null) {
            throw new IllegalStateException(
                    "Implementation could not resolve synchronized upstream artifacts"
            );
        }

        List<Artifact> upstreamArtifacts =
                List.of(
                        architectureArtifact,
                        testPlanArtifact
                );

        if (upstreamArtifacts.isEmpty()) {
            throw new IllegalStateException(
                    "Implementation requires upstream SDLC artifacts"
            );
        }

        EngineeringTask task =
                new EngineeringTask(
                        AgentRole.IMPLEMENTATION,
                        "Implement the greenfield Analytics capability described by the upstream SDLC artifacts.",
                        upstreamArtifacts,
                        List.of(
                                "Use Java 21 and Spring Boot",
                                "Use in-memory/local components only",
                                "Do not introduce external infrastructure unless justified by upstream artifacts",
                                "Do not modify the existing URL-shortener implementation",
                                "Do not modify the orchestration engine",
                                "Generate only files necessary for a coherent, compilable implementation"
                        ),
                        "src/main/java/com/cs/urlshortenerorchestrator/analytics/"
                );

        // ADD THIS
        if (engineeringAgent == null) {
            throw new IllegalStateException(
                    "EngineeringAgent required for implementation execution"
            );
        }

        CodeGenerationResult generationResult =
                engineeringAgent.execute(task);

        List<String> artifactIds = new ArrayList<>();

        int index = 1;

        for (GeneratedFile generatedFile : generationResult.files()) {

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
                                    "scenario", "GREENFIELD",
                                    "feature", "ANALYTICS",
                                    "generatedBy", "EngineeringAgent"
                            ),
                            Instant.now()
                    );

            context.publishArtifact(artifact);
            artifactIds.add(artifactId);
        }

        return successfulExecution(
                node,
                attemptNumber,
                executionId,
                startedAt,
                artifactIds
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