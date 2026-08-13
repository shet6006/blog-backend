# 기존 운영 아키텍처

2026-08-13에 실제 AWS와 EC2를 읽기 전용으로 조사한 결과다. 비밀값은 기록하지 않았다.

## 요청 흐름

```text
사용자
  → 가비아 DNS(kimdongwon.me, www.kimdongwon.me)
  → CloudFront(HTTPS 종료, ACM 인증서)
  → 서울 리전 EC2의 Nginx:80
      ├─ /      → Next.js:3000
      └─ /api/* → Spring Boot:8080
                     ├─ MariaDB:3306
                     └─ 로컬 업로드 파일
```

## AWS와 서버 현황

- 리전: 서울 `ap-northeast-2`
- Amazon Linux 2023 `t3.micro` EC2 한 대
- 8GiB 비암호화 gp3 루트 볼륨 하나
- 인스턴스 종료 시 루트 볼륨도 삭제되는 설정
- RDS, Elastic IP, EC2 IAM 역할 없음
- SSM Agent는 실행 중이지만 IAM 역할이 없어 관리 노드로 등록되지 않음
- SSH 22번이 `0.0.0.0/0`에 공개됨
- 80번은 CloudFront 관리형 Prefix List에서만 접근 가능
- Nginx 1.28, Node.js 20.19, Java 17, MariaDB 10.5
- Next.js와 Spring Boot는 PM2로 실행
- DB 데이터 약 125MiB, 업로드 파일은 당시 1개
- 루트 디스크 사용률 82%, 여유 공간 약 1.5GiB

## 기존 배포 방식

두 저장소의 `master` 푸시가 GitHub Actions를 실행한다. Actions에는 EC2 주소, 사용자,
SSH 개인키가 Secrets로 저장되어 있다. Actions가 EC2에 SSH로 접속하여 `git pull`, 서버
내 빌드, PM2 재시작을 수행한다. 새 방식이 검증될 때까지만 유지한다.
