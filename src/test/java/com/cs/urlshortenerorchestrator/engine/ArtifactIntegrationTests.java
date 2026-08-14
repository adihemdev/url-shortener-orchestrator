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
 * Artifact Integration Tests - Cross-Stage Artifact Context
 *
 * Demonstrates minimal artifact integration:
 * - Artifacts recorded as part of workflow execution state
 * - Cross-stage context availability for dependent nodes
 * - Linked to audit trail for observability
 * - No persistence, repository, or new abstractions
 */
@DisplayName("Minimal Artifact Integration Tests")
class ArtifactIntegrationTests {

    private Workflow workflow;
    private WorkflowState workflowState;
    private ExecutionMetrics metrics;

    @BeforeEach
    void setUp() {
        // Build workflow: arch → impl (impl depends on arch)
        WorkflowNode arch = WorkflowNode.builder("arch", NodeType.ARCHITECTURE_DESIGN)
            .description("Architecture design phase")
            .build();

        WorkflowNode impl = WorkflowNode.builder("impl", NodeType.IMPLEMENTATION)
            .description("Implementation phase")
            .dependsOn("arch")
            .build();

        workflow = Workflow.builder("artifact-test", "Artifact Integration Test")
            .root(arch)
            .node(impl)
            .build();

        workflowState = workflow.getCurrentState();
        metrics = new ExecutionMetrics();
        metrics.setTotalNodes(2);
        metrics.setWorkflowStartedAt(Instant.now());
    }

    /**
     * TEST 1: Artifact recorded and retrieved by ID
     *
     * Demonstrates: Artifact can be recorded in WorkflowState and retrieved.
     */
    @Test
    @DisplayName("artifact can be recorded and retrieved by ID")
    void testArtifactRecordedAndRetrieved() {
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

        // Record artifact
        workflowState.recordArtifact(schema);

        // Retrieve and verify
        Artifact retrieved = workflowState.getArtifactById("schema-v1");
        assertThat(retrieved).isNotNull();
        assertThat(retrieved.id()).isEqualTo("schema-v1");
        assertThat(retrieved.type()).isEqualTo(ArtifactType.SCHEMA);
        assertThat(retrieved.producedByNodeId()).isEqualTo("arch");
        assertThat(retrieved.metadata().get("version")).isEqualTo("1.0");
    }

    /**
     * TEST 2: Get artifacts produced by a specific node
     *
     * Demonstrates: Artifacts are indexed by producing node for cross-stage access.
     */
    @Test
    @DisplayName("get artifacts produced by a node")
    void testGetArtifactsByNode() {
        // Architecture produces 2 artifacts
        Artifact schema = new Artifact(
            "schema-v1", ArtifactType.SCHEMA, "database_schema.sql",
            "arch", "exec-arch-1", "/artifacts/schema.sql",
            Map.of("version", "1.0"), Instant.now()
        );

        Artifact archPlan = new Artifact(
            "plan-v1", ArtifactType.ARCHITECTURE_PLAN, "architecture.md",
            "arch", "exec-arch-1", "/artifacts/architecture.md",
            Map.of("components", "3"), Instant.now()
        );

        workflowState.recordArtifact(schema);
        workflowState.recordArtifact(archPlan);

        // Get artifacts from arch node
        List<String> archArtifactIds = workflowState.getArtifactIdsProducedByNode("arch");

        assertThat(archArtifactIds).hasSize(2);
        assertThat(archArtifactIds).contains("schema-v1", "plan-v1");
    }

    /**
     * TEST 3: Dependent node accesses artifacts from predecessor via ExecutionContext
     *
     * Demonstrates: ExecutionContext provides cross-stage artifact context.
     * Implementation node can access architecture artifacts as input context.
     */
    @Test
    @DisplayName("dependent node accesses predecessor artifacts via ExecutionContext")
    void testDependentNodeAccessesPredecessorArtifacts() {
        // Record artifacts produced by architecture phase
        Artifact schema = new Artifact(
            "schema-v1", ArtifactType.SCHEMA, "database_schema.sql",
            "arch", "exec-arch-1", "/artifacts/schema.sql",
            Map.of("version", "1.0"), Instant.now()
        );

        Artifact archPlan = new Artifact(
            "plan-v1", ArtifactType.ARCHITECTURE_PLAN, "architecture.md",
            "arch", "exec-arch-1", "/artifacts/architecture.md",
            Map.of("components", "3"), Instant.now()
        );

        workflowState.recordArtifact(schema);
        workflowState.recordArtifact(archPlan);

        // Get impl node and create execution context
        WorkflowNode impl = workflow.getNodesById().get("impl");
        ExecutionContext context = new ExecutionContext(impl, null, workflowState, metrics);

        // Impl node depends on arch, should access arch artifacts
        List<Artifact> predecessorArtifacts = context.getArtifactsFromAllPredecessors();

        assertThat(predecessorArtifacts).hasSize(2);
        assertThat(predecessorArtifacts.stream().map(Artifact::id))
            .containsExactlyInAnyOrder("schema-v1", "plan-v1");
    }

    /**
     * TEST 4: Dependent node accesses artifacts from specific predecessor
     *
     * Demonstrates: ExecutionContext can query artifacts from a specific predecessor.
     */
    @Test
    @DisplayName("dependent node accesses artifacts from specific predecessor")
    void testAccessArtifactsFromSpecificPredecessor() {
        Artifact schema = new Artifact(
            "schema-v1", ArtifactType.SCHEMA, "database_schema.sql",
            "arch", "exec-arch-1", "/artifacts/schema.sql",
            Map.of("version", "1.0"), Instant.now()
        );

        workflowState.recordArtifact(schema);

        WorkflowNode impl = workflow.getNodesById().get("impl");
        ExecutionContext context = new ExecutionContext(impl, null, workflowState, metrics);

        // Query artifacts from arch specifically
        List<Artifact> archArtifacts = context.getArtifactsFromPredecessor("arch");

        assertThat(archArtifacts).hasSize(1);
        assertThat(archArtifacts.get(0).id()).isEqualTo("schema-v1");
        assertThat(archArtifacts.get(0).type()).isEqualTo(ArtifactType.SCHEMA);
    }

    /**
     * TEST 5: Multiple artifact types from single node execution
     *
     * Demonstrates: A single node can produce multiple artifacts of different types.
     */
    @Test
    @DisplayName("node can produce multiple artifacts of different types")
    void testMultipleArtifactTypesFromNode() {
        // Architecture produces schema, plan, and API spec
        Artifact schema = new Artifact(
            "schema-v1", ArtifactType.SCHEMA, "database_schema.sql",
            "arch", "exec-arch-1", "/artifacts/schema.sql",
            Map.of("version", "1.0"), Instant.now()
        );

        Artifact plan = new Artifact(
            "plan-v1", ArtifactType.ARCHITECTURE_PLAN, "architecture.md",
            "arch", "exec-arch-1", "/artifacts/architecture.md",
            Map.of("version", "1.0"), Instant.now()
        );

        Artifact apiSpec = new Artifact(
            "api-v1", ArtifactType.API_SPEC, "api.yaml",
            "arch", "exec-arch-1", "/artifacts/api.yaml",
            Map.of("version", "1.0"), Instant.now()
        );

        workflowState.recordArtifact(schema);
        workflowState.recordArtifact(plan);
        workflowState.recordArtifact(apiSpec);

        List<String> artifactIds = workflowState.getArtifactIdsProducedByNode("arch");
        assertThat(artifactIds).hasSize(3);

        List<Artifact> artifacts = artifactIds.stream()
            .map(id -> workflowState.getArtifactById(id))
            .toList();

        assertThat(artifacts.stream().map(Artifact::type))
            .containsExactlyInAnyOrder(ArtifactType.SCHEMA, ArtifactType.ARCHITECTURE_PLAN, ArtifactType.API_SPEC);
    }

    /**
     * TEST 6: Artifact metadata provides context
     *
     * Demonstrates: Artifact metadata captures context for dependent stages.
     */
    @Test
    @DisplayName("artifact metadata provides execution context")
    void testArtifactMetadataContext() {
        Artifact schema = new Artifact(
            "schema-v1", ArtifactType.SCHEMA, "database_schema.sql",
            "arch", "exec-arch-1", "/artifacts/schema.sql",
            Map.of(
                "version", "1.0",
                "validated", "true",
                "tables", "42",
                "indexes", "18",
                "approval_status", "reviewed"
            ),
            Instant.now()
        );

        workflowState.recordArtifact(schema);

        Artifact retrieved = workflowState.getArtifactById("schema-v1");
        assertThat(retrieved.metadata())
            .containsEntry("version", "1.0")
            .containsEntry("validated", "true")
            .containsEntry("tables", "42")
            .containsEntry("approval_status", "reviewed");
    }

    /**
     * TEST 7: Artifact from predecessor is available before dependent node executes
     *
     * Demonstrates: Artifacts are available as input context during dependent node setup.
     */
    @Test
    @DisplayName("artifact available to dependent node before execution")
    void testArtifactAvailableBeforeDependentExecution() {
        // Mark arch as completed and record its artifact
        workflowState.markNodeCompleted("arch");

        Artifact schema = new Artifact(
            "schema-v1", ArtifactType.SCHEMA, "database_schema.sql",
            "arch", "exec-arch-1", "/artifacts/schema.sql",
            Map.of("version", "1.0"), Instant.now()
        );

        workflowState.recordArtifact(schema);

        // Impl is about to execute - should already have access to arch artifact
        WorkflowNode impl = workflow.getNodesById().get("impl");

        // Verify arch is completed
        assertThat(workflowState.getCompletedNodeIds()).contains("arch");

        // Verify schema artifact is available
        Artifact available = workflowState.getArtifactById("schema-v1");
        assertThat(available).isNotNull();
        assertThat(available.producedByNodeId()).isEqualTo("arch");
    }

    /**
     * TEST 8: No artifacts initially available
     *
     * Demonstrates: Artifact storage starts empty and is populated through recording.
     */
    @Test
    @DisplayName("no artifacts initially - must be explicitly recorded")
    void testNoArtifactsInitially() {
        // Initially empty
        List<String> archArtifactIds = workflowState.getArtifactIdsProducedByNode("arch");
        assertThat(archArtifactIds).isEmpty();

        Artifact artifact = workflowState.getArtifactById("nonexistent");
        assertThat(artifact).isNull();
    }

    /**
     * TEST 9: Artifact storage across multiple nodes
     *
     * Demonstrates: Different nodes can produce artifacts, all stored and queryable.
     */
    @Test
    @DisplayName("artifacts from multiple nodes stored and queryable")
    void testArtifactsFromMultipleNodes() {
        // Architecture produces schema
        Artifact schema = new Artifact(
            "schema-v1", ArtifactType.SCHEMA, "database_schema.sql",
            "arch", "exec-arch-1", "/artifacts/schema.sql",
            Map.of("version", "1.0"), Instant.now()
        );

        // Implementation produces code
        Artifact code = new Artifact(
            "code-v1", ArtifactType.CODE, "src/main/java/...",
            "impl", "exec-impl-1", "/repo/code",
            Map.of("lines", "5000"), Instant.now()
        );

        workflowState.recordArtifact(schema);
        workflowState.recordArtifact(code);

        // Query by node
        assertThat(workflowState.getArtifactIdsProducedByNode("arch")).contains("schema-v1");
        assertThat(workflowState.getArtifactIdsProducedByNode("impl")).contains("code-v1");

        // Query by ID
        assertThat(workflowState.getArtifactById("schema-v1")).isNotNull();
        assertThat(workflowState.getArtifactById("code-v1")).isNotNull();
    }
}
