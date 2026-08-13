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

[Sections for Engine, Scenarios, and Trade-offs to follow]

