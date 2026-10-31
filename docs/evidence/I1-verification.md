# Matriz — Increment I1 verification record (2026-10-30)

**Status:** Evidence for the project team's review of increment [I1](../implementation-plan.md#i1--walking-skeleton), which is Awaiting review. *Created on 2026-10-30 by decision of the project team, taken in the I1 implementation conversation: there is no commit or CI run yet, so the status of I1 links to this record. The record is a snapshot and is not updated; the commit and the CI run are linked from the plan after the push.* *Added on 2026-10-30 by team decision in the I1 review (proposal d): section 9, which records the checks after the changes the review requested ([ADR-008](../adr/ADR-008-domain-driven-design.md); [ADR-007](../adr/ADR-007-build-persistence-and-demonstration-data.md), decision 10, amended); previously the record ended at section 8. Sections 1–8 are unchanged: they record the version first reviewed, and where they mention the start-up loaders, `DemoPeopleLoader`, `DemoProjects`, or the package `com.matriz.identity.demo`, they describe code that no longer exists.* *Added on 2026-10-30 by team decision (the conversation “I1 review: remaining decisions”): section 10, which records the checks after those decisions; previously the record ended at section 9. Decision 8 of that conversation also changed section 8, section 9.8, and the summary's row 8, each flagged where changed. Sections 1–9 are otherwise unchanged; sections 9.1 and 9.2 show the domain rule of `LayeringTests` as it was before decision 9.* *Changed on 2026-10-31 by team decision (the conversation “I1 review: last decisions”, decisions 1 and 2): sections 10.1, 10.2, and 10.6 and the summary's row 10 record the team's decisions on the JVM's line about class data sharing and on the limit of the domain rule, each flagged where changed. No check was run again.*

**Environment:** macOS on arm64; JDK 25.0.5 (Temurin); Maven 3.9.16 through the wrapper; Node.js 24.22.2, installed by the build; Docker 28.3.2 with Compose; PostgreSQL 18.6, both in [compose.yaml](../../compose.yaml) and in the tests. The commands are those of the [README](../../README.md#run-matriz-locally). Maven ran with the trust-store setting in the README's troubleshooting note, which this network needs. Outputs are trimmed: `…` marks omitted text, and timestamps and process numbers are removed.

## Summary

| # | Check | I1 acceptance criterion or verification | Result |
|---|---|---|---|
| 1 | [Build and automated tests](#1-build-and-automated-tests) | The boundary test; integration tests against PostgreSQL in a container; responses match openapi.yaml; an end-to-end test of the User interface shell | Passed: 35 tests, 0 failures |
| 2 | [Clean checkout](#2-clean-checkout) | The application builds and starts from a clean checkout with the documented commands | Passed |
| 3 | [Start from an empty database](#3-start-from-an-empty-database) | The migrations are applied, and the health check reports the database as up | Passed |
| 4 | [Restart after changes by hand](#4-restart-after-changes-by-hand) | Restarting deletes, overwrites, and duplicates nothing | Passed |
| 5 | [Missing environment variables](#5-missing-environment-variables) | A missing variable stops start-up with a clear message | Passed |
| 6 | [Boundary violations](#6-boundary-violations) | The boundary test fails when a module uses another module's internals | Passed: 3 violations detected, then reverted |
| 7 | [Secrets and personal data](#7-secrets-and-personal-data) | No secret, password, or real personal data in the repository; `.env` ignored by git | Passed locally; GitHub's secret scanning not run |
| 8 | [Not yet done](#8-not-yet-done) | CI on push. *Changed on 2026-10-30 by team decision: previously also “a second person follows the README”.* | Open |
| 9 | [Re-verification after the team's review](#9-re-verification-after-the-teams-review) | The checks above, repeated after the requested changes, plus: the layering check fails on a violation; a demonstration row deleted by hand stays deleted; without the demonstration data, and in the test databases, there is none | Passed: 47 tests, 0 failures; 1 layering violation detected, then removed |
| 10 | [Re-verification after the review's remaining decisions](#10-re-verification-after-the-reviews-remaining-decisions) | The build after the team's remaining decisions: Mockito's warning and the warnings about an agent loaded dynamically are gone; the domain may use Jakarta Persistence's mapping annotations, and nothing else from it | Passed: 47 tests, 0 failures; the layering check accepted the mapping annotations and detected 2 violations, then the class was removed. The JVM's line about class data sharing remains: a known, harmless line, left by team decision on 2026-10-31. *Changed on 2026-10-31: previously ended at “remains”.* |

## 1. Build and automated tests

```text
$ ./mvnw -B clean verify
…
Tests run: 1, Failures: 0, Errors: 0, Skipped: 0 … in com.matriz.UserInterfaceTests
Tests run: 11, Failures: 0, Errors: 0, Skipped: 0 … in com.matriz.ProblemDetailsTests
Tests run: 12, Failures: 0, Errors: 0, Skipped: 0 … in com.matriz.SchemaMigrationTests
Tests run: 3, Failures: 0, Errors: 0, Skipped: 0 … in com.matriz.DemoDataTests
Tests run: 4, Failures: 0, Errors: 0, Skipped: 0 … in com.matriz.EnvironmentVariablesTests
Tests run: 2, Failures: 0, Errors: 0, Skipped: 0 … in com.matriz.ModuleBoundaryTests
Tests run: 2, Failures: 0, Errors: 0, Skipped: 0 … in com.matriz.HealthCheckTests
Tests run: 35, Failures: 0, Errors: 0, Skipped: 0
BUILD SUCCESS
Total time:  20.771 s
```

| Test class | What it checks |
|---|---|
| [SchemaMigrationTests](../../src/test/java/com/matriz/SchemaMigrationTests.java) | The first migration creates the whole data model on an empty database. Hibernate only validates the schema, and Flyway cannot clean it. The database enforces the rules decided in [ADR-007, decision 8](../adr/ADR-007-build-persistence-and-demonstration-data.md#8-physical-schema): nonempty text, required values, enumeration codes, unique and case-sensitive login names, at most one Project Owner, assignments within the task's project and its members, existing references, and no cascades. |
| [HealthCheckTests](../../src/test/java/com/matriz/HealthCheckTests.java) | The health check reports the application and its database as up, without details; no other Actuator endpoint is exposed. |
| [EnvironmentVariablesTests](../../src/test/java/com/matriz/EnvironmentVariablesTests.java) | Start-up stops when a required variable is missing or blank, or when `MATRIZ_DEMO_DATA` is neither true nor false; the message names the variables only. |
| [DemoDataTests](../../src/test/java/com/matriz/DemoDataTests.java) | The demonstration data is loaded once, and restarting deletes, overwrites, and duplicates nothing. A blank password, or a login name that another person already has, stops start-up. |
| [ProblemDetailsTests](../../src/test/java/com/matriz/ProblemDetailsTests.java) | Errors are Problem Details that match openapi.yaml: an unknown path (`404`, for every method), an unsupported method (`405`), another media type (`415`), malformed JSON (`400`), and an unexpected error (`500`, which reveals nothing about its cause). |
| [ModuleBoundaryTests](../../src/test/java/com/matriz/ModuleBoundaryTests.java) | The five modules are detected, and each keeps to its allowed dependencies. |
| [UserInterfaceTests](../../src/test/java/com/matriz/UserInterfaceTests.java) | End to end: Chromium opens the User interface shell that the application serves. |

The `ERROR` lines in the build log come from tests that cause failures on purpose: the `500` response, the blank password, the login name already taken, and the missing or invalid variables. The JDK also warns once that a Java agent was loaded dynamically, by Mockito; this is reported to the team as an open item.

The packaging command of README step 3 skips the tests and the Chromium installation:

```text
$ ./mvnw -B clean package -DskipTests
…
[INFO] --- exec:3.6.4:exec (install-playwright-chromium) @ matriz ---
[INFO] skipping execute as per configuration
…
BUILD SUCCESS
Total time:  5.533 s
```

## 2. Clean checkout

The 66 files that a commit would have contained before this record was written (`git ls-files --cached --others --exclude-standard`) were copied into an empty temporary directory, without `.env`, `target/`, `frontend/node/`, or `frontend/node_modules/`. In that directory:

```text
$ ./mvnw -B verify
…
Installing node version v24.22.2
…
added 24 packages, and audited 25 packages in 779ms
…
Tests run: 35, Failures: 0, Errors: 0, Skipped: 0
BUILD SUCCESS
Total time:  52.275 s
$ cp .env.example .env
$ docker compose up -d --wait
 Container matriz-postgres-1  Running
 Container matriz-postgres-1  Waiting
 Container matriz-postgres-1  Healthy
$ java -jar target/matriz-0.1.0-SNAPSHOT.jar
… Schema "public" is up to date. No migration necessary.
… Started MatrizApplication in 1.64 seconds (process running for 1.861)
$ curl http://localhost:8080/actuator/health
{"components":{"db":{"status":"UP"},"diskSpace":{"status":"UP"},"livenessState":{"status":"UP"},"ping":{"status":"UP"},"readinessState":{"status":"UP"},"ssl":{"status":"UP"}},"groups":["liveness","readiness"],"status":"UP"}
$ curl -s -o /dev/null -w '%{http_code} %{content_type}\n' http://localhost:8080/
200 text/html;charset=UTF-8
```

The database was already running from check 3. Compose reused it, because the Compose project is always named `matriz`. The example file sets `MATRIZ_DEMO_DATA=false`, so no demonstration data was loaded.

## 3. Start from an empty database

Before the start, there was no `.env`, no Matriz container or volume, no process listening on ports 5432 or 8080, and no `MATRIZ_` variable in the environment. `.env` was copied from `.env.example`, with `MATRIZ_DEMO_DATA=true` and four fictitious local passwords, which are not recorded here.

```text
$ docker compose up -d --wait
… Container matriz-postgres-1  Healthy
$ java -jar target/matriz-0.1.0-SNAPSHOT.jar
… Creating Schema History table "public"."flyway_schema_history" ...
… Migrating schema "public" to version "1 - create schema"
… Successfully applied 1 migration to schema "public", now at version v1 (execution time 00:00.013s)
… Started MatrizApplication in 1.884 seconds (process running for 2.116)
… Demonstration data: 4 of 4 people added; existing people were left unchanged.
… Demonstration data: 4 of 4 projects and 8 of 8 memberships added; existing rows were left unchanged.
… Demonstration data: 9 of 9 tasks, 3 of 3 assignments, and 2 of 2 comments added; existing rows were left unchanged.
$ curl http://localhost:8080/actuator/health
{"components":{"db":{"status":"UP"},…},"groups":["liveness","readiness"],"status":"UP"}
```

- The log had no `WARN` or `ERROR` line.
- `GET /` returned `200` with `text/html`. `GET /api/unknown` returned `404` with `application/problem+json` and the body `{"detail":"The requested resource does not exist.","instance":"/api/unknown","status":404,"title":"Not Found"}`.
- With psql, the migration history held one row, `1|create schema|t`. The tables held 4 people, 4 projects, 8 memberships, 9 tasks, 3 assignments, and 2 comments, as listed in [ADR-007, decision 10](../adr/ADR-007-build-persistence-and-demonstration-data.md#10-demonstration-data); Daniel has no membership.

## 4. Restart after changes by hand

With psql, a person was added by hand (login name `hand-added`, display name “Added by hand”), and the project Website Redesign was renamed “Website Redesign (renamed by hand)”. The application was stopped and started again:

```text
… Schema "public" is up to date. No migration necessary.
… Started MatrizApplication in 1.838 seconds (process running for 2.061)
… Demonstration data: 0 of 4 people added; existing people were left unchanged.
… Demonstration data: 0 of 4 projects and 0 of 8 memberships added; existing rows were left unchanged.
… Demonstration data: 0 of 9 tasks, 0 of 3 assignments, and 0 of 2 comments added; existing rows were left unchanged.
```

The jar of the final build was later started against the same database, with the same log lines and the health check up. Afterwards, psql showed the row counts, the two changes made by hand, and no duplicates. A duplicate is a group of rows with the same login name, project name, membership, task title within a project, assignment, or comment.

```text
migrations|1
people|5
projects|4
memberships|8
tasks|9
assignments|3
comments|2
hand-added person|1
renamed project|1
```

```text
person|0
project|0
membership|0
task|0
assignment|0
comment|0
```

## 5. Missing environment variables

From a directory without `.env`, with no `MATRIZ_` variable set, the application stopped with exit status 1:

```text
$ java -jar …/target/matriz-0.1.0-SNAPSHOT.jar
…
APPLICATION FAILED TO START
***************************

Description:

Matriz cannot start. These environment variables are missing or blank: MATRIZ_DB_URL, MATRIZ_DB_USER, MATRIZ_DB_PASSWORD.

Action:

Set each variable in the environment, or in the file .env in the directory from which Matriz is started: copy .env.example to .env and fill it in. See README.md.
```

With the demonstration data on, a blank environment variable overrides `.env`, and start-up stops with exit status 1, naming only that variable:

```text
$ MATRIZ_DEMO_PASSWORD_EVA= java -jar target/matriz-0.1.0-SNAPSHOT.jar
…
Matriz cannot start. These environment variables are missing or blank: MATRIZ_DEMO_PASSWORD_EVA.
```

Compose refuses to start the database without the variables, with exit status 1:

```text
$ docker compose --env-file /dev/null config
error while interpolating services.postgres.environment.POSTGRES_USER: required variable MATRIZ_DB_USER is missing a value: set MATRIZ_DB_USER in .env (see .env.example)
```

No log of these runs contains a password.

## 6. Boundary violations

Each violation was introduced, the build was run, and the change was reverted. The files were then compared byte for byte with the originals.

1. Projects code referring to `DemoPeopleLoader`, which is package-private in Identity: the compiler rejected it.
2. Identity code using `com.matriz.projects.DemoProjects`:

   ```text
   ModuleBoundaryTests.everyModuleKeepsToItsAllowedDependencies:20 » Violations - Cycle detected: Slice identity -> …
   - Module 'identity' depends on module 'projects' via com.matriz.identity.demo.DemoPeopleLoader -> com.matriz.projects.DemoProjects. Allowed targets: none.
   BUILD FAILURE
   ```

3. Projects code using a temporary public type in Identity's internal package `com.matriz.identity.demo`:

   ```text
   ModuleBoundaryTests.everyModuleKeepsToItsAllowedDependencies:20 » Violations - Module 'projects' depends on non-exposed type com.matriz.identity.demo.BoundaryProbe within module 'identity'!
   BUILD FAILURE
   ```

## 7. Secrets and personal data

```text
$ git check-ignore -v .env
.gitignore:30:.env	.env
```

The 67 files that a commit would contain, including this record, were searched:

| Search | Files found |
|---|---|
| The password values of the local `.env` | 1: `.env.example`, whose fictitious database password the local `.env` kept unchanged |
| Private keys and access-token patterns | 0 |
| The hosts of the private package registries configured on the machine | 0 |
| Paths in local home directories | 0 |
| E-mail addresses | 0 |
| The name of the person who ran the checks | 0 |

- The only people named are the fictitious people of the user stories and of the examples in [openapi.yaml](../../openapi.yaml).
- All 48 packages in `frontend/package-lock.json` resolve from `registry.npmjs.org`.
- No log of the runs in this record contains a password value.
- GitHub's secret scanning was not run, because the repository could not be reached from the session.

## 8. Not yet done

- **Continuous integration.** [ci.yml](../../.github/workflows/ci.yml) parses, and the action versions it uses exist (actions/checkout v7, actions/setup-java v6). It runs only after a push.
- *Removed on 2026-10-30 by team decision (the conversation “I1 review: remaining decisions”, decision 8): previously “**A second person follows the README instructions.** This is a manual verification in the plan.” The team is one person; the [clean-checkout check](#2-clean-checkout) and the continuous integration run after the push replace it.*
- **GitHub's secret scanning** of the repository.

## 9. Re-verification after the team's review

On 2026-10-30, the project team's review of I1 requested two changes: the demonstration data is added by a Flyway migration instead of the start-up loaders ([ADR-007, decision 10](../adr/ADR-007-build-persistence-and-demonstration-data.md#10-demonstration-data), amended), and the code inside each module follows domain-driven design ([ADR-008](../adr/ADR-008-domain-driven-design.md)). These checks were run after the changes, in the environment described above. As decided in the review (question 11), the run-time checks used two new databases of their own, so the `matriz` database was not used.

### 9.1 Build and automated tests

```text
$ ./mvnw -B clean verify
…
Tests run: 1, Failures: 0, Errors: 0, Skipped: 0 -- in com.matriz.UserInterfaceTests
Tests run: 11, Failures: 0, Errors: 0, Skipped: 0 -- in com.matriz.ProblemDetailsTests
Tests run: 2, Failures: 0, Errors: 0, Skipped: 0 -- in com.matriz.MigrationNamingTests
Tests run: 13, Failures: 0, Errors: 0, Skipped: 0 -- in com.matriz.SchemaMigrationTests
Tests run: 6, Failures: 0, Errors: 0, Skipped: 0 -- in com.matriz.DemoDataTests
Tests run: 5, Failures: 0, Errors: 0, Skipped: 0 -- in com.matriz.EnvironmentVariablesTests
Tests run: 2, Failures: 0, Errors: 0, Skipped: 0 -- in com.matriz.ModuleBoundaryTests
Tests run: 2, Failures: 0, Errors: 0, Skipped: 0 -- in com.matriz.HealthCheckTests
Tests run: 5, Failures: 0, Errors: 0, Skipped: 0 -- in com.matriz.LayeringTests
Tests run: 47, Failures: 0, Errors: 0, Skipped: 0
BUILD SUCCESS
Total time:  50.190 s
```

| Test class | Change since [section 1](#1-build-and-automated-tests) |
|---|---|
| [LayeringTests](../../src/test/java/com/matriz/LayeringTests.java) | New, 5 tests. The layers inside each module follow ADR-008: the domain is plain Java and uses no other layer; the application layer uses neither infrastructure nor web; nothing outside the web layer uses it; the web layer does not use infrastructure; a module's top-level package, its API, uses no layer. In I1 the layers hold no classes yet, so the rules accept them. |
| [MigrationNamingTests](../../src/test/java/com/matriz/MigrationNamingTests.java) | New, 2 tests. Schema migrations have whole version numbers and no placeholders; each demonstration migration is a sub-version of an existing schema migration ([ADR-007, decision 8](../adr/ADR-007-build-persistence-and-demonstration-data.md#8-physical-schema)). |
| [DemoDataTests](../../src/test/java/com/matriz/DemoDataTests.java) | Rewritten, from 3 to 6 tests, each in a new database. The demonstration migration is applied once, and a restart changes nothing. A blank password stops start-up before the database is used. A database started without the demonstration data can get it while its schema is still at version 1. Rows that conflict with the demonstration data stop start-up, and none of it is added. Turning the demonstration data off for a database that has it stops start-up. The demonstration data cannot be added once the schema is past its version. |
| [SchemaMigrationTests](../../src/test/java/com/matriz/SchemaMigrationTests.java) | One new test, from 12 to 13: the test databases have no demonstration data (see [9.6](#96-test-databases)). |
| [EnvironmentVariablesTests](../../src/test/java/com/matriz/EnvironmentVariablesTests.java) | One new test, from 4 to 5: the demonstration passwords are required only when the demonstration data is on. |

The `ERROR` lines in the build log come from tests that cause failures on purpose: 6 start-ups stopped by EnvironmentVariablesTests and DemoDataTests, Flyway's report that the demonstration migration failed and was rolled back (the conflicting rows), and the `500` response. Flyway's report does not include the SQL statement. The build log contains no password hash and none of the tests' passwords.

### 9.2 A layering violation fails the build

A temporary class `src/main/java/com/matriz/identity/domain/LayeringViolation.java` was added, with a field of type `jakarta.persistence.EntityManager`:

```text
$ ./mvnw -B verify
…
Tests run: 5, Failures: 1, Errors: 0, Skipped: 0, … <<< FAILURE! -- in com.matriz.LayeringTests
com.matriz.LayeringTests.theDomainIsPlainJavaAndUsesNoOtherLayer -- … <<< FAILURE!
…
Architecture Violation [Priority: MEDIUM] - Rule 'no classes that reside in a package 'com.matriz.*.domain..' should depend on classes that reside in any package ['com.matriz.*.application..', 'com.matriz.*.infrastructure..', 'com.matriz.*.web..', 'jakarta.persistence..', 'org.springframework..', 'org.hibernate..'], because the domain is plain Java (ADR-008)' was violated (1 times):
Field <com.matriz.identity.domain.LayeringViolation.entityManager> has type <jakarta.persistence.EntityManager> in (LayeringViolation.java:0)
…
Tests run: 47, Failures: 1, Errors: 0, Skipped: 0
BUILD FAILURE
```

The class was then deleted; `com.matriz.identity.domain` holds only its `package-info.java` again, and the build of [9.1](#91-build-and-automated-tests) ran afterwards.

### 9.3 Start from an empty database, with the demonstration data

The Compose database was started, and two empty databases were created in it: `matriz_review_demo` and `matriz_review_nodemo`. `.env` had `MATRIZ_DEMO_DATA=true` and four fictitious local passwords, which are not recorded here.

```text
$ docker compose up -d --wait
$ java -jar target/matriz-0.1.0-SNAPSHOT.jar --MATRIZ_DB_URL=jdbc:postgresql://localhost:5432/matriz_review_demo --MATRIZ_DEMO_DATA=true
… Demonstration data is on: the migrations in classpath:db/demo are applied once to this database.
… Successfully validated 2 migrations (execution time 00:00.002s)
… Creating Schema History table "public"."flyway_schema_history" ...
… Current version of schema "public": << Empty Schema >>
… Migrating schema "public" to version "1 - create schema"
… Migrating schema "public" to version "1.1 - demonstration data"
… Successfully applied 2 migrations to schema "public", now at version v1.1 (execution time 00:00.022s)
… Started MatrizApplication in 2.01 seconds (process running for 2.222)
$ curl http://localhost:8080/actuator/health
{"components":{"db":{"status":"UP"},…},"status":"UP"}
```

- The log had no `WARN` or `ERROR` line.
- With psql, the migration history held two rows, `1|1|create schema|t` and `2|1.1|demonstration data|t`. The tables held 4 people, 4 projects, 8 memberships, 9 tasks, 3 assignments, and 2 comments, as listed in [ADR-007, decision 10](../adr/ADR-007-build-persistence-and-demonstration-data.md#10-demonstration-data). Every password hash starts with `{bcrypt}`.

### 9.4 Restart after changes by hand

With psql, a person was added by hand (login name `hand-added`, display name “Added by hand”), and the demonstration comment `40000000-0000-4000-8000-000000000002` was deleted. The application was stopped and started again with the same command:

```text
… Demonstration data is on: the migrations in classpath:db/demo are applied once to this database.
… Successfully validated 2 migrations (execution time 00:00.003s)
… Current version of schema "public": 1.1
… Schema "public" is up to date. No migration necessary.
… Started MatrizApplication in 1.912 seconds (process running for 2.12)
```

The health check reported the database as up, the migration history was unchanged, and psql showed:

```text
people|5
projects|4
memberships|8
tasks|9
assignments|3
comments|1
```

- No login name occurs twice. The person added by hand still exists.
- The deleted demonstration comment did not come back (0 rows).

The application was then started on the same database with `MATRIZ_DEMO_DATA=false`. It stopped with exit status 1, ran no migration, and changed no data:

```text
APPLICATION FAILED TO START
***************************

Description:

Matriz cannot start. The demonstration data (migration 1.1) was added to this database, but MATRIZ_DEMO_DATA is not true. The setting is fixed for each database.

Action:

Set MATRIZ_DEMO_DATA=true and the demonstration passwords (MATRIZ_DEMO_PASSWORD_ANA, MATRIZ_DEMO_PASSWORD_RUI, MATRIZ_DEMO_PASSWORD_EVA, MATRIZ_DEMO_PASSWORD_DANIEL), or reset the local database as described in README.md (this deletes all its data).
```

### 9.5 Start from an empty database, without the demonstration data

```text
$ java -jar target/matriz-0.1.0-SNAPSHOT.jar --MATRIZ_DB_URL=jdbc:postgresql://localhost:5432/matriz_review_nodemo --MATRIZ_DEMO_DATA=false
… Successfully validated 1 migration (execution time 00:00.003s)
… Creating Schema History table "public"."flyway_schema_history" ...
… Current version of schema "public": << Empty Schema >>
… Migrating schema "public" to version "1 - create schema"
… Successfully applied 1 migration to schema "public", now at version v1 (execution time 00:00.013s)
… Started MatrizApplication in 1.713 seconds (process running for 1.918)
```

The log had no `WARN` or `ERROR` line, and the health check reported the database as up. With psql, the migration history held only version 1, and every table was empty.

### 9.6 Test databases

The automated tests never get the demonstration data, except DemoDataTests, which turns it on in new databases of its own ([ADR-007, decision 10](../adr/ADR-007-build-persistence-and-demonstration-data.md#10-demonstration-data)). `SchemaMigrationTests.testDatabasesHaveNoDemonstrationData` checks that Flyway reads only `classpath:db/migration` and that the migration history holds only version 1; it passed in [9.1](#91-build-and-automated-tests).

### 9.7 Secrets and personal data

```text
$ git check-ignore -v .env
.gitignore:30:.env	.env
```

The files that a commit would contain, tracked and untracked but not ignored, were searched again:

| Search | Files found |
|---|---|
| The four demonstration password values of the local `.env` | 0 |
| The database password value of the local `.env` | 1: `.env.example`, whose fictitious value the local `.env` kept unchanged |
| BCrypt hashes, private keys, and access-token patterns | 0 |
| The hosts of the private package registries configured on the machine | 0 |
| Paths in local home directories, and the name of the person who ran the checks | 0 |
| E-mail addresses and telephone numbers | 0 |

- The only people named in `src` are the fictitious people of [ADR-007, decision 10](../adr/ADR-007-build-persistence-and-demonstration-data.md#10-demonstration-data): Ana Silva, Rui Martins, Eva Santos, and Daniel Rocha. Every password written in the tests starts with `fictitious-`.
- The logs of 9.1 to 9.5 contain no password hash, no password value of the local `.env`, and none of the passwords written in the tests.
- Every host named is public or reserved: the Maven and Apache sites (in the Maven wrapper and `pom.xml`), `registry.npmjs.org` (all 48 packages of `frontend/package-lock.json`), `www.w3.org`, `www.java.com`, `www.rfc-editor.org`, `localhost`, and the reserved `matriz.invalid`.
- GitHub's secret scanning was not run.

### 9.8 Clean-up and still open

- Both verification databases were dropped, and the Compose database was stopped with `docker compose stop`.
- Before these checks, Docker had no Matriz container or volume: the volume used in sections 2–4 had been removed outside the session. `docker compose up` created a new one, whose `matriz` database is empty.
- [Section 8](#8-not-yet-done) is still open: continuous integration on push and GitHub's secret scanning. *Changed on 2026-10-30 by team decision (the conversation “I1 review: remaining decisions”, decision 8): previously also “a second person following the README”.*

## 10. Re-verification after the review's remaining decisions

On 2026-10-30, the project team decided the remaining points of its I1 review, in the conversation “I1 review: remaining decisions”. The row “Remaining decisions of the I1 review” of the [plan's decisions table](../implementation-plan.md#decisions-affecting-the-increments) lists the nine decisions and where each is recorded. Two of them change the build or the code:

- decision 4: Surefire loads Mockito as a Java agent ([ADR-007, decision 2](../adr/ADR-007-build-persistence-and-demonstration-data.md#2-build-and-user-interface-packaging-open-point-of-adr-003));
- decision 9: the domain classes carry Jakarta Persistence's mapping annotations ([ADR-008, decision 4](../adr/ADR-008-domain-driven-design.md#4-persistence)), so the domain rule of `LayeringTests` changed.

The other decisions change documents only. Besides these two changes, only the Javadoc comments of the `package-info.java` files of the `domain` and `infrastructure` layers changed. No migration, configuration, or application class changed, so the start-up and restart checks of [9.3](#93-start-from-an-empty-database-with-the-demonstration-data) to [9.5](#95-start-from-an-empty-database-without-the-demonstration-data) were not repeated. Sections 9.1 and 9.2 show the domain rule as it was before decision 9.

### 10.1 Mockito loaded as a Java agent

[pom.xml](../../pom.xml) follows [Mockito's documented Maven setup](https://javadoc.io/doc/org.mockito/mockito-core/latest/org.mockito/org/mockito/Mockito.html#0.3): the `properties` goal of the Maven Dependency Plugin gives the path of each dependency, and Surefire starts the test JVM with `-javaagent:` and the path of `mockito-core`.

Before the change:

```text
$ ./mvnw -B test -Dtest=HealthCheckTests
…
Mockito is currently self-attaching to enable the inline-mock-maker. This will no longer work in future releases of the JDK. Please add Mockito as an agent to your build as described in Mockito's documentation: https://javadoc.io/doc/org.mockito/mockito-core/latest/org.mockito/org/mockito/Mockito.html#0.3
OpenJDK 64-Bit Server VM warning: Sharing is only supported for boot loader classes because bootstrap classpath has been appended
WARNING: A Java agent has been loaded dynamically (…/byte-buddy-agent-1.18.15.jar)
WARNING: If a serviceability tool is in use, please run with -XX:+EnableDynamicAgentLoading to hide this warning
WARNING: If a serviceability tool is not in use, please run with -Djdk.instrument.traceUsage for more information
WARNING: Dynamic loading of agents will be disallowed by default in a future release
…
Tests run: 2, Failures: 0, Errors: 0, Skipped: 0
BUILD SUCCESS
```

After the change, the same command passed its 2 tests again, and the full build of [10.3](#103-build-and-automated-tests) ran a new step, `[INFO] --- dependency:3.10.0:properties (dependency-paths) @ matriz ---`. Lines counted in each log:

| Line | Before, `HealthCheckTests` | After, `HealthCheckTests` | After, full build |
|---|---|---|---|
| Mockito's “self-attaching” message | 1 | 0 | 0 |
| `WARNING:` lines: the agent loaded dynamically, and the three that follow it | 4 | 0 | 0 |
| “Sharing is only supported for boot loader classes because bootstrap classpath has been appended” | 1 | 1 | 1 |

The last line remains. The JVM prints it when classes are added to the boot loader's class path while class data sharing is on. In `mockito-core` 5.23.0, the class that adds to it (`appendToBootstrapClassLoaderSearch`) is `InlineDelegateByteBuddyMockMaker`, the inline mock maker. The line was printed before the change too, and it is not one of the warnings that decision 4 addresses (see [10.6](#106-still-open)). *Added on 2026-10-31 by team decision (the conversation “I1 review: last decisions”, decision 1): the project team leaves this line, and records it as a known, harmless line. Class data sharing affects only the JVM's start-up time and memory use, and the tests passed with the line both before and after decision 4.*

### 10.2 The domain may use Jakarta Persistence's mapping annotations, and nothing else from it

The domain rule of [LayeringTests](../../src/test/java/com/matriz/LayeringTests.java), now named `theDomainUsesNoOtherLayerAndOnlyTheMappingAnnotationsOfJakartaPersistence`, forbids in the `domain` packages the other layers, Spring, Hibernate, and every class of Jakarta Persistence that is neither an annotation nor an enumeration ([ADR-008, decision 5](../adr/ADR-008-domain-driven-design.md#5-layering-check); this limit was accepted by the project team on 2026-10-31). *Changed on 2026-10-31 by team decision (the conversation “I1 review: last decisions”, decision 2): previously “this limit is a proposal for the team's review”.* A temporary class, `src/main/java/com/matriz/identity/domain/LayeringProbe.java`, checked it in two versions.

First, annotated with `@Entity` and `@Table`, with an `@Id` field, an `@Enumerated(EnumType.STRING)` field, and a protected constructor without arguments:

```text
$ ./mvnw -B test -Dtest=LayeringTests
…
Tests run: 5, Failures: 0, Errors: 0, Skipped: 0, … -- in com.matriz.LayeringTests
…
BUILD SUCCESS
```

Then, with a Spring annotation, `@Component`, and a field of type `jakarta.persistence.EntityManager` added:

```text
$ ./mvnw -B test -Dtest=LayeringTests
…
Tests run: 5, Failures: 1, Errors: 0, Skipped: 0, … <<< FAILURE! -- in com.matriz.LayeringTests
…
Architecture Violation [Priority: MEDIUM] - Rule 'no classes that reside in a package 'com.matriz.*.domain..' should depend on classes that reside in any package ['com.matriz.*.application..', 'com.matriz.*.infrastructure..', 'com.matriz.*.web..', 'org.springframework..', 'org.hibernate..'] or are Jakarta Persistence classes other than annotations and enumerations, because the domain uses no other layer, nor Spring or Hibernate, and of Jakarta Persistence only its mapping annotations (ADR-008)' was violated (2 times):
Class <com.matriz.identity.domain.LayeringProbe> is annotated with <org.springframework.stereotype.Component> in (LayeringProbe.java:0)
Field <com.matriz.identity.domain.LayeringProbe.entityManager> has type <jakarta.persistence.EntityManager> in (LayeringProbe.java:0)
…
Tests run: 5, Failures: 1, Errors: 0, Skipped: 0
BUILD FAILURE
```

The mapping annotations and `EnumType` were not reported. The class was then deleted; `com.matriz.identity.domain` holds only its `package-info.java` again, and the build of [10.3](#103-build-and-automated-tests) ran afterwards.

### 10.3 Build and automated tests

```text
$ ./mvnw -B clean verify
…
[INFO] --- dependency:3.10.0:properties (dependency-paths) @ matriz ---
…
Tests run: 1, Failures: 0, Errors: 0, Skipped: 0 -- in com.matriz.UserInterfaceTests
Tests run: 11, Failures: 0, Errors: 0, Skipped: 0 -- in com.matriz.ProblemDetailsTests
Tests run: 2, Failures: 0, Errors: 0, Skipped: 0 -- in com.matriz.MigrationNamingTests
Tests run: 13, Failures: 0, Errors: 0, Skipped: 0 -- in com.matriz.SchemaMigrationTests
Tests run: 6, Failures: 0, Errors: 0, Skipped: 0 -- in com.matriz.DemoDataTests
Tests run: 5, Failures: 0, Errors: 0, Skipped: 0 -- in com.matriz.EnvironmentVariablesTests
Tests run: 2, Failures: 0, Errors: 0, Skipped: 0 -- in com.matriz.ModuleBoundaryTests
Tests run: 2, Failures: 0, Errors: 0, Skipped: 0 -- in com.matriz.HealthCheckTests
Tests run: 5, Failures: 0, Errors: 0, Skipped: 0 -- in com.matriz.LayeringTests
Tests run: 47, Failures: 0, Errors: 0, Skipped: 0
BUILD SUCCESS
Total time:  50.895 s
```

- The test classes and their counts are those of [9.1](#91-build-and-automated-tests); only the domain rule of `LayeringTests` changed (10.2).
- `ProblemDetailsTests.aMethodThatAnOperationDoesNotSupportIsNotAllowed` checks decision 3: `405` as Problem Details, with an `Allow` header.
- The log has none of the lines that 10.1 removed, and no `WARNING:` line.
- Its `ERROR` lines come from the same tests that cause failures on purpose as in 9.1.
- None of the logs of this section contains a password hash or any of the tests' passwords.

### 10.4 Secrets and personal data

```text
$ git check-ignore -v .env
.gitignore:30:.env	.env
```

The 89 files that a commit would contain, tracked and untracked but not ignored, were searched again:

| Search | Files found |
|---|---|
| The four demonstration password values of the local `.env` | 0 |
| The database password value of the local `.env` | 1: `.env.example`, whose fictitious value the local `.env` kept unchanged |
| BCrypt hashes, private keys, and access-token patterns | 0 |
| The hosts of the private package registries configured on the machine | 0 |
| Paths in local home directories, and the name of the person who ran the checks | 0 |
| E-mail addresses and telephone numbers | 0 |

GitHub's secret scanning was not run.

### 10.5 Documents

- Every relative link in `README.md`, `AGENTS.md`, and the Markdown files of `docs/` leads to an existing file and, where it names a heading, to an existing heading.
- `git diff --check` reports no whitespace error.

### 10.6 Still open

- [Section 8](#8-not-yet-done): continuous integration on push, and GitHub's secret scanning.
- *Resolved on 2026-10-31 by team decision (the conversation “I1 review: last decisions”, decision 1): the JVM's line about class data sharing is left in the test runs, as a known, harmless line ([10.1](#101-mockito-loaded-as-a-java-agent)). Previously: “The JVM's line about class data sharing remains in the test runs (10.1). Whether to leave it or to turn class data sharing off for the tests is for the team.”*
- The temporary logs of 10.1 to 10.3 were deleted after this record was written.
