package com.cs.urlshortenerorchestrator.engine.agent;

import java.util.List;

public record AmbiguityAssessment(
        boolean sufficientlySpecified,
        boolean implementationBlocked,
        List<String> knownFacts,
        List<String> ambiguities,
        List<String> clarificationQuestions,
        List<String> unsafeAssumptions,
        String summary
) {}