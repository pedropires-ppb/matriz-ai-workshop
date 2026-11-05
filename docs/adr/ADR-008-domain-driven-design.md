# ADR-008 — Domain-driven design in the backend

- **Status:** Accepted (provisional). *Amended on 2026-10-30 by team decision in the conversation “I1 review: remaining decisions” (decision 9): decision 4 now puts Jakarta Persistence (JPA) annotations on the domain classes, which changes parts of decisions 1, 2, 5, and 6, the consequences, the alternatives, and the open points. Each change is flagged where it is made, with the previous text. The [ADR index](README.md) allows this amendment in place because I1, the increment that introduced this ADR, is not Accepted.* *Amended again on 2026-10-31 by team decision (the conversation “I1 review: last decisions”, decision 2): the limit for Jakarta Persistence in rule 1 of decision 5 is accepted and no longer a proposal.* *On 2026-11-02, by team decision (Lab Worksheet 03, question 13), the open point “Mapping conventions” was resolved in place. Flagged cross-references to [ADR-005](ADR-005-authentication-with-jwt.md) and [ADR-009](ADR-009-identity-from-the-token.md) were added to decisions 2 and 5. No decision changed.*
- **Date:** 2026-10-30
- **Decided by:** the project team, in a working session with the AI assistant on 2026-10-30 (the conversation “I1 review: demonstration data through Flyway, and a domain-driven backend”), while reviewing increment I1. For each question, the AI assistant presented two or three options with their trade-offs and a recommendation [Proposal]. The team chose an option for every question and confirmed the summary. The AI assistant decided nothing.
- **Related:** [ADR-001](ADR-001-modular-monolith.md), [ADR-003](ADR-003-technology-stack.md), [ADR-004](ADR-004-rest-api-single-entry-point.md), [ADR-005](ADR-005-authentication-with-jwt.md) and [ADR-009](ADR-009-identity-from-the-token.md) (added on 2026-11-02), [ADR-006](ADR-006-simultaneous-changes.md), [ADR-007](ADR-007-build-persistence-and-demonstration-data.md) (decisions 8, 10, and 11; amended on the same day); [architecture definition](../architecture.md#components-and-responsibilities) (module rules); [data model](../database.md); [glossary](../requirements.md#glossary); [implementation plan](../implementation-plan.md#increments); [Assumption A-029](../constraints.md#assumptions); [OQ-003](../constraints.md#open-questions), [OQ-016](../constraints.md#open-questions), [OQ-024](../constraints.md#open-questions)

## Context

- The team reviewed increment I1 and asked for the backend to be structured with domain-driven design (DDD), within the modular monolith ([ADR-001](ADR-001-modular-monolith.md)) and Spring Modulith ([ADR-003](ADR-003-technology-stack.md)).
- Spring Modulith treats a module's top-level package as its interface and its sub-packages as internal ([ADR-007](ADR-007-build-persistence-and-demonstration-data.md), decision 11). A test already checks the module boundaries.
- The [data model](../database.md) defines the entities and rules 1–8, and the [glossary](../requirements.md#glossary) defines the terms. DDD must add no business rule.
- Some rules span aggregates and modules, for example completing a project or removing a participant. [ADR-006](ADR-006-simultaneous-changes.md) requires each to be checked and saved in one transaction.
- I1 has no features, so its modules are empty.

## Decision

### 1. Packages and layers

- A module's **top-level package is its API**: the identifiers that other modules use, such as `PersonId` and `ProjectId`, and the interfaces that other modules call or implement. For example, Projects defines the interface through which it asks Tasks for task facts (module rule 2).
- Its **layers are internal sub-packages**:

  | Layer | Contents |
  |---|---|
  | `domain` | Aggregates, entities, value objects, and repository interfaces, with Jakarta Persistence mapping annotations (decision 4). *Changed on 2026-10-30 by team decision (decision 9 of the conversation “I1 review: remaining decisions”): previously “Aggregates, entities, value objects, and repository interfaces, in plain Java.”* |
  | `application` | Use cases, each one transaction ([ADR-006](ADR-006-simultaneous-changes.md)), including the checks of the rules that span aggregates (decision 3). |
  | `infrastructure` | The implementations of the domain's repository interfaces, with Spring Data JPA (decision 4), and the implementations of interfaces that other modules define. *Changed on 2026-10-30 by the same decision: previously “The persistence model and its mappers (decision 4), the implementations of the domain's repository interfaces, and the implementations of interfaces that other modules define.”* |
  | `web` | REST controllers and the request and response types of [openapi.yaml](../../openapi.yaml) ([ADR-004](ADR-004-rest-api-single-entry-point.md)). |

- Dependencies point inward: `web` → `application` → `domain`, and `infrastructure` → `domain`. Nothing depends on `web`. Decision 5 lists the rules that the build checks.
- Other modules are used only through their API (module rule 2), as before.
- Cross-cutting code stays in the base package, `com.matriz` ([ADR-007](ADR-007-build-persistence-and-demonstration-data.md), decision 11).

### 2. Aggregates, entities, and value objects

The glossary is the ubiquitous language: classes are named in its terms.

| Module | Aggregate | Entities inside | Value objects | Data model rules kept inside |
|---|---|---|---|---|
| Identity | Person | — | `PersonId`, `LoginName`, `DisplayName`, `PasswordHash` | — |
| Projects | Project | Membership, identified within its project by the member's `PersonId`, with a `MemberRole` | `ProjectId`, `ProjectState`, `MemberRole`, the project's name | Rule 1 (one Project Owner); rule 5 for the project and its Memberships (a Completed project does not change) |
| Tasks | Task | Assignment, identified within its task by the assignee's `PersonId` | `TaskId`, `TaskState`, `TaskPriority`, the task's title | No member is assigned twice to the same task ([FR-010](../requirements.md#functional-requirements)); deleting a task removes its Assignments (rule 6) |
| Tasks | Comment | — | `CommentId`, the comment's text | Rule 7 (a Comment is never changed); Comments can still be added in a Completed project (rule 5) |
| Progress | None: a read model, derived from the states of tasks | — | — | — |
| Access control | None: a policy that decides from data owned by other modules | — | — | — |

- Aggregates refer to each other by identifier only (`PersonId`, `ProjectId`, `TaskId`, `CommentId`), also within a module.
- Value objects check only what the data model requires:
  - text is present and nonempty, with no maximum length until [OQ-003](../constraints.md#open-questions) and [OQ-024](../constraints.md#open-questions) are answered. *Changed on 2026-11-02 by team decision (the conversation “Implement project management”, question 4): the project name and description now have maximum lengths (see the open points); the other texts keep the rule;*
  - an enumeration holds one of its values: `ProjectState`, `MemberRole`, `TaskState`, or `TaskPriority`.
- Task's `creation_order` is technical ([ADR-007](ADR-007-build-persistence-and-demonstration-data.md), decision 8). Whether and how the Task class maps it is decided in I4 (see the open points). *Changed on 2026-10-30 by team decision (decision 9 of the conversation “I1 review: remaining decisions”): previously “It stays outside the domain, in the persistence model”, which decision 4 no longer has.*
- This groups the [rules of the data model](../database.md#rules-not-shown-in-the-diagram) and adds none. Class names that this table does not give are chosen in the increment that creates the class.
- *Added on 2026-11-02 as a cross-reference to [ADR-005](ADR-005-authentication-with-jwt.md#open-points) (“How sign-out is enforced”, resolved by team decision in Lab Worksheet 03, question 3).* Identity also keeps the identifiers of ended access tokens. They are technical data, not an aggregate and not an entity of the [data model](../database.md).
  - The domain declares an interface for them, and `infrastructure` implements it with native SQL through Jakarta Persistence.
  - Native SQL is needed because the inserts use PostgreSQL's `ON CONFLICT DO NOTHING`.

### 3. Rules spanning aggregates

- Application services enforce them, with one transaction per use case ([ADR-006](ADR-006-simultaneous-changes.md), decision 2). Each such use case:
  1. locks the project's row first, as [ADR-006](ADR-006-simultaneous-changes.md#open-points) proposes; the lock is confirmed in I3;
  2. reads the facts it needs from other modules through their API. For example, Projects asks whether a project has unfinished tasks, or whether a member has tasks assigned, through an interface that Projects defines and Tasks implements (module rule 2);
  3. then changes one aggregate.

  Completing a project ([FR-007](../requirements.md#functional-requirements)) and removing a participant ([FR-005](../requirements.md#functional-requirements)) follow these steps.
- Database constraints remain the backstop ([ADR-007](ADR-007-build-persistence-and-demonstration-data.md), decision 8), for example `ON DELETE RESTRICT` for rule 2 and the composite foreign keys for rule 8.
- No rule is enforced through domain events or database triggers.

### 4. Persistence

- Domain classes carry the Jakarta Persistence mapping annotations, for example `@Entity` and `@Column`. There is no separate persistence model and no mapper.
- The domain declares repository interfaces, and `infrastructure` implements them with Spring Data JPA ([ADR-003](ADR-003-technology-stack.md), unchanged).
- Only Flyway migrations create or change the schema. Hibernate only validates it ([ADR-007](ADR-007-build-persistence-and-demonstration-data.md), decision 8, unchanged).

*Changed on 2026-10-30 by team decision (decision 9 of the conversation “I1 review: remaining decisions”): previously “Domain classes are plain Java: they use neither Jakarta Persistence nor Spring. The persistence model is separate, in `infrastructure`: JPA entities, Spring Data JPA repositories ([ADR-003](ADR-003-technology-stack.md), unchanged), and mappers between the entities and the domain. Each mapper has tests. The domain declares repository interfaces, and `infrastructure` implements them.”*

### 5. Layering check

- ArchUnit 1.4.2, the version that Spring Modulith 2.1.2 already uses, is declared explicitly as a test dependency in `pom.xml`. **Flag:** this adds a test library to the stack of [ADR-003](ADR-003-technology-stack.md), which notes it.
- [`LayeringTests`](../../src/test/java/com/matriz/LayeringTests.java), a plain JUnit test with rules written for Matriz, checks the production code of every module. The build fails when one of these rules is broken:
  1. The domain uses no other layer of any module, and neither Spring nor Hibernate. Of Jakarta Persistence, it uses only annotations and the enumerations their attributes take, such as `EnumType`. *Changed on 2026-10-30 by team decision (decision 9 of the conversation “I1 review: remaining decisions”): previously “The domain uses no other layer of any module, and neither Spring, Jakarta Persistence, nor Hibernate.” The limit for Jakarta Persistence was proposed by the AI assistant and accepted by the project team on 2026-10-31 (the conversation “I1 review: last decisions”, decision 2): previously tagged [Proposal].*
  2. The application layer uses neither infrastructure nor web.
  3. No class outside a web layer uses one.
  4. The web layer does not use infrastructure.
  5. A module's API uses no layer.

  The AI assistant proposed rules 4 and 5 during implementation, and the team confirmed them.
- *Added on 2026-11-02 as a cross-reference to [ADR-009](ADR-009-identity-from-the-token.md) (decision 3).* Another ArchUnit test fails the build when a class outside the Identity module uses Spring Security.
- [`ModuleBoundaryTests`](../../src/test/java/com/matriz/ModuleBoundaryTests.java) still checks the module boundaries with Spring Modulith.

### 6. What I1 delivers and what later increments add

I1 delivers the structure, the conventions, and the checks, with no aggregate, mapping, or use case:

- each module's API package and its four empty layers. Each has a `package-info.java` that states its role, what it may use, and the increment of its first classes;
- `LayeringTests`, with a layering violation shown to fail the build;
- the demonstration data as a Flyway migration ([ADR-007](ADR-007-build-persistence-and-demonstration-data.md), decision 10, amended).

Later increments add the following, each with the JPA mapping, repository implementations, use cases, and web layer it needs. *Changed on 2026-10-30 by team decision (decision 9 of the conversation “I1 review: remaining decisions”): previously “each with the persistence model, mappers, use cases, and web layer it needs”.* The [implementation plan](../implementation-plan.md#increments) lists them in each increment's components.

| Increment | Adds |
|---|---|
| I2 | Person |
| I3 | Project with the Project Owner's Membership; the access policy; the project lock |
| I4 | Task |
| I5 | The Progress read model |
| I6 | Changes of Membership; the task-facts interface |
| I7 | Assignments |
| I8 | Completing a project; the check that refuses changes in a Completed project |
| I9 | Comment |
| I10 | Deletion |
| I11 | The search and filter queries |

## Consequences

- **The domain is tested without Spring or a database.** Its mapping annotations have no effect outside a persistence provider. *Changed on 2026-10-30 by team decision (decision 9 of the conversation “I1 review: remaining decisions”): previously “**The domain is tested without Spring or a database**, because it is plain Java.”*
- **The domain carries its mapping.** Each aggregate is one class, mapped with annotations, with a repository implementation and no mapper. The Jakarta Persistence specification requires an entity class to have a public or protected constructor without arguments; the class, its methods, and its persistent fields cannot be final, and it cannot be a record. The domain classes follow these rules. Hibernate checks the mapping against the schema at start-up ([ADR-007](ADR-007-build-persistence-and-demonstration-data.md), decision 8). *Changed on 2026-10-30 by the same decision: previously “**More code per feature.** Each aggregate has a domain class, a JPA entity, a mapper, and a repository implementation. A field that a mapper omits is caught only by the mapper's tests.”*
- **One place for each rule.** A rule within an aggregate is in the aggregate, and a rule spanning aggregates is in an application service. The database constraints catch only some mistakes ([ADR-007](ADR-007-build-persistence-and-demonstration-data.md), decision 8).
- **Checked by the build.** `./mvnw verify` fails on a module boundary or layering violation, locally and in continuous integration. In I1 the layers are empty, so the rules pass trivially (`allowEmptyShould(true)`). A violation introduced on purpose showed that they work ([verification record](../evidence/I1-verification.md#9-re-verification-after-the-teams-review)). After decision 9 changed rule 1, [section 10 of the record](../evidence/I1-verification.md#10-re-verification-after-the-reviews-remaining-decisions) shows that the domain may use Jakarta Persistence's annotations and still may not use the rest of it. *The last sentence was added on 2026-10-30.*
- **ArchUnit follows Spring Modulith.** When Spring Modulith is upgraded, the explicit ArchUnit version is checked against the one it uses.

## Alternatives considered

These are the options not chosen, as presented to the team. No further rationale was recorded.

| Question | Options not chosen |
|---|---|
| Packages and layers | Hexagonal architecture (ports and adapters); flat packages, with each class's role marked by an annotation. |
| Aggregates | Membership as its own aggregate; Comment inside the Task aggregate. |
| Rules spanning aggregates | Synchronous Spring Modulith events, which cannot refuse a change beforehand and need an event table; database triggers, which would change [ADR-007](ADR-007-build-persistence-and-demonstration-data.md) decision 8. |
| Persistence | A separate persistence model in `infrastructure`, with JPA entities and tested mappers: chosen first in the I1 review, then replaced on the same day by team decision (decision 9 of the conversation “I1 review: remaining decisions”); Spring Data JDBC, which would change [ADR-003](ADR-003-technology-stack.md), and which deletes and reinserts child rows, against `ON DELETE RESTRICT`. *Changed on 2026-10-30 by that decision: previously the first option not chosen was “JPA annotations on the domain classes, which the AI assistant had recommended [Proposal]”.* |
| Layering check | jMolecules with ArchUnit; a third option, not recorded in detail. |
| Scope of I1 | I1 also creates the aggregates and their mappings; documentation only, with no code. |

## Open points

- **Mapping of Memberships and Assignments.** Decided on 2026-11-02 by team decision (the conversation “Implement project management”, question 12) for Memberships: they are entities with a composite identifier (`project_id`, `person_id`) in a `Set` inside the Project aggregate, not an `@ElementCollection`, so that changing one does not delete and reinsert the others, and `ON DELETE RESTRICT` (rule 2) holds. Assignments are decided in I7. *Previously: “[Proposal] Map them so that changing one does not delete and reinsert the others, for example as entities with composite identifiers in a `Set`, not as an `@ElementCollection` list. Otherwise `ON DELETE RESTRICT` (rule 2) refuses the change when an Assignment refers to a Membership. To be decided in I3.”*
- **Mapping conventions.** Resolved on 2026-11-02 by team decision (Lab Worksheet 03, question 13), with the first aggregate, Person:
  - **Value objects.** A value object is a record annotated `@Embeddable`, whose component names its column with `@Column`. An aggregate maps it with `@Embedded`, and with `@AttributeOverride` where the column's name differs.
  - **Identifiers.** An identifier is a record annotated `@Embeddable` that implements `Serializable`, mapped with `@EmbeddedId`.
  - **Shared identifiers.** An identifier that other modules use is in its module's API package; `PersonId` is in `com.matriz.identity`. Rule 5 of decision 5 still holds, because Jakarta Persistence is not a layer.
  - **Tests.** Each aggregate has a repository test against PostgreSQL in Testcontainers. It stores an aggregate, reloads it in a new persistence context, and compares every field. Hibernate still validates the mapping against the schema at start-up ([ADR-007](ADR-007-build-persistence-and-demonstration-data.md), decision 8).

  *Previously: “How identifiers and value objects are mapped with Jakarta Persistence annotations, and how the mapping is tested; to be decided in I2, with the first aggregate [Proposal].” That text was changed on 2026-10-30 by team decision (decision 9 of the conversation “I1 review: remaining decisions”); before that, it was “**Mapper conventions.** Names, and where the mapper tests are; to be decided in I2, with the first aggregate [Proposal].” There are no mappers.*
- **Task's `creation_order`.** Decided on 2026-11-05 by team decision (the conversation “Implement task management”, question 7): the Task class maps it as a read-only field that the database generates (it is never inserted or updated by the application), with no accessor outside the `infrastructure`-facing mapping; the API never exposes it, and the oldest-first list orders by it. *Previously: “Whether and how the Task class maps it, now that there is no separate persistence model (decision 2); to be decided in I4, with the Task aggregate [Proposal].”* *Added on 2026-10-30 with decision 9 of the conversation “I1 review: remaining decisions”.*
- **Maximum text lengths.** Decided on 2026-11-02 for projects (the conversation “Implement project management”, question 4): the value objects `ProjectName` (1 to 100 characters after trimming) and `ProjectDescription` (1 to 1000, stored only when present) check them, and the migration `V4` adds them ([Assumption A-031](../constraints.md#assumptions)). Task titles and comment text stay open ([OQ-003](../constraints.md#open-questions), [OQ-024](../constraints.md#open-questions)). *Previously: “When they are agreed in I3 …, the value objects check them too, and a new migration adds them …”.* *Updated on 2026-11-05 by team decision (the conversation “Implement task management”, questions 2 to 4): the value objects `TaskTitle` (1 to 150 characters after trimming) and `TaskDescription` (1 to 2000, stored only when present) check them, and the migration `V5` adds them ([Assumption A-033](../constraints.md#assumptions)); the value objects of projects and tasks treat Unicode space separators as whitespace. Comment text stays open.*
- **Layers of Progress and Access control.** Access control decided on 2026-11-02 (the conversation “Implement project management”, question 12): a policy with no aggregate. Its interface and its small types, which use no data of other modules, are in the API package of Projects, because Projects' use cases call the policy and Projects must not depend on Access control; the implementation is in the `application` layer of Access control, which may use Projects (the same pattern as the task facts of [architecture.md](../architecture.md#components-and-responsibilities), module rule 2). *Corrected on 2026-11-02 by team decision (the conversation “Implement project management”, the question about the access-control interface, answer A): the interface was to be in the API package of Access control, which would have made Projects and Access control depend on each other.* Progress is still to be decided in I5. *Previously: “Which layers they need; to be decided in I5 and I3.”*
- **The project lock.** Confirmed in I3 on 2026-11-02 ([ADR-006](ADR-006-simultaneous-changes.md#open-points)). *Previously: “To be confirmed in I3.”*
- **AGENTS.md.** Resolved on 2026-10-30 by team decision (decision 2 of the conversation “I1 review: remaining decisions”): [AGENTS.md](../../AGENTS.md#code-structure) links to this ADR and requires `LayeringTests` and `ModuleBoundaryTests` to pass. *Previously: “It does not mention these conventions. The AI assistant proposed a change to the team; it is not applied.”*
