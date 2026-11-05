# Matriz — Product Requirements (v0.2)

**Status:** Draft for client validation. Only F01–F03 are known client needs. All other proposed behaviours are marked with links to the [assumption register](constraints.md#assumptions); Must denotes a **proposed**, not approved, MoSCoW priority. See [user stories](user-stories.md) for acceptance tests and [constraints](constraints.md) for unresolved decisions. Revised on 2026-10-29 with the project team's provisional decisions ([ADR-002](adr/ADR-002-phase-scope-and-implementation-authorization.md)); every change to an existing entry is flagged and listed in the [change log](#change-log).

## Vision and goals

Matriz is intended to help work teams organize projects (F01), manage tasks (F02), and track work progress (F03). The proposed first-release goal is to let a team create and access a project, organize its tasks, update their states, and see progress within that project. On 2026-10-29 the project team extended this goal with sign-in, deletion, Task priority, several assignees, task comments, search and filters, and a My projects screen; these additions are team decisions, not client requests ([Assumption A-012](constraints.md#assumptions)). Better coordination and visibility are possible benefits, not client-confirmed business outcomes ([Assumption A-015](constraints.md#assumptions)). Business success measures remain to be agreed.

## User types

The distinction between these types, their responsibilities, and their permissions is [Assumption A-001](constraints.md#assumptions); access restrictions are [Assumption A-002](constraints.md#assumptions). A person can have different roles in different projects. How people receive initial access and who may create a project are unresolved ([OQ-001](constraints.md#open-questions)); the team provisionally lets any signed-in person create projects ([Assumption A-018](constraints.md#assumptions)).

| Type | Description and responsibilities | Proposed high-level permissions |
|---|---|---|
| Project Owner | Coordinates one project, manages its membership and tasks, and decides when work is complete. | Create a project; view, rename, and delete it; add/remove participants; create, rename, set the Task priority of, assign and unassign, delete, and change the state of its tasks; read and add comments; view members and progress; complete the project when eligible. |
| Project Participant | Works on a project to which they belong and keeps their assigned tasks current. | View the project, its members, tasks, comments, and progress; assign or unassign themselves; change the state of tasks assigned to them; add comments. |

The Project Owner is also a member of their project under [Assumption A-008](constraints.md#assumptions). All project modifications are subject to the completed-project restriction in [Assumption A-011](constraints.md#assumptions), except adding comments.

### Permission matrix (proposed)

Rows are actions; columns are the person's relationship to the project. **Yes** means allowed; **No** means refused with nothing changed; **Hidden** means nothing about the project or task is disclosed, as if it did not exist ([EX-003](user-stories.md#edge-cases-and-exceptions), [EX-012](user-stories.md#edge-cases-and-exceptions)). The last column applies to every member when the project is Completed ([Assumption A-011](constraints.md#assumptions)). Acceptance criteria remain only in [user stories](user-stories.md).

| Action | Project Owner | Assigned Project Participant | Unassigned Project Participant | Non-member | Not signed in | Project Completed | Basis |
|---|---|---|---|---|---|---|---|
| List or search own projects (My projects) | Yes | Yes | Yes | Not listed | Hidden | Still listed | FR-001, FR-022, FR-024; [Assumption A-002](constraints.md#assumptions), [Assumption A-024](constraints.md#assumptions), [Assumption A-025](constraints.md#assumptions) |
| Create a project | Yes: any signed-in person | Yes | Yes | Yes | No | — | FR-002; [Assumption A-008](constraints.md#assumptions), [Assumption A-018](constraints.md#assumptions) |
| View a project, its progress, and its members | Yes | Yes | Yes | Hidden | Hidden | Yes | FR-001, FR-012, FR-025; [Assumption A-002](constraints.md#assumptions), [Assumption A-027](constraints.md#assumptions) |
| Rename a project, or change its description | Yes | No | No | Hidden | Hidden | No | FR-003; [Assumption A-001](constraints.md#assumptions), [Assumption A-003](constraints.md#assumptions), [Assumption A-031](constraints.md#assumptions) |
| Delete a project | Yes | No | No | Hidden | Hidden | No | FR-016; [Assumption A-022](constraints.md#assumptions) |
| Complete a project | Yes, if no task is unfinished | No | No | Hidden | Hidden | No | FR-007; [Assumption A-007](constraints.md#assumptions) |
| Add or remove a participant | Yes; removal only of a non-owner without assigned tasks | No | No | Hidden | Hidden | No | FR-004, FR-005; [Assumption A-009](constraints.md#assumptions) |
| View tasks, task details, and filters | Yes | Yes | Yes | Hidden | Hidden | Yes | FR-006, FR-023; [Assumption A-002](constraints.md#assumptions), [Assumption A-024](constraints.md#assumptions) |
| Create a task | Yes | No | No | Hidden | Hidden | No | FR-008, FR-018; [Assumption A-001](constraints.md#assumptions), [Assumption A-021](constraints.md#assumptions) |
| Change a task's title or Task priority | Yes | No | No | Hidden | Hidden | No | FR-009, FR-018; [Assumption A-001](constraints.md#assumptions), [Assumption A-021](constraints.md#assumptions) |
| Delete a task | Yes | No | No | Hidden | Hidden | No | FR-017; [Assumption A-022](constraints.md#assumptions) |
| Change a task's state | Yes, any task | Yes | No, unless they first assign themselves | Hidden | Hidden | No | FR-011, FR-019; [Assumption A-001](constraints.md#assumptions), [Assumption A-019](constraints.md#assumptions), [Assumption A-020](constraints.md#assumptions) |
| Assign or unassign any member | Yes | No | No | Hidden | Hidden | No | FR-010; [Assumption A-019](constraints.md#assumptions) |
| Assign or unassign oneself | Yes | Yes: unassign | Yes: assign | Hidden | Hidden | No | FR-010, FR-019; [Assumption A-020](constraints.md#assumptions) |
| Read a task's comments | Yes | Yes | Yes | Hidden | Hidden | Yes | FR-021; [Assumption A-023](constraints.md#assumptions) |
| Add a comment | Yes | Yes | Yes | Hidden | Hidden | Yes | FR-020; [Assumption A-011](constraints.md#assumptions), [Assumption A-023](constraints.md#assumptions) |

- A Project Owner who is also an assignee keeps the Project Owner's permissions.
- Deleted projects and tasks are Hidden for everyone ([Assumption A-022](constraints.md#assumptions)).
- Hiding or disabling a control does not replace these checks ([NFR-002](#non-functional-requirements-and-unresolved-quality-criteria); [OQ-019](constraints.md#open-questions)).

## Functional requirements

All entries below describe the **proposed MVP**, not approved requirements. F01–F03 refer to the three client needs above. Origin links identify every additional proposed rule; their justifications, risks, and validation states are in [constraints](constraints.md#assumptions). Acceptance criteria are maintained only in [user stories](user-stories.md).

FR-013–FR-025 were added by the project team on 2026-10-29 ([ADR-002](adr/ADR-002-phase-scope-and-implementation-authorization.md)). They are **team decisions, not client requests**: their F reference names the client need they support, and their priority is marked “team decision”. Entries changed by the same decisions are marked *Changed in v0.2*.

| ID | Requirement | Priority | Origin |
|---|---|---|---|
| FR-001 | Show a person only the projects in which they participate, including completed projects, in pages ([Assumption A-032](constraints.md#assumptions)); deny access to other projects. *Changed on 2026-11-02 by team decision (the conversation “Implement project management”): pages added; previously “Show a person only the projects in which they participate, including completed projects; deny access to other projects.”* | Must (proposed) | F01; [Assumption A-002](constraints.md#assumptions), [Assumption A-011](constraints.md#assumptions) |
| FR-002 | Allow an authorized prospective Project Owner to create a named, Active project, with an optional description ([Assumption A-031](constraints.md#assumptions)); its creator becomes its Project Owner and a participant. *Changed in v0.2: origin. Changed on 2026-11-02 by team decision (the conversation “Implement project management”): the optional description is a team addition, not a client request; previously “Allow an authorized prospective Project Owner to create a named, Active project; its creator becomes its Project Owner and a participant.”* | Must (proposed) | F01; [Assumption A-003](constraints.md#assumptions), [Assumption A-008](constraints.md#assumptions), [Assumption A-018](constraints.md#assumptions), [Assumption A-031](constraints.md#assumptions) |
| FR-003 | Allow the Project Owner to change the name of an Active project to a valid name, and its description ([Assumption A-031](constraints.md#assumptions)). *Changed on 2026-11-02 by team decision (the conversation “Implement project management”, questions 4, 14): a valid name has 1 to 100 characters after trimming, and the description is editable; previously “Allow the Project Owner to change the name of an Active project to a nonempty name.”* | Must (proposed) | F01; [Assumption A-001](constraints.md#assumptions), [Assumption A-003](constraints.md#assumptions), [Assumption A-011](constraints.md#assumptions), [Assumption A-031](constraints.md#assumptions) |
| FR-004 | Allow the Project Owner to add a person as a participant in an Active project, granting access to that project. | Must (proposed) | F01; [Assumption A-001](constraints.md#assumptions), [Assumption A-002](constraints.md#assumptions) |
| FR-005 | Allow the Project Owner to remove a non-owner participant from an Active project only if that person has no assigned tasks there; removal ends their access. Changes to Project Owner membership are not specified. | Must (proposed) | F01; [Assumption A-002](constraints.md#assumptions), [Assumption A-009](constraints.md#assumptions) |
| FR-006 | Allow a participant to view the tasks of their project, including each task's state, Task priority, and assignees, if any. *Changed in v0.2: Task priority; several assignees.* | Must (proposed) | F02; [Assumption A-002](constraints.md#assumptions), [Assumption A-004](constraints.md#assumptions), [Assumption A-019](constraints.md#assumptions), [Assumption A-021](constraints.md#assumptions) |
| FR-007 | Allow the Project Owner to mark an Active project Completed only when no tasks are unfinished; completed projects remain viewable but not editable. | Must (proposed) | F01; [Assumption A-003](constraints.md#assumptions), [Assumption A-007](constraints.md#assumptions), [Assumption A-011](constraints.md#assumptions) |
| FR-008 | Allow the Project Owner to create a task with a nonempty title in an Active project; it starts To do and may be unassigned. | Must (proposed) | F02; [Assumption A-004](constraints.md#assumptions), [Assumption A-010](constraints.md#assumptions) |
| FR-009 | Allow the Project Owner to change the title of a task in an Active project to a nonempty title. | Must (proposed) | F02; [Assumption A-001](constraints.md#assumptions), [Assumption A-004](constraints.md#assumptions) |
| FR-010 | Allow the Project Owner to assign a task in an Active project to one or more members of that project, and to remove any of those assignments; a member is assigned to the same task at most once. *Changed in v0.2: several assignees and unassignment replace “at most one assignee”.* | Must (proposed) | F02; [Assumption A-001](constraints.md#assumptions), [Assumption A-004](constraints.md#assumptions), [Assumption A-019](constraints.md#assumptions) |
| FR-011 | Allow the Project Owner or any assignee of a task to change the state of a task in an Active project among To do, In progress, and Done without a required sequence, including reopening a Done task. *Changed in v0.2: any assignee.* | Must (proposed) | F02; [Assumption A-001](constraints.md#assumptions), [Assumption A-005](constraints.md#assumptions), [Assumption A-011](constraints.md#assumptions), [Assumption A-019](constraints.md#assumptions) |
| FR-012 | Show a project's task counts by state and the percentage Done (Done tasks / all tasks × 100, rounded to the nearest whole number, halves up), not counting deleted tasks; for zero tasks show “No tasks” instead of a percentage. *Changed in v0.2: rounding; deleted tasks.* | Must (proposed) | F03; [Assumption A-006](constraints.md#assumptions), [Assumption A-022](constraints.md#assumptions), [Assumption A-026](constraints.md#assumptions) |
| FR-013 | Allow a person with a prepared account to sign in with their login name and password; a wrong login name or password is refused without revealing which one was wrong, and gives no access. | Must (team decision) | F01–F03 (identification before access, NFR-001); team decision 2026-10-29 ([ADR-002](adr/ADR-002-phase-scope-and-implementation-authorization.md)); [Assumption A-014](constraints.md#assumptions), [Assumption A-016](constraints.md#assumptions), [Assumption A-017](constraints.md#assumptions) |
| FR-014 | Allow a signed-in person to sign out; afterwards, that sign-in can no longer be used to read or change information. | Must (team decision) | F01–F03 (NFR-001); team decision 2026-10-29 ([ADR-002](adr/ADR-002-phase-scope-and-implementation-authorization.md)); [Assumption A-016](constraints.md#assumptions) |
| FR-015 | Show the signed-in person their display name, so they know who they are signed in as. | Must (team decision) | F01–F03 (NFR-001); team decision 2026-10-29 ([ADR-002](adr/ADR-002-phase-scope-and-implementation-authorization.md)); [Assumption A-017](constraints.md#assumptions) |
| FR-016 | Allow the Project Owner to delete an Active project after confirming; the project is then hidden from everyone, as if it did not exist, while its data is kept. | Must (team decision) | F01; team decision 2026-10-29 ([ADR-002](adr/ADR-002-phase-scope-and-implementation-authorization.md)); [Assumption A-022](constraints.md#assumptions) |
| FR-017 | Allow the Project Owner to delete a task of an Active project after confirming; the task is then hidden from everyone and no longer counts in progress or in the unfinished-task check, and its assignments are removed, while its data is kept. | Must (team decision) | F02; team decision 2026-10-29 ([ADR-002](adr/ADR-002-phase-scope-and-implementation-authorization.md)); [Assumption A-022](constraints.md#assumptions) |
| FR-018 | Record a Task priority (Low, Medium, or High) for every task: the Project Owner may choose it when creating the task, Medium otherwise, and may change it while the project is Active. | Must (team decision) | F02; team decision 2026-10-29 ([ADR-002](adr/ADR-002-phase-scope-and-implementation-authorization.md)); [Assumption A-021](constraints.md#assumptions) |
| FR-019 | Allow a Project Participant to assign themselves to, or unassign themselves from, any task of an Active project in which they participate. | Must (team decision) | F02; team decision 2026-10-29 ([ADR-002](adr/ADR-002-phase-scope-and-implementation-authorization.md)); [Assumption A-019](constraints.md#assumptions), [Assumption A-020](constraints.md#assumptions) |
| FR-020 | Allow any member to add a comment with nonempty text to a task of their project, also while the project is Completed; the comment records its author and the date and time it was added. | Must (team decision) | F02; team decision 2026-10-29 ([ADR-002](adr/ADR-002-phase-scope-and-implementation-authorization.md)); [Assumption A-011](constraints.md#assumptions), [Assumption A-023](constraints.md#assumptions) |
| FR-021 | Show every member a task's comments, oldest first, each with its text, its author's display name, and the date and time it was added. | Must (team decision) | F02; team decision 2026-10-29 ([ADR-002](adr/ADR-002-phase-scope-and-implementation-authorization.md)); [Assumption A-017](constraints.md#assumptions), [Assumption A-023](constraints.md#assumptions) |
| FR-022 | Allow a signed-in person to search their accessible projects by name; a project is listed if its name contains the search text, ignoring letter case; accents are significant, and the results are paged like the list ([Assumption A-032](constraints.md#assumptions)). *Changed on 2026-11-02 by team decision (the conversation “Implement project management”, question 8): accents and paging added; previously “…ignoring letter case.”* | Must (team decision) | F01; team decision 2026-10-29 ([ADR-002](adr/ADR-002-phase-scope-and-implementation-authorization.md)); [Assumption A-002](constraints.md#assumptions), [Assumption A-024](constraints.md#assumptions) |
| FR-023 | Allow a member to filter a project's task list by one or more states and one or more Task priorities; filters change only the list, never progress. | Must (team decision) | F02, F03; team decision 2026-10-29 ([ADR-002](adr/ADR-002-phase-scope-and-implementation-authorization.md)); [Assumption A-021](constraints.md#assumptions), [Assumption A-024](constraints.md#assumptions) |
| FR-024 | After signing in, show My projects: every accessible project, Active or Completed, with its name, state, and task counts by state, linking to its Project dashboard; no totals across projects are shown. | Must (team decision) | F01, F03; team decision 2026-10-29 ([ADR-002](adr/ADR-002-phase-scope-and-implementation-authorization.md)); [Assumption A-002](constraints.md#assumptions), [Assumption A-025](constraints.md#assumptions) |
| FR-025 | Allow every member of a project to see its members and their roles. | Must (team decision) | F01; team decision 2026-10-29 ([ADR-002](adr/ADR-002-phase-scope-and-implementation-authorization.md)); [Assumption A-027](constraints.md#assumptions) |

## Non-functional requirements and unresolved quality criteria

No numerical service targets or security policies have been supplied by the client. A pending criterion below is **not** a claimed commitment; it must be agreed before it can be tested as a release target.

| ID | Area | Proposed requirement / measurable criterion | Basis and status |
|---|---|---|---|
| NFR-001 | Authentication | Establish a person's identity before allowing project access. The way identity is established and how initial access is granted remain open; the team provisionally uses sign-in with prepared accounts and signed access tokens ([ADR-005](adr/ADR-005-authentication-with-jwt.md)). Verification: an unidentified person cannot read or change project or task information. *Changed in v0.2: interim method.* | [Assumption A-014](constraints.md#assumptions), [Assumption A-016](constraints.md#assumptions); proposed, pending client validation ([OQ-018](constraints.md#open-questions), [OQ-027](constraints.md#open-questions)). |
| NFR-002 | Authorization | Enforce project membership and role permissions for both project discovery and attempts to access or change a project directly. Verification: a nonparticipant cannot see the project or its tasks; an unassigned, non-owner participant cannot change a task's state; an unauthorized change leaves information unchanged. *Changed in v0.2: basis.* | [Assumption A-001](constraints.md#assumptions), [Assumption A-002](constraints.md#assumptions); proposed, pending client validation. [Assumption A-020](constraints.md#assumptions) lets a Project Participant assign themselves first, which weakens the intent of this requirement. |
| NFR-003 | Personal data and privacy | Identify any personal data processed, its authorized access, and applicable legal/internal obligations before fixing retention or protection criteria. If GDPR applies, comply with the obligations established with the client and competent stakeholders; no data categories, retention periods, or legal interpretation are presumed. | [OQ-011](constraints.md#open-questions); criteria pending. |
| NFR-004 | Response time | Client to identify common operations and acceptable response-time limits, plus conditions under which they are measured. | [OQ-010](constraints.md#open-questions); target pending. |
| NFR-005 | Availability | Client to identify required operating hours and availability target. | [OQ-010](constraints.md#open-questions); target pending. |
| NFR-006 | Concurrent use | Client to provide expected initial and future simultaneous-user counts and conditions for verifying capacity. | [OQ-010](constraints.md#open-questions); target pending. |
| NFR-007 | Accessibility | Client to identify applicable accessibility needs or standard and verifiable conformance level. | [OQ-010](constraints.md#open-questions); criterion pending. |
| NFR-008 | Language | Proposed first-release interface language: English; other required languages remain open. Verification: MVP user-facing interactions are available in English. *Changed in v0.2: English replaces Portuguese.* | [Assumption A-028](constraints.md#assumptions) (supersedes A-013), [OQ-013](constraints.md#open-questions); team decision (provisional), pending client validation. |

See [constraints — security and privacy](constraints.md#security-and-privacy) for the open decisions affecting these requirements.

## MVP scope

**Objective (proposed):** let a team create and access a project, organize its tasks, update their states, and see progress within that project (F01–F03).

**Included (proposed, awaiting client approval):** FR-001–FR-025.
- FR-001–FR-012 cover project membership and access, project creation/renaming/completion, task creation/renaming/assignment/state updates, task consultation, and per-project progress.
- FR-013–FR-025, added by the project team on 2026-10-29 ([ADR-002](adr/ADR-002-phase-scope-and-implementation-authorization.md)), cover sign-in and sign-out, deletion, Task priority, several assignees and self-assignment, task comments, project search, task filters, My projects, and member lists.

The owner/participant rules, states, completion rule, and team additions are assumptions, not confirmed client requirements. The team has provisionally authorized implementation in increments ([implementation plan](implementation-plan.md)) while approval remains open ([OQ-016](constraints.md#open-questions)).

**Excluded (proposed):** see [Outside the initial scope](#outside-the-initial-scope).

**MVP release criteria (proposed):** the first release is acceptable when every acceptance criterion of US-001–US-026 and every exceptional case EX-001–EX-027 in [user stories](user-stories.md) pass, and NFR-001 and NFR-002 are verified as described above. Business success measures are not yet defined ([OQ-009](constraints.md#open-questions)). Actual priority and sufficiency of the MVP are client decisions ([OQ-016](constraints.md#open-questions)).

## Outside the initial scope

The following exclusions are a **proposal** under [Assumption A-012](constraints.md#assumptions), not confirmed client decisions. No related MVP stories are specified.

| Candidate | Proposed disposition |
|---|---|
| Notifications | Possible later collaboration release (proposed Should). Task comments were moved into the MVP by the project team on 2026-10-29 (FR-020, FR-021; [Assumption A-023](constraints.md#assumptions)). *Changed in v0.2.* |
| Subtasks, task dependencies, and cross-project reports/overview | Possible later planning/reporting release (proposed Could). My projects (FR-024) shows per-project counts only and is not this overview ([Assumption A-025](constraints.md#assumptions)). |
| File attachments | Outside the proposed scope; no release assigned (proposed Won't for now). |
| Public registration and password recovery | Optional extensions of the team's phase scope; not provided ([Assumption A-016](constraints.md#assumptions)). *Added in v0.2.* |
| Restoring deleted projects or tasks; editing or deleting comments | Not provided ([Assumption A-022](constraints.md#assumptions), [Assumption A-023](constraints.md#assumptions)). *Added in v0.2.* |

The suggested future sequence has no approved dates or delivery commitments.

## Glossary

The workshop scope of 2026-10-29 used other words for some terms; they map as follows: “user” → Person; “authenticated user” → Signed-in person; “eligible user” → Eligible assignee; “home dashboard” → My projects; “edit” a project → rename it or change its description (FR-003; *changed on 2026-11-02: previously “rename it”*); “edit” a task → change its title or Task priority (FR-009, FR-018); “state” of a task → its state (To do, In progress, Done); “priority” of a task → Task priority.

| Term | Meaning in this draft |
|---|---|
| Person | Anyone known to Matriz who can sign in; a person is a Member only of the projects to which they belong. |
| Signed-in person | A person whose identity has been established by signing in (FR-013) and who has not yet signed out. |
| Login name | The unique name a person enters to sign in ([Assumption A-017](constraints.md#assumptions)). |
| Display name | The name by which a person is shown to the other members of their projects ([Assumption A-017](constraints.md#assumptions)). |
| Project Owner | Proposed per-project coordinating role; also a member of that project. |
| Project Participant | Proposed per-project working role distinct from Project Owner; lower-case “participant” means any project member, including the owner. |
| Member | Any person with access to a project, including its Project Owner; synonym of lower-case “participant”. |
| Accessible project | A project, not deleted, of which the person is a Member ([Assumption A-002](constraints.md#assumptions), [Assumption A-022](constraints.md#assumptions)). |
| Project | Unit of organized team work; proposed states: Active and Completed. |
| Task | Unit of work belonging to exactly one project; proposed states: To do, In progress, and Done. |
| Task priority | Importance of a task within its project: Low, Medium, or High ([Assumption A-021](constraints.md#assumptions)). Not the MoSCoW priority of a requirement. |
| Assignee | A member of a task's project responsible for its execution; a task can have zero, one, or several assignees ([Assumption A-019](constraints.md#assumptions)). *Changed in v0.2.* |
| Eligible assignee | A Member of the task's project, including its Project Owner, who can be assigned to the task ([Assumption A-019](constraints.md#assumptions)). |
| Comment | Text added by a member to a task, shown with its author and the date and time it was added; it cannot be edited or deleted ([Assumption A-023](constraints.md#assumptions)). |
| Delete | Hide a project or task from everyone while keeping its data ([Assumption A-022](constraints.md#assumptions)). Not the same as removing a participant from a project. |
| Progress | Proposed per-project Done-task percentage, rounded to a whole number with halves up, and task counts by state, not counting deleted tasks; “No tasks” if there are none ([Assumption A-026](constraints.md#assumptions)). *Changed in v0.2.* |
| My projects | The screen shown after signing in, listing a person's accessible projects with per-project task counts by state ([Assumption A-025](constraints.md#assumptions)). Not a cross-project overview. |
| Project dashboard | The screen showing one project's progress, tasks, and members ([wireframe](ui/project-dashboard.md)). |
| MVP | Proposed minimum viable first release; contents and priorities await client approval. |

## Change log

| Version | Date | Entry | Change | Basis |
|---|---|---|---|---|
| v0.2 | 2026-10-29 | FR-013–FR-025 | Added as team decisions. | [ADR-002](adr/ADR-002-phase-scope-and-implementation-authorization.md) |
| v0.2 | 2026-10-29 | FR-002 | Origin adds who may create projects. | [Assumption A-018](constraints.md#assumptions) |
| v0.2 | 2026-10-29 | FR-006 | Shows Task priority and several assignees. | [Assumption A-019](constraints.md#assumptions), [Assumption A-021](constraints.md#assumptions) |
| v0.2 | 2026-10-29 | FR-010 | One or more assignees and unassignment replace “at most one assignee; reassignment and unassignment unspecified”. | [Assumption A-019](constraints.md#assumptions) |
| v0.2 | 2026-10-29 | FR-011 | Any assignee, instead of the single assigned Project Participant, may change the state. | [Assumption A-019](constraints.md#assumptions) |
| v0.2 | 2026-10-29 | FR-012 | Rounding defined; deleted tasks not counted. | [Assumption A-022](constraints.md#assumptions), [Assumption A-026](constraints.md#assumptions) |
| v0.2 | 2026-10-29 | NFR-001 | Interim identification method recorded. | [Assumption A-016](constraints.md#assumptions), [ADR-005](adr/ADR-005-authentication-with-jwt.md) |
| v0.2 | 2026-10-29 | NFR-002 | Basis notes the self-assignment risk. | [Assumption A-020](constraints.md#assumptions) |
| v0.2 | 2026-10-29 | NFR-008 | English replaces Portuguese. | [Assumption A-028](constraints.md#assumptions) |
| v0.2 | 2026-10-29 | User types | Permissions extended; permission matrix added. | [ADR-002](adr/ADR-002-phase-scope-and-implementation-authorization.md) |
| v0.2 | 2026-10-29 | MVP scope | FR-001–FR-025; release criteria US-001–US-026 and EX-001–EX-027. | [Assumption A-012](constraints.md#assumptions) |
| v0.2 | 2026-11-02 | FR-001, FR-002, FR-003, FR-022, permission matrix, glossary | Project description (optional), name limits, paging, and search details; no new FR number, so the MVP stays FR-001–FR-025. Decided by the project team in the conversation “Implement project management”. | [Assumption A-031](constraints.md#assumptions), [Assumption A-032](constraints.md#assumptions), [OQ-003](constraints.md#open-questions), [OQ-028](constraints.md#open-questions), [OQ-029](constraints.md#open-questions) |
| v0.2 | 2026-10-29 | Outside the initial scope | Task comments moved into the MVP; notifications stay excluded; registration, password recovery, restore, and comment editing listed as not provided. | [Assumption A-012](constraints.md#assumptions), [Assumption A-016](constraints.md#assumptions), [Assumption A-022](constraints.md#assumptions), [Assumption A-023](constraints.md#assumptions) |
| v0.2 | 2026-10-29 | Glossary | Assignee and Progress changed; workshop terms mapped; new terms added. | [ADR-002](adr/ADR-002-phase-scope-and-implementation-authorization.md) |








