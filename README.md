# 그때그때

> 사진이 아니라, 사진 뒤에 남긴 우리의 말로 추억을 다시 찾는 소그룹 공유 다이어리
>
> AI Builder Sprint 2026 · 주제 "AI를 통해 인간다움을 더 잘 드러내는 서비스"

---

## 소개

우리의 추억은 수천 장의 사진 속에 흩어져 있지만, 그 사진 뒤의 *이야기와 감정*은 어디에도 남지 않습니다. 클라우드에 잘 저장돼 있어도 "그때 우리 진짜 웃겼던 그날"을 다시 꺼내려면 폴더를 끝없이 뒤져야 합니다.

**그때그때**는 가족·연인·친구 같은 소그룹이 사진과 함께 그 순간의 **메모(이야기)**를 남기고, 나중에는 검색창이 아니라 **AI와의 대화**로 그 기억을 다시 찾는 공유 다이어리입니다.

핵심 설계 원칙은 하나입니다 — **사진의 픽셀이 아니라 사람이 남긴 문장을 분석합니다.** 이미지 캡셔닝이나 문서 인식을 쓰지 않는 것은 기술적 한계가 아니라 의도된 선택입니다. 검색의 유일한 근거는 사용자가 직접 쓴 메모, 장소, 날짜뿐입니다.

## 주요 기능

- **기억 남기기** — 사진과 함께 그날의 이야기(메모), 장소, 함께한 사람을 기록합니다. 사진 촬영일을 EXIF에서 읽어 날짜 기본값으로 채웁니다.
- **그룹 피드** — 그룹원이 남긴 기억을 피드로 보고, 좋아요·댓글을 남기거나 그룹 채팅방에 공유합니다.
- **AI 추억 찾기** — "그때 그거 기억나?" 하고 물으면 AI가 조건이 부족할 땐 되묻고, 충분해지면 그룹의 기록 안에서만 관련 기억을 찾아 보여줍니다. 검색 결과에 없는 기억은 절대 지어내지 않습니다.
- **그룹 채팅** — 그룹 멤버끼리 대화하고, 피드의 기억을 채팅방에 바로 공유할 수 있습니다.
- **마이페이지** — 닉네임·프로필 사진·상태 메시지를 관리하고, 내가 남긴 기억과 좋아요한 기억을 모아 봅니다.
- **그룹 관리** — 그룹 생성, 이름/대표 사진 변경, 초대 코드로 멤버 추가.

### 사용 예시

```
나:  우리 그때 밤에 치킨 먹다가 다 같이 운 날 사진 있어?
AI:  언제쯤이었는지 기억나세요? 어떤 여행이나 모임이었어요?
나:  작년 겨울, 종강하고 강릉 갔을 때
AI:  찾았어요! 2025년 12월 강릉이네요.
     "시험 끝나고 치킨 먹다 졸업 얘기 나와서 다 같이 운 날"이라고 메모돼 있어요.
     [사진 카드]
```

## 기술 스택

| 영역 | 기술 |
|---|---|
| 앱 | Kotlin · Jetpack Compose (MVVM + Repository, Hilt DI, Coroutines/Flow) |
| 앱 라이브러리 | Coil3 · supabase-kt(auth/postgrest/storage/functions/compose-auth) · Navigation Compose · Photo Picker |
| 백엔드 | Supabase — Auth · Postgres(+pgvector) · Storage · Edge Functions |
| Edge Functions | `chat` (대화형 검색 오케스트레이션) · `embed-memory` (검색 인덱스 임베딩), 단 2개 — 단순 CRUD는 RLS + SDK로 직접 처리 |
| AI · 대화 Agent | Upstage **Solar Pro 3** (OpenAI SDK 호환, function calling) |
| AI · 임베딩 | Upstage **Embed 2** (의미 기반 검색) |

> 이미지 Vision 캡셔닝·문서 파싱(OCR)은 쓰지 않습니다 — "사람이 남긴 문장만이 검색의 근거"라는 설계 원칙에 따른 의도된 선택입니다 (자세한 이유는 [`AI_USAGE.md`](./AI_USAGE.md) 참고).

## 시스템 구조

```
[안드로이드 앱 · Kotlin/Compose]
   기억 작성/피드/채팅/마이페이지 · AI 대화형 검색 UI
        │                                    │
        │ supabase-kt (anon key, RLS로 보호)  │ functions.invoke("chat" / "embed-memory")
        ▼                                    ▼
[Supabase]                           [Edge Functions · Deno/TS]  ← Upstage 키는 secrets에만
  Postgres + pgvector                  chat: Solar Pro 3 대화 + function calling
  Storage (private, signed URL)        embed-memory: search_text 조립 + Embed 2 임베딩
  Auth (Google 네이티브 로그인)               │
        ▲                                    ▼
        └───────────────────────── [Upstage] Solar Pro 3 · Embed 2
```

- **일반 CRUD**(기억·메모·좋아요·댓글 등)는 앱이 Supabase SDK로 직접 처리하며, `is_member()` 기반 RLS가 유일한 보안 경계입니다.
- **검색**만 Edge Function을 거칩니다: 질의 임베딩(Embed 2) → `match_memories()` RPC로 유사도 검색 → Solar Pro 3가 결과 안에서만 자연어로 요약합니다.
- Upstage API 키는 앱에 절대 포함하지 않고 Edge Function secrets로만 관리합니다 (APK 디컴파일 유출 방지).

## 실행/배포 환경

| 항목 | 내용 |
|---|---|
| 배포 링크 (APK) | [app-debug.apk](https://github.com/jeongwy04/AI-Builder-Sprint/releases/download/v1.0/app-debug.apk) — 다운로드 후 "출처를 알 수 없는 앱 설치" 허용하면 바로 설치됩니다. |
| 빌드 타입 | Debug (디버그 키 자동 서명, 별도 키스토어 설정 없이 설치 가능) |
| 최소/타겟 SDK | minSdk 26 (Android 8.0) · targetSdk 36 |
| 백엔드 | Supabase 프로젝트(Postgres + pgvector, Storage, Auth, Edge Functions) — 데모/심사용으로 별도 배포된 실 프로젝트에 연결됨 |
| Edge Functions | `chat`, `embed-memory` 배포 완료 (`supabase functions deploy`) |
| AI 모델 | Upstage Solar Pro 3(대화) · Embed 2(임베딩), Edge Function 환경변수로 모델명 주입 |

> 코드를 직접 빌드하지 않고 앱만 써보고 싶다면 위 배포 링크로 APK를 받아 설치하면 됩니다. 이미 배포된 Supabase 프로젝트/Edge Functions에 연결되어 있어 별도 설정이 필요 없습니다.
>
> **다운로드/설치가 안 될 경우** 기본 브라우저(특히 삼성 인터넷 등) 대신 **Chrome**으로 링크를 열어 다시 받아보세요. 일부 브라우저는 다운로드가 100%까지 진행되고도 완료 처리가 안 되거나 설치 화면이 안 뜨는 경우가 있습니다.

## 로컬 기동 가이드

### 사전 준비
- JDK 17+, Android Studio
- Supabase 프로젝트, Supabase CLI, Upstage API 키(Solar Pro 3 · Embed 2)

### 1. Supabase 설정

```bash
supabase login
supabase link --project-ref <your-project-ref>
supabase db push   # supabase/migrations/ 의 스키마 + RLS + RPC 적용
```

마이그레이션에는 `pgvector` 확장, 11개 테이블(`profiles`/`archives`/`memberships`/`invitations`/`memories`/`media_assets`/`notes`/`chat_sessions`/`chat_messages`/`post_likes`/`comments`), `is_member()` 헬퍼 기반 RLS, `match_memories()`/`create_archive()`/`accept_invitation()` 등의 RPC, private Storage 버킷(`memories`, `avatars`)이 모두 포함되어 있습니다.

### 2. 백엔드 (Supabase Edge Functions)

Upstage 키는 함수 시크릿으로만 저장합니다. 앱에는 넣지 않습니다.

```bash
supabase secrets set UPSTAGE_API_KEY=your_key
supabase secrets set SOLAR_MODEL=solar-pro3
supabase secrets set EMBED_PASSAGE_MODEL=embedding-passage
supabase secrets set EMBED_QUERY_MODEL=embedding-query

supabase functions deploy chat
supabase functions deploy embed-memory
```

### 3. 안드로이드 앱

`front/local.properties.example` 을 `front/local.properties` 로 복사하고 값을 채웁니다 (anon 키는 RLS로 보호되므로 클라이언트에 포함해도 안전합니다).

```properties
SUPABASE_URL=https://xxxx.supabase.co
SUPABASE_ANON_KEY=eyJ...
GOOGLE_WEB_CLIENT_ID=xxxx.apps.googleusercontent.com
```

```bash
cd front
./gradlew assembleDebug
```

Android Studio에서 `front` 프로젝트를 열어 실행할 수도 있습니다.

## 환경변수

**앱 (`front/local.properties`, 커밋 금지 · `local.properties.example` 참고)**

| 변수 | 설명 |
|---|---|
| `SUPABASE_URL` | Supabase 프로젝트 URL. `BuildConfig`로 주입. |
| `SUPABASE_ANON_KEY` | Supabase anon 키. RLS가 보안 경계라 클라이언트에 포함돼도 안전. |
| `GOOGLE_WEB_CLIENT_ID` | Google 네이티브 로그인용 Web 클라이언트 ID. |

> Upstage API 키는 앱에 절대 포함하지 않습니다 — APK는 디컴파일될 수 있어 여기 두는 순간 키가 공개됩니다.

**Supabase Edge Functions (`supabase secrets set`)**

| 변수 | 설명 |
|---|---|
| `UPSTAGE_API_KEY` | Upstage API 키. Edge Function secrets에만 저장, 앱에는 없음. |
| `SOLAR_MODEL` | 대화 Agent 모델명 (예: `solar-pro3`). 코드에 하드코딩하지 않고 환경변수로 주입. |
| `EMBED_PASSAGE_MODEL` | 저장 시 임베딩 모델명 (예: `embedding-passage`). |
| `EMBED_QUERY_MODEL` | 검색 질의 임베딩 모델명 (예: `embedding-query`). |

## 프로젝트 구조

```
gttgtt/
├── front/                          # 안드로이드 (Kotlin + Jetpack Compose)
│   └── app/src/main/java/.../gttgtt/
│       ├── data/                   # remote(Supabase 접근) · dto · repository
│       ├── domain/                 # model · repository 인터페이스
│       ├── ui/                     # screen(auth/grouplist/groupfeed/groupchat/chat/
│       │                             memorycreate/memorydetail/memorylist/mypage) · component
│       └── di/                     # Hilt 모듈
├── supabase/
│   ├── functions/                  # chat · embed-memory (Edge Functions, 이 2개뿐)
│   └── migrations/                 # 스키마 · RLS · RPC
├── prompts/                        # 검색 Agent 시스템 프롬프트 (버전 관리)
├── evals/                          # 프롬프트 품질 검증 산출물
├── docs/                           # 아키텍처 문서 · 스키마 레퍼런스
├── .claude/agents/                 # 코드 리뷰 서브에이전트 정의
├── CLAUDE.md / AGENTS.md           # AI 코딩 에이전트 지침 (동일 내용)
├── AI_USAGE.md                     # AI 활용 기록
└── README.md
```

## AI 활용

- **Upstage Solar Pro 3** — 대화형 검색 Agent. 조건이 부족하면 되묻고, 충분하면 `search_memories` 도구를 호출해 결과 안에서만 답합니다 (`supabase/functions/chat`).
- **Upstage Embed 2** — 메모·장소·날짜를 조합한 `search_text`를 벡터로 변환해 의미 기반 검색에 사용합니다 (`supabase/functions/embed-memory`).
- **개발 과정** — AI 코딩 에이전트를 프로젝트 전반의 구현·리뷰·마이그레이션 작성에 활용했으며, 지침은 [`CLAUDE.md`](./CLAUDE.md)에, 활용 내역과 설계 이유는 [`AI_USAGE.md`](./AI_USAGE.md)에 정리되어 있습니다.

## 팀

| 이름 | 역할 |
|---|---|
| 정우영 | 안드로이드 (Compose) |
| 조우진 | Supabase / Edge Functions |
| 정우영, 조우진 | AI / 기획 |

## 라이선스

_삠삠_
