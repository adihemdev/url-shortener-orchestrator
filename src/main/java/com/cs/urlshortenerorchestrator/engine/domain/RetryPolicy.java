package com.cs.urlshortenerorchestrator.engine.domain;

import java.util.List;
import java.util.Objects;
import lombok.Getter;

/**
 * RetryPolicy: bounded retry configuration with backoff strategy.
 */
public class RetryPolicy {
    @Getter
    private final int maxRetries;
    @Getter
    private final BackoffStrategy backoffStrategy;
    @Getter
    private final int initialDelaySeconds;
    @Getter
    private final int maxDelaySeconds;
    @Getter
    private final List<String> retryOnExceptions;
    @Getter
    private final List<String> doNotRetryOnExceptions;
    @Getter
    private final int maxDurationSeconds;

    public RetryPolicy(int maxRetries, BackoffStrategy backoffStrategy, int initialDelaySeconds,
                      int maxDelaySeconds, List<String> retryOnExceptions,
                      List<String> doNotRetryOnExceptions, int maxDurationSeconds) {
        this.maxRetries = maxRetries;
        this.backoffStrategy = Objects.requireNonNull(backoffStrategy, "backoffStrategy required");
        this.initialDelaySeconds = initialDelaySeconds;
        this.maxDelaySeconds = maxDelaySeconds;
        this.retryOnExceptions = retryOnExceptions != null ? retryOnExceptions : List.of();
        this.doNotRetryOnExceptions = doNotRetryOnExceptions != null ? doNotRetryOnExceptions : List.of();
        this.maxDurationSeconds = maxDurationSeconds;
    }


    /**
     * Calculate delay before attempt based on backoff strategy.
     */
    public int calculateDelaySeconds(int attemptNumber) {
        if (attemptNumber <= 0) return 0;

        int delay = switch (backoffStrategy) {
            case FIXED -> initialDelaySeconds;
            case LINEAR -> initialDelaySeconds * attemptNumber;
            case EXPONENTIAL -> (int) Math.min(
                initialDelaySeconds * Math.pow(2, attemptNumber - 1),
                maxDelaySeconds
            );
        };

        return Math.min(delay, maxDelaySeconds);
    }

    public boolean shouldRetry(String exceptionType, int attemptsSoFar) {
        if (attemptsSoFar >= maxRetries) return false;
        if (doNotRetryOnExceptions.contains(exceptionType)) return false;
        return retryOnExceptions.isEmpty() || retryOnExceptions.contains(exceptionType);
    }

    public static RetryPolicy noRetry() {
        return new RetryPolicy(1, BackoffStrategy.FIXED, 0, 0, List.of(), List.of(), 0);
    }

    public static RetryBuilder builder() {
        return new RetryBuilder();
    }

    public static class RetryBuilder {
        private int maxRetries = 3;
        private BackoffStrategy backoffStrategy = BackoffStrategy.EXPONENTIAL;
        private int initialDelaySeconds = 1;
        private int maxDelaySeconds = 60;
        private List<String> retryOnExceptions = List.of();
        private List<String> doNotRetryOnExceptions = List.of();
        private int maxDurationSeconds = 600;

        public RetryBuilder maxRetries(int max) {
            this.maxRetries = max;
            return this;
        }

        public RetryBuilder backoff(BackoffStrategy strategy) {
            this.backoffStrategy = strategy;
            return this;
        }

        public RetryBuilder initialDelay(int seconds) {
            this.initialDelaySeconds = seconds;
            return this;
        }

        public RetryBuilder maxDelay(int seconds) {
            this.maxDelaySeconds = seconds;
            return this;
        }

        public RetryBuilder retryOn(List<String> exceptions) {
            this.retryOnExceptions = exceptions;
            return this;
        }

        public RetryBuilder doNotRetryOn(List<String> exceptions) {
            this.doNotRetryOnExceptions = exceptions;
            return this;
        }

        public RetryBuilder maxDuration(int seconds) {
            this.maxDurationSeconds = seconds;
            return this;
        }

        public RetryPolicy build() {
            return new RetryPolicy(maxRetries, backoffStrategy, initialDelaySeconds,
                maxDelaySeconds, retryOnExceptions, doNotRetryOnExceptions, maxDurationSeconds);
        }
    }
}

