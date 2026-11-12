# Matriz — Verification of the nine scenarios (exercise “Verify the MVP”, 2026-11-12)

**Status:** In progress. Part of increment [I12](implementation-plan.md#i12--release-verification), which is Under way. The rest of I12 is in the [I12 verification record](evidence/I12-verification.md). Nothing here is a client approval ([OQ-016](constraints.md#open-questions)).

**Decisions of the project team's member for this exercise** (the conversation “Verify the MVP”): the scope is the nine scenarios plus the rest of I12; the evidence is text only, with every secret masked and fictitious names only; the automated tests use their own databases, and the manual checks use a fresh database, not the developer's local one; a known problem is recorded as a known limitation and not fixed; a defect is fixed only after the diagnosis is approved.

## Summary

| Case | Scenario | Type | Status |
|---|---|---|---|
| 01 | Access to the application | Automated and manual | Confirmed |
| 02 | Project management | Automated and manual | Confirmed |
| 03 | Task persistence | Automated and manual | Partly confirmed (no automated restart test yet; added in Step 4) |
| 04 | Data validation | — | Not run |
| 05 | State and priority | — | Not run |
| 06 | Assignees | — | Not run |
| 07 | Comments | — | Not run |
| 08 | Authorization in the API | — | Not run |
| 09 | Deletion and errors | — | Not run |

## Environment and conventions

| Item | Value |
|---|---|
| Machine | macOS, arm64; Docker 28.3.2; Node.js 24.8.0 (the build uses its own pinned Node.js) |
| JDK | Temurin 25.0.3, selected with `JAVA_HOME` for each command (the shell's default is JDK 21, which the build refuses) |
| Automated tests | Their own PostgreSQL containers (Testcontainers), with no demonstration data |
| Manual checks | The jar of the baseline build, on port 18080, against a fresh PostgreSQL 18.6 from a separate Compose project (`docker compose -p matriz-ex8`, its own volume, started empty). The developer's local volume was not touched. The four demonstration people, with passwords and a signing key generated for the run, kept outside the repository and not recorded here |
| Masking | Tokens, passwords, hashes, and keys are never written here; `<masked>` marks where one was |

**Baseline.** `JAVA_HOME=<JDK 25> ./mvnw -B clean verify` ended with `BUILD SUCCESS` (exit 0, 3 min 25 s): **626 backend tests, 0 failures, 0 errors, 0 skipped**, and **23 front-end unit tests** (Vitest, 5 files), including `UserInterfaceTests` (43, Playwright, Chromium). The raw log is outside the repository.

**Types.** *Automated* means a test in the build that fails if the behaviour breaks. *Manual* means a command or a step that I ran once; it proves the behaviour of that run only.

## Case 01 — Access to the application

- **Expected:** valid credentials let a person in; invalid credentials and requests without authentication are handled ([US-013](user-stories.md#us-013--sign-in), [US-014](user-stories.md#us-014--sign-out), [US-015](user-stories.md#us-015--see-who-is-signed-in), [EX-012](user-stories.md#edge-cases-and-exceptions), [EX-014](user-stories.md#edge-cases-and-exceptions), [NFR-001](requirements.md#non-functional-requirements-and-unresolved-quality-criteria)).
- **Type:** automated and manual. **Status: Confirmed.**
- **Automated evidence** (baseline build, all passed): `SignInTests` (27), `AccessTokenTests` (47), `EveryOperationRequiresATokenTests` (45: every operation gets 401 with no token and with a signed-out token), `ProblemDetailsTests` (16), `TokenCheckFailureTests` (1), `SignOutAfterRestartTests` (1), and in `UserInterfaceTests` the sign-in, incorrect-credentials, empty-fields, reload, expired-session and two-people tests.
- **Manual evidence** (fresh database, port 18080; secrets masked):

| Step | Request | Observed |
|---|---|---|
| 1 | `POST /api/session` as `ana` with her password | `200`, `application/json`; `tokenType` Bearer, `expiresIn` 28800, person “Ana Silva”; token `<masked>` |
| 2 | `GET /api/me` with that token | `200`, `{"personId":"10000000-0000-4000-8000-000000000001","displayName":"Ana Silva"}` |
| 3 | The same sign-in with the login name `ANA` | `200` (letter case does not matter) |
| 4 | Sign-in with `ana` and a wrong password | `401`, `{"detail":"The login name or password is incorrect.","instance":"/api/session","status":401,"title":"Unauthorized"}` |
| 5 | Sign-in with an unknown login name | `401`; the status, every header except `Date`, and the body are identical to step 4 (`diff` empty) |
| 6 | `GET /api/projects` with no token | `401`, `application/problem+json`, `WWW-Authenticate: Bearer`, “Identity has not been established.” |
| 7 | `GET /api/me` with `Bearer abc.def.ghi` | `401` |
| 8 | `GET /api/me` with a token whose last character was changed | `401` |
| 9 | `DELETE /api/session`, then `GET /api/me` with the same token | `204`, then `401` “The access token is invalid, expired, or no longer accepted.” |
| 10 | Sign-in with an empty login name and password | `422` |

- **Not verified here:** the absence of any limit on failed sign-ins is a recorded limitation ([OQ-027](constraints.md#open-questions)); it is reviewed in the [I12 record](evidence/I12-verification.md). A failure while a token is checked gives `500`, not `401` (`TokenCheckFailureTests`; [I2 record](evidence/I2-verification.md#13-not-yet-done-and-still-open), point 4); recorded as a known limitation, not fixed. The expiry after 8 hours and the effect of a new signing key are checked by `AccessTokenTests` and `SignOutAfterRestartTests`, not by hand.

## Case 02 — Project management

- **Expected:** creating, searching, editing, and deleting work according to the permissions ([US-001](user-stories.md#us-001--create-project), [US-007](user-stories.md#us-007--view-accessible-projects), [US-008](user-stories.md#us-008--rename-project), [US-016](user-stories.md#us-016--delete-project), [US-023](user-stories.md#us-023--search-projects); [EX-003](user-stories.md#edge-cases-and-exceptions), [EX-016](user-stories.md#edge-cases-and-exceptions), [EX-025](user-stories.md#edge-cases-and-exceptions)): the Project Owner can do all four; a Project Participant can view but not edit or delete (403); a non-member sees nothing (404) and the data stays unchanged.
- **Type:** automated and manual. **Status: Confirmed.**
- **Automated evidence** (baseline build, all passed): `ProjectApiTests` (67), `ProjectRepositoryTests` (5), `ProjectDomainTests` (22), `ParticipantsApiTests` (27), `MembersApiTests` (8), and in `UserInterfaceTests` the project tests (create, edit, delete with a confirmation, paging and search, the participant who cannot edit, at widths 390 and 1280).
- **Manual evidence** (fresh database; ana is the owner, rui a participant, eva a non-member):

| Step | Request | Observed |
|---|---|---|
| 1 | Ana creates a project named `"  Verif Árvore  "` with a description | `201`; the name is stored trimmed (“Verif Árvore”); `myRole` PROJECT_OWNER; `state` ACTIVE; no percentage (no tasks) |
| 2 | Ana lists her projects | 4 projects, including the new one; the list holds only Ana's projects |
| 3 | Search `name=VERIF` | 1 project (letter case ignored) |
| 4 | Search `name=arvore` | 0 projects: accents are not ignored today; the team has not decided this ([OQ-026](constraints.md#open-questions), interim answer) |
| 5 | Eva searches `name=verif` | 0 projects (a non-member's search never shows it, [EX-025](user-stories.md#edge-cases-and-exceptions)) |
| 6 | Ana renames the project and removes its description | `200`, then again `200` with another name |
| 7 | Ana adds Rui as a participant | `201`, role PROJECT_PARTICIPANT |
| 8 | Rui views the project | `200` |
| 9 | Rui edits it, and Rui deletes it | `403` each, “Only the Project Owner of this project can do this.”; the project is unchanged afterwards |
| 10 | Eva views, edits, and deletes it | `404` each; the project is unchanged afterwards |
| 11 | Ana deletes the project | `204`; then Ana and Rui get `404`, and Ana's search no longer lists it |
| 12 | psql on the fresh database | the project row is kept with `deleted_at` set, and its 2 memberships are kept ([A-022](constraints.md#assumptions)) |

- **Not verified here:** paging with many projects, and performance (see the [I12 record](evidence/I12-verification.md)); the name search matches partial words ignoring letter case ([A-024](constraints.md#assumptions)); whether accents are ignored is still [Open OQ-026](constraints.md#open-questions). Deleting a Completed project ([EX-015](user-stories.md#edge-cases-and-exceptions)) was not run by hand; it is covered only by the automated `ProjectApiTests.aCompletedProjectCannotBeDeleted`.

## Case 03 — Task persistence

- **Expected:** a created task appears without a reload, and it stays after the backend restarts ([US-003](user-stories.md#us-003--create-task), [US-004](user-stories.md#us-004--view-tasks)).
- **Type:** automated (appears without a reload; creation followed by a query) and manual (the restart). **Status: Partly confirmed** until the automated restart test of Step 4 exists; the manual restart check itself is Confirmed.
- **Automated evidence** (baseline build, all passed): `UserInterfaceTests.theProjectOwnerCreatesChangesFiltersAndDeletesTasksWithoutReloadingThePage` and `…assignsSeveralMembersAndRemovesEachOneWithoutReloadingThePage` (Playwright, Chromium); `TaskApiTests` (93) and `TaskRepositoryTests` (9). No existing automated test restarts the backend over a task: `SignOutAfterRestartTests` restarts it only for tokens, and `DemoDataTests` only for demonstration data. A new test will fill that gap (Step 4).
- **Manual evidence** (fresh database; ids as returned by the API):

| Step | Action | Observed |
|---|---|---|
| 1 | Ana creates the project “Persistence Check” | `201` |
| 2 | Ana creates the task `"  Write the report  "`, description “two pages”, priority HIGH | `201`; title stored trimmed; state TO_DO; no assignees |
| 3 | Ana lists the project's tasks right away | the new task is listed (`totalItems` 1) |
| 4 | Ana adds Rui as a participant, assigns him, moves the task to In progress, and adds the comment `"  First note  "` | `201`, `200`, `200`, `201`; the comment is trimmed, author Ana Silva, `createdAt` 2026-11-12T19:33:49Z |
| 5 | Snapshot before the restart | task IN_PROGRESS, HIGH, assignee Rui Martins; 1 comment; progress `0 / 1 / 0` and `percentDone` 0 |
| 6 | The backend process is stopped (`kill`, graceful) and started again on the same database | health unreachable while down, then `200`; the log says “Schema "public" is up to date. No migration necessary.” (version 6) |
| 7 | The same reads after the restart | task, assignee, comment (same `commentId`, author, and date), progress, and member list (Rui has assigned tasks) are **identical** to step 5; Rui, signed in again, also sees the project and his task |
| 8 | The access token from before the restart | still `200`, because the signing key was unchanged in this run (see [Case 01](#case-01--access-to-the-application)) |

- **Not verified here:** “appears without a reload” was not checked by hand in a browser; it relies on the Playwright tests above. An abrupt stop (crash) of the backend or of PostgreSQL was not run; the restart was a graceful stop. One call of mine returned `405` because I used the wrong method for assigning (`PUT` on a person path instead of `POST` on `/assignees`); that was my mistake, not an application defect, and the repeat with the right call is the one recorded.
