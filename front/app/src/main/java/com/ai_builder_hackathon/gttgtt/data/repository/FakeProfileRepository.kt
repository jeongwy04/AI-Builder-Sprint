package com.ai_builder_hackathon.gttgtt.data.repository

import com.ai_builder_hackathon.gttgtt.domain.model.UserProfile
import com.ai_builder_hackathon.gttgtt.domain.repository.ProfileRepository
import kotlinx.coroutines.delay
import javax.inject.Inject
import javax.inject.Singleton

/**
 * ⚠️ 임시 구현. Supabase Auth 가 붙으면 SupabaseProfileRepository 로 교체하고 이 파일은 지운다.
 */
@Singleton
class FakeProfileRepository @Inject constructor() : ProfileRepository {

    override suspend fun getMyProfile(): Result<UserProfile> {
        delay(FAKE_NETWORK_DELAY_MILLIS)
        return Result.success(
            UserProfile(
                id = "u-me",
                name = "김그때",
                streakDays = 120,
                statusMessage = "추억을 모으는 중 ✨",
                memoryCount = 128,
                mediaCount = 342,
            )
        )
    }

    override suspend fun signOut(): Result<Unit> {
        delay(FAKE_NETWORK_DELAY_MILLIS)
        return Result.success(Unit)
    }

    private companion object {
        const val FAKE_NETWORK_DELAY_MILLIS = 300L
    }
}
