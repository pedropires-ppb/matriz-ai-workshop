# Matriz — Verification record for I13, User interface consolidation (2026-11-09)

**Status:** Evidence for the project team's review of increment [I13](../implementation-plan.md#i13--user-interface-consolidation). Nothing here is accepted: only the project team accepts an increment ([plan rule 4](../implementation-plan.md#increment-status)). The decisions are the team's scope decisions and I13 decisions, recorded in [Decisions affecting the increments](../implementation-plan.md#decisions-affecting-the-increments), from the conversation “Implement the Matriz front end”. The client's validation is pending.

**Environment:** macOS on arm64; JDK 25 (Temurin); Maven through the wrapper; Node.js 24 installed by the build; Vitest; Playwright with Chromium; Docker; PostgreSQL 18 in the tests. `gh` is not authenticated on this machine, so no continuous integration run is linked. The work is on the branch `week3-ex7`, not pushed.

## 1. What was built

| Area | Result |
|---|---|
| Screens | Sign-in, Home (`#/`), Projects (`#/projects`, `#/projects/new`), Project detail (`#/projects/{id}`), and Task detail (`#/projects/{id}/tasks/{taskId}`). The search, the filters, and the page are kept in the address. An unknown address leads to Home. |
| Backend address | Set in one place: `frontend/src/config.ts`, from `VITE_API_BASE_URL` at build time. It is empty by default, which means the same origin. Only the same origin is tested: another origin would need CORS, which is not configured. |
| API client | One `call` in `http.ts` adds the token, reads the answer, and turns a 401 into the end of the session. |
| Shared parts | `components.tsx` (fields, buttons with a busy state, notices, Loading, LoadFailed with Retry, empty states, paging, state badges, confirmation), `styles.css` (CSS variables, one breakpoint at 720 px), `validation.ts`, and `messages.ts` (English, [Assumption A-028](../constraints.md#assumptions)). The old `projectValidation.ts` and `taskValidation.ts` are merged into `validation.ts`. |
| States | `useLoad` gives loading, failed (with Retry), and ready. It keeps the value while it refreshes, so a change does not blank the list. `useSubmit` with `Gate` lets only one submission run at a time. |
| Synchronisation | The changed record is shown from the confirmed answer. The affected list page and its totals are then fetched again from the API. Totals come from `totalItems`, never from the rows shown. |
| Reload | A reload still signs the person out ([ADR-005](../adr/ADR-005-authentication-with-jwt.md)). The address is kept, so the screen opens again after sign-in. |
| I7 defect | The task list now shows the assignees of each task (“Assigned to: …” or “No one is assigned.”), as [US-004](../user-stories.md#us-004--view-tasks) criterion 1 requires (I13 decision 5). |

## 2. Checks

| Check | Command | Result |
|---|---|---|
| Whole build | `./mvnw -B clean verify` (JDK 25) | BUILD SUCCESS: 529 Java tests, 0 failures, 0 errors |
| Front-end type check | `tsc --noEmit -p tsconfig.json` (in the build) | Passed |
| Front-end unit tests | `npm test` (Vitest, in the build) | 5 files, 23 tests passed: `router`, `gate`, `config`, `validation`, `http` |
| Layering and module boundaries | `LayeringTests` (5), `ModuleBoundaryTests` (4) | Passed |
| Contract | The API tests check the answers against `openapi.yaml` (`Contract`) | Passed; `openapi.yaml` did not change |
| End to end | `UserInterfaceTests` (Playwright, Chromium) | 36 tests passed (30 updated, 6 new) |

```text
$ ./mvnw -B clean verify
…
[INFO] > tsc --noEmit -p tsconfig.json
[INFO]  Test Files  5 passed (5)
[INFO]       Tests  23 passed (23)
…
[INFO] Tests run: 36, Failures: 0, Errors: 0, Skipped: 0 -- in com.matriz.UserInterfaceTests
[INFO] Tests run: 4, Failures: 0, Errors: 0, Skipped: 0 -- in com.matriz.ModuleBoundaryTests
[INFO] Tests run: 5, Failures: 0, Errors: 0, Skipped: 0 -- in com.matriz.LayeringTests
…
[INFO] Tests run: 529, Failures: 0, Errors: 0, Skipped: 0
[INFO] BUILD SUCCESS
```

## 3. End-to-end tests added for I13

| Test | What it shows |
|---|---|
| `theFiveScreensServeEachKindOfPerson` at 390 and at 1280 | **Ana** (Project Owner of Mobile App): Home shows “Welcome, Ana Silva.” and “2 projects”. She searches Projects and opens Mobile App, sees the members and “2 tasks”, and sees the assignees in each row. She filters To do, opens the task (the filter stays in the address), comments, and sets it to Done. “Back to the project” keeps the filter, which now shows “No task matches the filters.” and “0 tasks”. **Rui** (Project Participant): sees “1 project”, has no New task or Edit project, reads Ana's comment, and changes the state as an assignee. **Eva** (a member of another project): a deep link to a Mobile App task shows “This project does not exist or is not accessible.” and “Go to Home”, and the task is not shown. **Daniel** (a member of no project): “No projects yet.”, then “Create a project”, then the new project with its notice and its empty task list. At 1280 the tasks are beside the details; at 390 they are under them. Neither width scrolls sideways. The database holds the final state. |
| `aListThatFailsToLoadOffersRetryAndShowsLoadingWhileItWaits` | A 500 on the task list shows the error alert with Retry and no count. Retry shows “Loading…” while the answer is held back, then shows the list and “1 task”. |
| `aFailedSubmissionKeepsWhatWasTyped` | A 500 on creating a task keeps the title, description, and priority, and the button can be used again. A second try creates exactly one task in the database. |
| `aRepeatedClickSendsOneRequestAndCreatesOneRecord` | Two clicks in the same event on Create project, Create task, and Add comment send one POST each (4 POSTs, counting the sign-in). Each makes one row in the database, and the screen shows one task and one comment. |
| `anAddressOpensItsScreenAfterSigningInAndAnUnknownOneLeadsHome` | `#/projects/{id}/tasks/{taskId}?state=TO_DO&priority=LOW` opens Sign-in, then that task. Back keeps both filters. The browser's Back returns to the task. `#/nowhere` leads to `#/` and Home. |

The 30 existing tests were updated to the new screens. Projects and tasks are now links, and the task detail is a screen of its own. Home shows the API's total (“26 projects”) with five rows. A 401 still sends the person to Sign-in with the existing message (`aSessionEndedElsewhereSendsThePersonToSignInWhenTheyUseTheProjects`, `…UseTheTasks`, and the visibility and expiry tests).

**Defect found and fixed while testing.** The filter checkboxes are driven by the address. The address changed only at the next `hashchange`, so React put back the old value and Playwright reported “Clicking the checkbox did not change its state”. `go()` in `navigation.ts` now writes the history entry and tells the screens in the same event (commit `b63b5c1`).

## 4. Mocks, fixtures, and secrets

```text
$ grep -rniE "mock|fixture|fake|stub|sample data|dummy" frontend/src frontend/index.html
frontend/src/http.test.ts:15:  vi.unstubAllGlobals();
frontend/src/http.test.ts:40:    vi.stubGlobal('fetch', fetch);
frontend/src/http.test.ts:44:    const [address, init] = fetch.mock.calls[0];
frontend/src/http.test.ts:54:    vi.stubGlobal('fetch', vi.fn(async () => json(401, {})));
```

Only the unit test of `http.ts` replaces `fetch`. No screen uses a mock or fixture: every operation calls the real API. A search for literal passwords, secrets, keys, and tokens in `frontend/src`, `src/main`, `docs`, and `README.md` found nothing. `.env` and the ignored files are not staged. The test people (Ana Silva, Rui Martins, Eva Santos, Daniel Reis) are invented demonstration names, and each is created with a random password by `TestAccess`.

## 5. The wireframe and the screens

Only the [Project dashboard wireframe](../ui/project-dashboard.md) exists. The Project detail and Task detail follow it, with these differences:

| Marker | Wireframe | Built | Why |
|---|---|---|---|
| 1 | Back to the project list | “Back to Projects” | My projects is I5. Home lists the person's first projects in the meantime. |
| 2 | Rename | “Edit project” and “Delete project”, Project detail only | I3, I10; I13 decision 2 |
| 3 | Complete project | Not built | I8 |
| 4 | Progress | Not built; Home shows no counts by state | I5 ([Assumption A-026](../constraints.md#assumptions)) |
| 5 | “Unassigned” | “No one is assigned.” | I13 decision 5 |
| 6 | Add task, title only | An inline form: title, description, and Task priority | I4 |
| 7–9 | State, Assign, and Rename on the list | On the Task detail screen | I13 decision 3 |
| 10 | Members | Built, for every member | I6 (member list) |
| 11, 12 | Remove participant, Add participant | Not built | Rest of I6 ([Assumption A-030](../constraints.md#assumptions); [OQ-011](../constraints.md#open-questions)) |

**Screens with no wireframe.** Sign-in, Home, Projects, and Task detail are built from the data and the rules, as the team decided, and no design was invented. A wireframe for each would need to settle:

- **Sign-in:** the order of the fields, and where the messages go.
- **Home:** how many projects to show, and where progress and the counts by state go (I5).
- **Projects:** the search, the creation form, and what each row shows.
- **Task detail:** the order of the details, the assignees, and the comments panels, and where the state and priority controls go.

## 6. Still open

- Totals by state (counts and progress) wait for I5.
- The token is kept in memory, so a reload signs the person out ([ADR-005](../adr/ADR-005-authentication-with-jwt.md), open point).
- Two notices can show at once on Task detail: the details panel's and the comments panel's. Each belongs to its own panel.
- Another backend origin needs CORS, which is not configured.
