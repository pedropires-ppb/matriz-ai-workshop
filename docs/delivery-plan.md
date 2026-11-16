# Matriz Delivery Preparation Plan

> **Execution boundary:** This document records the plan approved for Exercise 1 on 2026-11-14. Each later exercise requires its own prompt and approval; this plan alone does not authorize implementation.

**Goal:** Prepare a reproducible, non-published Docker delivery candidate while preserving Matriz's approved single-application, same-origin deployment.

**Architecture:** Keep React assets packaged in the Spring Boot application, which serves both the UI and `/api` from one origin. PostgreSQL remains the only external runtime service. Compose keeps its current database-only default and adds the application as an opt-in profile; host-side `curl` verifies application readiness without adding a probe package to the runtime image.

**Tech stack:** Existing Maven/Spring Boot and Vite build; Docker multi-stage build; official Eclipse Temurin Java 25 builder/JRE images pinned by immutable digest; Docker Compose; GitHub Actions; PostgreSQL 18.6.

**Sources:** [ADR-003](adr/ADR-003-technology-stack.md), [ADR-004](adr/ADR-004-rest-api-single-entry-point.md), [ADR-007](adr/ADR-007-build-persistence-and-demonstration-data.md), [ADR-008](adr/ADR-008-domain-driven-design.md), [architecture](architecture.md), [requirements](requirements.md), [constraints](constraints.md), [AGENTS.md](../AGENTS.md), [README](../README.md), and [OpenAPI](../openapi.yaml).

## Global constraints

- Preserve the single Spring Boot deployable and same-origin UI/API; do not add a separate frontend runtime.
- Do not change business rules, API behavior, authentication, dependencies, approved decisions, or unrelated production code.
- Use JDK 25, Maven Wrapper 3.3.4/Maven 3.9.16, Node 24.22.2/npm 11.19.1, and PostgreSQL 18.6 as already pinned/documented.
- Build and validate `linux/amd64` and `linux/arm64`; do not publish or upload the candidate image.
- Keep secrets out of the Docker context, build arguments, image layers, Compose output, logs, screenshots, and commits. Never use a developer's `.env` or application database for isolated checks.
- Keep Compose database-only by default. Use a uniquely named Compose project and volume, fictitious test data, and loopback-only ports for delivery checks. Do not stop existing containers or remove resources; ask before cleanup naming the exact resources.
- Keep demo data off by default. For an isolated, fresh database only, enable it before the first application startup and supply all four fictitious demo passwords; the setting is fixed for that database.
- Stay on `delivery-preparation` for all six exercises. Make small commits with `[delivery][exN]:` prefixes and the required Copilot co-author trailer. Do not push before Exercise 4 explicitly authorizes it. Do not merge, publish a release, push tags, or deploy.
- Treat the requirements and ADRs as provisional where they say so; OQ-016 and hosting OQ-020 remain open.

## Approved decisions

| Area | Decision |
|---|---|
| Image build | Build the application inside a multi-stage Docker build. |
| Base images | Official Temurin 25 JDK builder and Temurin 25 JRE runtime, pinned by immutable digests. Target `linux/amd64` and `linux/arm64`. |
| Compose compatibility | Add the application under an opt-in `app` profile. The existing default `docker compose up -d --wait` remains database-only. |
| Runtime configuration | Reuse the established ignored `.env` workflow for ordinary local operation. Isolated verification uses a separate ignored env file with fictitious values, never the developer's `.env`. No runtime secrets enter the image build. |
| Demonstration data | Off by default; opt in only before the first startup of a fresh isolated database. |
| Isolated ports/resources | Use a unique Compose project and volume, loopback app port `18089`, and database host port `15432`; check port availability before startup. |
| Documentation | Record the plan here and exercise evidence in [delivery-preparation evidence](evidence/delivery-preparation.md); link the plan from README. |
| Architecture and agent rules | Preserve the existing architecture and `AGENTS.md`; document operational commands in README. Add no ADR unless a later approved decision changes architecture or resolves hosting. |

## Files and responsibilities

| File | Planned change |
|---|---|
| `Dockerfile` | Create the multi-stage, same-application image build. |
| `.dockerignore` | Create exclusions for local env files, generated output, dependencies, and repository metadata. |
| `compose.yaml` | Add the optional application profile, internal database networking, readiness, and port mapping without changing the existing database-only default. |
| `.env.example` | Document the optional application host-port setting; retain fictitious placeholders only. |
| `.github/workflows/ci.yml` | Add an image-build check after verification, with no publishing credentials or push. |
| `README.md` | Document the optional container run path and link this plan; preserve the existing Java run path and database-only Compose command. |
| `docs/delivery-plan.md` | This approved plan and decisions. |
| `docs/evidence/delivery-preparation.md` | Record actual commands, results, limitations, and checks for each exercise without secret values. |

Do not change `pom.xml`, application source, API contract, business tests, or dependencies for this delivery work unless a later exercise explicitly approves a tightly scoped change.

## Ordered workstreams and completion conditions

These are dependencies, not assignments to future exercise numbers; each later exercise's prompt determines its authorized scope.

1. **Build context and image.** Add `.dockerignore` before the Dockerfile. The JDK 25 builder uses the Maven Wrapper and existing integrated frontend build; the JRE runtime serves the resulting Spring Boot JAR. Build the platform-independent JAR on the builder platform, then assemble runtime variants for both target platforms. Pin verified multi-platform base-image digests. The build context and image contain no local env files, generated dependencies, credentials, or build-time secrets.
2. **Compose runtime.** Add the app as an opt-in profile while preserving the database-only default and existing volume semantics. Compose gates the app on PostgreSQL health; the app connects by Compose service DNS and exposes the existing Actuator health endpoint. Verify app readiness from the host with `curl`, without adding a probe package to the minimal runtime image. The app serves UI and API on the same origin. A separately named Compose project and volume use loopback ports 18089 and 15432 for isolated checks.
3. **Verification versus packaging.** Run `./mvnw -B clean verify` as the full verification gate. Image packaging uses `./mvnw -B -DskipTests package`: it skips Java tests and Chromium setup but still runs frontend typecheck, Vitest, and Vite build. Do not treat image packaging as a substitute for verification.
4. **CI image check.** Keep the current verification job and make the image-build job depend on it. Build both architectures on existing push/pull-request triggers with read-only repository permissions and no registry push, upload, release, or deployment. Use the existing Maven project version `0.1.0-SNAPSHOT` plus the commit SHA as the non-published candidate identifier; do not change the Maven version or push tags.
5. **Documentation and final verification.** Keep README commands consistent with Compose's database-only default and opt-in app profile. Record only observed results in the evidence file. Do not claim production readiness, client approval, or a passing hosted CI run without evidence.

**Observable completion:** Full Maven verification passes; both image platforms build without publishing; isolated Compose waits for PostgreSQL health, host-side `curl` confirms application readiness, and the UI and API are served from one origin; migrations complete and the named database volume survives application restart; no secret appears in build context, image, logs, or committed files; documentation matches observed behavior and evidence states all unrun checks.

## Implementation tasks

The tasks are ordered by dependency. Each later exercise must authorize its own task before implementation.

### Task 1: Build the multi-platform application image

**Files:**
- Create: `Dockerfile`
- Create: `.dockerignore`

**Consumes:** The existing Maven build, which puts the React build under `target/classes/static`.

**Produces:** A non-published image identified by Maven version plus commit SHA. The image contains the Spring Boot JAR and JRE, not a separate frontend service.

- [ ] Exclude local env files (including `.env.example`), `target/`, `frontend/node/`, `frontend/node_modules/`, and `.git/` from the Docker context.
- [ ] Use a Temurin 25 JDK builder and Temurin 25 JRE runtime, each pinned to a verified immutable digest supporting both target architectures. Build on the builder platform; copy the platform-independent JAR into each target runtime image.
- [ ] Package with `./mvnw -B -DskipTests package`. Confirm this skips Java tests/Chromium setup but still runs frontend typecheck, Vitest, and Vite build.
- [ ] Run `docker buildx build --platform linux/amd64,linux/arm64 --output=type=oci,dest=/tmp/matriz-candidate.oci.tar --tag matriz:0.1.0-SNAPSHOT-$GIT_SHA .` with `GIT_SHA=$(git rev-parse --short HEAD)`. Confirm both variants build; do not upload or push the OCI output.
- [ ] Review the Docker context exclusions and image configuration to confirm no env file or secret was copied, passed as a build argument, or stored in a layer.

### Task 2: Extend Compose without changing its default

**Files:**
- Modify: `compose.yaml`
- Modify: `.env.example`

**Consumes:** Task 1 image; the current PostgreSQL healthcheck, credentials, volume, and environment-variable contract.

**Produces:** An opt-in application service connected to PostgreSQL on the Compose network, with the existing Postgres-only default retained.

- [ ] Add the application service to the `app` profile, use PostgreSQL's service name for the JDBC host, and gate startup on PostgreSQL's healthy state. Keep application readiness testing outside the runtime image.
- [ ] Preserve Postgres-only default behavior: with the isolated env file, `docker compose --env-file .env.delivery -p matriz-delivery config --services` lists only Postgres; adding `--profile app` lists both services.
- [ ] Bind the app to `127.0.0.1:18089` and Postgres to `127.0.0.1:15432` for isolated verification. Use a uniquely named Compose project and volume; do not inspect, stop, or reuse existing resources.
- [ ] Pass database credentials and the signing key only at runtime. Keep demos off by default. If a demo-enabled smoke test is needed, use a fresh isolated database and set the flag plus four fictitious passwords before its first app startup.
- [ ] Start with `docker compose --env-file .env.delivery -p matriz-delivery --profile app up --build -d --wait`; then use host-side `curl --fail-with-body` against `/actuator/health` and `/` at `http://127.0.0.1:18089`. Confirm `GET /api/projects` returns the expected unauthenticated `401` on that same origin.
- [ ] Restart only the application service in the isolated project and verify database migration/data state is retained. Ask before any cleanup of the named project or volume.

### Task 3: Add a non-publishing CI image check

**Files:**
- Modify: `.github/workflows/ci.yml`

**Consumes:** The image build from Task 1 and the existing `verify` job.

**Produces:** A pull-request/push image-build check for both platforms, with no credentials or publishing permissions.

- [ ] Keep the existing verification job unchanged and make the image job depend on it.
- [ ] Build both target architectures to temporary OCI output on the runner. Do not push an image, upload an artifact, add registry credentials, or change permissions from read-only contents access.
- [ ] Validate the workflow change by review and by its GitHub Actions run only when the corresponding exercise authorizes a push/PR. Record whether a hosted run actually occurred; do not report an unrun workflow as passing.

### Task 4: Document and verify the delivery candidate

**Files:**
- Modify: `README.md`
- Update: `docs/evidence/delivery-preparation.md`

**Consumes:** Tasks 1–3 and their actual command output.

**Produces:** Run instructions and an evidence record that distinguish completed checks from limitations.

- [ ] Document the unchanged database-only Compose command and the separate opt-in app profile, including runtime configuration and the health endpoint.
- [ ] Retain Maven version `0.1.0-SNAPSHOT`; identify a local/CI candidate by appending the commit SHA. Do not publish it, create a release, or push a tag.
- [ ] Run the full `./mvnw -B clean verify`, the authorized isolated image and Compose checks, and any authorized CI workflow. Record actual commands/results, redact secrets, and list checks not run.
- [ ] Review the final diff and staged diff. Commit only files authorized for that exercise with its `[delivery][exN]:` prefix and the required co-author trailer.

**Verification commands, when authorized and when Docker/ports are available:**

```sh
./mvnw -B clean verify
docker compose --env-file .env.delivery -p matriz-delivery --profile app config --quiet
GIT_SHA=$(git rev-parse --short HEAD)
docker buildx build --platform linux/amd64,linux/arm64 \
  --output=type=oci,dest=/tmp/matriz-candidate.oci.tar \
  --tag matriz:0.1.0-SNAPSHOT-$GIT_SHA .
docker compose --env-file .env.delivery -p matriz-delivery --profile app up --build -d --wait
curl --fail-with-body http://127.0.0.1:18089/actuator/health
curl --fail-with-body http://127.0.0.1:18089/
test "$(curl --silent --show-error --output /dev/null --write-out '%{http_code}' \
  http://127.0.0.1:18089/api/projects)" = 401
```

The OCI output is temporary and is not uploaded or published. Create `.env.delivery` with restrictive permissions and fictitious values, using a fresh Compose project/database; never print resolved Compose configuration. Compose readiness covers PostgreSQL; the host-side `curl` verifies application readiness. Record the actual commands and results, redact all secret values, and ask before cleaning up the named resources.

## Risks and open points

- Hosting, operations, and production secret management remain unresolved under OQ-020; this plan does not select a production host or secret store.
- The proposed MVP and its requirements remain unapproved by the client under OQ-016.
- Digest pinning requires deliberate base-image digest refreshes. Registries and build-time downloads remain external dependencies.
- Multi-platform builds and local port availability must be verified in the actual CI/host environment; neither is asserted by this plan.
- Compose env configuration can expose resolved values if printed. Use only fictitious test values for configuration checks and do not emit resolved configuration.

## Exercise 4 approved refinement — CI visibility

On 2026-11-15, the user approved replacing the single CI Maven verification step with separately named frontend and backend build/test steps, without reducing coverage or changing the ordinary `./mvnw -B verify` or Docker build behavior. This refines the earlier instruction to keep the verification job unchanged; the `verify` job ID and its role as the image-build prerequisite remain.

CI installs the frontend toolchain through the existing Maven execution, then runs the existing npm scripts separately. A default-off `frontend.skip` Maven property prevents repeating those steps during backend compilation and final packaging. Backend tests run through Surefire with the existing Mockito agent: unit, contract, architecture, integration/persistence, and browser groups. The integration selector includes all remaining tests, rather than a fixed allowlist. Separate report directories and a completeness check detect omitted or duplicated Java suites.

The summary lists frontend and backend suites separately, with actual test-case names and passed, failed/error, and skipped counts. It reads only JUnit result metadata, never captured application output or exception messages. It also runs after a failed step, and labels missing reports as unavailable rather than passing. Images and reports are not uploaded or published.

The user reported a successful [baseline run](https://github.com/pedropires-ppb/matriz-ai-workshop/actions/runs/43718405348). This is user-provided evidence, not an independently inspected hosted result. The controlled-failure demonstration remains pending and is paused during this refinement; no deliberate failure has been introduced.

### Separate checks approved on 2026-11-15

The user subsequently requested separate jobs instead of steps inside the `verify` job, with a Docker check named simply `Build Docker image`. This supersedes retaining the `verify` job ID: compilation, frontend unit tests, backend unit tests, backend integration/contract/architecture tests, and end-to-end tests now appear as distinct checks. No branch protection or required-check setting is changed automatically.

Compilation runs first. Test jobs use the same compiled classes and frontend assets, then Docker depends on compilation and every test job. Backend matrix fail-fast is disabled so one failing group does not cancel the others. Docker's first steps aggregate test metadata and reject missing/duplicated Java suites before building the image.

The user explicitly authorized temporary GitHub Actions artifacts for compiled output and sanitized reports, with one-day retention, replacing the earlier no-artifact-upload restriction for these files only. Artifacts contain `target/classes`, `target/test-classes`, or XML reconstructed from suite names, test-case names, and status; never raw reports, credentials, exception messages, logs, or images. Individual jobs retain distinct frontend/backend summaries. The image remains unpublished and still targets both approved processor architectures without highlighting them in the job name.

### Push-only trigger approved on 2026-11-15

The user explicitly selected CI on pushes to any branch only, removing the `pull_request` trigger to avoid duplicate runs for an open PR. This supersedes the earlier requirement to retain both triggers. Every push still executes the same compilation, test and Docker jobs; the workflow no longer separately checks GitHub's synthetic PR merge result. No branch protection or required-check configuration is changed.
