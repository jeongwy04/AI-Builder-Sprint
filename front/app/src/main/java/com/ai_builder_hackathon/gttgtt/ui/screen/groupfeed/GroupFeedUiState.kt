package com.ai_builder_hackathon.gttgtt.ui.screen.groupfeed

import com.ai_builder_hackathon.gttgtt.domain.model.Post

data class GroupFeedUiState(
    val isLoading: Boolean = true,
    val groupName: String = "",
    val memberCount: Int = 0,
    val posts: List<Post> = emptyList(),
    val errorMessage: String? = null,

    // ── 그룹 설정 (우측 상단 톱니 버튼) ──
    val isSettingsSheetOpen: Boolean = false,

    // 이름 변경
    val isRenameDialogOpen: Boolean = false,
    val renameText: String = "",
    val isRenaming: Boolean = false,
    val renameError: String? = null,

    // 삭제
    val isDeleteConfirmOpen: Boolean = false,
    val isDeleting: Boolean = false,
    val deleteError: String? = null,
    /** true 가 되면 화면이 이 이벤트를 보고 뒤로 나간다. */
    val isGroupDeleted: Boolean = false,

    // 친구 초대
    val isInviteDialogOpen: Boolean = false,
    val isInviteLoading: Boolean = false,
    val inviteToken: String? = null,
    val inviteError: String? = null,

    // 게시물(기억) 삭제 — 카드 우상단 점 세개 버튼
    /** 삭제 확인창이 떠 있는 게시물 id. null 이면 닫힌 상태. */
    val postPendingDeleteId: String? = null,
    val isDeletingPost: Boolean = false,
    val deletePostError: String? = null,
) {
    val isEmpty: Boolean
        get() = !isLoading && posts.isEmpty()
}
