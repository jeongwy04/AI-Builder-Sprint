# Trace Archive — 개발 지침

> 이 문서는 이 저장소에서 작업하는 모든 AI 코딩 에이전트와 개발자가 따르는 규칙이다.
> 코드를 작성하기 전에 **§5 아키텍처 불변 규칙**과 **§13 스코프 밖**을 반드시 확인한다.

---

## 1. 프로젝트 개요

**우리의 흔적, 보관소 (Trace Archive)** — 소그룹이 함께 만드는 **대화형 공유 다이어리** (Android 앱).

가족·연인·친구 단위의 소그룹이 사진과 동영상을 올리고 **메모(이야기)** 를 함께 적는다.
나중에 그 기억을 찾을 때는 검색창이 아니라 **AI와의 대화**로 찾는다.
AI는 조건이 부족하면 되묻고, 확보된 조건으로 그룹의 기록을 검색해 결과를 보여준다.

**핵심 설계 명제: 사진의 픽셀이 아니라 사람이 남긴 문장을 분석한다.**
이미지 캡셔닝을 하지 않는 것은 기술적 한계가 아니라 의도된 선택이다.
`search_text`(사용자 메모 + 장소 + 날짜)만이 검색의 근거다.

---

## 2. 기술 스택

| 레이어 | 선택 |
|---|---|
| App | **Kotlin + Jetpack Compose** (minSdk 26, targetSdk 36) |
| 아키텍처 | MVVM + Repository, Hilt DI, Coroutines/Flow |
| 네비게이션 | Navigation Compose (type-safe routes, kotlinx.serialization) |
| 이미지 | Coil |
| BaaS | **Supabase** — Auth · Postgres(+pgvector) · Storage · Edge Functions |
| SDK | `supabase-kt` (`auth-kt`, `postgrest-kt`, `storage-kt`, `functions-kt`, `compose-auth`) |
| 서버 로직 | **Supabase Edge Functions** (Deno / TypeScript) — 2개뿐 |
| LLM | **Solar Pro 3** (`https://api.upstage.ai/v1`) — 대화 + function calling |
| Embedding | **Upstage Embed 2** — 의미 검색 |

**서비스 런타임의 LLM은 Upstage 단독 구성이다.** 타사 모델을 추가하지 않는다.

---

## 3. 디렉토리 구조

```
trace-archive/
├── CLAUDE.md / AGENTS.md          # 본 문서 (동일 내용, 복사본)
├── AI_USAGE.md                    # AI 활용 기록
├── README.md
├── .claude/agents/                # 서브에이전트 정의
├── prompts/                       # LLM 프롬프트 (버전 관리 대상)
├── evals/                         # 프롬프트 품질 검증 산출물
├── docs/ARCHITECTURE.md
│
├── design/                        # UI 시안 이미지 (구현 참조용)
├── front/                         # Android Studio 프로젝트 루트
│   ├── app/src/main/java/com/ai_builder_hackathon/gttgtt/
│   │   ├── TraceArchiveApp.kt
│   │   ├── di/                    # Hilt 모듈 (SupabaseClient 제공)
│   │   ├── data/
│   │   │   ├── remote/            # Supabase 접근 (여기서만)
│   │   │   ├── dto/               # @Serializable DTO
│   │   │   └── repository/        # Repository 구현
│   │   ├── domain/
│   │   │   ├── model/             # 도메인 모델
│   │   │   └── repository/        # Repository 인터페이스
│   │   └── ui/
│   │       ├── navigation/
│   │       ├── theme/
│   │       ├── component/         # 재사용 Composable
│   │       └── screen/
│   │           ├── auth/          # S0 로그인
│   │           ├── grouplist/     # S1 그룹 선택
│   │           ├── chat/          # S2 AI 대화 (홈) ⭐
│   │           ├── memorydetail/  # S3 기억 상세
│   │           ├── memorycreate/  # S4 기억 작성
│   │           └── timeline/      # S5 타임라인
│   ├── gradle/libs.versions.toml  # 버전 카탈로그
│   └── build.gradle.kts
│
└── supabase/
    ├── config.toml
    ├── migrations/                # SQL 마이그레이션 (RLS 정책 포함)
    └── functions/
        ├── chat/index.ts          # Solar 대화 + function calling
        └── embed-memory/index.ts  # Embed 2 임베딩
```

각 화면 패키지는 `XxxScreen.kt`, `XxxViewModel.kt`, `XxxUiState.kt` 3파일 구성을 기본으로 한다.

---

## 4. 개발 명령어

```bash
# Supabase 로컬 기동
supabase start
supabase db reset                      # 마이그레이션 재적용
supabase migration new <이름>
supabase functions serve chat          # Edge Function 로컬 실행
supabase functions deploy chat
supabase secrets set UPSTAGE_API_KEY=...

# Android
cd front
./gradlew assembleDebug
./gradlew test                         # 단위 테스트
./gradlew lint
```

---

## 5. 아키텍처 불변 규칙 (위반 금지)

### 5.1 멀티테넌시 — RLS가 유일한 보안 경계

앱은 `anon` 키로 Postgres에 직접 접근한다. **따라서 RLS가 뚫리면 곧바로 데이터 유출이다.**

- 테넌트 데이터를 담는 **모든 테이블은 `archive_id UUID NOT NULL`** 을 가진다.
- 새 테이블을 추가하면 **같은 마이그레이션에 RLS 활성화 + 정책을 반드시 포함**한다.
- 멤버십 판정은 아래 헬퍼 함수만 사용한다. **정책 안에서 `memberships` 를 직접 조회하면 무한 재귀가 발생한다.**

```sql
-- 최초 마이그레이션에 1회 정의
create or replace function public.is_member(p_archive_id uuid)
returns boolean
language sql
security definer          -- ⚠️ 재귀 방지에 필수
stable
as $$
  select exists (
    select 1 from public.memberships
    where archive_id = p_archive_id
      and user_id = auth.uid()
  );
$$;

-- 이후 모든 테넌트 테이블은 이 패턴을 따른다
alter table public.memories enable row level security;

create policy "members_select" on public.memories
  for select using (public.is_member(archive_id));
create policy "members_insert" on public.memories
  for insert with check (public.is_member(archive_id));
create policy "members_update" on public.memories
  for update using (public.is_member(archive_id));
create policy "members_delete" on public.memories
  for delete using (public.is_member(archive_id));
```

- **`service_role` 키는 Edge Function에서만 사용한다.** 앱에는 `anon` 키만 넣는다.
- 앱에 포함되는 `anon` 키는 공개되어도 안전하다. RLS가 지키기 때문이다. 이 전제를 깨는 코드를 쓰지 않는다.

### 5.2 Storage 격리

- 버킷: `memories` (**private**)
- 경로 규칙: `{archive_id}/{memory_id}/{uuid}.{ext}`
- 조회는 **signed URL**로만 한다. public 버킷으로 전환하지 않는다.

```sql
create policy "members_can_read_media" on storage.objects
  for select using (
    bucket_id = 'memories'
    and public.is_member(((storage.foldername(name))[1])::uuid)
  );
```

### 5.3 계층 분리 (Android)

```
Composable → ViewModel → Repository → Supabase SDK
```

- **Composable에서 `SupabaseClient` 를 직접 참조하지 않는다.** 예외 없음.
- ViewModel은 `StateFlow<XxxUiState>` 하나만 외부에 노출한다.
- `suspend` 함수는 Repository 계층에만 둔다.
- Repository는 `Result<T>` 를 반환한다. 예외를 UI까지 던지지 않는다.

```kotlin
// domain/repository/MemoryRepository.kt
interface MemoryRepository {
    suspend fun getTimeline(archiveId: String, cursor: String?): Result<List<Memory>>
    suspend fun addNote(memoryId: String, body: String): Result<Note>
}
```

### 5.4 Upstage 호출 — 앱에서 직접 금지 🚨

APK는 디컴파일된다. **Upstage API 키를 앱에 넣는 순간 키가 공개된다.**

```kotlin
// ❌ 절대 금지
val client = OpenAI(apiKey = BuildConfig.UPSTAGE_KEY)

// ✅ 반드시 Edge Function 경유
supabase.functions.invoke("chat", body = ChatRequest(archiveId, message))
```

- `UPSTAGE_API_KEY` 는 `supabase secrets` 로만 관리한다.
- Android 소스·`local.properties`·`build.gradle.kts` 어디에도 Upstage 키를 두지 않는다.

### 5.5 프롬프트 관리
- **프롬프트를 코드에 하드코딩하지 않는다.** `prompts/*.md` 에 두고 Edge Function이 로드한다.
- 프롬프트 수정 시 파일을 덮어쓰지 말고 `_v2`, `_v3` 로 버전을 올린다. 개선 이력이 남아야 한다.

### 5.6 검색 인덱스 일관성
- `memories.search_text` 와 `memories.embedding` 은 항상 함께 갱신한다.
- **메모(note)가 추가·수정·삭제되면 앱이 반드시 `embed-memory` Edge Function을 호출한다.**
- `search_text` 조립 로직은 `supabase/functions/embed-memory/index.ts` 한 곳에만 둔다. 앱에서 조립하지 않는다.

---

## 6. 도메인 규칙

### 6.1 권한 — 역할 개념 없음
- 그룹 멤버는 **전원 동일 권한**이다. `owner`/`editor`/`viewer` 등급을 만들지 않는다.
- 권한 체크는 **`is_member(archive_id)` 단 하나**다.
- 다른 멤버가 올린 기억에도 누구나 메모를 추가할 수 있다. 버그가 아니라 사양이다.

### 6.2 날짜
- `memories.memory_date` = 추억이 실제 일어난 날 (**타임라인·검색 기준**)
- `memories.created_at` = 업로드 시각
- **둘을 혼용하지 않는다.** 정렬·검색은 항상 `memory_date`.
- 업로드 시 `ExifInterface` 로 촬영일을 읽어 `memory_date` 기본값으로 채운다.

### 6.3 검색 근거
- LLM은 `search_memories` 도구가 반환한 결과 **안에서만** 답한다.
- 도구 결과에 없는 기억·날짜·장소를 문장으로 만들어내지 않는다.
- 결과가 0건이면 지어내지 말고 조건 완화를 제안한다.

### 6.4 대화 흐름
- 그룹 진입 시 AI가 먼저 묻는다: "어떤 추억을 찾고 싶으신가요?"
- 검색 조건(기간·장소·인물·주제)이 **부족하면 즉시 검색하지 말고 한 번 되묻는다.**
- 메모가 비어 있는 기억이 결과에 섞이면 메모 작성을 유도하고, 사용자의 답변을 메모로 저장할 수 있다.

---

## 7. 데이터 모델

테이블은 다음 11개다. **임의로 추가하지 말고 먼저 제안한다.**

| 테이블 | 용도 | archive_id |
|---|---|---|
| `profiles` | 사용자 프로필 (`auth.users` 참조) | — |
| `archives` | 그룹 = 보관소 | (자신이 루트) |
| `memberships` | 그룹 멤버 (**role 컬럼 없음**) | ✅ |
| `invitations` | 초대 토큰 | ✅ |
| `memories` | 기억 단위 + 검색 인덱스 | ✅ |
| `media_assets` | Storage 미디어 메타 | ✅ |
| `notes` | 메모·문구 (memory 1:N) | ✅ |
| `chat_sessions` / `chat_messages` | AI 대화 이력 | ✅ |
| `post_likes` | 게시물 좋아요 (post_id + user_id 유니크) | ✅ |
| `comments` | 게시물 댓글 | ✅ |

- Supabase Auth를 쓰므로 `users` 테이블을 직접 만들지 않는다. `auth.users` 를 참조하는 `profiles` 를 쓴다.
- 스키마 변경은 `supabase migration new` 로만 한다. 대시보드에서 직접 수정 금지 (이력이 남지 않는다).
- `embedding` 차원은 Embed 2 실제 출력 차원에 맞춘다. **하드코딩 전에 확인할 것.**
- pgvector 인덱스는 현 단계에서 만들지 않는다 (데이터 규모상 순차 스캔으로 충분).

---

## 8. Edge Function / RPC 규약

### 8.1 Edge Function은 2개뿐

| 함수 | 역할 |
|---|---|
| `chat` | Solar Pro 3 대화 + function calling → `match_memories()` RPC 호출 → 결과 반환 |
| `embed-memory` | `search_text` 조립 → Embed 2 임베딩 → `memories` 갱신 |

**단순 CRUD를 위해 Edge Function을 만들지 않는다.** RLS + `postgrest-kt` 로 처리한다.

### 8.2 규약
- 클라이언트의 `Authorization` 헤더를 그대로 전달받아 사용자 컨텍스트를 유지한다.
- 요청·응답은 명시적 TypeScript 타입으로 정의한다. `any` 금지.
- 에러 응답 형식은 고정한다.

```json
{ "error": { "code": "MEMORY_NOT_FOUND", "message": "기억을 찾을 수 없습니다." } }
```

- **스트리밍(SSE)을 구현하지 않는다.** 일반 요청/응답 + 로딩 인디케이터로 처리한다.
- 타임아웃 20초, 재시도 1회. 무한 재시도 금지.

### 8.3 검색 RPC

```sql
-- 메타데이터 필터 + 벡터 유사도 하이브리드. RLS 자동 적용.
create or replace function public.match_memories(
  p_archive_id uuid,
  p_query_embedding vector,
  p_date_from date default null,
  p_date_to   date default null,
  p_limit     int  default 10
) returns setof public.memories
language sql stable
as $$
  select * from public.memories
  where archive_id = p_archive_id
    and (p_date_from is null or memory_date >= p_date_from)
    and (p_date_to   is null or memory_date <= p_date_to)
    and embedding is not null
  order by embedding <=> p_query_embedding
  limit p_limit;
$$;
```

`security definer` 를 붙이지 않는다. 호출자 권한으로 실행되어야 RLS가 적용된다.

---

## 9. LLM 연동 규약

- 모델명은 **Edge Function 환경변수로 주입**한다. 코드에 문자열로 박지 않는다.
- Upstage API는 OpenAI SDK 호환이므로 `baseURL` 교체 방식으로 사용한다.
- 대화 오케스트레이션은 `supabase/functions/chat/index.ts` 에 둔다.
- Solar Pro 3의 **function calling** 으로 `search_memories` 도구를 호출한다.
- 도구 스키마와 시스템 프롬프트는 `prompts/search_agent_v{n}.md` 에서 로드한다.
- 도구 실행 결과(memory id 목록)는 `chat_messages.result_memory_ids` 에 저장한다.
- **LLM 호출 실패 시 사용자에게 빈 화면을 주지 않는다.** 키워드 기반 폴백 검색으로 전환한다.
- 모든 LLM 호출은 모델명·토큰 수·소요시간을 로그로 남긴다.

---

## 10. 코드 스타일

**Kotlin**
- `ktlint` 규칙 준수. 들여쓰기 4칸.
- Composable 함수는 `PascalCase`, `@Preview` 를 가능한 한 함께 작성한다.
- `!!` 사용 금지. `?:` 또는 early return.
- `GlobalScope` 금지. `viewModelScope` / `rememberCoroutineScope` 사용.
- DTO는 `@Serializable` + `@SerialName` 으로 DB 컬럼명(snake_case)과 매핑한다.
- Composable에 비즈니스 로직을 넣지 않는다. 상태는 ViewModel에서 내려받는다.

**TypeScript (Edge Functions)**
- `any` 금지. 요청/응답 타입을 명시한다.
- 환경변수는 `Deno.env.get()` 으로 읽고, 누락 시 즉시 실패시킨다.

**공통**
- 주석은 "무엇을"이 아니라 **"왜"** 를 적는다.
- 매직 넘버 금지. 상수로 뺀다.

---

## 11. 테스트

- **새 Repository / ViewModel에는 최소 1개의 테스트를 동반한다.** 예외 없음.
- 반드시 검증해야 하는 영역:
  - **RLS 격리** — 다른 archive의 데이터가 조회되지 않는지 (SQL 스크립트로 검증, `supabase/tests/`)
  - Storage 정책 — 멤버가 아닌 사용자가 미디어에 접근할 수 없는지
  - `search_text` 재생성 — 메모 추가/삭제 시 갱신되는지
  - 검색 결과 0건일 때 LLM이 기억을 지어내지 않는지
- ViewModel 테스트는 `MockK` + `kotlinx-coroutines-test` 를 사용한다.
- LLM 호출은 테스트에서 목으로 대체한다. 실제 API를 호출하지 않는다.
- 프롬프트 변경 시 `evals/` 테스트 케이스를 재실행하고 결과를 커밋한다.

---

## 12. 커밋 규약

- Conventional Commits 형식. 본문은 한국어 허용.
  ```
  feat(chat): Solar function calling 기반 기억 검색 구현
  fix(auth): 초대 링크 딥링크 파라미터 유실 수정
  test(rls): 테넌트 격리 검증 스크립트 추가
  ```
- **작은 단위로 자주 커밋한다.** 대량 변경을 한 번에 푸시하지 않는다.
- 커밋 하나가 하나의 의도를 갖게 한다.
- 커밋 금지 대상: `local.properties`, `*.keystore`, `.env`, `supabase/.temp/`, 빌드 산출물.

---

## 13. 스코프 밖 — 구현하지 않는다

아래는 **명시적 지시가 있기 전까지 구현하지 않는다.** 관련 코드·테이블·의존성을 추가하지 말 것.

- ❌ 타임캡슐, 암호화 봉인
- ❌ AI 회고록 / 디지털 자서전 자동 생성
- ❌ Document Parse, Information Extract, OCR, 문서 파싱
- ❌ 이미지 Vision 캡셔닝 (사진 내용 분석)
- ❌ 음성 STT
- ❌ 지역 공개 보관소, 공개 열람
- ❌ 멤버 역할·권한 등급
- ❌ SSE 스트리밍 응답
- ❌ iOS / 웹 클라이언트
- ⏸ 과거 사진 알림 / 푸시 — **보류.** 별도 지시가 있을 때만 착수한다.

기존 기획 문서에 위 항목이 언급되어 있더라도 **본 문서가 우선한다.**

---

## 14. 금지 사항

- 앱에서 Upstage API 직접 호출 / 앱에 Upstage 키 포함
- 앱에 `service_role` 키 포함
- RLS 정책 없는 테넌트 테이블 생성
- RLS 정책 안에서 `memberships` 직접 조회 (→ `is_member()` 사용)
- Composable에서 `SupabaseClient` 직접 접근
- 프롬프트 하드코딩 (→ `prompts/`)
- Supabase 대시보드에서 스키마 직접 수정 (→ 마이그레이션)
- 역할 기반 권한 분기 코드
- 새 외부 의존성 임의 추가 (먼저 제안하고 승인받는다)

---

## 15. 자주 하는 실수

| 실수 | 결과 | 올바른 방법 |
|---|---|---|
| 메모 저장 후 `embed-memory` 호출 누락 | 검색에 안 잡힘 | Repository에서 항상 연쇄 호출 |
| `created_at` 으로 타임라인 정렬 | 옛날 사진이 오늘 자리에 표시 | `memory_date` 사용 |
| RLS 정책에서 `memberships` 직접 조회 | 무한 재귀 오류 | `is_member()` (security definer) |
| Storage를 public 버킷으로 전환 | 전체 미디어 노출 | private + signed URL |
| Solar에 이미지 전달 시도 | 동작 안 함 (텍스트 전용 모델) | `search_text` 만 사용 |
| 초대 링크를 미로그인 상태로 열 때 토큰 유실 | 가입 실패 | 딥링크 파라미터를 로그인 후까지 보존 |
| ViewModel에서 `Dispatchers.IO` 직접 지정 | 테스트 어려움 | Repository에서 처리, DI로 주입 |

---

## 16. 환경변수 / 설정

**Android — `front/local.properties` (커밋 금지)**
```properties
SUPABASE_URL=https://xxxx.supabase.co
SUPABASE_ANON_KEY=eyJ...
```
`build.gradle.kts` 에서 `BuildConfig` 로 주입한다. **Upstage 키는 여기 두지 않는다.**

**Supabase Edge Functions — `supabase secrets set`**
```bash
UPSTAGE_API_KEY=up_...
SOLAR_MODEL=solar-pro3
EMBED_MODEL=<Embed 2 모델명 — 콘솔에서 확인>
```

`.env.example` 과 `local.properties.example` 을 항상 최신 상태로 유지한다. 새 변수를 추가하면 같은 커밋에 반영한다.
