# Flowit Main Server

API Version: 0.2.17-volt · up-to-date

[English](README.en.md)

## 기술 스택

Flowit main server는 Java 17, Spring Boot, MySQL, Redis, Spring REST Docs 기반으로 구성합니다.

<details>
<summary>기술 스택 상세</summary>

### Backend

| 구분 | 기술 |
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

| 구분 | 기술 |
|---|---|
| RDBMS | MySQL |
| Migration | Flyway Migration |

### Observability

| 구분 | 기술 |
|---|---|
| Health Check / Metrics | Spring Boot Actuator |
| Metrics Collection | Prometheus |
| Metrics Visualization | Grafana |
| Error Monitoring | Sentry |

### Documentation

| 구분 | 기술 |
|---|---|
| API Documentation | Spring REST Docs |

### Development

| 구분 | 기술 |
|---|---|
| Boilerplate Reduction | Lombok |
| Developer Tooling | Spring Boot DevTools |

</details>

## API 문서

API 문서는 애플리케이션 기동 후 `/docs`에서 확인할 수 있습니다.

```text
http://localhost:8080/docs
http://localhost:8080/docs/index.html
```

`/docs`는 API 문서 목록 페이지입니다. API별 상세 문서는 목록에서 이동하십시오.

예시 상세 문서 경로는 아래와 같습니다.

```text
http://localhost:8080/docs/docs-preview.html
```

문서 산출물은 Spring REST Docs 테스트와 Asciidoctor를 통해 생성되며, 로컬 `start`, `localStart`, `bootRun`, `bootJar` 실행 시 애플리케이션 정적 리소스에 함께 복사됩니다.

<details>
<summary>API 문서 생성 명령</summary>

```bash
./gradlew restdocsTest
```

`restdocsTest`는 API 문서에 사용할 Spring REST Docs snippets를 생성합니다.

```bash
./gradlew bootJar
```

`bootJar`는 snippets를 기반으로 HTML 문서를 생성하고, 실행 가능한 JAR에 문서를 포함합니다.

</details>

## 로컬 Docker 실행 환경

> [!WARNING]
>
> 필수: Docker Desktop을 반드시 실행하십시오.

로컬 JDK 없이 실행하려면 프로젝트 루트에서 OS에 맞는 스크립트를 사용하십시오.

```powershell
.\local.bat start
```

```bash
./local.sh start
```

이 저장소는 Mac/Linux에서 사용할 `local.sh`와 `gradlew` 실행 권한을 Git에 포함해 추적합니다. 권한 오류가 발생하면 먼저 최신 `main`을 받고 있는지 확인하십시오.

로컬 JDK가 설치되어 있다면 기존 Gradle 명령도 계속 사용할 수 있습니다.

```bash
./gradlew localStart
```

`start`와 `localStart`는 Spring Boot 애플리케이션 이미지를 Docker 안에서 빌드하고 MySQL, Redis, Prometheus, Grafana와 함께 로컬 환경을 실행합니다. JDK가 없는 환경에서는 `local.bat` 또는 `local.sh`가 Gradle Wrapper를 거치지 않고 Docker 명령을 직접 실행합니다.

애플리케이션 Docker 이미지는 컨테이너 내부에서 Eclipse Temurin Java 17을 사용해 빌드 및 실행됩니다.

안전한 자동 소스 갱신을 사용하려면 fork 저장소가 아니라 조직 원본 저장소 `runtime-lab/flowit-main-server`를 clone해야 합니다. fork 또는 다른 remote에서 clone한 작업 트리는 자동 갱신 대상이 아니며, 별도 확인 없이 현재 소스로 계속 진행합니다.

이미지를 빌드하기 전 `local.bat start`, `local.bat build-image`, `./local.sh start`, `./local.sh build-image`는 허용된 소스만 확인합니다. 허용된 소스는 `origin`이 `https://github.com/runtime-lab/flowit-main-server.git` 또는 `git@github.com:runtime-lab/flowit-main-server.git`을 가리키는 경우입니다. 허용된 원본 저장소에서 현재 브랜치가 `main`이 아니면 에러와 함께 중단합니다. `main` 브랜치가 뒤처져 있고 작업 트리가 깨끗하면 해당 브랜치만 가져와 fast-forward로 갱신합니다. 허용된 원본 `main`에서 fetch, 비교, 상태 확인, merge가 실패하거나 로컬 변경 또는 diverge 상태 때문에 자동 갱신할 수 없으면 현재 소스로 계속 진행할지 묻습니다. 기본값은 중단입니다. 이 소스 갱신 확인을 건너뛰려면 `FLOWIT_SKIP_AUTO_UPDATE=true`를 설정하십시오.

서버 환경과 혼동하기 쉬운 Linux에서는 기본적으로 실행을 차단합니다. Linux 로컬 개발 환경에서만 아래처럼 명시적으로 허용하십시오.

```bash
FLOWIT_ALLOW_LOCAL_DOCKER=true ./local.sh start
```

이 명령들은 클라이언트/API 연동용 로컬 개발 환경 전용입니다. `FLOWIT_ENV=prod|production`, `SPRING_PROFILES_ACTIVE=prod|production`, `SPRING_PROFILES_INCLUDE=prod|production`, `CI=true`, `GITHUB_ACTIONS=true`, `GITLAB_CI=true`, `JENKINS_URL`, `KUBERNETES_SERVICE_HOST` 같은 운영 또는 CI 신호가 있으면 `FLOWIT_ALLOW_LOCAL_DOCKER=true`가 설정되어 있어도 실행이 차단됩니다.

상태와 로그는 아래 명령으로 확인합니다.

```powershell
.\local.bat status
.\local.bat logs
```

```bash
./local.sh status
./local.sh logs
```

Gradle을 사용하는 경우에는 아래 명령도 가능합니다.

```bash
./gradlew localStatus
docker compose logs -f app
```

중지할 때는 아래 명령을 사용합니다. Docker 볼륨은 유지됩니다.

```powershell
.\local.bat stop
```

```bash
./local.sh stop
```

<details>
<summary>시작 시간 참고</summary>

최초 실행 또는 Docker 이미지와 Gradle 의존성을 새로 내려받아야 하는 경우 `local.bat start`, `./local.sh start`, `./gradlew localStart`가 몇 분 정도 걸릴 수 있습니다.

빌드 명령은 `--progress=plain` 옵션을 사용하므로 Dockerfile 단계, Gradle 다운로드, `bootJar` 실행 상태가 터미널에 출력됩니다.

Docker Desktop에서 컨테이너 uptime이 유지되고 있다면 애플리케이션 컨테이너는 계속 실행 중인 상태입니다. 이때 터미널 명령은 이미지 빌드, Compose 상태 확인, health 확인 단계에 있을 수 있습니다.

로컬 시작 명령은 `src/main`, `src/docs`, REST Docs 테스트, REST Docs 템플릿, Gradle 설정, Docker 설정의 소스 해시가 기존 `flowit-main-server:local` 이미지와 같으면 이미지를 재사용하고, 달라지면 자동으로 다시 빌드합니다. 강제로 다시 빌드해야 할 때는 `local.bat build-image`, `./local.sh build-image`, 또는 `./gradlew localBuildImage`를 실행하십시오.

Windows에서는 JDK 없이 실행할 때 `local.bat`를 사용하면 Gradle Wrapper 기동을 건너뛰고 PowerShell 기반 Docker 명령만 실행합니다. Mac에서는 `./local.sh start`를 사용하십시오.

</details>

<details>
<summary>추가 로컬 명령</summary>

```powershell
.\local.bat build-image
```

```bash
./local.sh build-image
./gradlew localBuildImage
```

`build-image`와 `localBuildImage`는 애플리케이션 이미지만 다시 빌드하며, Docker build 진행 상황을 터미널에 출력합니다.

```bash
./local.sh status
./gradlew localStatus
```

```powershell
.\local.bat status
```

`localStatus`는 actuator health 상태와 Docker Compose 서비스 상태를 출력합니다.

```bash
./local.sh stop
./gradlew localStop
```

```powershell
.\local.bat stop
```

`localStop`은 `docker compose down`을 실행합니다. Docker 볼륨은 유지됩니다.

```bash
./local.sh infra-start
./gradlew localInfraStart
```

```powershell
.\local.bat infra-start
```

`localInfraStart`는 Spring Boot 애플리케이션 없이 MySQL, Redis, Prometheus, Grafana만 실행합니다.

```bash
./local.sh infra-stop
./gradlew localInfraStop
```

```powershell
.\local.bat infra-stop
```

`localInfraStop`은 인프라 서비스 컨테이너만 중지합니다. Docker 볼륨은 유지됩니다.

로컬 스크립트와 Gradle 로컬 명령어는 OS에 맞는 Docker Compose 설정을 자동으로 선택합니다.

- Windows/Mac: `compose.yaml`
- Linux: `compose.yaml` + `compose.linux.yaml`

</details>

<details>
<summary>Linux Docker 명령</summary>

Linux에서는 `host.docker.internal`을 사용하기 위해 override 파일을 함께 지정하십시오.

```bash
docker build --progress=plain -t flowit-main-server:local -f Dockerfile .
docker compose -f compose.yaml -f compose.linux.yaml up -d
```

</details>

<details>
<summary>로컬 개발 엔드포인트</summary>

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

Redis 컨테이너는 로컬 개발 환경에서 캐시 및 메시지 기능을 사용하기 위해 함께 생성합니다.

Prometheus 컨테이너는 Spring Boot Actuator metrics를 수집하고, Grafana 컨테이너는 Prometheus datasource가 자동 등록된 상태로 생성합니다.

애플리케이션, MySQL, Redis, Prometheus, Grafana의 호스트 포트는 모두 `127.0.0.1`에만 바인딩되므로 로컬에서만 접근할 수 있습니다.

컨테이너 내부에서는 명시적인 bridge network인 `flowit-local`을 통해 `app`, `mysql`, `redis`, `prometheus` 서비스명으로 통신합니다.

Spring Boot Actuator는 컨테이너 내부에서 `0.0.0.0`에 바인딩되어 Prometheus가 bridge network로 metrics를 수집할 수 있고, 호스트 노출은 Docker 포트 바인딩으로 `127.0.0.1`에 제한됩니다.

</details>

<details>
<summary>컨테이너 유지보수 명령</summary>

### 컨테이너 재생성

초기화 SQL을 다시 실행하거나 로컬 데이터베이스, Redis, Prometheus, Grafana 데이터를 초기 상태로 되돌려야 할 때 사용하십시오.

```bash
docker compose down -v
docker build --progress=plain -t flowit-main-server:local -f Dockerfile .
docker compose up -d
```

`-v` 옵션은 MySQL, Redis, Prometheus, Grafana 데이터 볼륨을 제거하므로 기존 로컬 데이터가 모두 삭제됩니다.

### 컨테이너 중지

컨테이너를 중지할 때 사용하십시오. 데이터 볼륨은 유지되므로 로컬 데이터는 보존됩니다.

```bash
docker compose down
```

</details>

<details>
<summary>백엔드 의존성 유지보수</summary>

의존성 공급망은 Gradle dependency verification과 Docker image digest pinning으로 고정합니다. 의존성 또는 이미지 버전을 갱신할 때는 verification metadata와 digest를 함께 갱신하고, Dependabot PR은 자동 병합하지 말고 테스트 후 수동으로 병합하십시오. GitHub 저장소에서는 Dependency graph, Dependabot alerts, Dependabot security updates를 활성화하십시오.

> [!WARNING]
>
> 백엔드 전용: 의존성을 추가하거나 버전을 변경하는 백엔드 개발자는 metadata 작성 옵션을 붙여 빌드 artifact와 IntelliJ가 요청하는 dependency source JAR을 함께 갱신한 다음, 변경된 metadata를 검토하고 일반 빌드를 다시 실행하십시오.
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
> DDL 및 ALTER 문은 Flyway Migration 라이브러리를 통해 애플리케이션 실행 시 데이터베이스에 자동으로 반영됩니다.
>
> 별도의 SQL 파일을 데이터베이스에 강제로 실행하지 마십시오.
