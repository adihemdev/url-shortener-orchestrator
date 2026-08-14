package com.cs.urlshortenerorchestrator.engine.execution;

import com.cs.urlshortenerorchestrator.engine.domain.Execution;
import com.cs.urlshortenerorchestrator.engine.domain.WorkflowNode;

/**
 * ApprovalHandler: Manages human-in-the-loop approval checkpoints.
 * Demonstrates governance and human oversight.
 */
public interface ApprovalHandlerInterface {
    /**
     * Request approval for a node execution.
     * Blocks until approval received or timeout.
     */
    ApprovalResult requestApproval(WorkflowNode node, Execution execution)
            throws WorkflowExecutor.ApprovalTimeoutException, InterruptedException;
}

/**
 * Default implementation for testing and demonstration.
 */
class DefaultApprovalHandler implements ApprovalHandlerInterface {
    @Override
    public ApprovalResult requestApproval(WorkflowNode node, Execution execution)
            throws WorkflowExecutor.ApprovalTimeoutException {
        // Auto-approve for testing
        return ApprovalResult.builder()
            .approved(true)
            .approver("SYSTEM")
            .reason("Auto-approved for testing")
            .approvalTimeMs(0)
            .build();
    }
}
