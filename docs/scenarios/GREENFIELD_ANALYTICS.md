# Greenfield Analytics Scenario

## Goal

Demonstrate a governed agentic SDLC workflow that can plan, implement, test,
validate, and safely replan a new Analytics capability for the URL shortener.

## Scenario

The workflow builds a greenfield Analytics capability that:

- accepts click events
- stores analytics data in memory
- aggregates usage statistics
- detects traffic spikes
- exposes dashboard data
- validates generated implementation evidence before completion

## Workflow

```text
Requirement
    ↓
Requirements Analysis
    ↓
Architecture Design + Test Planning
    ↓
Synchronization
    ↓
Implementation Agent
    ↓
Testing Agent
    ↓
Controlled Test Execution
    ↓
Validation Agent
    ↓
PASS → workflow completes
FAIL → bounded replan from implementation