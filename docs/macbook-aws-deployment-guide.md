# MacBook에서 새 AWS 계정에 블로그 구축하기

이 문서는 새 MacBook에서 빈 상태로 시작해 GitHub 코드를 받고, 새 AWS 계정에 SSH 없이
배포하는 순서다. 명령의 `<...>`는 자신의 값으로 바꾼다. 운영 비밀번호는 터미널 기록이나
GitHub에 붙여 넣지 않는다.

## 0. 준비값

- AWS 리전: `ap-northeast-2`
- GitHub 사용자: `shet6006`
- 저장소: `blog`, `blog-backend`
- 도메인: `kimdongwon.me`, `www.kimdongwon.me`
- DNS 업체: 가비아

## 1. MacBook 개발 환경

```bash
xcode-select --install
/bin/bash -c "$(curl -fsSL https://raw.githubusercontent.com/Homebrew/install/HEAD/install.sh)"
brew install git gh awscli
mkdir -p ~/Projects/blog-migration && cd ~/Projects/blog-migration
gh auth login
gh repo clone shet6006/blog frontend
gh repo clone shet6006/blog-backend backend
cd frontend && git switch migration/docker-ssm
cd ../backend && git switch migration/docker-ssm
```

Docker Desktop도 설치한다. Apple Silicon Mac에서도 운영 이미지는 GitHub의 Linux 러너가
빌드하므로 EC2 CPU 아키텍처 불일치를 피할 수 있다.

로컬 확인:

```bash
cd ~/Projects/blog-migration/backend
./mvnw test
docker compose up --build
```

다른 터미널:

```bash
cd ~/Projects/blog-migration/frontend
cp .env.example .env.local
npm ci --legacy-peer-deps
npm run dev
```

## 2. 새 AWS 계정 기본 보안

1. Root 사용자 MFA를 활성화한다.
2. Root Access Key가 있다면 삭제한다.
3. 결제 알림과 월 예산 알림을 만든다.
4. 일상 작업에 Root를 사용하지 않는다.
5. 가능하면 IAM Identity Center에서 관리자 사용자를 만들고 임시 권한으로 로그인한다.

개인 단일 계정에서 Identity Center 구성이 부담되면 초기 구축 동안만 브라우저 기반 AWS CLI
임시 로그인을 사용한다.

```bash
aws login --profile blog-new --region ap-northeast-2
aws sts get-caller-identity --profile blog-new
export AWS_PROFILE=blog-new
export AWS_REGION=ap-northeast-2
export AWS_ACCOUNT_ID=$(aws sts get-caller-identity --query Account --output text)
```

## 3. IAM: GitHub OIDC 공급자

AWS Console → IAM → Identity providers → Add provider:

- Provider type: OpenID Connect
- Provider URL: `https://token.actions.githubusercontent.com`
- Audience: `sts.amazonaws.com`

OIDC는 장기 AWS Access Key 없이 GitHub가 배포 순간에만 임시 권한을 받게 한다.

## 4. IAM: EC2 인스턴스 역할

IAM → Roles → Create role → AWS service → EC2를 선택하고 역할 이름을 `BlogEc2Role`로 한다.

관리형 정책:

- `AmazonSSMManagedInstanceCore`
- `AmazonEC2ContainerRegistryReadOnly`

비밀값을 Secrets Manager에 넣는다면 아래 인라인 정책을 추가한다. 계정 ID와 리전을 바꾼다.

```json
{
  "Version": "2012-10-17",
  "Statement": [{
    "Effect": "Allow",
    "Action": ["secretsmanager:GetSecretValue"],
    "Resource": "arn:aws:secretsmanager:ap-northeast-2:<AWS_ACCOUNT_ID>:secret:blog/prod/*"
  }]
}
```

## 5. ECR 저장소

```bash
aws ecr create-repository --repository-name blog-frontend \
  --image-scanning-configuration scanOnPush=true
aws ecr create-repository --repository-name blog-backend \
  --image-scanning-configuration scanOnPush=true
```

각 저장소에 최근 10개 이미지만 남기는 수명주기 정책을 설정한다.

```json
{
  "rules": [{
    "rulePriority": 1,
    "description": "최근 이미지 10개 유지",
    "selection": {
      "tagStatus": "any",
      "countType": "imageCountMoreThan",
      "countNumber": 10
    },
    "action": {"type": "expire"}
  }]
}
```

## 6. 네트워크와 EC2

소규모 단일 서버 기준으로 기본 VPC의 public subnet을 사용한다. NAT Gateway와 유료 VPC
Endpoint는 만들지 않는다.

보안그룹 `blog-web-sg`:

- TCP 80: 우선 본인 IP만 허용하여 테스트
- TCP 22: 생성하지 않음
- TCP 3306, 8080: 외부에 열지 않음
- 아웃바운드: 기본 허용

EC2 생성:

- Amazon Linux 2023
- 초기에는 `t3.small` 권장. 기존 `t3.micro`는 메모리 여유가 적었음
- 20GiB 이상 암호화 gp3
- IAM Instance Profile: `BlogEc2Role`
- Public IP 사용
- Key pair 없이 시작 가능. 관리는 SSM 사용
- 종료 방지 설정 권장
- 루트 볼륨 `DeleteOnTermination` 여부를 이해하고 정기 스냅샷/백업 설정

인스턴스 생성 후 Systems Manager → Fleet Manager에서 `Online`인지 확인한다. 안 보이면 IAM
역할, SSM Agent, 인터넷 아웃바운드를 확인한다.

## 7. EC2 하네스 설치

Mac에서 배포 파일을 S3로 옮기거나, 초기 1회 SSM Run Command 문서에 내용을 전달한다.
최종 서버에는 다음 파일이 있어야 한다.

```text
/opt/blog/compose.production.yaml
/opt/blog/deploy-service.sh
/etc/blog/runtime.env       # chmod 600
/etc/blog/images.env        # chmod 600
/srv/blog/mysql
/srv/blog/uploads
```

저장소 원본:

- `backend/deploy/bootstrap-host.sh`
- `backend/deploy/compose.production.yaml`
- `backend/deploy/deploy-service.sh`

`bootstrap-host.sh`를 SSM Run Command로 실행하여 Docker와 디렉터리를 준비한다. 이후 두
하네스 파일을 `/opt/blog`에 설치하고 실행 파일에 `chmod 755`를 적용한다.

## 8. 운영 비밀값

Secrets Manager에 `blog/prod/runtime`이라는 JSON 비밀을 만든다.

```json
{
  "DB_NAME": "blog",
  "DB_USER": "blog",
  "DB_PASSWORD": "<긴 무작위 암호>",
  "DB_ROOT_PASSWORD": "<서로 다른 긴 무작위 암호>",
  "JWT_SECRET": "<32바이트 이상의 무작위 문자열>",
  "CORS_ALLOWED_ORIGINS": "https://kimdongwon.me,https://www.kimdongwon.me"
}
```

비밀값을 조회해 `/etc/blog/runtime.env`로 변환하는 초기화 스크립트를 SSM으로 실행한다.
명령 출력이나 CloudWatch 로그에 값이 나타나지 않게 주의한다. 파일 권한은 `600`, 소유자는
root로 한다.

## 9. IAM: GitHub 배포 역할

역할 이름 `BlogGitHubDeployRole`, Trusted entity는 Web identity,
`token.actions.githubusercontent.com`, Audience `sts.amazonaws.com`을 선택한다.

처음에는 `master`만 배포하도록 두 저장소를 명시한다. `<AWS_ACCOUNT_ID>`를 교체한다.

```json
{
  "Version": "2012-10-17",
  "Statement": [{
    "Effect": "Allow",
    "Principal": {
      "Federated": "arn:aws:iam::<AWS_ACCOUNT_ID>:oidc-provider/token.actions.githubusercontent.com"
    },
    "Action": "sts:AssumeRoleWithWebIdentity",
    "Condition": {
      "StringEquals": {
        "token.actions.githubusercontent.com:aud": "sts.amazonaws.com",
        "token.actions.githubusercontent.com:sub": [
          "repo:shet6006/blog:ref:refs/heads/master",
          "repo:shet6006/blog-backend:ref:refs/heads/master"
        ]
      }
    }
  }]
}
```

권한 정책은 ECR 로그인·두 저장소 push와 특정 EC2에 대한 SSM 명령으로 제한한다.

```json
{
  "Version": "2012-10-17",
  "Statement": [
    {
      "Effect": "Allow",
      "Action": "ecr:GetAuthorizationToken",
      "Resource": "*"
    },
    {
      "Effect": "Allow",
      "Action": [
        "ecr:BatchCheckLayerAvailability",
        "ecr:CompleteLayerUpload",
        "ecr:InitiateLayerUpload",
        "ecr:PutImage",
        "ecr:UploadLayerPart"
      ],
      "Resource": [
        "arn:aws:ecr:ap-northeast-2:<AWS_ACCOUNT_ID>:repository/blog-frontend",
        "arn:aws:ecr:ap-northeast-2:<AWS_ACCOUNT_ID>:repository/blog-backend"
      ]
    },
    {
      "Effect": "Allow",
      "Action": "ssm:SendCommand",
      "Resource": [
        "arn:aws:ec2:ap-northeast-2:<AWS_ACCOUNT_ID>:instance/<INSTANCE_ID>",
        "arn:aws:ssm:ap-northeast-2::document/AWS-RunShellScript"
      ]
    },
    {
      "Effect": "Allow",
      "Action": ["ssm:GetCommandInvocation", "ssm:ListCommandInvocations"],
      "Resource": "*"
    }
  ]
}
```

GitHub 두 저장소에 `AWS_ROLE_ARN`, `AWS_REGION`, `EC2_INSTANCE_ID`를 Variables 또는
Environments로 등록한다. AWS Access Key와 SSH 키는 등록하지 않는다.

## 10. 데이터 복원

최종 이전 때 기존 서비스의 쓰기를 중단하고 MariaDB 전체 덤프와 업로드 파일을 만든다.
덤프 SHA-256을 기록한다. 새 서버의 `/srv/blog/mysql`, `/srv/blog/uploads`에 복원한다.

검증 항목:

- DB 테이블별 행 수
- 글, 댓글, 좋아요, 방문자/조회수 데이터
- 관리자 로그인
- 업로드 이미지 수와 해시
- 한글과 시간대

DB와 파일이 준비된 뒤에만 애플리케이션 컨테이너를 기동한다.

## 11. CloudFront와 인증서

CloudFront 인증서는 반드시 `us-east-1`에서 새로 발급한다.

1. ACM 버지니아 북부에서 `kimdongwon.me`, `www.kimdongwon.me` 인증서 요청
2. 가비아 DNS에 검증 CNAME 추가
3. 새 CloudFront 배포 생성
4. Origin은 새 EC2 Public DNS, Origin protocol은 HTTP
5. Alternate domain names 두 개 추가
6. 새 ACM 인증서 연결
7. HTTP를 HTTPS로 리디렉션
8. `/api/*`는 쿠키·필요 헤더·모든 메서드를 원본에 전달하고 캐시 비활성화
9. 정적 페이지는 필요에 맞게 캐시
10. EC2 보안그룹 80번 소스를 CloudFront 관리형 Prefix List로 제한

## 12. DNS 전환

새 CloudFront 기본 도메인에서 모든 기능을 먼저 검사한다. 검증 후 가비아에서 루트와 `www`
CNAME을 새 CloudFront 도메인으로 바꾼다. 기존 TTL은 600초였다.

전환 후 확인:

```bash
curl -I https://kimdongwon.me/
curl https://kimdongwon.me/api/actuator/health
```

글 목록/상세, 검색, 댓글, 좋아요, 조회수 증가, 관리자 로그인, 글 작성·수정·삭제, 이미지
업로드를 브라우저에서 검사한다.

## 13. 롤백과 마무리

애플리케이션 배포 실패는 `deploy-service.sh`가 이전 이미지로 자동 복원한다. DNS 전환 문제는
가비아 레코드를 이전 CloudFront 도메인으로 되돌린다.

안정화 후:

1. GitHub의 `EC2_HOST`, `EC2_USER`, `EC2_SSH_KEY` Secrets 삭제
2. 새 보안그룹에 22번이 없는지 확인
3. ECR 수명주기 정책과 예산 알림 확인
4. DB와 업로드 정기 백업 및 실제 복구 테스트
5. 기존 AWS는 일정 기간 보존 후 별도 승인 아래 종료

## 공식 참고자료

- [AWS IAM: OIDC 자격 증명 공급자 만들기](https://docs.aws.amazon.com/IAM/latest/UserGuide/id_roles_providers_create_oidc.html)
- [AWS IAM: OIDC 역할 만들기](https://docs.aws.amazon.com/IAM/latest/UserGuide/id_roles_create_for-idp_oidc.html)
- [AWS Systems Manager: 인스턴스 권한 구성](https://docs.aws.amazon.com/systems-manager/latest/userguide/setup-instance-permissions.html)
- [AWS Systems Manager 관리형 정책](https://docs.aws.amazon.com/systems-manager/latest/userguide/security-iam-awsmanpol.html)
- [GitHub Actions에서 AWS OIDC 구성](https://docs.github.com/en/actions/how-tos/secure-your-work/security-harden-deployments/oidc-in-aws)
