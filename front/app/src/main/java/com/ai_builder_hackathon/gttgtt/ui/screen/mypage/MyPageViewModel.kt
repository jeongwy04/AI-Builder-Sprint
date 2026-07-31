package com.ai_builder_hackathon.gttgtt.ui.screen.mypage

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ai_builder_hackathon.gttgtt.domain.repository.ProfileRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class MyPageViewModel @Inject constructor(
    private val profileRepository: ProfileRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(MyPageUiState())
    val uiState: StateFlow<MyPageUiState> = _uiState.asStateFlow()

    init {
        loadProfile()
    }

    /** 한 줄 상태 메시지 저장. 성공 시 화면 상태도 즉시 갱신한다. */
    fun onStatusSave(newStatus: String) {
        viewModelScope.launch {
            profileRepository.updateStatus(newStatus)
                .onSuccess {
                    val shown = newStatus.trim().ifBlank { DEFAULT_STATUS }
                    _uiState.update { state ->
                        state.copy(profile = state.profile?.copy(statusMessage = shown))
                    }
                }
                .onFailure { throwable ->
                    _uiState.update {
                        it.copy(errorMessage = throwable.message ?: "상태 메시지를 저장하지 못했습니다.")
                    }
                }
        }
    }

    /** 프로필 헤더의 닉네임을 탭했을 때 다이얼로그를 연다. 현재 닉네임을 초안으로 미리 채운다. */
    fun onEditNicknameClick() {
        val current = _uiState.value.profile?.name.orEmpty()
        _uiState.update {
            it.copy(isEditingNickname = true, nicknameDraft = current, nicknameError = null)
        }
    }

    fun onNicknameDraftChange(value: String) {
        _uiState.update { it.copy(nicknameDraft = value, nicknameError = null) }
    }

    fun onNicknameEditDismiss() {
        if (_uiState.value.isSavingNickname) return
        _uiState.update { it.copy(isEditingNickname = false, nicknameError = null) }
    }

    /**
     * 회원가입 화면과 같은 순서로 확인한다: 먼저 `is_nickname_taken` RPC 로 중복부터 물어보고,
     * 통과하면 [ProfileRepository.updateNickname] 을 부른다. 그 사이에 다른 사람이 같은
     * 닉네임을 먼저 가져가는 경쟁 조건은 `profiles_display_name_unique_idx` 유니크 제약이
     * 최종 방어선이라 SupabaseProfileRepository.updateNickname() 이 23505 를 사용자 메시지로
     * 바꿔서 돌려준다.
     */
    fun onNicknameSaveClick() {
        val state = _uiState.value
        val trimmed = state.nicknameDraft.trim()

        if (trimmed.isEmpty()) {
            _uiState.update { it.copy(nicknameError = "닉네임을 입력해주세요.") }
            return
        }

        // 기존 닉네임과 동일하면 중복 확인·저장 없이 그냥 닫는다.
        if (trimmed == state.profile?.name) {
            _uiState.update { it.copy(isEditingNickname = false, nicknameError = null) }
            return
        }

        _uiState.update { it.copy(isSavingNickname = true, nicknameError = null) }

        viewModelScope.launch {
            val taken = profileRepository.isNicknameTaken(trimmed).getOrDefault(false)
            if (taken) {
                _uiState.update {
                    it.copy(isSavingNickname = false, nicknameError = "이미 사용 중인 닉네임이에요.")
                }
                return@launch
            }

            profileRepository.updateNickname(trimmed)
                .onSuccess {
                    _uiState.update { current ->
                        current.copy(
                            isSavingNickname = false,
                            isEditingNickname = false,
                            profile = current.profile?.copy(name = trimmed),
                        )
                    }
                }
                .onFailure { throwable ->
                    _uiState.update {
                        it.copy(
                            isSavingNickname = false,
                            nicknameError = throwable.message ?: "닉네임을 저장하지 못했어요.",
                        )
                    }
                }
        }
    }

    /**
     * Photo Picker 로 고른 로컬 [imageUri] 를 프로필 사진으로 올린다.
     * 업로드가 끝나면 [ProfileRepository.updateAvatar] 가 곧바로 signed URL 을 돌려주니
     * 그걸로 화면 상태를 갱신한다 — [imageUri] 는 로컬 content:// URI 라 다시 읽을 수 없어서다.
     */
    fun onAvatarPicked(imageUri: String) {
        _uiState.update { it.copy(isAvatarUploading = true, avatarError = null) }
        viewModelScope.launch {
            profileRepository.updateAvatar(imageUri)
                .onSuccess { upload ->
                    _uiState.update { state ->
                        state.copy(
                            isAvatarUploading = false,
                            profile = state.profile?.copy(avatarUrl = upload.url, avatarPath = upload.path),
                        )
                    }
                }
                .onFailure { throwable ->
                    _uiState.update {
                        it.copy(
                            isAvatarUploading = false,
                            avatarError = throwable.message ?: "프로필 사진을 변경하지 못했습니다.",
                        )
                    }
                }
        }
    }

    fun onDismissAvatarError() {
        _uiState.update { it.copy(avatarError = null) }
    }

    fun onSignOutClick() {
        viewModelScope.launch {
            profileRepository.signOut()
                .onSuccess { _uiState.update { it.copy(isSignedOut = true) } }
                .onFailure { throwable ->
                    _uiState.update {
                        it.copy(errorMessage = throwable.message ?: "로그아웃하지 못했습니다.")
                    }
                }
        }
    }

    private fun loadProfile() {
        _uiState.update { it.copy(isLoading = true, errorMessage = null) }
        viewModelScope.launch {
            profileRepository.getMyProfile()
                .onSuccess { profile ->
                    _uiState.update { it.copy(isLoading = false, profile = profile) }
                }
                .onFailure { throwable ->
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            errorMessage = throwable.message ?: "프로필을 불러오지 못했습니다.",
                        )
                    }
                }
        }
    }

    private companion object {
        const val DEFAULT_STATUS = "추억을 모으는 중 ✨"
    }
}
