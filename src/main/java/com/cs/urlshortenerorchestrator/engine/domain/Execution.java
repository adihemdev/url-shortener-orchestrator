package com.cs.urlshortenerorchestrator.engine.domain;

import java.time.Instant;
import java.util.List;
import java.util.ArrayList;
import lombok.Builder;
import lombok.Getter;

/**
 * Execution: a single attempt to execute a workflow node.
 * Immutable value object tracking execution details and artifacts.
 */
@Builder
@Getter
public class Execution {
    private final String id;
    private final String workflowId;
    private final String nodeId;
    private final int attemptNumber;
    private final ExecutionStatus status;
    private final Instant startedAt;
    private final Instant endedAt;
    @Builder.Default
    private final List<String> producedArtifactIds = new ArrayList<>();
    private final String errorDetails;
    @Builder.Default
    private final List<String> logs = new ArrayList<>();
    @Builder.Default
    private final List<String> decisionIds = new ArrayList<>();

    public long getDurationMs() {
        if (startedAt == null || endedAt == null) return 0;
        return endedAt.toEpochMilli() - startedAt.toEpochMilli();
    }
}

