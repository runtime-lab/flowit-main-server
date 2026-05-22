## Tech Stack

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



## Local Database / Redis Initialization

### ! 필수 : Docker Desktop을 반드시 실행하십시오. <br>! Required: Docker Desktop must be running.

루트 디렉토리에서 아래의 명령을 실행하십시오.<br>
From the project root, run:

```bash
docker compose up -d
```

### Local Development Database Information
- Host: localhost
- Port: 3306
- Database: project_flowit
- Username: flowit_dev
- Password: flowitDevPass

### Local Development Redis Information
- Host: localhost
- Port: 6379
- Password: flowitLocalDev

Redis 컨테이너는 로컬 개발 환경에서 캐시 및 메시징 기능을 사용하기 위해 함께 생성됩니다.<br>
The Redis container is created together for local cache and messaging features.

MySQL과 Redis는 모두 `127.0.0.1`에만 바인딩되므로 로컬에서만 접근할 수 있습니다.<br>
Both MySQL and Redis are bound only to `127.0.0.1`, so they are accessible only from the local.

### Recreate container

초기화 SQL을 다시 실행하거나 로컬 데이터베이스 및 Redis 데이터를 초기 상태로 되돌려야 할 때 사용하십시오.<br>
Use this when you need to re-run the initialization SQL or reset the local database and Redis data.

```bash
docker compose down -v
docker compose up -d
```

`-v` 옵션은 MySQL 및 Redis 데이터 볼륨을 삭제하므로 기존 로컬 데이터가 모두 제거됩니다.<br>
The `-v` option removes the MySQL and Redis data volumes, so all existing local data will be deleted.

### Stop container

컨테이너를 중지할 때 사용하십시오. 데이터 볼륨은 유지되므로 로컬 데이터는 보존됩니다.<br>
Use this to stop the containers. The data volumes are preserved, so local data will remain.

```bash
docker compose down
```

### Flyway Migration

> [!WARNING]
> DDL 및 ALTER 문은 Flyway Migration 라이브러리를 통해 애플리케이션 실행 시 데이터베이스에 자동으로 반영됩니다.<br>
> DDL and ALTER statements are automatically applied to the database through the Flyway Migration library when the application runs.
>
> 별도의 SQL 파일을 데이터베이스에 강제로 실행하지 마십시오.<br>
> Do not force-run separate SQL files directly against the database.
