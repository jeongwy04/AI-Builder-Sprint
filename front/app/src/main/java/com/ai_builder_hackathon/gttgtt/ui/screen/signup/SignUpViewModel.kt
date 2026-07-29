package com.ai_builder_hackathon.gttgtt.ui.screen.signup

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ai_builder_hackathon.gttgtt.domain.repository.AuthRepository
import com.ai_builder_hackathon.gttgtt.domain.repository.ProfileRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import io.github.jan.supabase.SupabaseClient
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * S0-B 회원가입 ViewModel.
 *
 * 닉네임은 여기서 들고 있다가, 구글 로그인 또는 이메일/비밀번호 가입이 성공한 직후
 * `profiles.display_name` 에 반영한다. 구글 로그인 자체는 이름을 물어보지 않는다 —
 * `handle_new_user()` 트리거가 구글 계정 이름/이메일 앞부분으로 기본값만 채우기 때문에,
 * 사용자가 고른 닉네임으로 다시 덮어써야 한다.
 *
 * [supabase] 를 공개 프로퍼티로 노출하는 건 AuthViewModel 과 같은 이유의 의도적인 예외다
 * (CLAUDE.md §5.3). compose-auth 의 `rememberSignInWithGoogle()` 은 Composable 함수라서
 * ViewModel 안에서 만들 수 없고 반드시 Composition 안에서 호출해야 한다. 이 예외는
 * "구글 로그인 트리거를 만드는 것"에만 한정되고, 이메일/비밀번호 가입은 suspend 함수라
 * [authRepository] 를 거친다.
 */
@HiltViewModel
class SignUpViewModel @Inject constructor(
    val supabase: SupabaseClient,
    private val profileRepository: ProfileRepository,
    private val authRepository: AuthRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(SignUpUiState())
    val uiState: StateFlow<SignUpUiState> = _uiState.asStateFlow()

    fun onNicknameChange(value: String) {
        _uiState.update { it.copy(nickname = value, errorMessage = null) }
    }

    fun onEmailChange(value: String) {
        _uiState.update { it.copy(email = value, errorMessage = null) }
    }

    fun onPasswordChange(value: String) {
        _uiState.update { it.copy(password = value, errorMessage = null) }
    }

    fun togglePasswordVisibility() {
        _uiState.update { it.copy(passwordVisible = !it.passwordVisible) }
    }

    /** SignUpScreen 에서 googleSignInState.startFlow() 를 부르기 직전에 호출한다. */
    fun onGoogleFlowStarted() {
        _uiState.update { it.copy(isSubmitting = true, errorMessage = null) }
    }

    /**
     * compose-auth 의 결과 콜백. AuthViewModel.onGoogleResult 와 같은 이유로
     * `NativeSignInResult` 를 그대로 안 받고 [SignUpGoogleOutcome] 으로 변환해서 받는다.
     */
    fun onGoogleResult(outcome: SignUpGoogleOutcome, message: String? = null) {
        when (outcome) {
            SignUpGoogleOutcome.SUCCESS -> viewModelScope.launch { applyNickname() }

            SignUpGoogleOutcome.CANCELLED -> _uiState.update {
                it.copy(isSubmitting = false)
            }

            SignUpGoogleOutcome.ERROR -> _uiState.update {
                it.copy(isSubmitting = false, errorMessage = message ?: "구글 회원가입에 실패했어요.")
            }
        }
    }

    /** 닉네임·이메일·비밀번호로 가입한다. */
    fun onEmailSignUpClick() {
        val state = _uiState.value
        if (!state.canSubmitEmail) {
            _uiState.update { it.copy(errorMessage = "닉네임, 이메일, 비밀번호를 모두 입력해주세요.") }
            return
        }

        _uiState.update { it.copy(isSubmitting = true, errorMessage = null, confirmEmailMessage = null) }

        viewModelScope.launch {
            authRepository.signUpWithEmail(state.email, state.password)
                .onSuccess {
                    // Confirm email 이 켜져 있으면 가입 직후에도 세션이 안 생길 수 있다 —
                    // 그럴 땐 닉네임을 지금 반영할 수 없으니(로그인 상태 아님) 안내만 띄운다.
                    if (!authRepository.hasActiveSession()) {
                        _uiState.update {
                            it.copy(
                                isSubmitting = false,
                                confirmEmailMessage = "가입 메일함에서 인증을 완료한 뒤 로그인해주세요.",
                            )
                        }
                    } else {
                        applyNickname()
                    }
                }
                .onFailure { throwable ->
                    _uiState.update {
                        it.copy(isSubmitting = false, errorMessage = throwable.message ?: "회원가입에 실패했어요.")
                    }
                }
        }
    }

    /**
     * 인증(구글/이메일) 자체는 이미 끝난 뒤라, 여기서 실패해도 로그인은 유지된다.
     * 실패하면 화면에 남겨서 다시 시도할 수 있게 한다 (재인증 없이 재시도 가능).
     */
    private suspend fun applyNickname() {
        val nickname = _uiState.value.nickname.trim()
        profileRepository.updateNickname(nickname)
            .onSuccess {
                _uiState.update { it.copy(isSubmitting = false, isSignedUp = true) }
            }
            .onFailure { throwable ->
                _uiState.update {
                    it.copy(
                        isSubmitting = false,
                        errorMessage = throwable.message ?: "닉네임을 저장하지 못했어요.",
                    )
                }
            }
    }
}

/** compose-auth 의 `NativeSignInResult` 를 이 화면에 필요한 만큼만 단순화한 결과. */
enum class SignUpGoogleOutcome {
    SUCCESS,
    CANCELLED,
    ERROR,
}
