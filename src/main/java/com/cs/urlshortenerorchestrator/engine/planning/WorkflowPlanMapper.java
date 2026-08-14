package com.cs.urlshortenerorchestrator.engine.planning;

import com.cs.urlshortenerorchestrator.engine.domain.*;
import com.cs.urlshortenerorchestrator.engine.execution.ExecutionContext;

import java.util.List;

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

        if (plannedNode.type() == NodeType.TESTING) {

            ValidationRule testsPassedRule =
                    new ValidationRule(
                            "generated_tests_pass",
                            ValidationRule.RuleType.CUSTOM,
                            "analytics-test-execution-v1",
                            value -> {

                                if (!(value instanceof ExecutionContext context)) {
                                    return false;
                                }

                                Artifact artifact =
                                        context.getArtifactById(
                                                "analytics-test-execution-v1"
                                        );

                                return artifact != null
                                        && "PASS".equals(
                                        artifact.metadata()
                                                .get("status")
                                );
                            },
                            ValidationRule.Severity.ERROR,
                            "Generated Analytics tests must execute successfully"
                    );

            builder.exitGate(
                    Gate.builder(
                                    plannedNode.id()
                                            + "-testing-gate"
                            )
                            .validation()
                            .validationRules(
                                    List.of(testsPassedRule)
                            )
                            .failureAction(
                                    Gate.FailureAction.TRIGGER_REPLAN
                            )
                            .build()
            );

            builder.replanTrigger(
                    ReplanTrigger.builder(
                                    plannedNode.id()
                                            + "-testing-replan",
                                    ReplanTrigger.TriggerType.REGRESSION_DETECTED,
                                    "testing"
                            )
                            .maxReplans(1)
                            .reason(
                                    "Generated test suite failed and requires repair"
                            )
                            .build()
            );
        }

        if (plannedNode.type() == NodeType.VALIDATION) {

            ValidationRule validationPassedRule =
                    new ValidationRule(
                            "analytics_acceptance_validation",
                            ValidationRule.RuleType.CUSTOM,
                            "analytics-validation-v1",
                            value -> {
                                if (!(value instanceof ExecutionContext context)) {
                                    return false;
                                }

                                Artifact artifact =
                                        context.getArtifactById(
                                                "analytics-validation-v1"
                                        );

                                return artifact != null
                                        && "PASS".equals(
                                        artifact.metadata()
                                                .get("status")
                                );
                            },
                            ValidationRule.Severity.ERROR,
                            "Analytics acceptance validation must pass"
                    );

            builder.exitGate(
                    Gate.builder(
                                    plannedNode.id()
                                            + "-validation-gate"
                            )
                            .validation()
                            .validationRules(
                                    List.of(validationPassedRule)
                            )
                            .failureAction(
                                    Gate.FailureAction.TRIGGER_REPLAN
                            )
                            .build()
            );

            builder.replanTrigger(
                    ReplanTrigger.builder(
                                    plannedNode.id() + "-replan",
                                    ReplanTrigger.TriggerType.VALIDATION_FAILED,
                                    "implementation"
                            )
                            .maxReplans(2)
                            .reason(
                                    "Validation failure requires implementation repair"
                            )
                            .build()
            );
        }

        return builder.build();
    }
}