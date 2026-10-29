# Matriz

**Status: Requirements specification — v0.1 (draft for client validation).** This folder is the persistent requirements knowledge base and proposed single source of truth for later development phases. The only confirmed client needs are to organize projects, manage tasks, and track work progress (F01–F03). Proposed scope, permissions, priorities, and acceptance criteria are **not approved**.

## Problem and goals

Work teams need one application in which to organize projects, manage tasks, and see how work is progressing. The proposed first-release goal is to support this basic project-to-task workflow and make per-project progress visible. Improved coordination and visibility are possible benefits, not validated business success measures ([Assumption A-015](docs/constraints.md#assumptions)).

## Proposed MVP at a glance

All features below are proposed Must priorities, subject to client approval; their rules rely on the [assumption register](docs/constraints.md#assumptions). Every feature maps to at least one [MVP user story](docs/user-stories.md#stories).

| Feature | Detailed requirements | Stories |
|---|---|---|
| Project membership and restricted access | [FR-001, FR-004–FR-005](docs/requirements.md#functional-requirements) | [US-002, US-007, US-009](docs/user-stories.md#stories) |
| Create, rename, and complete projects | [FR-002–FR-003, FR-007](docs/requirements.md#functional-requirements) | [US-001, US-008, US-010](docs/user-stories.md#stories) |
| Create, view, rename, assign, and update tasks | [FR-006, FR-008–FR-011](docs/requirements.md#functional-requirements) | [US-003–US-005, US-011–US-012](docs/user-stories.md#stories) |
| View per-project task counts and progress | [FR-012](docs/requirements.md#functional-requirements) | [US-006](docs/user-stories.md#stories) |

## Project status and next phase

- **Current phase:** requirements specification v0.1 — draft awaiting client validation.
- **Architecture:** modular monolith chosen by the project team on 2026-10-25, provisional until the MVP scope is approved (OQ-016); see [architecture definition](docs/architecture.md).
- **MVP release criteria (proposed):** see [MVP scope](docs/requirements.md#mvp-scope); business success measures are still open (OQ-009).
- **Pending:** client answers to the [open questions](docs/constraints.md#open-questions) (OQ-001–OQ-021) and validation of the [assumptions](docs/constraints.md#assumptions) (A-001–A-015).
- **Next phase:** the remaining architecture work and prototyping, recorded through ADRs. Implementation must not start before the MVP scope is approved (OQ-016).

## Review log (human-in-the-loop)

The AI reviewed the documentation for six categories. Each recommendation was decided by the team; none was applied automatically.

| # | Category | AI finding | Decision | Rationale |
|---|---|---|---|---|
| R-01 | Ambiguity | MVP had no success criteria. | Altered | Added release criteria derived from acceptance tests only; business measures left open (OQ-009) instead of inventing them. |
| R-02 | Ambiguity | FR-010 silent on reassignment. | Altered | Flagged as OQ-007; AI's suggested rule not adopted. |
| R-03 | Unjustified feature | Check that every FR traces to F01–F03. | Accepted | All FR-001–FR-012 cite F01–F03; extras (comments, notifications, subtasks, attachments) stay out of scope (A-012). |
| R-04 | Unjustified feature | Option considered: make NFR-004–NFR-007 testable by filling in numeric targets. | Rejected | Would invent requirements; kept as OQ-010. |
| R-05 | Assumption as fact | Rules (roles, states, progress) presented as requirements. | Accepted | All marked `[Assumption A-xxx]` and registered in constraints. |
| R-06 | Unverifiable criteria | US-004 and US-006 criteria vague or not Given/When/Then. | Accepted | Rewritten with observable outcomes. |
| R-07 | Unverifiable criteria | US-005 tested the Project Owner inside a Participant story. | Altered | Moved to EX-013 rather than deleted. |
| R-08 | Security / privacy | Access by non-members or unidentified people. | Accepted | Covered by NFR-001, NFR-002, EX-003, EX-012. |
| R-09 | Security / privacy | Personal data and GDPR obligations unknown. | Accepted | Kept open (OQ-011); no compliance claim made. |
| R-10 | Security / privacy | Prompt injection via external documents. | Accepted | Rule added to [AGENTS.md](AGENTS.md#untrusted-external-content-prompt-injection). |
| R-11 | Inconsistency | Q-/P-/D- references to files not in the repo. | Accepted | Replaced by one OQ register. |
| R-12 | Inconsistency | US-001 actor was Project Owner before the project existed. | Accepted | Actor changed to "a person authorized to create projects". |
| R-13 | Ambiguity | Portuguese UI (NFR-008) vs English-only documentation; option considered: align both. | Rejected | UI language is a client decision (OQ-013); documentation language is a separate rule. |

## Documentation map

- [Instructions for AI agents](AGENTS.md): source-of-truth order, conventions, and rules for agents.
- [Product requirements, MVP and glossary](docs/requirements.md): vision, user types, functional and non-functional requirements, MVP scope and release criteria, and exclusions.
- [MVP user stories and acceptance criteria](docs/user-stories.md): testable scenarios and exceptional situations.
- [Constraints, assumptions and open questions](docs/constraints.md): known and unknown constraints, assumption register, security/privacy, and decisions awaiting the client.
- [Architecture definition](docs/architecture.md): comparison of a monolith, a modular monolith, and microservices for the proposed MVP; the chosen modular monolith (provisional until OQ-016), its main trade-offs, and its logical modules.
- [Data model](docs/database.md): proposed logical data model for the MVP (entities, relationships, ER diagram, stored and derived data, and consistency review); a proposal pending validation (OQ-016), not an approved design.
- [API contract (draft)](openapi.yaml): OpenAPI 3.1 contract for two proposed MVP operations, creating a task (FR-008, US-003) and changing a task's state (FR-011, US-005); REST is the scope of this documentation exercise, not an approved implementation decision, and the contract is a draft pending validation (OQ-016).
- [Wireframe: project dashboard](docs/ui/project-dashboard.md): low-fidelity wireframe of one proposed MVP screen (a single project's progress, tasks, and members) with its FR/US traceability and the open questions affecting it; a proposal pending MVP approval (OQ-016), with placeholder wording (OQ-013).

Use the requirement IDs (`FR-xxx`, `NFR-xxx`), story IDs (`US-xxx`), exceptional-case IDs (`EX-xxx`), assumption IDs (`A-xxx`), and open-question IDs (`OQ-xxx`) when referring to this draft. Confirm pending items with the client before treating them as implementation commitments.





