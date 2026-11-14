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

## Exercises 2–6

No execution evidence yet. Add a dated section for each exercise only after it is requested and run; include exact non-secret commands, outcomes, limitations, and checks not executed.

## Exercise 2 — Docker containerization

- **Date:** 2026-11-14
- **Branch/base:** `delivery-preparation`, based on Exercise 1 commit `25d1a8c`.
- **Files changed:** `Dockerfile` and `.dockerignore`. The Dockerfile uses digest-pinned Temurin 25 Noble JDK/JRE multi-platform images, builds with the Maven Wrapper, and copies only the executable JAR to a non-root JRE runtime. `unzip` is installed in the builder stage so the wrapper can verify its pinned Maven ZIP checksum.
- **Commands and results:**
  - `docker version --format 'Docker client {{.Client.Version}}' && docker buildx version && docker info --format 'Docker server {{.ServerVersion}}'` — Docker client/server 28.3.2 and Buildx 0.25.0 were available.
  - `docker buildx build --check .` — completed with no warnings before and after the builder-only `unzip` addition; the final run includes the `.m2` context exclusions.
  - `GIT_SHA=$(git rev-parse --short HEAD) && docker buildx build --no-cache --platform linux/amd64,linux/arm64 --progress plain --output=type=oci,dest=/tmp/matriz-ex2-candidate.oci.tar --tag matriz:0.1.0-SNAPSHOT-$GIT_SHA .` — failed because the clean JDK image lacked `unzip`; the wrapper selected TAR.GZ while the configured checksum is for the ZIP.
  - After adding `unzip`, `GIT_SHA=$(git rev-parse --short HEAD) && docker buildx build --no-cache --platform linux/amd64,linux/arm64 --progress plain --output=type=oci,dest=/tmp/matriz-ex2-candidate-success.oci.tar --tag matriz:0.1.0-SNAPSHOT-$GIT_SHA .` — reached the integrated frontend build but failed downloading Node.js 24.22.2 from `nodejs.org` with Java `PKIX path building failed` (`certificate_unknown`). The Maven distribution SHA-256 was independently confirmed to match `.mvn/wrapper/maven-wrapper.properties`; no TLS verification was disabled.
  - `git check-ignore -v .env.delivery` — confirmed the isolated env file is ignored. It contains generated fictitious values and was not printed.
- **Certificate investigation:** Read-only inspection of QBS, WTP, FBP and BLE workflows found their Maven builds use `/etc/pki/ca-trust/extracted/java/cacerts` on `centos-self-hosted` runners. QBS Dockerfiles copy a previously built JAR and do not resolve this in a Docker builder. The local Docker JDK does not trust the local Zscaler interception CA. Per user direction, no CA certificate was exported or mounted, no TLS validation was bypassed, and nothing in `Flutter-repos` was modified.
- **Not completed:** No OCI image was produced. Compose/PostgreSQL and application containers were not started; database readiness/migrations, HTTP process health, static assets, sign-in, project/task operations, missing-configuration startup failure, and image metadata/history checks were not run.
- **Limitations/blocker:** The approved clean in-Docker build is blocked in this environment by Java trust of the HTTPS proxy certificate for the Node.js download. The user declined using a temporary BuildKit CA secret. The Dockerfile has not been verified by a successful build; a temporary zero-byte OCI output may remain at `/tmp/matriz-ex2-candidate.oci.tar`. No images were pushed or published.
- **Review:** `git diff --check` passed. Only `.dockerignore`, `Dockerfile`, and this evidence section are in Exercise 2 scope.
