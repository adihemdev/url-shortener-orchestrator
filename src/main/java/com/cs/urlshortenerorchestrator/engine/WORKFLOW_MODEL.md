# Orchestration Engine - Workflow Model Specification

## Overview

The Workflow Model defines how SDLC tasks are orchestrated as a directed acyclic graph (DAG) with first-class support for:
- **Task dependency graphs** with sequential and parallel execution paths
- **Synchronization points** (join nodes where parallel tasks converge)
- **Entry/Exit Gates** with validation and precondition checking
- **Approval checkpoints** for human-in-the-loop decisions
- **Retry/Fallback/Rollback policies** with bounded retries and backoff strategies
- **Re-planning triggers** that regenerate DAG when assumptions break

---

## Workflow Structure

### Node Types

Every task in a workflow is represented as a `WorkflowNode` with a specific type:

```
REQUIREMENT_ANALYSIS     → Understand and decompose the requirement
ARCHITECTURE_DESIGN      → Design solution architecture and schemas
TEST_PLANNING            → Plan test strategy and cases
IMPLEMENTATION           → Implement the solution
TESTING                  → Execute tests
VALIDATION               → Validate against requirements
RELEASE_READY            → Prepare for production
APPROVAL                 → Human approval checkpoint (synchronous)
SYNCHRONIZATION          → Join point for parallel paths
REPLAN                   → Trigger workflow regeneration
ROLLBACK                 → Undo previous changes
```

### Node Anatomy

Each node has the following structure:

```
Node(id, type):
  ├─ dependsOnNodeIds: Set<String>          # Predecessor nodes (must complete before this runs)
  ├─ entryGate: Gate                         # Preconditions to run (BLOCKED, PASS_THROUGH, or VALIDATION)
  ├─ executor: String                        # Agent/function to execute (e.g., "analyze_requirements", "design_api")
  ├─ exitGate: Gate                          # Postconditions to declare success (BLOCKED, PASS_THROUGH, or VALIDATION)
  ├─ approvalGate: ApprovalGate              # Human approval (if required)
  ├─ retryPolicy: RetryPolicy                # Bounded retry configuration
  ├─ timeoutSeconds: int                     # Execution timeout
  └─ parallelPaths: Set<String>              # Node IDs that can run in parallel after this
```

### Gate Types

#### Entry Gate (preconditions before execution)
```
EntryGate:
  ├─ type: BLOCKED | PASS_THROUGH | VALIDATION
  ├─ validations: List<ValidationRule>      # Rules to validate
  │   ├─ dependency_check (predecessors complete)
  │   ├─ artifact_check (required artifacts exist)
  │   ├─ policy_check (governance policies allow)
  │   └─ resource_check (required resources available)
  └─ failureAction: BLOCK | WARN | LOG
```

#### Exit Gate (postconditions for success)
```
ExitGate:
  ├─ type: BLOCKED | PASS_THROUGH | VALIDATION
  ├─ validations: List<ValidationRule>      # Rules to validate
  │   ├─ artifact_produced (required artifacts created)
  │   ├─ quality_check (test coverage, code metrics)
  │   ├─ policy_compliance (satisfied policies)
  │   └─ schema_validation (artifact format valid)
  └─ failureAction: BLOCK | WARN | TRIGGER_REPLAN
```

#### Approval Gate (human decision)
```
ApprovalGate:
  ├─ required: bool                          # Is approval mandatory?
  ├─ gateName: String                        # "Code Review", "Security Audit", etc.
  ├─ appliesTo: Set<NodeType>               # Which nodes require this approval
  ├─ defaultApprover: String                 # Role/team for approval
  ├─ autoApproveRules: List<Rule>           # Rules for auto-approval
  └─ timeoutMinutes: int                     # How long to wait for approval
```

---

## Dependency Graph Structure

### Sequential Execution
```
[RequirementAnalysis] → [ArchitectureDesign] → [Implementation]

Each node waits for ALL predecessors to complete successfully.
If ANY predecessor fails, this node is BLOCKED unless retry is triggered.
```

### Parallel Execution (Forks)
```
                ┌─→ [ArchitectureDesign]
[RequirementAnalysis] ┤
                └─→ [TestPlanning]

After RequirementAnalysis completes:
- Architecture and TestPlanning can run CONCURRENTLY
- Neither blocks the other
- Both must complete before synchronization point
```

### Synchronization (Joins)
```
[ArchitectureDesign] ──┐
                       ├─→ [Synchronization] → [Implementation]
[TestPlanning] ────────┘

Synchronization node:
- Waits for ALL predecessors (Architecture + TestPlanning)
- Only proceeds when both complete
- Can validate that parallel paths produced compatible artifacts
- Single successor (Implementation) after join
```

### Conditional Branching
```
[Validation] ──→ [Pass?]
                  ├─→ YES: [Release]
                  └─→ NO: [Replan]

Exit gate result determines next path.
Decision is YES/NO/REPLAN based on exit gate validation outcome.
```

---

## Retry Policy Specification

```
RetryPolicy:
  ├─ maxRetries: int (1-10)                 # Hard limit on attempts
  ├─ backoffStrategy: LINEAR | EXPONENTIAL | FIXED
  │   ├─ LINEAR: 2s, 4s, 6s, 8s, ...
  │   ├─ EXPONENTIAL: 1s, 2s, 4s, 8s, 16s, ...
  │   └─ FIXED: 5s, 5s, 5s, ...
  ├─ initialDelaySeconds: int
  ├─ maxDelaySeconds: int
  ├─ maxDurationSeconds: int                # Total time budget (not just delay)
  ├─ retryOnExceptions: List<ExceptionType> # Which exceptions trigger retry
  │   ├─ TemporaryFailure (transient)
  │   ├─ ResourceExhaustion (rate limit, OOM)
  │   └─ ExternalServiceError (downstream unavailable)
  └─ doNotRetryOnExceptions: List<ExceptionType> # Which fail immediately
      ├─ ValidationError (logic error)
      ├─ ConfigurationError (misconfiguration)
      └─ PreconditionNotMet (missing artifact)
```

---

## Rollback Policy Specification

```
RollbackPolicy:
  ├─ isReversible: bool                     # Can changes be undone?
  ├─ reversibleOperations: List<Operation>
  │   ├─ git_reset (reset to previous commit)
  │   ├─ schema_rollback (database schema downgrade)
  │   ├─ config_restore (configuration restore)
  │   ├─ artifact_cleanup (delete created artifacts)
  │   └─ resource_cleanup (terminate created resources)
  ├─ autoRollbackTriggers: List<Trigger>
  │   ├─ on_validation_failure (exit gate fails)
  │   ├─ on_downstream_failure (dependent node fails)
  │   └─ on_approval_rejection (approval gate rejected)
  └─ rollbackTimeoutSeconds: int            # How long rollback can take
```

---

## Validation Gate Specification

```
ValidationRule:
  ├─ name: String                           # "api_schema_valid", "test_coverage > 80%"
  ├─ type: ARTIFACT_EXISTS | SCHEMA_VALID | METRICS_OK | POLICY_CHECK
  ├─ targetArtifact: String                 # Which artifact to validate
  ├─ expectedCondition: Condition
  │   ├─ exists() → artifact file exists
  │   ├─ schema_matches(expectedSchema) → artifact format valid
  │   ├─ metrics.coverage > 0.80 → test coverage threshold
  │   ├─ policy_satisfied(policyName) → governance check
  │   └─ custom(condition) → user-defined validation
  ├─ severity: ERROR | WARNING | INFO
  └─ onFailure: BLOCK | WARN | TRIGGER_REPLAN
```

---

## Re-Planning Triggers

When assumptions break or new info emerges, the workflow can regenerate its DAG:

```
ReplanTrigger:
  ├─ triggerType: ASSUMPTION_BROKEN | VALIDATION_FAILED | EXPLICIT_REQUEST
  ├─ assumptionBroken:
  │   ├─ persistenceModelIncompatible (discovered during implementation)
  │   ├─ performanceRequirementUnmet (discovered during testing)
  │   ├─ securityAssumptionInvalid (discovered during validation)
  │   └─ downstreamIntegrationImpossible (external system incompatible)
  ├─ replanScope: FULL | PARTIAL (from current node)
  ├─ replanContext: {
  │     previousAttempt: Execution,
  │     failureReason: String,
  │     assumptionsMissing: List<String>,
  │     suggestedNewPlan: String
  │   }
  └─ maxReplans: int (prevent infinite loops)
```

---

## Artifact Flow

Artifacts are produced by nodes and consumed by successors:

```
Artifact:
  ├─ id: String
  ├─ type: CODE | SCHEMA | API_SPEC | TEST | DOCUMENTATION | DEPLOYMENT
  ├─ producedBy: NodeId + ExecutionId
  ├─ storageLocation: String (file path or git commit)
  ├─ version: String
  ├─ dependencies: List<ArtifactId> (which input artifacts required)
  └─ validationStatus: PENDING | VALID | INVALID
```

### Artifact Dependency Chain
```
[RequirementAnalysis] produces: requirement_spec.md
  ↓
[ArchitectureDesign] depends on: requirement_spec.md, produces: api_schema.yml, db_schema.sql
  ↓
[Implementation] depends on: api_schema.yml + db_schema.sql, produces: code_artifact, migration_artifact
  ↓
[Testing] depends on: code_artifact + test_cases.md, produces: test_results.json
  ↓
[Validation] depends on: test_results.json + code_artifact, produces: validation_report.md
  ↓
[Release] depends on: validation_report.md, produces: deployed_artifact
```

---

## Decision Lineage

Every decision made during workflow execution is tracked:

```
Decision:
  ├─ id: String
  ├─ madeBy: NodeId + ExecutionId
  ├─ decisionType: ARCHITECTURE_CHOICE | IMPLEMENTATION_STRATEGY | RETRY_ON_FAILURE | REPLAN | APPROVAL
  ├─ reasoning: String (why this decision?)
  ├─ outcome: String (what changed)
  ├─ timestamp: Instant
  ├─ reversible: bool (can it be undone)
  ├─ relatedDecisions: List<DecisionId> (causality chain)
  └─ metadata: {
       context: String,
       assumptions: List<String>,
       alternatives_considered: List<String>
     }

DecisionLineage:
  ├─ decisions: List<Decision> (ordered)
  ├─ getDecisionPath(): List<Decision> (why we're here)
  └─ canRollback(untilDecision): bool (revert to previous state)
```

---

## Execution Model

### Execution Lifecycle
```
1. PENDING
   ├─ Entry gate validation
   ├─ All dependencies satisfied?
   └─ Artifacts available?

2. IN_PROGRESS (with timeout)
   ├─ Execute node's executor function
   ├─ Produce artifacts
   ├─ Capture logs and metrics

3. SUCCESS or FAILED
   ├─ Exit gate validation
   ├─ Artifacts produced as expected?
   ├─ Quality thresholds met?

4. If FAILED:
   ├─ Check retry policy
   ├─ If retries left: PENDING (restart from step 1)
   ├─ If no retries: check approval gate
   ├─ If approval gate exists: WAITING_FOR_APPROVAL
   └─ Else: FAILED (workflow stops)

5. WAITING_FOR_APPROVAL
   ├─ Human reviews and decides
   ├─ If APPROVED: proceed to next node
   ├─ If REJECTED: trigger rollback

6. ROLLED_BACK
   ├─ Undo changes (revert git, schema, etc.)
   ├─ Restore previous state
   └─ Re-plan or stop workflow
```

### Execution Metrics
```
ExecutionMetrics (calculated incrementally):
  ├─ totalNodes: int
  ├─ completedNodes: int
  ├─ failedNodes: int
  ├─ successRate: double (completed / (completed + failed))
  ├─ totalRetries: int
  ├─ totalRollbacks: int
  ├─ averageMTTRSeconds: double (mean time to resolution after failure)
  ├─ e2eLatencySeconds: double (total workflow duration)
  ├─ approvalWaitSeconds: double (sum of approval gate wait times)
  └─ replanCount: int
```

---

## Workflow Definition Checklist

When defining a new workflow, ensure:

- [ ] All nodes have unique IDs
- [ ] All dependencies are acyclic (no loops)
- [ ] All artifact dependencies are satisfied (producer before consumer)
- [ ] All entry gates specify what they need
- [ ] All exit gates specify what success looks like
- [ ] Parallel paths have synchronization points before joining
- [ ] Approval gates are placed at appropriate checkpoints
- [ ] Retry policies are bounded (maxRetries ≤ 10, maxDurationSeconds set)
- [ ] Rollback policies match reversible operations
- [ ] Re-planning triggers are specific and bounded
- [ ] All executors are defined and available
- [ ] Timeouts are realistic for each node

---

## Example: DAG Visualization

```
                    ┌─→ [Architecture] ──────┐
                    │                        │
[Requirements] ─────┤                        ├─→ [Sync] → [Implementation]
                    │                        │
                    └─→ [TestPlan] ──────────┘

Sequential:
  Requirements → Sync (waits for both Architecture and TestPlan)
  Sync → Implementation

Parallel:
  Architecture and TestPlan run concurrently after Requirements

Retry+Rollback:
  Architecture: maxRetries=3, exponential backoff, auto-rollback on schema validation failure
  Implementation: maxRetries=5, manual approval on failure

Approval:
  Implementation: requires code review before proceeding to Testing
  Release: requires production promotion approval

Re-planning:
  If Validation fails: trigger replan from Implementation phase
  New plan: redesign architecture, re-implement, re-test
```

