# Matriz

**Status: Implemented proposed-MVP delivery candidate; I12 Awaiting review, not Accepted.** Requirements v0.2, scope and acceptance criteria remain **unapproved by the client** ([OQ-016](docs/constraints.md#open-questions)). Only organizing projects, managing tasks and tracking progress (F01–F03) are confirmed client needs. Implementation is at the team's risk under [ADR-002](docs/adr/ADR-002-phase-scope-and-implementation-authorization.md). *Updated on 2026-11-12 by the project team's delivery decision 13: previously “Requirements specification — v0.2 (draft for client validation)”; no client approval is implied.*

## Problem and goals

Work teams need one application in which to organize projects, manage tasks, and see how work is progressing. The proposed first-release goal is to support this basic project-to-task workflow and make per-project progress visible. Improved coordination and visibility are possible benefits, not validated business success measures ([Assumption A-015](docs/constraints.md#assumptions)).

## Proposed MVP at a glance

All features below are proposed Must priorities, subject to client approval; their rules rely on the [assumption register](docs/constraints.md#assumptions). Every feature maps to at least one [MVP user story](docs/user-stories.md#stories). Features whose origin is “Team decision” were added by the project team on 2026-10-29, not requested by the client.

| Feature | Detailed requirements | Stories | Origin |
|---|---|---|---|
| Project membership and restricted access; member lists | [FR-001, FR-004–FR-005, FR-025](docs/requirements.md#functional-requirements) | [US-002, US-007, US-009, US-026](docs/user-stories.md#stories) | Proposal; member lists for all members: team decision |
| Create, rename, and complete projects | [FR-002–FR-003, FR-007](docs/requirements.md#functional-requirements) | [US-001, US-008, US-010](docs/user-stories.md#stories) | Proposal |
| Create, view, rename, assign, and update tasks, with several assignees | [FR-006, FR-008–FR-011, FR-019](docs/requirements.md#functional-requirements) | [US-003–US-005, US-011–US-012, US-019–US-020](docs/user-stories.md#stories) | Proposal; several assignees and self-assignment: team decision |
| View per-project task counts and progress | [FR-012](docs/requirements.md#functional-requirements) | [US-006](docs/user-stories.md#stories) | Proposal |
| Sign in, sign out, and see who is signed in | [FR-013–FR-015](docs/requirements.md#functional-requirements) | [US-013–US-015](docs/user-stories.md#stories) | Team decision |
| Delete projects and tasks | [FR-016–FR-017](docs/requirements.md#functional-requirements) | [US-016–US-017](docs/user-stories.md#stories) | Team decision |
| Task priority | [FR-018](docs/requirements.md#functional-requirements) | [US-018](docs/user-stories.md#stories) | Team decision |
| Task comments | [FR-020–FR-021](docs/requirements.md#functional-requirements) | [US-021–US-022](docs/user-stories.md#stories) | Team decision |
| Search projects and filter tasks | [FR-022–FR-023](docs/requirements.md#functional-requirements) | [US-023–US-024](docs/user-stories.md#stories) | Team decision |
| My projects | [FR-024](docs/requirements.md#functional-requirements) | [US-025](docs/user-stories.md#stories) | Team decision |

## Project status and next phase

- **Current phase:** implemented delivery candidate; I12 Awaiting review. Requirements and client validation remain pending ([ADR-002](docs/adr/ADR-002-phase-scope-and-implementation-authorization.md)). *Changed on 2026-11-12 by delivery decision 13: previously “requirements specification v0.2 — draft awaiting client validation”.*
- **Architecture:** modular monolith chosen by the project team on 2026-10-25 ([ADR-001](docs/adr/ADR-001-modular-monolith.md)); see [architecture definition](docs/architecture.md). On 2026-10-29 the team chose the technology stack ([ADR-003](docs/adr/ADR-003-technology-stack.md)), a REST API as the single entry point ([ADR-004](docs/adr/ADR-004-rest-api-single-entry-point.md)), signed access tokens ([ADR-005](docs/adr/ADR-005-authentication-with-jwt.md)), and how simultaneous changes are handled ([ADR-006](docs/adr/ADR-006-simultaneous-changes.md)). All are provisional until the MVP scope is approved (OQ-016). *Added on 2026-10-30 by team decision in the review of increment I1:* the backend is structured with domain-driven design inside the modules ([ADR-008](docs/adr/ADR-008-domain-driven-design.md)).
- **Implementation:** provisionally authorized by the project team on 2026-10-29, at the team's risk, while the client's approval stays open (OQ-016). The [implementation plan](docs/implementation-plan.md) covers I0–I13; every increment except I12 is Accepted. The team approved the matching update of the current phase in [AGENTS.md](AGENTS.md#current-phase-and-next-steps) on 2026-10-30. *Changed in v0.2: previously, implementation was not to start before the MVP scope was approved. Reconciled on 2026-11-12 by delivery decision 13: previously “I1–I12”, omitting I13.*
- **MVP release criteria (proposed):** see [MVP scope](docs/requirements.md#mvp-scope); business success measures are still open (OQ-009).
- **Pending:** client answers to the [open questions](docs/constraints.md#open-questions) (OQ-001–OQ-031) and validation of the [assumptions](docs/constraints.md#assumptions) (A-001–A-037). The team's [interim answers](docs/constraints.md#interim-team-answers-2026-10-29) apply until the client answers. *Ranges reconciled on 2026-11-12 by delivery decision 13: previously OQ-030/A-033.*
- **Next phase:** the first increment whose Status is not Accepted in the [implementation plan](docs/implementation-plan.md#increments), then the following increments in dependency order. Client validation continues in parallel. *Changed on 2026-11-05 after I4 was accepted (the conversation “Lab Worksheet 03 — Implement task management”): previously “the project team's review of increment I4, creating, listing, viewing, changing, and deleting tasks, with the task filters (the task parts of I10 and I11, brought forward); they are Awaiting review in the implementation plan, and their evidence is in the task management verification record. Then increment I5, in dependency order. Client validation continues in parallel.”* *Changed on 2026-11-05 by the exercise “Implement task management”: previously “increment I4, creating, listing, viewing, and changing tasks, with the task parts of I10 (deleting a task) and I11 (filtering tasks) as the team decides in the [implementation plan](docs/implementation-plan.md#increments), since I3, with the project parts of I10 and I11, is Accepted. Client validation continues in parallel.”* *Changed on 2026-11-05 after I3 was accepted (the conversation “Lab Worksheet 03 — Implement task management”): previously “the project team's review of increment I3, creating, listing, viewing, and editing projects, together with the project parts of I10 (deleting a project) and I11 (searching projects by name); they are Awaiting review in the implementation plan, and their evidence is in the project management verification record. Then the following increments in dependency order. Client validation continues in parallel.”* *Changed on 2026-11-02 by the exercise “Implement project management”: previously “increment I3, creating, listing, viewing, and renaming projects, since I2 (signing in, signing out, and seeing the signed-in person) is Accepted in the [implementation plan](docs/implementation-plan.md#increments); I2's evidence is in the [I2 verification record](docs/evidence/I2-verification.md). Client validation continues in parallel.”* *Changed on 2026-11-02 after I2 was accepted (the conversation “I2 review and acceptance”): previously “the project team's review of increment I2: signing in, signing out, and seeing the signed-in person. I2 is Awaiting review; its evidence is in the [I2 verification record](docs/evidence/I2-verification.md), and its status in the [implementation plan](docs/implementation-plan.md#increments). Then increment I3, once I2 is accepted. Client validation continues in parallel.”* *Changed on 2026-10-30: previously “increment I1, the walking skeleton; client validation continues in parallel”. Changed again on 2026-10-30 after the team's review of I1: previously “the project team's review of increment I1, the walking skeleton, which is awaiting review in the implementation plan; then increment I2”. Changed on 2026-11-02 (Lab Worksheet 03, Step 4), after I1 was accepted on 2026-10-31: previously “the project team's review of increment I1, the walking skeleton, with the changes requested in its first review: demonstration data through a Flyway migration ([ADR-007](docs/adr/ADR-007-build-persistence-and-demonstration-data.md), decision 10, amended) and domain-driven design ([ADR-008](docs/adr/ADR-008-domain-driven-design.md)). I1's status is in the [implementation plan](docs/implementation-plan.md#increments). Then increment I2. Client validation continues in parallel.”*

*Next-phase wording reconciled on 2026-11-12 by delivery decision 13: the dynamic rule is retained; the former “since I4 … is Accepted” clause is a historical milestone, not the current review target.*

<a id="run-matriz-locally"></a>

## Requirements

*Run guide completed on 2026-11-12 by the project team's delivery decisions 13 and 18, replacing the I1/I2 startup guide. Previously that guide started/migrated the database before enabling demonstration data; enable it **before the first application startup**, as the fixed per-database setting in [ADR-007](docs/adr/ADR-007-build-persistence-and-demonstration-data.md#10-demonstration-data) requires.*

| Requirement | Version / purpose |
|---|---|
| JDK | 25; verified with Temurin 25.0.5. `java -version` and `JAVA_HOME` must select it. |
| Docker + Compose | Docker running, Compose v2 with `up --wait`; verified with Engine 28.3.2. PostgreSQL 18.6 is downloaded, not installed on the host. |
| Maven / Node.js | Wrapper downloads Maven 3.9.16; Maven installs Node.js 24.22.2 and npm under `frontend/node`. No global Maven or Node installation is needed for the normal build. |
| Shell utilities | Bash, Git, curl and OpenSSL for the commands below; network access to dependency/image registries and the Chromium download. |
| Browser tests | Playwright 1.64.0's Chromium distribution is downloaded automatically. On Linux, its system libraries must also be installed; see the pinned `install-deps chromium` command in [CI](.github/workflows/ci.yml). macOS is the locally verified platform. |

On macOS with several JDKs, select the installed JDK for the current terminal:

```sh
export JAVA_HOME="$(/usr/libexec/java_home -v 25)"
export PATH="$JAVA_HOME/bin:$PATH"
java -version
docker compose version
```

## Installation

For the published version:

```sh
git clone https://github.com/pedropires-ppb/matriz-ai-workshop.git
cd matriz-ai-workshop
```

Until the candidate branch is published, use the local fresh-clone recipe in [Isolated clean-copy verification](#isolated-clean-copy-verification). All following commands run from the clone's root. Do not copy another checkout's `.env`, build outputs or `node_modules`.

## Configuration and first start

On a new checkout only, copy the configuration example:

```sh
test ! -e .env && cp .env.example .env
chmod 600 .env
```

Edit this ignored file locally; every published example value is fictitious. Use unquoted values. Real environment variables and command-line properties override the file. The database and application must use the same credentials/port. Missing required settings stop startup and name the setting, never its value.

| Setting | Meaning |
|---|---|
| `MATRIZ_DB_URL` | Required JDBC URL, normally `jdbc:postgresql://localhost:5432/matriz`. |
| `MATRIZ_DB_USER`, `MATRIZ_DB_PASSWORD` | Required; Compose creates these credentials only on a fresh volume. Changing the file does not rotate existing database credentials. |
| `MATRIZ_DB_PORT` | Compose host port, default 5432, bound to loopback; match it in the JDBC URL. The container port stays 5432. |
| `MATRIZ_JWT_SIGNING_KEY` | Required Base64 key with at least 32 decoded random bytes. The example placeholder is rejected. Generate privately with `openssl rand -base64 32`; changing it invalidates existing tokens at the next startup. |
| `MATRIZ_DEMO_DATA` | Choose **before the first startup**: `true` loads the demonstration migration once; false/absent does not. This choice is fixed for that database. |
| `MATRIZ_DEMO_PASSWORD_ANA`, `MATRIZ_DEMO_PASSWORD_RUI`, `MATRIZ_DEMO_PASSWORD_EVA`, `MATRIZ_DEMO_PASSWORD_DANIEL` | Required at **every** startup with demo mode true: each 15+ characters and at most 72 UTF-8 bytes. Used only when the seed runs; later edits do not rotate stored passwords. |
| `--server.port=18089` | Optional run-command override of the normal 8080 application port; not the database port. |
| `VITE_API_BASE_URL` | Optional frontend build-time setting in `frontend/src/config.ts`; default empty, same origin. Another origin is not supported without a separately approved CORS change. |

An empty database without demonstration data has no provisioned sign-in account; account administration is outside the MVP. For a usable local demonstration, set demo mode and all four passwords now. A database already migrated past the seed's version (1.1) cannot enable it later. Mismatched mode or conflicting rows stops startup without partially loading seed data. Use a fresh, separate database rather than resetting somebody else's.

Start PostgreSQL, build, then start the application in the foreground:

```sh
docker compose up -d --wait
./mvnw -B -DskipTests package
java -jar target/matriz-0.1.0-SNAPSHOT.jar
```

Flyway applies [versioned schema migrations](src/main/resources/db/migration) V1–V6 and, when selected, the [SQL demo migration](src/main/resources/db/demo/V1_1__demonstration_data.sql) between V1 and V2. There is no separate schema/seed command and no manual SQL to run. Hibernate only validates the migrated schema; Flyway clean is disabled. Normal restarts preserve rows and do not restore changed/deleted seed data. PostgreSQL's named volume persists independently of the backend.

In a second terminal:

```sh
curl --fail-with-body http://localhost:8080/actuator/health
```

Expect overall `UP` and database `UP`, without component details. Only this Actuator endpoint is public; `/actuator` and other Actuator endpoints are 404. Open <http://localhost:8080/>. For an alternate application port, change the URL accordingly. Stop **your own backend** with Ctrl+C; stopping a database container requires separate approval when an agent operates it.

**Troubleshooting:** a port conflict needs a separate project and free host port, not stopping an unknown container. For a private-CA Maven download error (`PKIX path building failed`), use `export MAVEN_OPTS="-Djavax.net.ssl.trustStore=/path/to/truststore"` with an approved local trust store; never commit trust stores or registry settings.

## Demonstration accounts

All four people are fictitious; there is **no published/default real password**. Sign in with the password you configured before seeding. Login-name case is ignored, but whitespace is not trimmed.

| Display name | Login name | Password setting |
|---|---|---|
| Ana Silva | `ana` | `MATRIZ_DEMO_PASSWORD_ANA` |
| Rui Martins | `rui` | `MATRIZ_DEMO_PASSWORD_RUI` |
| Eva Santos | `eva` | `MATRIZ_DEMO_PASSWORD_EVA` |
| Daniel Rocha | `daniel` | `MATRIZ_DEMO_PASSWORD_DANIEL` |

The [seed inventory](docs/adr/ADR-007-build-persistence-and-demonstration-data.md#10-demonstration-data) has 4 people, 4 projects, 8 memberships, 9 tasks, 3 assignments and 2 comments. Ana, Rui and Eva demonstrate different project memberships; Daniel starts without projects. These are per-project roles, not global account roles.

<a id="use-the-user-interface"></a>

## Use the application

*Added on 2026-11-09 with increment [I13](docs/implementation-plan.md#i13--user-interface-consolidation).* After signing in at <http://localhost:8080/>, the User interface has five screens, each with its own address:

| Screen | Address | Content |
|---|---|---|
| Sign-in | any address, before signing in | Login name and password. After signing in, the address that was asked for opens. |
| Home | `#/` | The signed-in person, the number of their projects, the first five with their task counts by state, and a link to all. *Changed on 2026-11-09 with increment [I5](docs/implementation-plan.md#i5--progress-and-my-projects): previously without the task counts.* |
| Projects | `#/projects`, `#/projects/new` | Search by name, paging, each project's task counts by state, and the “New project” form. *Changed on 2026-11-09 with increment I5: previously without the task counts.* |
| Project detail | `#/projects/{projectId}` | The details, with Edit, Complete (*added on 2026-11-09 with increment I8*), and Delete for the Project Owner; the progress, with the percentage Done, a bar, and the task counts by state, or “No tasks” (*added on 2026-11-09 with increment I5*); the members, where the Project Owner of an Active project adds and removes Project Participants (*added on 2026-11-09 with increment I6*); and the tasks, with filters, paging, the assignees of each task, and the “New task” form. |
| Task detail | `#/projects/{projectId}/tasks/{taskId}` | The details, state, and priority; Edit and Delete; the assignees; and the comments. |

The search, the filters, and the page are kept in the address, so Back and Forward work. An unknown address leads to Home. Reloading the page signs the person out ([ADR-005](docs/adr/ADR-005-authentication-with-jwt.md)); the address is kept, so the same screen opens after signing in again. The screens fit widths from 390 pixels; from 720 pixels, the project and task screens use two columns.

**Front-end development.** The front end is in `frontend/` (React, TypeScript, Vite). The build runs its type check and unit tests (Vitest); to run them alone, with the Node.js that the build installs: `cd frontend && PATH="$PWD/node:$PATH" npm test`. The address of the backend is configured in one place, `frontend/src/config.ts`, from `VITE_API_BASE_URL` at build time. It is empty by default, which means the same origin as the page. Only the same origin is supported for now: another origin would need CORS, which is not configured.

### Sign in, use the API, and sign out

*Added on 2026-11-02 with increment I2 ([ADR-005](docs/adr/ADR-005-authentication-with-jwt.md#open-points), [ADR-009](docs/adr/ADR-009-identity-from-the-token.md)).* With the demonstration data, open <http://localhost:8080/> and sign in with a demonstration login name and the password you chose in `.env`. Letter case in the login name does not matter. The [API contract](openapi.yaml) specifies the access token's life cycle under “Token life cycle”; in short:

1. **Sign-in.** `POST /api/session` with the login name and password returns an access token, `expiresIn` (28800 seconds, that is, 8 hours), and the person's identifier and display name. A wrong password and an unknown login name get the same 401.
2. **Use.** Every other request under `/api` sends the token in the header `Authorization: Bearer <token>`. The User interface keeps the token only in memory, so reloading the page or opening a new tab asks you to sign in again.
3. **Expiry.** The token is refused 8 hours after sign-in, whatever its use. There is no refresh token: sign in again.
4. **Sign-out.** `DELETE /api/session` ends only that token. It stays refused, also after Matriz restarts. Changing `MATRIZ_JWT_SIGNING_KEY` ends every token.
5. **Refusal.** A request without a token, or with an altered, expired, or ended token, gets 401 with Problem Details and the header `WWW-Authenticate: Bearer` (`Bearer error="invalid_token"` when a token was sent). The User interface then shows the sign-in screen with “Your session has ended. Please sign in again.”

**Sign-out limitation:** [ADR-009](docs/adr/ADR-009-identity-from-the-token.md) deliberately discards the in-memory token and shows “You have signed out” even when the server sign-out request fails. That confirms local discard, **not server revocation**: a separately retained token may remain usable until expiry. This approved policy is unchanged; session policy remains [Open OQ-027](docs/constraints.md#open-questions). *Clarified on 2026-11-12 by the project team's delivery decision 8.*

The same steps with curl, as Ana. The commands never show the password or the token, and keep the password out of the shell history and the list of processes; they need no other tool. They assume that the password contains no `"` or `\`. While curl runs, the token it sends is visible in the list of processes; this suits a local, single-person setup.

```sh
# Type Ana's password from .env; it is not shown.
printf 'Password: '; read -rs PASSWORD; echo

# Sign in, and keep the token in a shell variable without printing it.
TOKEN=$(printf '{"loginName":"ana","password":"%s"}' "$PASSWORD" |
  curl -s http://localhost:8080/api/session -H 'Content-Type: application/json' --data-binary @- |
  sed -n 's/.*"accessToken":"\([^"]*\)".*/\1/p')

# See who is signed in: {"personId":"10000000-0000-4000-8000-000000000001","displayName":"Ana Silva"}
curl -s http://localhost:8080/api/me -H "Authorization: Bearer $TOKEN"

# Sign out (204); afterwards the same token gets 401 with Bearer error="invalid_token".
curl -s -o /dev/null -w '%{http_code}\n' -X DELETE http://localhost:8080/api/session -H "Authorization: Bearer $TOKEN"
curl -si http://localhost:8080/api/me -H "Authorization: Bearer $TOKEN"

# Without a token: 401 with WWW-Authenticate: Bearer.
curl -si http://localhost:8080/api/me

# A wrong password and an unknown login name get the same 401: "The login name or password is incorrect."
curl -si http://localhost:8080/api/session -H 'Content-Type: application/json' -d '{"loginName":"ana","password":"not-the-right-password"}'
curl -si http://localhost:8080/api/session -H 'Content-Type: application/json' -d '{"loginName":"nobody","password":"not-the-right-password"}'

unset PASSWORD TOKEN
```

### Manage projects

*Added on 2026-11-02 with increments I3, I10 (project deletion only), and I11 (search by name only) ([A-031 and A-032](docs/constraints.md#assumptions), [ADR-008](docs/adr/ADR-008-domain-driven-design.md)).* The Projects screen lists the projects of the signed-in person, with a search by name, a pager, and a “New project” form; opening a project shows the Project detail, where the Project Owner of an Active project edits or deletes it. Deleting asks for confirmation in the User interface. *Changed on 2026-11-09 with increment I13: previously “After signing in, “My projects” lists the projects of the signed-in person, …; opening a project shows its page, …”.* The [API contract](openapi.yaml) specifies each operation; in short:

| Operation | Request | Success | Notes |
|---|---|---|---|
| `listProjects` | `GET /api/projects?name=&page=&size=` | 200 | Only the person's projects. `name` is a case-insensitive substring search on the trimmed name; accents are significant. `page` starts at 1; `size` is 20 by default, at most 100. Ordered by name, then identifier. A page past the end returns an empty list. |
| `createProject` | `POST /api/projects` with `name` and an optional `description` | 201 and a `Location` header | The signed-in person becomes the Project Owner; the request cannot choose the owner. |
| `getProject` | `GET /api/projects/{projectId}` | 200 | Members only. |
| `updateProject` | `PATCH /api/projects/{projectId}` as `application/merge-patch+json` | 200 | Only the Project Owner, only while the project is Active. `{"description":null}` removes the description. |
| `completeProject` | `PUT /api/projects/{projectId}/state` with `{"state":"COMPLETED"}` | 200 | *Added on 2026-11-09 with increment I8.* Only the Project Owner, only while the project is Active, and only when none of its tasks is To do or In progress (deleted tasks do not count); a project with no tasks can be completed ([A-007](docs/constraints.md#assumptions), pending [OQ-006](docs/constraints.md#open-questions)). Unfinished tasks or an already Completed project get 409; any other content gets 422. Cannot be undone. Afterwards the project and its tasks are read-only; comments can still be added. In the User interface, “Complete project” asks for confirmation and is unavailable while tasks are To do or In progress. |
| `deleteProject` | `DELETE /api/projects/{projectId}` | 204 | Only the Project Owner, only while the project is Active. A soft deletion: the project, its tasks, comments, and assignments disappear from every read; no restore. |

The name has 1 to 100 characters and the optional description at most 1000, counted after trimming; a name that is empty after trimming, or a value over a limit, gets 422 with field errors. A person who is not a member of the project, and a deleted or unknown project, get 404; a Project Participant who tries to edit or delete gets 403; a Completed project gets 409. The limits and the other rules are team decisions awaiting client validation ([A-031](docs/constraints.md#assumptions), [A-032](docs/constraints.md#assumptions)).

### Manage tasks

*Added on 2026-11-05 with increments I4, I10 (task deletion only), and I11 (task filters only) ([A-033](docs/constraints.md#assumptions), [A-004, A-024, A-031, and A-032 as amended](docs/constraints.md#assumptions), [ADR-008](docs/adr/ADR-008-domain-driven-design.md)).* The Project detail has a “Tasks” section: filters by state and priority, the list with a count, the assignees of each task, and a pager, and a “New task” form. Each task opens the Task detail, where the Project Owner of an Active project edits the task, changes its state and priority, or deletes it after a confirmation. *Changed on 2026-11-09 with increment I13: previously “The project page has a “Tasks” section: filters by state and priority, the list with a count and a pager, a “New task” form, and a details panel where …”.* After each operation the page of tasks is fetched again, so the list and the count are current without reloading the page. A Project Participant who is an assignee can change the state; any Project Participant can assign or unassign themselves. *Updated on 2026-11-12 by delivery decision 13: the previous I4-only wording said participants could not change tasks until I7.*

| Operation | Request | Success | Notes |
|---|---|---|---|
| `listTasks` | `GET /api/projects/{projectId}/tasks?state=&priority=&page=&size=` | 200 | Any member. `state` (`TO_DO`, `IN_PROGRESS`, `DONE`) and `priority` (`LOW`, `MEDIUM`, `HIGH`) may be repeated: values of one field are alternatives (OR), and the two fields combine (AND). An invalid or empty value gets 400. `page` starts at 1; `size` is 20 by default, at most 100. Oldest first. A page past the end returns an empty list. |
| `createTask` | `POST /api/projects/{projectId}/tasks` with `title`, and optional `description` and `priority` | 201 and a `Location` header | The task starts To do, with the priority Medium unless another is given. |
| `getTask` | `GET /api/projects/{projectId}/tasks/{taskId}` | 200 | Any member. |
| `updateTask` | `PATCH /api/projects/{projectId}/tasks/{taskId}` as `application/merge-patch+json` | 200 | `title`, `description`, and `priority`. `{"description":null}` removes the description. |
| `changeTaskState` | `PUT /api/projects/{projectId}/tasks/{taskId}/state` with `state` | 200 | Any state to any state. |
| `deleteTask` | `DELETE /api/projects/{projectId}/tasks/{taskId}` | 204 | A soft deletion: the task and its Assignments disappear from every read; no restore. |

The title has 1 to 150 characters and the optional description at most 2000, counted after trimming; text that is empty after trimming gets 422 (for the description, it means none). Spaces, tabs, and the no-break spaces U+00A0, U+2007, and U+202F count as blank, for project and task texts. Only the Project Owner creates, edits details/priority, and deletes tasks; the Project Owner or an assignee changes state. An unassigned Project Participant gets 403 on a state change; a person who is not a member, and an unknown, deleted, or foreign task, get 404; a Completed project gets 409 for every change. Two simultaneous changes are applied one after the other, and the last prevails. The limits, the values, and the rules are team decisions awaiting client validation ([A-033](docs/constraints.md#assumptions), [OQ-030](docs/constraints.md#open-questions)).

### Assignees and comments

On Task detail, the Project Owner assigns/unassigns project members; a Project Participant can assign/unassign only themselves. Several assignees are allowed; assigning an existing assignee is idempotent. Assignment changes do not change state, and removing the last assignee is allowed. Only Active projects permit these changes ([Assumption A-035](docs/constraints.md#assumptions)).

Every Member can read/add comments, including in Completed projects. Text has 1–2000 Unicode code points after the approved trim; the server supplies the author/time and refuses extra properties. Comments are oldest first, cannot be edited/deleted, and retain their author after departure. The browser renders plain text and shows dates in local time; the API uses UTC ([Assumption A-036](docs/constraints.md#assumptions)). Both assumptions retain their recorded provisional/pending status.

| Operation | Request | Success |
|---|---|---|
| Assign / unassign | `POST /api/projects/{projectId}/tasks/{taskId}/assignees` with `personId`; `DELETE /api/projects/{projectId}/tasks/{taskId}/assignees/{personId}` | 200 / 200, each with the updated Task |
| Read / add comments | `GET` / `POST /api/projects/{projectId}/tasks/{taskId}/comments`, POST with `text` only | 200 / 201 with `Location` |

Across forms, A-037 refuses NUL/unpaired-surrogate text rather than silently changing it; approved whitespace and code-point limits match the server. No notification, attachment, account administration or restore interface is included.

*Unassignment summary corrected during the final review on 2026-11-12: the previous README said 204, but the controller, tests and OpenAPI already specify 200 with a Task. No implementation or rule was changed.*

### Manage members

*Added on 2026-11-09 with increment I6 ([A-009](docs/constraints.md#assumptions), [A-030](docs/constraints.md#assumptions), [EX-007](docs/user-stories.md#edge-cases-and-exceptions)).* The Members panel of the Project detail lists every member. The Project Owner of an Active project opens “Add participant”, searches people by name, and adds them; each Project Participant has “Remove”, which asks for confirmation and is unavailable while the person is assigned to tasks. The [API contract](openapi.yaml) specifies each operation; in short:

| Operation | Request | Success | Notes |
|---|---|---|---|
| `listProjectMembers` | `GET /api/projects/{projectId}/members` | 200 | Any member. The Project Owner first, then by display name. `hasAssignedTasks` says whether the member is an assignee of a task that is not deleted, in any state. |
| `listEligibleParticipants` | `GET /api/projects/{projectId}/eligible-participants?name=` | 200 | Only the Project Owner, in any state. Every person who is not a member, without paging; `name` is a trimmed, case-insensitive substring search, and accents are significant. An empty `name` gets 400. A team decision pending the client, with a privacy risk ([A-030](docs/constraints.md#assumptions), [OQ-011](docs/constraints.md#open-questions)). |
| `addParticipant` | `POST /api/projects/{projectId}/members` with `personId` | 201 and a `Location` header | Only the Project Owner, only while the project is Active. An unknown person gets 422; a member gets 409. The person sees the project immediately. |
| `removeParticipant` | `DELETE /api/projects/{projectId}/members/{personId}` | 204 | Only the Project Owner, only while the project is Active. The Project Owner, or a person assigned to a task of the project, gets 409; a person who is not a member gets 404. The person loses access immediately; their comments are kept. |

## Verification

Docker must be running. Testcontainers creates its own databases; the tests do not use your local Compose volume or `.env` database. Only `DemoDataTests` enables seed data, on databases it owns.

```sh
./mvnw -B clean verify
```

This installs frontend dependencies from the lock file, checks TypeScript, runs Vitest, builds Vite, installs Chromium, runs Java unit/API/contract/Playwright tests and the layering/module-boundary checks, then packages the application. `-DskipTests` skips Java tests/Chromium setup, **not** frontend Vitest. CI uses `./mvnw -B verify`; `clean` additionally removes generated Maven outputs.

Focused checks, after the first build:

```sh
./mvnw -B -Dtest=TaskDomainTests test
./mvnw -B -Dtest=TaskApiTests test
./mvnw -B -Dtest=UserInterfaceTests#theProjectOwnerCreatesChangesFiltersAndDeletesTasksWithoutReloadingThePage test
./mvnw -B -Dtest=RequestSchemaTests,ContractTextTests,EveryOperationRequiresATokenTests test
./mvnw -B -Dtest=LayeringTests,ModuleBoundaryTests test
(cd frontend && export PATH="$PWD/node:$PATH" && npm run typecheck && npm test && npm run build)
```

Select JDK 25 or later before running Maven, including a focused selector.
`TaskApiTests` and `UserInterfaceTests` require Docker for their isolated
PostgreSQL Testcontainers database; `UserInterfaceTests` also launches Chromium
through Playwright.
The focused contract command is not a full HTTP/OpenAPI validator: API tests also assert schemas, statuses and headers. See [the recorded checks and limitations](docs/verification.md#contract-and-documentation-reconciliation), [I12](docs/evidence/I12-verification.md) and [the implementation plan](docs/implementation-plan.md#increments). No passing command accepts I12 or answers the client's open questions.

### Isolated clean-copy verification

*Approved by the project team on 2026-11-12, delivery decisions 18–19.* This Bash recipe tests the **committed** local `week3-ex9` candidate without copying a developer `.env`, using the normal startup sequence. Run it from the candidate's original checkout. JDK 25 and Docker must already be selected/running as above. The example uses free host ports **15439** (database) and **18089** (application); if occupied, choose two unused ports and change the JDBC/run/base URLs consistently. Never stop an unknown listener.

```sh
set -euo pipefail
if env | cut -d= -f1 | grep -Eq '^(MATRIZ_|SPRING_)'; then
  echo 'Use a fresh terminal without MATRIZ_ or SPRING_ overrides.' >&2
  exit 1
fi
SOURCE="$PWD"
CLEAN_COPY="$(mktemp -d "${TMPDIR:-/tmp}/matriz-ex9.XXXXXX")"
git clone --no-hardlinks --branch week3-ex9 "$SOURCE" "$CLEAN_COPY"
cd "$CLEAN_COPY"
git status --short
git log -1 --format='%h %s'
export COMPOSE_PROJECT_NAME="matriz-ex9-$(date +%Y%m%d%H%M%S)"
printf 'Clone: %s\nCompose project: %s\n' "$CLEAN_COPY" "$COMPOSE_PROJECT_NAME"
test ! -e .env
(
  umask 077
  sed \
    -e 's|^MATRIZ_DB_URL=.*|MATRIZ_DB_URL=jdbc:postgresql://localhost:15439/matriz|' \
    -e 's|^MATRIZ_DB_PORT=.*|MATRIZ_DB_PORT=15439|' \
    -e '/^MATRIZ_JWT_SIGNING_KEY=/d' \
    -e 's|^MATRIZ_DEMO_DATA=.*|MATRIZ_DEMO_DATA=true|' \
    -e 's|^MATRIZ_DEMO_PASSWORD_ANA=.*|MATRIZ_DEMO_PASSWORD_ANA=fictitious-ana-ex9-only|' \
    -e 's|^MATRIZ_DEMO_PASSWORD_RUI=.*|MATRIZ_DEMO_PASSWORD_RUI=fictitious-rui-ex9-only|' \
    -e 's|^MATRIZ_DEMO_PASSWORD_EVA=.*|MATRIZ_DEMO_PASSWORD_EVA=fictitious-eva-ex9-only|' \
    -e 's|^MATRIZ_DEMO_PASSWORD_DANIEL=.*|MATRIZ_DEMO_PASSWORD_DANIEL=fictitious-daniel-ex9-only|' \
    .env.example > .env
  printf 'MATRIZ_JWT_SIGNING_KEY=' >> .env
  openssl rand -base64 32 >> .env
)
docker compose config --quiet
./mvnw -B clean verify
docker compose up -d --wait
java -jar target/matriz-0.1.0-SNAPSHOT.jar --server.port=18089
```

The passwords above are public **fictitious test values**, only for this isolated, disposable local database; never use them for real accounts. The random signing key stays only in its ignored `.env`. Keep the clone path and Compose project name. In another terminal, `cd` to that printed path and export the **same** `COMPOSE_PROJECT_NAME` before any Compose command. Do not copy the original checkout's environment variables; exported overrides take precedence over this test configuration.

After startup, check health, the Flyway history and initial seed counts:

```sh
BASE=http://127.0.0.1:18089
curl --fail-with-body "$BASE/actuator/health"
docker compose exec -T postgres psql -U matriz -d matriz -c \
  'SELECT version, description, success FROM flyway_schema_history ORDER BY installed_rank;'
docker compose exec -T postgres psql -U matriz -d matriz -c \
  'SELECT (SELECT count(*) FROM person) AS people, (SELECT count(*) FROM project) AS projects, (SELECT count(*) FROM membership) AS memberships, (SELECT count(*) FROM task) AS tasks, (SELECT count(*) FROM assignment) AS assignments, (SELECT count(*) FROM comment) AS comments;'
```

Expect schema V6, successful demo version 1.1 and counts `4 / 4 / 8 / 9 / 3 / 2`. The following uses only the Node executable installed by Maven to parse JSON; no jq dependency. Do not enable shell tracing or print the token. Sign in, create a project and a task, and retain their identifiers:

```sh
NODE="$PWD/frontend/node/node"
json_field() {
  "$NODE" -e 'const body = JSON.parse(require("fs").readFileSync(0, "utf8")); const value = body[process.argv[1]]; if (typeof value !== "string" || !value) throw new Error("Missing JSON field"); process.stdout.write(value);' "$1"
}
TOKEN=$(printf '{"loginName":"ana","password":"fictitious-ana-ex9-only"}' |
  curl --silent --show-error --fail-with-body "$BASE/api/session" \
    -H 'Content-Type: application/json' --data-binary @- | json_field accessToken)
api() {
  printf 'Authorization: Bearer %s\n' "$TOKEN" |
    curl --silent --show-error --fail-with-body --header @- "$@"
}
api "$BASE/api/me"
PROJECT=$(api "$BASE/api/projects" -H 'Content-Type: application/json' \
  --data '{"name":"Clean-copy delivery project","description":"Fictitious verification data"}')
printf '%s\n' "$PROJECT"
PROJECT_ID=$(printf '%s' "$PROJECT" | json_field projectId)
TASK=$(api "$BASE/api/projects/$PROJECT_ID/tasks" -H 'Content-Type: application/json' \
  --data '{"title":"Clean-copy persistence task","priority":"HIGH"}')
printf '%s\n' "$TASK"
TASK_ID=$(printf '%s' "$TASK" | json_field taskId)
printf 'Project: %s\nTask: %s\n' "$PROJECT_ID" "$TASK_ID"
```

Open the UI at <http://127.0.0.1:18089/>, sign in as Ana and open the new project/task. Reload: sign in again (tokens are memory-only), then verify the same rows. In the backend terminal press Ctrl+C, **without stopping PostgreSQL**, then repeat the exact `java -jar ... --server.port=18089` command. Once health is UP, in the API terminal:

```sh
curl --fail-with-body "$BASE/actuator/health"
api "$BASE/api/projects/$PROJECT_ID"
api "$BASE/api/projects/$PROJECT_ID/tasks/$TASK_ID"
unset TOKEN
```

The same identifiers/content and the valid token after restart prove persistence without reseeding. The full browser suite additionally checks the permission/error paths and UI reloads; this API smoke test alone is not a complete manual UI walkthrough.

### Manual reset

**Destructive and irreversible.** Reset only a database you own, after confirming the exact Compose project, container and volume and stopping your own backend. An agent must ask immediately before stopping any container or running `down -v`. Inspect first:

```sh
docker compose ps -a
docker volume ls --filter "label=com.docker.compose.project=${COMPOSE_PROJECT_NAME:-matriz}"
```

Only after explicit approval for those exact resources:

```sh
docker compose down -v
```

This removes that project's containers/network and named volume, including all rows. For a reset you intend to use again, run `docker compose up -d --wait` and restart the backend to recreate schema/seed. For the disposable clean-copy exercise, instead remove **only the printed clone directory** after cleanup; it contains the temporary `.env`. Never use a broad/wildcard recursive delete. Do not stop or remove any pre-existing project. With no override the normal Compose project is `matriz`, volume `matriz_matriz-db`; that is **not** the disposable project above.

## Documentation map

- [Instructions for AI agents](AGENTS.md): source-of-truth order, conventions, and rules for agents.
- [Product requirements, MVP and glossary](docs/requirements.md): vision, user types, functional and non-functional requirements, MVP scope and release criteria, and exclusions.
- [MVP user stories and acceptance criteria](docs/user-stories.md): testable scenarios and exceptional situations.
- [Constraints, assumptions and open questions](docs/constraints.md): known and unknown constraints, assumption register, security/privacy, and decisions awaiting the client.
- [Architecture definition](docs/architecture.md): comparison of a monolith, a modular monolith, and microservices for the proposed MVP; the chosen modular monolith (provisional until OQ-016), its main trade-offs, and its logical modules.
- [Architecture decision records](docs/adr/README.md): the project team's decisions (ADR-001–ADR-009): modular monolith, phase scope and provisional implementation authorization, technology stack, REST API, authentication, simultaneous changes, build, persistence, and demonstration data, domain-driven design, and identity from the token; all provisional until OQ-016. *Changed on 2026-10-30: ADR-007 added; then ADR-008, after the team's review of I1. Changed on 2026-11-02: ADR-009 added.*
- [Implementation plan](docs/implementation-plan.md): increments I0–I13, each with its status, goal, components, dependencies, acceptance criteria (US/EX), and verification, and the rule for updating the status.
- [Verification record of increment I1](docs/evidence/I1-verification.md): the checks run on 2026-10-30 and their trimmed outputs, the evidence to which I1's status links. *Added on 2026-10-30; the re-verification after the team's review of I1 was added the same day.*
- [Verification record of increment I2](docs/evidence/I2-verification.md): the checks run on 2026-11-02 and their trimmed outputs, the evidence to which I2's status links. *Added on 2026-11-02.*
- [MVP verification and delivery review](docs/verification.md): the nine scenarios, corrected defects, diff review, behavior-preserving refactoring and delivery evidence; [I12's remaining release checks](docs/evidence/I12-verification.md) keep their Awaiting review status.
- [Data model](docs/database.md): proposed logical data model for the MVP (entities, relationships, ER diagram, stored and derived data, and consistency review); a proposal pending validation (OQ-016), not an approved design.
- [API contract (draft)](openapi.yaml): OpenAPI 3.1 contract (0.2.0-draft) for the operations of the proposed MVP, FR-001–FR-025; REST is the single entry point by team decision (ADR-004), and the contract is a draft pending validation (OQ-016).
- [Wireframe: project dashboard](docs/ui/project-dashboard.md): low-fidelity wireframe of one proposed MVP screen (a single project's progress, tasks, and members) with its FR/US traceability, the open questions affecting it, and the changes required by the team decisions of 2026-10-29; a proposal pending MVP approval (OQ-016).

Use the requirement IDs (`FR-xxx`, `NFR-xxx`), story IDs (`US-xxx`), exceptional-case IDs (`EX-xxx`), assumption IDs (`A-xxx`), open-question IDs (`OQ-xxx`), and decision IDs (`ADR-xxx`) when referring to this draft. Confirm pending items with the client before treating them as client commitments.




## Review log (human-in-the-loop)

The AI reviewed the documentation for six categories (R-01–R-13). From R-14 onwards, the log records the recommendations of the phase 1 analysis of 2026-10-29 that the team rejected or altered; the team accepted the other recommendations ([ADR-002](docs/adr/ADR-002-phase-scope-and-implementation-authorization.md)). Each recommendation was decided by the team; none was applied automatically.

| # | Category | AI finding | Decision | Rationale |
|---|---|---|---|---|
| R-01 | Ambiguity | MVP had no success criteria. | Altered | Added release criteria derived from acceptance tests only; business measures left open (OQ-009) instead of inventing them. |
| R-02 | Ambiguity | FR-010 silent on reassignment. | Altered | Flagged as OQ-007; AI's suggested rule not adopted. |
| R-03 | Unjustified feature | Check that every FR traces to F01–F03. | Accepted | All FR-001–FR-012 cite F01–F03; extras (comments, notifications, subtasks, attachments) stay out of scope (A-012). *Since 2026-10-29, task comments are in scope by team decision (A-023, ADR-002); FR-013–FR-025 cite F01–F03 and the team decision.* |
| R-04 | Unjustified feature | Option considered: make NFR-004–NFR-007 testable by filling in numeric targets. | Rejected | Would invent requirements; kept as OQ-010. |
| R-05 | Assumption as fact | Rules (roles, states, progress) presented as requirements. | Accepted | All marked `[Assumption A-xxx]` and registered in constraints. |
| R-06 | Unverifiable criteria | US-004 and US-006 criteria vague or not Given/When/Then. | Accepted | Rewritten with observable outcomes. |
| R-07 | Unverifiable criteria | US-005 tested the Project Owner inside a Participant story. | Altered | Moved to EX-013 rather than deleted. |
| R-08 | Security / privacy | Access by non-members or unidentified people. | Accepted | Covered by NFR-001, NFR-002, EX-003, EX-012. |
| R-09 | Security / privacy | Personal data and GDPR obligations unknown. | Accepted | Kept open (OQ-011); no compliance claim made. |
| R-10 | Security / privacy | Prompt injection via external documents. | Accepted | Rule added to [AGENTS.md](AGENTS.md#untrusted-external-content-prompt-injection). |
| R-11 | Inconsistency | Q-/P-/D- references to files not in the repo. | Accepted | Replaced by one OQ register. |
| R-12 | Inconsistency | US-001 actor was Project Owner before the project existed. | Accepted | Actor changed to "a person authorized to create projects". |
| R-13 | Ambiguity | Portuguese UI (NFR-008) vs English-only documentation; option considered: align both. | Rejected | UI language is a client decision (OQ-013); documentation language is a separate rule. *Since 2026-10-29, the interim UI language is English by team decision (A-028, R-18); OQ-013 remains open.* |
| R-14 | Security / privacy | Phase 1: use server-side sessions in a cookie, not JWT. | Rejected | Team chose signed access tokens (JWT) (ADR-005); no rationale recorded. |
| R-15 | Security / privacy | Phase 1: delete projects and tasks permanently. | Altered | Team chose soft deletion: hidden from everyone, data kept (A-022); no rationale recorded. The AI flagged that no retention period is defined (OQ-023). |
| R-16 | Inconsistency | Phase 1: refuse comments in Completed projects, as for every other change (A-011). | Rejected | Team allowed comments in Completed projects (A-023); no rationale recorded. |
| R-17 | Security / privacy | Phase 1: only the Project Owner assigns and unassigns. | Altered | Team also lets a Project Participant assign or unassign themselves (A-020); no rationale recorded. The AI flagged that this lets any Project Participant change any task's state (OQ-025). |
| R-18 | Ambiguity | Phase 1: Portuguese UI, keeping A-013. | Rejected | Team chose English (A-028); no rationale recorded. |
