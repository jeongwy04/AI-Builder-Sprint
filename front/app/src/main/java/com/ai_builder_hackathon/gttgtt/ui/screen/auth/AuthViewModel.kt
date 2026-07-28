package com.ai_builder_hackathon.gttgtt.ui.screen.auth

import androidx.lifecycle.ViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import io.github.jan.supabase.SupabaseClient
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import javax.inject.Inject

/**
 * S0 로그인 ViewModel.
 *
 * ⚠️ 지금은 **구글 로그인만** 구현한다. 이메일/비밀번호는 폼만 있고 실제 인증은 안 붙어 있다.
 *
 * [supabase] 를 공개 프로퍼티로 노출하는 건 CLAUDE.md §5.3("Composable에서 SupabaseClient
 * 직접 참조 금지")의 의도적인 예외다. compose-auth 의 `rememberSignInWithGoogle()` 은
 * Composable 함수라서(내부적으로 remember + Credential Manager 를 씀) Repository 나
 * ViewModel 안에서 만들 수 없고 반드시 Composition 안에서 호출해야 한다.
 * 대신 실제 쿼리(Postgrest/Storage/Functions)는 이 화면에서 전혀 하지 않는다 —
 * 이 예외는 "구글 로그인 트리거를 만드는 것"에만 한정된다.
 */
@HiltViewModel
class AuthViewModel @Inject constructor(
    val supabase: SupabaseClient,
) : ViewModel() {

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

    /** 이메일/비밀번호 로그인은 아직 스코프 밖이다. 입력은 받되 안내만 한다. */
    fun onLoginClick() {
        if (!_uiState.value.canSubmit) {
            _uiState.update { it.copy(errorMessage = "이메일과 비밀번호를 입력해주세요.") }
            return
        }
        _uiState.update {
            it.copy(errorMessage = "지금은 구글 로그인만 지원해요. 아래 구글 버튼을 이용해주세요.")
        }
    }

    /** Apple/카카오는 아직 안 붙였다. 구글만 [onGoogleFlowStarted]/[onGoogleResult] 로 처리한다. */
    fun onSocialClick(provider: String) {
        if (provider != "google") {
            _uiState.update { it.copy(errorMessage = "지금은 구글 로그인만 지원해요.") }
        }
    }

    /** AuthScreen 에서 googleSignInState.startFlow() 를 부르기 직전에 호출한다. */
    fun onGoogleFlowStarted() {
        _uiState.update { it.copy(isSubmitting = true, errorMessage = null) }
    }

    /**
     * compose-auth 의 결과 콜백.
     *
     * `NativeSignInResult` (compose-auth 타입)를 그대로 받지 않고 [GoogleSignInOutcome]으로
     * 변환해서 받는다 — AuthScreen 이 `is NativeSignInResult.Success` 처럼 패턴 매칭만 하고
     * 변환해서 넘기므로, ViewModel/테스트는 외부 라이브러리의 내부 데이터 구조를 몰라도 된다.
     */
    fun onGoogleResult(outcome: GoogleSignInOutcome, message: String? = null) {
        when (outcome) {
            GoogleSignInOutcome.SUCCESS -> _uiState.update {
                it.copy(isSubmitting = false, errorMessage = null, isAuthenticated = true)
            }

            GoogleSignInOutcome.CANCELLED -> _uiState.update {
                it.copy(isSubmitting = false)
            }

            GoogleSignInOutcome.ERROR -> _uiState.update {
                it.copy(isSubmitting = false, errorMessage = message ?: "구글 로그인에 실패했어요.")
            }
        }
    }
}

/** compose-auth 의 `NativeSignInResult` 를 이 화면에 필요한 만큼만 단순화한 결과. */
enum class GoogleSignInOutcome {
    SUCCESS,
    CANCELLED,
    ERROR,
}
