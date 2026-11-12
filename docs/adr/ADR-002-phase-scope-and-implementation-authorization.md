# ADR-002 — Phase scope and provisional implementation authorization

- **Status:** Accepted (provisional). This ADR does not answer [OQ-016](../constraints.md#open-questions): the client's approval of the MVP scope is still required, and the client's answers prevail over every decision below.
- **Date:** 2026-10-29
- **Decided by:** the project team (workshop trainer), by answering the closed questions of the phase 1 analysis
- **Related:** [FR-013–FR-025](../requirements.md#functional-requirements); [Assumption A-012](../constraints.md#assumptions), [Assumptions A-016–A-029](../constraints.md#assumptions); [OQ-016](../constraints.md#open-questions), [OQ-022–OQ-027](../constraints.md#open-questions); [ADR-003](ADR-003-technology-stack.md) to [ADR-006](ADR-006-simultaneous-changes.md)

## Context

Only F01–F03 are confirmed client needs. The proposed MVP, FR-001–FR-012, awaits the client's approval ([OQ-016](../constraints.md#open-questions)), and [AGENTS.md](../../AGENTS.md#current-phase-and-next-steps) planned no implementation before that approval.

For this phase, the project team set the following minimum scope. It is a direct request from the team, not a client-validated need:

| Area | Minimum behaviour |
|---|---|
| Authentication | Sign in and sign out; get the authenticated user. Seeded demo accounts may be used; public registration and password recovery are optional extensions. |
| Projects | Create, list, view, edit, and delete authorized projects. |
| Tasks | Create, view, edit, and delete a project's tasks, with state and priority. |
| Assignees | Assign several eligible users to a task and remove assignments. |
| Comments | Add and list a task's comments, with author and date. |
| Search and filters | Search projects by name; filter tasks by state and priority. |
| Home dashboard | Show accessible projects and their task counts by state, with navigation to the project detail. |

The phase 1 analysis compared this scope with the proposed MVP and found these differences:

- Task comments were excluded ([Assumption A-012](../constraints.md#assumptions)).
- A task had at most one assignee, and unassigning was unspecified (FR-010, [Assumption A-004](../constraints.md#assumptions), [OQ-007](../constraints.md#open-questions)).
- Nothing could be deleted.
- “Edit” meant renaming only (FR-003, FR-009).
- Tasks had no priority.
- No search or filter was specified.
- A cross-project overview was excluded ([Assumption A-012](../constraints.md#assumptions), [OQ-005](../constraints.md#open-questions)).
- How identity is established was undecided (NFR-001, [Assumption A-014](../constraints.md#assumptions), [OQ-001](../constraints.md#open-questions), [OQ-018](../constraints.md#open-questions)), and no data about a person was defined ([OQ-011](../constraints.md#open-questions)).

The analysis ended with 18 closed questions. Each had options, and the AI assistant recommended one of them.

## Decision

The project team answered the 18 questions as follows. The column “Recommended option” shows the assistant's recommendation only where the team chose differently.

| # | Question | Team decision | Recommended option | Recorded in |
|---|---|---|---|---|
| 1 | May implementation start before the client approves the MVP scope? | Yes, provisionally and at the team's risk; OQ-016 stays open. | — | This ADR; [interim team answers](../constraints.md#interim-team-answers-2026-10-29) |
| 2 | Are task comments in scope? | Yes; notifications stay excluded. | — | FR-020, FR-021; A-012, A-023 |
| 3 | Which technology stack? | Option A: Java with Spring Boot and Spring Modulith, a React and TypeScript user interface, and PostgreSQL. | — | [ADR-003](ADR-003-technology-stack.md) |
| 4 | Is the drafted REST API kept? | Yes, as the only entry point of the User interface. | — | [ADR-004](ADR-004-rest-api-single-entry-point.md) |
| 5 | Which user-facing language? | English. | Portuguese ([Assumption A-013](../constraints.md#assumptions)) | A-028, NFR-008 |
| 6 | Who may create projects? | Any signed-in person. | — | A-018, FR-002 |
| 7 | How do assignments work? | A task may have several assignees. The Project Owner assigns and unassigns any member; a Project Participant may assign or unassign only themselves; any assignee may change the task's state. | Several assignees, assigned and unassigned only by the Project Owner | A-019, A-020; FR-010, FR-011, FR-019 |
| 8 | How are projects and tasks deleted? | Soft delete: hidden from everyone, data kept. | Permanent deletion | A-022; FR-016, FR-017 |
| 9 | What do Task priority and “edit” mean? | Low, Medium, or High; Medium by default; set by the Project Owner. “Edit” means renaming a project, or changing a task's title and Task priority. | — | A-021; FR-003, FR-009, FR-018 |
| 10 | What does the home dashboard show? | My projects: task counts by state for each project, with no totals across projects. | — | A-025, FR-024 |
| 11 | Who may comment, and when? | Any member, also while the project is Completed; comments cannot be edited or deleted. | Comments refused in Completed projects | A-011, A-023; FR-020, FR-021 |
| 12 | Are FR-004, FR-005, and FR-007, which the workshop scope does not mention, kept? | Kept. | — | FR-004, FR-005, FR-007 |
| 13 | How are simultaneous changes resolved? | The last change saved prevails; changes subject to rules spanning projects, members, and tasks are applied as one indivisible change. | — | A-029; [ADR-006](ADR-006-simultaneous-changes.md) |
| 14 | Which session mechanism? | Signed access tokens (JWT). | Server-side sessions in a cookie | [ADR-005](ADR-005-authentication-with-jwt.md) |
| 15 | How do search and filters match? | Search: the name contains the text, ignoring letter case. Filters: several values per filter, combined. | — | A-024; FR-022, FR-023 |
| 16 | How is the percentage Done rounded? | To a whole number, halves up. | — | A-026, FR-012 |
| 17 | Who sees a project's member list? | Every member, including Project Participants. | — | A-027, FR-025 |
| 18 | How are these decisions recorded? | New FR, US, and EX entries marked as team decisions and backed by assumptions and open questions; ADRs in this folder. | — | This folder |

The workshop scope's terms are mapped to the [glossary](../requirements.md#glossary). In short: “user” is a Person, “authenticated user” a Signed-in person, “eligible user” an Eligible assignee, and “home dashboard” My projects.

## Consequences

- **Recorded as team decisions, not client requirements.** The decisions are recorded in [constraints](../constraints.md), [requirements](../requirements.md#change-log), [user stories](../user-stories.md#change-log), the [data model](../database.md#change-log), the [architecture definition](../architecture.md#effect-of-the-2026-10-29-decisions), and [openapi.yaml](../../openapi.yaml). They are listed below.
  - New requirements and acceptance criteria: FR-013–FR-025, US-013–US-026, and EX-014–EX-027.
  - Changed proposals, each flagged where it is defined: FR-002, FR-006, FR-010, FR-011, FR-012, NFR-001, NFR-002, and NFR-008.
  - Assumptions: A-001, A-004, A-011, A-012, and A-014 amended; A-013 superseded by A-028; A-016–A-029 added.
  - Open questions: OQ-022–OQ-027 added, and interim answers given to OQ-001, OQ-002, OQ-005, OQ-007, OQ-011, OQ-013, OQ-016–OQ-019, and OQ-022–OQ-027. Each is recorded as “Decided by the project team on 2026-10-29; not validated by the client” in the [interim team answers](../constraints.md#interim-team-answers-2026-10-29), which also state the parts left undecided.
  - [AGENTS.md](../../AGENTS.md): only the approved changes are applied, namely the `ADR-xxx` ID row (question 18), the MVP range in the scope rules (question 2), and the update of its current phase, which records the provisional authorization of question 1 and which the team approved on 2026-10-30. The other edits proposed for AGENTS.md were not applied.
- **Confirmed needs unchanged.** F01–F03 are unchanged. No requirement had been approved by the client, so no approved decision was changed.
- **Incremental implementation.** Implementation proceeds in increments ([implementation plan](../implementation-plan.md)). The user stories and exceptional cases are the acceptance criteria of each increment.
- **Risks accepted by the team:**
  - The client may reject or change any addition (OQ-022–OQ-027), which would mean rework.
  - Self-assignment lets any Project Participant change any task's state ([Assumption A-020](../constraints.md#assumptions)).
  - Deleting unfinished tasks lets a project be completed ([Assumption A-022](../constraints.md#assumptions), EX-018).
  - New personal data is processed with no identified obligations ([OQ-011](../constraints.md#open-questions)), so only fictitious data is used.

## Alternatives considered

- **For question 1:** implementing nothing yet and aligning only the documentation, or waiting for the client's approval. Not chosen; no rationale recorded.
- **For questions 5, 7, 8, 11, and 14:** the recommended options shown above. Not chosen; no rationale recorded.
- **Other questions:** the remaining options are in the phase 1 analysis; the recommended option was chosen.

## Open points

*Resolution links added on 2026-11-12 by the project team's delivery decision 6; the original open-point list below is historical, not a list of current blockers.* Deletion details were decided for projects on 2026-11-02 and tasks on 2026-11-05 ([Assumption A-022](../constraints.md#assumptions)); comment details are recorded with their precise validation status in [Assumption A-036](../constraints.md#assumptions). Filters leaving progress unchanged are implemented and checked in [I5](../evidence/I5-progress-and-my-projects-verification.md). Login-name uniqueness is enforced by migration V1 and [ADR-007](ADR-007-build-persistence-and-demonstration-data.md#8-physical-schema); comment placement and the independent Comment aggregate are decided in [ADR-008](ADR-008-domain-driven-design.md). Client validation remains pending; these links do not retroactively approve every proposed comment detail or the pending detail of A-024.

These details were not decided by the team. They are the assistant's proposed reading of the decisions, flagged where they are recorded, and need the team's and the client's confirmation:

- How deletion works: only the Project Owner deletes, only in Active projects, after confirming, with no restore, and with the task's assignments removed ([Assumption A-022](../constraints.md#assumptions)).
- Comment details: nonempty text, listed oldest first ([Assumption A-023](../constraints.md#assumptions)).
- Filters never change progress ([Assumption A-024](../constraints.md#assumptions)).
- Login names are unique ([Assumption A-017](../constraints.md#assumptions)).
- Comments are placed in the Tasks module ([architecture definition](../architecture.md#effect-of-the-2026-10-29-decisions)).
