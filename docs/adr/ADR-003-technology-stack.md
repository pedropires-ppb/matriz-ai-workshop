# ADR-003 — Technology stack

- **Status:** Accepted (provisional)
- **Date:** 2026-10-29; amended on 2026-10-30 (build tool; open points due in I1, decided in [ADR-007](ADR-007-build-persistence-and-demonstration-data.md)); amended again on 2026-10-30 (a test library for the layering check, decided in [ADR-008](ADR-008-domain-driven-design.md)); a consequence removed on 2026-10-30 (IDE configuration, by team decision in the conversation “I1 review: remaining decisions”); a cross-reference added on 2026-11-02 (the token library, decided in [ADR-005](ADR-005-authentication-with-jwt.md))
- **Decided by:** the project team ([ADR-002](ADR-002-phase-scope-and-implementation-authorization.md), question 3). The team chose among three options that the AI assistant presented at the team's request.
- **Related:** [ADR-001](ADR-001-modular-monolith.md), [ADR-004](ADR-004-rest-api-single-entry-point.md), [ADR-005](ADR-005-authentication-with-jwt.md), [ADR-006](ADR-006-simultaneous-changes.md), [ADR-008](ADR-008-domain-driven-design.md) (added on 2026-10-30); [NFR-004–NFR-007](../requirements.md#non-functional-requirements-and-unresolved-quality-criteria); [OQ-010](../constraints.md#open-questions), [OQ-020](../constraints.md#open-questions), [OQ-021](../constraints.md#open-questions)

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
| Automated tests | JUnit for unit tests; Spring's test support with MockMvc for API tests; Testcontainers to run PostgreSQL in integration tests; Playwright for end-to-end tests through the User interface. *Added on 2026-10-30 by team decision in the I1 review ([ADR-008](ADR-008-domain-driven-design.md), decision 5): ArchUnit checks the layers inside each module.* |
| Build tool | Maven. *Added on 2026-10-30 by team decision, chosen over Gradle; this was an open point of this ADR.* |
| Authentication | Signed access tokens ([ADR-005](ADR-005-authentication-with-jwt.md)). Option A had proposed server-side sessions. *Added on 2026-11-02 as a cross-reference to [ADR-005](ADR-005-authentication-with-jwt.md#open-points) (“Signing”, resolved by team decision in Lab Worksheet 03, question 7): the tokens are issued and verified with Spring Security's OAuth2 resource server support (`spring-boot-starter-security-oauth2-resource-server`), which uses Nimbus JOSE+JWT. Spring Boot manages its version.* |

Exact versions are chosen and pinned in increment I1 ([implementation plan](../implementation-plan.md)). This ADR fixes only the Java release. *Added on 2026-10-30: the versions chosen are listed in [ADR-007](ADR-007-build-persistence-and-demonstration-data.md) (decision 1).*

## Consequences

- **Two languages and toolchains.** The server needs a JDK. Node.js is needed to build the User interface, but not to run it.
- **Simple to run.** Running Matriz requires one deployable unit and one PostgreSQL database. Where it is hosted remains open ([OQ-020](../constraints.md#open-questions)).
- **Boundaries checked by a test.** The Spring Modulith test fails when a module uses another module's internals or when modules depend on each other in a cycle. This supports the module rules of the [architecture definition](../architecture.md#components-and-responsibilities). *Added on 2026-10-30 ([ADR-008](ADR-008-domain-driven-design.md)): a second test fails when a layer inside a module uses a layer it must not.*
- **Concurrency support.** PostgreSQL transactions and row locks support [ADR-006](ADR-006-simultaneous-changes.md).
- **Container runtime required.** Testcontainers needs one on developers' machines and in continuous integration.
- *Removed on 2026-10-30 by team decision (the conversation “I1 review: remaining decisions”, decision 7): previously “**IDE configuration.** The local IDE configuration (the untracked `.idea/misc.xml`) targets JDK 24, which is not a long-term support release. It should target JDK 25 from I1.” The repository does not set the IDE's JDK: git ignores `.idea/misc.xml`, which each machine keeps locally, and the tracked module file inherits the project's JDK.*

## Alternatives considered

These options were presented in the phase 1 analysis with the pros and cons below. The team chose option A and recorded no further rationale.

| Option | Summary | Pros presented | Cons presented |
|---|---|---|---|
| A (chosen) | As above, with server-side sessions in a cookie. Boundaries checked by Spring Modulith or ArchUnit. | Typed; boundaries checked automatically; mature security support; matches the repository's existing signals (a Java `.gitignore` and IDE configuration). | Two languages and toolchains. |
| B | TypeScript on Node.js with NestJS. Boundaries checked by dependency-cruiser. PostgreSQL with Prisma. Vitest or Jest, Supertest, and Playwright. A React single-page application served by the same process. | One language end to end. | Security assembled from separate libraries; boundaries enforced only by a lint rule. |
| C | Python with Django, plus Django REST framework if the REST API is kept. Boundaries checked by import-linter. PostgreSQL (SQLite locally) with Django migrations. pytest-django and Playwright. Server-rendered templates with htmx. | Fastest path to sign-in, basic screens, and seed data. | Dynamic typing. The Django admin site would bypass Access control ([OQ-019](../constraints.md#open-questions)). A REST API plus templates means two entry points. |

## Open points

*Changed on 2026-10-30 by team decision ([ADR-007](ADR-007-build-persistence-and-demonstration-data.md)): the first four points below, due in I1, are decided; previously all were open. The Java release fixed above is unchanged.*

- **Decided:** how the User interface's build is included in the deployable unit. The build tool is Maven (see the decision above). frontend-maven-plugin builds `frontend/` with a pinned Node.js into the application's static resources, so one Maven build produces one deployable unit ([ADR-007](ADR-007-build-persistence-and-demonstration-data.md), decision 2).
- **Decided:** the versions of every component ([ADR-007](ADR-007-build-persistence-and-demonstration-data.md), decision 1).
- **Decided:** the continuous integration service is GitHub Actions ([ADR-007](ADR-007-build-persistence-and-demonstration-data.md), decision 4).
- **Decided provisionally:** the browsers covered by end-to-end tests. Chromium only, with Playwright for Java, until [OQ-021](../constraints.md#open-questions) is answered ([ADR-007](ADR-007-build-persistence-and-demonstration-data.md), decision 3).
- **Open:** hosting and deployment ([OQ-020](../constraints.md#open-questions)).
