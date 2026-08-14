package com.cs.urlshortenerorchestrator.analytics.orchestration;

import com.cs.urlshortenerorchestrator.analytics.orchestration.AnalyticsScenarioExecutor;
import com.cs.urlshortenerorchestrator.engine.domain.*;
import com.cs.urlshortenerorchestrator.engine.execution.ExecutionContext;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class AnalyticsScenarioExecutorTest {

    @Test
    void requirementsExecutorPublishesRequirementArtifact() {
        WorkflowNode node = WorkflowNode.builder(
                        "requirements",
                        NodeType.REQUIREMENT_ANALYSIS)
                .build();

        WorkflowState state =
                new WorkflowState("requirements");

        ExecutionMetrics metrics =
                new ExecutionMetrics();

        List<Artifact> published =
                new ArrayList<>();

        ExecutionContext context =
                new ExecutionContext(
                        node,
                        null,
                        state,
                        metrics,
                        published::add
                );

        AnalyticsScenarioExecutor executor =
                new AnalyticsScenarioExecutor(
                        "analytics-greenfield");

        Execution execution =
                executor.execute(node, 1, context);

        assertThat(execution.getStatus())
                .isEqualTo(ExecutionStatus.SUCCESS);

        assertThat(published)
                .hasSize(1);

        assertThat(published.get(0).type())
                .isEqualTo(ArtifactType.REQUIREMENT_SPEC);
    }
}