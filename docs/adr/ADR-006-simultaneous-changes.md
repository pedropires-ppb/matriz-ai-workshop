# ADR-006 — Simultaneous changes

- **Status:** Accepted (provisional)
- **Date:** 2026-10-29
- **Decided by:** the project team ([ADR-002](ADR-002-phase-scope-and-implementation-authorization.md), question 13)
- **Related:** [Assumption A-029](../constraints.md#assumptions); [OQ-017](../constraints.md#open-questions); [FR-005](../requirements.md#functional-requirements), [FR-007](../requirements.md#functional-requirements), [FR-010](../requirements.md#functional-requirements), [FR-011](../requirements.md#functional-requirements), [FR-016](../requirements.md#functional-requirements), [FR-017](../requirements.md#functional-requirements), [FR-019](../requirements.md#functional-requirements); [Assumption A-007](../constraints.md#assumptions), [Assumption A-009](../constraints.md#assumptions), [Assumption A-011](../constraints.md#assumptions), [Assumption A-022](../constraints.md#assumptions); [ADR-001](ADR-001-modular-monolith.md), [ADR-003](ADR-003-technology-stack.md)

## Context

The client has not decided how simultaneous changes to the same project or task are resolved ([OQ-017](../constraints.md#open-questions)). Some rules span projects, members, and tasks. Two changes that are each valid on their own could together break such a rule. Examples:

- The Project Owner completes a project while an assignee reopens one of its tasks, or while a task is being created ([Assumption A-007](../constraints.md#assumptions), [Assumption A-011](../constraints.md#assumptions)).
- The Project Owner removes a participant while that participant assigns themselves to a task ([Assumption A-009](../constraints.md#assumptions), [Assumption A-020](../constraints.md#assumptions)).
- The Project Owner deletes a project while one of its tasks is being changed ([Assumption A-022](../constraints.md#assumptions)).

## Decision

1. **Last change wins.** When people change the same information at nearly the same time, the last change saved prevails, without warning. Nobody is asked to retry. For example, if two renames of a project overlap, the later one is kept ([Assumption A-029](../constraints.md#assumptions)).
2. **Rules are never broken.** Every change subject to a rule spanning projects, members, and tasks is checked and saved in one database transaction. No simultaneous change can break such a rule. These changes are:
   - completing a project;
   - removing a participant;
   - creating, changing, or deleting a task;
   - assigning or unassigning;
   - deleting a project;
   - any change refused in a Completed project.
3. **Concurrency tests.** In increment I8, automated tests run conflicting changes simultaneously and check that every rule still holds ([implementation plan](../implementation-plan.md)).

## Consequences

- **Simple contract.** The API needs no version numbers or retry handling.
- **Overwritten changes.** A person may overwrite someone else's recent change without noticing ([Assumption A-029](../constraints.md#assumptions)).
- **Serialized changes.** With the protection proposed under Open points, changes within one project are applied one at a time. No concurrent-use target exists against which to judge this ([NFR-006](../requirements.md#non-functional-requirements-and-unresolved-quality-criteria), [OQ-010](../constraints.md#open-questions)).
- **One database.** The decision relies on all data being in one database ([ADR-001](ADR-001-modular-monolith.md), [ADR-003](ADR-003-technology-stack.md)).

## Alternatives considered

The alternative was optimistic versioning (question 13, option b). Each change would state the version it is based on, and a change based on an outdated version would be refused with 409, asking the person to reload. The team did not choose it and recorded no rationale.

## Open points

- **How rules are protected.** [Proposal] Every change within a project first locks that project's row in the database (`SELECT … FOR UPDATE`). Changes to the same project then run one at a time, and two transactions cannot each rely on a state that the other is changing. This is the assistant's proposal, to be confirmed in increment I3 and tested in I8.
- **If the client decides otherwise.** If the client answers [OQ-017](../constraints.md#open-questions) differently, for example by requiring a warning before an overwrite, optimistic versioning would have to be added to the API.
