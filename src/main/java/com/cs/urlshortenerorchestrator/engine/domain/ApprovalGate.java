package com.cs.urlshortenerorchestrator.engine.domain;

import java.util.List;

/**
 * ApprovalGate: explicit human approval required.
 */
public record ApprovalGate(
    String gateName,
    String description,
    List<String> requiredRoles  // who can approve (e.g., ["TECH_LEAD", "ARCHITECT"])
) {}

