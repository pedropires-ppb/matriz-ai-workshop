# Matriz - Week 4 Task Test Plan

**Status:** Exercises 1–5 have been completed under their separate approvals.
Exercise 1 scenarios remain **Not run**; targeted checks, the bounded Exercise 5
review, and full validation were performed on 2026-11-13. These exercises do not
imply client acceptance.

**Branch:** `week-4`, created from freshly fetched `origin/main`.
**Week 4 review base:** `08e531fde03a87fd7a6c4f38a56f5b1bd2e105a8`.
Use this fixed base for the eventual single Week 4 review against `main`.
The unrelated untracked `copilot-worktrees/` directory was left untouched.

## Scope and sources

Selected functionality: implemented task creation, retrieval and title editing,
with project membership and operation permissions. Task management, assignments,
comments, deletion, filters and paging are implemented; the earlier I4 assignee
gap was closed by I7. See the [increment statuses](implementation-plan.md#increments).
I12 remains Awaiting review, not Accepted. This plan does not claim complete MVP
coverage or satisfaction of the [release criteria](requirements.md#mvp-scope).

Expected results come from [requirements](requirements.md#functional-requirements),
the [permission matrix](requirements.md#permission-matrix-proposed), and the linked
[acceptance criteria](user-stories.md#acceptance-criteria), not observed application
behavior. The [constraints](constraints.md), [architecture](architecture.md),
[approved provisional ADRs](adr/README.md), [data model](database.md), and
[API contract](../openapi.yaml) were inspected before proposing scenarios.
Only F01-F03 are confirmed client needs; detailed expectations remain provisional
pending client validation.

## Selected scenario matrix

IDs are local test-plan identifiers, not new FR, US or EX entries.
`P` means `/api/projects/{projectId}/tasks`; `T` means `P/{taskId}`.
Coverage means tests found in source, not freshly executed evidence.

| ID and traceability | Actor and preconditions | Inputs and action | Observable expected result | Test level | Existing coverage | Proposed gap or change |
|---|---|---|---|---|---|---|
| **TP-001: Invalid title and boundaries.** [FR-008/009](requirements.md#functional-requirements); [US-003](user-stories.md#us-003--create-task), [US-011](user-stories.md#us-011--rename-task), [EX-002/011](user-stories.md#edge-cases-and-exceptions). Technical text restriction: [Assumption A-037](constraints.md#assumptions). | Signed-in Project Owner; Active project; an existing task for edit checks. | `POST P` with JSON; `PATCH T` with JSON Merge Patch. Exercise missing/null title on create, null title on edit, empty and whitespace-only strings, including tabs/newlines and U+00A0/U+2007/U+202F; 1, 150 and 151 code points after trimming; supplementary Unicode and padded boundaries. Technical cases: NUL and unpaired surrogates. | Invalid title: 422 Problem Details with an error pointing to `/title`; no task created or existing task changed. Valid boundary: 201 create or 200 edit, with trimmed title retained. Missing title in an otherwise valid edit leaves the title unchanged. Browser invalid submission identifies the field and sends no mutation request. | Domain/value object, API/persistence, browser. | [TaskDomainTests](../src/test/java/com/matriz/tasks/TaskDomainTests.java): null/blank, Unicode lengths and trimming. [TaskApiTests](../src/test/java/com/matriz/TaskApiTests.java): create/edit blank and length refusals, defaults, unchanged data; `aTitleThatIsEmptyAfterTrimmingIsRefusedWithoutChangingExistingTasks` preserves a valid task and row count after invalid create. [ContractTextTests](../src/test/java/com/matriz/ContractTextTests.java): padded Unicode request-schema boundaries. [UnusualTextTests](../src/test/java/com/matriz/UnusualTextTests.java): unstorable text create/edit regressions. [UserInterfaceTests](../src/test/java/com/matriz/UserInterfaceTests.java): creation validation. | Reuse ordinary boundaries. Exercise 2 added direct TaskTitle rejection assertions for NUL/unpaired surrogates. Exercise 3 strengthened the API invalid-create case to check a prior task is unchanged. API edit boundaries with padded supplementary Unicode and browser edit validation with retained input remain planned for Exercise 4. Do not duplicate API technical-text regressions. |
| **TP-002: Create valid task and retrieve through API.** [FR-006/008/018](requirements.md#functional-requirements); [US-003](user-stories.md#us-003--create-task), [US-004](user-stories.md#us-004--view-tasks), [US-018](user-stories.md#us-018--set-task-priority). | Signed-in Project Owner; Active project; no assignee supplied. | `POST P` with `{"title":"  Prepare proposal  "}`, then `GET` the returned `Location`. | Create: 201 with `Location` identifying the created task. Retrieve: 200 with the same task/project identifiers, normalized title, `TO_DO`, `MEDIUM`, empty `assignees`, and omitted description. Both bodies satisfy the contract's `Task` schema and agree. | API with persistence; reuse browser workflow. | [TaskApiTests](../src/test/java/com/matriz/TaskApiTests.java): `creatingATaskStartsItToDoWithPriorityMediumAndNoAssignee` now checks the created row in PostgreSQL, follows `Location` with a separate GET, validates the response schema, and compares the full bodies. `tasksAreListedOldestFirstWithTheirDetails` also retrieves created tasks. [TaskRepositoryTests](../src/test/java/com/matriz/TaskRepositoryTests.java): every field reloaded in a new persistence context. [UserInterfaceTests](../src/test/java/com/matriz/UserInterfaceTests.java): create and open task. | Extended the existing default-creation API test with a subsequent `GET Location`, response status/schema and full-body equality, plus stored project/title assertions. No duplicate test class or equivalent repository/browser happy-path test. |
| **TP-003: Authenticated non-member and cross-project task access.** [FR-001/006](requirements.md#functional-requirements); [EX-003](user-stories.md#edge-cases-and-exceptions), [NFR-002](requirements.md#non-functional-requirements-and-unresolved-quality-criteria). | Signed-in Member of project A, not B; separately a Member of both projects using B's task identifier under A. Tasks exist and are visible to their legitimate owners. | Attempt task retrieval, edit, state change and deletion through B's path or a mismatched project/task path. Existing broader coverage also checks comments and assignments. Open the inaccessible task's browser address. | API: 404, not 403, with generic non-disclosing Problem Details; legitimate owner can still retrieve unchanged data. Browser: inaccessible-project notice with no task content disclosed. | Access policy, API, repository scoping, browser. | [TaskAccessRulesTests](../src/test/java/com/matriz/accesscontrol/application/TaskAccessRulesTests.java): non-member decisions. [TaskApiTests](../src/test/java/com/matriz/TaskApiTests.java): non-member and mismatched-path refusals with unchanged data. [CrossProjectAccessTests](../src/test/java/com/matriz/CrossProjectAccessTests.java): eight task operations across seven actor/path combinations, with unchanged-data snapshots. [TaskRepositoryTests](../src/test/java/com/matriz/TaskRepositoryTests.java): project-scoped reads. Browser `theFiveScreensServeEachKindOfPerson`: inaccessible direct task address at two existing widths. | Reuse these tests; no demonstrated new-test gap. Do not create a duplicate cross-project permission suite merely to satisfy the worksheet. |

**Rationale:** TP-001 checks rejected input and normalization boundaries; TP-002
checks the API's create-to-read boundary and persisted defaults; TP-003 checks
project isolation independently of authentication and global account identity.
Assertions strengthen specific gaps rather than treating all existing coverage
as missing.

## Permission and contract distinctions

These explain the selected scenarios; they are not additional selected cases.
Follow [ADR-004's status precedence](adr/ADR-004-rest-api-single-entry-point.md#decision).

| Caller or condition | Contract distinction |
|---|---|
| Person without a valid sign-in | 401, Bearer challenge and Problem Details; no task information disclosed or changed. An ended token is also refused. |
| Signed-in non-member | 404 for inaccessible project/task, never 403. Membership in another project grants no access. |
| Project Participant without operation permission | 403 for owner-only create/edit/delete, even if assigned. An unassigned Project Participant gets 403 on task state changes. |
| Project Owner or assigned Project Participant, Active project | State changes use `PUT T/state` with JSON; any state may change to any other. Assignment does not grant title/priority editing permissions. |
| Completed project | Valid mutations by otherwise authorized callers get 409. Membership/permission and input errors take precedence. Members can still read tasks and add comments. |

State codes are `TO_DO`, `IN_PROGRESS`, `DONE`; Task priority codes are
`LOW`, `MEDIUM`, `HIGH`. Create and state requests use `application/json`;
task edits use `application/merge-patch+json`. Errors use
`application/problem+json`. Creation accepts only title, optional description
and optional priority; assignees are added by the separate assignment operation.
Descriptions have at most 2000 code points after trimming; null/blank means none
and the response omits the description.

## Assumptions, corrections and unresolved matters

No new assumption is introduced. Applicable existing assumptions:

| Basis | Limits on interpretation |
|---|---|
| [Assumption A-001](constraints.md#assumptions), [Assumption A-002](constraints.md#assumptions) | Per-project roles and membership permissions remain client-unvalidated (OQ-002). |
| [Assumption A-004](constraints.md#assumptions), [Assumption A-033](constraints.md#assumptions) | Title length is **1-150**, not the example's minimum of 3; count Unicode code points after the approved Java-whitespace-plus-space-separator trim (OQ-003/OQ-030). |
| [Assumption A-005](constraints.md#assumptions), [Assumption A-010](constraints.md#assumptions) | Starting state is `TO_DO`, not `TODO`; transitions are not a fixed sequence (OQ-004). |
| [Assumption A-019](constraints.md#assumptions), [Assumption A-020](constraints.md#assumptions), [Assumption A-035](constraints.md#assumptions) | Any assignee may change state. Self-assignment enables a participant's state changes; suggesting that participants can never change tasks would contradict the interim decision. The known tension with NFR-002 remains open (OQ-002/OQ-025). |
| [Assumption A-021](constraints.md#assumptions) | Task priority defaults to Medium; API codes are uppercase, not the display labels (OQ-022). |
| [Assumption A-011](constraints.md#assumptions) | Completed-project restrictions have a comment exception. Do not derive a product reopening operation from tests that alter fixture state directly (OQ-006). |
| [Assumption A-014](constraints.md#assumptions), [Assumption A-016](constraints.md#assumptions) | Use prepared fictitious identities and valid sign-in for membership tests; authentication failure is a separate condition (OQ-018/OQ-027). |
| [Assumption A-037](constraints.md#assumptions) | NUL/unpaired surrogates are refused, not silently changed. OQ-031 remains unresolved: U+200B/U+FEFF are not whitespace under the interim trim and must not be rejected merely for looking blank. |

All client questions remain in the [open-question register](constraints.md#open-questions).
The historical I4 assignee refusal is not the current expectation: the register's
[delivery reconciliation](constraints.md#delivery-reconciliation--2026-11-12) and
I7 close that gap. API identifiers remain opaque to clients; use returned identifiers
or the existing UUID fixture helpers, not illustrative short IDs from examples.
No new naming, uniqueness, reopening, performance or accessibility rule is proposed.

### Human decisions - 2026-11-13

| Decision | Recorded answer and consequence |
|---|---|
| Scenario selection | User chose **only TP-001-TP-003**, rather than the recommended TP-001-TP-005 or all seven rows. TP-004 (authentication), TP-005 (member operation permissions), TP-006 (Completed project), and TP-007 (filters/paging) are not selected scenarios. Existing coverage is not removed. |
| Exercise 1 approval | User approved creating only this document, recording selected scenarios/coverage, proposed later work and Not run statuses, then reviewing and committing it. No tests or production changes are authorized by this approval. |
| Exercise 2 approval | User approved the focused domain-test plan: assert NUL and unpaired-surrogate rejection, strengthen the 150-code-point boundary assertion, run `TaskDomainTests` twice, and update this plan and README. Production code remains out of scope. |
| Exercise 3 approval | User approved extending the existing TaskApiTests create/retrieve flow and invalid-title no-change assertion, running the targeted integration tests twice, and updating this plan and README. No production, API-contract, or dependency changes were authorized. |
| Exercise 4 approval | User approved extending the existing `theProjectOwnerCreatesChangesFiltersAndDeletesTasksWithoutReloadingThePage` browser test—not duplicating the flow—to verify the real create/list requests, immediate no-reload behavior, visible task values and PostgreSQL persistence, then run that method twice and update this plan and README. No production code, hooks, or successful-response mocks were authorized. |
| Exercise 5 review approval | User approved review of the Week 4 diff and I7 task-assignment diff, the named authorization paths, and isolated local verification using existing tests. No public deployment test, broad repository audit, new tools, or edits before review were authorized. |
| Exercise 5 documentation disposition | User chose to retain the focused commands in both README and this test plan as specifically required by the exercise, recording the repetition as an exception to the general no-duplicate-content rule. |
| Corrections | No further user correction to expected behavior was supplied. The corrections above are source-derived AI suggestion corrections, not new human or client decisions. |

## Exercise 2-5 results and review

Exercises 2–4 were executed under their separate approvals. Exercise 4 reuses
the existing browser journey because it already creates tasks and verifies the
task list without reloading; the extension adds evidence for the subsequent
real list request and the persisted row rather than duplicating that journey.
Exercise 5 reuses existing authorization tests and makes no code changes.

| Exercise | Test changes / planned reuse | Targeted command | Current result |
|---|---|---|---|
| 2 | TP-001: added direct `TaskTitle` rejection of NUL and unpaired high/low surrogates in `aTitleThatCannotBeStoredIsRefused`; strengthened `aTitleIsTrimmedAndHasOneToOneHundredAndFiftyCharacters` to assert the 150-code-point supplementary-character boundary. Existing valid, whitespace and 1/150/151 cases were reused. | `./mvnw -B -Dtest=TaskDomainTests test` (select JDK 25+) | **Passed twice:** 22 tests each run; 0 failures, errors or skips. |
| 3 | TP-001: extended `aTitleThatIsEmptyAfterTrimmingIsRefusedWithoutChangingExistingTasks` in [TaskApiTests](../src/test/java/com/matriz/TaskApiTests.java) to preserve an existing row and its retrieved representation after invalid create. TP-002: extended `creatingATaskStartsItToDoWithPriorityMediumAndNoAssignee` with a separate `GET Location`, schema/body equality, and persisted project/title assertions. | `./mvnw -B -Dtest=TaskApiTests test` (select JDK 25+; Docker required) | **Passed three times:** 94 tests each run; 0 failures, errors or skips. |
| 4 | TP-002: extended `theProjectOwnerCreatesChangesFiltersAndDeletesTasksWithoutReloadingThePage` in [UserInterfaceTests](../src/test/java/com/matriz/UserInterfaceTests.java). The existing fictitious Project Owner opens an Active project, creates `  Write the report ` with the existing accessible controls, and verifies the visible normalized title, `To do`, `High`, no assignee and task count. Added assertions for POST 201 followed by collection GET 200, the no-reload marker immediately after creation, and the PostgreSQL task/project/title/state/priority/deletion/assignment values. | `./mvnw -B -Dtest=UserInterfaceTests#theProjectOwnerCreatesChangesFiltersAndDeletesTasksWithoutReloadingThePage test` (select JDK 25+; Docker required) | **Passed twice:** 1 test each run; 0 failures, errors or skips. |
| 5 | TP-003: reused `aPersonWhoIsNotAMemberGets404ForEveryOperationAndNothingChanges`, `aTaskOfAnotherProjectIsNotFoundThroughThisOne`, and `aTaskOfOneProjectNamedThroughAnotherIsNotFoundForEveryOperationAndEveryPerson`, plus project/task permission-rule tests. Account A's authorized GET is a control; account B's non-member reads/changes and mismatched project/task identifiers receive 404, with unchanged task/database state. | `./mvnw -B -Dtest=CrossProjectAccessTests,TaskApiTests,ProjectApiTests,AssignmentApiTests,TaskAccessRulesTests,ProjectAccessRulesTests test` (select JDK 25+; Docker required) | **Passed:** 197 tests; 0 failures, errors or skips. |

The browser test uses the existing `TestAccess`, `addProject`, `inATab`, and
accessible-label/role helpers. It uses no test-specific selectors or fixed sleeps.
See the [bounded quality and security review](quality-security-review.md) for
review ranges, authorization evidence, findings, and limitations.

### Isolation, repeatability and safe evidence

Follow [README verification](../README.md#verification) and
[ADR-007](adr/ADR-007-build-persistence-and-demonstration-data.md).
[DatabaseTestSupport](../src/test/java/com/matriz/DatabaseTestSupport.java) obtains
an empty database per application context from
[SharedPostgres](../src/test/java/com/matriz/SharedPostgres.java), which shares
one Testcontainers PostgreSQL 18.6 container per test run. Flyway migrates it;
demo data is disabled for these suites. This is not a fresh database or rollback
per test method: tests must create unique fictitious people/projects and scope
assertions to their own identifiers. Existing fixture helpers support that.
Repository reloads use separate transactions/persistence contexts. Browser
`inATab` creates and closes a fresh browser context for each workflow.
Testcontainers owns cleanup of its resources.

For Exercise 3, `TaskApiTests` has no test-managed transaction: its authenticated
MockMvc `POST` returns before the subsequent `GET Location`, and the test also
checks the PostgreSQL task row directly. The suite uses a fictitious Project
Owner and Active project created for the test; the created title, normalized
title, project ID, default state/priority, empty assignees and absent description
are checked. The invalid-title case first creates a valid task, then verifies
422, unchanged GET representation and unchanged project task-row count. No
test-specific cleanup is needed: the Testcontainers PostgreSQL container and
its per-context database are owned by the test run and removed by Testcontainers.

For Exercise 4, `UserInterfaceTests` starts the real application on a random
port and uses the same Testcontainers PostgreSQL infrastructure with demo data
disabled. The browser signs in as a generated fictitious person and opens a
uniquely created Active project fixture. The POST and subsequent task-list GET
use the real application; the test checks the created row directly in PostgreSQL
before later edits and deletion. `inATab` closes its fresh browser context, the class
teardown closes Playwright/Chromium, and Testcontainers owns its database and
container cleanup. Both runs created their own PostgreSQL 18.6 test container;
no local Compose database, `.env`, or existing Docker volume was used.

Never use/reset the local Compose database, edit `.env`, delete existing volumes,
or print/record credentials, tokens, hashes, signing keys or real personal data.
Do not enable browser traces, HAR, videos or screenshots containing authentication
material; no artifacts were needed. Leave unrelated
files and generated dependencies unstaged.

## Execution record and limitations

**Exercise 1 tests: Not run.** Source inspection and historical coverage are not
fresh execution.

**Exercise 2:** `TaskDomainTests` ran twice with
`./mvnw -B -Dtest=TaskDomainTests test` under the installed Temurin JDK 25.0.5,
selected temporarily through `JAVA_HOME`. Each run reported 22 tests, 0 failures,
0 errors and 0 skipped. The existing valid trimmed title and one-character
minimum assertions, the updated 150-code-point supplementary-character
assertion, existing 151-code-point rejection, and the three values in
`aTitleThatCannotBeStoredIsRefused` were exercised. Maven also completed
frontend typecheck, 31 Vitest tests and the Vite production build on each run.
The first invocation without a JDK override stopped at Maven Enforcer because
the shell selected JDK 21; this was an environment configuration limitation,
not a test failure. Selecting the already-installed JDK 25 resolved it. No
application was started or database-backed test selected. Broader verification
was not run.

**Exercise 3:** `TaskApiTests` ran three times with
`./mvnw -B -Dtest=TaskApiTests test`, selecting the installed Temurin JDK 25.0.5
through `JAVA_HOME` and using Testcontainers PostgreSQL 18.6. Each run reported
94 tests, 0 failures, 0 errors and 0 skipped. All three runs also completed
frontend typecheck, 31 Vitest tests and the Vite production build. Each created
and used an isolated Testcontainers database; none accessed the local Compose
database or `.env`. The invalid-title parameter cases and the explicit
create-then-GET persistence flow ran as part of this test class. Broader
verification was not run.

**Exercise 4:** The targeted browser method ran twice with
`./mvnw -B -Dtest=UserInterfaceTests#theProjectOwnerCreatesChangesFiltersAndDeletesTasksWithoutReloadingThePage test`,
selecting Temurin JDK 25.0.5 through `JAVA_HOME`. Each run reported 1 test,
0 failures, 0 errors and 0 skipped. Each run also completed frontend typecheck,
31 Vitest tests and the Vite production build. Playwright used Chromium against
the random-port application and an isolated Testcontainers PostgreSQL 18.6
database. The real task POST returned 201, the subsequent task-list GET returned
200, the task appeared with the expected visible values without a page reload,
and PostgreSQL contained the expected active task row with no assignment. The
Exercise 4 targeted method ran twice; the later full validation also ran all 46
`UserInterfaceTests` with no failures.

**Exercise 5:** On 2026-11-13, with Temurin JDK 25.0.5 selected through
`JAVA_HOME`, `./mvnw -B -Dtest=CrossProjectAccessTests,TaskApiTests,ProjectApiTests,AssignmentApiTests,TaskAccessRulesTests,ProjectAccessRulesTests test`
passed 197 tests (0 failures, errors or skips) using isolated Testcontainers
PostgreSQL. The existing owner GET is the successful control; the unauthorized
and mismatched-ID cases returned the contract's 404 and preserved the task/data.
`./mvnw -B -Dtest=LayeringTests,ModuleBoundaryTests test` passed 9 tests
(0 failures, errors or skips). Each Maven run also completed frontend
typecheck, 31 Vitest tests and Vite production build. The bounded Security review
reported no confirmed vulnerability; the Spec review reported no findings.
The Standards review's duplicate-command note was retained as the
instruction-required exception recorded above and in the
[quality/security review](quality-security-review.md). No static security
analyzer is configured; no tool was installed. No public deployment was tested.
The full suite is recorded below.

Git preparation executed successfully: `git status --short`, branch/current-base
inspection, history inspection, `git fetch origin`,
`git rev-list --left-right --count main...origin/main` (0/0),
`git ls-remote --heads origin week-4` (no remote branch),
`git worktree list --porcelain`, and `git switch -c week-4 origin/main`.
No existing local `week-4` branch was present. Only read-only source/coverage
inspection and documentation work followed.

The [README](../README.md#verification) and [pom.xml](../pom.xml) confirm JDK 25+,
Maven Wrapper, JUnit/AssertJ, MockMvc, networknt JSON Schema validation,
Testcontainers, Playwright Java/Chromium, Vitest, ArchUnit and Spring Modulith.
Maven also runs frontend installation, typecheck, Vitest and Vite build before
Java tests and installs Chromium unless Java tests are skipped. Focused selectors
therefore do not eliminate those lifecycle prerequisites.

Application commands are documented in
[Configuration and first start](../README.md#configuration-and-first-start):
`./mvnw -B -DskipTests package` and
`java -jar target/matriz-0.1.0-SNAPSHOT.jar`. Neither was run; startup was
unnecessary for these exercises. JDK 25, Docker, Testcontainers PostgreSQL and
Chromium were available for the executed checks. There is no demonstrated
runtime failure to classify.

**Full validation:** On 2026-11-13, `./mvnw -B clean verify` completed with
`BUILD SUCCESS` using Temurin JDK 25.0.5. Maven reported 708 Java tests with
0 failures, 0 errors and 0 skipped; the frontend typecheck completed, all 31
Vitest tests passed, and the Vite production build completed. The build used the
repository's Testcontainers PostgreSQL and browser-test setup. It does not
accept I12 or answer client questions.

Create the single Week 4 PR only after final validation and staged-file review.
