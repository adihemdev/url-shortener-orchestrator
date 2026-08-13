package com.cs.urlshortenerorchestrator.engine.domain;

import java.time.Instant;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map;
import java.util.Set;

/**
 * WorkflowState: current execution state of a workflow.
 */
public class WorkflowState {
    private String currentNodeId;
    private ExecutionPhase phase;
    private final LinkedList<String> completedNodeIds = new LinkedList<>();
    private final Map<String, ExecutionFailure> failedNodes = new HashMap<>();
    private Set<String> pendingNodeIds;
    private Instant startedAt;
    private Instant completedAt;

    public enum ExecutionPhase {
        RUNNING, WAITING_FOR_APPROVAL, WAITING_FOR_RETRY,
        COMPLETED, FAILED, ROLLED_BACK, SAFE_STOPPED
    }

    public WorkflowState(String rootNodeId) {
        this.currentNodeId = rootNodeId;
        this.phase = ExecutionPhase.RUNNING;
        this.startedAt = Instant.now();
    }

    // Getters and setters
    public String getCurrentNodeId() { return currentNodeId; }
    public void setCurrentNodeId(String nodeId) { this.currentNodeId = nodeId; }

    public ExecutionPhase getPhase() { return phase; }
    public void setPhase(ExecutionPhase phase) { this.phase = phase; }

    public LinkedList<String> getCompletedNodeIds() { return completedNodeIds; }
    public void markNodeCompleted(String nodeId) {
        completedNodeIds.add(nodeId);
    }

    public Map<String, ExecutionFailure> getFailedNodes() { return failedNodes; }
    public void markNodeFailed(String nodeId, ExecutionFailure failure) {
        failedNodes.put(nodeId, failure);
    }

    public boolean isCompleted() { return phase == ExecutionPhase.COMPLETED; }
    public void markCompleted() {
        this.phase = ExecutionPhase.COMPLETED;
        this.completedAt = Instant.now();
    }

    public Instant getStartedAt() { return startedAt; }
    public Instant getCompletedAt() { return completedAt; }
    public Set<String> getPendingNodeIds() { return pendingNodeIds; }
    public void setPendingNodeIds(Set<String> nodeIds) { this.pendingNodeIds = nodeIds; }
}

