package com.ai_builder_hackathon.gttgtt.data.repository

import com.ai_builder_hackathon.gttgtt.domain.repository.AuthRepository
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.auth.auth
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
        supabase.auth.signUpWith(Email) {
            this.email = email.trim()
            this.password = password
        }
    }

    override fun hasActiveSession(): Boolean = supabase.auth.currentUserOrNull() != null
}
