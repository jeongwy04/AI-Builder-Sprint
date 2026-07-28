package com.ai_builder_hackathon.gttgtt.ui.screen.auth

/**
 * S0 로그인 화면 상태.
 *
 * ⚠️ 지금은 **구글 로그인만** 실제로 붙어 있다. 이메일/비밀번호는 UI만 있고 동작하지 않는다
 * (Supabase Auth 이메일 로그인은 스코프 밖 — 필요해지면 별도로 연결).
 */
data class AuthUiState(
    val email: String = "",
    val password: String = "",
    val passwordVisible: Boolean = false,
    /** 시안 기본값이 체크 상태다. */
    val keepSignedIn: Boolean = true,
    val isSubmitting: Boolean = false,
    val errorMessage: String? = null,
    /** true가 되는 순간 AuthScreen 이 onAuthenticated() 를 한 번 호출한다. */
    val isAuthenticated: Boolean = false,
) {
    /** 이메일·비밀번호가 모두 채워지고, 진행 중이 아닐 때만 로그인 가능. */
    val canSubmit: Boolean
        get() = !isSubmitting && email.isNotBlank() && password.isNotBlank()
}
