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
     * Note: This is a Recovery Delay Proxy measuring the average time added by
     * retries and rollbacks, rather than wall-clock service restoration time.
     */
    public long getAverageMTTRMs() {
        if (retriedNodes == 0 && rolledBackNodes == 0) return 0;
        long totalWait = retryDelayTotals.stream().mapToLong(Long::longValue).sum();
        int recoveryCount = retriedNodes + rolledBackNodes;
        return recoveryCount > 0 ? totalWait / recoveryCount : 0;
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
}

