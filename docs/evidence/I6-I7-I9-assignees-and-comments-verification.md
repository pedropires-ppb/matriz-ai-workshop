# Matriz — Verification record for the assignees and comments exercise (2026-11-05)

**Status:** Evidence for the project team's review of the member list of increment [I6](../implementation-plan.md#i6--members), and of [I7](../implementation-plan.md#i7--several-assignees-and-self-assignment) and [I9](../implementation-plan.md#i9--comments). Nothing here is accepted: only the project team accepts an increment ([plan rule 4](../implementation-plan.md#increment-status)). Every decision below is the assistant's proposal, confirmed by the user's blanket confirmation in the conversation “Implement assignees and comments”; the team's own review and the client's validation are pending.

**Environment:** macOS on arm64; JDK 25 (Temurin); Maven through the wrapper; Node.js installed by the build; Playwright with Chromium; Docker; PostgreSQL 18 in the tests. `gh` is not authenticated on this machine, so no continuous integration run is linked.

## 1. I6 — the member list (`listProjectMembers`, US-026)

**Scope.** Only the list is built ([plan revision](../implementation-plan.md#decisions-affecting-the-increments)). Adding, removing, and finding participants ([A-030](../constraints.md#assumptions); [OQ-011](../constraints.md#open-questions)), the removal checks, [US-002](../user-stories.md#us-002--add-participant), and [US-009](../user-stories.md#us-009--remove-participant) stay Planned.

**Decisions** ([Assumption A-034](../constraints.md#assumptions)): any member reads the list, also in a Completed project; the whole list, with no paging; `personId`, display name, and role, never a login name; the Project Owner first, then by display name ignoring letter case, then by identifier; a non-member, a deleted or unknown project, or an identifier that is not a UUID gets `404`, and there is no `403`.

**Code.** `ListProjectMembers` (Projects, application), `Project.members()` (domain), `MemberResponse` and a mapping in `ProjectController` (web). Person names come through Identity's `PersonDirectory`. `openapi.yaml` already defined the operation, so it did not change.

| Check | Result |
|---|---|
| `./mvnw -B clean verify` | Passed: 443 tests, 0 failures (baseline 434) |
| `MembersApiTests` (8) | Order and fields against the contract's `Member` schema; no login name or hash in the body; Completed project readable; non-member `404`; deleted, unknown, and malformed project `404`; no token `401`; `PUT` and `DELETE` on the collection `405` |
| `UserInterfaceTests` (25, one new) | A Project Participant sees the members, Ana first, in the agreed order, with no login name |
| `LayeringTests`, `ModuleBoundaryTests` | Passed |

```text
$ ./mvnw -B clean verify
…
[INFO] Tests run: 8, Failures: 0, Errors: 0, Skipped: 0 -- in com.matriz.MembersApiTests
[INFO] Tests run: 25, Failures: 0, Errors: 0, Skipped: 0 -- in com.matriz.UserInterfaceTests
[INFO] Tests run: 443, Failures: 0, Errors: 0, Skipped: 0
[INFO] BUILD SUCCESS
```

**Known points.** The list has no paging, so a project with very many members would return a long answer ([A-034](../constraints.md#assumptions)). The display names of the members of a project are visible to every member, which is personal data ([A-027](../constraints.md#assumptions); [OQ-011](../constraints.md#open-questions)). The demonstration data was not used by the automated tests.

## 2. I7 — several assignees and self-assignment (`assignTask`, `unassignTask`, US-012, US-019, US-020, US-005)

**Decisions** ([Assumption A-035](../constraints.md#assumptions); [ADR-004](../adr/ADR-004-rest-api-single-entry-point.md); [ADR-008](../adr/ADR-008-domain-driven-design.md)):

- the Project Owner assigns or unassigns any member; a Project Participant only themselves ([A-019](../constraints.md#assumptions), [A-020](../constraints.md#assumptions));
- only a member of the project is eligible; a person who does not exist or is not a member gets the same `422` (EX-029);
- a repeated assignment is idempotent and returns `200` (EX-028); removing a person who is not an assignee gets `404` (EX-030); the last assignee can be removed; assignments never change the state;
- the Project Owner or an assignee changes the state; a Project Participant who is not an assignee gets `403`. This closes the gap that I4 documented (FR-011, EX-004);
- order of checks in `assignTask`: `401`, `405`, `404`, `415`, `400`, `422` (shape), `403`, `422` (member), `409`; a Completed project gives `409`; the project row is locked first ([ADR-006](../adr/ADR-006-simultaneous-changes.md)).

**Code.** Tasks module: `AssignTask`, `UnassignTask` (application), `Task.assign` and `Task.unassign` (domain, inside the aggregate), `TaskFacts` and the `ASSIGN` and `UNASSIGN` actions of the access policy; the rules are in `TaskAccessRules` (Access control). Whether the person is a member comes from the Projects module's API. The primary key `(task_id, person_id)` and the foreign key to the membership are the database's backstop. No migration was needed. `openapi.yaml` changed in text only (the rules and `403` of `changeTaskState`, and the order of `assignTask`).

| Check | Result |
|---|---|
| `./mvnw -B clean verify` | Passed: 472 tests, 0 failures (baseline 443), including `LayeringTests`, `ModuleBoundaryTests`, and the contract check |
| `AssignmentApiTests` (21) | Several assignees and persistence; a repeated and a simultaneous assignment give one row; a person who is not a member or does not exist `422`; the order of checks; a participant's self-assignment and state change; `401`, `404`, `405`; a deleted task; unassigning; a Completed project `409` (fixture); the primary key and the foreign key |
| `TaskApiTests` (93), `TaskAccessRulesTests` (6), `TaskDomainTests` (19) | The `changeTaskState` rules for assignees; the access rows; `Task.assign` and `Task.unassign` |
| `UserInterfaceTests` (28, three new) | See below |

**End to end** (Chromium, **no page reload**: a marker on the page's `window` survives to the end):

- Ana picks Rui and Eva in turn from the list of members who are not yet assigned; both appear; she removes Eva with “Remove Eva Costa”; the picker offers the removed member again; one row is left in `assignment`;
- Rui, a Project Participant, sees no state control, uses “Assign me”, can then change the state (the list shows “In progress”) but not the priority, uses “Unassign me”, and loses the state control;
- in a Completed project, Ana sees no assignment control and no state control.

**Direct requests and restart.** The packaged application, with the demonstration data, on a throwaway PostgreSQL 18 database, and curl; two runs on the same database and the same signing key. Passwords and key were generated for the run and not recorded; the throwaway database was removed.

```text
== run 1
Ana POST assignees Rui (owner assigns a member)                        -> 200
Ana POST assignees Eva (a second assignee)                             -> 200
   assignees: ['Eva Santos', 'Rui Martins']
Ana POST assignees Rui again (repeated)                                -> 200
   assignees: ['Eva Santos', 'Rui Martins']
Ana POST assignees Daniel (member of no project)                       -> 422
Ana POST assignees random UUID (does not exist)                        -> 422
Ana POST assignees 'not-a-uuid'                                        -> 422
Ana POST assignees {} (missing personId)                               -> 422
Rui POST assignees Eva (participant assigns another)                   -> 403
Rui PUT state IN_PROGRESS (assignee; closes the I4 gap)                -> 200
Rui DELETE assignee Eva (participant removes another)                  -> 403
Rui DELETE assignee Rui (own assignment)                               -> 200
Rui PUT state DONE (no longer an assignee)                             -> 403
Rui POST assignees Rui (assigns himself)                               -> 200
Ana DELETE assignee Daniel (not an assignee)                           -> 404
Eva GET /projects/M/tasks (not a member of Mobile App)                 -> 404
Eva POST assignees with Mobile App's path                              -> 404
Daniel POST assignees (member of no project)                           -> 404
no token POST assignees                                                -> 401
no token DELETE assignee                                               -> 401
Ana PUT /assignees (method not allowed)                                -> 405
== run 2 (restart, same database and key)
Ana GET task after restart                                             -> 200
   assignees after restart: ['Eva Santos', 'Rui Martins']; state: IN_PROGRESS
Rui GET task after restart                                             -> 200
Ana DELETE task                                                        -> 204
== rows without a parent
assignments without task: 0
assignments without membership: 0
assignments of the deleted task: 0
secrets in log: 0
```

The Completed-project `409` is checked by the automated tests with a fixture, not in this run. The assignments of a deleted task are removed with it ([A-022](../constraints.md#assumptions)).

**Known points.** [OQ-025](../constraints.md#open-questions) (self-assignment) stays high risk: any member can make themselves an assignee and then change the state. The removal check of `removeParticipant` and [EX-007](../user-stories.md#edge-cases-and-exceptions) stay with the rest of I6.

## 3. I9 — task comments (`addTaskComment`, `listTaskComments`, US-021, US-022)

**Decisions** ([Assumption A-036](../constraints.md#assumptions), [A-023](../constraints.md#assumptions) as amended; EX-031, EX-032; [ADR-004](../adr/ADR-004-rest-api-single-entry-point.md); [ADR-006](../adr/ADR-006-simultaneous-changes.md); [ADR-008](../adr/ADR-008-domain-driven-design.md)):

- the text has 1 to 2000 Unicode characters after trimming, with the whitespace rule of [A-033](../constraints.md#assumptions) (no-break spaces included); the trimmed text is stored ([OQ-024](../constraints.md#open-questions) interim answer);
- an empty, whitespace-only, missing, non-string, or too long text, or any other property (an author or a date), gets `422` with the error at `/text` (or at the extra property);
- the author comes from the token and the date from a server `Clock` (UTC, milliseconds, RFC 3339 `Z`); the client sends only `text`;
- the list is whole, with no paging, oldest first (date, then the technical `creation_order`);
- no edit and no delete: `DELETE`, `PUT`, and `PATCH` on the collection give `405`;
- every member comments and reads, also in a Completed project (the only change such a project allows); a non-member, a deleted task, or a deleted project gets `404`, never `403`; the project row is locked on adding;
- a comment keeps its author after the person leaves the project.

**Code.** Tasks module: `Comment` (entity, with `CommentId` in the API package and `CommentText` as an embeddable value), `CommentRepository`, `AddTaskComment`, `ListTaskComments`, `CommentViews`, the `COMMENT` action of the access policy, `CommentResponse`, two operations in `TaskController`, and a `Clock` bean. Migration `V6__add_comment_text_limit_and_order.sql` (new; no applied migration was edited) adds the 2000-character check, `creation_order`, and an index on `(task_id, created_at, creation_order)`; [database.md](../database.md) is at v0.8. `openapi.yaml`: `CommentText` has `minLength` 1 and `maxLength` 2000, and `addTaskComment` states the `422` rules.

| Check | Result |
|---|---|
| `./mvnw -B clean verify` | Passed: 523 tests, 0 failures (baseline 472), including `LayeringTests` (5), `ModuleBoundaryTests` (4), and the contract check |
| `CommentApiTests` (36) | Boundary values 1, 2000, and 2001; empty, spaces, and no-break spaces; a client-sent author or date refused; author and date from the server; order; a Completed project allows comments (fixture); `401`, `404` (non-member, deleted task, deleted project, unknown and malformed ids), `405`; the stored text is trimmed |
| `CommentTextTests` (11), `TaskAccessRulesTests`, `SchemaMigrationTests`, `DemoDataTests` | The value rules; the `COMMENT` row of the policy; the V6 migration and the length check; the demonstration data at version 6 |
| `UserInterfaceTests` (30, two new) | See below |

**End to end** (Chromium, **no page reload**: a marker on the page's `window` survives): a member adds comments and sees the author and the date at once; a whitespace-only text and a 2001-character text show an error and send no request; the request body is exactly `{"text": …}`; reopening the task shows the comments again; a second person sees them with the right author; in a Completed project the form is still there and a comment can be added.

**Direct requests and restart.** The packaged application, with the demonstration data, on a throwaway PostgreSQL 18 database, and curl; two runs on the same database and the same signing key. Passwords and key were generated for the run and not recorded; the database and the temporary files were removed. Long bodies are cut.

```text
== run 1
Rui adds (padded text)                -> 201, author Rui Martins, createdAt 2026-11-05T20:33:54.246Z
Ana adds 2000 characters              -> 201
Eva adds 1 character                  -> 201
2001 characters                       -> 422 (/text)
empty                                 -> 422 (/text)
spaces only                           -> 422 (/text)
no-break spaces only                  -> 422 (/text)
client-sent author                    -> 422 (/authorId)
client-sent date                      -> 422 (/createdAt)
Daniel (member of no project) add     -> 404
Daniel list                           -> 404
no token: add, list                   -> 401, 401
Completed project (Project Gamma): Ana adds -> 201
DELETE, PUT on the collection         -> 405, 405
list Website Redesign task: Rui Martins (27, trimmed), Ana Silva (2000), Eva Santos (1), oldest first
list Project Gamma task: Ana Silva "Closing remark"
orphan comments: 0; total comments: 6
== run 2 (restart, same database and key)
the same three comments with the same authors and dates; the Project Gamma comment; orphan comments: 0; total comments: 6
```

**Points to flag.**

- **[ADR-008](../adr/ADR-008-domain-driven-design.md) is inconsistent about the Comment.** Its aggregates table makes Comment an aggregate of its own in Tasks; a later line says “Comment inside the Task aggregate”; the plan says “the Comment aggregate in Tasks”. I built it as an aggregate of its own, referring to `TaskId` and `PersonId`, with a `CommentRepository`, because the list must not load the task and comments are never changed. The team should confirm which reading holds. *Resolved on 2026-11-09 by team decision (the conversation “Implement the Matriz front end”, review question 4): a Comment is an aggregate of its own. On re-reading, the later line is in ADR-008's “Alternatives considered”, where it names the option not chosen, so the ADR does not contradict itself; only the plan's wording “placed in Tasks” and the original reading of the line were unclear.*
- The list has no paging, so a task with very many comments returns a long answer ([A-036](../constraints.md#assumptions)).
- Comments in a Completed project are checked with a fixture and in the direct run on the demonstration project; I8 repeats the check once projects can be completed.
- The display time in the screen is the browser's local time; the contract value is UTC.
- Process: some edits in this exercise were made with scripts and a heredoc instead of the edit tool (the plan, ADR, `openapi.yaml`, and `database.md` edits, and one test clean-up). The diff was reviewed afterwards; the content is as intended.
