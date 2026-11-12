# Matriz — Verification record for I12, release verification (2026-11-12)

**Status:** In progress. It holds the part of [I12](../implementation-plan.md#i12--release-verification) that is not one of the nine scenarios of the [verification of nine cases](../verification.md): the user stories and exceptional cases, NFR-001, NFR-002, and NFR-008, the MVP release criteria, the secret scan, the review of failed sign-ins ([OQ-027](../constraints.md#open-questions)), the README on a clean checkout, and the list of open questions.

**Result: Awaiting review.** I12 is not Accepted: every item below is either Confirmed with evidence, Partly confirmed, or Not run, with the reason, and the project team decides. Nothing here changes a requirement. The nine scenarios, the three new automated tests, and the two bugs found and fixed are in the [verification of nine cases](../verification.md).

## Summary of the I12 items

| Item | Status | Where |
|---|---|---|
| Full automated suite (permission matrix, contract checks, module boundary and layering) | Confirmed | `./mvnw -B clean verify` → BUILD SUCCESS, 685 backend tests and 23 Vitest tests, 0 failures ([Automated suite](#automated-suite)) |
| Every acceptance criterion of US-001–US-026 | Partly confirmed: traced and every suite passes; not walked criterion by criterion | [User stories](#user-stories-us-001us-026) |
| Every exceptional case EX-001–EX-032 | Partly confirmed: 32 of 32 have a test or a manual check; 7 have no test or record that names them | [Exceptional cases](#exceptional-cases-ex-001ex-032) |
| NFR-001 (authentication) | Confirmed | [Non-functional requirements](#non-functional-requirements) |
| NFR-002 (authorization) | Confirmed | [Non-functional requirements](#non-functional-requirements) |
| NFR-008 (language) | Partly confirmed: source scan and the English tests; no full manual walkthrough | [Non-functional requirements](#non-functional-requirements) |
| NFR-003–NFR-007 | Not verifiable: no criterion ([OQ-010](../constraints.md#open-questions), [OQ-011](../constraints.md#open-questions)) | [Non-functional requirements](#non-functional-requirements) |
| MVP release criteria | Not met yet: depends on the two lines above, and the client has not approved the scope ([OQ-016](../constraints.md#open-questions)) | [MVP release criteria](#mvp-release-criteria) |
| End-to-end walkthrough of the stories in English | Partly confirmed: Playwright tests and the browser probes of [Case 09](../verification.md#case-09--deletion-and-errors); no complete manual walkthrough | [Walkthrough](#end-to-end-walkthrough) |
| Secret and personal-data scan | Confirmed for the repository and its history, as far as a local scan goes; no GitHub secret scan was run | [Secret scan](#secret-and-personal-data-scan) |
| Failed sign-ins (OQ-027) | Reviewed: there is still no limit; the client's answer is pending | [Failed sign-ins](#failed-sign-ins-oq-027) |
| README followed on a clean machine | Partly confirmed: the build and start from a fresh clone; not on a clean machine | [README](#readme-on-a-clean-checkout) |
| Updated open questions and assumptions | Done | [Open questions](#open-questions-and-assumptions-for-the-client) |
| Documentation links | Confirmed: 2562 relative links in 30 Markdown files, 0 broken | [Links](#documentation-links) |

## Automated suite

- Command: `./mvnw -B clean verify` (JDK 25, Docker running, macOS), run on the branch `week3-ex8` after the fixes of the two bugs and the three new test classes.
- Result: BUILD SUCCESS in 3 min 40 s. 685 backend tests (JUnit, with Testcontainers PostgreSQL), 0 failures, 0 errors, 0 skipped; 5 Vitest files, 23 tests, all passed; `LayeringTests` and `ModuleBoundaryTests` passed. The Playwright tests (`UserInterfaceTests`, Chromium) are in the 685.
- Before my changes the same command gave 626 backend tests and 23 Vitest tests, BUILD SUCCESS. The 59 added tests are `UnusualTextTests` (55), `CrossProjectAccessTests` (2), `DataAfterRestartTests` (1), and one regression test in `ParticipantsApiTests` (Bug 1).

## User stories (US-001–US-026)

“Tests that name it” are the test classes and front-end files whose text cites the story; the acceptance criteria were not re-read one by one for this record. The increment's own evidence record, linked in the last column, holds that detail and was accepted by the team.

| Story | Tests that name it | Evidence record of the increment |
|---|---|---|
| US-001 Create project | `ProjectApiTests`, `UnusualTextTests` | [I3, I10, I11](I3-I10-I11-project-management-verification.md) |
| US-002 Add participant | `ParticipantsApiTests`, `MembersSection.tsx` | [I6 participants](I6-participants-verification.md) |
| US-003 Create task | `TaskApiTests` | [I4, I10, I11](I4-I10-I11-task-management-verification.md) |
| US-004 View tasks | `TaskApiTests`, `TasksSection.tsx` | [I4, I10, I11](I4-I10-I11-task-management-verification.md), [I13](I13-user-interface-consolidation-verification.md) |
| US-005 Change task state | `AssignmentApiTests`, `TaskApiTests` | [I6, I7, I9](I6-I7-I9-assignees-and-comments-verification.md) |
| US-006 View progress | `ProgressApiTests`, `UserInterfaceTests` | [I5](I5-progress-and-my-projects-verification.md) |
| US-007 View accessible projects | `ProjectApiTests` | [I3, I10, I11](I3-I10-I11-project-management-verification.md) |
| US-008 Rename project | `ProjectApiTests` | [I3, I10, I11](I3-I10-I11-project-management-verification.md) |
| US-009 Remove participant | `ParticipantsApiTests`, `MembersSection.tsx` | [I6 participants](I6-participants-verification.md) |
| US-010 Complete project | `CompletionApiTests`, `UserInterfaceTests` | [I8](I8-completion-verification.md) |
| US-011 Rename task | `TaskApiTests`, `UnusualTextTests` | [I4, I10, I11](I4-I10-I11-task-management-verification.md) |
| US-012 Assign task | `AssignmentApiTests` | [I6, I7, I9](I6-I7-I9-assignees-and-comments-verification.md) |
| US-013 Sign in | `SignInTests`, `AccessTokenTests`, `UserInterfaceTests` | [I2](I2-verification.md) |
| US-014 Sign out | `AccessTokenTests` | [I2](I2-verification.md) |
| US-015 See who is signed in | `UserInterfaceTests`, `AccessTokenTests` | [I2](I2-verification.md) |
| US-016 Delete project | `ProjectApiTests`, `UserInterfaceTests` | [I3, I10, I11](I3-I10-I11-project-management-verification.md) |
| US-017 Delete task | `TaskApiTests`, `ProgressApiTests`, `UserInterfaceTests` | [I4, I10, I11](I4-I10-I11-task-management-verification.md), [I5](I5-progress-and-my-projects-verification.md) |
| US-018 Set task priority | `TaskApiTests` | [I4, I10, I11](I4-I10-I11-task-management-verification.md) |
| US-019 Unassign a member | `AssignmentApiTests` | [I6, I7, I9](I6-I7-I9-assignees-and-comments-verification.md) |
| US-020 Assign or unassign myself | `AssignmentApiTests` | [I6, I7, I9](I6-I7-I9-assignees-and-comments-verification.md) |
| US-021 Add comment | `CommentApiTests`, `UnusualTextTests` | [I6, I7, I9](I6-I7-I9-assignees-and-comments-verification.md) |
| US-022 Read comments | `CommentApiTests`, `CommentsSection.tsx` | [I6, I7, I9](I6-I7-I9-assignees-and-comments-verification.md) |
| US-023 Search projects | `ProjectApiTests` | [I3, I10, I11](I3-I10-I11-project-management-verification.md) |
| US-024 Filter tasks | `TaskApiTests` | [I4, I10, I11](I4-I10-I11-task-management-verification.md) |
| US-025 View My projects | `ProgressApiTests`, `UserInterfaceTests` | [I5](I5-progress-and-my-projects-verification.md) |
| US-026 View members | `MembersApiTests`, `MembersSection.tsx` | [I6, I7, I9](I6-I7-I9-assignees-and-comments-verification.md) |

No story is without a test that names it. Status: **Partly confirmed**, for two reasons: a mention is not proof that every criterion is checked, and I did not repeat the walkthrough by hand.

## Exceptional cases (EX-001–EX-032)

| Case | Covered by |
|---|---|
| EX-001 to EX-007, EX-010, EX-011, EX-012, EX-014, EX-018, EX-020, EX-022 to EX-024, EX-026 to EX-028, EX-030 to EX-032 | A test class that names it: `ProjectApiTests` (EX-001, EX-010), `TaskApiTests` (EX-002, EX-011), `MembersApiTests` and `CrossProjectAccessTests` (EX-003; `MembersApiTests` also EX-012), `AssignmentApiTests` (EX-004, EX-020, EX-022, EX-028, EX-030), `ProgressApiTests`, `ProgressDomainTests`, `UserInterfaceTests` (EX-005, EX-027), `CompletionApiTests` and `UserInterfaceTests` (EX-006, EX-018), `ParticipantsApiTests` (EX-007), `SignInTests` and `UserInterfaceTests` (EX-014), `CommentApiTests` (EX-023, EX-024, EX-031, EX-032), `TaskApiTests` and `ProgressApiTests` (EX-026) |
| EX-008, EX-009, EX-029 | No test names them, but the increment records do: [I8](I8-completion-verification.md) (EX-008, EX-009) and [I6, I7, I9](I6-I7-I9-assignees-and-comments-verification.md) (EX-029) |
| EX-013, EX-015, EX-016, EX-017, EX-019, EX-021, EX-025 | **No test and no record names them.** I checked each by hand on the running application (below and [Case 08](../verification.md#case-08--authorization-in-the-api), set D), and I name the existing tests that look likely to cover them. [Proposal] add the identifiers to those tests |

Manual checks, on the application built from this branch (fresh database, fictitious people):

| Case | What I did | Observed |
|---|---|---|
| EX-013 | Ana, owner, creates an unassigned task and changes it to Done | `200`; state DONE; no assignees |
| EX-015 | A project whose only task is Done is completed; its owner then deletes the project | `409` “The project is Completed; it cannot be changed.”; the project is still readable (`200`, COMPLETED) |
| EX-017 | In that Completed project, the owner deletes the task | `409` the same text; the task is still readable (`200`) |
| EX-025 | Eva creates “Security Audit”, of which Ana is not a member; Ana searches “audit” | `items` 0, `totalItems` 0; Eva, who is the owner, finds her project |

Likely candidate tests (named from their method names, not read in full): EX-013 `AssignmentApiTests.everyAssigneeAndTheOwnerChangeTheStateAndNobodyElseDoes`; EX-015 `ProjectApiTests.aCompletedProjectCannotBeDeleted`; EX-016 `ProjectApiTests.aProjectParticipantCannotDelete` and `AssignmentApiTests.aProjectParticipantCannotCreateEditOrDeleteATaskEvenWhenAssigned`; EX-017 `TaskApiTests.aCompletedProjectRefusesEveryChangeButStillShowsItsTasks` and `CompletionApiTests.aCompletedProjectRefusesEveryChangeAndChangesNothing`; EX-019 `TaskApiTests.aProjectParticipantMayViewAndListButNotChangeAnything`; EX-021 `AssignmentApiTests.aProjectParticipantRemovesOnlyTheirOwnAssignment`; EX-025 `ProjectApiTests.searchFindsTheTextInTheNameIgnoringCaseButNotAccentsAndItIsPaged`. For EX-016, EX-019, and EX-021 the manual evidence is [Case 08](../verification.md#case-08--authorization-in-the-api), set D (a Project Participant gets `403` for deleting, editing, changing the priority, and assigning someone else); in that run the unassigning of Ana was `404` because she was not an assignee ([A-035](../constraints.md#assumptions)), so the “unchanged assignees” half of EX-021 rests on the candidate test. Status: **Partly confirmed**: all 32 have a test or a manual check, but seven have only a manual check or an unnamed candidate test as evidence. A first attempt at EX-015 used a wrong endpoint (`POST …/complete`, `404`), so it did not complete the project and the deletion worked; that was my mistake, and I repeated it with `PUT …/state`, which is the one recorded.

## Non-functional requirements

| Requirement | Status | Evidence |
|---|---|---|
| NFR-001 Authentication | Confirmed | [Case 01](../verification.md#case-01--access-to-the-application) (valid and invalid credentials, no token, tampered tokens) and [Case 08](../verification.md#case-08--authorization-in-the-api) (identity only from the token); `SignInTests`, `AccessTokenTests`, `EveryOperationRequiresATokenTests`, `SignOutAfterRestartTests` |
| NFR-002 Authorization | Confirmed | [Case 08](../verification.md#case-08--authorization-in-the-api) (a non-member gets `404` for 18 operations; a participant gets `403` for owner-only operations; nothing changes), `CrossProjectAccessTests`, the permission-matrix tests (`ProjectAccessRulesTests`, `TaskAccessRulesTests`). [Bug 1](../verification.md#bug-1--the-removal-of-a-member-told-a-non-member-whether-a-project-exists) was found and fixed |
| NFR-008 Language | Partly confirmed | A search of `frontend/src` and `src/main` for accented letters and the Portuguese words “Projeto”, “Tarefa”, and “Utilizador” found nothing; the Playwright tests and my browser probes show English messages. I did not walk every interaction by hand. The language is still [OQ-013](../constraints.md#open-questions), decided by the team, not the client |
| NFR-003–NFR-007 | Not verifiable | Their criteria are not defined ([OQ-010](../constraints.md#open-questions), [OQ-011](../constraints.md#open-questions)). I ran no performance, load, availability, or accessibility test |

## MVP release criteria

The [criteria](../requirements.md#mvp-scope) are: every acceptance criterion of US-001–US-026, every exceptional case EX-001–EX-032, and NFR-001 and NFR-002 verified as described. Status: **not yet shown as met.** NFR-001 and NFR-002 are Confirmed; the stories and the exceptional cases are Partly confirmed for the reasons above. Even then, the release is a candidate, not the MVP, until the client approves the scope ([OQ-016](../constraints.md#open-questions)).

## End-to-end walkthrough

The plan asks for a walkthrough of the stories in English. What exists: the Playwright tests (Chromium) in `UserInterfaceTests`, which sign in as the demonstration people and walk project, task, member, assignee, and comment stories through the real interface and API, with no mocks; and my browser probes in [Case 09](../verification.md#case-09--deletion-and-errors). I did not walk every story by hand in a browser. Status: **Partly confirmed**.

## Secret and personal-data scan

- Tracked files, a pattern search for secret-like assignments (long values after `password`, `secret`, `token`, `api key`, `signing key`): two hits, both harmless: `openapi.yaml` has the example value `example-password`, and `DemoDataMigrations.java` has a variable named `passwordEncoder`.
- Private keys, AWS key ids, GitHub tokens, and JWT-shaped strings in tracked files: none.
- `.env` is ignored by git (`.gitignore` line 30) and is not tracked; `.env.example` holds fictitious values. I did not print or read the values of `.env`.
- History: I searched `git log --all -S` for each value of `.env` of 8 characters or more, without printing them. The database password and the four demonstration passwords: 0 commits each. The database URL: 1 commit, which is a public `localhost` address and not a secret.
- E-mail addresses in tracked files: none. The only people are the fictitious Ana Silva, Rui Martins, Eva Costa, and Daniel, with fixed fictitious identifiers ([ADR-007](../adr/ADR-007-build-persistence-and-demonstration-data.md)).
- The records of this exercise contain no token, password, hash, or signing key: tokens and passwords were kept in files outside the repository and were never printed into it.
- **Not run:** the GitHub secret scanning of the hosted repository, because I did not push; and a scan with a dedicated tool such as gitleaks (not installed; I did not add tooling). [Proposal] run one of them before the client demonstration.

## Failed sign-ins (OQ-027)

Thirty wrong passwords for the login name `rui` were sent in a row to the running application: `401` ×30. Then the correct password: `200`. So there is no limit, no delay, and no lock-out, as [ADR-005](../adr/ADR-005-authentication-with-jwt.md#consequences) says. The log held none of the guessed passwords. The client's policy is still open ([OQ-027](../constraints.md#open-questions)); the team's interim answer has no limit. I changed nothing.

## README on a clean checkout

I cloned the branch into a new directory (a fresh working tree with no `target` and no `.env`), ran `./mvnw -B package -DskipTests` (BUILD SUCCESS in 14 s), started the jar with the environment of my test setup on another port, and got `200` for `/actuator/health` with `"db":{"status":"UP"}` and `200` for `/`. That follows README step 3. **Not run:** a clean machine (the Maven, Node.js, and npm downloads were already cached on this one, which is why the build took 14 s); README steps 2 and 6 as written, because they use the fixed Compose project `matriz` and port 5432 of the person's own database, which I did not touch (I used a separate Compose project `matriz-ex8`); and step 4 with the demonstration data from `.env`, which I ran through the same variables in my own environment file.

README findings, reported and not fixed: its summary says the register runs to A-001–A-033 and OQ-001–OQ-030, but the register runs to A-037 and OQ-031 (A-034–A-036 existed before this exercise; A-037 and OQ-031 are added by it); and the plan's I12 row said EX-001–EX-027 while the I12 section says EX-001–EX-032, which I corrected as a flagged revision.

## Documentation links

A script (Python, read-only, not kept) checked every relative Markdown link and anchor in the 30 `.md` files outside `node_modules` and `target`: 2562 links, 0 broken. It checks only relative links, not external URLs.

## Open questions and assumptions for the client

The [register](../constraints.md) is the source. Items this exercise touches, none resolved by me:

| Item | What the exercise showed |
|---|---|
| [OQ-031](../constraints.md#open-questions) (new) | Zero-width and other invisible characters are accepted and kept ([A-037](../constraints.md#assumptions), new, covers only text the database cannot store) |
| [A-037](../constraints.md#assumptions) (new) | The team's review of the refusal rule from [Bug 2](../verification.md#bug-2--text-with-the-null-character-or-an-unpaired-surrogate-gave-a-server-error-or-was-changed) |
| [OQ-027](../constraints.md#open-questions) | No limit on failed sign-ins ([above](#failed-sign-ins-oq-027)) |
| [OQ-025](../constraints.md#open-questions), [A-020](../constraints.md#assumptions) | Several assignees and self-assignment: still the team's answer, not the client's |
| [A-030](../constraints.md#assumptions), [OQ-011](../constraints.md#open-questions) | Every Project Owner can search the display names of all people |
| [A-026](../constraints.md#assumptions) | The percentage is rounded; the exact rule is not the client's |
| [OQ-023](../constraints.md#open-questions) | Soft deletion: the rows are kept; nobody decided for how long |
| [A-034](../constraints.md#assumptions), [A-036](../constraints.md#assumptions) | The member and comment lists are not paged |
| [OQ-010](../constraints.md#open-questions) | NFR-004–NFR-007 cannot be verified |
| [OQ-013](../constraints.md#open-questions) | English only |
| [OQ-016](../constraints.md#open-questions) | The client has not approved the MVP |
