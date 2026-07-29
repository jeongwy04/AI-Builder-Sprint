package com.ai_builder_hackathon.gttgtt.data.repository

import com.ai_builder_hackathon.gttgtt.domain.repository.AuthRepository
import kotlinx.coroutines.delay
import javax.inject.Inject

/**
 * ⚠️ 임시 구현. Supabase 이메일 인증 설정이 확정되면 SupabaseAuthRepository 로 교체하고
 * 이 파일은 지운다.
 */
class FakeAuthRepository @Inject constructor() : AuthRepository {

    override suspend fun signInWithEmail(email: String, password: String): Result<Unit> {
        delay(FAKE_NETWORK_DELAY_MILLIS)
        if (email.isBlank() || password.isBlank()) {
            return Result.failure(IllegalArgumentException("이메일과 비밀번호를 입력해주세요."))
        }
        return Result.success(Unit)
    }

    override suspend fun signUpWithEmail(email: String, password: String): Result<Unit> {
        delay(FAKE_NETWORK_DELAY_MILLIS)
        if (email.isBlank() || password.isBlank()) {
            return Result.failure(IllegalArgumentException("이메일과 비밀번호를 입력해주세요."))
        }
        return Result.success(Unit)
    }

    // Confirm email 흐름을 흉내낼 필요가 없는 Fake라 항상 세션이 있다고 가정한다.
    override fun hasActiveSession(): Boolean = true

    private companion object {
        const val FAKE_NETWORK_DELAY_MILLIS = 300L
    }
}
