package com.ai_builder_hackathon.gttgtt.data.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

// 스키마 원천: docs/SCHEMA_REFERENCE.md

// ── 읽기용 ──

/** embedding(4096차원)은 절대 select 하지 않는다. */
@Serializable
data class MemoryDto(
    @SerialName("id") val id: String,
    @SerialName("archive_id") val archiveId: String,
    @SerialName("author_id") val authorId: String,
    @SerialName("memory_date") val memoryDate: String, // yyyy-MM-dd
    @SerialName("place_name") val placeName: String? = null,
    @SerialName("search_text") val searchText: String? = null,
    @SerialName("created_at") val createdAt: String,
)

@Serializable
data class MediaAssetDto(
    @SerialName("id") val id: String,
    @SerialName("memory_id") val memoryId: String,
    @SerialName("storage_path") val storagePath: String,
    @SerialName("media_type") val mediaType: String,
    @SerialName("created_at") val createdAt: String,
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

@Serializable
data class MemoryPersonDto(
    @SerialName("memory_id") val memoryId: String,
    @SerialName("user_id") val userId: String,
)

@Serializable
data class ReactionDto(
    @SerialName("id") val id: String,
    @SerialName("memory_id") val memoryId: String,
    @SerialName("user_id") val userId: String,
)

// ── 쓰기용 (id/author_id/created_at 은 서버 default) ──

@Serializable
data class MemoryInsert(
    @SerialName("archive_id") val archiveId: String,
    @SerialName("memory_date") val memoryDate: String,
    @SerialName("place_name") val placeName: String? = null,
)

@Serializable
data class NoteInsert(
    @SerialName("memory_id") val memoryId: String,
    @SerialName("archive_id") val archiveId: String,
    @SerialName("body") val body: String,
)

// ── 수정용 (부분 update payload) ──

@Serializable
data class MemoryDateUpdate(
    @SerialName("memory_date") val memoryDate: String,
)

@Serializable
data class NoteBodyUpdate(
    @SerialName("body") val body: String,
)

@Serializable
data class MediaAssetInsert(
    @SerialName("memory_id") val memoryId: String,
    @SerialName("archive_id") val archiveId: String,
    @SerialName("storage_path") val storagePath: String,
    @SerialName("media_type") val mediaType: String,
)

@Serializable
data class MemoryPersonInsert(
    @SerialName("memory_id") val memoryId: String,
    @SerialName("archive_id") val archiveId: String,
    @SerialName("user_id") val userId: String,
)

@Serializable
data class ReactionInsert(
    @SerialName("memory_id") val memoryId: String,
    @SerialName("archive_id") val archiveId: String,
)

/** embed-memory Edge Function 요청 */
@Serializable
data class EmbedMemoryRequest(
    @SerialName("memory_id") val memoryId: String,
)
