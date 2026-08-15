package com.cs.urlshortenerorchestrator.engine.execution;

import com.cs.urlshortenerorchestrator.engine.domain.*;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;

import java.time.Instant;
import java.util.*;
import java.util.concurrent.*;
import java.util.stream.Collectors;

/**
 * WorkflowExecutor: Orchestrates DAG execution with dependency resolution,
 * state management, retry logic, and policy enforcement.
 *
 * Demonstrates:
 * - Explicit dependency graph traversal
 * - Pending vs Ready node distinction
 * - Sequential and parallel path execution
 * - Synchronization point coordination
 * - Decision lineage tracking
 * - Audit-grade observability
 */
@Slf4j
public class WorkflowExecutor {
    private final Workflow workflow;
    @Getter
    private final ExecutionMetrics metrics;
    @Getter
    private final List<AuditEntry> auditTrail;
    @Getter
    private final DecisionRecorder decisionRecorder;
    @Getter
    @Setter
    private int maxReplans = 3;
    private int replanCount = 0;

    public WorkflowExecutor(Workflow workflow) {
        this.workflow = Objects.requireNonNull(workflow, "workflow required");
        this.metrics = new ExecutionMetrics();
        this.auditTrail = Collections.synchronizedList(new ArrayList<>());
        this.decisionRecorder = new DecisionRecorder(workflow.getId());

        // Validate workflow DAG before execution
        List<String> validationErrors = workflow.validate();
        if (!validationErrors.isEmpty()) {
            throw new IllegalStateException("Workflow validation failed: " + validationErrors);
        }

        metrics.setTotalNodes(workflow.getNodesById().size());
        metrics.setWorkflowStartedAt(Instant.now());
        auditLog("WORKFLOW_STARTED", "Workflow initialization", workflow.getId());
    }

    /**
     * Executes the workflow to completion.
     * Manages parallel path execution, human approval gates, and failure recovery.
     */
    public ExecutionResult execute(NodeExecutor nodeExecutor, ApprovalHandlerInterface approvalHandler)
            throws InterruptedException {

        try {
            while (!isWorkflowComplete() && !isTerminalState()) {

                WorkflowState.ExecutionPhase phase =
                        workflow.getCurrentState().getPhase();

                if (phase == WorkflowState.ExecutionPhase.WAITING_FOR_APPROVAL) {
                    auditLog(
                            "WORKFLOW_WAITING",
                            "Workflow waiting for approval",
                            workflow.getId()
                    );
                    return ExecutionResult.waiting("Waiting for approval");
                }

                // Get all nodes ready for execution (dependencies satisfied)
                Set<String> readyNodes = getReadyNodes();

                if (readyNodes.isEmpty() && !isWorkflowComplete()) {
                    // Check if we're in a waiting-for-approval state
                    if (workflow.getCurrentState().getPhase() == WorkflowState.ExecutionPhase.WAITING_FOR_APPROVAL) {
                        auditLog("WORKFLOW_WAITING", "Workflow waiting for approval", workflow.getId());
                        return ExecutionResult.waiting("Waiting for approval");
                    }
                    throw new IllegalStateException("No ready nodes and workflow not complete");
                }

                // Execute ready nodes in parallel when possible
                executeReadyNodes(readyNodes, nodeExecutor, approvalHandler);
            }

            if (isTerminalState()) {
                metrics.setWorkflowEndedAt(Instant.now());

                WorkflowState.ExecutionPhase phase =
                        workflow.getCurrentState().getPhase();

                auditLog(
                        "WORKFLOW_TERMINATED",
                        "Workflow terminated in state: " + phase,
                        workflow.getId()
                );

                return ExecutionResult.failure(
                        "Workflow terminated: " + phase,
                        metrics
                );
            }

            if (!workflow.getCurrentState().getFailedNodeIds().isEmpty()) {
                workflow.getCurrentState().setPhase(
                        WorkflowState.ExecutionPhase.FAILED
                );

                metrics.setWorkflowEndedAt(Instant.now());

                auditLog(
                        "WORKFLOW_FAILED",
                        "Workflow completed with failed nodes",
                        workflow.getId()
                );

                return ExecutionResult.failure(
                        "Workflow failed: one or more nodes failed",
                        metrics
                );
            }
            // Workflow complete
            workflow.getCurrentState().markCompleted();
            metrics.setWorkflowEndedAt(Instant.now());
            auditLog("WORKFLOW_COMPLETED", "Workflow execution completed successfully", workflow.getId());

            return ExecutionResult.success(metrics);

        } catch (Exception e) {
            workflow.getCurrentState().setPhase(WorkflowState.ExecutionPhase.FAILED);
            metrics.setWorkflowEndedAt(Instant.now());
            auditLog("WORKFLOW_FAILED", "Workflow execution failed: " + e.getMessage(), workflow.getId());
            return ExecutionResult.failure(e.getMessage(), metrics);
        }
    }

    /**
     * Identify all nodes that can run now (dependencies complete, not yet executed).
     * Demonstrates distinction between PENDING (dependencies not satisfied) and READY.
     */
    private Set<String> getReadyNodes() {
        Set<String> completed = new HashSet<>(workflow.getCurrentState().getCompletedNodeIds());
        Set<String> failed = workflow.getCurrentState().getFailedNodeIds();
        Set<String> ready = new HashSet<>();

        for (WorkflowNode node : workflow.getNodesById().values()) {
            if (completed.contains(node.getId()) || failed.contains(node.getId())) {
                continue; // Already executed
            }

            // All dependencies must be completed
            boolean dependenciesMet = node.getDependsOnNodeIds().stream()
                .allMatch(completed::contains);

            if (dependenciesMet) {
                ready.add(node.getId());
            }
        }

        return ready;
    }

    /**
     * For testing: expose ready nodes without execution.
     */
    public Set<String> getReadyNodesViaReflection() {
        return getReadyNodes();
    }

    /**
     * Record an artifact produced during workflow execution.
     * Makes artifact available as cross-stage context to dependent nodes.
     * Links artifact production to audit trail for observability.
     */
    public void recordArtifact(Artifact artifact) {
        workflow.getCurrentState().recordArtifact(artifact);
        auditLog("ARTIFACT_PRODUCED",
            "Artifact produced: " + artifact.name() + " (type=" + artifact.type() + ")",
            artifact.producedByNodeId());
    }

    /**
     * Execute all ready nodes in parallel. Independent nodes run concurrently
     * using a bounded thread pool. Synchronization points still work correctly
     * as dependency resolution happens per-iteration of the main execute loop.
     * Respects all execution semantics: retries, approvals, rollback, metrics, audit logging.
     */
    private void executeReadyNodes(Set<String> readyNodes, NodeExecutor nodeExecutor,
                                   ApprovalHandlerInterface approvalHandler) throws InterruptedException {

        if (readyNodes.isEmpty()) {
            return;
        }

        // Create bounded thread pool for parallel node execution
        // Limited to 4 threads max to prevent thread explosion
        int threadCount = Math.min(readyNodes.size(), 4);
        ExecutorService executor = Executors.newFixedThreadPool(threadCount);
        List<Future<?>> futures = new ArrayList<>();

        try {
            // Submit each ready node as a task for parallel execution
            for (String nodeId : readyNodes) {
                WorkflowNode node = workflow.getNodesById().get(nodeId);
                futures.add(executor.submit(() -> {
                    try {
                        executeNode(node, nodeExecutor, approvalHandler);
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                    }
                }));
            }

            // Wait for all node executions to complete
            for (Future<?> future : futures) {
                try {
                    future.get();
                } catch (ExecutionException e) {
                    // Task execution failed - shouldn't happen as executeNode doesn't throw checked exceptions
                    throw new RuntimeException("Node execution failed", e);
                }
            }

        } catch (InterruptedException e) {
            // Main thread interrupted: cancel all pending tasks and propagate
            executor.shutdownNow();
            Thread.currentThread().interrupt();
            throw e;
        } finally {
            executor.shutdown();
        }
    }

    /**
     * Execute a single node with entry gate validation, retry logic, and approval.
     * Demonstrates: entry/exit gates, retry policy, approval checkpoints, decision lineage.
     */
    private void executeNode(WorkflowNode node, NodeExecutor nodeExecutor,
                            ApprovalHandlerInterface approvalHandler) throws InterruptedException {

        auditLog("NODE_EXECUTION_STARTED", "Starting execution of " + node.getId(), node.getId());

        // Entry gate: check preconditions and policies
        try {
            validateEntryGate(node);
            // Policy enforcement: check if any enforceable policies block execution
            validatePolicies(node);
        } catch (ValidationException e) {
            handleGateFailure(node, null, e, "entry gate or policy validation failed");
            return;
        }

        // Execute with retry policy
        Execution execution = executeWithRetry(node, nodeExecutor);

        if (execution.getStatus() == ExecutionStatus.SUCCESS) {
            // Exit gate: check postconditions
            try {
                validateExitGate(node, execution);
            } catch (ValidationException e) {
                handleGateFailure(node, execution, e, "exit gate validation failed");
                return;
            }

            // Approval gate (if required)
            if (node.getApprovalGate() != null && node.getApprovalGate().isRequired()) {
                try {
                    ApprovalResult approval = approvalHandler.requestApproval(node, execution);

                    if (approval.isApproved()) {
                        auditLog("APPROVAL_GRANTED", "Approval granted for " + node.getId(), node.getId());
                        decisionRecorder.recordApprovalDecision(node.getId(), execution.getId(),
                            true, approval.getApprover(), approval.getReason());
                    } else {
                        auditLog("APPROVAL_REJECTED", "Approval rejected for " + node.getId(), node.getId());
                        decisionRecorder.recordApprovalDecision(node.getId(), execution.getId(),
                            false, approval.getApprover(), approval.getReason());
                        handleApprovalRejection(node, nodeExecutor);
                        return;
                    }
                } catch (ApprovalTimeoutException e) {
                    auditLog("APPROVAL_TIMEOUT", "Approval timed out for " + node.getId(), node.getId());
                    workflow.getCurrentState().setPhase(WorkflowState.ExecutionPhase.WAITING_FOR_APPROVAL);
                    return;
                }
            }

            // Mark node as completed
            recordNodeRecovery(node.getId());
            workflow.getCurrentState().markNodeCompleted(node.getId());
            metrics.incrementCompletedNodes();
            auditLog("NODE_COMPLETED", "Completed " + node.getId(), node.getId());

        } else {
            // Node execution failed
            workflow.getCurrentState().markNodeFailed(node.getId(),
                new ExecutionFailure("Execution failed after retries",
                    new Exception(execution.getErrorDetails()), Instant.now()));
            metrics.incrementFailedNodes();
            auditLog("NODE_FAILED", "Failed: " + node.getId() + " - " + execution.getErrorDetails(), node.getId());
        }
    }

    /**
     * Execute node with bounded retry logic and backoff.
     * Demonstrates retry policy enforcement and WAITING_FOR_RETRY state.
     */
    private Execution executeWithRetry(WorkflowNode node, NodeExecutor nodeExecutor) {
        RetryPolicy policy = node.getRetryPolicy();
        int attempt = 0;
        long totalDelayMs = 0;
        Execution lastExecution = null;

        while (attempt < policy.getMaxRetries()) {
            attempt++;

            try {
                // Execute node
                ExecutionContext context = new ExecutionContext(
                        node,
                        null,
                        workflow.getCurrentState(),
                        metrics,
                        this::recordArtifact
                );

                if (nodeExecutor instanceof ContextAwareNodeExecutor contextAwareExecutor) {
                    lastExecution = contextAwareExecutor.execute(node, attempt, context);
                } else {
                    lastExecution = nodeExecutor.execute(node, attempt);
                }

                if (lastExecution.getStatus() == ExecutionStatus.SUCCESS) {
                    if (attempt > 1) {
                        metrics.incrementRetriedNodes();
                        metrics.recordRetryDelayTotal(totalDelayMs);
                        auditLog("RETRY_SUCCESS", node.getId() + " succeeded on attempt " + attempt, node.getId());
                    }
                    return lastExecution;
                }

                // Check if exception should trigger retry
                if (!shouldRetry(lastExecution, policy)) {
                    workflow.getCurrentState().recordNodeFirstFailure(node.getId(), lastExecution.getEndedAt());
                    return lastExecution;
                }

                // Retry with backoff
                if (attempt < policy.getMaxRetries()) {
                    workflow.getCurrentState().recordNodeFirstFailure(node.getId(), lastExecution.getEndedAt());
                    int delaySeconds = policy.calculateDelaySeconds(attempt);

                    if (totalDelayMs + (delaySeconds * 1000L) > (policy.getMaxDurationSeconds() * 1000L)) {
                        auditLog("RETRY_TIMEOUT", node.getId() + " exceeded max duration", node.getId());
                        return lastExecution; // Time budget exceeded
                    }

                    auditLog("RETRY_SCHEDULED", node.getId() + " will retry after " + delaySeconds + "s", node.getId());
                    decisionRecorder.recordRetryDecision(node.getId(), lastExecution.getId(),
                        attempt + 1, lastExecution.getErrorDetails(), delaySeconds);

                    workflow.getCurrentState().setPhase(WorkflowState.ExecutionPhase.WAITING_FOR_RETRY);
                    Thread.sleep(delaySeconds * 1000L);
                    totalDelayMs += (delaySeconds * 1000L);
                    workflow.getCurrentState().setPhase(WorkflowState.ExecutionPhase.RUNNING);
                }

            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                return Execution.builder()
                    .status(ExecutionStatus.FAILED)
                    .errorDetails("Execution interrupted: " + e.getMessage())
                    .build();
            }
        }

        return lastExecution != null ? lastExecution :
            Execution.builder()
                .status(ExecutionStatus.FAILED)
                .errorDetails("Max retries exceeded")
                .build();
    }

    private boolean shouldRetry(Execution execution, RetryPolicy policy) {
        if (execution.getStatus() == ExecutionStatus.SUCCESS) {
            return false;
        }
        if (execution.getErrorDetails() == null) {
            return false;
        }

        List<String> retryOn = policy.getRetryOnExceptions();
        List<String> doNotRetry = policy.getDoNotRetryOnExceptions();
        String error = execution.getErrorDetails();

        // If explicit do-not-retry exceptions listed, check them first
        if (!doNotRetry.isEmpty()) {
            for (String exception : doNotRetry) {
                if (error.contains(exception)) {
                    return false;
                }
            }
        }

        // If no retry-on list, retry on any error
        if (retryOn.isEmpty()) {
            return true;
        }

        // Check if error matches any retry-on exception
        for (String exception : retryOn) {
            if (error.contains(exception)) {
                return true;
            }
        }

        return false;
    }

    private void validateEntryGate(WorkflowNode node) throws ValidationException {
        ExecutionContext context = new ExecutionContext(
                node,
                null,
                workflow.getCurrentState(),
                metrics
        );
        Gate entryGate = node.getEntryGate();
        if (entryGate == null || entryGate.getType() == Gate.Type.PASS_THROUGH) {
            return; // No validation needed
        }

        // Check entry gate validations
        for (ValidationRule rule : entryGate.getValidationRules()) {
            // Simplified validation: in production, would check artifacts, policies, etc.
            if (!rule.evaluate(context)) {
                throw new ValidationException("Entry gate validation failed: " + rule.getDescription());
            }
        }
    }

    private void validateExitGate(WorkflowNode node, Execution execution) throws ValidationException {
        ExecutionContext context = new ExecutionContext(
                node,
                execution,
                workflow.getCurrentState(),
                metrics
        );
        Gate exitGate = node.getExitGate();
        if (exitGate == null || exitGate.getType() == Gate.Type.PASS_THROUGH) {
            return; // No validation needed
        }

        // Check exit gate validations
        for (ValidationRule rule : exitGate.getValidationRules()) {
            if (!rule.evaluate(context)) {
                throw new ValidationException("Exit gate validation failed: " + rule.getDescription());
            }
        }
    }

    /**
     * Enforces governance policies at the current execution context.
     * Blocks execution if an enforceable policy would be violated.
     */
    private void validatePolicies(WorkflowNode node) throws ValidationException {
        // Create execution context from current runtime state (no execution yet)
        ExecutionMetrics currentMetrics = this.metrics;
        WorkflowState currentState = workflow.getCurrentState();

        ExecutionContext context = new ExecutionContext(node, null, currentState, currentMetrics);

        // Check each policy on the node
        for (Policy policy : node.getPolicies()) {
            // Skip non-enforceable policies
            if (!policy.enforceable()) {
                auditLog("POLICY_SKIPPED", "Policy " + policy.name() + " is not enforceable", node.getId());
                continue;
            }

            // Skip policies that don't apply to this node type
            if (!context.policyAppliesToNode(policy)) {
                auditLog("POLICY_SKIPPED", "Policy " + policy.name() + " does not apply to node type " + node.getType(), node.getId());
                continue;
            }

            // Policy applies and is enforceable - BLOCK execution
            auditLog("POLICY_BLOCKED", "Enforced policy " + policy.name() + ": " + policy.description(), node.getId());
            throw new ValidationException("Policy enforcement blocked execution: " + policy.name() + " - " + policy.description());
        }
    }

    private void handleGateFailure(WorkflowNode node, Execution execution, ValidationException e, String reason) {
        auditLog("GATE_FAILURE", node.getId() + ": " + reason, node.getId());

        // Record first failure detection for MTTR
        Instant detectionTime = (execution != null) ? execution.getEndedAt() : Instant.now();
        workflow.getCurrentState().recordNodeFirstFailure(node.getId(), detectionTime);

        Gate gate = node.getExitGate() != null ? node.getExitGate() : node.getEntryGate();
        Gate.FailureAction action = gate != null ? gate.getFailureAction() : Gate.FailureAction.BLOCK;

        switch (action) {
            case BLOCK:
                workflow.getCurrentState().markNodeFailed(node.getId(),
                    new ExecutionFailure(reason, new Exception(e.getMessage()), Instant.now()));
                metrics.incrementFailedNodes();
                break;
            case TRIGGER_REPLAN:
                auditLog("REPLAN_TRIGGERED", "Triggering replan due to gate failure", node.getId());
                triggerReplan(node, reason);
                break;
            case FALLBACK_TO_PREVIOUS:
                handleArtifactFallback(node, execution, reason);
                break;
            case WARN:
                auditLog("GATE_WARNING", "Gate validation warning: " + reason, node.getId());
                break;
        }
    }

    private void handleArtifactFallback(WorkflowNode node, Execution execution, String reason) {
        if (execution == null) {
            // Fallback not possible without an execution to roll back
            workflow.getCurrentState().markNodeFailed(node.getId(),
                new ExecutionFailure("Artifact fallback not possible: no failed execution provided",
                    new Exception(reason), Instant.now()));
            metrics.incrementFailedNodes();
            return;
        }

        List<String> currentArtifactIds = workflow.getCurrentState().getArtifactIdsProducedByNode(node.getId());
        
        // Deactivate artifacts from the failed execution
        workflow.getCurrentState().deactivateArtifactsFromExecution(node.getId(), execution.getId());
        
        List<String> remainingArtifactIds = workflow.getCurrentState().getArtifactIdsProducedByNode(node.getId());

        if (remainingArtifactIds.isEmpty()) {
            // No previous artifacts to fall back to
            auditLog("FALLBACK_FAILED", "No previous artifacts found for fallback on " + node.getId(), node.getId());
            workflow.getCurrentState().markNodeFailed(node.getId(),
                new ExecutionFailure("Artifact fallback failed: no previous artifacts found",
                    new Exception(reason), Instant.now()));
            metrics.incrementFailedNodes();
        } else {
            // Success - fallback to remaining artifacts
            recordNodeRecovery(node.getId());
            metrics.incrementFallbackCount();
            decisionRecorder.recordFallbackDecision(node.getId(), execution.getId(), reason, remainingArtifactIds);
            workflow.getCurrentState().markNodeCompleted(node.getId());
            metrics.incrementCompletedNodes();
            auditLog("ARTIFACT_FALLBACK", "Successfully fell back to previous artifacts for " + node.getId(), node.getId());
        }
    }

    private void recordNodeRecovery(String nodeId) {
        workflow.getCurrentState().getAndClearNodeFirstFailure(nodeId).ifPresent(firstFail -> {
            long duration = Instant.now().toEpochMilli() - firstFail.toEpochMilli();
            metrics.recordRecovery(duration);
        });
    }

    private void handleApprovalRejection(WorkflowNode node, NodeExecutor nodeExecutor) throws InterruptedException {
        auditLog("APPROVAL_REJECTED", "Initiating rollback for " + node.getId(), node.getId());

        RollbackPolicy rollback = node.getRollbackPolicy();
        if (rollback.isReversible()) {
            metrics.incrementRolledBackNodes();
            workflow.getCurrentState().setPhase(WorkflowState.ExecutionPhase.ROLLED_BACK);

            // Execute compensating operations
            nodeExecutor.rollback(node, rollback.getReversibleOperations());

            decisionRecorder.recordRollbackDecision(node.getId(), null,
                "Approval rejected", rollback.getReversibleOperations());
        } else {
            workflow.getCurrentState().setPhase(WorkflowState.ExecutionPhase.SAFE_STOPPED);

            decisionRecorder.recordSafeStopDecision(node.getId(), null,
                "Approval rejected but rollback not possible");
        }
    }

    /**
     * Initiates governed recovery through bounded replanning.
     * Re-opens upstream dependencies to allow corrective action.
     */
    private void triggerReplan(
            WorkflowNode failedNode,
            String reason) {

        ReplanTrigger trigger =
                failedNode.getReplanTrigger();

        int allowedReplans =
                trigger != null
                        ? Math.min(
                        maxReplans,
                        trigger.getMaxReplans()
                )
                        : maxReplans;

        if (replanCount >= allowedReplans) {

            auditLog(
                    "REPLAN_EXHAUSTED",
                    "Max replans exceeded",
                    failedNode.getId()
            );

            workflow.getCurrentState()
                    .setPhase(
                            WorkflowState.ExecutionPhase.FAILED
                    );

            return;
        }

        replanCount++;

        metrics.incrementReplannedCount();

        /*
         * A validation node may discover that an earlier engineering
         * stage needs to run again.
         *
         * If no explicit trigger exists, preserve the original behavior
         * and replan from the failed node.
         */
        String replanFromNodeId =
                trigger != null
                        ? trigger.getNodeIdToReplanFrom()
                        : failedNode.getId();

        String replanReason =
                trigger != null
                        && trigger.getReasonDescription() != null
                        ? trigger.getReasonDescription()
                        : reason;

        List<String> assumptions =
                trigger != null
                        ? trigger.getAssumptionsBroken()
                        : List.of(
                        "Exit gate validation failed"
                );

        decisionRecorder.recordReplanDecision(
                failedNode.getId(),
                replanReason,
                assumptions,
                replanCount
        );

        /*
         * Reopen the configured node AND every downstream node.
         *
         * Example:
         *
         * implementation
         *      ↓
         * testing
         *      ↓
         * validation
         *
         * If validation identifies an implementation gap and its
         * ReplanTrigger specifies "implementation", all three nodes
         * become executable again.
         */
        Set<String> nodesToReopen =
                getDownstreamNodes(
                        replanFromNodeId
                );

        nodesToReopen.add(
                replanFromNodeId
        );

        workflow.getCurrentState()
                .reopenNodesForReplan(nodesToReopen);


        workflow.getCurrentState()
                .setPhase(
                        WorkflowState.ExecutionPhase.RUNNING
                );

        auditLog(
                "REPLAN_EXECUTED",
                "Replan "
                        + replanCount
                        + " triggered by "
                        + failedNode.getId()
                        + "; restarting from "
                        + replanFromNodeId
                        + ": "
                        + replanReason,
                failedNode.getId()
        );
    }

    private Set<String> getDownstreamNodes(String nodeId) {
        Set<String> downstream = new HashSet<>();
        Queue<String> queue = new LinkedList<>(Arrays.asList(nodeId));

        while (!queue.isEmpty()) {
            String current = queue.poll();
            for (WorkflowNode node : workflow.getNodesById().values()) {
                if (node.getDependsOnNodeIds().contains(current)) {
                    downstream.add(node.getId());
                    queue.offer(node.getId());
                }
            }
        }

        return downstream;
    }

    private boolean isWorkflowComplete() {
        LinkedList<String> completed = workflow.getCurrentState().getCompletedNodeIds();
        Set<String> failed = workflow.getCurrentState().getFailedNodeIds();
        int totalNodes = workflow.getNodesById().size();

        return (completed.size() + failed.size()) == totalNodes;
    }

    private boolean isTerminalState() {
        WorkflowState.ExecutionPhase phase = workflow.getCurrentState().getPhase();

        return phase == WorkflowState.ExecutionPhase.FAILED
                || phase == WorkflowState.ExecutionPhase.ROLLED_BACK
                || phase == WorkflowState.ExecutionPhase.SAFE_STOPPED;
    }

    private void auditLog(String eventType, String message, String entityId) {
        AuditEntry entry = new AuditEntry(eventType, message, entityId, Instant.now());
        auditTrail.add(entry);  // Thread-safe: auditTrail is a synchronized list
        log.info("[AUDIT] {} - {} ({})", eventType, message, entityId);
    }

    // Inner classes for audit trail and exceptions
    @Getter
    public static class AuditEntry {
        private final String eventType;
        private final String message;
        private final String entityId;
        private final Instant timestamp;

        public AuditEntry(String eventType, String message, String entityId, Instant timestamp) {
            this.eventType = eventType;
            this.message = message;
            this.entityId = entityId;
            this.timestamp = timestamp;
        }
    }

    public static class ValidationException extends Exception {
        public ValidationException(String message) {
            super(message);
        }
    }

    public static class ApprovalTimeoutException extends Exception {
        public ApprovalTimeoutException(String message) {
            super(message);
        }
    }

    @Getter
    public static class ExecutionResult {
        private final boolean success;
        private final String message;
        private final ExecutionMetrics metrics;

        private ExecutionResult(boolean success, String message, ExecutionMetrics metrics) {
            this.success = success;
            this.message = message;
            this.metrics = metrics;
        }

        public static ExecutionResult success(ExecutionMetrics metrics) {
            return new ExecutionResult(true, "Workflow completed successfully", metrics);
        }

        public static ExecutionResult failure(String message, ExecutionMetrics metrics) {
            return new ExecutionResult(false, message, metrics);
        }

        public static ExecutionResult waiting(String reason) {
            return new ExecutionResult(false, reason, null);
        }
    }
}
