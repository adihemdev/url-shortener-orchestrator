package com.cs.urlshortenerorchestrator.engine.domain;

import java.util.Set;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import lombok.Getter;

/**
 * WorkflowNode represents a stage in the SDLC (Requirements, Design, Implementation, Testing, Validation, Release).
 * Supports entry/exit gates, parallel execution, retries, approvals, rollbacks, and runtime policy enforcement.
 */
public class WorkflowNode {
    private final String id;
    private final NodeType type;
    private final String description;
    private final Set<String> dependsOnNodeIds;
    private final Set<String> parallelNodeIds;
    private final Gate entryGate;
    private final String executorName;
    private final Gate exitGate;
    private final ApprovalGate approvalGate;
    private final RetryPolicy retryPolicy;
    private final RollbackPolicy rollbackPolicy;
    private final int timeoutSeconds;
    private final List<Policy> policies;

    public WorkflowNode(String id, NodeType type, String description,
                       Set<String> dependsOnNodeIds, Set<String> parallelNodeIds,
                       Gate entryGate, String executorName, Gate exitGate,
                       ApprovalGate approvalGate, RetryPolicy retryPolicy,
                       RollbackPolicy rollbackPolicy, int timeoutSeconds, List<Policy> policies) {
        this.id = Objects.requireNonNull(id, "id required");
        this.type = Objects.requireNonNull(type, "type required");
        this.description = description;
        this.dependsOnNodeIds = dependsOnNodeIds != null ? dependsOnNodeIds : Set.of();
        this.parallelNodeIds = parallelNodeIds != null ? parallelNodeIds : Set.of();
        this.entryGate = entryGate;
        this.executorName = executorName;
        this.exitGate = exitGate;
        this.approvalGate = approvalGate;
        this.retryPolicy = retryPolicy != null ? retryPolicy : RetryPolicy.noRetry();
        this.rollbackPolicy = rollbackPolicy != null ? rollbackPolicy : RollbackPolicy.noRollback();
        this.timeoutSeconds = timeoutSeconds;
        this.policies = policies != null ? policies : List.of();
    }

    // Getters
    public String getId() { return id; }
    public NodeType getType() { return type; }
    public String getDescription() { return description; }
    public Set<String> getDependsOnNodeIds() { return dependsOnNodeIds; }
    public Set<String> getParallelNodeIds() { return parallelNodeIds; }
    public Gate getEntryGate() { return entryGate; }
    public String getExecutorName() { return executorName; }
    public Gate getExitGate() { return exitGate; }
    public ApprovalGate getApprovalGate() { return approvalGate; }
    public RetryPolicy getRetryPolicy() { return retryPolicy; }
    public RollbackPolicy getRollbackPolicy() { return rollbackPolicy; }
    public int getTimeoutSeconds() { return timeoutSeconds; }
    public List<Policy> getPolicies() { return policies; }


    public boolean isParallelWith(String nodeId) {
        return parallelNodeIds.contains(nodeId);
    }

    public boolean requiresApproval() {
        return approvalGate != null;
    }

    public static Builder builder(String id, NodeType type) {
        return new Builder(id, type);
    }

    public static class Builder {
        private final String id;
        private final NodeType type;
        private String description;
        private Set<String> dependsOnNodeIds = new HashSet<>();
        private Set<String> parallelNodeIds = new HashSet<>();
        private Gate entryGate;
        private String executorName;
        private Gate exitGate;
        private ApprovalGate approvalGate;
        private RetryPolicy retryPolicy = RetryPolicy.noRetry();
        private RollbackPolicy rollbackPolicy = RollbackPolicy.noRollback();
        private int timeoutSeconds = 600;
        private List<Policy> policies = List.of();

        public Builder(String id, NodeType type) {
            this.id = id;
            this.type = type;
        }

        public Builder description(String desc) {
            this.description = desc;
            return this;
        }

        public Builder dependsOn(String... nodeIds) {
            this.dependsOnNodeIds.addAll(Set.of(nodeIds));
            return this;
        }

        public Builder parallelWith(String... nodeIds) {
            this.parallelNodeIds.addAll(Set.of(nodeIds));
            return this;
        }

        public Builder entryGate(Gate gate) {
            this.entryGate = gate;
            return this;
        }

        public Builder executor(String name) {
            this.executorName = name;
            return this;
        }

        public Builder exitGate(Gate gate) {
            this.exitGate = gate;
            return this;
        }

        public Builder approvalGate(ApprovalGate gate) {
            this.approvalGate = gate;
            return this;
        }

        public Builder retryPolicy(RetryPolicy policy) {
            this.retryPolicy = policy;
            return this;
        }

        public Builder rollbackPolicy(RollbackPolicy policy) {
            this.rollbackPolicy = policy;
            return this;
        }

        public Builder timeout(int seconds) {
            this.timeoutSeconds = seconds;
            return this;
        }

        public Builder policies(List<Policy> policies) {
            this.policies = policies != null ? policies : List.of();
            return this;
        }

        public WorkflowNode build() {
            return new WorkflowNode(id, type, description, dependsOnNodeIds,
                parallelNodeIds, entryGate, executorName, exitGate,
                approvalGate, retryPolicy, rollbackPolicy, timeoutSeconds, policies);
        }
    }
}

