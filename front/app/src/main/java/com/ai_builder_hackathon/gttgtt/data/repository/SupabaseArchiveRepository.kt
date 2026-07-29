package com.ai_builder_hackathon.gttgtt.data.repository

import com.ai_builder_hackathon.gttgtt.data.dto.ArchiveRenameDto
import com.ai_builder_hackathon.gttgtt.data.dto.ArchiveRowDto
import com.ai_builder_hackathon.gttgtt.data.dto.ArchiveWithMembersDto
import com.ai_builder_hackathon.gttgtt.data.dto.InvitationInsert
import com.ai_builder_hackathon.gttgtt.data.dto.InvitationRowDto
import com.ai_builder_hackathon.gttgtt.data.dto.MessagePreviewDto
import com.ai_builder_hackathon.gttgtt.data.dto.ProfileDto
import com.ai_builder_hackathon.gttgtt.data.dto.UnreadCountDto
import com.ai_builder_hackathon.gttgtt.domain.model.ArchiveSummary
import com.ai_builder_hackathon.gttgtt.domain.model.GradientTheme
import com.ai_builder_hackathon.gttgtt.domain.model.GroupType
import com.ai_builder_hackathon.gttgtt.domain.repository.ArchiveRepository
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.postgrest.postgrest
import io.github.jan.supabase.postgrest.query.Columns
import io.github.jan.supabase.postgrest.query.Order
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
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

    private val json = Json { ignoreUnknownKeys = true }

    override suspend fun getMyArchives(): Result<List<ArchiveSummary>> = runCatching {
        // RLS: 내가 멤버인 archives 만 내려온다. memberships 는 내장 조인.
        val archives = supabase.postgrest.from("archives")
            .select(Columns.raw("id,name,group_type,created_at,memberships(user_id)"))
            .decodeList<ArchiveWithMembersDto>()

        if (archives.isEmpty()) return@runCatching emptyList()

        // 미리보기 작성자 이름용 프로필을 한 번에 당겨온다.
        val allMemberIds = archives.flatMap { a -> a.memberships.map { it.userId } }.distinct()
        val nameById = fetchProfileNames(allMemberIds)
        val unreadByArchiveId = fetchUnreadCounts()

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
                unreadCount = unreadByArchiveId[archive.id] ?: 0,
            )
        }.sortedByDescending { it.lastActivityAtMillis }
    }

    override suspend fun createArchive(name: String, groupType: GroupType): Result<ArchiveSummary> = runCatching {
        val trimmed = name.trim()
        require(trimmed.isNotEmpty()) { "그룹 이름을 입력해주세요." }

        // ⚠️ archives 에 직접 insert 하지 않는다 — RLS readback 함정이 있어 이 RPC 로만 만든다
        // (RPC 가 archives insert + 내 membership insert 를 한 트랜잭션으로 처리한다).
        // 이 버전의 postgrest-kt 는 rpc() 파라미터로 JsonObject 만 받는다 (리파이드 제네릭 오버로드 없음).
        val result = supabase.postgrest.rpc(
            "create_archive",
            buildJsonObject {
                put("p_name", trimmed)
                put("p_group_type", groupType.rawValue)
            },
        )

        // ⚠️ decodeSingle() 은 배열([...])의 첫 원소를 꺼내는 방식이라 여기선 안 맞는다.
        // create_archive 는 `returns public.archives` (setof 아님) 라서 응답이 배열이 아니라
        // 객체({"id":...}) 하나로 그대로 온다. 그래서 raw JSON을 직접 파싱한다.
        val created = json.decodeFromString<ArchiveRowDto>(result.data)

        val myId = supabase.auth.currentUserOrNull()?.id

        ArchiveSummary(
            id = created.id,
            name = created.name,
            lastMessagePreview = "그룹을 만들었어요! 첫 기억을 남겨보세요.",
            lastActivityAtMillis = parseMillis(created.createdAt),
            theme = themeFor(created.groupType, created.id),
            memberIds = listOfNotNull(myId),
            totalMemberCount = 1,
        )
    }

    override suspend fun renameArchive(archiveId: String, newName: String): Result<Unit> = runCatching {
        val trimmed = newName.trim()
        require(trimmed.isNotEmpty()) { "그룹 이름을 입력해주세요." }

        // archives_update_member 정책 = is_member(id) → 멤버 누구나 바꿀 수 있다 (역할 구분 없음).
        supabase.postgrest.from("archives")
            .update(ArchiveRenameDto(name = trimmed)) {
                filter { eq("id", archiveId) }
            }
    }

    override suspend fun deleteArchive(archiveId: String): Result<Unit> = runCatching {
        // memberships/memories/media_assets/notes/... 는 archive_id FK 에 on delete cascade 라
        // archives 한 행만 지우면 하위 데이터가 함께 정리된다 (마이그레이션 §2~8).
        supabase.postgrest.from("archives").delete {
            filter { eq("id", archiveId) }
        }
    }

    override suspend fun createInvitation(archiveId: String): Result<String> = runCatching {
        val row = supabase.postgrest.from("invitations")
            .insert(InvitationInsert(archiveId = archiveId)) {
                select(Columns.raw("token"))
            }
            .decodeSingle<InvitationRowDto>()
        row.token
    }

    override suspend fun joinArchiveByToken(token: String): Result<String> = runCatching {
        val trimmed = token.trim()
        require(trimmed.isNotEmpty()) { "초대 코드를 입력해주세요." }

        val result = supabase.postgrest.rpc(
            "accept_invitation",
            buildJsonObject { put("p_token", trimmed) },
        )

        // accept_invitation() 은 `returns uuid` (테이블 행이 아니라 스칼라 하나) 라서
        // create_archive 때와 같은 이유로 decodeSingle() 을 못 쓴다 — 응답이 배열이 아니라
        // 따옴표로 감싼 문자열 리터럴(예: "550e8400-...") 그대로 온다. raw JSON을 직접 디코드한다.
        json.decodeFromString<String>(result.data)
    }

    override suspend fun getMemberNames(memberIds: List<String>): Result<Map<String, String>> = runCatching {
        fetchProfileNames(memberIds)
    }

    /** id → display_name. profiles 조회 로직 한 곳에 모아 getMyArchives()/getMemberNames() 가 같이 쓴다. */
    private suspend fun fetchProfileNames(memberIds: List<String>): Map<String, String> {
        if (memberIds.isEmpty()) return emptyMap()
        return supabase.postgrest.from("profiles")
            .select(Columns.raw("id,display_name,avatar_url")) {
                filter { isIn("id", memberIds.distinct()) }
            }
            .decodeList<ProfileDto>()
            .associate { it.id to (it.displayName ?: "이름없음") }
    }

    /**
     * archive_id → 안 읽은 메시지 수. `unread_counts()` RPC (호출자 RLS로 이미 내 그룹만 계산됨).
     * 배지 하나 못 그린다고 그룹 목록 전체가 실패하면 안 되니, 실패하면 조용히 빈 맵으로 넘어간다.
     */
    private suspend fun fetchUnreadCounts(): Map<String, Int> = runCatching {
        supabase.postgrest.rpc("unread_counts", buildJsonObject {})
            .decodeList<UnreadCountDto>()
            .associate { it.archiveId to it.unread }
    }.getOrDefault(emptyMap())

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
