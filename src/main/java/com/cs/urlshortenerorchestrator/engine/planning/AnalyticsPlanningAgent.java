package com.cs.urlshortenerorchestrator.engine.planning;

import com.cs.urlshortenerorchestrator.engine.domain.NodeType;
import com.cs.urlshortenerorchestrator.engine.planning.PlannedNode;
import com.cs.urlshortenerorchestrator.engine.planning.WorkflowPlan;
import com.cs.urlshortenerorchestrator.engine.planning.WorkflowPlanner;

import java.util.List;
import java.util.Set;

/**
 * Bounded planning agent for the greenfield Analytics scenario.
 *
 * Converts a raw Analytics requirement into a structured workflow plan
 * using only supported SDLC node types and explicit dependencies.
 */
public class AnalyticsPlanningAgent implements WorkflowPlanner {

    @Override
    public WorkflowPlan plan(String requirement) {
        if (requirement == null || requirement.isBlank()) {
            throw new IllegalArgumentException("requirement required");
        }

        String normalizedRequirement =
                "Build a greenfield URL analytics capability that consumes click events, " +
                        "aggregates usage statistics, detects simple traffic spikes, exposes dashboard data, " +
                        "and produces a validated release-ready engineering outcome.";

        List<String> assumptions = List.of(
                "Analytics is independent from the existing URL-shortener implementation",
                "Event storage is in-memory for the prototype",
                "Spike detection uses a deterministic threshold-based rule",
                "No Kafka, external database, distributed deployment, or frontend UI is required",
                "Human approval is required before release readiness"
        );

        List<String> acceptanceCriteria = List.of(
                "Click events can be accepted and stored",
                "Click counts can be aggregated",
                "A deterministic traffic spike can be detected",
                "Dashboard data can be produced as JSON",
                "Analytics tests pass",
                "Validation succeeds before release",
                "Release approval is recorded"
        );

        List<PlannedNode> nodes = List.of(
                new PlannedNode(
                        "requirements",
                        NodeType.REQUIREMENT_ANALYSIS,
                        "Normalize Analytics requirements, assumptions and acceptance criteria",
                        Set.of(),
                        false
                ),
                new PlannedNode(
                        "architecture",
                        NodeType.ARCHITECTURE_DESIGN,
                        "Design Analytics components, APIs and data flow",
                        Set.of("requirements"),
                        false
                ),
                new PlannedNode(
                        "test-plan",
                        NodeType.TEST_PLANNING,
                        "Define Analytics unit, integration and acceptance tests",
                        Set.of("requirements"),
                        false
                ),
                new PlannedNode(
                        "sync",
                        NodeType.SYNCHRONIZATION,
                        "Synchronize architecture and test planning outputs",
                        Set.of("architecture", "test-plan"),
                        false
                ),
                new PlannedNode(
                        "implementation",
                        NodeType.IMPLEMENTATION,
                        "Create the Analytics implementation using approved upstream artifacts",
                        Set.of("sync"),
                        false
                ),
                new PlannedNode(
                        "testing",
                        NodeType.TESTING,
                        "Execute Analytics tests against the generated implementation",
                        Set.of("implementation"),
                        false
                ),
                new PlannedNode(
                        "validation",
                        NodeType.VALIDATION,
                        "Validate implementation against requirements and acceptance criteria",
                        Set.of("testing"),
                        false
                ),
                new PlannedNode(
                        "release",
                        NodeType.RELEASE_READY,
                        "Produce a reviewable release-ready Analytics outcome",
                        Set.of("validation"),
                        true
                )
        );

        return new WorkflowPlan(
                normalizedRequirement,
                assumptions,
                acceptanceCriteria,
                nodes
        );
    }
}