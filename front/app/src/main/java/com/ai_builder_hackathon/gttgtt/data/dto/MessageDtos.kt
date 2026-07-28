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
    @SerialName("created_at") val createdAt: String,
)

/** body/image_path 중 하나는 필수 (DB check 제약과 동일). */
@Serializable
data class MessageInsert(
    @SerialName("archive_id") val archiveId: String,
    @SerialName("body") val body: String? = null,
    @SerialName("image_path") val imagePath: String? = null,
)

/** id 만 필요한 카운트 조회용 (postgrest count() API 대신 앱에서 size 로 센다). */
@Serializable
data class IdOnlyDto(
    @SerialName("id") val id: String,
)
