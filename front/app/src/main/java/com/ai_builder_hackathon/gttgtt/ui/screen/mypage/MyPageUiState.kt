package com.ai_builder_hackathon.gttgtt.ui.screen.mypage

import com.ai_builder_hackathon.gttgtt.domain.model.UserProfile

data class MyPageUiState(
    val isLoading: Boolean = true,
    val profile: UserProfile? = null,
    val errorMessage: String? = null,
    /** 로그아웃이 끝나면 화면이 로그인으로 나가야 한다. */
    val isSignedOut: Boolean = false,
    /** 닉네임 수정 다이얼로그가 열려 있는지. */
    val isEditingNickname: Boolean = false,
    /** 다이얼로그 안 입력창의 임시 값. 저장 성공 전까지는 [UserProfile.name] 을 건드리지 않는다. */
    val nicknameDraft: String = "",
    /** 중복 확인 + 저장이 진행 중일 때 true — 이 동안 저장 버튼을 막는다. */
    val isSavingNickname: Boolean = false,
    /** 다이얼로그 안에서만 보여주는 에러 (예: "이미 사용 중인 닉네임이에요."). */
    val nicknameError: String? = null,
    /** 프로필 사진 업로드가 진행 중일 때 true — 아바타 위에 로딩 오버레이를 띄운다. */
    val isAvatarUploading: Boolean = false,
    /** 프로필 사진 업로드 실패 메시지. */
    val avatarError: String? = null,
)
