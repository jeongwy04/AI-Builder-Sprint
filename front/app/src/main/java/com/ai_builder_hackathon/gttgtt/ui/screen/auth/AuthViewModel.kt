package com.ai_builder_hackathon.gttgtt.ui.screen.auth

import androidx.lifecycle.ViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import javax.inject.Inject

/**
 * S0 로그인 ViewModel.
 *
 * 현재는 폼 상태(이메일·비밀번호·옵션)와 입력 검증만 담당한다.
 * 실제 로그인은 이후 AuthRepository 를 주입받아 Supabase Auth 로 연결한다:
 *   - 이메일/비밀번호: signInWith(Email)
 *   - Google: compose-auth 의 네이티브 로그인(serverClientId = 웹 클라이언트 ID)
 * 그 전까지 onLoginClick/onGoogleClick 등은 상태만 바꾸는 스텁이다.
 */
@HiltViewModel
class AuthViewModel @Inject constructor() : ViewModel() {

    // 외부에는 StateFlow 하나만 노출한다 (CLAUDE.md §5.3).
    private val _uiState = MutableStateFlow(AuthUiState())
    val uiState: StateFlow<AuthUiState> = _uiState.asStateFlow()

    fun onEmailChange(value: String) {
        _uiState.update { it.copy(email = value, errorMessage = null) }
    }

    fun onPasswordChange(value: String) {
        _uiState.update { it.copy(password = value, errorMessage = null) }
    }

    fun togglePasswordVisibility() {
        _uiState.update { it.copy(passwordVisible = !it.passwordVisible) }
    }

    fun toggleKeepSignedIn() {
        _uiState.update { it.copy(keepSignedIn = !it.keepSignedIn) }
    }

    /** 이메일/비밀번호 로그인. 지금은 검증 후 진행 상태만 세운다. */
    fun onLoginClick() {
        if (!_uiState.value.canSubmit) {
            _uiState.update { it.copy(errorMessage = "이메일과 비밀번호를 입력해주세요.") }
            return
        }
        // TODO(auth): AuthRepository.signInWithEmail(email, password) 연결 후
        //   성공 시 화면 이동, 실패 시 errorMessage 갱신. (Supabase Auth)
        _uiState.update { it.copy(isSubmitting = true, errorMessage = null) }
    }

    /** 소셜 로그인 스텁. 실제 흐름은 Repository/compose-auth 연결 시 채운다. */
    fun onSocialClick() {
        // TODO(auth): Google 네이티브 로그인 등 소셜 인증 연결.
        _uiState.update { it.copy(isSubmitting = true, errorMessage = null) }
    }
}
