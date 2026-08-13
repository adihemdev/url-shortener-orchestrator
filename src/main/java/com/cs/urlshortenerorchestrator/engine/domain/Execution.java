package com.cs.urlshortenerorchestrator.engine.domain;

import java.time.Instant;
import java.util.List;

/**
 * Execution: a single attempt to execute a workflow node.
 */
public record Execution(
    String id,
    String workflowId,
    String nodeId,
    int attemptNumber,
    ExecutionStatus status,
    Instant startedAt,
    Instant endedAt,
    List<String> producedArtifactIds,  // artifact IDs produced
    String errorDetails,
    List<String> logs,
    List<String> decisionIds  // decisions made during this execution
) {
    public long getDurationMs() {
        if (startedAt == null || endedAt == null) return 0;
        return endedAt.toEpochMilli() - startedAt.toEpochMilli();
    }
}

