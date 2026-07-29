package com.ai_builder_hackathon.gttgtt.data.repository

import com.ai_builder_hackathon.gttgtt.data.dto.EmbedMemoryRequest
import com.ai_builder_hackathon.gttgtt.data.dto.MediaAssetDto
import com.ai_builder_hackathon.gttgtt.data.dto.MediaAssetInsert
import com.ai_builder_hackathon.gttgtt.data.dto.MemoryDateUpdate
import com.ai_builder_hackathon.gttgtt.data.dto.MemoryDto
import com.ai_builder_hackathon.gttgtt.data.dto.MemoryIdRefDto
import com.ai_builder_hackathon.gttgtt.data.dto.MemoryInsert
import com.ai_builder_hackathon.gttgtt.data.dto.MemoryPersonDto
import com.ai_builder_hackathon.gttgtt.data.dto.MemoryPersonInsert
import com.ai_builder_hackathon.gttgtt.data.dto.NoteBodyUpdate
import com.ai_builder_hackathon.gttgtt.data.dto.NoteDto
import com.ai_builder_hackathon.gttgtt.data.dto.NoteInsert
import com.ai_builder_hackathon.gttgtt.data.dto.ProfileDto
import com.ai_builder_hackathon.gttgtt.data.dto.ReactionDto
import com.ai_builder_hackathon.gttgtt.data.remote.MediaUploader
import com.ai_builder_hackathon.gttgtt.domain.model.Comment
import com.ai_builder_hackathon.gttgtt.domain.model.MemoryDetail
import com.ai_builder_hackathon.gttgtt.domain.model.MemorySummary
import com.ai_builder_hackathon.gttgtt.domain.model.NewMemory
import com.ai_builder_hackathon.gttgtt.domain.model.Participant
import com.ai_builder_hackathon.gttgtt.domain.repository.MemoryRepository
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.functions.functions
import io.github.jan.supabase.postgrest.postgrest
import io.github.jan.supabase.postgrest.query.Columns
import io.github.jan.supabase.postgrest.query.Order
import java.time.Instant
import java.time.LocalDate
import java.time.OffsetDateTime
import java.time.ZoneId
import javax.inject.Inject

/**
 * 기억 상세 조회와 작성.
 *
 * ⚠️ 작성 시 마지막에 반드시 embed-memory 를 호출한다.
 * 빠뜨리면 search_text/embedding 이 비어 AI 검색에 영영 안 잡힌다 (CLAUDE.md §5.6, §15).
 */
class SupabaseMemoryRepository @Inject constructor(
    private val supabase: SupabaseClient,
    private val media: MediaUploader,
) : MemoryRepository {

    override suspend fun getDetail(memoryId: String): Result<MemoryDetail> = runCatching {
        val memory = supabase.postgrest.from("memories")
            // embedding 은 select 하지 않는다 (4096차원).
            .select(Columns.raw("id,archive_id,author_id,memory_date,place_name,search_text,created_at")) {
                filter { eq("id", memoryId) }
            }
            .decodeSingle<MemoryDto>()

        val assets = supabase.postgrest.from("media_assets")
            .select(Columns.raw("id,memory_id,storage_path,media_type,created_at")) {
                filter { eq("memory_id", memoryId) }
                order("created_at", Order.ASCENDING)
            }
            .decodeList<MediaAssetDto>()

        val notes = supabase.postgrest.from("notes")
            .select(Columns.raw("id,memory_id,archive_id,author_id,body,created_at")) {
                filter { eq("memory_id", memoryId) }
                order("created_at", Order.ASCENDING)
            }
            .decodeList<NoteDto>()

        val people = supabase.postgrest.from("memory_people")
            .select(Columns.raw("memory_id,user_id")) {
                filter { eq("memory_id", memoryId) }
            }
            .decodeList<MemoryPersonDto>()

        // 좋아요 — 피드(SupabasePostRepository.buildPosts())와 같은 reactions 테이블을 읽는다.
        val myId = supabase.auth.currentUserOrNull()?.id
        val reactions = supabase.postgrest.from("reactions")
            .select(Columns.raw("id,memory_id,user_id")) {
                filter { eq("memory_id", memoryId) }
            }
            .decodeList<ReactionDto>()

        val nameById = fetchNames(notes.map { it.authorId } + people.map { it.userId })

        // 첫 note 가 본문, 나머지는 댓글처럼 취급한다.
        // 스키마에 comments 테이블이 없어 notes 를 겸용하는 구조다.
        val storedBody = notes.firstOrNull()?.body.orEmpty()
        val title = storedBody.lineSequence().firstOrNull { it.isNotBlank() }?.take(TITLE_MAX)
            ?: "제목 없는 기억"
        val comments = notes.drop(1).map { note ->
            Comment(
                id = note.id,
                authorId = note.authorId,
                authorName = nameById[note.authorId] ?: "멤버",
                text = note.body,
                createdAtMillis = parseMillis(note.createdAt),
            )
        }

        MemoryDetail(
            id = memory.id,
            archiveId = memory.archiveId,
            memoryDateMillis = parseDateMillis(memory.memoryDate),
            title = title,
            // storedBody 는 저장용 원문이라 title 이 첫 줄에 접혀 들어가 있다.
            // 화면엔 title 을 따로 보여주므로, 본문에서는 그 줄을 떼어내야 안 겹친다.
            body = stripTitleLine(title, storedBody),
            photos = assets.map { asset -> media.toPhoto(asset.id, asset.storagePath) },
            participants = people.map { p ->
                Participant(id = p.userId, name = nameById[p.userId] ?: "멤버")
            },
            relatedPhotos = emptyList(), // 연관 사진은 후순위 기능
            comments = comments,
            likeCount = reactions.size,
            likedByMe = myId != null && reactions.any { it.userId == myId },
        )
    }

    override suspend fun getMyMemories(): Result<List<MemorySummary>> = runCatching {
        val user = supabase.auth.currentUserOrNull() ?: error("로그인이 필요합니다.")
        supabase.postgrest.from("memories")
            .select(Columns.raw("id,archive_id,author_id,memory_date,place_name,search_text,created_at")) {
                filter { eq("author_id", user.id) }
                order("memory_date", Order.DESCENDING)
            }
            .decodeList<MemoryDto>()
            .map { it.toSummary() }
    }

    override suspend fun getLikedMemories(): Result<List<MemorySummary>> = runCatching {
        val user = supabase.auth.currentUserOrNull() ?: error("로그인이 필요합니다.")
        // 1) 내가 좋아요한 memory_id 목록
        val likedIds = supabase.postgrest.from("reactions")
            .select(Columns.raw("memory_id")) {
                filter { eq("user_id", user.id) }
            }
            .decodeList<MemoryIdRefDto>()
            .map { it.memoryId }
        if (likedIds.isEmpty()) return@runCatching emptyList()

        // 2) 그 기억들을 최근순으로
        supabase.postgrest.from("memories")
            .select(Columns.raw("id,archive_id,author_id,memory_date,place_name,search_text,created_at")) {
                filter { isIn("id", likedIds) }
                order("memory_date", Order.DESCENDING)
            }
            .decodeList<MemoryDto>()
            .map { it.toSummary() }
    }

    /** MemoryDto → 목록 요약. 미리보기는 search_text 첫 줄(= 메모 첫 줄), 없으면 장소. */
    private fun MemoryDto.toSummary(): MemorySummary {
        val preview = searchText?.substringBefore("\n")?.trim()?.takeIf { it.isNotEmpty() }
            ?: placeName?.takeIf { it.isNotBlank() }
            ?: "메모가 없는 기억"
        return MemorySummary(
            id = id,
            archiveId = archiveId,
            memoryDateMillis = parseDateMillis(memoryDate),
            placeName = placeName,
            preview = preview,
        )
    }

    override suspend fun createMemory(memory: NewMemory): Result<String> = runCatching {
        // 1) memories insert — id 를 받아야 이후 경로를 만들 수 있다.
        val created = supabase.postgrest.from("memories")
            .insert(
                MemoryInsert(
                    archiveId = memory.archiveId,
                    memoryDate = toDateString(memory.memoryDateMillis),
                )
            ) { select(Columns.raw("id,archive_id,author_id,memory_date,place_name,search_text,created_at")) }
            .decodeSingle<MemoryDto>()

        // 2) 사진 업로드 → media_assets 등록
        val paths = media.uploadMemoryPhotos(memory.archiveId, created.id, memory.photoUris)
        if (paths.isNotEmpty()) {
            supabase.postgrest.from("media_assets").insert(
                paths.map { path ->
                    MediaAssetInsert(
                        memoryId = created.id,
                        archiveId = memory.archiveId,
                        storagePath = path,
                        mediaType = "image",
                    )
                }
            )
        }

        // 3) 본문을 note 로 저장 — 검색의 유일한 재료다.
        // ⚠️ memories 테이블엔 title 컬럼이 없다. getDetail() 이 저장된 본문 첫 줄로 title 을
        // 다시 계산하므로, 여기서 title 을 첫 줄로 접어 넣어야 다음에 불러올 때도 살아남는다.
        val storedBody = composeStoredBody(memory.title, memory.body)
        if (storedBody.isNotBlank()) {
            supabase.postgrest.from("notes").insert(
                NoteInsert(
                    memoryId = created.id,
                    archiveId = memory.archiveId,
                    body = storedBody,
                )
            )
        }

        // 4) 함께한 사람 태깅
        if (memory.participantIds.isNotEmpty()) {
            supabase.postgrest.from("memory_people").insert(
                memory.participantIds.map { userId ->
                    MemoryPersonInsert(
                        memoryId = created.id,
                        archiveId = memory.archiveId,
                        userId = userId,
                    )
                }
            )
        }

        // 5) ⚠️ 임베딩 갱신 — 이 호출을 빠뜨리면 검색에 안 잡힌다.
        requestEmbedding(created.id)

        created.id
    }

    override suspend fun addComment(memoryId: String, text: String): Result<Comment> = runCatching {
        val trimmed = text.trim()
        require(trimmed.isNotEmpty()) { "빈 댓글은 등록할 수 없습니다." }

        val archiveId = supabase.postgrest.from("memories")
            .select(Columns.raw("id,archive_id,author_id,memory_date,place_name,search_text,created_at")) {
                filter { eq("id", memoryId) }
            }
            .decodeSingle<MemoryDto>()
            .archiveId

        val note = supabase.postgrest.from("notes")
            .insert(NoteInsert(memoryId = memoryId, archiveId = archiveId, body = trimmed)) {
                select(Columns.raw("id,memory_id,archive_id,author_id,body,created_at"))
            }
            .decodeSingle<NoteDto>()

        // 댓글도 note 라서 검색 재료가 바뀐다 → 임베딩 재갱신 (§5.6).
        requestEmbedding(memoryId)

        Comment(
            id = note.id,
            authorId = note.authorId,
            authorName = currentUserName() ?: "나",
            text = note.body,
            createdAtMillis = parseMillis(note.createdAt),
        )
    }

    override suspend fun updateMemory(
        memoryId: String,
        archiveId: String,
        title: String,
        body: String,
        memoryDateMillis: Long,
        participantIds: List<String>,
        newPhotoUris: List<String>,
        removedPhotoIds: List<String>,
    ): Result<Unit> = runCatching {
        // 1) 날짜 갱신
        supabase.postgrest.from("memories")
            .update(MemoryDateUpdate(memoryDate = toDateString(memoryDateMillis))) {
                filter { eq("id", memoryId) }
            }

        // 2) 본문(첫 note) 갱신 — 검색 재료가 바뀐다.
        // title 은 첫 줄로 접어 넣는다 — createMemory() 와 같은 이유(§주석 참고).
        val storedBody = composeStoredBody(title, body)

        val firstNote = supabase.postgrest.from("notes")
            .select(Columns.raw("id,memory_id,archive_id,author_id,body,created_at")) {
                filter { eq("memory_id", memoryId) }
                order("created_at", Order.ASCENDING)
                limit(1)
            }
            .decodeList<NoteDto>()
            .firstOrNull()

        when {
            firstNote != null -> supabase.postgrest.from("notes")
                .update(NoteBodyUpdate(body = storedBody)) {
                    filter { eq("id", firstNote.id) }
                }
            storedBody.isNotBlank() -> supabase.postgrest.from("notes")
                .insert(NoteInsert(memoryId = memoryId, archiveId = archiveId, body = storedBody))
        }

        // 3) 함께한 사람 — 전체 삭제 후 재삽입이 가장 단순하고 안전하다.
        supabase.postgrest.from("memory_people").delete { filter { eq("memory_id", memoryId) } }
        if (participantIds.isNotEmpty()) {
            supabase.postgrest.from("memory_people").insert(
                participantIds.map { userId ->
                    MemoryPersonInsert(memoryId = memoryId, archiveId = archiveId, userId = userId)
                }
            )
        }

        // 4) 사진 제거 — Storage 오브젝트를 먼저 지우고 나서 media_assets 행을 지운다.
        // 순서를 바꾸면(행부터 지우면) Storage 업로드가 실패했을 때 고아 오브젝트가 남는다.
        if (removedPhotoIds.isNotEmpty()) {
            val toRemove = supabase.postgrest.from("media_assets")
                .select(Columns.raw("id,memory_id,storage_path,media_type,created_at")) {
                    filter { isIn("id", removedPhotoIds) }
                }
                .decodeList<MediaAssetDto>()
            media.deleteMemoryPhotos(toRemove.map { it.storagePath })
            supabase.postgrest.from("media_assets").delete { filter { isIn("id", removedPhotoIds) } }
        }

        // 5) 사진 추가 — createMemory() 와 같은 순서(업로드 → media_assets insert).
        val uploadedPaths = media.uploadMemoryPhotos(archiveId, memoryId, newPhotoUris)
        if (uploadedPaths.isNotEmpty()) {
            supabase.postgrest.from("media_assets").insert(
                uploadedPaths.map { path ->
                    MediaAssetInsert(
                        memoryId = memoryId,
                        archiveId = archiveId,
                        storagePath = path,
                        mediaType = "image",
                    )
                }
            )
        }

        // 6) ⚠️ 임베딩 갱신 — 이 호출을 빠뜨리면 검색에 안 잡힌다.
        requestEmbedding(memoryId)
    }

    override suspend fun deleteMemory(memoryId: String): Result<Unit> = runCatching {
        // media_assets/notes/memory_people 은 memory_id FK 에 on delete cascade 라
        // memories 한 행만 지우면 하위 데이터가 함께 정리된다.
        supabase.postgrest.from("memories").delete { filter { eq("id", memoryId) } }
    }

    /**
     * 임베딩 갱신 요청.
     * 실패해도 기억 저장 자체는 성공으로 둔다 — 사용자가 쓴 글이 날아가는 것보다,
     * 검색이 늦게 잡히는 편이 낫다. 실패는 로그로 남긴다.
     */
    private suspend fun requestEmbedding(memoryId: String) {
        runCatching {
            supabase.functions.invoke(
                function = "embed-memory",
                body = EmbedMemoryRequest(memoryId = memoryId),
            )
        }.onFailure {
            android.util.Log.w(TAG, "embed-memory 호출 실패 (memoryId=$memoryId). 검색에 늦게 잡힐 수 있음.", it)
        }
    }

    private suspend fun fetchNames(userIds: List<String>): Map<String, String> {
        val ids = userIds.distinct()
        if (ids.isEmpty()) return emptyMap()
        return supabase.postgrest.from("profiles")
            .select(Columns.raw("id,display_name,avatar_url")) {
                filter { isIn("id", ids) }
            }
            .decodeList<ProfileDto>()
            .associate { it.id to (it.displayName ?: "멤버") }
    }

    private fun currentUserName(): String? =
        supabase.auth.currentUserOrNull()?.userMetadata?.get("full_name")?.toString()?.trim('"')

    private fun toDateString(millis: Long): String =
        Instant.ofEpochMilli(millis).atZone(ZoneId.systemDefault()).toLocalDate().toString()

    private fun parseDateMillis(date: String): Long =
        runCatching {
            LocalDate.parse(date).atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()
        }.getOrDefault(0L)

    private fun parseMillis(iso: String): Long =
        runCatching { OffsetDateTime.parse(iso).toInstant().toEpochMilli() }.getOrDefault(0L)

    /**
     * memories 테이블엔 title 컬럼이 없어서, 저장용 본문 첫 줄에 title 을 접어 넣는 방식으로
     * 흉내낸다. title 이 이미 본문 첫 줄과 같으면(=고칠 게 없으면) 그대로 둔다 — 안 그러면
     * 저장할 때마다 제목 줄이 계속 쌓인다.
     */
    private fun composeStoredBody(title: String, body: String): String {
        if (title.isBlank()) return body
        val firstLine = body.lineSequence().firstOrNull { it.isNotBlank() }
        if (firstLine == title) return body
        return if (body.isBlank()) title else "$title\n$body"
    }

    /** composeStoredBody() 의 반대 — 화면에 보여줄 본문에서는 title 로 쓰인 첫 줄을 뺀다. */
    private fun stripTitleLine(title: String, storedBody: String): String {
        val lines = storedBody.lines()
        val firstNonBlankIndex = lines.indexOfFirst { it.isNotBlank() }
        if (firstNonBlankIndex == -1 || lines[firstNonBlankIndex] != title) return storedBody

        val rest = lines.toMutableList()
        rest.removeAt(firstNonBlankIndex)
        while (rest.isNotEmpty() && rest.first().isBlank()) rest.removeAt(0)
        return rest.joinToString("\n")
    }

    private companion object {
        const val TITLE_MAX = 40
        const val TAG = "MemoryRepository"
    }
}
