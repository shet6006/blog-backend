# 환경변수 관리 기준

운영 실값은 GitHub와 문서에 저장하지 않는다.

| 변수 | 운영 필수 | 용도 | 운영 저장 위치 |
|---|---:|---|---|
| `DB_URL` | 예 | MariaDB JDBC 주소 | 배포 설정 |
| `DB_USER` | 예 | 애플리케이션 DB 사용자 | Secrets Manager |
| `DB_PASSWORD` | 예 | 애플리케이션 DB 암호 | Secrets Manager |
| `DB_ROOT_PASSWORD` | 예 | MariaDB 관리자 암호 | Secrets Manager |
| `DB_NAME` | 예 | DB 이름 | Parameter Store 또는 배포 설정 |
| `DB_POOL_SIZE` | 아니오 | DB 연결 수 | 배포 설정 |
| `JPA_DDL_AUTO` | 예 | 운영에서는 `validate` | 배포 설정 |
| `JWT_SECRET` | 예 | 관리자 로그인 토큰 서명 | Secrets Manager |
| `CORS_ALLOWED_ORIGINS` | 예 | 허용할 프론트 주소 | 배포 설정 |
| `STORAGE_LOCATION` | 예 | 업로드 영구 경로 | 배포 설정 |
| `SERVER_PORT` | 아니오 | 기본 8080 | 배포 설정 |

프론트는 `NEXT_PUBLIC_API_URL`만 사용한다. Nginx가 동일 도메인의 `/api`를 프록시하는
운영에서는 빈 값이며, 분리된 로컬 개발에서는 `http://localhost:8080`을 사용한다.

`JWT_SECRET`을 교체하면 기존 로그인 쿠키만 무효화된다. 사용자·글·댓글·조회수는 DB
백업으로, 업로드 이미지는 파일 백업으로 별도 보존한다.
