package com.cs.urlshortenerorchestrator.engine.domain;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

/**
 * ExecutionMetrics: aggregated metrics from workflow execution.
 * Continuously updated as execution progresses.
 */
public class ExecutionMetrics {
    private int totalNodes;
    private int completedNodes;
    private int failedNodes;
    private int retriedNodes;
    private int rolledBackNodes;
    private final List<Long> approvalWaitTimes = new ArrayList<>();
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

    public long getAverageMTTRMs() {
        if (retriedNodes == 0) return 0;
        long totalApprovalWaitTime = approvalWaitTimes.stream().mapToLong(Long::longValue).sum();
        return totalApprovalWaitTime / retriedNodes;
    }

    // Getters and adders
    public int getTotalNodes() { return totalNodes; }
    public void setTotalNodes(int total) { this.totalNodes = total; }

    public int getCompletedNodes() { return completedNodes; }
    public void incrementCompletedNodes() { this.completedNodes++; }

    public int getFailedNodes() { return failedNodes; }
    public void incrementFailedNodes() { this.failedNodes++; }

    public int getRetriedNodes() { return retriedNodes; }
    public void incrementRetriedNodes() { this.retriedNodes++; }

    public int getRolledBackNodes() { return rolledBackNodes; }
    public void incrementRolledBackNodes() { this.rolledBackNodes++; }

    public void recordApprovalWaitTime(long ms) {
        approvalWaitTimes.add(ms);
    }

    public Instant getWorkflowStartedAt() { return workflowStartedAt; }
    public void setWorkflowStartedAt(Instant instant) { this.workflowStartedAt = instant; }

    public Instant getWorkflowEndedAt() { return workflowEndedAt; }
    public void setWorkflowEndedAt(Instant instant) { this.workflowEndedAt = instant; }
}

