# 블로그 백엔드

`kimdongwon.me`의 Spring Boot 3.4 / Java 17 백엔드입니다.

## 빠른 시작

Docker Desktop과 Docker Compose가 필요합니다.

```bash
docker compose up --build
```

API는 `http://localhost:8080`, 상태 확인은 `http://localhost:8080/actuator/health`에서 사용할 수 있습니다.

로컬 기본값을 바꿀 때만 `.env.example`을 `.env`로 복사합니다. `.env`, DB 덤프, 개인 키, 운영 인증 정보는 절대 커밋하지 않습니다.

## 테스트

macOS/Linux:

```bash
./mvnw test
```

Windows:

```powershell
.\mvnw.cmd test
```

테스트는 격리된 H2 DB를 사용합니다. 로컬 실행은 기존 운영 환경과 같은 MariaDB 10.5를 Compose로 사용합니다.

## 문서

- [기존 운영 아키텍처](docs/architecture-as-is.md)
- [새 AWS 목표 아키텍처](docs/architecture-to-be.md)
- [MacBook에서 새 AWS 구축·배포하는 전체 절차](docs/macbook-aws-deployment-guide.md)
- [로컬과 운영의 차이 및 축소 방안](docs/local-production-parity.md)
- [환경 변수 명세](docs/environment-variables.md)
- [마이그레이션 진행 상태](docs/migration-status.md)
