# Orchestration Engine - Scenario Definitions

This document defines three concrete workflow scenarios that demonstrate the orchestration engine capabilities:
1. **Greenfield**: Adding new feature (Analytics API)
2. **Brownfield**: Improving existing system (Optimize collision detection)
3. **Ambiguous**: Starting with vague requirement (Improve reliability)

---

## Scenario 1: Greenfield - Add Analytics API

**Objective**: Add a new Analytics API to track URL usage (view counts, geographic data, referrer tracking).

**Characteristics**:
- Clear requirements (add Analytics API)
- No existing implementation to preserve
- Full SDLC from scratch
- Parallel architecture and test planning
- Approval gates for code review and production promote

### Workflow DAG

```
┌─────────────────────────────────────────────────────────────────────┐
│ Greenfield: Add Analytics API for URL Usage Tracking                │
│ Scenario Type: GREENFIELD                                           │
│ RequiredArtifacts: [requirement_spec, api_schema, code, tests]     │
└─────────────────────────────────────────────────────────────────────┘

Phase 1: Requirements Phase
┌──────────────────────────────────────
│ RequirementAnalysis (node-1)
├──────────────────────────────────────
│ Type: REQUIREMENT_ANALYSIS
│ Executor: analyze_analytics_requirements
│ DependsOn: []
│
│ Entry Gate: PASS_THROUGH
││ (root node, no preconditions)
│
│ Exit Gate: VALIDATION
│  ├─ artifact_produced("requirement_spec.md")
│  ├─ metrics.sections > 3 (Analysis, API Endpoints, Data Model)
│  └─ schema_validation(requirement_spec_schema)
│
│ Produces:
│  ├─ requirement_spec.md (500-1000 words)
│  │   ├─ Problem statement: Why analytics matters
│  │   ├─ Use cases: What queries teams need
│  │   ├─ API endpoints: GET /api/v1/urls/{id}/analytics
│  │   ├─ Data model: Events, sessions, geography
│  │   └─ SLAs: Response time, data freshness
│  ├─ acceptance_criteria.md
│  │   ├─ Analytics data persisted after 1 minute
│  │   ├─ API responds in < 500ms
│  │   └─ Data accuracy > 99%
│  └─ constraints.md (privacy, storage, performance)
│
│ Exit Condition: SUCCESS
│ Timeout: 30 minutes
│ Retry: maxRetries=1, no retry on validation failure
└──────────────────────────────────────

Phase 2: Parallel Tasks [Architecture + Test Planning]

┌──────────────────────────────────────
│ ArchitectureDesign (node-2)
├──────────────────────────────────────
│ Type: ARCHITECTURE_DESIGN
│ Executor: design_analytics_architecture
│ DependsOn: [node-1]  (after RequirementAnalysis completes)
│ ParallelWith: [node-3]  (TestPlanning can run concurrently)
│
│ Entry Gate: VALIDATION
│  ├─ dependency_check([node-1])
│  ├─ artifact_check("requirement_spec.md", "acceptance_criteria.md")
│  └─ policy_check(SecurityPolicy.dataClassification)
│
│ Exit Gate: VALIDATION
│  ├─ artifact_produced("api_schema.yml", "db_schema.sql", "architecture_diagram.md")
│  ├─ schema_validation(openapi_v3_schema)
│  ├─ artifact_count() == 3
│  └─ policy_compliance(SecurityPolicy, PerformancePolicy)
│
│ Produces:
│  ├─ api_schema.yml (OpenAPI 3.0)
│  │   ├─ GET /api/v1/analytics/urls/{url_id}
│  │   ├─ POST /api/v1/analytics/events (for tracking)
│  │   ├─ RequestBody: event_type, timestamp, user_id
│  │   └─ ResponseBody: view_count, geography, referrers
│  ├─ db_schema.sql
│  │   ├─ CREATE TABLE analytics_events (...) PARTITION BY DATE
│  │   ├─ CREATE TABLE analytics_summary (...)
│  │   ├─ CREATE INDEX idx_url_date
│  │   └─ WITH data_retention = 90 DAYS
│  ├─ architecture_diagram.md
│  │   ├─ Event ingestion: HTTP endpoint
│  │   ├─ Stream processor: Kafka / Timeseries
│  │   ├─ Storage: Partition by URL+Date
│  │   └─ Query API: Read-optimized views
│  └─ migration_strategy.md
│      ├─ Phase 1: Create schema (backward compatible)
│      ├─ Phase 2: Dual-write (new + old)
│      └─ Phase 3: Cutover
│
│ Exit Condition: SUCCESS
│ Timeout: 60 minutes
│ Retry: maxRetries=2, exponential backoff, don't retry validation
│
│ Approval Gate: NONE (architecture review via code review)
└──────────────────────────────────────

┌──────────────────────────────────────
│ TestPlanning (node-3)
├──────────────────────────────────────
│ Type: TEST_PLANNING
│ Executor: plan_analytics_tests
│ DependsOn: [node-1]  (after RequirementAnalysis completes)
│ ParallelWith: [node-2]  (ArchitectureDesign can run concurrently)
│
│ Entry Gate: VALIDATION
│  ├─ dependency_check([node-1])
│  ├─ artifact_check("requirement_spec.md", "acceptance_criteria.md")
│  └─ resource_check("test_env_available")
│
│ Exit Gate: VALIDATION
│  ├─ artifact_produced("test_cases.md", "test_strategy.md")
│  ├─ metrics.test_coverage_plan > 80  (% of requirements covered)
│  └─ schema_validation(test_plan_schema)
│
│ Produces:
│  ├─ test_cases.md
│  │   ├─ Unit tests: Analytics service (isolated)
│  │   ├─ Integration tests: Event ingestion → Database
│  │   ├─ API tests: Endpoint contracts
│  │   ├─ Performance tests: Response time < 500ms
│  │   └─ Data accuracy tests: Aggregation correctness
│  ├─ test_strategy.md
│  │   ├─ Coverage targets: 85% code, 80% requirements
│  │   ├─ Test pyramid: 60% unit, 30% integration, 10% E2E
│  │   ├─ Mock strategy: Mock Kafka, DB for unit tests
│  │   └─ Performance baselines
│  └─ test_fixtures.md
│      ├─ Sample events (1M+ records)
│      ├─ Geolocation coordinates
│      └─ Referrer variations
│
│ Exit Condition: SUCCESS
│ Timeout: 45 minutes
│ Retry: maxRetries=1
│
│ Approval Gate: NONE
└──────────────────────────────────────

Phase 3: Synchronization Point [Wait for both Architecture & TestPlanning]

┌──────────────────────────────────────
│ Synchronization (node-4)
├──────────────────────────────────────
│ Type: SYNCHRONIZATION
│ Executor: sync_architecture_and_tests
│ DependsOn: [node-2, node-3]  (both must complete)
│
│ Entry Gate: VALIDATION
│  ├─ dependency_check([node-2, node-3])  # Both predecessors SUCCESS?
│  ├─ artifact_check("api_schema.yml", "db_schema.sql", "test_cases.md")
│  └─ compatibility_check:
│      └─ do_schema_and_tests_align?  # API endpoints covered by tests?
│
│ Exit Gate: PASS_THROUGH
│
│ Exit Condition: SUCCESS (merged artifacts ready)
│ Timeout: 10 minutes
└──────────────────────────────────────

Phase 4: Implementation Phase

┌──────────────────────────────────────
│ Implementation (node-5)
├──────────────────────────────────────
│ Type: IMPLEMENTATION
│ Executor: implement_analytics_api
│ DependsOn: [node-4]  (after synchronization)
│
│ Entry Gate: VALIDATION
│  ├─ dependency_check([node-4])
│  ├─ artifact_check(["api_schema.yml", "db_schema.sql", "test_cases.md"])
│  ├─ policy_check(CodeQualityPolicy: linting, formatting)
│  └─ resource_check(dev_env_available, git_repo_writable)
│
│ Exit Gate: VALIDATION
│  ├─ artifact_produced("code_artifact.jar", "migration_artifact.sql")
│  ├─ metrics.code.coverage >= 0.70  (minimum)
│  ├─ metrics.code.cyclomatic_complexity < 10 (per method)
│  ├─ policy_compliance(CodeQualityPolicy, SecurityPolicy)
│  └─ no_breaking_changes_to_url_service
│
│ Produces:
│  ├─ code_artifact.jar (Spring Boot + analytics module)
│  │   ├─ AnalyticsController.java
│  │   ├─ AnalyticsService.java
│  │   ├─ Event.java (domain model)
│  │   ├─ EventRepository.java (JPA)
│  │   └─ EventIntegration.java (Kafka producer)
│  ├─ migration_artifact.sql (V002_add_analytics_schema.sql)
│  │   ├─ Run on app startup (Flyway automated)
│  │   ├─ Backward compatible with existing data
│  │   └─ Partition strategy for performance
│  ├─ git_commit_hash (implementation commit)
│  └─ code_review_checklist.md
│      ├─ [ ] API contracts match schema
│      ├─ [ ] Database indices optimized
│      ├─ [ ] Error handling complete
│      └─ [ ] Logging sufficient for debugging
│
│ Exit Condition: SUCCESS
│ Timeout: 120 minutes
│
│ RetryPolicy:
│  ├─ maxRetries: 3
│  ├─ backoffStrategy: exponential (1s, 2s, 4s)
│  ├─ retryOnExceptions: [CompilationError, EnvironmentSetupError]
│  └─ doNotRetryOnExceptions: [ArchitectureViolation, ValidationError]
│
│ ApprovalGate:
│  ├─ gateName: "Code Review"
│  ├─ required: true
│  ├─ appliesTo: [IMPLEMENTATION]
│  ├─ defaultApprover: "senior-backend-engineer"
│  ├─ autoApproveRules:
│  │   └─ coverage >= 85% AND no_security_issues
│  └─ timeoutMinutes: 24  (24hr to review)
│
│ RollbackPolicy:
│  ├─ isReversible: true
│  ├─ reversibleOperations:
│  │   ├─ git_reset (before this commit)
│  │   ├─ schema_rollback (drop new tables)
│  │   └─ config_restore (remove analytics feature flag)
│  ├─ autoRollbackTriggers: [validation_failure, approval_rejection]
│  └─ rollbackTimeoutSeconds: 300
│
│ ReplanTrigger:
│  ├─ if Validation_gate fails with "schema_incompatible":
│  │   └─ Trigger replan from ArchitectureDesign
│  └─ if Validation_gate fails with "performance_insufficient":
│      └─ Trigger replan from ArchitectureDesign
└──────────────────────────────────────

Phase 5: Testing Phase

┌──────────────────────────────────────
│ Testing (node-6)
├──────────────────────────────────────
│ Type: TESTING
│ Executor: run_analytics_tests
│ DependsOn: [node-5]  (after implementation + approval)
│
│ Entry Gate: VALIDATION
│  ├─ dependency_check([node-5])
│  ├─ artifact_check(["code_artifact.jar", "test_cases.md"])
│  ├─ approval_check (node-5 approved?)
│  └─ resource_check(test_env_available, db_available)
│
│ Exit Gate: VALIDATION
│  ├─ artifact_produced("test_results.json", "coverage_report.html")
│  ├─ metrics.all_tests_passed == true
│  ├─ metrics.code_coverage >= 0.75  (must improve from implementation)
│  ├─ metrics.performance_tests_passed == true
│  ├─ metrics.integration_tests_passed == true
│  └─ onFailure: TRIGGER_REPLAN  (failed tests → code quality issue)
│
│ Produces:
│  ├─ test_results.json
│  │   ├─ unit_tests: 45 PASS, 0 FAIL
│  │   ├─ integration_tests: 18 PASS, 0 FAIL
│  │   ├─ performance_tests: 12 PASS, 0 FAIL
│  │   ├─ total_duration: 240 seconds
│  │   └─ flaky_tests: 0
│  ├─ coverage_report.html (80%+ coverage)
│  └─ performance_report.md
│      ├─ Event ingestion latency: median 50ms, p99 200ms
│      ├─ Analytics query latency: median 150ms, p99 450ms
│      └─ Database load: CPU 35%, Memory 45%
│
│ Exit Condition: SUCCESS
│ Timeout: 180 minutes
│
│ RetryPolicy:
│  ├─ maxRetries: 3
│  ├─ backoffStrategy: linear (10s, 20s, 30s)
│  ├─ maxDurationSeconds: 600  (10 minutes of retries max)
│  ├─ retryOnExceptions: [TransientTestFailure, ResourceUnavailable]
│  └─ doNotRetryOnExceptions: [AssertionError, TimeoutException]  (logic errors)
│
│ ApprovalGate: NONE (automated testing)
│
│ ReplanTrigger:
│  ├─ if coverage < 75%: trigger replan (implement missing tests)
│  ├─ if performance > baseline: trigger replan (optimize design)
│  └─ if regression detected: trigger replan (implementation issue)
└──────────────────────────────────────

Phase 6: Validation Phase

┌──────────────────────────────────────
│ Validation (node-7)
├──────────────────────────────────────
│ Type: VALIDATION
│ Executor: validate_analytics_requirements
│ DependsOn: [node-6]  (after testing)
│
│ Entry Gate: VALIDATION
│  ├─ dependency_check([node-6])
│  ├─ artifact_check(["test_results.json", "code_artifact.jar", "requirement_spec.md"])
│  └─ all_previous_steps_successful
│
│ Exit Gate: VALIDATION
│  ├─ artifact_produced("validation_report.md", "requirements_traceability.md")
│  ├─ validation_result: PASS or FAIL
│  ├─ metrics.requirements_coverage >= 0.95
│  └─ metrics.acceptance_criteria_met == true
│
│ Produces:
│  ├─ validation_report.md
│  │   ├─ Requirements vs Implementation traceability
│  │   ├─ Acceptance criteria validation: [✓] all 5 criteria met
│  │   ├─ Performance SLAs met: [✓] analytics API < 500ms
│  │   ├─ Data accuracy validation: [✓] > 99%
│  │   ├─ Privacy compliance: [✓] GDPR rules enforced
│  │   └─ Conclusion: [PASS] Ready for release
│  └─ requirements_traceability.md
│      ├─ Req 1.1: API endpoint → test_cases:10,11  → coverage: 100%
│      ├─ Req 1.2: Data model → test_cases:15,16,17 → coverage: 100%
│      └─ ... (all requirements traced)
│
│ Exit Condition: SUCCESS (PASS) or FAILURE (FAIL)
│ Timeout: 60 minutes
│
│ RetryPolicy: NONE (validation is deterministic)
│
│ Conditional Branch:
│  ├─ IF validation_result == PASS:
│  │   └─ Next: Release (node-8)
│  └─ IF validation_result == FAIL:
│      └─ Next: Replan (regenerate from Implementation)
└──────────────────────────────────────

Phase 7: Release Phase

┌──────────────────────────────────────
│ Release (node-8)
├──────────────────────────────────────
│ Type: RELEASE_READY
│ Executor: release_analytics_to_production
│ DependsOn: [node-7]  (after validation PASS)
│
│ Entry Gate: VALIDATION
│  ├─ dependency_check([node-7])
│  ├─ validation_result_check(node-7) == PASS
│  ├─ artifact_check(["validation_report.md", "code_artifact.jar"])
│  ├─ policy_check(ReleasePolicy.requiresApproval)
│  └─ resource_check(prod_deployment_available)
│
│ Exit Gate: VALIDATION
│  ├─ artifact_produced("deployed_artifact", "deployment_summary.md")
│  ├─ health_check(prod_analytics_api_responsive)
│  ├─ metrics.post_deployment_smoke_tests: all PASS
│  └─ metrics.data_fidelity_check: OK
│
│ Produces:
│  ├─ deployed_artifact (git tag v1.1.0)
│  ├─ deployment_summary.md
│  │   ├─ Deployment command: kubectl apply -f analytics-service-v1.1.0.yaml
│  │   ├─ Data migration: ran successfully
│  │   ├─ Post-deployment: smoke tests passed
│  │   ├─ Rollback procedure: git reset v1.0.0
│  │   └─ Validation: production queries returning data
│  └─ release_notes.md
│      ├─ New features: Analytics API, event tracking
│      ├─ Database changes: analytics_events table added
│      ├─ Prerequisites: Kafka instance available
│      └─ Rollback: No data loss, schema can be dropped
│
│ Exit Condition: SUCCESS
│ Timeout: 30 minutes
│
│ ApprovalGate:
│  ├─ gateName: "Production Promotion"
│  ├─ required: true
│  ├─ appliesTo: [RELEASE_READY]
│  ├─ defaultApprover: "devops-team"
│  ├─ autoApproveRules: NONE (always require human approval)
│  └─ timeoutMinutes: 120
│
│ RollbackPolicy:
│  ├─ isReversible: true
│  ├─ reversibleOperations:
│  │   ├─ kubernetes_rollout_undo
│  │   ├─ schema_rollback (drop analytics tables)
│  │   ├─ feature_flag_disable (disable analytics API)
│  │   └─ data_cleanup (delete analytics events)
│  ├─ autoRollbackTriggers: [approval_rejected, health_check_failed]
│  └─ rollbackTimeoutSeconds: 600
└──────────────────────────────────────
```

### Artifacts Flow Diagram

```
RequirementAnalysis
  ├─ requirement_spec.md
  │   ├─→ used by ArchitectureDesign
  │   └─→ used by TestPlanning
  ├─ acceptance_criteria.md
  │   └─→ used by Validation (requirement coverage check)
  └─ constraints.md

ArchitectureDesign
  ├─ api_schema.yml
  │   ├─→ used by Implementation
  │   └─→ used by Validation (API contract traceability)
  ├─ db_schema.sql
  │   └─→ used by Implementation
  └─ architecture_diagram.md

TestPlanning
  └─ test_cases.md
      └─→ used by Testing

Implementation
  ├─ code_artifact.jar
  │   ├─→ used by Testing
  │   ├─→ used by Validation
  │   └─→ used by Release
  └─ migration_artifact.sql

Testing
  ├─ test_results.json
  │   └─→ used by Validation
  └─ coverage_report.html

Validation
  └─ validation_report.md
      └─→ used by Release
```

### Key Characteristics

| Aspect | Greenfield Analytics API |
|--------|--------------------------|
| **Scenario Type** | GREENFIELD |
| **Phases** | 8 (Requirements → Release) |
| **Parallel Paths** | Architecture + TestPlanning (2 concurrent) |
| **Synchronization Points** | 1 (after parallel phase) |
| **Approval Gates** | 2 (Code Review, Production Promotion) |
| **Retry Points** | ArchitectureDesign (2x), Implementation (3x), Testing (3x) |
| **Rollback Points** | Implementation, Release |
| **Replan Triggers** | Implementation (schema/performance), Testing (coverage/performance), Validation (requirements not met) |
| **Total Artifacts** | 18 (requirements, schema, code, tests, reports) |
| **Max Workflow Duration** | ~8 hours (with sequential execution) or ~5 hours (with parallelism) |

---

## Scenario 2: Brownfield - Optimize Collision Detection

**Objective**: Optimize the existing URL shortener's collision detection mechanism to reduce retry overhead while maintaining uniqueness guarantees.

**Characteristics**:
- Existing implementation must be preserved
- Focus on performance improvement
- No API changes (backward compatible)
- Requires regression testing (no breaking changes)
- Investigation phase to diagnose current bottleneck

### Workflow DAG

```
┌─────────────────────────────────────────────────────────────────────┐
│ Brownfield: Optimize Collision Detection in URL Shortener          │
│ Scenario Type: BROWNFIELD                                          │
│ ExistingSystem: UrlService with SecureRandom collision detection   │
│ ConstraintPreserve: [API contracts, database schema, short code]   │
└─────────────────────────────────────────────────────────────────────┘

Phase 1: Investigation Phase

┌──────────────────────────────────────
│ RequirementAnalysis (node-1)
├──────────────────────────────────────
│ Type: REQUIREMENT_ANALYSIS
│ Executor: analyze_collision_optimization_requirement
│ DependsOn: []
│
│ Entry Gate: PASS_THROUGH
│
│ Exit Gate: VALIDATION
│  ├─ artifact_produced("optimization_spec.md", "performance_baseline.md")
│  └─ metrics.baseline_established == true
│
│ Produces:
│  ├─ optimization_spec.md
│  │   ├─ Problem: Current implementation retries up to 5x on collision
│  │   ├─ Pain point: 0.2-0.5 second latency for collision retry loop
│  │   ├─ Goal: Reduce to < 0.1 second 99th percentile
│  │   ├─ Constraint: NOT breaking API changes
│  │   ├─ Approach options to consider:
│  │   │   ├─ Option A: Hierarchical bloom filter (fast negative check)
│  │   │   ├─ Option B: Allocator service (pre-allocate code ranges)
│  │   │   └─ Option C: Smaller code space (7 chars instead of 6)
│  │   └─ Success metric: p99 latency < 0.1s
│  ├─ performance_baseline.md
│  │   ├─ Current metrics (measured on staging):
│  │   ├─ Average shortening latency: 10ms (no collision)
│  │   ├─ With collision: 200-400ms (retry loop)
│  │   ├─ Collision rate: ~0.5% (1 in 200)
│  │   ├─ p99 latency: 480ms
│  │   ├─ Error rate: 0
│  │   └─ Database row count: 1.2M URLs
│  ├─ improvement_targets.md
│  │   ├─ Reduce collision retry p99 from 480ms to < 100ms
│  │   ├─ Maintain 99.99% success rate (≤ 1 failure per 10k)
│  │   └─ No API contract changes
│  └─ constraint_verification.md
│      ├─ Existing short code format: [a-zA-Z0-9]{6}
│      ├─ Cannot change: API response format, database schema
│      ├─ Can change: internal optimization, database indices
│      └─ Risk assessment: Medium (live system, existing users)
│
│ Exit Condition: SUCCESS
│ Timeout: 45 minutes
│ Retry: maxRetries=1
└──────────────────────────────────────

Phase 2: Investigation + Parallel Design

┌──────────────────────────────────────
│ InvestigateCurrentImplementation (node-2)
├──────────────────────────────────────
│ Type: ARCHITECTURE_DESIGN (investigation sub-task)
│ Executor: analyze_current_url_service
│ DependsOn: [node-1]
│ ParallelWith: [node-3]
│
│ Entry Gate: VALIDATION
│  ├─ dependency_check([node-1])
│  └─ artifact_check(["optimization_spec.md"])
│
│ Exit Gate: VALIDATION
│  ├─ artifact_produced("current_implementation_analysis.md", "bottleneck_report.md")
│  └─ findings.documented == true
│
│ Produces:
│  ├─ current_implementation_analysis.md
│  │   ├─ Code review: UrlService collision detection
│  │   ├─ Current approach: SecureRandom + loop with exponential backoff
│  │   ├─ Retry logic:
│  │   │   ├─ Generate random code
│  │   │   ├─ Insert into database
│  │   │   ├─ If UNIQUE constraint violation: retry (up to 5x)
│  │   │   └─ Backoff: 1ms, 2ms, 4ms, 8ms, 16ms
│  │   ├─ Thread safety: SecureRandom.nextInt() is thread-safe ✓
│  │   ├─ Code coverage: 95% ✓
│  │   └─ Known issues:
│  │       ├─ Each failed attempt hits database (I/O overhead)
│  │       ├─ No pre-check before database insert
│  │       └─ Exponential backoff may not scale well as collision rate rises
│  └─ bottleneck_report.md
│      ├─ Root cause: Database round-trip for each collision
│      ├─ Metrics:
│      │   ├─ Failed DB inserts: 500/day (0.5% of 100k daily URLs)
│      │   ├─ Average retry count: 1.2 attempts
│      │   ├─ Database server CPU impact: 2-3% (from collision retries)
│      │   └─ Application thread contention: Low (each has own SecureRandom)
│      ├─ Opportunities:
│      │   ├─ Cache recent codes in local bloom filter
│      │   ├─ Pre-check before database insert
│      │   └─ Batch check for code uniqueness
│      └─ Recommendation: Implement bloom filter + batch preview
│
│ Exit Condition: SUCCESS
│ Timeout: 60 minutes
│ Retry: maxRetries=2
└──────────────────────────────────────

┌──────────────────────────────────────
│ DesignOptimizedApproach (node-3)
├──────────────────────────────────────
│ Type: ARCHITECTURE_DESIGN
│ Executor: design_collision_optimization
│ DependsOn: [node-1]
│ ParallelWith: [node-2]
│
│ Entry Gate: VALIDATION
│  ├─ dependency_check([node-1])
│  └─ artifact_check(["optimization_spec.md"])
│
│ Exit Gate: VALIDATION
│  ├─ artifact_produced("optimized_design.md", "algorithm_spec.md", "migration_plan.md")
│  └─ design.technically_sound == true
│
│ Produces:
│  ├─ optimized_design.md
│  │   ├─ Chosen approach: Dual-layer collision detection
│  │   │   ├─ Layer 1 (client-side): Local bloom filter (64KB, prob error 0.1%)
│  │   │   ├─ Layer 2 (server-side): Database insert + retry (fallback)
│  │   │   └─ Layer 3 (optimization): Batch preview before insert
│  │   ├─ Implementation strategy:
│  │   │   ├─ Add 2nd index on url_mapping.short_code (UNIQUE violation → 5x retry)
│  │   │   ├─ Add in-memory bloom filter (256MB → 1M codes, 0.1% FP rate)
│  │   │   ├─ Sync bloom filter periodically from DB
│  │   │   └─ Warm bloom filter on startup (load all existing codes)
│  │   ├─ Backward compatibility: ✓ No API changes
│  │   ├─ Database impact: ✓ One new index (negligible cost)
│  │   ├─ Performance gain: 90% reduction in retries (9 out of 10 collisions prevented in memory)
│  │   └─ Rollback risk: Low (can disable bloom filter via flag)
│  ├─ algorithm_spec.md
│  │   ├─ Bloom filter configuration:
│  │   │   ├─ Size: 256 MB (2^28 bits)
│  │   │   ├─ Hash functions: 3 (MurmurHash3)
│  │   │   ├─ False positive rate: 0.1% (acceptable)
│  │   │   └─ Capacity: 1M codes
│  │   ├─ Sync process:
│  │   │   ├─ Frequency: Every 5 minutes
│  │   │   ├─ Query: SELECT short_code FROM url_mapping
│  │   │   ├─ Add to bloom filter (idempotent)
│  │   │   └─ Timeout: 1 second
│  │   ├─ False positives handling:
│  │   │   └─ When filter says "exists" but DB says "available": try alternate code
│  │   └─ Memory budget: 256MB within acceptable heap size
│  └─ migration_plan.md
│      ├─ Phase 1 (Day 1): Deploy with bloom filter disabled (feature flag off)
│      ├─ Phase 2 (Day 2): Enable for 10% of shortening requests
│      ├─ Phase 3 (Day 3): Enable for 50% of requests
│      ├─ Phase 4 (Day 4): Enable for all requests (100%)
│      └─ Rollback: Disable feature flag → use old algorithm
│
│ Exit Condition: SUCCESS
│ Timeout: 90 minutes
│ Retry: maxRetries=2
│
│ Approval Gate: NONE (design review in code review)
└──────────────────────────────────────

Phase 3: Synchronization

┌──────────────────────────────────────
│ SyncDesignAndAnalysis (node-4)
├──────────────────────────────────────
│ Type: SYNCHRONIZATION
│ Executor: verify_design_aligns_with_analysis
│ DependsOn: [node-2, node-3]
│
│ Entry Gate: VALIDATION
│  ├─ dependency_check([node-2, node-3])
│  └─ compatibility_check:
│      └─ does_design_address_bottleneck? (from investigation)
│
│ Exit Gate: PASS_THROUGH
│
│ Exit Condition: SUCCESS
│ Timeout: 10 minutes
└──────────────────────────────────────

Phase 4: Implementation Phase

┌──────────────────────────────────────
│ Implementation (node-5)
├──────────────────────────────────────
│ Type: IMPLEMENTATION
│ Executor: implement_collision_optimization
│ DependsOn: [node-4]
│
│ Entry Gate: VALIDATION
│  ├─ dependency_check([node-4])
│  ├─ artifact_check(["optimized_design.md", "algorithm_spec.md"])
│  └─ policy_check(BackwardCompatibilityPolicy)
│
│ Exit Gate: VALIDATION
│  ├─ artifact_produced("optimized_code.jar", "migration.sql", "feature_flag_config.yaml")
│  ├─ metrics.api_unchanged == true  (no breaking changes)
│  ├─ metrics.database_schema_compatible == true
│  ├─ metrics.code_coverage >= 0.85
│  └─ policy_compliance(BackwardCompatibilityPolicy)
│
│ Produces:
│  ├─ optimized_code.jar
│  │   ├─ BloomFilterCollisionDetector.java
│  │   │   ├─ Implements CollisionDetector interface
│  │   │   ├─ add(shortCode) / likely_exists(shortCode)
│  │   │   ├─ sync() / loadFromDatabase()
│  │   │   └─ Feature flag: shortener.bloom_filter.enabled
│  │   ├─ UrlServiceOptimized.java
│  │   │   ├─ Check bloom filter BEFORE database insert
│  │   │   ├─ If filter says "probably exists": try alternate code
│  │   │   ├─ Fallback to existing retry logic (5x)
│  │   │   └─ Backward compatible (same API)
│  │   └─ CollisionOptimizationMetrics.java
│  │       ├─ Track: bloom filter hits, misses, false positives
│  │       ├─ Track: retry reduction statistics
│  │       └─ Exportable to monitoring (Prometheus)
│  ├─ migration.sql
│  │   ├─ No schema changes needed
│  │   └─ Optional: Add index on short_code (already exists for uniqueness)
│  ├─ feature_flag_config.yaml
│  │   ├─ shortener.bloom_filter.enabled: false (default)
│  │   ├─ shortener.bloom_filter.sync_interval_minutes: 5
│  │   ├─ shortener.bloom_filter.false_positive_rate: 0.001
│  │   └─ shortener.bloom_filter.max_memory_mb: 256
│  └─ performance_improvement_report.md (pre-testing)
│      ├─ Estimated retry reduction: 90%
│      ├─ Estimated latency improvement: 400ms → 50ms p99
│      └─ Resource cost: 256 MB heap + 2 threads for sync
│
│ Exit Condition: SUCCESS
│ Timeout: 150 minutes
│
│ RetryPolicy:
│  ├─ maxRetries: 3
│  ├─ backoffStrategy: exponential
│  ├─ retryOnExceptions: [CompilationError, DependencyIssue]
│  └─ doNotRetryOnExceptions: [BackwardCompatibilityViolation, SchemaViolation]
│
│ ApprovalGate:
│  ├─ gateName: "Backward Compatibility Review"
│  ├─ required: true
│  ├─ appliesTo: [IMPLEMENTATION]
│  ├─ defaultApprover: "senior-architect"
│  ├─ autoApproveRules:
│  │   └─ api_unchanged AND no_breaking_changes AND coverage >= 85%
│  └─ timeoutMinutes: 24
│
│ RollbackPolicy:
│  ├─ isReversible: true
│  ├─ reversibleOperations:
│  │   ├─ git_reset (revert optimization commit)
│  │   ├─ feature_flag_disable (turn off bloom filter)
│  │   └─ restart_app (reload with old code)
│  ├─ autoRollbackTriggers: [approval_rejected, compatibility_violation]
│  └─ rollbackTimeoutSeconds: 300
│
│ ReplanTrigger:
│  ├─ if compatibility violation discovered: replan from Design phase
│  └─ if performance regression: replan from Investigation phase
└──────────────────────────────────────

Phase 5: Testing Phase (Regression + Performance)

┌──────────────────────────────────────
│ RegressionTesting (node-6)
├──────────────────────────────────────
│ Type: TESTING
│ Executor: run_regression_tests
│ DependsOn: [node-5]
│
│ Entry Gate: VALIDATION
│  ├─ dependency_check([node-5])
│  ├─ artifact_check(["optimized_code.jar"])
│  └─ approval_check (Implementation approved)
│
│ Exit Gate: VALIDATION
│  ├─ artifact_produced("regression_results.json", "regression_report.md")
│  ├─ metrics.all_existing_tests_pass == true  (100% backward compat)
│  ├─ metrics.api_contracts_verified == true
│  ├─ metrics.no_breaking_changes == true
│  └─ onFailure: TRIGGER_REPLAN
│
│ Produces:
│  ├─ regression_results.json
│  │   ├─ unit_tests: 42 PASS (all UrlService tests)
│  │   ├─ integration_tests: 8 PASS (all shortening scenarios)
│  │   ├─ contract_tests: 5 PASS (API endpoints)
│  │   ├─ smoke_tests: 100 shortening operations → 100 SUCCESS
│  │   ├─ performance_tests:
│  │   │   ├─ Without collision: 10ms (unchanged)
│  │   │   ├─ With collision (bloom filter): 15ms (vs 200-400ms before)
│  │   │   └─ Improvement: 93% reduction
│  │   └─ total_duration: 180s
│  └─ regression_report.md
│      ├─ Backward compatibility check: PASS
│      ├─ API contracts: PASS
│      ├─ Database schema: PASS
│      ├─ Short code format: PASS (unchanged)
│      ├─ Error handling: PASS (unchanged)
│      ├─ No new failures detected
│      └─ Ready for performance validation
│
│ Exit Condition: SUCCESS
│ Timeout: 120 minutes
│
│ RetryPolicy:
│  ├─ maxRetries: 3
│  ├─ backoffStrategy: linear (transient failures)
│  ├─ retryOnExceptions: [TransientTestFailure, ResourceUnavailable]
│  └─ doNotRetryOnExceptions: [AssertionError]  (logic errors = replan)
└──────────────────────────────────────

┌──────────────────────────────────────
│ PerformanceTesting (node-7)
├──────────────────────────────────────
│ Type: TESTING (specialized performance test)
│ Executor: run_performance_benchmarks
│ DependsOn: [node-5]
│
│ Entry Gate: VALIDATION
│  ├─ dependency_check([node-5])
│  └─ artifact_check(["optimized_code.jar", "performance_baseline.md"])
│
│ Exit Gate: VALIDATION
│  ├─ artifact_produced("performance_results.json", "performance_comparison.md")
│  ├─ metrics.latency_improved >= 0.85  (85% improvement)
│  ├─ metrics.p99_latency_under_100ms == true
│  ├─ metrics.no_regression_in_normal_path == true
│  └─ onFailure: TRIGGER_REPLAN
│
│ Produces:
│  ├─ performance_results.json
│  │   ├─ Test scenario: 10k shortening requests with 0.5% collision rate
│  │   ├─ Old implementation:
│  │   │   ├─ Mean: 35ms, P50: 30ms, P95: 150ms, P99: 480ms
│  │   │   ├─ Total retries: 50
│  │   │   └─ Failures: 0
│  │   ├─ New implementation:
│  │   │   ├─ Mean: 15ms, P50: 12ms, P95: 25ms, P99: 85ms
│  │   │   ├─ Total retries: 5  (90% reduction!)
│  │   │   ├─ Bloom filter hits: 45 (prevented DB round-trip)
│  │   │   ├─ Bloom filter false positives: 0
│  │   │   └─ Failures: 0
│  │   └─ Improvement summary:
│  │       ├─ Mean latency: 35ms → 15ms (57% reduction)
│  │       ├─ P99 latency: 480ms → 85ms (82% reduction) ✓ GOAL!
│  │       └─ Retry operations: 50 → 5 (90% reduction)
│  └─ performance_comparison.md
│      ├─ Baseline vs optimized:
│      │   ├─ Shortening without collision: 10ms vs 12ms (no regression)
│      │   ├─ Shortening with collision: 400ms vs 25ms (94% improvement)
│      │   ├─ Memory overhead: +256MB (acceptable)
│      │   └─ CPU overhead: +2% (sync process)
│      ├─ Success criteria met:
│      │   ├─ [✓] P99 latency < 100ms (achieved 85ms)
│      │   ├─ [✓] Retry reduction 85% (achieved 90%)
│      │   ├─ [✓] No regression on normal path
│      │   └─ [✓] Zero new failures
│      └─ Recommendation: APPROVE FOR RELEASE
│
│ Exit Condition: SUCCESS
│ Timeout: 180 minutes
│
│ RetryPolicy:
│  ├─ maxRetries: 2
│  ├─ backoffStrategy: fixed (10s)
│  ├─ maxDurationSeconds: 30  (performance tests should be stable)
│  └─ retryOnExceptions: [EnvironmentUnavailable]
│
│ ReplanTrigger:
│  ├─ if P99 > 150ms: insufficient improvement, replan
│  └─ if regression on normal path > 5%: compatibility issue, replan
│
│ Note: Dependencies are not enforced between RegressionTesting and
│       PerformanceTesting; both test node-5, but can run in parallel
└──────────────────────────────────────

Phase 6: Validation Phase

┌──────────────────────────────────────
│ Validation (node-8)
├──────────────────────────────────────
│ Type: VALIDATION
│ Executor: validate_optimization_goals
│ DependsOn: [node-6, node-7]  (both testing phases must pass)
│
│ Entry Gate: VALIDATION
│  ├─ dependency_check([node-6, node-7])
│  ├─ artifact_check(["regression_results.json", "performance_results.json"])
│  └─ validation_checks:
│      ├─ regression_result == PASS
│      └─ performance_result == PASS
│
│ Exit Gate: VALIDATION
│  ├─ artifact_produced("validation_report.md", "readiness_checklist.md")
│  ├─ validation_result: PASS or FAIL
│  ├─ metrics.optimization_goals_met == true
│  └─ metrics.no_breaking_changes == true
│
│ Produces:
│  ├─ validation_report.md
│  │   ├─ Optimization goals:
│  │   │   ├─ [✓] Reduce p99 latency < 100ms (achieved 85ms)
│  │   │   ├─ [✓] Maintain zero broken API contracts
│  │   │   ├─ [✓] Maintain > 99.99% success rate
│  │   │   ├─ [✓] No production data loss
│  │   │   └─ [✓] Backward compatibility verified
│  │   ├─ Testing coverage:
│  │   │   ├─ Regression tests: 55/55 PASS
│  │   │   ├─ Performance tests: All thresholds met
│  │   │   ├─ Load test: 10k ops/day equivalent, 0 failures
│  │   │   └─ Code coverage: 87% (delta +2% from implementation)
│  │   ├─ Risk assessment:
│  │   │   ├─ Rollback readiness: High (feature flag toggle)
│  │   │   ├─ Data safety: High (no schema changes)
│  │   │   └─ User impact: None (transparent optimization)
│  │   └─ Conclusion: [PASS] Ready for production release
│  └─ readiness_checklist.md
│      ├─ [✓] All code reviewed
│      ├─ [✓] All tests passing
│      ├─ [✓] Performance baselines exceeded
│      ├─ [✓] Backward compatibility verified
│      ├─ [✓] Rollback procedure documented
│      ├─ [✓] Monitoring configured (prometheus metrics)
│      ├─ [✓] Runbooks prepared
│      └─ READY FOR RELEASE
│
│ Exit Condition: SUCCESS (PASS)
│ Timeout: 60 minutes
│ Retry: NONE
│
│ Conditional Branch:
│  ├─ IF validation_result == PASS → Release (node-9)
│  └─ IF validation_result == FAIL → Replan (from Implementation)
└──────────────────────────────────────

Phase 7: Release Phase

┌──────────────────────────────────────
│ Release (node-9)
├──────────────────────────────────────
│ Type: RELEASE_READY
│ Executor: release_optimization_to_production
│ DependsOn: [node-8]  (validation PASS)
│
│ Entry Gate: VALIDATION
│  ├─ dependency_check([node-8])
│  ├─ validation_result_check == PASS
│  ├─ artifact_check(["validation_report.md", "feature_flag_config.yaml"])
│  └─ deployment_readiness_check:
│      ├─ Production system stable
│      └─ Rollback procedure available
│
│ Exit Gate: VALIDATION
│  ├─ artifact_produced("deployment_summary.md", "monitoring_dashboard_link")
│  ├─ production_health_checks: OK
│  └─ optimization_active: true
│
│ Produces:
│  ├─ deployment_summary.md
│  │   ├─ Deployment method: Feature flag enable + reboot (blue-green)
│  │   ├─ Phase 1 (Canary): Enable for 5% of traffic (50k users)
│  │   │   └─ Monitor for 4 hours
│  │   ├─ Phase 2: Enable for 50% of traffic
│  │   │   └─ Monitor for 2 hours
│  │   ├─ Phase 3: Enable for 100% of traffic
│  │   ├─ Success criteria:
│  │   │   ├─ Error rate ≤ baseline
│  │   │   ├─ P99 latency < 100ms
│  │   │   └─ Bloom filter false positive rate < 0.1%
│  │   ├─ Rollback plan:
│  │   │   ├─ If error rate increases: disable feature flag
│  │   │   ├─ Session duration: < 5 minutes
│  │   │   └─ No data loss (no schema changes)
│  │   └─ Runbook: SEE ops/brownfield-optimization-runbook.md
│  ├─ monitoring_dashboard_link
│  │   └─ /metrics/shortener/collision-optimization
│  │       ├─ Bloom filter hits (should be > 90%)
│  │       ├─ Retry count trend (should decrease)
│  │       ├─ P99 latency trend (should be stable < 100ms)
│  │       └─ False positive rate (should be < 0.1%)
│  └─ release_notes.md
│      ├─ Title: "URL Shortener Collision Detection Optimization"
│      ├─ Summary: "Reduced p99 latency by 82% using bloom filter pre-check"
│      ├─ User impact: None (transparent, faster shortening)
│      ├─ Admin action required: None (automatic monitoring)
│      ├─ Rollback: Disable feature flag if issues detected
│      └─ Next steps: Monitor metrics for 7 days
│
│ Exit Condition: SUCCESS
│ Timeout: 30 minutes
│
│ ApprovalGate:
│  ├─ gateName: "Production Release Approval"
│  ├─ required: true
│  ├─ appliesTo: [RELEASE_READY]
│  ├─ defaultApprover: "devops-engineer"
│  ├─ autoApproveRules:
│  │   └─ validation_passed AND monitoring_configured
│  └─ timeoutMinutes: 60
│
│ RollbackPolicy:
│  ├─ isReversible: true
│  ├─ reversibleOperations:
│  │   ├─ feature_flag_disable (disable bloom filter)
│  │   ├─ app_restart (reload with fallback logic)
│  │   ├─ monitoring_reset (clear dashboards)
│  │   └─ metrics_cleanup (archive collections)
│  ├─ autoRollbackTriggers:
│  │   ├─ error_rate_increase > 2%
│  │   ├─ latency_p99_increase > 50%
│  │   └─ approval_rejected
│  └─ rollbackTimeoutSeconds: 300
└──────────────────────────────────────
```

### Key Characteristics

| Aspect | Brownfield Optimization |
|--------|------------------------|
| **Scenario Type** | BROWNFIELD |
| **Phases** | 7 (Investigation → Release) |
| **Parallel Paths** | Investigation + Design (2 concurrent) |
| **Synchronization Points** | 1 (after parallel analysis) |
| **Approval Gates** | 2 (Backward Compatibility, Production Release) |
| **Testing Strategy** | Regression (no breaking changes) + Performance (improvement verification) |
| **Key Risk Mitigation** | Feature flag, blue-green deployment, automatic rollback |
| **Rollback Complexity** | Low (feature flag toggle, no schema changes) |
| **Replan Triggers** | Compatibility violation, performance regression, approval rejection |
| **Total Artifacts** | 15 (analysis, design, code, tests, metrics, ops runbook) |
| **Max Workflow Duration** | ~6 hours |

---

## Scenario 3: Ambiguous - Improve Reliability

**Objective**: Start with vague requirement "Improve reliability" and decompose into concrete tasks (discover failure modes, propose improvements, implement and test).

**Characteristics**:
- Requirement is intentionally ambiguous
- Requires decomposition phase to clarify
- Multiple sub-workflows for different improvement areas
- Evidence-based decision making (metrics-driven)
- Possible replans if investigation reveals unexpected issues

### Workflow DAG

```
┌─────────────────────────────────────────────────────────────────────┐
│ Ambiguous: Improve Reliability of URL Shortener                    │
│ Scenario Type: AMBIGUOUS                                           │
│ InitialRequirement: "Improve reliability" (VAGUE)                  │
│ Decomposition Strategy: Evidence-based via monitoring and soak-test│
└─────────────────────────────────────────────────────────────────────┘

Phase 1: Decomposition (Clarify the Vague Requirement)

┌──────────────────────────────────────
│ RequirementAnalysis (node-1)
├──────────────────────────────────────
│ Type: REQUIREMENT_ANALYSIS
│ Executor: decompose_reliability_requirement
│ DependsOn: []
│
│ Entry Gate: PASS_THROUGH
│
│ Exit Gate: VALIDATION
│  ├─ artifact_produced("reliability_analysis.md", "failure_modes_report.md", "improvement_roadmap.md")
│  └─ interpretations_documented = true
│
│ Produces:
│  ├─ reliability_analysis.md
│  │   ├─ Problem statement clarification:
│  │   │   ├─ Current reliability SLA: 99.9% (2.75 hours/month downtime)
│  │   │   ├─ Target reliability SLA: 99.95% (22 minutes/month downtime)
│  │   │   ├─ Gap: Reduce downtime by 5x
│  │   ├─ Possible interpretations of "reliability":
│  │   │   ├─ Interpretation A: Reduce API latency variance (tail latencies)
│  │   │   ├─ Interpretation B: Handle temporary failures better (retries, timeouts)
│  │   │   ├─ Interpretation C: Improve data consistency (transaction integrity)
│  │   │   ├─ Interpretation D: Better error handling and recovery
│  │   │   ├─ Interpretation E: Improve deployment safety (canary, rollback)
│  │   │   └─ Interpretation F: Detect and prevent cascading failures
│  │   ├─ Investigation plan:
│  │   │   ├─ Step 1: Analyze production logs for failures (last 30 days)
│  │   │   ├─ Step 2: Identify top 3 failure sources
│  │   │   ├─ Step 3: Estimate impact of each failure source
│  │   │   └─ Step 4: Determine which improvement provides best ROI
│  │   └─ Data collection plan:
│  │       ├─ Query monitoring: error rates, latencies, timeouts
│  │       ├─ Review logs: stack traces, root causes
│  │       ├─ Interview ops team: manual interventions
│  │       └─ Run soak test: 24-hour continuous load test
│  ├─ failure_modes_report.md (PLACEHOLDER for investigation results)
│  │   ├─ Will be filled by node-2 (FaultAnalysis)
│  │   └─ Expected content:
│  │       ├─ Failure rate: X% (top causes: transaction deadlocks, DB connection pool exhaustion, GC pauses)
│  │       ├─ MTTR (mean time to recovery): ~5 minutes
│  │       ├─ Incidents last 30 days: 8 (avg duration 15 min each)
│  │       └─ Estimated cost: $XXX per incident
│  └─ improvement_roadmap.md (DYNAMIC: updated after fault analysis)
│      ├─ Will be refined based on findings
│      ├─ Initial candidates:
│      │   ├─ Add connection pooling tuning (low effort, ~10% improvement)
│      │   ├─ Add circuit breaker for DB (medium effort, ~20% improvement)
│      │   ├─ Implement request batching (high effort, ~40% improvement)
│      │   ├─ Add observability instrumentation (medium effort, enables better debugging)
│      │   └─ Improve error handling in collision retry (low effort, fixes edge case)
│      └─ Next step: Fault analysis to prioritize
│
│ Exit Condition: SUCCESS
│ Timeout: 60 minutes
│ Retry: maxRetries=1
│
│ ReplanTrigger:
│  └─ If investigation in node-2 reveals infrastructure issue (not code issue)
│      → Trigger replan to infrastructure improvements
└──────────────────────────────────────

Phase 2: Evidence Collection (Parallel Investigation & Soak Test)

┌──────────────────────────────────────
│ FaultAnalysis (node-2)
├──────────────────────────────────────
│ Type: ARCHITECTURE_DESIGN (investigation)
│ Executor: analyze_failure_modes
│ DependsOn: [node-1]
│ ParallelWith: [node-3]
│
│ Entry Gate: VALIDATION
│  ├─ dependency_check([node-1])
│  └─ artifact_check(["reliability_analysis.md"])
│
│ Exit Gate: VALIDATION
│  ├─ artifact_produced("failure_modes_detailed.md", "failure_root_causes.json")
│  └─ metrics.top_3_causes_identified = true
│
│ Produces:
│  ├─ failure_modes_detailed.md
│  │   ├─ Analysis period: Last 90 days
│  │   ├─ Total incidents: 12
│  │   ├─ Top failure sources (ranked by frequency × impact):
│  │   │   ├─ ROOT CAUSE #1: Database connection pool exhaustion (45% of incidents)
│  │   │   │   ├─ Symptom: "Unable to get a connection, pool error"
│  │   │   │   ├─ Frequency: 1-2x per week
│  │   │   ├─ Average duration: 8 minutes
│  │   │   ├─ Affected users: ~50% (read-only queries fail, shortening works)
│  │   │   ├─ Manual recovery: Restart app
│  │   │   ├─ Root cause hypothesis: No max pool size limit (HikariCP default)
│  │   │   ├─ Fix effort: LOW (configuration only)
│  │   │   └─ Estimated reliability gain: 20%
│  │   │
│  │   ├─ ROOT CAUSE #2: GC pause on heap pressure (35% of incidents)
│  │   │   ├─ Symptom: "p99 latency spikes to 2-5 seconds"
│  │   │   ├─ Frequency: 1-2x per week
│  │   │   ├─ Average duration: 3-5 seconds (pause), 10 min (recovery)
│  │   │   ├─ Affected users: All (response timeout if > 5s)
│  │   │   ├─ Manual recovery: Monitor and wait
│  │   │   ├─ Root cause hypothesis: No heap size limit (runs with defaults)
│  │   │   ├─ Fix effort: MEDIUM (GC tuning, memory profiling)
│  │   │   ├─ Estimated reliability gain: 35%
│  │   │   └─ Investigation needed: Full heap dump analysis
│  │   │
│  │   └─ ROOT CAUSE #3: Unhandled exception in collision retry (20% of incidents)
│  │       ├─ Symptom: "ShortCodeGenerationException after max retries"
│  │       ├─ Frequency: 1x every 10 days (but high impact when happens)
│  │       ├─ Average duration: Request fails, user receives 500 error
│  │       ├─ Affected users: ~1% (those shortening during collision burst)
│  │       ├─ Manual recovery: User retries (usually succeeds)
│  │       ├─ Root cause hypothesis: Retry logic doesn't handle 100% saturation
│  │       ├─ Fix effort: LOW (add graceful fallback)
│  │       ├─ Estimated reliability gain: 15%
│  │       └─ Investigation needed: Test with extreme load
│  │
│  ├─ Validation opportunities:
│  │   ├─ [✓] Pool exhaustion fix + GC tuning: ~50% reliability gain
│  │   ├─ [✓] All three fixes combined: ~60% reliability gain
│  │   └─ [ ] Requires soak test to confirm (see node-3)
│  │
│  └─ failure_root_causes.json
│      ├─ cause: "database_pool_exhaustion"
│      │   ├─ frequency_per_week: 1-2
│      │   ├─ mean_duration_minutes: 8
│      │   ├─ user_impact_percentage: 50
│      │   ├─ fix_effort: "LOW"
│      │   └─ estimated_reliability_gain_percentage: 20
│      ├─ cause: "gc_pause_heap_pressure"
│      │   ├─ frequency_per_week: 1-2
│      │   ├─ mean_duration_minutes: 10
│      │   ├─ user_impact_percentage: 100
│      │   ├─ fix_effort: "MEDIUM"
│      │   └─ estimated_reliability_gain_percentage: 35
│      ├─ cause: "collision_retry_saturation"
│      │   ├─ frequency_per_week: 0.15
│      │   ├─ mean_duration_seconds: 5
│      │   ├─ user_impact_percentage: 1
│      │   ├─ fix_effort: "LOW"
│      │   └─ estimated_reliability_gain_percentage: 15
│      └─ recommendation: "Fix all three: combined gain ~60%, effort = LOW+MEDIUM+LOW"
│
│ Exit Condition: SUCCESS
│ Timeout: 120 minutes
│ Retry: maxRetries=1
└──────────────────────────────────────

┌──────────────────────────────────────
│ SoakTesting (node-3)
├──────────────────────────────────────
│ Type: TESTING (investigation via load)
│ Executor: run_soak_test
│ DependsOn: [node-1]
│ ParallelWith: [node-2]
│
│ Entry Gate: VALIDATION
│  ├─ dependency_check([node-1])
│  └─ resource_check(test_cluster_available)
│
│ Exit Gate: VALIDATION
│  ├─ artifact_produced("soak_test_results.json", "soak_test_analysis.md")
│  └─ metrics.at_least_one_failure_observed = true  (needed to confirm issues)
│
│ Produces:
│  ├─ soak_test_results.json
│  │   ├─ test_duration_hours: 24
│  │   ├─ total_requests: 2160000  (1000 req/s × 86400s)
│  │   ├─ success_rate: 0.9987  (99.87%)
│  │   ├─ failure_count: 2840
│  │   ├─ failure_breakdown:
│  │   │   ├─ pool_exhaustion_errors: 1400  (49%)
│  │   │   ├─ timeout_errors: 1000  (35%) - likely GC induced
│  │   │   ├─ shortcode_generation_failures: 440  (15%)
│  │   │   └─ other: 0  (0%)
│  │   ├─ latency_metrics:
│  │   │   ├─ p50: 12ms
│  │   │   ├─ p95: 45ms
│  │   │   ├─ p99: 250ms (normal)
│  │   │   ├─ p99.9: 2500ms (GC pause)
│  │   │   └─ max: 4800ms
│  │   ├─ gc_pause_events:
│  │   │   ├─ count: 8
│  │   │   ├─ avg_duration_ms: 1200
│  │   │   ├─ causation: "heap pressure > 85%"
│  │   │   └─ correlation_with_failures: "YES"
│  │   ├─ db_pool_events:
│  │   │   ├─ count: 6
│  │   │   ├─ max_active_connections: 32  (default HikariCP)
│  │   │   ├─ queue_wait_time_avg: 500ms
│  │   │   └─ correlation_with_failures: "YES"
│  │   └─ heap_metrics:
│  │       ├─ initial_heap: 1GB (default)
│  │       ├─ final_heap: 950MB
│  │       ├─ peak_heap: 1200MB (exceeds -Xmx)
│  │       └─ gc_frequency: every 5-10 seconds
│  │
│  └─ soak_test_analysis.md
│      ├─ Key findings:
│      │   ├─ [✓] Fault analysis is ACCURATE - same 3 failure modes reproduced
│      │   ├─ [✓] Pool exhaustion confirmed (need to increase pool size)
│      │   ├─ [✓] GC pressure confirmed (need heap tuning or refactoring)
│      │   ├─ [✓] Retry exhaustion confirmed (need fallback strategy)
│      │   └─ [✓] Recommendations from node-2 are VALIDATED
│      ├─ Severity ranking (by frequency × impact):
│      │   ├─ Priority 1: GC pause (affects 100% of users, ~1% fail)
│      │   ├─ Priority 2: Pool exhaustion (affects 50% of users, ~1% fail)
│      │   └─ Priority 3: Retry exhaustion (affects 1% of users, ~0.1% fail)
│      ├─ Improvement strategy:
│      │   ├─ Phase 1: Pool tuning (easy quick win)
│      │   ├─ Phase 2: GC/heap analysis (higher impact)
│      │   ├─ Phase 3: Retry fallback (edge case)
│      │   └─ Sequence: Do all 3 in parallel (combined low-medium effort)
│      └─ Next step: Design reliability improvement solution
│
│ Exit Condition: SUCCESS
│ Timeout: 1440 minutes (24 hours + analysis)
│ Retry: maxRetries=1
└──────────────────────────────────────

Phase 3: Synchronization After Investigation

┌──────────────────────────────────────
│ SyncInvestigations (node-4)
├──────────────────────────────────────
│ Type: SYNCHRONIZATION
│ Executor: consolidate_analysis_findings
│ DependsOn: [node-2, node-3]
│
│ Entry Gate: VALIDATION
│  ├─ dependency_check([node-2, node-3])
│  └─ findings_alignment_check:
│      └─ does_fault_analysis_match_soak_test? (should show same causes)
│
│ Exit Gate: VALIDATION
│  ├─ artifact_produced("consolidated_findings.md", "reliability_improvement_plan.md")
│  └─ consensus_on_next_steps = true
│
│ Produces:
│  ├─ consolidated_findings.md
│  │   ├─ Analysis method #1 (fault analysis): Identified 3 causes
│  │   ├─ Analysis method #2 (soak test): Reproduced same 3 causes
│  │   ├─ Confidence level: HIGH (both methods converged)
│  │   ├─ Top 3 causes (CONFIRMED):
│  │   │   ├─ 1. Database connection pool exhaustion (40-50% of failures)
│  │   │   ├─ 2. GC pause on heap pressure (35-45% of failures)
│  │   │   └─ 3. Retry exhaustion on collision burst (15-20% of failures)
│  │   ├─ Expected reliability gain if all fixed:
│  │   │   └─ Current: 99.87% (soak test) → Target: 99.98% (60% improvement)
│  │   └─ Effort estimate: LOW + MEDIUM + LOW = 1.5 weeks work
│  │
│  └─ reliability_improvement_plan.md
│      ├─ Proposed improvements (EVIDENCE-BASED):
│      │   ├─ Improvement A: Tune database connection pool
│      │   │   ├─ Action: Set HikariCP maxPoolSize=50 (from default 32)
│      │   │   ├─ Effort: LOW (config only)
│      │   │   ├─ Risk: Very low (pool will shrink back if load decreases)
│      │   │   ├─ Benefit: Reduce pool exhaustion by 80%
│      │   │   └─ Expected gain: +15% reliability
│      │   │
│      │   ├─ Improvement B: Optimize GC with heap tuning
│      │   │   ├─ Action: Profile heap usage and increase -Xmx to 2GB
│      │   │   ├─ Action: Enable G1GC (lower pause times)
│      │   │   ├─ Effort: MEDIUM (profiling + testing required)
│      │   │   ├─ Risk: Medium (GC tuning can be system-specific)
│      │   │   ├─ Benefit: Reduce GC pause time from 1200ms → 200ms target
│      │   │   └─ Expected gain: +25% reliability
│      │   │
│      │   └─ Improvement C: Add fallback for retry exhaustion
│      │       ├─ Action: If collision retry fails 5x, assign deterministic code
│      │       ├─ Effort: LOW (graceful fallback)
│      │       ├─ Risk: Very low (only activates on extreme edge case)
│      │       ├─ Benefit: Eliminate user-facing 500 errors on collision burst
│      │       └─ Expected gain: +10% reliability
│      │
│      ├─ Implementation order:
│      │   ├─ Can do A + C in parallel (both low effort, low risk)
│      │   ├─ Should do B after A+C (medium effort, needs testing)
│      │   └─ Estimated timeline: 2 weeks end-to-end
│      │
│      └─ Alternative: Pick just A+C for quicker /win (1 week, +25% gain)
│          └─ Revisit B later if remaining 15% gap is problematic
│
│ Exit Condition: SUCCESS
│ Timeout: 30 minutes
└──────────────────────────────────────

Phase 4: Implementation of Reliability Improvements

┌──────────────────────────────────────
│ ImplementReliabilityImprovements (node-5)
├──────────────────────────────────────
│ Type: IMPLEMENTATION
│ Executor: implement_all_reliability_fixes
│ DependsOn: [node-4]
│
│ Entry Gate: VALIDATION
│  ├─ dependency_check([node-4])
│  ├─ artifact_check(["consolidated_findings.md", "reliability_improvement_plan.md"])
│  └─ resource_check(dev_env_writable)
│
│ Exit Gate: VALIDATION
│  ├─ artifact_produced("improved_code.jar", "config_changes.yaml", "gc_tuning_guide.md")
│  ├─ metrics.no_api_breakage = true
│  ├─ metrics.backward_compatible = true
│  └─ policy_compliance(ProductionReadinessPolicy)
│
│ Produces:
│  ├─ improved_code.jar
│  │   ├─ PoolConfiguration.java (configurable pool size)
│  │   ├─ FallbackCodeAllocationStrategy.java (deterministic code on retry exhaustion)
│  │   ├─ ReliabilityMetrics.java (track reliability improvements)
│  │   ├─ HealthCheck.java (enhanced with pool status, memory status)
│  │   └─ Updated UrlService with graceful fallback
│  ├─ config_changes.yaml
│  │   ├─ spring:
│  │   │   └─ datasource:
│  │   │         hikari:
│  │   │           maximumPoolSize: 50  # was default 32
│  │   │           minimumIdle: 5
│  │   │           maxLifetime: 1800000
│  │   ├─ server:
│  │   │   └─ undertow:
│  │   │         worker-threads: 200  # increase from 16 × CPU cores
│  │   └─ springBootApp:
│  │         jvmArgs: "-Xms1G -Xmx2G -XX:+UseG1GC -XX:+ParallelRefProcEnabled"
│  ├─ gc_tuning_guide.md
│  │   ├─ Why G1GC: Lower pause times (target: < 200ms)
│  │   ├─ Heap sizing: 2GB recommended (scale based on load)
│  │   ├─ Tuning knobs: G1ReservedPercentage, InitiatingHeapOccupancyPercent
│  │   ├─ Monitoring: Track pause time and GC frequency via Prometheus
│  │   ├─ Validation: Run soak test with new settings
│  │   └─ Rollback: Revert to -XX:+UseG1GC -Xmx1G
│  └─ fallback_strategy.md
│      ├─ Normal flow: Random code generation + retry up to 5x
│      ├─ Edge case (rare): If 5x fails, fallback to deterministic code
│      │   ├─ Deterministic code = hash(timestamp + random seed) % available_space
│      │   ├─ Uniqueness guarantee: Maintained (fallback only adds 1 code per retry failure)
│      │   ├─ Risk: Deterministic code slightly predictable, but only on rare edge case
│      │   └─ Benefit: Eliminates user-facing 500 errors
│      └─ Tradeoff: Maintain reliability over perfect randomness in extreme cases
│
│ Exit Condition: SUCCESS
│ Timeout: 120 minutes
│
│ RetryPolicy:
│  ├─ maxRetries: 3
│  ├─ backoffStrategy: exponential
│  ├─ retryOnExceptions: [CompilationError, ConfigurationError]
│  └─ doNotRetryOnExceptions: [DesignViolation]
│
│ ApprovalGate:
│  ├─ gateName: "Reliability Implementation Review"
│  ├─ required: true
│  ├─ appliesTo: [IMPLEMENTATION]
│  ├─ defaultApprover: "reliability-engineer"
│  ├─ autoApproveRules:
│  │   └─ backward_compatible AND findings_addressing all_three_root_causes
│  └─ timeoutMinutes: 24
│
│ RollbackPolicy:
│  ├─ isReversible: true
│  ├─ reversibleOperations:
│  │   ├─ git_reset (revert code changes)
│  │   ├─ config_rollback (revert to old settings)
│  │   └─ app_restart (load with old config)
│  ├─ autoRollbackTriggers: [approval_rejected]
│  └─ rollbackTimeoutSeconds: 180
│
│ ReplanTrigger:
│  └─ if findings_do_not_address_root_cause: replan from Investigation phase
└──────────────────────────────────────

Phase 5: Testing (Validate Improvements)

┌──────────────────────────────────────
│ ValidationTesting (node-6)
├──────────────────────────────────────
│ Type: TESTING
│ Executor: validate_reliability_improvements
│ DependsOn: [node-5]
│
│ Entry Gate: VALIDATION
│  ├─ dependency_check([node-5])
│  ├─ artifact_check(["improved_code.jar", "config_changes.yaml"])
│  └─ approval_check (Implementation approved)
│
│ Exit Gate: VALIDATION
│  ├─ artifact_produced("validation_test_results.json", "reliability_improvement_report.md")
│  ├─ metrics.reliability_improved >= 0.15  (minimum 15% improvement)
│  ├─ metrics.pool_exhaustion_reduced >= 0.70  (70% reduction)
│  ├─ metrics.gc_pause_reduced >= 0.50  (50% reduction)
│  ├─ metrics.retry_exhaustion_eliminated == true
│  └─ onFailure: TRIGGER_REPLAN
│
│ Produces:
│  ├─ validation_test_results.json
│  │   ├─ soak_test_24hours:
│  │   │   ├─ total_requests: 2160000
│  │   │   ├─ success_rate_before: 0.9987  (from earlier soak test)
│  │   │   ├─ success_rate_after: 0.9996   (improved!)
│  │   │   ├─ failure_count_before: 2840
│  │   │   ├─ failure_count_after: 864  (70% reduction!)
│  │   │   └─ improvement_percentage: 69.5%
│  │   ├─ failure_breakdown_after:
│  │   │   ├─ pool_exhaustion: 50 (was 1400, -96%)  ← Config fix worked!
│  │   │   ├─ timeout_errors: 400 (was 1000, -60%)  ← GC tuning helped
│  │   │   ├─ retry_exhaustion: 0 (was 440, -100%)  ← Fallback strategy eliminated!
│  │   │   └─ other: 414
│  │   ├─ latency_metrics_after:
│  │   │   ├─ p50: 12ms (unchanged)
│  │   │   ├─ p95: 45ms (unchanged)
│  │   │   ├─ p99: 180ms (was 250ms, -28%)  ← GC pause reduction
│  │   │   ├─ p99.9: 1200ms (was 2500ms, -52%)  ← Significant improvement
│  │   │   └─ max: 1800ms (was 4800ms, -63%)
│  │   ├─ gc_metrics_after:
│  │   │   ├─ pause_count: 8 (unchanged - same load)
│  │   │   ├─ avg_pause_time: 180ms (was 1200ms, -85%)  ← G1GC working!
│  │   │   ├─ max_pause_time: 450ms (was 4800ms)
│  │   │   └─ heap_pressure: 70% (was 95% before)
│  │   └─ pool_metrics_after:
│  │       ├─ active_connections_max: 50 (new limit)
│  │       ├─ queue_wait_time_avg: 50ms (was 500ms, -90%)
│  │       └─ saturation_events: 0 (was 6, -100%)
│  │
│  └─ reliability_improvement_report.md
│      ├─ Summary: ALL THREE FIXES VALIDATED
│      ├─ Fix A (Pool size): ✓ EFFECTIVE (eliminated 96% of pool exhaustion)
│      ├─ Fix B (GC tuning): ✓ EFFECTIVE (reduced pause time 85%)
│      ├─ Fix C (Retry fallback): ✓ EFFECTIVE (eliminated 100% of user-facing failures)
│      ├─ Combined results:
│      │   ├─ Failure rate: 99.87% → 99.96% (✓ Exceeded 15% target)
│      │   ├─ P99 latency: 250ms → 180ms (-28%)
│      │   ├─ P99.9 latency: 2500ms → 1200ms (-52%)
│      │   └─ MTTR (mean time to recovery): Improved (fewer failures = less recovery needed)
│      ├─ Reliability goals MET:
│      │   ├─ [✓] Improve uptime from 99.87% → 99.96%
│      │   ├─ [✓] Reduce tail latencies by >25%
│      │   ├─ [✓] Eliminate user-facing 500 errors on collision burst
│      │   └─ [✓] No API breakage, backward compatible
│      ├─ Validation criteria:
│      │   ├─ [✓] Pool exhaustion reduced ≥70%
│      │   ├─ [✓] GC pause reduced ≥50%
│      │   ├─ [✓] Retry exhaustion eliminated
│      │   └─ [✓] Overall reliability improved ≥15% (achieved 69.5%)
│      └─ Recommendation: READY FOR PRODUCTION RELEASE
│
│ Exit Condition: SUCCESS
│ Timeout: 1440 minutes (24-hour soak test + analysis)
│
│ RetryPolicy:
│  ├─ maxRetries: 2
│  ├─ backoffStrategy: fixed (1 hour between retries, don't repeat long tests)
│  ├─ retryOnExceptions: [TemporaryTestEnvironmentIssue]
│  └─ doNotRetryOnExceptions: [AssertionError]  (validation failed = replan)
│
│ ReplanTrigger:
│  ├─ if improvements < 15%: replan (need different approach)
│  ├─ if new failures appear: replan (regression detected)
│  └─ if specific fixes didn't work: replan (to design)
└──────────────────────────────────────

Phase 6: Validation & Approval

┌──────────────────────────────────────
│ Final Validation (node-7)
├──────────────────────────────────────
│ Type: VALIDATION
│ Executor: approve_reliability_improvements
│ DependsOn: [node-6]
│
│ Entry Gate: VALIDATION
│  ├─ dependency_check([node-6])
│  ├─ artifact_check(["validation_test_results.json", "reliability_improvement_report.md"])
│  └─ test_criteria_check:
│      ├─ reliability_improved >= 15%
│      └─ no_regressions_detected
│
│ Exit Gate: VALIDATION
│  ├─ artifact_produced("final_validation_report.md", "deployment_readiness.md")
│  ├─ validation_result: PASS or FAIL
│  └─ metrics.ready_for_production = true
│
│ Produces:
│  ├─ final_validation_report.md
│  │   ├─ Executive summary:
│  │   │   └─ "URL Shortener reliability improved from 99.87% to 99.96% (69.5% improvement)"
│  │   ├─ Detailed results:
│  │   │   ├─ Pool exhaustion fix: ✓ EFFECTIVE
│  │   │   ├─ GC tuning fix: ✓ EFFECTIVE
│  │   │   ├─ Retry fallback fix: ✓ EFFECTIVE
│  │   │   ├─ No API breakage: ✓ VERIFIED
│  │   │   ├─ Backward compatibility: ✓ CONFIRMED
│  │   │   └─ Production safety: ✓ APPROVED
│  │   ├─ Risk assessment:
│  │   │   ├─ Rollback complexity: Low (config changes, no schema)
│  │   │   ├─ User impact: Positive (fewer failures, faster responses)
│  │   │   └─ Ops impact: Positive (monitoring provides visibility)
│  │   └─ Conclusion: [PASS] APPROVED FOR RELEASE
│  │
│  └─ deployment_readiness.md
│      ├─ Prerequisites:
│      │   ├─ [✓] Code signed off (approved)
│      │   ├─ [✓] Tests passing (validated)
│      │   ├─ [✓] Monitoring instrumented (enhanced for tracking improvements)
│      │   ├─ [✓] Rollback procedure documented
│      │   └─ [✓] Ops team trained (GC tuning changes)
│      ├─ Deployment steps:
│      │   ├─ Step 1: Apply config changes (pool size, GC settings)
│      │   ├─ Step 2: Restart application with new jar
│      │   ├─ Step 3: Monitor metrics (should see immediate improvement)
│      │   ├─ Step 4: Run smoke tests (verify API still works)
│      │   └─ Step 5: Monitor for 24 hours (soak test results should hold)
│      ├─ Rollback plan:
│      │   ├─ Revert config (old pool size, old GC settings)
│      │   ├─ Revert code (previous jar)
│      │   ├─ Restart application
│      │   └─ Estimated rollback time: 5 minutes
│      └─ Success metrics (monitor after release):
│          ├─ Uptime target: ≥ 99.95% (currently 99.96%)
│          ├─ P99 latency: < 200ms (currently 180ms)
│          ├─ Error rate: < 0.04% (currently 0.04%)
│          └─ Incidents per week: < 1 (currently ~2)
│
│ Exit Condition: SUCCESS (PASS)
│ Timeout: 60 minutes
│ Retry: NONE (validation is pass/fail)
│
│ Conditional Branch:
│  ├─ IF validation == PASS → Release (node-8)
│  └─ IF validation == FAIL → Replan (from Implementation)
└──────────────────────────────────────

Phase 7: Release

┌──────────────────────────────────────
│ Release (node-8)
├──────────────────────────────────────
│ Type: RELEASE_READY
│ Executor: release_reliability_patch
│ DependsOn: [node-7]  (validation PASS)
│
│ Entry Gate: VALIDATION
│  ├─ dependency_check([node-7])
│  ├─ validation_result_check == PASS
│  ├─ artifact_check(["deployment_readiness.md"])
│  └─ deployment_approval_check
│
│ Exit Gate: VALIDATION
│  ├─ artifact_produced("deployment_summary.md", "post_release_checklist.md")
│  ├─ production_health_check: OK
│  └─ reliability_improvements_active: true
│
│ Produces:
│  ├─ deployment_summary.md
│  │   ├─ Release timing: 2026-08-13 2:00 PM UTC
│  │   ├─ Deployment method: Blue-green (zero downtime)
│  │   ├─ Canary phase:
│  │   │   ├─ Enable for 5% of traffic (production baseline)
│  │   │   ├─ Monitor for 1 hour
│  │   │   ├─ Metrics check: Error rate stable, latency improved
│  │   │   └─ Decision: PROCEED
│  │   ├─ Staged rollout:
│  │   │   ├─ Phase 1 (5%): 1 hour
│  │   │   ├─ Phase 2 (25%): 2 hours
│  │   │   ├─ Phase 3 (50%): 2 hours
│  │   │   ├─ Phase 4 (100%): at hour 5
│  │   │   └─ Total: 5 hours to full rollout
│  │   ├─ Monitoring dashboards:
│  │   │   ├─ Uptime: Target ≥ 99.95%
│  │   │   ├─ Error rate: Target < 0.1%
│  │   │   ├─ P99 latency: Target < 200ms
│  │   │   ├─ GC pause time: Target < 200ms
│  │   │   └─ Pool saturation: Target 0% (should never happen)
│  │   ├─ Rollback criteria:
│  │   │   ├─ Error rate increase > 0.2%
│  │   │   ├─ P99 latency increase > 50%
│  │   │   ├─ Incidents detected
│  │   │   └─ Action: Immediate revert to previous version
│  │   └─ Post-release: Monitor for 7 days (stabilization)
│  │
│  └─ post_release_checklist.md
│      ├─ [✓] Deployment completed successfully
│      ├─ [✓] Canary phase: Metrics nominal
│      ├─ [✓] Full rollout: No issues detected
│      ├─ [✓] Health checks passing
│      ├─ [✓] Uptime: 99.96% (exceeds target 99.95%)
│      ├─ [✓] P99 latency: 180ms (exceeds target < 200ms)
│      ├─ [✓] Error rate: 0.04% (within target < 0.1%)
│      ├─ [✓] GC pause time: 180ms (exceeds target < 200ms)
│      ├─ [✓] No user complaints reported
│      ├─ [✓] Ops team notified of improvements
│      ├─ Next: Monitor metrics for 7 days (normal post-release observation)
│      └─ Success: Reliability goals achieved!
│
│ Exit Condition: SUCCESS
│ Timeout: 360 minutes (6 hours for staged rollout + validation)
│
│ ApprovalGate:
│  ├─ gateName: "Production Release Approval"
│  ├─ required: true
│  ├─ appliesTo: [RELEASE_READY]
│  ├─ defaultApprover: "ops-manager"
│  ├─ autoApproveRules:
│  │   └─ validation_passed AND monitoring_ready
│  └─ timeoutMinutes: 60
│
│ RollbackPolicy:
│  ├─ isReversible: true
│  ├─ reversibleOperations:
│  │   ├─ revert_jar (deploy previous version)
│  │   ├─ revert_config (old pool size, old GC settings)
│  │   ├─ restart_app
│  │   └─ verify_rollback (confirm old behavior)
│  ├─ autoRollbackTriggers:
│  │   ├─ error_rate_increase > 0.2%
│  │   ├─ latency_p99_increase > 50%
│  │   ├─ incident_detected
│  │   └─ approval_rejected
│  └─ rollbackTimeoutSeconds: 300 (5 minutes to revert)
└──────────────────────────────────────
```

### Key Characteristics

| Aspect | Ambiguous Reliability |
|--------|----------------------|
| **Scenario Type** | AMBIGUOUS |
| **Phases** | 8 (Decomposition → Release) |
| **Parallel Paths** | Fault Analysis + Soak Testing (2 concurrent) |
| **Synchronization Points** | 1 (after investigation convergence) |
| **Approval Gates** | 2 (Implementation Review, Production Release) |
| **Testing Strategy** | Evidence-based (soak tests drive implementation) |
| **Key Decisions** | Choice of which 3 fixes to implement (all chosen, evidence-based) |
| **Key Uncertainty** | Initial definition of "reliability" (resolved via investigation) |
| **Replan Triggers** | Findings don't address root cause, validation fails, regression detected |
| **Total Artifacts** | 20+ (analysis, reports, code, tests, monitoring dashboards) |
| **Max Workflow Duration** | ~8-9 hours (due to 24-hour soak tests—can run parallel phases) |
| **Learning Path** | Demonstrate how orchestration engine decomposes vague requirement into evidence-driven action |

---

## Summary Table

| Aspect | Greenfield | Brownfield | Ambiguous |
|--------|-----------|-----------|----------|
| **Scenario Type** | GREENFIELD | BROWNFIELD | AMBIGUOUS |
| **Starting Point** | Clear requirement | Existing system | Vague requirement |
| **Primary Path** | Full SDLC | Investigation + optimization | Decomposition + Evidence |
| **Phases** | 8 | 7 | 8 |
| **Parallel Paths** | 1 (Architecture + Testing) | 1 (Fault + Soak) | 1 (Fault + Soak) |
| **Key Challenge** | Correct architecture | Backward compatibility | Defining "reliability" |
| **Approval Gates** | 2 (Code Review, Prod) | 2 (Compatibility, Prod) | 2 (Implementation, Prod) |
| **Rollback Complexity** | High (full system) | Low (feature flag) | Medium (config) |
| **Estimated Duration** | 5-8 hours | 4-6 hours | 6-9 hours |
| **Testing Depth** | Moderate (new feature) | Deep (regression) | Deep (soak test validation) |
| **Human Decisions** | Architecture review | Compatibility approval | Evidence interpretation |
| **Replan Likelihood** | Medium | Low | High (if findings diverge) |

---

## Next Steps (Not Implemented in This Phase)

1. **Workflow Planner**: Implement DAG generator that converts scenario definitions into executable workflows
2. **Execution Engine**: Implement scheduler, retry handler, approval gate manager, rollback executor
3. **Observability**: Add metrics, logging, audit trail collection throughout execution
4. **Testing**: Create integration tests for all 3 scenarios
5. **UI/CLI**: Build interface to visualize workflows and approve gates

---

