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
) {
    val isEmpty: Boolean
        get() = !isLoading && posts.isEmpty()
}
