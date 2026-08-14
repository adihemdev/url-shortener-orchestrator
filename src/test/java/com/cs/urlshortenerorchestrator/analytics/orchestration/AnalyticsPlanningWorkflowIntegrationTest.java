package com.cs.urlshortenerorchestrator.analytics.orchestration;


import com.cs.urlshortenerorchestrator.engine.domain.ArtifactType;
import com.cs.urlshortenerorchestrator.engine.domain.Workflow;
import com.cs.urlshortenerorchestrator.engine.execution.ApprovalHandlerInterface;
import com.cs.urlshortenerorchestrator.engine.execution.ApprovalResult;
import com.cs.urlshortenerorchestrator.engine.execution.WorkflowExecutor;
import com.cs.urlshortenerorchestrator.engine.planning.AnalyticsPlanningAgent;
import com.cs.urlshortenerorchestrator.engine.planning.WorkflowPlan;
import com.cs.urlshortenerorchestrator.engine.planning.WorkflowPlanMapper;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class AnalyticsPlanningWorkflowIntegrationTest {

    @Test
    void requirementIsDecomposedAndExecutedThroughDesignSynchronization()
            throws InterruptedException {

        // 1. Raw greenfield requirement
        String requirement =
                "Add analytics for URL clicks with aggregation, "
                        + "traffic spike detection and dashboard data.";

        // 2. Planning agent turns requirement into structured decomposition
        AnalyticsPlanningAgent planner =
                new AnalyticsPlanningAgent();

        WorkflowPlan fullPlan =
                planner.plan(requirement);

        /*
         * For this milestone execute only the stages that are genuinely
         * implemented so far:
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
         */
        WorkflowPlan planningPlan =
                new WorkflowPlan(
                        fullPlan.normalizedRequirement(),
                        fullPlan.assumptions(),
                        fullPlan.acceptanceCriteria(),
                        fullPlan.nodes().stream()
                                .filter(node -> List.of(
                                        "requirements",
                                        "architecture",
                                        "test-plan",
                                        "sync"
                                ).contains(node.id()))
                                .toList()
                );

        // 3. Convert agent-produced plan into Phase 2 executable Workflow
        Workflow workflow =
                new WorkflowPlanMapper().toWorkflow(
                        "analytics-greenfield-planning",
                        "Analytics Greenfield Planning Workflow",
                        planningPlan
                );

        // 4. Stage-specific context-aware executor
        AnalyticsScenarioExecutor scenarioExecutor =
                new AnalyticsScenarioExecutor(workflow.getId());

        // No approval exists in this partial workflow, but WorkflowExecutor
        // requires an ApprovalHandlerInterface.
        ApprovalHandlerInterface approvalHandler =
                (node, execution) -> ApprovalResult.builder()
                        .approved(true)
                        .approver("TEST_REVIEWER")
                        .reason("Approved for integration testing")
                        .approvalTimeMs(0)
                        .build();

        WorkflowExecutor executor =
                new WorkflowExecutor(workflow);

        // 5. Execute actual DAG
        WorkflowExecutor.ExecutionResult result =
                executor.execute(
                        scenarioExecutor,
                        approvalHandler
                );

        // ---- Workflow outcome ----

        assertThat(result.isSuccess()).isTrue();

        assertThat(workflow.getCurrentState().getCompletedNodeIds())
                .containsExactlyInAnyOrder(
                        "requirements",
                        "architecture",
                        "test-plan",
                        "sync"
                );

        // ---- Requirement artifact ----

        assertThat(
                workflow.getCurrentState()
                        .getArtifactIdsProducedByNode("requirements")
        ).contains("analytics-requirements-v1");

        // ---- Architecture consumed requirements and produced design ----

        assertThat(
                workflow.getCurrentState()
                        .getArtifactIdsProducedByNode("architecture")
        ).contains("analytics-architecture-v1");

        // ---- Test planning independently consumed requirements ----

        assertThat(
                workflow.getCurrentState()
                        .getArtifactIdsProducedByNode("test-plan")
        ).contains("analytics-test-plan-v1");

        // ---- Validate actual artifact types ----

        assertThat(
                workflow.getCurrentState()
                        .getArtifactById("analytics-requirements-v1")
                        .type()
        ).isEqualTo(ArtifactType.REQUIREMENT_SPEC);

        assertThat(
                workflow.getCurrentState()
                        .getArtifactById("analytics-architecture-v1")
                        .type()
        ).isEqualTo(ArtifactType.ARCHITECTURE_PLAN);

        assertThat(
                workflow.getCurrentState()
                        .getArtifactById("analytics-test-plan-v1")
                        .type()
        ).isEqualTo(ArtifactType.TEST);

        // ---- Audit evidence ----

        assertThat(executor.getAuditTrail())
                .anySatisfy(entry ->
                        assertThat(entry.getEventType())
                                .isEqualTo("WORKFLOW_COMPLETED")
                );

        assertThat(executor.getAuditTrail())
                .filteredOn(entry ->
                        entry.getEventType().equals("ARTIFACT_PRODUCED"))
                .hasSize(4);

        // ---- Metrics ----

        assertThat(executor.getMetrics().getTotalNodes())
                .isEqualTo(4);

        assertThat(executor.getMetrics().getCompletedNodes())
                .isEqualTo(4);
    }
}