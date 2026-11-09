# Matriz — Verification record for I5, Progress and My projects (2026-11-09)

**Status:** Evidence for the project team's review of increment [I5](../implementation-plan.md#i5--progress-and-my-projects). Nothing here is accepted. Only the project team accepts an increment ([plan rule 4](../implementation-plan.md#increment-status)). The decisions used are the I5 decisions, recorded in [Decisions affecting the increments](../implementation-plan.md#decisions-affecting-the-increments).

**Environment:** macOS on arm64; JDK 25 (Temurin); Maven through the wrapper; Node.js 24, installed by the build; Vitest; Playwright with Chromium; Docker; PostgreSQL 18 in the tests. `gh` is not authenticated on this machine, so no continuous integration run is linked. The work is on the branch `week3-ex7`, which is not pushed.

## 1. What was built

| Area | Result |
|---|---|
| Domain | `progress/domain/Progress`: the counts by state, and the percentage Done rounded half up ([Assumption A-026](../constraints.md#assumptions)). The percentage is absent when there are no tasks ([EX-005](../user-stories.md#edge-cases-and-exceptions)). Negative counts are refused. |
| Modules | Projects' API defines `ProjectProgress`, which Progress's application layer implements. Tasks' API offers `TaskCounting`: one grouped query per call, which leaves deleted tasks out. This amends architecture module rule 2 and [ADR-008](../adr/ADR-008-domain-driven-design.md) (I5 decision 1, flagged in both). |
| Migration | None (I5 decision 2). Performance is checked in I12. |
| Contract | `Project.progress` (`taskCounts` and `percentDone`, which is null with no tasks) and `ProjectSummary.taskCounts` are required again in `openapi.yaml`. There are no totals across projects ([FR-024](../requirements.md#functional-requirements)). |
| Project detail | A “Progress” panel, as wireframe marker 4 draws it: “N% Done”, a `<progress>` bar that repeats the figure, and “1 To do, 1 In progress, 2 Done”. With no tasks, it shows “No tasks” and no bar (I5 decisions 3 and 5). From 720 pixels, the panel sits above the tasks in the second column. |
| Home and Projects | Each row, from one shared `ProjectRow` component, shows its own counts by state, with no percentage and no total across projects (I5 decision 4). |
| Synchronisation | The figures always come from the API. Creating a task inline fetches the project again. Deleting a task, or changing one on Task detail, is followed by the Project detail opening again, which fetches the project. Editing the project shows the confirmed answer, which includes the progress. The filters change only the task list, never the progress ([EX-026](../user-stories.md#edge-cases-and-exceptions)). |

Commits: `45589e6` (the decisions), `1949cfd` (the backend and the contract), `77b7d18` (the User interface), and the commit of the end-to-end tests and this record.

## 2. Checks

| Check | Command | Result |
|---|---|---|
| Whole build | `./mvnw -B clean verify` (JDK 25) | BUILD SUCCESS: 553 Java tests, 0 failures, 0 errors |
| Front-end type check | `tsc --noEmit -p tsconfig.json` (in the build) | Passed |
| Front-end unit tests | `npm test` (Vitest, in the build) | 5 files, 23 tests passed |
| Rounding | `ProgressDomainTests` (14, new) | 1 of 3 is 33%, 2 of 3 is 67%, 1 of 8 is 13%, 1 of 200 is 1%, 199 of 200 is 100%, 1 of 201 is 0% (halves up); In progress counts in the total only; no tasks gives no percentage; negative counts are refused |
| API | `ProgressApiTests` (9, new); `ProjectApiTests` (67) and `TaskApiTests` (93), unchanged and passing against the stricter contract | The counts per project; deleted tasks are left out; no totals across projects; `percentDone` is null with no tasks; the figures follow each task change and ignore the task filters; a Completed project keeps its figures |
| Layering and module boundaries | `LayeringTests` (5), `ModuleBoundaryTests` (4) | Passed |
| Contract | The API tests check the answers against `openapi.yaml` | Passed |
| End to end | `UserInterfaceTests` (Playwright, Chromium) | 37 tests passed: 1 new, and the walkthrough extended at 390 and 1280 pixels |

```text
$ ./mvnw -B clean verify
…
[INFO]  Test Files  5 passed (5)
[INFO]       Tests  23 passed (23)
…
[INFO] Tests run: 14, Failures: 0, Errors: 0, Skipped: 0 -- in com.matriz.progress.ProgressDomainTests
[INFO] Tests run: 4, Failures: 0, Errors: 0, Skipped: 0 -- in com.matriz.ModuleBoundaryTests
[INFO] Tests run: 5, Failures: 0, Errors: 0, Skipped: 0 -- in com.matriz.LayeringTests
[INFO] Tests run: 9, Failures: 0, Errors: 0, Skipped: 0 -- in com.matriz.ProgressApiTests
[INFO] Tests run: 67, Failures: 0, Errors: 0, Skipped: 0 -- in com.matriz.ProjectApiTests
…
[INFO] Tests run: 553, Failures: 0, Errors: 0, Skipped: 0
[INFO] BUILD SUCCESS

$ ./mvnw -B test -Dtest=UserInterfaceTests
[INFO] Tests run: 37, Failures: 0, Errors: 0, Skipped: 0 -- in com.matriz.UserInterfaceTests
```

## 3. End-to-end tests for I5

| Test | What it shows |
|---|---|
| `theFiveScreensServeEachKindOfPerson`, at 390 and at 1280 (extended) | **Ana** (Project Owner): on Home, Mobile App shows “1 To do, 1 In progress, 0 Done” and Website Redesign “0 To do, 0 In progress, 0 Done”. The Projects row shows the same counts. The Project detail shows “0% Done”. After she sets a task to Done on Task detail, “Back to the project”, with the To do filter still on, shows “50% Done” and “0 To do, 1 In progress, 1 Done”. Home shows the new counts. **Rui** (Project Participant) and **Eva** (not a member of Mobile App) are unchanged. Eva does not see Mobile App or its figures. **Daniel** (a member of no project) creates Side Project, which shows “No tasks” with no bar, and Home shows its zero counts. |
| `theProgressAndTheCountsFollowEachChangeButNotTheFilters` (1280, new) | Home and Projects list four projects with their own counts, and no percentage. A Completed project (a SQL fixture) shows its counts and “50% Done” ([US-025](../user-stories.md#us-025--view-my-projects)). Empty shows “No tasks”, with no bar ([EX-005](../user-stories.md#edge-cases-and-exceptions)). 1 of 3 Done shows “33% Done” ([EX-027](../user-stories.md#edge-cases-and-exceptions)). With 1/1/2, the panel shows “50% Done”, and it stays at 50% with the Done filter on ([EX-026](../user-stories.md#edge-cases-and-exceptions)). Deleting the To do task shows “67% Done” and “0 To do, 1 In progress, 2 Done” ([US-017](../user-stories.md#us-017--delete-task) criterion 1). Creating a task inline shows “50% Done” at once. Home then shows “1 To do, 1 In progress, 2 Done”. The database records the deleted task as deleted. |

The states (loading, an empty list, success, and error), the data kept after a failed submission, a repeated click sending one request, and a 401 sending the person to Sign-in with “Your session has ended. Please sign in again.” are still covered by the I13 tests. All of them pass with the new panel.

## 4. Mocks, fixtures, and secrets

```text
$ grep -rniE "mock|stub|fixture|fake|until I5|come with I5" frontend/src
frontend/src/http.test.ts:15:  vi.unstubAllGlobals();
frontend/src/http.test.ts:40:    vi.stubGlobal('fetch', fetch);
frontend/src/http.test.ts:44:    const [address, init] = fetch.mock.calls[0];
frontend/src/http.test.ts:54:    vi.stubGlobal('fetch', vi.fn(async () => json(401, {})));
```

Only the unit test of `http.ts` replaces `fetch`. The figures come from the real API and database. The comment that said the counts “come with I5” is removed. The diff contains no literal password, secret, key, or token. `.env` and the ignored files are not staged. The test people are invented demonstration names.

## 5. The wireframe

Marker 4 (Progress) of the [Project dashboard wireframe](../ui/project-dashboard.md) is now built: the title, the percentage with “Done”, the bar, and the counts. The bar is omitted when there are no tasks, as the wireframe's notes say. Two differences remain:

- The counts read as one line, “1 To do, 1 In progress, 2 Done”, where the wireframe shows them as a list (I5 decision 5).
- At 390 pixels, the panel comes after the details, and before the members and the tasks.

Markers 3 (Complete project, I8) and 11–12 (adding and removing participants, the rest of I6) are still not built.

## 6. Still open

- [Assumption A-026](../constraints.md#assumptions): the rounding is the team's proposal. The client has not validated it.
- Performance with many projects and tasks is checked in I12 (I5 decision 2).
- A project's state is set to Completed only by a SQL fixture in the tests, until I8.
