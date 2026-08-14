package com.cs.urlshortenerorchestrator.engine.domain;

import java.time.Instant;
import java.util.List;

/**
 * ValidationResult: outcome of validation/testing phase.
 */
public record ValidationResult(
    String id,
    String executedByNodeId,
    int totalTests,
    int passedTests,
    int failedTests,
    List<String> validatedArtifactIds,
    ValidationStatus status,
    String error,
    Instant executedAt
) {
    public double getPassRate() {
        if (totalTests == 0) return 0;
        return (double) passedTests / totalTests;
    }
}

