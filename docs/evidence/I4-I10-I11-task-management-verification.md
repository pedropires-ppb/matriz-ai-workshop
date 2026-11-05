# Matriz — Increment I4 verification record, with the task parts of I10 and I11 (2026-11-05)

**Status:** Evidence for the project team's review of increment [I4](../implementation-plan.md#i4--tasks-and-task-priority), and of the task parts of [I10](../implementation-plan.md#i10--deletion) (deleting a task) and [I11](../implementation-plan.md#i11--search-and-filters) (filtering tasks by state and priority). They are Accepted since 2026-11-05 ([plan](../implementation-plan.md#increments)); the gap of [section 6](#6-not-done-and-points-for-the-team) was accepted by the team and I7 closes it. *Changed on 2026-11-05 by team decision (the conversation “Lab Worksheet 03 — Implement task management”): previously “They are Awaiting review.”* *Created on 2026-11-05 in the conversation “Lab Worksheet 03 — Implement task management”. The decisions it implements are in the [implementation plan](../implementation-plan.md#decisions-affecting-the-increments), [A-033](../constraints.md#assumptions) with the amended A-004, A-024, A-031, and A-032, [OQ-030](../constraints.md#open-questions), and [ADR-008](../adr/ADR-008-domain-driven-design.md).*

**Environment:** macOS on arm64; JDK 25 (Temurin); Maven through the wrapper; Node.js installed by the build; Playwright with Chromium; Docker; PostgreSQL 18, in the tests and in the demonstration run. The build ran at commit `9b567ef`, which precedes this record; this record and the plan change documents only.

## Summary

| # | Check | Result |
|---|---|---|
| 1 | [Build and automated tests](#1-build-and-automated-tests) | Passed: 434 tests, 0 failures (I3 baseline: 302) |
| 2 | [Operations and exit criteria](#2-operations-and-exit-criteria) | Met, with the points of section 6 |
| 3 | [Direct requests and restart](#3-direct-requests-and-restart) | Passed: a task created before a restart is listed and viewable after it |
| 4 | [End to end](#4-end-to-end) | Passed: 24 tests in the User interface, 6 of them new |
| 5 | [Secrets](#5-secrets) | Passed locally; GitHub's secret scanning and continuous integration not run for this branch |
| 6 | [Not done, and points for the team](#6-not-done-and-points-for-the-team) | Open |

## 1. Build and automated tests

```text
$ ./mvnw -B clean verify
…
[INFO] Tests run: 93, Failures: 0, Errors: 0, Skipped: 0 -- in com.matriz.TaskApiTests
[INFO] Tests run: 24, Failures: 0, Errors: 0, Skipped: 0 -- in com.matriz.UserInterfaceTests
[INFO] Tests run: 16, Failures: 0, Errors: 0, Skipped: 0 -- in com.matriz.tasks.TaskDomainTests
[INFO] Tests run: 8, Failures: 0, Errors: 0, Skipped: 0 -- in com.matriz.TaskRepositoryTests
[INFO] Tests run: 4, Failures: 0, Errors: 0, Skipped: 0 -- in com.matriz.accesscontrol.application.TaskAccessRulesTests
[INFO] Tests run: 67, Failures: 0, Errors: 0, Skipped: 0 -- in com.matriz.ProjectApiTests
[INFO] Tests run: 16, Failures: 0, Errors: 0, Skipped: 0 -- in com.matriz.projects.ProjectDomainTests
[INFO] Tests run: 13, Failures: 0, Errors: 0, Skipped: 0 -- in com.matriz.RequestSchemaTests
[INFO] Tests run: 16, Failures: 0, Errors: 0, Skipped: 0 -- in com.matriz.ProblemDetailsTests
[INFO] Tests run: 45, Failures: 0, Errors: 0, Skipped: 0 -- in com.matriz.EveryOperationRequiresATokenTests
[INFO] Tests run: 14, Failures: 0, Errors: 0, Skipped: 0 -- in com.matriz.SchemaMigrationTests
[INFO] Tests run: 7, Failures: 0, Errors: 0, Skipped: 0 -- in com.matriz.DemoDataTests
[INFO] Tests run: 4, Failures: 0, Errors: 0, Skipped: 0 -- in com.matriz.ModuleBoundaryTests
[INFO] Tests run: 5, Failures: 0, Errors: 0, Skipped: 0 -- in com.matriz.LayeringTests
…
[INFO] Tests run: 434, Failures: 0, Errors: 0, Skipped: 0
[INFO] BUILD SUCCESS
```

The other classes (sign-in, tokens, health, log secrecy, migration naming, and so on) also passed.

## 2. Operations and exit criteria

| Criterion | Evidence |
|---|---|
| 1. Acceptance criteria as API tests; the main flow end to end | `TaskApiTests` (create, list, view, update, change state, delete; the limits at their boundaries; filters alone and combined, and an empty result; paging) and `UserInterfaceTests` |
| 2. Each operation for the people of the permission matrix | `TaskApiTests`: no token 401; non-member, deleted, foreign-project task, and malformed identifier 404; Project Participant 403 on every change; Completed project 409 for every change, from a fixture; nothing changes in each case. `TaskAccessRulesTests` covers the whole matrix without a database |
| 3. Responses match the contract; errors are Problem Details | Every response is checked against `openapi.yaml`; `RequestSchemaTests` and `ProblemDetailsTests` pass |
| 4. Module boundary and layering tests | `ModuleBoundaryTests` and `LayeringTests` pass |
| 5. No secret committed or logged | Section 5 |
| 6. Documentation updated | The decisions are recorded in the plan, A-004, A-024, A-031, A-032, A-033, OQ-030, the requirements, the user stories, `docs/database.md` (v0.7), and ADR-004, ADR-007, and ADR-008; the [README](../../README.md#manage-tasks) lists the operations |

Limits at the boundary, tested in `TaskApiTests` and `TaskDomainTests`: a title of 1 and of 150 characters is accepted, one of 151 is refused (422); the same with Unicode characters outside the Basic Multilingual Plane, counted as one each; a description of 2000 is accepted and one of 2001 is refused; text that is only spaces, tabs, or no-break spaces (U+00A0, U+2007, U+202F) is refused as a title and means none as a description; an invalid state or priority gets 422, and an invalid filter value, page, or size gets 400.

Soft deletion leaves no orphan: after a deletion, the task row stays with `deleted_at` set, its Assignments are removed, and every read (list, view, filters, a second deletion) hides it; no task is without a project and no assignment without a task (`TaskApiTests`, `TaskRepositoryTests`, and section 3).

Two simultaneous changes of the same task: one test shows that they are applied one after the other and the last prevails (ADR-006); the simultaneous-change tests of I8 repeat it with Completion.

## 3. Direct requests and restart

The packaged application, with the demonstration data (Ana, owner of Website Redesign; Rui, a participant; Eva, a participant of Website Redesign and not a member of Mobile App; Daniel, a member of no project), on a throwaway PostgreSQL 18 database, and curl. Two runs on the same database and the same signing key. No secret was printed.

```text
== run 1
Ana POST /projects/W/tasks                                               -> 201
== direct requests (demonstration people)
Ana GET /projects/W/tasks?size=2 (page 1)                                -> 200   "totalItems":5,"totalPages":3
Ana GET /projects/W/tasks?state=TO_DO&priority=HIGH                      -> 200   "totalItems":2
Rui GET /projects/W/tasks/ID (participant views)                         -> 200
Rui POST /projects/W/tasks (participant, until I7)                       -> 403
Rui DELETE /projects/W/tasks/ID                                          -> 403
Eva GET /projects/M/tasks (not a member of Mobile App)                   -> 404
Eva POST /projects/M/tasks                                               -> 404
Daniel GET /projects/W/tasks (member of no project)                      -> 404
Daniel DELETE /projects/W/tasks/ID                                       -> 404
no token GET /projects/W/tasks                                           -> 401
Ana POST title 'a'*150                                                   -> 201
Ana POST title 'a'*151                                                   -> 422
Ana POST title '   '                                                     -> 422
Ana POST title NBSP only                                                 -> 422
Ana POST priority URGENT                                                 -> 422
Ana PUT state BLOCKED                                                    -> 422
Ana PUT state IN_PROGRESS                                                -> 200
Ana PATCH priority LOW                                                   -> 200
Ana GET ?state=NOPE                                                      -> 400
Ana GET ?page=0                                                          -> 400
Ana POST in Completed Project Gamma                                      -> 409
== run 2 (restart, same database and key)
Ana signs in again, GET /projects/W/tasks/ID                             -> 200   state IN_PROGRESS, priority LOW, description kept
Ana GET ?state=IN_PROGRESS&priority=LOW                                  -> 200   "totalItems":1
Ana DELETE /projects/W/tasks/ID                                          -> 204
Ana GET /projects/W/tasks/ID (after deletion)                            -> 404
Ana GET ?state=IN_PROGRESS&priority=LOW (after deletion)                 -> 200   "totalItems":0
== rows without a parent
tasks without project: 0
assignments without task: 0
deleted task still in a read: 0
secrets in log: 0
```

The password and key values were generated for this run, used only in it, and are not recorded. Rui's 403 on the changes is the documented gap of section 6.

## 4. End to end

`UserInterfaceTests`, in Chromium, six new tests:

- Ana opens Website Redesign, sees the empty list and “0 tasks”, creates a task (title padded with spaces, a description, priority High) and a second one, and sees both in the list with the count “2 tasks”, **with no page reload** (a marker on the page's `window` survives to the end);
- she changes the state to In progress and the priority to Low from the details panel, and the list shows both;
- she filters by state and by priority, alone and combined (an empty result shows “No task matches the filters.” and “0 tasks”), and clears the filters;
- she starts a deletion and cancels it (no `DELETE` request is sent, both tasks stay), then deletes it after the confirmation (204; the list and the count become “1 task”);
- a task is edited and its description cleared; invalid titles and descriptions (empty, spaces only, no-break spaces only, 151 characters, a 2001-character description) are explained next to the field with no request sent, and the boundary values go through;
- a Project Participant sees the tasks and the explanation, and no New task, Edit, Delete, or state control;
- 21 tasks give two pages; deleting the only task of page 2 returns to page 1, and the count becomes “20 tasks”;
- a session ended elsewhere sends the person to sign in when they use the filters.

## 5. Secrets

`.env` is ignored by git and is not tracked or staged. The commits of this branch add no credential; the demonstration run used passwords and a key generated at run time and kept only in that process's environment.

## 6. Not done, and points for the team

- **Rui's gap against FR-011 and EX-004.** Until assignees arrive in I7, only the Project Owner changes tasks, so Rui, who is assigned to a demonstration task, gets 403 when he changes its state. I7 closes it ([plan](../implementation-plan.md#decisions-affecting-the-increments)).
- **Change of behaviour since I3: no-break spaces.** The I3 record said a project name made only of U+00A0 was accepted. It is now refused (and a description of them means none), for projects and tasks alike, by migration V5 and a shared rule (the team's decision in this conversation). A project search text of them is blank (400).
- **`changeTaskState` is a `PUT`** with the body `{"state": …}`, as the contract already defined it; I4 did not change the verb.
- **Counts per state and progress** are not in the User interface: it shows the total of the list and the page. `taskCounts` stays omitted until I5.
- **Completed projects** refuse every change from a fixture here; completing a project is I8.
- **Comments** and **Assignments** do not exist yet (I9, I7); deletion keeps comments and removes assignments (A-022), and the tests for them arrive with I9 and I7.
- Client validation of A-033 (description, limits, values, filters) and of the amended A-004, A-024, A-031, and A-032 is pending; the text limits of comments (OQ-024) stay open.
- The confirmation of the deletion of tasks (OQ-023, A-022) was treated as given by the team's answers in this conversation; the client has not validated it.
- Continuous integration for this branch has not run; nothing was pushed.
