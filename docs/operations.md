# 블로그 운영·개발 가이드

이 문서가 로컬 개발, AWS 운영, 배포, 복구 절차의 단일 기준이다. 실제 비밀번호, JWT
비밀키, SSH 개인키, DB 덤프와 업로드 백업은 Git에 저장하지 않는다.

## 1. 현재 구조

두 저장소는 계속 분리해서 관리한다.

- 프론트엔드: `https://github.com/shet6006/blog.git`
- 백엔드: `https://github.com/shet6006/blog-backend.git`
- 운영 브랜치: `master`
- 작업 브랜치에서 검증 후 `master`에 병합하면 자동 배포된다.

운영 요청 흐름은 다음과 같다.

```text
사용자
  → CloudFront (TLS 인증서, 캐시, 도메인)
      ├─ /*       → 비공개 S3 → Next.js 정적 export
      └─ /api/*   → EC2:80 → Nginx → 127.0.0.1:8080 → Spring Boot
                                      ├─ MariaDB 컨테이너 → /srv/blog/mysql
                                      └─ 업로드 파일      → /srv/blog/uploads
```

프론트는 서버나 컨테이너로 실행하지 않는다. 백엔드와 MariaDB만 EC2의 Docker Compose로
실행한다. EC2의 80번 포트는 CloudFront 관리형 Prefix List에서만 접근할 수 있다.

### 현재 AWS 리소스

| 항목 | 값 |
|---|---|
| 계정 | `847835841110` |
| 리전 | `ap-northeast-2` (서울) |
| 도메인 | `kimdongwon.me`, `www.kimdongwon.me` |
| CloudFront | `E9RA6FWWCXEPE` / `dxsqm6tx64iht.cloudfront.net` |
| 프론트 S3 | `blog-frontend-847835841110` |
| 백엔드 ECR | `blog-backend` |
| CodeDeploy S3 | `blog-codedeploy-847835841110` |
| CodeDeploy | 앱 `blog-backend`, 그룹 `blog-backend-production` |
| EC2 | `i-074e940dfcda59a4d`, `t3.micro` |
| GitHub OIDC 역할 | `arn:aws:iam::847835841110:role/blog-github-actions` |

Elastic IP를 사용하지 않는다. EC2를 중지 후 시작하면 Public IP와 Public DNS가 바뀔 수
있으며, 이때 CloudFront의 `/api/*` 원본 도메인을 새 EC2 Public DNS로 수정해야 한다.

## 2. 새 기기에서 로컬 개발

### 필요한 런타임

- 백엔드: Temurin JDK 17. Maven은 설치하지 않고 저장소의 Maven Wrapper를 사용한다.
- 프론트엔드: Node.js 20, npm 10 (`.nvmrc` 기준)
- 로컬 개발에는 Docker와 로컬 MySQL이 필요 없다.

```bash
mkdir Blog && cd Blog
git clone https://github.com/shet6006/blog-backend.git backend
git clone https://github.com/shet6006/blog.git frontend
```

백엔드 실행:

```bash
cd backend
./mvnw test
SPRING_PROFILES_ACTIVE=local ./mvnw spring-boot:run
```

IntelliJ에서는 프로젝트 SDK와 Maven Runner JRE를 모두 Temurin 17로 지정한다. 저장소의
`BlogBackendApplication` 실행 구성은 `local` 프로필과 JDK 17을 사용한다.

`local` 프로필은 매 실행마다 H2 메모리 DB를 새로 만들고 종료하면 폐기한다.

- 관리자 아이디: `admin`
- 관리자 비밀번호: `admin1234`
- 샘플 카테고리와 게시글 1개
- API: `http://localhost:8080`
- 헬스체크: `http://localhost:8080/actuator/health`
- 로컬 업로드: `backend/data/uploads` (Git 제외)

프론트엔드 실행:

```bash
cd ../frontend
cp .env.example .env.local
npm ci --legacy-peer-deps
npm run dev
```

`.env.local`에는 다음 한 줄만 필요하다.

```dotenv
NEXT_PUBLIC_API_URL=http://localhost:8080
```

프론트는 `http://localhost:3000`에서 실행한다. 로컬 백엔드를 먼저 실행한다.

## 3. 환경변수

### 로컬

백엔드는 `application-local.yml`의 H2와 개발용 기본값을 사용하므로 환경변수가 필요 없다.
프론트만 `.env.local`에 `NEXT_PUBLIC_API_URL=http://localhost:8080`을 둔다.

### 운영

실제 운영 값은 EC2의 `/etc/blog/runtime.env` 한 파일에서 관리한다. 권한은 반드시
`root:root`, `600`으로 유지한다.

```dotenv
SPRING_PROFILES_ACTIVE=prod
DB_NAME=blog
DB_USER=blog
DB_PASSWORD=<운영 DB 사용자 비밀번호>
DB_ROOT_PASSWORD=<운영 DB root 비밀번호>
DB_POOL_SIZE=5
JAVA_TOOL_OPTIONS=-Xms128m -Xmx384m
JWT_SECRET=<32바이트 이상의 임의 문자열>
CORS_ALLOWED_ORIGINS=https://kimdongwon.me,https://www.kimdongwon.me
```

`DB_URL`, `JPA_DDL_AUTO`, `STORAGE_LOCATION`은 운영 Compose가 고정한다. 이미지 주소는
CodeDeploy가 `/etc/blog/images.env`의 `BACKEND_IMAGE`로 자동 관리하므로 직접 수정하지
않는다. 운영 프론트의 `NEXT_PUBLIC_API_URL`은 빈 문자열이며 동일 출처 `/api`를 사용한다.

환경변수를 바꾼 뒤 적용:

```bash
sudo chmod 600 /etc/blog/runtime.env /etc/blog/images.env
sudo /opt/blog/deploy-service.sh "$(sudo sed -n 's/^BACKEND_IMAGE=//p' /etc/blog/images.env)"
```

`JWT_SECRET`을 바꾸면 기존 관리자 로그인 쿠키가 무효화된다. DB 비밀번호를 임의로 바꾸면
MariaDB 내부 사용자 비밀번호와 불일치할 수 있으므로 단순 파일 수정만으로 교체하지 않는다.

## 4. 자동 배포

GitHub Actions는 장기 Access Key 대신 OIDC로 `blog-github-actions` 역할을 잠시 맡는다.
SSM, SSH GitHub Secret, 셀프 호스트 러너, EventBridge는 사용하지 않는다.

### 프론트엔드

`master` push → Node 20 빌드 → `out/`을 S3에 동기화 → CloudFront `/*` 무효화 순서다.
해시가 있는 `/_next/static/*`는 1년 immutable 캐시를 사용하고, HTML 등 나머지는
`no-cache`로 업로드한다.

GitHub Repository Variables:

```text
AWS_DEPLOY_ROLE_ARN=arn:aws:iam::847835841110:role/blog-github-actions
AWS_REGION=ap-northeast-2
FRONTEND_BUCKET=blog-frontend-847835841110
CLOUDFRONT_DISTRIBUTION_ID=E9RA6FWWCXEPE
```

### 백엔드

`master` push → 테스트/이미지 빌드 → 커밋 SHA 태그로 ECR push → 작은 CodeDeploy 묶음을
S3에 업로드 → CodeDeploy Agent가 수신 → EC2에서 이미지 pull과 컨테이너 교체 순서다.
헬스체크가 실패하면 `deploy-service.sh`가 직전 이미지로 되돌린다.

GitHub Repository Variables:

```text
AWS_DEPLOY_ROLE_ARN=arn:aws:iam::847835841110:role/blog-github-actions
AWS_REGION=ap-northeast-2
ECR_REPOSITORY=blog-backend
CODEDEPLOY_APPLICATION=blog-backend
CODEDEPLOY_DEPLOYMENT_GROUP=blog-backend-production
CODEDEPLOY_BUCKET=blog-codedeploy-847835841110
```

수동 재배포는 GitHub Actions에서 해당 `Deploy` workflow의 `Run workflow`를 누른다.
운영 배포 전 작업 브랜치나 PR에서 verify workflow가 통과해야 한다.

## 5. 운영 확인과 장애 대응

외부 확인:

```bash
curl -I https://kimdongwon.me/
curl -s https://kimdongwon.me/api/actuator/health
```

EC2 접속 후 확인:

```bash
sudo docker compose \
  --env-file /etc/blog/runtime.env \
  --env-file /etc/blog/images.env \
  -f /opt/blog/compose.production.yaml ps
sudo journalctl -u nginx -u codedeploy-agent --since '1 hour ago'
sudo tail -n 200 /var/log/nginx/error.log
```

CodeDeploy 실패 시 AWS 콘솔의 배포 이벤트와 EC2의
`/var/log/aws/codedeploy-agent/`를 먼저 확인한다. 프론트 파일이 갱신되지 않으면 GitHub
Actions의 S3 sync 성공 여부를 확인한 뒤 CloudFront에서 `/*` 무효화를 한 번 실행한다.
가비아 DNS TTL은 CloudFront 주소 전환에만 영향을 주며 정적 파일 캐시 TTL과 별개다.

공개 웹 서비스에는 `.env`, WordPress, 경로 순회 같은 자동 스캔이 상시 들어온다. 이전
서버 로그에서도 같은 유형이 확인됐지만 Nginx 차단 또는 404로 끝났고 침해 성공 흔적은
없었다. 운영 비밀 파일은 웹 루트가 아닌 `/etc/blog`에 두고, EC2 80번은 CloudFront에서만
허용하며, SSH 22번은 필요한 관리자 IP로만 제한한다.

## 6. 새 EC2 또는 다른 AWS 환경으로 복구

새 서버는 Amazon Linux 2023, Docker, Docker Compose, Nginx, CodeDeploy Agent가 필요하다.
EC2 역할에는 ECR pull과 CodeDeploy 묶음 S3 read 권한을 부여하고, 인스턴스 태그는
CodeDeploy 배포 그룹이 선택하는 값과 맞춘다.

초기화 스크립트는 대상이 새 서버인지 확인한 후 EC2에서 실행한다.

```bash
sudo env BLOG_BOOTSTRAP_CONFIRM=NEW_BLOG_HOST AWS_REGION=ap-northeast-2 \
  bash deploy/bootstrap-host.sh
```

그다음 `/etc/blog/runtime.env`를 직접 만들고 첫 백엔드 workflow를 실행한다. 기존 데이터를
옮겨야 할 때만 아래 스크립트를 사용한다.

```bash
sudo env BLOG_RESTORE_CONFIRM=NEW_BLOG_EMPTY_DATABASE \
  deploy/restore-production-data.sh blog.sql.gz uploads.tar.gz

sudo env BLOG_SCHEMA_MIGRATION_CONFIRM=NEW_BLOG_DATABASE \
  deploy/migrate-legacy-schema.sh

# 썸네일 기능을 처음 배포할 때 한 번 실행(재실행해도 안전)
sudo deploy/migrate-post-thumbnail.sh
```

복원 스크립트는 빈 대상 DB에서만 실행한다. 다른 AWS 계정으로 옮길 때는 S3, ECR,
CloudFront/OAC, ACM 인증서, GitHub OIDC 역할, EC2 역할, CodeDeploy 역할·앱·그룹을 새로
만들고 위 GitHub Variables를 새 리소스 값으로 교체한다. 운영 비밀값은 GitHub가 아니라
새 EC2의 `/etc/blog/runtime.env`로 직접 이전한다.

## 7. Git에 넣지 않는 파일

두 저장소의 `.gitignore`는 다음 항목을 제외한다.

- `.env`, `.env.local`, `runtime.env`, `images.env`
- `.keys/`, `*.pem`, `*.key`
- `*.sql`, `*.sql.gz`, `*.tar.gz`, `migration-backup/`
- 백엔드 로컬 업로드 `data/`

현재 개인 키와 마이그레이션 백업은 두 Git 저장소 바깥에서 사용자가 따로 보관한다. 백업을
장기간 유지할 경우 암호화된 외장 저장소나 비공개 클라우드 저장소에 한 번 더 복사한다.
