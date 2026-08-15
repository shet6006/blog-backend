# 목표 아키텍처

```text
GitHub Actions
  ├─ frontend: Next.js 정적 빌드 → S3 → CloudFront invalidation
  └─ backend: Docker 이미지 → ECR → CodeDeploy

사용자 → CloudFront(HTTPS)
  ├─ /*     → 비공개 S3 정적 프론트
  └─ /api/* → Blog EC2:80
                 └─ Host Nginx → Spring Boot 컨테이너:8080
                                      └─ MariaDB 컨테이너

EC2 영구 데이터
  ├─ /srv/blog/mysql
  └─ /srv/blog/uploads
```

프론트에는 ECR, CodeDeploy, EC2 컨테이너가 필요 없다. CloudFront Function이 확장자 없는
정적 경로를 `/index.html`로 변환한다. 백엔드 배포에만 CodeDeploy Agent를 사용한다.

SSM, 배포용 SSH 키, 셀프 호스트 GitHub Runner, EventBridge는 사용하지 않는다. ECR push
자체가 EC2를 깨우는 것이 아니라 CodeDeploy Agent가 배포 작업을 폴링한다.
