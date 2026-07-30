package com.ai_builder_hackathon.gttgtt.ui.screen.groupchat

import com.ai_builder_hackathon.gttgtt.domain.model.ChatListItem

data class GroupChatUiState(
    val isLoading: Boolean = true,
    val groupName: String = "",
    val memberCount: Int = 0,
    /**
     * 날짜 구분선까지 섞인, 그리기만 하면 되는 목록.
     * 검색 중([isSearchActive])이면 검색어에 매치되는 메시지만 남긴 목록이다.
     */
    val items: List<ChatListItem> = emptyList(),
    val input: String = "",
    val errorMessage: String? = null,

    // 대화 검색 — 상단바 검색 버튼
    val isSearchActive: Boolean = false,
    val searchQuery: String = "",
) {
    /** 공백만 입력한 상태로는 보낼 수 없다. */
    val canSend: Boolean get() = input.isNotBlank()

    /** [items] 중 날짜 구분선을 뺀 실제 메시지 수 — 검색 결과 건수 표시에 쓴다. */
    val messageCount: Int get() = items.count { it is ChatListItem.Message }
}
