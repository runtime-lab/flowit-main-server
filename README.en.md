# Flowit Main Server

API Version: 0.2.14-volt · up-to-date

[Korean](README.md)

## Tech Stack

Flowit main server is built with Java 17, Spring Boot, MySQL, Redis, and Spring REST Docs.

<details>
<summary>Tech stack details</summary>

### Backend

| Category | Technology |
|---|---|
| Language | Java 17 |
| Framework | Spring Boot |
| API | Spring Web |
| Realtime API | WebSocket |
| Security | Spring Security |
| Validation | Bean Validation |
| ORM | Spring Data JPA |
| Cache / Messaging | Spring Data Redis |
| Mail | Java Mail Sender |

### Database

| Category | Technology |
|---|---|
| RDBMS | MySQL |
| Migration | Flyway Migration |

### Observability

| Category | Technology |
|---|---|
| Health Check / Metrics | Spring Boot Actuator |
| Metrics Collection | Prometheus |
| Metrics Visualization | Grafana |
| Error Monitoring | Sentry |

### Documentation

| Category | Technology |
|---|---|
| API Documentation | Spring REST Docs |

### Development

| Category | Technology |
|---|---|
| Boilerplate Reduction | Lombok |
| Developer Tooling | Spring Boot DevTools |

</details>

## API Documentation

You can view the API documentation at `/docs` after the application starts.

```text
http://localhost:8080/docs
http://localhost:8080/docs/index.html
```

`/docs` is the API documentation index page. Use the index to navigate to each API detail page.

An example detail page is available at:

```text
http://localhost:8080/docs/docs-preview.html
```

The documentation is generated through Spring REST Docs tests and Asciidoctor, then copied into application static resources when running local `start`, `localStart`, `bootRun`, or `bootJar`.

<details>
<summary>API documentation generation commands</summary>

```bash
./gradlew restdocsTest
```

`restdocsTest` generates Spring REST Docs snippets for the API documentation.

```bash
./gradlew bootJar
```

`bootJar` generates HTML documentation from snippets and includes it in the executable JAR.

</details>

## Local Docker Runtime

> [!WARNING]
>
> Required: Docker Desktop must be running.

Use the OS-specific script from the project root when you want to run without a local JDK.

```powershell
.\local.bat start
```

```bash
./local.sh start
```

This repository tracks the executable bits for `local.sh` and `gradlew` for Mac/Linux usage. If you see a permission error, first make sure your local `main` branch is up to date.

If a local JDK is installed, the existing Gradle command remains supported.

```bash
./gradlew localStart
```

`start` and `localStart` build the Spring Boot application image inside Docker and start it with MySQL, Redis, Prometheus, and Grafana. In a JDK-free environment, `local.bat` or `local.sh` runs Docker commands directly without going through the Gradle Wrapper.

The application Docker image is built and run with Eclipse Temurin Java 17 inside the container.

To use safe automatic source updates, clone the organization source repository `runtime-lab/flowit-main-server`, not a fork. Worktrees cloned from forks or other remotes are not eligible for automatic updates and continue with the current source without prompting.

Before `local.bat start`, `local.bat build-image`, `./local.sh start`, or `./local.sh build-image` builds the image, the script checks only the allowed source: `origin` pointing to `https://github.com/runtime-lab/flowit-main-server.git` or `git@github.com:runtime-lab/flowit-main-server.git`. If the allowed source is configured but the current branch is not `main`, the script stops with an error. If `main` is behind and the worktree is clean, it fetches that branch and fast-forwards automatically. If fetch, comparison, status inspection, or merge fails on the allowed `main`, or local changes or divergence prevent the update, the script asks whether to continue with the current source. The default answer is no. Set `FLOWIT_SKIP_AUTO_UPDATE=true` to skip this source update check.

Linux is blocked by default because it is easy to confuse with server environments. Opt in only on a local Linux development machine as shown below.

```bash
FLOWIT_ALLOW_LOCAL_DOCKER=true ./local.sh start
```

These commands are only for local client/API integration development. When production or CI signals such as `FLOWIT_ENV=prod|production`, `SPRING_PROFILES_ACTIVE=prod|production`, `SPRING_PROFILES_INCLUDE=prod|production`, `CI=true`, `GITHUB_ACTIONS=true`, `GITLAB_CI=true`, `JENKINS_URL`, or `KUBERNETES_SERVICE_HOST` are present, execution is blocked even if `FLOWIT_ALLOW_LOCAL_DOCKER=true` is set.

Use the following commands to check status and logs.

```powershell
.\local.bat status
.\local.bat logs
```

```bash
./local.sh status
./local.sh logs
```

When using Gradle, these commands are also available.

```bash
./gradlew localStatus
docker compose logs -f app
```

Use the following command to stop the containers. Docker volumes are preserved.

```powershell
.\local.bat stop
```

```bash
./local.sh stop
```

<details>
<summary>Startup time notes</summary>

The first run, or any run that needs to download Docker images and Gradle dependencies again, can make `local.bat start`, `./local.sh start`, or `./gradlew localStart` take a few minutes.

The build command uses `--progress=plain`, so Dockerfile steps, Gradle downloads, and `bootJar` execution status are printed in the terminal.

If the container uptime continues in Docker Desktop, the application container is still running. In that case, the terminal command can be spending time on image build, Compose status checks, or health checks.

Local start commands reuse the existing `flowit-main-server:local` image when the source hash for `src/main`, `src/docs`, REST Docs tests, REST Docs templates, Gradle settings, and Docker settings matches the image, and rebuild it automatically when the hash changes. Run `local.bat build-image`, `./local.sh build-image`, or `./gradlew localBuildImage` when you need to force a rebuild.

On Windows, `local.bat` skips Gradle Wrapper startup and runs PowerShell-based Docker commands when running without a JDK. On Mac, use `./local.sh start`.

</details>

<details>
<summary>Additional local commands</summary>

```powershell
.\local.bat build-image
```

```bash
./local.sh build-image
./gradlew localBuildImage
```

`build-image` and `localBuildImage` rebuild only the application image and print Docker build progress in the terminal.

```bash
./local.sh status
./gradlew localStatus
```

```powershell
.\local.bat status
```

`localStatus` shows the actuator health status and Docker Compose service status.

```bash
./local.sh stop
./gradlew localStop
```

```powershell
.\local.bat stop
```

`localStop` runs `docker compose down`. Docker volumes are preserved.

```bash
./local.sh infra-start
./gradlew localInfraStart
```

```powershell
.\local.bat infra-start
```

`localInfraStart` starts only MySQL, Redis, Prometheus, and Grafana without the Spring Boot application.

```bash
./local.sh infra-stop
./gradlew localInfraStop
```

```powershell
.\local.bat infra-stop
```

`localInfraStop` stops only the infrastructure service containers. Docker volumes are preserved.

The local scripts and Gradle local commands automatically select the Docker Compose configuration for the current OS.

- Windows/Mac: `compose.yaml`
- Linux: `compose.yaml` + `compose.linux.yaml`

</details>

<details>
<summary>Linux Docker command</summary>

On Linux, include the override file so `host.docker.internal` is mapped correctly.

```bash
docker build --progress=plain -t flowit-main-server:local -f Dockerfile .
docker compose -f compose.yaml -f compose.linux.yaml up -d
```

</details>

<details>
<summary>Local development endpoints</summary>

### Application

- Host: localhost
- Port: 8080

### Database

- Host: localhost
- Port: 3306
- Database: project_flowit
- Username: flowit_dev
- Password: flowitDevPass

### Redis

- Host: localhost
- Port: 6379
- Password: flowitLocalDev

### Actuator

- Host: localhost
- Port: 8081
- Binding: 127.0.0.1
- Exposed endpoints: health, prometheus

### Prometheus

- Host: localhost
- Port: 9090
- Binding: 127.0.0.1
- Scrape target: app:8081/actuator/prometheus

### Grafana

- Host: localhost
- Port: 3050
- Binding: 127.0.0.1
- Username: admin
- Password: flowitLocalAdmin

The Redis container is created together for local cache and messaging features.

The Prometheus container collects Spring Boot Actuator metrics, and the Grafana container is created with the Prometheus datasource provisioned.

The host ports for the application, MySQL, Redis, Prometheus, and Grafana are bound only to `127.0.0.1`, so they are accessible only from the local machine.

Inside Docker, services communicate through the explicit `flowit-local` bridge network using service names such as `app`, `mysql`, `redis`, and `prometheus`.

Spring Boot Actuator binds to `0.0.0.0` inside the container so Prometheus can scrape metrics over the bridge network, while host exposure remains limited to `127.0.0.1` through Docker port binding.

</details>

<details>
<summary>Container maintenance commands</summary>

### Recreate container

Use this when you need to re-run the initialization SQL or reset the local database, Redis, Prometheus, and Grafana data.

```bash
docker compose down -v
docker build --progress=plain -t flowit-main-server:local -f Dockerfile .
docker compose up -d
```

The `-v` option removes the MySQL, Redis, Prometheus, and Grafana data volumes, so all existing local data will be deleted.

### Stop container

Use this to stop the containers. The data volumes are preserved, so local data will remain.

```bash
docker compose down
```

</details>

<details>
<summary>Backend dependency maintenance</summary>

The dependency supply chain is pinned with Gradle dependency verification and Docker image digest pinning. When updating dependencies or image versions, update the verification metadata and digests together, and merge Dependabot pull requests manually after testing. Enable Dependency graph, Dependabot alerts, and Dependabot security updates in the GitHub repository settings.

> [!WARNING]
>
> Backend Only: Backend developers who add dependencies or change dependency versions should update verification metadata for both build artifacts and dependency source JARs requested by IntelliJ, review the metadata diff, then run a normal build again.
>
> ```bash
> ./gradlew --write-verification-metadata sha256 build
> ./gradlew --write-verification-metadata sha256 resolveDependencySources
> ./gradlew build
> ```

</details>

## Flyway Migration

> [!WARNING]
>
> DDL and ALTER statements are automatically applied to the database through the Flyway Migration library when the application runs.
>
> Do not force-run separate SQL files directly against the database.
