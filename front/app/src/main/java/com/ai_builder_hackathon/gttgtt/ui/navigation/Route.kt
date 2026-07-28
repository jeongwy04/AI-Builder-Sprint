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

    /** 마이페이지 — 홈 우상단 MY 버튼으로 진입 */
    @Serializable
    data object MyPage : Route

    /** 그룹 피드 — 그룹에 들어가면 처음 보이는 화면 */
    @Serializable
    data class GroupFeed(val archiveId: String) : Route

    /** 그룹 채팅 — 멤버들끼리의 대화 */
    @Serializable
    data class GroupChat(val archiveId: String) : Route

    // AI 추억 찾기는 별도 목적지가 아니다.
    // 그룹 피드 위에 뜨는 패널(AiChatPanel)이라서 라우트를 두지 않는다.

    /** S3 기억 상세 */
    @Serializable
    data class MemoryDetail(val memoryId: String) : Route

    /** S4 기억 작성 */
    @Serializable
    data class MemoryCreate(val archiveId: String) : Route
}
