package com.cs.urlshortenerorchestrator.engine.planning;

import com.cs.urlshortenerorchestrator.engine.domain.ApprovalGate;
import com.cs.urlshortenerorchestrator.engine.domain.Workflow;
import com.cs.urlshortenerorchestrator.engine.domain.WorkflowNode;

public class WorkflowPlanMapper {

    public Workflow toWorkflow(
            String workflowId,
            String workflowName,
            WorkflowPlan plan) {

        if (plan.nodes() == null || plan.nodes().isEmpty()) {
            throw new IllegalArgumentException("Workflow plan must contain nodes");
        }

        PlannedNode rootPlan = plan.nodes().stream()
                .filter(node -> node.dependsOnNodeIds() == null
                        || node.dependsOnNodeIds().isEmpty())
                .findFirst()
                .orElseThrow(() ->
                        new IllegalArgumentException("Workflow plan requires a root node"));

        WorkflowNode root = toWorkflowNode(rootPlan);

        Workflow.Builder builder = Workflow.builder(workflowId, workflowName)
                .description(plan.normalizedRequirement())
                .root(root);

        for (PlannedNode plannedNode : plan.nodes()) {
            if (!plannedNode.id().equals(rootPlan.id())) {
                builder.node(toWorkflowNode(plannedNode));
            }
        }

        return builder.build();
    }

    private WorkflowNode toWorkflowNode(PlannedNode plannedNode) {

        WorkflowNode.Builder builder = WorkflowNode.builder(
                        plannedNode.id(),
                        plannedNode.type())
                .description(plannedNode.description());

        if (plannedNode.dependsOnNodeIds() != null
                && !plannedNode.dependsOnNodeIds().isEmpty()) {

            builder.dependsOn(
                    plannedNode.dependsOnNodeIds().toArray(String[]::new)
            );
        }

        if (plannedNode.approvalRequired()) {
            builder.approvalGate(
                    ApprovalGate.builder(plannedNode.id() + "-approval")
                            .required(true)
                            .approver("ENGINEERING_REVIEWER")
                            .description(
                                    "Human approval required for " + plannedNode.id())
                            .build()
            );
        }

        return builder.build();
    }
}