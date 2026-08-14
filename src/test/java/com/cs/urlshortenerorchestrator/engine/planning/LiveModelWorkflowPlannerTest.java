package com.cs.urlshortenerorchestrator.engine.planning;

import com.cs.urlshortenerorchestrator.engine.agent.OpenAiCompatibleAgentClient;
import org.junit.jupiter.api.Test;

import tools.jackson.databind.ObjectMapper;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

class LiveModelWorkflowPlannerTest {

    @Test
    void liveModelDecomposesAnalyticsRequirementIntoValidWorkflow() {

        /*
         * This is intentionally NOT part of the mandatory deterministic
         * test path. It only runs when live-model configuration is present.
         */
        assumeTrue(
                System.getenv("LLM_BASE_URL") != null
                        && System.getenv("LLM_MODEL") != null
                        && System.getenv("LLM_API_KEY") != null,
                "Live LLM environment variables are not configured"
        );

        var agentClient =
                OpenAiCompatibleAgentClient.fromEnvironment();

        var planner =
                new ModelBackedWorkflowPlanner(
                        agentClient,
                        new ObjectMapper()
                );

        String requirement = """
            Build a greenfield analytics capability for shortened URLs.

            The capability should accept click events, aggregate usage
            statistics, detect simple traffic spikes, and expose dashboard
            data for review.

            Treat this as a production-quality prototype with controlled
            autonomy and human approval before release.
            """;

        WorkflowPlan plan =
                planner.plan(requirement);

        // Requirement understanding
        assertThat(plan.normalizedRequirement())
                .isNotBlank();

        assertThat(plan.assumptions())
                .isNotEmpty();

        assertThat(plan.acceptanceCriteria())
                .isNotEmpty();

        // Decomposition
        assertThat(plan.nodes())
                .isNotEmpty();

        assertThat(plan.nodes().stream()
                .filter(node ->
                        node.dependsOnNodeIds() == null
                                || node.dependsOnNodeIds().isEmpty())
                .count())
                .isEqualTo(1);

        // Governance
        assertThat(plan.nodes())
                .anySatisfy(node -> {
                    if (node.type()
                            == com.cs.urlshortenerorchestrator.engine.domain.NodeType.RELEASE_READY) {
                        assertThat(node.approvalRequired()).isTrue();
                    }
                });

        // Convert AI plan into governed Phase 2 workflow
        WorkflowPlanMapper mapper =
                new WorkflowPlanMapper();

        var workflow =
                mapper.toWorkflow(
                        "live-analytics-greenfield",
                        "Live Analytics Greenfield",
                        plan
                );

        // Existing engine still has final authority over DAG validity
        assertThat(workflow.validate())
                .isEmpty();

        System.out.println(
                "Normalized requirement: "
                        + plan.normalizedRequirement()
        );

        System.out.println(
                "Assumptions: "
                        + plan.assumptions()
        );

        System.out.println(
                "Acceptance criteria: "
                        + plan.acceptanceCriteria()
        );

        System.out.println("Generated DAG:");

        plan.nodes().forEach(node ->
                System.out.println(
                        "  "
                                + node.id()
                                + " ["
                                + node.type()
                                + "] dependsOn="
                                + node.dependsOnNodeIds()
                                + " approval="
                                + node.approvalRequired()
                )
        );
    }
}