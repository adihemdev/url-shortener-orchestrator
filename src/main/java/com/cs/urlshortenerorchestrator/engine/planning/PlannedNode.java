package com.cs.urlshortenerorchestrator.engine.planning;

import com.cs.urlshortenerorchestrator.engine.domain.NodeType;

import java.util.Set;

public record PlannedNode(
        String id,
        NodeType type,
        String description,
        Set<String> dependsOnNodeIds,
        boolean approvalRequired
) {
}