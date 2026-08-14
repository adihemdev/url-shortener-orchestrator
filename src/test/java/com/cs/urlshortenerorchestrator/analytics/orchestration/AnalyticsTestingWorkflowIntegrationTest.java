package com.cs.urlshortenerorchestrator.analytics.orchestration;

import com.cs.urlshortenerorchestrator.engine.agent.*;
import com.cs.urlshortenerorchestrator.engine.domain.ArtifactType;
import com.cs.urlshortenerorchestrator.engine.domain.ValidationResult;
import com.cs.urlshortenerorchestrator.engine.domain.ValidationStatus;
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
import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class AnalyticsTestingWorkflowIntegrationTest {

    @TempDir
    Path projectRoot;

    @Test
    void workflowDrivesImplementationAndTestingAgents()
            throws InterruptedException {

        AgentClient fakeClient = (systemPrompt, userPrompt) -> {

            if (systemPrompt.contains("testing agent")) {
                return new AgentResponse(
                        """
                        {
                          "files": [
                            {
                              "path": "src/test/java/com/cs/urlshortenerorchestrator/analytics/EventStoreTest.java",
                              "content": "package com.cs.urlshortenerorchestrator.analytics; import org.junit.jupiter.api.Test; class EventStoreTest { @Test void storesEvents() {} }"
                            }
                          ],
                          "summary": "Generated Analytics tests."
                        }
                        """,
                        "test-model",
                        100,
                        100
                );
            }

            return new AgentResponse(
                    """
                    {
                      "files": [
                        {
                          "path": "src/main/java/com/cs/urlshortenerorchestrator/analytics/Event.java",
                          "content": "package com.cs.urlshortenerorchestrator.analytics; public class Event {}"
                        },
                        {
                          "path": "src/main/java/com/cs/urlshortenerorchestrator/analytics/EventStore.java",
                          "content": "package com.cs.urlshortenerorchestrator.analytics; public class EventStore {}"
                        }
                      ],
                      "summary": "Generated Analytics implementation."
                    }
                    """,
                    "test-model",
                    100,
                    100
            );
        };

        BoundedWorkspaceTool workspace =
                new BoundedWorkspaceTool(projectRoot);

        EngineeringAgent engineeringAgent =
                new EngineeringAgent(
                        fakeClient,
                        workspace,
                        new ObjectMapper()
                );

        WorkflowPlan fullPlan =
                new AnalyticsPlanningAgent()
                        .plan("Build Analytics capability");

        WorkflowPlan testingPlan =
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
                                        "implementation",
                                        "testing"
                                ).contains(node.id()))
                                .toList()
                );

        Workflow workflow =
                new WorkflowPlanMapper().toWorkflow(
                        "analytics-greenfield-testing",
                        "Analytics Greenfield Testing",
                        testingPlan
                );

        TestExecutionTool testExecutionTool =
                (nodeId, request) ->
                        new ValidationResult(
                                "test-result-1",
                                nodeId,
                                request.testTargets().size(),
                                request.testTargets().size(),
                                0,
                                List.of(),
                                ValidationStatus.PASS,
                                null,
                                Instant.now()
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
                workflow.getCurrentState()
                        .getCompletedNodeIds()
        ).contains(
                "implementation",
                "testing"
        );

        assertThat(
                workflow.getCurrentState()
                        .getArtifactIdsProducedByNode("testing")
        ).isNotEmpty();

        assertThat(
                workflow.getCurrentState()
                        .getArtifactIdsProducedByNode("testing")
                        .stream()
                        .map(id ->
                                workflow.getCurrentState()
                                        .getArtifactById(id)
                                        .type()
                        )
        ).allMatch(type -> type == ArtifactType.TEST);

        assertThat(
                workspace.exists(
                        "src/test/java/com/cs/urlshortenerorchestrator/analytics/EventStoreTest.java"
                )
        ).isTrue();

        assertThat(
                workflow.getCurrentState()
                        .getArtifactIdsProducedByNode("testing")
        ).contains("analytics-test-execution-v1");
    }
}