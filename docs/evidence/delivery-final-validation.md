# Final delivery validation — Exercise 6

**Status:** Local full-build and supplementary host-run validation completed; Docker results are user-reported. Final hosted CI inspection and PR update remain pending. This is not client acceptance or production-readiness approval.

## Revision and isolation

| Item | Observed value |
|---|---|
| Execution date | 2026-11-16 UTC; plan approved on 2026-11-15 |
| Tested source | `f3f0e54a9059abe76407a260710ffa656dc86dc1`, branch `delivery-preparation` |
| Delivery review base | `bf8eb12f827cd23da567b88cf33e128dab7d48c7` |
| Clean committed copy | `target/matriz-ex6-20261115-01` in the original checkout; cloned with `--no-hardlinks --branch delivery-preparation` |
| Private configuration | New ignored `.env.delivery`, mode `600`, generated fictitious passwords and random signing key; never displayed or copied from another environment |
| Compose project | `matriz-ex6-20261115-01` |
| Runtime actually checked | Integrated JAR on the host plus `matriz-ex6-20261115-01-postgres-1`; no local app container |
| Database volume | `matriz-ex6-20261115-01_matriz-db`, mount `/var/lib/postgresql` |
| Ports | `127.0.0.1:18089` application, `127.0.0.1:15432` database; both free at preflight |
| Tools | Docker Engine 28.3.2, Compose v2.38.2-desktop.1, installed Temurin JDK 25.0.5 |

No existing `.env`, application data, container or volume was reused. No new system tool was installed. The user subsequently instructed that versions/build configuration be left unchanged and reported Docker working in other environments. Supplementary runtime checks therefore used the README's existing host-run JAR path with the isolated Compose database, not an undocumented image or a replacement Dockerfile. That deviation does not establish app-container behavior.

## Checks and results

| Check | Status | Evidence / boundary |
|---|---|---|
| Clean committed source and isolated configuration | Passed | Clone HEAD matches tested SHA; tracked working tree clean before and after the build |
| Compose configuration | Passed | Quiet validation of default and `app` profiles; default service listing is only `postgres` |
| Full documented Maven verification | Passed | `./mvnw -B clean verify`: BUILD SUCCESS, 3 min 45 s; 708 Java tests in 37 suites, zero failures/errors/skips; Vitest 31 tests in 6 files passed |
| Browser end-to-end suite | Passed, automated test environment | 46 `UserInterfaceTests` included in the 708 Java tests; these run against test-owned application/database instances, not the Compose candidate |
| CI summary-generator guards | Passed | `python3 -m unittest discover -s .github/scripts -p 'test_test_summary.py'`: 6 tests passed |
| Docker image / full-app Compose | User-reported passing elsewhere; local verification incomplete | The local image-build attempt did not complete. No new successful local image build or app-container startup is claimed; user requested proceeding without a build change |
| Host-run startup, health and communication | Passed | PostgreSQL healthy; `/actuator/health` reports application/database UP; UI assets return 200; protected API without a token returns 401; other Actuator endpoints return 404 |
| Direct-API operations and authorization | Passed, supplementary host-run app | 45 HTTP status assertions: project/task create/find/edit/delete; state, priority, filters/counts, assignment/unassignment, comments, sign-out; Ana owner, Rui participant, Daniel non-member |
| Browser reload | Passed, automated Chromium smoke | Sign-in, retained task/assignee/comment visible; reload requires sign-in again and returns to the same task URL/data |
| Persistence after database-container recreation | Passed, supplementary host-run app | Stopped only the owned host JVM; recreated only the approved PostgreSQL test container; container ID changed and exact named volume stayed identical; restarted the same JAR, then repeated 12 API assertions and browser smoke |
| Application-container recreation | Not run | Application was a host JVM, not a local app container; the database-container result must not be presented as recreation of both services |
| Runtime logs | Passed within inspected scope | Inspected first/restarted application logs and bounded PostgreSQL logs: no WARN/ERROR/FATAL/PANIC lines or generated credentials; database logs confirm existing data directory and skipped initialization |
| Tracked-file completeness | Passed | 326 tracked files; source, frontend, tests, build/wrapper, six schema migrations, demo migration, config example, contract and candidate docs present |
| Targeted credential checks | Passed within stated scope | Generated isolated secrets and selected private-key/token signatures absent from tracked files; not a comprehensive guarantee that no possible credential exists |
| Packaged JAR checks | Passed within stated scope | Frontend index/assets and schema bundled; no `.env` entries or generated isolated secrets in 419 entries and nested dependency JAR contents |
| Image metadata/history/layers and OCI export | Not run | No completed candidate image or OCI archive available |
| Final delivery diff | Reviewed | Docker/Compose/build/workflow/scripts/docs reviewed against recorded base; independent read-only review reported no configuration/code defect, but correctly requested the then-missing Exercise 6 evidence |
| Documentation consistency | Passed within checked scope | 421 relative links/anchors across six delivery documents; unchanged version mapping; `git diff --check`; no generated runtime secrets in reviewed documents |
| Hosted CI and existing PR | Blocked: agent authentication | `gh run list` cannot authenticate; user reported retry success, but no independently inspected current run URL or PR update is claimed |

No manual browser walkthrough is claimed. Historical user acceptance of Exercises 2–4 and the reported CI retry remain distinct from this fresh execution. No application, runtime configuration, dependency or version correction was retained.

## Commands executed

From the original checkout:

```sh
docker version --format 'Docker client {{.Client.Version}}; server {{.Server.Version}}'
docker compose version
git clone --quiet --no-hardlinks --branch delivery-preparation . target/matriz-ex6-20261115-01
python3 -m unittest discover -s .github/scripts -p 'test_test_summary.py'
```

In the clean copy, after generating the separate configuration from `.env.example` with restrictive permissions, fresh random values, demo mode selected and the ports above:

```sh
git status --short
git rev-parse HEAD
git check-ignore .env.delivery
env -i PATH="$PATH" HOME="$HOME" \
  docker compose --env-file .env.delivery -p matriz-ex6-20261115-01 config -q
env -i PATH="$PATH" HOME="$HOME" \
  docker compose --env-file .env.delivery -p matriz-ex6-20261115-01 config --services
env -i PATH="$PATH" HOME="$HOME" \
  docker compose --env-file .env.delivery -p matriz-ex6-20261115-01 --profile app config -q
env -i PATH="$PATH" HOME="$HOME" \
  docker compose --env-file .env.delivery -p matriz-ex6-20261115-01 --profile app build app
env -i PATH="$PATH" HOME="$HOME" JAVA_HOME="$(/usr/libexec/java_home -v 25)" \
  ./mvnw -B clean verify
```

The empty child environment prevents developer `MATRIZ_`/`SPRING_` overrides affecting the documented setup; it does not import developer secrets. Java was selected using the README's macOS procedure. No undocumented successful-build workaround was used.

Additional standard-library checks parsed Surefire XML, inspected tracked paths and JAR ZIP entries (including nested JARs), compared generated test-secret bytes without printing matches, and confirmed the ports could be bound before startup.

Local raw build/test logs are retained outside Git in session artifacts `ex6-compose-build.log` and `ex6-clean-verify.log`. They are not uploaded or committed.

## Supplementary runtime execution

The host-run JAR used the generated `.env.delivery` settings injected into its child-process environment, not the developer environment. Its command was `java -jar target/matriz-0.1.0-SNAPSHOT.jar --server.port=18089`, using installed JDK 25. Demo mode was selected before the first database startup.

```sh
docker compose --env-file .env.delivery -p matriz-ex6-20261115-01 up -d --wait postgres
docker compose --env-file .env.delivery -p matriz-ex6-20261115-01 ps
docker compose --env-file .env.delivery -p matriz-ex6-20261115-01 exec -T postgres \
  pg_isready -U matriz_ex6 -d matriz
curl --silent --show-error --fail-with-body --retry 30 --retry-connrefused \
  --retry-delay 2 --max-time 5 http://127.0.0.1:18089/actuator/health
curl --silent --show-error --fail-with-body --output /dev/null http://127.0.0.1:18089/
./mvnw -q dependency:build-classpath \
  -Dmdep.outputFile=target/ex6-classpath.txt -Dmdep.includeScope=test
```

Standard-library Python API checks and a Java source-launcher Chromium check used the existing Playwright dependency, with no new package installation. Both harnesses are retained outside Git in session artifacts `ex6-api-smoke.py` and `Ex6BrowserSmoke.java`. From the clean-copy root, with `ARTIFACTS` set to that session's `files` directory:

```sh
python3 "$ARTIFACTS/ex6-api-smoke.py" prepare
"$(/usr/libexec/java_home -v 25)/bin/java" \
  --class-path "$(cat target/ex6-classpath.txt)" "$ARTIFACTS/Ex6BrowserSmoke.java"
python3 "$ARTIFACTS/ex6-api-smoke.py" verify
```

The preparation phase passed 45 HTTP status assertions; each verification phase passed 12, comparing complete retained project/task/comment/member/list responses and checking deleted items remain 404. The retained project is `4523ffa7-345a-4499-988f-3f6608b7591c`; task `cbae92ff-0010-4c7d-8d39-069e03b65b3e`.

After stopping only the owned host JVM, ran:

```sh
docker compose --env-file .env.delivery -p matriz-ex6-20261115-01 \
  up -d --force-recreate --wait postgres
```

An in-memory inspection compared only container IDs and volume names, not environment values: ID changed, volume `matriz-ex6-20261115-01_matriz-db` stayed the same. After restarting the same JAR with the same configuration, repeated HTTP health, API verification and Chromium smoke successfully. Flyway history remained seven successful entries in order `1 / 1.1 / 2 / 3 / 4 / 5 / 6`; PostgreSQL logged that initialization was skipped.

Bounded database logs were obtained with `docker compose --env-file .env.delivery -p matriz-ex6-20261115-01 logs --no-color --tail 150 postgres`, captured privately, checked against generated secrets and sanitized before inspection. Runtime log checks were repeated after stopping the owned JVM; the application logs still contained no warning/error-level entries.

The inspected JAR SHA-256 is `17d342ba059252309a4de86afe800ce3e2ca3448356a3fcf734e95dfffc0a809`. This identifies the local tested artifact, not a bit-for-bit reproducibility guarantee.

## Resources and review boundaries

The owned host JVM was stopped after checks. The isolated PostgreSQL container remains running, with its network and volume retained; no cleanup approval was assumed. The clean copy, private configuration, generated build outputs, Docker build cache and diagnostic image data remain local and uncommitted. No container, volume or image was removed for cleanup.

At the user's request, compared build-version declarations in `flutter-repos` read-only. Nothing there was changed or executed, and no private artifact, image or credential was imported. No version or build-strategy change was retained.

## Test-log observations

The passing test run was not free of warning/error-level log entries:

- Invalid HTTP method warnings and missing/invalid environment or conflicting demo-migration startup failures occur in negative-path tests. These are not evidence of candidate runtime startup failure.
- `TokenCheckFailureTests` intentionally throws a fictitious database error to assert a safe 500 response; error logs from this path are expected.
- Three Tomcat `Socket accept failed` messages with `AsynchronousCloseException` were observed around test-server shutdown, including the two application contexts of `DataAfterRestartTests`. All test assertions passed; no production correction or log suppression was made. Their presence remains recorded, not relabelled as clean runtime logs.

## Candidate identity and remaining gates

The approved mapping remains [candidate `0.1.0-mvp-rc.2`](../releases/0.1.0-mvp-rc.2.md): Maven `0.1.0-SNAPSHOT`, private frontend `0.1.0`, OpenAPI `0.2.0-draft`. No version files changed.

Outstanding independently verified gates are the full-app container/image checks and observable hosted CI/PR handoff. The user instructed proceeding with their Docker confirmation rather than a local build correction; those results remain explicitly user-reported.

`gh run list` could not authenticate, and the alternative GitHub integration could not access the repository. No token was requested or extracted. The handoff must identify the tested source SHA above separately from the documentation-only evidence commit; its final hash is obtained from Git after commit. A hosted run URL and PR update must be supplied or confirmed through an authenticated environment.

From an authenticated checkout after the evidence push, the human handoff commands are:

```sh
DELIVERY_SHA=$(git rev-parse HEAD)
gh run list --branch delivery-preparation --commit "$DELIVERY_SHA" --limit 3 \
  --json databaseId,headSha,conclusion,url
gh run view <run-id> --json headSha,conclusion,jobs,url
gh pr list --head delivery-preparation --base main --state open \
  --json number,url
```

Use the existing PR only. Its update should link this report, identify both SHAs and the actual final run, retain the user-reported versus agent-observed distinction, and state the remaining limitations. No merge, tag or release command is authorized by this handoff.

I12 remains Awaiting review. Client scope validation (OQ-016), hosting/operations (OQ-020), session policy (OQ-027) and soft-deletion retention (OQ-023) remain open in the [constraints register](../constraints.md#open-questions). The earlier controlled CI failure/recovery demonstration remains unexecuted. No merge, tag, image/release publication or deployment occurred.
