package com.ai_builder_hackathon.gttgtt.ui.screen.mypage

import com.ai_builder_hackathon.gttgtt.domain.model.UserProfile

data class MyPageUiState(
    val isLoading: Boolean = true,
    val profile: UserProfile? = null,
    val errorMessage: String? = null,
    /** 로그아웃이 끝나면 화면이 로그인으로 나가야 한다. */
    val isSignedOut: Boolean = false,
)
