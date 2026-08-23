# 백엔드 성능 기준선 (2026-08-23)

## 1. 목적

개인 블로그의 현재 규모에서 과도한 모니터링이나 부하 테스트 없이 개선 전 기준선을 남긴다.
이 문서는 서버의 최대 처리량을 평가하지 않는다. 공개 endpoint의 단발 응답 특성과 코드상 쿼리
구조를 기록해 이후 변경이 실제로 요청 수, SQL 수, 응답 크기를 줄였는지 비교하는 용도다.

## 2. 측정 원칙과 범위

- 대상: `https://kimdongwon.me`
- 시각: 2026-08-23 저녁 (KST)
- 운영 요청 수: 총 10회
- 방식: 동시 요청이나 반복 부하 없이 순차 GET만 수행
- 쓰기, 로그인, 이미지 업로드, 댓글/좋아요 변경 요청은 수행하지 않음
- 첫 측정 7회는 요청마다 새 curl process를 사용했다.
- 추가 3회는 한 curl process에서 연결 재사용 여부를 확인했다.
- 결과는 단발 표본이므로 평균, p95, 서버 최대 성능으로 해석하지 않는다.

현재 데이터 규모는 운영 `/api/stats` 응답 기준 공개 글 19개, 좋아요 2개, 댓글 4개,
방문자 203명이다. 카테고리는 8개다.

### 측정 도구와 실행 명령

측정에는 macOS 기본 `/usr/bin/curl`을 사용했다. 브라우저 Lighthouse나 서버 내부 profiler가
아니라, 측정 PC에서 CloudFront와 EC2 origin까지 실제 HTTPS 요청을 보내는 외부 관점의 측정이다.

첫 7회는 아래 형식으로 endpoint마다 별도 curl process를 실행했다.

```bash
/usr/bin/curl \
  --max-time 10 \
  --connect-timeout 5 \
  --compressed \
  -sS \
  -o /dev/null \
  -w 'status=%{http_code} dns=%{time_namelookup}s connect=%{time_connect}s tls=%{time_appconnect}s ttfb=%{time_starttransfer}s total=%{time_total}s bytes=%{size_download}\n' \
  'https://kimdongwon.me/api/categories'
```

같은 형식으로 호출한 경로는 다음 7개다.

```text
/
/api/actuator/health
/api/posts?page=1&limit=10&sortBy=created_at
/api/categories
/api/stats
/api/admin/profile
/api/about
```

추가 3회는 하나의 curl process에 URL을 연속 전달해, 첫 요청 이후 연결이 재사용되는 경우를
확인했다.

```bash
/usr/bin/curl \
  --max-time 10 \
  --connect-timeout 5 \
  --compressed \
  -sS \
  -o /dev/null \
  -w 'url=%{url_effective} status=%{http_code} connect=%{time_connect}s tls=%{time_appconnect}s ttfb=%{time_starttransfer}s total=%{time_total}s bytes=%{size_download}\n' \
  'https://kimdongwon.me/api/posts?page=1&limit=10&sortBy=created_at' \
  'https://kimdongwon.me/api/categories' \
  'https://kimdongwon.me/api/stats'
```

운영 요청 수는 별도 process 7회와 연결 재사용 process의 URL 3개를 합한 총 10회다. 모든 요청은
GET이며 동시에 실행하지 않았다.

### curl 지표의 의미

| 지표 | 의미 |
|---|---|
| `time_namelookup` | DNS 조회 완료까지의 누적 시간 |
| `time_connect` | TCP 연결 완료까지의 누적 시간 |
| `time_appconnect` | TLS handshake 완료까지의 누적 시간 |
| `time_starttransfer` | 응답 첫 byte가 도착할 때까지의 누적 시간(TTFB) |
| `time_total` | 응답 body 다운로드 완료까지의 전체 시간 |
| `size_download` | curl이 다운로드한 응답 body byte 수 |

TTFB에는 DNS/TCP/TLS, 인터넷 구간, CloudFront, Nginx, Spring Boot, MariaDB 처리시간이 모두
포함된다. 따라서 이 측정만으로 Spring 또는 SQL의 순수 실행시간을 분리할 수 없다.

### 수행하지 않은 측정

- 반복 호출을 통한 평균, p95, p99 계산
- 동시 요청, spike, 최대 처리량 부하 테스트
- POST/PUT/DELETE, 로그인, 이미지 업로드 등 상태를 변경하는 요청
- 운영 서버 내부 CPU, heap, Hikari, MariaDB slow query 측정
- 로컬 Hibernate SQL log를 통한 실제 SQL 횟수 측정
- H2 또는 MariaDB에서의 SQL `EXPLAIN`

따라서 5장의 SQL 수는 측정 결과가 아니라 코드 구조로 계산한 예상치다.

## 3. 운영 단발 측정 결과

### 새 연결을 사용한 요청

| 경로 | 상태 | DNS | TCP | TLS 완료 | TTFB | 전체 | body bytes |
|---|---:|---:|---:|---:|---:|---:|---:|
| `/` | 200 | 15ms | 157ms | 313ms | 1,032ms | 1,033ms | 2,853 |
| `/api/actuator/health` | 200 | 4ms | 149ms | 307ms | 721ms | 721ms | 49 |
| `/api/posts?page=1&limit=10&sortBy=created_at` | 200 | 5ms | 148ms | 301ms | 609ms | 750ms | 31,650 |
| `/api/categories` | 200 | 4ms | 151ms | 315ms | 614ms | 615ms | 753 |
| `/api/stats` | 200 | 5ms | 150ms | 301ms | 584ms | 584ms | 70 |
| `/api/admin/profile` | 200 | 5ms | 146ms | 300ms | 575ms | 575ms | 327 |
| `/api/about` | 200 | 4ms | 150ms | 309ms | 596ms | 596ms | 175 |

각 API의 TTFB에는 DNS, TCP, TLS, CloudFront, 인터넷 구간과 origin 처리가 모두 포함된다.
따라서 위 값에서 Spring 처리시간만 분리할 수 없다. 이 표본에서 TLS 완료 후 첫 byte까지의 추가
시간은 대략 275~414ms였다.

### 한 client에서 연결 재사용

| 순서 | 경로 | 새 연결 | TTFB | 전체 | body bytes |
|---|---|---:|---:|---:|---:|
| 1 | 게시글 목록 | 필요 | 1,831ms | 1,961ms | 31,650 |
| 2 | 카테고리 | 재사용 | 293ms | 293ms | 753 |
| 3 | 통계 | 재사용 | 443ms | 443ms | 70 |

첫 요청의 TCP/TLS 연결이 평소보다 느린 네트워크 표본이었으므로 1,961ms를 게시글 API의 일반적인
성능으로 보지 않는다. 연결이 재사용된 카테고리와 통계는 약 293ms, 443ms였다. 표본이 각 1회라
두 endpoint의 우열 역시 결론 내리지 않는다.

## 4. 현재 요청 구조 기준선

### 익명 사용자의 홈 최초 진입

`frontend/app/page.tsx`와 `components/header.tsx` 기준으로 최소 다음 요청이 발생한다.

1. 게시글 목록
2. 인기 글 목록
3. 카테고리
4. 통계
5. 프로필
6. 방문자 추적
7. Header 인증 확인

홈의 1~5는 현재 순차 실행된다. 카테고리, 검색, 페이지, 정렬 조건이 바뀌면 1~6이 다시 실행된다.
따라서 현재 체감 지연은 개별 API의 처리시간보다 요청 시작 waterfall과 불변 데이터 재요청의 영향을
크게 받는다.

### 게시글 목록 payload

10개 목록의 body는 curl 기준 31,650 bytes로, 다른 공개 JSON endpoint보다 훨씬 크다. 현재
`PostResponse`가 목록에서도 `LONGTEXT content`를 포함하기 때문이다. HTTP content encoding은 이번
측정에서 별도 확인하지 않았으므로 이 값은 curl이 보고한 실제 download body 크기로만 기록한다.

## 5. 코드 기반 SQL 기준선

아래 수치는 운영 SQL log로 센 값이 아니라 현재 Repository/Service 구현에서 계산한 예상치다.

| endpoint | 예상 SQL 구조 | 개선 기준 |
|---|---|---|
| 게시글 목록 | page content 1 + count 1 + lazy category 최대 C | summary projection으로 목록/count 외 lazy 조회 0 |
| 카테고리 | category 1 + 카테고리별 공개 글 count N | 집계 projection 1 |
| 통계 | 공개 글, 좋아요, 댓글, distinct 방문자 count 각 1 = 4 | 현재 규모에서는 유지 또는 짧은 cache |
| 프로필 | profile 조회 1 | 유지 |
| 소개 | 최신 about 조회 1 | 유지 |
| 댓글 관리자 목록 | 전체 댓글 1 + 서로 다른 post id마다 조회 P | pagination + comment/post projection |
| 좋아요 토글 | post + like 조회 + insert/delete + count + post update | unique 제약과 원자적 증감 |

`C`는 결과에서 실제 접근하는 서로 다른 카테고리 수, `N`은 전체 카테고리 수, `P`는 댓글이 달린
서로 다른 게시글 수다. 일반 JPQL join은 fetch join이 아니므로 lazy association 초기화를 보장하지
않는다.

현재 운영 카테고리 8개를 기준으로 `/api/categories`는 코드상 1 + 8, 즉 약 9 SQL 후보가 있다.
정확한 횟수는 개선 작업 때 로컬 Hibernate SQL log로 한 번 확인하면 충분하다.

## 6. 기준선에서 확인된 우선순위

### 바로 진행

1. 게시글 목록 전용 `PostSummaryResponse`와 projection을 만들어 content를 제외한다.
2. 게시글 목록의 category name을 projection에서 함께 가져와 lazy 추가 조회를 제거한다.
3. 카테고리별 post count를 단일 집계 projection으로 바꾼다.
4. 프론트 홈 요청을 병렬화하고, 필터 변경 시 게시글 목록만 재요청한다.
5. 방문자 추적을 최초 진입 한 번으로 분리한다.

### 기능 규모가 커질 때 진행

- 관리자 댓글 pagination
- 공개 통계의 짧은 TTL cache
- 검색 FULLTEXT 검토
- 이미지 variant 생성과 S3 직접 업로드

### 현재 보류

- Prometheus/Grafana 상시 운영
- p95/p99를 위한 대량 반복 요청
- 운영 복제 서버와 대규모 부하 테스트
- Redis, 검색 전용 서버, Hikari/GC 미세 튜닝

## 7. 변경 후 비교 방법

각 개선 PR에서 운영 반복 측정 대신 아래만 기록한다.

- 목록 JSON에서 `content`가 사라졌는지 API 계약 테스트
- 로컬 SQL log에서 목록과 카테고리 API의 SQL 수
- 동일 fixture 10개 기준 JSON byte 크기
- 프론트 Network에서 홈 최초 요청 수와 병렬 시작 여부
- 기존 백엔드 테스트 전체 통과

운영 배포 후에는 기능 확인을 겸해 해당 공개 endpoint를 1~2회만 호출하고, 응답 상태와 큰 회귀가
없는지만 확인한다. 정기적인 합성 트래픽은 만들지 않는다.
