package com.cs.urlshortenerorchestrator.analytics.orchestration;

import com.cs.urlshortenerorchestrator.engine.agent.*;
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
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;

class AnalyticsValidationWorkflowIntegrationTest {

    @TempDir
    Path projectRoot;

    @Test
    void validationFailureReplansFromImplementationUntilLimitReached()
            throws InterruptedException {

        AtomicInteger implementationCalls =
                new AtomicInteger();

        AtomicInteger validationCalls =
                new AtomicInteger();

        AgentClient fakeClient =
                (systemPrompt, userPrompt) -> {

                    /*
                     * Validation always reports the same missing criterion.
                     *
                     * This deliberately forces the workflow to exercise
                     * bounded replanning until maxReplans is reached.
                     */
                    if (systemPrompt.contains(
                            "engineering validation agent")) {

                        validationCalls.incrementAndGet();

                        return new AgentResponse(
                                """
                                {
                                  "status": "FAIL",
                                  "criteriaResults": {
                                    "event ingestion":
                                      "PASS - implementation accepts events",
                                    "aggregation":
                                      "PASS - click counts are aggregated",
                                    "dashboard":
                                      "PASS - summary endpoint exists",
                                    "traffic spike detection":
                                      "FAIL - no implementation evidence"
                                  },
                                  "gaps": [
                                    "Traffic spike detection is not implemented"
                                  ],
                                  "summary":
                                    "Analytics implementation is incomplete."
                                }
                                """,
                                "test-model",
                                100,
                                100
                        );
                    }

                    if (systemPrompt.contains(
                            "testing agent")) {

                        return new AgentResponse(
                                """
                                {
                                  "files": [
                                    {
                                      "path":
                                        "src/test/java/com/cs/urlshortenerorchestrator/analytics/EventStoreTest.java",
                                      "content":
                                        "package com.cs.urlshortenerorchestrator.analytics; import org.junit.jupiter.api.Test; class EventStoreTest { @Test void storesEvents() {} }"
                                    }
                                  ],
                                  "summary":
                                    "Generated Analytics tests."
                                }
                                """,
                                "test-model",
                                100,
                                100
                        );
                    }

                    /*
                     * Every implementation execution is counted.
                     *
                     * Initial execution = 1
                     * replan #1          = 2
                     * replan #2          = 3
                     */
                    implementationCalls.incrementAndGet();

                    return new AgentResponse(
                            """
                            {
                              "files": [
                                {
                                  "path":
                                    "src/main/java/com/cs/urlshortenerorchestrator/analytics/Event.java",
                                  "content":
                                    "package com.cs.urlshortenerorchestrator.analytics; public class Event {}"
                                },
                                {
                                  "path":
                                    "src/main/java/com/cs/urlshortenerorchestrator/analytics/EventStore.java",
                                  "content":
                                    "package com.cs.urlshortenerorchestrator.analytics; public class EventStore {}"
                                }
                              ],
                              "summary":
                                "Generated Analytics implementation."
                            }
                            """,
                            "test-model",
                            100,
                            100
                    );
                };

        BoundedWorkspaceTool workspace =
                new BoundedWorkspaceTool(
                        projectRoot
                );

        EngineeringAgent engineeringAgent =
                new EngineeringAgent(
                        fakeClient,
                        workspace,
                        new ObjectMapper()
                );

        /*
         * Generated tests always execute successfully.
         *
         * This is intentional:
         * test success must not override an unmet acceptance criterion.
         */
        TestExecutionTool testExecutionTool =
                (nodeId, request) ->
                        new ValidationResult(
                                "test-result-"
                                        + System.currentTimeMillis(),
                                nodeId,
                                request.testTargets().size(),
                                request.testTargets().size(),
                                0,
                                List.of(),
                                ValidationStatus.PASS,
                                null,
                                Instant.now()
                        );

        WorkflowPlan fullPlan =
                new AnalyticsPlanningAgent()
                        .plan(
                                "Build a greenfield Analytics capability"
                        );

        WorkflowPlan validationPlan =
                new WorkflowPlan(
                        fullPlan.normalizedRequirement(),
                        fullPlan.assumptions(),
                        fullPlan.acceptanceCriteria(),
                        fullPlan.nodes()
                                .stream()
                                .filter(node ->
                                        List.of(
                                                "requirements",
                                                "architecture",
                                                "test-plan",
                                                "sync",
                                                "implementation",
                                                "testing",
                                                "validation"
                                        ).contains(
                                                node.id()
                                        )
                                )
                                .toList()
                );

        Workflow workflow =
                new WorkflowPlanMapper()
                        .toWorkflow(
                                "analytics-greenfield-validation",
                                "Analytics Greenfield Validation",
                                validationPlan
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
                new WorkflowExecutor(
                        workflow
                );

        WorkflowExecutor.ExecutionResult result =
                executor.execute(
                        scenarioExecutor,
                        approvalHandler
                );

        /*
         * Validation never becomes PASS, so after two allowed replans
         * the workflow must terminate rather than loop forever.
         */
        assertThat(result.isSuccess())
                .isFalse();

        /*
         * Initial implementation + two replan executions.
         */
        assertThat(implementationCalls.get())
                .isEqualTo(3);

        /*
         * Validation also executes once per implementation cycle.
         */
        assertThat(validationCalls.get())
                .isEqualTo(3);

        /*
         * Validation evidence survives each cycle and contains the
         * acceptance gap that caused replanning.
         */
        var validationArtifact =
                workflow.getCurrentState()
                        .getArtifactById(
                                "analytics-validation-v1"
                        );

        assertThat(validationArtifact)
                .isNotNull();

        assertThat(
                validationArtifact.metadata()
                        .get("status")
        ).isEqualTo("FAIL");

        assertThat(
                validationArtifact.metadata()
                        .get("gaps")
        ).contains(
                "Traffic spike detection is not implemented"
        );

        /*
         * The gate/replan path should have executed twice.
         */
        assertThat(
                executor.getAuditTrail()
                        .stream()
                        .filter(entry ->
                                "REPLAN_EXECUTED"
                                        .equals(
                                                entry.getEventType()
                                        )
                        )
                        .count()
        ).isEqualTo(2);

        /*
         * Third validation failure exceeds the bounded replan policy.
         */
        assertThat(
                executor.getAuditTrail()
                        .stream()
                        .anyMatch(entry ->
                                "REPLAN_EXHAUSTED"
                                        .equals(
                                                entry.getEventType()
                                        )
                        )
        ).isTrue();
    }
}