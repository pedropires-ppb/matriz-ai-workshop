# Matriz — MVP User Stories (v0.2)

**Status:** Proposed backlog, not client-approved. Every priority is a proposed Must; ordering does not indicate relative priority. [Requirements and glossary](requirements.md) define shared terminology; [constraints and assumption register](constraints.md#assumptions) record the unconfirmed rules. Prepared projects, people, and tasks may be used to test each story independently. The reference column identifies the applicable unvalidated assumptions; it does not convert them into facts. “Project Participant” is distinct from “Project Owner”; lower-case “participant” includes any project member, including the owner (see glossary).

Revised on 2026-10-29: US-013–US-026 and EX-014–EX-027 record the project team's provisional decisions ([ADR-002](adr/ADR-002-phase-scope-and-implementation-authorization.md)), not client requests, and their priority is marked “team decision”. Existing entries changed by those decisions are marked *Changed in v0.2* and listed in the [change log](#change-log).

## Stories

| ID | Story | Priority (MoSCoW) | Related requirement | Proposed assumptions |
|---|---|---|---|---|
| US-001 | As a person authorized to create projects, I want to create a project with an optional description, so that I can organize the team's work as its Project Owner. *Changed on 2026-11-02 by team decision (the conversation “Implement project management”): description added; previously “…to create a project, so that…”* | Must (proposed) | [FR-002](requirements.md#functional-requirements) | [Assumption A-003](constraints.md#assumptions), [Assumption A-008](constraints.md#assumptions), [Assumption A-018](constraints.md#assumptions), [Assumption A-031](constraints.md#assumptions) |
| US-002 | As a Project Owner, I want to add a Project Participant to a project, so that they can access the team's work. | Must (proposed) | [FR-004](requirements.md#functional-requirements) | [Assumption A-001](constraints.md#assumptions), [Assumption A-002](constraints.md#assumptions) |
| US-003 | As a Project Owner, I want to create a task in a project, with an optional description, so that I can record work to be done. *Changed on 2026-11-05 by team decision (the conversation “Implement task management”): description added; previously “…to create a task in a project, so that I can record work to be done.”* | Must (proposed) | [FR-008](requirements.md#functional-requirements) | [Assumption A-004](constraints.md#assumptions), [Assumption A-010](constraints.md#assumptions), [Assumption A-033](constraints.md#assumptions) |
| US-004 | As a Project Participant, I want to view a project's tasks, so that I can see its recorded work. | Must (proposed) | [FR-006](requirements.md#functional-requirements) | [Assumption A-002](constraints.md#assumptions), [Assumption A-004](constraints.md#assumptions), [Assumption A-032](constraints.md#assumptions) |
| US-005 | As a Project Participant, I want to change the state of a task assigned to me, so that I can record its progress. | Must (proposed) | [FR-011](requirements.md#functional-requirements) | [Assumption A-001](constraints.md#assumptions), [Assumption A-005](constraints.md#assumptions), [Assumption A-011](constraints.md#assumptions), [Assumption A-019](constraints.md#assumptions) |
| US-006 | As a Project Participant, I want to view project progress, so that I can see how much work is Done. | Must (proposed) | [FR-012](requirements.md#functional-requirements) | [Assumption A-006](constraints.md#assumptions) |
| US-007 | As a Project Participant, I want to view projects in which I participate, so that I can find my work without seeing others' projects. | Must (proposed) | [FR-001](requirements.md#functional-requirements) | [Assumption A-002](constraints.md#assumptions), [Assumption A-011](constraints.md#assumptions) |
| US-008 | As a Project Owner, I want to rename a project and change its description, so that they stay accurate. *Changed on 2026-11-02 by team decision (the conversation “Implement project management”, question 14): previously “…to rename a project, so that its name stays accurate.”* | Must (proposed) | [FR-003](requirements.md#functional-requirements) | [Assumption A-001](constraints.md#assumptions), [Assumption A-003](constraints.md#assumptions), [Assumption A-011](constraints.md#assumptions), [Assumption A-031](constraints.md#assumptions) |
| US-009 | As a Project Owner, I want to remove a non-owner Project Participant from a project, so that I can update who can view the work. | Must (proposed) | [FR-005](requirements.md#functional-requirements) | [Assumption A-002](constraints.md#assumptions), [Assumption A-009](constraints.md#assumptions) |
| US-010 | As a Project Owner, I want to complete a project with no unfinished tasks, so that completed work is distinguished from active work. | Must (proposed) | [FR-007](requirements.md#functional-requirements) | [Assumption A-003](constraints.md#assumptions), [Assumption A-007](constraints.md#assumptions), [Assumption A-011](constraints.md#assumptions) |
| US-011 | As a Project Owner, I want to change a task's title or description, so that I can correct how the work is identified and described. *Changed on 2026-11-05 by team decision (the conversation “Implement task management”): description added; previously “…to change a task's title, so that I can correct how the work is identified.”* | Must (proposed) | [FR-009](requirements.md#functional-requirements) | [Assumption A-001](constraints.md#assumptions), [Assumption A-004](constraints.md#assumptions), [Assumption A-033](constraints.md#assumptions) |
| US-012 | As a Project Owner, I want to assign a task to one or more members of the project, so that responsibility for its execution is clear. *Changed in v0.2: one or more members.* | Must (proposed) | [FR-010](requirements.md#functional-requirements) | [Assumption A-001](constraints.md#assumptions), [Assumption A-004](constraints.md#assumptions), [Assumption A-019](constraints.md#assumptions) |
| US-013 | As a person with a prepared account, I want to sign in, so that I can reach the projects in which I participate. | Must (team decision) | [FR-013](requirements.md#functional-requirements) | [Assumption A-016](constraints.md#assumptions), [Assumption A-017](constraints.md#assumptions) |
| US-014 | As a signed-in person, I want to sign out, so that my sign-in cannot be used by anyone else afterwards. | Must (team decision) | [FR-014](requirements.md#functional-requirements) | [Assumption A-016](constraints.md#assumptions) |
| US-015 | As a signed-in person, I want to see who I am signed in as, so that I know under whose name my actions are recorded. | Must (team decision) | [FR-015](requirements.md#functional-requirements) | [Assumption A-017](constraints.md#assumptions) |
| US-016 | As a Project Owner, I want to delete a project, so that a project created by mistake or no longer needed stops appearing. | Must (team decision) | [FR-016](requirements.md#functional-requirements) | [Assumption A-022](constraints.md#assumptions) |
| US-017 | As a Project Owner, I want to delete a task, so that a task recorded by mistake or no longer needed stops appearing and counting in progress. | Must (team decision) | [FR-017](requirements.md#functional-requirements) | [Assumption A-022](constraints.md#assumptions) |
| US-018 | As a Project Owner, I want to set a task's Task priority, so that the team knows which work matters most. | Must (team decision) | [FR-018](requirements.md#functional-requirements) | [Assumption A-021](constraints.md#assumptions) |
| US-019 | As a Project Owner, I want to unassign a member from a task, so that responsibility stays accurate and the member can later be removed from the project if needed. | Must (team decision) | [FR-010](requirements.md#functional-requirements) | [Assumption A-009](constraints.md#assumptions), [Assumption A-019](constraints.md#assumptions) |
| US-020 | As a Project Participant, I want to assign or unassign myself on a task, so that I can take on or hand back work. | Must (team decision) | [FR-019](requirements.md#functional-requirements) | [Assumption A-019](constraints.md#assumptions), [Assumption A-020](constraints.md#assumptions) |
| US-021 | As a Member of a project, I want to add a comment to one of its tasks, so that I can share information about the work. | Must (team decision) | [FR-020](requirements.md#functional-requirements) | [Assumption A-011](constraints.md#assumptions), [Assumption A-023](constraints.md#assumptions) |
| US-022 | As a Member of a project, I want to read a task's comments, so that I know what others have said about the work. | Must (team decision) | [FR-021](requirements.md#functional-requirements) | [Assumption A-017](constraints.md#assumptions), [Assumption A-023](constraints.md#assumptions) |
| US-023 | As a signed-in person, I want to search my projects by name, so that I can find a project quickly. | Must (team decision) | [FR-022](requirements.md#functional-requirements) | [Assumption A-002](constraints.md#assumptions), [Assumption A-024](constraints.md#assumptions) |
| US-024 | As a Member of a project, I want to filter its tasks by state and Task priority, so that I can focus on the relevant work. | Must (team decision) | [FR-023](requirements.md#functional-requirements) | [Assumption A-021](constraints.md#assumptions), [Assumption A-024](constraints.md#assumptions) |
| US-025 | As a signed-in person, I want to see My projects with their task counts by state, so that I can choose where to work next. | Must (team decision) | [FR-024](requirements.md#functional-requirements) | [Assumption A-002](constraints.md#assumptions), [Assumption A-025](constraints.md#assumptions) |
| US-026 | As a Member of a project, I want to see its members and their roles, so that I know who works on it. | Must (team decision) | [FR-025](requirements.md#functional-requirements) | [Assumption A-027](constraints.md#assumptions) |

## Acceptance criteria

The following are **proposed** test outcomes, dependent on the assumptions listed beside each story. “When” describes the person's action, not an implementation mechanism.

### US-001 — Create project

1. **Given** a person is authorized to create projects, **When** they create “Project Alpha”, **Then** it is visible to them under that name with state Active.
2. **Given** that person created “Project Alpha”, **When** they view its details, **Then** they appear as both Project Owner and a member of the project ([Assumption A-008](constraints.md#assumptions)).
3. **Given** a person is authorized to create projects, **When** they create “Project Alpha” with the description “First release”, **Then** its details show that description; **and When** they create another project without a description, **Then** its details show no description ([Assumption A-031](constraints.md#assumptions)). *Added on 2026-11-02.*
4. **Given** a person is authorized to create projects, **When** they create projects whose names have 1 and 100 characters, and a description of 1000 characters, **Then** all are created; **and When** they submit a name of 101 characters, a name of only spaces, or a description of 1001 characters, **Then** no project is created and they are told which text is invalid and why ([Assumption A-031](constraints.md#assumptions)). *Added on 2026-11-02.*

### US-002 — Add participant

1. **Given** an Active project and its Project Owner, **When** the owner adds another person as a Project Participant, **Then** that person appears among the project's members and can view the project.

### US-003 — Create task

1. **Given** an Active project and its Project Owner, **When** the owner creates a task titled “Prepare proposal” without an assignee, **Then** it appears in that project with title “Prepare proposal”, state To do, and no assignee ([Assumption A-010](constraints.md#assumptions)).
2. **Given** an Active project and its Project Owner, **When** the owner creates a task with title “Prepare proposal” and description “Draft the offer for the client”, **Then** the task shows both ([Assumption A-033](constraints.md#assumptions)). *Added on 2026-11-05.*
3. **Given** an Active project and its Project Owner, **When** the owner creates a task whose title has exactly 150 characters, and another whose description has exactly 2000 characters, **Then** both tasks are created; a title of 151 characters or a description of 2001 characters is refused with a message that states the limit ([Assumption A-033](constraints.md#assumptions)). *Added on 2026-11-05.*
4. **Given** an Active project and its Project Owner, **When** the owner creates a task whose title has spaces around the text, or whose description is made only of spaces or of no-break spaces, **Then** the title is saved without the surrounding whitespace and the description is saved as no description ([Assumption A-033](constraints.md#assumptions)). *Added on 2026-11-05.*

### US-004 — View tasks

1. **Given** a project containing “Prepare proposal” (To do, no assignee) and “Review proposal” (In progress, assigned to member Ana), **When** one of its Project Participants views its tasks, **Then** they see “Prepare proposal” with state To do and no assignee, and “Review proposal” with state In progress and assignee Ana.
2. **Given** a project with 25 tasks, **When** a Member views its tasks without choosing a page size, **Then** they see the first 20 tasks, oldest first, the total of 25 tasks, and 2 pages; page 2 shows the other 5, and page 3 is empty ([Assumption A-032](constraints.md#assumptions)). *Added on 2026-11-05.*

### US-005 — Change task state

1. **Given** an Active project with a To do task assigned to a Project Participant, **When** that participant changes the task to Done, **Then** it shows Done.
2. **Given** that task is Done in the Active project, **When** its assignee changes it to In progress, **Then** it shows In progress (reopened).
3. **Given** an Active project with a To do task assigned to Project Participants Ana and Rui, **When** Rui changes it to In progress, **Then** it shows In progress; any assignee may change the state ([Assumption A-019](constraints.md#assumptions)). *Added in v0.2.*

The Project Owner's ability to change the state of any task in an Active project is tested in [EX-013](#edge-cases-and-exceptions).

### US-006 — View progress

1. **Given** a project with four tasks, two Done, one In progress, and one To do, **When** a Project Participant views progress, **Then** the counts are 2 Done, 1 In progress, and 1 To do, and the Done percentage is 50%.
2. **Given** a project with zero tasks, **When** a Project Participant views its progress, **Then** “No tasks” is shown instead of a percentage ([Assumption A-006](constraints.md#assumptions)).

### US-007 — View accessible projects

1. **Given** a person participates in project A but not project B, **When** they view their accessible projects, **Then** A appears and B does not.
2. **Given** a person is a member of a Completed project, **When** they view their accessible projects, **Then** it remains visible with state Completed ([Assumption A-011](constraints.md#assumptions)).
3. **Given** a person participates in 25 projects, **When** they view their accessible projects without choosing a page size, **Then** the first 20 are listed, ordered by name ignoring letter case, with the total of 25; **and When** they view page 2, **Then** the remaining 5 are listed; **and When** they view page 3, **Then** no project is listed and no error is shown ([Assumption A-032](constraints.md#assumptions)). *Added on 2026-11-02.*
4. **Given** a person views their accessible projects, **When** they ask for page 0, a page size of 0, or a page size above 100, **Then** the request is refused with a message saying which value is invalid, and nothing changes ([Assumption A-032](constraints.md#assumptions)). *Added on 2026-11-02.*

### US-008 — Rename project

1. **Given** an Active project named “Project Alpha” and its Project Owner, **When** the owner renames it “Project Beta”, **Then** it displays “Project Beta”.
2. **Given** an Active project and its Project Owner, **When** the owner changes the description to “Second release”, and later removes it, **Then** the details show “Second release”, and then no description; the name is unchanged ([Assumption A-031](constraints.md#assumptions)). *Added on 2026-11-02.*
3. **Given** an Active project and its Project Owner, **When** the owner renames it to a name of 101 characters or to only spaces, or sets a description of 1001 characters, **Then** the project is unchanged and the owner is told which text is invalid and why; **and When** the new name has 100 characters, **Then** the project is renamed ([Assumption A-031](constraints.md#assumptions)). *Added on 2026-11-02.*
4. **Given** a Project Participant who is not the Project Owner, **When** they attempt to rename the project or change its description, **Then** the project is unchanged ([Assumption A-001](constraints.md#assumptions)). *Added on 2026-11-02.*

### US-009 — Remove participant

1. **Given** an Active project and a non-owner Project Participant with no assigned tasks, **When** its Project Owner removes that participant, **Then** that person no longer appears among the project's members and can no longer view the project.

### US-010 — Complete project

1. **Given** an Active project whose tasks are all Done, **When** its Project Owner completes the project, **Then** its state is Completed.
2. **Given** an Active project with zero tasks, **When** its Project Owner completes the project, **Then** its state is Completed under the proposed no-unfinished-tasks rule; this interpretation needs explicit client approval ([Assumption A-007](constraints.md#assumptions)).

### US-011 — Rename task

1. **Given** an Active project with task “Prepare proposal” and its Project Owner, **When** the owner changes the task title to “Prepare final proposal”, **Then** the task shows the new title.
2. **Given** an Active project with a task that has a description and its Project Owner, **When** the owner changes the description, **Then** the task shows the new description; **When** the owner clears it, **Then** the task shows no description, and the title is unchanged ([Assumption A-033](constraints.md#assumptions)). *Added on 2026-11-05.*

### US-012 — Assign task

1. **Given** an Active project, an unassigned task, and a Project Participant of that project, **When** its Project Owner assigns the task to that participant, **Then** the task shows that participant as an assignee. *Changed in v0.2: “its single assignee” became “an assignee”.*
2. **Given** an Active project and a task assigned to Project Participant Ana, **When** its Project Owner also assigns Project Participant Rui, **Then** the task shows both Ana and Rui as assignees ([Assumption A-019](constraints.md#assumptions)). *Added in v0.2.*
3. **Given** an Active project and an unassigned task, **When** its Project Owner assigns the task to themselves, **Then** the task shows the Project Owner as an assignee ([Assumption A-019](constraints.md#assumptions)). *Added in v0.2.*

### US-013 — Sign in

1. **Given** a prepared account with login name “ana” and its password, **When** Ana signs in with them, **Then** she is signed in and the projects in which she participates are shown to her ([Assumption A-016](constraints.md#assumptions)).

### US-014 — Sign out

1. **Given** Ana is signed in, **When** she signs out, **Then** she must sign in again before any project information is shown to her.
2. **Given** Ana has signed out, **When** someone tries to reuse her ended sign-in to view one of her projects, **Then** no project information is disclosed ([Assumption A-016](constraints.md#assumptions)).

### US-015 — See who is signed in

1. **Given** Ana, whose display name is “Ana Silva”, is signed in, **When** she views My projects, **Then** “Ana Silva” is shown as the signed-in person ([Assumption A-017](constraints.md#assumptions)).

### US-016 — Delete project

1. **Given** an Active project “Project Alpha” and its Project Owner, **When** the owner deletes it and confirms, **Then** it no longer appears to any of its members, and an attempt to view it directly discloses nothing, as for a project that does not exist ([Assumption A-022](constraints.md#assumptions)).
2. **Given** the Project Owner has started to delete “Project Alpha”, **When** they cancel instead of confirming, **Then** the project is unchanged.

### US-017 — Delete task

1. **Given** an Active project with four tasks, two Done, one In progress, and one To do, **When** its Project Owner deletes the To do task and confirms, **Then** that task no longer appears to any member, and progress shows 2 Done, 1 In progress, 0 To do, and 67% ([Assumption A-022](constraints.md#assumptions), [Assumption A-026](constraints.md#assumptions)).
2. **Given** an Active project and a non-owner Project Participant whose only assigned task is deleted by the Project Owner, **When** the owner then removes that participant, **Then** the person no longer appears among the project's members ([Assumption A-009](constraints.md#assumptions), [Assumption A-022](constraints.md#assumptions)).

### US-018 — Set task priority

1. **Given** an Active project and its Project Owner, **When** the owner creates a task without choosing a Task priority, **Then** the task shows Task priority Medium ([Assumption A-021](constraints.md#assumptions)).
2. **Given** an Active project and its Project Owner, **When** the owner creates a task with Task priority High, **Then** the task shows Task priority High.
3. **Given** an Active project with a task of Task priority Medium, **When** its Project Owner changes it to Low, **Then** the task shows Task priority Low.

### US-019 — Unassign a member

1. **Given** an Active project and a task assigned to Ana and Rui, **When** its Project Owner unassigns Rui, **Then** the task shows only Ana as assignee ([Assumption A-019](constraints.md#assumptions)).
2. **Given** an Active project and a non-owner Project Participant whose only assigned task is unassigned from them by the Project Owner, **When** the owner then removes that participant, **Then** the person no longer appears among the project's members ([Assumption A-009](constraints.md#assumptions)).

### US-020 — Assign or unassign myself

1. **Given** an Active project and a task not assigned to its Project Participant Rui, **When** Rui assigns himself, **Then** the task shows Rui among its assignees, and Rui can change its state ([Assumption A-019](constraints.md#assumptions), [Assumption A-020](constraints.md#assumptions)).
2. **Given** an Active project and a task assigned to Ana and Rui, **When** Rui unassigns himself, **Then** the task shows only Ana as assignee.

### US-021 — Add comment

1. **Given** a task in a project of which Ana is a Member, **When** Ana adds the comment “Draft sent to the client”, **Then** the comment appears on the task with Ana's display name and the date and time it was added ([Assumption A-023](constraints.md#assumptions)).

### US-022 — Read comments

1. **Given** a task with the comment “First” added by Ana and a later comment “Second” added by Rui, **When** a Member of its project views the task's comments, **Then** “First” is shown before “Second”, each with its author's display name and the date and time it was added ([Assumption A-023](constraints.md#assumptions)).
2. **Given** a task with no comments, **When** a Member of its project views the task's comments, **Then** they are told there are no comments yet.

### US-023 — Search projects

1. **Given** Ana is a member of “Website Redesign” and “Mobile App”, and not of “Website Audit”, **When** she searches her projects for “web”, **Then** only “Website Redesign” is listed ([Assumption A-024](constraints.md#assumptions)).
2. **Given** Ana is a member of a project named “Café Menu”, **When** she searches for “cafe”, **Then** it is not listed, and when she searches for “CAFÉ” it is listed; the search combines with paging: the total counts only the matching projects ([Assumption A-024](constraints.md#assumptions), [Assumption A-032](constraints.md#assumptions)). *Added on 2026-11-02.*

### US-024 — Filter tasks

1. **Given** a project with tasks T1 (To do, High), T2 (In progress, High), T3 (Done, Low), and T4 (To do, Medium), **When** a Member filters its tasks by states To do and In progress and by Task priority High, **Then** only T1 and T2 are listed ([Assumption A-024](constraints.md#assumptions)).
2. **Given** that filter is applied, **When** the Member clears all filters, **Then** all four tasks are listed.
3. **Given** a project with more tasks than fit in one page, **When** a Member applies a filter, **Then** the total and the pages count only the tasks that match, and the filter stays applied while the Member moves between pages ([Assumption A-024](constraints.md#assumptions), [Assumption A-032](constraints.md#assumptions)). *Added on 2026-11-05.*

### US-025 — View My projects

1. **Given** Ana is a member of Active project A (2 To do, 1 In progress, 1 Done tasks) and Completed project B (3 Done tasks), and not of project C, **When** she views My projects, **Then** A is shown as Active with 2 To do, 1 In progress, and 1 Done; B is shown as Completed with 0 To do, 0 In progress, and 3 Done; C is not shown; and no totals across projects are shown ([Assumption A-025](constraints.md#assumptions)).
2. **Given** Ana is viewing My projects, **When** she selects project A, **Then** the Project dashboard of project A is shown.
3. **Given** a signed-in person who is a member of no project, **When** they view My projects, **Then** they are told they have no projects yet.

### US-026 — View members

1. **Given** a project with Project Owner Ana and Project Participants Rui and Eva, **When** Rui views the project's members, **Then** Ana is shown as Project Owner, and Rui and Eva as Project Participants ([Assumption A-027](constraints.md#assumptions)).
2. **Given** a Completed project with Project Owner Ana and Project Participant Rui, **When** Rui views the project's members, **Then** the list is shown, with Ana first, as for an Active project ([Assumption A-034](constraints.md#assumptions)).
3. **Given** a project with Project Owner Ana and Project Participants Rui and Eva, **When** Ana views the members, **Then** each person appears once, with the display name and the role, no login name is shown, Ana comes first, and Eva comes before Rui ([Assumption A-034](constraints.md#assumptions)). *Added on 2026-11-05 (the conversation “Implement assignees and comments”).*

A person who is not a member asks for the members: see [EX-003](#edge-cases-and-exceptions).

## Edge cases and exceptions

All are proposed; none grants permissions beyond its linked requirement. Each outcome is observable and leaves existing information unchanged where access or validation fails.

| ID | Story | Situation and expected behaviour (Given / When / Then) |
|---|---|---|
| EX-001 | US-001 | **Given** a person authorized to create projects, **When** they submit an empty project name, **Then** no project is created and they are told a name is required ([Assumption A-003](constraints.md#assumptions)). |
| EX-002 | US-003 | **Given** an Active project and its Project Owner, **When** the owner submits an empty task title, **Then** no task is created and they are told a title is required ([Assumption A-004](constraints.md#assumptions)); the same happens when the title is made only of spaces or no-break spaces, and a title of more than 150 characters is refused with a message that states the limit ([Assumption A-033](constraints.md#assumptions)). *Changed on 2026-11-05.* |
| EX-003 | US-007 | **Given** a person who is not a member of a project, **When** they attempt to view that project directly, **Then** neither the project nor its tasks are disclosed ([Assumption A-002](constraints.md#assumptions)). |
| EX-004 | US-005 | **Given** an Active project and a task assigned to someone else, **When** a Project Participant who is not its Project Owner attempts to change the task's state, **Then** its state is unchanged ([Assumption A-001](constraints.md#assumptions)). |
| EX-005 | US-006 | **Given** a project with zero tasks, **When** a Project Participant views its progress, **Then** the counts are 0 To do, 0 In progress, and 0 Done, and no percentage value (neither 0% nor 100%) is shown ([Assumption A-006](constraints.md#assumptions)). |
| EX-006 | US-010 | **Given** an Active project with at least one To do or In progress task, **When** its Project Owner attempts to complete it, **Then** it remains Active ([Assumption A-007](constraints.md#assumptions)). |
| EX-007 | US-009 | **Given** a non-owner Project Participant with at least one task assigned to them in an Active project, **When** its Project Owner attempts to remove that participant, **Then** the person retains access and the owner is told there are assigned tasks ([Assumption A-009](constraints.md#assumptions)). |
| EX-008 | US-005 | **Given** a task in a Completed project, **When** its assignee attempts to change its state, **Then** the state is unchanged ([Assumption A-011](constraints.md#assumptions)). |
| EX-009 | US-008 | **Given** a Completed project, **When** its Project Owner attempts to rename it, **Then** its name is unchanged ([Assumption A-011](constraints.md#assumptions)). |
| EX-010 | US-008 | **Given** an Active project and its Project Owner, **When** the owner tries to rename it to an empty name, **Then** its name is unchanged and the owner is told a name is required ([Assumption A-003](constraints.md#assumptions)). |
| EX-011 | US-011 | **Given** an Active project with a task and its Project Owner, **When** the owner tries to change the task title to an empty title, **Then** the title is unchanged and the owner is told a title is required ([Assumption A-004](constraints.md#assumptions)); the same applies to a title made only of spaces or no-break spaces and to a title of more than 150 characters ([Assumption A-033](constraints.md#assumptions)). *Changed on 2026-11-05.* |
| EX-012 | US-007 | **Given** a person whose identity has not been established, **When** they attempt to view or change a project or task directly, **Then** no project or task information is disclosed and existing information is unchanged ([NFR-001](requirements.md#non-functional-requirements-and-unresolved-quality-criteria), [Assumption A-014](constraints.md#assumptions)). |
| EX-013 | US-005 | **Given** an Active project with an unassigned To do task, **When** its Project Owner changes the task to Done, **Then** it shows Done; the owner may change the state without being the assignee ([FR-011](requirements.md#functional-requirements), [Assumption A-001](constraints.md#assumptions)). |
| EX-014 | US-013 | **Given** a prepared account, **When** someone tries to sign in with its login name and a wrong password, or with a login name that does not exist, **Then** they are not signed in and are told only that the login name or password is incorrect, without saying which ([Assumption A-016](constraints.md#assumptions)). |
| EX-015 | US-016 | **Given** a Completed project, **When** its Project Owner attempts to delete it, **Then** it remains visible and unchanged, and the owner is told that a Completed project cannot be changed ([Assumption A-011](constraints.md#assumptions), [Assumption A-022](constraints.md#assumptions)). |
| EX-016 | US-016, US-017 | **Given** an Active project, **When** one of its Project Participants attempts to delete the project or one of its tasks, **Then** nothing is deleted ([Assumption A-001](constraints.md#assumptions), [Assumption A-022](constraints.md#assumptions)). |
| EX-017 | US-017 | **Given** a task in a Completed project, **When** its Project Owner attempts to delete the task, **Then** the task remains visible and unchanged ([Assumption A-011](constraints.md#assumptions), [Assumption A-022](constraints.md#assumptions)). |
| EX-018 | US-017 | **Given** an Active project whose only unfinished task is In progress, **When** its Project Owner deletes that task, **Then** the project can be completed, because deleted tasks are not counted as unfinished. This lets the completion rule be bypassed and needs explicit client approval ([Assumption A-007](constraints.md#assumptions), [Assumption A-022](constraints.md#assumptions), [OQ-023](constraints.md#open-questions)). |
| EX-019 | US-018 | **Given** an Active project, **When** one of its Project Participants attempts to change a task's Task priority, **Then** the Task priority is unchanged ([Assumption A-021](constraints.md#assumptions)). |
| EX-020 | US-012 | **Given** an Active project, **When** its Project Owner attempts to assign a task to a person who is not a member of the project, **Then** the task's assignees are unchanged and the owner is told the person must be a member ([Assumption A-019](constraints.md#assumptions)). |
| EX-021 | US-020 | **Given** an Active project and a task assigned to Ana, **When** Project Participant Rui attempts to unassign Ana or to assign another member, **Then** the task's assignees are unchanged ([Assumption A-020](constraints.md#assumptions)). |
| EX-022 | US-012, US-019, US-020 | **Given** a task in a Completed project, **When** any member attempts to assign or unassign a member, **Then** the task's assignees are unchanged ([Assumption A-011](constraints.md#assumptions)). |
| EX-023 | US-021 | **Given** a task in a project of which Ana is a Member, **When** she submits an empty comment, **Then** no comment is added and she is told text is required ([Assumption A-023](constraints.md#assumptions)). |
| EX-024 | US-021 | **Given** a task in a Completed project, **When** one of its members adds a comment, **Then** the comment is added and the project's other information is unchanged ([Assumption A-011](constraints.md#assumptions), [Assumption A-023](constraints.md#assumptions)). |
| EX-025 | US-023 | **Given** Ana is a member of no project whose name contains “audit”, and a project she is not a member of is named “Security Audit”, **When** she searches her projects for “audit”, **Then** no project is listed and she is told no projects match ([Assumption A-002](constraints.md#assumptions), [Assumption A-024](constraints.md#assumptions)). |
| EX-026 | US-024 | **Given** a project with four tasks, two of them Done, **When** a Member filters its tasks by state To do, **Then** only To do tasks are listed while progress still counts all four tasks and shows 50%; if no task matches, the Member is told no tasks match the filters ([Assumption A-024](constraints.md#assumptions)); a filter value that is not a state or a Task priority is refused. *Changed on 2026-11-05.* |
| EX-027 | US-006 | **Given** projects with 1 of 3, 2 of 3, and 1 of 8 tasks Done, **When** a Project Participant views their progress, **Then** the Done percentages shown are 33%, 67%, and 13% ([Assumption A-026](constraints.md#assumptions)). |

Client decisions that may change these tests, including naming rules (OQ-003), access provisioning (OQ-001), simultaneous edits (OQ-017), and the team's provisional decisions (OQ-022–OQ-027), are listed in the [open questions](constraints.md#open-questions).

## Change log

| Version | Date | Entry | Change | Basis |
|---|---|---|---|---|
| v0.2 | 2026-10-29 | US-013–US-026, EX-014–EX-027 | Added as team decisions. | [ADR-002](adr/ADR-002-phase-scope-and-implementation-authorization.md) |
| v0.2 | 2026-11-02 | US-001, US-007, US-008, US-023 | Criteria for the project description, text limits, paging, and accents added; US-001 and US-008 wording extended. No new EX; EX-001 and EX-010 stay as written. Decided by the project team in the conversation “Implement project management”. | [Assumption A-031](constraints.md#assumptions), [Assumption A-032](constraints.md#assumptions), [Assumption A-024](constraints.md#assumptions) |
| v0.2 | 2026-11-05 | US-003, US-004, US-011, US-024, EX-002, EX-011, EX-026 | Criteria for the task description, title limits, whitespace, paging, and filter details added; US-003 and US-011 wording extended. No new EX. Decided by the project team in the conversation “Implement task management”. | [Assumption A-033](constraints.md#assumptions), [Assumption A-032](constraints.md#assumptions), [Assumption A-024](constraints.md#assumptions) |
| v0.2 | 2026-11-05 | US-026 | Criteria 2 and 3 added: a Completed project, and the order and fields of the list. No new EX. The assistant's proposal in the conversation “Implement assignees and comments”, confirmed by the user; the team's review is pending. | [Assumption A-034](constraints.md#assumptions) |
| v0.2 | 2026-10-29 | US-001 | Assumptions add who may create projects. | [Assumption A-018](constraints.md#assumptions) |
| v0.2 | 2026-10-29 | US-005 | Assumptions add several assignees; acceptance criterion 3 added. | [Assumption A-019](constraints.md#assumptions) |
| v0.2 | 2026-10-29 | US-012 | Story extended to one or more members; criterion 1 no longer says “single assignee”; criteria 2 and 3 added. | [Assumption A-019](constraints.md#assumptions) |










