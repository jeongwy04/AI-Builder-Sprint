# Trace Archive — 프로젝트 인수인계 문서

> **작성일: 2026.07.27** · 새 대화/새 작업자가 이 문서만 읽고 바로 이어받을 수 있도록 작성됨.
> 함께 있는 `CLAUDE.md` 는 코딩 에이전트용 개발 지침이다. 본 문서는 **의사결정 맥락**을 담는다.
>
> **스택 확정: Android (Kotlin + Jetpack Compose) + Supabase**

---

## 1. 대회 개요

**AI BUILDER SPRINT 2026** (주최: 부산대 APPTIVE · 정보컴퓨터공학부 동아리연합)
후원: Upstage · 부산대 AI융합교육원 · Anchor · MODUSIGN

**주제:** AI를 통해 인간다움을 더욱 잘 드러낼 수 있는 서비스 개발
- 문제 정의: 사람에게 진짜 필요한 문제인가
- AI 활용: **AI는 목적이 아닌 문제 해결의 도구**
- 개발 유형: 웹·모바일 등 제한 없음

### 일정 (⚠️ 초기 기획서의 "4일"은 오류. 실제 7일)

| 일자 | 내용 |
|---|---|
| 07.27 (월) | OT & 아이디어톤 |
| **07.27 ~ 08.03** | **개발 기간 (약 7일 / 168시간)** |
| 08.03 18:00 | **예선 제출 마감** |
| 08.05 | 본선팀 발표 |
| 08.07 (금) | **본선 발표 7분 + Q&A 5분** |

08.03~08.07은 개발이 아니라 **발표 준비 기간**이다.

---

## 2. 심사 기준 (반드시 숙지)

### 2-1. 적격성 심사 — Pass/Fail, 미충족 시 채점 없이 제외

| 항목 | 대응 상태 |
|---|---|
| 주제 부합성 | ✅ "인간다움" 주제에 부합 |
| **코드 기반 개발** | ⚠️ 노코드·로우코드 단독 불가 → **Upstage Studio 사용 금지, REST API 직접 호출** |
| **개발 기간 준수** | 🚨 **커밋 히스토리상 주요 개발이 07.27~08.03 안에 있어야 함.** 사전 코드 일괄 푸시 = 실격 |
| 제출물 완비 | 데모영상 · 배포링크 · 코드저장소 · 발표자료 · **AI 활용 기술서** 5종 |

> **프롬프트 인젝션 실격 조항:** README·주석·지침 파일에 심사 지시나 자기 홍보 문구를 넣으면 실격.
> `CLAUDE.md` 에는 개발 지침 외 내용을 절대 추가하지 말 것.

### 2-2. 예선 (80점) — 발표 없이 제출물만으로 심사

| 항목 | 세부 | 배점 |
|---|---|---|
| **창의성 (20)** | 문제 정의·아이디어 독창성 | 10 |
| | 핵심 기능 구성의 참신성 | 10 |
| **AI 활용도 (20)** | **개발 프로세스 전반의 AI 통합** | **10** |
| | **에이전트 설정·지침 체계성** | **5** |
| | 결과물 품질 기여도 | 5 |
| **완성도 (20)** | 핵심 기능 구현·정상 작동 | 10 |
| | 서비스 이용 흐름 | 5 |
| | 코드 품질·오류 처리 | 5 |
| **실용성 (20)** | 문제의 구체성·타당성 | 10 |
| | 실제 활용 가능성·기대 효과 | 10 |

> ⭐ **가장 중요한 발견: "AI 활용도" 20점 중 15점은 서비스 안의 AI 기능이 아니라 "개발할 때 AI를 어떻게 썼는가"를 평가한다.**
> → `CLAUDE.md`, `.claude/agents/`, `prompts/` 버전 관리, `evals/`, `AI_USAGE.md` 가 직접 점수다. 대부분의 팀이 놓치는 지점.

### 2-3. 본선 (100점 + 가점 10)

- 예선 4개 항목(80점) 동일 재평가 + **발표력 20점** (발표 구성·논리성 10 / 시연·Q&A 대응 10)
- 가점: **Upstage API 활용 +5** / 지역사회 기여도 +5

### 2-4. 심사 운영

- 심사위원 3인: **Upstage 앰버서더 1**, APPTIVE 1, Untoc 1
- **AI 에이전트가 레포를 읽고 적격성 자동 판정 + 점수 초안 산출** (팀당 독립 3회 실행 후 평균)
- → **README가 사실상 1차 심사관.** 구조화·명확성이 곧 점수.

---

## 3. 서비스 정의 (최종 확정)

### 한 줄 정의
**소그룹이 함께 만드는 대화형 공유 다이어리 (Android 앱).** 기억을 검색창이 아니라 AI와의 대화로 찾는다.

### 핵심 설계 명제
> **사진의 픽셀이 아니라 사람이 남긴 문장을 분석한다.**

이미지 캡셔닝을 하지 않는 것은 기술적 한계가 아니라 **의도된 선택**이다.

발표용 문장:
> "이미지 캡션은 '해변에 서 있는 두 사람'이라고 말합니다.
> 사용자의 메모는 '비 오다 갠 날, 우산 하나로 둘이 걸었던 날'이라고 말합니다.
> 우리가 찾으려는 건 장면이 아니라 이야기입니다."

이것이 대회 주제 "AI를 통해 인간다움을 드러내는 서비스"와 정확히 맞물린다.
**Q&A에서 "왜 이미지 분석을 안 하나요?"는 거의 확실히 나온다. 위 답변을 준비할 것.**

### 앱 흐름
```
로그인 → 그룹 선택 → [AI 대화 화면 = 홈]
   AI: "어떤 추억을 찾고 싶으신가요?"
   사용자: "작년 여름 바다 갔을 때 사진 보여줘"
   AI: (조건 부족 시) "여름이 맞을까요? 누구와 갔는지 기억나세요?"  ← 되묻기
   AI: "3장 찾았어요"  [사진 카드 인라인 표시]
```

**"되묻기 루프"가 이 서비스의 유일한 차별점이다.** 일반 검색창과 다른 지점이므로 발표에서 반드시 시연.

---

## 4. 기능 명세

### F1. 그룹 & 멤버 `필수`
- 그룹(보관소) 생성 — 가족 / 연인 / 친구 / 동아리
- 초대 링크(딥링크) 발급 → 수락 → 가입
- **역할 구분 없음. 전원 동일 권한**
- 권한 체크는 `is_member(archive_id)` 단 하나

### F2. 공유 다이어리 — 기록 & 메모 `필수`
- 사진 / 동영상 업로드 (Supabase Storage)
- **각 항목에 메모·짧은 문구 작성** ← 검색의 유일한 재료
- 멤버 누구나 남의 기록에도 메모 추가 가능 (= "같이 만드는" 다이어리, 사양임)
- 날짜: `ExifInterface` 로 촬영일 추출 → `memory_date` 기본값
- 메모 저장 시 `embed-memory` Edge Function 호출 → 임베딩 갱신

### F3. AI 대화형 기억 찾기 ⭐ `메인 기능`

```
사용자 질의
   ↓ Edge Function: chat
Solar Pro 3 (function calling)
   → search_memories(semantic_query, date_from, date_to, place, people, media_type)
   ↓
Postgres RPC: match_memories()  — 메타 필터 + pgvector 유사도
   ↓
결과 있음 → 자연어 요약 + 사진 카드 인라인
결과 부족 → 되묻기
결과 0건  → 지어내지 말고 조건 완화 제안
```

**메모 없는 사진 처리 (이 방식의 유일한 약점을 제품 루프로 전환):**
```
AI: "2025년 8월 사진 12장 중 5장은 아직 이야기가 없어요.
     어떤 날이었는지 기억나세요?"
사용자: "속초 갔을 때야. 비 와서 하루 종일 숙소에 있었어"
AI: "기록해 둘게요." → 메모로 저장
```
→ **대화가 곧 기록을 만드는 선순환.** "대화 기능이 추가된 다이어리" 컨셉의 실체.

### F4. 타임라인 `필수`
- `memory_date` 역순 전체 목록 (대화 검색의 보조 뷰)
- "아직 이야기가 없는 사진" 필터

### F5. 과거 사진 알림 `⏸ 보류`
- "1년 전 오늘" 알림
- pg_cron은 **Supabase Pro 플랜(유료)** → **GitHub Actions cron으로 Edge Function 호출하면 무료**
- **별도 지시가 있을 때만 착수**

---

## 5. 기술 스택

| 레이어 | 선택 |
|---|---|
| App | **Kotlin + Jetpack Compose** (minSdk 26, targetSdk 35) |
| 아키텍처 | MVVM + Repository, Hilt DI, Coroutines/Flow |
| 네비게이션 | Navigation Compose (type-safe routes) |
| 이미지 | Coil |
| BaaS | **Supabase** — Auth · Postgres(+pgvector) · Storage · Edge Functions |
| SDK | `supabase-kt` (`auth-kt`, `postgrest-kt`, `storage-kt`, `functions-kt`, `compose-auth`) |
| 서버 로직 | **Edge Functions (Deno/TS) 2개뿐** |
| LLM | **Solar Pro 3** — 대화 + function calling |
| Embedding | **Upstage Embed 2** |

### 아키텍처

```
┌─────────────────────────────────────────┐
│  Android App (Kotlin + Compose)         │
│         supabase-kt SDK                 │
└───┬──────┬──────┬──────────────────┬────┘
    │      │      │                  │
    ▼      ▼      ▼                  ▼
  Auth  Postgres Storage      Edge Functions (Deno)
        (RLS로   (private,    ┌──────────────────────┐
         보호)    signed URL) │ chat                 │
                              │  Solar function call │
   ▲ CRUD는 SDK 직접          │   └→ match_memories()│
     (백엔드 코드 없음)        │                      │
                              │ embed-memory         │
                              │  Embed 2 임베딩       │
                              └──────────┬───────────┘
                                         ▼
                                   Upstage API
                            (키는 Edge Function에만)
```

### ⚠️ 반드시 알아야 할 기술 제약

| 사실 | 영향 |
|---|---|
| **Solar Pro 3는 텍스트 전용. 이미지 입력 불가** | 사진 직접 분석 불가 → `search_text` 기반 검색으로 설계 |
| **Solar Pro 3는 function calling 지원** | 대화형 검색을 Solar 단독으로 구현 가능 |
| Upstage API는 OpenAI SDK 호환 | `baseURL="https://api.upstage.ai/v1"` 교체만으로 사용 |
| **APK는 디컴파일됨** | 🚨 Upstage 키를 앱에 넣으면 유출. Edge Function 필수 |
| Supabase RLS 정책 안에서 `memberships` 직접 조회 시 무한 재귀 | `is_member()` (security definer) 헬퍼 사용 |
| pg_cron은 Pro 플랜 | F5 구현 시 GitHub Actions로 대체 |
| Embed 2는 2026-08-23까지 무료 | 대회 기간 전체가 무료 구간 |

---

## 6. 데이터 모델 (8개 테이블)

```
profiles       id(=auth.users.id), display_name, avatar_url
               -- Supabase Auth 사용. users 테이블 직접 생성 금지

archives       id, name, group_type, cover_image_path, created_at

memberships    id, archive_id, user_id, joined_at
               ⚠️ role 컬럼 없음

invitations    id, archive_id, token, expires_at, used_count

memories       id, archive_id, author_id,
               memory_date DATE,      -- 추억이 일어난 날 (정렬·검색 기준)
               created_at,            -- 업로드 시각
               place_name, lat, lng,
               search_text TEXT,      -- 메모 전부 + 장소 + 날짜표현 ⭐
               embedding VECTOR(n)    -- Embed 2 (차원 확인 필요)

media_assets   id, memory_id, archive_id, storage_path,
               media_type, mime_type, size_bytes, duration_sec, exif JSONB

notes          id, memory_id, archive_id, author_id, body, created_at

chat_sessions  id, archive_id, user_id, created_at
chat_messages  id, session_id, role, content,
               tool_calls JSONB, result_memory_ids UUID[]
```

**핵심 불변식**
- `notes` 변경 → `embed-memory` 호출 → `search_text` + `embedding` 함께 갱신
- 모든 테넌트 테이블에 RLS 4종(select/insert/update/delete) 정책 필수
- Storage 경로: `{archive_id}/{memory_id}/{uuid}.{ext}`, private 버킷 + signed URL

---

## 7. 서버 로직 (Edge Function 2개 + RPC 1개)

| 이름 | 종류 | 역할 |
|---|---|---|
| `chat` | Edge Function | Solar 대화 + function calling → `match_memories()` 호출 |
| `embed-memory` | Edge Function | `search_text` 조립 → Embed 2 임베딩 → `memories` 갱신 |
| `match_memories()` | Postgres RPC | 메타 필터 + 벡터 유사도 하이브리드 검색 |
| `is_member()` | Postgres 함수 | RLS 멤버십 판정 (security definer) |

**단순 CRUD를 위해 Edge Function을 만들지 않는다.** RLS + `postgrest-kt` 로 처리.
**SSE 스트리밍은 구현하지 않는다.** 일반 요청/응답 + 로딩 인디케이터.

---

## 8. 화면 (6개)

```
S0  auth/           로그인 (Google 소셜 로그인 권장)
S1  grouplist/      그룹 선택 — 카드 목록 + [새 그룹] [초대코드 입력]
S2  chat/      ⭐   AI 대화 (홈) — 진입 시 AI가 먼저 질문, 결과는 카드 인라인
S3  memorydetail/   기억 상세 — 미디어 뷰어 + 메모 목록 + 메모 추가
S4  memorycreate/   기억 작성 — 업로드 + 메모 + 날짜
S5  timeline/       타임라인 — 전체 목록 + "이야기 없는 사진" 필터
```

각 화면 패키지는 `XxxScreen.kt` / `XxxViewModel.kt` / `XxxUiState.kt` 3파일 구성.

---

## 9. 확정된 의사결정 — 삭제한 기능과 이유

**새 작업자는 아래를 다시 제안하지 말 것.** 사용자가 명시적으로 제외했다.

| 삭제 항목 | 이유 |
|---|---|
| 멤버 역할·권한 등급 | 전원 동일 권한으로 단순화 |
| AI 회고록 / 디지털 자서전 생성 | 사용자가 제외. 사용자가 직접 쓰는 메모로 대체 |
| 종이 기억 디지털화 (Document Parse / Information Extract / OCR) | 사용자가 제외. Solar LLM만 사용 |
| 이미지 Vision 캡셔닝 | 사용자가 제외. 메모 텍스트만 분석 |
| 타임캡슐 (암호화 봉인) | 사용자가 제외 |
| 지역 기억 보관소 (공개 열람) | 사용자가 제외 → 지역사회 가점 +5 포기 |
| 음성 STT | 범위 밖 |
| AWS (RDS/S3/Lambda/Cognito) | **Supabase로 전환** |
| FastAPI 백엔드 | **불필요** — RLS + SDK 직접 호출로 대체 |
| Next.js 웹 프론트 | **Android 네이티브로 전환** |
| SSE 스트리밍 | Android에서 실익 없음 |
| Kubernetes | 7일 내 불가, 배점에도 없음 |
| MODUSIGN 특별상 도전 | CLM 배점 35점이 전자서명 중심 구조 → 본상 집중 판단 |

---

## 10. 진행 상황

### ✅ 완료
- 기획 확정 (기능 F1~F4 + F5 보류)
- 심사 기준 분석 및 대응 전략
- 데이터 모델 · 서버 로직 · 화면 설계
- 기술 제약 검증 (Solar 텍스트 전용 / function calling 지원 / Supabase pgvector·Kotlin SDK)
- 스택 전환 결정 (AWS+Next.js → Supabase+Android)
- **`CLAUDE.md` 작성 완료** (Supabase + Android 기준, 16개 섹션)

### ⬜ 다음 작업 (우선순위 순)
1. **`git init` + 첫 커밋** — 커밋 히스토리가 적격성 요건. 오늘 안에 필수
2. `AGENTS.md` — `CLAUDE.md` 내용 **복사** (심볼릭 링크 금지, GitHub에서 안 보임)
3. `.claude/agents/` 3종 — `rls-reviewer`, `compose-reviewer`, `prompt-tester`
4. Supabase 프로젝트 생성 + Upstage 콘솔 가입 → **Embed 2 모델명·차원 확인** (마이그레이션 선행 조건)
5. 최초 마이그레이션 (8테이블 + `is_member()` + RLS 정책 + Storage 정책)
6. F3 `chat` Edge Function + `prompts/search_agent_v1.md`
7. `README.md` (심사 에이전트 대응형), `AI_USAGE.md` 뼈대

---

## 11. 개발 로드맵 (7일)

| 일자 | 작업 |
|---|---|
| **D1 (7/27 밤)** | `git init` + `CLAUDE.md` 커밋. Supabase 프로젝트 생성. Upstage 가입·키 발급. Android 프로젝트 스캐폴딩 |
| **D2 (7/28)** | 마이그레이션(8테이블 + RLS + Storage 정책). Supabase Auth 연동 → 로그인 → 그룹 생성/목록 E2E 관통 |
| **D3 (7/29)** | Storage 업로드 + ExifInterface 날짜 추출. S4 기억 작성 화면 |
| **D4 (7/30)** | 메모 기능(S3) + `embed-memory` Edge Function + 임베딩 파이프라인 |
| **D5 (7/31)** | ⭐ `chat` Edge Function (Solar function calling) + `match_memories()` RPC |
| **D6 (8/1)** | ⭐ S2 대화 화면 Compose UI + 되묻기 루프 프롬프트 튜닝 + `evals/` |
| **D7 (8/2)** | S5 타임라인 · 초대 딥링크 마감. **시드 데이터 (사진 40장 + 실제 문장 메모 40개)** |
| **D8 (8/3 ~18:00)** | **개발 중단.** README · AI_USAGE.md · 데모영상 · APK 릴리스 · 발표자료 |
| 8/4~8/6 | 발표 리허설 (7분 엄수), Q&A 대비 |
| 8/7 | 본선 |

**시드 데이터 경고:** 메모가 "테스트1" 같으면 검색 데모가 전부 죽는다. 실제 문장으로 채울 것. 반나절 투자 가치 있음.

---

## 12. 제출물 체크리스트 (5종)

- [ ] **발표 자료** — 문제 정의 · 사용자 · 핵심 기능 · AI 활용 방식 · 기대 효과
- [ ] **코드 저장소** — README · 실행 방법 · 주요 코드 · 커밋 내역 · **AI 지침 파일**
- [ ] **배포 / 데모** — ⚠️ **모바일 앱이라 웹처럼 URL 하나로 끝나지 않는다**
  - **APK를 GitHub Releases에 업로드** + README 상단에 다운로드 링크
  - **데모 영상 품질이 웹 프로젝트보다 훨씬 중요** (2~3분, 나레이션 포함)
  - 테스트 계정 + 시드 데이터가 채워진 그룹 준비
- [ ] **AI 활용 증빙** — 모델 · API 사용 위치 · 프롬프트/설정 · **테스트·검증 산출물**

### 레포에 반드시 있어야 할 것 (AI 활용도 15점 직결)
```
CLAUDE.md          ✅ 작성 완료
AGENTS.md          ⬜ 복사본
AI_USAGE.md        ⬜
.claude/agents/    ⬜ 3종
prompts/           ⬜ 프롬프트를 코드처럼 버전 관리 (v1→v2 개선 이력 = 증빙)
evals/             ⬜ 프롬프트 품질 검증 산출물
README.md          ⬜ 심사 에이전트 대응형
docs/ARCHITECTURE.md ⬜
```

---

## 13. 비용

| 항목 | 단가 |
|---|---|
| Supabase | 무료 티어 (해커톤 규모 충분, pg_cron만 Pro) |
| Solar Pro 3 | $0.15 / 1M in, $0.60 / 1M out |
| Embed 2 | **무료 (~2026-08-23)** |
| Upstage 신규 가입 크레딧 | **$10** |

**참가팀 전원에게 Upstage API 크레딧이 별도 지급된다.**

---

## 14. 미결정 사항

1. **Embed 2 모델명 · 출력 차원** — 콘솔 확인 필요. `VECTOR(n)` 확정의 선행 조건 🚨
2. **Solar 모델 문자열** — `solar-pro3` / `solar-pro-3` 표기가 문서마다 상이. 콘솔 예제로 확정
3. **소셜 로그인 제공자** — Google 단독 vs 카카오 추가
4. **동영상 검색 범위** — 사용자 메모만으로 처리할지
5. **APK 배포 경로** — GitHub Releases vs Firebase App Distribution

---

## 15. 새 대화 시작 시 첨부할 것

- 본 문서 (`HANDOFF.md`)
- `CLAUDE.md`
- (있다면) 현재 레포 구조

**첫 요청 예시:**
> "HANDOFF.md와 CLAUDE.md 첨부했습니다. §10의 다음 작업 5번 최초 마이그레이션부터 작성해 주세요."
