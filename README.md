# Agentic SDLC Orchestration Engine for URL Shortener Service

A Java and Spring Boot prototype demonstrating governed agentic SDLC orchestration across greenfield, brownfield, and ambiguous-requirement workflows.

The engine combines deterministic workflow control with LLM-driven engineering agents, bounded workspace access, approval gates, decision lineage, testing, and validation.

---

## Architecture Overview

The project separates the target application from the agentic orchestration engine:

```text
url-shortener-orchestrator/
├── src/main/java/com/cs/urlshortenerorchestrator/
│   ├── targetapp/              # URL Shortener application
│   └── engine/                 # Agentic SDLC orchestration engine
│       ├── agent/              # Engineering agents and bounded tools
│       ├── domain/             # Workflow, artifact, decision and policy model
│       └── execution/          # Workflow execution and governance
│
└── src/test/java/com/cs/urlshortenerorchestrator/
    ├── targetapp/              # Application tests
    ├── engine/                 # Deterministic orchestration tests
    └── analytics/orchestration # Live agent scenario tests
```

### Workflow Model

The engine represents the SDLC as an explicit workflow graph with controlled transitions between engineering stages.

Depending on the scenario, the workflow can include:

1. Requirement analysis
2. Impact analysis
3. Architecture / implementation planning
4. Implementation
5. Testing
6. Validation
7. Human approval checkpoints
8. Release-readiness decisions

The orchestration layer remains deterministic about **what may execute and when**, while LLM agents perform bounded engineering tasks within those constraints.

---

## Demonstrated Scenarios

The project exercises three agentic SDLC pathways.

### 1. Greenfield

The greenfield scenario demonstrates building a new analytics capability from scratch. The engine orchestrates a complete SDLC pipeline:
- **Requirement Analysis**: Agent identifies tasks from an ambiguous prompt.
- **Decomposition**: Tasks are sequenced into a dependency graph.
- **Implementation & Testing**: Parallel execution of code generation and test suite creation.
- **Synchronization**: Engine waits for parallel paths to converge before validation.
- **Validation**: Execution of generated tests against the new implementation.
- **Governance**: Human approval checkpoint before final release.
- **Decision Lineage**: Full audit trail of all agent and human decisions.

### 2. Brownfield

The brownfield scenario operates against the existing URL shortener application.

The agent:

- inspects the existing codebase
- performs impact analysis
- identifies behavior that must be preserved
- adds DELETE support to the existing application
- updates existing tests
- executes validation
- remains restricted to explicitly permitted workspace roots

This demonstrates modifying an existing system without giving the agent unrestricted repository access.

### 3. Ambiguous Requirements

The ambiguous-requirement scenario begins with an intentionally underspecified request:

> Users need more control over their shortened URLs. Add the necessary support without breaking existing behavior.

The analysis agent inspects the existing application and distinguishes:

- known codebase facts
- unresolved requirements
- clarification questions
- unsafe assumptions

When material ambiguity remains, implementation is explicitly blocked.

The scenario also demonstrates iterative clarification. A partial clarification may still be rejected when consequential API or product decisions remain unresolved. Once the stakeholder provides sufficient clarification, the requirement is reassessed and downstream planning is allowed to proceed through the existing approval and decision-governance path.

The feature used for clarification is intentionally not implemented because the greenfield and brownfield scenarios already demonstrate implementation, testing, and validation behavior.

See `docs/scenarios/AMBIGUOUS_REQUIREMENTS.md` for the detailed scenario.

---

## Governance and Safety

Agent execution is constrained by the orchestration engine rather than allowing unrestricted autonomous changes.

### Bounded Workspace Access

Engineering agents access files through a bounded workspace abstraction.

Only explicitly configured source or test roots may be read or modified. Attempts to access paths outside those roots are rejected.

This prevents an implementation agent from freely modifying areas such as orchestration internals, repository metadata, or unrelated application code.

### Human Approval and Rollback Control

Workflow nodes can require an approval gate before downstream execution proceeds.

If an approval is rejected and the node is configured with a reversible `RollbackPolicy`, the engine invokes a **Rollback Compensation Hook** (`NodeExecutor.rollback`) to programmatically undo side effects.

Approval decisions and rollback actions are captured through the workflow's existing decision lineage and audit mechanisms.

### Bounded Replanning and Fallback

The engine supports governed recovery through Bounded Replanning. When a node's failure path is configured for TRIGGER_REPLAN, the engine can re-open upstream dependencies and resume execution from an earlier stable stage.

Replanning is bounded to prevent infinite recovery loops. If recovery is not possible or permitted, the system enters a Safe-Stop phase to prevent destructive automated progress.

### Ambiguity Blocking

The analysis agent can explicitly determine that a requirement is not sufficiently specified.

Rather than inventing missing product requirements, it can return:

- unresolved ambiguities
- clarification questions
- unsafe assumptions
- an explicit implementation-blocked decision

### Decision Lineage and Auditability

Workflow execution records decisions and execution events so that important transitions can be traced back to their originating workflow and execution context.

### Reliability Metrics

The engine programmatically computes and exposes key metrics to measure orchestration health:
- **Success Rate**: The ratio of completed nodes to total terminal nodes (completed + failed) within a workflow execution.
- **Retry/Rollback Frequency**: The percentage of unique nodes that required at least one retry or triggered a rollback state.
- **MTTR (Mean Time To Recovery)**: A **Recovery Delay Proxy** measuring the average delay (wait time) introduced by retry policies and rollback transitions.
- **E2E Latency**: The total wall-clock duration of the workflow execution from start to finish.

---

## URL Shortener Application

The repository includes a Spring Boot URL shortener that acts as the target application for the orchestration scenarios.

### Core API Endpoints

- **Create Short Link**  
  `POST /api/v1/urls`

- **Resolve Short Link**  
  `GET /api/v1/urls/{shortCode}`

- **Delete Short Link**  
  `DELETE /api/v1/urls/{shortCode}`

DELETE removes an existing short-code mapping and returns `404 Not Found` when the mapping does not exist.

---

## Setup

### Prerequisites

- Java 21+
- Maven 3.8+ or the included Maven wrapper

### Build

```bash
./mvnw clean install
```

The system can also be built with a locally installed Maven distribution:

```bash
mvn clean install
```

### Run the Application

```bash
./mvnw spring-boot:run
```

---

## LLM Configuration

The project separates deterministic tests from tests that make real LLM calls.

### Deterministic Tests

Normal unit and integration tests do **not** require an LLM API key.

Run them with:

```bash
unset LIVE_AGENT_TESTS
./mvnw clean test
```

Live-agent tests are skipped unless explicitly enabled.

### Live Agent Tests

Live tests require access to an OpenAI-compatible model endpoint.

**Environment Setup**:

```bash
# Required for live tests
export LLM_BASE_URL="<openai-compatible-base-url>"
export LLM_MODEL="<model-name>"
export LLM_API_KEY="<api-key>"

# Must be set to true to enable live test execution
export LIVE_AGENT_TESTS=true
```

Run specific scenarios:

```bash
# Run the live ambiguity scenario
./mvnw -Dtest=LiveAmbiguityAssessmentTest test

# Run the live clarified-requirement scenario
./mvnw -Dtest=LiveClarifiedRequirementAssessmentTest test

# Run all live orchestration scenarios
./mvnw -Dtest=com.cs.urlshortenerorchestrator.analytics.orchestration.* test
```

Credentials must be provided through environment variables and must not be committed to the repository. The engine uses these to authenticate with the configured LLM provider.

---

## Deterministic vs. Live Testing

The project intentionally uses both deterministic and live-agent tests.

### Deterministic Tests

Deterministic tests use controlled agent responses to verify:

- orchestration behavior
- parsing and structured agent outputs
- ambiguity blocking
- approval workflow behavior
- workspace boundaries
- application behavior

These tests should provide stable CI-friendly verification.

### Live Agent Tests

Live tests make actual model calls to verify that the orchestration and prompts behave correctly with a real LLM.

Because model output is probabilistic, these tests are inherently less deterministic than the standard test suite.

Some live implementation and testing scenarios may modify files within explicitly permitted workspace roots. Run those scenarios against a clean working tree or isolated branch when inspecting generated changes.

---

## Technology Choices

### Java 21 and Spring Boot

The orchestration engine and target application use Java 21 and Spring Boot, keeping the agentic workflow integrated with a conventional enterprise application stack.

### Explicit Orchestration Model

Workflow state, nodes, entry/exit gates, artifacts, approvals, decision lineage, sequential/parallel paths, and bounded replanning behavior are represented explicitly in the application.

This makes workflow behavior inspectable and allows domain-specific governance rules to remain under application control. The engine handles synchronization between parallel paths (e.g., waiting for both implementation and testing to complete before validation).

The trade-offs and possible use of an agent orchestration framework such as LangGraph are discussed separately in the project's design documentation.

### OpenAI-Compatible Agent Client

LLM access is isolated behind an agent-client abstraction.

The orchestration and engineering-agent logic therefore depends on the application's agent interface rather than directly embedding provider-specific calls throughout the workflow implementation.

---

## Current Scope

This repository is a prototype intended to demonstrate agentic SDLC architecture and governance patterns.

It demonstrates:

- sequential/parallel paths with synchronization
- bounded retries, fallback (replanning), rollback compensation and safe-stop
- policy guardrails
- audit/traceability via decision lineage
- reliability metrics including success rate, retry/rollback frequency, MTTR and E2E latency
- dynamic replanning
- controlled agent autonomy with human oversight
- greenfield, brownfield and ambiguous requirement scenarios

It is not intended to represent a complete production SDLC platform. Production adoption would require additional consideration around areas such as persistent workflow state, distributed execution, authentication and authorization, secrets management, production observability, model cost controls, and operational recovery.