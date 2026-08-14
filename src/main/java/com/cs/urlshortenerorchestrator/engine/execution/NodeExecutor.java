package com.cs.urlshortenerorchestrator.engine.execution;

import com.cs.urlshortenerorchestrator.engine.domain.Execution;
import com.cs.urlshortenerorchestrator.engine.domain.ExecutionStatus;
import com.cs.urlshortenerorchestrator.engine.domain.WorkflowNode;
import lombok.Builder;
import lombok.Getter;
import java.time.Instant;

/**
 * NodeExecutor: Executes individual workflow nodes.
 * Can be implemented by different agents/functions.
 * Demonstrates controlled agent autonomy within governance framework.
 */
public interface NodeExecutor {
    /**
     * Execute a node. May be implemented by AI agents, functions, or scripts.
     * Must return execution result with status and error details.
     */
    Execution execute(WorkflowNode node, int attemptNumber) throws InterruptedException;
}

/**
 * Default implementation for testing and demonstration.
 */
class DefaultNodeExecutor implements NodeExecutor {
    @Override
    public Execution execute(WorkflowNode node, int attemptNumber) {
        // Simulate node execution with random outcome for demo
        try {
            Thread.sleep(100); // Simulate work

            return Execution.builder()
                .id("exec-" + node.getId() + "-" + attemptNumber)
                .nodeId(node.getId())
                .attemptNumber(attemptNumber)
                .status(ExecutionStatus.SUCCESS)
                .startedAt(Instant.now())
                .endedAt(Instant.now())
                .build();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return Execution.builder()
                .status(ExecutionStatus.FAILED)
                .errorDetails("Execution interrupted")
                .build();
        }
    }
}
