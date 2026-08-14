package com.cs.urlshortenerorchestrator.engine.agent;

import com.cs.urlshortenerorchestrator.engine.domain.ValidationStatus;

import java.util.List;
import java.util.Map;

public record ValidationAssessment(
        ValidationStatus status,
        Map<String, String> criteriaResults,
        List<String> gaps,
        String summary
) {}