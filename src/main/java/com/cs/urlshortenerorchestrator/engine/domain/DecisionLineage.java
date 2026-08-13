package com.cs.urlshortenerorchestrator.engine.domain;

import java.time.Instant;
import java.util.LinkedList;

/**
 * DecisionLineage: audit trail of decisions that led to current state.
 */
public record DecisionLineage(
    String id,
    String workflowId,
    LinkedList<Decision> decisions,
    Instant createdAt
) {}

