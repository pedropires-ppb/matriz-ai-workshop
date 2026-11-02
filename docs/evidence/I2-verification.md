# Matriz — Increment I2 verification record (2026-11-02)

**Status:** Evidence for the project team's review of increment [I2](../implementation-plan.md#i2--identity), which is Awaiting review. *Created on 2026-11-02 at the project team's request (Lab Worksheet 03, Step 4), in the conversation “Lab Worksheet 03 — access to the application”. The scope is I2 alone (question 1), so the record is named after I2. There is no push or CI run yet, so the status of I2 links to this record. The record is a snapshot and is not updated; after the push, the status can also link the commit and the CI run.*

**Environment:** macOS on arm64; JDK 25.0.5 (Temurin); Maven 3.9.16 through the wrapper; Node.js 24.22.2, installed by the build; Playwright 1.64.0 with Chromium; Docker 28.3.2; PostgreSQL 18.6, in the tests and in the demonstration runs; Spring Boot 4.1.2, Spring Framework 7.0.10, and Spring Security 7.1.2. The work is on the branch `week3-3`, from `main` at `8b44ad6`. Outputs are trimmed: `…` marks omitted text; the start of log lines (time, process, and thread), the `Date` header, and trailing spaces are removed. Every access token is replaced by `<access token>`. The demonstration passwords come from environment variables and never appear; `not-the-right-password` is a deliberately wrong, fictitious one.

## Summary

| # | Check | I2 acceptance criterion or verification | Result |
|---|---|---|---|
| 1 | [Build and automated tests](#1-build-and-automated-tests) | Every test, including the module boundary and layering tests; the API and end-to-end tests listed for I2 | Passed: 207 tests in 17 classes, 0 failures |
| 2 | [Exit criteria 1–6](#2-exit-criteria-16) | The plan's exit criteria for every increment | Met. The cases of criterion 2 that need a project do not apply to the operations of I2 |
| 3 | [Demonstration runs](#3-demonstration-runs) | Manual checks of the packaged application, with curl, outside the User interface | The setup of sections 4–9 |
| 4 | [Sign-in, the signed-in person, and sign-out](#4-sign-in-the-signed-in-person-and-sign-out) | Valid credentials; a wrong password and an unknown login name give identical responses; no token, and an altered, expired, or signed-out token, get `401` with `WWW-Authenticate: Bearer` and Problem Details; `GET /api/me` returns the person in the token; passwords are stored only as hashes | Passed |
| 5 | [Two people](#5-two-people) | Two demonstration people with different access, also with direct requests | Passed for the operations of I2. That Eva gets `404` for Mobile App needs the project operations of I3 |
| 6 | [The identity comes only from the token](#6-the-identity-comes-only-from-the-token) | An owner or author identifier sent by the client is refused (`422`), and a query parameter is ignored (question 15) | Passed for the operations of I2 and for the contract. The owner (I3) and the author (I9) are checked when their operations exist |
| 7 | [Order of the checks, and the operations still to come](#7-order-of-the-checks-and-the-operations-still-to-come) | `401` before `405`; every operation except `signIn` requires a token; the health check stays public | Passed |
| 8 | [Restart](#8-restart) | A signed-out token stays refused after a restart | Passed |
| 9 | [No secret in responses or logs](#9-no-secret-in-responses-or-logs) | No response or log contains a password, a password hash, or a token | Passed after a fix. A misuse probe found that a password or a token sent in the query string was logged at `DEBUG` and `TRACE`; the team decided the fix (question 32) |
| 10 | [Mutation checks](#10-mutation-checks) | The new tests fail when the behaviour they check is broken | Passed: 4 changes detected, then reverted |
| 11 | [End to end](#11-end-to-end) | A person who has not signed in is sent to sign in; signing in shows the display name; after signing out, a protected screen asks to sign in again; a session that is no longer valid shows the agreed message | Passed: 10 end-to-end tests |
| 12 | [Secrets and personal data in the repository](#12-secrets-and-personal-data-in-the-repository) | No secret or real personal data in the repository; `.env` ignored by git | Passed locally, with one search inconclusive; GitHub's secret scanning not run |
| 13 | [Not yet done, and still open](#13-not-yet-done-and-still-open) | Continuous integration on push; points for the team | Open |

## 1. Build and automated tests

The build ran at commit `b1442df`; the commit that adds this record changes documents only.

```text
$ ./mvnw -B clean verify
…
[INFO] Running 'npm ci' in …/frontend
…
[INFO] Running 'npm run typecheck' in …/frontend
…
[INFO] > tsc --noEmit -p tsconfig.json
…
[INFO] Running 'npm run build' in …/frontend
…
[INFO] > vite build
…
[INFO] ✓ built in 60ms
…
[INFO] Tests run: 10, Failures: 0, Errors: 0, Skipped: 0, Time elapsed: 40.24 s -- in com.matriz.UserInterfaceTests
[INFO] Tests run: 3, Failures: 0, Errors: 0, Skipped: 0, Time elapsed: 0.516 s -- in com.matriz.PersonRepositoryTests
[INFO] Tests run: 16, Failures: 0, Errors: 0, Skipped: 0, Time elapsed: 1.364 s -- in com.matriz.ProblemDetailsTests
[INFO] Tests run: 2, Failures: 0, Errors: 0, Skipped: 0, Time elapsed: 0.006 s -- in com.matriz.MigrationNamingTests
[INFO] Tests run: 14, Failures: 0, Errors: 0, Skipped: 0, Time elapsed: 0.127 s -- in com.matriz.SchemaMigrationTests
[INFO] Tests run: 7, Failures: 0, Errors: 0, Skipped: 0, Time elapsed: 4.825 s -- in com.matriz.DemoDataTests
[INFO] Tests run: 13, Failures: 0, Errors: 0, Skipped: 0, Time elapsed: 0.074 s -- in com.matriz.RequestSchemaTests
[INFO] Tests run: 8, Failures: 0, Errors: 0, Skipped: 0, Time elapsed: 0.192 s -- in com.matriz.EnvironmentVariablesTests
[INFO] Tests run: 27, Failures: 0, Errors: 0, Skipped: 0, Time elapsed: 3.274 s -- in com.matriz.SignInTests
[INFO] Tests run: 47, Failures: 0, Errors: 0, Skipped: 0, Time elapsed: 7.002 s -- in com.matriz.AccessTokenTests
[INFO] Tests run: 1, Failures: 0, Errors: 0, Skipped: 0, Time elapsed: 7.302 s -- in com.matriz.SignOutAfterRestartTests
[INFO] Tests run: 45, Failures: 0, Errors: 0, Skipped: 0, Time elapsed: 0.238 s -- in com.matriz.EveryOperationRequiresATokenTests
[INFO] Tests run: 4, Failures: 0, Errors: 0, Skipped: 0, Time elapsed: 0.801 s -- in com.matriz.ModuleBoundaryTests
[INFO] Tests run: 2, Failures: 0, Errors: 0, Skipped: 0, Time elapsed: 0.610 s -- in com.matriz.LogSecrecyTests
[INFO] Tests run: 2, Failures: 0, Errors: 0, Skipped: 0, Time elapsed: 0.029 s -- in com.matriz.HealthCheckTests
[INFO] Tests run: 1, Failures: 0, Errors: 0, Skipped: 0, Time elapsed: 0.522 s -- in com.matriz.TokenCheckFailureTests
[INFO] Tests run: 5, Failures: 0, Errors: 0, Skipped: 0, Time elapsed: 0.180 s -- in com.matriz.LayeringTests
…
[INFO] Tests run: 207, Failures: 0, Errors: 0, Skipped: 0
…
[INFO] BUILD SUCCESS
…
[INFO] Total time:  01:18 min
[INFO] Finished at: 2026-11-02T12:15:16Z
```

- Maven downloaded nothing: every dependency was already in the local Maven repository.
- The test classes added or changed for I2:

| Test class | What it checks |
|---|---|
| [SignInTests](../../src/test/java/com/matriz/SignInTests.java) (27, new) | Valid credentials give a token for 8 hours, with only the agreed header and claims, and the person. Each sign-in issues a new token. The login name ignores letter case. A wrong password, an unknown login name, and a password longer than 72 bytes get identical responses. Another media type gets `415`; absent content, or content that is not well-formed JSON, gets `400`; content other than a login name and a password gets `422`. An `Authorization` header sent with the request is ignored, even with a signed-out token. |
| [AccessTokenTests](../../src/test/java/com/matriz/AccessTokenTests.java) (47, new) | `GET /api/me` returns the person in the token, whatever identifier the client sends. Two people each see themselves and sign out separately. Signing out ends only the presented token, which is refused from the next request on, and forgets the ended tokens that have expired. A signed-out token cannot sign out again. No token, another scheme, a token in the query string, and a malformed header are refused. A token made as Matriz makes them is accepted; 17 forged tokens and a token altered to name another person are refused, and a refused sign-out changes nothing. A valid token of a person who does not exist is refused. Unsupported methods get `405`, and `401` comes before `405`. |
| [EveryOperationRequiresATokenTests](../../src/test/java/com/matriz/EveryOperationRequiresATokenTests.java) (45, new) | The first guard of [ADR-009](../adr/ADR-009-identity-from-the-token.md), decision 3. Driven by openapi.yaml: `signIn` is the only operation without a token, and each of the other 22 is refused without a token and with a signed-out token. |
| [RequestSchemaTests](../../src/test/java/com/matriz/RequestSchemaTests.java) (13, new) | The second guard. Every request content in openapi.yaml is a reviewed schema, with `additionalProperties: false` and exactly its reviewed properties. Only adding a participant and assigning a task take a person in their content. Every parameter is reviewed, and a person appears in a path only to remove a participant or unassign a task. |
| [ModuleBoundaryTests](../../src/test/java/com/matriz/ModuleBoundaryTests.java) (4; 2 new) | The third guard: only Identity uses Spring Security, except password hashing (question 31), and the rule refuses any other use. |
| [LogSecrecyTests](../../src/test/java/com/matriz/LogSecrecyTests.java) (2, new) | With the Spring Security, Spring Web, and Matriz loggers at `TRACE`, signing in, failed and malformed sign-ins, using a token, and signing out log no password, password hash, token, or signing key; nor does a password or a token sent in the query string. The three loggers that write a request's address stay at `INFO` (question 32). |
| [SignOutAfterRestartTests](../../src/test/java/com/matriz/SignOutAfterRestartTests.java) (1, new) | A signed-out token stays refused after a restart, and a new signing key refuses every earlier token. |
| [TokenCheckFailureTests](../../src/test/java/com/matriz/TokenCheckFailureTests.java) (1, new) | A token that cannot be checked, because the store of ended tokens fails, is not accepted: `500` with Problem Details that hide the cause. |
| [PersonRepositoryTests](../../src/test/java/com/matriz/PersonRepositoryTests.java) (3, new) | The Person repository ([ADR-008](../adr/ADR-008-domain-driven-design.md#open-points)): a stored person is read back with every field in a new persistence context; each field is stored in its column; unknown values find nobody. |
| [EnvironmentVariablesTests](../../src/test/java/com/matriz/EnvironmentVariablesTests.java) (8; 3 new, 1 renamed) | Start-up stops when the signing key is missing, is the placeholder of `.env.example`, or is not a Base64 key of at least 256 bits. The demonstration passwords must follow the password rules, only when the demonstration data is on. |
| [DemoDataTests](../../src/test/java/com/matriz/DemoDataTests.java) (7; 1 new) | A demonstration password that breaks the password rules stops start-up before the database is used. |
| [SchemaMigrationTests](../../src/test/java/com/matriz/SchemaMigrationTests.java) (14; 1 new, 2 renamed) | Migrations `V2` and `V3`: an ended access token is recorded once, with its expiry; login names are unique and in lower case. |
| [ProblemDetailsTests](../../src/test/java/com/matriz/ProblemDetailsTests.java) (16; 2 replaced) | An unknown path under `/api` first needs a token (`401`), and is not found (`404`) once the token is accepted. |
| [UserInterfaceTests](../../src/test/java/com/matriz/UserInterfaceTests.java) (10; 9 new) | End to end, in Chromium: see [section 11](#11-end-to-end). |

- `HealthCheckTests`, `LayeringTests`, and `MigrationNamingTests` are unchanged.
- The log's 11 `ERROR` lines come from tests that cause failures on purpose:
  - 8 refused start-ups: 5 for missing or invalid environment variables, one of them a demonstration password that breaks the rules, and 3 for demonstration data that does not match the database;
  - 1 failed demonstration migration, in `DemoDataTests`;
  - 2 `Unexpected error`: the `500` of `ProblemDetailsTests` and of `TokenCheckFailureTests`.
- Its 16 `WARN` lines are 8 start-ups cancelled by those failures, and 8 `Request method '…' is not supported` lines from the `405` tests, which name only the method.
- Two earlier runs of the build each had one more `ERROR` line, Tomcat's “Socket accept failed”, logged while a test's server shut down. It did not occur in this run.
- During the implementation, the tests found three defects, all fixed before these checks: a key thumbprint in the token's header, a defaulted `iat` claim, and a password sent without quotes that a `DEBUG` log line repeated.

## 2. Exit criteria 1–6

| Exit criterion of the [plan](../implementation-plan.md#exit-criteria-for-every-increment-proposal) | How I2 meets it |
|---|---|
| 1. The acceptance criteria pass as automated API tests, and the main flow end to end | [US-013](../user-stories.md#us-013--sign-in): `SignInTests` and the end-to-end tests; showing the person's projects is completed in I5, as the plan says. [US-014](../user-stories.md#us-014--sign-out): `AccessTokenTests`, the first guard with a signed-out token on every operation (criterion 2), and the end-to-end tests. [US-015](../user-stories.md#us-015--see-who-is-signed-in): “Signed in as Ana Silva” in the end-to-end tests; My projects is completed in I5. [EX-012](../user-stories.md#edge-cases-and-exceptions): the first guard, on 22 operations, and `ProblemDetailsTests`, on unknown paths. [EX-014](../user-stories.md#edge-cases-and-exceptions): `SignInTests` and the end-to-end tests. The main flow: [section 11](#11-end-to-end). |
| 2. Every new operation is tested for the people the permission matrix distinguishes, and nothing changes | The permission matrix has no rows for I2's operations. `signOut` and `getCurrentPerson`: a person who is not signed in gets `401`, and a refused sign-out changes nothing: the ended tokens are compared before and after. `signIn` needs no sign-in. The `404`, `403`, and `409` cases do not apply: no operation of I2 is about a project. |
| 3. Responses match openapi.yaml, and errors are Problem Details | `Contract.assertSatisfies` checks responses against the `SignInResponse`, `PersonSummary`, and `ValidationProblem` schemas of openapi.yaml, and `Contract.assertProblem` checks errors against its `Problem` schema. `TestAccess.assertNoToken` and `assertInvalidToken` check `WWW-Authenticate`. |
| 4. The module boundary test and the layering test pass | `ModuleBoundaryTests` (4 tests) and `LayeringTests` (5) pass in [section 1](#1-build-and-automated-tests). |
| 5. No secret, password, or real personal data is committed or logged | Sections [9](#9-no-secret-in-responses-or-logs) and [12](#12-secrets-and-personal-data-in-the-repository). |
| 6. The documentation is updated, and any new assumption is registered first | [openapi.yaml](../../openapi.yaml) and the [README](../../README.md#sign-in-use-the-api-and-sign-out) document the token's life cycle. [ADR-003](../adr/ADR-003-technology-stack.md), [ADR-004](../adr/ADR-004-rest-api-single-entry-point.md), [ADR-005](../adr/ADR-005-authentication-with-jwt.md), [ADR-007](../adr/ADR-007-build-persistence-and-demonstration-data.md), and [ADR-008](../adr/ADR-008-domain-driven-design.md) record the decisions in place, flagged; [ADR-009](../adr/ADR-009-identity-from-the-token.md) is new. The [interim answers](../constraints.md#interim-team-answers-2026-10-29) record the decisions on OQ-027, and the [data model](../database.md) records `V2` and `V3`. No new assumption: the work relies on [A-016](../constraints.md#assumptions), [A-017](../constraints.md#assumptions), and [A-028](../constraints.md#assumptions). |

## 3. Demonstration runs

The manual checks used the packaged application, `target/matriz-0.1.0-SNAPSHOT.jar`, on port 18080, with a new PostgreSQL 18.6 database in a container of its own, separate from any other local database. The demonstration data was on. A signing key, a database password, and the four demonstration passwords were generated for these runs, kept outside the repository, and passed as environment variables.

| Run | Built at | Logging | Sections |
|---|---|---|---|
| 1 | `4bb8654` | The default, `INFO` | 4–7 |
| 2 | `4bb8654`, restarted with the same database and key | `TRACE` for `org.springframework.security`, `org.springframework.web`, `org.springframework.http`, and `com.matriz` | 8, 9.1, 9.2 |
| 3 | `de7f041`, with the fix of question 32 | `TRACE`, as in run 2 | 9.3 |
| 4 | `de7f041` | The default, `INFO` | 9.4 |

- The people are the fictitious demonstration people ([Assumption A-016](../constraints.md#assumptions)): Ana Silva (`ana`), Rui Martins (`rui`), Eva Santos (`eva`), and Daniel Rocha (`daniel`).
- `$B` is `http://localhost:18080`; `$ANA_ID` and `$EVA_ID` are Ana's and Eva's identifiers in the demonstration data.
- `sign_in LOGIN PASSWORD` signs in as the README does, sending the content to curl's standard input, and prints the token.
- `jwt.py` is a short script that uses only Python's standard library:
  - `show` prints a token's header and claims;
  - `signature` alters the signature;
  - `subject ID` changes the `sub` claim and keeps the signature;
  - `none ID` makes an unsigned token, with `alg` `none`;
  - `make SUB IAT EXP` signs a token with the run's key, with `iat` and `exp` in seconds from now.
- `psql_` runs psql in the database's container; `tokens/` keeps tokens between sections, outside the repository.
- The four responses of section 4.1 and the first response of section 4.2 were captured with all their headers. Every other response was captured with only its status line, `WWW-Authenticate`, `Content-Type`, `Allow`, and body.

## 4. Sign-in, the signed-in person, and sign-out

### 4.1 The README's flow, as Ana

The commands are those of the [README](../../README.md#sign-in-use-the-api-and-sign-out), except that `$B` replaces `http://localhost:8080`, and the password comes from the environment instead of `read -rs`.

```text
$ PASSWORD=$MATRIZ_DEMO_PASSWORD_ANA
$ TOKEN=$(printf '{"loginName":"ana","password":"%s"}' "$PASSWORD" |
  curl -s $B/api/session -H 'Content-Type: application/json' --data-binary @- |
  sed -n 's/.*"accessToken":"\([^"]*\)".*/\1/p')
$ python3 jwt.py show <<< "$TOKEN"
{"typ":"JWT","alg":"HS256"} {"sub":"10000000-0000-4000-8000-000000000001","exp":1793648608,"iat":1793619808,"jti":"b638fe39-ad3b-48a3-bf59-9473be017b11"}

$ curl -s $B/api/me -H "Authorization: Bearer $TOKEN"
{"personId":"10000000-0000-4000-8000-000000000001","displayName":"Ana Silva"}
$ curl -s -o /dev/null -w '%{http_code}\n' -X DELETE $B/api/session -H "Authorization: Bearer $TOKEN"
204

$ curl -si $B/api/me -H "Authorization: Bearer $TOKEN"
HTTP/1.1 401
WWW-Authenticate: Bearer error="invalid_token"
X-Content-Type-Options: nosniff
X-XSS-Protection: 0
Cache-Control: no-cache, no-store, max-age=0, must-revalidate
Pragma: no-cache
Expires: 0
X-Frame-Options: DENY
Content-Type: application/problem+json
Transfer-Encoding: chunked

{"detail":"The access token is invalid, expired, or no longer accepted.","instance":"/api/me","status":401,"title":"Unauthorized"}
$ curl -si $B/api/me
HTTP/1.1 401
WWW-Authenticate: Bearer
…
Content-Type: application/problem+json

{"detail":"Identity has not been established.","instance":"/api/me","status":401,"title":"Unauthorized"}
$ curl -si $B/api/session -H 'Content-Type: application/json' -d '{"loginName":"ana","password":"not-the-right-password"}'
HTTP/1.1 401
WWW-Authenticate: Bearer
…
Content-Type: application/problem+json

{"detail":"The login name or password is incorrect.","instance":"/api/session","status":401,"title":"Unauthorized"}
$ curl -si $B/api/session -H 'Content-Type: application/json' -d '{"loginName":"nobody","password":"not-the-right-password"}'
HTTP/1.1 401
WWW-Authenticate: Bearer
…
Content-Type: application/problem+json

{"detail":"The login name or password is incorrect.","instance":"/api/session","status":401,"title":"Unauthorized"}
$ unset PASSWORD TOKEN
```

- The token's header and claims are those decided in [ADR-005](../adr/ADR-005-authentication-with-jwt.md#open-points): `exp` − `iat` is 28,800 seconds, 8 hours.
- Without a token, `WWW-Authenticate` has no error code; with a token that is refused, it has `error="invalid_token"` ([RFC 6750](https://www.rfc-editor.org/rfc/rfc6750#section-3)).

### 4.2 Valid credentials, and letter case in the login name

```text
$ printf '{"loginName":"ana","password":"%s"}' "$MATRIZ_DEMO_PASSWORD_ANA" | curl -si $B/api/session -H 'Content-Type: application/json' --data-binary @-
HTTP/1.1 200
Cache-Control: no-store
…
Content-Type: application/json

{"accessToken":"<access token>","tokenType":"Bearer","expiresIn":28800,"person":{"personId":"10000000-0000-4000-8000-000000000001","displayName":"Ana Silva"}}
$ printf '{"loginName":"ANA","password":"%s"}' "$MATRIZ_DEMO_PASSWORD_ANA" | curl -si $B/api/session -H 'Content-Type: application/json' --data-binary @-
HTTP/1.1 200
Content-Type: application/json

{"accessToken":"<access token>","tokenType":"Bearer","expiresIn":28800,"person":{"personId":"10000000-0000-4000-8000-000000000001","displayName":"Ana Silva"}}
```

The sign-in response is not cached (`Cache-Control: no-store`), and `ANA` signs in as `ana` (question 10).

### 4.3 A wrong password and an unknown login name give identical responses

```text
$ curl -si $B/api/session -H 'Content-Type: application/json' -d '{"loginName":"ana","password":"not-the-right-password"}' | tr -d '\r' | grep -v '^Date:' > wrong-password.http
$ curl -si $B/api/session -H 'Content-Type: application/json' -d '{"loginName":"nobody","password":"not-the-right-password"}' | tr -d '\r' | grep -v '^Date:' > unknown-login.http
$ printf '{"loginName":"eva","password":"%s"}' "$MATRIZ_DEMO_PASSWORD_ANA" | curl -si $B/api/session -H 'Content-Type: application/json' --data-binary @- | tr -d '\r' | grep -v '^Date:' > another-persons-password.http
$ diff wrong-password.http unknown-login.http && diff wrong-password.http another-persons-password.http && echo "identical: status, every header except Date, and body"
identical: status, every header except Date, and body

# Indicative timing only: the median of 10 sign-ins each (seconds).
$ for login in ana nobody; do printf '%s ' $login; for i in $(seq 10); do curl -s -o /dev/null -w '%{time_total}\n' $B/api/session -H 'Content-Type: application/json' -d "{\"loginName\":\"$login\",\"password\":\"not-the-right-password\"}"; done | sort -n | sed -n '5,6p' | paste -sd' ' - | awk '{printf "%.3f\n", ($1+$2)/2}'; done
ana 0.074
nobody 0.066
```

- The third response is Eva's login name with Ana's password.
- The timing is indicative only: the median of ten sign-ins each, on one machine. An unknown login name is also compared with a BCrypt hash (question 9).

### 4.4 Refused tokens

```text
$ TOKEN_ANA=$(sign_in ana "$MATRIZ_DEMO_PASSWORD_ANA")
$ curl -si -X DELETE $B/api/session
HTTP/1.1 401
WWW-Authenticate: Bearer
Content-Type: application/problem+json

{"detail":"Identity has not been established.","instance":"/api/session","status":401,"title":"Unauthorized"}

$ curl -si $B/api/me -H "Authorization: Bearer $(python3 jwt.py signature <<< "$TOKEN_ANA")"
HTTP/1.1 401
WWW-Authenticate: Bearer error="invalid_token"
Content-Type: application/problem+json

{"detail":"The access token is invalid, expired, or no longer accepted.","instance":"/api/me","status":401,"title":"Unauthorized"}

$ curl -si $B/api/me -H "Authorization: Bearer $(python3 jwt.py subject $EVA_ID <<< "$TOKEN_ANA")"
HTTP/1.1 401
WWW-Authenticate: Bearer error="invalid_token"
Content-Type: application/problem+json

{"detail":"The access token is invalid, expired, or no longer accepted.","instance":"/api/me","status":401,"title":"Unauthorized"}

$ curl -si $B/api/me -H "Authorization: Bearer $(python3 jwt.py none $EVA_ID <<< "$TOKEN_ANA")"
HTTP/1.1 401
WWW-Authenticate: Bearer error="invalid_token"
Content-Type: application/problem+json

{"detail":"The access token is invalid, expired, or no longer accepted.","instance":"/api/me","status":401,"title":"Unauthorized"}

$ curl -si $B/api/me -H "Authorization: Bearer not-a-token"
HTTP/1.1 401
WWW-Authenticate: Bearer error="invalid_token"
Content-Type: application/problem+json

{"detail":"The access token is invalid, expired, or no longer accepted.","instance":"/api/me","status":401,"title":"Unauthorized"}

$ curl -si $B/api/me -H "Authorization: Basic YW5hOnNlY3JldA=="
HTTP/1.1 401
WWW-Authenticate: Bearer
Content-Type: application/problem+json

{"detail":"Identity has not been established.","instance":"/api/me","status":401,"title":"Unauthorized"}

# A token signed with Matriz's key, issued 8 h 1 min ago and expired 1 min ago:
$ curl -si $B/api/me -H "Authorization: Bearer $(python3 jwt.py make $ANA_ID -28860 -60)"
HTTP/1.1 401
WWW-Authenticate: Bearer error="invalid_token"
Content-Type: application/problem+json

{"detail":"The access token is invalid, expired, or no longer accepted.","instance":"/api/me","status":401,"title":"Unauthorized"}

# Control: the same script, with an expiry 7 h 59 min ahead, is accepted, so only the expiry differs:
$ curl -si $B/api/me -H "Authorization: Bearer $(python3 jwt.py make $ANA_ID -60 28740)"
HTTP/1.1 200
Content-Type: application/json

{"personId":"10000000-0000-4000-8000-000000000001","displayName":"Ana Silva"}
```

- The `Basic` credentials are a fictitious pair, `ana:secret`.
- The expired token and the control differ only in their times, so the `401` comes from the expiry.

### 4.5 Passwords are stored only as hashes

```text
$ psql_ -c "SELECT login_name, display_name, substr(password_hash, 1, 15) || '...' AS hash_start, length(password_hash) AS hash_length FROM person ORDER BY login_name"
 login_name | display_name |     hash_start     | hash_length
------------+--------------+--------------------+-------------
 ana        | Ana Silva    | {bcrypt}$2a$10$... |          68
 daniel     | Daniel Rocha | {bcrypt}$2a$10$... |          68
 eva        | Eva Santos   | {bcrypt}$2a$10$... |          68
 rui        | Rui Martins  | {bcrypt}$2a$10$... |          68
(4 rows)

$ psql_ -c "SELECT count(*) AS ended_tokens, min(expires_at) > now() AS all_still_unexpired FROM ended_access_token"
 ended_tokens | all_still_unexpired
--------------+---------------------
            2 | t
(1 row)

$ printf %s "$TOKEN_ANA" > tokens/ana-2.token
```

- Each password is stored as a hash made by Spring Security's delegating password encoder, which uses BCrypt. The query shows only the first 15 characters, which name the algorithm and its cost.
- These queries ran at the end of run 1, after sections 5–7. The two ended tokens are those signed out in sections 4.1 and 5; each is kept until it expires.

## 5. Two people

```text
$ TOKEN_EVA=$(sign_in eva "$MATRIZ_DEMO_PASSWORD_EVA")
$ curl -s $B/api/me -H "Authorization: Bearer $TOKEN_ANA"
{"personId":"10000000-0000-4000-8000-000000000001","displayName":"Ana Silva"}
$ curl -s $B/api/me -H "Authorization: Bearer $TOKEN_EVA"
{"personId":"10000000-0000-4000-8000-000000000003","displayName":"Eva Santos"}
$ curl -s -o /dev/null -w '%{http_code}\n' -X DELETE $B/api/session -H "Authorization: Bearer $TOKEN_ANA"
204

$ curl -si $B/api/me -H "Authorization: Bearer $TOKEN_ANA"
HTTP/1.1 401
WWW-Authenticate: Bearer error="invalid_token"
Content-Type: application/problem+json

{"detail":"The access token is invalid, expired, or no longer accepted.","instance":"/api/me","status":401,"title":"Unauthorized"}

$ curl -si -X DELETE $B/api/session -H "Authorization: Bearer $TOKEN_ANA"
HTTP/1.1 401
WWW-Authenticate: Bearer error="invalid_token"
Content-Type: application/problem+json

{"detail":"The access token is invalid, expired, or no longer accepted.","instance":"/api/session","status":401,"title":"Unauthorized"}

$ curl -s $B/api/me -H "Authorization: Bearer $TOKEN_EVA"
{"personId":"10000000-0000-4000-8000-000000000003","displayName":"Eva Santos"}
$ printf %s "$TOKEN_ANA" > tokens/ana-ended.token; printf %s "$TOKEN_EVA" > tokens/eva.token
```

- As [ADR-009](../adr/ADR-009-identity-from-the-token.md#consequences) foresees, two people can be compared in I2 only through `GET /api/me` and signing out: each sees only themselves, and Ana's sign-out does not end Eva's session.
- The worksheet's example, Eva getting `404` for Mobile App ([ADR-004](../adr/ADR-004-rest-api-single-entry-point.md)), needs `getProject`, which comes in I3. In the demonstration data, Mobile App has Rui as Project Owner and Ana as Project Participant, and Eva is not a member. Until I3, `GET /api/projects` gets `404` for every signed-in person ([section 7](#7-order-of-the-checks-and-the-operations-still-to-come)).
- The same check runs end to end, in two browser contexts ([section 11](#11-end-to-end)).

## 6. The identity comes only from the token

```text
$ TOKEN_ANA=$(sign_in ana "$MATRIZ_DEMO_PASSWORD_ANA")
$ curl -s "$B/api/me?personId=$EVA_ID" -H "Authorization: Bearer $TOKEN_ANA"
{"personId":"10000000-0000-4000-8000-000000000001","displayName":"Ana Silva"}
$ printf '{"loginName":"ana","password":"%s","personId":"%s"}' "$MATRIZ_DEMO_PASSWORD_ANA" "$EVA_ID" | curl -si $B/api/session -H 'Content-Type: application/json' --data-binary @-
HTTP/1.1 422
Content-Type: application/problem+json

{"detail":"Only a login name and a password can be sent.","instance":"/api/session","status":422,"title":"Unprocessable Content","errors":[{"pointer":"/personId","detail":"This property is not allowed."}]}

$ printf '{"loginName":"ana","password":"%s","ownerId":"%s"}' "$MATRIZ_DEMO_PASSWORD_ANA" "$EVA_ID" | curl -si $B/api/session -H 'Content-Type: application/json' --data-binary @-
HTTP/1.1 422
Content-Type: application/problem+json

{"detail":"Only a login name and a password can be sent.","instance":"/api/session","status":422,"title":"Unprocessable Content","errors":[{"pointer":"/ownerId","detail":"This property is not allowed."}]}
```

- As decided (question 15), a property that names an owner or an author is refused with `422`, as any unknown property, and a query parameter such as `personId` is ignored.
- No request schema in openapi.yaml has an owner or author property. The second guard, `RequestSchemaTests`, fails if one is added without review.
- The owner (`createProject`, I3) and the author (`addTaskComment`, I9) are checked when their operations exist. Today, `createProject` with an `ownerId` gets `401` without a token ([section 7](#7-order-of-the-checks-and-the-operations-still-to-come)).

## 7. Order of the checks, and the operations still to come

```text
$ curl -si $B/api/session
HTTP/1.1 401
WWW-Authenticate: Bearer
Content-Type: application/problem+json

{"detail":"Identity has not been established.","instance":"/api/session","status":401,"title":"Unauthorized"}

$ curl -si $B/api/session -H "Authorization: Bearer $TOKEN_ANA"
HTTP/1.1 405
Allow: POST, DELETE
Content-Type: application/problem+json

{"detail":"Method 'GET' is not supported.","instance":"/api/session","status":405,"title":"Method Not Allowed"}

$ curl -si -X PUT $B/api/me
HTTP/1.1 401
WWW-Authenticate: Bearer
Content-Type: application/problem+json

{"detail":"Identity has not been established.","instance":"/api/me","status":401,"title":"Unauthorized"}

$ curl -si -X PUT $B/api/me -H "Authorization: Bearer $TOKEN_ANA"
HTTP/1.1 405
Allow: GET
Content-Type: application/problem+json

{"detail":"Method 'PUT' is not supported.","instance":"/api/me","status":405,"title":"Method Not Allowed"}
```

```text
$ curl -si $B/api/projects
HTTP/1.1 401
WWW-Authenticate: Bearer
Content-Type: application/problem+json

{"detail":"Identity has not been established.","instance":"/api/projects","status":401,"title":"Unauthorized"}

$ curl -si -X POST $B/api/projects -H 'Content-Type: application/json' -d "{\"name\":\"Fictitious\",\"ownerId\":\"$EVA_ID\"}"
HTTP/1.1 401
WWW-Authenticate: Bearer
Content-Type: application/problem+json

{"detail":"Identity has not been established.","instance":"/api/projects","status":401,"title":"Unauthorized"}

$ curl -si $B/api/projects -H "Authorization: Bearer $TOKEN_ANA"
HTTP/1.1 404
Content-Type: application/problem+json

{"detail":"The requested resource does not exist.","instance":"/api/projects","status":404,"title":"Not Found"}

$ curl -si $B/actuator/health
HTTP/1.1 200
Content-Type: application/vnd.spring-boot.actuator.v3+json

{"components":{"db":{"status":"UP"},"diskSpace":{"status":"UP"},"livenessState":{"status":"UP"},"ping":{"status":"UP"},"readinessState":{"status":"UP"},"ssl":{"status":"UP"}},"groups":["liveness","readiness"],"status":"UP"}

$ curl -si $B/actuator/env
HTTP/1.1 404
Content-Type: application/problem+json

{"detail":"The requested resource does not exist.","instance":"/actuator/env","status":404,"title":"Not Found"}
```

- `401` comes before `405` (question 12).
- Every operation of a later increment already requires a token: without one it gets `401`; with one, `404` until it is implemented.
- The health check stays public (question 11), with each component's status and no details; the other Actuator endpoints are not exposed.

## 8. Restart

Run 2 is a new process, started on run 1's database with the same signing key.

```text
$ grep -cE "Started MatrizApplication" app-run2.log
1

# Ana's token, ended by signing out before the restart:
$ curl -si $B/api/me -H "Authorization: Bearer $(cat tokens/ana-ended.token)"
HTTP/1.1 401
WWW-Authenticate: Bearer error="invalid_token"
Content-Type: application/problem+json

{"detail":"The access token is invalid, expired, or no longer accepted.","instance":"/api/me","status":401,"title":"Unauthorized"}

# Eva's and Ana's other tokens, issued before the restart and never ended:
$ curl -s $B/api/me -H "Authorization: Bearer $(cat tokens/eva.token)"
{"personId":"10000000-0000-4000-8000-000000000003","displayName":"Eva Santos"}
$ curl -s $B/api/me -H "Authorization: Bearer $(cat tokens/ana-2.token)"
{"personId":"10000000-0000-4000-8000-000000000001","displayName":"Ana Silva"}
```

`SignOutAfterRestartTests` checks the same automatically, and also that a new signing key refuses every earlier token.

## 9. No secret in responses or logs

### 9.1 Sign-ins, mistakes, and sign-out at TRACE

Rui signs in and uses his token. Then come two malformed sign-ins, two in other media types, and one with his password for Daniel's login name. Finally he signs out, and his ended token is refused.

```text
$ TOKEN_RUI=$(sign_in rui "$MATRIZ_DEMO_PASSWORD_RUI")
$ curl -s $B/api/me -H "Authorization: Bearer $TOKEN_RUI"
{"personId":"10000000-0000-4000-8000-000000000002","displayName":"Rui Martins"}
$ printf '{"loginName":"rui","password":"%s"' "$MATRIZ_DEMO_PASSWORD_RUI" | curl -s -o /dev/null -w '%{http_code}\n' $B/api/session -H 'Content-Type: application/json' --data-binary @-
400

$ printf '{"loginName":"rui","password":%s}' "$MATRIZ_DEMO_PASSWORD_RUI" | curl -s -o /dev/null -w '%{http_code}\n' $B/api/session -H 'Content-Type: application/json' --data-binary @-
400

$ printf 'loginName=rui&password=%s' "$MATRIZ_DEMO_PASSWORD_RUI" | curl -s -o /dev/null -w '%{http_code}\n' $B/api/session -H 'Content-Type: application/x-www-form-urlencoded' --data-binary @-
415

$ printf '{"loginName":"rui","password":"%s"}' "$MATRIZ_DEMO_PASSWORD_RUI" | curl -s -o /dev/null -w '%{http_code}\n' $B/api/session -H 'Content-Type: text/plain' --data-binary @-
415

$ printf '{"loginName":"daniel","password":"%s"}' "$MATRIZ_DEMO_PASSWORD_RUI" | curl -s -o /dev/null -w '%{http_code}\n' $B/api/session -H 'Content-Type: application/json' --data-binary @-
401

$ curl -s -o /dev/null -w '%{http_code}\n' -X DELETE $B/api/session -H "Authorization: Bearer $TOKEN_RUI"
204

$ curl -s -o /dev/null -w '%{http_code}\n' $B/api/me -H "Authorization: Bearer $TOKEN_RUI"
401

$ printf %s "$TOKEN_RUI" > tokens/rui-ended.token
$ grep -cE " (DEBUG|TRACE) " app-run2.log
387
```

Up to this point, run 2's log had 387 lines at `DEBUG` or `TRACE`. The scan in [9.5](#95-scan-of-the-logs-and-transcripts) found no secret in run 2's log before the probes of 9.2.

### 9.2 A misuse that leaked: secrets in the query string

The contract never asks for a password or a token in the query string, and Matriz does not read them there.

```text
$ BEFORE=$(wc -l < app-run2.log)
$ curl -s -o /dev/null -w '%{http_code}\n' "$B/api/session?loginName=rui&password=$MATRIZ_DEMO_PASSWORD_RUI" -X POST -H 'Content-Type: application/json' -d '{"loginName":"rui","password":"not-the-right-password"}'
401

$ curl -s -o /dev/null -w '%{http_code}\n' "$B/api/me?access_token=$(cat tokens/eva.token)"
401

$ tail -n +$((BEFORE + 1)) app-run2.log > app-run2-probes.log
```

At `DEBUG` and `TRACE`, three loggers wrote the request's address, including its query string. Spring masks the parameters and headers, but not the query string. These are the lines, with the values replaced and the start of each line removed:

```text
DEBUG o.s.security.web.FilterChainProxy: Securing POST /api/session?loginName=rui&password=<password>
TRACE estMatcherDelegatingAuthorizationManager: Authorizing POST /api/session?loginName=rui&password=<password>
TRACE estMatcherDelegatingAuthorizationManager: Checking authorization on POST /api/session?loginName=rui&password=<password> using org.springframework.security.authorization.SingleResultAuthorizati…
DEBUG o.s.security.web.FilterChainProxy: Secured POST /api/session?loginName=rui&password=<password>
TRACE o.s.web.servlet.DispatcherServlet: POST "/api/session?loginName=rui&password=<password>", parameters={masked}, headers={masked} in DispatcherServlet 'dispatcherServlet'
DEBUG o.s.security.web.FilterChainProxy: Securing GET /api/me?access_token=<access token>
TRACE estMatcherDelegatingAuthorizationManager: Authorizing GET /api/me?access_token=<access token>
TRACE estMatcherDelegatingAuthorizationManager: Checking authorization on GET /api/me?access_token=<access token> using org.springframework.security.authorization.AuthenticatedAuthorizationManager@5…
```

The project team decided to keep these three loggers at `INFO` (question 32; [ADR-005](../adr/ADR-005-authentication-with-jwt.md), Consequences). The fix, in commit `de7f041`, is in [application.properties](../../src/main/resources/application.properties):

```properties
logging.level.org.springframework.security.web.FilterChainProxy=INFO
logging.level.org.springframework.security.web.access.intercept.RequestMatcherDelegatingAuthorizationManager=INFO
logging.level.org.springframework.web.servlet.DispatcherServlet=INFO
```

`LogSecrecyTests` checks these three levels, and that the misuse leaves no secret in the logs.

### 9.3 After the fix, at TRACE

```text
$ BEFORE=$(wc -l < app-run3.log)
$ curl -s -o /dev/null -w '%{http_code}\n' "$B/api/session?loginName=rui&password=$MATRIZ_DEMO_PASSWORD_RUI" -X POST -H 'Content-Type: application/json' -d '{"loginName":"rui","password":"not-the-right-password"}'
401

$ curl -s -o /dev/null -w '%{http_code}\n' "$B/api/me?access_token=$(cat tokens/eva.token)"
401

$ curl -s -o /dev/null -w '%{http_code}\n' "$B/actuator/health?access_token=$(cat tokens/eva.token)"
200

$ sleep 1; tail -n +$((BEFORE + 1)) app-run3.log > app-run3-probes.log
$ wc -l < app-run3-probes.log
     128

$ grep -cE 'DEBUG|TRACE' app-run3-probes.log
30
```

The scan in [9.5](#95-scan-of-the-logs-and-transcripts) found no secret in these 128 lines, 30 of them at `DEBUG` or `TRACE`. The same probes, with all their headers shown:

```text
$ curl -si "$B/api/session?loginName=rui&password=$MATRIZ_DEMO_PASSWORD_RUI" -X POST -H 'Content-Type: application/json' -d '{"loginName":"rui","password":"not-the-right-password"}'
HTTP/1.1 401
WWW-Authenticate: Bearer
Content-Type: application/problem+json

{"detail":"The login name or password is incorrect.","instance":"/api/session","status":401,"title":"Unauthorized"}

$ curl -si "$B/api/me?access_token=$(cat tokens/eva.token)"
HTTP/1.1 401
WWW-Authenticate: Bearer
Content-Type: application/problem+json

{"detail":"Identity has not been established.","instance":"/api/me","status":401,"title":"Unauthorized"}
```

The responses do not repeat the query string: `instance` is the path alone.

### 9.4 At the default level

```text
$ BEFORE=$(wc -l < app-run4.log)
$ curl -s -o /dev/null -w '%{http_code}\n' "$B/api/session?loginName=rui&password=$MATRIZ_DEMO_PASSWORD_RUI" -X POST -H 'Content-Type: application/json' -d '{"loginName":"rui","password":"not-the-right-password"}'
401

$ curl -s -o /dev/null -w '%{http_code}\n' "$B/api/me?access_token=$(cat tokens/eva.token)"
401

$ curl -s -o /dev/null -w '%{http_code}\n' "$B/actuator/health?access_token=$(cat tokens/eva.token)"
200

$ sleep 1; tail -n +$((BEFORE + 1)) app-run4.log > app-run4-probes.log
$ wc -l < app-run4-probes.log
       3

$ grep -cE 'DEBUG|TRACE' app-run4-probes.log
0
```

### 9.5 Scan of the logs and transcripts

Each log and transcript of the runs was searched for the four demonstration passwords, the signing key, and the database password of the runs; for `{bcrypt}`, `$2a$`, and text shaped like a token; and for the tokens saved during the runs.

| File | Lines | Found |
|---|---|---|
| Run 1's log | 66 | Nothing |
| Run 2's log | 771 | Rui's password on 5 lines and tokens on 3, all in the probes of 9.2, before the fix |
| Run 3's log | 323 | Nothing |
| Run 4's log | 57 | Nothing |
| The transcripts of sections 4–9 | 418 | Only the four hash prefixes, `{bcrypt}$2a$10$`, of section 4.5 |
| The excerpt of 9.2 | 8 | Nothing |

## 10. Mutation checks

Each change below was made by hand, the matching tests were run, and the change was then reverted.

| # | Change | Tests run | Result |
|---|---|---|---|
| 1 | The line that keeps `DispatcherServlet` at `INFO` removed from application.properties | `LogSecrecyTests` | Failed: “expected: INFO but was: TRACE”. The output of this run was not kept. |
| 2 | The User interface's expiry timer, in [App.tsx](../../frontend/src/App.tsx), made to fire after the expiry | `UserInterfaceTests.anExpiredSessionShowsTheMessageWithoutWaitingForARequest` | Failed at line 228: “Locator expected to have text”, expected “Your session has ended. Please sign in again.” |
| 3 | The same timer made to fire before the expiry | The same | Failed at line 223: “Locator expected to be visible”, for the My projects heading 1 ms before the expiry |
| 4 | `AccessTokenValidator.validate` made to record a new ended token each time it runs | The 19 tests of `AccessTokenTests` that compare the ended tokens before and after a refused sign-out: `withoutATokenThereIsNoCurrentPersonAndNoSessionToEnd`, `aSignedOutTokenCannotSignOutAgain`, and the 17 forged tokens | 11 failed with “[a refused sign-out changes nothing]”: `aSignedOutTokenCannotSignOutAgain`, and the 10 forged tokens whose signature is valid, so that the validator runs (forgeries 4, 5, and 8–15). The other 8 are refused before the validator runs. |

After changes 2 and 3 were reverted, the 10 end-to-end tests passed. The final build of [section 1](#1-build-and-automated-tests) ran after all the reverts.

## 11. End to end

`UserInterfaceTests` drives the User interface, as Matriz serves it, in Chromium, against a PostgreSQL database of its own. Each test adds its own fictitious people. Each test also checks that the page had no error, and logged no console error other than those it expects.

| Check | Test | What it asserts |
|---|---|---|
| A person who has not signed in is sent to sign in | `aPersonWhoHasNotSignedInIsSentToSignIn` | The sign-in screen, with no message and no API request. |
| Sign in, and see the display name | `signingInShowsTheDisplayNameAndSigningOutAsksToSignInAgain` | “Signed in as Ana Silva” and the My projects heading. Nothing in `localStorage`, `sessionStorage`, or cookies: the token is kept in memory only (question 4). |
| Sign out, and a protected screen asks to sign in again | The same test | The `DELETE` gets `204`; “You have signed out.” and the sign-in screen; the ended token then gets `401` from a direct request. |
| A session that is no longer valid shows the agreed message | `aSessionEndedElsewhereShowsTheMessageWhenThePageIsVisibleAgain` | The session is ended by a direct request. When the tab becomes visible again, its check gets `401`, and the tab shows “Your session has ended. Please sign in again.” and the sign-in screen. |
| | `anExpiredSessionShowsTheMessageWithoutWaitingForARequest` | With the page's clock under the test's control: still signed in 1 ms before the 8 hours end; at 8 hours, the same message and the sign-in screen, without any request. |
| Two people | `twoPeopleEachSeeThemselvesAndOneSigningOutLeavesTheOtherSignedIn` | Ana and Eva, in two browser contexts, each see themselves; Ana signs out, and Eva's tab stays signed in. |
| Other behaviour | `reloadingThePageSignsThePersonOutWithoutAMessage`, `incorrectCredentialsGetOneMessageThatDoesNotSayWhichIsWrong`, `anEmptyLoginNameOrPasswordIsRefusedWithoutARequest`, `whenMatrizCannotBeReachedThePersonIsToldSo`, `theApplicationServesTheUserInterfaceShell` | Reloading signs the person out, with no message. Incorrect credentials get one message, and the password field is cleared. Empty fields send no request. “Matriz could not be reached. Please try again.” The page's title and heading. |

## 12. Secrets and personal data in the repository

```text
$ git check-ignore -v .env
.gitignore:30:.env	.env
$ git ls-files .env
$
```

The 140 files that a commit would contain, tracked and untracked but not ignored, including this record, were searched, and so was the branch's history, `git log -p 8b44ad6..HEAD`, with the commit messages:

| Search | Files | History |
|---|---|---|
| The signing key, the database password, and the four demonstration passwords of the runs | 0 | 0 |
| The tokens issued in the runs | 0 | 0 |
| The four demonstration passwords of the local `.env` | 0 | 0 |
| The database password of the local `.env` | Inconclusive, for a reason reported to the team outside this record | Inconclusive |
| Private keys, text shaped like a token, and complete BCrypt hashes | 0 | 0 |
| The hosts of the private package registries configured on the machine | 0 | 0 |
| Paths in local home directories | 0 | 0 |
| E-mail addresses | 0 | Only `users.noreply.github.com`, in the commits' Co-authored-by trailers |
| The account name of the person who ran the checks | 1: the plan's I1 row, whose links to GitHub, on `main` already, contain the repository's owner | No added line |

- The local `.env` is ignored by git, and this branch did not change the database password of `.env.example`.
- The only people named are the fictitious people of the user stories and of the demonstration data.
- All 48 packages in `frontend/package-lock.json` resolve from `registry.npmjs.org`; the branch did not change it.
- The branch adds two Maven dependencies: `spring-boot-starter-security-oauth2-resource-server` ([ADR-003](../adr/ADR-003-technology-stack.md), question 7), and, for the tests only, `jackson-dataformat-yaml`, which reads openapi.yaml.
- GitHub's secret scanning was not run.

## 13. Not yet done, and still open

**Not yet done**

- **Continuous integration.** [ci.yml](../../.github/workflows/ci.yml) runs only after the push, which the project team does.
- **GitHub's secret scanning** of the repository, after the push.
- **The project team's review of I2** (exit criterion 7).

**For the project team.** Nothing below has been decided; each point is a [Proposal] for the team.

1. **Details of the User interface that no decision names.** The end-to-end tests check them:
   - a sign-in answered with a status other than `200`, `401`, or `422` shows “Matriz could not be reached. Please try again.”;
   - the password field is cleared after an incorrect attempt, and a message is hidden when the form is sent again;
   - empty fields send no request;
   - signing out waits for the answer to its request.

   The sign-in screen's texts also need the team's confirmation: “The login name or password is incorrect.”, “A login name and a password are required.”, and “Matriz could not be reached. Please try again.”
2. **Token checks.** [ADR-005](../adr/ADR-005-authentication-with-jwt.md#open-points) says a token has exactly four claims. Matriz issues exactly these, and refuses a token that lacks one of them. It accepts a token with an additional claim, and a token without a `typ` header; a token of type `at+jwt` is refused. Making any of these needs the signing key. The team may refuse them, or read “exactly” as what Matriz issues.
3. **Logging.** Setting `org.hibernate.orm.jdbc.extract` to `TRACE` would log the values read from the database, including the password hash. Keeping the three loggers of 9.2 at `INFO` removes their trace of each request when debugging.
4. **A failure while a token is checked** gives `500`, not `401` (`TokenCheckFailureTests`). The token is not accepted, and the cause is hidden.
5. **Failed sign-ins are not limited** (question 9). Guessing is slowed only by the password rules and by the time each comparison takes.
6. **The second guard also reviews every parameter** of the contract, and the paths that name a person. [ADR-009](../adr/ADR-009-identity-from-the-token.md), decision 3, asks only for the request schemas. The team may keep this or drop it.
7. **The README's curl examples** assume that the password has no `"` or `\`. While curl runs, the token is visible in the list of processes.

**Waiting for others**

- The client: [OQ-027](../constraints.md#open-questions), since the sign-in and session policy is the team's interim answer, and [OQ-013](../constraints.md#open-questions), since the messages are in English ([Assumption A-028](../constraints.md#assumptions)).
- Later increments (question 1): Eva gets `404` for Mobile App, and an owner field gets `422`, in I3; an author field gets `422` in I9.

The logs, transcripts, tokens, and secrets of the runs were kept outside the repository and deleted after this record was written, and the database container was stopped.
