# Agentic SDLC Orchestration Engine for URL Shortener Service

A production-grade Java and Spring Boot prototype demonstrating controlled agentic autonomy, a deterministic state machine control plane, enterprise governance, and end-to-end SDLC automation.

---

## 🏗️ Architecture Overview

The system is engineered with a strict architectural separation between the target application domain and the agentic control plane:

```text
url-shortener-orchestrator/
├── src/main/java/com/cs/urlshortenerorchestrator/
│   ├── targetapp/                 # Layer 1: URL Shortener Domain (REST APIs, Data Layer)
│   └── engine/                    # Layer 2: Agentic Orchestration Control Plane
│       ├── state/                 # Explicit State Machine & DAG Graph Execution
│       ├── governance/            # Human-in-the-Loop (HITL) Checkpoints & Policy Gates
│       ├── metrics/               # Audit Logging, MTTR, & Success Rate Tracking
│       └── execution/             # Copilot SDK / LLM Client Integration

```

### Workflow Orchestration & Control Flow Model

The engine implements a stateful, non-linear orchestration control plane using an explicit Directed Acyclic Graph (DAG) with strict entry and exit gates, supporting:

* **DAG Execution & Synchronization**
* **Immutable Decision Lineage**
* **Adaptive Re-Planning**

The lifecycle stages execute as follows:
1. **Requirement Analysis:** Interprets raw intent, identifies ambiguity, and normalizes input into a clear engineering problem.
2. **Task Decomposition:** Converts high-level requirements into actionable tasks with explicit dependencies and sequencing.
3. **Architecture Design:** Formulates database schema updates, API contracts, and component changes.
4. **Implementation:** Produces production-quality code and schema definitions.
5. **Testing & Validation:** Executes automated tests and validation suites.
6. **Documentation & Release Readiness:** Compiles documentation and prepares the build for deployment sign-off.

---

## ⚙️ The Three Demanded Scenarios

This engine processes three distinct operational pathways:

1. **Greenfield Scenario:** Bootstraps brand-new feature requirements from scratch (e.g., adding custom short-link aliases with expiration timestamps).
2. **Brownfield Scenario:** Points the orchestrator at an existing component to execute refactoring or performance optimization (e.g., introducing a caching layer to the redirect lookup path).
3. **Ambiguous Scenario:** Intercepts vague prompts , routes them through the requirement analysis node to identify gaps, pauses execution for operator input, and normalizes the task list.

---

## 🛡️ Governance, Safety, & Guardrails

* **Human-in-the-Loop (HITL) Checkpoints:** High-impact operations (e.g. database schema changes, promoting builds to release readiness) trigger an automatic pause in the state machine, requiring an explicit REST API or CLI sign-off to proceed.
* **Resiliency & Self-Healing:** Test and compilation failures are intercepted in real-time, incrementing bounded retry counters and routing context back upstream to correct implementation flaws.
* **Auditability & Metrics:** Every pipeline execution writes immutable audit records tracking success rates, rollback frequencies, and mean time to resolution (MTTR).
* **Enforcement Mechanism:** High-impact checkpoints transition the state machine into a `WAITING_FOR_APPROVAL` mode, locking execution until an explicit REST API sign-off or CLI signal resumes the DAG. Failures automatically trip bounded retry limits before triggering automated rollbacks and safe-stops.

---

## 🚀 Setup & Execution Instructions

* **Tech Stack Note:** Built on Java 21 LTS, leveraging Virtual Threads for high-throughput concurrent agent execution and records/sealed classes for type-safe domain modeling.

### Prerequisites

* Java 21 or higher
* Maven 3.8+

### Build Instructions

Clone the repository, configure your environment settings, and build the project using Maven:

```bash
mvn clean install

```

### Run Instructions

Start the Spring Boot application:

```bash
mvn spring-boot:run

```
### Core API Endpoints

* **Create Short Link:** `POST /api/shorten` — Generates a new short code for a given long URL.
* **Redirection:** `GET /{code}` — Resolves the short code and redirects the client to the target URL.
* **Get Analytics:** `GET /api/analytics/{code}` — Fetches click-through counts, creation dates, and metadata for a specific short link.
* **Update Short Link:** `PUT /api/shorten/{code}` — Modifies properties of an existing short link (e.g., updating the destination URL or expiration parameters).
* **Delete Short Link:** `DELETE /api/shorten/{code}` — Permanently removes or deactivates a short link from the system.
* **Trigger Orchestration Pipeline:** `POST /api/pipeline/trigger` — Submits a feature request or refactoring task (Greenfield, Brownfield, or Ambiguous).
* **Governance Approval Gate:** `POST /api/pipeline/approve` — Submits an explicit approval or rejection sign-off for high-risk execution checkpoints.

---

## ⚖️ Testing Approach, Limitations, and Trade-offs

* **Testing Strategy:** Combines unit tests for state machine DAG transitions with integration tests verifying core URL shortener database behavior.
* **Trade-offs:** Employs an embedded H2 database for streamlined local execution over distributed production clusters, and supports mock runtime fallback adapters to guarantee deterministic pipeline demonstrations.