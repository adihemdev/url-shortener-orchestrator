# Design Decisions - URL Shortener v1

## 1. Short Code Generation

**Decision:** 6-character random code using A-Z, a-z, 0-9 alphabet.

**Rationale:** Balances readability and entropy (62^6 ≈ 56B combinations) for typical workloads.

**Future Consideration:** For high-volume production, consider sequential ID + base62 encoding or distributed ID generation.

---

## 2. Short Code Uniqueness

**Decision:** Enforce uniqueness at database level via unique constraint.

**Validation Strategy:**
- Short code: non-null, unique (DB constraint)
- Long URL: non-blank, valid URL format (@URL validator)
- Database raises DataIntegrityViolationException on collision

---

## 3. Integration Test Strategy

**Decision:** Full-stack integration tests verify API contracts AND database persistence.

**Rationale:** Unit tests alone insufficient for API reliability; catch layer misalignment early.

---

## 4. Agentic Orchestration

**Decision:** Implement the orchestration model directly using explicit workflow nodes, execution state, artifacts, approval gates, policies, retries, and decision lineage.

**Rationale:** Keeping these concepts explicit provides visibility and control over workflow execution and allows SDLC-specific governance behavior to remain part of the application model.

### Custom Orchestration vs. LangGraph

The custom orchestration approach provides direct control over execution and governance and can remain a viable production model where customized workflow semantics and minimal framework coupling are priorities.

LangGraph could have reduced development time by providing graph execution, state management, checkpointing, and human-in-the-loop primitives out of the box. It also introduces another framework abstraction and learning curve.

**Learning:** A future iteration should evaluate LangGraph earlier and consider using it for generic orchestration mechanics while retaining domain-specific governance, artifacts, validation, and safety controls in the application.

---

## 5. Bounded Agent Access

**Decision:** Engineering agents interact with the codebase through explicitly bounded workspace tools.

**Rationale:** Agent capabilities should be narrower than the permissions of the process hosting them. Restricting accessible source roots reduces the blast radius of generated changes and makes permitted modifications explicit.

---

## 6. Deterministic and Live Agent Testing

**Decision:** Maintain both deterministic tests using controlled model responses and opt-in live tests using a configured LLM.

**Rationale:** Deterministic tests provide repeatable verification of orchestration behavior, while live tests demonstrate that prompts and agent boundaries work with real model behavior.

**Trade-off:** Live model output is probabilistic and requires external credentials, so live tests are kept separate from the default deterministic test path.

