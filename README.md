# rest-api-4 — 4부 연습문제 (70강 ~ 79강)

Spring Boot 4 · JPA · H2 로 만든 글/댓글 REST API 에 **회원·액터·작성자·권한** 개념을 TDD 로 더해 가는 학습 레포.

- 출발점: [jhs512/p-14172-2 @ 068](https://github.com/jhs512/p-14172-2/commit/068)
- 강의 자료: <https://www.slog.gg/p/14172>
- 용어: [GLOSSARY.md](./GLOSSARY.md) · 결정: [docs/adr/](./docs/adr/)

## 읽는 법

- 모든 커밋은 `NNN: [강] 설명` 형식이고, **같은 번호의 태그**가 있다. 아래 번호를 누르면 그 커밋으로 이동한다.
- 특정 시점의 코드를 직접 보려면: `git checkout 017`
- 72강부터는 **RED → GREEN → REFACTOR** 로 커밋을 나눈다. `RED` 태그에서는 테스트가 **일부러 실패**한다.
- 테스트 실행: `./gradlew test`

## 문제 요약 (69강)

| 강 | 문제 | API |
|---|---|---|
| 70 | 회원가입 | `POST /api/v1/members/join` |
| 71 | 내 정보 | `GET /api/v1/members/me?actorId=` |
| 72 | 샘플 회원 6명 | system, admin, user1~4 (비밀번호 `12345678`) |
| 73 | 글·댓글에 작성자 | `Post.author`, `PostComment.author` |
| 74 | 글 작성 시 액터 필요 | `POST /api/v1/posts?actorId=` |
| 75 | 댓글 작성 시 액터 필요 | `POST /api/v1/posts/{id}/comments?actorId=` |
| 76 | 글 삭제는 작성자만 | `DELETE /api/v1/posts/{id}?actorId=` |
| 77 | 글 수정은 작성자만 | `PUT /api/v1/posts/{id}?actorId=` |
| 78 | 댓글 삭제는 작성자만 | `DELETE /api/v1/posts/{id}/comments/{cid}?actorId=` |
| 79 | 댓글 수정은 작성자만 | `PUT /api/v1/posts/{id}/comments/{cid}?actorId=` |

공통 오류 응답

| 상황 | 예외 | 상태 | resultCode | msg |
|---|---|---|---|---|
| username 중복 | `MemberDuplicateUsernameException` | 409 | `409-1` | `%s(은)는 이미 사용중인 username 입니다.` |
| 액터 없음/존재하지 않음 | `UnauthenticatedException` | 401 | `401-1` | `로그인 후 이용해주세요.` |
| 작성자가 아님 | `AccessDeniedException` | 403 | `403-1` | `권한이 없습니다.` |

> ⚠️ `actorId` 는 로그인 대역일 뿐 보안 수단이 아니며, 비밀번호는 평문 저장한다. ([ADR 0001](./docs/adr/0001-actor-id-query-param.md))

---

## 커밋별 설명

### 셋업

#### [000](https://github.com/zoek1211/rest-api-4/commit/000) — 셋업

- 원본 068 커밋의 코드를 그대로 가져왔다: `Post`/`PostComment` 엔티티, `ApiV1PostController`, `ApiV1PostCommentController`, `RsData`, `ResponseAspect`, `GlobalExceptionHandler`, SpringDoc, CORS.
- 에이전트 작업 흐름 설정: `CLAUDE.md`, `docs/agents/`.

출발점에서 기억할 핵심 구조 — `RsData` 의 `resultCode` 앞자리가 곧 HTTP 상태코드가 된다.

```java
public record RsData<T>(String resultCode, @JsonIgnore int statusCode, String msg, T data) {
    public RsData(String resultCode, String msg, T data) {
        // "201-1" → 201
        this(resultCode, Integer.parseInt(resultCode.split("-", 2)[0]), msg, data);
    }
}
```

#### [001](https://github.com/zoek1211/rest-api-4/commit/001) — 용어집과 ADR

- [GLOSSARY.md](./GLOSSARY.md): **회원 / 액터 / 작성자**를 구분한다. "로그인 회원", "writer" 같은 말은 쓰지 않는다.
  - 회원: 가입한 사용자
  - 액터: 지금 요청을 보내는 회원 (`?actorId=`)
  - 작성자: 글·댓글을 쓴 회원
- [ADR 0001](./docs/adr/0001-actor-id-query-param.md): 인증을 `?actorId` 로 대신하고, 나중에 교체하기 쉽게 액터 조회를 한 곳(`Rq`)에 모으기로 했다.
