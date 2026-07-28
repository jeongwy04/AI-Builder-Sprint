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
) {
    val canSubmitComment: Boolean
        get() = commentInput.isNotBlank() && !isSubmittingComment
}
