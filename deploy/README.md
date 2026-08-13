# 운영 배포 하네스

운영 서버에서는 Git 저장소를 내려받거나 애플리케이션을 빌드하지 않습니다. GitHub Actions가 프론트엔드와 백엔드의 변경 불가능한 이미지를 만들고, 커밋 SHA를 태그로 ECR에 올린 뒤 SSM Run Command를 호출합니다.

SSM은 다음 명령을 실행합니다.

```bash
sudo /opt/blog/deploy-service.sh backend <immutable-ecr-image-uri>
```

프론트엔드는 같은 형식으로 `frontend`를 지정합니다. 스크립트는 배포가 겹치지 않게 잠그고, 지정된 서비스만 내려받은 뒤 상태 검사를 기다립니다. 정상화되지 않으면 이전 이미지로 자동 복구합니다.

서버의 파일 구성:

- `/opt/blog/compose.production.yaml`: 비밀값이 없는 스택 정의
- `/opt/blog/deploy-service.sh`: 배포와 롤백 로직
- `/etc/blog/runtime.env`: 권한 600, AWS 보안 저장소에서 가져온 실행 환경 변수
- `/etc/blog/images.env`: 권한 600, 현재 이미지 URI
- `/srv/blog/mysql`: 영구 MariaDB 데이터
- `/srv/blog/uploads`: 영구 업로드 파일

22번 포트는 필요하지 않습니다. EC2는 SSM으로 명령을 받고 인스턴스 역할을 사용해 ECR 이미지를 내려받습니다.
