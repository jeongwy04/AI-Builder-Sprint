package com.ai_builder_hackathon.gttgtt.ui.screen.groupfeed

import com.ai_builder_hackathon.gttgtt.domain.model.Post

data class GroupFeedUiState(
    val isLoading: Boolean = true,
    val groupName: String = "",
    val memberCount: Int = 0,
    val posts: List<Post> = emptyList(),
    val errorMessage: String? = null,
    /** 하단 네비게이션 바 채팅 버튼 우측 상단 배지 — 이 그룹방의 안 읽은 메시지 수. */
    val chatUnreadCount: Int = 0,

    // ── 그룹 설정 (우측 상단 톱니 버튼) ──
    val isSettingsSheetOpen: Boolean = false,

    // 이름 변경
    val isRenameDialogOpen: Boolean = false,
    val renameText: String = "",
    val isRenaming: Boolean = false,
    val renameError: String? = null,

    // 그룹 대표 사진 변경
    val coverImageUrl: String? = null,
    val isCoverImageUpdating: Boolean = false,
    val coverImageError: String? = null,

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

    // 게시물(기억) 채팅방 공유 — 카드 우하단 보내기 버튼
    /** 지금 전송 중인 게시물 id. 동시에 하나만 보낼 수 있다. */
    val sharingPostId: String? = null,
    /** 공유에 실패한 게시물 id — 그 카드에만 에러를 보여준다. */
    val shareErrorPostId: String? = null,
    val shareError: String? = null,
    /** 공유에 성공할 때마다 1씩 늘어난다. 화면이 이 값 변화를 보고 채팅방으로 이동한다. */
    val shareSuccessCount: Int = 0,
) {
    val isEmpty: Boolean
        get() = !isLoading && posts.isEmpty()
}
