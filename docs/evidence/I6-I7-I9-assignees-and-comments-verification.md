# Matriz — Verification record for the assignees and comments exercise (2026-11-05)

**Status:** Evidence for the project team's review of the member list of increment [I6](../implementation-plan.md#i6--members), and later of [I7](../implementation-plan.md#i7--several-assignees-and-self-assignment) and [I9](../implementation-plan.md#i9--comments). Nothing here is accepted: only the project team accepts an increment ([plan rule 4](../implementation-plan.md#increment-status)). Every decision below is the assistant's proposal, confirmed by the user's blanket confirmation in the conversation “Implement assignees and comments”; the team's own review and the client's validation are pending.

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
