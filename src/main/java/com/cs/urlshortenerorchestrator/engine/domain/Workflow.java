package com.cs.urlshortenerorchestrator.engine.domain;

import java.time.Instant;
import java.util.Map;
import java.util.Objects;

/**
 * Workflow represents a complete SDLC orchestration plan.
 * Contains nodes, edges, and execution state.
 */
public record Workflow(
    String id,
    String name,
    String description,
    WorkflowNode rootNode,
    Map<String, WorkflowNode> nodesById,
    WorkflowState currentState,
    Instant createdAt,
    Map<String, Object> metadata
) {
    public Workflow {
        Objects.requireNonNull(id, "id required");
        Objects.requireNonNull(rootNode, "rootNode required");
        Objects.requireNonNull(nodesById, "nodesById required");
    }
}

