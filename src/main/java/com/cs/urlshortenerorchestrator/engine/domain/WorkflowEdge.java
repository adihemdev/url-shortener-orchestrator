package com.cs.urlshortenerorchestrator.engine.domain;

/**
 * WorkflowEdge: directed edge in the DAG.
 */
public record WorkflowEdge(
    String sourceNodeId,
    String targetNodeId,
    boolean conditional,  // can branch based on gate result
    boolean syncPoint     // all predecessors must complete before this edge
) {}

