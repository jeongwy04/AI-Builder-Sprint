package com.ai_builder_hackathon.gttgtt.domain.repository

/**
 * 이메일/비밀번호 인증. 구글 로그인은 compose-auth 가 Composable 안에서 직접 트리거해야 해서
 * (AuthViewModel/SignUpViewModel 상단 주석 참고) 이 Repository 를 거치지 않는다.
 */
interface AuthRepository {
    /** 이메일/비밀번호로 로그인한다. 성공하면 세션이 생긴다. */
    suspend fun signInWithEmail(email: String, password: String): Result<Unit>

    /**
     * 이메일/비밀번호로 새 계정을 만든다.
     * ⚠️ Supabase 프로젝트에 이메일 인증(Confirm email)이 켜져 있으면 가입 직후에도
     * 세션이 안 생길 수 있다 — 호출한 쪽에서 [hasActiveSession] 으로 로그인 여부를 다시 확인해야 한다.
     */
    suspend fun signUpWithEmail(email: String, password: String): Result<Unit>

    /**
     * 지금 세션이 있는지 동기로 확인한다 (네트워크 호출 없이 메모리에 캐시된 상태만 읽는다).
     * `signUpWithEmail` 직후 Confirm email 때문에 세션이 없을 수 있는 케이스를 구분하는 데 쓴다.
     */
    fun hasActiveSession(): Boolean
}
