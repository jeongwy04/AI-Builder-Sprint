package com.ai_builder_hackathon.gttgtt.data.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

// 스키마 원천: docs/SCHEMA_REFERENCE.md — messages 테이블 (멤버 그룹 채팅, AI 대화와 별개)

@Serializable
data class MessageDto(
    @SerialName("id") val id: String,
    @SerialName("archive_id") val archiveId: String,
    @SerialName("sender_id") val senderId: String,
    @SerialName("body") val body: String? = null,
    @SerialName("image_path") val imagePath: String? = null,
    @SerialName("shared_memory_id") val sharedMemoryId: String? = null,
    @SerialName("created_at") val createdAt: String,
)

/** body/image_path/shared_memory_id 중 하나는 필수 (DB check 제약과 동일). */
@Serializable
data class MessageInsert(
    @SerialName("archive_id") val archiveId: String,
    @SerialName("body") val body: String? = null,
    @SerialName("image_path") val imagePath: String? = null,
    @SerialName("shared_memory_id") val sharedMemoryId: String? = null,
)

/** id 만 필요한 카운트 조회용 (postgrest count() API 대신 앱에서 size 로 센다). */
@Serializable
data class IdOnlyDto(
    @SerialName("id") val id: String,
)

/**
 * chat_reads upsert 요청 바디. `user_id` 는 DB 기본값(auth.uid())에 맡긴다 —
 * 어차피 RLS(`user_id = auth.uid()`)로만 내 행이 걸리니 명시할 필요가 없다.
 * `last_read_at` 은 반드시 명시한다 — upsert 충돌(on conflict) 시 값을 보낸 컬럼만 갱신되기 때문에,
 * 빠뜨리면 기존 값이 그대로 남아 안 읽음 배지가 줄지 않는다.
 */
@Serializable
data class ChatReadUpsertDto(
    @SerialName("archive_id") val archiveId: String,
    @SerialName("last_read_at") val lastReadAt: String,
)
