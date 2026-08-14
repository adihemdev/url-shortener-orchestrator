# Orchestration Engine - Workflow DSL (Domain-Specific Language)

This document defines the Java DSL (using the domain model classes) for programmatically defining workflows.
Instead of manual configuration, workflows are code-first and type-safe.

---

## Core DSL Concepts

### 1. Workflow Builder Pattern

```java
// Fluent builder for constructing workflows
Workflow greenfield = new WorkflowBuilder("greenfield-analytics")
    .description("Add Analytics API for URL usage tracking")
    .scenario(ScenarioType.GREENFIELD)
    .requirementArtifacts("requirement_spec.md", "acceptance_criteria.md")
    .root(
        node("requirements")
            .type(NodeType.REQUIREMENT_ANALYSIS)
            .executor("analyze_analytics_requirements")
            .exitGate(gate()
                .validation("artifact_produced", "requirement_spec.md")
                .validation("schema_valid", "requirement_spec_schema")
                .build())
            .timeout(30, TimeUnit.MINUTES)
            .parallelNext("architecture", "testplan")
            .build()
    )
    .node(
        node("architecture")
            .type(NodeType.ARCHITECTURE_DESIGN)
            .executor("design_analytics_architecture")
            .dependsOn("requirements")
            .entryGate(gate()
                .dependencyCheck("requirements")
                .artifactCheck("requirement_spec.md")
                .policyCheck(SecurityPolicy.dataClassification)
                .build())
            .exitGate(gate()
                .artifactProduced("api_schema.yml", "db_schema.sql")
                .schemaValid("openapi_v3_schema")
                .policyCompliant(SecurityPolicy.all(), PerformancePolicy.all())
                .build())
            .timeout(60, TimeUnit.MINUTES)
            .retryPolicy(RetryPolicy.builder()
                .maxRetries(2)
                .backoff(BackoffStrategy.EXPONENTIAL, 1, 2, 4)
                .retryOn(CompilationError.class, EnvironmentSetupError.class)
                .doNotRetryOn(ArchitectureViolation.class, ValidationError.class)
                .build())
            .next("sync")
            .build()
    )
    .node(
        node("testplan")
            .type(NodeType.TEST_PLANNING)
            .executor("plan_analytics_tests")
            .dependsOn("requirements")
            .entryGate(gate()
                .dependencyCheck("requirements")
                .artifactCheck("requirement_spec.md")
                .build())
            .exitGate(gate()
                .artifactProduced("test_cases.md", "test_strategy.md")
                .metricsAbove("test_coverage_plan", 0.80)
                .build())
            .timeout(45, TimeUnit.MINUTES)
            .retryPolicy(RetryPolicy.maxRetries(1))
            .next("sync")
            .build()
    )
    .synchronization("sync")
        .waitFor("architecture", "testplan")
        .compatibilityCheck("do_schema_and_tests_align")
        .next("implementation")
        .build()
    )
    .node(
        node("implementation")
            .type(NodeType.IMPLEMENTATION)
            .executor("implement_analytics_api")
            .dependsOn("sync")
            .approvalGate(ApprovalGate.builder()
                .gateName("Code Review")
                .required(true)
                .defaultApprover("senior-backend-engineer")
                .autoApproveWhen(coverage().greaterThan(0.85).and(noSecurityIssues()))
                .timeoutMinutes(24)
                .build())
            .retryPolicy(RetryPolicy.builder()
                .maxRetries(3)
                .backoff(BackoffStrategy.EXPONENTIAL)
                .maxDurationSeconds(1800)
                .retryOn(CompilationError.class, EnvironmentSetupError.class)
                .build())
            .rollbackPolicy(RollbackPolicy.builder()
                .reversible(true)
                .operations(
                    GitReset.beforeCommit(implementation),
                    SchemaRollback.dropNewTables(),
                    ConfigRestore.featureFlagOff("analytics_enabled")
                )
                .autoRollbackOn(
                    ValidationFailure.class,
                    ApprovalRejection.class
                )
                .timeoutSeconds(300)
                .build())
            .replanTrigger(when -> when
                .validationFailsWith("schema_incompatible")
                .regenerateDAGFrom("architecture")
                .orWhen()
                .validationFailsWith("performance_insufficient")
                .regenerateDAGFrom("architecture")
            )
            .next("testing")
            .timeout(120, TimeUnit.MINUTES)
            .build()
    )
    .node(
        node("testing")
            .type(NodeType.TESTING)
            .executor("run_analytics_tests")
            .dependsOn("implementation")
            .entryGate(gate()
                .dependencyCheck("implementation")
                .artifactCheck("code_artifact.jar", "test_cases.md")
                .approvalCheck("implementation") // Implementation must be approved
                .build())
            .exitGate(gate()
                .metricsAbove("code_coverage", 0.75)
                .metricsAbove("all_tests_passed", 1.0)
                .metricsAbove("performance_tests_passed", 1.0)
                .onFailure(TRIGGER_REPLAN)
                .build())
            .retryPolicy(RetryPolicy.builder()
                .maxRetries(3)
                .backoff(BackoffStrategy.LINEAR, 10, 20, 30)
                .maxDurationSeconds(600)
                .retryOn(TransientTestFailure.class, ResourceUnavailable.class)
                .doNotRetryOn(AssertionError.class, TimeoutException.class)
                .build())
            .timeout(180, TimeUnit.MINUTES)
            .next("validation")
            .build()
    )
    .node(
        node("validation")
            .type(NodeType.VALIDATION)
            .executor("validate_analytics_requirements")
            .dependsOn("testing")
            .exitGate(gate()
                .artifactProduced("validation_report.md")
                .metrics("requirements_coverage", greaterThanOrEqual(0.95))
                .metrics("acceptance_criteria_met", equals(true))
                .build())
            .conditionalBranch(validationResult ->
                validationResult.isPASS() 
                    ? route().to("release")
                    : route().to("replan").scope(FROM_IMPLEMENTATION)
            )
            .timeout(60, TimeUnit.MINUTES)
            .build()
    )
    .node(
        node("release")
            .type(NodeType.RELEASE_READY)
            .executor("release_analytics_to_production")
            .dependsOn("validation")
            .approvalGate(ApprovalGate.builder()
                .gateName("Production Promotion")
                .required(true)
                .defaultApprover("devops-team")
                .autoApproveWhen() // Never auto-approve production
                    .never()
                .timeoutMinutes(120)
                .build())
            .rollbackPolicy(RollbackPolicy.builder()
                .reversible(true)
                .operations(
                    KubernetesRollout.undo(),
                    SchemaRollback.dropAnalyticsTables(),
                    FeatureFlagRestore.disable("analytics_enabled"),
                    DataCleanup.deleteEvents()
                )
                .autoRollbackOn(ApprovalRejection.class, HealthCheckFailure.class)
                .timeoutSeconds(600)
                .build())
            .timeout(30, TimeUnit.MINUTES)
            .build()
    )
    .build();
```

### 2. Node Definition DSL

```java
// Reusable node builder
WorkflowNode createAnalysisNode(String nodeId, String executor) {
    return WorkflowNode.builder()
        .id(nodeId)
        .type(NodeType.REQUIREMENT_ANALYSIS)
        .executor(executor)
        .dependsOn(/* predecessors */)
        .entryGate(gate()
            .passThrough() // No preconditions
            .build())
        .exitGate(gate()
            .artifactProduced("spec.md")
            .build())
        .retryPolicy(
            RetryPolicy.builder()
                .maxRetries(1)
                .doNotRetryOn(ValidationError.class) // Analysis errors are deterministic
                .build()
        )
        .timeoutSeconds(1800) // 30 minutes
        .build();
}
```

### 3. Gate DSL

```java
// Entry Gate: Preconditions
Gate entryGate = gate()
    .dependencyCheck(/* predecessorNodeIds */)
    .artifactCheck("schema.yml", "config.yaml")
    .policyCheck(SecurityPolicy.all(), CompliancePolicy.dataProtection)
    .resourceCheck("database_available", "test_env_online")
    .failureAction(BLOCK) // or WARN or LOG
    .build();

// Exit Gate: Postconditions
Gate exitGate = gate()
    .artifactProduced("code.jar", "test_results.json")
    .schemaValid("artifact_schema")
    .metricsAbove("test_coverage", 0.80)
    .metricsAbove("code_quality_score", 8.5)
    .policyCompliant(CodeQualityPolicy.all())
    .onFailure(BLOCK) // or WARN or TRIGGER_REPLAN
    .build();

// Validation Gate:  At any point
Gate validationGate = gate()
    .when("artifact_count").equals(3)
    .when("all_tests").equal("PASS")
    .when("error_rate").lessThan(0.001)
    .orWhen()
    .when("performance").meetsBaseline()
    .build();
```

### 4. Retry Policy DSL

```java
RetryPolicy retryWithExponentialBackoff = RetryPolicy.builder()
    .maxRetries(5)
    .backoff(BackoffStrategy.EXPONENTIAL)
    .initialDelaySeconds(1)
    .maxDelaySeconds(32)
    .maxDurationSeconds(600) // Total budget across all retries
    .retryOnExceptions(
        TemporaryFailure.class,
        ResourceExhaustion.class,
        ExternalServiceError.class
    )
    .doNotRetryOnExceptions(
        ValidationError.class,
        ConfigurationError.class,
        PreconditionNotMet.class
    )
    .build();

// Or use predefined strategies
RetryPolicy quickRetry = RetryPolicy.quickRetry(); // 3 retries, fixed 5s
RetryPolicy aggressiveRetry = RetryPolicy.aggressiveRetry(); // 10 retries, exponential
RetryPolicy noRetry = RetryPolicy.noRetry(); // fail immediately
```

### 5. Rollback Policy DSL

```java
RollbackPolicy rollbackWithReversals = RollbackPolicy.builder()
    .reversible(true)
    .operations(
        GitOperation.reset(beforeCommit),
        SchemaOperation.rollback(previousVersion),
        ConfigOperation.restore(previousConfig),
        CleanupOperation.deleteArtifacts(created),
        ResourceOperation.terminate(provisioned)
    )
    .autoRollbackTriggers(
        ValidationFailure.class,
        ApprovalRejection.class,
        DownstreamFailure.class
    )
    .timeoutSeconds(300)
    .build();

// Or for situations where rollback is not possible
RollbackPolicy noRollback = RollbackPolicy.builder()
    .reversible(false)
    .fallbackAction(REPLAN) // Can't undo, so replan from here
    .build();
```

### 6. Parallel Paths DSL

```java
// Define two tasks that run in parallel after Requirements
Workflow parallel = new WorkflowBuilder("parallel-example")
    .root(
        node("requirements")
            .parallelNext("design", "testing")
            .build()
    )
    .node(
        node("design")
            .dependsOn("requirements")
            .next("sync")
            .build()
    )
    .node(
        node("testing")
            .dependsOn("requirements")
            .next("sync")
            .build()
    )
    .synchronization("sync")
        .waitFor("design", "testing")
        .next("implementation")
        .build()
    )
    .build();
```

### 7. Conditional Branching DSL

```java
// Route based on exit gate result
Workflow conditional = new WorkflowBuilder("conditional-example")
    .node(
        node("validation")
            .exitGate(gate()
                .producesValidationResult() // PASS or FAIL
                .build())
            .conditionalNext(result ->
                result.isPASS()
                    ? route().to("release").branch("success")
                    : route().to("replan").branch("failure")
                                          .scope(FROM_IMPLEMENTATION)
                                          .reason("Requirements not met")
            )
            .build()
    )
    .build();
```

### 8. Re-Planning Trigger DSL

```java
ReplanTrigger intelligentReplan = ReplanTrigger.builder()
    .on(ASSUMPTION_BROKEN)
    .assumption("persistenceModelCompatible")
    .when(discoveredDuring(IMPLEMENTATION))
    .regenerateDAGFrom("architecture")
    .preserveArtifacts(onlyValidatedOnes())
    .maxReplans(3) // Prevent infinite loops
    .build();

ReplanTrigger onValidationFailure = ReplanTrigger.builder()
    .on(VALIDATION_FAILED)
    .when(failureCode().equals("requirements_not_met"))
    .regenerateDAGFrom("implementation")
    .addContextFromFailure(previousFailureDetails)
    .build();
```

### 9. Approval Gate DSL

```java
ApprovalGate codeReview = ApprovalGate.builder()
    .gateName("Code Review")
    .required(true)
    .appliesTo(NodeType.IMPLEMENTATION)
    .defaultApprover("senior-backend-engineer") // or role "architects"
    .autoApproveWhen(
        coverage().greaterThan(0.85)
            .and(securityScan().passed())
            .and(noBlockingLints())
    )
    .requiresHumanIfAny(
        newDatabaseSchema(),
        externalServiceIntegration(),
        securitySensitiveCode()
    )
    .timeoutMinutes(24)
    .escalationTo("engineering-manager", after(12).hours())
    .build();

ApprovalGate productionPromotion = ApprovalGate.builder()
    .gateName("Production Release Approval")
    .required(true)
    .appliesTo(NodeType.RELEASE_READY)
    .defaultApprover("devops-team")
    .autoApproveWhen() // NEVER auto-approve in production
        .never()
    .requiresHumanSignoff(true)
    .requiresSecondApproval(true) // 2/2 approvals needed
    .timeoutMinutes(120)
    .build();
```

### 10. Artifact Flow DSL

```java
// Describe artifact dependencies
Artifact apiSchema = Artifact.builder()
    .id("api_schema")
    .type(ArtifactType.API_SPEC)
    .name("Analytics API Schema")
    .producedBy("architecture")
    .storageLocation("src/main/resources/api/analytics-openapi.yml")
    .version("1.0.0")
    .dependencies() // What this artifact depends on
        .requires("requirements_spec")
        .requires("acceptance_criteria")
        .build()
    .validationRules(
        schema().matches("openapi_v3_schema"),
        endpoints().count().greaterThan(2),
        requestBodies().allRequiredFieldsDocumented()
    )
    .build();

// Describe artifact consumption
artifactFlow()
    .from("requirements")
        .produces("requirement_spec.md")
    .to("architecture", "testplan")
        .consumes("requirement_spec.md")
    .from("architecture")
        .produces("api_schema.yml", "db_schema.sql")
    .to("implementation")
        .consumes(all())
    .from("implementation")
        .produces("code.jar", "migration.sql")
    .to("testing")
        .consumes(all())
    .build();
```

### 11. Metrics and Observability DSL

```java
ExecutionMetrics metrics = ExecutionMetrics.builder()
    .trackSuccessRate(totalCompleted, totalFailed)
    .trackMTTR(from(failure).to(recovery))
    .trackE2ELatency(from(start).to(end))
    .trackRetryFrequency(retryCount / totalExecutions)
    .trackRollbackFrequency(rollbackCount / totalExecutions)
    .track(customMetric("analytics_api_deployment_time"))
    .track(customMetric("code_review_cycle_time"))
    .exportTo(PrometheusRegistry)
    .build();

// Thresholds for alerts
MetricThreshold successRateThreshold = MetricThreshold.builder()
    .metric("success_rate")
    .lowerBound(0.95)
    .alert(AlertLevel.WARNING, when(below(0.95)))
    .alert(AlertLevel.CRITICAL, when(below(0.90)))
    .build();
```

### 12. Decision Lineage DSL

```java
// Automatically tracked by engine
Decision decisionMade = Decision.builder()
    .id(UUID.randomUUID())
    .madeBy(nodeId, executionId)
    .type(DecisionType.ARCHITECTURE_CHOICE)
    .reasoning("Chose bloom filter for collision detection optimization because...")
    .outcome("Implementation will precheck codes using bloom filter before DB insert")
    .timestamp(Instant.now())
    .reversible(true)
    .relatedDecisions(previousDecisions) // Build lineage chain
    .metadata(
        context("Attempting to reduce p99 latency from 500ms to < 100ms"),
        assumption("Collision rate will remain < 1%"),
        alternativeConsidered("Allocator service approach"),
        alternativeConsidered("Smaller code space approach")
    )
    .build();

// Query decision lineage
DecisionLineage lineage = execution.getDecisionLineage();
List<Decision> whyWereThere5Retries = lineage.traceDecision("retry_policy_maxRetries_5");
List<Decision> whyWasRollbackTriggered = lineage.traceDecision("automatic_rollback_triggered");
```

---

## Full Examples

### Example 1: Greenfield Scenario DSL

```java
public class GreenfielAnalyticsWorkflow {
    public static Workflow create() {
        return new WorkflowBuilder("greenfield-analytics-api")
            .description("Add Analytics API for URL usage tracking")
            .scenario(ScenarioType.GREENFIELD)
            .root(requirementAnalysisNode())
            .parallelNodes(architectureDesignNode(), testPlanningNode())
            .synchronization(syncPoint())
            .sequentialNodes(
                implementationNode(),
                testingNode(),
                validationNode(),
                releaseNode()
            )
            .build();
    }

    private static WorkflowNode requirementAnalysisNode() {
        return node("requirements")
            .type(REQUIREMENT_ANALYSIS)
            .executor("analyze_analytics_requirements")
            .exitGate(gate()
                .artifactProduced("requirement_spec.md")
                .build())
            .timeout(30, MINUTES)
            .build();
    }

    // ... other node definitions
}

// Usage
Workflow workflow = GreenfielAnalyticsWorkflow.create();
WorkflowExecution execution = orchestrationEngine.execute(workflow);
execution.awaitCompletion();
```

### Example 2: Brownfield Scenario DSL

```java
public class BrownfieldOptimizationWorkflow {
    public static Workflow create() {
        return new WorkflowBuilder("brownfield-collision-optimization")
            .description("Optimize collision detection in URL shortener")
            .scenario(ScenarioType.BROWNFIELD)
            .constraint(BackwardCompatibility.required())
            .constraint(NoBreakingChanges.enforced())
            // ... investigation phase
            .node(faultAnalysisNode())
            .node(soakTestNode())
            .synchronization(investigationSync())
            // ... implementation phase
            .node(implementationNode())
            .node(regressionTestingNode())
            .node(performanceTestingNode())
            .node(validationNode())
            .node(releaseNode())
            .build();
    }
}
```

### Example 3: Ambiguous Scenario DSL

```java
public class AmbiguousReliabilityWorkflow {
    public static Workflow create() {
        return new WorkflowBuilder("ambiguous-improve-reliability")
            .description("Improve reliability of URL shortener")
            .scenario(ScenarioType.AMBIGUOUS)
            // Decomposition phase
            .node(decomposeRequirementNode())
            // Investigation phase (parallel)
            .parallelNodes(faultAnalysisNode(), soakTestingNode())
            .synchronization(investigationSync())
            // Implementation phase
            .node(implementAllFixesNode()
                .detailedDescription("""
                    Implement all identified reliability improvements:
                    - Database pool tuning
                    - GC/heap optimization
                    - Retry fallback strategy
                """))
            // Validation phase
            .node(validationTestingNode()
                .successCriteria("""
                    - Reliability improved >= 15%
                    - Pool exhaustion reduced >= 70%
                    - GC pause reduced >= 50%
                    - Retry exhaustion eliminated
                """))
            // Release phase
            .node(releaseNode())
            .build();
    }
}
```

---

## DSL Extension Points

The DSL is extensible for custom scenarios:

```java
// Custom executor
WorkflowNode customNode = node("custom-analysis")
    .executor(new CustomExecutor() {
        @Override
        public ExecutionResult execute(ExecutionContext ctx) {
            // Custom logic here
            return ExecutionResult.success()
                .artifact("output.md", /* path */)
                .metric("custom_metric", value)
                .build();
        }
    })
    .build();

// Custom validation rule
Gate customGate = gate()
    .validationRule(new ValidationRule() {
        @Override
        public ValidationResult validate(ExecutionContext ctx) {
            // Custom validation logic
            return ValidationResult.pass();
        }
    })
    .build();

// Custom retry logic
RetryPolicy customRetry = RetryPolicy.custom(
    new RetryStrategy() {
        @Override
        public boolean shouldRetry(ExecutionFailure failure, int attemptCount) {
            return attemptCount < 10 && failure.isTemporary();
        }

        @Override
        public Duration delayBeforeRetry(int attemptCount) {
            return Duration.ofSeconds(attemptCount * 5);
        }
    }
);
```

---

## Summary

The Workflow DSL enables:
- **Type-safe** workflow definitions (catch errors at compile time)
- **Composable** node building (reusable patterns)
- **Readable** specifications (fluent API)
- **Testable** workflows (mock executors, replay scenarios)
- **Extensible** design (custom executors, validators, strategies)

Workflows defined via DSL are directly executable by the orchestration engine—no separate interpreter needed.

