package com.cs.urlshortenerorchestrator.engine.planning;

import java.util.List;

public record WorkflowPlan(
        String normalizedRequirement,
        List<String> assumptions,
        List<String> acceptanceCriteria,
        List<PlannedNode> nodes
) {
}