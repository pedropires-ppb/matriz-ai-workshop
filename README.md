# Matriz

**Status: Requirements specification — v0.2 (draft for client validation), with the project team's provisional decisions of 2026-10-29.** This folder is the persistent requirements knowledge base and proposed single source of truth for later development phases. The only confirmed client needs are to organize projects, manage tasks, and track work progress (F01–F03). Proposed scope, permissions, priorities, and acceptance criteria are **not approved** by the client. The project team has extended the proposed scope and provisionally authorized implementation ([ADR-002](docs/adr/ADR-002-phase-scope-and-implementation-authorization.md)); these are team decisions, not client requirements.

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

- **Current phase:** requirements specification v0.2 — draft awaiting client validation, with the project team's provisional decisions of 2026-10-29 ([ADR-002](docs/adr/ADR-002-phase-scope-and-implementation-authorization.md)).
- **Architecture:** modular monolith chosen by the project team on 2026-10-25 ([ADR-001](docs/adr/ADR-001-modular-monolith.md)); see [architecture definition](docs/architecture.md). On 2026-10-29 the team chose the technology stack ([ADR-003](docs/adr/ADR-003-technology-stack.md)), a REST API as the single entry point ([ADR-004](docs/adr/ADR-004-rest-api-single-entry-point.md)), signed access tokens ([ADR-005](docs/adr/ADR-005-authentication-with-jwt.md)), and how simultaneous changes are handled ([ADR-006](docs/adr/ADR-006-simultaneous-changes.md)). All are provisional until the MVP scope is approved (OQ-016). *Added on 2026-10-30 by team decision in the review of increment I1:* the backend is structured with domain-driven design inside the modules ([ADR-008](docs/adr/ADR-008-domain-driven-design.md)).
- **Implementation:** provisionally authorized by the project team on 2026-10-29, at the team's risk, while the client's approval stays open (OQ-016). Work proceeds in increments I1–I12 of the [implementation plan](docs/implementation-plan.md); the team approved the matching update of the current phase in [AGENTS.md](AGENTS.md#current-phase-and-next-steps) on 2026-10-30. *Changed in v0.2: previously, implementation was not to start before the MVP scope was approved.*
- **MVP release criteria (proposed):** see [MVP scope](docs/requirements.md#mvp-scope); business success measures are still open (OQ-009).
- **Pending:** client answers to the [open questions](docs/constraints.md#open-questions) (OQ-001–OQ-027) and validation of the [assumptions](docs/constraints.md#assumptions) (A-001–A-030). The team's [interim answers](docs/constraints.md#interim-team-answers-2026-10-29) apply until the client answers.
- **Next phase:** the project team's review of increment I1, the walking skeleton, with the changes requested in its first review: demonstration data through a Flyway migration ([ADR-007](docs/adr/ADR-007-build-persistence-and-demonstration-data.md), decision 10, amended) and domain-driven design ([ADR-008](docs/adr/ADR-008-domain-driven-design.md)). I1's status is in the [implementation plan](docs/implementation-plan.md#increments). Then increment I2. Client validation continues in parallel. *Changed on 2026-10-30: previously “increment I1, the walking skeleton; client validation continues in parallel”. Changed again on 2026-10-30 after the team's review of I1: previously “the project team's review of increment I1, the walking skeleton, which is awaiting review in the implementation plan; then increment I2”.*

## Run Matriz locally

*Added on 2026-10-30 with increment I1 ([ADR-007](docs/adr/ADR-007-build-persistence-and-demonstration-data.md)).* Increment I1 has no features yet. The application starts, creates the database schema, loads the demonstration data when asked, reports its health, and serves an empty User interface shell. Run the commands from the repository root in a macOS or Linux shell; they were verified on macOS.

**Prerequisites:** JDK 25 (`java -version` reports 25), and Docker with Docker Compose, running. The build downloads Maven, Node.js, npm, and the other dependencies at their pinned versions. The tests also download Chromium.

1. **Set up the environment (once).** Copy the example file, then replace its fictitious values with your own:

   ```sh
   cp .env.example .env
   ```

   git ignores `.env`; never commit it. Matriz and Docker Compose read it when started from the repository root, and real environment variables take precedence over it. If a variable that Matriz needs is missing or blank, Matriz refuses to start and names the variable.

2. **Start the database.** PostgreSQL 18.6 listens on `127.0.0.1:5432` and keeps its data in the Docker volume `matriz_matriz-db`:

   ```sh
   docker compose up -d --wait
   ```

   Compose takes the database user and password from `.env` only when it creates the volume; to change them later, reset the database (step 6). `docker compose stop` stops the database and keeps the data.

3. **Build and run the application.** At start-up, Matriz applies any pending database migrations. It never deletes data.

   ```sh
   ./mvnw -B package -DskipTests
   java -jar target/matriz-0.1.0-SNAPSHOT.jar
   ```

   In another terminal, check that the application and its database are up:

   ```sh
   curl http://localhost:8080/actuator/health
   ```

   The answer includes `"status":"UP"` and `"db":{"status":"UP"}`. The User interface shell is at <http://localhost:8080/>. Stop the application with Ctrl+C.

4. **Add the demonstration data.** In `.env`, set `MATRIZ_DEMO_DATA=true`, and choose a password for each demonstration account: `MATRIZ_DEMO_PASSWORD_ANA`, `MATRIZ_DEMO_PASSWORD_RUI`, `MATRIZ_DEMO_PASSWORD_EVA`, and `MATRIZ_DEMO_PASSWORD_DANIEL`. Then start the application as in step 3. At that start, a database migration adds the demonstration people, projects, tasks, and comments, once for this database. A demonstration row that you later change or delete stays so; the next start does not restore it.
   - The setting is fixed for each database. Keep `MATRIZ_DEMO_DATA=true` and the passwords in `.env` while you use this database: Matriz requires them at every start, but uses the passwords only when the migration runs, so changing a password later does not change the account.
   - If the setting does not match the database, for example `false` for a database that has the demonstration data, or if existing rows conflict with the demonstration data, Matriz refuses to start, adds nothing, and names the remedy.
   - To get fresh demonstration data or new passwords, reset the database (step 6).
   - The people, projects, tasks, and comments are listed in [ADR-007, decision 10](docs/adr/ADR-007-build-persistence-and-demonstration-data.md#10-demonstration-data). Signing in arrives in increment I2.

   *Changed on 2026-10-30 by team decision in the I1 review ([ADR-007](docs/adr/ADR-007-build-persistence-and-demonstration-data.md), decision 10, amended): previously this step was “Load the demonstration data”; each start added only the demonstration rows that were missing, so a deleted demonstration row came back, and a password was used only when its account was first added.*

5. **Run the tests.** Docker must be running: the tests start their own PostgreSQL container and never touch the local database.

   ```sh
   ./mvnw -B verify
   ```

   The tests include the module boundary and layering checks ([ADR-008](docs/adr/ADR-008-domain-driven-design.md)). Only `DemoDataTests` uses the demonstration data, on databases of its own in the test container. *Added on 2026-10-30 in the I1 review.* Continuous integration runs the same command on every push and pull request ([.github/workflows/ci.yml](.github/workflows/ci.yml)).

6. **Reset the local database (manual step; it deletes all local data).** Matriz never does this by itself. The first command deletes the volume and every row in it, including data added by hand, and cannot be undone:

   ```sh
   docker compose down -v
   docker compose up -d --wait
   ```

   The next start of the application (step 3) creates the schema again and, if requested, the demonstration data. Resetting is also the way to get fresh demonstration data, to apply new demonstration passwords, or to change `MATRIZ_DEMO_DATA` for the database. *Added on 2026-10-30 in the I1 review.*

**Troubleshooting.** If Maven cannot download because of a certificate error, such as `PKIX path building failed`, on a network that uses a private certificate authority, point the JDK at a trust store that contains that authority: `export MAVEN_OPTS="-Djavax.net.ssl.trustStore=/path/to/truststore"`. Never commit trust stores or registry settings.

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

## Documentation map

- [Instructions for AI agents](AGENTS.md): source-of-truth order, conventions, and rules for agents.
- [Product requirements, MVP and glossary](docs/requirements.md): vision, user types, functional and non-functional requirements, MVP scope and release criteria, and exclusions.
- [MVP user stories and acceptance criteria](docs/user-stories.md): testable scenarios and exceptional situations.
- [Constraints, assumptions and open questions](docs/constraints.md): known and unknown constraints, assumption register, security/privacy, and decisions awaiting the client.
- [Architecture definition](docs/architecture.md): comparison of a monolith, a modular monolith, and microservices for the proposed MVP; the chosen modular monolith (provisional until OQ-016), its main trade-offs, and its logical modules.
- [Architecture decision records](docs/adr/README.md): the project team's decisions (ADR-001–ADR-008): modular monolith, phase scope and provisional implementation authorization, technology stack, REST API, authentication, simultaneous changes, build, persistence, and demonstration data, and domain-driven design; all provisional until OQ-016. *Changed on 2026-10-30: ADR-007 added; then ADR-008, after the team's review of I1.*
- [Implementation plan](docs/implementation-plan.md): increments I0–I12, each with its status, goal, components, dependencies, acceptance criteria (US/EX), and verification, and the rule for updating the status.
- [Verification record of increment I1](docs/evidence/I1-verification.md): the checks run on 2026-10-30 and their trimmed outputs, the evidence to which I1's status links. *Added on 2026-10-30; the re-verification after the team's review of I1 was added the same day.*
- [Data model](docs/database.md): proposed logical data model for the MVP (entities, relationships, ER diagram, stored and derived data, and consistency review); a proposal pending validation (OQ-016), not an approved design.
- [API contract (draft)](openapi.yaml): OpenAPI 3.1 contract (0.2.0-draft) for the operations of the proposed MVP, FR-001–FR-025; REST is the single entry point by team decision (ADR-004), and the contract is a draft pending validation (OQ-016).
- [Wireframe: project dashboard](docs/ui/project-dashboard.md): low-fidelity wireframe of one proposed MVP screen (a single project's progress, tasks, and members) with its FR/US traceability, the open questions affecting it, and the changes required by the team decisions of 2026-10-29; a proposal pending MVP approval (OQ-016).

Use the requirement IDs (`FR-xxx`, `NFR-xxx`), story IDs (`US-xxx`), exceptional-case IDs (`EX-xxx`), assumption IDs (`A-xxx`), open-question IDs (`OQ-xxx`), and decision IDs (`ADR-xxx`) when referring to this draft. Confirm pending items with the client before treating them as client commitments.





