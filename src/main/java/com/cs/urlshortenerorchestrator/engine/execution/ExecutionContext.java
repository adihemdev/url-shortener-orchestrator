package com.cs.urlshortenerorchestrator.engine.execution;

import com.cs.urlshortenerorchestrator.engine.domain.*;
import lombok.Getter;

import java.util.Objects;
import java.util.List;
import java.util.ArrayList;
import java.util.function.Consumer;

/**
 * ExecutionContext: captures the actual runtime context for policy evaluation
 * at gate boundaries. Holds current node, execution, and workflow state.
 * Provides access to artifact cross-stage context for dependent nodes.
 *
 * Minimal MVP: lightweight value object for passing execution state to policy checks
 * without null context and for artifact integration.
 */
@Getter
public class ExecutionContext {
    private final WorkflowNode node;
    private final Execution execution;
    private final WorkflowState workflowState;
    private final ExecutionMetrics metrics;
    private final Consumer<Artifact> artifactPublisher;

    public ExecutionContext(WorkflowNode node, Execution execution,
                           WorkflowState workflowState, ExecutionMetrics metrics) {
        this.node = Objects.requireNonNull(node, "node required");
        this.execution = execution;  // can be null before execution
        this.workflowState = Objects.requireNonNull(workflowState, "workflowState required");
        this.metrics = Objects.requireNonNull(metrics, "metrics required");
        this.artifactPublisher = artifact -> {};
    }

    public ExecutionContext(
            WorkflowNode node,
            Execution execution,
            WorkflowState workflowState,
            ExecutionMetrics metrics,
            Consumer<Artifact> artifactPublisher) {

        this.node = Objects.requireNonNull(node, "node required");
        this.execution = execution;
        this.workflowState =
                Objects.requireNonNull(workflowState, "workflowState required");
        this.metrics =
                Objects.requireNonNull(metrics, "metrics required");
        this.artifactPublisher =
                Objects.requireNonNull(artifactPublisher, "artifactPublisher required");
    }

    /**
     * Check if a policy applies to this node's execution context
     */
    public boolean policyAppliesToNode(Policy policy) {
        if (policy == null || !policy.enforceable()) {
            return false;
        }
        return policy.appliesToNodeTypes().contains(node.getType());
    }

    /**
     * Get artifacts produced by a predecessor node.
     * Used by dependent nodes to access outputs as cross-stage context.
     */
    public List<Artifact> getArtifactsFromPredecessor(String predecessorNodeId) {
        List<Artifact> result = new ArrayList<>();
        List<String> artifactIds = workflowState.getArtifactIdsProducedByNode(predecessorNodeId);

        for (String artifactId : artifactIds) {
            Artifact artifact = workflowState.getArtifactById(artifactId);
            if (artifact != null) {
                result.add(artifact);
            }
        }

        return result;
    }

    /**
     * Get artifacts from all direct predecessor nodes.
     */
    public List<Artifact> getArtifactsFromAllPredecessors() {
        List<Artifact> result = new ArrayList<>();

        for (String predecessorId : node.getDependsOnNodeIds()) {
            result.addAll(getArtifactsFromPredecessor(predecessorId));
        }

        return result;
    }

    /**
     * Publish an artifact produced by the current executor.
     * WorkflowExecutor remains responsible for storing and auditing it.
     */
    public void publishArtifact(Artifact artifact) {
        Objects.requireNonNull(artifact, "artifact required");

        if (!node.getId().equals(artifact.producedByNodeId())) {
            throw new IllegalArgumentException(
                    "Artifact producer must match current node: " + node.getId()
            );
        }

        artifactPublisher.accept(artifact);
    }

    public Artifact getArtifactById(String artifactId) {
        return workflowState.getArtifactById(artifactId);
    }

    public List<Artifact> getAllArtifacts() {
        return new ArrayList<>(
                workflowState.getArtifacts().values()
        );
    }

    @Override
    public String toString() {
        return String.format("ExecutionContext[node=%s, executionStatus=%s, workflowPhase=%s]",
            node.getId(),
            execution != null ? execution.getStatus() : "NOT_EXECUTED",
            workflowState.getPhase());
    }
}
