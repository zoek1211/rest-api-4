# rest-api-4

Spring Boot 4 / JPA / H2 기반 글·댓글 REST API 학습 프로젝트.
[jhs512/p-14172-2](https://github.com/jhs512/p-14172-2) 의 068 커밋에서 출발해, 4부 연습문제(70강 ~ 79강)를 커밋 단위로 풀어 나간다.

- 테스트: `./gradlew test` (프로필 `test`, 인메모리 H2)
- 커밋 메시지: `<3자리 순차번호>: [<강>] <설명>` (예: `017: [74강] 글 작성 시 actorId 필수 - 실패 테스트(RED)`). `000` 은 셋업.
- 모든 커밋에 같은 번호의 태그를 단다 (`000`, `001`, …). 번호는 빠짐없이 이어진다.
- 72강부터는 RED(실패 테스트) → GREEN(최소 구현) → REFACTOR 커밋으로 나눈다. RED 태그는 테스트가 실패하는 것이 정상이다.
- 코드에는 학습용 한국어 주석을 단다.
- `README.md` 의 커밋별 설명(태그 링크 + 핵심 코드)은 별도 커밋이 아니라 각 커밋에 함께 갱신한다.
- 작업은 `practice-4` 브랜치에서 한다. **강 하나가 끝날 때마다** `main` 을 그 강의 마지막 커밋으로 fast-forward 하고, `main` 과 그 강의 태그들을 push 한다 (README 태그 링크가 바로 열리도록).

## 작업 흐름

각 강(문제)은 다음 순서로 진행한다.

1. `/grill-with-docs` — 요구사항을 질문으로 다듬고 용어는 `GLOSSARY.md`, 결정은 `docs/adr/` 에 기록
2. `/to-spec` — 합의된 내용을 스펙(GitHub 이슈)으로 정리
3. `/to-tickets` — 스펙을 커밋 단위 티켓(하위 이슈)으로 분할
4. `/implement-spec` — 티켓 순서대로 TDD 로 구현, 커밋·태그·README 갱신

## Agent skills

### Issue tracker

Issues live in this repo's GitHub Issues (`gh` CLI). See `docs/agents/issue-tracker.md`.

### Domain docs

Single-context: one `GLOSSARY.md` + `docs/adr/` at the repo root (created lazily). See `docs/agents/domain.md`.
