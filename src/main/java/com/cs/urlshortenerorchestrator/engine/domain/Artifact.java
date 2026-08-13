package com.cs.urlshortenerorchestrator.engine.domain;

import java.time.Instant;
import java.util.Map;

/**
 * Artifact: any output produced by a node execution (code, schema, test, doc, etc.).
 */
public record Artifact(
    String id,
    ArtifactType type,
    String name,
    String producedByNodeId,
    String producedByExecutionId,
    String storageLocation,  // file path or git commit hash
    Map<String, String> metadata,
    Instant createdAt
) {}

