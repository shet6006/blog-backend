# 블로그 백엔드

`kimdongwon.me`의 Spring Boot 3.4 / Java 17 API입니다.

```bash
SPRING_PROFILES_ACTIVE=local ./mvnw spring-boot:run
```

로컬 프로필은 별도 DB나 환경변수 없이 H2와 샘플 데이터를 사용합니다. 테스트는
`./mvnw test`로 실행합니다.

로컬 개발, 운영 환경변수, AWS 아키텍처, 자동 배포 및 새 서버 복구 절차는
[운영·개발 가이드](docs/operations.md) 한 문서에서 관리합니다.
