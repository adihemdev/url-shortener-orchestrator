package com.cs.urlshortenerorchestrator.engine.domain;

/**
 * Backoff strategy for retries.
 */
public enum BackoffStrategy {
    EXPONENTIAL, LINEAR, FIXED
}

