package com.cs.urlshortenerorchestrator.engine.domain;

import java.time.Instant;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map;
import java.util.Set;
import java.util.ArrayList;
import java.util.List;
import java.util.Collections;
import lombok.Getter;
import lombok.Setter;

/**
 * WorkflowState: current execution state of a workflow.
 * Includes artifact storage for cross-stage context availability.
 * Thread-safe for parallel node execution: completedNodeIds, failedNodes, and artifact indexes are synchronized.
 */
public class WorkflowState {
    @Getter
    @Setter
    private String currentNodeId;
    @Getter
    @Setter
    private ExecutionPhase phase;
    private final List<String> completedNodeIds = Collections.synchronizedList(new LinkedList<>());
    private final Map<String, ExecutionFailure> failedNodes = Collections.synchronizedMap(new HashMap<>());
    @Getter
    private Instant startedAt;
    @Getter
    @Setter
    private Instant completedAt;
    private final Map<String, Artifact> artifacts = Collections.synchronizedMap(new HashMap<>());
    private final Map<String, List<String>> nodeToArtifactIds = Collections.synchronizedMap(new HashMap<>());

    public enum ExecutionPhase {
        RUNNING, WAITING_FOR_APPROVAL, WAITING_FOR_RETRY,
        COMPLETED, FAILED, ROLLED_BACK, SAFE_STOPPED
    }

    public WorkflowState(String rootNodeId) {
        this.currentNodeId = rootNodeId;
        this.phase = ExecutionPhase.RUNNING;
        this.startedAt = Instant.now();
    }

    public LinkedList<String> getCompletedNodeIds() {
        return new LinkedList<>(completedNodeIds);
    }

    public Map<String, ExecutionFailure> getFailedNodes() {
        return new HashMap<>(failedNodes);
    }

    public Map<String, Artifact> getArtifacts() {
        return new HashMap<>(artifacts);
    }

    public Map<String, List<String>> getNodeToArtifactIds() {
        return new HashMap<>(nodeToArtifactIds);
    }

    // ...existing code...

    public boolean isCompleted() { return phase == ExecutionPhase.COMPLETED; }

    public void markCompleted() {
        this.phase = ExecutionPhase.COMPLETED;
        this.completedAt = Instant.now();
    }

    public void markNodeCompleted(String nodeId) {
        completedNodeIds.add(nodeId);
    }

    public void markNodeFailed(String nodeId, ExecutionFailure failure) {
        failedNodes.put(nodeId, failure);
    }

    public Set<String> getFailedNodeIds() {
        return new java.util.HashSet<>(failedNodes.keySet());
    }

    /**
     * Record an artifact produced during workflow execution.
     * Makes artifact available as cross-stage context to dependent nodes.
     * Thread-safe: uses synchronized maps.
     */
    public void recordArtifact(Artifact artifact) {
        artifacts.put(artifact.id(), artifact);

        // Index by producing node for cross-stage access
        String producingNodeId = artifact.producedByNodeId();
        nodeToArtifactIds.computeIfAbsent(producingNodeId, k -> new ArrayList<>())
            .add(artifact.id());
    }

    /**
     * Get all artifacts produced by a specific node.
     * Used by dependent nodes to access predecessor outputs.
     * Returns a safe snapshot.
     */
    public List<String> getArtifactIdsProducedByNode(String nodeId) {
        List<String> ids = nodeToArtifactIds.get(nodeId);
        return ids != null ? new ArrayList<>(ids) : new ArrayList<>();
    }

    /**
     * Get artifact by ID.
     * Thread-safe: artifacts map is synchronized.
     */
    public Artifact getArtifactById(String artifactId) {
        return artifacts.get(artifactId);
    }

    public void deactivateArtifactsFromExecution(String nodeId, String executionId) {
        List<String> ids = nodeToArtifactIds.get(nodeId);
        if (ids != null) {
            ids.removeIf(artifactId -> {
                Artifact artifact = artifacts.get(artifactId);
                return artifact != null && executionId.equals(artifact.producedByExecutionId());
            });
        }
    }

    public void reopenNodesForReplan(Set<String> nodeIds) {
        completedNodeIds.removeAll(nodeIds);
        failedNodes.keySet().removeAll(nodeIds);
    }
}

