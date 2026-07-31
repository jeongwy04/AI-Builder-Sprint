# 그때그때

> 대화하면 기억이 알아서 튀어나오는, 우리 그룹의 살아있는 추억 아카이브
>
> AI Builder Sprint 2026 · 주제 "AI를 통해 인간다움을 더 잘 드러내는 서비스"

---

## 소개

우리의 추억은 수천 장의 사진 속에 흩어져 있지만, 그 사진 뒤의 *이야기와 감정*은 어디에도 남지 않습니다. 클라우드에 잘 저장돼 있어도 "그때 우리 진짜 웃겼던 그날"을 다시 꺼내려면 폴더를 끝없이 뒤져야 합니다.

**그때그때**는 사진에 담긴 *사람의 이야기(메모)*를 함께 저장하고, 대화만으로 그 순간을 되살려줍니다. 기술이 추억을 차가운 저장소로 만들었다면, 그때그때는 그 뒤의 사람 이야기를 다시 잇습니다.

## 주요 기능

- **추억 수집** — 사진·문서를 올리고, 그에 얽힌 썰(메모)을 남깁니다
- **AI 메모 초안** — 업로드 시 AI가 사진을 보고 메모 초안을 제안 → 사용자는 진짜 이야기만 덧붙입니다
- **대화형 검색** — "그때 그거 기억나?" 하고 물으면, AI가 필요한 만큼 되물어 좁힌 뒤 관련 사진을 찾아줍니다
- **추억 상세** — 사진 + 남겨진 이야기 + 작성자/시점

### 사용 예시

```
나:  우리 그때 밤에 치킨 먹다가 다 같이 운 날 사진 있어?
AI:  언제쯤이었는지 기억나세요? 어떤 여행이나 모임이었어요?
나:  작년 겨울, 종강하고 강릉 갔을 때
AI:  찾았어요! 2025년 12월 강릉이네요.
     "시험 끝나고 치킨 먹다 졸업 얘기 나와서 다 같이 운 날"이라고 메모돼 있어요.
     [사진 카드]
```

## 데모

- 데모 영상: `docs/demo.mp4` _(제출 시 링크로 대체)_
- APK 다운로드: _(Firebase App Distribution 링크)_
- 테스트 계정: _(제출 시 기입)_

## 기술 스택

| 영역 | 기술 |
|---|---|
| 앱 (프론트) | Kotlin · Jetpack Compose |
| 앱 라이브러리 | Coil · Supabase Kotlin SDK · Coroutines/Flow · Photo Picker |
| 백엔드 | Supabase Edge Functions (Deno / TypeScript) |
| 데이터 | Supabase (Postgres + pgvector + Storage + Auth) |
| AI · 대화 Agent | Upstage Solar LLM |
| AI · 문서 처리 | Upstage Document Parse / Information Extract |
| AI · 임베딩 | 임베딩 모델 (메모 벡터화) |

## 시스템 구조

```
[안드로이드 앱 · Kotlin/Compose]
   업로드(사진/문서+메모) · 채팅형 검색 UI
        │                          │
        │ Supabase Kotlin SDK      │ Edge Function 호출
        │ (사진→Storage, 메모 CRUD) │ (임베딩·Agent 검색·문서 파싱)
        ▼                          ▼
[Supabase]                 [Edge Functions · Deno/TS]  ← Upstage 키는 secrets에만
  Postgres + pgvector        Document Parse / 임베딩 / Solar Agent
  Storage · Auth (RLS)              │
        ▲                          ▼
        └──────────────── [Upstage] Solar LLM · Document Parse · Information Extract
```

- **RAG**: 메모 임베딩 → pgvector 의미 검색 → 사진 소환
- **Agent**: 질문이 모호할 때 Edge Function 안의 Solar가 스스로 되물어 검색 정확도를 높임

> 일반 CRUD(사진·메모)는 앱에서 Supabase SDK로 직접 처리하며 RLS로 보호합니다.
> Upstage API 키는 앱에 포함하지 않고 Edge Function secrets에만 저장합니다.

## 실행 방법

### 사전 준비
- JDK 17+, Android Studio (Koala 이상)
- Supabase 프로젝트, Supabase CLI, Upstage API 키

### 1. Supabase 설정
`vector` 확장을 켜고 스키마를 생성합니다.

```sql
create extension if not exists vector;

create table groups (
  id uuid primary key default gen_random_uuid(),
  name text not null
);

create table memories (
  id uuid primary key default gen_random_uuid(),
  group_id uuid references groups(id),
  image_url text not null,
  memo_text text,
  memo_embedding vector(N),   -- N = 사용 임베딩 모델의 차원
  taken_at timestamptz,
  author text,
  created_at timestamptz default now()
);
```

의미 검색은 pgvector 코사인 거리(`<=>`)를 쓰는 RPC 함수로 수행합니다.
사진 파일은 Supabase Storage 버킷(`memories`)에 업로드합니다.
테이블에는 RLS 정책을 적용합니다.

### 2. 백엔드 (Supabase Edge Functions)
Upstage 키는 함수 시크릿으로만 저장합니다. 앱에는 넣지 않습니다.

```bash
supabase login
supabase link --project-ref <your-project-ref>

# Upstage 키를 함수 시크릿으로 등록 (앱에는 미포함)
supabase secrets set UPSTAGE_API_KEY=your_key

# 함수 배포 (예: 임베딩·검색·문서파싱)
supabase functions deploy embed
supabase functions deploy search
supabase functions deploy parse-doc
```

### 3. 안드로이드 앱
`app` 설정(`local.properties` 등)에 Supabase 접속 정보를 넣습니다. anon 키는 클라이언트 공개가 안전하며 RLS로 보호됩니다.

```properties
SUPABASE_URL=https://xxxx.supabase.co
SUPABASE_ANON_KEY=your_anon_key
```

Android Studio에서 `app` 모듈을 실행합니다. 앱은 사진/메모는 Supabase SDK로, AI 검색은 Edge Function 호출로 처리합니다.

## 프로젝트 구조

```
ttgttg/
├── app/                    # 안드로이드 (Kotlin + Jetpack Compose)
├── supabase/
│   ├── functions/          # Edge Functions (embed · search · parse-doc)
│   └── migrations/         # DB 스키마 · RPC 함수
├── docs/                   # 발표자료 · 데모 영상 · AI 활용 증빙
├── CLAUDE.md               # AI 코딩 어시스턴트 지침
└── README.md
```

## AI 활용

- **Upstage Solar LLM** — 대화형 검색 Agent의 질문 분석·되묻기·응답 생성
- **Upstage Document Parse / Information Extract** — 업로드된 문서에서 텍스트·핵심 정보 추출
- **임베딩 모델** — 메모를 벡터로 변환해 의미 기반 검색에 사용
- **개발 과정** — AI 코딩 어시스턴트를 활용했으며, 지침은 [`CLAUDE.md`](./CLAUDE.md)에 정리
- 모델·API 사용 위치, 프롬프트, 검증 산출물은 `docs/ai-usage.md` 참고

## 팀

| 이름 | 역할 |
|---|---|
| _정우영_ | 안드로이드 (Compose) |
| _조우진_ | Supabase / Edge Functions |
| _정우영, 조우진_ | AI / 기획 |

## 라이선스

_삠삠_
