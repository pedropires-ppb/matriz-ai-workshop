# Matriz — Verification of the nine scenarios (exercise “Verify the MVP”, 2026-11-12)

**Status:** In progress. Part of increment [I12](implementation-plan.md#i12--release-verification), which is Under way. The rest of I12 is in the [I12 verification record](evidence/I12-verification.md). Nothing here is a client approval ([OQ-016](constraints.md#open-questions)).

**Decisions of the project team's member for this exercise** (the conversation “Verify the MVP”): the scope is the nine scenarios plus the rest of I12; the evidence is text only, with every secret masked and fictitious names only; the automated tests use their own databases, and the manual checks use a fresh database, not the developer's local one; a known problem is recorded as a known limitation and not fixed; a defect is fixed only after the diagnosis is approved.

## Summary

| Case | Scenario | Type | Status |
|---|---|---|---|
| 01 | Access to the application | Automated and manual | Confirmed |
| 02 | Project management | Automated and manual | Confirmed |
| 03 | Task persistence | Automated and manual | Partly confirmed (no automated restart test yet; added in Step 4) |
| 04 | Data validation | Automated and manual | Confirmed (control and zero-width text pending in Step 4) |
| 05 | State and priority | Automated and manual | Confirmed |
| 06 | Assignees | Automated and manual | Confirmed |
| 07 | Comments | Automated and manual | Confirmed (browser rendering of markup not checked) |
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

## Case 04 — Data validation

- **Expected:** empty required fields, fields with only spaces, and invalid values are rejected, nothing is stored, and the person is told what is wrong ([US-001](user-stories.md#us-001--create-project), [US-003](user-stories.md#us-003--create-task), [US-008](user-stories.md#us-008--rename-project), [US-011](user-stories.md#us-011--rename-task), [US-021](user-stories.md#us-021--add-comment); [EX-010](user-stories.md#edge-cases-and-exceptions), [EX-011](user-stories.md#edge-cases-and-exceptions); [A-003](constraints.md#assumptions), [A-033](constraints.md#assumptions)).
- **Type:** automated and manual. **Status: Confirmed** for the three kinds of input the case names; text with control or zero-width characters is a separate probe in Step 4 and is not part of this status.
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

- **Not verified here:** how the browser form shows each message was checked only by the two `UserInterfaceTests` above. Text made of control characters, zero-width characters, or lone surrogates is covered by no rule or decision ([A-033](constraints.md#assumptions) names only whitespace); it is probed by the new validation test in Step 4. Limits are in characters as the API counts them; a count by user-perceived character (emoji, combining marks) was not tested.

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
