package com.ai_builder_hackathon.gttgtt.data.repository

import com.ai_builder_hackathon.gttgtt.domain.model.Comment
import com.ai_builder_hackathon.gttgtt.domain.model.GradientTheme
import com.ai_builder_hackathon.gttgtt.domain.model.MemoryDetail
import com.ai_builder_hackathon.gttgtt.domain.model.Participant
import com.ai_builder_hackathon.gttgtt.domain.repository.MemoryRepository
import kotlinx.coroutines.delay
import java.time.LocalDate
import java.time.ZoneId
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

/**
 * ⚠️ 임시 구현. Supabase 스키마가 올라가면 SupabaseMemoryRepository 로 교체하고 이 파일은 지운다.
 *
 * 댓글이 실제로 달리는지 보려고 메모리에 상태를 들고 있다. 앱을 재시작하면 초기화된다.
 * 기억 id 는 FakePostRepository 와 같은 값을 쓴다 — 피드의 게시물이 곧 기억이기 때문.
 */
@Singleton
class FakeMemoryRepository @Inject constructor() : MemoryRepository {

    private val memories = seed().associateBy { it.id }.toMutableMap()

    override suspend fun getDetail(memoryId: String): Result<MemoryDetail> {
        delay(FAKE_NETWORK_DELAY_MILLIS)
        val memory = memories[memoryId]
            ?: return Result.failure(NoSuchElementException("기억을 찾을 수 없습니다."))
        return Result.success(memory)
    }

    override suspend fun addComment(memoryId: String, text: String): Result<Comment> {
        val trimmed = text.trim()
        if (trimmed.isEmpty()) {
            return Result.failure(IllegalArgumentException("빈 댓글은 등록할 수 없습니다."))
        }
        val memory = memories[memoryId]
            ?: return Result.failure(NoSuchElementException("기억을 찾을 수 없습니다."))

        val comment = Comment(
            id = UUID.randomUUID().toString(),
            authorId = ME_ID,
            authorName = "나",
            text = trimmed,
            createdAtMillis = System.currentTimeMillis(),
        )
        memories[memoryId] = memory.copy(comments = memory.comments + comment)
        return Result.success(comment)
    }

    private fun seed(): List<MemoryDetail> = listOf(
        MemoryDetail(
            id = "mem-chicken",
            archiveId = DEMO_ARCHIVE_ID,
            memoryDateMillis = dateOf(2025, 12, 22),
            title = "시험 끝나고 치킨 먹다 울었던 날",
            body = "치킨 먹다 졸업 얘기 나와서 다 같이 울었던 날. 정말 잊지 못할 추억 ❤️",
            photos = listOf(
                GradientTheme.FOOD,
                GradientTheme.NIGHT,
                GradientTheme.BEACH,
                GradientTheme.SEA,
            ),
            participants = listOf(
                Participant(ME_ID, "나"),
                Participant("u-minji", "민지"),
                Participant("u-hyunwoo", "현우"),
                Participant("u-jihun", "지훈"),
                Participant("u-seoyeon", "소연"),
            ),
            relatedPhotos = listOf(
                GradientTheme.SEA,
                GradientTheme.FOOD,
                GradientTheme.FOREST,
                GradientTheme.NIGHT,
            ),
            comments = listOf(
                Comment(
                    id = "c1",
                    authorId = "u-minji",
                    authorName = "민지",
                    text = "진짜 그때 생각하면 아직도 울컥 😢",
                    createdAtMillis = dateOf(2025, 12, 22),
                    likeCount = 2,
                ),
                Comment(
                    id = "c2",
                    authorId = "u-hyunwoo",
                    authorName = "현우",
                    text = "나만 운 거 아니었네 ㅋㅋㅋ",
                    createdAtMillis = dateOf(2025, 12, 23),
                    likeCount = 1,
                ),
            ),
        ),
        MemoryDetail(
            id = "mem-sea",
            archiveId = DEMO_ARCHIVE_ID,
            memoryDateMillis = dateOf(2025, 12, 21),
            title = "바다 진짜 예뻤던 강릉 첫날",
            body = "날씨도 완벽했고 파도 소리도 좋았다. 다음에 또 가자 ☀️",
            photos = listOf(GradientTheme.SEA, GradientTheme.BEACH, GradientTheme.FOREST),
            participants = listOf(
                Participant(ME_ID, "나"),
                Participant("u-hyunwoo", "현우"),
                Participant("u-minji", "민지"),
            ),
            relatedPhotos = listOf(GradientTheme.BEACH, GradientTheme.NIGHT, GradientTheme.FOOD),
            comments = listOf(
                Comment(
                    id = "c3",
                    authorId = "u-jihun",
                    authorName = "지훈",
                    text = "사진 진짜 잘 나왔다",
                    createdAtMillis = dateOf(2025, 12, 21),
                    likeCount = 3,
                ),
            ),
        ),
        MemoryDetail(
            id = "mem-night",
            archiveId = DEMO_ARCHIVE_ID,
            memoryDateMillis = dateOf(2025, 12, 20),
            title = "숙소에서 야식 먹으며 새벽까지",
            body = "다들 안 잔다고 버티다가 결국 네 시에 잠들었다. 그 수다가 제일 기억에 남아.",
            photos = listOf(GradientTheme.NIGHT, GradientTheme.FOOD),
            participants = listOf(
                Participant(ME_ID, "나"),
                Participant("u-jihun", "지훈"),
                Participant("u-seoyeon", "소연"),
            ),
            relatedPhotos = listOf(GradientTheme.FOOD, GradientTheme.SEA),
            comments = emptyList(),
        ),
    )

    private fun dateOf(y: Int, m: Int, d: Int): Long =
        LocalDate.of(y, m, d).atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()

    private companion object {
        const val FAKE_NETWORK_DELAY_MILLIS = 300L
        const val DEMO_ARCHIVE_ID = "archive-gangneung"
        const val ME_ID = "u-me"
    }
}
