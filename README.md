# Blog backend

Spring Boot 3.4 / Java 17 backend for `kimdongwon.me`.

## Quick start

Requirements: Docker Desktop with Compose.

```bash
docker compose up --build
```

The API is available at `http://localhost:8080` and its health check at
`http://localhost:8080/actuator/health`.

Copy `.env.example` to `.env` only when overriding local defaults. Never commit
`.env`, database dumps, private keys, or production credentials.

## Tests

macOS/Linux:

```bash
./mvnw test
```

Windows:

```powershell
.\mvnw.cmd test
```

Tests use an isolated H2 database. Local runtime uses MariaDB 10.5 through
Compose, matching the current production database major/minor version.

## Documentation

- [Current architecture](docs/architecture-as-is.md)
- [Local and production differences](docs/local-production-parity.md)
- [Environment variable contract](docs/environment-variables.md)
- [Migration status](docs/migration-status.md)
