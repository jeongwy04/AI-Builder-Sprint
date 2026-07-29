package com.ai_builder_hackathon.gttgtt.ui.screen.signup

/**
 * S0-B 회원가입 화면 상태.
 *
 * 닉네임은 항상 받고, 인증은 구글 계정 또는 이메일/비밀번호 중 하나로 한다.
 */
data class SignUpUiState(
    val nickname: String = "",
    val email: String = "",
    val password: String = "",
    val passwordVisible: Boolean = false,
    val isSubmitting: Boolean = false,
    val errorMessage: String? = null,
    /** 가입 직후 이메일 인증(Confirm email)이 필요해 세션이 아직 없을 때 보여줄 안내. */
    val confirmEmailMessage: String? = null,
    /**
     * true가 되는 순간 SignUpScreen 이 onSignedUp() 을 한 번 호출한다.
     * NavHost 는 이때 GroupList 가 아니라 로그인 화면(Route.Auth)으로 보낸다 — 가입 직후
     * 바로 앱에 들어가지 않고 최초 로그인을 직접 하게 하려는 의도다. ViewModel 이 이 시점에
     * 이미 signOut() 까지 끝내둔다.
     */
    val isSignedUp: Boolean = false,
) {
    /** 닉네임이 채워지고, 진행 중이 아닐 때만 구글 가입을 시작할 수 있다. */
    val canSubmitGoogle: Boolean
        get() = !isSubmitting && nickname.trim().isNotEmpty()

    /** 닉네임·이메일·비밀번호가 모두 채워지고, 진행 중이 아닐 때만 이메일 가입을 시작할 수 있다. */
    val canSubmitEmail: Boolean
        get() = !isSubmitting && nickname.trim().isNotEmpty() && email.isNotBlank() && password.isNotBlank()
}
