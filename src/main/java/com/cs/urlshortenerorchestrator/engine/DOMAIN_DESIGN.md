# Orchestration Engine - Domain Model Design

## Core Concepts

### 1. Workflow & DAG Structure

**Workflow**
- Unique ID
- Name/Description
- Root node (entry point)
- All nodes keyed by ID
- Current execution state
- Created timestamp
- Metadata (scenario type, requirement, etc.)

**WorkflowNode**
- Unique ID within workflow
- Type (REQUIREMENT_ANALYSIS, ARCHITECTURE_DESIGN, IMPLEMENTATION, TESTING, VALIDATION, RELEASE_READY)
- Entry Gate (preconditions to execute)
- Executor (agent/function to run)
- Exit Gate (postconditions to declare success)
- Retry Policy (max attempts, backoff)
- Approval Gate (requires human sign-off or automatic)
- Timeout
- Dependencies (predecessor node IDs)

**WorkflowEdge**
- Source node ID
- Target node ID
- Conditional? (can branch based on exit gate result)
- Sync point? (all predecessors must complete before this edge can traverse)

**WorkflowState**
- Current active node
- Completed nodes (ordered)
- Failed nodes (with reason)
- Pending nodes
- State enum: RUNNING, WAITING_FOR_APPROVAL, WAITING_FOR_RETRY, COMPLETED, FAILED, ROLLED_BACK, SAFE_STOPPED

### 2. Execution & History

**Execution**
- Unique ID
- Workflow ID
- Node ID
- Attempt number
- Status: PENDING, IN_PROGRESS, SUCCESS, FAILED, APPROVED, REJECTED
- Start time, end time
- Duration
- Produced artifacts (file paths, git commits, etc.)
- Error details (if failed)
- Logs
- Decision lineage (which decisions led to this execution)

**ExecutionMetrics** (built incrementally as workflow progresses)
- Total nodes: N
- Completed: X
- Failed: Y
- Success rate: X/(X+Y)
- Retries executed: count
- Rollbacks executed: count
- MTTR (mean time to resolution): avg(time from failure to recovery)
- E2E latency: total workflow duration
- Approval time: sum of approval gate wait times

### 3. Artifacts & Outputs

**Artifact**
- Unique ID
- Type (CODE, SCHEMA, API_SPEC, TEST, DOCUMENTATION, etc.)
- Name
- Produced by: node ID + execution ID
- Storage location (file path, git commit, etc.)
- Metadata (language, framework, status)
- Validation status (if required)

### 4. Decisions & Lineage

**Decision**
- Unique ID
- Made by: node ID + execution ID
- Decision type (ARCHITECTURE_CHOICE, IMPLEMENTATION_STRATEGY, RETRY_ON_FAILURE, REPLAN, etc.)
- Reasoning/Context (why this decision?)
- Outcome (what changed as a result)
- Timestamp
- Reversible? (can it be rolled back)
- Related decisions (causality chain)

**DecisionLineage**
- Chain of decisions that led to current state
- Used for audit trail and explanability
- Enables "why" tracing

### 5. Validation & Gates

**ValidationResult**
- Unique ID
- Executed by: node ID
- Test coverage: % passing
- Error details
- Artifacts validated (which artifacts passed/failed)
- Status: PASS, FAIL
- Recommendations (detected via validation)

**Approval**
- Unique ID
- Gate name (high-impact change approval, schema change, etc.)
- Created timestamp
- Requires approval: bool
- Approved by: (user/role)
- Approval timestamp
- Status: PENDING, APPROVED, REJECTED
- Reason (if rejected)

### 6. Governance & Policies

**Policy**
- Unique ID
- Name (SECURITY_POLICY, COMPLIANCE_POLICY, CHANGE_CONTROL_POLICY, etc.)
- Rules (constraints to enforce)
- Enforceable? (can engine block execution)
- Applies to: node types

**RetryPolicy**
- Max retries: int (bounded)
- Backoff strategy: exponential, linear, fixed
- Retry conditions: which exceptions/failures trigger retry
- Max duration: total time budget for retries

**RollbackPolicy**
- Reversible operations (git reset, schema rollback, etc.)
- Automatic rollback triggers
- Manual rollback points

### 7. Scenario Specifics

**Scenario** (within workflow metadata)
- Type: GREENFIELD, BROWNFIELD, AMBIGUOUS
- Greenfield: requires full SDLC from requirements
- Brownfield: starts with existing code, focuses on improvements
- Ambiguous: starts with vague requirement, needs decomposition first

---

## State Transitions

```
PENDING → IN_PROGRESS → SUCCESS
          ↓
          FAILED → (retry?) → IN_PROGRESS
                 → (no retry) → FAILED
                 
FAILED → WAITING_FOR_APPROVAL (human decision)
      → APPROVED → ROLLBACK → ROLLED_BACK or RETRY
      → REJECTED → ROLLED_BACK

IN_PROGRESS → WAITING_FOR_APPROVAL (if approval gate)
           → APPROVED → SUCCESS
           → REJECTED → SAFE_STOPPED

SUCCESS → (all nodes done?) → COMPLETED
       → (next node failed) → could trigger replan
```

---

## Key Properties

### Entry Gate
Preconditions before node execution:
- Dependencies satisfied?
- Artifacts available?
- Policies allow execution?

### Exit Gate
Postconditions to declare success:
- Artifact produced?
- Validation passed?
- Policies satisfied?

### Adaptive Re-Planning
When upstream output changes or assumption broken:
- Detect: node discovers assumption mismatch
- Replan: regenerate DAG from current node onward
- Continue: resume with new plan
- Track: all replans in decision lineage

---

## Example: Greenfield Analytics API

```
Workflow (Greenfield: "Add Analytics API")
  ├─ RequirementAnalysis
  │   ├─ Entry: (none - root)
  │   ├─ Exit: requirement_spec artifact produced
  │   └─ Next: [ArchitectureDesign, TestPlan] (PARALLEL)
  │
  ├─ ArchitectureDesign
  │   ├─ Entry: requirement_spec artifact available
  │   ├─ Exit: api_schema, db_schema artifacts produced
  │   └─ Next: Implementation
  │
  ├─ TestPlan
  │   ├─ Entry: requirement_spec artifact available
  │   ├─ Exit: test_cases artifact produced
  │   └─ Next: Testing (waits for Implementation)
  │
  ├─ Implementation
  │   ├─ Entry: api_schema and db_schema artifacts required
  │   ├─ Approval gate: "code review required"
  │   ├─ Exit: code_artifact, migration_artifact produced
  │   └─ Next: Testing
  │
  ├─ Testing
  │   ├─ Entry: code_artifact + test_cases required
  │   ├─ Exit: test_results artifact (must PASS)
  │   ├─ Retry: up to 3x if transient failures
  │   └─ Next: Validation
  │
  ├─ Validation
  │   ├─ Entry: all artifacts collected
  │   ├─ Exit: validation_report (PASS/FAIL)
  │   └─ Next: Release (if PASS) or Replan (if FAIL)
  │
  └─ Release
      ├─ Entry: validation passed
      ├─ Approval gate: "promote to production?"
      └─ Exit: deployed_artifact
```

---

## Observability Integration

- **Every execution** is tracked: start → end, success → failure
- **Every decision** is logged with reasoning
- **Every approval** is recorded with timestamp and actor
- **Every retry** increments counter and records outcome
- **Every rollback** records reason and previous state
- **Metrics calculated continuously** as execution progresses


