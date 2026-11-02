# Matriz — Implementation Plan (v0.2)

**Status:** Proposal by the AI assistant, adopted provisionally by the project team on 2026-10-29 ([ADR-002](adr/ADR-002-phase-scope-and-implementation-authorization.md)). Revised on 2026-10-30 by team decision ([ADR-007](adr/ADR-007-build-persistence-and-demonstration-data.md)): I1 now creates the whole schema and loads the demonstration data; each change is marked where it is made. Revised again on 2026-10-30 by team decision in the review of I1 ([ADR-007](adr/ADR-007-build-persistence-and-demonstration-data.md) decision 10, amended; [ADR-008](adr/ADR-008-domain-driven-design.md)): the demonstration data is a Flyway migration, and the backend is structured with domain-driven design; each change is marked “in the I1 review”. The team authorized implementation provisionally, at its own risk, while the client's approval of the MVP scope is pending ([OQ-016](constraints.md#open-questions)). If the client changes the scope, this plan changes.

Requirements, acceptance criteria, and assumptions are defined only in [requirements](requirements.md), [user stories](user-stories.md), and [constraints](constraints.md); this plan links to them. The technologies are those of [ADR-003](adr/ADR-003-technology-stack.md), and the API is [openapi.yaml](../openapi.yaml) ([ADR-004](adr/ADR-004-rest-api-single-entry-point.md)).

## Approach

- **Vertical slices.** Each increment from I2 onwards delivers its API operations, the User interface screens that use them, any further database migrations it needs, and its automated tests. I1 creates the whole schema of the [data model](database.md); later increments change it only through new migrations, and an applied migration is never edited ([ADR-007](adr/ADR-007-build-persistence-and-demonstration-data.md)). [Proposal] *Changed on 2026-10-30 by team decision ([ADR-007](adr/ADR-007-build-persistence-and-demonstration-data.md), plan revision): previously each increment from I2 onwards added the migrations it needed. Any technical data needed to end a sign-in is still added in I2, with the open points of [ADR-005](adr/ADR-005-authentication-with-jwt.md).* Within each module, an increment adds the aggregates with their JPA mapping, the repository implementations, use cases, and web layer it needs, in the layers of [ADR-008](adr/ADR-008-domain-driven-design.md). *Added on 2026-10-30 by team decision in the I1 review. Changed on 2026-10-30 by team decision (decision 9 of the conversation “I1 review: remaining decisions”): previously “the aggregates, use cases, persistence model, and web layer it needs”.*
- **Acceptance.** The acceptance criteria of an increment are the user stories (US) and exceptional cases (EX) listed for it. A story listed as partial is completed in the increment named beside it.
- **Test fixtures.** Before I6 adds members and I8 completes projects, tests that need a Project Participant or a Completed project create it directly in the database. Automated tests never use the demonstration data; they create their own. *The last sentence was added on 2026-10-30 by team decision ([ADR-007](adr/ADR-007-build-persistence-and-demonstration-data.md)).* The exception is `DemoDataTests`, which checks the demonstration data migration on databases of its own. *Added on 2026-10-30 by team decision in the I1 review ([ADR-007](adr/ADR-007-build-persistence-and-demonstration-data.md), decision 10, amended).*
- **Demonstration data.** Demonstration people, projects, tasks, assignments, and comments are fictitious and match the people and examples in the user stories, for example login name “ana” with display name “Ana Silva” ([Assumption A-016](constraints.md#assumptions)). From I1 onwards, a Flyway migration adds them once per database, only when requested (`MATRIZ_DEMO_DATA=true`); a demonstration row deleted later is not restored. *Changed on 2026-10-30 by team decision in the I1 review ([ADR-007](adr/ADR-007-build-persistence-and-demonstration-data.md), decision 10, amended): previously “They are loaded from I1 onwards, only when requested”.* Their passwords are supplied when the application starts and are never committed ([ADR-005](adr/ADR-005-authentication-with-jwt.md), [ADR-007](adr/ADR-007-build-persistence-and-demonstration-data.md)). *Changed on 2026-10-30 by team decision ([ADR-007](adr/ADR-007-build-persistence-and-demonstration-data.md), plan revision): previously “Test data”, with demonstration accounts only, seeded in I2.*
- **Order.** Increments may be reordered or run in parallel when their dependencies allow.

### Exit criteria for every increment [Proposal]

1. Every acceptance criterion listed for the increment passes as an automated API test. The main flow also passes as an end-to-end test through the User interface.
2. Every new operation is tested for the people the [permission matrix](requirements.md#permission-matrix-proposed) distinguishes:
   - a person who is not signed in gets 401 ([EX-012](user-stories.md#edge-cases-and-exceptions));
   - a non-member gets 404 ([EX-003](user-stories.md#edge-cases-and-exceptions));
   - a member without permission gets 403;
   - a change refused because the project is Completed gets 409.

   In each case, nothing changes.
3. Responses match [openapi.yaml](../openapi.yaml), and errors are Problem Details ([ADR-004](adr/ADR-004-rest-api-single-entry-point.md)).
4. The module boundary test and the layering test pass ([ADR-001](adr/ADR-001-modular-monolith.md), [ADR-008](adr/ADR-008-domain-driven-design.md)). *The layering test was added on 2026-10-30 by team decision in the I1 review.*
5. No secret, password, or real personal data is committed or logged.
6. The documentation is updated if behaviour or the contract changed. Any new assumption is registered in [constraints](constraints.md#assumptions) before it is used.
7. The project team reviews and accepts the increment.

## Increment status

*Changed on 2026-10-31 by team decision (the conversation “I1 review: last decisions”, decision 6), which accepted rules 1–7: previously the heading was “Increment status [Proposal]”. Rule 8 was accepted on 2026-10-30.*

The status of each increment is kept in one place only: the **Status** column of the [Increments](#increments) table. Its values are deliberately different from the task states (To do, In progress, Done) and the project states (Active, Completed).

| Status | Meaning |
|---|---|
| Planned | Not started. |
| Under way | Work has started. |
| Awaiting review | Exit criteria 1–6 are met; the project team has not yet reviewed the increment. |
| Accepted | The project team reviewed and accepted the increment (exit criterion 7). |
| Blocked | Work cannot continue; the cell names the cause. |

**Rules for updating the status during implementation.**

1. Whoever works on an increment updates its status in the same change (commit) that causes the transition.
2. **Planned → Under way:** only when every increment it depends on is Accepted and the decisions it needs (see [Decisions affecting the increments](#decisions-affecting-the-increments)) are taken.
3. **Under way → Awaiting review:** when exit criteria 1–6 are met. The cell links to the evidence, for example the commit or the test run.
4. **Awaiting review → Accepted:** set only by the project team, with the date, for example “Accepted 2026-11-05”. An AI assistant never sets it.
5. **Any status → Blocked:** when a dependency, an unanswered question, or a failed check stops the work. The cell states the cause, for example an OQ, an assumption, or an increment, and the earlier status, which is restored when the cause is removed.
6. **Accepted → Under way:** when a client answer, a team decision, or a defect requires changing an accepted increment. The cell states the reason.
7. Changing a status never changes requirements or acceptance criteria. If they must change, they are changed first in [requirements](requirements.md) or [user stories](user-stories.md).
8. **Awaiting review → Under way:** when the project team reviews the increment and requests changes. The cell names the requested changes and links to the decisions that record them. When the changes are done, rule 3 applies again. *Added on 2026-10-30 in the I1 review, which requested changes that rules 1–7 had no transition for. Accepted by the project team on 2026-10-30 (the conversation “I1 review: remaining decisions”, decision 5): previously tagged [Proposal].*

## Increments

| ID | Goal | Depends on | Acceptance criteria | Status |
|---|---|---|---|---|
| [I0](#i0--documentation-alignment) | Record the team's decisions in the documentation. | ADR-002 | Documentation checks | Accepted 2026-10-30 by the project team |
| [I1](#i1--walking-skeleton) | A running application with no features yet: the whole schema, demonstration data, and all quality checks. | I0 | Technical criteria | Accepted 2026-10-31 by the project team. Evidence: [I1 verification record](evidence/I1-verification.md), commit [9ecaf29](https://github.com/pedropires-ppb/matriz-ai-workshop/commit/9ecaf297dab121abb0e3ab45da8f9aca11fccbe5), and [CI run](https://github.com/pedropires-ppb/matriz-ai-workshop/actions/runs/41153638855). |
| [I2](#i2--identity) | Sign in, sign out, and see the signed-in person. | I1 | US-013–US-015; EX-012, EX-014 | Under way since 2026-11-02. I1 is Accepted, and the decisions I2 needs were taken on 2026-11-02 ([Decisions affecting the increments](#decisions-affecting-the-increments)). |
| [I3](#i3--projects-and-access-control) | Create, list, view, and rename projects. | I2 | US-001, US-007, US-008; EX-001, EX-003, EX-009, EX-010 | Planned |
| [I4](#i4--tasks-and-task-priority) | Create, view, and change tasks, including Task priority. | I3 | US-003, US-004 (partial), US-011, US-018; EX-002, EX-011, EX-013, EX-019 | Planned |
| [I5](#i5--progress-and-my-projects) | Show progress and My projects. | I4 | US-006, US-025; EX-005, EX-027 | Planned |
| [I6](#i6--members) | Add, remove, and list members. | I3 | US-002, US-009, US-026 | Planned |
| [I7](#i7--several-assignees-and-self-assignment) | Assign and unassign several members; assignees change states. | I4, I6 | US-004, US-005, US-012, US-019, US-020; EX-004, EX-007, EX-020, EX-021 | Planned |
| [I8](#i8--completion-and-simultaneous-changes) | Complete projects; refuse changes to Completed projects; test simultaneous changes. | I5, I7 | US-010; EX-006, EX-008, EX-009, EX-022 | Planned |
| [I9](#i9--comments) | Add and read task comments. | I4, I8 | US-021, US-022; EX-023, EX-024 | Planned |
| [I10](#i10--deletion) | Delete projects and tasks. | I5, I7, I8 | US-016, US-017; EX-015–EX-018 | Planned |
| [I11](#i11--search-and-filters) | Search projects and filter tasks. | I5 | US-023, US-024; EX-025, EX-026 | Planned |
| [I12](#i12--release-verification) | Verify the whole proposed MVP. | I1–I11 | US-001–US-026; EX-001–EX-027; NFR-001, NFR-002, NFR-008 | Planned |

### I0 — Documentation alignment

- **Goal:** Record the team's decisions of 2026-10-29 without writing code.
- **Files:**
  - [constraints](constraints.md), [requirements](requirements.md), [user stories](user-stories.md), the [data model](database.md), and the [architecture definition](architecture.md);
  - the [ADRs](adr/README.md) and this plan;
  - [openapi.yaml](../openapi.yaml), version 0.2.0-draft;
  - [README](../README.md) and the [Project dashboard wireframe notes](ui/project-dashboard.md);
  - [AGENTS.md](../AGENTS.md): only the changes the team approved, which are the `ADR-xxx` ID row (question 18), the MVP range in the scope rules (question 2), and the update of the current phase (approved on 2026-10-30).
- **Depends on:** [ADR-002](adr/ADR-002-phase-scope-and-implementation-authorization.md).
- **Acceptance:**
  - Every new FR cites its origin and assumptions, and every US links to an FR.
  - IDs are unique and nothing is renumbered.
  - Every relative link and anchor resolves.
  - openapi.yaml is a valid OpenAPI document.
- **Verification:** an automated link, anchor, and ID check, an OpenAPI lint, and a review by the project team.

### I1 — Walking skeleton

*Revised on 2026-10-30 by team decision ([ADR-007](adr/ADR-007-build-persistence-and-demonstration-data.md), plan revision). Previously the goal was “a running, empty application”; the first migration was not the whole schema; the database connection, the environment variables, the local database, and password hashing were not listed; the demonstration accounts were seeded in I2; and the last three acceptance criteria below did not exist.*

*Revised again on 2026-10-30 by team decision in the I1 review ([ADR-007](adr/ADR-007-build-persistence-and-demonstration-data.md) decision 10, amended; [ADR-008](adr/ADR-008-domain-driven-design.md)): the demonstration data is added by a Flyway migration instead of start-up loaders, and each module gets the layers and the layering check of domain-driven design. Each change is marked below.*

- **Goal:** A running application with no features yet, in which every later increment can be built, tested, and verified. It already holds the whole schema and, when requested, the demonstration data.
- **Components:**
  - the build, with the tool and versions chosen and pinned ([ADR-003](adr/ADR-003-technology-stack.md), [ADR-007](adr/ADR-007-build-persistence-and-demonstration-data.md));
  - empty Identity, Projects, Tasks, Progress, and Access control modules, declared for Spring Modulith. Each has its API package and the empty layers `domain`, `application`, `infrastructure`, and `web` ([ADR-008](adr/ADR-008-domain-driven-design.md)); *layers added in the I1 review;*
  - the first database migration, which creates the whole schema of the [data model](database.md): Person, Project, Membership, Task, Assignment, and Comment, with the constraints of [ADR-007](adr/ADR-007-build-persistence-and-demonstration-data.md); *moved forward from I2–I9;*
  - the database connection, configured only through environment variables, with an example file of fictitious values and a local PostgreSQL; *new;*
  - the demonstration data, as a Flyway migration applied once per database and only when requested, with passwords supplied at start-up and stored as hashes ([ADR-005](adr/ADR-005-authentication-with-jwt.md), [ADR-007](adr/ADR-007-build-persistence-and-demonstration-data.md) decision 10); *moved from I2; changed in the I1 review: previously “loaded only when requested”, by start-up loaders;*
  - the layering check, `LayeringTests`, with ArchUnit ([ADR-008](adr/ADR-008-domain-driven-design.md)); *added in the I1 review;*
  - error handling that returns Problem Details;
  - a health check that reports whether the database is up ([ADR-007](adr/ADR-007-build-persistence-and-demonstration-data.md));
  - the User interface shell, served from the same origin as the API;
  - continuous integration running every test.
- **Depends on:** I0; [ADR-001](adr/ADR-001-modular-monolith.md), [ADR-003](adr/ADR-003-technology-stack.md), [ADR-004](adr/ADR-004-rest-api-single-entry-point.md), [ADR-005](adr/ADR-005-authentication-with-jwt.md), [ADR-007](adr/ADR-007-build-persistence-and-demonstration-data.md), [ADR-008](adr/ADR-008-domain-driven-design.md) (*added in the I1 review*).
- **Acceptance (technical, new):** [Proposal]
  - The application builds and starts from a clean checkout with the documented commands.
  - A request to an unknown API path returns a Problem Details response.
  - Request content that is not in the operation's media type returns `415` as a Problem Details response ([ADR-004](adr/ADR-004-rest-api-single-entry-point.md)).
  - The module boundary test fails when a module uses another module's internals.
  - The layering check fails when a layer inside a module uses a layer it must not ([ADR-008](adr/ADR-008-domain-driven-design.md)). *Added on 2026-10-30 in the I1 review.*
  - Started against an empty database, the application applies the migrations, and the health check reports the database as up. *Added on 2026-10-30.*
  - Restarting the application deletes, overwrites, and duplicates nothing: neither the demonstration data nor data added by hand. *Added on 2026-10-30.*
  - A demonstration row deleted by hand stays deleted after a restart. Started against an empty database without the demonstration data, and in the test databases, the application has none of it. *Added on 2026-10-30 in the I1 review.*
  - No secret, password, or real personal data is in the repository, and the real environment file is ignored by git. *Added on 2026-10-30.*
- **Verification:**
  - automated:
    - the boundary test, and the layering test (*added in the I1 review*);
    - integration tests against PostgreSQL in a container: the migration on an empty database, the database constraints, the health check, a missing environment variable stopping start-up, and the demonstration data migration applied once, with a restart that changes nothing, and refused when the setting does not match the database or a row conflicts; the test databases have no demonstration data. *Changed in the I1 review: previously “the demonstration data loaded twice and after a restart without duplicates”;*
    - a check that schema migrations have whole version numbers and demonstration data migrations are sub-versions (*added in the I1 review*);
    - a check that responses match openapi.yaml;
    - an end-to-end test that opens the User interface shell;
  - manual:
    - introduce a boundary violation and confirm the build fails, then revert it;
    - introduce a layering violation and confirm the build fails, then revert it (*added in the I1 review*);
    - start from an empty database with the demonstration data, add a row by hand, restart, and confirm that nothing was lost or duplicated;
    - delete a demonstration row by hand, restart, and confirm that it stays deleted; start from an empty database without the demonstration data and confirm that it has none (*added in the I1 review*);
    - search the repository for secrets and real personal data;
    - build from a clean checkout with the README's commands, and confirm that continuous integration passes after the push. *Changed on 2026-10-30 by team decision (the conversation “I1 review: remaining decisions”, decision 8): previously “a second person follows the README instructions”. The team is one person.*

### I2 — Identity

- **Goal:** People sign in with prepared accounts, see who they are signed in as, and sign out. Every other operation requires a signed-in person.
- **Components:**
  - Identity: Person, with login name, display name, and password hash ([Assumption A-017](constraints.md#assumptions)); its table and the demonstration accounts exist from I1 ([ADR-007](adr/ADR-007-build-persistence-and-demonstration-data.md)); *changed on 2026-10-30 by team decision (plan revision): previously this list also had “seeded demonstration accounts”, which moved to I1;*
  - token issuing, verification, and revocation on sign-out ([ADR-005](adr/ADR-005-authentication-with-jwt.md));
  - operations `signIn`, `signOut`, and `getCurrentPerson`;
  - User interface: the sign-in screen, and a header with the display name and a sign-out control.
  - the Person aggregate in Identity's layers, mapped with JPA annotations, with its repository; the mapping conventions are decided here ([ADR-008](adr/ADR-008-domain-driven-design.md)); *added on 2026-10-30 by team decision in the I1 review; changed on 2026-10-30 by team decision (decision 9 of the conversation “I1 review: remaining decisions”): previously “with its persistence model and mappers; the mapper conventions are decided here”.*
  - the table of ended access tokens (migration `V2`) and the check that login names are in lower case (migration `V3`) ([ADR-005](adr/ADR-005-authentication-with-jwt.md#open-points), [ADR-007](adr/ADR-007-build-persistence-and-demonstration-data.md#open-points)); *added on 2026-11-02 by team decision (the conversation “Lab Worksheet 03 — access to the application”, questions 3 and 10);*
  - the start-up checks of the signing key and of the demonstration passwords ([ADR-005](adr/ADR-005-authentication-with-jwt.md#open-points)); *added on 2026-11-02 by the same decision (questions 6 and 8);*
  - what the User interface shows when a session ends ([ADR-009](adr/ADR-009-identity-from-the-token.md), decision 1); *added on 2026-11-02 by the same decision (question 14);*
  - three guards for the operations still to come ([ADR-009](adr/ADR-009-identity-from-the-token.md), decision 3):
    - a test, driven by openapi.yaml, that every operation except `signIn` requires a token;
    - a test of the properties of every request schema;
    - `SignedInPerson`, the only way for a controller to learn the acting person, with an ArchUnit test that only Identity uses Spring Security.

    *Added on 2026-11-02 by the same decision (questions 15 and 16).*
- **Depends on:** I1. Before starting, the team decides the open points of [ADR-005](adr/ADR-005-authentication-with-jwt.md): token lifetime, how sign-out is enforced, and where the token is kept ([OQ-027](constraints.md#open-questions)). *Decided on 2026-11-02 by the project team (Lab Worksheet 03, questions 2–9 and 17), together with the open points of [ADR-004](adr/ADR-004-rest-api-single-entry-point.md#open-points), [ADR-007](adr/ADR-007-build-persistence-and-demonstration-data.md#open-points), and [ADR-008](adr/ADR-008-domain-driven-design.md#open-points) that were due in I2 (questions 10–13). The scope stays I2 alone (question 1), so this plan is not revised.*
- **Acceptance:**
  - [US-013](user-stories.md#us-013--sign-in), [US-014](user-stories.md#us-014--sign-out), and [US-015](user-stories.md#us-015--see-who-is-signed-in). Showing the person's projects (US-013) and My projects (US-015) is completed in I5.
  - [EX-012](user-stories.md#edge-cases-and-exceptions) and [EX-014](user-stories.md#edge-cases-and-exceptions).
- **Verification:**
  - API tests:
    - a wrong password and an unknown login name give identical responses;
    - a token used after sign-out, an expired token, and an altered token all get 401 with `WWW-Authenticate: Bearer`;
    - no response contains a password or password hash.
    - *added on 2026-11-02 by team decision (Lab Worksheet 03, questions 12–16 and the confirmed summary):*
      - valid credentials;
      - a request without a token;
      - `GET /api/me` returns the person in the token, whatever its query parameters;
      - no response or log contains a token, either;
      - an owner or author field sent by the client gets 422;
      - the place of `405`;
      - the three guards of [ADR-009](adr/ADR-009-identity-from-the-token.md);
      - the Person repository test ([ADR-008](adr/ADR-008-domain-driven-design.md#open-points));
      - two demonstration people with different access, also with direct requests through curl.
  - End-to-end: sign in, see the display name, sign out, and confirm that a protected screen asks to sign in again. *Added on 2026-11-02 by the same decision:* a person who has not signed in is sent to sign in, and a session that is no longer valid shows the message of [ADR-009](adr/ADR-009-identity-from-the-token.md), decision 1.

### I3 — Projects and Access control

- **Goal:** A signed-in person creates projects and sees, opens, and renames the projects of which they are a member. Non-members learn nothing about other projects.
- **Components:**
  - Projects: Project and its members with their roles;
  - Access control: membership and role checks;
  - the protection of rules spanning modules ([ADR-006](adr/ADR-006-simultaneous-changes.md); the proposed project lock is confirmed here);
  - operations `createProject`, `listProjects` (without search), `getProject`, and `renameProject`;
  - User interface: a basic project list, project creation, and the Project dashboard header with renaming.
  - the Project aggregate with the Project Owner's Membership, and Access control's policy, in the layers of [ADR-008](adr/ADR-008-domain-driven-design.md); the mapping of Memberships and the layers that Access control needs are decided here; *added on 2026-10-30 by team decision in the I1 review.*
- **Depends on:** I2; [Assumption A-018](constraints.md#assumptions).
- **Acceptance:**
  - [US-001](user-stories.md#us-001--create-project); its AC2 is checked through the member list in I6.
  - [US-007](user-stories.md#us-007--view-accessible-projects); its AC2 uses a Completed project from a fixture.
  - [US-008](user-stories.md#us-008--rename-project).
  - [EX-001](user-stories.md#edge-cases-and-exceptions), [EX-003](user-stories.md#edge-cases-and-exceptions), [EX-009](user-stories.md#edge-cases-and-exceptions) (fixture), and [EX-010](user-stories.md#edge-cases-and-exceptions).
- **Verification:**
  - API tests: the permission-matrix rows for creating, viewing, and renaming a project, for each kind of person.
  - End-to-end: create a project and rename it.

### I4 — Tasks and Task priority

- **Goal:** The Project Owner records tasks with a title and a Task priority, and changes their state. Every member sees the tasks.
- **Components:**
  - Tasks: Task, with title, state, and Task priority;
  - operations `createTask`, `listTasks` (without filters), `getTask`, `updateTask` (title and Task priority), and `changeTaskState` (by the Project Owner for now; assignees follow in I7);
  - User interface: the task list on the Project dashboard, task creation, and a task detail showing title, Task priority, and state.
  - the Task aggregate in the layers of [ADR-008](adr/ADR-008-domain-driven-design.md); whether and how the Task class maps the technical `creation_order` is decided here; *added on 2026-10-30 by team decision in the I1 review; the part on `creation_order` added on 2026-10-30 with decision 9 of the conversation “I1 review: remaining decisions”.*
- **Depends on:** I3; [Assumption A-021](constraints.md#assumptions).
- **Acceptance:**
  - [US-003](user-stories.md#us-003--create-task), [US-011](user-stories.md#us-011--rename-task), and [US-018](user-stories.md#us-018--set-task-priority).
  - [US-004](user-stories.md#us-004--view-tasks), partially: without assignees, which are completed in I7.
  - [EX-002](user-stories.md#edge-cases-and-exceptions), [EX-011](user-stories.md#edge-cases-and-exceptions), [EX-013](user-stories.md#edge-cases-and-exceptions), and [EX-019](user-stories.md#edge-cases-and-exceptions) (with a Project Participant from a fixture).
- **Verification:**
  - Unit tests: state changes in any order, including reopening a Done task.
  - API tests: the permission-matrix rows for tasks.
  - End-to-end: create a task with Task priority High and move it to Done.

### I5 — Progress and My projects

- **Goal:** Members see a project's progress. After signing in, a person sees My projects.
- **Components:**
  - Progress: counts by state and the rounded percentage Done ([Assumption A-026](constraints.md#assumptions));
  - progress and per-project counts in `getProject` and `listProjects`;
  - User interface: My projects as the first screen after signing in, and the progress area of the Project dashboard.
  - Progress as a read model, with no aggregate; the layers it needs are decided here ([ADR-008](adr/ADR-008-domain-driven-design.md)); *added on 2026-10-30 by team decision in the I1 review.*
- **Depends on:** I4; [Assumption A-025](constraints.md#assumptions). Before starting, the team decides whether to keep the rounding of [Assumption A-026](constraints.md#assumptions) despite its edge case.
- **Acceptance:**
  - [US-006](user-stories.md#us-006--view-progress) and [US-025](user-stories.md#us-025--view-my-projects).
  - The parts of US-013 and US-015 left from I2.
  - [EX-005](user-stories.md#edge-cases-and-exceptions) and [EX-027](user-stories.md#edge-cases-and-exceptions).
- **Verification:**
  - Unit tests: rounding (1 of 3 is 33%, 2 of 3 is 67%, 1 of 8 is 13%) and the “No tasks” case.
  - API tests: counts per project, with no totals across projects.
  - End-to-end: sign in, see My projects, and open a Project dashboard.

### I6 — Members

- **Goal:** The Project Owner adds and removes participants. Every member sees who belongs to the project.
- **Components:**
  - operations `listProjectMembers`, `listEligibleParticipants` (Project Owner only; a proposal, [Assumption A-030](constraints.md#assumptions)), `addParticipant`, and `removeParticipant`;
  - User interface: the members area of the Project dashboard, with add and remove controls for the Project Owner.
  - changes of Membership in the Project aggregate, and the task-facts interface that Projects defines and Tasks implements ([ADR-008](adr/ADR-008-domain-driven-design.md), decision 3); *added on 2026-10-30 by team decision in the I1 review.*
- **Depends on:** I3; [Assumption A-027](constraints.md#assumptions). Before starting, the team decides how a Project Owner finds the person to add ([Assumption A-030](constraints.md#assumptions); [OQ-011](constraints.md#open-questions)).
- **Acceptance:**
  - [US-002](user-stories.md#us-002--add-participant), [US-009](user-stories.md#us-009--remove-participant), and [US-026](user-stories.md#us-026--view-members).
  - AC2 of US-001.
- **Verification:**
  - API tests:
    - a removed participant immediately loses access (404);
    - the Project Owner cannot be removed;
    - Project Participants cannot add or remove anyone.
  - End-to-end: add a participant, then sign in as that participant and see the project.

### I7 — Several assignees and self-assignment

- **Goal:** Tasks have zero or more assignees. The Project Owner assigns and unassigns any member. A Project Participant assigns or unassigns only themselves. Any assignee changes the task's state.
- **Components:**
  - Tasks: assignments ([data model](database.md#entities));
  - operations `assignTask` and `unassignTask`, and `changeTaskState` for assignees;
  - the removal check of `removeParticipant` now counts assignments;
  - User interface: assignees on the task list and detail, assign and unassign controls, and “Assign me” and “Unassign me” for Project Participants.
  - Assignments inside the Task aggregate ([ADR-008](adr/ADR-008-domain-driven-design.md)); *added on 2026-10-30 by team decision in the I1 review.*
- **Depends on:** I4, I6; [Assumption A-019](constraints.md#assumptions), [Assumption A-020](constraints.md#assumptions). Before starting, the team decides the order in which `assignTask` checks a request ([ADR-004](adr/ADR-004-rest-api-single-entry-point.md)).
- **Acceptance:**
  - [US-004](user-stories.md#us-004--view-tasks) (completed), [US-005](user-stories.md#us-005--change-task-state), [US-012](user-stories.md#us-012--assign-task), [US-019](user-stories.md#us-019--unassign-a-member), and [US-020](user-stories.md#us-020--assign-or-unassign-myself).
  - [EX-004](user-stories.md#edge-cases-and-exceptions), [EX-007](user-stories.md#edge-cases-and-exceptions), [EX-020](user-stories.md#edge-cases-and-exceptions), and [EX-021](user-stories.md#edge-cases-and-exceptions).
- **Verification:**
  - Database tests: a person cannot be assigned twice to the same task, and an assignee must be a member of the task's project.
  - API tests:
    - every assignment row of the permission matrix;
    - a repeated assignment succeeds and changes nothing, and `assignTask` follows its own status order ([openapi.yaml](../openapi.yaml); [ADR-004](adr/ADR-004-rest-api-single-entry-point.md)).
  - End-to-end: a Project Participant assigns themselves and changes the state.

### I8 — Completion and simultaneous changes

- **Goal:** The Project Owner completes a project with no unfinished tasks. A Completed project refuses every change except comments. Simultaneous changes never break a rule.
- **Components:**
  - operation `completeProject`;
  - a check, shared by every modifying operation, that refuses changes to Completed projects;
  - concurrency tests ([ADR-006](adr/ADR-006-simultaneous-changes.md));
  - User interface: the completion control, and read-only display of Completed projects.
  - completing a project, and the check that refuses changes in a Completed project, in application services, as [ADR-008](adr/ADR-008-domain-driven-design.md) decision 3 describes; *added on 2026-10-30 by team decision in the I1 review.*
- **Depends on:** I5, I7.
- **Acceptance:**
  - [US-010](user-stories.md#us-010--complete-project).
  - [EX-006](user-stories.md#edge-cases-and-exceptions), [EX-008](user-stories.md#edge-cases-and-exceptions), [EX-009](user-stories.md#edge-cases-and-exceptions) (now without a fixture), and [EX-022](user-stories.md#edge-cases-and-exceptions).
- **Verification:**
  - A parameterized API test: every modifying operation except `addTaskComment` returns 409 for a Completed project and changes nothing.
  - Concurrency tests run these pairs simultaneously and check that the rules hold:
    - completing a project while reopening one of its tasks;
    - completing a project while creating a task;
    - removing a participant while that participant assigns themselves.

### I9 — Comments

- **Goal:** Members add and read task comments, also in Completed projects.
- **Components:**
  - Comments, placed in Tasks (a proposal, [architecture definition](architecture.md#effect-of-the-2026-10-29-decisions));
  - operations `listTaskComments` and `addTaskComment`;
  - User interface: comments on the task detail.
  - the Comment aggregate in Tasks ([ADR-008](adr/ADR-008-domain-driven-design.md)); *added on 2026-10-30 by team decision in the I1 review.*
- **Depends on:** I4, I8; [Assumption A-023](constraints.md#assumptions).
- **Acceptance:**
  - [US-021](user-stories.md#us-021--add-comment) and [US-022](user-stories.md#us-022--read-comments).
  - [EX-023](user-stories.md#edge-cases-and-exceptions) and [EX-024](user-stories.md#edge-cases-and-exceptions).
- **Verification:**
  - API tests:
    - comments are listed oldest first, with the author's display name and the date and time;
    - no operation edits or deletes a comment;
    - non-members get 404.
  - End-to-end: add a comment and see it on the task.

### I10 — Deletion

- **Goal:** The Project Owner deletes Active projects and their tasks after confirming. Deleted items are hidden but kept.
- **Components:**
  - operations `deleteProject` and `deleteTask`;
  - every query excludes deleted items, including progress and the unfinished-task check;
  - deleting a task removes its assignments ([Assumption A-022](constraints.md#assumptions));
  - User interface: delete controls with a confirmation step.
  - deletion in the Project and Task aggregates; a deleted task's Assignments are removed inside the Task aggregate ([ADR-008](adr/ADR-008-domain-driven-design.md), decision 2); *added on 2026-10-30 by team decision in the I1 review.*
- **Depends on:** I5, I7, I8. Before starting, the team confirms the proposed details of [Assumption A-022](constraints.md#assumptions) ([OQ-023](constraints.md#open-questions)).
- **Acceptance:**
  - [US-016](user-stories.md#us-016--delete-project) and [US-017](user-stories.md#us-017--delete-task).
  - [EX-015](user-stories.md#edge-cases-and-exceptions), [EX-016](user-stories.md#edge-cases-and-exceptions), [EX-017](user-stories.md#edge-cases-and-exceptions), and [EX-018](user-stories.md#edge-cases-and-exceptions).
- **Verification:**
  - API tests: deleted projects and tasks return 404 to everyone and disappear from every list and count.
  - Database check: the deleted rows are still stored.
  - End-to-end: cancelling the confirmation changes nothing.

### I11 — Search and filters

- **Goal:** People search their projects by name, and members filter a project's tasks by state and Task priority.
- **Components:**
  - the `name` parameter of `listProjects`;
  - the `state` and `priority` parameters of `listTasks`;
  - User interface: a search box on My projects and filter controls on the Project dashboard.
  - the search and filter queries, in the layers of [ADR-008](adr/ADR-008-domain-driven-design.md); *added on 2026-10-30 by team decision in the I1 review.*
- **Depends on:** I5; [Assumption A-024](constraints.md#assumptions).
- **Acceptance:**
  - [US-023](user-stories.md#us-023--search-projects) and [US-024](user-stories.md#us-024--filter-tasks).
  - [EX-025](user-stories.md#edge-cases-and-exceptions) and [EX-026](user-stories.md#edge-cases-and-exceptions).
- **Verification:**
  - API tests:
    - matching ignores letter case;
    - several values of one filter combine;
    - a search never lists a project of which the person is not a member;
    - filters never change progress;
    - an invalid filter value returns 400.
  - End-to-end: search, filter, and clear the filters.

### I12 — Release verification

- **Goal:** Verify the whole proposed MVP before presenting it to the client.
- **Depends on:** I1–I11.
- **Acceptance:**
  - every acceptance criterion of US-001–US-026 and every exceptional case EX-001–EX-027;
  - [NFR-001, NFR-002, and NFR-008](requirements.md#non-functional-requirements-and-unresolved-quality-criteria), verified as described there;
  - the [MVP release criteria](requirements.md#mvp-scope).

  NFR-003–NFR-007 cannot be verified until their criteria are known ([OQ-010](constraints.md#open-questions), [OQ-011](constraints.md#open-questions)).
- **Verification:**
  - the full automated suite, including the complete permission matrix, the contract checks, and the module boundary and layering tests; *the layering tests were added on 2026-10-30 by team decision in the I1 review ([ADR-008](adr/ADR-008-domain-driven-design.md));*
  - an end-to-end walkthrough of the user stories in English;
  - a check that no secret or real personal data is in the repository;
  - the README instructions followed on a clean machine;
  - an updated list of open questions and assumptions for the client.
- **Result:** a demonstrable release candidate. It becomes the MVP only when the client approves the scope ([OQ-016](constraints.md#open-questions)).

## Decisions affecting the increments

Interim answers are the team's ([interim team answers](constraints.md#interim-team-answers-2026-10-29)); the client's answers prevail.

| Decision | Increments | Status |
|---|---|---|
| Current phase in [AGENTS.md](../AGENTS.md#current-phase-and-next-steps) | I1–I12 | Approved by the team and applied on 2026-10-30. AGENTS.md now records the provisional authorization, so it no longer blocks I1. |
| Client approval of the MVP scope ([OQ-016](constraints.md#open-questions)) | All | Open. Implementation is provisional ([ADR-002](adr/ADR-002-phase-scope-and-implementation-authorization.md)), and the release depends on it (I12). |
| User-facing language ([OQ-013](constraints.md#open-questions)) | I1–I12 | Interim: English ([Assumption A-028](constraints.md#assumptions)). [Proposal] Keep all user-facing text in one place, so that another language can be added. |
| Simultaneous changes ([OQ-017](constraints.md#open-questions)) | I3, I7, I8, I10 | Interim: [Assumption A-029](constraints.md#assumptions), [ADR-006](adr/ADR-006-simultaneous-changes.md). The locking mechanism is confirmed in I3. |
| Sign-in and session policy ([OQ-027](constraints.md#open-questions)) | I2 | Decided by the project team on 2026-11-02 (the conversation “Lab Worksheet 03 — access to the application”, questions 2–10 and 17). Recorded in [ADR-005](adr/ADR-005-authentication-with-jwt.md#open-points) and [ADR-007](adr/ADR-007-build-persistence-and-demonstration-data.md#open-points), and in the [interim answers](constraints.md#interim-team-answers-2026-10-29). It no longer blocks I2, and the client has not validated it. *Changed on 2026-11-02: previously “Open points of [ADR-005](adr/ADR-005-authentication-with-jwt.md); the team decides them before I2.”* |
| Naming rules and text lengths ([OQ-003](constraints.md#open-questions), [OQ-024](constraints.md#open-questions)) | I3, I4, I9 | Open. [Proposal] Check only that names, titles, and comments are nonempty, and set a technical maximum length agreed by the team in I3. *Added on 2026-10-30 by team decision ([ADR-007](adr/ADR-007-build-persistence-and-demonstration-data.md)): the I1 schema checks only that text is nonempty; the maximum agreed in I3 is added by a new migration.* |
| Personal data ([OQ-011](constraints.md#open-questions)) | All | Interim: fictitious data only ([Assumption A-016](constraints.md#assumptions)). Blocks any use with real people. |
| Status for content not in the operation's media type ([ADR-004](adr/ADR-004-rest-api-single-entry-point.md)) | I1 | Decided by the project team on 2026-10-30: `415`, replacing the draft's `400`. Recorded in ADR-004 and [openapi.yaml](../openapi.yaml); it no longer blocks I1. Its place in the status order, just before `400`, is a proposal. |
| Rounding of the percentage Done ([Assumption A-026](constraints.md#assumptions); [OQ-005](constraints.md#open-questions)) | I5 | Open; blocks I5. The team decided rounding halves up; whether to keep it, given that 199 of 200 tasks Done shows 100%, is not decided. The team is asked before I5 starts. |
| Finding the person to add as a participant ([Assumption A-030](constraints.md#assumptions); [OQ-001](constraints.md#open-questions), [OQ-011](constraints.md#open-questions)) | I6 | Open; blocks I6. A-030 is a proposal, not decided by the team. High risk: any signed-in person can list the display names of everyone known to Matriz. The team is asked before I6 starts. |
| Order of checks in `assignTask` ([ADR-004](adr/ADR-004-rest-api-single-entry-point.md); [openapi.yaml](../openapi.yaml)) | I7 | Open; blocks I7. The 0.2.0-draft contract checks `403` before `422` for a person who is not a member, unlike the usual order [Proposal], so a Project Participant naming another person always receives `403`. The team is asked before I7 starts. |
| Deletion details ([OQ-023](constraints.md#open-questions)) | I10 | The proposed details of [Assumption A-022](constraints.md#assumptions) need the team's confirmation before I10. |
| Self-assignment ([OQ-025](constraints.md#open-questions)) | I7 | Interim: [Assumption A-020](constraints.md#assumptions). High risk: any Project Participant can change any task's state. Review with the client. |
| Task priority, search and filter matching, progress and My projects ([OQ-022](constraints.md#open-questions), [OQ-026](constraints.md#open-questions), [OQ-005](constraints.md#open-questions)) | I4, I5, I11 | Interim: [Assumption A-021](constraints.md#assumptions), [Assumption A-024](constraints.md#assumptions), [Assumption A-025](constraints.md#assumptions), [Assumption A-026](constraints.md#assumptions). |
| Build tool ([ADR-003](adr/ADR-003-technology-stack.md#open-points)) | I1 | Decided by the project team on 2026-10-30: Maven. Recorded in ADR-003; it no longer blocks I1. |
| Continuous integration service ([ADR-003](adr/ADR-003-technology-stack.md#open-points)) | I1 | Decided by the project team on 2026-10-30: GitHub Actions. Recorded in ADR-003 and [ADR-007](adr/ADR-007-build-persistence-and-demonstration-data.md); it no longer blocks I1. *Changed on 2026-10-30: previously open and blocking the continuous integration part of I1.* |
| Devices and hosting ([OQ-021](constraints.md#open-questions), [OQ-020](constraints.md#open-questions)) | I1, I12 | Open. [Proposal] Develop for desktop browsers and run locally only until the client answers. *Added on 2026-10-30 by team decision ([ADR-007](adr/ADR-007-build-persistence-and-demonstration-data.md)): end-to-end tests use Chromium only, provisionally, until OQ-021 is answered.* |
| Versions, packaging, local database, environment variables, physical schema, demonstration data, and packages ([ADR-007](adr/ADR-007-build-persistence-and-demonstration-data.md)) | I1 | Decided by the project team on 2026-10-30; recorded in ADR-007, and the open points of ADR-003 in ADR-003. They no longer block I1. |
| Domain-driven design ([ADR-008](adr/ADR-008-domain-driven-design.md)); demonstration data through Flyway ([ADR-007](adr/ADR-007-build-persistence-and-demonstration-data.md), decision 10, amended) | I1–I11 | Decided by the project team on 2026-10-30 in the I1 review. Due later ([ADR-008](adr/ADR-008-domain-driven-design.md#open-points)): the mapping conventions (I2); the mapping of Memberships and Assignments, the layers of Access control, and the project lock (I3); the mapping of Task's `creation_order` (I4); the layers of Progress (I5). *Added on 2026-10-30 in the I1 review. Changed on 2026-10-30 by team decision (decision 9 of the conversation “I1 review: remaining decisions”): previously “the mapper conventions (I2)”, and without `creation_order`.* *The mapping conventions were decided on 2026-11-02 by the project team (Lab Worksheet 03, question 13; [ADR-008](adr/ADR-008-domain-driven-design.md#open-points)).* |
| Remaining decisions of the I1 review | I1–I11 | Decided by the project team on 2026-10-30 (the conversation “I1 review: remaining decisions”): (1) the rule for amending ADRs ([ADR index](adr/README.md)); (2) a code-structure rule and a “Next:” that follows this plan's Status column ([AGENTS.md](../AGENTS.md)); (3) `405` with Problem Details and `Allow` for unsupported methods ([ADR-004](adr/ADR-004-rest-api-single-entry-point.md), decision 9; [openapi.yaml](../openapi.yaml)); (4) Mockito loaded as a Java agent by Surefire ([ADR-007](adr/ADR-007-build-persistence-and-demonstration-data.md), decision 2); (5) [rule 8](#increment-status) and ADR-007's implementation details accepted; (6) the password hashes that PostgreSQL may log locally, an accepted risk ([ADR-007](adr/ADR-007-build-persistence-and-demonstration-data.md)); (7) the item on the IDE's JDK (`.idea/misc.xml`) dropped ([ADR-003](adr/ADR-003-technology-stack.md)); (8) the I1 verification without a second person ([I1](#i1--walking-skeleton); [verification record](evidence/I1-verification.md)); (9) JPA annotations on the domain classes ([ADR-008](adr/ADR-008-domain-driven-design.md), decision 4). Due later: the place of `405` in the status order (I2, [ADR-004](adr/ADR-004-rest-api-single-entry-point.md#open-points)) [Proposal]. *Added on 2026-10-30.* *The place of `405` was decided on 2026-11-02 by the project team: just after `401` (Lab Worksheet 03, question 12).* |
| Last decisions of the I1 review | I1–I4 | Decided by the project team on 2026-10-31 (the conversation “I1 review: last decisions”): (1) the JVM's line about class data sharing is left in the test runs as a known, harmless line ([verification record](evidence/I1-verification.md#101-mockito-loaded-as-a-java-agent)); (2) the domain may use only Jakarta Persistence's annotations and enumerations ([ADR-008](adr/ADR-008-domain-driven-design.md#5-layering-check), decision 5, rule 1), while the mapping conventions (I2), the mapping of Task's `creation_order` (I4), and the place of `405` in the status order (I2) stay open; (3) resolving an open point or adding a cross-reference may be done in place, flagged ([ADR index](adr/README.md)); (4) no change for `.idea/`: the team set JDK 25 in its local IDE; (5) the sentence on superseded ADRs is kept ([ADR index](adr/README.md)); (6) rules 1–7 of [Increment status](#increment-status) accepted. These decisions changed documents only, and the team kept I1 Awaiting review. *Added on 2026-10-31.* *The two points due in I2 were decided on 2026-11-02 (Lab Worksheet 03, questions 12 and 13).* |
| Access to the application (the conversation “Lab Worksheet 03 — access to the application”) | I2–I11 | Decided by the project team on 2026-11-02:<br>– (1) the scope is I2 alone. The project's owner and Access control stay in I3, and the comment's author in I9, so the plan is not revised;<br>– (2)–(9) and (17) the open points of [ADR-005](adr/ADR-005-authentication-with-jwt.md#open-points);<br>– (10) login names and (11) the health check ([ADR-007](adr/ADR-007-build-persistence-and-demonstration-data.md#open-points));<br>– (12) the place of `405` ([ADR-004](adr/ADR-004-rest-api-single-entry-point.md));<br>– (13) the mapping conventions ([ADR-008](adr/ADR-008-domain-driven-design.md#open-points));<br>– (14)–(16) ended sessions in the User interface, the identity taken only from the token, and three guards for the operations still to come ([ADR-009](adr/ADR-009-identity-from-the-token.md)).<br>*Added on 2026-11-02.* |
| Quality targets ([OQ-010](constraints.md#open-questions)) | I12 | Open. NFR-004–NFR-007 are not verified. |
| Task states, completing a project with no tasks, ownership changes, integrations ([OQ-004](constraints.md#open-questions), [OQ-006](constraints.md#open-questions), [OQ-008](constraints.md#open-questions), [OQ-012](constraints.md#open-questions)) | — | Open, but not blocking: the current proposals are implemented. |
