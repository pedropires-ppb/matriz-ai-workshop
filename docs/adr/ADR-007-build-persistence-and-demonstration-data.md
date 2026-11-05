# ADR-007 — Build, persistence, and demonstration data

- **Status:** Accepted (provisional). *Amended on 2026-10-30 by team decision in the I1 review (the conversation “I1 review: demonstration data through Flyway, and a domain-driven backend”, questions 1–4 and 12): decision 10 now adds the demonstration data through a Flyway migration instead of start-up loaders, which changes parts of decisions 1, 6, 8, and 11, the consequences, the alternatives, and the open points. Each change is flagged where it is made, with the previous text. The same review recorded domain-driven design in [ADR-008](ADR-008-domain-driven-design.md).* *Amended again on 2026-10-30 by team decision in the conversation “I1 review: remaining decisions” (decisions 3–6): Mockito is loaded as a Java agent (decision 2), a risk is accepted (consequences), and two open points are resolved. The [ADR index](README.md) now allows amending an ADR in place while the increment that introduced it, here I1, is not Accepted (decision 1 of that conversation).* *On 2026-11-02, by team decision (Lab Worksheet 03, questions 10 and 11), the open points “Case of login names” and “Health check after I2” were resolved in place. Decisions 6, 8, and 10 gained flagged cross-references to the resolved points of [ADR-005](ADR-005-authentication-with-jwt.md) and to these two points. None of these changes alters a decision made for I1.*
- **Date:** 2026-10-30
- **Decided by:** the project team, in a working session with the AI assistant on 2026-10-30 (the conversation “Prepare the application and persistence”, parts 1 and 2). For each question, the AI assistant presented two or three options with their trade-offs and a recommendation [Proposal]. The team chose an option for every question and confirmed the summary. The AI assistant decided nothing.
- **Related:** [ADR-001](ADR-001-modular-monolith.md), [ADR-003](ADR-003-technology-stack.md), [ADR-004](ADR-004-rest-api-single-entry-point.md), [ADR-005](ADR-005-authentication-with-jwt.md), [ADR-006](ADR-006-simultaneous-changes.md), [ADR-008](ADR-008-domain-driven-design.md) (added on 2026-10-30 in the I1 review); [data model](../database.md); [implementation plan](../implementation-plan.md#i1--walking-skeleton) (I1); [architecture definition](../architecture.md#components-and-responsibilities) (module rule 2); [Assumption A-016](../constraints.md#assumptions), [Assumption A-017](../constraints.md#assumptions), [Assumption A-028](../constraints.md#assumptions); [OQ-003](../constraints.md#open-questions), [OQ-011](../constraints.md#open-questions), [OQ-016](../constraints.md#open-questions), [OQ-020](../constraints.md#open-questions), [OQ-021](../constraints.md#open-questions), [OQ-024](../constraints.md#open-questions), [OQ-027](../constraints.md#open-questions)

## Context

- Increment I1, the walking skeleton, needs the open points of [ADR-003](ADR-003-technology-stack.md#open-points) that are due in I1: the versions, how the User interface build is packaged, the continuous integration service, and the browsers covered by end-to-end tests.
- The [data model](../database.md) is logical and leaves the physical schema to versioned migrations. It does not fix the identifier type, which [openapi.yaml](../../openapi.yaml) also leaves undecided, nor how enumerations are stored, the text lengths ([OQ-003](../constraints.md#open-questions), [OQ-024](../constraints.md#open-questions)), or which of its rules 1–8 the database enforces.
- [ADR-005](ADR-005-authentication-with-jwt.md) decision 6 requires demonstration passwords to be supplied when the application starts and never committed, but not how.
- The team asked for the application and its persistence to be prepared:
  - the project structure;
  - the database connection through environment variables, with an example file;
  - the schema for people, projects, tasks, comments, and assignments;
  - reproducible demonstration data that no start-up deletes;
  - proof that the application reaches the database.

  This brings forward two things that the [implementation plan](../implementation-plan.md) had placed later: the whole schema (migrations per increment from I2 onwards) and the demonstration accounts (I2).

## Decision

### 1. Versions (open point of ADR-003)

| Component | Version |
|---|---|
| Java | 25 (Temurin 25.0.5 when decided). The build refuses an older JDK. |
| Maven | 3.9.16, run through the Maven Wrapper 3.3.4. The wrapper is a script only, with no jar in the repository. |
| Spring Boot | 4.1.2. It manages these versions: Spring Framework 7.0.10, Spring Security 7.1.2, Hibernate 7.4.13.Final, Flyway 12.4.0, Jackson 3.1.7, JUnit Jupiter 6.0.3, Testcontainers 2.0.5, and the PostgreSQL JDBC driver 42.7.14. |
| Spring Modulith | 2.1.2 |
| PostgreSQL | 18.6 (container image `postgres:18.6`) |
| Node.js | 24.22.2 (long-term support), with its bundled npm 11.19.1. It is used only to build the User interface. |
| User interface | TypeScript 6.0.3, React and React DOM 19.3.1, Vite 8.3.5, @vitejs/plugin-react 6.1.2 |
| End-to-end tests | Playwright for Java 1.64.0 |
| Contract check | networknt json-schema-validator 3.0.8 |
| Build plugins | frontend-maven-plugin 2.0.2 and exec-maven-plugin 3.6.4. The other plugins are the versions Spring Boot 4.1.2 manages. |
| Layering check | ArchUnit 1.4.2 ([ADR-008](ADR-008-domain-driven-design.md), decision 5). *Added on 2026-10-30 by team decision in the I1 review (question 9).* |

Versions are pinned exactly in `pom.xml` and `frontend/package.json`; `frontend/package-lock.json` pins the npm dependency tree.

### 2. Build and User interface packaging (open point of ADR-003)

- One Maven project, `com.matriz:matriz`, builds and tests everything with `./mvnw verify`.
- The User interface is in `frontend/`. During the build, frontend-maven-plugin:
  1. installs the pinned Node.js in `frontend/node/`, which is not committed;
  2. runs `npm ci` and the TypeScript check;
  3. runs `vite build`, which writes into the application's static resources.

  The application then serves the User interface shell from the same origin as the API ([ADR-004](ADR-004-rest-api-single-entry-point.md)).
- Surefire loads Mockito, in the version Spring Boot 4.1.2 manages, as a Java agent (`-javaagent`), as [Mockito's documentation](https://javadoc.io/doc/org.mockito/mockito-core/latest/org.mockito/org/mockito/Mockito.html#0.3) describes for Maven. The JVM warns that loading agents dynamically will be disallowed by default in a future release. maven-dependency-plugin's `properties` goal supplies the path of Mockito's jar, and an empty `argLine` property lets Surefire start when no other plugin sets one. *Added on 2026-10-30 by team decision (the conversation “I1 review: remaining decisions”, decision 4). Previously Mockito attached itself while the tests ran, and the build log showed its warning and the JVM's.*

### 3. End-to-end tests (open point of ADR-003)

- Playwright for Java runs as a JUnit test inside `./mvnw verify`. The application runs on a random port, with PostgreSQL in a container.
- Chromium only, provisionally, until [OQ-021](../constraints.md#open-questions) is answered. The build installs Chromium in the user's cache (`ms-playwright`), outside the repository.

### 4. Continuous integration (open point of ADR-003)

GitHub Actions (`.github/workflows/ci.yml`) runs `./mvnw -B verify` with Temurin 25 on every push and pull request. It uses GitHub-hosted Ubuntu runners, which provide Docker for Testcontainers.

### 5. Local database

- `compose.yaml` runs PostgreSQL 18.6 (`postgres:18.6`) with a named volume (`matriz-db`). Its port is bound to `127.0.0.1` only. It is started by hand with `docker compose up -d`.
- The database user and password come from the same variables as the application's (decision 6). The compose file has no default credentials.

### 6. Environment variables

- The database connection is configured only through environment variables. Configuration files contain no credentials and no default values for them.
- Locally, the variables are kept in `.env`, which is copied from `.env.example` and ignored by git. `.env.example` holds fictitious values only.
  - Docker Compose reads `.env` directly.
  - The application imports it (`spring.config.import=optional:file:.env[.properties]`) when started from the repository root.
  - Real environment variables and command-line arguments take precedence over `.env`.
  - Values are written unquoted, as `KEY=value`.
- When a required variable is missing or blank, start-up stops with a message that names the variables, never their values.

| Variable | Required | Use |
|---|---|---|
| `MATRIZ_DB_URL` | Always | The JDBC URL of the database. |
| `MATRIZ_DB_USER` | Always | The database user. Compose also creates it. |
| `MATRIZ_DB_PASSWORD` | Always | That user's password. |
| `MATRIZ_DEMO_DATA` | No; `false` when absent | `true` applies the demonstration data migration (decision 10). The setting is fixed for each database. Any value other than `true` or `false` stops start-up. *Changed on 2026-10-30 by team decision in the I1 review (questions 2 and 4): previously, `true` loaded the demonstration data at every start.* |
| `MATRIZ_DEMO_PASSWORD_ANA`, `MATRIZ_DEMO_PASSWORD_RUI`, `MATRIZ_DEMO_PASSWORD_EVA`, `MATRIZ_DEMO_PASSWORD_DANIEL` | When `MATRIZ_DEMO_DATA=true` | The passwords of the demonstration accounts (decision 10). |
| `MATRIZ_JWT_SIGNING_KEY` | Always | The key that signs and verifies access tokens: at least 256 bits, Base64-encoded. Start-up also stops when it is not valid. *Added on 2026-11-02 as a cross-reference to [ADR-005](ADR-005-authentication-with-jwt.md#open-points) (“Signing”, resolved by team decision in Lab Worksheet 03, questions 6 and 17).* |

### 7. Health check (flagged exception to ADR-004 decision 1)

- Spring Boot Actuator exposes only its health endpoint, at `/actuator/health`, outside `/api` and outside [openapi.yaml](../../openapi.yaml).
- It reports the overall status and the status of each component, including the database (`db`), without details. It shows no product data.
- **Flag:** [ADR-004](ADR-004-rest-api-single-entry-point.md) decision 1 makes the API in openapi.yaml the only way to read information. The team approved this endpoint as an exception: it reads only whether the application and its database are working.

### 8. Physical schema

The schema is created by versioned Flyway migrations in [`src/main/resources/db/migration/`](../../src/main/resources/db/migration/). The first, `V1__create_schema.sql`, creates the whole data model. Later changes are new migrations; an applied migration is never edited. Hibernate only validates the schema and never creates or changes it. Flyway's `clean` command is disabled.

Schema migrations have whole version numbers (`V2`, `V3`, …). Demonstration data migrations are in [`src/main/resources/db/demo/`](../../src/main/resources/db/demo/), and each is a sub-version of the schema migration it was written for (decision 10). [`MigrationNamingTests`](../../src/test/java/com/matriz/MigrationNamingTests.java) checks both. *Added on 2026-10-30 by team decision in the I1 review (question 3).*

| Point | Decision |
|---|---|
| Identifiers | UUID version 4, generated by the application and stored in `uuid` columns. The contract's `Identifier` stays opaque, so [openapi.yaml](../../openapi.yaml) is unchanged. An identifier in a path that is not a valid UUID is answered as not found (404). Demonstration rows use fixed UUIDs (decision 10). |
| Enumerations | `text` columns with a CHECK listing the API codes: `ACTIVE`, `COMPLETED`; `PROJECT_OWNER`, `PROJECT_PARTICIPANT`; `TO_DO`, `IN_PROGRESS`, `DONE`; `LOW`, `MEDIUM`, `HIGH`. JPA maps them by name. |
| Text | `text` columns with only a nonempty CHECK (`<> ''`), like the contract's `minLength: 1`. Whitespace-only names and titles, and duplicate ones, are accepted until [OQ-003](../constraints.md#open-questions) is answered. `login_name` is unique, as the data model requires, and stored in lower case, so login names are case-insensitive. *Changed on 2026-11-02 by resolving the open point “Case of login names” (team decision, Lab Worksheet 03, question 10): previously “and case-sensitive”. The migration `V3` adds the check.* The maximum length is agreed in I3 and added by a new migration ([OQ-003](../constraints.md#open-questions), [OQ-024](../constraints.md#open-questions)). |
| Order of tasks | A technical column, `creation_order` (a `bigint` the database generates), records the order in which tasks are created. It supports the contract's oldest-first task list, because random identifiers carry no order. The API never exposes it. **Flag:** this changes the [data model](../database.md#stored-and-derived-data), which stored only the attributes in its diagram. |
| Order of comments | By `created_at`, with ties broken by `comment_id` [Proposal]. |

The database enforces only declarative rules:

| Data model rule | Enforced by the database | Left to the application |
|---|---|---|
| Keys and references | Primary keys; NOT NULL on required attributes; unique `login_name`; foreign keys only along module dependencies (decision 9); no cascading deletes. | — |
| 1. One Project Owner | At most one Membership with role Project Owner per project (a partial unique index). | At least one owner per project. |
| 2. Member removal | A Membership that an Assignment references cannot be deleted (`ON DELETE RESTRICT`). | The role must be Project Participant, and the project Active. |
| 3. Initial values | Defaults: a project is Active; a task is To do, with Task priority Medium. | — |
| 4. State changes | Only the values of each enumeration (CHECK). | Which changes are allowed. |
| 5. Completed projects | — | All of it. |
| 6. Deletion | — | All of it, including removing a deleted task's Assignments. |
| 7. Comments are not changed | — | All of it. |
| 8. Assignments stay within the project | Composite foreign keys: an Assignment's task and project match a Task of that project, and its project and person match a Membership. | — |

The application applies each rule spanning modules as one indivisible change ([ADR-006](ADR-006-simultaneous-changes.md)).

### 9. Tasks depends on Identity (flagged clarification of module rule 2)

- Module rule 2 of the [architecture definition](../architecture.md#components-and-responsibilities) says that Tasks uses Projects. It does not list Identity. Yet the [data model](../database.md#entities) refers a Comment's author directly to the Person, so that a comment keeps its author after the author leaves the project.
- The team allowed Tasks to depend on Identity:
  - Spring Modulith allows Tasks to depend on Projects and on Identity;
  - the comment author has a foreign key to Person.

  The dependencies stay acyclic.

### 10. Demonstration data

**Loading.** *Changed on 2026-10-30 by team decision in the I1 review (questions 1–4), so that a demonstration row that someone deletes does not come back at the next start-up. Previously, after the migrations, each owning module's loader inserted its own data in its own transaction (Identity, then Projects, then Tasks) with `INSERT … ON CONFLICT DO NOTHING`, at every start with `MATRIZ_DEMO_DATA=true`. A demonstration row deleted by hand returned at the next start, and one changed by hand kept its changes. Automated tests never enabled the demonstration data, except one test that ran the loaders twice.*

- The data is added only when `MATRIZ_DEMO_DATA=true`. It is off by default.
- It is one versioned Flyway migration in SQL, [`V1_1__demonstration_data.sql`](../../src/main/resources/db/demo/V1_1__demonstration_data.sql), in `src/main/resources/db/demo/`, apart from the schema migrations. Its sections follow the owning modules: Identity (people), Projects (projects and memberships), and Tasks (tasks, assignments, and comments).
- A Flyway configuration customizer in the base package (decision 11) adds that location only when `MATRIZ_DEMO_DATA=true`. Flyway then applies the migration once per database, in one transaction, and records it in the same history table as the schema migrations.
- Consequently, start-up deletes, overwrites, and duplicates nothing. A demonstration row deleted or changed by hand stays so; only resetting the local database restores it.
- **Numbering.** A demonstration migration is a sub-version of the schema migration it was written for: `V1_1` follows `V1`. Schema migrations have whole numbers (decision 8), so the two never clash.
- **All or nothing.** The migration uses plain `INSERT` statements. If a row conflicts with existing data, for example a login name that another person already has, none of the demonstration data is added, Flyway records nothing, and start-up stops. The message gives the database's error and the remedy, never the failed statement, which contains the password hashes.
- **Fixed for each database.** Before migrating, start-up compares the setting with the database, and stops with the remedy when they differ:
  - `MATRIZ_DEMO_DATA` is not `true`, but the database has the demonstration data. Remedy: set it to `true`, with the passwords, or reset the local database.
  - `MATRIZ_DEMO_DATA=true`, but the database's schema is already past the demonstration migration's version. Remedy: set it to `false`, or reset the local database.

  An empty database, or one whose schema is still at `V1`, can therefore get the demonstration data.
- **Tests.** Automated tests never enable the demonstration data, so the test databases have only the schema migrations. The exception is [`DemoDataTests`](../../src/test/java/com/matriz/DemoDataTests.java), which enables it on databases of its own.

**Passwords.**

- Each account has its own variable (decision 6), which is empty in `.env.example`.
- If demonstration data is requested and a variable is blank, start-up stops. The message names the variable, never its value.
- Only nonemptiness is checked; password rules are open ([OQ-027](../constraints.md#open-questions)). *Changed on 2026-11-02 as a cross-reference: the team resolved the password rules in [ADR-005](ADR-005-authentication-with-jwt.md#open-points) (Lab Worksheet 03, question 8). Each password must have at least 15 characters and at most 72 bytes in UTF-8. The check runs at start-up, and the message names the variable and the rule, never the value. [OQ-027](../constraints.md#open-questions) stays open for the client.*
- Passwords are hashed at start-up with Spring Security's delegating password encoder, which uses BCrypt and stores the prefix `{bcrypt}`. The prefix leaves I2 free to choose another adaptive function ([ADR-005](ADR-005-authentication-with-jwt.md) decision 5).
- I1 uses only `spring-security-crypto`; signing in comes in I2.
- While `MATRIZ_DEMO_DATA=true`, the passwords are hashed at every start, and only the hashes reach the migration, as Flyway placeholders. No password and no hash is in the repository. Flyway's checksum excludes the placeholders' values, so a new hash does not change the migration's checksum. The hashes are stored only when the migration runs: changing a variable later does not change an existing account. *Changed on 2026-10-30 by team decision in the I1 review (question 1): previously, “A password is hashed only for an account being inserted. Changing a variable later does not change an existing account.”*

**Contents.** All of it is fictitious, using the people and examples in the [user stories](../user-stories.md#stories) ([Assumption A-016](../constraints.md#assumptions)). The surnames, “Project Gamma”, and Daniel Rocha, the person whom the US-002 examples add to a project, come from the examples in [openapi.yaml](../../openapi.yaml), and the titles T1–T4 from [US-024](../user-stories.md#us-024--filter-tasks). *Corrected on 2026-10-30 after the I1 checks: previously this sentence did not say that Daniel himself, not only his surname, comes from openapi.yaml; the user stories do not name him.*

| Person | Identifier | Login name | Display name | Member of |
|---|---|---|---|---|
| Ana | `10000000-0000-4000-8000-000000000001` | `ana` | Ana Silva | Website Redesign (owner), Mobile App, Project Gamma (owner) |
| Rui | `10000000-0000-4000-8000-000000000002` | `rui` | Rui Martins | Website Redesign, Mobile App (owner), Project Gamma |
| Eva | `10000000-0000-4000-8000-000000000003` | `eva` | Eva Santos | Website Redesign, Website Audit (owner) |
| Daniel | `10000000-0000-4000-8000-000000000004` | `daniel` | Daniel Rocha | No project |

| Project | Identifier | State | Project Owner | Project Participants |
|---|---|---|---|---|
| Website Redesign | `20000000-0000-4000-8000-000000000001` | Active | Ana | Rui, Eva |
| Mobile App | `20000000-0000-4000-8000-000000000002` | Active | Rui | Ana |
| Website Audit | `20000000-0000-4000-8000-000000000003` | Active | Eva | — |
| Project Gamma | `20000000-0000-4000-8000-000000000004` | Completed | Ana | Rui |

| Task | Identifier | Project | State | Task priority | Assignees |
|---|---|---|---|---|---|
| T1 | `30000000-0000-4000-8000-000000000001` | Website Redesign | To do | High | Ana, Rui |
| T2 | `30000000-0000-4000-8000-000000000002` | Website Redesign | In progress | High | — |
| T3 | `30000000-0000-4000-8000-000000000003` | Website Redesign | Done | Low | — |
| T4 | `30000000-0000-4000-8000-000000000004` | Website Redesign | To do | Medium | — |
| Prepare proposal | `30000000-0000-4000-8000-000000000005` | Mobile App | To do | Medium | — |
| Review proposal | `30000000-0000-4000-8000-000000000006` | Mobile App | In progress | High | Ana |
| T1 | `30000000-0000-4000-8000-000000000007` | Project Gamma | Done | Medium | — |
| T2 | `30000000-0000-4000-8000-000000000008` | Project Gamma | Done | Medium | — |
| T3 | `30000000-0000-4000-8000-000000000009` | Project Gamma | Done | Medium | — |

| Comment | Identifier | Task | Author | Added at |
|---|---|---|---|---|
| First | `40000000-0000-4000-8000-000000000001` | Review proposal | Ana | 2026-10-29T09:30:00Z |
| Second | `40000000-0000-4000-8000-000000000002` | Review proposal | Rui | 2026-10-29T10:05:00Z |

- **Totals:** 4 people, 4 projects, 8 memberships, 9 tasks, 3 assignments, and 2 comments.
- **Different access:** Ana and Rui see 3 projects each, but not Website Audit. Eva sees 2, but not Mobile App or Project Gamma. Daniel sees none.
- **Not loaded:** deleted items, and anything that the user stories create during a demonstration.

### 11. Packages

- The base package, `com.matriz`, holds the application class and cross-cutting code: Problem Details, the check of the environment variables, and the switch that adds the demonstration data migrations to Flyway (decision 10). *The switch was added on 2026-10-30 by team decision in the I1 review (question 2).*
- The modules are `com.matriz.identity`, `com.matriz.projects`, `com.matriz.tasks`, `com.matriz.progress`, and `com.matriz.accesscontrol`. A module's interface is its top-level package; its internals are in sub-packages, the layers of [ADR-008](ADR-008-domain-driven-design.md), for example `com.matriz.identity.domain`. *Changed on 2026-10-30 by team decision in the I1 review (question 5): previously, the example was `com.matriz.identity.demo`, the package of a demonstration data loader that no longer exists.*
- The User interface is in `frontend/`.

### 12. Plan revision

The team accepted the plan revision proposed in the analysis. It is recorded, and flagged, in the [implementation plan](../implementation-plan.md#i1--walking-skeleton):

- I1 creates the whole schema. The exception is any technical data needed to end a sign-in, which stays in I2 with the open points of [ADR-005](ADR-005-authentication-with-jwt.md).
- The demonstration data moves from I2 to I1. So do the environment variables, `.env.example`, `compose.yaml`, the health check, and `spring-security-crypto`.
- I1 gets three more acceptance criteria:
  - the database is reported as up after the migrations are applied to an empty database;
  - a restart loses and duplicates nothing;
  - the repository contains no secret.
- Tests never use the demonstration data, the maximum text length stays in I3, and continuous integration no longer blocks I1.
- *Added on 2026-10-30 by team decision in the I1 review: the plan was revised again. I1 also delivers the layers and the layering check of [ADR-008](ADR-008-domain-driven-design.md) and the demonstration data migration (decision 10), and `DemoDataTests` is the only test class that uses the demonstration data, on databases of its own.*

## Consequences

- **One command.** `./mvnw verify` builds and tests everything, including the User interface and an end-to-end test. The first build downloads Node.js, the npm packages, and Chromium, so it needs internet access. Testcontainers needs a running container runtime.
- **Local secrets stay local.** Each person creates their own `.env` from `.env.example`; continuous integration needs no secret, because its tests create their own data.
- **The application carries most rules.** The database enforces only declarative rules. Rules 4–7, “at least one owner”, and the rest of rule 2 depend on the application and its tests.
- **Demonstration data is applied once per database.** A demonstration row deleted or changed by hand stays so. Resetting the local database restores the demonstration data, and is the only way to apply new demonstration passwords. Turning `MATRIZ_DEMO_DATA` off for a database that has the demonstration data, or on for one whose schema is past the demonstration migration's version, stops start-up (decision 10). *Changed on 2026-10-30 by team decision in the I1 review (questions 1–4): previously, “Demonstration data is insert-only. A deleted demonstration row comes back at the next start while `MATRIZ_DEMO_DATA=true`. A new demonstration password applies only after the local database is reset.”*
- **Later demonstration data.** It is a new migration, never an edit of an applied one, numbered as a sub-version of the schema migration it was written for, for example `V2_1` after `V2`. A database whose schema is already past that version cannot get it (decision 10). *Added on 2026-10-30 in the I1 review.*
- **One migration across modules.** Like the schema migration, the demonstration migration writes to the tables of several modules. It is sectioned by owning module, but the module boundary test does not check SQL. *Added on 2026-10-30 in the I1 review.*
- **Hashes in the database server's log.** Matriz's messages never show a failed demonstration statement, but PostgreSQL's own log in the local container may contain it, with the password hashes, though not the passwords. The project team accepts this risk. *Added on 2026-10-30 in the I1 review. The acceptance was added on 2026-10-30 by team decision (the conversation “I1 review: remaining decisions”, decision 6).*
- **Data model change.** The data model gains a technical task column (decision 8) and a dependency of Tasks on Identity (decision 9); both are flagged in the [data model](../database.md).
- **Health check outside the contract.** Its response is Spring Boot's, not a Problem Details response, and it is not covered by the contract check.

## Alternatives considered

These are the options not chosen, as presented to the team. No further rationale was recorded.

| Question | Options not chosen |
|---|---|
| Versions | Other release lines; not recorded in detail. |
| User interface packaging | A Maven multi-module build; a separate npm build with the machine's own Node.js, its output copied into the application. |
| End-to-end tests | Playwright Test in TypeScript in `frontend/`; Chromium, Firefox, and WebKit. |
| Continuous integration | Deferring it, which would leave I1 unable to complete; another service. |
| Local database | Spring Boot's Docker Compose support; PostgreSQL installed on each machine. |
| Environment variables | Real environment variables only; direnv. |
| Health check | A health operation under `/api`, added to openapi.yaml; no endpoint, with proof from logs and SQL only. |
| Identifiers | UUID version 7 generated by PostgreSQL, which embeds the creation time; sequential numbers, which can be guessed. |
| Enumerations | PostgreSQL enumerated types; lookup tables. |
| Text lengths | Maximum lengths now, as a new assumption added to the contract; `varchar(255)`. |
| Rules enforced by the database | Declarative rules plus triggers for rules 5–7; only keys and plain foreign keys. |
| Order of tasks | Not recorded. |
| Demonstration data loading | Start-up loaders in each owning module, with `INSERT … ON CONFLICT DO NOTHING`: chosen first, then replaced on the same day in the I1 review, because a demonstration row deleted by hand came back at the next start; one `demo` module, against data ownership. *Changed on 2026-10-30 by team decision in the I1 review: previously, the options not chosen were one `demo` module and Flyway Java migrations in a separate location, which mix demonstration data into the migration history. That mix is now accepted (decision 10).* |
| Demonstration migration | A Java-based Flyway migration: a Spring bean that inserts with JDBC and hashes with the password encoder; SQL that hashes with PostgreSQL's `pgcrypto` `crypt()`, which puts the plain passwords in the SQL text and in logs, and needs an extension. |
| Enabling the demonstration data | A Spring profile, `demo`; a location on the file system, outside the jar. |
| Numbering of demonstration migrations | A separate history table for the demonstration data, which could be switched on and off freely, but whose SQL must always match the latest schema. |
| Demonstration data on an existing database | Adding it only to an empty database; Flyway's `outOfOrder` option, not recommended. |
| Demonstration passwords | One variable shared by every account; supplying password hashes instead of passwords. |
| Plan revision and demonstration data | Accepting either with changes; keeping the plan; demonstration data without Daniel. |
| Packages | The organisation's reverse domain name; other names. |
| Tasks depends on Identity | Tasks depends on Projects only, either keeping the comment-author foreign key (a dependency the boundary test cannot see) or without it. |

*The rows “Demonstration migration”, “Enabling the demonstration data”, “Numbering of demonstration migrations”, and “Demonstration data on an existing database” were added on 2026-10-30 by team decision in the I1 review (questions 1–4).*

## Open points

- **Unsupported methods.** Resolved on 2026-10-30 by team decision (the conversation “I1 review: remaining decisions”, decision 3): `405` with Problem Details and an `Allow` header, documented once as a general rule in [ADR-004](ADR-004-rest-api-single-entry-point.md) (decision 9) and in the description of [openapi.yaml](../../openapi.yaml). An unknown API path gets `404` for every method. *Previously: “An unknown API path gets `404` with Problem Details for every method. A path of an existing operation called with a method it does not support gets `405` with Problem Details and an `Allow` header, which the contract does not define. Whether the contract documents `405` is to be decided by the team [Proposal].” That text was corrected on 2026-10-30 after the I1 tests; before the correction, it said that methods other than GET on unknown API paths get `405`.*
- **Demonstration data in tests.** Resolved on 2026-10-30 by team decision in the I1 review (question 2): only `DemoDataTests` enables the demonstration data, on databases of its own (decision 10). Its six tests cover applying it once with a restart that changes nothing; a blank password; a database started without it that gets it while at its schema version; rows that conflict with it; turning it off for a database that has it; and adding it once the schema is past its version. *Previously, this point proposed one test class, `DemoDataTests`, with three tests (loading twice with a restart, a blank password, and a login name that another person already has), for the team to confirm.*
- **Implementation details chosen in I1.** Accepted by the project team on 2026-10-30 (the conversation “I1 review: remaining decisions”, decision 5). None changes a decision above. *Changed on 2026-10-30 by that decision: previously “Implementation details chosen in I1, for the team's review [Proposal].”*
  - Timestamps (`deleted_at`, `created_at`) are `timestamptz`.
  - Constraint names follow `<table>_<purpose>_<kind>`, where the kind is `pk`, `uk`, `fk`, `ck`, or `ux` (a unique index), for example `membership_one_owner_ux`.
  - If a demonstration row conflicts with existing data, for example a login name that belongs to another person, start-up stops with the database's error, which names the constraint, and the remedy; none of the demonstration data is added (decision 10). *Changed on 2026-10-30 in the I1 review: previously, the loader's message named the login name, and no demonstration person was added.*
  - A migration is recognised as demonstration data by its sub-version (a version with a dot), which `MigrationNamingTests` keeps unambiguous. *Added on 2026-10-30 in the I1 review.*
  - The details of the `400`, `404`, and `415` Problem Details responses are in `src/main/resources/messages.properties`. A `500` response reveals nothing about its cause.
  - npm 11 blocks the install scripts of dependencies by default. `frontend/package.json` explicitly denies the only one requested, by `fsevents` (an optional file watcher for macOS), with `allowScripts`, so the build log shows no warning.
- **Case of login names.** Resolved on 2026-11-02 by team decision (Lab Worksheet 03, question 10): login names are case-insensitive.
  - They are stored in lower case. The migration `V3` adds a check that each stored login name equals its lower-case form, which the demonstration login names already satisfy.
  - Signing in converts the login name entered to lower case with `Locale.ROOT` before looking it up, so “Ana” and “ana” sign in the same person.
  - The database check uses PostgreSQL's `lower()`, which agrees with Java's conversion for the demonstration login names. These are ASCII; other characters are not tested.

  *Previously: “Whether login names are case-insensitive; to be decided in I2 with [OQ-027](../constraints.md#open-questions).”*
- **Maximum text lengths.** Agreed in I3 and added by a new migration ([OQ-003](../constraints.md#open-questions), [OQ-024](../constraints.md#open-questions)). *Updated on 2026-11-02 by team decision (the conversation “Implement project management”, question 4): for projects, the migration `V4` limits the name to 100 characters and adds the description, limited to 1000 ([Assumption A-031](../constraints.md#assumptions)); task titles and comment text stay open.* *Updated on 2026-11-05 by team decision (the conversation “Implement task management”, questions 2 to 4): the migration `V5` limits the task title to 150 characters and adds the task description, limited to 2000 ([Assumption A-033](../constraints.md#assumptions)); comment text stays open.*
- **Indexes for performance.** Added from I4 onwards, when the queries are known; no volumes are known ([OQ-010](../constraints.md#open-questions)) [Proposal].
- **Health check after I2.** Resolved on 2026-11-02 by team decision (Lab Worksheet 03, question 11): `/actuator/health` stays reachable without signing in, unchanged (decision 7). Its response shows the overall status and each component's status, with no details, and every other Actuator path still gets 404. The access tokens are read only under `/api`, so a token sent to the health check is ignored. *Previously: “Whether it stays reachable without signing in, once I2 adds Spring Security; to be decided in I2 [Proposal].”*
- **Browsers.** Chromium only until [OQ-021](../constraints.md#open-questions) is answered.
- **Hosting.** Remains open ([OQ-020](../constraints.md#open-questions)).
