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

#### [015](https://github.com/zoek1211/rest-api-4/commit/015) — 내 정보 성공 테스트

- 가입 후 `GET /members/me?actorId=` 응답이 200 이고 id/createDate/modifyDate/username/name 이 일치하는지 검증한다.
- `$.password` 가 존재하지 않는지까지 확인해 비밀번호 비노출을 보장한다.
- 날짜는 표현 자릿수 차이를 피하려고 앞 20자만 `startsWith` 로 비교한다.

```java
resultActions
        .andExpect(handler().methodName("me"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.id").value(member.getId()))
        .andExpect(jsonPath("$.username").value(member.getUsername()))
        .andExpect(jsonPath("$.name").value(member.getName()))
        .andExpect(jsonPath("$.password").doesNotExist());
```

#### [016](https://github.com/zoek1211/rest-api-4/commit/016) — 내 정보 실패 테스트

- 존재하지 않는 actorId(`Integer.MAX_VALUE`)로 조회하면 `401-1` 과 `로그인 후 이용해주세요.` 가 오는지 검증한다.

```java
resultActions
        .andExpect(status().isUnauthorized())
        .andExpect(jsonPath("$.resultCode").value("401-1"))
        .andExpect(jsonPath("$.msg").value("로그인 후 이용해주세요."));
```

### 72강 — 샘플 회원 6명

#### [017](https://github.com/zoek1211/rest-api-4/commit/017) — 샘플 회원 존재 검증 테스트 (RED)

- 가입 없이 `GET /members/me?actorId=1~6` 을 호출해 system, admin, user1~user4 가 순서대로(id 1~6) 존재하는지 검증한다.
- 번호까지 고정해 두어야 다음 강에서 글의 작성자로 번호를 믿고 쓸 수 있다.
- 이 태그에서는 테스트가 실패하는 것이 정상 (샘플 회원이 아직 없어 401 이 온다).

```java
for (int i = 0; i < samples.length; i++) {
    int id = i + 1;
    mvc.perform(get("/api/v1/members/me?actorId=%d".formatted(id)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.id").value(id))
            .andExpect(jsonPath("$.username").value(samples[i][0]))
            .andExpect(jsonPath("$.name").value(samples[i][1]));
}
```

#### [018](https://github.com/zoek1211/rest-api-4/commit/018) — 샘플 회원 6명 생성 (GREEN)

- `BaseInitData` 에 회원 생성(`work1`)을 추가하고, 글 생성(`work2`)보다 먼저 실행한다.
- 저장 순서대로 id 가 1~6 이 된다: system, admin, user1, user2, user3, user4 (비밀번호는 모두 `12345678`).
- 회원 수(`memberService.count()`)가 0보다 크면 건너뛰어 재시작 시 중복 생성을 막는다.

```java
@Transactional
public void work1() {
    if (memberService.count() > 0) return; // 중복 생성 방지

    memberService.join("system", "12345678", "시스템"); // id 1
    memberService.join("admin", "12345678", "관리자");  // id 2
    memberService.join("user1", "12345678", "유저1");   // id 3
    // ... user2, user3, user4
}
```

### 73강 — 글·댓글 작성자

#### [019](https://github.com/zoek1211/rest-api-4/commit/019) — 글 응답에 작성자 (RED)

- 글 조회 응답에 `authorId`, `authorName` 이 있고, 샘플 글 작성자가 글1=user1(id 3), 글2=user2(id 4), 글3=user3(id 5) 인지 검증하는 테스트(단건·다건)를 추가했다.
- 기대값은 엔티티에서 꺼내지 않고 고정값으로 쓴다. 엔티티를 기준으로 삼으면 "작성자가 문제에 정해진 대로인가" 를 검증할 수 없다.
- 컴파일을 위해 `PostDto` 에 `authorId`(0), `authorName`(null) 스텁 필드만 추가했다. 동작은 아직 없다.
- 이 태그에서는 테스트가 실패하는 것이 정상 (작성자가 0/null 로 내려온다).

```java
String[][] expected = {{"3", "유저1"}, {"4", "유저2"}, {"5", "유저3"}};

for (int i = 0; i < expected.length; i++) {
    mvc.perform(get("/api/v1/posts/" + (i + 1)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.authorId").value(Integer.parseInt(expected[i][0])))
            .andExpect(jsonPath("$.authorName").value(expected[i][1]));
}
```

#### [020](https://github.com/zoek1211/rest-api-4/commit/020) — 글 작성자 (GREEN)

- `Post` 가 `Member author` 를 다대일(지연 로딩)로 가진다. 샘플 글은 `BaseInitData` 에서 글1=user1, 글2=user2, 글3=user3 으로 작성자를 지정한다.
- `PostDto` 에는 `authorId`, `authorName` 만 추가한다. `Member` 를 통째로 내보내면 비밀번호 등이 응답에 섞이기 때문이다.
- 임시 처리: 글 작성 API 는 74강 전까지 액터를 받지 않는다. 작성자가 필수가 되었으므로 컨트롤러에서 user1(id 3)을 작성자로 고정해 동작만 유지한다 (`TODO(74강)` 주석). 74강에서 actorId 로 찾은 회원으로 교체한다.

```java
// Post.java
@ManyToOne(fetch = LAZY) // 목록 조회 시 회원까지 불필요하게 읽지 않는다
private Member author;

// PostDto.java
post.getAuthor().getId(),
post.getAuthor().getName()

// ApiV1PostController.write — 74강에서 교체될 임시 처리
Member author = memberService.findById(3).get();
Post post = postService.write(author, reqBody.title, reqBody.content);
```

#### [021](https://github.com/zoek1211/rest-api-4/commit/021) — 댓글 응답에 작성자 (RED)

- 댓글 조회 응답의 `authorId`, `authorName` 과 샘플 댓글 작성자(댓글1=user1, 2=user2, 3=user3, 4=user1, 5=user2)를 검증하는 테스트(단건·다건)를 추가했다.
- 글과 마찬가지로 기대값은 엔티티가 아니라 고정값이다.
- 컴파일을 위해 `PostCommentDto` 에 `authorId`(0), `authorName`(null) 스텁 필드만 추가했다.
- 이 태그에서는 테스트가 실패하는 것이 정상 (작성자가 0/null 로 내려온다).

```java
// (postId, 댓글 id, authorId, authorName)
Object[][] expected = {
        {1, 1, 3, "유저1"}, {1, 2, 4, "유저2"}, {1, 3, 5, "유저3"},
        {2, 4, 3, "유저1"}, {2, 5, 4, "유저2"},
};
for (Object[] e : expected) {
    mvc.perform(get("/api/v1/posts/%d/comments/%d".formatted(e[0], e[1])))
            .andExpect(jsonPath("$.authorId").value(e[2]))
            .andExpect(jsonPath("$.authorName").value(e[3]));
}
```

#### [022](https://github.com/zoek1211/rest-api-4/commit/022) — 댓글 작성자 (GREEN)

- `PostComment` 가 `Member author` 를 다대일(지연 로딩)로 가진다. `Post.addComment(author, content)` 로 작성자를 함께 받는다.
- 샘플 댓글 작성자: 댓글1=user1, 2=user2, 3=user3, 4=user1, 5=user2. `PostCommentDto` 에 `authorId`, `authorName` 만 추가했다.
- 임시 처리: 댓글 작성 API 는 75강 전까지 액터를 받지 않으므로 user1(id 3)을 작성자로 고정한다 (`TODO(75강)` 주석). 75강에서 actorId 로 교체한다.

```java
// PostComment.java
@ManyToOne(fetch = LAZY)
private Member author;

// BaseInitData.work2
post1.addComment(user1, "댓글 1-1"); // id 1
post1.addComment(user2, "댓글 1-2"); // id 2
post1.addComment(user3, "댓글 1-3"); // id 3
post2.addComment(user1, "댓글 2-1"); // id 4
post2.addComment(user2, "댓글 2-2"); // id 5

// ApiV1PostCommentController.write — 75강에서 교체될 임시 처리
Member author = memberService.findById(3).get();
PostComment postComment = postService.writeComment(post, author, reqBody.content);
```

### 74강 — 글 작성 시 액터 필요
#### [023](https://github.com/zoek1211/rest-api-4/commit/023) — 글 작성 액터 필요 (RED)
- `POST /api/v1/posts?actorId={id}` 의 actorId 회원이 작성자가 되어야 한다. 없거나 존재하지 않는 회원이면 401 `401-1` `로그인 후 이용해주세요.`.
- 새 테스트 3개: actorId=4(user2)로 작성 시 `authorId`/`authorName` 이 user2 (임시 고정값 user1 과 달라야 의미가 있다), actorId 누락 → 401, 없는 actorId → 401.
- 기존 글 작성 테스트(성공, 400 검증, 잘못된 JSON)에는 `?actorId=3` 을 붙여 이후에도 의미가 유지되게 했다.
- 이 태그에서는 테스트가 실패하는 것이 정상 (아직 user1 로 고정되어 있고 actorId 를 보지 않는다).
```java
// actorId=4 → user2 가 작성자여야 한다 (현재는 user1 고정이라 실패)
post("/api/v1/posts?actorId=4") ...
        .andExpect(jsonPath("$.data.authorId").value(4))
        .andExpect(jsonPath("$.data.authorName").value("유저2"));
// actorId 누락 / 없는 회원
.andExpect(status().isUnauthorized())
.andExpect(jsonPath("$.resultCode").value("401-1"))
```
#### [024](https://github.com/zoek1211/rest-api-4/commit/024) — 글 작성 액터 필요 (GREEN)
- 글 작성 API 가 `actorId` 쿼리 파라미터로 회원을 찾아 작성자로 쓴다. 임시로 user1 을 고정하던 `TODO(74강)` 코드를 제거했다.
- `ApiV1MemberController.me` 와 같은 방식: 기본값 0 으로 누락을 "없는 회원"에 합치고, 없으면 `UnauthenticatedException`(401) 을 던진다.
- 댓글 작성은 여전히 user1 고정 (`TODO(75강)`). Rq 도입은 75강 리팩터링에서 한다.
```java
@RequestParam(defaultValue = "0") int actorId
...
Member author = memberService.findById(actorId).orElseThrow(
        UnauthenticatedException::new
);
Post post = postService.write(author, reqBody.title, reqBody.content);
```

### 75강 — 댓글 작성 시 액터 필요 + Rq
#### [025](https://github.com/zoek1211/rest-api-4/commit/025) — 댓글 작성 액터 필요 (RED)
- `POST /api/v1/posts/{postId}/comments?actorId={id}` 의 actorId 회원이 작성자가 되어야 한다. 없거나 존재하지 않는 회원이면 401 `401-1` `로그인 후 이용해주세요.`.
- 새 테스트 3개: actorId=4(유저2)로 작성 시 `authorId`/`authorName` 이 유저2 (임시 고정값 user1 과 달라야 의미가 있다), actorId 누락 → 401, 없는 actorId → 401.
- 기존 댓글 작성 테스트에는 `?actorId=3` 을 붙여 이후에도 의미가 유지되게 했다.
- 이 태그에서는 테스트가 실패하는 것이 정상 (아직 user1 로 고정되어 있고 actorId 를 보지 않는다).
```java
// actorId=4 → 유저2 가 작성자여야 한다 (현재는 user1 고정이라 실패)
post("/api/v1/posts/1/comments?actorId=4") ...
        .andExpect(jsonPath("$.data.authorId").value(4))
        .andExpect(jsonPath("$.data.authorName").value("유저2"));
// actorId 누락 / 없는 회원
.andExpect(status().isUnauthorized())
.andExpect(jsonPath("$.resultCode").value("401-1"))
```
#### [026](https://github.com/zoek1211/rest-api-4/commit/026) — 댓글 작성 액터 필요 (GREEN)
- 댓글 작성 API 가 `actorId` 쿼리 파라미터로 회원을 찾아 작성자로 쓴다. user1 을 고정하던 `TODO(75강)` 코드를 제거했다.
- 글 작성(074)과 같은 방식: 기본값 0 으로 누락을 "없는 회원"에 합치고, 없으면 `UnauthenticatedException`(401).
- 이제 members/me, 글 작성, 댓글 작성 세 곳에 같은 조회 코드가 중복된다. 다음 커밋에서 `Rq` 로 모은다.
```java
@RequestParam(defaultValue = "0") int actorId
...
Member author = memberService.findById(actorId).orElseThrow(
        UnauthenticatedException::new
);
PostComment postComment = postService.writeComment(post, author, reqBody.content);
```
