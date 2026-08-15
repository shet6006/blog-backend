# 운영 배포 하네스

프론트엔드는 이 하네스를 사용하지 않는다. 프론트 GitHub Actions가 Next.js `out/`을 비공개
S3에 동기화하고 CloudFront 캐시를 무효화한다.

백엔드만 다음 구조로 배포한다.

```text
GitHub Actions → ECR → CodeDeploy → EC2 Docker Compose
```

CodeDeploy Agent가 작은 배포 묶음을 폴링해 받고 ECR에서 커밋 SHA 이미지를 내려받는다.
`deploy-service.sh`는 MariaDB를 유지한 채 백엔드만 교체하고 healthcheck 실패 시 이전 이미지로
복구한다. SSM, GitHub SSH 키, EC2 셀프 호스트 러너는 사용하지 않는다.

EC2 구성:

- Host Nginx `:80` → `127.0.0.1:8080`
- Spring Boot 컨테이너 → MariaDB 컨테이너
- `/etc/blog/runtime.env`: 운영 비밀값, 권한 `600`
- `/etc/blog/images.env`: 현재 백엔드 이미지 URI, 권한 `600`
- `/srv/blog/mysql`: MariaDB 영구 데이터
- `/srv/blog/uploads`: 업로드 영구 파일

백엔드 GitHub Actions Variables:

- `AWS_REGION`
- `AWS_DEPLOY_ROLE_ARN`
- `ECR_REPOSITORY`
- `CODEDEPLOY_APPLICATION`
- `CODEDEPLOY_DEPLOYMENT_GROUP`
- `CODEDEPLOY_BUCKET`

실행 순서는 `docs/tomorrow-deployment-runbook.md`를 따른다.
