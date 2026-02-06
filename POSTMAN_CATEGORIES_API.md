-bac# Postman으로 Categories API 테스트하기

blog-backend는 **포트 8080**에서 실행됩니다. Postman에서 Base URL: `http://localhost:8080`

---

## 1. 카테고리 목록 조회

| 항목 | 값 |
|------|-----|
| **Method** | `GET` |
| **URL** | `http://localhost:8080/api/categories` |
| **Body** | 없음 |

**예상 응답 (200):**
```json
[
  {
    "id": 1,
    "name": "개발",
    "slug": "dev",
    "post_count": 5,
    "created_at": "2025-01-01T00:00:00"
  }
]
```

---

## 2. 카테고리 생성

| 항목 | 값 |
|------|-----|
| **Method** | `POST` |
| **URL** | `http://localhost:8080/api/categories` |
| **Headers** | `Content-Type: application/json` |
| **Body (raw JSON)** | `{"name": "새 카테고리"}` |

- `name`만 보내면 됩니다. slug는 백엔드에서 자동 생성(한글→영문 등).
- 이름 누락 시 400 + `{"error": "Category name is required"}`

**예상 응답 (201):**
```json
{
  "id": 2,
  "name": "새 카테고리",
  "slug": "sae-kategoli",
  "post_count": 0,
  "created_at": "2025-02-06T12:00:00"
}
```

---

## 3. slug로 카테고리 조회

| 항목 | 값 |
|------|-----|
| **Method** | `GET` |
| **URL** | `http://localhost:8080/api/categories/{slug}` |
| **예시** | `http://localhost:8080/api/categories/dev` |
| **Body** | 없음 |

- 존재하지 않는 slug → 404 + `{"error": "카테고리를 찾을 수 없습니다."}`

**예상 응답 (200):**
```json
{
  "id": 1,
  "name": "개발",
  "slug": "dev",
  "post_count": 5,
  "created_at": "2025-01-01T00:00:00"
}
```

---

## 4. slug로 카테고리 수정

| 항목 | 값 |
|------|-----|
| **Method** | `PUT` |
| **URL** | `http://localhost:8080/api/categories/{slug}` |
| **예시** | `http://localhost:8080/api/categories/dev` |
| **Headers** | `Content-Type: application/json` |
| **Body (raw JSON)** | `{"name": "개발 일지"}` |

**예상 응답 (200):** 수정된 카테고리 객체 (위와 동일 형식)

---

## 5. slug로 카테고리 삭제

| 항목 | 값 |
|------|-----|
| **Method** | `DELETE` |
| **URL** | `http://localhost:8080/api/categories/{slug}` |
| **예시** | `http://localhost:8080/api/categories/dev` |
| **Body** | 없음 |

**예상 응답 (200):**
```json
{
  "message": "카테고리가 삭제되었습니다."
}
```

---

## Postman에서 빠르게 설정하는 방법

1. **Environment 변수 (선택)**
   - Variable: `baseUrl` → Value: `http://localhost:8080`
   - URL 입력 시 `{{baseUrl}}/api/categories` 로 사용

2. **Collection 생성**
   - 새 Collection 생성 후 위 5개 요청을 각각 추가
   - GET 목록 / POST 생성 / GET by slug / PUT by slug / DELETE by slug

3. **실행 전 확인**
   - blog-backend가 실행 중인지 확인 (`./mvnw spring-boot:run` 또는 IDE 실행)
   - MySQL이 떠 있고 `blog` DB에 `categories` 테이블이 있어야 함

---

## Next.js 블로그와 연동 시 참고

- **공개 API** (목록/조회): Next.js의 `apiClient.getCategories()` → `GET /api/categories` 호출과 동일한 스펙.
- **프론트 baseUrl 변경**: blog를 blog-backend로 완전 이전하려면 Next.js의 `api-client.ts`에서 `baseUrl`을 `http://localhost:8080/api` 로 바꾸거나, Next.js 서버에 프록시를 두고 `/api` → `http://localhost:8080/api` 로 프록시하면 됩니다.
