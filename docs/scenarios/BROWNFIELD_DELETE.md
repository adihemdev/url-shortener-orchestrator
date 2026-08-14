# Brownfield DELETE Scenario

## Goal

Demonstrate a governed brownfield SDLC workflow that analyzes an existing URL-shortener application, makes a minimal change, preserves existing behavior, and validates the result.

## Change Request

Add support for:

`DELETE /api/v1/urls/{shortCode}`

Required behavior:

- return 204 when the mapping exists and is deleted
- return 404 when the short code does not exist
- preserve existing POST shortening behavior
- preserve existing GET redirect behavior

## Brownfield Flow

Existing application → Impact analysis → Bounded implementation change → Focused test updates → Regression execution → Validation → PASS

## Governance

The analysis agent has read-only access to the existing target application.

The implementation agent can read existing source and tests but can write only to the target application's production source.

The testing agent can read production source and existing tests but can write only to the target application's test source.

Existing engine code and unrelated repository files remain outside the permitted write boundary.

## Result

The live impact-analysis agent identified the change surface before modification.

The live implementation agent made a small change to the existing controller, service, and repository.

The live testing agent updated existing service, controller, and integration coverage while preserving existing regression tests.

The service, controller, and integration suites passed after the change.

The live validation agent returned PASS against the DELETE requirement and regression-safety criteria.

## Human Review

One generated integration test initially seeded database state inside a transactional test before making a RANDOM_PORT HTTP request.

Because the HTTP request executes in a separate transaction, the uncommitted test setup was not visible to the application request.

The test was manually corrected to create prerequisite state through the application's POST endpoint before exercising DELETE.

This demonstrates that agent-generated changes remain subject to review and validation rather than being accepted blindly.
