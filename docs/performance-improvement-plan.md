# 백엔드 성능 개선 계획

## 1. 목적과 운영 제약

이 문서는 Spring Boot API와 MariaDB의 성능 개선 백로그다. 현재 운영은 t3.micro 한 대에서 Nginx,
Spring Boot, MariaDB를 함께 실행하고 JVM heap은 128~384 MB, Hikari maximum pool size는 5다.
따라서 외부 캐시나 서버 증설보다 쿼리 수, 응답 크기, 인덱스, 메모리 사용량을 먼저 줄인다.

현재 병목 수치는 아직 계측하지 않았다. 아래 항목은 코드에서 확인한 병목 후보이며, 실제 적용 순서는
운영 측정 결과로 조정한다.

## 2. P0: 관측성과 기준선

### B-01. API와 DB 성능 지표 추가

**근거:** Actuator는 health만 노출하며 latency, error rate, JVM, Hikari, SQL 지표를 지속적으로 볼
수 없다.

**작업**

- Micrometer의 HTTP server duration, JVM heap/GC, CPU, Hikari active/pending 지표를 수집한다.
- 운영 로그에 request ID, method, normalized URI, status, elapsed time을 남기되 비밀번호, JWT,
  cookie, 댓글 본문은 기록하지 않는다.
- Hibernate statistics 또는 datasource proxy는 개발/부하 테스트 프로필에서만 켜서 endpoint별 SQL
  수와 slow query를 확인한다.
- MariaDB slow query log를 짧은 기간 활성화하고 `EXPLAIN` 결과를 개선 기록에 남긴다.
- 단일 인스턴스에 Prometheus까지 상주시킬지, CloudWatch/로그 기반으로 보낼지는 메모리 비용을
  비교한 뒤 선택한다.

**초기 SLO**

| 항목 | 목표 |
|---|---:|
| 공개 GET API p95 | 300ms 이하 |
| 쓰기 API p95 | 500ms 이하 |
| 5xx 비율 | 0.1% 미만 |
| Hikari pending | 정상 부하에서 0 유지 |
| 홈 구성 API 전체 SQL | endpoint 설계 후 예산 명시 |

### B-02. 재현 가능한 부하 테스트 작성

- k6 또는 Gatling으로 홈 목록, 글 상세, 댓글 조회, 좋아요 토글, 방문자 추적 시나리오를 만든다.
- 빈 DB뿐 아니라 게시글 1천/1만, 댓글 1만, 방문자 10만 수준의 fixture로 측정한다.
- t3.micro의 CPU credit을 고려해 짧은 spike와 15~30분 steady load를 구분한다.
- p50/p95/p99, error rate, DB query count, CPU, heap, GC, connection wait를 함께 저장한다.

## 3. P1: 데이터 접근과 응답 크기

### B-03. 게시글 목록 전용 projection/DTO 사용

**근거:** `PostService.findAll`은 `Post` entity 전체를 조회하고 `toResponse`가 `LONGTEXT content`까지
목록 응답에 포함한다. 목록 화면은 본문을 사용하지 않는다. 100개 limit에서는 DB I/O, heap,
JSON serialization과 네트워크 비용이 크게 증가할 수 있다.

**작업**

- `PostSummaryResponse`를 만들고 필요한 컬럼만 projection으로 조회한다.
- 목록 필드: id, title, excerpt, category id/name, slug, thumbnail, 공개 여부, 작성자, 좋아요/댓글 수,
  생성/수정 시각으로 제한한다.
- 상세 endpoint만 content와 GitHub URL 등 전체 필드를 반환한다.
- 관리자 목록도 상세 본문이 필요하지 않으면 같은 경량 projection을 사용한다.

**완료 조건:** 10개/100개 목록의 DB bytes, response bytes와 heap allocation이 감소하고 API 계약
테스트가 통과한다.

### B-04. 카테고리 N+1 쿼리 제거

**근거:** `CategoryService.findAll`은 카테고리를 한 번 조회한 뒤 각 카테고리마다
`countPostsByCategoryId`를 호출한다. 카테고리가 N개면 최소 N+1 SQL이다. Repository에
`findAllWithPostCount`가 있지만 count 값을 projection하지 않아 현재 서비스에서 사용되지 않는다.

**작업:** category와 공개 게시글 수를 `GROUP BY`한 DTO projection 한 번으로 반환한다. 공개 목록과
관리자 목록이 요구하는 count 기준이 다른지도 API 계약에 명시한다.

### B-05. 게시글-카테고리 lazy 접근 최적화

**근거:** 게시글 목록 변환에서 `p.getCategory().getName()`을 접근한다. 목록 Specification이 category
filter/search가 없으면 join하지 않으므로 결과 개수만큼 lazy query가 발생할 수 있다.

**작업:** B-03 projection query에서 category name을 함께 선택한다. 임시로 entity graph/fetch join을
쓸 경우 count query와 pagination이 깨지지 않는지 테스트한다.

### B-06. 필요한 복합 인덱스를 migration으로 관리

**근거:** entity에는 slug/name의 unique 외에 목록 정렬·좋아요·댓글·방문자 조회용 인덱스가 선언되어
있지 않고, 운영 스키마 변경이 개별 shell script에 분산되어 있다.

**우선 검토할 인덱스**

```sql
posts(is_public, created_at)
posts(is_public, likes_count)
posts(category_id, is_public, created_at)
comments(post_id, created_at)
likes(post_id, device_id) UNIQUE
visitors(ip_address, visit_date) UNIQUE
```

실제 컬럼 순서는 `EXPLAIN`과 cardinality로 확정한다. `LIKE '%term%'` 검색은 일반 B-tree를 타지 않으므로
데이터가 커질 때 MariaDB FULLTEXT(title, content) 또는 별도 검색 서비스를 비교한다. 현재 규모에서
외부 검색 엔진을 먼저 도입하지 않는다.

**작업:** Flyway 또는 Liquibase 하나를 도입해 인덱스와 스키마 버전을 코드로 관리한다. 운영 적용 전
백업, 예상 lock 시간, 롤백 SQL을 준비한다.

### B-07. 관리자 전체 목록에 pagination 적용

**근거:** `AdminCommentsController`와 `CommentRepository.findAllByOrderByCreatedAtDesc`는 모든 댓글을
메모리에 올린다. 각 post id에 대해 글을 찾아 제목 map을 만드는 추가 쿼리도 발생한다. 글별 댓글
조회도 `List` 전체 반환이다.

**작업**

- 관리자 댓글은 `Pageable`과 comment + post title projection을 사용한다.
- 공개 댓글도 기본 limit과 cursor 또는 page를 둔다.
- 관리자 게시글 limit 100의 UX와 최대치를 명시하고 서버 기본값을 보수적으로 둔다.

## 4. P1: 쓰기 경로와 동시성

### B-08. 좋아요 토글 쿼리 수와 경쟁 조건 개선

**근거:** 좋아요 토글은 게시글 조회, 기존 좋아요 조회, insert/delete, count, 게시글 count 갱신까지
여러 SQL을 실행한다. `(post_id, device_id)` unique 제약이 코드에서 보이지 않아 동시 요청 시 중복
좋아요가 생길 수 있다. count 후 entity 저장 방식은 lost update 위험도 있다.

**작업**

- `(post_id, device_id)` unique index를 필수로 둔다.
- 서비스 쓰기 메서드에 transaction 경계를 명확히 둔다.
- insert/delete 결과에 따라 `posts.likes_count = likes_count +/- 1` 원자적 update를 사용한다.
- 정합성 복구용 count 재계산 작업을 별도로 둔다.
- 중복 클릭과 동시에 반대 토글되는 요청에 대한 concurrency test를 작성한다.

### B-09. 댓글 count 갱신 원자화

**근거:** 댓글 생성/삭제 뒤 전체 `countByPostId`를 다시 실행하고 Post entity를 저장한다. 동시 쓰기에서
count가 뒤로 돌아갈 수 있고 댓글 수가 많을수록 매번 count 비용이 든다.

**작업:** 댓글 insert/delete와 `comments_count +/- 1`을 한 transaction에서 원자적으로 수행한다.
삭제된 row 수가 1일 때만 감소시키고 0 미만 방지를 둔다. 정기 또는 관리자용 reconcile 기능으로
denormalized count를 검증한다.

### B-10. 방문자 중복 확인을 DB 제약으로 보장

**근거:** `existsByIpAddressAndVisitDate` 후 insert는 두 요청 사이에 경쟁 조건이 있다. 홈 필터 변경마다
프론트가 추적을 재호출하는 문제도 현재 있다.

**작업:** `(ip_address, visit_date)` unique index와 insert-if-absent를 사용한다. 중복은 정상 결과로
처리하고 예외 로그를 쌓지 않는다. 개인정보 보관 정책에 맞춰 원본 IP 대신 keyed hash와 보존 기간도
별도로 검토한다.

## 5. P2: 캐시, 전송, 파일 처리

### B-11. 변경 빈도가 낮은 공개 조회 캐시

**후보:** 카테고리, 프로필, 소개, 통계, 인기 글 첫 페이지.

**작업**

- 우선 단일 JVM의 Caffeine cache로 짧은 TTL을 적용한다. t3.micro 단일 인스턴스에서는 Redis가
  필수는 아니다.
- 글/댓글/좋아요/프로필 변경 시 관련 cache key를 명시적으로 무효화한다.
- 인증 여부에 따라 응답이 달라지는 endpoint는 공개 캐시와 분리한다.
- 동일한 공개 GET에는 ETag/Last-Modified와 조건부 요청을 적용한다.
- CloudFront 캐시는 쿠키와 Authorization이 없는 공개 API만 별도 behavior로 검토한다.

### B-12. 통계 집계 비용 제한
 
**근거:** `/api/stats`는 호출마다 게시글, 좋아요, 댓글 count와 방문자 distinct count 등 최소 네 개의
집계 SQL을 실행한다. 방문자 테이블이 커지면 distinct가 특히 비싸진다.

**작업:** 짧은 TTL cache를 우선 적용한다. 규모가 커지면 일별 집계 table이나 이벤트 시 증분 counter를
사용하고 원본 count와 주기적으로 reconcile한다.

### B-13. HTTP 압축과 응답 캐시 헤더

- CloudFront/Nginx의 Brotli 또는 gzip 적용 여부를 실제 응답 헤더로 확인한다.
- JSON과 긴 Markdown 상세 응답을 압축한다.
- 공개 GET과 인증/쓰기 API의 Cache-Control을 분리한다.
- Nginx access log에 upstream response time을 추가한다.
- timeout을 늘려 느린 요청을 숨기지 말고 endpoint별 timeout과 최대 payload를 정한다.

### B-14. 이미지 업로드 최적화

**근거:** `ImageService`는 최대 10 MB 원본을 동기적으로 로컬 디스크에 복사하고 검증/리사이즈 없이
그대로 제공한다. 큰 파일은 업로드 thread와 디스크, 이후 사용자의 전송량을 모두 차지한다.

**작업**

- magic byte 기반 실제 이미지 검증과 최대 pixel dimension 제한을 추가한다.
- 원본 정책을 정하고 WebP/AVIF 및 thumbnail variant를 생성한다.
- 변환이 길어지면 bounded executor 또는 비동기 job으로 분리하며 queue 상한을 둔다.
- 파일 응답에 content type, content length, ETag, immutable cache header를 설정한다.
- 트래픽/가용성 요구가 커지면 S3 직접 업로드(presigned URL) + CloudFront 제공으로 이전한다.

## 6. P2: 런타임과 DB 설정

### B-15. Hikari와 Tomcat pool을 측정 기반 조정

현재 DB pool 5는 t3.micro에는 합리적인 시작점이지만 적정값은 요청 시간과 DB CPU에 따라 다르다.

- connection timeout, max lifetime, idle timeout을 명시하고 MariaDB timeout과 맞춘다.
- Hikari active/pending과 Tomcat threads를 같이 보고, DB pool만 무작정 늘리지 않는다.
- 느린 요청에서 connection을 오래 점유하는 SQL부터 개선한다.
- read-only transaction을 Category/About/Stats 등 조회 서비스에도 일관되게 적용한다.

### B-16. JVM 메모리와 GC 기준선 확립

- `-Xms128m -Xmx384m`에서 heap 사용량, allocation rate, GC pause, OOM 여유를 기록한다.
- Docker memory limit을 명시해 EC2 전체가 swap/OOM 상태로 가는 것을 막는다.
- 긴 게시글 목록의 content 제거와 이미지 처리 제한을 먼저 적용한 뒤 heap을 조정한다.
- 작은 heap에서 GC가 병목이면 Java 17 기본 G1 설정을 측정하고 조정한다. 근거 없이 GC 옵션을
  추가하지 않는다.

### B-17. 검색 전략 개선

현재 제목/본문/카테고리에 `%검색어%` LIKE를 사용하며 본문은 LONGTEXT다. 데이터 증가 시 full scan이
예상된다.

1. 검색 호출률과 데이터 규모를 측정한다.
2. 검색어 최소 길이, 최대 길이, rate limit을 둔다.
3. MariaDB FULLTEXT와 한국어 검색 품질을 검증한다.
4. 검색 요구가 복잡해질 때만 OpenSearch 같은 별도 시스템을 검토한다.

## 7. P3: 구조와 지속 검증

- field injection을 constructor injection으로 바꿔 테스트와 dependency 파악을 쉽게 한다. 직접적인
  속도 개선보다 성능 회귀 테스트 기반을 만드는 작업이다.
- Map 기반 응답을 DTO로 바꿔 serialization 계약과 payload를 통제한다.
- 목록/상세/관리자 API의 최대 page와 payload 크기를 계약 테스트로 고정한다.
- N+1 검출 테스트, query-count test, unique constraint concurrency test를 추가한다.
- 배포 전 부하 테스트는 운영 DB가 아닌 production-like fixture에서 수행한다.
- MariaDB 10.5의 지원 수명과 업그레이드 경로를 별도 운영 작업으로 관리한다.

## 8. 권장 실행 순서

1. B-01, B-02로 기준선과 부하 시나리오 확보
2. B-03, B-04, B-05로 조회 SQL/응답량 축소
3. B-06 인덱스와 migration 체계 도입
4. B-07 관리자/댓글 pagination
5. B-08, B-09, B-10 동시성 및 counter 개선
6. B-11, B-12 공개 조회 캐시
7. B-13, B-14 전송/이미지 최적화
8. 측정 결과에 따라 B-15~B-17 조정

각 변경은 동일 fixture와 부하 조건에서 전후 p95, SQL 수, response bytes, CPU, heap을 비교하고,
데이터 정합성 검사와 롤백 절차를 함께 남긴다.

