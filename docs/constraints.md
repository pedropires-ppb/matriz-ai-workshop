# Matriz — Constraints and Assumptions (v0.1)

**Status:** Draft; no proposed rule, priority, or delivery commitment is client-approved. The only established needs are organizing projects (F01), managing tasks (F02), and tracking progress (F03). See [requirements](requirements.md) for the proposed behaviour and [user stories](user-stories.md) for acceptance scenarios.

## Known constraints and unresolved business constraints

| Area | What is known | What remains to be confirmed |
|---|---|---|
| Business | The application is for work teams to organize projects, manage tasks, and track work progress (F01–F03). | Which MVP outcomes, exclusions, priorities, and success measures the client approves (OQ-009, OQ-016). |
| Legal / privacy | No particular personal-data categories, internal policy, or legal obligations were specified. GDPR may be relevant, but its application and concrete obligations have not been established. | Personal data processed, authorized access, applicable GDPR duties and internal policies, including retention (OQ-011). |
| Time | No first-release deadline is known. | Deadline or desired delivery window (OQ-014). |
| Budget | No budget ceiling is known. | Budget range or limit (OQ-015). |
| External dependencies | No integration requirements were supplied; this does **not** mean integrations are unnecessary. | Whether exchange with existing applications is mandatory (OQ-012). |

## Assumptions

Every entry is **unvalidated**; risk is the impact of being wrong, not an implementation priority. A reference to an A-number anywhere in this knowledge base means **[Assumption A-xxx]**, never an established fact. Each assumption is closed by answering its related [open question](#open-questions).

| ID | Proposed assumption | Justification | Risk | Related open question | Validation status |
|---|---|---|---|---|---|
| A-001 | Two per-project roles, **Project Owner** and **Project Participant**; the owner manages the project, members, and tasks, while participants update only tasks assigned to them. A person can have different roles in different projects. | A small permission model sufficient for F01–F02. | High | OQ-002 | Pending client confirmation. |
| A-002 | Project access is limited to its members; an owner is a member of their project, and removing a member ends that person's access. | Delimits access between teams. | High | OQ-002 | Pending client confirmation. |
| A-003 | A project has a required nonempty name and two states, Active and Completed; only the Project Owner can rename or complete an Active project. | Minimum project lifecycle. | Medium | OQ-003, OQ-006 | Pending client confirmation. |
| A-004 | Every task belongs to one project, has a required nonempty title, and has at most one assignee, optionally selected from that project's members. | Allows recording unassigned work. | Medium | OQ-003, OQ-007 | Pending client confirmation. |
| A-005 | Task states are To do, In progress, and Done; permitted state changes need not follow a fixed sequence and can reopen Done tasks while the project is Active. | Minimal workflow. | Medium | OQ-004 | Pending client confirmation. |
| A-006 | Project progress consists of task counts by state and the percentage of Done tasks among all tasks; a project without tasks displays “No tasks” instead of a percentage. | Simple measurable progress indicator, not a measure of effort. | High | OQ-005 | Pending client confirmation. |
| A-007 | The Project Owner cannot complete a project with any unfinished task; a project with no tasks is eligible because it has no unfinished tasks. | Avoids declaring pending work complete; the empty-project case follows the rule but needs explicit confirmation. | High | OQ-006 | Pending client confirmation, especially for empty projects. |
| A-008 | The person authorized to create a project becomes its Project Owner and a member. | Provides an initial owner; who is authorized to create projects is still open. | High | OQ-001 | Pending client confirmation. |
| A-009 | A Project Owner can remove only a non-owner member, and not if that person has any task assigned to them in that project; changes to the owner's membership are unspecified. | Avoids leaving assigned work with someone who has lost access and avoids silently removing the project's owner. | Medium | OQ-007, OQ-008 | Pending client confirmation. |
| A-010 | A newly created task starts in To do. | Gives a testable starting state. | Low | OQ-004 | Pending client confirmation. |
| A-011 | A Completed project and its tasks remain viewable to members but cannot be modified. | Maintains a readable record of completed work. | Medium | OQ-006, OQ-008 | Pending client confirmation. |
| A-012 | Only the proposed FR-001–FR-012 are in the MVP; the candidates in [Outside the initial scope](requirements.md#outside-the-initial-scope) are excluded. | Keeps the first release focused on the three stated needs. | High | OQ-016 | Pending client approval of scope and roadmap. |
| A-013 | The initial application's user-facing language can be Portuguese. | No language was explicitly requested. | Medium | OQ-013 | Pending client confirmation. |
| A-014 | Project access requires establishing the person's identity first; the method of doing so is undecided. | Identity is necessary to apply the proposed per-person access rules. | High | OQ-001, OQ-018 | Pending client confirmation. |
| A-015 | Better coordination and visibility may be benefits of the application; these are not agreed business success measures. | The client's description covers teamwork and progress, not measurable business outcomes. | Medium | OQ-009 | Pending client confirmation. |

## Security and privacy

- **Authentication:** [Assumption A-014](#assumptions) proposes identity verification before access. The identity method, access provisioning, and who may create projects remain unresolved (OQ-001, OQ-018). See [NFR-001](requirements.md#non-functional-requirements-and-unresolved-quality-criteria).
- **Authorization:** [Assumption A-001](#assumptions) and [Assumption A-002](#assumptions) propose enforcing per-project membership and role permissions for both listings and direct attempts to view or change a project or task (OQ-019). Refused changes leave the information unchanged. See [NFR-002](requirements.md#non-functional-requirements-and-unresolved-quality-criteria) and the [exceptional cases](user-stories.md#edge-cases-and-exceptions).
- **Data protection and GDPR:** categories of personal data, access to them, retention, and applicable legal and internal obligations must be identified with the client and competent stakeholders before concrete controls or periods are specified (OQ-011). No claim of GDPR compliance is made; see [NFR-003](requirements.md#non-functional-requirements-and-unresolved-quality-criteria).

No technologies or security implementation approaches have been selected. The provisionally chosen architectural style is recorded in the [architecture definition](architecture.md).

## Open questions

This is the single register of decisions awaiting the client. No unconfirmed answer may be treated as a fixed constraint. References from earlier exercises (Q-, P-, D-numbers, backlog questions) are superseded by these IDs.

| ID | Question | Affects | Blocks |
|---|---|---|---|
| OQ-001 | Who may create projects, and how do people receive initial access? | FR-002, NFR-001, A-008, A-014 | Implementation |
| OQ-002 | Are the two roles (Project Owner, Project Participant) and their permissions correct? | FR-001–FR-011, NFR-002, A-001, A-002 | Implementation |
| OQ-003 | What naming rules apply to project names and task titles (whitespace-only, duplicates, length)? | FR-002, FR-003, FR-008, FR-009, A-003, A-004 | Implementation |
| OQ-004 | Are the task states and free transitions (including reopening Done tasks and the To do starting state) correct? | FR-008, FR-011, A-005, A-010 | Implementation |
| OQ-005 | What should progress measure, and is an overview across projects needed? | FR-012, A-006 | Implementation |
| OQ-006 | Can a project with zero tasks be completed? Can a Completed project be reopened? | FR-007, A-007, A-011 | Implementation |
| OQ-007 | Can a task be reassigned or unassigned, and what happens to a departing member's assigned tasks? | FR-005, FR-010, A-004, A-009 | Implementation |
| OQ-008 | Can project ownership be transferred or the owner removed? Does access change after completion? | FR-005, FR-007, A-009, A-011 | Implementation |
| OQ-009 | What business success measures should the first release achieve? | README goals, A-015 | Release evaluation |
| OQ-010 | What response-time, availability, concurrent-use, and accessibility targets apply? | NFR-004–NFR-007 | NFR verification |
| OQ-011 | What personal data is processed, who may access it, and which GDPR, retention, and internal obligations apply? | NFR-003 | Implementation |
| OQ-012 | Are integrations with existing applications required, and what information must be exchanged? | Scope | Architecture |
| OQ-013 | Which user-facing language(s) are required? | NFR-008, A-013 | Implementation |
| OQ-014 | Is there a deadline or desired delivery window? | Planning | Planning |
| OQ-015 | Is there a budget range or limit? | Planning | Planning |
| OQ-016 | Does the client approve the MVP content, priorities, sufficiency, and exclusions? | FR-001–FR-012, A-012 | Implementation |
| OQ-017 | How should simultaneous changes to the same project or task be resolved? | FR-003, FR-009–FR-011 | Architecture |
| OQ-018 | How will a person's identity be established? | NFR-001, A-014 | Architecture |
| OQ-019 | How will project and task permissions be enforced consistently across every way of accessing or changing work? | NFR-002 | Architecture |
| OQ-020 | Who will operate and maintain Matriz after its first release, and are there constraints on where or how it must be hosted? | Operation and maintenance, Planning | Architecture |
| OQ-021 | On which devices and in which contexts will people use Matriz (for example, a computer or a phone; at a desk or away from it)? | User interface layout and delivery | User interface design |

## Scope limitations

The [MVP scope](requirements.md#mvp-scope) and [exclusions](requirements.md#outside-the-initial-scope) are proposals under [Assumption A-012](#assumptions), not delivery commitments; approval is OQ-016.

