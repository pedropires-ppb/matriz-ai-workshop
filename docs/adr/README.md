# Architecture Decision Records

This folder holds the architecture decision records (ADRs) of Matriz. Each ADR records one decision: its context, the decision, its consequences, and the alternatives considered.

- ADRs are numbered `ADR-xxx` with three digits. Numbers are never reused or renumbered ([AGENTS.md](../../AGENTS.md#id-conventions-and-traceability)).
- A decision that changes an earlier one is recorded in a new ADR that supersedes it. While the increment that introduced an ADR is not Accepted, the ADR may instead be amended in place, with each change flagged with its date, decider, and previous text. A superseded ADR's status is updated; its text is otherwise kept. Resolving an ADR's open point, or adding a cross-reference, does not change a decision; it may be done in place, flagged. *Changed on 2026-10-30 by team decision (the conversation “I1 review: remaining decisions”, decision 1): previously “A decision that changes an earlier one is recorded in a new ADR that supersedes it. The earlier ADR's status is updated; its text is otherwise kept.” This rule covers the amendments of ADR-007 and ADR-008 made in the review of I1, the increment that introduced them.* *Changed on 2026-10-31 by team decision (the conversation “I1 review: last decisions”, decision 3, and its follow-up question): the sentence on open points and cross-references was added; previously the rule ended at “its text is otherwise kept.” It covers the in-place changes made to ADR-003 and ADR-004 on 2026-10-30, although I0, the increment that introduced them, is Accepted. The project team counts the removal of ADR-003's follow-up item on the IDE configuration (decision 7 of “I1 review: remaining decisions”) as resolving an open point.*
- The ADRs below are decisions of the project team, not of the client. They stay provisional until the client approves the MVP scope ([OQ-016](../constraints.md#open-questions)).
- An ADR states a decision and its consequences. It links to requirements, assumptions, and acceptance criteria instead of repeating them.

| ADR | Title | Status | Date |
|---|---|---|---|
| [ADR-001](ADR-001-modular-monolith.md) | Modular monolith | Accepted (provisional) | 2026-10-25 |
| [ADR-002](ADR-002-phase-scope-and-implementation-authorization.md) | Phase scope and provisional implementation authorization | Accepted (provisional) | 2026-10-29 |
| [ADR-003](ADR-003-technology-stack.md) | Technology stack | Accepted (provisional) | 2026-10-29 |
| [ADR-004](ADR-004-rest-api-single-entry-point.md) | REST API as the single entry point | Accepted (provisional) | 2026-10-29 |
| [ADR-005](ADR-005-authentication-with-jwt.md) | Authentication with signed access tokens (JWT) | Accepted (provisional) | 2026-10-29 |
| [ADR-006](ADR-006-simultaneous-changes.md) | Simultaneous changes | Accepted (provisional) | 2026-10-29 |
| [ADR-007](ADR-007-build-persistence-and-demonstration-data.md) | Build, persistence, and demonstration data | Accepted (provisional) | 2026-10-30 |
| [ADR-008](ADR-008-domain-driven-design.md) | Domain-driven design in the backend | Accepted (provisional) | 2026-10-30 |
| [ADR-009](ADR-009-identity-from-the-token.md) | Identity from the token, and ended sessions in the User interface | Accepted (provisional) | 2026-11-02 |

## Template

File name: `ADR-xxx-short-title.md`.

```markdown
# ADR-xxx — Title

- **Status:** Proposed | Accepted (provisional) | Accepted | Superseded by ADR-yyy
- **Date:** YYYY-MM-DD
- **Decided by:** the person or team who decided (never an AI assistant)
- **Related:** FR-xxx, NFR-xxx, A-xxx, OQ-xxx, other ADRs

## Context

The forces and constraints behind the decision, with links to their sources.

## Decision

What was decided, stated so that it can be verified.

## Consequences

What becomes easier or harder, the risks accepted, and the follow-up work.

## Alternatives considered

The options not chosen and why, or “no rationale recorded” if none was given.

## Open points

Details still to be decided, with the increment or person responsible.
```
