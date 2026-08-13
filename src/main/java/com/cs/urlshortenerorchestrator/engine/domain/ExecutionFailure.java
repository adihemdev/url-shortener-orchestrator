package com.cs.urlshortenerorchestrator.engine.domain;

import java.time.Instant;

/**
 * Represents an execution failure with details.
 */
public record ExecutionFailure(
    String reason,
    Exception exception,
    Instant failedAt
) {}

