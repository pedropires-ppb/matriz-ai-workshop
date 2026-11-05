# ADR-005 — Authentication with signed access tokens (JWT)

- **Status:** Accepted (provisional)
- **Date:** 2026-10-29; open points resolved on 2026-11-02 by team decision (Lab Worksheet 03, questions 2–9 and 17); a consequence added the same day (question 32); points of the I2 review added the same day (the conversation “I2 review and acceptance”, questions 8–11)
- **Decided by:** the project team ([ADR-002](ADR-002-phase-scope-and-implementation-authorization.md), question 14). The AI assistant had recommended server-side sessions in a cookie instead.
- **Related:** [FR-013–FR-015](../requirements.md#functional-requirements); [US-013–US-015](../user-stories.md#stories); [EX-012](../user-stories.md#edge-cases-and-exceptions), [EX-014](../user-stories.md#edge-cases-and-exceptions); [NFR-001](../requirements.md#non-functional-requirements-and-unresolved-quality-criteria), [NFR-003](../requirements.md#non-functional-requirements-and-unresolved-quality-criteria); [Assumption A-014](../constraints.md#assumptions), [Assumption A-016](../constraints.md#assumptions), [Assumption A-017](../constraints.md#assumptions); [OQ-001](../constraints.md#open-questions), [OQ-011](../constraints.md#open-questions), [OQ-018](../constraints.md#open-questions), [OQ-027](../constraints.md#open-questions); [ADR-003](ADR-003-technology-stack.md), [ADR-004](ADR-004-rest-api-single-entry-point.md); [ADR-007](ADR-007-build-persistence-and-demonstration-data.md), [ADR-009](ADR-009-identity-from-the-token.md) (added on 2026-11-02)

## Context

- [NFR-001](../requirements.md#non-functional-requirements-and-unresolved-quality-criteria) requires a person's identity to be established before any project or task information is disclosed or changed. The client has not decided how ([OQ-018](../constraints.md#open-questions)).
- The workshop scope asks for sign-in, sign-out, and the authenticated user, with seeded demonstration accounts; registration and password recovery are optional. The team adopted prepared accounts ([Assumption A-016](../constraints.md#assumptions)) with a login name, a display name, and a password hash ([Assumption A-017](../constraints.md#assumptions)).
- The User interface is a single-page application served from the same origin as the API ([ADR-003](ADR-003-technology-stack.md), [ADR-004](ADR-004-rest-api-single-entry-point.md)).

## Decision

1. **Sign-in** ([FR-013](../requirements.md#functional-requirements)). A person signs in with their login name and password (`POST /api/session`).
   - If both are correct, the API returns a signed access token: a JSON Web Token ([RFC 7519](https://www.rfc-editor.org/rfc/rfc7519)) that identifies the person and has an expiry time.
   - Otherwise, the API returns 401 with the same generic message, whichever was wrong ([EX-014](../user-stories.md#edge-cases-and-exceptions)).
2. **Every other request.** It carries the token in the `Authorization: Bearer` header ([RFC 6750](https://www.rfc-editor.org/rfc/rfc6750)).
   - The server verifies the token's signature and expiry on every request.
   - A missing, invalid, expired, or revoked token gets 401 with a `WWW-Authenticate: Bearer` header ([EX-012](../user-stories.md#edge-cases-and-exceptions)).
3. **Sign-out** ([FR-014](../requirements.md#functional-requirements)). Signing out (`DELETE /api/session`) makes the token unusable for any later request, even before it expires.
4. **The signed-in person** ([FR-015](../requirements.md#functional-requirements)). `GET /api/me` returns the signed-in person's identifier and display name.
5. **Passwords.** They are stored only as hashes made with an adaptive password-hashing function, such as BCrypt or Argon2. They are never logged or returned.
6. **Demonstration accounts.** They contain fictitious data only. Their passwords are supplied when the application is deployed or started, and are never committed to the repository ([security and privacy](../constraints.md#security-and-privacy)).
7. **Not provided:** public registration and password recovery ([Assumption A-016](../constraints.md#assumptions)).

## Consequences

- **Sign-out needs server-side state.** To meet [FR-014](../requirements.md#functional-requirements), the server must keep some state for signed-out tokens. This removes part of the benefit of stateless tokens.
- **Reduced forgery risk, higher script exposure.** Browsers do not attach a header automatically, so cross-site request forgery does not apply to the API. In exchange, the User interface must keep the token where its scripts can read it. A cross-site scripting flaw could therefore expose the token until it expires.
- **The signing key is a secret.** It is supplied at deployment and never committed. Replacing it signs everyone out.
- **Standard challenge.** 401 responses carry the standard `WWW-Authenticate: Bearer` challenge, which the cookie alternative lacked.
- **No limit on password guessing.** *Added on 2026-11-02 by team decision (Lab Worksheet 03, question 9).* Nothing slows down repeated failed sign-ins. The project team accepts this risk while only fictitious demonstration accounts exist ([Assumption A-016](../constraints.md#assumptions)). It must be revisited before Matriz is used with real people ([OQ-001](../constraints.md#open-questions), [OQ-011](../constraints.md#open-questions)). *Added on 2026-11-02 by team decision (the conversation “I2 review and acceptance”, question 11): the risk is kept, and reviewed in I12 ([implementation plan](../implementation-plan.md#i12--release-verification)) and with the client ([OQ-027](../constraints.md#open-questions)).*
- **A failure while a token is checked gives `500`.** *Added on 2026-11-02 by team decision (the conversation “I2 review and acceptance”, question 10).* If the list of ended tokens cannot be read, for example because the database is down, the token is not accepted and the response is `500` with Problem Details that hide the cause (`TokenCheckFailureTests`). The failure is the server's, so a `401` would send the person to sign in again for nothing.
- **A logger that must stay off.** *Added on 2026-11-02 by team decision (the conversation “I2 review and acceptance”, question 9).* Setting `org.hibernate.orm.jdbc.extract` to `TRACE` would log the values read from the database, including a password hash. It must never be set in a shared environment.
- **Signing in again.** *Added on 2026-11-02 by team decision (Lab Worksheet 03, questions 2, 4, and 5).* The token is kept only in the User interface's memory, and there is no refresh token. A person therefore signs in again after 8 hours, after reloading the page, and in each new tab.
- **Stored identifiers of ended tokens.** *Added on 2026-11-02 by the same decision (question 3).* The database keeps the identifier and expiry of each token ended by signing out, but no token, until the token expires. Each request that carries a token reads this list once.
- **Request addresses kept out of the logs.** *Added on 2026-11-02 by team decision (Lab Worksheet 03, question 32, asked during the verification of I2).*
  - The API never reads a password or a token from the query string. Even so, three framework loggers write each request's address, including its query string, at DEBUG or TRACE: Spring Security's `FilterChainProxy` and `RequestMatcherDelegatingAuthorizationManager`, and Spring MVC's `DispatcherServlet`. If a client wrongly put a secret in the query string, it would be logged at those levels.
  - `application.properties` keeps these three loggers at INFO, whatever the level of their parents. `LogSecrecyTests` checks this (decision 5).
  - As a result, raising the level of `org.springframework.security` or `org.springframework.web` no longer logs each request's address. Setting one of these loggers by name brings the address back, and the risk with it.

## Alternatives considered

The AI assistant recommended server-side sessions instead (question 14, option a). The session would be identified by an HttpOnly, SameSite cookie, with:

- a token against cross-site request forgery;
- an idle timeout set by the team;
- hashed passwords;
- demonstration passwords supplied through the environment.

The team did not choose this option and recorded no rationale.

## Open points

These belong to [OQ-027](../constraints.md#open-questions). Unless the client answers first, they are decided in increment I2. Items marked [Proposal] are the assistant's suggestions, not decisions.

*Resolved on 2026-11-02 by team decision (Lab Worksheet 03, questions 2–9 and 17), before increment I2 started. Each point keeps its previous text. They remain the team's provisional answers: [OQ-027](../constraints.md#open-questions) stays open, and the client's answer prevails.*

- **Token lifetime.** Resolved (question 2):
  - A token lasts 8 hours from sign-in, so `expiresIn` is 28800 seconds. No idle timeout applies.
  - The lifetime is set in `application.properties` (`matriz.security.token-lifetime`), because it is not a secret; tests may shorten it.
  - A token is refused from its expiry time on, with no clock tolerance: one server both signs and verifies the tokens. Spring Security's default tolerance of 60 seconds is set to zero.

  *Previously: “How long a token lasts, and whether an idle timeout also applies.”*
- **How sign-out is enforced.** Resolved (question 3):
  - Signing out adds the token's identifier (its `jti` claim) and its expiry time to a list of ended tokens in the database, the table `ended_access_token` created by the migration `V2`.
  - Every request that carries a token checks the list, so an ended token stays refused after a restart.
  - Each sign-out also deletes the rows of tokens that have already expired.
  - The list is technical data of Identity, not an aggregate ([ADR-008](ADR-008-domain-driven-design.md)).

  *Previously: “[Proposal] The server keeps the identifiers of signed-out tokens until those tokens expire.”*
- **Token storage in the User interface.** Resolved (question 4): in memory only. Reloading the page, or opening a new tab, requires signing in again. What the User interface does when a session ends is decided in [ADR-009](ADR-009-identity-from-the-token.md). *Previously: “[Proposal] Keep the token in memory, so that reloading the page requires signing in again. Session storage is the alternative; it survives a reload but is more exposed to scripts.”*
- **Refresh tokens.** Resolved (question 5): none. A person signs in again when the token expires or the page is reloaded; the contract has no renewal operation. *Previously: “[Proposal] None: a person signs in again when the token expires.”*
- **Signing.** Resolved (questions 6, 7, and 17):
  - **Algorithm.** HS256 (HMAC with SHA-256), with one key. Only HS256 is accepted.
  - **Claims.** A token has exactly `sub` (the person's identifier), `jti` (a random UUID), `iat`, and `exp`, and all four are required. It carries no login name, display name, issuer, or audience. *Clarified on 2026-11-02 by team decision (the conversation “I2 review and acceptance”, question 8): “exactly” describes the tokens that Matriz issues. Matriz refuses a token that lacks one of the four claims, but accepts a token with an additional claim and a token without a `typ` header, and refuses a token of type `at+jwt`. Making such a token needs the signing key.*
  - **Key.** A random key of at least 256 bits, Base64-encoded, in the environment variable `MATRIZ_JWT_SIGNING_KEY`, which is required at every start-up ([ADR-007](ADR-007-build-persistence-and-demonstration-data.md), decision 6). Start-up stops if the key is missing, blank, not Base64, or shorter than 256 bits once decoded. The message names the variable and how to generate a key, never the value.
  - **Replacing the key.** Change the variable and restart. Every token signed with the old key is then refused, so everyone is signed out (see Consequences).
  - **Example file.** `.env.example` holds a placeholder that start-up rejects, so a copy of the example cannot run with a published key. Tests generate a random key at each run.
  - **Library.** Spring Security's OAuth2 resource server support, through `spring-boot-starter-security-oauth2-resource-server`, which uses Nimbus JOSE+JWT. Spring Boot manages its version. This is flagged in [ADR-003](ADR-003-technology-stack.md).

  *Previously: “The signing algorithm and how the signing key is managed and replaced.”*
- **Passwords and failed attempts.** Resolved (questions 8 and 9):
  - **Password rules.** There is no registration and no password change, so the rules apply to the demonstration passwords ([ADR-007](ADR-007-build-persistence-and-demonstration-data.md), decision 10). Each password must have at least 15 characters, the minimum that NIST SP 800-63B-4 sets for a password used on its own, and at most 72 bytes in UTF-8, BCrypt's limit. The rules are checked at start-up while `MATRIZ_DEMO_DATA=true`. Start-up stops with a message naming the variable and the rule, never the value. No list of common passwords is checked.
  - **Signing in.** Any nonempty password is accepted for checking. A wrong one, including one longer than 72 bytes, gets the 401 of decision 1.
  - **Hashing.** Hashing is unchanged from I1: Spring Security's delegating password encoder, which uses BCrypt and stores the prefix `{bcrypt}` (decision 5).
  - **Failed attempts.** No limit applies; the risk is accepted (see Consequences). An unknown login name is still compared with a dummy BCrypt hash, so the response time does not reveal whether the login name exists.

  *Previously: “Password rules, and the response to repeated failed sign-in attempts.”*
