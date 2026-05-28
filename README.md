## Tech Stack

Flowit main server는 Java 17, Spring Boot, MySQL, Redis, Spring REST Docs 기반으로 구성됩니다.<br>
Flowit main server is built with Java 17, Spring Boot, MySQL, Redis, and Spring REST Docs.

<details>
<summary>Tech stack details</summary>

### Backend

| Category | Technology        |
|---|-------------------|
| Language | Java 17           |
| Framework | Spring Boot       |
| API | Spring Web        |
| Realtime API | WebSocket         |
| Security | Spring Security   |
| Validation | Bean Validation   |
| ORM | Spring Data JPA   |
| Cache / Messaging | Spring Data Redis |
| Mail | Java Mail Sender  |

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

API 문서는 애플리케이션 기동 후 `/docs`에서 확인할 수 있습니다.<br>
You can view the API documentation at `/docs` after the application starts.

```text
http://localhost:8080/docs
http://localhost:8080/docs/index.html
```

`/docs`는 API 문서 목차 페이지입니다. API별 상세 문서는 목차에서 이동하십시오.<br>
`/docs` is the API documentation index page. Use the index to navigate to each API detail page.

예시 상세 문서 경로는 아래와 같습니다.<br>
An example detail page is available at:

```text
http://localhost:8080/docs/docs-preview.html
```

문서 산출물은 Spring REST Docs 테스트와 Asciidoctor를 통해 생성되며, 로컬 `start`, `localStart`, `bootRun`, `bootJar` 실행 시 애플리케이션 정적 리소스에 함께 복사됩니다.<br>
The documentation is generated through Spring REST Docs tests and Asciidoctor, then copied into application static resources when running local `start`, `localStart`, `bootRun`, or `bootJar`.

<details>
<summary>API documentation generation commands</summary>

```bash
./gradlew restdocsTest
```

`restdocsTest`는 API 문서에 사용할 Spring REST Docs 스니펫을 생성합니다.<br>
`restdocsTest` generates Spring REST Docs snippets for the API documentation.

```bash
./gradlew bootJar
```

`bootJar`는 스니펫을 기반으로 HTML 문서를 생성하고, 실행 가능한 JAR에 문서를 포함합니다.<br>
`bootJar` generates HTML documentation from snippets and includes it in the executable JAR.

</details>

## Local Docker Runtime

> [!WARNING]
>
> 필수: Docker Desktop을 반드시 실행하십시오.<br>
> Required: Docker Desktop must be running.

로컬 JDK 없이 실행하려면 프로젝트 루트에서 OS에 맞는 스크립트를 사용하십시오.<br>
Use the OS-specific script from the project root when you want to run without a local JDK.

```powershell
.\local.bat start
```

```bash
./local.sh start
```

Mac에서 `./local.sh` 실행 권한이 없다면 한 번만 아래 명령을 실행하십시오.<br>
If `./local.sh` is not executable on Mac, run the following command once.

```bash
chmod +x local.sh
```

로컬 JDK가 설치되어 있다면 기존 Gradle 명령도 계속 사용할 수 있습니다.<br>
If a local JDK is installed, the existing Gradle command remains supported.

```bash
./gradlew localStart
```

`start`와 `localStart`는 Spring Boot 애플리케이션 이미지를 Docker 안에서 빌드하고 MySQL, Redis, Prometheus, Grafana와 함께 로컬 환경을 실행합니다. JDK가 없는 환경에서는 `local.bat` 또는 `local.sh`가 Gradle Wrapper를 거치지 않고 Docker 명령을 직접 실행합니다.<br>
`start` and `localStart` build the Spring Boot application image inside Docker and start it with MySQL, Redis, Prometheus, and Grafana. In a JDK-free environment, `local.bat` or `local.sh` runs Docker commands directly without going through the Gradle Wrapper.

애플리케이션 Docker 이미지는 컨테이너 내부에서 Eclipse Temurin Java 17을 사용해 빌드 및 실행됩니다.<br>
The application Docker image is built and run with Eclipse Temurin Java 17 inside the container.

안전한 자동 소스 갱신을 사용하려면 fork 저장소가 아니라 조직 원본 저장소 `runtime-lab/flowit-main-server`를 clone해야 합니다. fork 또는 다른 remote에서 clone한 작업 트리는 자동 갱신 대상이 아니며, 스크립트는 경고만 출력하고 현재 소스로 계속 진행합니다.<br>
To use safe automatic source updates, clone the organization source repository `runtime-lab/flowit-main-server`, not a fork. Worktrees cloned from forks or other remotes are not eligible for automatic updates; the script prints a warning and continues with the current source.

이미지를 빌드하기 전 `local.bat start`, `local.bat build-image`, `./local.sh start`, `./local.sh build-image`는 허용된 소스만 확인합니다. 허용된 소스는 `origin`이 `https://github.com/runtime-lab/flowit-main-server.git` 또는 `git@github.com:runtime-lab/flowit-main-server.git`을 가리키고, 현재 브랜치가 `main`인 경우입니다. 브랜치가 뒤처져 있고 작업 트리가 깨끗하면 해당 브랜치만 가져와 fast-forward로 갱신합니다. remote/branch가 맞지 않거나, 로컬 변경이 있거나, 브랜치가 diverge 되었거나, Git을 사용할 수 없거나, fetch에 실패하면 경고만 출력하고 현재 소스로 계속 진행합니다. 이 소스 갱신 확인을 건너뛰려면 `FLOWIT_SKIP_AUTO_UPDATE=true`를 설정하십시오.<br>
Before `local.bat start`, `local.bat build-image`, `./local.sh start`, or `./local.sh build-image` builds the image, the script checks only the allowed source: `origin` pointing to `https://github.com/runtime-lab/flowit-main-server.git` or `git@github.com:runtime-lab/flowit-main-server.git`, on branch `main`. If the branch is behind and the worktree is clean, it fetches that branch and fast-forwards automatically. If the remote/branch does not match, local changes are present, the branch has diverged, Git is unavailable, or fetch fails, the script prints a warning and continues with the current source. Set `FLOWIT_SKIP_AUTO_UPDATE=true` to skip this source update check.

서버 환경과 혼동하기 쉬운 Linux에서는 기본적으로 차단됩니다. Linux 로컬 개발 환경에서만 아래처럼 명시적으로 허용하십시오.<br>
Linux is blocked by default because it is easy to confuse with server environments. Opt in only on a local Linux development machine as shown below.

```bash
FLOWIT_ALLOW_LOCAL_DOCKER=true ./local.sh start
```

이 명령들은 클라이언트/API 연동용 로컬 개발 환경 전용입니다. `FLOWIT_ENV=prod|production`, `SPRING_PROFILES_ACTIVE=prod|production`, `SPRING_PROFILES_INCLUDE=prod|production`, `CI=true`, `GITHUB_ACTIONS=true`, `GITLAB_CI=true`, `JENKINS_URL`, `KUBERNETES_SERVICE_HOST` 같은 운영 또는 CI 신호가 있으면 `FLOWIT_ALLOW_LOCAL_DOCKER=true`가 설정되어 있어도 실행이 차단됩니다.<br>
These commands are only for local client/API integration development. When production or CI signals such as `FLOWIT_ENV=prod|production`, `SPRING_PROFILES_ACTIVE=prod|production`, `SPRING_PROFILES_INCLUDE=prod|production`, `CI=true`, `GITHUB_ACTIONS=true`, `GITLAB_CI=true`, `JENKINS_URL`, or `KUBERNETES_SERVICE_HOST` are present, execution is blocked even if `FLOWIT_ALLOW_LOCAL_DOCKER=true` is set.

상태와 로그는 아래 명령으로 확인합니다.<br>
Use the following commands to check status and logs.

```powershell
.\local.bat status
.\local.bat logs
```

```bash
./local.sh status
./local.sh logs
```

Gradle을 사용하는 경우에는 아래 명령도 가능합니다.<br>
When using Gradle, these commands are also available.

```bash
./gradlew localStatus
docker compose logs -f app
```

중지할 때는 아래 명령을 사용합니다. Docker 볼륨은 유지됩니다.<br>
Use the following command to stop the containers. Docker volumes are preserved.

```powershell
.\local.bat stop
```

```bash
./local.sh stop
```

<details>
<summary>Startup time notes</summary>

최초 실행 또는 Docker 이미지와 Gradle 의존성을 새로 내려받아야 하는 경우 `local.bat start`, `./local.sh start`, `./gradlew localStart`가 몇 분 정도 걸릴 수 있습니다.<br>
The first run, or any run that needs to download Docker images and Gradle dependencies again, can make `local.bat start`, `./local.sh start`, or `./gradlew localStart` take a few minutes.

빌드 명령은 `--progress=plain` 옵션을 사용하므로 Dockerfile 단계, Gradle 다운로드, `bootJar` 실행 상태가 터미널에 출력됩니다.<br>
The build command uses `--progress=plain`, so Dockerfile steps, Gradle downloads, and `bootJar` execution status are printed in the terminal.

Docker Desktop에서 컨테이너 uptime이 유지되고 있다면 애플리케이션 컨테이너는 계속 실행 중인 상태입니다. 이때 터미널 명령은 이미지 빌드, Compose 상태 확인, health 확인 단계에 있을 수 있습니다.<br>
If the container uptime continues in Docker Desktop, the application container is still running. In that case, the terminal command can be spending time on image build, Compose status checks, or health checks.

로컬 시작 명령은 `src/main`, `src/docs`, REST Docs 테스트, REST Docs 템플릿, Gradle 설정, Docker 설정의 소스 해시가 기존 `flowit-main-server:local` 이미지와 같으면 이미지를 재사용하고, 달라지면 자동으로 다시 빌드합니다. 강제로 다시 빌드해야 할 때는 `local.bat build-image`, `./local.sh build-image`, 또는 `./gradlew localBuildImage`를 실행하십시오.<br>
Local start commands reuse the existing `flowit-main-server:local` image when the source hash for `src/main`, `src/docs`, REST Docs tests, REST Docs templates, Gradle settings, and Docker settings matches the image, and rebuild it automatically when the hash changes. Run `local.bat build-image`, `./local.sh build-image`, or `./gradlew localBuildImage` when you need to force a rebuild.

Windows에서는 JDK 없이 실행할 때 `local.bat`를 사용하면 Gradle Wrapper 기동을 건너뛰고 PowerShell 기반 Docker 명령만 실행합니다. Mac에서는 `./local.sh start`를 사용하십시오.<br>
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

`build-image`와 `localBuildImage`는 애플리케이션 이미지만 다시 빌드하며, Docker build 진행 상황을 터미널에 출력합니다.<br>
`build-image` and `localBuildImage` rebuild only the application image and print Docker build progress in the terminal.

```bash
./local.sh status
./gradlew localStatus
```

```powershell
.\local.bat status
```

`localStatus`는 actuator health 상태와 Docker Compose 서비스 상태를 출력합니다.<br>
`localStatus` shows the actuator health status and Docker Compose service status.

```bash
./local.sh stop
./gradlew localStop
```

```powershell
.\local.bat stop
```

`localStop`은 `docker compose down`을 실행합니다. Docker 볼륨은 유지됩니다.<br>
`localStop` runs `docker compose down`. Docker volumes are preserved.

Mac에서 `./gradlew` 실행 권한이 없다면 아래 명령도 한 번 실행하십시오.<br>
If `./gradlew` is not executable on Mac, also run the following command once.

```bash
chmod +x gradlew
```

```bash
./local.sh infra-start
./gradlew localInfraStart
```

```powershell
.\local.bat infra-start
```

`localInfraStart`는 Spring Boot 애플리케이션 없이 MySQL, Redis, Prometheus, Grafana만 실행합니다.<br>
`localInfraStart` starts only MySQL, Redis, Prometheus, and Grafana without the Spring Boot application.

```bash
./local.sh infra-stop
./gradlew localInfraStop
```

```powershell
.\local.bat infra-stop
```

`localInfraStop`은 인프라 서비스 컨테이너만 중지합니다. Docker 볼륨은 유지됩니다.<br>
`localInfraStop` stops only the infrastructure service containers. Docker volumes are preserved.

로컬 스크립트와 Gradle 로컬 명령어는 OS에 맞는 Docker Compose 설정을 자동으로 선택합니다.<br>
The local scripts and Gradle local commands automatically select the Docker Compose configuration for the current OS.

- Windows/Mac: `compose.yaml`
- Linux: `compose.yaml` + `compose.linux.yaml`

</details>

<details>
<summary>Linux Docker command</summary>

Linux에서는 `host.docker.internal`을 사용하기 위해 override 파일을 함께 지정하십시오.<br>
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

Redis 컨테이너는 로컬 개발 환경에서 캐시 및 메시징 기능을 사용하기 위해 함께 생성됩니다.<br>
The Redis container is created together for local cache and messaging features.

Prometheus 컨테이너는 Spring Boot Actuator metrics를 수집하고, Grafana 컨테이너는 Prometheus datasource가 자동 등록된 상태로 생성됩니다.<br>
The Prometheus container collects Spring Boot Actuator metrics, and the Grafana container is created with the Prometheus datasource provisioned.

애플리케이션, MySQL, Redis, Prometheus, Grafana의 호스트 포트는 모두 `127.0.0.1`에만 바인딩되므로 로컬에서만 접근할 수 있습니다.<br>
The host ports for the application, MySQL, Redis, Prometheus, and Grafana are bound only to `127.0.0.1`, so they are accessible only from the local machine.

컨테이너 내부에서는 명시적인 bridge network인 `flowit-local`을 통해 `app`, `mysql`, `redis`, `prometheus` 서비스명으로 통신합니다.<br>
Inside Docker, services communicate through the explicit `flowit-local` bridge network using service names such as `app`, `mysql`, `redis`, and `prometheus`.

Spring Boot Actuator는 컨테이너 내부에서 `0.0.0.0`에 바인딩되어 Prometheus가 bridge network로 metrics를 수집할 수 있고, 호스트 노출은 Docker 포트 바인딩으로 `127.0.0.1`에 제한됩니다.<br>
Spring Boot Actuator binds to `0.0.0.0` inside the container so Prometheus can scrape metrics over the bridge network, while host exposure remains limited to `127.0.0.1` through Docker port binding.

</details>

<details>
<summary>Container maintenance commands</summary>

### Recreate container

초기화 SQL을 다시 실행하거나 로컬 데이터베이스, Redis, Prometheus, Grafana 데이터를 초기 상태로 되돌려야 할 때 사용하십시오.<br>
Use this when you need to re-run the initialization SQL or reset the local database, Redis, Prometheus, and Grafana data.

```bash
docker compose down -v
docker build --progress=plain -t flowit-main-server:local -f Dockerfile .
docker compose up -d
```

`-v` 옵션은 MySQL, Redis, Prometheus, Grafana 데이터 볼륨을 삭제하므로 기존 로컬 데이터가 모두 제거됩니다.<br>
The `-v` option removes the MySQL, Redis, Prometheus, and Grafana data volumes, so all existing local data will be deleted.

### Stop container

컨테이너를 중지할 때 사용하십시오. 데이터 볼륨은 유지되므로 로컬 데이터는 보존됩니다.<br>
Use this to stop the containers. The data volumes are preserved, so local data will remain.

```bash
docker compose down
```

</details>

<details>
<summary>Backend dependency maintenance</summary>

의존성 공급망은 Gradle dependency verification과 Docker image digest pinning으로 고정합니다. 의존성 또는 이미지 버전을 갱신할 때는 verification metadata와 digest를 함께 갱신하고, Dependabot PR은 자동 병합하지 말고 테스트 후 수동으로 병합하십시오. GitHub 저장소에서는 Dependency graph, Dependabot alerts, Dependabot security updates를 활성화하십시오.<br>
The dependency supply chain is pinned with Gradle dependency verification and Docker image digest pinning. When updating dependencies or image versions, update the verification metadata and digests together, and merge Dependabot pull requests manually after testing. Enable Dependency graph, Dependabot alerts, and Dependabot security updates in the GitHub repository settings.

> [!WARNING]
>
> Backend Only: 의존성을 추가하거나 버전을 변경하는 백엔드 개발자는 metadata 작성 옵션을 붙여 빌드 artifact와 IntelliJ가 요청하는 dependency source JAR을 함께 갱신한 다음, 변경된 metadata를 검토하고 일반 빌드를 다시 실행하십시오.<br>
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
> DDL 및 ALTER 문은 Flyway Migration 라이브러리를 통해 애플리케이션 실행 시 데이터베이스에 자동으로 반영됩니다.<br>
> DDL and ALTER statements are automatically applied to the database through the Flyway Migration library when the application runs.
>
> 별도의 SQL 파일을 데이터베이스에 강제로 실행하지 마십시오.<br>
> Do not force-run separate SQL files directly against the database.
