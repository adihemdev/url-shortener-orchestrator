package com.cs.urlshortenerorchestrator.engine;

import com.cs.urlshortenerorchestrator.engine.domain.*;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.*;

import static org.assertj.core.api.Assertions.*;

/**
 * Domain Model Tests - Non-Linear Workflow Behavior Demonstration
 *
 * These tests demonstrate core orchestration concepts using the domain model:
 * - Dependency graph structure and validation
 * - Entry/exit gates and validation rules
 * - Retry and rollback policies
 * - Approval checkpoints
 * - Execution metrics and audit trail
 * - Workflow state transitions
 */
@DisplayName("Orchestration Domain Model - Non-Linear Behavior Tests")
class WorkflowDomainTests {

    /**
     * TEST 1: DAG Structure - Dependency Validation
     *
     * Demonstrates: Workflow validates DAG structure (acyclic, consistent references).
     */
    @Test
    @DisplayName("workflow validates DAG is acyclic")
    void testWorkflowValidatesAcyclicDAG() {
        // Build valid linear DAG: req → arch → impl
        WorkflowNode req = WorkflowNode.builder("req", NodeType.REQUIREMENT_ANALYSIS)
            .build();
        WorkflowNode arch = WorkflowNode.builder("arch", NodeType.ARCHITECTURE_DESIGN)
            .dependsOn("req")
            .build();
        WorkflowNode impl = WorkflowNode.builder("impl", NodeType.IMPLEMENTATION)
            .dependsOn("arch")
            .build();

        Workflow workflow = Workflow.builder("linear", "Linear Dependencies")
            .root(req)
            .node(arch)
            .node(impl)
            .build();

        // Validation should pass
        List<String> errors = workflow.validate();
        assertThat(errors).isEmpty();
    }

    /**
     * TEST 2: Parallel Paths with Synchronization
     *
     * Demonstrates: DAG supports multiple independent paths that converge at sync point.
     * Structure: req ─┬→ arch ─┐
     *                └→ test ──┼→ sync → impl
     */
    @Test
    @DisplayName("workflow supports parallel paths with synchronization")
    void testParallelPathsWithSync() {
        // Build parallel DAG
        WorkflowNode req = WorkflowNode.builder("req", NodeType.REQUIREMENT_ANALYSIS).build();
        WorkflowNode arch = WorkflowNode.builder("arch", NodeType.ARCHITECTURE_DESIGN)
            .dependsOn("req").build();
        WorkflowNode test = WorkflowNode.builder("test", NodeType.TEST_PLANNING)
            .dependsOn("req").build();

        // Sync depends on both arch and test
        WorkflowNode sync = WorkflowNode.builder("sync", NodeType.SYNCHRONIZATION)
            .dependsOn("arch", "test").build();

        Workflow workflow = Workflow.builder("parallel", "Parallel Paths")
            .root(req)
            .node(arch)
            .node(test)
            .node(sync)
            .build();

        // Validation passes
        List<String> errors = workflow.validate();
        assertThat(errors).isEmpty();

        // Sync depends on both predecessors
        assertThat(sync.getDependsOnNodeIds()).contains("arch", "test");
    }

    /**
     * TEST 3: Entry/Exit Gate Validation Rules
     *
     * Demonstrates: Nodes have entry and exit gates with validation rules.
     */
    @Test
    @DisplayName("entry/exit gates define validation rules for node execution")
    void testGateValidationRules() {
        // Create gate with validation rules
        ValidationRule depCheck = ValidationRule.dependency("requirements");
        ValidationRule artifactCheck = ValidationRule.artifactExists("spec.json");

        Gate entryGate = Gate.builder("precheck")
            .validation()
            .validationRules(Arrays.asList(depCheck, artifactCheck))
            .failureAction(Gate.FailureAction.BLOCK)
            .description("Must have requirements and spec before implementation")
            .build();

        // Verify gate configuration
        assertThat(entryGate.getType()).isEqualTo(Gate.Type.VALIDATION);
        assertThat(entryGate.getValidationRules()).hasSize(2);
        assertThat(entryGate.getFailureAction()).isEqualTo(Gate.FailureAction.BLOCK);

        // Create node with gate
        WorkflowNode impl = WorkflowNode.builder("impl", NodeType.IMPLEMENTATION)
            .entryGate(entryGate)
            .build();

        assertThat(impl.getEntryGate()).isEqualTo(entryGate);
    }

    /**
     * TEST 4: Bounded Retry Policy with Backoff
     *
     * Demonstrates: Nodes have configurable retry policies with backoff strategies.
     */
    @Test
    @DisplayName("retry policy configures bounded attempts with backoff")
    void testBoundedRetryPolicy() {
        RetryPolicy retry = RetryPolicy.builder()
            .maxRetries(3)
            .backoff(BackoffStrategy.EXPONENTIAL)
            .initialDelay(1)
            .maxDelay(30)
            .retryOn(Arrays.asList("TRANSIENT_ERROR", "TIMEOUT"))
            .doNotRetryOn(Arrays.asList("VALIDATION_ERROR"))
            .maxDuration(300)
            .build();

        // Verify policy configuration
        assertThat(retry.getMaxRetries()).isEqualTo(3);
        assertThat(retry.getBackoffStrategy()).isEqualTo(BackoffStrategy.EXPONENTIAL);
        assertThat(retry.getMaxDurationSeconds()).isEqualTo(300);

        // Test backoff calculation
        assertThat(retry.calculateDelaySeconds(1)).isEqualTo(1);      // 2^0
        assertThat(retry.calculateDelaySeconds(2)).isEqualTo(2);      // 2^1
        assertThat(retry.calculateDelaySeconds(3)).isEqualTo(4);      // 2^2
        assertThat(retry.shouldRetry("TRANSIENT_ERROR", 1)).isTrue();
        assertThat(retry.shouldRetry("VALIDATION_ERROR", 1)).isFalse();
    }

    /**
     * TEST 5: Approval Gate with Human-in-the-Loop
     *
     * Demonstrates: Approval gates enforce human decision checkpoints.
     */
    @Test
    @DisplayName("approval gate requires human decision")
    void testApprovalGate() {
        ApprovalGate approval = ApprovalGate.builder("code_review")
            .required(true)
            .approver("TECH_LEAD")
            .description("Code must be reviewed before merging")
            .timeoutMinutes(120)
            .build();

        // Verify approval gate configuration
        assertThat(approval.isRequired()).isTrue();
        assertThat(approval.getDefaultApprover()).isEqualTo("TECH_LEAD");
        assertThat(approval.getTimeoutMinutes()).isEqualTo(120);

        // Create node with approval requirement
        WorkflowNode impl = WorkflowNode.builder("impl", NodeType.IMPLEMENTATION)
            .approvalGate(approval)
            .build();

        assertThat(impl.requiresApproval()).isTrue();
    }

    /**
     * TEST 6: Rollback Policy
     *
     * Demonstrates: Nodes have reversible/irreversible rollback strategies.
     */
    @Test
    @DisplayName("rollback policy defines reversible operations")
    void testRollbackPolicy() {
        // Reversible rollback
        RollbackPolicy rollback = RollbackPolicy.builder()
            .reversible(true)
            .operations(Arrays.asList("git reset --soft", "schema rollback", "cleanup"))
            .autoRollbackOn(Arrays.asList("APPROVAL_REJECTED", "VALIDATION_FAILED"))
            .timeout(300)
            .build();

        // Verify configuration
        assertThat(rollback.isReversible()).isTrue();
        assertThat(rollback.getReversibleOperations()).hasSize(3);
        assertThat(rollback.shouldAutoRollback("APPROVAL_REJECTED")).isTrue();
        assertThat(rollback.shouldAutoRollback("OTHER_ERROR")).isFalse();
    }

    /**
     * TEST 7: Workflow State Transitions
     *
     * Demonstrates: Workflow state tracks execution lifecycle.
     * States: RUNNING → WAITING_FOR_APPROVAL → COMPLETED/ROLLED_BACK/SAFE_STOPPED
     */
    @Test
    @DisplayName("workflow state transitions through execution lifecycle")
    void testWorkflowStateTransitions() {
        WorkflowState state = new WorkflowState("root");

        // Initial state
        assertThat(state.getPhase()).isEqualTo(WorkflowState.ExecutionPhase.RUNNING);

        // Mark node completed
        state.markNodeCompleted("req");
        assertThat(state.getCompletedNodeIds()).contains("req");

        // Transition to waiting for approval
        state.setPhase(WorkflowState.ExecutionPhase.WAITING_FOR_APPROVAL);
        assertThat(state.getPhase()).isEqualTo(WorkflowState.ExecutionPhase.WAITING_FOR_APPROVAL);

        // Mark node failed
        ExecutionFailure failure = new ExecutionFailure(
            "Approval rejected",
            new Exception("User rejected"),
            Instant.now()
        );
        state.markNodeFailed("impl", failure);
        assertThat(state.getFailedNodeIds()).contains("impl");

        // Transition to rolled back
        state.setPhase(WorkflowState.ExecutionPhase.ROLLED_BACK);
        assertThat(state.getPhase()).isEqualTo(WorkflowState.ExecutionPhase.ROLLED_BACK);

        // Complete workflow
        state.markCompleted();
        assertThat(state.isCompleted()).isTrue();
    }

    /**
     * TEST 8: Execution Metrics
     *
     * Demonstrates: Metrics track workflow execution quality (success rate, latency, etc.)
     */
    @Test
    @DisplayName("execution metrics track success rate and performance")
    void testExecutionMetrics() {
        ExecutionMetrics metrics = new ExecutionMetrics();

        // Set up execution
        metrics.setTotalNodes(10);
        metrics.setWorkflowStartedAt(Instant.now().minusSeconds(100));

        // Simulate execution results
        for (int i = 0; i < 8; i++) {
            metrics.incrementCompletedNodes();
        }
        for (int i = 0; i < 2; i++) {
            metrics.incrementFailedNodes();
        }

        metrics.recordNodeLatency(1000);
        metrics.recordNodeLatency(1500);

        // Simulate retry and rollback
        metrics.incrementRetriedNodes();
        metrics.recordRetryDelayTotal(5000);
        metrics.incrementRolledBackNodes();
        metrics.recordApprovalWaitTime(30000);

        // Verify metrics
        assertThat(metrics.getTotalNodes()).isEqualTo(10);
        assertThat(metrics.getCompletedNodes()).isEqualTo(8);
        assertThat(metrics.getRetriedNodes()).isEqualTo(1);
        assertThat(metrics.getRolledBackNodes()).isEqualTo(1);
        assertThat(metrics.getSuccessRate()).isEqualTo(0.8); // 8 success / 10 total
        assertThat(metrics.getRetryFrequency()).isEqualTo(0.1); // 1 retry / 10 total
        assertThat(metrics.getRollbackFrequency()).isEqualTo(0.1); // 1 rollback / 10 total
        assertThat(metrics.getAverageNodeLatencyMs()).isGreaterThan(0);
    }

    /**
     * TEST 9: Artifact Tracking
     *
     * Demonstrates: Artifacts track outputs produced during workflow execution.
     */
    @Test
    @DisplayName("artifacts track outputs produced by node execution")
    void testArtifactTracking() {
        Artifact schema = new Artifact(
            "schema-v1",
            ArtifactType.SCHEMA,
            "database_schema.sql",
            "arch",
            "exec-arch-1",
            "/artifacts/schema.sql",
            Map.of("version", "1.0", "validated", "true"),
            Instant.now()
        );

        // Verify artifact metadata
        assertThat(schema.id()).isEqualTo("schema-v1");
        assertThat(schema.type()).isEqualTo(ArtifactType.SCHEMA);
        assertThat(schema.producedByNodeId()).isEqualTo("arch");
        assertThat(schema.metadata().get("version")).isEqualTo("1.0");
    }

    /**
     * TEST 10: Decision Lineage
     *
     * Demonstrates: Decisions track reasoning for actions taken during execution.
     */
    @Test
    @DisplayName("decision lineage tracks execution decisions")
    void testDecisionLineage() {
        Decision decision = new Decision(
            "dec-1",
            "impl",
            "exec-impl-1",
            DecisionType.RETRY_ON_FAILURE,
            "Network timeout detected, retrying implementation",
            "Scheduled retry after 5s backoff",
            Instant.now(),
            true,  // reversible
            Arrays.asList("dec-0")  // depends on previous decision
        );

        // Verify decision structure
        assertThat(decision.id()).isEqualTo("dec-1");
        assertThat(decision.type()).isEqualTo(DecisionType.RETRY_ON_FAILURE);
        assertThat(decision.reasoning()).contains("Network timeout");
        assertThat(decision.reversible()).isTrue();
        assertThat(decision.relatedDecisionIds()).contains("dec-0");
    }

    /**
     * TEST 11: Policy Enforcement
     *
     * Demonstrates: Policies constrain execution (security, compliance, change control).
     */
    @Test
    @DisplayName("policies define governance rules for node execution")
    void testPolicyEnforcement() {
        Policy securityPolicy = new Policy(
            "sec-001",
            "Schema Changes Need DBA Approval",
            "Any schema modification requires DBA sign-off",
            Arrays.asList("schema.create", "schema.modify", "schema.drop"),
            true,  // enforceable
            Set.of(NodeType.ARCHITECTURE_DESIGN, NodeType.IMPLEMENTATION)
        );

        // Verify policy configuration
        assertThat(securityPolicy.name()).isEqualTo("Schema Changes Need DBA Approval");
        assertThat(securityPolicy.enforceable()).isTrue();
        assertThat(securityPolicy.appliesToNodeTypes()).contains(NodeType.ARCHITECTURE_DESIGN);
        assertThat(securityPolicy.rules()).hasSize(3);
    }

    /**
     * TEST 12: Complex Scenario - Full DAG with Multi-Stage Execution
     *
     * Demonstrates: Multi-stage workflow with parallel branches, approvals, retries.
     */
    @Test
    @DisplayName("complex workflow DAG with all non-linear elements")
    void testComplexWorkflowDAG() {
        // Requirements (no dependencies)
        WorkflowNode req = WorkflowNode.builder("req", NodeType.REQUIREMENT_ANALYSIS).build();

        // Parallel branches after requirements
        WorkflowNode arch = WorkflowNode.builder("arch", NodeType.ARCHITECTURE_DESIGN)
            .dependsOn("req")
            .entryGate(Gate.passThrough("arch_entry"))
            .retryPolicy(RetryPolicy.builder().maxRetries(2).build())
            .build();

        WorkflowNode testPlan = WorkflowNode.builder("test_plan", NodeType.TEST_PLANNING)
            .dependsOn("req")
            .build();

        // Sync point: both must complete
        WorkflowNode sync = WorkflowNode.builder("sync", NodeType.SYNCHRONIZATION)
            .dependsOn("arch", "test_plan")
            .build();

        // Implementation with retry policy
        RetryPolicy implRetry = RetryPolicy.builder()
            .maxRetries(3)
            .backoff(BackoffStrategy.LINEAR)
            .maxDuration(600)
            .build();

        WorkflowNode impl = WorkflowNode.builder("impl", NodeType.IMPLEMENTATION)
            .dependsOn("sync")
            .retryPolicy(implRetry)
            .build();

        // Validation
        WorkflowNode validation = WorkflowNode.builder("validation", NodeType.VALIDATION)
            .dependsOn("impl")
            .build();

        // Release with approval gate
        ApprovalGate releaseApproval = ApprovalGate.builder("final_approval")
            .required(true)
            .approver("RELEASE_MANAGER")
            .build();

        WorkflowNode release = WorkflowNode.builder("release", NodeType.RELEASE_READY)
            .dependsOn("validation")
            .approvalGate(releaseApproval)
            .rollbackPolicy(RollbackPolicy.builder()
                .reversible(true)
                .operations(Arrays.asList("git revert", "deployment rollback"))
                .build())
            .build();

        // Build workflow
        Workflow workflow = Workflow.builder("complex", "Full Release Pipeline")
            .description("Multi-stage workflow with parallel execution, approval, and rollback")
            .root(req)
            .node(arch)
            .node(testPlan)
            .node(sync)
            .node(impl)
            .node(validation)
            .node(release)
            .build();

        // Validate DAG
        List<String> errors = workflow.validate();
        assertThat(errors).isEmpty();

        // Verify structure
        assertThat(workflow.getNodesById()).hasSize(7);
        assertThat(sync.getDependsOnNodeIds()).contains("arch", "test_plan");
        assertThat(impl.getRetryPolicy().getMaxRetries()).isEqualTo(3);
        assertThat(release.requiresApproval()).isTrue();
        assertThat(release.getRollbackPolicy().isReversible()).isTrue();
    }
}
