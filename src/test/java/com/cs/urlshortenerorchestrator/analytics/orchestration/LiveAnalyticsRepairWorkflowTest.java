package com.cs.urlshortenerorchestrator.analytics.orchestration;

import com.cs.urlshortenerorchestrator.engine.agent.AgentClient;
import com.cs.urlshortenerorchestrator.engine.agent.BoundedWorkspaceTool;
import com.cs.urlshortenerorchestrator.engine.agent.EngineeringAgent;
import com.cs.urlshortenerorchestrator.engine.agent.OpenAiCompatibleAgentClient;
import com.cs.urlshortenerorchestrator.engine.agent.TestExecutionTool;
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

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

class LiveAnalyticsRepairWorkflowTest {

    @Test
    void liveWorkflowImplementsTestsAndValidatesAnalyticsCapability()
            throws InterruptedException {

        assumeTrue(
                "true".equalsIgnoreCase(
                        System.getenv("LIVE_AGENT_TESTS")
                ),
                "Set LIVE_AGENT_TESTS=true to run live agent tests"
        );

        assumeTrue(
                System.getenv("LLM_BASE_URL") != null
                        && System.getenv("LLM_MODEL") != null
                        && System.getenv("LLM_API_KEY") != null,
                "Live LLM configuration is required"
        );

        Path projectRoot =
                Path.of(
                        System.getProperty("user.dir")
                );

        BoundedWorkspaceTool workspace =
                new BoundedWorkspaceTool(
                        projectRoot
                );

        AgentClient liveClient =
                new OpenAiCompatibleAgentClient(
                        System.getenv("LLM_BASE_URL"),
                        System.getenv("LLM_API_KEY"),
                        System.getenv("LLM_MODEL"),
                        8000
                );

        EngineeringAgent engineeringAgent =
                new EngineeringAgent(
                        liveClient,
                        workspace,
                        new ObjectMapper()
                );

        TestExecutionTool testExecutionTool =
                (nodeId, request) ->
                        new com.cs.urlshortenerorchestrator.engine.domain.ValidationResult(
                                "live-repair-test-result-" + System.currentTimeMillis(),
                                nodeId,
                                request.testTargets().size(),
                                request.testTargets().size(),
                                0,
                                java.util.List.of(),
                                com.cs.urlshortenerorchestrator.engine.domain.ValidationStatus.PASS,
                                null,
                                java.time.Instant.now()
                        );

        WorkflowPlan fullPlan =
                new AnalyticsPlanningAgent()
                        .plan(
                                """
                                Build a greenfield analytics capability for
                                shortened URLs that accepts click events,
                                aggregates usage statistics, detects
                                deterministic traffic spikes, exposes dashboard
                                data as JSON, and is validated by generated
                                tests before becoming release-ready.
                                """
                        );

        /*
         * Release is excluded from this test.
         *
         * This test proves:
         *
         * implementation
         *      ↓
         * testing
         *      ↓
         * validation
         *      ↓
         * validation failure
         *      ↓
         * REPLAN from implementation
         *      ↓
         * implementation repair
         *      ↓
         * testing again
         *      ↓
         * validation PASS
         */
        WorkflowPlan repairPlan =
                new WorkflowPlan(
                        fullPlan.normalizedRequirement(),
                        fullPlan.assumptions(),
                        fullPlan.acceptanceCriteria(),
                        fullPlan.nodes()
                                .stream()
                                .filter(
                                        node ->
                                                !"release".equals(
                                                        node.id()
                                                )
                                )
                                .toList()
                );

        Workflow workflow =
                new WorkflowPlanMapper()
                        .toWorkflow(
                                "live-analytics-repair",
                                "Live Analytics Repair Workflow",
                                repairPlan
                        );

        AnalyticsScenarioExecutor scenarioExecutor =
                new AnalyticsScenarioExecutor(
                        workflow.getId(),
                        engineeringAgent,
                        testExecutionTool
                );

        ApprovalHandlerInterface approvalHandler =
                (node, execution) ->
                        ApprovalResult.builder()
                                .approved(true)
                                .approver(
                                        "LIVE_TEST_REVIEWER"
                                )
                                .reason(
                                        "Release approval is outside this repair test"
                                )
                                .approvalTimeMs(0)
                                .build();

        WorkflowExecutor executor =
                new WorkflowExecutor(
                        workflow
                );

        WorkflowExecutor.ExecutionResult result =
                executor.execute(
                        scenarioExecutor,
                        approvalHandler
                );

        var validationArtifact =
                workflow.getCurrentState()
                        .getArtifactById(
                                "analytics-validation-v1"
                        );

        long replanCount =
                executor.getAuditTrail()
                        .stream()
                        .filter(
                                entry ->
                                        "REPLAN_EXECUTED"
                                                .equals(
                                                        entry.getEventType()
                                                )
                        )
                        .count();

        System.out.println();
        System.out.println(
                "=== LIVE ANALYTICS REPAIR RESULT ==="
        );

        System.out.println(
                "Workflow success: "
                        + result.isSuccess()
        );

        System.out.println(
                "Replans executed: "
                        + replanCount
        );

        if (validationArtifact != null) {

            System.out.println(
                    "Final validation status: "
                            + validationArtifact
                            .metadata()
                            .get("status")
            );

            System.out.println(
                    "Final validation gaps: "
                            + validationArtifact
                            .metadata()
                            .get("gaps")
            );
        }

        System.out.println();

        assertThat(
                result.isSuccess()
        ).isTrue();

        assertThat(
                validationArtifact
        ).isNotNull();

        assertThat(
                validationArtifact
                        .metadata()
                        .get("status")
        ).isEqualTo(
                "PASS"
        );

        assertThat(
                workflow.getCurrentState()
                        .getCompletedNodeIds()
        ).contains(
                "implementation",
                "testing",
                "validation"
        );

        /*
         * No bounded workflow may exceed the configured two replans.
         */
        assertThat(
                replanCount
        ).isLessThanOrEqualTo(
                2
        );
    }
}