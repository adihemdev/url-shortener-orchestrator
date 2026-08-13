package com.cs.urlshortenerorchestrator.engine.domain;

import java.util.List;

/**
 * RollbackPolicy: defines how to revert changes.
 */
public record RollbackPolicy(
    List<String> reversibleOperations,  // git reset, schema rollback, etc.
    List<String> rollbackTriggers,      // which failures trigger auto-rollback
    boolean requiresApproval
) {}

