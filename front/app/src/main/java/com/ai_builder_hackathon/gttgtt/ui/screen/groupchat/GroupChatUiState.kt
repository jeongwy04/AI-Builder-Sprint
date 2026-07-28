package com.ai_builder_hackathon.gttgtt.ui.screen.groupchat

import com.ai_builder_hackathon.gttgtt.domain.model.ChatListItem

data class GroupChatUiState(
    val isLoading: Boolean = true,
    val groupName: String = "",
    val memberCount: Int = 0,
    /** 날짜 구분선까지 섞인, 그리기만 하면 되는 목록 */
    val items: List<ChatListItem> = emptyList(),
    val input: String = "",
    val errorMessage: String? = null,
) {
    /** 공백만 입력한 상태로는 보낼 수 없다. */
    val canSend: Boolean get() = input.isNotBlank()
}
