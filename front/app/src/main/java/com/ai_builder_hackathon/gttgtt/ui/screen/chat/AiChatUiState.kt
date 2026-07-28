package com.ai_builder_hackathon.gttgtt.ui.screen.chat

import com.ai_builder_hackathon.gttgtt.domain.model.AiMessage

data class AiChatUiState(
    val messages: List<AiMessage> = emptyList(),
    val input: String = "",
    /** AI 응답을 기다리는 중. 입력은 막고 "생각 중" 표시를 띄운다. */
    val isThinking: Boolean = false,
    val errorMessage: String? = null,
) {
    val canSend: Boolean get() = input.isNotBlank() && !isThinking
}
