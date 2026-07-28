package com.ai_builder_hackathon.gttgtt.data.repository

import com.ai_builder_hackathon.gttgtt.data.dto.MediaAssetDto
import com.ai_builder_hackathon.gttgtt.data.dto.MemoryDto
import com.ai_builder_hackathon.gttgtt.data.dto.NoteDto
import com.ai_builder_hackathon.gttgtt.data.dto.ProfileDto
import com.ai_builder_hackathon.gttgtt.data.dto.ReactionDto
import com.ai_builder_hackathon.gttgtt.data.dto.ReactionInsert
import com.ai_builder_hackathon.gttgtt.data.remote.MediaUploader
import com.ai_builder_hackathon.gttgtt.domain.model.Post
import com.ai_builder_hackathon.gttgtt.domain.repository.PostRepository
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.postgrest.postgrest
import io.github.jan.supabase.postgrest.query.Columns
import io.github.jan.supabase.postgrest.query.Order
import java.time.LocalDate
import java.time.ZoneId
import javax.inject.Inject

/**
 * 그룹 피드 = `memories` 를 게시물처럼 보여준다.
 *
 * ⚠️ `comments` 테이블이 스키마에 없다. `notes` 의 첫 글을 캡션(body)으로,
 * 나머지를 댓글로 취급하는 SupabaseMemoryRepository 와 같은 규칙을 여기서도 따른다 (commentCount 계산).
 * 좋아요는 `reactions` 테이블 (memory_id, user_id) 유니크 제약을 그대로 토글에 쓴다.
 */
class SupabasePostRepository @Inject constructor(
    private val supabase: SupabaseClient,
    private val media: MediaUploader,
) : PostRepository {

    override suspend fun getFeed(archiveId: String): Result<List<Post>> = runCatching {
        val memories = supabase.postgrest.from("memories")
            // embedding 은 4096차원이라 select 하지 않는다.
            .select(Columns.raw("id,archive_id,author_id,memory_date,place_name,search_text,created_at")) {
                filter { eq("archive_id", archiveId) }
                // 정렬은 항상 memory_date (CLAUDE.md §6.2). created_at 아님.
                order("memory_date", Order.DESCENDING)
            }
            .decodeList<MemoryDto>()

        buildPosts(memories)
    }

    override suspend fun toggleLike(postId: String): Result<Post> = runCatching {
        val userId = supabase.auth.currentUserOrNull()?.id
            ?: error("좋아요를 누르려면 로그인이 필요합니다.")

        val memory = supabase.postgrest.from("memories")
            .select(Columns.raw("id,archive_id,author_id,memory_date,place_name,search_text,created_at")) {
                filter { eq("id", postId) }
            }
            .decodeSingle<MemoryDto>()

        val existing = supabase.postgrest.from("reactions")
            .select(Columns.raw("id,memory_id,user_id")) {
                filter {
                    eq("memory_id", postId)
                    eq("user_id", userId)
                }
            }
            .decodeList<ReactionDto>()
            .firstOrNull()

        if (existing != null) {
            supabase.postgrest.from("reactions").delete {
                filter { eq("id", existing.id) }
            }
        } else {
            supabase.postgrest.from("reactions").insert(
                ReactionInsert(memoryId = postId, archiveId = memory.archiveId)
            )
        }

        buildPosts(listOf(memory)).first()
    }

    /** 여러 개를 한꺼번에 조립한다 — 게시물마다 왕복하면 피드가 N+1로 느려진다. */
    private suspend fun buildPosts(memories: List<MemoryDto>): List<Post> {
        if (memories.isEmpty()) return emptyList()
        val memoryIds = memories.map { it.id }
        val myId = supabase.auth.currentUserOrNull()?.id

        val assetsByMemory = supabase.postgrest.from("media_assets")
            .select(Columns.raw("id,memory_id,storage_path,media_type,created_at")) {
                filter { isIn("memory_id", memoryIds) }
                order("created_at", Order.ASCENDING)
            }
            .decodeList<MediaAssetDto>()
            .groupBy { it.memoryId }

        val notesByMemory = supabase.postgrest.from("notes")
            .select(Columns.raw("id,memory_id,archive_id,author_id,body,created_at")) {
                filter { isIn("memory_id", memoryIds) }
                order("created_at", Order.ASCENDING)
            }
            .decodeList<NoteDto>()
            .groupBy { it.memoryId }

        val reactionsByMemory = supabase.postgrest.from("reactions")
            .select(Columns.raw("id,memory_id,user_id")) {
                filter { isIn("memory_id", memoryIds) }
            }
            .decodeList<ReactionDto>()
            .groupBy { it.memoryId }

        val nameById = fetchNames(memories.map { it.authorId })

        return memories.map { memory ->
            val notes = notesByMemory[memory.id].orEmpty()
            val reactions = reactionsByMemory[memory.id].orEmpty()
            val photos = media.toPhotos(assetsByMemory[memory.id].orEmpty().map { it.storagePath })

            Post(
                id = memory.id,
                archiveId = memory.archiveId,
                authorId = memory.authorId,
                authorName = nameById[memory.authorId] ?: "멤버",
                memoryDateMillis = parseDateMillis(memory.memoryDate),
                photos = photos,
                // 첫 note = 본문/캡션, 나머지 = 댓글 (comments 테이블 부재 워크어라운드).
                caption = notes.firstOrNull()?.body.orEmpty(),
                likeCount = reactions.size,
                commentCount = (notes.size - 1).coerceAtLeast(0),
                likedByMe = myId != null && reactions.any { it.userId == myId },
            )
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

    private fun parseDateMillis(date: String): Long =
        runCatching {
            LocalDate.parse(date).atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()
        }.getOrDefault(0L)
}
