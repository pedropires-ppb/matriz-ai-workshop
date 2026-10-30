# ADR-003 — Technology stack

- **Status:** Accepted (provisional)
- **Date:** 2026-10-29; amended on 2026-10-30 (build tool)
- **Decided by:** the project team ([ADR-002](ADR-002-phase-scope-and-implementation-authorization.md), question 3). The team chose among three options that the AI assistant presented at the team's request.
- **Related:** [ADR-001](ADR-001-modular-monolith.md), [ADR-004](ADR-004-rest-api-single-entry-point.md), [ADR-005](ADR-005-authentication-with-jwt.md), [ADR-006](ADR-006-simultaneous-changes.md); [NFR-004–NFR-007](../requirements.md#non-functional-requirements-and-unresolved-quality-criteria); [OQ-010](../constraints.md#open-questions), [OQ-020](../constraints.md#open-questions), [OQ-021](../constraints.md#open-questions)

## Context

- [ADR-001](ADR-001-modular-monolith.md) requires one deployable application whose module boundaries are kept and, ideally, verified automatically.
- [AGENTS.md](../../AGENTS.md#agents-must-not) forbids choosing technologies without an approved decision. No stack had been chosen, so the team asked for two or three simple options compatible with the modular monolith and chose one.
- Several things are unknown: targets for response time, availability, concurrent use, and accessibility ([OQ-010](../constraints.md#open-questions)); hosting constraints ([OQ-020](../constraints.md#open-questions)); and the devices used ([OQ-021](../constraints.md#open-questions)).

## Decision

The team chose option A:

| Concern | Choice |
|---|---|
| Server language | Java 25, a long-term support release. |
| Application framework | Spring Boot, with its web, security, data access (Spring Data JPA), and validation support. |
| Module boundaries | Spring Modulith declares the modules of [ADR-001](ADR-001-modular-monolith.md) and verifies their dependencies in a test. |
| Database | PostgreSQL. Schema changes are versioned as Flyway migrations. |
| User interface | A single-page application written in React and TypeScript and built with Vite. The same deployable unit serves it, from the same origin as the API ([ADR-004](ADR-004-rest-api-single-entry-point.md)). |
| Automated tests | JUnit for unit tests; Spring's test support with MockMvc for API tests; Testcontainers to run PostgreSQL in integration tests; Playwright for end-to-end tests through the User interface. |
| Build tool | Maven. *Added on 2026-10-30 by team decision, chosen over Gradle; this was an open point of this ADR.* |
| Authentication | Signed access tokens ([ADR-005](ADR-005-authentication-with-jwt.md)). Option A had proposed server-side sessions. |

Exact versions are chosen and pinned in increment I1 ([implementation plan](../implementation-plan.md)). This ADR fixes only the Java release.

## Consequences

- **Two languages and toolchains.** The server needs a JDK. Node.js is needed to build the User interface, but not to run it.
- **Simple to run.** Running Matriz requires one deployable unit and one PostgreSQL database. Where it is hosted remains open ([OQ-020](../constraints.md#open-questions)).
- **Boundaries checked by a test.** The Spring Modulith test fails when a module uses another module's internals or when modules depend on each other in a cycle. This supports the module rules of the [architecture definition](../architecture.md#components-and-responsibilities).
- **Concurrency support.** PostgreSQL transactions and row locks support [ADR-006](ADR-006-simultaneous-changes.md).
- **Container runtime required.** Testcontainers needs one on developers' machines and in continuous integration.
- **IDE configuration.** The local IDE configuration (the untracked `.idea/misc.xml`) targets JDK 24, which is not a long-term support release. It should target JDK 25 from I1.

## Alternatives considered

These options were presented in the phase 1 analysis with the pros and cons below. The team chose option A and recorded no further rationale.

| Option | Summary | Pros presented | Cons presented |
|---|---|---|---|
| A (chosen) | As above, with server-side sessions in a cookie. Boundaries checked by Spring Modulith or ArchUnit. | Typed; boundaries checked automatically; mature security support; matches the repository's existing signals (a Java `.gitignore` and IDE configuration). | Two languages and toolchains. |
| B | TypeScript on Node.js with NestJS. Boundaries checked by dependency-cruiser. PostgreSQL with Prisma. Vitest or Jest, Supertest, and Playwright. A React single-page application served by the same process. | One language end to end. | Security assembled from separate libraries; boundaries enforced only by a lint rule. |
| C | Python with Django, plus Django REST framework if the REST API is kept. Boundaries checked by import-linter. PostgreSQL (SQLite locally) with Django migrations. pytest-django and Playwright. Server-rendered templates with htmx. | Fastest path to sign-in, basic screens, and seed data. | Dynamic typing. The Django admin site would bypass Access control ([OQ-019](../constraints.md#open-questions)). A REST API plus templates means two entry points. |

## Open points

These are decided in increment I1 unless stated otherwise:

- How the User interface's build is included in the deployable unit. The build tool is Maven (see the decision above).
- The versions of every component.
- The continuous integration service.
- The browsers covered by end-to-end tests. This depends on [OQ-021](../constraints.md#open-questions).
- Hosting and deployment, which remain open ([OQ-020](../constraints.md#open-questions)).
