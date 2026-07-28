package com.ai_builder_hackathon.gttgtt.data.repository

import com.ai_builder_hackathon.gttgtt.domain.model.ArchiveSummary
import com.ai_builder_hackathon.gttgtt.domain.model.GroupTheme
import com.ai_builder_hackathon.gttgtt.domain.repository.ArchiveRepository
import kotlinx.coroutines.delay
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import javax.inject.Inject

/**
 * ⚠️ 임시 구현. Supabase 스키마가 올라가면 SupabaseArchiveRepository 로 교체하고 이 파일은 지운다.
 *
 * 시안대로 화면을 확인하기 위한 것이라 데이터가 하드코딩되어 있다.
 * 로딩 인디케이터가 실제로 보이는지 확인하려고 짧은 지연을 넣었다.
 */
class FakeArchiveRepository @Inject constructor() : ArchiveRepository {

    override suspend fun getMyArchives(): Result<List<ArchiveSummary>> {
        delay(FAKE_NETWORK_DELAY_MILLIS)
        return Result.success(
            listOf(
                ArchiveSummary(
                    id = "archive-gangneung",
                    name = "강릉 여행",
                    lastMessagePreview = "민지: 바다 너무 예뻤어",
                    lastActivityAtMillis = todayAt(20, 45),
                    theme = GroupTheme.BEACH,
                    memberIds = listOf("u-minji", "u-seoyeon", "u-jaehun"),
                    totalMemberCount = 6,
                ),
                ArchiveSummary(
                    id = "archive-univ",
                    name = "우리 대학 동기들",
                    lastMessagePreview = "현우: 다음에 또 보자 ㅋㅋ",
                    lastActivityAtMillis = todayAt(20, 30),
                    theme = GroupTheme.FOREST,
                    memberIds = listOf("u-hyunwoo", "u-doyun"),
                    totalMemberCount = 6,
                ),
                ArchiveSummary(
                    id = "archive-devteam",
                    name = "개발팀",
                    lastMessagePreview = "지훈: PR 리뷰 부탁드려요!",
                    lastActivityAtMillis = todayAt(17, 20),
                    theme = GroupTheme.LAPTOP,
                    memberIds = listOf("u-jihun", "u-sora", "u-taeyang"),
                    totalMemberCount = 6,
                ),
                ArchiveSummary(
                    id = "archive-family",
                    name = "가족",
                    lastMessagePreview = "엄마: 주말에 만나자~",
                    lastActivityAtMillis = todayAt(12, 10),
                    theme = GroupTheme.FAMILY,
                    memberIds = listOf("u-mom", "u-dad"),
                    totalMemberCount = 4,
                ),
            )
        )
    }

    private fun todayAt(hour: Int, minute: Int): Long =
        LocalDate.now()
            .atTime(LocalTime.of(hour, minute))
            .atZone(ZoneId.systemDefault())
            .toInstant()
            .toEpochMilli()

    private companion object {
        const val FAKE_NETWORK_DELAY_MILLIS = 300L
    }
}
