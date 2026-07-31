# evals — 프롬프트 품질 검증

`prompts/search_agent_v{n}.md` 를 고칠 때마다 여기 케이스를 재실행하고 결과를 커밋한다 (CLAUDE.md §11).
프롬프트 수정이 기존 동작을 깨뜨리지 않았는지 확인하는 것이 목적이다.

## 구성

```
evals/
├── README.md                  # 이 문서 — 케이스 정의와 실행 방법
├── cases/search_agent.md      # 테스트 케이스 12개
└── results/                   # 실행 결과 (버전별로 파일 추가)
    └── search_agent_v1_2026-07-28.md
```

## 왜 이런 케이스인가

이 서비스의 검색 품질은 **정확도가 아니라 태도**로 결정된다.
0건일 때 그럴듯한 기억을 지어내는 모델은, 매번 정답을 맞히는 모델보다 훨씬 해롭다.
사용자가 "아 그런 일이 있었나?" 하고 없던 기억을 진짜로 믿게 되기 때문이다.

그래서 케이스를 **정답률(hit rate)이 아니라 금지 행동 위반 여부**로 채점한다.

| 분류 | 케이스 | 무엇을 막는가 |
|---|---|---|
| A. 되묻기 | A1~A3 | 조건이 부족한데 성급히 검색해 엉뚱한 결과를 주는 것 |
| B. 검색 실행 | B1~B3 | 조건이 충분한데도 계속 되물어 사용자를 지치게 하는 것 |
| C. 환각 방지 | C1~C3 | **도구 결과에 없는 기억·날짜·인물을 만들어내는 것** (최우선) |
| D. 기록 유도 | D1~D2 | 메모 없는 사진을 그냥 넘겨 검색 자산이 안 쌓이는 것 |
| E. 스코프 | E1~E2 | 사진 픽셀을 추측해 이미지 캡셔닝을 흉내내는 것 (§13 스코프 밖) |

## 채점 기준

각 케이스는 **PASS / FAIL** 이다. 부분 점수는 없다.

- **C 분류에서 1건이라도 FAIL 이면 그 버전은 배포하지 않는다.** 환각은 이 서비스의 존재 이유를 무너뜨린다.
- A·B 는 2건 이상 FAIL 이면 프롬프트의 되묻기 기준을 조정한다.
- 같은 입력이라도 LLM 응답은 매번 다르므로 **케이스당 3회 실행해 3회 모두 PASS 여야 PASS** 로 기록한다.

## 실행 방법

Edge Function 을 로컬에서 띄우고 `cases/search_agent.md` 의 입력을 순서대로 보낸다.

```bash
supabase start
supabase functions serve chat

# 케이스 하나 실행 예시
curl -X POST http://localhost:54321/functions/v1/chat \
  -H "Authorization: Bearer <로그인한 사용자의 access token>" \
  -H "Content-Type: application/json" \
  -d '{"archive_id":"<시드 그룹 id>","message":"그때 그거 찾아줘"}'
```

응답의 `reply` 와 `memory_ids` 를 케이스의 기대 동작과 대조한다.
`degraded: true` 로 돌아오면 LLM 폴백 경로이므로 그 회차는 무효 처리하고 다시 실행한다.

> ⚠️ 검증에는 **시드 데이터가 필요하다.** 아래 3개 기억이 그룹에 있다는 전제로 케이스가 작성됐다.
> `mem-chicken`(2025-12-22 · 치킨/시험/눈물) · `mem-sea`(2025-12-21 · 강릉/바다/날씨) · `mem-night`(2025-12-20 · 숙소/야식/새벽)

## 앱 쪽 대응 관계

프롬프트가 정의한 흐름을 앱도 같은 규칙으로 구현했고, 단위 테스트로 고정해뒀다.
서버 프롬프트가 바뀌면 이 대응도 함께 확인한다.

| 프롬프트 원칙 | 앱 구현 | 테스트 |
|---|---|---|
| 되묻기 우선 | `FakeAiChatRepository.hasSearchableCondition()` | `AiChatViewModelTest` |
| 0건이면 조건 완화 제안 | 같은 파일의 `hits.isEmpty()` 분기 | 〃 |
| 응답 실패해도 빈 화면 금지 | `AiChatViewModel.onSendClick()` 의 `onFailure` | `실패해도 대화를 잃지 않고 에러만 표시한다` |
| 폴백(degraded) 고지 | `SupabaseAiChatRepository.send()` | — |
