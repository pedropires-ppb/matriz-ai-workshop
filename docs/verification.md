# Matriz — Verification of the nine scenarios (exercise “Verify the MVP”, 2026-11-12)

**Status:** Awaiting review. Part of increment [I12](implementation-plan.md#i12--release-verification), which is Awaiting review, not Accepted. The rest of I12 is in the [I12 verification record](evidence/I12-verification.md). Nothing here is a client approval ([OQ-016](constraints.md#open-questions)).

**Decisions of the project team's member for this exercise** (the conversation “Verify the MVP”): the scope is the nine scenarios plus the rest of I12; the evidence is text only, with every secret masked and fictitious names only; the automated tests use their own databases, and the manual checks use a fresh database, not the developer's local one; a known problem is recorded as a known limitation and not fixed; a defect is fixed only after the diagnosis is approved.

## Summary

| Case | Scenario | Type | Status |
|---|---|---|---|
| 01 | Access to the application | Automated and manual | Confirmed |
| 02 | Project management | Automated and manual | Confirmed |
| 03 | Task persistence | Automated and manual | Confirmed (restart: automated and manual; “without a reload”: Playwright only) |
| 04 | Data validation | Automated and manual | Confirmed (Bug 2 found, fixed; other unusual text is OQ-031) |
| 05 | State and priority | Automated and manual | Confirmed |
| 06 | Assignees | Automated and manual | Confirmed |
| 07 | Comments | Automated and manual | Confirmed (browser rendering of markup not checked) |
| 08 | Authorization in the API | Automated and manual | Confirmed (Bug 1 fixed; set A repeated live on the rebuilt jar) |
| 09 | Deletion and errors | Automated and manual | Partly confirmed (failed project deletion and failed assignment not run in a browser) |

## Environment and conventions

| Item | Value |
|---|---|
| Machine | macOS, arm64; Docker 28.3.2; Node.js 24.8.0 (the build uses its own pinned Node.js) |
| JDK | Temurin 25.0.3, selected with `JAVA_HOME` for each command (the shell's default is JDK 21, which the build refuses) |
| Automated tests | Their own PostgreSQL containers (Testcontainers), with no demonstration data |
| Manual checks | The jar of the baseline build (cases 01–09, before the two fixes) and, for the re-checks after the fixes, the jar of the final build, on port 18080, against a fresh PostgreSQL 18.6 from a separate Compose project (`docker compose -p matriz-ex8`, its own volume, started empty). The developer's local volume was not touched. The four demonstration people, with passwords and a signing key generated for the run, kept outside the repository and not recorded here |
| Masking | Tokens, passwords, hashes, and keys are never written here; `<masked>` marks where one was |

**Baseline.** `JAVA_HOME=<JDK 25> ./mvnw -B clean verify` ended with `BUILD SUCCESS` (exit 0, 3 min 25 s): **626 backend tests, 0 failures, 0 errors, 0 skipped**, and **23 front-end unit tests** (Vitest, 5 files), including `UserInterfaceTests` (43, Playwright, Chromium). The raw log is outside the repository. **Final build** (after the fixes of Bug 1 and Bug 2 and the three new test classes): the same command ended with `BUILD SUCCESS` (exit 0, 3 min 40 s): **685 backend tests, 0 failures, 0 errors, 0 skipped**, and **23 front-end unit tests**.

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
- **Type:** automated (appears without a reload; creation followed by a query; the restart) and manual (the restart, by hand). **Status: Confirmed** for the data after a restart; “appears without a reload” rests on the Playwright tests only (not by hand, see below), so the status of that half is automated-only.
- **Automated evidence** (baseline build, all passed): `UserInterfaceTests.theProjectOwnerCreatesChangesFiltersAndDeletesTasksWithoutReloadingThePage` and `…assignsSeveralMembersAndRemovesEachOneWithoutReloadingThePage` (Playwright, Chromium); `TaskApiTests` (93) and `TaskRepositoryTests` (9). Before this exercise no automated test restarted the backend over a task: `SignOutAfterRestartTests` restarts it only for tokens, and `DemoDataTests` only for demonstration data. The new `DataAfterRestartTests` fills that gap ([Step 4](#the-three-automated-tests)): it creates a project, two tasks, an assignee, a comment, and a state change over real HTTP, deletes one task, restarts the application on the same database, and compares five reads whole (project, task list, task, comments, members), which are identical; the deleted task is still `404`.
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

## Case 04 — Data validation

- **Expected:** empty required fields, fields with only spaces, and invalid values are rejected, nothing is stored, and the person is told what is wrong ([US-001](user-stories.md#us-001--create-project), [US-003](user-stories.md#us-003--create-task), [US-008](user-stories.md#us-008--rename-project), [US-011](user-stories.md#us-011--rename-task), [US-021](user-stories.md#us-021--add-comment); [EX-010](user-stories.md#edge-cases-and-exceptions), [EX-011](user-stories.md#edge-cases-and-exceptions); [A-003](constraints.md#assumptions), [A-033](constraints.md#assumptions)).
- **Type:** automated and manual. **Status: Confirmed** for the three kinds of input the case names. The probe with unusual text (Step 4, new test `UnusualTextTests`) found [Bug 2](#bug-2--text-with-the-null-character-or-an-unpaired-surrogate-gave-a-server-error-or-was-changed), fixed and covered by regression tests; the other unusual text is open as [OQ-031](constraints.md#open-questions).
- **Automated evidence** (baseline build, all passed): `ProjectDomainTests` (22), `TaskDomainTests` (19), `CommentTextTests` (11), `RequestSchemaTests` (13), `ProjectApiTests` (67), `TaskApiTests` (93), `CommentApiTests` (36), and in `UserInterfaceTests` `invalidNamesAndDescriptionsAreExplainedWithoutARequest` and `invalidTitlesAndDescriptionsAreExplainedWithoutARequest`.
- **Manual evidence** (fresh database; one Python script sending each request to the running backend as Ana; every rejected request got a Problem Details body with a `pointer` and a `detail`):

| Request | Observed |
|---|---|
| Create project: empty name; spaces only; tab and newline only; non-breaking spaces only | `422`, `/name`, “A name is required.” (four times) |
| Create project: 101-character name; 1001-character description | `422`, “The name must have at most 100 characters.”; “The description must have at most 1000 characters.” |
| Create project: exactly 100 characters | `201` (the limit is inclusive) |
| Create project: name as a number; `null`; missing; an unknown property `state` | `422`, “The name must be a string.”; “A name is required.” (twice); “This property is not allowed.” |
| Create project: malformed JSON; a media type other than JSON | `400`; `415` |
| Create task: empty title; spaces only; 151 characters | `422`, “A title is required.” (twice); “The title must have at most 150 characters.” |
| Create task: 2001-character description; priority `URGENT`; priority `high`; an initial `state` | `422`, “…at most 2000 characters.”; “Use one of LOW, MEDIUM, or HIGH.” (twice); “This property is not allowed.” |
| Edit task: title as spaces; title `null` | `422`, “A title is required.”; “The title cannot be removed.” |
| Change state: `FINISHED`; no state | `422`, “Must be TO_DO, IN_PROGRESS, or DONE.” (twice) |
| Add comment: empty; spaces only; 2001 characters; an `author` property | `422`, “The text is required.” (twice); “The text must have at most 2000 characters.”; “This property is not allowed.” |
| Afterwards, the existing data | the task is still “Write the report”, IN_PROGRESS, HIGH; the project still has 1 task and the task 1 comment |

- **Not verified here:** how the browser form shows each message was checked only by the two `UserInterfaceTests` above, and the browser form was not tried with the characters of Bug 2. Text made of zero-width characters, other control characters, or the right-to-left override is accepted and kept as sent, because no rule covers it ([A-033](constraints.md#assumptions) names only whitespace); it is [OQ-031](constraints.md#open-questions). Limits are in characters as the API counts them; a count by user-perceived character (emoji, combining marks) was not tested.

## Case 05 — State and priority

- **Expected:** editing a task's state or priority updates the task, the filters, and the counts coherently ([US-005](user-stories.md#us-005--change-task-state), [US-006](user-stories.md#us-006--view-progress), [US-018](user-stories.md#us-018--set-task-priority), [US-024](user-stories.md#us-024--filter-tasks); [EX-004](user-stories.md#edge-cases-and-exceptions), [EX-008](user-stories.md#edge-cases-and-exceptions), [EX-013](user-stories.md#edge-cases-and-exceptions); [A-011](constraints.md#assumptions), [A-024](constraints.md#assumptions)).
- **Type:** automated and manual. **Status: Confirmed.**
- **Automated evidence** (baseline build, all passed): `ProgressApiTests` (9, including `creatingEditingAndChangingTheStateOfTasksUpdatesTheProgress` and `taskFiltersNeverChangeTheProgress`), `ProgressDomainTests` (14), `TaskApiTests` (93), `CompletionApiTests` (32), and in `UserInterfaceTests` `theProgressAndTheCountsFollowEachChangeButNotTheFilters`, `theProjectOwnerCreatesChangesFiltersAndDeletesTasksWithoutReloadingThePage`, `aTaskCanBeEditedAndItsDescriptionCleared`, and `theProjectOwnerCompletesAProjectOnlyWhenNoTaskIsUnfinished`.
- **Manual evidence** (fresh database; project “Case 05 State and priority”, four tasks T1 LOW, T2 MEDIUM, T3 HIGH, T4 HIGH, created To do):

| Step | Action | Observed |
|---|---|---|
| 1 | Create the four tasks | each `201`, state TO_DO; priority as given (MEDIUM when absent); progress `4 / 0 / 0`, `percentDone` 0 |
| 2 | T1 → Done, T2 and T3 → In progress | `200` each; progress `1 / 2 / 1`, 25% |
| 3 | Filters `state=IN_PROGRESS`; `state=DONE`; `priority=HIGH`; `state=IN_PROGRESS&priority=HIGH`; `state=TO_DO&state=DONE` | T2+T3; T1; T3+T4; T3; T1+T4 (several values of one filter are alternatives; different filters combine). The filter never changed the progress |
| 4 | An unknown filter value (`state=BOGUS`, `priority=urgent`) | `400`, “The query parameter state must be TO_DO, IN_PROGRESS, or DONE.” (and the priority equivalent) |
| 5 | T1 Done → To do (any state to any state) | `200`; progress `2 / 2 / 0`, 0% |
| 6 | Edit T2's priority to HIGH, then its title | `200`; the state and the other field stay; `priority=HIGH` now lists T2, T3, T4 and `priority=MEDIUM` lists none |
| 7 | Priority `CRITICAL` | `422` |
| 8 | All four Done | progress `0 / 0 / 4`, 100% |
| 9 | Rui, not a member, changes a state | `404` |
| 10 | Rui, a participant not assigned, changes a state; changes a priority | `403` “Only the Project Owner or an assignee of the task can change its state.”; `403` “Only the Project Owner of this project can do this.”; the task stays Done and LOW |
| 11 | Ana assigns Rui; Rui moves T1 to In progress | `200`; progress `0 / 1 / 3`, 75% (this is the behaviour behind [A-020](constraints.md#assumptions) / [OQ-025](constraints.md#open-questions)) |
| 12 | Ana tries to complete the project while T1 is In progress | `409` “The project has tasks that are To do or In progress; it remains Active.” |
| 13 | All Done, Ana completes the project, then changes a task's state and priority | project `COMPLETED`; each change `409` “The project is Completed; it cannot be changed.”; the task keeps Done and LOW; progress stays 100% |

- **Not verified here:** the rounding of the percentage at the edges (for example 199 of 200 showing 100%, [A-026](constraints.md#assumptions)) is covered by `ProgressDomainTests`, not run by hand. The first run of my script stopped on the error body of the invalid filter (my script's mistake) and a later step I wrote tested an Active project by accident (the project had not completed, step 12); both were repeated correctly and only the corrected results are recorded.

## Case 06 — Assignees

- **Expected:** assigning and removing work according to the permissions, and the same assignment is not duplicated ([US-012](user-stories.md#us-012--assign-task), [US-019](user-stories.md#us-019--unassign-a-member), [US-020](user-stories.md#us-020--assign-or-unassign-myself), [US-009](user-stories.md#us-009--remove-participant); [EX-007](user-stories.md#edge-cases-and-exceptions), [EX-020](user-stories.md#edge-cases-and-exceptions), [EX-021](user-stories.md#edge-cases-and-exceptions), [EX-022](user-stories.md#edge-cases-and-exceptions), [EX-028](user-stories.md#edge-cases-and-exceptions); [A-019](constraints.md#assumptions), [A-020](constraints.md#assumptions), [A-035](constraints.md#assumptions)).
- **Type:** automated and manual. **Status: Confirmed.**
- **Automated evidence** (baseline build, all passed): `AssignmentApiTests` (21), including `assigningAPersonWhoIsAlreadyAnAssigneeChangesNothing`, `twoRequestsToAssignTheSamePersonAtTheSameTimeLeaveOneAssignment`, `theDatabaseRefusesATaskAssignedTwiceToTheSamePersonAndAnAssigneeWhoIsNotAMember`, `aProjectParticipantAssignsOnlyThemselvesAndThenChangesTheState`, `aCompletedProjectRefusesToChangeItsAssignees`; `ParticipantsApiTests` (27); in `UserInterfaceTests` `theProjectOwnerAssignsSeveralMembersAndRemovesEachOneWithoutReloadingThePage`, `aProjectParticipantAssignsThemselvesChangesTheStateAndUnassignsThemselves`, and `aCompletedProjectOffersNoAssignmentControls`.
- **Manual evidence** (fresh database; project “Case 06 Assignees”, tasks “Task one” and “Task two”; ana owner, rui participant, eva a non-member):

| Step | Action | Observed |
|---|---|---|
| 1 | Ana assigns Rui before he is a member; an unknown person id; a malformed id; no `personId` | `422` “Only members of the project can be assigned to its tasks.” (three times; the same refusal for a person who does not exist, [A-035](constraints.md#assumptions)); `422` |
| 2 | Ana adds Rui, assigns Rui, then herself | `200`; assignees Rui, then Ana and Rui |
| 3 | Ana assigns Rui a second and a third time | `200` each; the assignees stay Ana and Rui, once each ([EX-028](user-stories.md#edge-cases-and-exceptions)) |
| 4 | Rui assigns Ana; Rui unassigns Ana | `403` “A Project Participant can assign only themselves.”; `403` “…unassign only themselves.” ([EX-021](user-stories.md#edge-cases-and-exceptions)) |
| 5 | Rui assigns himself to Task two, twice; unassigns himself; unassigns himself again | `200`, `200` (still one), `200` (no assignees), `404` “…does not exist or is not accessible.” |
| 6 | Eva assigns herself; Eva unassigns Rui | `404`; `404` |
| 7 | Ana unassigns Rui | `200`; only Ana remains |
| 8 | Ana removes Rui from the project while he is assigned; then after unassigning him | `409` “The person is assigned to tasks of this project; remove those assignments first.” ([EX-007](user-stories.md#edge-cases-and-exceptions)); `204` |
| 9 | 20 simultaneous assignments of Rui to one task | all `200`; one assignee; `psql` shows 1 row, and the table has a unique index on `(task_id, person_id)` |
| 10 | After Ana completes the project: assign; unassign | `409` “The project is Completed; it cannot be changed.” (twice, [EX-022](user-stories.md#edge-cases-and-exceptions)) |

- **Observation, not a defect:** an identifier that is not a valid one in the body (`"not-an-id"`) gets the same `422` “Only members of the project can be assigned to its tasks.” as an unknown person. That is consistent with [A-035](constraints.md#assumptions) (nothing is disclosed) and with `openapi.yaml`, which keeps the identifier format undecided and opaque, so such a value is just an unknown person.
- **Not verified here:** the assignee controls in a browser by hand (only the Playwright tests above).

## Case 07 — Comments

- **Expected:** the content, the authenticated author, and the date are saved and visible; the author and date come from Matriz, not from the request ([US-021](user-stories.md#us-021--add-comment), [US-022](user-stories.md#us-022--read-comments); [EX-023](user-stories.md#edge-cases-and-exceptions), [EX-024](user-stories.md#edge-cases-and-exceptions), [EX-031](user-stories.md#edge-cases-and-exceptions), [EX-032](user-stories.md#edge-cases-and-exceptions); [A-023](constraints.md#assumptions), [A-036](constraints.md#assumptions)).
- **Type:** automated and manual. **Status: Confirmed.**
- **Automated evidence** (baseline build, all passed): `CommentApiTests` (36), `CommentTextTests` (11), and in `UserInterfaceTests` `aMemberAddsCommentsSeesTheirAuthorAndDateAndKeepsThemWhenTheTaskIsOpenedAgain` and `aCompletedProjectStillAllowsComments`. Persistence over a backend restart is shown by hand in [Case 03](#case-03--task-persistence) and by the new test of Step 4.
- **Manual evidence** (fresh database; project “Case 07 Comments”, ana owner, rui participant, eva a non-member):

| Step | Action | Observed |
|---|---|---|
| 1 | Rui lists the comments of a task with none | `200`, `[]` |
| 2 | Ana adds `"  First  "`; Rui adds a text with `<b>`, `&`, quotes, an accent, and an emoji | `201` each; the text is trimmed and otherwise stored exactly; the author is the signed-in person (Ana Silva, Rui Martins); `createdAt` is a UTC time (for example 2026-11-12T19:37:38.852Z), within 5 s of my clock |
| 3 | Ana and Rui list the comments | the same list for both, in the order added (“First”, then “Second”), with the dates in ascending order |
| 4 | Add a comment that also sends `author`, `createdAt`, or `authorId` | `422` each, “This property is not allowed.” (nothing added) |
| 5 | A comment of 2000 characters; of 2001; of non-breaking spaces and a space only | `201`; `422`; `422` |
| 6 | Eva lists and adds; no token lists | `404`, `404`; `401` |
| 7 | After Ana completes the project, Rui adds a comment | `201` (comments are allowed in a Completed project, [EX-024](user-stories.md#edge-cases-and-exceptions)) |
| 8 | `PUT`, `PATCH`, and `DELETE` on a comment | `404` each (no operation exists to change or delete a comment, [A-023](constraints.md#assumptions); [outside the initial scope](requirements.md#outside-the-initial-scope)) |

- **Not verified here:** the HTML text was returned as plain data in JSON; how the browser shows it (escaped, not interpreted) was not checked by hand; the Playwright test for comments does not use markup. I did not run a test that renders `<script>` text. [Proposal] check that in the browser as part of I12's remaining items. The date's time zone as shown in the browser was not checked by hand.

## Case 08 — Authorization in the API

- **Expected:** another person cannot access or change a resource without permission, even by changing the identifiers in the request: a non-member gets the same `404` as for a resource that does not exist, a Project Participant gets `403` for owner-only operations, and nothing changes ([EX-003](user-stories.md#edge-cases-and-exceptions), [EX-012](user-stories.md#edge-cases-and-exceptions), [EX-016](user-stories.md#edge-cases-and-exceptions), [EX-019](user-stories.md#edge-cases-and-exceptions), [EX-021](user-stories.md#edge-cases-and-exceptions); [A-022](constraints.md#assumptions); [NFR-001](requirements.md#non-functional-requirements-and-unresolved-quality-criteria); permission matrix in [requirements](requirements.md#user-types)).
- **Type:** automated and manual. **Status: Confirmed.** The manual run (below) found [Bug 1](#bug-1--the-removal-of-a-member-told-a-non-member-whether-a-project-exists) on a jar built before the fix; after the fix and its regression test, I rebuilt the jar (`./mvnw -B clean verify`, BUILD SUCCESS), restarted the application on it, and repeated set A live: Eva's 18 operations on Ana's project and the same 18 on a non-existent project give `404` ×18 with identical bodies (the `instance` path removed), including `DELETE …/members/{personId}`.
- **Automated evidence** (baseline build, all passed): `EveryOperationRequiresATokenTests` (45), `ProjectApiTests` (67), `TaskApiTests` (93), `AssignmentApiTests` (21, including `aTaskOfAnotherProjectIsNotFoundThroughThisOne`), `ParticipantsApiTests` (27), `MembersApiTests` (8), `CommentApiTests` (36), `ProblemDetailsTests` (16), `TaskAccessRulesTests` (7), `ProjectAccessRulesTests` (5), and in `UserInterfaceTests` the tests of the participant who cannot edit. The new class `CrossProjectAccessTests` ([Step 4](#the-three-automated-tests)) adds the missing identifier-swapping checks: 56 task operations (read, edit, state, delete, comments, assignees) by three people across two projects, and the removal, addition, and listing of members through the wrong project, all `404`, with a fingerprint of the five data tables unchanged.
- **Manual evidence** (fresh database; Ana owner of “Case 08 Ana project” with Rui as participant, Eva owner of her own project and not a member of Ana's; a snapshot hash of both projects, their tasks, comments, and members taken before and after):

| Set | Request | Observed |
|---|---|---|
| A | Eva → Ana's project and task, 18 operations (project, members, eligible people, tasks, task, state, assignees, comments; read and write) | `404` ×18; every response equal to the one for non-existent ids, except one (see Bug 1) |
| B | Eva, owner of her project, uses Ana's task id under her own project, 8 task operations (also with Ana's and Rui's person ids) | `404` ×8 and ×8 |
| C | Eva uses her own task id under Ana's project, 8 operations | `404` ×8 |
| D | Rui (participant, not assignee) → 12 owner-only operations on Ana's project | `403` ×11 (edit, complete, delete the project; add and remove a member; the eligible people; create, edit, and delete a task; change a state; assign someone else); `404` ×1 (unassign Ana, who is not an assignee, [A-035](constraints.md#assumptions)) |
| E | Rui → Eva's project (he is no member), 18 operations | `404` ×18 |
| F | Rui (member of Ana's project) uses Eva's task id under it, 8 operations | `404` ×8 |
| G | Ana → her own task id under Eva's project (she is no member), 8 operations | `404` ×8 |
| H | `GET`, `PUT`, `PATCH`, `DELETE` of Eva's comment id under Ana's path | `404` ×4 |
| I | Rui removes the Project Owner | `403` “Only the Project Owner of this project can do this.” |
| J | Eva's token plus `X-Person-Id`, `X-User-Id`, `X-Forwarded-User`, or `X-HTTP-Method-Override` headers, or `?personId=` and `?authorId=` | `404` each; the identity comes only from the token |
| K | Snapshot of both projects after sets A–J | identical hash before and after; Ana's data intact |

- **First run, discarded:** my first script also ran project-level operations as Eva on a project Eva owns, so it deleted her own project and its results for sets B, C, and E and the snapshot were meaningless; it also used a project that was already Completed. I repeated the whole matrix with attack-only operations on fresh Active projects; only that second run is recorded above. The first run did show the `DELETE member` difference of Bug 1.
- **Not verified here:** the authorization of the `eligible-participants` search against a *large* directory, and the display-name disclosure to every Project Owner ([A-030](constraints.md#assumptions) / [OQ-011](constraints.md#open-questions)), which is a recorded privacy risk, not tested here. Concurrent permission changes (a person removed while they send a request) are covered by `ParticipantsApiTests` (a simultaneous-removal test) but were not run by hand. Rate limiting and brute force are [Open OQ-027](constraints.md#open-questions).

## Case 09 — Deletion and errors

- **Expected:** deleting respects the dependencies; an API error produces no success message and does not lose what the person typed ([US-016](user-stories.md#us-016--delete-project), [US-017](user-stories.md#us-017--delete-task); [EX-006](user-stories.md#edge-cases-and-exceptions), [EX-007](user-stories.md#edge-cases-and-exceptions), [EX-015](user-stories.md#edge-cases-and-exceptions), [EX-016](user-stories.md#edge-cases-and-exceptions), [EX-017](user-stories.md#edge-cases-and-exceptions), [EX-018](user-stories.md#edge-cases-and-exceptions); [A-022](constraints.md#assumptions)). The rows below describe what was observed; the acceptance-criterion-by-criterion traceability is in the [I12 record](evidence/I12-verification.md).
- **Type:** automated and manual. **Status: Partly confirmed.** The backend rules are confirmed by tests and by hand. In the browser, only the failures I forced or that existing tests force were checked (task creation, lists that fail to load, a repeated click, and, by hand, a failed task deletion, comment, and state change). A failed *project* deletion and a failed *assignment* were not run in a browser.
- **Automated evidence** (baseline build, all passed): `ProjectApiTests` (`theProjectOwnerDeletesAnActiveProjectAndItDisappearsEverywhere`, `aProjectParticipantCannotDelete`, `aNonMemberCannotDeleteAndTheProjectStays`, `aCompletedProjectCannotBeDeleted`, `aDeletedProjectStaysDeletedWhenTheNameIsReused`), `TaskApiTests.aDeletedTaskIsInNoReadPathAndItsAssignmentsAreRemoved`, `AssignmentApiTests` (`aDeletedTaskCannotBeAssignedOrUnassignedAndItsAssignmentsAreRemoved`, `aProjectParticipantCannotCreateEditOrDeleteATaskEvenWhenAssigned`), `ParticipantsApiTests` (`aMemberAssignedToATaskInAnyStateCannotBeRemoved`, `unassigningOrDeletingTheTaskMakesTheMemberRemovable`), `CommentApiTests` (`aDeletedTaskOrProjectHasNoCommentsToAddOrRead`, `aDeletedProjectHasNoCommentsToAddOrRead`), `ProjectRepositoryTests.aDeletedProjectIsNotFoundAndNotListedButItsRowsStay`. In the browser (`UserInterfaceTests`, rerun by hand for this case: 5 tests, 0 failures, command `./mvnw -B test -Dtest='UserInterfaceTests#…'` with JDK 25): `aFailedSubmissionKeepsWhatWasTyped` (a forced `500` on creating a task: an alert, the title, description, and priority kept, nothing stored, and a second click then creates exactly one task), `aListThatFailsToLoadOffersRetryAndShowsLoadingWhileItWaits`, `aRepeatedClickSendsOneRequestAndCreatesOneRecord`, `aProjectsTasksArePagedAndTheLastPageIsLeftWhenItsOnlyTaskIsDeleted`, and `theMembersPanelGuardsEachChangeAndOnlyTheProjectOwnerOfAnActiveProjectSeesIt`. `confirmingTheDeletionRemovesTheProjectFromTheListAndEverywhere` passed in the baseline.
- **Manual evidence, backend** (fresh database; project “Case 09 Deletion”: Ana owner, Rui participant assigned to a To do task that has a comment, one In progress task, two Done tasks):

| Step | Action | Observed |
|---|---|---|
| 1 | Rui (participant) deletes a task; deletes the project | `403`, `403` |
| 2 | Ana removes Rui while he is assigned | `409` (nothing removed) |
| 3 | Ana deletes the To do task that is assigned to Rui and has a comment | `204` |
| 4 | After step 3: `GET` the task (Ana and Rui), list its comments, assign someone to it, delete it again | `404` ×5 |
| 5 | The task list and the progress after step 3 | the three other tasks; 0 To do, 1 In progress, 2 Done → 67% |
| 6 | The database after step 3 | the task row stays (`deleted_at` set), its comment row stays, its assignment rows are gone (0) ([A-022](constraints.md#assumptions)) |
| 7 | Ana removes Rui | `204` (he is no longer assigned) |
| 8 | Ana completes the project while a task is In progress | `409`; after she deletes that task, completing works: `200` |
| 9 | In the Completed project, Ana deletes a task; deletes the project | `409`, `409`; the project stays Completed with 0 To do, 0 In progress, 2 Done |
| 10 | Ana deletes an Active project that has a member, an assigned task, and a comment | `204` |
| 11 | After step 10: `GET` the project (Ana and Rui), its tasks, the task, its comments, its members | `404` ×6; the project is in no list; a search for the name from Rui's list returns nothing |
| 12 | The database after step 10 | the project row is marked deleted; its task, comment, 2 memberships, and 1 assignment rows are all kept; no task without a project, no comment without a task, no assignment without a task (0, 0, 0) |

- **Manual evidence, browser** (Chromium headless shell, driven by a throwaway script kept outside the repository, against the same running application; the project “Case 09 UI probe”: one To do task and one In progress task; I forced the failures with request interception, so these responses are stubs, not real server behaviour):

| Step | Action | Observed |
|---|---|---|
| A | Open the project with a task In progress | the “Complete project” button is disabled, with the reason “Unavailable while tasks are To do or In progress.” (the `409` of step 8 is not reachable from this screen) |
| B | Task detail: the deletion request answers `500` | an alert “Something went wrong. Please try again.”; no success message; the person stays on the task; by the API afterwards the task still exists |
| C | The comment request answers `409` | the alert “The project is Completed; it cannot be changed.”; no success message; the typed comment stays in the box; by the API afterwards the task still has 0 comments. *Explanation corrected on 2026-11-12 by the project team's delivery decision 6: the intercepted 409 is only a stub, not a valid Completed-project refusal; comments are allowed there under [Assumption A-023](constraints.md#assumptions) and [Assumption A-036](constraints.md#assumptions). The observed output is preserved.* |
| D | The state request answers `500`; the selector is set to Done | an alert; no success message; the selector shows To do again; by the API afterwards both tasks keep their states |

- **Observation (not investigated):** in steps C and D the alert of step B stayed on the screen, so two alerts showed at once. The [I13 evidence](evidence/I13-user-interface-consolidation-verification.md) already lists that two notices can show at once on Task detail, one per panel (the details panel's and the comments panel's). What I saw is that an old alert of the details panel is not cleared when a later action in another panel fails. [Proposal] keep it as a known limitation for the project team to rank; no requirement says when an alert must go away.
- **Not verified here:** a failed *project* deletion and a failed *assignment* in the browser; the real-server `409` of step 9 on the screen (I did not look at whether the screen offers those buttons in a Completed project); a failure of the network itself (connection refused, a time-out) beyond the retry test for lists; whether a screen reader announces the alerts; whether the soft-deleted rows can be restored or purged, which no requirement defines (see [A-022](constraints.md#assumptions)).

## The three automated tests

Command for each (JDK 25; Docker running, for Testcontainers PostgreSQL): `./mvnw -B test -Dtest=<Class>`. For each test I broke the rule it checks on purpose, ran it, and reverted the break with `git checkout` (the working tree was clean again afterwards, and each test was green again).

| # | Kind | Class (file under `src/test/java/com/matriz/`) | New or extended | Result with the rule intact | Result with the rule broken |
|---|---|---|---|---|---|
| 1 | Validation rule | `UnusualTextTests` | New: no existing test covered control characters, unpaired surrogates, or invisible characters | 55 tests, 0 failures | break: `StorableText.isStorable` in the three modules accepts everything (the loop does not run) → 55 tests, **33 failures** |
| 2 | Authorization | `CrossProjectAccessTests` | New: existing classes swap identifiers for tasks only; this one covers comments, state, assignees, members, and every person | 2 tests, 0 failures | break 1: the task lookup in `SpringDataTaskRepository` ignores the project of the path → `ana GET …/tasks/{task of the other project}` answers `200` with the task, test **fails**. Break 2: `RemoveParticipant` does not answer `404` for a non-member → the member test **fails** |
| 3 | Creation followed by a query that checks persistence | `DataAfterRestartTests` | New: no test restarted the backend over project data | 1 test, 0 failures | break 1: `JpaCommentRepository.save` does not save → **fails** (the comment list is empty); break 2: the task lookup ignores `deleted_at` → after the restart the deleted task answers `200` instead of `404`, **fails** |

What each one asserts:

1. `UnusualTextTests` sends U+0000, unpaired surrogates, zero-width characters, BEL, U+202E, and CR/LF in a project name and description, a task title and description, a comment, an edit, a search, and a sign-in. Rule: a refusal with the field named (`422`, `400`, or `401`), or a text that is read back exactly as sent; never a `5xx`, never a changed text, and nothing stored for a refused request ([Bug 2](#bug-2--text-with-the-null-character-or-an-unpaired-surrogate-gave-a-server-error-or-was-changed), [A-037](constraints.md#assumptions), [OQ-031](constraints.md#open-questions)).
2. `CrossProjectAccessTests` has Ana owning project A and Eva owning project B, Rui a participant of both, and Ana's task assigned to Rui. Seven combinations of person, project, and task (a non-member, or a task of another project) each try eight operations; every one is `404` with the same text as for a task that does not exist; a second test does the same for member removal, addition, and the two member lists. A fingerprint (an MD5 of every row) of `project`, `task`, `comment`, `membership`, and `assignment` is identical before and after.
3. `DataAfterRestartTests` runs the whole application on a real port with a real PostgreSQL: creates a project, two tasks, an assignee, a comment, and a state change, and deletes one task; reads five resources; stops the application; starts it again on the same database; reads the same five resources and asserts that they are equal as whole bodies; and asserts that the deleted task is still `404`.

Output of the last green run of each (`-Dtest=…`):

```
CrossProjectAccessTests  Tests run: 2, Failures: 0, Errors: 0, Skipped: 0   BUILD SUCCESS
DataAfterRestartTests    Tests run: 1, Failures: 0, Errors: 0, Skipped: 0   BUILD SUCCESS
UnusualTextTests         Tests run: 55, Failures: 0, Errors: 0, Skipped: 0  BUILD SUCCESS
```

Output with the rule broken (the summary line of each run, and the first failing assertion):

```
UnusualTextTests          Tests run: 55, Failures: 33   BUILD FAILURE
CrossProjectAccessTests   Tests run: 2, Failures: 1     BUILD FAILURE  (…:109 [ana GET /api/projects/…/tasks/… -> {"taskId":…}] expected: 404)
CrossProjectAccessTests   Tests run: 2, Failures: 1     BUILD FAILURE  (aMemberOfOneProjectNamedThroughAnotherIs…:132)
DataAfterRestartTests     Tests run: 1, Failures: 1     BUILD FAILURE  (:77, the comment list holds no comment)
DataAfterRestartTests     Tests run: 1, Failures: 1     BUILD FAILURE  (:86 GET deleted task … expected: 404)
```

Limitation: the restart test stops and starts the Spring application in the same JVM against the same PostgreSQL; it does not restart PostgreSQL, and it is not a crash. The manual check in [Case 03](#case-03--task-persistence) restarted a separate Java process.

## Bugs found

### Bug 1 — The removal of a member told a non-member whether a project exists

- **Found in:** Case 08, manual run, set A.
- **Steps:** as Eva, who is not a member of the project, `DELETE /api/projects/{projectId}/members/{personId}` with (1) the id of an existing project, (2) a valid id that does not exist, (3) the id of a deleted project, and (4) `not-a-uuid` as the project.
- **Expected:** the same `404` for every project Eva cannot access ([EX-003](user-stories.md#edge-cases-and-exceptions); the 404 of this operation in `openapi.yaml` is “The project or member does not exist or is not accessible.”).
- **Observed:** (1) and (4) “The project **or member** does not exist or is not accessible.”; (2) and (3) “The project does not exist or is not accessible.” Every other operation gave one text. Impact: Low. Anyone signed in could tell, from the text, whether a project identifier exists; the identifiers are random UUIDs, so guessing one is impractical.
- **Cause:** `RemoveParticipant.remove` threw `ProjectNotFoundException` when the project was not found, and `MemberNotFoundException` (a different text) when the project existed but the caller was not a member. `ProjectController` already turned a malformed project id into `MemberNotFoundException`. The existing tests covered the non-member and the malformed id, never an unknown or deleted valid id.
- **Fix** (approved by the project team's member): `RemoveParticipant.remove` throws `MemberNotFoundException` when the project is missing or deleted, so every refusal of this operation has one text. One line of code and its Javadoc; no other operation, message, or document changed.
- **Regression test:** `ParticipantsApiTests.aProjectThatDoesNotExistOrIsDeletedAnswersTheRemovalOfAMemberLikeAPersonWhoIsNotAMember`. Without the fix: it failed (`JSON path "$.detail" expected:<The project or member does not exist or is not accessible.> but was:<The project does not exist or is not accessible.>`, 28 tests, 1 failure). With the fix: `ParticipantsApiTests` 28/28, `MembersApiTests` 8/8, `ProjectApiTests` 67/67, `LayeringTests` 5/5, and `ModuleBoundaryTests` 4/4 passed (command: `./mvnw -B test -Dtest='ParticipantsApiTests,MembersApiTests,ProjectApiTests,LayeringTests,ModuleBoundaryTests'`, JDK 25).

### Bug 2 — Text with the null character or an unpaired surrogate gave a server error or was changed

- **Found in:** Step 4, the new validation test `UnusualTextTests` (Case 04). Impact: Low to Medium. A person, or a client, could cause a `500` on every operation that stores text, and on sign-in without being signed in; the error log fills with stack traces. No data was lost for U+0000 (the request was rolled back), but an unpaired surrogate was stored changed.
- **Steps:** send `{"name":"a\u0000b"}` to `POST /api/projects` (the same with a task title, a task or project description, a comment text, a project or task edit); `POST /api/session` with `"loginName":"a\u0000b"`; `GET /api/projects?name=a%00b`; and the same texts with `\ud800` (an unpaired surrogate) instead of `\u0000`.
- **Expected:** a refusal that says what is wrong, or a creation that is stored exactly as answered; never a server error and never a different text (the exercise's aim is that the application keeps the data; the contract in `openapi.yaml` says `422` for well-formed content that does not satisfy the schema, and a request that fails changes nothing).
- **Observed (before the fix, running application and tests):** U+0000 gave `500 An unexpected error occurred.` on all of the operations above (the search gave `500` too, and the sign-in `500`). An unpaired surrogate gave `201` and the answer showed the text as sent, but the database held `a?b` (bytes `61 3f 62`), so the next read showed a different text. 12 of the first 33 cases of the test failed; with the regression tests added, 32 of 54 failed.
- **Evidence:** the log of the failing cases shows `PSQLException: invalid byte sequence for encoding "UTF8": 0x00` under `DataIntegrityViolationException` handled by `ProblemDetailsExceptionHandler` as “Unexpected error”; the stored value was read with `SELECT name, encode(convert_to(name,'UTF8'),'hex')`.
- **Cause:** nothing validated the characters of a text before it was sent to PostgreSQL. PostgreSQL's `text` cannot hold U+0000, so the insert fails (a conclusion, shown by the log); and an unpaired surrogate cannot be encoded as UTF-8, so the driver replaces it with `?` (a hypothesis, supported by the stored bytes; I did not trace the driver). Other unusual characters (zero-width characters, BEL, U+202E, CR/LF) round-trip unchanged and are not defects ([OQ-031](constraints.md#open-questions)).
- **Fix** (approved by the project team's member, option 1): `StorableText.isStorable` in each of the modules `projects`, `tasks`, and `identity`; the five value objects `ProjectName`, `ProjectDescription`, `TaskTitle`, `TaskDescription`, and `CommentText` refuse such text with a message that names the field (`422`); the two search parameters `name` (`GET /api/projects` and `eligible-participants`) answer `400`; `SignIn` treats such a login name like an unknown login name (`401`, the password still checked). New [A-037](constraints.md#assumptions) and [OQ-031](constraints.md#open-questions) record the rule and what it leaves open. The messages and codes are not confirmed by the client.
- **Regression tests:** `UnusualTextTests` (55): `anUnusualTextIsRefusedOrKeptWholeAndNeverAnError` (33, decision-neutral: never a server error, and what is kept is what was answered), and the new `aTextTheDatabaseCannotKeepIsRefusedOnItsFieldAndNothingIsStored` (12), `aDescriptionTheDatabaseCannotKeepIsRefusedWithNothingStored` (2), `anEditWithATextTheDatabaseCannotKeepRefusesItAndKeepsTheOldValues` (2), `aSearchTextTheDatabaseCannotKeepIsABadRequest` (4), `aSignInWithATextTheDatabaseCannotKeepIsAnIncorrectLoginLikeAnUnknownLoginName` (2). Without the fix: 32 of 54 failed. With the fix: 55 of 55 passed, and `ProjectApiTests` (67), `TaskApiTests` (93), `CommentApiTests` (36), `SignInTests` (27), `ParticipantsApiTests` (28), `ProjectDomainTests` (22), `TaskDomainTests` (19), `CommentTextTests` (11), `RequestSchemaTests` (13), `LayeringTests` (5), and `ModuleBoundaryTests` (4) passed. Command: `./mvnw -B test -Dtest='UnusualTextTests,…'`, JDK 25. The full `./mvnw -B clean verify` also passed afterwards (685 backend tests, 23 Vitest tests).
- **Live re-check** (jar rebuilt after the fix): `POST /api/projects` with `a\u0000b` and with `a\ud800b` → `422` with the pointer `/name` and “The name contains a character that cannot be stored.”; `POST /api/session` with a NUL login name → `401` “The login name or password is incorrect.”; `GET /api/projects?name=a%00b` → `400`; a project named with a zero-width space (U+200B) → `201`, accepted ([OQ-031](constraints.md#open-questions)).

## Commands run

All with JDK 25 (`JAVA_HOME` set for each command), from the repository root, on the branch `week3-ex8`.

| Command | Purpose | Result |
|---|---|---|
| `./mvnw -B clean verify` | Baseline, before any change | BUILD SUCCESS; 626 backend and 23 Vitest tests |
| `./mvnw -B test -Dtest=ParticipantsApiTests` | Bug 1 regression test, red then green | 28 tests, 1 failure without the fix; 0 with it |
| `./mvnw -B test -Dtest=UnusualTextTests` | Bug 2 and the validation test | 54 tests, 32 failures without the fix; 55 tests, 0 failures with it |
| `./mvnw -B test -Dtest=CrossProjectAccessTests`, `…DataAfterRestartTests`, `…UnusualTextTests` | The three new tests, intact and with the rule broken | [Step 4](#the-three-automated-tests) |
| `./mvnw -B clean verify` | Final build | BUILD SUCCESS; 685 backend and 23 Vitest tests |
| `docker compose -p matriz-ex8 … up -d --wait`, `java -jar target/matriz-0.1.0-SNAPSHOT.jar` (port 18080), `curl` | The manual checks of cases 01–09 and the re-checks | As recorded in each case |
| A throwaway browser script (headless Chromium, kept outside the repository) | The browser probes of Case 09, with request interception | As recorded in Case 09 |
| A Python script (read-only) over the Markdown files | Link and anchor check | 2562 links, 0 broken |
| `git clone` of the branch, `./mvnw -B package -DskipTests`, start of the jar | README step 3 on a fresh clone | Health `UP`, interface `200` |

## Limitations

- **Manual checks prove one run.** They are not repeatable tests; where a check matters, a test now exists or is proposed.
- **Browser probes through my own throwaway script** (headless Chromium), because the project's Playwright tests run only inside the Java build. The failures were forced with request interception, so those responses are stubs. I did not use any other browser, and I did not test narrow screens by hand.
- **Not a clean machine.** The Maven, Node.js, npm, Docker image, and Chromium downloads were cached.
- **The restart** is a graceful stop and start of the Java process (and, in the automated test, of the Spring context). A crash, a restart of PostgreSQL, or a restore from a backup were not run.
- **The data are small.** No check used more than a few dozen records, so nothing here says how the application behaves with many projects, tasks, or people ([OQ-010](constraints.md#open-questions)).
- **Fixes after the baseline** were checked by the tests and by a repeat of only the affected manual checks (set A of Case 08 and the Bug 2 requests), not by repeating all nine cases on the final jar.
- **Not independent.** The same assistant wrote the new tests and ran the checks; a human review of the tests' assertions is the check that is missing.

## Not run

- Cases that need a person or a tool I did not have: a screen reader and accessibility checks (NFR-007); response-time, availability, and concurrent-use checks (NFR-004–NFR-006); a legal or privacy review (NFR-003).
- In a browser: a failed *project* deletion, a failed *assignment*, the rendering of markup in a comment, and the time zone of a comment's date ([Case 07](#case-07--comments), [Case 09](#case-09--deletion-and-errors)).
- A crash or PostgreSQL restart ([Case 03](#case-03--task-persistence)).
- The GitHub secret scanning of the hosted repository, and a dedicated secret scanner; a clean-machine run of the README; the whole README with its fixed Compose project (see the [I12 record](evidence/I12-verification.md#readme-on-a-clean-checkout)).
- A criterion-by-criterion walk of every acceptance criterion of US-001–US-026 (see the [I12 record](evidence/I12-verification.md#user-stories-us-001us-026)).

## Known problems

Recorded as known limitations, not fixed, as the project team's member decided. None is a client decision yet.

| Problem | Where it comes from |
|---|---|
| No limit on failed sign-ins: 30 wrong passwords, then the correct one worked | [OQ-027](constraints.md#open-questions); [I12 record](evidence/I12-verification.md#failed-sign-ins-oq-027) |
| A failure while an access token is checked gives `500`, not `401` | [Case 01](#case-01--access-to-the-application); `TokenCheckFailureTests`; [I2 record](evidence/I2-verification.md#13-not-yet-done-and-still-open) |
| A page reload signs the person out | [ADR-005](adr/ADR-005-authentication-with-jwt.md); [README](../README.md#use-the-user-interface) |
| Two alerts can show at once on Task detail, and an old alert is not cleared by a later failure in another panel | [Case 09](#case-09--deletion-and-errors); [I13 record](evidence/I13-user-interface-consolidation-verification.md) |
| A `404` on removing a member shows a generic message in the interface | [Case 09](#case-09--deletion-and-errors) |
| Several assignees and self-assignment are the team's answer, not the client's | [OQ-025](constraints.md#open-questions), [A-020](constraints.md#assumptions) |
| The percentage is rounded by a rule the client has not set | [A-026](constraints.md#assumptions) |
| Every Project Owner can find any person by display name when adding participants | [A-030](constraints.md#assumptions), [OQ-011](constraints.md#open-questions) |
| Soft deletion keeps rows (and a deleted task's comments) with no retention rule | [OQ-023](constraints.md#open-questions), [A-022](constraints.md#assumptions) |
| The member list and the comment list are not paged | [A-034](constraints.md#assumptions), [A-036](constraints.md#assumptions) |
| Invisible characters (zero-width, bidirectional, and control characters other than U+0000) are accepted and kept | [OQ-031](constraints.md#open-questions) |
| NFR-003–NFR-007 cannot be verified; the interface is in English only | [OQ-010](constraints.md#open-questions), [OQ-011](constraints.md#open-questions), [OQ-013](constraints.md#open-questions) |
| The README summary counts are out of date (A-033, OQ-030) | Reported, not changed ([I12 record](evidence/I12-verification.md#readme-on-a-clean-checkout)) |
| The lone-surrogate replacement (`?`) is attributed to the database driver, which I did not trace | [Bug 2](#bug-2--text-with-the-null-character-or-an-unpaired-surrogate-gave-a-server-error-or-was-changed) |

## Open questions for the client

For the client, via the project team; the [register](constraints.md#open-questions) holds the wording.

1. [OQ-027](constraints.md#open-questions): what limit applies to failed sign-ins, and what must happen after it?
2. [OQ-031](constraints.md#open-questions): which invisible or control characters must a name, a title, a description, or a comment refuse or keep?
3. [OQ-025](constraints.md#open-questions) and [A-020](constraints.md#assumptions): are several assignees and self-assignment wanted?
4. [OQ-023](constraints.md#open-questions): for how long are deleted projects and tasks kept, and who may restore them?
5. [OQ-010](constraints.md#open-questions): the response-time, availability, concurrent-use, and accessibility targets, so that NFR-004–NFR-007 can be verified.
6. [OQ-016](constraints.md#open-questions): does the client approve the MVP?

For the project team: review [A-037](constraints.md#assumptions) (the refusal rule of Bug 2), add the missing identifiers to the tests of EX-013, EX-015, EX-016, EX-017, EX-019, EX-021, and EX-025, and correct the README counts.

## Delivery review — 2026-11-12

*Added by the assistant following the project team's confirmed decisions in “Lab Worksheet 03 — Review and prepare the delivery”, questions 1–20 and the subsequent findings approvals. This is a new delivery record, not a replacement of the earlier I12 observations. I12 remains Awaiting review; client validation remains pending.*

### Review scope and findings

The fixed comparison was `git diff c888aa9...f716bc4`: ten commits and 309 changed files, including the first implementation. Two independent review axes inspected documented standards and specification conformance. The order was configuration/migrations, Identity/HTTP, Projects/membership, Tasks/assignments/comments/progress, frontend, then contract/documents. Test sources were sampled by behavior and assertion, not read exhaustively. Passing tests do not certify every acceptance criterion.

| Finding | Team decision and disposition | Regression evidence |
|---|---|---|
| Eligible-participant query validation preceded visibility/role checks (ADR-004) | Approved correction; access is checked before query validation and rechecked when listing (`b86dbba`). | New combined blank/NUL/lone-surrogate queries initially returned 400 instead of 404/403; now pass in `ParticipantsApiTests`. |
| Actuator discovery exceeded the health-only decision (ADR-007) | Approved correction; disable discovery (`0112a5a`). | The replacement assertion first failed with 200 instead of 404; `HealthCheckTests` now passes, including health. |
| Frontend trimming differed from [Assumption A-033](constraints.md#assumptions) | Approved correction across validation, description payloads and searches (`3a017be`). | Five new Vitest checks failed first; FEFF/zero-width text is preserved, Java whitespace is trimmed and boundary lengths agree. |
| Invalid-character errors were described as excessive length | Approved [Assumption A-037](constraints.md#assumptions) client validation and accurate field explanations (`03d932b`). | Two new Vitest checks failed first; all 31 frontend tests, typecheck and production build then passed. |
| Accepted extreme pages produced 500 rather than empty pages | Reproduced by the approved gap checks; team separately approved correction (`acbacfc`). | Project/task API regressions failed at `page=2147483647&size=100`, then passed. Filtered counts precede fetches; PostgreSQL receives long offsets, avoiding JPA's integer-offset limit without a smaller business page limit. |
| Implicit HEAD/OPTIONS extended the API beyond ADR-004 | Reproduced by the approved gap checks; team separately approved an API-only correction (`2cbff75`). | Two method cases failed with 200 instead of 405. Declared Allow values and missing/ended-token precedence now pass. Health/static resources are outside the guard. |
| Contract length/status prose differs from implementation | Reconcile under the approved Step C, preserving existing rules. | Existing contract checks are component/property/token assertions, not a complete operation-aware checker. |

No excluded product-feature or dependency scope creep was identified within the inspected surfaces. Local patterns over 102 reachable commits and 675 unique text blobs found no high-confidence real secret/token/application-personal-data exposure. This cannot prove absence; unreachable objects, uncached remote history and hosted scanning were outside scope. Contributor metadata exists. Ignored dotenv contents were not inspected.

### Checks performed

All commands ran with installed JDK 25.0.5 selected for the command (`export JAVA_HOME="$(/usr/libexec/java_home -v 25)"; export PATH="$JAVA_HOME/bin:$PATH"` on this macOS host). The first baseline attempt used the default JDK 21 and stopped at Enforcer before compilation; no global Java configuration or dependency manifest was changed.

- Initial targeted baseline: 414 Java tests, including 43 browser tests, plus 23 Vitest tests; typecheck/build passed.
- `./mvnw -B -Dtest=ParticipantsApiTests,HealthCheckTests,EveryOperationRequiresATokenTests,LayeringTests,ModuleBoundaryTests test`: 85 tests passed after the first two fixes.
- `./mvnw -B -Dtest=ProjectRepositoryTests,TaskRepositoryTests,ProjectApiTests,TaskApiTests,LayeringTests,ModuleBoundaryTests test`: 185 tests passed after the pagination correction.
- `./mvnw -B -Dtest=AccessTokenTests,EveryOperationRequiresATokenTests,HealthCheckTests,LayeringTests,ModuleBoundaryTests test`: 107 tests passed after the method correction.
- From `frontend`, with `PATH="$PWD/node:$PATH"` exported: `npm run typecheck`, `npm test`, `npm run build`: passed; 31 tests.
- Three added browser checks passed: failed project deletion keeps the project with no success notice; failed assignment keeps the selected person with no success notice or assignment; comment markup is literal text, not elements.

The full clean verification and clean-copy exercise are recorded separately when executed. The earlier “not tested” entries remain historical observations; later results supersede only the specifically covered gaps.

### Behavior-preserving refactoring

*Decision confirmed by the project team on 2026-11-12, delivery questions 10–11.* Extract the duplicated controller page/size parsing into `com.matriz.PageParameters`. The benefit is one implementation of defaults, accepted integers and error messages. Alternatives were keeping duplication, consolidating Unicode test vectors, or extracting task access/form mechanics; the latter options were broader or did not simplify production code.

The helper is cross-cutting HTTP code in the base package, the location already authorized by [ADR-008](adr/ADR-008-domain-driven-design.md#1-packages-and-layers), not a new business module or module dependency. Access checks, query-validation order, media handling, transactions, 400 details and defaults remain unchanged. Existing project/task API tests cover defaults, ranges, malformed values, boundaries and access precedence; no uncovered behavior required a new test for this extraction.

With the same command-local JDK selection, both runs used exactly `./mvnw -B clean verify`:

| Run | Java tests | Frontend | Result |
|---|---|---|---|
| Before extraction, commit `f3740fc` | 695; 0 failures/errors/skips | Typecheck, 31 Vitest tests, Vite build passed | BUILD SUCCESS; 3m37s |
| After extraction | 695; 0 failures/errors/skips | Typecheck, 31 Vitest tests, Vite build passed | BUILD SUCCESS; 3m34s |

Both counts include 46 browser tests and the layering/module-boundary checks. The diff removes the two private parsers/constants, adds one base-package helper and changes the four call sites; it does not bundle any behavior correction.

### Contract and documentation reconciliation

*Applied on 2026-11-12 under the project team's confirmed delivery decisions 3–9, 12 and 18.* The draft OpenAPI contract now separates normalized input text from bounded output strings, describes A-037, declares generic 500 responses on all 23 operations, bounds page inputs at 2147483647, covers task pagination in the general list rule and corrects validation examples. Required request text permits exactly 1–N Unicode code points after the approved trim; optional descriptions also permit blank text. These are existing implementation rules, not new product limits.

`ContractTextTests` first failed all nine text/request combinations against the old raw-length schemas. After reconciliation, `./mvnw -B -Dtest=ContractTextTests,RequestSchemaTests,EveryOperationRequiresATokenTests test` passed **68 tests**, with no failures/errors/skips (14.162s). The new checks include padded supplementary characters at each limit, over-limit text, blank text, preserved FEFF, NUL/unpaired-surrogate refusal, all 23 server-error references and both technical page bounds. Existing request-property/media/parameter guards remain unchanged.

This harness validates named JSON schemas plus explicitly asserted route/status/header cases; it is **not** a complete operation-aware OpenAPI/HTTP validator. In particular, property guards alone do not prove every authenticated response or `format`, and a no-token test does not prove a successful mapping. The API suites and source comparison remain necessary.

The register, stories, plan, architecture, historical ADR open points, I12 header and frozen wireframe now have dated corrections/resolution links. Assumption A-024 and unreviewed details of A-034–A-036 keep their pending status; no client answer or increment acceptance is inferred. The logical data model agrees with V1–V6 and needed no migration/model change. AGENTS.md changes only the approved command section and current-phase wording. Compose now supports an isolated host port through `MATRIZ_DB_PORT`, default 5432.

A local checker found **2723 valid relative inline Markdown links/anchors across 30 tracked documents**; external links were not fetched. `docker compose --env-file .env.example config --quiet` and `git diff --check` passed. No developer `.env` or database was used.

### Three examples of AI use

*Selected and confirmed by the project team's member on 2026-11-12, delivery questions 14–16. Goals/decisions below are sourced from that confirmation and the linked repository records; they do not claim that the developer personally ran the commands.*

| Goal of the request | Developer's decision | How it was verified |
|---|---|---|
| Review direct API authorization and check that an outsider cannot discover a project through member removal. | Approve the uniform 404 correction after the AI identified the disclosure; keep the approved permission policy, not a new role rule. | [Bug 1](#bugs-found) and [Case 08](#case-08--authorization-in-the-api) record the failing/passing authorization test and live comparison of existing, missing and malformed projects. |
| Investigate unusual text that could not be persisted unchanged. | Approve 422 on text fields, 400 on searches and generic 401 on login names containing NUL/unpaired surrogates; leave other invisible characters open as [OQ-031](constraints.md#open-questions). | [Bug 2](#bugs-found) records the live read-back check and 55 passing `UnusualTextTests`; the [mutation check](#the-three-automated-tests) made 33 tests fail when the storage guard was disabled. |
| Check invalid-request handling against ADR-004 rather than adopting the AI's suggested status blindly. | **Correct the suggestion:** a well-formed JSON value of the wrong type is 422, not the assistant's proposed 400; reserve 400 for malformed JSON/bad query parameters. | [Project-management decision 11](implementation-plan.md#decisions-affecting-the-increments) records the correction. `ProjectApiTests` asserts the 422/field-error behavior; it also ran in both successful [before/after refactoring checks](#behavior-preserving-refactoring). |

### Clean-copy delivery exercise

Executed on 2026-11-12 under delivery decisions 18–19. The committed candidate was cloned locally with `git clone --no-hardlinks --branch week3-ex9`; no ignored configuration or generated dependencies were copied. The first clone at `63a59d0` passed full verification (705 Java tests, 31 Vitest, 3m40s). A final read-only reviewer found one documentation-only error: the new README unassignment summary said 204 although the existing controller/OpenAPI return 200 with a Task. It was independently checked and corrected in `a88c428`; there were no Critical/Important production-code findings. The fresh-clone recipe was then repeated from that corrected commit.

Only the README was needed to configure/build/start the application. Its UI steps were automated with a throwaway Playwright source launcher outside the repository, using existing test dependencies; this is probe instrumentation, not an extra application setup step. The two temporary clones used unique paths; only the second started Compose project `matriz-ex9-20261112215110`, database port 15439 and application port 18089.

| README step / command | Observed output |
|---|---|
| Clone, configuration derived from `.env.example`, `docker compose config --quiet` | Clean tracked tree; generated `.env` mode 600; only fictitious test passwords and a privately generated signing key. No developer `.env` read or copied. |
| `./mvnw -B clean verify` in the corrected fresh clone | **BUILD SUCCESS**, 3m37s; **705 Java tests, 0 failures/errors/skips**, including 46 browser tests; **31 Vitest**, TypeScript and Vite build passed. |
| `docker compose up -d --wait` | Only the named project's container/network/volume created; PostgreSQL 18.6 healthy on loopback port 15439. |
| `java -jar target/matriz-0.1.0-SNAPSHOT.jar --server.port=18089` | Seven successful migrations: V1, demo 1.1, V2–V6; Tomcat on 18089; application started in 2.938s. |
| Health request and the two README `psql` queries | Overall/db `UP`; Flyway at V6; exactly **4 people / 4 projects / 8 memberships / 9 tasks / 3 assignments / 2 comments** before adding verification rows. |
| README sign-in and `/api/me` | Fictitious Ana authenticated; returned her fixed identifier/display name. The token was held only in the live API terminal's memory, never printed. |
| README project/task creation | Project `aa171762-0765-4560-b329-b29d5ce9a533`, “Clean-copy delivery project”, Active; task `96c2cade-a918-469d-86bc-9fd162f2cab4`, “Clean-copy persistence task”, To do / High / no assignees. |
| Open UI, sign in, open project/task, reload and sign in again | Live Chromium probe passed: same project/task displayed after reload, direct route preserved, no page errors. |
| Stop only the backend and repeat the exact run command | PostgreSQL remained running; startup 3.052s; “Schema public is up to date. No migration necessary.” |
| Repeat health and the two authenticated GET requests; repeat the UI probe | Same token remained valid; same identifiers/content persisted, project progress now counted one To do task; UI/reload/re-sign-in passed again. |

Cleanup was **explicitly authorized immediately beforehand** for the exact resources. The backend was stopped; `docker compose down -v` removed only the named project's container, network and volume; filtered Docker queries confirmed all gone. Both exact clone directories (including their temporary `.env` files) were removed. The UI probe's first default Playwright initialization unexpectedly downloaded unused Firefox/WebKit caches; the probe then disabled auto-download, and only those two newly created cache directories were removed with approval. Existing Chromium/build caches, the original checkout/database and pre-existing containers were not removed. Port 18089 no longer served the application; the original working tree remained clean.

This verifies the README on a clean copy on **macOS**, not every platform or every story in a complete manual walkthrough. No tag, push, merge, rebase, client approval or increment acceptance occurred.
