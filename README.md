# 블로그 백엔드

`kimdongwon.me`의 Spring Boot 3.4 / Java 17 백엔드입니다.

## 빠른 시작

Java 17에서 `local` 프로필로 실행합니다. 별도의 MySQL/MariaDB나
환경변수는 필요 없습니다.

```bash
SPRING_PROFILES_ACTIVE=local ./mvnw spring-boot:run
```

`local` 프로필은 매번 새 인메모리 H2 DB를 만들고 다음 데이터를 초기화합니다.

- 관리자 아이디: `admin`
- 관리자 비밀번호: `admin1234`
- `개발` 카테고리와 샘플 게시글 1개

API는 `http://localhost:8080`, 상태 확인은 `http://localhost:8080/actuator/health`에서
사용할 수 있습니다. 프로세스를 종료하면 로컬 DB는 사라지고 다음 실행에서 다시
생성됩니다.

Docker Compose는 운영과 동일한 MariaDB/컨테이너 구성을 확인할 때만 사용합니다.

```bash
docker compose up --build
```

`.env`, DB 덤프, 개인 키, 운영 인증 정보는 절대 커밋하지 않습니다.

## 테스트

macOS/Linux:

```bash
./mvnw test
```

Windows:

```powershell
.\mvnw.cmd test
```

테스트와 `local` 프로필은 각각 격리된 H2 DB를 사용합니다. 운영은 MariaDB 10.5와
EC2의 영구 볼륨을 사용합니다.

## 문서

- [기존 운영 아키텍처](docs/architecture-as-is.md)
- [새 AWS 목표 아키텍처](docs/architecture-to-be.md)
- [MacBook에서 새 AWS 구축·배포하는 전체 절차](docs/macbook-aws-deployment-guide.md)
- [로컬과 운영의 차이 및 축소 방안](docs/local-production-parity.md)
- [환경 변수 명세](docs/environment-variables.md)
- [마이그레이션 진행 상태](docs/migration-status.md)
