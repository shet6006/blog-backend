# 로컬과 운영 환경의 차이 및 통합 방법

| 항목 | 이전 로컬 | 기존 운영 | 새 공통 기준 |
|---|---|---|---|
| OS | Windows, 향후 macOS | Amazon Linux 2023 | Docker 이미지 사용 |
| Java | PC 설치 버전에 의존 | Corretto 17 | Java 17 이미지 |
| DB | 설정 누락 | EC2 MariaDB 10.5 | 로컬·운영 모두 MariaDB 10.5 Compose |
| 백엔드 설정 | 무시되는 YAML | 서버 전용 YAML | Git에는 환경변수 자리표시자만 저장 |
| 프론트 | Next 개발 서버 | Next+PM2 | 정적 빌드+S3/CloudFront |
| 업로드 | 저장소 상대경로 | 서버 파일 | `/srv/blog/uploads` 영구 볼륨 |
| 비밀값 | 임의 `.env` | 서버 파일·SSH 키 | 로컬 `.env`, 운영 EC2 `runtime.env` |
| 배포 | 수동 실행 | SSH 후 서버 빌드 | 프론트 S3, 백엔드 ECR/CodeDeploy |
| 상태 검사 | 없음 | 프로세스 존재 여부 | Actuator와 Docker healthcheck |
| 테스트 | 운영 DB 설정 없으면 실패 | 배포에서 생략 | H2 테스트와 Compose 검증 |

이미 적용한 내용:

- Node 20/npm 10, Java 17, MariaDB 10.5 버전 고정
- 백엔드 DB·JWT·CORS·업로드 설정을 공통 환경변수로 전환
- 운영 IP와 CloudFront 주소 하드코딩 제거
- DB 없이 실행 가능한 H2 테스트 추가
- 백엔드 비-root Docker 이미지와 상태 검사 추가
- Next.js 정적 export와 S3/CloudFront 배포 추가
- 임의 게시글 및 편집 URL의 정적 라우팅 스모크 테스트 추가
- CodeDeploy, 헬스체크, 실패 시 이전 이미지 롤백 추가

Windows OneDrive는 `node_modules` 파일을 잠가 설치를 방해할 수 있다. Windows와 Mac 모두
동기화 폴더가 아닌 `~/Projects` 같은 일반 개발 폴더에 clone하는 것을 권장한다.
