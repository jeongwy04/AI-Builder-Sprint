package com.ai_builder_hackathon.gttgtt.data.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

// chat Edge Function 계약 (docs/SCHEMA_REFERENCE.md §2)

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
    /** true 면 LLM 실패 → 키워드 폴백 검색 결과 (CLAUDE.md §9) */
    @SerialName("degraded") val degraded: Boolean? = null,
)

/**
 * 검색 결과 카드를 그리는 데 필요한 memories 최소 컬럼.
 * embedding 은 절대 select 하지 않는다 (4096차원 벡터, §memories 주석).
 *
 * select: id,memory_date,search_text
 */
@Serializable
data class MemoryCardDto(
    @SerialName("id") val id: String,
    @SerialName("memory_date") val memoryDate: String, // yyyy-MM-dd
    @SerialName("search_text") val searchText: String? = null,
)
