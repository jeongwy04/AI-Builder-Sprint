package com.ai_builder_hackathon.gttgtt.domain.model

/**
 * 목록 화면(내 추억 / 좋아요한 추억)의 한 행.
 *
 * 상세(MemoryDetail)보다 가벼운 요약이다. 검색·상세와 달리 여러 archive 를 가로질러 모을 수 있다.
 */
data class MemorySummary(
    val id: String,
    val archiveId: String,
    /** 추억이 일어난 날 (정렬·표시 기준, created_at 아님). */
    val memoryDateMillis: Long,
    val placeName: String?,
    /** 목록에 보여줄 한 줄 미리보기 — 메모 첫 줄(없으면 장소). */
    val preview: String,
)
