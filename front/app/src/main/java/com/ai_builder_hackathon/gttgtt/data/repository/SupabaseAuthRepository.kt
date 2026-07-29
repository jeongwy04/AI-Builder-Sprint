package com.ai_builder_hackathon.gttgtt.data.repository

import com.ai_builder_hackathon.gttgtt.domain.repository.AuthRepository
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.auth.exception.AuthErrorCode
import io.github.jan.supabase.auth.exception.AuthRestException
import io.github.jan.supabase.auth.providers.builtin.Email
import javax.inject.Inject

class SupabaseAuthRepository @Inject constructor(
    private val supabase: SupabaseClient,
) : AuthRepository {

    override suspend fun signInWithEmail(email: String, password: String): Result<Unit> = runCatching {
        supabase.auth.signInWith(Email) {
            this.email = email.trim()
            this.password = password
        }
    }

    override suspend fun signUpWithEmail(email: String, password: String): Result<Unit> = runCatching {
        try {
            supabase.auth.signUpWith(Email) {
                this.email = email.trim()
                this.password = password
            }
        } catch (e: AuthRestException) {
            // Confirm email 이 꺼져 있으면(§16 config.toml) 이미 가입된 이메일은 항상 이 에러로
            // 걸러진다 — GoTrue 가 이 경우에만 이메일 존재 여부를 숨기는 안전 경로를 안 탄다.
            if (e.errorCode == AuthErrorCode.UserAlreadyExists) {
                throw IllegalStateException("이미 가입된 이메일이에요.")
            }
            throw e
        }
    }

    override fun hasActiveSession(): Boolean = supabase.auth.currentUserOrNull() != null
}
