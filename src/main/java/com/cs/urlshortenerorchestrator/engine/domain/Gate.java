package com.cs.urlshortenerorchestrator.engine.domain;

import java.util.List;

/**
 * Gate: precondition or postcondition logic for a node.
 */
public record Gate(
    String name,
    List<String> requiredArtifactTypes,  // what artifacts must exist
    List<String> requiredPolicies,       // which policies must be satisfied
    String description
) {}

