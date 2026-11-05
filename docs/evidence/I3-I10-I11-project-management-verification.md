# Matriz — Increment I3 verification record, with the project parts of I10 and I11 (2026-11-02)

**Status:** Evidence for the project team's review of increment [I3](../implementation-plan.md#i3--projects-and-access-control), and of the project parts of [I10](../implementation-plan.md#i10--deletion) (deleting a project) and [I11](../implementation-plan.md#i11--search-and-filters) (searching projects by name). They are Awaiting review. *Created on 2026-11-02 in the conversation “Implement project management”. The decisions it implements are in the [implementation plan](../implementation-plan.md#decisions-affecting-the-increments), [A-031 and A-032](../constraints.md#assumptions), and [ADR-008](../adr/ADR-008-domain-driven-design.md).*

**Environment:** macOS on arm64; JDK 25 (Temurin); Maven through the wrapper; Node.js installed by the build; Playwright with Chromium; Docker; PostgreSQL 18, in the tests and in the demonstration run.

## Summary

| # | Check | Result |
|---|---|---|
| 1 | [Build and automated tests](#1-build-and-automated-tests) | Passed: 302 tests in 21 classes, 0 failures |
| 2 | [Operations and exit criteria](#2-operations-and-exit-criteria) | Met, with the points of section 2 |
| 3 | [Restart](#3-restart) | Passed: a project created before a restart is listed and viewable after it |
| 4 | [End to end](#4-end-to-end) | Passed: 18 tests in the User interface, 8 of them new |
| 5 | [Secrets](#5-secrets) | Passed locally; GitHub's secret scanning and continuous integration not run for this branch |
| 6 | [Not done, and points for the team](#6-not-done-and-points-for-the-team) | Open |

## 1. Build and automated tests

```text
$ ./mvnw -B clean verify
…
[INFO] Tests run: 18, Failures: 0, Errors: 0, Skipped: 0 -- in com.matriz.UserInterfaceTests
[INFO] Tests run: 4, Failures: 0, Errors: 0, Skipped: 0 -- in com.matriz.accesscontrol.application.ProjectAccessRulesTests
[INFO] Tests run: 13, Failures: 0, Errors: 0, Skipped: 0 -- in com.matriz.projects.ProjectDomainTests
[INFO] Tests run: 5, Failures: 0, Errors: 0, Skipped: 0 -- in com.matriz.ProjectRepositoryTests
[INFO] Tests run: 14, Failures: 0, Errors: 0, Skipped: 0 -- in com.matriz.SchemaMigrationTests
[INFO] Tests run: 7, Failures: 0, Errors: 0, Skipped: 0 -- in com.matriz.DemoDataTests
[INFO] Tests run: 13, Failures: 0, Errors: 0, Skipped: 0 -- in com.matriz.RequestSchemaTests
[INFO] Tests run: 16, Failures: 0, Errors: 0, Skipped: 0 -- in com.matriz.ProblemDetailsTests
[INFO] Tests run: 45, Failures: 0, Errors: 0, Skipped: 0 -- in com.matriz.EveryOperationRequiresATokenTests
[INFO] Tests run: 4, Failures: 0, Errors: 0, Skipped: 0 -- in com.matriz.ModuleBoundaryTests
[INFO] Tests run: 5, Failures: 0, Errors: 0, Skipped: 0 -- in com.matriz.LayeringTests
[INFO] Tests run: 65, Failures: 0, Errors: 0, Skipped: 0 -- in com.matriz.ProjectApiTests
…
[INFO] Tests run: 302, Failures: 0, Errors: 0, Skipped: 0
[INFO] BUILD SUCCESS
```

The other classes (sign-in, tokens, health, log secrecy, migration naming, and so on) also passed. The build ran at the commit that precedes this record; this record and the plan change documents only.

## 2. Operations and exit criteria

| Criterion | Evidence |
|---|---|
| 1. Acceptance criteria as API tests; the main flow end to end | `ProjectApiTests` (create, list, view, update, delete; the limits at their boundaries; search; paging) and `UserInterfaceTests` |
| 2. Each operation for the people of the permission matrix | `ProjectApiTests`: no token 401; non-member, deleted, and malformed identifier 404; Project Participant 403 on update and delete; Completed project 409; nothing changes in each case |
| 3. Responses match the contract; errors are Problem Details | Every response is checked against `openapi.yaml` (`Contract.assertSatisfies`, `Contract.assertProblem`); `RequestSchemaTests` and `ProblemDetailsTests` pass |
| 4. Module boundary and layering tests | `ModuleBoundaryTests` and `LayeringTests` pass |
| 5. No secret committed or logged | Section 5 |
| 6. Documentation updated | The decisions are recorded in the plan, A-003, A-024, A-031, A-032, the requirements, the user stories, and ADR-008; the [README](../../README.md#manage-projects) lists the operations |

Soft deletion leaves no orphan: after a deletion, the rows of the memberships, tasks, assignments, and comments stay, and every read hides them (tested in `ProjectApiTests`).

## 3. Restart

The same database and the same signing key, two runs of the packaged application, with curl; no secret was printed.

```text
run 1: created (201, Location: /api/projects/186ad1a8-…), then stopped
run 2: signed in again
GET /api/projects?name=restart%20check&size=5  -> {"items":[{"projectId":"186ad1a8-…","name":"Restart check …",…}],"page":1,"size":5,"totalItems":1,"totalPages":1}
GET /api/projects/186ad1a8-…                   -> 200
DELETE /api/projects/186ad1a8-…                -> 204
GET /api/projects/186ad1a8-…                   -> 404
```

## 4. End to end

`UserInterfaceTests`, in Chromium: sign in, create a project, see it in the list, reload (the person signs in again, A-028), and see it again; validation messages with no request sent; renaming and clearing the description; cancelling a deletion changes nothing; confirming it removes the project; a Project Participant sees no edit or delete button; paging and search; a session ended elsewhere sends the person to sign in.

## 5. Secrets

`.env` is ignored by git and is not tracked or staged. The demonstration passwords do not appear in the committed tree. The local database password is a short common word that also appears in documents as an ordinary word; the team should replace it (outside the repository).

## 6. Not done, and points for the team

- Client validation of A-031 and A-032 (the description, the limits, the paging) is pending.
- A reload signs the person out, because the token is kept in memory (A-028, [ADR-005](../adr/ADR-005-authentication-with-jwt.md)).
- A name made only of no-break spaces (U+00A0) is accepted, because trimming follows Java's definition of whitespace. The team should decide whether to treat it as empty.
- Continuous integration for this branch has not run; nothing was pushed.
## 7. Review of 2026-11-05

*Added on 2026-11-05 by the assistant, in the conversation “Lab Worksheet 03 — Implement task management”. The project team accepted I3 and the project parts of I10 and I11 on that day ([implementation plan](../implementation-plan.md#increments)); the status line at the top of this record describes the state before the review.*

Follow-ups requested by the I2 review and by the I3 review:

- **Ended token.** `ProjectApiTests.aTokenEndedBySigningOutIsRefused` checks that a token ended by signing out is refused with 401 on a project operation, and `EveryOperationRequiresATokenTests.withASignedOutTokenAnOperationIsRefused` checks every operation. Both passed in the build of section 1.
- **Local database password.** The search was repeated on 2026-11-05: the value in the ignored `.env` file occurs in no tracked file and in no commit of any branch (`git grep` and `git log --all -S`; the value was not printed). `.env.example` holds only a placeholder. The point in section 5 about replacing a short common word stays with the team.
- **No-break space (U+00A0).** Still open; the question is asked again for projects and tasks in the next increment.
- **Client validation** of A-031 and A-032 is pending.
- **Continuous integration** passed, as reported by the team on 2026-11-05; there is no run link.
