# 마이그레이션 진행 현황

## 완료

- [x] 두 GitHub 저장소 조사 및 마이그레이션 브랜치 생성
- [x] 기존 AWS/EC2/CloudFront/DNS/DB 구조 확인
- [x] EC2 내부 MariaDB와 업로드 위치 확인
- [x] 로컬·운영 환경변수 계약 통합
- [x] 백엔드 테스트와 Docker 이미지 검증
- [x] 프론트 정적 export, Nginx 이미지와 URL 스모크 테스트 검증
- [x] SSM 배포·헬스체크·자동 롤백 하네스 검증
- [x] 운영에 영향 없는 Draft PR 생성

## 사용자가 새 AWS에서 수행

- [ ] 새 계정 보안 초기화와 관리자 로그인 구성
- [ ] GitHub OIDC 공급자와 배포 역할 생성
- [ ] EC2 인스턴스 역할 생성
- [ ] ECR 저장소, VPC/보안그룹, EC2 생성
- [ ] 비밀값 등록 및 하네스 설치
- [ ] 기존 DB/업로드 백업과 새 서버 복원
- [ ] 기능 검증 후 가비아 DNS 전환
- [ ] 기존 SSH Secrets 제거와 구 계정 정리

상세 절차는 `docs/macbook-aws-deployment-guide.md`를 따른다.
