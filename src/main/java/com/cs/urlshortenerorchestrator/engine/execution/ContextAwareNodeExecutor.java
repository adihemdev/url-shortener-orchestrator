package com.cs.urlshortenerorchestrator.engine.execution;

import com.cs.urlshortenerorchestrator.engine.domain.Execution;
import com.cs.urlshortenerorchestrator.engine.domain.WorkflowNode;

/**
 * Executor for agents that need workflow state and artifacts produced
 * by predecessor nodes.
 *
 * Existing NodeExecutor implementations remain supported.
 */
public interface ContextAwareNodeExecutor extends NodeExecutor {

    Execution execute(
            WorkflowNode node,
            int attemptNumber,
            ExecutionContext context
    ) throws InterruptedException;

    @Override
    default Execution execute(WorkflowNode node, int attemptNumber)
            throws InterruptedException {
        throw new UnsupportedOperationException(
                "ExecutionContext is required for this executor"
        );
    }
}