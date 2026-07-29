package com.ai_builder_hackathon.gttgtt.ui.navigation

import kotlinx.serialization.Serializable

/**
 * Navigation Compose type-safe routes.
 * 문자열 경로를 조립하지 않는다 — 오타가 런타임에야 드러나기 때문.
 */
sealed interface Route {

    /** 온보딩 — 첫 진입(로그아웃 상태) 인트로 캐러셀. 로그인돼 있으면 자동 스킵. */
    @Serializable
    data object Onboarding : Route

    /** S0 로그인 */
    @Serializable
    data object Auth : Route

    /** S0-B 회원가입 — 닉네임을 받고 구글 계정으로 가입한다. */
    @Serializable
    data object SignUp : Route

    /** S1 그룹(보관소) 선택 */
    @Serializable
    data object GroupList : Route

    /** 마이페이지 — 홈 우상단 MY 버튼으로 진입 */
    @Serializable
    data object MyPage : Route

    /** 내가 남긴 추억 목록 — 마이페이지에서 진입 */
    @Serializable
    data object MyMemories : Route

    /** 좋아요한 추억 목록 — 마이페이지에서 진입 */
    @Serializable
    data object LikedMemories : Route

    /** 그룹 피드 — 그룹에 들어가면 처음 보이는 화면 */
    @Serializable
    data class GroupFeed(val archiveId: String) : Route

    /** 그룹 채팅 — 멤버들끼리의 대화 */
    @Serializable
    data class GroupChat(val archiveId: String) : Route

    // AI 추억 찾기는 별도 목적지가 아니다.
    // 그룹 피드 위에 뜨는 패널(AiChatPanel)이라서 라우트를 두지 않는다.

    /**
     * S3 기억 상세.
     * @param focusComment 진입하자마자 댓글 입력창에 포커스를 주고 키보드를 띄울지.
     * 피드 카드의 댓글 버튼으로 들어올 때만 true — 카드 전체 탭이나 다른 진입 경로는 false.
     */
    @Serializable
    data class MemoryDetail(val memoryId: String, val focusComment: Boolean = false) : Route

    /**
     * S4 기억 작성 / 수정.
     * memoryId 가 있으면 수정 모드 — 제목/본문/날짜/함께한 사람만 고칠 수 있고 사진은 그대로 둔다.
     * initialPhotoUri 가 있으면(홈에서 카메라로 촬영해 들어온 경우) 그 사진을 미리 담은 채 연다.
     */
    @Serializable
    data class MemoryCreate(
        val archiveId: String,
        val memoryId: String? = null,
        val initialPhotoUri: String? = null,
    ) : Route
}
