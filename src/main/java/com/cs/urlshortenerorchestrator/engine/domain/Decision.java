package com.cs.urlshortenerorchestrator.engine.domain;

import java.time.Instant;
import java.util.List;

/**
 * Decision: a choice made during workflow execution, with reasoning and outcome.
 */
public record Decision(
    String id,
    String madeByNodeId,
    String madeByExecutionId,
    DecisionType type,
    String reasoning,          // why was this decision made
    String outcome,            // what changed as a result
    Instant madeAt,
    boolean reversible,        // can it be rolled back
    List<String> relatedDecisionIds  // causality chain
) {}

