package com.cs.urlshortenerorchestrator.engine.execution;

import com.cs.urlshortenerorchestrator.engine.domain.Execution;
import com.cs.urlshortenerorchestrator.engine.domain.ExecutionStatus;
import com.cs.urlshortenerorchestrator.engine.domain.WorkflowNode;
import lombok.Builder;
import lombok.Getter;
import java.time.Instant;

    /**
     * Executes individual workflow nodes and manages their side effects.
     * Can be implemented by engineering agents, automated functions, or scripts.
     * Provides the integration point for bounded agent autonomy.
     */
public interface NodeExecutor {
    /**
     * Executes a node and returns the result.
     * @param node The node to execute
     * @param attemptNumber Current retry attempt number
     */
    Execution execute(WorkflowNode node, int attemptNumber) throws InterruptedException;

    /**
     * Invokes the rollback compensation hook.
     * Triggered when an approval is rejected for a reversible node.
     * @param node The node being rolled back
     * @param operations The specific reversible operations to perform
     */
    default void rollback(WorkflowNode node, java.util.List<String> operations) throws InterruptedException {
        // Default no-op for non-reversible implementations
    }
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
