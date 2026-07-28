package com.ai_builder_hackathon.gttgtt.data.repository

import com.ai_builder_hackathon.gttgtt.data.dto.IdOnlyDto
import com.ai_builder_hackathon.gttgtt.data.dto.ProfileDto
import com.ai_builder_hackathon.gttgtt.domain.model.UserProfile
import com.ai_builder_hackathon.gttgtt.domain.repository.ProfileRepository
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.postgrest.postgrest
import io.github.jan.supabase.postgrest.query.Columns
import javax.inject.Inject

/**
 * 마이페이지 프로필. `auth.currentUserOrNull()` 로 로그인 사용자를 확인하고 `profiles` 를 붙인다.
 *
 * ⚠️ [UserProfile.streakDays] 와 [UserProfile.statusMessage] 는 스키마에 대응 테이블/컬럼이 없다.
 * (연속 기록일을 재는 테이블이 없음 — 타임캡슐/알림처럼 스코프 밖 §13으로 미루지는 않았지만
 * 실제로 만들려면 새 테이블이 필요해 백엔드와 상의가 먼저다.) 지금은 고정값을 둔다.
 */
class SupabaseProfileRepository @Inject constructor(
    private val supabase: SupabaseClient,
) : ProfileRepository {

    override suspend fun getMyProfile(): Result<UserProfile> = runCatching {
        val user = supabase.auth.currentUserOrNull() ?: error("로그인이 필요합니다.")

        val profile = supabase.postgrest.from("profiles")
            .select(Columns.raw("id,display_name,avatar_url")) {
                filter { eq("id", user.id) }
            }
            .decodeSingle<ProfileDto>()

        // 내가 작성한 기억 id 목록 — RLS 상 내가 멤버인 archive 것만 내려온다.
        val myMemoryIds = supabase.postgrest.from("memories")
            .select(Columns.raw("id")) {
                filter { eq("author_id", user.id) }
            }
            .decodeList<IdOnlyDto>()
            .map { it.id }

        val mediaCount = if (myMemoryIds.isEmpty()) {
            0
        } else {
            supabase.postgrest.from("media_assets")
                .select(Columns.raw("id")) {
                    filter { isIn("memory_id", myMemoryIds) }
                }
                .decodeList<IdOnlyDto>()
                .size
        }

        UserProfile(
            id = user.id,
            name = profile.displayName ?: "이름 없음",
            // TODO: 연속 기록일 집계 테이블이 생기면 실제 값으로 교체.
            streakDays = 0,
            statusMessage = "추억을 모으는 중 ✨",
            memoryCount = myMemoryIds.size,
            mediaCount = mediaCount,
        )
    }

    override suspend fun signOut(): Result<Unit> = runCatching {
        supabase.auth.signOut()
    }
}
