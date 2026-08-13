package com.cs.urlshortenerorchestrator.engine.domain;

import java.util.List;
import java.util.Set;

/**
 * Policy: governance rule that can constrain execution or require approval.
 */
public record Policy(
    String id,
    String name,
    String description,
    List<String> rules,        // semantic rules (e.g., "schema changes need DBA approval")
    boolean enforceable,       // can engine block execution
    Set<NodeType> appliesToNodeTypes
) {}

