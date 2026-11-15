# Delivery Preparation Evidence

This record contains observed results for the six delivery-preparation exercises. Record commands and outcomes as run; never include secret values, tokens, or real personal data. A plan or expected result is not execution evidence.

## Exercise 1 — Technical analysis and plan

- **Date:** 2026-11-14
- **Branch:** `delivery-preparation`
- **Base commit:** `bf8eb12f827cd23da567b88cf33e128dab7d48c7` (`origin/main` after fetch)
- **Plan:** [Matriz Delivery Preparation Plan](../delivery-plan.md)
- **Working tree:** tracked working tree clean before documentation edits; no unrelated changes were present. Existing ignored generated output was not staged.
- **Host architecture:** `arm64`.
- **Commands run:**

  ```sh
  git status --short --branch
  git branch --list delivery-preparation main
  git log -1 --oneline --decorate
  git fetch origin main
  git switch -c delivery-preparation origin/main
  git rev-parse HEAD
  uname -m
  ```

- **Results:** `main` and fetched `origin/main` were at `bf8eb12`; `delivery-preparation` was created at that commit and tracks `origin/main`; `uname -m` returned `arm64`.
- **Safety:** No local `.env` was read or used. No Docker daemon, containers, volumes, or ports were inspected or changed. No files were staged before this evidence was written.
- **Not run:** Maven build/tests, Docker image build, Compose startup/configuration, OpenAPI validation, and GitHub Actions. No current-branch CI result is asserted. The older I12 verification record is evidence for its recorded earlier branch/commit only.
- **Limitations:** Docker/Compose versions and availability of ports 18089/15432 were not checked. Image digests and multi-platform support remain to be verified during an authorized implementation exercise.

## Exercise 2 — Docker containerization

- **Date:** 2026-11-14
- **Branch/base:** `delivery-preparation`, based on Exercise 1 commit `25d1a8c`.
- **Files:** `Dockerfile` and `.dockerignore`; the Dockerfile uses digest-pinned Temurin 25 Noble builder/runtime images, packages the integrated frontend and backend with the Maven Wrapper, and copies only the executable JAR into a non-root JRE runtime. The runtime's writable home and Java temporary directory are `/tmp`.
- **Verification:** Per the user's instruction, no Docker build, image inspection, container startup, or runtime check was run for this update. No successful image build or runtime behavior is claimed. Previous unsuccessful build diagnostics are omitted here at the user's request.
- **Runtime checks not performed:** PostgreSQL readiness and migrations, application HTTP response, static assets, sign-in, authorized project/task operations, missing-configuration failure, image metadata/history, and credential absence from a built image.
- **Resources:** No image was pushed or published. The isolated fictitious `.env.delivery` remains ignored. No files under `Flutter-repos` were changed.

## Exercise 3 — Integrated Docker Compose execution

- **Date:** 2026-11-14
- **Branch:** `delivery-preparation`
- **Changes:** Added an opt-in `app` profile to `compose.yaml`, retaining the database-only default, PostgreSQL service address/health dependency, existing PostgreSQL port and data mount, and loopback-only app port. Added `MATRIZ_APP_PORT` to `.env.example`. The isolated `.env.delivery` is ignored and was not displayed or staged.
- **Commands and outcomes:**

  ```sh
  docker compose version
  # Docker Compose version v2.38.2-desktop.1

  docker compose --env-file .env.delivery -p matriz-ex3 config -q
  # Passed; output was quiet.

  docker compose --env-file .env.delivery -p matriz-ex3 config --services
  # postgres

  docker compose --env-file .env.delivery -p matriz-ex3 --profile app config -q
  # Passed; output was quiet.

  docker compose --env-file .env.delivery -p matriz-ex3 --profile app config --services
  # postgres
  # app

  git diff --check
  # Passed.

  docker compose --env-file .env.delivery -p matriz-ex3 --profile app build app
  # Failed with exit code 1; output captured outside the repository.
  ```

- **Build limitation:** The Maven packaging step could not download its pinned Node.js 24.22.2 `linux-arm64` archive from `nodejs.org`; Java reported `certificate_unknown` / `PKIX path building failed`. This is an environment/build-time certificate trust limitation observed during the build, not evidence of an application runtime defect. No TLS bypass, trust-store change, or alternate build path was attempted. No successful application image is claimed.
- **Runtime and persistence checks not performed:** `up`, `ps`, logs, host HTTP/health/frontend/API checks, database communication, migrations, demonstration-data behavior, sign-in, project/task creation, assignment, comments, container recreation, and persistence verification. Because the image build failed, no Compose containers or project volume were started or created. No existing containers, volumes, or images were stopped or removed.
- **Secrets and diagnostics:** Resolved Compose configuration was never printed. Build output was captured to a session-local log and contains the Node.js download failure; no runtime credentials were emitted.
- **Status:** Compose configuration and default/profile service selection are verified. The integrated runtime exercise remains incomplete until the image can be built in an environment that trusts the required download certificate.

## Exercise 4 — Continuous integration

- **Date:** 2026-11-15
- **Initial implementation (`abadacc`):** Workflow and Compose changes were committed locally while GitHub authentication was unavailable to the agent. The user subsequently pushed the branch and reported a successful [baseline run](https://github.com/pedropires-ppb/matriz-ai-workshop/actions/runs/43718405348). The agent could not independently inspect that run or its commit SHA; no PR reference has been supplied. The deliberate test failure is not present.
- **Initial workflow:** The existing `verify` job, push and pull-request triggers, and `contents: read` permission were retained. A new `image` job depends on `verify`; it validates Compose with fictitious configuration, then builds both target platforms to a runner-local OCI file without publishing or uploading it.
- **Compose compatibility:** Database-only configuration no longer requires the application's signing key during Compose interpolation. Application startup remains responsible for rejecting a missing or invalid key.
- **Local checks:**
  - Before the correction, the new database-only `docker compose --env-file /dev/null -p matriz-ci config -q` check failed with the signing key missing. Afterward, that check and the `--profile app` check passed with isolated fictitious environment variables, and missing database credentials were rejected.
  - Ruby's YAML parser and structural assertions confirmed the workflow parses and retains both triggers, read-only contents permission, the Maven verify step, and `image`'s `needs: verify`. This is not a hosted GitHub Actions validation.
  - With the locally installed JDK 25, `./mvnw -B -Dtest=EnvironmentVariablesTests,CommentTextTests test` passed: 19 tests, no failures or errors. The environment tests exercise rejection of a blank signing key. Maven also ran the configured frontend build steps; this focused command is not full verification.
  - `git diff --check` passed. Docker Buildx is available locally, but the multi-platform build was not rerun; the Exercise 3 certificate failure remains unresolved.
- **Initial GitHub blocker:** `gh auth status` reported no authenticated GitHub host. At the initial local commit, no CI run or PR URL was available, and no passing CI status was claimed. The subsequent baseline is user-reported above; deliberate-failure and recovery commit SHAs and run URLs, including the failed test and skipped image job, remain to be collected.

### Approved refinement — Separate frontend and backend stages

- **Approval:** The user approved separately named build/test steps, distinct frontend/backend reports, and the limited Maven build-property changes; see the [approved refinement](../delivery-plan.md#exercise-4-approved-refinement--ci-visibility).
- **Files:** `.github/workflows/ci.yml`, `pom.xml`, `.github/scripts/test_summary.py`, its standard-library regression tests, the delivery plan, and this record. No application behavior, dependency versions, or test assertions were changed.
- **Coverage:** CI runs the existing npm scripts and all Java test suites in complementary groups. The integration group selects all remaining tests, including future tests not assigned to a specific group. A report check rejects missing or duplicated Java suites. Frontend and backend reports remain distinct. The `verify` and `image` job IDs, triggers, read-only workflow permissions, and image dependency are retained; human-readable job names are improved. No branch protection was changed.
- **Local commands:** Selected the already installed JDK 25 with `JAVA_HOME=$(/usr/libexec/java_home -v 25)`. Executed the workflow's shell steps in order, using the workflow's selectors and working directories, via Ruby's existing YAML parser and `bash -e -o pipefail`. Skipped only the Linux-specific Chromium system-library installation on macOS and the checkout/setup Actions; Chromium installation and all application verification/packaging commands ran.

  ```sh
  ./mvnw -B frontend:install-node-and-npm@install-node-and-npm
  # With frontend/node on PATH, from frontend:
  npm ci
  npm run typecheck
  npm test -- --reporter=default --reporter=junit --outputFile.junit=../target/test-results/frontend/junit.xml
  npm run build
  # From the repository root:
  ./mvnw -B -Dfrontend.skip=true test-compile
  # Each of the five backend groups uses its exact selector from ci.yml:
  ./mvnw -B dependency:properties surefire:test "-Dtest=$UNIT_TESTS" \
    -Dtest.reportsDirectory=target/test-results/backend/unit
  # Contract, architecture, integration and browser invocations: see ci.yml.
  ./mvnw -B exec:exec@install-playwright-chromium
  python3 -m unittest discover -s .github/scripts -p 'test_test_summary.py'
  python3 .github/scripts/test_summary.py --require-complete
  ./mvnw -B -Dfrontend.skip=true -DskipTests verify
  git diff --check
  ```

- **Observed application results:**

  | Group | Suites | Passed | Failed/errors | Skipped |
  |---|---:|---:|---:|---:|
  | Frontend unit | 6 | 31 | 0 | 0 |
  | Backend unit | 8 | 91 | 0 | 0 |
  | Backend contract | 2 | 23 | 0 | 0 |
  | Backend architecture | 2 | 9 | 0 | 0 |
  | Backend API and persistence integration | 24 | 539 | 0 | 0 |
  | Backend browser end-to-end | 1 | 46 | 0 | 0 |

- **Additional results:** Typecheck, frontend asset build, backend compilation and executable JAR packaging passed. All 37 Java suites produced exactly one report. Five report-generator tests passed, including failure/skipped results, missing reports, omitted/duplicated suites, and ignoring non-test helpers. The helper regression check was first observed failing and then passing. YAML parsing, workflow trigger/permission/dependency assertions, and `bash -n` for every workflow command passed.
- **Summary and confidentiality:** Suite counts and test-case names/statuses come from JUnit XML; captured logs, exception messages and environment properties are not copied into the summary or uploaded. Missing reports are explicitly marked unavailable, including after a failed pipeline step. Local raw reports and logs remain untracked/ignored or in the session workspace.
- **Limitations:** These are local results, not a hosted run of the refined workflow. The agent's `gh run view` request remained unauthenticated, and unauthenticated web access returned 404 for the supplied run. The Docker job was not rerun locally. The controlled-failure demonstration remains pending and is paused during this refinement; no deliberate failure is present.

### Approved refinement — Separate jobs

- **Request:** The user reported all four push/PR checks successful for the preceding workflow, then requested distinct jobs and removal of processor architecture details from the visible Docker check name. This is user-reported hosted evidence; no new run URL or SHA was supplied.
- **Changes:** Replaced the single verification job with `Compile frontend and backend`, `Frontend unit tests`, and three backend matrix jobs: `Backend unit tests`, `Backend integration, contract and architecture tests`, and `End-to-end tests`. The `Build Docker image` job requires compilation and every test job to succeed. Its coverage check must also pass before Docker runs. Both push and pull-request triggers, read-only permissions, the same test selectors and tool versions, and non-publishing image behavior remain.
- **Artifact approval:** The user explicitly approved sharing compiled classes/assets and sanitized test reports through one-day GitHub Actions artifacts. Upload paths exclude JARs, dependency directories, raw Surefire/Vitest reports, local configuration and logs. A new exporter reconstructs XML with only suite/test names and pass/fail/skip status; exception text, system output and report properties are omitted. Each test job also writes its own summary.
- **Local verification:** Created an ignored, fresh source snapshot at `target/ci-jobs-uA8WE1` using `git archive HEAD`, overlaid the changed workflow/scripts, and executed its compilation/frontend/backend shell commands with the installed JDK 25. Executed all test groups, Chromium installation, summary/completeness checks and quiet Compose validation. As in the preceding validation, GitHub-specific checkout/setup/artifact actions, Linux system-library installation and the Docker build were not executed locally.
- **Results:** Compilation and executable JAR packaging succeeded; 31 frontend tests and 708 backend tests passed, with no failures or skipped tests. All 43 application suites (6 frontend, 37 backend) passed the completeness check. Six standard-library generator tests passed; the sanitization test first failed before implementation. Exporting all actual reports and re-rendering them produced an identical summary, preserving all suite/test counts and statuses without raw diagnostics.
- **Workflow validation:** YAML parsing, every shell command's `bash -n`, push/PR triggers, minimal permissions, Docker's complete dependency list, matrix fail-fast disabled, and the exact `Build Docker image` name were checked locally. Artifact transfer itself and the split-job workflow require a hosted run; no success is claimed for those unexecuted Actions.
- **Remaining:** The controlled-failure demonstration is still pending; no deliberate failure was introduced. No branch protection or required-check configuration was changed. The ignored validation snapshot and its generated output were left in place rather than destructively cleaned up.

### Approved refinement — Push-only execution

- **Decision:** The user selected pushes to any branch only, with no separate pull-request event. Removed only the `pull_request` trigger; all jobs, test commands, image gating and non-publication behavior are unchanged. GitHub's synthetic PR merge result is no longer separately verified.
- **Local verification:** Parsed the old and new YAML and compared them after removing the old pull-request trigger; the remaining workflow is identical. Verified that the sole event is an unrestricted `push`, and `git diff --check` passed. No application tests were rerun for this trigger-only change.
- **Limitations:** No hosted result is claimed for this revision. No branch protection was changed, and no deliberate failure is present.

## Exercises 5–6

No execution evidence yet. Add a dated section for each exercise only after it is requested and run; include exact non-secret commands, outcomes, limitations, and checks not executed.
