# 내일 실행할 블로그 이전 체크리스트

목표:

```text
가비아 DNS
  → 새 CloudFront
      ├─ /*     → 비공개 S3의 Next.js 정적 파일
      └─ /api/* → 새 Blog EC2:80 → Nginx → Spring Boot 컨테이너:8080
                                      └─ MariaDB 컨테이너
```

기존 AWS 계정 `254909274481`과 기존 EC2 `i-0c32f5a550af5f7e8`은 조사와 백업만 한다.
삭제, 중지, 재시작, 보안그룹 변경을 하지 않는다. 새 계정의 기존 `tunelog-server`도 다른
프로젝트이므로 건드리지 않는다.

## 확인된 기존 운영 데이터

- DB `blog`, `utf8mb4_unicode_ci`
- `posts=20`, `categories=8`, `comments=4`, `likes=2`, `visitors=244`
- MariaDB 데이터 디렉터리 약 125MB
- `/var/blog-uploads`: 이미지 7개, 약 1.6MB
- `frontend/public/uploads/avatar_admin.png`: 관리자 이미지 1개
- 기존 운영 설정에는 DB 암호와 JWT secret이 있으므로 값은 화면이나 로그에 출력하지 않는다.

## 0. 시작 전 안전 확인

- [ ] AWS CLI `get-caller-identity`로 현재 계정을 확인한다.
- [ ] 모든 생성 리소스 이름에 `blog`를 사용하고 `tunelog` 리소스를 선택하지 않는다.
- [ ] 기존 운영 사이트가 정상인지 확인한다.
- [ ] 로컬 디스크에 백업을 저장할 충분한 공간이 있는지 확인한다.
- [ ] 저장소 변경은 검토 후 별도 승인으로 커밋·푸시한다.

## 1. 저장소 준비

- [ ] 프론트 `npm run build`가 성공하는지 확인한다.
- [ ] 백엔드 `./mvnw test`와 Docker 이미지 빌드가 성공하는지 확인한다.
- [ ] 프론트 workflow가 `out/ → S3 → CloudFront invalidation`인지 확인한다.
- [ ] 백엔드 workflow가 `Docker → ECR → CodeDeploy`인지 확인한다.
- [ ] 운영 Compose에 프론트 컨테이너가 없고 backend 포트가 `127.0.0.1:8080`에만 열리는지 확인한다.

## 2. 기존 운영 데이터 백업

최종 DNS 전환 전까지 기존 서비스는 계속 동작한다. 먼저 예비 백업을 만들고, 전환 직전에
쓰기 작업을 잠시 중단한 뒤 최종 백업을 한 번 더 만든다.

백업 항목:

- [ ] `mariadb-dump --single-transaction --routines --triggers --events blog`
- [ ] `/var/blog-uploads`
- [ ] `frontend/public/uploads/avatar_admin.png`
- [ ] 각 백업의 SHA-256
- [ ] 기존 JWT secret을 안전한 경로로 이전하되 터미널 출력이나 Git에 남기지 않기

DB 원본 디렉터리 `/var/lib/mysql`은 실행 중인 상태에서 복사하지 않는다. 반드시 논리 덤프를
사용한다. 기존 서버 디스크 여유가 1.5GB뿐이므로 백업은 SSH 표준 출력으로 Mac에 바로
저장하는 방식을 사용한다.

## 3. 새 계정에 만들 리소스

- [ ] 비공개 S3 버킷 2개: 프론트 정적 파일용, CodeDeploy 배포 묶음용
- [ ] ECR `blog-backend`
- [ ] 블로그 전용 EC2와 암호화 gp3 볼륨
- [ ] 블로그 전용 보안그룹
- [ ] EC2 Instance Role: ECR pull, CodeDeploy revision S3 read
- [ ] GitHub OIDC Role: 프론트 S3 sync/CloudFront invalidation, 백엔드 ECR push/CodeDeploy 시작
- [ ] CodeDeploy Service Role
- [ ] 백엔드 CodeDeploy Application과 Deployment Group
- [ ] 새 CloudFront와 `us-east-1` ACM 인증서

S3 버킷은 public access를 모두 차단하고 CloudFront Origin Access Control만 정적 파일을 읽게
한다. CodeDeploy prefix는 CloudFront 원본 경로 밖에 두거나 IAM 정책으로 분리한다.

## 4. 새 CloudFront 동작

- [ ] 기본 origin: S3
- [ ] 기본 behavior: GET/HEAD, 정적 캐시
- [ ] `/api/*` origin: 새 Blog EC2의 public DNS
- [ ] `/api/*`: 모든 필요한 메서드 허용, 캐시 비활성화, 쿠키와 필요한 헤더 전달
- [ ] viewer protocol: HTTPS redirect
- [ ] `frontend/deploy/cloudfront-uri-rewrite.js`를 Viewer Request Function으로 연결

URI rewrite는 `/posts/view?slug=x` 요청을 S3 객체 `/posts/view/index.html`로 연결한다. 쿼리는
브라우저에 그대로 남아 클라이언트가 slug를 읽는다.

## 5. 새 Blog EC2 최초 준비

인스턴스 ID와 Name 태그가 새 블로그 서버인지 재확인한 뒤에만 실행한다.

```bash
sudo BLOG_BOOTSTRAP_CONFIRM=NEW_BLOG_HOST \
  AWS_REGION=ap-northeast-2 \
  /path/to/bootstrap-host.sh
```

이 스크립트는 Docker, Docker Compose, Nginx, CodeDeploy Agent와 필요한 디렉터리를 준비한다.
기존 운영 EC2에서는 실행하지 않는다.

`/etc/blog/runtime.env`에 직접 입력:

```ini
SPRING_PROFILES_ACTIVE=prod
DB_NAME=blog
DB_USER=blog
DB_PASSWORD=<새 DB 암호>
DB_ROOT_PASSWORD=<새 root 암호>
DB_POOL_SIZE=5
JWT_SECRET=<기존 JWT secret 또는 의도적으로 교체한 값>
CORS_ALLOWED_ORIGINS=https://kimdongwon.me,https://www.kimdongwon.me
```

`/etc/blog/images.env`에는 최초 백엔드 이미지 URI를 넣는다. 두 파일 권한은 `600`으로 둔다.

## 6. 데이터 복원

백엔드 첫 CodeDeploy보다 먼저 수행한다. 그렇지 않으면 빈 DB에서 `ddl-auto=validate`가
실패한다. 백업 파일을 새 EC2에 전달한 뒤, 새 서버이고 `/srv/blog/mysql`이 비어 있는지
재확인한다.

```bash
sudo BLOG_RESTORE_CONFIRM=NEW_BLOG_EMPTY_DATABASE \
  /path/to/restore-production-data.sh \
  /path/to/blog.sql.gz \
  /path/to/uploads.tar.gz
```

스크립트는 임시 MariaDB 10.5 컨테이너로 영구 볼륨을 초기화하고 덤프와 업로드를 복원한 뒤
임시 컨테이너만 제거한다. DB 디렉터리가 비어 있지 않으면 중단한다.

- [ ] 복원 스크립트가 DB healthcheck 후 덤프를 `blog` DB에 복원한다.
- [ ] `/srv/blog/uploads`에 업로드 압축을 푼다.
- [ ] 프론트 정적 빌드에 `avatar_admin.png`가 포함됐는지 확인한다.
- [ ] 테이블별 정확한 행 수를 기존 서버와 비교한다.
- [ ] 업로드 파일 수 7개와 SHA-256을 비교한다.
- [ ] 최초 DB 컨테이너 기동 후 `migrate-legacy-schema.sh`로 기존 `INT` ID를 엔티티의 `BIGINT`에 맞춘다.

## 7. 최초 배포 및 검증

- [ ] 백엔드 workflow를 수동 실행한다.
- [ ] CodeDeploy 성공과 `/api/actuator/health`를 확인한다.
- [ ] 프론트 workflow를 수동 실행한다.
- [ ] 새 CloudFront 기본 도메인에서 기능을 검사한다.

기능 검사:

- [ ] 홈, 소개, 카테고리
- [ ] `/posts/view?slug=...`
- [ ] 관리자 로그인
- [ ] 글 작성·수정·삭제
- [ ] 댓글과 좋아요
- [ ] 기존 이미지 7개 표시
- [ ] 새 이미지 업로드
- [ ] 새 컨테이너 배포 후 DB와 업로드 유지

## 8. 최종 데이터 동기화와 DNS 전환

- [ ] 관리자 쓰기 작업을 잠시 중단한다.
- [ ] 기존 서버에서 최종 DB dump와 업로드 백업을 다시 만든다.
- [ ] 새 서버에 최종 복원하고 행 수·파일 해시를 재검증한다.
- [ ] 가비아 DNS를 새 CloudFront로 변경한다.
- [ ] HTTPS와 실제 도메인 기능을 확인한다.
- [ ] 문제가 있으면 DNS를 기존 CloudFront로 되돌린다.

## 9. 안정화 후

- [ ] 기존 AWS와 EC2는 즉시 삭제하지 않고 합의한 기간 보존한다.
- [ ] 새 EC2 DB/업로드 백업 일정을 만든다.
- [ ] S3/ECR 수명주기 정책을 적용한다.
- [ ] GitHub의 기존 SSH Secrets는 새 배포가 안정화된 뒤 제거한다.
- [ ] root AWS CLI 세션에서 로그아웃하고 일상 작업용 최소 권한을 사용한다.
