package com.ai_builder_hackathon.gttgtt.ui.screen.grouplist

import com.ai_builder_hackathon.gttgtt.domain.model.ArchiveSummary

data class GroupListUiState(
    val isLoading: Boolean = true,
    val query: String = "",
    /** 최근 활동순으로 정렬된 채팅방 목록 */
    val groups: List<ArchiveSummary> = emptyList(),
    val errorMessage: String? = null,
) {
    val isEmpty: Boolean
        get() = !isLoading && groups.isEmpty()
}
