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

## Exercises 4–6

No execution evidence yet. Add a dated section for each exercise only after it is requested and run; include exact non-secret commands, outcomes, limitations, and checks not executed.
