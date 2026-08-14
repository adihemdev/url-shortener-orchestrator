package com.cs.urlshortenerorchestrator.engine.planning;

import com.cs.urlshortenerorchestrator.engine.agent.AgentClient;
import com.cs.urlshortenerorchestrator.engine.agent.AgentResponse;


import tools.jackson.databind.ObjectMapper;


import java.util.List;
import java.util.Objects;
import java.util.Set;

public class ModelBackedWorkflowPlanner implements WorkflowPlanner {

    private final AgentClient agentClient;
    private final tools.jackson.databind.ObjectMapper objectMapper;

    public ModelBackedWorkflowPlanner(
            AgentClient agentClient,
            ObjectMapper objectMapper) {

        this.agentClient =
                Objects.requireNonNull(agentClient, "agentClient required");

        this.objectMapper =
                Objects.requireNonNull(objectMapper, "objectMapper required");
    }

    @Override
    public WorkflowPlan plan(String requirement) {

        if (requirement == null || requirement.isBlank()) {
            throw new IllegalArgumentException("requirement required");
        }

        String systemPrompt = """
            You are a senior software engineering planning agent operating
            inside a governed SDLC orchestration engine.

            Convert the user's engineering requirement into an executable
            workflow plan.

            You may ONLY use these node types:

            REQUIREMENT_ANALYSIS
            ARCHITECTURE_DESIGN
            TEST_PLANNING
            IMPLEMENTATION
            TESTING
            VALIDATION
            RELEASE_READY
            SYNCHRONIZATION

            Requirements:

            - Produce a dependency DAG with exactly one root node.
            - REQUIREMENT_ANALYSIS should normally be the root.
            - Use parallel branches when work is genuinely independent; architecture
              design and test planning should normally run in parallel after requirements.
            - Use SYNCHRONIZATION when parallel outputs must converge.
            - IMPLEMENTATION must not precede required design inputs.
            - TESTING must depend on implementation.
            - VALIDATION must occur before RELEASE_READY.
            - High-impact release actions require human approval.
            - Do not introduce external infrastructure unless justified by the requirement;
              prefer simple in-memory/local components for this prototype.
            - Keep the plan appropriate for a production-quality prototype.
            - Capture assumptions explicitly.
            - Capture testable acceptance criteria explicitly.
            - Avoid unnecessary task fragmentation; prefer roughly 7-10 nodes unless
              additional decomposition adds meaningful dependency or parallelism.

            Return ONLY JSON matching this exact structure:

            {
              "normalizedRequirement": "...",
              "assumptions": ["..."],
              "acceptanceCriteria": ["..."],
              "nodes": [
                {
                  "id": "...",
                  "type": "REQUIREMENT_ANALYSIS",
                  "description": "...",
                  "dependsOnNodeIds": [],
                  "approvalRequired": false
                }
              ]
            }

            Do not include markdown fences or explanatory prose.
            """;

        AgentResponse response =
                agentClient.execute(systemPrompt, requirement);

        WorkflowPlan plan;

        try {
            plan = objectMapper.readValue(
                    response.content(),
                    WorkflowPlan.class
            );
        } catch (Exception e) {
            throw new IllegalStateException(
                    "Planning agent returned invalid workflow JSON",
                    e
            );
        }

        validateAgentPlan(plan);

        return plan;
    }

    private void validateAgentPlan(WorkflowPlan plan) {

        if (plan == null) {
            throw new IllegalArgumentException(
                    "Planning agent returned no plan");
        }

        if (plan.normalizedRequirement() == null
                || plan.normalizedRequirement().isBlank()) {
            throw new IllegalArgumentException(
                    "Normalized requirement required");
        }

        if (plan.nodes() == null || plan.nodes().isEmpty()) {
            throw new IllegalArgumentException(
                    "Planning agent must produce workflow nodes");
        }

        long roots = plan.nodes().stream()
                .filter(node ->
                        node.dependsOnNodeIds() == null
                                || node.dependsOnNodeIds().isEmpty())
                .count();

        if (roots != 1) {
            throw new IllegalArgumentException(
                    "Agent plan must contain exactly one root node");
        }

        Set<String> nodeIds = plan.nodes().stream()
                .map(PlannedNode::id)
                .collect(java.util.stream.Collectors.toSet());

        if (nodeIds.size() != plan.nodes().size()) {
            throw new IllegalArgumentException(
                    "Agent plan contains duplicate node IDs");
        }

        for (PlannedNode node : plan.nodes()) {

            if (node.id() == null || node.id().isBlank()) {
                throw new IllegalArgumentException(
                        "Every planned node requires an ID");
            }

            if (node.type() == null) {
                throw new IllegalArgumentException(
                        "Every planned node requires a supported NodeType");
            }

            if (node.dependsOnNodeIds() != null) {
                for (String dependency : node.dependsOnNodeIds()) {
                    if (!nodeIds.contains(dependency)) {
                        throw new IllegalArgumentException(
                                "Unknown dependency "
                                        + dependency
                                        + " referenced by "
                                        + node.id()
                        );
                    }
                }
            }
        }

        boolean releaseWithoutApproval =
                plan.nodes().stream()
                        .anyMatch(node ->
                                node.type()
                                        == com.cs.urlshortenerorchestrator.engine.domain.NodeType.RELEASE_READY
                                        && !node.approvalRequired());

        if (releaseWithoutApproval) {
            throw new IllegalArgumentException(
                    "Release-ready nodes require human approval");
        }
    }
}