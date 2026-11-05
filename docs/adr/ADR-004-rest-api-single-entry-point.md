# ADR-004 — REST API as the single entry point

- **Status:** Accepted (provisional)
- **Date:** 2026-10-29; amended on 2026-10-30 (status 415 in decision 5; health check exception to decision 1, and text lengths, in [ADR-007](ADR-007-build-persistence-and-demonstration-data.md)); amended again on 2026-10-30 (decision 9, unsupported methods, by team decision in the conversation “I1 review: remaining decisions”); open point on the place of `405` resolved on 2026-11-02 (decision 5, by team decision in Lab Worksheet 03, question 12)
- **Decided by:** the project team ([ADR-002](ADR-002-phase-scope-and-implementation-authorization.md), question 4)
- **Related:** [NFR-001](../requirements.md#non-functional-requirements-and-unresolved-quality-criteria), [NFR-002](../requirements.md#non-functional-requirements-and-unresolved-quality-criteria); [Assumption A-001](../constraints.md#assumptions), [Assumption A-002](../constraints.md#assumptions), [Assumption A-022](../constraints.md#assumptions); [EX-003](../user-stories.md#edge-cases-and-exceptions), [EX-012](../user-stories.md#edge-cases-and-exceptions); [OQ-012](../constraints.md#open-questions), [OQ-019](../constraints.md#open-questions); [ADR-001](ADR-001-modular-monolith.md), [ADR-003](ADR-003-technology-stack.md), [ADR-005](ADR-005-authentication-with-jwt.md)

## Context

- [openapi.yaml](../../openapi.yaml) contained a REST contract for part of the MVP, drafted before any decision about the API.
- Permissions must be enforced consistently, both for listings and for direct attempts to view or change a project or task ([NFR-002](../requirements.md#non-functional-requirements-and-unresolved-quality-criteria)). The client has not decided how ([OQ-019](../constraints.md#open-questions)).
- The architecture requires every read or change to pass through Identity and then Access control ([module rule 3](../architecture.md#components-and-responsibilities)).

## Decision

1. **Single entry point.** The HTTP API described in [openapi.yaml](../../openapi.yaml) is the only way to read or change information. The User interface uses only this API. No server-rendered page or administration screen reads or changes data.

   *Exception added on 2026-10-30 by team decision ([ADR-007](ADR-007-build-persistence-and-demonstration-data.md), decision 7): a health check at `/actuator/health`, outside `/api` and outside openapi.yaml, reports only whether the application and its database are up. It reads and changes no product information.*
2. **Format and location.** The API exchanges JSON under the base path `/api`, served from the same origin as the User interface ([ADR-003](ADR-003-technology-stack.md)).
3. **Errors.** Errors are Problem Details ([RFC 9457](https://www.rfc-editor.org/rfc/rfc9457)), with the media type `application/problem+json`.
4. **Checks on every request.** Every operation except signing in requires a signed-in person ([ADR-005](ADR-005-authentication-with-jwt.md)). Access control decides on every request before Projects, Tasks, or Progress act on it.
5. **Status precedence.** When several errors apply, the first one in this order is returned:

   | Status | Meaning |
   |---|---|
   | 401 | The person is not signed in. |
   | 405 | The path does not define the request's method (decision 9). |
   | 404 | The project or task does not exist, is deleted, or the person is not a member. |
   | 403 | A member without permission for the action. |
   | 415 | The request content is not in the operation's media type. |
   | 400 | A malformed request, for example content that is not well-formed JSON. |
   | 422 | Invalid content, for example an empty name. |
   | 409 | A conflict with the current state, for example a Completed project. |

   *415 added on 2026-10-30 by team decision, chosen over the 400 that the 0.2.0-draft contract had proposed for this case; Spring Boot returns 415 by default ([ADR-003](ADR-003-technology-stack.md)). Its place, just before 400, is a proposal [Proposal]: the media type is checked before the content is read.*

   *405 placed on 2026-11-02 by team decision (Lab Worksheet 03, question 12), which resolved the open point “Place of `405` in the status order”. The token is checked before the method, so a request without a valid token gets 401 whatever its method. 405 reveals only the methods that the contract already publishes.*

   Signing in is the first documented exception: 415, then 400, then 422, then 401. Assigning a task is a second exception, decided on 2026-11-05 as the assistant's proposal, confirmed by the user's blanket confirmation in the conversation “Implement assignees and comments” (the team's review is pending). Whether a Project Participant may assign depends on the person named in the request ([Assumption A-020](../constraints.md#assumptions)), so the order is 401, 404, 415, 400, 422 for invalid content, 403, 422 for a person who is not a member, then 409.
6. **No disclosure to non-members.** Non-members receive 404, never 403, so the API discloses nothing about projects they cannot access ([EX-003](../user-stories.md#edge-cases-and-exceptions)). Deleted projects and tasks also return 404 ([Assumption A-022](../constraints.md#assumptions)).
7. **Partial changes.** Partial changes use `PATCH` with JSON Merge Patch ([RFC 7396](https://www.rfc-editor.org/rfc/rfc7396)).
8. **Versioning.** The contract stays a draft (`0.x`) until the client approves the MVP scope ([OQ-016](../constraints.md#open-questions)).
9. **Unsupported methods.** A request to a path of the contract with a method that the path does not define gets `405` as a Problem Details response, with an `Allow` header that lists the methods the path defines. This general rule is documented once, here and in the description of [openapi.yaml](../../openapi.yaml), because OpenAPI cannot attach a response to a method that a path does not define. *Added on 2026-10-30 by team decision (the conversation “I1 review: remaining decisions”, decision 3), which resolved the open point “Unsupported methods”.*

## Consequences

- **One enforcement path.** Every row of the [permission matrix](../requirements.md#permission-matrix-proposed) can be tested automatically through the API.
- **The contract is the boundary.** It separates the User interface from the server. API tests check responses against [openapi.yaml](../../openapi.yaml) from increment I1.
- **Future integrations.** Any integration ([OQ-012](../constraints.md#open-questions)) must use this API, or the same Identity and Access control.
- **Breaking change.** Version 0.2.0-draft replaced a task's single `assignee` with a list of `assignees` ([FR-010](../requirements.md#functional-requirements)).
- **Server checks are required.** The User interface may hide or disable controls a person cannot use, but that never replaces the check on the server.

## Alternatives considered

The alternative was a server-rendered User interface, with the REST API deferred (question 4, option b). The team did not choose it and recorded no rationale. The phase 1 analysis had noted that serving both server-rendered pages and an API would mean two entry points to keep consistent ([OQ-019](../constraints.md#open-questions)).

## Open points

- **Lengths and sizes.** Maximum lengths of names, titles, and comment text, and the maximum request size. These are part of [OQ-003](../constraints.md#open-questions) and [OQ-024](../constraints.md#open-questions). *Updated on 2026-10-30 by team decision ([ADR-007](ADR-007-build-persistence-and-demonstration-data.md), decision 8): the I1 schema checks only that text is nonempty; the maximum lengths are agreed in I3 and added by a new migration. The maximum request size is still open.* *Updated on 2026-11-02 by team decision (the conversation “Implement project management”, question 4): the project name has at most 100 characters and the project description at most 1000 ([Assumption A-031](../constraints.md#assumptions)), added by the migration `V4`; the limits of task titles and comment text remain open.* *Updated on 2026-11-05 by team decision (the conversation “Implement task management”, questions 2 to 4): the task title has 1 to 150 characters and the task description at most 2000 ([Assumption A-033](../constraints.md#assumptions)), added by the migration `V5`; text made only of whitespace, including no-break spaces, is empty. The limit of comment text remains open.*
- **Pagination.** Resolved on 2026-11-02 by team decision (the conversation “Implement project management”, questions 6 and 7), for the list of projects: pages by page number and size, default 20, maximum 100, ordered by name ignoring letter case and then by identifier, as [Assumption A-032](../constraints.md#assumptions) records; the contract has the response with the page and the total. Other lists, such as comments, are still returned whole until the team decides otherwise. *Extended on 2026-11-05 by team decision (the conversation “Implement task management”, questions 6 and 7) to the list of a project's tasks: the same pages, sizes, default, and total, ordered oldest first by the technical `creation_order`, which the API does not expose ([Assumption A-032](../constraints.md#assumptions)); the contract changes the `200` of `listTasks` from an array to a page.* *Previously: “Other lists, such as tasks and comments, are still returned whole until the team decides otherwise.”* *Previously: “Whether lists are paginated. No volumes are known ([OQ-010](../constraints.md#open-questions)).”*
- **Unsupported methods.** Resolved on 2026-10-30 by team decision (the conversation “I1 review: remaining decisions”, decision 3): decision 9. An unknown API path gets `404` for every method. *Previously: “An unknown API path gets `404` for every method, and a path of an existing operation called with a method it does not support gets `405`, which the contract does not define. Whether the contract documents `405` is to be decided by the team [Proposal].” That text was added on 2026-10-30 and corrected the same day after the I1 tests; before the correction, it said that methods other than GET on unknown API paths get `405`.*
- **Place of `405` in the status order.** Resolved on 2026-11-02 by team decision (Lab Worksheet 03, question 12): just after `401` (decision 5). For example, `GET /api/session` gets `401` without a valid token, and `405` with `Allow: DELETE, POST` with one. *Previously: “Decision 9 does not place `405` in the order of decision 5, for example before or after `401`. [Proposal] Decide it in I2, when signing in is added.” That text was added on 2026-10-30 with decision 9.*
