package com.cs.urlshortenerorchestrator.analytics.orchestration;

import com.cs.urlshortenerorchestrator.engine.agent.AgentClient;
import com.cs.urlshortenerorchestrator.engine.agent.BoundedWorkspaceTool;
import com.cs.urlshortenerorchestrator.engine.agent.EngineeringAgent;
import com.cs.urlshortenerorchestrator.engine.agent.OpenAiCompatibleAgentClient;
import com.cs.urlshortenerorchestrator.engine.domain.ArtifactType;
import com.cs.urlshortenerorchestrator.engine.domain.Workflow;
import com.cs.urlshortenerorchestrator.engine.execution.ApprovalHandlerInterface;
import com.cs.urlshortenerorchestrator.engine.execution.ApprovalResult;
import com.cs.urlshortenerorchestrator.engine.execution.WorkflowExecutor;
import com.cs.urlshortenerorchestrator.engine.planning.AnalyticsPlanningAgent;
import com.cs.urlshortenerorchestrator.engine.planning.WorkflowPlan;
import com.cs.urlshortenerorchestrator.engine.planning.WorkflowPlanMapper;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.ObjectMapper;

import java.nio.file.Path;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

class LiveAnalyticsImplementationWorkflowTest {

    @Test
    void liveAgentCreatesAnalyticsImplementationThroughWorkflow()
            throws InterruptedException {

        /*
         * Safety guard:
         * this test makes a real model call and writes generated source
         * files into the local project.
         *
         * It will be skipped during normal ./mvnw test runs unless
         * LIVE_AGENT_TESTS=true is explicitly set.
         */
        assumeTrue(
                "true".equalsIgnoreCase(
                        System.getenv("LIVE_AGENT_TESTS")),
                "Set LIVE_AGENT_TESTS=true to run live agent tests"
        );

        assumeTrue(
                System.getenv("LLM_BASE_URL") != null
                        && System.getenv("LLM_MODEL") != null
                        && System.getenv("LLM_API_KEY") != null,
                "Live LLM configuration is required"
        );

        /*
         * Real model client.
         *
         * Planning uses the normal smaller token budget elsewhere.
         * Code generation gets a larger completion budget because the
         * model may produce multiple complete Java source files.
         */
        AgentClient liveClient =
                new OpenAiCompatibleAgentClient(
                        System.getenv("LLM_BASE_URL"),
                        System.getenv("LLM_API_KEY"),
                        System.getenv("LLM_MODEL"),
                        6000
                );

        /*
         * Point the bounded workspace at the actual project.
         *
         * BoundedWorkspaceTool still restricts writes to:
         *
         * src/main/java/.../analytics/**
         * src/test/java/.../analytics/**
         *
         * so the live model cannot modify the orchestration engine,
         * target application, pom.xml, or repository metadata.
         */
        Path projectRoot =
                Path.of(System.getProperty("user.dir"));

        BoundedWorkspaceTool workspace =
                new BoundedWorkspaceTool(projectRoot);

        EngineeringAgent engineeringAgent =
                new EngineeringAgent(
                        liveClient,
                        workspace,
                        new ObjectMapper()
                );

        /*
         * We already proved separately that a LIVE model can perform
         * requirement understanding and DAG decomposition.
         *
         * This test intentionally uses the deterministic planner so we
         * isolate and prove the next capability:
         *
         * governed workflow execution -> live implementation agent ->
         * real source-code generation.
         *
         * This also avoids paying for another planning call.
         */
        AnalyticsPlanningAgent planner =
                new AnalyticsPlanningAgent();

        WorkflowPlan fullPlan =
                planner.plan(
                        """
                        Build a greenfield analytics capability for shortened URLs
                        that accepts click events, aggregates usage statistics,
                        detects simple traffic spikes, and exposes dashboard data.
                        """
                );

        /*
         * Execute only through IMPLEMENTATION for this milestone.
         *
         * requirements
         *      |
         *   +--+--+
         *   |     |
         * architecture  test-plan
         *   |     |
         *   +--+--+
         *      |
         *     sync
         *      |
         * implementation
         *
         * Testing, validation and release are deliberately handled in
         * later workflow milestones.
         */
        WorkflowPlan implementationPlan =
                new WorkflowPlan(
                        fullPlan.normalizedRequirement(),
                        fullPlan.assumptions(),
                        fullPlan.acceptanceCriteria(),
                        fullPlan.nodes().stream()
                                .filter(node ->
                                        List.of(
                                                "requirements",
                                                "architecture",
                                                "test-plan",
                                                "sync",
                                                "implementation"
                                        ).contains(node.id())
                                )
                                .toList()
                );

        Workflow workflow =
                new WorkflowPlanMapper()
                        .toWorkflow(
                                "live-analytics-implementation",
                                "Live Analytics Implementation",
                                implementationPlan
                        );

        AnalyticsScenarioExecutor scenarioExecutor =
                new AnalyticsScenarioExecutor(
                        workflow.getId(),
                        engineeringAgent
                );

        /*
         * This workflow slice contains no approval node, but
         * WorkflowExecutor requires an ApprovalHandlerInterface.
         */
        ApprovalHandlerInterface approvalHandler =
                (node, execution) ->
                        ApprovalResult.builder()
                                .approved(true)
                                .approver("LIVE_TEST_REVIEWER")
                                .reason(
                                        "No approval stage in implementation slice")
                                .approvalTimeMs(0)
                                .build();

        WorkflowExecutor executor =
                new WorkflowExecutor(workflow);

        WorkflowExecutor.ExecutionResult result =
                executor.execute(
                        scenarioExecutor,
                        approvalHandler
                );

        /*
         * Workflow itself must succeed.
         */
        assertThat(result.isSuccess())
                .isTrue();

        assertThat(
                workflow.getCurrentState()
                        .getCompletedNodeIds()
        ).containsExactlyInAnyOrder(
                "requirements",
                "architecture",
                "test-plan",
                "sync",
                "implementation"
        );

        /*
         * Implementation must have produced actual CODE artifacts.
         */
        List<String> implementationArtifactIds =
                workflow.getCurrentState()
                        .getArtifactIdsProducedByNode(
                                "implementation"
                        );

        assertThat(implementationArtifactIds)
                .isNotEmpty();

        assertThat(
                implementationArtifactIds.stream()
                        .map(id ->
                                workflow.getCurrentState()
                                        .getArtifactById(id)
                                        .type()
                        )
        ).allMatch(
                type -> type == ArtifactType.CODE
        );

        /*
         * Print generated paths so we can inspect the model's design
         * before compiling/running the generated Analytics feature.
         */
        System.out.println();
        System.out.println(
                "Generated Analytics source files:"
        );

        implementationArtifactIds.forEach(id -> {

            var artifact =
                    workflow.getCurrentState()
                            .getArtifactById(id);

            System.out.println(
                    "  " + artifact.storageLocation()
            );

            assertThat(
                    workspace.exists(
                            artifact.storageLocation()
                    )
            ).isTrue();
        });

        System.out.println();

        /*
         * Evidence that orchestration, not the test itself, caused the
         * implementation work.
         */
        assertThat(executor.getAuditTrail())
                .anySatisfy(entry ->
                        assertThat(
                                entry.getEventType()
                        ).isEqualTo(
                                "ARTIFACT_PRODUCED"
                        )
                );

        assertThat(executor.getAuditTrail())
                .anySatisfy(entry ->
                        assertThat(
                                entry.getEventType()
                        ).isEqualTo(
                                "WORKFLOW_COMPLETED"
                        )
                );
    }
}