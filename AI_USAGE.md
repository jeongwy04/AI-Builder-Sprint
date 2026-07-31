# AI 활용 기술서 (AI Usage)

> 이 문서는 개발 과정 전반에서 AI를 **어디에, 어떻게** 사용했는지 기록한다.
> 서비스 안의 AI 기능(런타임)과 개발 과정의 AI 활용(프로세스)을 구분해 정리한다.

---

## 1. 개요

| 구분 | 내용 |
|---|---|
| 프로젝트 | 그때그때 |
| 런타임 AI | Upstage Solar Pro 3 (대화 + function calling), Upstage Embed 2 (의미 검색) |
| 개발 보조 AI | AI 코딩 에이전트 (지침 파일 · 서브에이전트 · 프롬프트/eval 기반) |

---

## 2. 서비스 안의 AI (런타임)

### 2-1. 대화형 기억 검색 — Solar Pro 3

- **위치:** `supabase/functions/chat/index.ts`
- **역할:** 사용자 자연어 질의를 받아 function calling으로 `search_memories` 도구를 호출하고,
  결과(기억 목록) 안에서만 자연어로 답한다. 조건이 부족하면 되묻는다.
- **모델 문자열:** 환경변수 `SOLAR_MODEL` 로 주입 (현재 값: `solar-pro3`)
- **프롬프트:** `prompts/search_agent_v{n}.md` *(현재 활성 버전: v2, 버전별 이력 관리)*
- **호출 방식:** OpenAI SDK 호환, `baseURL="https://api.upstage.ai/v1"`
- **로그:** 모델명 · 토큰 수 · 소요시간 기록
- **폴백:** LLM 호출 실패 시 키워드 기반 검색으로 전환 (빈 화면 금지)

### 2-2. 의미 임베딩 — Embed 2

- **위치:** `supabase/functions/embed-memory/index.ts` (임베딩), `supabase/functions/chat/index.ts` (질의 임베딩)
- **역할:** `search_text`(메모 + 장소 + 날짜) 조립 → 임베딩 → `memories.embedding` 갱신
- **모델 문자열:** 환경변수 `EMBED_PASSAGE_MODEL`(문서 임베딩, 현재 값: `embedding-passage`) / `EMBED_QUERY_MODEL`(질의 임베딩, 현재 값: `embedding-query`) 로 각각 주입
- **출력 차원:** `memories.embedding vector(4096)` — 마이그레이션 주석상 TODO로 남아 있어 배포 전 콘솔 값과 재확인 필요
- **트리거:** 메모(note) 추가·수정·삭제 시 앱이 이 함수를 연쇄 호출

### 2-3. 왜 이미지 Vision을 쓰지 않는가 (설계 의도)

Solar Pro 3는 텍스트 전용이며, 더 중요하게는 **사람이 남긴 문장**이 우리가 찾으려는 "이야기"의
유일한 근거이기 때문이다. 이미지 캡셔닝 미사용은 한계가 아니라 대회 주제("인간다움")에 맞춘 선택이다.

---

## 3. 개발 과정의 AI 활용 (프로세스)

### 3-1. 에이전트 지침 — `CLAUDE.md` / `AGENTS.md`

- 16개 섹션의 아키텍처 불변 규칙·금지 사항·자주 하는 실수 정리
- 코딩 에이전트가 매 작업에서 참조 → 계층 분리·RLS·키 보안 규칙을 일관 적용
- AGENTS.md는 CLAUDE.md의 복사본 (동일 내용)

### 3-2. 도메인 특화 서브에이전트 — `.claude/agents/`

| 에이전트 | 검토 대상 | 핵심 판정 |
|---|---|---|
| `rls-reviewer` | 마이그레이션 SQL | archive_id · RLS 4종 · is_member() · Storage 격리 |
| `compose-reviewer` | Android Kotlin/Compose | 계층 분리 · Upstage 키 부재 · embed 연쇄 호출 |
| `prompt-tester` | 검색 프롬프트 | 되묻기 · 환각 금지 · 0건 처리 |

### 3-3. 프롬프트 버전 관리 — `prompts/`

- 검색 에이전트 시스템 프롬프트와 도구 스키마를 코드처럼 버전 관리
- 수정 시 덮어쓰지 않고 `_v2`, `_v3` 로 이력 유지 → 개선 과정 자체가 증빙

### 3-4. 프롬프트 품질 검증 — `evals/`

- 필수 케이스: 모호 질의(되묻기) · 명확 질의(검색) · 0건(완화 제안) · 환각 유도 방어 · 메모 없는 사진 루프
- 프롬프트 버전별 통과율과 회귀 여부 기록

---

## 4. AI 사용 위치 요약표

| 위치 | AI | 용도 | 상태 |
|---|---|---|---|
| `functions/chat` | Solar Pro 3 | 대화형 검색 오케스트레이션 | ✅ 구현 완료 |
| `functions/embed-memory` | Embed 2 | search_text 임베딩 | ✅ 구현 완료 |
| `CLAUDE.md`/`AGENTS.md` | 코딩 에이전트 지침 | 규칙 강제 | ✅ |
| `.claude/agents/` | 리뷰 서브에이전트 | 코드/SQL/프롬프트 검토 | ✅ (정의) |
| `prompts/` | 프롬프트 자산 | 버전 관리 | ✅ (v1 → v2 이력) |
| `evals/` | 검증 산출물 | 회귀 방지 | 🟡 진행 중 — 앱 측 규칙은 단위 테스트로 검증 완료, 서버 프롬프트 실 실행은 대기 (`evals/results/` 참조) |

---

## 5. 재현 정보

- 런타임 AI 키는 저장소에 포함하지 않는다 (`supabase secrets` 로만 관리).
- 모델 문자열·차원 등 확정값은 확인 즉시 이 문서와 `.env.example` 에 함께 반영한다.
