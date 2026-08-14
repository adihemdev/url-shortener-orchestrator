package com.cs.urlshortenerorchestrator.engine.agent;

import java.util.List;

public record TestExecutionRequest(
        List<String> command,
        List<String> testTargets
) {}