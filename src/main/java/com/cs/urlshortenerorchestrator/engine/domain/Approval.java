package com.cs.urlshortenerorchestrator.engine.domain;

import java.time.Instant;

/**
 * Approval: human approval gate record.
 */
public record Approval(
    String id,
    String nodeId,
    String gateName,
    ApprovalStatus status,
    String approvedBy,
    Instant requestedAt,
    Instant respondedAt,
    String reason
) {}

