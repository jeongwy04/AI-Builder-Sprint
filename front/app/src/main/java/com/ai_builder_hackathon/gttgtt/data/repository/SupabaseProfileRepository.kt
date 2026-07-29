package com.ai_builder_hackathon.gttgtt.data.repository

import com.ai_builder_hackathon.gttgtt.data.dto.IdOnlyDto
import com.ai_builder_hackathon.gttgtt.data.dto.ProfileDto
import com.ai_builder_hackathon.gttgtt.data.dto.ProfileNicknameUpdate
import com.ai_builder_hackathon.gttgtt.domain.model.UserProfile
import com.ai_builder_hackathon.gttgtt.domain.repository.ProfileRepository
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.postgrest.exception.PostgrestRestException
import io.github.jan.supabase.postgrest.postgrest
import io.github.jan.supabase.postgrest.query.Columns
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
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
            .select(Columns.raw("id,display_name,avatar_url,status")) {
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
            statusMessage = profile.status?.takeIf { it.isNotBlank() } ?: DEFAULT_STATUS,
            memoryCount = myMemoryIds.size,
            mediaCount = mediaCount,
        )
    }

    override suspend fun updateStatus(status: String): Result<Unit> = runCatching {
        val user = supabase.auth.currentUserOrNull() ?: error("로그인이 필요합니다.")
        // 빈 값이면 null 로 저장해 기본 문구로 되돌린다.
        val value = status.trim().ifBlank { null }
        supabase.postgrest.from("profiles")
            .update({ set("status", value) }) {
                filter { eq("id", user.id) }
            }
    }

    override suspend fun signOut(): Result<Unit> = runCatching {
        supabase.auth.signOut()
    }

    override suspend fun updateNickname(nickname: String): Result<Unit> = runCatching {
        val trimmed = nickname.trim()
        require(trimmed.isNotEmpty()) { "닉네임을 입력해주세요." }

        val user = supabase.auth.currentUserOrNull() ?: error("로그인이 필요합니다.")

        try {
            // profiles_update_self 정책이 id = auth.uid() 라 본인 행만 바꿀 수 있다.
            supabase.postgrest.from("profiles")
                .update(ProfileNicknameUpdate(displayName = trimmed)) {
                    filter { eq("id", user.id) }
                }
        } catch (e: PostgrestRestException) {
            // 23505 = unique_violation. profiles_display_name_unique_idx 에 걸린 것 —
            // isNicknameTaken() 사전 확인을 통과했더라도 그 사이에 다른 사람이 먼저 같은
            // 닉네임으로 가입했을 수 있다(경쟁 조건). 이게 최종 방어선이다.
            if (e.code == "23505") {
                throw IllegalStateException("이미 사용 중인 닉네임이에요.")
            }
            throw e
        }
    }

    override suspend fun isNicknameTaken(nickname: String): Result<Boolean> = runCatching {
        val trimmed = nickname.trim()
        if (trimmed.isEmpty()) return@runCatching false

        supabase.postgrest
            .rpc("is_nickname_taken", buildJsonObject { put("p_nickname", trimmed) })
            .decodeAs<Boolean>()
    }

    private companion object {
        const val DEFAULT_STATUS = "추억을 모으는 중 ✨"
    }
}
