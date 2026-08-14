package com.cs.urlshortenerorchestrator.engine.domain;

/**
 * DecisionType enum: types of decisions made during workflow execution.
 */
public enum DecisionType {
    ARCHITECTURE_CHOICE, IMPLEMENTATION_STRATEGY, RETRY_ON_FAILURE,
    REPLAN, APPROVAL_DECISION, ROLLBACK_DECISION, SAFE_STOP_DECISION
}

