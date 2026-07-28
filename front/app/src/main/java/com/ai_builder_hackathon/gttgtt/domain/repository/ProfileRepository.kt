package com.ai_builder_hackathon.gttgtt.domain.repository

import com.ai_builder_hackathon.gttgtt.domain.model.UserProfile

interface ProfileRepository {
    suspend fun getMyProfile(): Result<UserProfile>

    suspend fun signOut(): Result<Unit>
}
