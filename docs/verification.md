# Matriz — Verification of the nine scenarios (exercise “Verify the MVP”, 2026-11-12)

**Status:** In progress. Part of increment [I12](implementation-plan.md#i12--release-verification), which is Under way. The rest of I12 is in the [I12 verification record](evidence/I12-verification.md). Nothing here is a client approval ([OQ-016](constraints.md#open-questions)).

**Decisions of the project team's member for this exercise** (the conversation “Verify the MVP”): the scope is the nine scenarios plus the rest of I12; the evidence is text only, with every secret masked and fictitious names only; the automated tests use their own databases, and the manual checks use a fresh database, not the developer's local one; a known problem is recorded as a known limitation and not fixed; a defect is fixed only after the diagnosis is approved.

## Summary

| Case | Scenario | Type | Status |
|---|---|---|---|
| 01 | Access to the application | Automated and manual | Confirmed |
| 02 | Project management | — | Not run |
| 03 | Task persistence | — | Not run |
| 04 | Data validation | — | Not run |
| 05 | State and priority | — | Not run |
| 06 | Assignees | — | Not run |
| 07 | Comments | — | Not run |
| 08 | Authorization in the API | — | Not run |
| 09 | Deletion and errors | — | Not run |

## Environment and conventions

| Item | Value |
|---|---|
| Machine | macOS, arm64; Docker 28.3.2; Node.js 24.8.0 (the build uses its own pinned Node.js) |
| JDK | Temurin 25.0.3, selected with `JAVA_HOME` for each command (the shell's default is JDK 21, which the build refuses) |
| Automated tests | Their own PostgreSQL containers (Testcontainers), with no demonstration data |
| Manual checks | The jar of the baseline build, on port 18080, against a fresh PostgreSQL 18.6 from a separate Compose project (`docker compose -p matriz-ex8`, its own volume, started empty). The developer's local volume was not touched. The four demonstration people, with passwords and a signing key generated for the run, kept outside the repository and not recorded here |
| Masking | Tokens, passwords, hashes, and keys are never written here; `<masked>` marks where one was |

**Baseline.** `JAVA_HOME=<JDK 25> ./mvnw -B clean verify` ended with `BUILD SUCCESS` (exit 0, 3 min 25 s): **626 backend tests, 0 failures, 0 errors, 0 skipped**, and **23 front-end unit tests** (Vitest, 5 files), including `UserInterfaceTests` (43, Playwright, Chromium). The raw log is outside the repository.

**Types.** *Automated* means a test in the build that fails if the behaviour breaks. *Manual* means a command or a step that I ran once; it proves the behaviour of that run only.

## Case 01 — Access to the application

- **Expected:** valid credentials let a person in; invalid credentials and requests without authentication are handled ([US-013](user-stories.md#us-013--sign-in), [US-014](user-stories.md#us-014--sign-out), [US-015](user-stories.md#us-015--see-who-is-signed-in), [EX-012](user-stories.md#edge-cases-and-exceptions), [EX-014](user-stories.md#edge-cases-and-exceptions), [NFR-001](requirements.md#non-functional-requirements-and-unresolved-quality-criteria)).
- **Type:** automated and manual. **Status: Confirmed.**
- **Automated evidence** (baseline build, all passed): `SignInTests` (27), `AccessTokenTests` (47), `EveryOperationRequiresATokenTests` (45: every operation gets 401 with no token and with a signed-out token), `ProblemDetailsTests` (16), `TokenCheckFailureTests` (1), `SignOutAfterRestartTests` (1), and in `UserInterfaceTests` the sign-in, incorrect-credentials, empty-fields, reload, expired-session and two-people tests.
- **Manual evidence** (fresh database, port 18080; secrets masked):

| Step | Request | Observed |
|---|---|---|
| 1 | `POST /api/session` as `ana` with her password | `200`, `application/json`; `tokenType` Bearer, `expiresIn` 28800, person “Ana Silva”; token `<masked>` |
| 2 | `GET /api/me` with that token | `200`, `{"personId":"10000000-0000-4000-8000-000000000001","displayName":"Ana Silva"}` |
| 3 | The same sign-in with the login name `ANA` | `200` (letter case does not matter) |
| 4 | Sign-in with `ana` and a wrong password | `401`, `{"detail":"The login name or password is incorrect.","instance":"/api/session","status":401,"title":"Unauthorized"}` |
| 5 | Sign-in with an unknown login name | `401`; the status, every header except `Date`, and the body are identical to step 4 (`diff` empty) |
| 6 | `GET /api/projects` with no token | `401`, `application/problem+json`, `WWW-Authenticate: Bearer`, “Identity has not been established.” |
| 7 | `GET /api/me` with `Bearer abc.def.ghi` | `401` |
| 8 | `GET /api/me` with a token whose last character was changed | `401` |
| 9 | `DELETE /api/session`, then `GET /api/me` with the same token | `204`, then `401` “The access token is invalid, expired, or no longer accepted.” |
| 10 | Sign-in with an empty login name and password | `422` |

- **Not verified here:** the absence of any limit on failed sign-ins is a recorded limitation ([OQ-027](constraints.md#open-questions)); it is reviewed in the [I12 record](evidence/I12-verification.md). A failure while a token is checked gives `500`, not `401` (`TokenCheckFailureTests`; [I2 record](evidence/I2-verification.md#13-not-yet-done-and-still-open), point 4); recorded as a known limitation, not fixed. The expiry after 8 hours and the effect of a new signing key are checked by `AccessTokenTests` and `SignOutAfterRestartTests`, not by hand.
