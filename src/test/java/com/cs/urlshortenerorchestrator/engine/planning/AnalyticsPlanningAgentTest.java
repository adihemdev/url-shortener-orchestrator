package com.cs.urlshortenerorchestrator.engine.planning;

import com.cs.urlshortenerorchestrator.engine.domain.NodeType;
import com.cs.urlshortenerorchestrator.engine.planning.WorkflowPlan;
import com.cs.urlshortenerorchestrator.engine.planning.WorkflowPlanMapper;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class AnalyticsPlanningAgentTest {

    @Test
    void plansGreenfieldAnalyticsWorkflowFromRequirement() {
        AnalyticsPlanningAgent planner = new AnalyticsPlanningAgent();

        WorkflowPlan plan = planner.plan(
                "Add analytics for URL clicks with aggregation and anomaly detection"
        );

        assertThat(plan.normalizedRequirement()).isNotBlank();
        assertThat(plan.assumptions()).isNotEmpty();
        assertThat(plan.acceptanceCriteria()).isNotEmpty();

        assertThat(plan.nodes())
                .extracting(node -> node.type())
                .contains(
                        NodeType.REQUIREMENT_ANALYSIS,
                        NodeType.ARCHITECTURE_DESIGN,
                        NodeType.TEST_PLANNING,
                        NodeType.SYNCHRONIZATION,
                        NodeType.IMPLEMENTATION,
                        NodeType.TESTING,
                        NodeType.VALIDATION,
                        NodeType.RELEASE_READY
                );

        assertThat(plan.nodes().stream()
                .filter(node -> node.id().equals("release"))
                .findFirst()
                .orElseThrow()
                .approvalRequired())
                .isTrue();

        WorkflowPlanMapper mapper = new WorkflowPlanMapper();

        var workflow = mapper.toWorkflow(
                "analytics-greenfield",
                "Analytics Greenfield Workflow",
                plan
        );

        assertThat(workflow.validate()).isEmpty();
        assertThat(workflow.getNodesById()).hasSize(8);
    }
}