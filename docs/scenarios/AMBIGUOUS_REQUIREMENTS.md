# Ambiguous Requirements Scenario

## Goal

Demonstrate that the agentic SDLC does not implement an underspecified request by inventing product requirements.

## Initial Request

Users need more control over their shortened URLs. Add the necessary support without breaking existing behavior.

## Workflow

Vague request
↓
Inspect existing application
↓
Ambiguity assessment
↓
Material ambiguity detected
↓
Implementation blocked
↓
Stakeholder clarification
↓
Reassessment
↓
Still ambiguous? → remain blocked
↓
Sufficiently specified
↓
Human approval / decision recorded
↓
Implementation planning may proceed

## Behavior Demonstrated

The analysis agent distinguishes:

- facts established from the existing codebase
- unresolved product requirements
- clarification questions
- unsafe assumptions

Implementation is blocked while consequential ambiguity remains.

A partial clarification is not automatically accepted. In the live scenario, the agent independently identified additional unresolved decisions around endpoint design, response structure, empty results, pagination, and ordering.

After those decisions were explicitly resolved, reassessment marked the requirement sufficiently specified and implementation was unblocked.

## Governance

The ambiguity-analysis stage is read-only.

The workflow reuses the existing approval and decision-recording mechanisms for stakeholder clarification rather than creating a separate governance system.

Downstream implementation planning becomes reachable only after the required clarification/approval checkpoint succeeds.

## Result

The scenario demonstrates:

- live ambiguity detection
- explicit implementation blocking
- iterative clarification
- human approval
- decision governance
- safe transition from ambiguous request to actionable requirement

The listing feature itself is intentionally not implemented because greenfield and brownfield scenarios already demonstrate implementation, testing, and validation behavior.