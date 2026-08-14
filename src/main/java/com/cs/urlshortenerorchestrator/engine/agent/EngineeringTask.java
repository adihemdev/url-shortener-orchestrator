package com.cs.urlshortenerorchestrator.engine.agent;

import com.cs.urlshortenerorchestrator.engine.domain.Artifact;

import java.util.List;

public record EngineeringTask(
        AgentRole role,
        String objective,
        List<Artifact> upstreamArtifacts,
        List<String> constraints,
        String allowedPackageRoot
) {}