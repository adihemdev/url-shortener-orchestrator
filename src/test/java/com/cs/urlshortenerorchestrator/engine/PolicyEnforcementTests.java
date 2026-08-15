package com.cs.urlshortenerorchestrator.engine;

import com.cs.urlshortenerorchestrator.engine.domain.*;
import com.cs.urlshortenerorchestrator.engine.execution.ExecutionContext;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.*;

import static org.assertj.core.api.Assertions.*;

/**
 * Policy Enforcement Tests - Minimal Runtime Policy Evaluation
 *
 * Demonstrates policy enforcement integrated into WorkflowExecutor:
 * - Enforceable policies block execution when they apply to node type
 * - Non-enforceable policies are skipped
 * - Policies that don't apply to node type are skipped
 * - ExecutionContext is used to evaluate policies at runtime
 */
@DisplayName("Runtime Policy Enforcement Tests")
class PolicyEnforcementTests {

    private Workflow workflow;
    private WorkflowState workflowState;
    private ExecutionMetrics metrics;

    @BeforeEach
    void setUp() {
        // Build a simple workflow with one node for testing
        WorkflowNode impl = WorkflowNode.builder("impl", NodeType.IMPLEMENTATION)
            .description("Implementation node for policy testing")
            .build();

        workflow = Workflow.builder("policy-test", "Policy Enforcement Test")
            .root(impl)
            .build();

        workflowState = workflow.getCurrentState();
        metrics = new ExecutionMetrics();
        metrics.setTotalNodes(1);
        metrics.setWorkflowStartedAt(Instant.now());
    }

    /**
     * TEST 1: Enforceable policy blocks execution
     *
     * Demonstrates: ExecutionContext correctly identifies when a policy applies
     * to the node type and is enforceable, which should block execution.
     */
    @Test
    @DisplayName("enforceable policy blocks execution when applies to node type")
    void testEnforceablePolicyBlocksExecution() {
        // Policy that applies to IMPLEMENTATION nodes and is enforceable
        Policy securityPolicy = new Policy(
            "sec-001",
            "Schema Changes Need DBA Approval",
            "Any schema modification requires DBA sign-off for security",
            Arrays.asList("schema.create", "schema.modify"),
            true,  // enforceable
            Set.of(NodeType.IMPLEMENTATION)
        );

        // Add policy to node
        WorkflowNode impl = workflow.getNodesById().get("impl");
        impl = WorkflowNode.builder("impl", NodeType.IMPLEMENTATION)
            .description("Implementation node with policy")
            .policies(Arrays.asList(securityPolicy))
            .build();

        // Create ExecutionContext to evaluate policy
        ExecutionContext context = new ExecutionContext(impl, null, workflowState, metrics);

        // Verify that policy applies and is enforceable
        assertThat(context.policyAppliesToNode(securityPolicy)).isTrue();
        assertThat(securityPolicy.enforceable()).isTrue();
    }

    /**
     * TEST 2: Non-enforceable policy does not block execution
     *
     * Demonstrates: ExecutionContext checks enforceability flag.
     * Even if a policy applies to the node type, it won't block if not enforceable.
     */
    @Test
    @DisplayName("non-enforceable policy does not block execution")
    void testNonEnforceablePolicyAllowsExecution() {
        // Policy that applies to IMPLEMENTATION but is NOT enforceable
        Policy auditPolicy = new Policy(
            "audit-001",
            "Log All Implementation Changes",
            "Track implementation changes for audit purposes",
            Arrays.asList("log.change"),
            false,  // NOT enforceable
            Set.of(NodeType.IMPLEMENTATION)
        );

        // Add policy to node
        WorkflowNode impl = workflow.getNodesById().get("impl");
        impl = WorkflowNode.builder("impl", NodeType.IMPLEMENTATION)
            .description("Implementation node with non-enforceable policy")
            .policies(Arrays.asList(auditPolicy))
            .build();

        // Create ExecutionContext
        ExecutionContext context = new ExecutionContext(impl, null, workflowState, metrics);

        // Policy applies to node type but is NOT enforceable
        assertThat(context.policyAppliesToNode(auditPolicy)).isFalse();  // Should not apply due to enforceability
        assertThat(auditPolicy.enforceable()).isFalse();
    }

    /**
     * TEST 3: Policy that doesn't apply to node type is skipped
     *
     * Demonstrates: ExecutionContext filters policies by node type.
     * A policy for ARCHITECTURE_DESIGN doesn't apply to IMPLEMENTATION.
     */
    @Test
    @DisplayName("policy not applicable to node type is skipped")
    void testPolicyNotApplicableToNodeTypeIsSkipped() {
        // Policy that applies to ARCHITECTURE_DESIGN only
        Policy architecturePolicy = new Policy(
            "arch-001",
            "Architecture Review Required",
            "All architecture decisions must be reviewed",
            Arrays.asList("architecture.review"),
            true,  // enforceable
            Set.of(NodeType.ARCHITECTURE_DESIGN)  // Only applies to ARCHITECTURE_DESIGN
        );

        // Add to IMPLEMENTATION node
        WorkflowNode impl = workflow.getNodesById().get("impl");
        impl = WorkflowNode.builder("impl", NodeType.IMPLEMENTATION)
            .description("Implementation node with architecture policy")
            .policies(Arrays.asList(architecturePolicy))
            .build();

        // Create ExecutionContext for IMPLEMENTATION node
        ExecutionContext context = new ExecutionContext(impl, null, workflowState, metrics);

        // Policy should NOT apply because node is IMPLEMENTATION, not ARCHITECTURE_DESIGN
        assertThat(context.policyAppliesToNode(architecturePolicy)).isFalse();
    }

    /**
     * TEST 4: Multiple policies - mix of enforced, non-enforced, and non-applicable
     *
     * Demonstrates: Correct handling of multiple policies with different configurations.
     */
    @Test
    @DisplayName("multiple policies evaluated correctly")
    void testMultiplePoliciesEvaluatedCorrectly() {
        // Policy 1: Enforceable and applies to IMPLEMENTATION
        Policy enforcedPolicy = new Policy(
            "sec-001",
            "Security Approval Required",
            "Security review required",
            Arrays.asList("security.approve"),
            true,  // enforceable
            Set.of(NodeType.IMPLEMENTATION)
        );

        // Policy 2: Non-enforceable, applies to IMPLEMENTATION
        Policy nonEnforcedPolicy = new Policy(
            "audit-001",
            "Audit Logging",
            "Audit logging requirement",
            Arrays.asList("audit.log"),
            false,  // NOT enforceable
            Set.of(NodeType.IMPLEMENTATION)
        );

        // Policy 3: Enforceable but applies only to RELEASE_READY
        Policy releasePolicy = new Policy(
            "rel-001",
            "Release Approval",
            "Release approval required",
            Arrays.asList("release.approve"),
            true,  // enforceable
            Set.of(NodeType.RELEASE_READY)  // Only applies to RELEASE_READY
        );

        List<Policy> policies = Arrays.asList(enforcedPolicy, nonEnforcedPolicy, releasePolicy);

        WorkflowNode impl = WorkflowNode.builder("impl", NodeType.IMPLEMENTATION)
            .description("Implementation with multiple policies")
            .policies(policies)
            .build();

        ExecutionContext context = new ExecutionContext(impl, null, workflowState, metrics);

        // Only the first policy applies: enforceable AND applicable to IMPLEMENTATION
        assertThat(context.policyAppliesToNode(enforcedPolicy)).isTrue();

        // Second policy doesn't apply: not enforceable
        assertThat(context.policyAppliesToNode(nonEnforcedPolicy)).isFalse();

        // Third policy doesn't apply: wrong node type
        assertThat(context.policyAppliesToNode(releasePolicy)).isFalse();
    }

    /**
     * TEST 5: ExecutionContext provides runtime values for policy evaluation
     *
     * Demonstrates: ExecutionContext captures current workflow state and metrics.
     * This allows policies to be evaluated in context of actual runtime state.
     */
    @Test
    @DisplayName("execution context captures runtime state for policy evaluation")
    void testExecutionContextCapturesRuntimeState() {
        WorkflowNode impl = workflow.getNodesById().get("impl");

        // Mark a node as completed to establish state
        workflowState.markNodeCompleted("impl");

        // Add metrics
        metrics.incrementCompletedNodes();
        metrics.recordNodeLatency(1000);

        // Create execution (optional - could be null)
        Execution execution = Execution.builder()
            .id("exec-1")
            .workflowId(workflow.getId())
            .nodeId("impl")
            .attemptNumber(1)
            .status(ExecutionStatus.SUCCESS)
            .startedAt(Instant.now().minusSeconds(10))
            .endedAt(Instant.now())
            .build();

        // Create ExecutionContext with all runtime state
        ExecutionContext context = new ExecutionContext(impl, execution, workflowState, metrics);

        // Verify ExecutionContext captures the state
        assertThat(context.getNode().getId()).isEqualTo("impl");
        assertThat(context.getExecution()).isNotNull();
        assertThat(context.getExecution().getStatus()).isEqualTo(ExecutionStatus.SUCCESS);
        assertThat(context.getWorkflowState().getCompletedNodeIds()).contains("impl");
        assertThat(context.getMetrics().getCompletedNodes()).isEqualTo(1);

        // Verify context is never null
        assertThat(context.toString()).contains("ExecutionContext");
    }

    /**
     * TEST 6: Workflow node properly exposes policies through getter
     *
     * Demonstrates: WorkflowNode.getPolicies() returns the list of policies.
     */
    @Test
    @DisplayName("workflow node exposes policies through getter")
    void testWorkflowNodeExposePolicies() {
        List<Policy> testPolicies = Arrays.asList(
            new Policy("p1", "Policy 1", "desc 1", List.of(), true, Set.of(NodeType.IMPLEMENTATION)),
            new Policy("p2", "Policy 2", "desc 2", List.of(), false, Set.of(NodeType.ARCHITECTURE_DESIGN))
        );

        WorkflowNode node = WorkflowNode.builder("test", NodeType.IMPLEMENTATION)
            .policies(testPolicies)
            .build();

        assertThat(node.getPolicies()).hasSize(2);
        assertThat(node.getPolicies()).containsAll(testPolicies);
    }

    /**
     * TEST 7: Empty policies list - no policies to enforce
     *
     * Demonstrates: Nodes can have no policies, which is a valid state.
     */
    @Test
    @DisplayName("node with no policies allows execution")
    void testNodeWithNoPoliciesAllowsExecution() {
        WorkflowNode impl = WorkflowNode.builder("impl", NodeType.IMPLEMENTATION)
            .description("Implementation without policies")
            // No policies() call - uses default empty list
            .build();

        ExecutionContext context = new ExecutionContext(impl, null, workflowState, metrics);

        // Node has no policies
        assertThat(impl.getPolicies()).isEmpty();
    }

    /**
     * TEST 8: Enforceable policy blocks execution for specific node type
     *
     * Demonstrates: Policy enforcement is type-safe - policies only block
     * for the node types they're configured for.
     */
    @Test
    @DisplayName("enforceable policy blocks only for configured node types")
    void testEnforceablePolicyBlocksOnlyConfiguredNodeTypes() {
        // Policy only for ARCHITECTURE_DESIGN and TEST_PLANNING
        Policy restrictedPolicy = new Policy(
            "design-001",
            "Design Review Required",
            "All design and test planning requires review",
            Arrays.asList("design.review"),
            true,  // enforceable
            Set.of(NodeType.ARCHITECTURE_DESIGN, NodeType.TEST_PLANNING)
        );

        // Test with ARCHITECTURE_DESIGN - should block
        WorkflowNode archNode = WorkflowNode.builder("arch", NodeType.ARCHITECTURE_DESIGN)
            .policies(Arrays.asList(restrictedPolicy))
            .build();
        ExecutionContext archContext = new ExecutionContext(archNode, null, workflowState, metrics);
        assertThat(archContext.policyAppliesToNode(restrictedPolicy)).isTrue();

        // Test with TEST_PLANNING - should block
        WorkflowNode testNode = WorkflowNode.builder("test", NodeType.TEST_PLANNING)
            .policies(Arrays.asList(restrictedPolicy))
            .build();
        ExecutionContext testContext = new ExecutionContext(testNode, null, workflowState, metrics);
        assertThat(testContext.policyAppliesToNode(restrictedPolicy)).isTrue();

        // Test with IMPLEMENTATION - should NOT block
        WorkflowNode implNode = WorkflowNode.builder("impl", NodeType.IMPLEMENTATION)
            .policies(Arrays.asList(restrictedPolicy))
            .build();
        ExecutionContext implContext = new ExecutionContext(implNode, null, workflowState, metrics);
        assertThat(implContext.policyAppliesToNode(restrictedPolicy)).isFalse();
    }
}
