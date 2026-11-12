# Matriz — Verification record for I8, Completion and simultaneous changes (2026-11-09)

**Status:** This is evidence for the project team's review of increment [I8](../implementation-plan.md#i8--completion-and-simultaneous-changes). Nothing in it is accepted yet, because only the project team accepts an increment ([plan rule 4](../implementation-plan.md#increment-status)).

The decisions used are the I8 decisions, recorded in [Decisions affecting the increments](../implementation-plan.md#decisions-affecting-the-increments). I8 started while [I5](../implementation-plan.md#i5--progress-and-my-projects) was Awaiting review, as an exception to rule 2 (I8 decision 1). If the review of I5 requests changes, I8 is verified again.

**Environment:**

- macOS on arm64, JDK 25 (Temurin), and Maven through the wrapper.
- Node.js 24, installed by the build, and Vitest.
- Playwright with Chromium.
- Docker, with PostgreSQL 18 in the tests.

`gh` is not authenticated on this machine, so no continuous integration run is linked. The work is on the branch `week3-ex7`, which is not pushed.

## 1. What was built

| Area | Result |
|---|---|
| Domain | `Project.complete(actor, hasUnfinishedTasks)` guards the rules: only the Project Owner completes, only an Active project that is not deleted, and only when no task is unfinished ([Assumptions A-001, A-007, A-011](../constraints.md#assumptions)). Completing cannot be undone ([OQ-006](../constraints.md#open-questions)). `ProjectAction.COMPLETE` follows the same access rules as Edit and Delete. |
| Modules | Projects' API defines `ProjectTasks`. Tasks implements it in its application layer as `ProjectTasksProvider`, which runs with `@Transactional(MANDATORY)` and one query. Deleted tasks are not counted as unfinished ([EX-018](../user-stories.md#edge-cases-and-exceptions)). This follows I8 decision 2. |
| Application | `CompleteProject` locks the project's row, decides the access, checks for unfinished tasks, and changes the state, all in one transaction ([ADR-006](../adr/ADR-006-simultaneous-changes.md); [Assumption A-029](../constraints.md#assumptions)). If a task is created or reopened at the same moment, that request waits for the lock and then finds the project Completed. |
| API | `PUT /api/projects/{projectId}/state` with `{"state":"COMPLETED"}`. The checks run in the order of [ADR-004](../adr/ADR-004-rest-api-single-entry-point.md): 404 or 403, then 415, 400, and 422, then 409. A 409 comes either from unfinished tasks or from a project that is already Completed. The 200 response is the project, with its progress. No migration was needed, and `openapi.yaml` already described the operation, so it was not changed. |
| Project detail | "Complete project" sits in the actions of the details panel, between Edit and Delete, and only the Project Owner of an Active project sees it. It is disabled while the API's counts show a To do or In progress task, with "Unavailable while tasks are To do or In progress." linked by `aria-describedby`. Clicking it asks for a confirmation, as deleting does, and on success shows "The project has been completed." (I8 decisions 4 and 5). |
| A refused completion | On a 409, the User interface fetches the project and its tasks again. It then shows "The project has tasks that are To do or In progress; it remains Active." or "The project is Completed; it cannot be changed." (I8 decision 6). |
| Read-only view | A Completed project shows the "Completed" badge, and no Edit, Delete, Complete, or New task. Its tasks are read-only, but comments can still be added. |

Commits:

- `8973e7d`: the decisions;
- `a360f5a`: the backend;
- `3017010`: the User interface and the README;
- `aeda760`: the API and end-to-end tests;
- the commit that adds this record and the review fix.

## 2. Checks

| Check | Command | Result |
|---|---|---|
| Whole build | `./mvnw -B clean verify` (JDK 25) | BUILD SUCCESS: 590 Java tests, 0 failures, 0 errors |
| Front-end type check | `tsc --noEmit -p tsconfig.json` (in the build) | Passed |
| Front-end unit tests | `npm test` (Vitest, in the build) | 5 files, 23 tests passed |
| Domain and rules | `ProjectDomainTests` (2 new), `ProjectAccessRulesTests` (extended to COMPLETE), and `TaskRepositoryTests` (1 new) | Passed. Completing is refused for a Participant, a Completed project, and a project with unfinished tasks. Only To do and In progress tasks that are not deleted count as unfinished. |
| API | `CompletionApiTests` (32 tests, all new) | See the table below. |
| Layering and module boundaries | `LayeringTests` (5), `ModuleBoundaryTests` (4) | Passed |
| Contract | `RequestSchemaTests` (13), `EveryOperationRequiresATokenTests` (45), and the API tests, which check the answers against `openapi.yaml` | Passed |
| End to end | `UserInterfaceTests` (Playwright, Chromium) | 39 tests passed: 2 new, and the walkthrough and the I5 test extended |

`CompletionApiTests` covers these cases:

- **Successful completion:** 200 with the progress, for a project with zero tasks or only Done tasks ([US-010](../user-stories.md#us-010--complete-project)).
- **Unfinished tasks:** a project with a To do or In progress task gets 409, and stays Active ([EX-006](../user-stories.md#edge-cases-and-exceptions)). Deleted tasks are not counted.
- **Access:** a Participant gets 403. A non-member, an unknown project, or an identifier that is not a UUID gets 404. A request without a token gets 401.
- **Order of the checks:** 415, 400, and 422 come only after the access checks.
- **Changes to a Completed project:** 9 modifying operations get 409, and the database snapshot is unchanged ([EX-009](../user-stories.md#edge-cases-and-exceptions)).
- **The assignee:** the assignee of a task in a Completed project cannot change its state, assign, or unassign ([EX-008, EX-022](../user-stories.md#edge-cases-and-exceptions)).
- **Comments:** adding a comment still gets 201.
- **Simultaneous changes:**
  - completing while a task is reopened, 5 rounds;
  - completing while a task is created, 5 rounds;
  - completing twice at once, where exactly one request succeeds.

  In every round, no Completed project ends up with an unfinished task.

```text
$ ./mvnw -B clean verify
…
[INFO]  Test Files  5 passed (5)
[INFO]       Tests  23 passed (23)
…
[INFO] Tests run: 9, Failures: 0, Errors: 0, Skipped: 0 -- in com.matriz.TaskRepositoryTests
[INFO] Tests run: 39, Failures: 0, Errors: 0, Skipped: 0 -- in com.matriz.UserInterfaceTests
[INFO] Tests run: 32, Failures: 0, Errors: 0, Skipped: 0 -- in com.matriz.CompletionApiTests
[INFO] Tests run: 4, Failures: 0, Errors: 0, Skipped: 0 -- in com.matriz.accesscontrol.application.ProjectAccessRulesTests
[INFO] Tests run: 18, Failures: 0, Errors: 0, Skipped: 0 -- in com.matriz.projects.ProjectDomainTests
[INFO] Tests run: 13, Failures: 0, Errors: 0, Skipped: 0 -- in com.matriz.RequestSchemaTests
[INFO] Tests run: 93, Failures: 0, Errors: 0, Skipped: 0 -- in com.matriz.TaskApiTests
[INFO] Tests run: 45, Failures: 0, Errors: 0, Skipped: 0 -- in com.matriz.EveryOperationRequiresATokenTests
[INFO] Tests run: 4, Failures: 0, Errors: 0, Skipped: 0 -- in com.matriz.ModuleBoundaryTests
[INFO] Tests run: 5, Failures: 0, Errors: 0, Skipped: 0 -- in com.matriz.LayeringTests
[INFO] Tests run: 67, Failures: 0, Errors: 0, Skipped: 0 -- in com.matriz.ProjectApiTests
…
[INFO] Tests run: 590, Failures: 0, Errors: 0, Skipped: 0
[INFO] BUILD SUCCESS
```

## 3. End-to-end tests for I8

| Test | What it shows |
|---|---|
| `theFiveScreensServeEachKindOfPerson`, at 390 and at 1280 pixels (extended) | **Rui**, a Project Participant, sees no "Complete project". **Daniel**, a member of no other project, completes his empty Side Project through the confirmation. He then sees "Completed", and no Edit, Delete, Complete, or New task, and Home shows "Completed". |
| `theProjectOwnerCompletesAProjectOnlyWhenNoTaskIsUnfinished` (1280 pixels, new) | **Disabled control:** with a To do task, **Ana**'s button is disabled, with its reason linked. When the task is set to Done, the button becomes enabled.<br>**Cancel:** sends no request.<br>**A 409 race:** Rui reopens the task through the API while Ana is about to confirm. Ana gets the "remains Active" message, fresh counts, the task row shows "To do", and the button is disabled again.<br>**A repeated click:** a double click on "Complete" sends one request.<br>**After completion:** the project, its task, Home, and Projects show "Completed". Rui can still add a comment. **Eva**, who is not a member, gets "not accessible".<br>**Database:** the project is `COMPLETED`, and the comment is stored. |
| `aSessionEndedElsewhereSendsThePersonToSignInWhenTheyCompleteAProject` (new) | **A 401:** the person is taken to Sign-in, with "Your session has ended. Please sign in again." The project stays Active. |
| `theProgressAndTheCountsFollowEachChangeButNotTheFilters` (changed) | **No SQL fixture:** the Archive project now has two Done tasks, and is completed through the User interface. Its row then shows "0 To do, 0 In progress, 2 Done", and its panel "100% Done". |

The loading, empty, success, and error states are still covered by the I13 tests, and so is a failed submission keeping the entered data. All of them pass.

## 4. Mocks, fixtures, and secrets

```text
$ grep -rniE "mock|stub|fixture|fake|until I8|with I8|not built" frontend/src
frontend/src/http.test.ts:15:  vi.unstubAllGlobals();
frontend/src/http.test.ts:40:    vi.stubGlobal('fetch', fetch);
frontend/src/http.test.ts:44:    const [address, init] = fetch.mock.calls[0];
frontend/src/http.test.ts:54:    vi.stubGlobal('fetch', vi.fn(async () => json(401, {})));
```

**Mocks:** only the unit test of `http.ts` replaces `fetch`.

**SQL fixtures:** the end-to-end tests no longer set a project to Completed with SQL. The API tests of earlier increments still do, and that is decided (I8 decision 8).

**Secrets and personal data:**

- The diff contains no literal password, secret, key, or token.
- `.env` and the ignored files are not staged.
- The test people have invented demonstration names.

## 5. The wireframe

Marker 3 (Complete project) of the [Project dashboard wireframe](../ui/project-dashboard.md) is now built, with two differences from the drawing:

- It sits in the actions of the details panel, not in the header (I8 decision 5).
- Variant C, a Completed project, is shown read-only: no edit or create controls, and comments are still allowed.

Markers 11 and 12 (adding and removing participants, the rest of I6) are still not built.

## 6. Still open

- **Completing a project with zero tasks:** allowed, but the client has not confirmed it ([Assumption A-007](../constraints.md#assumptions); [OQ-006](../constraints.md#open-questions)). Reopening a project is not defined.
- ~~**The exception to rule 2:** I8 depends on I5, which is still Awaiting review.~~ *Resolved on 2026-11-12: the project team accepted I5, I8, and I6 (the conversation “Implement the Matriz front end”, the acceptance review).*
- **The style of the Complete button:** in the confirmation, "Complete" uses the shared danger style of `ConfirmBox`, as "Delete" does. The I8 decisions did not settle the style, so it is left to the team's review.
- **Simultaneous change moved to I6:** removing a participant while that participant assigns themselves is now tested in I6 (I8 decision 7).
