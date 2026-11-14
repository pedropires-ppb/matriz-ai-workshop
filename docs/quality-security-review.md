# Week 4 Quality and Security Review

**Status:** Bounded code and security reviews and full validation completed on 2026-11-13.
No confirmed security vulnerability or Spec-axis issue was found. The only
Standards-axis note and the user's disposition are recorded below. This review
does not accept I12 or validate the proposed requirements with the client.

## Scope

The review was limited to the following diffs and authorization context.

| Scope | Commit range | Files |
|---|---|---|
| Week 4 changes | `08e531fde03a87fd7a6c4f38a56f5b1bd2e105a8..d192ae3b6899d643b90cf928bfb6a81b93afcbf0` | `README.md`; `docs/test-plan.md`; `src/test/java/com/matriz/TaskApiTests.java`; `src/test/java/com/matriz/tasks/TaskDomainTests.java`; `src/test/java/com/matriz/UserInterfaceTests.java` |
| I7 task assignment and assignee state-change functionality | `4acf95413ed8e4f49535702358ed2eaadc267277..067223e0827a9b2b44abf17a413804cd997b52e8` | `src/main/java/com/matriz/accesscontrol/application/TaskAccessRules.java`; `src/main/java/com/matriz/tasks/TaskAccessPolicy.java`; `src/main/java/com/matriz/tasks/TaskAction.java`; `src/main/java/com/matriz/tasks/TaskFacts.java`; `src/main/java/com/matriz/tasks/application/AssignTask.java`; `src/main/java/com/matriz/tasks/application/ChangeTaskState.java`; `src/main/java/com/matriz/tasks/application/CheckTaskAccess.java`; `src/main/java/com/matriz/tasks/application/InvalidTaskException.java`; `src/main/java/com/matriz/tasks/application/NotProjectOwnerException.java`; `src/main/java/com/matriz/tasks/application/TaskNotFoundException.java`; `src/main/java/com/matriz/tasks/application/TaskRules.java`; `src/main/java/com/matriz/tasks/application/UnassignTask.java`; `src/main/java/com/matriz/tasks/domain/Assignment.java`; `src/main/java/com/matriz/tasks/domain/Task.java`; `src/main/java/com/matriz/tasks/web/TaskContent.java`; `src/main/java/com/matriz/tasks/web/TaskController.java`; `src/test/java/com/matriz/AssignmentApiTests.java`; `src/test/java/com/matriz/TaskApiTests.java`; `src/test/java/com/matriz/accesscontrol/application/TaskAccessRulesTests.java`; `src/test/java/com/matriz/tasks/TaskDomainTests.java` |

Authorization context was read, not treated as an additional functional diff:
project/task controllers and use cases, `ProjectAccess`, `ProjectAccessPolicy`,
`ProjectFactsProvider`, `ProjectAccessRules`, `TaskAccessPolicy`,
`TaskAccessRules`, `CheckTaskAccess`, task and project persistence adapters,
and `SecurityConfiguration`. Relevant existing API and access-rule tests were
included in the checks below. No public deployment or unrelated repository-wide
audit was in scope.

The expectations follow [the proposed permission matrix](requirements.md#permission-matrix-proposed),
[NFR-001/NFR-002](requirements.md#non-functional-requirements-and-unresolved-quality-criteria),
[US-003/004/005/007/009/011/012/016/017/019/020](user-stories.md#acceptance-criteria),
[EX-003/004](user-stories.md#edge-cases-and-exceptions), [TP-003](test-plan.md),
[the API contract](../openapi.yaml), and [ADR-004](adr/ADR-004-rest-api-single-entry-point.md)
and [ADR-005](adr/ADR-005-authentication-with-jwt.md). These permission rules are
proposals based on assumptions and remain pending client validation.

## Checks and results

| Command or review | Result |
|---|---|
| `./mvnw -B -Dtest=CrossProjectAccessTests,TaskApiTests,ProjectApiTests,AssignmentApiTests,TaskAccessRulesTests,ProjectAccessRulesTests test` (Temurin JDK 25.0.5; Testcontainers PostgreSQL) | Passed: 197 tests, 0 failures, 0 errors, 0 skipped. The run covered owner/non-member access, refusals, unchanged data, and mismatched project/task paths. It included an authorized task GET control before the unauthorized requests. |
| `./mvnw -B -Dtest=LayeringTests,ModuleBoundaryTests test` (Temurin JDK 25.0.5) | Passed: 9 tests, 0 failures, 0 errors, 0 skipped. |
| Both Maven runs' frontend lifecycle | Each completed TypeScript typecheck, 31 Vitest tests, and Vite production build. |
| `./mvnw -B clean verify` (Temurin JDK 25.0.5; Testcontainers PostgreSQL; Chromium) | Passed: 708 Java tests, 0 failures, 0 errors, 0 skipped; 31 Vitest tests passed; TypeScript typecheck and Vite production build completed; Maven reported `BUILD SUCCESS`. |
| `git diff --check` and local `rg` scan of both approved diffs and this report for PEM private-key, AWS access-key, GitHub-token, and JWT-shaped patterns | Whitespace check passed; secret-pattern scan returned 0 matches. This is a narrow heuristic scan, not a general secret scanner. |
| Standards and Spec review of the Week 4 range | One low-priority Standards note below; no Spec findings. |
| Bounded security review of Week 4, I7, and authorization context | No confirmed vulnerabilities reported. |

The authorization tests use isolated Testcontainers databases, generated
fictitious identities and random test signing keys; demo data is disabled.
Account A (Ana) owns the project and task; account B (Eva) is not a member of
that project. The existing `TaskApiTests#aPersonWhoIsNotAMemberGets404ForEveryOperationAndNothingChanges`
checks a successful owner read as a control, then non-member read/change
refusals and unchanged data. `TaskApiTests#aTaskOfAnotherProjectIsNotFoundThroughThisOne`
and `CrossProjectAccessTests#aTaskOfOneProjectNamedThroughAnotherIsNotFoundForEveryOperationAndEveryPerson`
exercise mismatched identifiers and re-check persisted state. The test classes
also create other fictitious fixtures for their broader cases; the checks did
not use an exclusively two-person database fixture. They did verify the
authorized A / unauthorized B relationship with those fictitious accounts.

## Findings and decisions

| ID | Severity | Evidence and impact | Confidence | Classification and disposition |
|---|---|---|---|---|
| QSR-001 | LOW | At review time, focused command text appeared in `README.md:255–257` and `docs/test-plan.md:113, 165, 178, 189`. Repetition could allow documentation to drift; no drift or runtime defect was observed. | 9/10 | Confirmed documentation duplication, not a vulnerability. The user chose to keep both copies as an exception because the exercise separately requires README command documentation and actual test commands/results in the test plan. No edit was made. |

**Proposed resolution considered:** retain commands where they are required;
if those documentation requirements change, establish one canonical command
source and link to it from the other document. No automated documentation
regression test is configured; `git diff --check` is the available text hygiene
check, not a duplication detector.

No finding was corrected, deferred for technical work, or rejected as a
vulnerability. The sole standards recommendation was reviewed and retained as
an explicit exception to the general no-duplication rule. No code change was
made from the review.

## Tooling and limitations

The repository configures Maven Enforcer and build/test plugins, but no static
security analyzer, Java linter, or documentation linter was found in the
reviewed build and CI configuration. Existing `LayeringTests` and
`ModuleBoundaryTests` are architecture tests, not static security analysis.
No new tool was installed. No independent secret-scanner was configured or run;
the bounded review and narrow pattern scan above did not identify a secret.
The tests use fictitious data and do not expose tokens or signing keys.

`./mvnw -B clean verify` completed successfully as recorded above. No production
fix or regression test was needed. No PR was created or pushed as part of this
review record.

## Human decisions — 2026-11-13

| Decision | Recorded answer and consequence |
|---|---|
| Review scope | User approved the Week 4 range, I7 functional range, named authorization paths, configured checks, and isolated local authorization verification. No broader audit or public deployment test was authorized. |
| QSR-001 disposition | User chose to keep commands in both README and the test plan and record this as an instruction-required exception. |
