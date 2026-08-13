# 새 AWS 목표 아키텍처

```text
GitHub Actions
  ├─ OIDC 임시 권한으로 AWS 배포 역할 사용
  ├─ 프론트 정적 Nginx 이미지 빌드 → ECR
  ├─ Spring Boot 이미지 빌드 → ECR
  └─ SSM Run Command로 배포 지시
                         │
사용자 → 가비아 DNS → CloudFront(HTTPS) → EC2:80
                                      ├─ frontend(Nginx 정적 파일)
                                      │     └─ /api/* → backend:8080
                                      ├─ backend(Spring Boot)
                                      └─ db(MariaDB 10.5)
EC2 영구 경로
  ├─ /srv/blog/mysql
  └─ /srv/blog/uploads
```

## IAM 역할 분리

1. `BlogEc2Role`: EC2만 사용한다. SSM 등록, ECR 이미지 pull, 필요한 비밀값 읽기 권한.
2. `BlogGitHubDeployRole`: GitHub Actions만 사용한다. ECR push와 지정 EC2에 SSM 명령 전송 권한.
3. 사람의 관리자 권한: AWS 브라우저 로그인 또는 IAM Identity Center의 임시 권한 사용.

IAM 사용자 Access Key와 SSH 개인키를 배포에 사용하지 않는다. 보안그룹에는 22번을 만들지
않으며, EC2 관리 접속은 Session Manager로 수행한다.

## 비용에 영향을 주는 구성

- EC2와 EBS: 주요 고정 비용
- CloudFront/전송량: 사용량 기반
- ECR: 저장된 이미지 용량 기반. 수명주기 정책으로 최근 이미지만 보존
- SSM Session Manager와 Run Command 기본 기능: EC2에서 추가 요금 없음
- VPC Interface Endpoint: 비용이 있으므로 이 소규모 구성에서는 사용하지 않음
- NAT Gateway: 비용이 크므로 사용하지 않음
- Secrets Manager: 비밀 개수와 API 호출에 따라 소액 과금. 비용 우선이면 SecureString
  Parameter Store로 대체할 수 있음
