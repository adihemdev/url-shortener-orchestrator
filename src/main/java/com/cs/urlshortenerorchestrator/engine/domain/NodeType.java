package com.cs.urlshortenerorchestrator.engine.domain;

/**
 * Represents the type of node in a workflow DAG.
 * Each type represents a stage in the SDLC.
 */
public enum NodeType {
    REQUIREMENT_ANALYSIS,
    ARCHITECTURE_DESIGN,
    TEST_PLANNING,
    IMPLEMENTATION,
    TESTING,
    VALIDATION,
    RELEASE_READY,
    SYNCHRONIZATION
}

