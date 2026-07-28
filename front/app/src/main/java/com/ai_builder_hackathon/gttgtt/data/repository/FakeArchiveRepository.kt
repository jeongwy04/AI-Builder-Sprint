package com.ai_builder_hackathon.gttgtt.data.repository

import com.ai_builder_hackathon.gttgtt.domain.model.ArchiveSummary
import com.ai_builder_hackathon.gttgtt.domain.model.GradientTheme
import com.ai_builder_hackathon.gttgtt.domain.model.GroupType
import com.ai_builder_hackathon.gttgtt.domain.repository.ArchiveRepository
import kotlinx.coroutines.delay
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import java.util.UUID
import javax.inject.Inject

/**
 * ⚠️ 임시 구현. Supabase 스키마가 올라가면 SupabaseArchiveRepository 로 교체하고 이 파일은 지운다.
 *
 * 시안대로 화면을 확인하기 위한 것이라 데이터가 하드코딩되어 있다.
 * 로딩 인디케이터가 실제로 보이는지 확인하려고 짧은 지연을 넣었다.
 */
class FakeArchiveRepository @Inject constructor() : ArchiveRepository {

    // 그룹 생성이 실제로 목록에 반영되는지 보려고 메모리에 들고 있는다. 앱 재시작하면 초기화된다.
    private val archives = seedArchives().associateBy { it.id }.toMutableMap()

    override suspend fun getMyArchives(): Result<List<ArchiveSummary>> {
        delay(FAKE_NETWORK_DELAY_MILLIS)
        return Result.success(archives.values.toList())
    }

    override suspend fun createArchive(name: String, groupType: GroupType): Result<ArchiveSummary> {
        delay(FAKE_NETWORK_DELAY_MILLIS)
        val trimmed = name.trim()
        if (trimmed.isEmpty()) {
            return Result.failure(IllegalArgumentException("그룹 이름을 입력해주세요."))
        }

        val created = ArchiveSummary(
            id = "archive-${UUID.randomUUID()}",
            name = trimmed,
            lastMessagePreview = "그룹을 만들었어요! 첫 기억을 남겨보세요.",
            lastActivityAtMillis = System.currentTimeMillis(),
            theme = groupType.toFakeTheme(),
            memberIds = listOf(ME_ID),
            totalMemberCount = 1,
        )
        archives[created.id] = created
        return Result.success(created)
    }

    override suspend fun renameArchive(archiveId: String, newName: String): Result<Unit> {
        delay(FAKE_NETWORK_DELAY_MILLIS)
        val trimmed = newName.trim()
        if (trimmed.isEmpty()) {
            return Result.failure(IllegalArgumentException("그룹 이름을 입력해주세요."))
        }
        val existing = archives[archiveId]
            ?: return Result.failure(NoSuchElementException("그룹을 찾을 수 없습니다."))
        archives[archiveId] = existing.copy(name = trimmed)
        return Result.success(Unit)
    }

    override suspend fun deleteArchive(archiveId: String): Result<Unit> {
        delay(FAKE_NETWORK_DELAY_MILLIS)
        archives.remove(archiveId)
        return Result.success(Unit)
    }

    override suspend fun createInvitation(archiveId: String): Result<String> {
        delay(FAKE_NETWORK_DELAY_MILLIS)
        // 실제 토큰 포맷(32자 hex)까진 흉내 내지 않는다 — 화면에서 보여주고 복사/공유하는 흐름만 확인하면 된다.
        return Result.success(UUID.randomUUID().toString().take(8).uppercase())
    }

    override suspend fun joinArchiveByToken(token: String): Result<String> {
        delay(FAKE_NETWORK_DELAY_MILLIS)
        if (token.isBlank()) {
            return Result.failure(IllegalArgumentException("초대 코드를 입력해주세요."))
        }
        // Fake 는 토큰 유효성을 실제로 검증할 방법이 없다 — 아무 그룹에나 들어간 것처럼 흉내낸다.
        val target = archives.values.firstOrNull()
            ?: return Result.failure(NoSuchElementException("유효하지 않은 코드입니다."))
        return Result.success(target.id)
    }

    /** SupabaseArchiveRepository#themeFor 와 같은 매핑을 쓴다 — 실데이터로 바뀌어도 색이 안 바뀌게. */
    private fun GroupType.toFakeTheme(): GradientTheme = when (this) {
        GroupType.FAMILY -> GradientTheme.FAMILY
        GroupType.COUPLE -> GradientTheme.BEACH
        GroupType.FRIENDS -> GradientTheme.FOREST
        GroupType.CLUB -> GradientTheme.LAPTOP
    }

    private fun seedArchives(): List<ArchiveSummary> =
        listOf(
                ArchiveSummary(
                    id = "archive-gangneung",
                    name = "강릉 여행",
                    lastMessagePreview = "민지: 바다 너무 예뻤어",
                    lastActivityAtMillis = todayAt(20, 45),
                    theme = GradientTheme.BEACH,
                    memberIds = listOf("u-minji", "u-seoyeon", "u-jaehun"),
                    totalMemberCount = 6,
                ),
                ArchiveSummary(
                    id = "archive-univ",
                    name = "우리 대학 동기들",
                    lastMessagePreview = "현우: 다음에 또 보자 ㅋㅋ",
                    lastActivityAtMillis = todayAt(20, 30),
                    theme = GradientTheme.FOREST,
                    memberIds = listOf("u-hyunwoo", "u-doyun"),
                    totalMemberCount = 6,
                ),
                ArchiveSummary(
                    id = "archive-devteam",
                    name = "개발팀",
                    lastMessagePreview = "지훈: PR 리뷰 부탁드려요!",
                    lastActivityAtMillis = todayAt(17, 20),
                    theme = GradientTheme.LAPTOP,
                    memberIds = listOf("u-jihun", "u-sora", "u-taeyang"),
                    totalMemberCount = 6,
                ),
                ArchiveSummary(
                    id = "archive-family",
                    name = "가족",
                    lastMessagePreview = "엄마: 주말에 만나자~",
                    lastActivityAtMillis = todayAt(12, 10),
                    theme = GradientTheme.FAMILY,
                    memberIds = listOf("u-mom", "u-dad"),
                    totalMemberCount = 4,
                ),
            )

    private fun todayAt(hour: Int, minute: Int): Long =
        LocalDate.now()
            .atTime(LocalTime.of(hour, minute))
            .atZone(ZoneId.systemDefault())
            .toInstant()
            .toEpochMilli()

    private companion object {
        const val FAKE_NETWORK_DELAY_MILLIS = 300L

        /** FakeChatRepository 등 다른 Fake 들과 같은 "나" id. */
        const val ME_ID = "u-me"
    }
}
