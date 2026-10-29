# Matriz — Product Requirements (v0.1)

**Status:** Draft for client validation. Only F01–F03 are known client needs. All other proposed behaviours are marked with links to the [assumption register](constraints.md#assumptions); Must denotes a **proposed**, not approved, MoSCoW priority. See [user stories](user-stories.md) for acceptance tests and [constraints](constraints.md) for unresolved decisions.

## Vision and goals

Matriz is intended to help work teams organize projects (F01), manage tasks (F02), and track work progress (F03). The proposed first-release goal is to let a team create and access a project, organize its tasks, update their states, and see progress within that project. Better coordination and visibility are possible benefits, not client-confirmed business outcomes ([Assumption A-015](constraints.md#assumptions)). Business success measures remain to be agreed.

## User types

The distinction between these types, their responsibilities, and their permissions is [Assumption A-001](constraints.md#assumptions); access restrictions are [Assumption A-002](constraints.md#assumptions). A person can have different roles in different projects. How people receive initial access and who may create a project are unresolved ([OQ-001](constraints.md#open-questions)).

| Type | Description and responsibilities | Proposed high-level permissions |
|---|---|---|
| Project Owner | Coordinates one project, manages its membership and tasks, and decides when work is complete. | Create a project if authorized; view and rename it; add/remove participants; create, rename, assign, and change the state of its tasks; view progress; complete the project when eligible. |
| Project Participant | Works on a project to which they belong and keeps their assigned tasks current. | View the project, its tasks, and its progress; change the state of tasks assigned to them while the project is active. |

The Project Owner is also a member of their project under [Assumption A-008](constraints.md#assumptions). All project modifications are subject to the completed-project restriction in [Assumption A-011](constraints.md#assumptions).

## Functional requirements

All entries below describe the **proposed MVP**, not approved requirements. F01–F03 refer to the three client needs above. Origin links identify every additional proposed rule; their justifications, risks, and validation states are in [constraints](constraints.md#assumptions). Acceptance criteria are maintained only in [user stories](user-stories.md).

| ID | Requirement | Priority | Origin |
|---|---|---|---|
| FR-001 | Show a person only the projects in which they participate, including completed projects; deny access to other projects. | Must (proposed) | F01; [Assumption A-002](constraints.md#assumptions), [Assumption A-011](constraints.md#assumptions) |
| FR-002 | Allow an authorized prospective Project Owner to create a named, Active project; its creator becomes its Project Owner and a participant. | Must (proposed) | F01; [Assumption A-003](constraints.md#assumptions), [Assumption A-008](constraints.md#assumptions) |
| FR-003 | Allow the Project Owner to change the name of an Active project to a nonempty name. | Must (proposed) | F01; [Assumption A-001](constraints.md#assumptions), [Assumption A-003](constraints.md#assumptions), [Assumption A-011](constraints.md#assumptions) |
| FR-004 | Allow the Project Owner to add a person as a participant in an Active project, granting access to that project. | Must (proposed) | F01; [Assumption A-001](constraints.md#assumptions), [Assumption A-002](constraints.md#assumptions) |
| FR-005 | Allow the Project Owner to remove a non-owner participant from an Active project only if that person has no assigned tasks there; removal ends their access. Changes to Project Owner membership are not specified. | Must (proposed) | F01; [Assumption A-002](constraints.md#assumptions), [Assumption A-009](constraints.md#assumptions) |
| FR-006 | Allow a participant to view the tasks of their project, including each task's state and assignee, if any. | Must (proposed) | F02; [Assumption A-002](constraints.md#assumptions), [Assumption A-004](constraints.md#assumptions) |
| FR-007 | Allow the Project Owner to mark an Active project Completed only when no tasks are unfinished; completed projects remain viewable but not editable. | Must (proposed) | F01; [Assumption A-003](constraints.md#assumptions), [Assumption A-007](constraints.md#assumptions), [Assumption A-011](constraints.md#assumptions) |
| FR-008 | Allow the Project Owner to create a task with a nonempty title in an Active project; it starts To do and may be unassigned. | Must (proposed) | F02; [Assumption A-004](constraints.md#assumptions), [Assumption A-010](constraints.md#assumptions) |
| FR-009 | Allow the Project Owner to change the title of a task in an Active project to a nonempty title. | Must (proposed) | F02; [Assumption A-001](constraints.md#assumptions), [Assumption A-004](constraints.md#assumptions) |
| FR-010 | Allow the Project Owner to assign a task in an Active project to one of that project's participants; at most one assignee is recorded. Reassignment and unassignment are unspecified (OQ-007). | Must (proposed) | F02; [Assumption A-001](constraints.md#assumptions), [Assumption A-004](constraints.md#assumptions) |
| FR-011 | Allow the Project Owner or the assigned Project Participant to change the state of a task in an Active project among To do, In progress, and Done without a required sequence, including reopening a Done task. | Must (proposed) | F02; [Assumption A-001](constraints.md#assumptions), [Assumption A-005](constraints.md#assumptions), [Assumption A-011](constraints.md#assumptions) |
| FR-012 | Show a project's task counts by state and the percentage Done (Done tasks / all tasks × 100); for zero tasks show “No tasks” instead of a percentage. | Must (proposed) | F03; [Assumption A-006](constraints.md#assumptions) |

## Non-functional requirements and unresolved quality criteria

No numerical service targets or security policies have been supplied by the client. A pending criterion below is **not** a claimed commitment; it must be agreed before it can be tested as a release target.

| ID | Area | Proposed requirement / measurable criterion | Basis and status |
|---|---|---|---|
| NFR-001 | Authentication | Establish a person's identity before allowing project access. The way identity is established and how initial access is granted remain open. Verification: an unidentified person cannot read or change project or task information. | [Assumption A-014](constraints.md#assumptions); proposed, pending client validation. |
| NFR-002 | Authorization | Enforce project membership and role permissions for both project discovery and attempts to access or change a project directly. Verification: a nonparticipant cannot see the project or its tasks; an unassigned, non-owner participant cannot change a task's state; an unauthorized change leaves information unchanged. | [Assumption A-001](constraints.md#assumptions), [Assumption A-002](constraints.md#assumptions); proposed, pending client validation. |
| NFR-003 | Personal data and privacy | Identify any personal data processed, its authorized access, and applicable legal/internal obligations before fixing retention or protection criteria. If GDPR applies, comply with the obligations established with the client and competent stakeholders; no data categories, retention periods, or legal interpretation are presumed. | [OQ-011](constraints.md#open-questions); criteria pending. |
| NFR-004 | Response time | Client to identify common operations and acceptable response-time limits, plus conditions under which they are measured. | [OQ-010](constraints.md#open-questions); target pending. |
| NFR-005 | Availability | Client to identify required operating hours and availability target. | [OQ-010](constraints.md#open-questions); target pending. |
| NFR-006 | Concurrent use | Client to provide expected initial and future simultaneous-user counts and conditions for verifying capacity. | [OQ-010](constraints.md#open-questions); target pending. |
| NFR-007 | Accessibility | Client to identify applicable accessibility needs or standard and verifiable conformance level. | [OQ-010](constraints.md#open-questions); criterion pending. |
| NFR-008 | Language | Proposed first-release interface language: Portuguese; other required languages remain open. Verification if confirmed: MVP user-facing interactions are available in Portuguese. | [Assumption A-013](constraints.md#assumptions), [OQ-013](constraints.md#open-questions); proposed, pending client validation. |

See [constraints — security and privacy](constraints.md#security-and-privacy) for the open decisions affecting these requirements.

## MVP scope

**Objective (proposed):** let a team create and access a project, organize its tasks, update their states, and see progress within that project (F01–F03).

**Included (proposed, awaiting client approval):** FR-001–FR-012. This covers project membership and access, project creation/renaming/completion, task creation/renaming/assignment/state updates, task consultation, and per-project progress. The owner/participant rules, states, and completion rule are assumptions, not confirmed client requirements.

**Excluded (proposed):** see [Outside the initial scope](#outside-the-initial-scope).

**MVP release criteria (proposed):** the first release is acceptable when every acceptance criterion of US-001–US-012 and every exceptional case EX-001–EX-013 in [user stories](user-stories.md) pass, and NFR-001 and NFR-002 are verified as described above. Business success measures are not yet defined ([OQ-009](constraints.md#open-questions)). Actual priority and sufficiency of the MVP are client decisions ([OQ-016](constraints.md#open-questions)).

## Outside the initial scope

The following exclusions are a **proposal** under [Assumption A-012](constraints.md#assumptions), not confirmed client decisions. No related MVP stories are specified.

| Candidate | Proposed disposition |
|---|---|
| Task comments and notifications | Possible later collaboration release (proposed Should). |
| Subtasks, task dependencies, and cross-project reports/overview | Possible later planning/reporting release (proposed Could). |
| File attachments | Outside the proposed scope; no release assigned (proposed Won't for now). |

The suggested future sequence has no approved dates or delivery commitments.

## Glossary

| Term | Meaning in this draft |
|---|---|
| Project Owner | Proposed per-project coordinating role; also a member of that project. |
| Project Participant | Proposed per-project working role distinct from Project Owner; lower-case “participant” means any project member, including the owner. |
| Member | Any person with access to a project, including its Project Owner; synonym of lower-case “participant”. |
| Project | Unit of organized team work; proposed states: Active and Completed. |
| Task | Unit of work belonging to exactly one project; proposed states: To do, In progress, and Done. |
| Assignee | The single optional project participant responsible for a task's execution. |
| Progress | Proposed per-project Done-task percentage and task counts by state; “No tasks” if there are none. |
| MVP | Proposed minimum viable first release; contents and priorities await client approval. |








