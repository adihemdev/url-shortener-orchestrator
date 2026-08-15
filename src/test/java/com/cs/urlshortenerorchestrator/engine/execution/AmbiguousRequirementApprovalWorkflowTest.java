package com.cs.urlshortenerorchestrator.engine.execution;

import com.cs.urlshortenerorchestrator.engine.domain.ApprovalGate;
import com.cs.urlshortenerorchestrator.engine.domain.Execution;
import com.cs.urlshortenerorchestrator.engine.domain.ExecutionStatus;
import com.cs.urlshortenerorchestrator.engine.domain.NodeType;
import com.cs.urlshortenerorchestrator.engine.domain.Workflow;
import com.cs.urlshortenerorchestrator.engine.domain.WorkflowNode;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.assertj.core.api.Assertions.assertThat;

class AmbiguousRequirementApprovalWorkflowTest {

    @Test
    void ambiguousRequirementRequiresHumanClarificationBeforePlanningCanContinue()
            throws InterruptedException {

        /*
         * Stage 1:
         * ambiguity analysis has already determined that implementation
         * cannot safely proceed.
         */
        WorkflowNode ambiguityAnalysis =
                WorkflowNode.builder(
                                "ambiguity-analysis",
                                NodeType.REQUIREMENT_ANALYSIS
                        )
                        .description(
                                "Assess whether the incoming requirement is sufficiently specified"
                        )
                        .build();

        /*
         * Stage 2:
         * clarification is explicitly governed by a human approval gate.
         *
         * The node may execute its preparation work, but WorkflowExecutor
         * cannot mark it complete until the approval handler supplies the
         * stakeholder decision.
         */
        WorkflowNode clarification =
                WorkflowNode.builder(
                                "stakeholder-clarification",
                                NodeType.REQUIREMENT_ANALYSIS
                        )
                        .description(
                                "Resolve material ambiguity with stakeholder input"
                        )
                        .dependsOn(
                                "ambiguity-analysis"
                        )
                        .approvalGate(
                                ApprovalGate.builder(
                                                "ambiguity-resolution-approval"
                                        )
                                        .required(true)
                                        .approver(
                                                "PRODUCT_OWNER"
                                        )
                                        .description(
                                                "Stakeholder clarification required before implementation planning"
                                        )
                                        .build()
                        )
                        .build();

        /*
         * Stage 3:
         * this represents planning after the requirement has become
         * sufficiently concrete.
         */
        WorkflowNode implementationPlanning =
                WorkflowNode.builder(
                                "implementation-planning",
                                NodeType.ARCHITECTURE_DESIGN
                        )
                        .description(
                                "Create a bounded implementation plan from the clarified requirement"
                        )
                        .dependsOn(
                                "stakeholder-clarification"
                        )
                        .build();

        Workflow workflow =
                Workflow.builder(
                                "ambiguous-requirement-workflow",
                                "Ambiguous Requirement Workflow"
                        )
                        .description(
                                "Block ambiguous requirements until stakeholder clarification is supplied"
                        )
                        .root(
                                ambiguityAnalysis
                        )
                        .node(
                                clarification
                        )
                        .node(
                                implementationPlanning
                        )
                        .build();

        AtomicBoolean planningExecuted =
                new AtomicBoolean(false);

        /*
         * Execution itself is deterministic in this test.
         *
         * We are testing orchestration/governance behavior here,
         * not LLM quality. Live ambiguity detection is covered
         * separately by LiveAmbiguityAssessmentTest.
         */
        NodeExecutor nodeExecutor =
                (node, attemptNumber) -> {

                    if ("implementation-planning"
                            .equals(node.getId())) {

                        planningExecuted.set(true);
                    }

                    return Execution.builder()
                            .id(
                                    "exec-"
                                            + node.getId()
                                            + "-"
                                            + attemptNumber
                            )
                            .workflowId(
                                    workflow.getId()
                            )
                            .nodeId(
                                    node.getId()
                            )
                            .attemptNumber(
                                    attemptNumber
                            )
                            .status(
                                    ExecutionStatus.SUCCESS
                            )
                            .startedAt(
                                    Instant.now()
                            )
                            .endedAt(
                                    Instant.now()
                            )
                            .build();
                };

        AtomicBoolean clarificationRequested =
                new AtomicBoolean(false);

        ApprovalHandlerInterface approvalHandler =
                (node, execution) -> {

                    assertThat(node.getId())
                            .isEqualTo(
                                    "stakeholder-clarification"
                            );

                    clarificationRequested.set(true);

                    /*
                     * Simulated human clarification.
                     *
                     * The approval reason carries the stakeholder decision
                     * that resolves the ambiguity.
                     */
                    return ApprovalResult.builder()
                            .approved(true)
                            .approver(
                                    "PRODUCT_OWNER"
                            )
                            .reason(
                                    """
                                    Add the ability to list shortened URLs.
                                    Return short code, destination URL and creation time.
                                    Do not add editing or expiration behavior.
                                    No authentication or ownership changes are in scope.
                                    Preserve existing POST, GET and DELETE behavior.
                                    """
                            )
                            .approvalTimeMs(25)
                            .build();
                };

        WorkflowExecutor executor =
                new WorkflowExecutor(
                        workflow
                );

        WorkflowExecutor.ExecutionResult result =
                executor.execute(
                        nodeExecutor,
                        approvalHandler
                );

        /*
         * Human clarification was actually required.
         */
        assertThat(
                clarificationRequested
        ).isTrue();

        /*
         * The workflow was allowed to continue only after the
         * clarification approval was granted.
         */
        assertThat(
                planningExecuted
        ).isTrue();

        assertThat(
                workflow.getCurrentState()
                        .getCompletedNodeIds()
        ).contains(
                "ambiguity-analysis",
                "stakeholder-clarification",
                "implementation-planning"
        );

        assertThat(
                result.isSuccess()
        ).isTrue();

        /*
         * WorkflowExecutor records an audit event for the approval.
         * Its existing approval path also records the corresponding
         * approval decision through DecisionRecorder.
         */
        assertThat(
                executor.getAuditTrail()
                        .stream()
                        .anyMatch(entry ->
                                "APPROVAL_GRANTED".equals(
                                        entry.getEventType()
                                )
                        )
        ).isTrue();
    }
}