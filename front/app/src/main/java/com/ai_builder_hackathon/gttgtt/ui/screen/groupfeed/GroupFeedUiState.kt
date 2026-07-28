package com.ai_builder_hackathon.gttgtt.ui.screen.groupfeed

import com.ai_builder_hackathon.gttgtt.domain.model.Post

data class GroupFeedUiState(
    val isLoading: Boolean = true,
    val groupName: String = "",
    val memberCount: Int = 0,
    val posts: List<Post> = emptyList(),
    val errorMessage: String? = null,
) {
    val isEmpty: Boolean
        get() = !isLoading && posts.isEmpty()
}
