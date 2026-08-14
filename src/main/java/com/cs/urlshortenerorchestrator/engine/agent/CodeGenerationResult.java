package com.cs.urlshortenerorchestrator.engine.agent;

import java.util.List;

public record CodeGenerationResult(
        List<GeneratedFile> files,
        String summary
) {}