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



## Local Database Initialization

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

### Recreate container

초기화 SQL을 다시 실행하거나 로컬 데이터베이스를 초기 상태로 되돌려야 할 때 사용하십시오.<br>
Use this when you need to re-run the initialization SQL or reset the local database.

```bash
docker compose down -v
docker compose up -d
```

`-v` 옵션은 MySQL 데이터 볼륨을 삭제하므로 기존 로컬 데이터가 모두 제거됩니다.<br>
The `-v` option removes the MySQL data volume, so all existing local database data will be deleted.

### Stop container

컨테이너를 중지할 때 사용하십시오. 데이터 볼륨은 유지되므로 로컬 데이터베이스 데이터는 보존됩니다.<br>
Use this to stop the container. The data volume is preserved, so local database data will remain.

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
