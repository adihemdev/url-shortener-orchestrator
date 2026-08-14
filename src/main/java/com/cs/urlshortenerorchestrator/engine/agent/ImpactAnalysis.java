package com.cs.urlshortenerorchestrator.engine.agent;

import java.util.List;

public record ImpactAnalysis(
        List<String> impactedFiles,
        List<String> preservedBehaviors,
        List<String> implementationSteps,
        List<String> testChanges,
        List<String> risks,
        String summary
) {}