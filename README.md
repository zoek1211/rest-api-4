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


### 70강 — 회원가입

#### [002](https://github.com/zoek1211/rest-api-4/commit/002) — 회원 도메인 뼈대

- `Member` 엔티티, `MemberRepository`, `MemberService`, `ApiV1MemberController` 의 빈 껍데기를 만든다. 아직 API 는 없다.
- `username` 에 `unique = true` 를 걸어 DB 레벨에서도 중복을 막을 준비를 한다. 비밀번호는 평문(학습용, ADR 0001).

```java
@Entity
@Getter
@NoArgsConstructor
public class Member extends BaseEntity {
    @Column(unique = true) // 로그인 아이디 역할이라 유일해야 한다
    private String username;
    private String password;
    private String name;
}
```


#### [003](https://github.com/zoek1211/rest-api-4/commit/003) — 회원가입 구현

- `POST /api/v1/members/join` 을 연다. 요청 DTO(`MemberJoinReqBody`)에 `@NotBlank`/`@Size` 검증을 선언하고 `@Valid` 로 켠다.
- 서비스가 `Member` 를 만들어 저장하고, 컨트롤러는 `201-1` 로 응답한다 (`resultCode` 앞자리가 HTTP 상태코드).

```java
public record MemberJoinReqBody(
        @NotBlank @Size(min = 4, max = 30) String username,
        @NotBlank @Size(min = 8, max = 30) String password,
        @NotBlank @Size(min = 2, max = 30) String name
) {}

@PostMapping("/join")
@Transactional
public RsData<Void> join(@RequestBody @Valid MemberJoinReqBody reqBody) {
    Member member = memberService.join(reqBody.username, reqBody.password, reqBody.name);
    return new RsData<>("201-1", "%s님 환영합니다. 회원가입이 완료되었습니다.".formatted(member.getName()));
}
```


#### [004](https://github.com/zoek1211/rest-api-4/commit/004) — username 중복 체크

- `findByUsername` 쿼리 메서드를 추가하고, 가입 전에 같은 username 이 있으면 `IllegalArgumentException` 으로 막는다.
- 아직 예외 핸들러가 없어서 응답은 500 이다 (다음 커밋에서 409 로 바꾼다).

```java
Optional<Member> opMember = memberRepository.findByUsername(username);

if (opMember.isPresent()) {
    throw new IllegalArgumentException(
            "%s(은)는 이미 사용중인 username 입니다.".formatted(username)
    );
}
```


#### [005](https://github.com/zoek1211/rest-api-4/commit/005) — Optional.ifPresent 로 모던하게 예외 발생

- `isPresent()` + `if` 검사를 `ifPresent` 로 바꾼다. 동작은 그대로다.
- 쓰지 않는 람다 파라미터는 `_` (Java 22+ unnamed variable) 로 표시한다.

```java
memberRepository.findByUsername(username)
        .ifPresent(_ -> {
            throw new IllegalArgumentException(
                    "%s(은)는 이미 사용중인 username 입니다.".formatted(username)
            );
        });
```


#### [006](https://github.com/zoek1211/rest-api-4/commit/006) — IllegalArgumentException 예외 처리

- 전역 핸들러에 `IllegalArgumentException` 을 잡아 `409-1` 로 응답하는 핸들러를 추가한다. 메시지는 예외 메시지를 그대로 쓴다.
- 단점: `IllegalArgumentException` 은 너무 흔한 예외라, 전혀 다른 곳에서 던져도 409 가 된다 (다음 커밋에서 전용 예외로 개선).

```java
@ExceptionHandler(IllegalArgumentException.class)
public ResponseEntity<RsData<Void>> handle(IllegalArgumentException ex) {
    return new ResponseEntity<>(new RsData<>("409-1", ex.getMessage()), CONFLICT);
}
```


#### [007](https://github.com/zoek1211/rest-api-4/commit/007) — MemberDuplicateUsernameException 도입

- 너무 추상적인 `IllegalArgumentException` 대신 `MemberDuplicateUsernameException` 을 던지고, 핸들러도 이 예외만 409 로 처리한다.
- 응답(`409-1`, 같은 메시지)은 그대로이고 예외 타입만 구체화됐다. 메시지 포맷은 예외 안으로 모았다.

```java
public class MemberDuplicateUsernameException extends RuntimeException {
    public MemberDuplicateUsernameException(String username) {
        super("%s(은)는 이미 사용중인 username 입니다.".formatted(username));
    }
}

// MemberService
memberRepository.findByUsername(username)
        .ifPresent(_ -> { throw new MemberDuplicateUsernameException(username); });
```


#### [008](https://github.com/zoek1211/rest-api-4/commit/008) — 회원가입 성공 테스트

- 뒤늦게 `ApiV1MemberControllerTest` 에 회원가입 성공 테스트를 추가한다.
- 응답에 회원 정보가 없으므로, 테스트용으로 `findLatest()` (`findTopByOrderByIdDesc`)를 서비스/레포지토리에 추가해 방금 가입한 회원을 꺼낸다.

```java
Member member = memberService.findLatest().get();

resultActions
        .andExpect(handler().methodName("join"))
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.resultCode").value("201-1"))
        .andExpect(jsonPath("$.msg").value("%s님 환영합니다. 회원가입이 완료되었습니다.".formatted(member.getName())));
```


#### [009](https://github.com/zoek1211/rest-api-4/commit/009) — 회원가입 실패(중복) 테스트

- 같은 username 으로 두 번 가입하면 `409-1` 과 중복 메시지가 오는지 검증한다.
- 첫 가입 결과는 확인하지 않고(준비 단계), 두 번째 요청의 응답만 검증한다.

```java
resultActions
        .andExpect(status().isConflict())
        .andExpect(jsonPath("$.resultCode").value("409-1"))
        .andExpect(jsonPath("$.msg").value("user10(은)는 이미 사용중인 username 입니다."));
```

### 71강 — 내 정보

#### [010](https://github.com/zoek1211/rest-api-4/commit/010) — 내 정보 API (DTO 없이)

- `GET /api/v1/members/me?actorId=` 로 해당 회원을 조회해 엔티티를 그대로 반환한다.
- 아직 DTO 가 없어서 응답에 `password` 가 포함된다. 이 문제는 다음 커밋(011)에서 해결한다.
- 없는 actorId 는 `.get()` 이 `NoSuchElementException` 을 던져 404 가 된다. (012~013 에서 401 로 바로잡는다)

```java
@GetMapping("/me")
@Transactional
public Member me(int actorId) {
    Member loginMember = memberService.findById(actorId).get();
    return loginMember; // password 까지 노출됨
}

// MemberService
public Optional<Member> findById(int id) {
    return memberRepository.findById(id);
}
```

#### [011](https://github.com/zoek1211/rest-api-4/commit/011) — MemberDto 도입

- 응답을 엔티티가 아닌 `MemberDto`(id, createDate, modifyDate, username, name)로 바꿔 `password` 노출을 막는다.
- 엔티티를 그대로 내보내면 필드가 추가될 때마다 의도치 않게 응답에 새어 나간다. DTO 는 "내보낼 필드" 를 명시적으로 고정한다.

```java
public record MemberDto(int id, LocalDateTime createDate, LocalDateTime modifyDate, String username, String name) {
    public MemberDto(Member member) {
        this(member.getId(), member.getCreateDate(), member.getModifyDate(), member.getUsername(), member.getName());
    }
}

// ApiV1MemberController
public MemberDto me(int actorId) {
    Member loginMember = memberService.findById(actorId).get();
    return new MemberDto(loginMember);
}
```

#### [012](https://github.com/zoek1211/rest-api-4/commit/012) — UnauthenticatedException 도입

- 존재하지 않는 actorId 는 `.get()` 대신 `orElseThrow(UnauthenticatedException::new)` 로 처리한다.
- 아직 핸들러가 없어서 이 예외는 처리되지 않고 500 이 된다. 401 변환은 다음 커밋(013)에서 한다.

```java
public class UnauthenticatedException extends RuntimeException {
    public UnauthenticatedException() {
        super("로그인 후 이용해주세요.");
    }
}

Member loginMember = memberService.findById(actorId).orElseThrow(
        UnauthenticatedException::new
);
```

#### [013](https://github.com/zoek1211/rest-api-4/commit/013) — 예외 핸들러에서 401 처리

- `UnauthenticatedException` 을 `401-1` + `로그인 후 이용해주세요.` 로 응답한다. (`UNAUTHORIZED`)
- 이제 존재하지 않는 actorId 는 500 이 아니라 401 이다.

```java
@ExceptionHandler(UnauthenticatedException.class)
public ResponseEntity<RsData<Void>> handle(UnauthenticatedException ex) {
    return new ResponseEntity<>(new RsData<>("401-1", ex.getMessage()), UNAUTHORIZED);
}
```

#### [014](https://github.com/zoek1211/rest-api-4/commit/014) — actorId 미입력도 401

- `actorId` 를 안 보내면 `int` 바인딩이 실패해 400/500 같은 엉뚱한 오류가 난다.
- `@RequestParam(defaultValue = "0")` 으로 기본값 0 을 주면 존재하지 않는 회원 경로로 통일되어 401 이 된다.

```java
public MemberDto me(
        @RequestParam(defaultValue = "0") int actorId
) { ... }
```
