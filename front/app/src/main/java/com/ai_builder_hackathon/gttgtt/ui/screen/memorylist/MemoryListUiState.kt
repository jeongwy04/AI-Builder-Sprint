package com.ai_builder_hackathon.gttgtt.ui.screen.memorylist

import com.ai_builder_hackathon.gttgtt.domain.model.MemorySummary

/** 목록 종류 — 같은 화면을 재사용한다. */
enum class MemoryListKind { MINE, LIKED }

data class MemoryListUiState(
    val isLoading: Boolean = true,
    val memories: List<MemorySummary> = emptyList(),
    val errorMessage: String? = null,
) {
    val isEmpty: Boolean
        get() = !isLoading && errorMessage == null && memories.isEmpty()
}
