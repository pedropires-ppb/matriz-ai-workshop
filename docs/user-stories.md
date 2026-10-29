# Matriz — MVP User Stories (v0.1)

**Status:** Proposed backlog, not client-approved. Every priority is a proposed Must; ordering does not indicate relative priority. [Requirements and glossary](requirements.md) define shared terminology; [constraints and assumption register](constraints.md#assumptions) record the unconfirmed rules. Prepared projects, people, and tasks may be used to test each story independently. The reference column identifies the applicable unvalidated assumptions; it does not convert them into facts. “Project Participant” is distinct from “Project Owner”; lower-case “participant” includes any project member, including the owner (see glossary).

## Stories

| ID | Story | Priority (MoSCoW) | Related requirement | Proposed assumptions |
|---|---|---|---|---|
| US-001 | As a person authorized to create projects, I want to create a project, so that I can organize the team's work as its Project Owner. | Must (proposed) | [FR-002](requirements.md#functional-requirements) | [Assumption A-003](constraints.md#assumptions), [Assumption A-008](constraints.md#assumptions) |
| US-002 | As a Project Owner, I want to add a Project Participant to a project, so that they can access the team's work. | Must (proposed) | [FR-004](requirements.md#functional-requirements) | [Assumption A-001](constraints.md#assumptions), [Assumption A-002](constraints.md#assumptions) |
| US-003 | As a Project Owner, I want to create a task in a project, so that I can record work to be done. | Must (proposed) | [FR-008](requirements.md#functional-requirements) | [Assumption A-004](constraints.md#assumptions), [Assumption A-010](constraints.md#assumptions) |
| US-004 | As a Project Participant, I want to view a project's tasks, so that I can see its recorded work. | Must (proposed) | [FR-006](requirements.md#functional-requirements) | [Assumption A-002](constraints.md#assumptions), [Assumption A-004](constraints.md#assumptions) |
| US-005 | As a Project Participant, I want to change the state of a task assigned to me, so that I can record its progress. | Must (proposed) | [FR-011](requirements.md#functional-requirements) | [Assumption A-001](constraints.md#assumptions), [Assumption A-005](constraints.md#assumptions), [Assumption A-011](constraints.md#assumptions) |
| US-006 | As a Project Participant, I want to view project progress, so that I can see how much work is Done. | Must (proposed) | [FR-012](requirements.md#functional-requirements) | [Assumption A-006](constraints.md#assumptions) |
| US-007 | As a Project Participant, I want to view projects in which I participate, so that I can find my work without seeing others' projects. | Must (proposed) | [FR-001](requirements.md#functional-requirements) | [Assumption A-002](constraints.md#assumptions), [Assumption A-011](constraints.md#assumptions) |
| US-008 | As a Project Owner, I want to rename a project, so that its name stays accurate. | Must (proposed) | [FR-003](requirements.md#functional-requirements) | [Assumption A-001](constraints.md#assumptions), [Assumption A-003](constraints.md#assumptions), [Assumption A-011](constraints.md#assumptions) |
| US-009 | As a Project Owner, I want to remove a non-owner Project Participant from a project, so that I can update who can view the work. | Must (proposed) | [FR-005](requirements.md#functional-requirements) | [Assumption A-002](constraints.md#assumptions), [Assumption A-009](constraints.md#assumptions) |
| US-010 | As a Project Owner, I want to complete a project with no unfinished tasks, so that completed work is distinguished from active work. | Must (proposed) | [FR-007](requirements.md#functional-requirements) | [Assumption A-003](constraints.md#assumptions), [Assumption A-007](constraints.md#assumptions), [Assumption A-011](constraints.md#assumptions) |
| US-011 | As a Project Owner, I want to change a task's title, so that I can correct how the work is identified. | Must (proposed) | [FR-009](requirements.md#functional-requirements) | [Assumption A-001](constraints.md#assumptions), [Assumption A-004](constraints.md#assumptions) |
| US-012 | As a Project Owner, I want to assign a task to a Project Participant, so that responsibility for its execution is clear. | Must (proposed) | [FR-010](requirements.md#functional-requirements) | [Assumption A-001](constraints.md#assumptions), [Assumption A-004](constraints.md#assumptions) |

## Acceptance criteria

The following are **proposed** test outcomes, dependent on the assumptions listed beside each story. “When” describes the person's action, not an implementation mechanism.

### US-001 — Create project

1. **Given** a person is authorized to create projects, **When** they create “Project Alpha”, **Then** it is visible to them under that name with state Active.
2. **Given** that person created “Project Alpha”, **When** they view its details, **Then** they appear as both Project Owner and a member of the project ([Assumption A-008](constraints.md#assumptions)).

### US-002 — Add participant

1. **Given** an Active project and its Project Owner, **When** the owner adds another person as a Project Participant, **Then** that person appears among the project's members and can view the project.

### US-003 — Create task

1. **Given** an Active project and its Project Owner, **When** the owner creates a task titled “Prepare proposal” without an assignee, **Then** it appears in that project with title “Prepare proposal”, state To do, and no assignee ([Assumption A-010](constraints.md#assumptions)).

### US-004 — View tasks

1. **Given** a project containing “Prepare proposal” (To do, no assignee) and “Review proposal” (In progress, assigned to member Ana), **When** one of its Project Participants views its tasks, **Then** they see “Prepare proposal” with state To do and no assignee, and “Review proposal” with state In progress and assignee Ana.

### US-005 — Change task state

1. **Given** an Active project with a To do task assigned to a Project Participant, **When** that participant changes the task to Done, **Then** it shows Done.
2. **Given** that task is Done in the Active project, **When** its assignee changes it to In progress, **Then** it shows In progress (reopened).

The Project Owner's ability to change the state of any task in an Active project is tested in [EX-013](#edge-cases-and-exceptions).

### US-006 — View progress

1. **Given** a project with four tasks, two Done, one In progress, and one To do, **When** a Project Participant views progress, **Then** the counts are 2 Done, 1 In progress, and 1 To do, and the Done percentage is 50%.
2. **Given** a project with zero tasks, **When** a Project Participant views its progress, **Then** “No tasks” is shown instead of a percentage ([Assumption A-006](constraints.md#assumptions)).

### US-007 — View accessible projects

1. **Given** a person participates in project A but not project B, **When** they view their accessible projects, **Then** A appears and B does not.
2. **Given** a person is a member of a Completed project, **When** they view their accessible projects, **Then** it remains visible with state Completed ([Assumption A-011](constraints.md#assumptions)).

### US-008 — Rename project

1. **Given** an Active project named “Project Alpha” and its Project Owner, **When** the owner renames it “Project Beta”, **Then** it displays “Project Beta”.

### US-009 — Remove participant

1. **Given** an Active project and a non-owner Project Participant with no assigned tasks, **When** its Project Owner removes that participant, **Then** that person no longer appears among the project's members and can no longer view the project.

### US-010 — Complete project

1. **Given** an Active project whose tasks are all Done, **When** its Project Owner completes the project, **Then** its state is Completed.
2. **Given** an Active project with zero tasks, **When** its Project Owner completes the project, **Then** its state is Completed under the proposed no-unfinished-tasks rule; this interpretation needs explicit client approval ([Assumption A-007](constraints.md#assumptions)).

### US-011 — Rename task

1. **Given** an Active project with task “Prepare proposal” and its Project Owner, **When** the owner changes the task title to “Prepare final proposal”, **Then** the task shows the new title.

### US-012 — Assign task

1. **Given** an Active project, an unassigned task, and a Project Participant of that project, **When** its Project Owner assigns the task to that participant, **Then** the task shows that participant as its single assignee.

## Edge cases and exceptions

All are proposed; none grants permissions beyond its linked requirement. Each outcome is observable and leaves existing information unchanged where access or validation fails.

| ID | Story | Situation and expected behaviour (Given / When / Then) |
|---|---|---|
| EX-001 | US-001 | **Given** a person authorized to create projects, **When** they submit an empty project name, **Then** no project is created and they are told a name is required ([Assumption A-003](constraints.md#assumptions)). |
| EX-002 | US-003 | **Given** an Active project and its Project Owner, **When** the owner submits an empty task title, **Then** no task is created and they are told a title is required ([Assumption A-004](constraints.md#assumptions)). |
| EX-003 | US-007 | **Given** a person who is not a member of a project, **When** they attempt to view that project directly, **Then** neither the project nor its tasks are disclosed ([Assumption A-002](constraints.md#assumptions)). |
| EX-004 | US-005 | **Given** an Active project and a task assigned to someone else, **When** a Project Participant who is not its Project Owner attempts to change the task's state, **Then** its state is unchanged ([Assumption A-001](constraints.md#assumptions)). |
| EX-005 | US-006 | **Given** a project with zero tasks, **When** a Project Participant views its progress, **Then** the counts are 0 To do, 0 In progress, and 0 Done, and no percentage value (neither 0% nor 100%) is shown ([Assumption A-006](constraints.md#assumptions)). |
| EX-006 | US-010 | **Given** an Active project with at least one To do or In progress task, **When** its Project Owner attempts to complete it, **Then** it remains Active ([Assumption A-007](constraints.md#assumptions)). |
| EX-007 | US-009 | **Given** a non-owner Project Participant with at least one task assigned to them in an Active project, **When** its Project Owner attempts to remove that participant, **Then** the person retains access and the owner is told there are assigned tasks ([Assumption A-009](constraints.md#assumptions)). |
| EX-008 | US-005 | **Given** a task in a Completed project, **When** its assignee attempts to change its state, **Then** the state is unchanged ([Assumption A-011](constraints.md#assumptions)). |
| EX-009 | US-008 | **Given** a Completed project, **When** its Project Owner attempts to rename it, **Then** its name is unchanged ([Assumption A-011](constraints.md#assumptions)). |
| EX-010 | US-008 | **Given** an Active project and its Project Owner, **When** the owner tries to rename it to an empty name, **Then** its name is unchanged and the owner is told a name is required ([Assumption A-003](constraints.md#assumptions)). |
| EX-011 | US-011 | **Given** an Active project with a task and its Project Owner, **When** the owner tries to change the task title to an empty title, **Then** the title is unchanged and the owner is told a title is required ([Assumption A-004](constraints.md#assumptions)). |
| EX-012 | US-007 | **Given** a person whose identity has not been established, **When** they attempt to view or change a project or task directly, **Then** no project or task information is disclosed and existing information is unchanged ([NFR-001](requirements.md#non-functional-requirements-and-unresolved-quality-criteria), [Assumption A-014](constraints.md#assumptions)). |
| EX-013 | US-005 | **Given** an Active project with an unassigned To do task, **When** its Project Owner changes the task to Done, **Then** it shows Done; the owner may change the state without being the assignee ([FR-011](requirements.md#functional-requirements), [Assumption A-001](constraints.md#assumptions)). |

Client decisions that may change these tests, including naming rules (OQ-003), access provisioning (OQ-001), and simultaneous edits (OQ-017), are listed in the [open questions](constraints.md#open-questions).










