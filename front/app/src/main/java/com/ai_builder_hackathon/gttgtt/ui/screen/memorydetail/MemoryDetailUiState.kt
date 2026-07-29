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
) {
    val canSubmitComment: Boolean
        get() = commentInput.isNotBlank() && !isSubmittingComment
}
