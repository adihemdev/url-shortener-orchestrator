package com.cs.urlshortenerorchestrator.engine.domain;

import java.util.List;

/**
 * RetryPolicy: bounded retry configuration for a node.
 */
public record RetryPolicy(
    int maxRetries,
    BackoffStrategy backoffStrategy,
    List<String> retryOnExceptions,  // which exceptions trigger retry
    int maxDurationSeconds
) {}

