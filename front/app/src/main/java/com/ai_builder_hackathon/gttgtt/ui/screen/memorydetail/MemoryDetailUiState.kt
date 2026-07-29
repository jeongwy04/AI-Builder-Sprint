package com.ai_builder_hackathon.gttgtt.ui.screen.memorydetail

import com.ai_builder_hackathon.gttgtt.domain.model.MemoryDetail

data class MemoryDetailUiState(
    val isLoading: Boolean = true,
    val memory: MemoryDetail? = null,
    /** 히어로에서 지금 보고 있는 사진 번호(0-base) */
    val currentPhotoIndex: Int = 0,
    val commentInput: String = "",
    val isSubmittingComment: Boolean = false,
    val errorMessage: String? = null,
    /** "더보기" 버튼을 누르면 뜨는 수정/삭제 메뉴 */
    val isOptionsSheetOpen: Boolean = false,
    val isDeleteConfirmOpen: Boolean = false,
    val isDeleting: Boolean = false,
    val deleteError: String? = null,
    /** 삭제 성공 신호. 화면을 나가는 신호로 쓴다 (MemoryCreateUiState.savedMemoryId 와 같은 패턴). */
    val isDeleted: Boolean = false,

    // 채팅방 공유 — 사진 아래 오른쪽 보내기 버튼 (GroupFeedUiState 와 같은 패턴)
    val isSharing: Boolean = false,
    val shareError: String? = null,
    /** 공유에 성공할 때마다 1씩 늘어난다. 화면이 이 값 변화를 보고 채팅방으로 이동한다. */
    val shareSuccessCount: Int = 0,

    /**
     * 피드 카드의 댓글 버튼으로 들어왔을 때만 true — 화면이 이 값을 보고 댓글 입력창에
     * 포커스를 주고 키보드를 띄운다. 한 번 처리하면 onCommentFocusHandled() 로 꺼야 한다 —
     * 안 그러면 수정 화면 등을 갔다 돌아왔을 때 이 화면이 재조립되며 또 키보드가 뜬다.
     */
    val shouldFocusCommentInput: Boolean = false,
) {
    val canSubmitComment: Boolean
        get() = commentInput.isNotBlank() && !isSubmittingComment
}
