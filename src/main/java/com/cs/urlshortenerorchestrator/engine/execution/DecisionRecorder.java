package com.cs.urlshortenerorchestrator.engine.execution;

import com.cs.urlshortenerorchestrator.engine.domain.*;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

import java.time.Instant;
import java.util.*;

/**
 * Records workflow execution decisions for audit trail and lineage tracking.
 * Minimal implementation: records key decisions as they occur.
 */
@Slf4j
public class DecisionRecorder {
    @Getter
    private final List<Decision> decisions = new ArrayList<>();
    private final String workflowId;
    private int decisionCounter = 0;

    public DecisionRecorder(String workflowId) {
        this.workflowId = workflowId;
    }

    /**
     * Record approval decision.
     */
    public void recordApprovalDecision(String nodeId, String executionId,
                                      boolean approved, String approver, String reason) {
        String outcome = approved ? "APPROVED" : "REJECTED";
        Decision decision = new Decision(
            generateDecisionId(),
            nodeId,
            executionId,
            DecisionType.APPROVAL_DECISION,
            String.format("Node %s approval requested. Approver: %s", nodeId, approver),
            outcome + " - " + reason,
            Instant.now(),
            true,  // approval is reversible
            getPreviousDecisionIds()
        );
        decisions.add(decision);
        log.debug("Recorded approval decision: {} - {}", nodeId, outcome);
    }

    /**
     * Record retry decision.
     */
    public void recordRetryDecision(String nodeId, String executionId,
                                   int attemptNumber, String failureReason,
                                   int delaySeconds) {
        Decision decision = new Decision(
            generateDecisionId(),
            nodeId,
            executionId,
            DecisionType.RETRY_ON_FAILURE,
            String.format("Attempt %d failed: %s", attemptNumber, failureReason),
            String.format("Scheduled retry after %d second backoff", delaySeconds),
            Instant.now(),
            true,  // retry is reversible
            getPreviousDecisionIds()
        );
        decisions.add(decision);
        log.debug("Recorded retry decision: {} attempt {}", nodeId, attemptNumber);
    }

    /**
     * Record rollback decision.
     */
    public void recordRollbackDecision(String nodeId, String executionId,
                                      String reason, List<String> operations) {
        Decision decision = new Decision(
            generateDecisionId(),
            nodeId,
            executionId,
            DecisionType.ROLLBACK_DECISION,
            "Rollback triggered: " + reason,
            "Executing rollback operations: " + String.join(", ", operations),
            Instant.now(),
            false,  // rollback is not reversible
            getPreviousDecisionIds()
        );
        decisions.add(decision);
        log.debug("Recorded rollback decision: {} - {}", nodeId, reason);
    }

    /**
     * Record safe-stop decision (when rollback not possible).
     */
    public void recordSafeStopDecision(String nodeId, String executionId, String reason) {
        Decision decision = new Decision(
            generateDecisionId(),
            nodeId,
            executionId,
            DecisionType.SAFE_STOP_DECISION,
            "Workflow cannot be safely rolled back: " + reason,
            "Transitioned to SAFE_STOPPED state",
            Instant.now(),
            false,  // safe-stop is not reversible
            getPreviousDecisionIds()
        );
        decisions.add(decision);
        log.debug("Recorded safe-stop decision: {} - {}", nodeId, reason);
    }

    public void recordFallbackDecision(String nodeId, String executionId, String reason, List<String> fallbackArtifactIds) {
        Decision decision = new Decision(
            generateDecisionId(),
            nodeId,
            executionId,
            DecisionType.ARTIFACT_FALLBACK,
            reason,
            "Fallback to previous artifacts: " + String.join(", ", fallbackArtifactIds),
            Instant.now(),
            false,
            getPreviousDecisionIds()
        );
        decisions.add(decision);
        log.debug("Recorded artifact fallback decision: {} - {}", nodeId, reason);
    }

    /**
     * Record replan decision.
     */
    public void recordReplanDecision(String failedNodeId, String reason,
                                    List<String> assumptionsBroken, int replanNumber) {
        Decision decision = new Decision(
            generateDecisionId(),
            failedNodeId,
            null,  // replan doesn't have an execution ID yet
            DecisionType.REPLAN,
            String.format("Replan #%d triggered at %s: %s. Assumptions broken: %s",
                replanNumber, failedNodeId, reason,
                String.join(", ", assumptionsBroken)),
            "Workflow regenerated from " + failedNodeId,
            Instant.now(),
            true,  // replan is reversible
            getPreviousDecisionIds()
        );
        decisions.add(decision);
        log.debug("Recorded replan decision: {} with {} assumptions broken",
            failedNodeId, assumptionsBroken.size());
    }

    /**
     * Record architecture choice decision.
     */
    public void recordArchitectureDecision(String nodeId, String executionId,
                                          String choice, String reasoning) {
        Decision decision = new Decision(
            generateDecisionId(),
            nodeId,
            executionId,
            DecisionType.ARCHITECTURE_CHOICE,
            reasoning,
            "Architecture choice: " + choice,
            Instant.now(),
            true,
            getPreviousDecisionIds()
        );
        decisions.add(decision);
        log.debug("Recorded architecture decision: {} - {}", nodeId, choice);
    }

    /**
     * Record implementation strategy decision.
     */
    public void recordImplementationDecision(String nodeId, String executionId,
                                            String strategy, String reasoning) {
        Decision decision = new Decision(
            generateDecisionId(),
            nodeId,
            executionId,
            DecisionType.IMPLEMENTATION_STRATEGY,
            reasoning,
            "Implementation strategy: " + strategy,
            Instant.now(),
            true,
            getPreviousDecisionIds()
        );
        decisions.add(decision);
        log.debug("Recorded implementation decision: {} - {}", nodeId, strategy);
    }

    /**
     * Get all decisions for audit trail.
     */
    public List<Decision> getDecisions() {
        return new ArrayList<>(decisions);
    }

    /**
     * Get decision summary for specific node.
     */
    public List<Decision> getDecisionsForNode(String nodeId) {
        return decisions.stream()
            .filter(d -> d.madeByNodeId().equals(nodeId))
            .toList();
    }

    /**
     * Get decision chain - linked decisions.
     */
    public List<Decision> getDecisionChain(String decisionId) {
        List<Decision> chain = new ArrayList<>();
        Decision current = decisions.stream()
            .filter(d -> d.id().equals(decisionId))
            .findFirst()
            .orElse(null);

        while (current != null) {
            chain.add(0, current);  // prepend to build chain backward

            // Find decision that this one depends on
            String relatedId = current.relatedDecisionIds().isEmpty() ?
                null : current.relatedDecisionIds().get(0);

            final String nextId = relatedId;
            current = nextId != null ?
                decisions.stream()
                    .filter(d -> d.id().equals(nextId))
                    .findFirst()
                    .orElse(null) :
                null;
        }

        return chain;
    }

    private String generateDecisionId() {
        return "dec-" + workflowId + "-" + (++decisionCounter);
    }

    private List<String> getPreviousDecisionIds() {
        if (decisions.isEmpty()) {
            return List.of();
        }
        return List.of(decisions.get(decisions.size() - 1).id());
    }
}
