# ADR-001 — Modular monolith

- **Status:** Accepted (provisional until the client approves the MVP scope, [OQ-016](../constraints.md#open-questions))
- **Date:** 2026-10-25
- **Decided by:** the project team
- **Related:** [NFR-001](../requirements.md#non-functional-requirements-and-unresolved-quality-criteria), [NFR-002](../requirements.md#non-functional-requirements-and-unresolved-quality-criteria); [OQ-010](../constraints.md#open-questions), [OQ-012](../constraints.md#open-questions), [OQ-017–OQ-020](../constraints.md#open-questions); [ADR-003](ADR-003-technology-stack.md)

The full record of this decision is the [architecture definition](../architecture.md#decision-and-rationale). This ADR indexes it; it was written on 2026-10-29, when the team decided to keep decision records in this folder ([ADR-002](ADR-002-phase-scope-and-implementation-authorization.md)). It summarizes that record without changing it.

## Context

A monolith, a modular monolith, and microservices were compared for the proposed MVP in terms of simplicity, maintenance, operational cost, and future evolution ([comparison of options](../architecture.md#comparison-of-options)). No scale targets, deadline, budget, or operating arrangement are known.

## Decision

Build Matriz as one deployable application divided into modules with explicit boundaries: Identity, Projects, Tasks, Progress, Access control, and the User interface ([components and responsibilities](../architecture.md#components-and-responsibilities)).

## Consequences

- Rules spanning projects, members, and tasks are checked within one unit, without network calls.
- The application is deployed, scaled, and fails as one unit.
- Module boundaries need ongoing discipline. They are verified automatically from increment I1 ([ADR-003](ADR-003-technology-stack.md), [implementation plan](../implementation-plan.md)).
- The decisions of 2026-10-29 did not change this choice ([effect of the 2026-10-29 decisions](../architecture.md#effect-of-the-2026-10-29-decisions)).

## Alternatives considered

A plain monolith and microservices; see the options not chosen in the [decision and rationale](../architecture.md#decision-and-rationale).

## Open points

The [questions that could change the choice](../architecture.md#questions-that-could-change-the-choice).
