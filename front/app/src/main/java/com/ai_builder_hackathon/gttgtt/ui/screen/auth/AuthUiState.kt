package com.ai_builder_hackathon.gttgtt.ui.screen.auth

/**
 * S0 로그인 화면 상태.
 *
 * 이 화면은 아직 UI 스캐폴딩 단계다. 실제 인증은 Supabase Auth(compose-auth)를
 * 통해 Repository 계층에서 처리하도록 뒤에서 연결한다 (CLAUDE.md §5.3 / §5.4).
 */
data class AuthUiState(
    val email: String = "",
    val password: String = "",
    val passwordVisible: Boolean = false,
    /** 시안 기본값이 체크 상태다. */
    val keepSignedIn: Boolean = true,
    val isSubmitting: Boolean = false,
    val errorMessage: String? = null,
) {
    /** 이메일·비밀번호가 모두 채워지고, 진행 중이 아닐 때만 로그인 가능. */
    val canSubmit: Boolean
        get() = !isSubmitting && email.isNotBlank() && password.isNotBlank()
}
