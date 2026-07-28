package com.ai_builder_hackathon.gttgtt.data.repository

import com.ai_builder_hackathon.gttgtt.data.dto.ArchiveWithMembersDto
import com.ai_builder_hackathon.gttgtt.data.dto.MessagePreviewDto
import com.ai_builder_hackathon.gttgtt.data.dto.ProfileDto
import com.ai_builder_hackathon.gttgtt.domain.model.ArchiveSummary
import com.ai_builder_hackathon.gttgtt.domain.model.GradientTheme
import com.ai_builder_hackathon.gttgtt.domain.repository.ArchiveRepository
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.postgrest.postgrest
import io.github.jan.supabase.postgrest.query.Columns
import io.github.jan.supabase.postgrest.query.Order
import java.time.OffsetDateTime
import javax.inject.Inject
import kotlin.math.absoluteValue

/**
 * Supabase 실구현. RLS 가 내 그룹만 걸러주므로 앱은 필터를 걸지 않는다.
 *
 * ⚠️ 로그인(세션)이 있어야 동작한다. 세션 없이 호출하면 RLS 가 빈 결과/401 을 돌려준다.
 * RepositoryModule 에서 Fake 와 스위칭한다.
 */
class SupabaseArchiveRepository @Inject constructor(
    private val supabase: SupabaseClient,
) : ArchiveRepository {

    override suspend fun getMyArchives(): Result<List<ArchiveSummary>> = runCatching {
        // RLS: 내가 멤버인 archives 만 내려온다. memberships 는 내장 조인.
        val archives = supabase.postgrest.from("archives")
            .select(Columns.raw("id,name,group_type,created_at,memberships(user_id)"))
            .decodeList<ArchiveWithMembersDto>()

        if (archives.isEmpty()) return@runCatching emptyList()

        // 미리보기 작성자 이름용 프로필을 한 번에 당겨온다.
        val allMemberIds = archives.flatMap { a -> a.memberships.map { it.userId } }.distinct()
        val nameById: Map<String, String> = if (allMemberIds.isEmpty()) {
            emptyMap()
        } else {
            supabase.postgrest.from("profiles")
                .select(Columns.raw("id,display_name,avatar_url")) {
                    filter { isIn("id", allMemberIds) }
                }
                .decodeList<ProfileDto>()
                .associate { it.id to (it.displayName ?: "이름없음") }
        }

        archives.map { archive ->
            val lastMessage = fetchLastMessage(archive.id)
            val preview = lastMessage?.let { msg ->
                val sender = nameById[msg.senderId] ?: "멤버"
                val content = msg.body ?: "사진을 보냈어요"
                "$sender: $content"
            } ?: "아직 대화가 없어요"

            ArchiveSummary(
                id = archive.id,
                name = archive.name,
                lastMessagePreview = preview,
                lastActivityAtMillis = parseMillis(lastMessage?.createdAt ?: archive.createdAt),
                theme = themeFor(archive.groupType, archive.id),
                memberIds = archive.memberships.map { it.userId },
                totalMemberCount = archive.memberships.size,
            )
        }.sortedByDescending { it.lastActivityAtMillis }
    }

    /** 그룹당 1건이라 N번 호출되지만, 소그룹 서비스 특성상 N이 작아 허용한다. */
    private suspend fun fetchLastMessage(archiveId: String): MessagePreviewDto? =
        supabase.postgrest.from("messages")
            .select(Columns.raw("body,image_path,sender_id,created_at")) {
                filter { eq("archive_id", archiveId) }
                order("created_at", Order.DESCENDING)
                limit(1)
            }
            .decodeList<MessagePreviewDto>()
            .firstOrNull()

    private fun parseMillis(iso: String): Long =
        runCatching { OffsetDateTime.parse(iso).toInstant().toEpochMilli() }
            .getOrDefault(0L)

    /**
     * group_type → 썸네일 그라디언트.
     * 값이 없거나 모르는 값이면 id 해시로 고정 배정해 항상 같은 색이 나오게 한다.
     */
    private fun themeFor(groupType: String?, archiveId: String): GradientTheme = when (groupType) {
        "family" -> GradientTheme.FAMILY
        "couple" -> GradientTheme.BEACH
        "friends" -> GradientTheme.FOREST
        "club" -> GradientTheme.LAPTOP
        else -> GradientTheme.entries[archiveId.hashCode().absoluteValue % GradientTheme.entries.size]
    }
}
