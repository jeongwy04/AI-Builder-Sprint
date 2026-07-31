# 그때그때 — 스키마 레퍼런스 (DTO 매핑용)

> 프론트엔드 DTO(`@Serializable`) 작성을 위한 참조 문서.
> **원천은 `supabase/migrations/` SQL 파일이다.** 스키마가 바뀌면 마이그레이션이 먼저 바뀌고, 이 문서를 뒤따라 갱신한다.
>
> - `20260727120000_init.sql` — 9테이블 + `is_member()` + RLS + Storage + `match_memories()`/`accept_invitation()`
> - `20260727130000_create_archive_rpc.sql` — `create_archive()`
> - `20260727140000_social_features.sql` — `messages` / `chat_reads`+`unread_counts()` / `reactions` / `memory_people`
> - `20260728120000_comments.sql` — `comments` (게시물 댓글)
> - `20260731180000_match_memories_similarity_threshold.sql` — `match_memories()`에 `p_max_distance` 유사도 임계값 추가

---

## 0. 타입 매핑 규칙

| Postgres | Kotlin | 비고 |
|---|---|---|
| `uuid` | `String` | |
| `text` | `String` / `String?` | `NOT NULL` 여부로 결정 |
| `timestamptz` | `String` (ISO-8601) | 또는 kotlinx-datetime `Instant` |
| `date` | `String` (`yyyy-MM-dd`) | 또는 `LocalDate`. **정렬·검색 기준은 `memory_date`** |
| `int` | `Int` | |
| `bigint` | `Long` | |
| `double precision` | `Double` | |
| `jsonb` | `JsonElement` / `JsonObject` | kotlinx.serialization |
| `uuid[]` | `List<String>` | |
| `vector(4096)` | **매핑하지 않음** | DTO에서 제외 (아래 §memories 주석) |

**공통 규칙 (CLAUDE.md §10)**
- 컬럼명은 snake_case → 프로퍼티는 camelCase + `@SerialName("snake_case")`.
- DB `NOT NULL` = Kotlin non-null (읽기 DTO 기준). nullable 컬럼만 `?`.
- **insert 시 서버 기본값 컬럼은 DTO에서 빼거나 null 로 둔다**: `id`(gen_random_uuid), `created_at`(now), `author_id`/`sender_id`/`created_by`/`user_id`(`default auth.uid()`). 읽기용과 쓰기용 DTO를 분리하거나, 쓰기 시 해당 필드를 직렬화에서 제외하는 방식을 권장.

---

## 1. 테이블

### profiles
`auth.users` 1:1. 회원가입 시 트리거로 자동 생성.

| 컬럼 | 타입 | Null | 비고 |
|---|---|---|---|
| id | uuid | NN | PK, = auth.users.id |
| display_name | text | nullable | |
| avatar_url | text | nullable | |
| created_at | timestamptz | NN | default now() |

### archives (그룹 = 보관소)
| 컬럼 | 타입 | Null | 비고 |
|---|---|---|---|
| id | uuid | NN | PK |
| name | text | NN | |
| group_type | text | nullable | `family`/`couple`/`friends`/`club` (관례값, DB 제약 아님) |
| cover_image_path | text | nullable | |
| created_by | uuid | NN | default auth.uid() |
| created_at | timestamptz | NN | |

> ⚠️ archives 직접 insert 금지 → `create_archive()` RPC 사용 (§2).

### memberships (⚠️ role 컬럼 없음 — 전원 동일 권한)
| 컬럼 | 타입 | Null | 비고 |
|---|---|---|---|
| id | uuid | NN | PK |
| archive_id | uuid | NN | |
| user_id | uuid | NN | |
| joined_at | timestamptz | NN | |

`unique(archive_id, user_id)`

### invitations (초대 토큰)
| 컬럼 | 타입 | Null | 비고 |
|---|---|---|---|
| id | uuid | NN | PK |
| archive_id | uuid | NN | |
| token | text | NN | unique, 자동 생성 |
| expires_at | timestamptz | NN | default now()+7d |
| used_count | int | NN | default 0 |
| created_by | uuid | NN | |
| created_at | timestamptz | NN | |

> 비멤버는 invitations select 불가 → 가입은 `accept_invitation()` RPC (§2).

### memories (기억 단위 + 검색 인덱스)
| 컬럼 | 타입 | Null | 비고 |
|---|---|---|---|
| id | uuid | NN | PK |
| archive_id | uuid | NN | |
| author_id | uuid | NN | default auth.uid() |
| **memory_date** | date | NN | **추억이 일어난 날 — 정렬·검색 기준** |
| place_name | text | nullable | |
| lat | double precision | nullable | |
| lng | double precision | nullable | |
| search_text | text | nullable | embed-memory가 조립 (앱에서 조립 X) |
| embedding | vector(4096) | nullable | **DTO에서 제외** (무겁고 앱에서 쓸 일 없음) |
| created_at | timestamptz | NN | 업로드 시각 (정렬에 쓰지 말 것) |

> ⚠️ 정렬·검색은 항상 `memory_date`. `created_at` 아님.
> `memories` select 시 `embedding` 을 안 받으려면 postgrest `select("id,archive_id,author_id,memory_date,place_name,lat,lng,search_text,created_at")` 처럼 컬럼을 명시한다.

### media_assets (Storage 미디어 메타)
| 컬럼 | 타입 | Null | 비고 |
|---|---|---|---|
| id | uuid | NN | PK |
| memory_id | uuid | NN | |
| archive_id | uuid | NN | |
| storage_path | text | NN | `{archive_id}/{memory_id}/{uuid}.{ext}` |
| media_type | text | NN | `image` / `video` |
| mime_type | text | nullable | |
| size_bytes | bigint | nullable | |
| duration_sec | double precision | nullable | |
| exif | jsonb | nullable | |
| created_at | timestamptz | NN | |

> 미디어는 **signed URL** 로만 조회 (버킷 private).

### notes (메모·문구 — 검색의 유일한 재료)
| 컬럼 | 타입 | Null | 비고 |
|---|---|---|---|
| id | uuid | NN | PK |
| memory_id | uuid | NN | |
| archive_id | uuid | NN | |
| author_id | uuid | NN | default auth.uid() |
| body | text | NN | |
| created_at | timestamptz | NN | |

> ⚠️ note 추가/수정/삭제 후 **반드시** `embed-memory` Function 호출 (안 하면 검색에 안 잡힘).

### chat_sessions (AI 대화 세션 — 사용자 개인 스코프)
| 컬럼 | 타입 | Null | 비고 |
|---|---|---|---|
| id | uuid | NN | PK |
| archive_id | uuid | NN | |
| user_id | uuid | NN | default auth.uid() |
| created_at | timestamptz | NN | |

### chat_messages (AI 대화 메시지)
| 컬럼 | 타입 | Null | 비고 |
|---|---|---|---|
| id | uuid | NN | PK |
| session_id | uuid | NN | |
| archive_id | uuid | NN | |
| role | text | NN | `user` / `assistant` / `tool` |
| content | text | nullable | |
| tool_calls | jsonb | nullable | |
| result_memory_ids | uuid[] | nullable | 검색된 memory id 목록 |
| created_at | timestamptz | NN | |

> 보통은 이 테이블을 직접 다루기보다 `chat` Function 응답을 사용 (§2).

### messages (멤버 그룹 채팅 — AI 대화와 별개)
| 컬럼 | 타입 | Null | 비고 |
|---|---|---|---|
| id | uuid | NN | PK |
| archive_id | uuid | NN | |
| sender_id | uuid | NN | default auth.uid() |
| body | text | nullable | |
| image_path | text | nullable | `{archive_id}/chat/{uuid}.{ext}` |
| created_at | timestamptz | NN | |

`check (body is not null or image_path is not null)` — 둘 중 하나는 필수.

### chat_reads (채팅방 마지막 읽은 시점 — 안읽음 배지)
| 컬럼 | 타입 | Null | 비고 |
|---|---|---|---|
| archive_id | uuid | NN | PK(복합) |
| user_id | uuid | NN | PK(복합), default auth.uid() |
| last_read_at | timestamptz | NN | 방 열 때 `now()` 로 upsert |

### reactions (게시물 좋아요)
| 컬럼 | 타입 | Null | 비고 |
|---|---|---|---|
| id | uuid | NN | PK |
| memory_id | uuid | NN | |
| archive_id | uuid | NN | |
| user_id | uuid | NN | default auth.uid() |
| created_at | timestamptz | NN | |

`unique(memory_id, user_id)` — 한 사람당 한 번.

### memory_people (함께한 사람 태깅)
| 컬럼 | 타입 | Null | 비고 |
|---|---|---|---|
| memory_id | uuid | NN | PK(복합) |
| archive_id | uuid | NN | |
| user_id | uuid | NN | PK(복합) |

### comments (게시물 댓글)
| 컬럼 | 타입 | Null | 비고 |
|---|---|---|---|
| id | uuid | NN | PK |
| memory_id | uuid | NN | |
| archive_id | uuid | NN | |
| author_id | uuid | NN | default auth.uid() |
| body | text | NN | FE `Comment.text` ↔ `body` 매핑 |
| created_at | timestamptz | NN | |

> notes 와 별개 — 댓글은 검색(embedding)에 관여하지 않는다.
> 조회·작성은 멤버 누구나, 수정·삭제는 작성자 본인만.
> FE `Comment.authorName` 은 `profiles.display_name` 조인, `likeCount` 는 현재 UI 기본값(댓글 좋아요 테이블 없음).

---

## 2. RPC / Edge Function (프론트 호출 계약)

### RPC (`postgrest-kt` `rpc(...)`)
| 함수 | 시그니처 | 반환 | 용도 |
|---|---|---|---|
| `create_archive` | `p_name text, p_group_type text?` | `archives` (1행) | 그룹 생성 (직접 insert 금지) |
| `accept_invitation` | `p_token text` | `uuid` (archive_id) | 초대 수락·가입 |
| `unread_counts` | (없음) | `[{archive_id uuid, unread int}]` | 홈 안읽음 배지 일괄 조회 |
| `match_memories` | 내부용 | `setof memories` | **FE 직접 호출 X** — `chat` Function 내부 |

### Edge Function (`functions-kt` `invoke(...)`)
**chat** — AI 대화 검색
```
요청:  { archive_id: String, message: String, session_id: String? }
응답:  { session_id: String, reply: String, memory_ids: List<String>, degraded: Boolean? }
```
→ `memory_ids` 로 `memories`(+ media_assets, notes) 조회해 결과 카드 구성. `degraded=true` 면 LLM 폴백(키워드) 검색 결과.

**embed-memory** — 메모 임베딩 갱신
```
요청:  { memory_id: String }
응답:  (성공/실패)
```
→ note 저장/수정/삭제 직후 **반드시** 호출.

---

## 3. DTO 예시 (Kotlin)

```kotlin
package com.ai_builder_hackathon.gttgtt.data.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

// ── 읽기용 ────────────────────────────────────────────────
@Serializable
data class MemoryDto(
    @SerialName("id") val id: String,
    @SerialName("archive_id") val archiveId: String,
    @SerialName("author_id") val authorId: String,
    @SerialName("memory_date") val memoryDate: String,   // yyyy-MM-dd
    @SerialName("place_name") val placeName: String? = null,
    @SerialName("lat") val lat: Double? = null,
    @SerialName("lng") val lng: Double? = null,
    @SerialName("search_text") val searchText: String? = null,
    @SerialName("created_at") val createdAt: String,
    // embedding 은 매핑하지 않는다 (select 시 컬럼 목록에서도 제외)
)

@Serializable
data class NoteDto(
    @SerialName("id") val id: String,
    @SerialName("memory_id") val memoryId: String,
    @SerialName("archive_id") val archiveId: String,
    @SerialName("author_id") val authorId: String,
    @SerialName("body") val body: String,
    @SerialName("created_at") val createdAt: String,
)

// ── 쓰기용 (서버 기본값 컬럼 제외) ─────────────────────────
@Serializable
data class NoteInsert(
    @SerialName("memory_id") val memoryId: String,
    @SerialName("archive_id") val archiveId: String,
    @SerialName("body") val body: String,
    // id/author_id/created_at 는 서버 default 로 채워짐 → 넣지 않음
)

// ── Edge Function ────────────────────────────────────────
@Serializable
data class ChatRequest(
    @SerialName("archive_id") val archiveId: String,
    @SerialName("message") val message: String,
    @SerialName("session_id") val sessionId: String? = null,
)

@Serializable
data class ChatResponse(
    @SerialName("session_id") val sessionId: String,
    @SerialName("reply") val reply: String,
    @SerialName("memory_ids") val memoryIds: List<String> = emptyList(),
    @SerialName("degraded") val degraded: Boolean? = null,
)
```

---

## 4. 반드시 지킬 규칙 (요약)
- 정렬·검색은 항상 `memory_date` (created_at 아님).
- 미디어는 signed URL 로만. 버킷 `memories`(private). 경로 `{archive_id}/{memory_id}/{uuid}.{ext}`, 채팅사진 `{archive_id}/chat/{uuid}.{ext}`.
- 프로필 아바타는 별도 버킷 `avatars`(private). 경로 `{user_id}/{uuid}.{ext}`, 본인만 쓰기, 로그인 사용자 전체 읽기(signed URL).
- note 변경 후 `embed-memory` 호출.
- 그룹 생성은 `create_archive` RPC, 가입은 `accept_invitation` RPC.
- 역할·권한 등급 없음 — 멤버 전원 동일. 권한 판정은 서버 RLS(`is_member`)가 담당.
- `embedding` 은 DTO/앱에서 다루지 않음.
