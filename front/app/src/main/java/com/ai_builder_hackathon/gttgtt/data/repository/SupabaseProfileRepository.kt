package com.ai_builder_hackathon.gttgtt.data.repository

import com.ai_builder_hackathon.gttgtt.data.dto.IdOnlyDto
import com.ai_builder_hackathon.gttgtt.data.dto.ProfileAvatarPathDto
import com.ai_builder_hackathon.gttgtt.data.dto.ProfileAvatarUpdate
import com.ai_builder_hackathon.gttgtt.data.dto.ProfileDto
import com.ai_builder_hackathon.gttgtt.data.dto.ProfileNicknameUpdate
import com.ai_builder_hackathon.gttgtt.data.remote.MediaUploader
import com.ai_builder_hackathon.gttgtt.domain.model.AvatarUpload
import com.ai_builder_hackathon.gttgtt.domain.model.UserProfile
import com.ai_builder_hackathon.gttgtt.domain.repository.ProfileRepository
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.auth.status.SessionStatus
import io.github.jan.supabase.postgrest.exception.PostgrestRestException
import io.github.jan.supabase.postgrest.postgrest
import io.github.jan.supabase.postgrest.query.Columns
import kotlinx.coroutines.flow.first
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
    private val media: MediaUploader,
) : ProfileRepository {

    /**
     * SupabaseArchiveRepository.awaitSessionReady() 와 같은 이유로 여기도 필요하다: 프로필 사진
     * 변경은 Photo Picker(별도 액티비티)를 띄웠다 돌아오는 흐름이라, 그 사이 OS가 프로세스를
     * 회수했다 복귀 시 새로 만들면 supabase-kt 가 저장된 세션을 비동기로 복원 중일 수 있다
     * (`SessionStatus.Initializing`). 이 상태에서 currentUserOrNull() 을 부르면 실제로는
     * 로그인되어 있는데도 null 이 나와 "로그인이 필요합니다" 오류로 잘못 보인다.
     */
    private suspend fun awaitSessionReady() {
        supabase.auth.sessionStatus.first { it !is SessionStatus.Initializing }
    }

    override suspend fun getMyProfile(): Result<UserProfile> = runCatching {
        awaitSessionReady()
        val user = supabase.auth.currentUserOrNull() ?: error("로그인이 필요합니다.")

        val profile = supabase.postgrest.from("profiles")
            .select(Columns.raw("id,display_name,avatar_url,status")) {
                filter { eq("id", user.id) }
            }
            .decodeSingle<ProfileDto>()

        // profiles.avatar_url 엔 storage path 가 들어있다(signed URL 자체가 아니다) —
        // 조회할 때마다 새로 발급받는다. 실패해도(네트워크 등) 프로필 자체는 보여줘야 하니
        // avatarUrl 만 null 로 남기고(그라디언트 기본 아바타로 대체) 예외를 던지지 않는다.
        val avatarUrl = profile.avatarUrl?.let { path -> media.avatarSignedUrl(path) }

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
            avatarUrl = avatarUrl,
            avatarPath = profile.avatarUrl,
        )
    }

    override suspend fun updateStatus(status: String): Result<Unit> = runCatching {
        awaitSessionReady()
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

        awaitSessionReady()
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

    /**
     * SupabaseArchiveRepository.updateCoverImage() 와 같은 순서: 이전 경로를 먼저 알아두고,
     * 새 사진을 올려 DB를 갱신한 뒤, 이전 오브젝트를 정리한다. 이전 경로 조회가 실패해도
     * (네트워크 등) 갱신 자체를 막을 이유는 없어 조용히 null 로 넘어간다.
     */
    override suspend fun updateAvatar(imageUri: String): Result<AvatarUpload> = runCatching {
        // Photo Picker 를 열었다 돌아온 직후라 특히 여기서 세션 복원이 덜 끝났을 가능성이 높다.
        awaitSessionReady()
        val user = supabase.auth.currentUserOrNull() ?: error("로그인이 필요합니다.")

        val previousPath = runCatching {
            supabase.postgrest.from("profiles")
                .select(Columns.raw("avatar_url")) {
                    filter { eq("id", user.id) }
                }
                .decodeList<ProfileAvatarPathDto>()
                .firstOrNull()
                ?.avatarUrl
        }.getOrNull()

        val uploadedPath = media.uploadAvatar(user.id, imageUri)
            ?: error("사진 업로드에 실패했습니다.")

        // profiles_update_self 정책이 id = auth.uid() 라 본인 행만 바꿀 수 있다.
        supabase.postgrest.from("profiles")
            .update(ProfileAvatarUpdate(avatarUrl = uploadedPath)) {
                filter { eq("id", user.id) }
            }

        if (previousPath != null && previousPath != uploadedPath) {
            media.deleteAvatarObject(previousPath)
        }

        val url = media.avatarSignedUrl(uploadedPath) ?: error("사진을 불러오지 못했습니다.")
        AvatarUpload(url = url, path = uploadedPath)
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
