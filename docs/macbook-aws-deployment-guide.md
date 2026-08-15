# S3 정적 프론트와 ECR/CodeDeploy 백엔드로 블로그 배포하기

이 문서는 기존 운영 EC2를 보존하면서 새 AWS 계정에 블로그 전용 구성을 만드는 절차다.

```text
frontend: GitHub Actions → S3 → CloudFront
backend:  GitHub Actions → ECR → CodeDeploy → 새 Blog EC2
```

SSM, GitHub SSH Secret, EC2 셀프 호스트 러너, EventBridge는 사용하지 않는다. AWS와 EC2
변경은 각 단계에서 대상을 재확인한 뒤 사용자가 승인할 때만 수행한다.

## 1. 로컬 개발

백엔드는 Java 17과 `local` 프로필을 사용한다. `application-local.yml`의 H2 DB는 실행할
때마다 관리자와 샘플 데이터를 새로 만든다. 프론트엔드는 Node 20에서 개발 서버를 띄운다.

```bash
cd backend
./mvnw test

cd ../frontend
npm ci --legacy-peer-deps
npm run dev
```

운영 이미지는 GitHub Actions의 Linux 러너가 만들므로 로컬에서 이미지를 빌드할 필요는 없다.

## 2. 한 번만 만드는 AWS 리소스

- 정적 프론트용 비공개 S3 버킷
- CodeDeploy 배포 묶음용 비공개 S3 버킷
- ECR 저장소 `blog-backend`
- 백엔드 CodeDeploy 애플리케이션과 배포 그룹
- S3와 EC2를 origin으로 쓰는 CloudFront
- GitHub Actions용 OIDC 역할
- CodeDeploy 서비스 역할
- 새 Blog EC2에 연결할 인스턴스 역할

CodeDeploy 배포 그룹은 새 Blog EC2의 고유 태그로 대상을 한 대만 선택한다. 자동 롤백을 켜고
배포 실패 시 이전 배포로 돌아가도록 설정한다.

## 3. IAM 권한 범위

GitHub OIDC 역할에는 다음 권한만 준다.

- 백엔드 ECR 저장소에 이미지 push
- 프론트 S3 sync, CloudFront invalidation, CodeDeploy 묶음 S3 쓰기
- 지정된 CodeDeploy 애플리케이션의 배포 생성·조회

EC2 역할에는 다음 권한만 준다.

- 백엔드 ECR 저장소에서 이미지 pull
- 배포 묶음 S3 버킷에서 객체 읽기

CodeDeploy 서비스 역할에는 AWS 관리형 `AWSCodeDeployRole`을 사용한다. 장기 AWS Access
Key나 EC2 SSH 개인키를 GitHub에 등록하지 않는다.

## 4. 새 Blog EC2의 최초 준비

최초 한 번 새 Blog EC2에 접속해 다음을 준비한다.

1. Docker와 Docker Compose 플러그인 설치
2. CodeDeploy Agent 설치 및 시작
3. `/opt/blog`, `/etc/blog`, `/srv/blog/mysql`, `/srv/blog/uploads` 생성
4. `/etc/blog/runtime.env`, `/etc/blog/images.env` 생성 후 권한 `600` 적용

저장소의 `backend/deploy/bootstrap-host.sh`는 Docker와 디렉터리를 준비한다. 실행 전 반드시
대상 인스턴스가 새 Blog 인스턴스인지 확인한다. 이 스크립트는 DB와 업로드 데이터를
삭제하지 않는다.

Amazon Linux 2023의 CodeDeploy Agent 설치 예시는 다음과 같다.

```bash
sudo dnf install -y ruby wget
cd /tmp
wget https://aws-codedeploy-ap-northeast-2.s3.ap-northeast-2.amazonaws.com/latest/install
chmod +x install
sudo ./install auto
sudo systemctl enable --now codedeploy-agent
sudo systemctl status codedeploy-agent
```

## 5. 운영 환경변수

EC2의 `/etc/blog/runtime.env`에만 실제 값을 저장한다.

```ini
SPRING_PROFILES_ACTIVE=prod
DB_NAME=blog
DB_USER=blog
DB_PASSWORD=<운영 DB 암호>
DB_ROOT_PASSWORD=<운영 DB root 암호>
DB_POOL_SIZE=5
JWT_SECRET=<32바이트 이상의 무작위 문자열>
CORS_ALLOWED_ORIGINS=https://kimdongwon.me,https://www.kimdongwon.me
```

`/etc/blog/images.env`에는 최초 백엔드 이미지 URI를 둔다. 첫 실제 배포가 이를 커밋 SHA
이미지로 교체한다.

```ini
BACKEND_IMAGE=eclipse-temurin:17-jre-alpine
```

운영 데이터는 `/srv/blog/mysql`, 업로드는 `/srv/blog/uploads`에 유지된다. 애플리케이션
컨테이너를 교체해도 이 두 경로는 삭제되지 않는다.

## 6. GitHub Actions Variables

백엔드 저장소 Actions Variables:

```text
AWS_REGION=ap-northeast-2
AWS_DEPLOY_ROLE_ARN=<GitHub OIDC 역할 ARN>
ECR_REPOSITORY=blog-backend
CODEDEPLOY_APPLICATION=<백엔드 CodeDeploy 애플리케이션>
CODEDEPLOY_DEPLOYMENT_GROUP=<백엔드 배포 그룹>
CODEDEPLOY_BUCKET=<배포 묶음 S3 버킷>
```

프론트엔드 저장소 Actions Variables:

```text
AWS_REGION=ap-northeast-2
AWS_DEPLOY_ROLE_ARN=<GitHub OIDC 역할 ARN>
FRONTEND_BUCKET=<정적 프론트 S3 버킷>
CLOUDFRONT_DISTRIBUTION_ID=<새 CloudFront ID>
```

운영 비밀번호는 GitHub Variables나 Secrets에 넣지 않는다.

## 7. 최초 배포 순서

1. 백엔드 workflow를 수동 실행한다.
2. CodeDeploy 성공과 backend healthcheck를 확인한다.
3. 프론트엔드 workflow를 수동 실행한다.
4. `/`, `/api/actuator/health`, 게시글 조회·수정·로그인을 확인한다.
5. 정상 확인 후 두 저장소의 `master` push 자동 배포를 사용한다.

백엔드를 먼저 배포해 API를 확인한 다음 프론트 정적 파일을 배포한다.

## 8. 배포와 롤백 동작

GitHub Actions는 이미지를 `:<commit-sha>` 태그로 ECR에 올린다. 그 다음 CodeDeploy 배포
묶음에 이미지 URI를 담아 S3에 올리고 배포를 시작한다. EC2의 Agent가 이를 폴링해 받은 뒤
ECR 로그인, pull, 대상 컨테이너 교체, healthcheck를 수행한다.

`deploy-service.sh`는 상태 검사가 실패하면 `/etc/blog/images.env`를 이전 값으로 되돌리고
이전 컨테이너를 다시 실행한다. DB 컨테이너와 영구 볼륨은 교체하지 않는다.

## 9. 완료 후 제거할 기존 설정

새 배포가 성공한 뒤에만 GitHub의 `EC2_HOST`, `EC2_USER`, `EC2_SSH_KEY`를 제거한다. 기존
PM2 프로세스와 서버의 Git clone도 즉시 삭제하지 말고 새 컨테이너 배포가 안정화된 후 별도
승인을 받아 정리한다. 기존 AWS 리소스와 EC2는 자동으로 수정하거나 삭제하지 않는다.
