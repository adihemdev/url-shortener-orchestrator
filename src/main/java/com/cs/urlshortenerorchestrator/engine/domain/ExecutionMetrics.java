package com.cs.urlshortenerorchestrator.engine.domain;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import lombok.Getter;
import lombok.Setter;

/**
 * ExecutionMetrics: aggregated metrics from workflow execution.
 * Continuously updated as execution progresses (success rate, latency, MTTR, etc.)
 */
@Getter
@Setter
public class ExecutionMetrics {
    private int totalNodes;
    private int completedNodes;
    private int failedNodes;
    private int retriedNodes;
    private int rolledBackNodes;
    private int replannedCount;
    private int fallbackCount;
    private long totalRecoveryDurationMs;
    private int recoveredNodesCount;
    private final List<Long> nodeLatencies = new ArrayList<>();
    private final List<Long> approvalWaitTimes = new ArrayList<>();
    private final List<Long> retryDelayTotals = new ArrayList<>();
    private Instant workflowStartedAt;
    private Instant workflowEndedAt;

    public double getSuccessRate() {
        if (completedNodes + failedNodes == 0) return 0;
        return (double) completedNodes / (completedNodes + failedNodes);
    }

    public long getE2ELatencyMs() {
        if (workflowStartedAt == null || workflowEndedAt == null) return 0;
        return workflowEndedAt.toEpochMilli() - workflowStartedAt.toEpochMilli();
    }

    /**
     * Calculates the Mean Time To Recovery (MTTR).
     * Measures the average wall-clock duration from first failure detection
     * until successful recovery.
     */
    public long getAverageMTTRMs() {
        if (recoveredNodesCount == 0) return 0;
        return totalRecoveryDurationMs / recoveredNodesCount;
    }

    public long getAverageNodeLatencyMs() {
        if (nodeLatencies.isEmpty()) return 0;
        return nodeLatencies.stream().mapToLong(Long::longValue).sum() / nodeLatencies.size();
    }

    public double getRetryFrequency() {
        if (totalNodes == 0) return 0;
        return (double) retriedNodes / totalNodes;
    }

    public double getRollbackFrequency() {
        if (totalNodes == 0) return 0;
        return (double) rolledBackNodes / totalNodes;
    }

    public void recordApprovalWaitTime(long ms) {
        approvalWaitTimes.add(ms);
    }

    public void recordNodeLatency(long ms) {
        nodeLatencies.add(ms);
    }

    public void recordRetryDelayTotal(long ms) {
        retryDelayTotals.add(ms);
    }

    public void recordRecovery(long durationMs) {
        this.totalRecoveryDurationMs += durationMs;
        this.recoveredNodesCount++;
    }

    public long getTotalApprovalWaitTimeMs() {
        return approvalWaitTimes.stream().mapToLong(Long::longValue).sum();
    }

    public void incrementCompletedNodes() {
        this.completedNodes++;
    }

    public void incrementFailedNodes() {
        this.failedNodes++;
    }

    public void incrementRetriedNodes() {
        this.retriedNodes++;
    }

    public void incrementRolledBackNodes() {
        this.rolledBackNodes++;
    }

    public void incrementReplannedCount() {
        this.replannedCount++;
    }

    public void incrementFallbackCount() {
        this.fallbackCount++;
    }
}

