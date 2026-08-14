package com.cs.urlshortenerorchestrator.engine.planning;

import com.cs.urlshortenerorchestrator.engine.agent.AgentClient;
import com.cs.urlshortenerorchestrator.engine.agent.AgentResponse;
import com.cs.urlshortenerorchestrator.engine.domain.NodeType;

import org.junit.jupiter.api.Test;
import tools.jackson.databind.ObjectMapper;

import static org.assertj.core.api.Assertions.assertThat;

class ModelBackedWorkflowPlannerTest {

    @Test
    void convertsAgentResponseIntoGovernedWorkflowPlan() {

        String modelOutput = """
            {
              "normalizedRequirement":
                "Build click analytics for shortened URLs",
              "assumptions": [
                "Prototype uses in-memory storage"
              ],
              "acceptanceCriteria": [
                "Click events are aggregated",
                "Analytics output is validated"
              ],
              "nodes": [
                {
                  "id": "requirements",
                  "type": "REQUIREMENT_ANALYSIS",
                  "description": "Normalize requirements",
                  "dependsOnNodeIds": [],
                  "approvalRequired": false
                },
                {
                  "id": "implementation",
                  "type": "IMPLEMENTATION",
                  "description": "Implement analytics",
                  "dependsOnNodeIds": ["requirements"],
                  "approvalRequired": false
                },
                {
                  "id": "testing",
                  "type": "TESTING",
                  "description": "Test analytics",
                  "dependsOnNodeIds": ["implementation"],
                  "approvalRequired": false
                },
                {
                  "id": "validation",
                  "type": "VALIDATION",
                  "description": "Validate analytics",
                  "dependsOnNodeIds": ["testing"],
                  "approvalRequired": false
                },
                {
                  "id": "release",
                  "type": "RELEASE_READY",
                  "description": "Release analytics",
                  "dependsOnNodeIds": ["validation"],
                  "approvalRequired": true
                }
              ]
            }
            """;

        AgentClient fakeClient =
                (systemPrompt, userPrompt) ->
                        new AgentResponse(
                                modelOutput,
                                "test-model",
                                100,
                                200
                        );

        ModelBackedWorkflowPlanner planner =
                new ModelBackedWorkflowPlanner(
                        fakeClient,
                        new ObjectMapper()
                );

        WorkflowPlan plan =
                planner.plan(
                        "Build analytics for shortened URLs"
                );

        assertThat(plan.normalizedRequirement())
                .contains("click analytics");

        assertThat(plan.nodes())
                .hasSize(5);

        assertThat(plan.nodes())
                .extracting(PlannedNode::type)
                .contains(
                        NodeType.REQUIREMENT_ANALYSIS,
                        NodeType.IMPLEMENTATION,
                        NodeType.TESTING,
                        NodeType.VALIDATION,
                        NodeType.RELEASE_READY
                );
    }
}