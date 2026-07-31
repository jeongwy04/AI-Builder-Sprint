package com.ai_builder_hackathon.gttgtt.data.repository

import com.ai_builder_hackathon.gttgtt.domain.model.AvatarUpload
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

    override suspend fun updateStatus(status: String): Result<Unit> {
        delay(FAKE_NETWORK_DELAY_MILLIS)
        return Result.success(Unit)
    }

    override suspend fun signOut(): Result<Unit> {
        delay(FAKE_NETWORK_DELAY_MILLIS)
        return Result.success(Unit)
    }

    override suspend fun updateNickname(nickname: String): Result<Unit> {
        delay(FAKE_NETWORK_DELAY_MILLIS)
        if (nickname.isBlank()) {
            return Result.failure(IllegalArgumentException("닉네임을 입력해주세요."))
        }
        if (nickname.trim().lowercase() in TAKEN_NICKNAMES) {
            return Result.failure(IllegalStateException("이미 사용 중인 닉네임이에요."))
        }
        return Result.success(Unit)
    }

    override suspend fun isNicknameTaken(nickname: String): Result<Boolean> {
        delay(FAKE_NETWORK_DELAY_MILLIS)
        return Result.success(nickname.trim().lowercase() in TAKEN_NICKNAMES)
    }

    override suspend fun updateAvatar(imageUri: String): Result<AvatarUpload> {
        delay(FAKE_NETWORK_DELAY_MILLIS)
        // 실제 업로드가 없는 Fake 라, 고른 로컬 content:// URI 를 그대로 "signed URL"인 척 돌려준다.
        // Coil 은 content URI 도 그대로 로드할 수 있어 프리뷰에서도 바로 화면에 뜬다.
        return Result.success(AvatarUpload(url = imageUri, path = imageUri))
    }

    private companion object {
        const val FAKE_NETWORK_DELAY_MILLIS = 300L

        /** 실 데이터가 없는 Fake 라, 중복 방지 UI 를 확인해볼 수 있게 데모용으로 몇 개만 막아둔다. */
        val TAKEN_NICKNAMES = setOf("김그때", "관리자", "admin")
    }
}
