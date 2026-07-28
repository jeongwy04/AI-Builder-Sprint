package com.ai_builder_hackathon.gttgtt.ui.navigation

import kotlinx.serialization.Serializable

/**
 * Navigation Compose type-safe routes.
 * 문자열 경로를 조립하지 않는다 — 오타가 런타임에야 드러나기 때문.
 */
sealed interface Route {

    /** S0 로그인 */
    @Serializable
    data object Auth : Route

    /** S1 그룹(보관소) 선택 */
    @Serializable
    data object GroupList : Route

    /** S2 AI 대화 — 그룹 진입 시의 홈 */
    @Serializable
    data class Chat(val archiveId: String) : Route

    /** S3 기억 상세 */
    @Serializable
    data class MemoryDetail(val memoryId: String) : Route

    /** S4 기억 작성 */
    @Serializable
    data class MemoryCreate(val archiveId: String) : Route

    /** S5 타임라인 */
    @Serializable
    data class Timeline(val archiveId: String) : Route
}
