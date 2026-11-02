# ADR-009 — Identity from the token, and ended sessions in the User interface

- **Status:** Accepted (provisional)
- **Date:** 2026-11-02
- **Decided by:** the project team, in a working session with the AI assistant on 2026-11-02 (the conversation “Lab Worksheet 03 — access to the application”, questions 14–16). For each question, the AI assistant presented two or three options with their trade-offs and a recommendation [Proposal]. The team chose an option for every question and confirmed the summary. The AI assistant decided nothing.
- **Related:** [FR-013–FR-015](../requirements.md#functional-requirements); [US-013–US-015](../user-stories.md#stories); [EX-012](../user-stories.md#edge-cases-and-exceptions), [EX-014](../user-stories.md#edge-cases-and-exceptions); [NFR-001](../requirements.md#non-functional-requirements-and-unresolved-quality-criteria), [NFR-002](../requirements.md#non-functional-requirements-and-unresolved-quality-criteria); [Assumption A-016](../constraints.md#assumptions), [Assumption A-017](../constraints.md#assumptions), [Assumption A-028](../constraints.md#assumptions); [OQ-013](../constraints.md#open-questions), [OQ-027](../constraints.md#open-questions); [ADR-004](ADR-004-rest-api-single-entry-point.md), [ADR-005](ADR-005-authentication-with-jwt.md), [ADR-008](ADR-008-domain-driven-design.md); [implementation plan](../implementation-plan.md#i2--identity) (I2)

## Context

- [ADR-005](ADR-005-authentication-with-jwt.md) decided signed access tokens. Its open points were resolved on the same day: the token lasts 8 hours, is kept only in the User interface's memory, and is never refreshed. Signing out ends it on the server.
- The worksheet for increment I2 asks for three things:
  - the person's identity is taken from the authentication, never from the client;
  - every operation is protected;
  - the User interface sends the person to sign in when needed, and says when a session is no longer valid.
- The team limited the work to I2 (Lab Worksheet 03, question 1). The operations on projects, tasks, assignments, and comments are added from I3 to I11, together with the project's owner (I3) and the comment's author (I9).
- I2 therefore cannot test those operations. What it can do is make sure that each one is protected, and takes its identity only from the token, as soon as it is added.
- [openapi.yaml](../../openapi.yaml) already refuses unknown request properties with 422 ([ADR-004](ADR-004-rest-api-single-entry-point.md)). None of its request schemas has an owner or author field.

## Decision

### 1. The User interface when a session ends (question 14)

| Situation | What the User interface shows |
|---|---|
| The person has not signed in, or has reloaded the page | The sign-in screen, with no message. |
| The person signed out | The sign-in screen, with “You have signed out.” |
| The token expired, or the server answers 401 to a request made with it | The User interface discards the token and shows the sign-in screen, with “Your session has ended. Please sign in again.” |

- A timer, set from `expiresIn` when the person signs in, discards the token when it expires. The message is then shown without waiting for a request.
- When the page becomes visible again, the User interface checks the token with `GET /api/me`.
- After signing in again, the person sees My projects.
- The texts are in English ([Assumption A-028](../constraints.md#assumptions)). They are kept in [`frontend/src/messages.ts`](../../frontend/src/messages.ts).

### 2. The identity comes only from the token (question 15)

- The server takes the acting person from the `sub` claim of the verified token. From it, the server sets a project's owner (I3) and a comment's author (I9).
- No request schema has an owner or author field. A request body that sends one gets 422 with `errors`, as for any unknown property ([ADR-004](ADR-004-rest-api-single-entry-point.md)).
- Query parameters and headers never set the identity. For example, `GET /api/me?personId=…` returns the person in the token, whatever the parameter names.
- A request names a person with `personId` only to add a member (`AddParticipantRequest`) or to assign a task (`AssignTaskRequest`). That person is never taken as the acting person. Access control decides whether the acting person may make the change (I3, I6, I7).

### 3. Three guards for the operations still to come (question 16)

1. **Every operation requires a token.** A test reads every operation in openapi.yaml, except `signIn`, and sends it twice:
   - without a token, it expects 401, `WWW-Authenticate: Bearer`, and Problem Details;
   - with a signed-out token, it expects 401, `WWW-Authenticate: Bearer error="invalid_token"`, and Problem Details. This also covers [US-014](../user-stories.md#us-014--sign-out) criterion 2.

   Path parameters are fictitious UUIDs. The token is checked before anything else, so the test also covers operations that the backend does not have yet. An operation added later is covered from the start.
2. **Request schemas are reviewed.** A test checks every request schema in openapi.yaml:
   - it has `additionalProperties: false`;
   - it has exactly the properties of a reviewed list in the test;
   - `personId` appears only in `AddParticipantRequest` and `AssignTaskRequest`.

   A schema that changes fails the test until its list is reviewed.
3. **One way to learn the acting person.** A controller learns who is acting only through a parameter of type `SignedInPerson`. It is a record in Identity's API package, `com.matriz.identity`, that holds the person's `PersonId`. Identity's web layer resolves it from the verified token. An ArchUnit test fails the build when a class outside the Identity module uses Spring Security.

## Consequences

- **Authentication is in place before the operations.** Later increments add no authentication code. They take `SignedInPerson`, and the first guard covers them.
- **Authorization is still to come.** The guards check identity, not access. Whether a person may see or change a project, a task, an assignment, or a comment is decided by Access control. It starts in I3 and is tested in each increment through the permission matrix ([NFR-002](../requirements.md#non-functional-requirements-and-unresolved-quality-criteria); [implementation plan](../implementation-plan.md#i3--projects-and-access-control)).
- **Two people with different access.** In I2, they can be compared only through `GET /api/me` and signing out. For example, Ana's sign-out does not end Eva's token. That Eva gets 404 for Mobile App, a project of which she is not a member, can be verified only when `getProject` exists, in I3.
- **Deliberate friction.** Changing a request schema needs a reviewed change to the second guard. Adding a controller that reads the identity in another way fails the build.
- **Reloading signs the person out.** This follows from keeping the token in memory ([ADR-005](ADR-005-authentication-with-jwt.md)). The User interface shows no message in that case, because it cannot tell a reload from a first visit.
- **The contract is the source of the first guard.** An operation that is not in openapi.yaml is not in the guard's list. Every other path under `/api` still requires a token ([ADR-005](ADR-005-authentication-with-jwt.md#decision), decision 2), and [ADR-004](ADR-004-rest-api-single-entry-point.md) decision 1 makes openapi.yaml the only API.

## Alternatives considered

For each question, the AI assistant presented two or three options. The options not chosen were not recorded in detail, and no further rationale was recorded.

## Open points

- **Wording and language of the messages.** The texts are the team's. They wait for the client's answer on the product's language ([OQ-013](../constraints.md#open-questions); [Assumption A-028](../constraints.md#assumptions)).
