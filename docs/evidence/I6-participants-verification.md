# Matriz — Verification record for I6, adding, removing, and finding participants (2026-11-09)

**Status:** This is evidence for the project team's review of the rest of increment [I6](../implementation-plan.md#i6--members): adding, removing, and finding Project Participants. The member list was accepted earlier. Nothing in this record is accepted yet, because only the project team accepts an increment ([plan rule 4](../implementation-plan.md#increment-status)).

The decisions used are the I6 decisions, recorded in [Decisions affecting the increments](../implementation-plan.md#decisions-affecting-the-increments). This work started while [I5](../implementation-plan.md#i5--progress-and-my-projects) and [I8](../implementation-plan.md#i8--completion-and-simultaneous-changes) were Awaiting review, as an exception to rule 2 (the front-end exercise, question 1). If the review of either requests changes, this work is verified again.

**Environment:**

- macOS on arm64, JDK 25 (Temurin), and Maven through the wrapper.
- Node.js 24, installed by the build, and Vitest.
- Playwright with Chromium.
- Docker, with PostgreSQL 18 in the tests.

`gh` is not authenticated on this machine, so no continuous integration run is linked. The work is on the branch `week3-ex7`, which is not pushed.

## 1. What was built

| Area | Result |
|---|---|
| Domain | `Project.addParticipant(actor, person)` and `Project.removeParticipant(actor, person, hasAssignedTasks)` guard the rules. Only the Project Owner of an Active project that is not deleted adds or removes. A member is not added twice. The Project Owner, a non-member, and a member with assigned tasks are not removed ([FR-004, FR-005](../requirements.md#functional-requirements); [Assumptions A-001, A-009, A-011](../constraints.md#assumptions)). |
| Access rules | `ProjectAction.MANAGE_MEMBERS` follows Edit, Delete, and Complete: Owner only, Active only, otherwise 409. `LIST_ELIGIBLE_PARTICIPANTS` is Owner only, in any state. |
| Modules | `ProjectTasks.assignedMembers` is implemented by Tasks with one query of the assignees of the tasks that are not deleted, whatever their state. `PersonDirectory.findOthers` is implemented by Identity. Projects still depends on Identity only (I6 decision 7). |
| Application | `AddParticipant` and `RemoveParticipant` lock the project's row, so a removal and a self-assignment of the same person run one after the other ([ADR-006](../adr/ADR-006-simultaneous-changes.md); [Assumption A-029](../constraints.md#assumptions)). The person to add is looked up through `PersonDirectory.displayNames`, not a new `exists` method, which is a small deviation from decision 7 that needs no new port. |
| API | `POST /api/projects/{projectId}/members` returns 201, a `Location`, and the `Member`. `DELETE /api/projects/{projectId}/members/{personId}` returns 204. `GET /api/projects/{projectId}/eligible-participants?name=` returns every person who is not a member: the search is trimmed, ignores letter case, treats accents as significant, does not use wildcards, and has no paging ([A-030](../constraints.md#assumptions), I6 decision 1). No migration was needed. |
| Contract | `Member` gains the required `hasAssignedTasks` (I6 decision 2). The examples and the description of `listEligibleParticipants` say that A-030 is decided by the team. Every change is flagged in `openapi.yaml`. |
| Members panel | Only the Project Owner of an Active project sees "Add participant". It opens a Name search with Search and Cancel. The results each have Add, or "No people to add." appears. Each Project Participant row has Remove, which is disabled with "Unavailable while assigned to tasks." when it applies, and asks "Remove <name> from <project>? They will no longer see the project; their comments are kept." (I6 decisions 3 and 5). |
| Synchronisation | After each change the member list is fetched again. An added person leaves the shown results, and the form stays open (I6 decision 6). After a 409, the project is fetched too. The message then says that the project is Completed, that the person is already a member, or that the person is assigned to tasks (I6 decision 4). |
| README | A new "Manage members" section, and the Project detail row mentions adding and removing. |

Commits:

- `2be7120`: the decisions;
- `c73a139`: the backend, the contract, the API and unit tests, and the README;
- `50dc7fb`: the User interface and the end-to-end tests;
- the commit that adds this record.

## 2. Checks

| Check | Command | Result |
|---|---|---|
| Whole build | `./mvnw -B clean verify` (JDK 25) | BUILD SUCCESS: 626 Java tests, 0 failures, 0 errors |
| Front-end type check | `tsc --noEmit -p tsconfig.json` (in the build) | Passed |
| Front-end unit tests | `npm test` (Vitest, in the build) | 5 files, 23 tests passed |
| Domain and rules | `ProjectDomainTests` (4 new), `ProjectAccessRulesTests` (1 new, 2 extended to `MANAGE_MEMBERS`) | Passed |
| API | `ParticipantsApiTests` (27 tests, all new), `MembersApiTests` (8, unchanged; their answers, now with `hasAssignedTasks`, are checked against the contract) | See the list below. |
| Layering and module boundaries | `LayeringTests` (5), `ModuleBoundaryTests` (4) | Passed |
| Contract | `RequestSchemaTests` (13), `EveryOperationRequiresATokenTests` (45), and the API tests, which check the answers against `openapi.yaml` | Passed |
| End to end | `UserInterfaceTests` (Playwright, Chromium) | 43 tests passed: 4 new, and the walkthrough extended |

`ParticipantsApiTests` covers these cases:

- **Adding ([US-002](../user-stories.md#us-002--add-participant)):** 201 with the `Member` and the `Location`; the added person can then view the project. A member, or the Project Owner, gets 409. An unknown person, or an identifier that is not a UUID, gets 422 at `/personId`. Other content gets 422, malformed JSON 400, and another media type 415.
- **Removing ([US-009](../user-stories.md#us-009--remove-participant)):** 204, and the person gets 404 on the next read. Their comments are kept, with their name. The Project Owner gets 409.
- **Assigned members ([EX-007](../user-stories.md#edge-cases-and-exceptions)):** an assignee of a To do, In progress, or Done task gets 409, and `hasAssignedTasks` is true. Unassigning the person, or deleting the task, makes them removable.
- **Access:** a Participant gets 403 for adding, removing, and listing people. A non-member gets 404. A target who is not a member gets 404, before the Owner check. A request without a token gets 401.
- **Completed projects ([A-011](../constraints.md#assumptions)):** adding and removing get 409. The Owner can still list who could be added.
- **Finding ([A-030](../constraints.md#assumptions)):** only people who are not members are listed, by display name ignoring letter case, without the login name. The search is trimmed and ignores letter case, but not accents; `%` and `_` are literal. An empty or blank `name` gets 400.
- **Simultaneous changes:** removing Rui while Rui assigns himself, 5 rounds. Either the removal succeeds and the assignment gets 404, or the assignment succeeds and the removal gets 409. No assignment is left without its membership.

```text
$ ./mvnw -B clean verify
…
[INFO]  Test Files  5 passed (5)
[INFO]       Tests  23 passed (23)
…
[INFO] Tests run: 43, Failures: 0, Errors: 0, Skipped: 0 -- in com.matriz.UserInterfaceTests
[INFO] Tests run: 5, Failures: 0, Errors: 0, Skipped: 0 -- in com.matriz.accesscontrol.application.ProjectAccessRulesTests
[INFO] Tests run: 22, Failures: 0, Errors: 0, Skipped: 0 -- in com.matriz.projects.ProjectDomainTests
[INFO] Tests run: 13, Failures: 0, Errors: 0, Skipped: 0 -- in com.matriz.RequestSchemaTests
[INFO] Tests run: 45, Failures: 0, Errors: 0, Skipped: 0 -- in com.matriz.EveryOperationRequiresATokenTests
[INFO] Tests run: 8, Failures: 0, Errors: 0, Skipped: 0 -- in com.matriz.MembersApiTests
[INFO] Tests run: 4, Failures: 0, Errors: 0, Skipped: 0 -- in com.matriz.ModuleBoundaryTests
[INFO] Tests run: 27, Failures: 0, Errors: 0, Skipped: 0 -- in com.matriz.ParticipantsApiTests
[INFO] Tests run: 5, Failures: 0, Errors: 0, Skipped: 0 -- in com.matriz.LayeringTests
…
[INFO] Tests run: 626, Failures: 0, Errors: 0, Skipped: 0
[INFO] BUILD SUCCESS
```

## 3. End-to-end tests for I6

| Test | What it shows |
|---|---|
| `theProjectOwnerAddsAPersonWhoSeesTheProjectUntilTheyAreRemoved`, at 390 and at 1280 pixels (new) | **Ana** searches "  rocha … " and adds **Daniel**, who had no project. The notice, the three members, "No people to add.", and the kept search text all appear, and the database has the membership. Daniel signs in and sees Mobile App on Home. Ana removes him after the confirmation, and the database has no membership. Daniel's next load of the project says "This project does not exist or is not accessible.", and Projects no longer lists it. The page fits both widths. |
| `theMembersPanelGuardsEachChangeAndOnlyTheProjectOwnerOfAnActiveProjectSeesIt` (1280 pixels, new) | **Disabled control:** Rui's Remove is disabled with its reason linked; after Ana unassigns him on the Task detail, it is enabled. The Owner's row has no Remove.<br>**Cancel:** sends no request.<br>**A 409 race:** Rui assigns himself through the API while Ana's confirmation is open. Ana gets "Rui Martins is assigned to tasks of this project; remove those assignments first.", and Remove is disabled again.<br>**A failed search:** a 500 shows "Something went wrong. Please try again." and keeps the text; the next search succeeds.<br>**Repeated clicks:** Add and the confirmation's Remove each send one request.<br>**Already a member:** Sofia, added through the API after the search, gets "… is already a member of the project.", and the list shows her.<br>**Rui** sees the members and no controls. **Eva**, not a member, gets "not accessible". After the project is completed, Ana sees no controls. The database has the expected three memberships. |
| `aSessionEndedElsewhereSendsThePersonToSignInWhenTheySearchForPeopleToAdd` (new) | **A 401:** the person is taken to Sign-in, with "Your session has ended. Please sign in again." |
| `theFiveScreensServeEachKindOfPerson` (changed) | Ana's member list now shows Remove, and its reason, on Rui's row, and "Add participant". |

The loading, empty, success, and error states of the screens are still covered by the I13 tests; the panel's empty state is "No people to add.".

## 4. Mocks, fixtures, and secrets

```text
$ grep -rniE "mock|fixture|fake|stub" frontend/src --include=*.ts --include=*.tsx | grep -v "\.test\."
(no output)
```

**Mocks:** none outside the unit test of `http.ts`.

**Secrets and personal data:**

- The diff contains no literal password, secret, key, or token.
- `.env` and the ignored files are not staged.
- The test people have invented demonstration names.

## 5. The wireframe

Markers 10–12 of the [Project dashboard wireframe](../ui/project-dashboard.md) are now built. Marker 12 chooses from a search, as the wireframe proposes. Removing asks for a confirmation, which the wireframe does not draw (I6 decision 3).

## 6. Still open

- **Privacy of the search:** every person's display name can be found by any Project Owner ([A-030](../constraints.md#assumptions); [OQ-011](../constraints.md#open-questions)). This is a High privacy risk, pending the client.
- **No paging** of the eligible people or of the members ([A-030](../constraints.md#assumptions), [A-034](../constraints.md#assumptions)).
- **A 404 on removal** (the person was removed elsewhere) shows the generic "This project does not exist or is not accessible." after the list is fetched again. The I6 decisions did not give a specific message.
- **New message:** "This person was not found." for a 422 on adding, which can only happen if a person disappears between the search and the click. It is not in the I6 decisions, and is left to the team's review.
- ~~**The exception to rule 2:** I5 and I8 are still Awaiting review.~~ *Resolved on 2026-11-12: the project team accepted I5, I8, and I6 (the conversation “Implement the Matriz front end”, the acceptance review).*
