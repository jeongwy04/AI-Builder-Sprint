package com.ai_builder_hackathon.gttgtt.data.repository

import com.ai_builder_hackathon.gttgtt.data.dto.ChatReadUpsertDto
import com.ai_builder_hackathon.gttgtt.data.dto.MediaAssetDto
import com.ai_builder_hackathon.gttgtt.data.dto.MemoryDto
import com.ai_builder_hackathon.gttgtt.data.dto.MessageDto
import com.ai_builder_hackathon.gttgtt.data.dto.MessageInsert
import com.ai_builder_hackathon.gttgtt.data.dto.NoteDto
import com.ai_builder_hackathon.gttgtt.data.dto.ProfileDto
import com.ai_builder_hackathon.gttgtt.data.remote.MediaUploader
import com.ai_builder_hackathon.gttgtt.domain.model.ChatMessage
import com.ai_builder_hackathon.gttgtt.domain.model.SharedMemoryPreview
import com.ai_builder_hackathon.gttgtt.domain.repository.ChatRepository
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.postgrest.postgrest
import io.github.jan.supabase.postgrest.query.Columns
import io.github.jan.supabase.postgrest.query.Order
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import java.time.LocalDate
import java.time.OffsetDateTime
import java.time.ZoneId
import javax.inject.Inject

/**
 * 그룹 멤버끼리의 채팅 (`messages` 테이블). AI 대화(`chat_messages`)와는 별개다.
 *
 * ⚠️ 실시간(SSE/Realtime)은 스코프 밖 (CLAUDE.md §13). 화면이 다시 열리거나
 * 사용자가 보낼 때마다 목록을 새로 불러오는 폴링 방식으로 충분하다.
 */
class SupabaseChatRepository @Inject constructor(
    private val supabase: SupabaseClient,
    private val media: MediaUploader,
) : ChatRepository {

    override suspend fun getMessages(archiveId: String): Result<List<ChatMessage>> = runCatching {
        val myId = supabase.auth.currentUserOrNull()?.id

        val rows = supabase.postgrest.from("messages")
            .select(Columns.raw("id,archive_id,sender_id,body,image_path,shared_memory_id,created_at")) {
                filter { eq("archive_id", archiveId) }
                order("created_at", Order.ASCENDING)
            }
            .decodeList<MessageDto>()

        coroutineScope {
            // 이름/공유 미리보기 조회와 메시지별 채팅 사진(signed URL 발급)은 서로 무관하니
            // 전부 동시에 시작한다 — 사진이 실린 메시지가 여러 개면 예전엔 순서대로 기다렸다.
            val namesDeferred = async { fetchNames(rows.map { it.senderId }) }
            val previewsDeferred = async { resolveSharedMemoryPreviews(rows.mapNotNull { it.sharedMemoryId }) }
            val photoDeferredById = rows.associate { row ->
                row.id to async { row.imagePath?.let { media.toPhoto(it) } }
            }

            val nameById = namesDeferred.await()
            val previewByMemoryId = previewsDeferred.await()

            rows.map { row ->
                ChatMessage(
                    id = row.id,
                    archiveId = row.archiveId,
                    senderId = row.senderId,
                    senderName = nameById[row.senderId] ?: "멤버",
                    sentAtMillis = parseMillis(row.createdAt),
                    text = row.body,
                    photo = photoDeferredById.getValue(row.id).await(),
                    sharedMemory = row.sharedMemoryId?.let { previewByMemoryId[it] ?: deletedMemoryPreview(it) },
                    isMine = myId != null && row.senderId == myId,
                )
            }
        }
    }

    override suspend fun sendMessage(archiveId: String, text: String): Result<ChatMessage> = runCatching {
        val trimmed = text.trim()
        require(trimmed.isNotEmpty()) { "빈 메시지는 보낼 수 없습니다." }

        val row = supabase.postgrest.from("messages")
            .insert(MessageInsert(archiveId = archiveId, body = trimmed)) {
                select(Columns.raw("id,archive_id,sender_id,body,image_path,shared_memory_id,created_at"))
            }
            .decodeSingle<MessageDto>()

        ChatMessage(
            id = row.id,
            archiveId = row.archiveId,
            senderId = row.senderId,
            senderName = currentUserName() ?: "나",
            sentAtMillis = parseMillis(row.createdAt),
            text = row.body,
            photo = row.imagePath?.let { media.toPhoto(it) },
            isMine = true,
        )
    }

    override suspend fun sendSharedMemory(archiveId: String, memoryId: String): Result<ChatMessage> = runCatching {
        val row = supabase.postgrest.from("messages")
            .insert(MessageInsert(archiveId = archiveId, sharedMemoryId = memoryId)) {
                select(Columns.raw("id,archive_id,sender_id,body,image_path,shared_memory_id,created_at"))
            }
            .decodeSingle<MessageDto>()

        val preview = resolveSharedMemoryPreviews(listOf(memoryId))[memoryId] ?: deletedMemoryPreview(memoryId)

        ChatMessage(
            id = row.id,
            archiveId = row.archiveId,
            senderId = row.senderId,
            senderName = currentUserName() ?: "나",
            sentAtMillis = parseMillis(row.createdAt),
            sharedMemory = preview,
            isMine = true,
        )
    }

    override suspend fun markRead(archiveId: String): Result<Unit> = runCatching {
        supabase.postgrest.from("chat_reads")
            .upsert(ChatReadUpsertDto(archiveId = archiveId, lastReadAt = OffsetDateTime.now().toString())) {
                onConflict = "archive_id,user_id"
            }
        Unit
    }

    /**
     * 메시지에 딸린 공유 기억 미리보기를 한꺼번에 조립한다 (메시지마다 왕복하면 N+1).
     * SupabasePostRepository.buildPosts() 와 같은 title/photo 계산 규칙을 쓴다.
     */
    private suspend fun resolveSharedMemoryPreviews(memoryIds: List<String>): Map<String, SharedMemoryPreview> {
        val ids = memoryIds.distinct()
        if (ids.isEmpty()) return emptyMap()

        val memories = supabase.postgrest.from("memories")
            .select(Columns.raw("id,archive_id,author_id,memory_date,place_name,search_text,created_at")) {
                filter { isIn("id", ids) }
            }
            .decodeList<MemoryDto>()
        if (memories.isEmpty()) return emptyMap()

        val foundIds = memories.map { it.id }

        return coroutineScope {
            // 노트/사진 조회는 서로 무관해 동시에 시작한다.
            val notesDeferred = async {
                supabase.postgrest.from("notes")
                    .select(Columns.raw("id,memory_id,archive_id,author_id,body,created_at")) {
                        filter { isIn("memory_id", foundIds) }
                        order("created_at", Order.ASCENDING)
                    }
                    .decodeList<NoteDto>()
                    .groupBy { it.memoryId }
                    .mapValues { (_, notes) -> notes.firstOrNull()?.body.orEmpty() }
            }

            val photoPathDeferred = async {
                supabase.postgrest.from("media_assets")
                    .select(Columns.raw("id,memory_id,storage_path,media_type,created_at")) {
                        filter { isIn("memory_id", foundIds) }
                        order("created_at", Order.ASCENDING)
                    }
                    .decodeList<MediaAssetDto>()
                    .groupBy { it.memoryId }
                    .mapValues { (_, assets) -> assets.firstOrNull()?.storagePath }
            }

            val firstNoteByMemory = notesDeferred.await()
            val firstPhotoPathByMemory = photoPathDeferred.await()

            // 기억마다 signed URL 발급도 순서대로 기다리지 않고 한꺼번에 병렬로 돌린다.
            val photoDeferredByMemory = memories.associate { memory ->
                memory.id to async { firstPhotoPathByMemory[memory.id]?.let { media.toPhoto(it) } }
            }

            memories.associate { memory ->
                val storedBody = firstNoteByMemory[memory.id].orEmpty()
                val title = storedBody.lineSequence().firstOrNull { it.isNotBlank() }?.take(TITLE_MAX)
                    ?: "제목 없는 기억"
                memory.id to SharedMemoryPreview(
                    memoryId = memory.id,
                    title = title,
                    photo = photoDeferredByMemory.getValue(memory.id).await(),
                    memoryDateMillis = parseMemoryDateMillis(memory.memoryDate),
                )
            }
        }
    }

    /** 공유 당시엔 있었지만 이제 조회가 안 되는(RLS/삭제) 기억. 말풍선은 비워서라도 보여준다. */
    private fun deletedMemoryPreview(memoryId: String) = SharedMemoryPreview(
        memoryId = memoryId,
        title = "삭제된 기억이에요",
        photo = null,
        memoryDateMillis = 0L,
    )

    private fun parseMemoryDateMillis(date: String): Long =
        runCatching {
            LocalDate.parse(date).atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()
        }.getOrDefault(0L)

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

    private fun parseMillis(iso: String): Long =
        runCatching { OffsetDateTime.parse(iso).toInstant().toEpochMilli() }.getOrDefault(0L)

    private companion object {
        const val TITLE_MAX = 40
    }
}
