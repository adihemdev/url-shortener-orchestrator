package com.cs.urlshortenerorchestrator.engine.domain;

import java.util.Set;
import java.util.Objects;

/**
 * WorkflowNode represents a stage in the SDLC (Requirements, Design, Implementation, Testing, Validation, Release).
 * Each node has entry/exit gates, execution strategy, and dependencies.
 */
public record WorkflowNode(
    String id,
    NodeType type,
    String description,
    Set<String> dependsOnNodeIds,  // predecessor node IDs
    Gate entryGate,                 // must pass before execution
    String executorName,            // which agent/function executes this
    Gate exitGate,                  // must pass to declare success
    ApprovalGate approvalGate,      // if null, auto-approve; if present, requires human approval
    RetryPolicy retryPolicy,
    int timeoutSeconds
) {
    public WorkflowNode {
        Objects.requireNonNull(id, "id required");
        Objects.requireNonNull(type, "type required");
        Objects.requireNonNull(dependsOnNodeIds, "dependsOnNodeIds required");
    }
}

