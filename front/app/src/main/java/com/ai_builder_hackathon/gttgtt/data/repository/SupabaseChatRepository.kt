package com.ai_builder_hackathon.gttgtt.data.repository

import com.ai_builder_hackathon.gttgtt.data.dto.MessageDto
import com.ai_builder_hackathon.gttgtt.data.dto.MessageInsert
import com.ai_builder_hackathon.gttgtt.data.dto.ProfileDto
import com.ai_builder_hackathon.gttgtt.data.remote.MediaUploader
import com.ai_builder_hackathon.gttgtt.domain.model.ChatMessage
import com.ai_builder_hackathon.gttgtt.domain.repository.ChatRepository
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.postgrest.postgrest
import io.github.jan.supabase.postgrest.query.Columns
import io.github.jan.supabase.postgrest.query.Order
import java.time.OffsetDateTime
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
            .select(Columns.raw("id,archive_id,sender_id,body,image_path,created_at")) {
                filter { eq("archive_id", archiveId) }
                order("created_at", Order.ASCENDING)
            }
            .decodeList<MessageDto>()

        val nameById = fetchNames(rows.map { it.senderId })

        rows.map { row ->
            ChatMessage(
                id = row.id,
                archiveId = row.archiveId,
                senderId = row.senderId,
                senderName = nameById[row.senderId] ?: "멤버",
                sentAtMillis = parseMillis(row.createdAt),
                text = row.body,
                photo = row.imagePath?.let { media.toPhoto(it) },
                isMine = myId != null && row.senderId == myId,
            )
        }
    }

    override suspend fun sendMessage(archiveId: String, text: String): Result<ChatMessage> = runCatching {
        val trimmed = text.trim()
        require(trimmed.isNotEmpty()) { "빈 메시지는 보낼 수 없습니다." }

        val row = supabase.postgrest.from("messages")
            .insert(MessageInsert(archiveId = archiveId, body = trimmed)) {
                select(Columns.raw("id,archive_id,sender_id,body,image_path,created_at"))
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
}
