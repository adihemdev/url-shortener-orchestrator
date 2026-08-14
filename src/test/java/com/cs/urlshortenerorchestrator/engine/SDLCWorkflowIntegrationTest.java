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
        // TODO: Construct workflow DAG
        // REQUIREMENT_ANALYSIS → ARCHITECTURE_DESIGN → (IMPLEMENTATION || TEST_PLANNING) → SYNCHRONIZATION → VALIDATION → RELEASE_READY
        // Parallel execution of IMPLEMENTATION and TEST_PLANNING
        // All stages complete successfully

        WorkflowNode reqNode = WorkflowNode.builder("req", NodeType.REQUIREMENT_ANALYSIS)
            .description("Gather and analyze requirements")
            .build();

        WorkflowNode archNode = WorkflowNode.builder("arch", NodeType.ARCHITECTURE_DESIGN)
            .description("Design system architecture")
            .dependsOn("req")
            .build();

        WorkflowNode implNode = WorkflowNode.builder("impl", NodeType.IMPLEMENTATION)
            .description("Implement feature")
            .dependsOn("arch")
            .build();

        WorkflowNode testNode = WorkflowNode.builder("test", NodeType.TEST_PLANNING)
            .description("Plan testing strategy")
            .dependsOn("arch")
            .build();

        WorkflowNode syncNode = WorkflowNode.builder("sync", NodeType.SYNCHRONIZATION)
            .description("Synchronize implementation and testing")
            .dependsOn("impl", "test")
            .build();

        WorkflowNode validationNode = WorkflowNode.builder("validation", NodeType.VALIDATION)
            .description("Validate implementation")
            .dependsOn("sync")
            .build();

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

        Workflow workflow = Workflow.builder("sdlc", "SDLC Greenfield Pipeline")
            .description("Full software development lifecycle workflow")
            .root(reqNode)
            .node(archNode)
            .node(implNode)
            .node(testNode)
            .node(syncNode)
            .node(validationNode)
            .node(releaseNode)
            .build();

        // TODO: Create deterministic NodeExecutor
        // Each node succeeds deterministically
        // Records artifacts when appropriate
        // Simulates work but doesn't fail

        Set<String> executingNodes = ConcurrentHashMap.newKeySet();
        AtomicInteger currentConcurrency = new AtomicInteger(0);
        AtomicInteger maxConcurrency = new AtomicInteger(0);

        NodeExecutor deterministicExecutor = new NodeExecutor() {
            @Override
            public Execution execute(WorkflowNode node, int attemptNumber) {
                // Each node succeeds on first attempt
                String executionId = "exec-" + node.getId() + "-1";

                // Simulate work
                executingNodes.add(node.getId());
                int current = currentConcurrency.incrementAndGet();
                maxConcurrency.updateAndGet(max -> Math.max(max, current));

                try {
                    Thread.sleep(100);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                } finally {
                    currentConcurrency.decrementAndGet();
                    executingNodes.remove(node.getId());
                }

                return Execution.builder()
                    .id(executionId)
                    .workflowId(workflow.getId())
                    .nodeId(node.getId())
                    .attemptNumber(attemptNumber)
                    .status(ExecutionStatus.SUCCESS)
                    .startedAt(Instant.now().minusSeconds(1))
                    .endedAt(Instant.now())
                    .build();
            }
        };

        // TODO: Configure approval checkpoint
        // Release node requires approval
        // Auto-approve for testing

        ApprovalHandlerInterface approvalHandler = new ApprovalHandlerInterface() {
            @Override
            public ApprovalResult requestApproval(WorkflowNode node, Execution execution)
                    throws WorkflowExecutor.ApprovalTimeoutException, InterruptedException {
                // Auto-approve all requests for deterministic testing
                return ApprovalResult.builder()
                    .approved(true)
                    .approver("TEST_APPROVER")
                    .reason("Auto-approved for integration testing")
                    .approvalTimeMs(0)
                    .build();
            }
        };

        WorkflowExecutor executor = new WorkflowExecutor(workflow);

        // TODO: Execute the workflow
        WorkflowExecutor.ExecutionResult result = null;
        try {
            result = executor.execute(deterministicExecutor, approvalHandler);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            fail("Workflow execution interrupted");
        }

        // TODO: Assert successful completion
        assertThat(result).isNotNull();
        assertThat(result.isSuccess()).isTrue();
        assertThat(result.getMessage()).contains("successfully");

        // TODO: Assert all expected nodes completed
        WorkflowState state = workflow.getCurrentState();
        assertThat(state.getCompletedNodeIds())
            .hasSize(7)
            .containsExactlyInAnyOrder("req", "arch", "impl", "test", "sync", "validation", "release");

        assertThat(state.getFailedNodeIds()).isEmpty();

        assertThat(maxConcurrency.get())
                .as("IMPLEMENTATION and TEST_PLANNING should execute concurrently")
                .isGreaterThan(1);

        // TODO: Assert execution metrics
        ExecutionMetrics metrics = executor.getMetrics();
        assertThat(metrics.getTotalNodes()).isEqualTo(7);
        assertThat(metrics.getCompletedNodes()).isEqualTo(7);
        assertThat(metrics.getFailedNodes()).isEqualTo(0);
        assertThat(metrics.getSuccessRate()).isEqualTo(1.0);

        // TODO: Assert audit events
        List<WorkflowExecutor.AuditEntry> auditTrail = executor.getAuditTrail();
        assertThat(auditTrail).isNotEmpty();

        List<String> audittedNodes = auditTrail.stream()
            .filter(entry -> entry.getEventType().equals("NODE_COMPLETED"))
            .map(WorkflowExecutor.AuditEntry::getEntityId)
            .toList();
        assertThat(audittedNodes)
            .contains("req", "arch", "impl", "test", "sync", "validation", "release");

        // Verify approval was requested and granted
        List<String> approvalEvents = auditTrail.stream()
            .filter(entry -> entry.getEventType().equals("APPROVAL_GRANTED"))
            .map(WorkflowExecutor.AuditEntry::getEntityId)
            .toList();
        assertThat(approvalEvents).contains("release");

        // TODO: Assert approval decision lineage
        DecisionRecorder decisionRecorder = executor.getDecisionRecorder();
        List<Decision> decisions = decisionRecorder.getDecisions();
        assertThat(decisions).isNotEmpty();

        // TODO: Demonstrate artifact/cross-stage context
        // Simulate artifact production
        Artifact architectureArtifact = new Artifact(
            "arch-design-v1",
            ArtifactType.ARCHITECTURE_PLAN,
            "architecture.md",
            "arch",
            "exec-arch-1",
            "/artifacts/architecture.md",
            Map.of("version", "1.0", "components", "5"),
            Instant.now()
        );

        executor.recordArtifact(architectureArtifact);

        // Verify artifact available in workflow state
        Artifact recorded = state.getArtifactById("arch-design-v1");
        assertThat(recorded).isNotNull();
        assertThat(recorded.producedByNodeId()).isEqualTo("arch");
        assertThat(recorded.name()).isEqualTo("architecture.md");

        // Verify artifact is indexed by producing node
        List<String> archArtifactIds = state.getArtifactIdsProducedByNode("arch");
        assertThat(archArtifactIds).contains("arch-design-v1");

        // Verify artifact is accessible through audit trail
        List<String> artifactEvents = auditTrail.stream()
            .filter(entry -> entry.getEventType().equals("ARTIFACT_PRODUCED"))
            .map(WorkflowExecutor.AuditEntry::getMessage)
            .toList();
        assertThat(artifactEvents)
            .anySatisfy(msg -> assertThat(msg).contains("architecture.md"));
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


}
