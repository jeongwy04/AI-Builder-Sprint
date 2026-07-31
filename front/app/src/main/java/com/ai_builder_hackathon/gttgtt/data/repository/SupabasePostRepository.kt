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
import io.github.jan.supabase.auth.status.SessionStatus
import io.github.jan.supabase.postgrest.postgrest
import io.github.jan.supabase.postgrest.query.Columns
import io.github.jan.supabase.postgrest.query.Order
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.first
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

    /**
     * SupabaseArchiveRepository/SupabaseProfileRepository 의 awaitSessionReady() 와 같은 이유로
     * 여기도 필요하다: 그룹 대표 사진 변경처럼 Photo Picker(별도 액티비티)를 띄웠다 돌아오는
     * 흐름에서는, 화면이 ON_RESUME 되며 피드를 다시 불러오는 시점에 세션 복원이 아직 덜 끝나
     * 있을 수 있다(`SessionStatus.Initializing`). 이 상태에서 조회하면 RLS가 막아 게시물이
     * 0건("아직 올라온 추억이 없어요.")으로 잘못 보였다가, 화면을 나갔다 다시 들어오면
     * (그때는 세션 복원이 끝나 있어) 정상적으로 보이는 문제로 나타났다.
     */
    private suspend fun awaitSessionReady() {
        supabase.auth.sessionStatus.first { it !is SessionStatus.Initializing }
    }

    override suspend fun getFeed(archiveId: String): Result<List<Post>> = runCatching {
        awaitSessionReady()
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
        awaitSessionReady()
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

    /**
     * 여러 개를 한꺼번에 조립한다 — 게시물마다 왕복하면 피드가 N+1로 느려진다.
     * 자산/메모/좋아요/작성자 이름 네 조회는 서로 의존하지 않으니 동시에 시작하고,
     * 게시물별 사진 변환(사진마다 signed URL 발급)도 게시물 순서대로 기다리지 않고 병렬로 돌린다.
     */
    private suspend fun buildPosts(memories: List<MemoryDto>): List<Post> {
        if (memories.isEmpty()) return emptyList()
        val memoryIds = memories.map { it.id }
        val myId = supabase.auth.currentUserOrNull()?.id

        return coroutineScope {
            val assetsDeferred = async {
                supabase.postgrest.from("media_assets")
                    .select(Columns.raw("id,memory_id,storage_path,media_type,created_at")) {
                        filter { isIn("memory_id", memoryIds) }
                        order("created_at", Order.ASCENDING)
                    }
                    .decodeList<MediaAssetDto>()
                    .groupBy { it.memoryId }
            }

            val notesDeferred = async {
                supabase.postgrest.from("notes")
                    .select(Columns.raw("id,memory_id,archive_id,author_id,body,created_at")) {
                        filter { isIn("memory_id", memoryIds) }
                        order("created_at", Order.ASCENDING)
                    }
                    .decodeList<NoteDto>()
                    .groupBy { it.memoryId }
            }

            val reactionsDeferred = async {
                supabase.postgrest.from("reactions")
                    .select(Columns.raw("id,memory_id,user_id")) {
                        filter { isIn("memory_id", memoryIds) }
                    }
                    .decodeList<ReactionDto>()
                    .groupBy { it.memoryId }
            }

            val authorsDeferred = async { fetchAuthors(memories.map { it.authorId }) }

            val assetsByMemory = assetsDeferred.await()
            val notesByMemory = notesDeferred.await()
            val reactionsByMemory = reactionsDeferred.await()
            val authorById = authorsDeferred.await()

            val photosByMemory = memories
                .associate { memory ->
                    memory.id to async {
                        media.toPhotos(assetsByMemory[memory.id].orEmpty().map { it.storagePath })
                    }
                }
                .mapValues { (_, deferred) -> deferred.await() }

            memories.map { memory ->
                val notes = notesByMemory[memory.id].orEmpty()
                val reactions = reactionsByMemory[memory.id].orEmpty()
                val photos = photosByMemory[memory.id].orEmpty()

                val author = authorById[memory.authorId]

                Post(
                    id = memory.id,
                    archiveId = memory.archiveId,
                    authorId = memory.authorId,
                    authorName = author?.name ?: "멤버",
                    authorAvatarUrl = author?.avatarUrl,
                    authorAvatarPath = author?.avatarPath,
                    memoryDateMillis = parseDateMillis(memory.memoryDate),
                    photos = photos,
                    // 피드에는 제목만 보여준다 — 첫 note 는 "title\n본문"으로 접혀 저장되어 있으므로
                    // (SupabaseMemoryRepository 와 같은 워크어라운드) 첫 줄만 뽑아 쓴다.
                    caption = extractTitle(notes.firstOrNull()?.body.orEmpty()),
                    likeCount = reactions.size,
                    commentCount = (notes.size - 1).coerceAtLeast(0),
                    likedByMe = myId != null && reactions.any { it.userId == myId },
                )
            }
        }
    }

    /**
     * 작성자 이름 + 아바타. `profiles.avatar_url` 엔 storage path 만 들어있어(signed URL 아님)
     * 매번 새로 발급받아야 한다 — SupabaseProfileRepository.getMyProfile() 과 같은 이유.
     * 작성자가 여러 명이면 발급도 병렬로 돌린다(사진 signed URL 발급과 같은 이유 — N명이면
     * 순차로는 N번 왕복해야 해서 느려진다).
     */
    private suspend fun fetchAuthors(userIds: List<String>): Map<String, AuthorInfo> {
        val ids = userIds.distinct()
        if (ids.isEmpty()) return emptyMap()

        val profiles = supabase.postgrest.from("profiles")
            .select(Columns.raw("id,display_name,avatar_url")) {
                filter { isIn("id", ids) }
            }
            .decodeList<ProfileDto>()

        return coroutineScope {
            profiles
                .map { profile ->
                    async {
                        profile.id to AuthorInfo(
                            name = profile.displayName ?: "멤버",
                            avatarUrl = profile.avatarUrl?.let { path -> media.avatarSignedUrl(path) },
                            avatarPath = profile.avatarUrl,
                        )
                    }
                }
                .associate { it.await() }
        }
    }

    private data class AuthorInfo(val name: String, val avatarUrl: String?, val avatarPath: String? = null)

    private fun parseDateMillis(date: String): Long =
        runCatching {
            LocalDate.parse(date).atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()
        }.getOrDefault(0L)

    /** SupabaseMemoryRepository.getDetail() 의 title 계산과 같은 규칙 — 저장된 본문 첫 줄. */
    private fun extractTitle(storedBody: String): String =
        storedBody.lineSequence().firstOrNull { it.isNotBlank() }?.take(TITLE_MAX)
            ?: "제목 없는 기억"

    private companion object {
        const val TITLE_MAX = 40
    }
}
