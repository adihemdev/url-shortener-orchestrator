package com.cs.urlshortenerorchestrator.analytics.orchestration;


import com.cs.urlshortenerorchestrator.engine.agent.*;
import com.cs.urlshortenerorchestrator.engine.domain.ArtifactType;
import com.cs.urlshortenerorchestrator.engine.domain.Workflow;
import com.cs.urlshortenerorchestrator.engine.execution.ApprovalHandlerInterface;
import com.cs.urlshortenerorchestrator.engine.execution.ApprovalResult;
import com.cs.urlshortenerorchestrator.engine.execution.WorkflowExecutor;
import com.cs.urlshortenerorchestrator.engine.planning.AnalyticsPlanningAgent;
import com.cs.urlshortenerorchestrator.engine.planning.WorkflowPlan;
import com.cs.urlshortenerorchestrator.engine.planning.WorkflowPlanMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import tools.jackson.databind.ObjectMapper;

import java.nio.file.Path;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class AnalyticsImplementationWorkflowIntegrationTest {

    @TempDir
    Path projectRoot;

    @Test
    void workflowDrivesImplementationAgentAndProducesCodeArtifacts()
            throws InterruptedException {

        String modelOutput = """
            {
              "files": [
                {
                  "path": "src/main/java/com/cs/urlshortenerorchestrator/analytics/domain/ClickEvent.java",
                  "content": "package com.cs.urlshortenerorchestrator.analytics.domain; public record ClickEvent(String shortCode) {}"
                },
                {
                  "path": "src/main/java/com/cs/urlshortenerorchestrator/analytics/service/EventStore.java",
                  "content": "package com.cs.urlshortenerorchestrator.analytics.service; public class EventStore {}"
                }
              ],
              "summary": "Created minimal analytics implementation."
            }
            """;

        AgentClient fakeClient =
                (systemPrompt, userPrompt) ->
                        new AgentResponse(
                                modelOutput,
                                "test-model",
                                100,
                                200
                        );

        BoundedWorkspaceTool workspaceTool =
                new BoundedWorkspaceTool(projectRoot);

        EngineeringAgent engineeringAgent =
                new EngineeringAgent(
                        fakeClient,
                        workspaceTool,
                        new ObjectMapper()
                );

        AnalyticsPlanningAgent planner =
                new AnalyticsPlanningAgent();

        WorkflowPlan fullPlan =
                planner.plan(
                        "Build analytics for shortened URLs"
                );

        WorkflowPlan implementationPlan =
                new WorkflowPlan(
                        fullPlan.normalizedRequirement(),
                        fullPlan.assumptions(),
                        fullPlan.acceptanceCriteria(),
                        fullPlan.nodes().stream()
                                .filter(node -> List.of(
                                        "requirements",
                                        "architecture",
                                        "test-plan",
                                        "sync",
                                        "implementation"
                                ).contains(node.id()))
                                .toList()
                );

        Workflow workflow =
                new WorkflowPlanMapper().toWorkflow(
                        "analytics-greenfield-implementation",
                        "Analytics Greenfield Implementation",
                        implementationPlan
                );

        AnalyticsScenarioExecutor scenarioExecutor =
                new AnalyticsScenarioExecutor(
                        workflow.getId(),
                        engineeringAgent
                );

        ApprovalHandlerInterface approvalHandler =
                (node, execution) ->
                        ApprovalResult.builder()
                                .approved(true)
                                .approver("TEST_REVIEWER")
                                .reason("Test approval")
                                .approvalTimeMs(0)
                                .build();

        WorkflowExecutor executor =
                new WorkflowExecutor(workflow);

        WorkflowExecutor.ExecutionResult result =
                executor.execute(
                        scenarioExecutor,
                        approvalHandler
                );

        assertThat(result.isSuccess()).isTrue();

        assertThat(
                workflow.getCurrentState().getCompletedNodeIds()
        ).containsExactlyInAnyOrder(
                "requirements",
                "architecture",
                "test-plan",
                "sync",
                "implementation"
        );

        assertThat(
                workflow.getCurrentState()
                        .getArtifactIdsProducedByNode("implementation")
        ).hasSize(2);

        assertThat(
                workflow.getCurrentState()
                        .getArtifactIdsProducedByNode("implementation")
                        .stream()
                        .map(id ->
                                workflow.getCurrentState()
                                        .getArtifactById(id)
                                        .type()
                        )
        ).allMatch(type -> type == ArtifactType.CODE);

        assertThat(workspaceTool.exists(
                "src/main/java/com/cs/urlshortenerorchestrator/analytics/domain/ClickEvent.java"
        )).isTrue();

        assertThat(workspaceTool.exists(
                "src/main/java/com/cs/urlshortenerorchestrator/analytics/service/EventStore.java"
        )).isTrue();

        assertThat(executor.getAuditTrail())
                .anySatisfy(entry ->
                        assertThat(entry.getEventType())
                                .isEqualTo("ARTIFACT_PRODUCED")
                );
    }
}