# ADR-005 — Authentication with signed access tokens (JWT)

- **Status:** Accepted (provisional)
- **Date:** 2026-10-29
- **Decided by:** the project team ([ADR-002](ADR-002-phase-scope-and-implementation-authorization.md), question 14). The AI assistant had recommended server-side sessions in a cookie instead.
- **Related:** [FR-013–FR-015](../requirements.md#functional-requirements); [US-013–US-015](../user-stories.md#stories); [EX-012](../user-stories.md#edge-cases-and-exceptions), [EX-014](../user-stories.md#edge-cases-and-exceptions); [NFR-001](../requirements.md#non-functional-requirements-and-unresolved-quality-criteria), [NFR-003](../requirements.md#non-functional-requirements-and-unresolved-quality-criteria); [Assumption A-014](../constraints.md#assumptions), [Assumption A-016](../constraints.md#assumptions), [Assumption A-017](../constraints.md#assumptions); [OQ-001](../constraints.md#open-questions), [OQ-011](../constraints.md#open-questions), [OQ-018](../constraints.md#open-questions), [OQ-027](../constraints.md#open-questions); [ADR-003](ADR-003-technology-stack.md), [ADR-004](ADR-004-rest-api-single-entry-point.md)

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

## Alternatives considered

The AI assistant recommended server-side sessions instead (question 14, option a). The session would be identified by an HttpOnly, SameSite cookie, with:

- a token against cross-site request forgery;
- an idle timeout set by the team;
- hashed passwords;
- demonstration passwords supplied through the environment.

The team did not choose this option and recorded no rationale.

## Open points

These belong to [OQ-027](../constraints.md#open-questions). Unless the client answers first, they are decided in increment I2. Items marked [Proposal] are the assistant's suggestions, not decisions.

- **Token lifetime.** How long a token lasts, and whether an idle timeout also applies.
- **How sign-out is enforced.** [Proposal] The server keeps the identifiers of signed-out tokens until those tokens expire.
- **Token storage in the User interface.** [Proposal] Keep the token in memory, so that reloading the page requires signing in again. Session storage is the alternative; it survives a reload but is more exposed to scripts.
- **Refresh tokens.** [Proposal] None: a person signs in again when the token expires.
- **Signing.** The signing algorithm and how the signing key is managed and replaced.
- **Passwords and failed attempts.** Password rules, and the response to repeated failed sign-in attempts.
