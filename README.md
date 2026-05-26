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

문서 산출물은 Spring REST Docs 테스트와 Asciidoctor를 통해 생성되며, `localStart`, `bootRun`, `bootJar` 실행 시 애플리케이션 정적 리소스에 함께 복사됩니다.<br>
The documentation is generated through Spring REST Docs tests and Asciidoctor, then copied into application static resources when running `localStart`, `bootRun`, or `bootJar`.

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

프론트엔드 개발자는 로컬에 Java/JDK를 설치하지 않고 Docker만으로 API 서버를 실행할 수 있습니다.<br>
Frontend developers can run the API server with Docker only, without installing Java/JDK locally.

애플리케이션 Docker 이미지는 컨테이너 내부에서 Eclipse Temurin Java 17을 사용해 빌드 및 실행됩니다.<br>
The application Docker image is built and run with Eclipse Temurin Java 17 inside the container.

Mac에서는 프로젝트 루트에서 아래 명령을 순서대로 실행하십시오.<br>
On Mac, run the following commands from the project root.

```bash
docker build --progress=plain -t flowit-main-server:local -f Dockerfile .
docker compose up -d
```

첫 번째 명령은 Spring Boot 애플리케이션 이미지를 Docker 안에서 빌드하고, 두 번째 명령은 MySQL, Redis, Prometheus, Grafana와 함께 로컬 환경을 실행합니다. 최초 실행 시 Gradle과 의존성을 컨테이너 내부에서 내려받으므로 시간이 걸릴 수 있습니다.<br>
The first command builds the Spring Boot application image inside Docker, and the second command starts it with MySQL, Redis, Prometheus, and Grafana. The first run can take a while because Gradle and dependencies are downloaded inside the container.

애플리케이션 이미지를 이미 빌드한 뒤에는 아래 명령만으로 다시 실행할 수 있습니다.<br>
After the application image has already been built, you can start the local environment again with only the following command.

```bash
docker compose up -d
```

상태와 로그는 아래 명령으로 확인합니다.<br>
Use the following commands to check status and logs.

```bash
docker compose ps
docker compose logs -f app
```

중지할 때는 아래 명령을 사용합니다. Docker 볼륨은 유지됩니다.<br>
Use the following command to stop the containers. Docker volumes are preserved.

```bash
docker compose down
```

<details>
<summary>Startup time notes</summary>

최초 실행 또는 Docker 이미지와 Gradle 의존성을 새로 내려받아야 하는 경우 `docker build --progress=plain -t flowit-main-server:local -f Dockerfile .` 또는 `./gradlew localBuildImage`가 몇 분 정도 걸릴 수 있습니다.<br>
The first run, or any run that needs to download Docker images and Gradle dependencies again, can make `docker build --progress=plain -t flowit-main-server:local -f Dockerfile .` or `./gradlew localBuildImage` take a few minutes.

빌드 명령은 `--progress=plain` 옵션을 사용하므로 Dockerfile 단계, Gradle 다운로드, `bootJar` 실행 상태가 터미널에 출력됩니다.<br>
The build command uses `--progress=plain`, so Dockerfile steps, Gradle downloads, and `bootJar` execution status are printed in the terminal.

Docker Desktop에서 컨테이너 uptime이 유지되고 있다면 애플리케이션 컨테이너는 계속 실행 중인 상태입니다. 이때 터미널 명령은 이미지 빌드, Compose 상태 확인, health 확인 단계에 있을 수 있습니다.<br>
If the container uptime continues in Docker Desktop, the application container is still running. In that case, the terminal command can be spending time on image build, Compose status checks, or health checks.

`./gradlew localStart`는 `app`, `mysql`, `redis`, `prometheus`, `grafana`가 이미 실행 중이고 actuator health가 정상이라면 Docker 빌드를 다시 실행하지 않고 종료하도록 구성되어 있습니다. 애플리케이션 이미지를 소스 기준으로 다시 빌드해야 할 때는 `./gradlew localBuildImage`를 실행하십시오.<br>
`./gradlew localStart` is configured to exit without running another Docker build when `app`, `mysql`, `redis`, `prometheus`, and `grafana` are already running and actuator health is healthy. Run `./gradlew localBuildImage` when you need to rebuild the application image from source.

Windows에서는 `gradlew.bat`와 Docker Compose 자식 프로세스 처리 방식 때문에 중단 후에도 일부 프로세스가 잠시 남아 명령이 오래 걸리는 것처럼 보일 수 있습니다. Mac/Linux는 shell 기반 `./gradlew`를 사용하므로 같은 현상이 발생할 가능성이 낮습니다.<br>
On Windows, the way `gradlew.bat` and Docker Compose child processes are handled can make a command appear to keep running for a while after interruption. Mac/Linux use the shell-based `./gradlew`, so the same symptom is much less likely.

</details>

<details>
<summary>Gradle local commands</summary>

Gradle 명령어는 동일한 Docker Compose 구성을 감싸는 편의 명령입니다. 단, Gradle Wrapper 자체는 JVM에서 실행되므로 이 경로는 로컬 Java가 있는 백엔드 개발자에게 적합합니다.<br>
The Gradle commands wrap the same Docker Compose setup. Because the Gradle Wrapper itself runs on the JVM, this path is intended for backend developers who have local Java installed.

```bash
./gradlew localStart
```

`localStart`는 애플리케이션 이미지가 없을 때만 `docker build --progress=plain`으로 이미지를 빌드하고 전체 로컬 환경을 실행합니다.<br>
`localStart` builds the application image with `docker build --progress=plain` only when the image is missing, then starts the full local environment.

```bash
./gradlew localBuildImage
```

`localBuildImage`는 애플리케이션 이미지만 다시 빌드하며, Docker build 진행 상황을 터미널에 출력합니다.<br>
`localBuildImage` rebuilds only the application image and prints Docker build progress in the terminal.

```bash
./gradlew localStatus
```

`localStatus`는 actuator health 상태와 Docker Compose 서비스 상태를 출력합니다.<br>
`localStatus` shows the actuator health status and Docker Compose service status.

```bash
./gradlew localStop
```

`localStop`은 `docker compose down`을 실행합니다. Docker 볼륨은 유지됩니다.<br>
`localStop` runs `docker compose down`. Docker volumes are preserved.

Mac에서 `./gradlew` 실행 권한이 없다면 한 번만 아래 명령을 실행하십시오.<br>
If `./gradlew` is not executable on Mac, run the following command once.

```bash
chmod +x gradlew
```

```bash
./gradlew localInfraStart
```

`localInfraStart`는 Spring Boot 애플리케이션 없이 MySQL, Redis, Prometheus, Grafana만 실행합니다.<br>
`localInfraStart` starts only MySQL, Redis, Prometheus, and Grafana without the Spring Boot application.

```bash
./gradlew localInfraStop
```

`localInfraStop`은 인프라 서비스 컨테이너만 중지합니다. Docker 볼륨은 유지됩니다.<br>
`localInfraStop` stops only the infrastructure service containers. Docker volumes are preserved.

Gradle 로컬 명령어는 OS에 맞는 Docker Compose 설정을 자동으로 선택합니다.<br>
The Gradle local commands automatically select the Docker Compose configuration for the current OS.

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

## Flyway Migration

> [!WARNING]
>
> DDL 및 ALTER 문은 Flyway Migration 라이브러리를 통해 애플리케이션 실행 시 데이터베이스에 자동으로 반영됩니다.<br>
> DDL and ALTER statements are automatically applied to the database through the Flyway Migration library when the application runs.
>
> 별도의 SQL 파일을 데이터베이스에 강제로 실행하지 마십시오.<br>
> Do not force-run separate SQL files directly against the database.
