package com.cs.urlshortenerorchestrator.engine;

import com.cs.urlshortenerorchestrator.engine.domain.*;
import com.cs.urlshortenerorchestrator.engine.execution.NodeExecutor;
import com.cs.urlshortenerorchestrator.engine.execution.WorkflowExecutor;
import com.cs.urlshortenerorchestrator.engine.execution.ApprovalHandlerInterface;
import com.cs.urlshortenerorchestrator.engine.execution.ApprovalResult;
import com.cs.urlshortenerorchestrator.engine.execution.DecisionRecorder;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;

import java.time.Instant;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;


import static org.assertj.core.api.Assertions.*;

@DisplayName("SDLC Greenfield Workflow Integration Test")
class SDLCWorkflowIntegrationTest {

    @Test
    @DisplayName("full SDLC pipeline executes successfully with approval and artifact flow")
    void testFullSDLCWorkflow() {
        // REQUIREMENT_ANALYSIS → ARCHITECTURE_DESIGN → (IMPLEMENTATION || TEST_PLANNING) → SYNCHRONIZATION → VALIDATION → RELEASE_READY
        // Demonstrates: Explicit dependency graph, parallel execution, synchronization,
        // artifact flow, decision lineage, metrics, and human approval.

        // REQUIREMENT_ANALYSIS
        WorkflowNode reqNode = WorkflowNode.builder("req", NodeType.REQUIREMENT_ANALYSIS)
            .description("Gather and analyze requirements")
            .build();

        // ARCHITECTURE_DESIGN
        WorkflowNode archNode = WorkflowNode.builder("arch", NodeType.ARCHITECTURE_DESIGN)
            .description("Design system architecture")
            .dependsOn("req")
            .build();

        // Parallel: IMPLEMENTATION and TEST_PLANNING
        WorkflowNode implNode = WorkflowNode.builder("impl", NodeType.IMPLEMENTATION)
            .description("Implement feature")
            .dependsOn("arch")
            .build();

        WorkflowNode testNode = WorkflowNode.builder("test", NodeType.TEST_PLANNING)
            .description("Plan testing strategy")
            .dependsOn("arch")
            .build();

        // SYNCHRONIZATION
        WorkflowNode syncNode = WorkflowNode.builder("sync", NodeType.SYNCHRONIZATION)
            .description("Synchronize implementation and testing")
            .dependsOn("impl", "test")
            .build();

        // VALIDATION
        WorkflowNode validationNode = WorkflowNode.builder("validation", NodeType.VALIDATION)
            .description("Validate implementation")
            .dependsOn("sync")
            .build();

        // RELEASE_READY with Approval
        WorkflowNode releaseNode = WorkflowNode.builder("release", NodeType.RELEASE_READY)
            .description("Prepare for release")
            .dependsOn("validation")
            .approvalGate(
                ApprovalGate.builder("release_approval")
                    .required(true)
                    .approver("RELEASE_MANAGER")
                    .description("Final approval before release")
                    .build()
            )
            .build();

        Workflow workflow = Workflow.builder("sdlc-acceptance", "SDLC Acceptance Pipeline")
            .root(reqNode).node(archNode).node(implNode).node(testNode)
            .node(syncNode).node(validationNode).node(releaseNode)
            .build();

        Set<String> executingNodes = ConcurrentHashMap.newKeySet();
        AtomicInteger maxConcurrency = new AtomicInteger(0);

        NodeExecutor acceptanceExecutor = new NodeExecutor() {
            @Override
            public Execution execute(WorkflowNode node, int attemptNumber) {
                executingNodes.add(node.getId());
                maxConcurrency.updateAndGet(max -> Math.max(max, executingNodes.size()));

                try {
                    Thread.sleep(50);
                    return Execution.builder().id("exec-" + node.getId() + "-" + attemptNumber)
                        .status(ExecutionStatus.SUCCESS).startedAt(Instant.now()).endedAt(Instant.now()).build();
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    return Execution.builder().status(ExecutionStatus.FAILED).build();
                } finally {
                    executingNodes.remove(node.getId());
                }
            }
        };

        ApprovalHandlerInterface approvalHandler = (node, execution) -> {
            return ApprovalResult.builder()
                .approved(true)
                .approver("RELEASE_MANAGER")
                .reason("All validation criteria met")
                .build();
        };

        WorkflowExecutor workflowExecutor = new WorkflowExecutor(workflow);
        workflow.getCurrentState().recordArtifact(new Artifact("arch-doc", ArtifactType.ARCHITECTURE_PLAN, "arch.md", "arch", "exec-arch-1", "/docs/arch.md", Map.of(), Instant.now()));

        try {
            workflowExecutor.execute(acceptanceExecutor, approvalHandler);
        } catch (InterruptedException e) {
            fail("Interrupted");
        }

        assertThat(workflow.getCurrentState().getPhase()).isEqualTo(WorkflowState.ExecutionPhase.COMPLETED);
        assertThat(maxConcurrency.get()).isGreaterThan(1);
        assertThat(workflowExecutor.getMetrics().getCompletedNodes()).isEqualTo(7);
        assertThat(workflowExecutor.getDecisionRecorder().getDecisions())
            .anySatisfy(d -> {
                assertThat(d.type()).isEqualTo(DecisionType.APPROVAL_DECISION);
                assertThat(d.madeByNodeId()).isEqualTo("release");
            });
        assertThat(workflow.getCurrentState().getArtifactById("arch-doc")).isNotNull();
    }


    @Test
    @DisplayName("workflow retries failed node and succeeds")
    void testRetryScenario() throws InterruptedException {
        WorkflowNode reqNode = WorkflowNode.builder("req", NodeType.REQUIREMENT_ANALYSIS)
                .retryPolicy(
                        RetryPolicy.builder()
                                .maxRetries(2)
                                .backoff(BackoffStrategy.FIXED)
                                .initialDelay(0)
                                .maxDelay(0)
                                .maxDuration(10)
                                .build()
                )
                .build();

        Workflow workflow = Workflow.builder("retry-test", "Retry Test Workflow")
                .root(reqNode)
                .build();

        AtomicInteger attempts = new AtomicInteger(0);

        NodeExecutor retryingExecutor = (node, attemptNumber) -> {
            int attempt = attempts.incrementAndGet();

            if (attempt == 1) {
                return Execution.builder()
                        .id("exec-req-1")
                        .workflowId(workflow.getId())
                        .nodeId(node.getId())
                        .attemptNumber(attemptNumber)
                        .status(ExecutionStatus.FAILED)
                        .errorDetails("TransientException")
                        .startedAt(Instant.now())
                        .endedAt(Instant.now())
                        .build();
            }

            return Execution.builder()
                    .id("exec-req-2")
                    .workflowId(workflow.getId())
                    .nodeId(node.getId())
                    .attemptNumber(attemptNumber)
                    .status(ExecutionStatus.SUCCESS)
                    .startedAt(Instant.now())
                    .endedAt(Instant.now())
                    .build();
        };

        ApprovalHandlerInterface approvalHandler = (node, execution) ->
                ApprovalResult.builder()
                        .approved(true)
                        .approver("TEST")
                        .reason("Test approval")
                        .approvalTimeMs(0)
                        .build();

        WorkflowExecutor executor = new WorkflowExecutor(workflow);

        WorkflowExecutor.ExecutionResult result =
                executor.execute(retryingExecutor, approvalHandler);

        assertThat(result.isSuccess()).isTrue();
        assertThat(attempts.get()).isEqualTo(2);

        assertThat(workflow.getCurrentState().getCompletedNodeIds())
                .containsExactly("req");

        assertThat(executor.getDecisionRecorder().getDecisions())
                .anySatisfy(decision ->
                        assertThat(decision.type())
                                .isEqualTo(DecisionType.RETRY_ON_FAILURE)
                );
    }

    @Test
    @DisplayName("workflow stops when required approval is rejected")
    void testApprovalRejection() throws InterruptedException {
        WorkflowNode reqNode = WorkflowNode.builder("req", NodeType.REQUIREMENT_ANALYSIS)
                .build();

        WorkflowNode releaseNode = WorkflowNode.builder("release", NodeType.RELEASE_READY)
                .dependsOn("req")
                .approvalGate(
                        ApprovalGate.builder("release_approval")
                                .required(true)
                                .approver("RELEASE_MANAGER")
                                .description("Final approval before release")
                                .build()
                )
                .build();

        Workflow workflow = Workflow.builder("approval-test", "Approval Rejection Test")
                .root(reqNode)
                .node(releaseNode)
                .build();

        NodeExecutor nodeExecutor = (node, attemptNumber) ->
                Execution.builder()
                        .id("exec-" + node.getId())
                        .workflowId(workflow.getId())
                        .nodeId(node.getId())
                        .attemptNumber(attemptNumber)
                        .status(ExecutionStatus.SUCCESS)
                        .startedAt(Instant.now())
                        .endedAt(Instant.now())
                        .build();

        ApprovalHandlerInterface rejectingApprovalHandler = (node, execution) ->
                ApprovalResult.builder()
                        .approved(false)
                        .approver("TEST_APPROVER")
                        .reason("Approval intentionally rejected")
                        .approvalTimeMs(0)
                        .build();

        WorkflowExecutor executor = new WorkflowExecutor(workflow);

        WorkflowExecutor.ExecutionResult result =
                executor.execute(nodeExecutor, rejectingApprovalHandler);

        assertThat(result).isNotNull();
        assertThat(result.isSuccess()).isFalse();

        // req should have completed, release should not.
        assertThat(workflow.getCurrentState().getCompletedNodeIds())
                .contains("req")
                .doesNotContain("release");

        // Verify approval rejection was recorded.
        assertThat(executor.getDecisionRecorder().getDecisions())
                .anySatisfy(decision -> {
                    assertThat(decision.type())
                            .isEqualTo(DecisionType.APPROVAL_DECISION);
                    assertThat(decision.outcome())
                            .contains("REJECTED");
                });
    }

    @Test
    @DisplayName("approval timeout puts workflow into waiting state")
    void testApprovalTimeout() throws InterruptedException {
        // Simple workflow: REQUIREMENT_ANALYSIS -> RELEASE_READY
        WorkflowNode reqNode = WorkflowNode.builder(
                        "req-timeout", NodeType.REQUIREMENT_ANALYSIS)
                .description("Requirements")
                .build();

        WorkflowNode releaseNode = WorkflowNode.builder(
                        "release-timeout", NodeType.RELEASE_READY)
                .description("Release requiring approval")
                .dependsOn("req-timeout")
                .approvalGate(
                        ApprovalGate.builder("release-approval")
                                .required(true)
                                .approver("RELEASE_MANAGER")
                                .description("Final release approval")
                                .build()
                )
                .build();

        Workflow workflow = Workflow.builder(
                        "approval-timeout-workflow",
                        "Approval Timeout Test")
                .description("Tests approval timeout behavior")
                .root(reqNode)
                .node(releaseNode)
                .build();

        // Deterministic executor: every node execution succeeds.
        NodeExecutor deterministicExecutor = new NodeExecutor() {
            @Override
            public Execution execute(WorkflowNode node, int attemptNumber) {
                return Execution.builder()
                        .id("exec-" + node.getId() + "-" + attemptNumber)
                        .workflowId(workflow.getId())
                        .nodeId(node.getId())
                        .attemptNumber(attemptNumber)
                        .status(ExecutionStatus.SUCCESS)
                        .startedAt(Instant.now().minusSeconds(1))
                        .endedAt(Instant.now())
                        .build();
            }
        };

        // Approval handler deliberately times out.
        ApprovalHandlerInterface timeoutHandler =
                new ApprovalHandlerInterface() {
                    @Override
                    public ApprovalResult requestApproval(
                            WorkflowNode node,
                            Execution execution)
                            throws WorkflowExecutor.ApprovalTimeoutException,
                            InterruptedException {

                        throw new WorkflowExecutor.ApprovalTimeoutException(
                                "Approval timed out");
                    }
                };

        WorkflowExecutor executor = new WorkflowExecutor(workflow);

        WorkflowExecutor.ExecutionResult result =
                executor.execute(deterministicExecutor, timeoutHandler);

        // Workflow should NOT be considered successful.
        assertThat(result).isNotNull();
        assertThat(result.isSuccess()).isFalse();

        // Executor should return a waiting result.
        assertThat(result.getMessage())
                .contains("Waiting for approval");

        // Workflow should be waiting for approval.
        assertThat(workflow.getCurrentState().getPhase())
                .isEqualTo(WorkflowState.ExecutionPhase.WAITING_FOR_APPROVAL);

        // The release node must NOT be marked completed.
        assertThat(workflow.getCurrentState().getCompletedNodeIds())
                .doesNotContain("release-timeout");

        // The release node must NOT be marked failed either.
        assertThat(workflow.getCurrentState().getFailedNodeIds())
                .doesNotContain("release-timeout");

        // Verify the timeout was recorded in the audit trail.
        assertThat(executor.getAuditTrail())
                .anySatisfy(entry -> {
                    assertThat(entry.getEventType())
                            .isEqualTo("APPROVAL_TIMEOUT");
                    assertThat(entry.getEntityId())
                            .isEqualTo("release-timeout");
                });
    }

    @Test
    @DisplayName("failed node retries and succeeds")
    void testRetryThenSuccess() throws InterruptedException {
        WorkflowNode node = WorkflowNode.builder(
                        "retry-node", NodeType.IMPLEMENTATION)
                .description("Node that fails once then succeeds")
                .retryPolicy(
                        RetryPolicy.builder()
                                .maxRetries(3)
                                .backoff(BackoffStrategy.FIXED)
                                .initialDelay(0)
                                .maxDelay(0)
                                .build()
                )
                .build();

        Workflow workflow = Workflow.builder(
                        "retry-workflow",
                        "Retry Test Workflow")
                .description("Tests retry behavior")
                .root(node)
                .build();

        // Fail once, then succeed.
        AtomicInteger attempts = new AtomicInteger();

        NodeExecutor retryExecutor = new NodeExecutor() {
            @Override
            public Execution execute(WorkflowNode node, int attemptNumber) {
                int attempt = attempts.incrementAndGet();

                if (attempt == 1) {
                    return Execution.builder()
                            .id("exec-retry-1")
                            .workflowId(workflow.getId())
                            .nodeId(node.getId())
                            .attemptNumber(attempt)
                            .status(ExecutionStatus.FAILED)
                            .errorDetails("TransientException: temporary failure")
                            .build();
                }

                return Execution.builder()
                        .id("exec-retry-" + attempt)
                        .workflowId(workflow.getId())
                        .nodeId(node.getId())
                        .attemptNumber(attempt)
                        .status(ExecutionStatus.SUCCESS)
                        .startedAt(Instant.now().minusSeconds(1))
                        .endedAt(Instant.now())
                        .build();
            }
        };

        WorkflowExecutor executor = new WorkflowExecutor(workflow);

        ApprovalHandlerInterface approvalHandler =
                new ApprovalHandlerInterface() {
                    @Override
                    public ApprovalResult requestApproval(
                            WorkflowNode node,
                            Execution execution)
                            throws WorkflowExecutor.ApprovalTimeoutException,
                            InterruptedException {
                        return ApprovalResult.builder()
                                .approved(true)
                                .approver("TEST")
                                .reason("Auto-approved")
                                .approvalTimeMs(0)
                                .build();
                    }
                };

        WorkflowExecutor.ExecutionResult result =
                executor.execute(retryExecutor, approvalHandler);

        // Overall workflow succeeded.
        assertThat(result).isNotNull();
        assertThat(result.isSuccess()).isTrue();

        // Exactly two attempts: failure + success.
        assertThat(attempts.get()).isEqualTo(2);

        // Node ultimately completed.
        assertThat(workflow.getCurrentState().getCompletedNodeIds())
                .containsExactly("retry-node");

        assertThat(workflow.getCurrentState().getFailedNodeIds())
                .isEmpty();

        // Retry metric recorded.
        assertThat(executor.getMetrics().getRetriedNodes())
                .isEqualTo(1);

        // Retry should be visible in audit trail.
        assertThat(executor.getAuditTrail())
                .anySatisfy(entry ->
                        assertThat(entry.getEventType())
                                .isEqualTo("RETRY_SUCCESS"));
    }

    @Test
    @DisplayName("node fails after exhausting retries")
    void testRetryExhausted() throws InterruptedException {
        WorkflowNode node = WorkflowNode.builder(
                        "failing-node", NodeType.IMPLEMENTATION)
                .description("Node that always fails")
                .retryPolicy(
                        RetryPolicy.builder()
                                .maxRetries(3)
                                .backoff(BackoffStrategy.FIXED)
                                .initialDelay(0)
                                .maxDelay(0)
                                .build()
                )
                .build();

        Workflow workflow = Workflow.builder(
                        "retry-exhausted-workflow",
                        "Retry Exhaustion Test")
                .description("Tests exhausted retry behavior")
                .root(node)
                .build();

        AtomicInteger attempts = new AtomicInteger();

        NodeExecutor failingExecutor = new NodeExecutor() {
            @Override
            public Execution execute(WorkflowNode node, int attemptNumber) {
                int attempt = attempts.incrementAndGet();

                return Execution.builder()
                        .id("exec-failing-" + attempt)
                        .workflowId(workflow.getId())
                        .nodeId(node.getId())
                        .attemptNumber(attempt)
                        .status(ExecutionStatus.FAILED)
                        .errorDetails("TransientException: service unavailable")
                        .build();
            }
        };

        WorkflowExecutor executor = new WorkflowExecutor(workflow);

        ApprovalHandlerInterface approvalHandler =
                new ApprovalHandlerInterface() {
                    @Override
                    public ApprovalResult requestApproval(
                            WorkflowNode node,
                            Execution execution)
                            throws WorkflowExecutor.ApprovalTimeoutException,
                            InterruptedException {
                        return ApprovalResult.builder()
                                .approved(true)
                                .approver("TEST")
                                .reason("Auto-approved")
                                .approvalTimeMs(0)
                                .build();
                    }
                };

        WorkflowExecutor.ExecutionResult result =
                executor.execute(failingExecutor, approvalHandler);

        // Overall workflow should fail.
        assertThat(result).isNotNull();
        assertThat(result.isSuccess()).isFalse();

        // Exactly 3 attempts.
        assertThat(attempts.get()).isEqualTo(3);

        // Node should be marked failed.
        assertThat(workflow.getCurrentState().getFailedNodeIds())
                .contains("failing-node");

        // Node should NOT be completed.
        assertThat(workflow.getCurrentState().getCompletedNodeIds())
                .doesNotContain("failing-node");

        // Failure metric should be recorded.
        assertThat(executor.getMetrics().getFailedNodes())
                .isEqualTo(1);

        // Workflow should eventually be marked failed.
        assertThat(workflow.getCurrentState().getPhase())
                .isEqualTo(WorkflowState.ExecutionPhase.FAILED);
    }

    @Test
    @DisplayName("entry gate failure blocks node and fails workflow")
    void testEntryGateFailure() throws InterruptedException {
        ValidationRule failingRule = new ValidationRule(
                "requirements-check",
                ValidationRule.RuleType.CUSTOM,
                null,
                context -> false,
                ValidationRule.Severity.ERROR,
                "Required validation failed"
        );

        Gate entryGate = Gate.builder("requirements-gate")
                .validation()
                .validationRules(List.of(failingRule))
                .failureAction(Gate.FailureAction.BLOCK)
                .description("Requirements must be valid")
                .build();

        WorkflowNode node = WorkflowNode.builder(
                        "gated-node", NodeType.IMPLEMENTATION)
                .description("Node protected by entry gate")
                .entryGate(entryGate)
                .build();

        Workflow workflow = Workflow.builder(
                        "gate-failure-workflow",
                        "Gate Failure Test")
                .description("Tests entry gate failure")
                .root(node)
                .build();

        AtomicInteger executions = new AtomicInteger();

        NodeExecutor nodeExecutor = new NodeExecutor() {
            @Override
            public Execution execute(WorkflowNode node, int attemptNumber) {
                executions.incrementAndGet();

                return Execution.builder()
                        .id("exec-gated-node")
                        .workflowId(workflow.getId())
                        .nodeId(node.getId())
                        .attemptNumber(attemptNumber)
                        .status(ExecutionStatus.SUCCESS)
                        .startedAt(Instant.now().minusSeconds(1))
                        .endedAt(Instant.now())
                        .build();
            }
        };

        WorkflowExecutor executor = new WorkflowExecutor(workflow);

        ApprovalHandlerInterface approvalHandler =
                new ApprovalHandlerInterface() {
                    @Override
                    public ApprovalResult requestApproval(
                            WorkflowNode node,
                            Execution execution)
                            throws WorkflowExecutor.ApprovalTimeoutException,
                            InterruptedException {
                        return ApprovalResult.builder()
                                .approved(true)
                                .approver("TEST")
                                .reason("Auto-approved")
                                .approvalTimeMs(0)
                                .build();
                    }
                };

        WorkflowExecutor.ExecutionResult result =
                executor.execute(nodeExecutor, approvalHandler);

        // Workflow should fail.
        assertThat(result).isNotNull();
        assertThat(result.isSuccess()).isFalse();

        // The node must be marked failed.
        assertThat(workflow.getCurrentState().getFailedNodeIds())
                .contains("gated-node");

        // The node must never have been executed.
        assertThat(executions.get()).isZero();

        // It must not be marked completed.
        assertThat(workflow.getCurrentState().getCompletedNodeIds())
                .doesNotContain("gated-node");

        // Workflow should be in FAILED state.
        assertThat(workflow.getCurrentState().getPhase())
                .isEqualTo(WorkflowState.ExecutionPhase.FAILED);

        // Audit trail should show gate failure.
        assertThat(executor.getAuditTrail())
                .anySatisfy(entry -> {
                    assertThat(entry.getEventType())
                            .isEqualTo("GATE_FAILURE");
                    assertThat(entry.getEntityId())
                            .isEqualTo("gated-node");
                });
    }

    @Test
    @DisplayName("independent nodes execute in parallel and synchronization waits for both")
    void testParallelExecution() throws InterruptedException {
        WorkflowNode req = WorkflowNode.builder(
                        "req-parallel", NodeType.REQUIREMENT_ANALYSIS)
                .build();

        WorkflowNode arch = WorkflowNode.builder(
                        "arch-parallel", NodeType.ARCHITECTURE_DESIGN)
                .dependsOn("req-parallel")
                .build();

        WorkflowNode impl = WorkflowNode.builder(
                        "impl-parallel", NodeType.IMPLEMENTATION)
                .dependsOn("arch-parallel")
                .build();

        WorkflowNode test = WorkflowNode.builder(
                        "test-parallel", NodeType.TEST_PLANNING)
                .dependsOn("arch-parallel")
                .build();

        WorkflowNode sync = WorkflowNode.builder(
                        "sync-parallel", NodeType.SYNCHRONIZATION)
                .dependsOn("impl-parallel", "test-parallel")
                .build();

        Workflow workflow = Workflow.builder(
                        "parallel-workflow",
                        "Parallel Execution Test")
                .root(req)
                .node(arch)
                .node(impl)
                .node(test)
                .node(sync)
                .build();

        CountDownLatch implStarted = new CountDownLatch(1);
        CountDownLatch testStarted = new CountDownLatch(1);

        AtomicBoolean implSawTestRunning = new AtomicBoolean(false);
        AtomicBoolean testSawImplRunning = new AtomicBoolean(false);

        AtomicBoolean implFinished = new AtomicBoolean(false);
        AtomicBoolean testFinished = new AtomicBoolean(false);

        NodeExecutor nodeExecutor = new NodeExecutor() {
            @Override
            public Execution execute(WorkflowNode node, int attemptNumber) {
                String id = node.getId();

                if (id.equals("impl-parallel")) {
                    implStarted.countDown();

                    try {
                        if (testStarted.await(2, TimeUnit.SECONDS)) {
                            implSawTestRunning.set(true);
                        }

                        Thread.sleep(100);
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                    }

                    implFinished.set(true);

                } else if (id.equals("test-parallel")) {
                    testStarted.countDown();

                    try {
                        if (implStarted.await(2, TimeUnit.SECONDS)) {
                            testSawImplRunning.set(true);
                        }

                        Thread.sleep(100);
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                    }

                    testFinished.set(true);

                } else {
                    try {
                        Thread.sleep(20);
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                    }
                }

                return Execution.builder()
                        .id("exec-" + id)
                        .workflowId(workflow.getId())
                        .nodeId(id)
                        .attemptNumber(attemptNumber)
                        .status(ExecutionStatus.SUCCESS)
                        .startedAt(Instant.now().minusMillis(10))
                        .endedAt(Instant.now())
                        .build();
            }
        };

        WorkflowExecutor executor = new WorkflowExecutor(workflow);

        ApprovalHandlerInterface approvalHandler =
                new ApprovalHandlerInterface() {
                    @Override
                    public ApprovalResult requestApproval(
                            WorkflowNode node,
                            Execution execution)
                            throws WorkflowExecutor.ApprovalTimeoutException,
                            InterruptedException {
                        return ApprovalResult.builder()
                                .approved(true)
                                .approver("TEST")
                                .reason("Auto-approved")
                                .approvalTimeMs(0)
                                .build();
                    }
                };

        WorkflowExecutor.ExecutionResult result =
                executor.execute(nodeExecutor, approvalHandler);

        assertThat(result.isSuccess()).isTrue();

        // Both parallel branches must have started while the other was running.
        assertThat(implSawTestRunning.get()).isTrue();
        assertThat(testSawImplRunning.get()).isTrue();

        // Both branches completed.
        assertThat(implFinished.get()).isTrue();
        assertThat(testFinished.get()).isTrue();

        // Synchronization node could only execute after both dependencies completed.
        assertThat(workflow.getCurrentState().getCompletedNodeIds())
                .containsExactlyInAnyOrder(
                        "req-parallel",
                        "arch-parallel",
                        "impl-parallel",
                        "test-parallel",
                        "sync-parallel"
                );
    }

    @Test
    @DisplayName("exit gate failure triggers replan and restarts from specified node")
    void testExitGateReplan() throws InterruptedException {
        WorkflowNode startNode = WorkflowNode.builder("start", NodeType.REQUIREMENT_ANALYSIS)
                .description("Start node")
                .build();

        ReplanTrigger trigger = ReplanTrigger.builder("replan-trigger",
                        ReplanTrigger.TriggerType.VALIDATION_FAILED, "start")
                .maxReplans(1)
                .reason("Validation failed, need to re-analyze requirements")
                .build();

        ValidationRule failingRule = new ValidationRule("fail", ValidationRule.RuleType.CUSTOM, null,
                context -> false, ValidationRule.Severity.ERROR, "Force fail");

        Gate exitGate = Gate.builder("fail-gate")
                .validation()
                .validationRules(List.of(failingRule))
                .failureAction(Gate.FailureAction.TRIGGER_REPLAN)
                .build();

        WorkflowNode failingNode = WorkflowNode.builder("failing-node", NodeType.IMPLEMENTATION)
                .dependsOn("start")
                .exitGate(exitGate)
                .replanTrigger(trigger)
                .build();

        Workflow workflow = Workflow.builder("replan-wf", "Replan Test")
                .root(startNode)
                .node(failingNode)
                .build();

        AtomicInteger startExecutions = new AtomicInteger();
        AtomicInteger failingExecutions = new AtomicInteger();

        NodeExecutor executor = (node, attempt) -> {
            if (node.getId().equals("start")) {
                startExecutions.incrementAndGet();
            } else if (node.getId().equals("failing-node")) {
                failingExecutions.incrementAndGet();
            }
            return Execution.builder()
                    .id("exec-" + node.getId() + "-" + attempt)
                    .status(ExecutionStatus.SUCCESS)
                    .startedAt(Instant.now())
                    .endedAt(Instant.now())
                    .build();
        };

        WorkflowExecutor workflowExecutor = new WorkflowExecutor(workflow);
        workflowExecutor.execute(executor, (node, exec) -> ApprovalResult.builder().approved(true).build());

        // Initial: start (1), failing (1) -> replan triggered
        // Replan 1: start (2), failing (2) -> replan limit reached (max 1) -> Failure
        assertThat(startExecutions.get()).isEqualTo(2);
        assertThat(failingExecutions.get()).isEqualTo(2);
        assertThat(workflow.getCurrentState().getPhase()).isEqualTo(WorkflowState.ExecutionPhase.FAILED);
        assertThat(workflowExecutor.getMetrics().getReplannedCount()).isEqualTo(1);
    }

    @Test
    @DisplayName("reversible approval rejection invokes executor rollback and transitions to ROLLED_BACK")
    void testReversibleApprovalRollback() throws InterruptedException {
        List<String> rollbackOps = List.of("DELETE_CLOUDFORMATION_STACK", "PURGE_S3_BUCKET");
        RollbackPolicy rollbackPolicy = RollbackPolicy.builder()
                .reversible(true)
                .operations(rollbackOps)
                .build();

        ApprovalGate approvalGate = ApprovalGate.builder("gate")
                .required(true)
                .build();

        WorkflowNode node = WorkflowNode.builder("deploy", NodeType.IMPLEMENTATION)
                .rollbackPolicy(rollbackPolicy)
                .approvalGate(approvalGate)
                .build();

        Workflow workflow = Workflow.builder("rollback-wf", "Rollback Test")
                .root(node)
                .build();

        AtomicBoolean rollbackInvoked = new AtomicBoolean(false);
        List<String> capturedOps = new ArrayList<>();

        NodeExecutor executor = new NodeExecutor() {
            @Override
            public Execution execute(WorkflowNode node, int attemptNumber) {
                return Execution.builder()
                        .id("exec-1")
                        .status(ExecutionStatus.SUCCESS)
                        .startedAt(Instant.now())
                        .endedAt(Instant.now())
                        .build();
            }

            @Override
            public void rollback(WorkflowNode node, List<String> operations) {
                rollbackInvoked.set(true);
                capturedOps.addAll(operations);
            }
        };

        ApprovalHandlerInterface rejectionHandler = (n, e) -> ApprovalResult.builder()
                .approved(false)
                .reason("Security violation")
                .build();

        WorkflowExecutor workflowExecutor = new WorkflowExecutor(workflow);
        workflowExecutor.execute(executor, rejectionHandler);

        assertThat(workflow.getCurrentState().getPhase()).isEqualTo(WorkflowState.ExecutionPhase.ROLLED_BACK);
        assertThat(rollbackInvoked.get()).isTrue();
        assertThat(capturedOps).containsExactlyElementsOf(rollbackOps);
        assertThat(workflowExecutor.getMetrics().getRolledBackNodes()).isEqualTo(1);
    }
}
