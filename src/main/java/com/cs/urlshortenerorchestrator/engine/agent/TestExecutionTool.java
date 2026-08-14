package com.cs.urlshortenerorchestrator.engine.agent;

import com.cs.urlshortenerorchestrator.engine.domain.ValidationResult;

public interface TestExecutionTool {

    ValidationResult runTests(
            String nodeId,
            TestExecutionRequest request
    );
}