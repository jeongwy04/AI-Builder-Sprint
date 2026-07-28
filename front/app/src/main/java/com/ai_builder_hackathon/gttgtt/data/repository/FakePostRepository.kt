package com.ai_builder_hackathon.gttgtt.data.repository

import com.ai_builder_hackathon.gttgtt.domain.model.GradientTheme
import com.ai_builder_hackathon.gttgtt.domain.model.Post
import com.ai_builder_hackathon.gttgtt.domain.repository.PostRepository
import kotlinx.coroutines.delay
import java.time.LocalDate
import java.time.ZoneId
import javax.inject.Inject
import javax.inject.Singleton

/**
 * ⚠️ 임시 구현. Supabase 스키마가 올라가면 SupabasePostRepository 로 교체하고 이 파일은 지운다.
 *
 * 좋아요 토글이 화면에서 실제로 반영되는지 보려고 메모리에 상태를 들고 있는다.
 * 앱을 재시작하면 초기화된다.
 */
@Singleton
class FakePostRepository @Inject constructor() : PostRepository {

    private val posts = seedPosts().associateBy { it.id }.toMutableMap()

    override suspend fun getFeed(archiveId: String): Result<List<Post>> {
        delay(FAKE_NETWORK_DELAY_MILLIS)
        val feed = posts.values
            .filter { it.archiveId == archiveId }
            .sortedByDescending { it.memoryDateMillis }
        return Result.success(feed)
    }

    override suspend fun toggleLike(postId: String): Result<Post> {
        val current = posts[postId]
            ?: return Result.failure(NoSuchElementException("게시물을 찾을 수 없습니다."))

        val updated = current.copy(
            likedByMe = !current.likedByMe,
            likeCount = current.likeCount + if (current.likedByMe) -1 else 1,
        )
        posts[postId] = updated
        return Result.success(updated)
    }

    private fun seedPosts(): List<Post> = listOf(
        Post(
            id = "mem-chicken",
            archiveId = DEMO_ARCHIVE_ID,
            authorId = "u-minji",
            authorName = "민지",
            memoryDateMillis = dateOf(2025, 12, 22),
            photos = listOf(GradientTheme.BEACH),
            caption = "시험 끝나고 치킨 먹다 다 같이 울었던 날 🥹",
            likeCount = 12,
            commentCount = 5,
        ),
        Post(
            id = "mem-sea",
            archiveId = DEMO_ARCHIVE_ID,
            authorId = "u-hyunwoo",
            authorName = "현우",
            memoryDateMillis = dateOf(2025, 12, 21),
            photos = listOf(GradientTheme.SEA, GradientTheme.BEACH, GradientTheme.FOREST),
            caption = "바다 진짜 예뻤다! 날씨도 완벽 ☀️",
            likeCount = 8,
            commentCount = 2,
        ),
        Post(
            id = "mem-night",
            archiveId = DEMO_ARCHIVE_ID,
            authorId = "u-jihun",
            authorName = "지훈",
            memoryDateMillis = dateOf(2025, 12, 20),
            photos = listOf(GradientTheme.NIGHT, GradientTheme.FOOD),
            caption = "숙소에서 야식 먹으면서 새벽까지 수다 떨었던 거 기억나?",
            likeCount = 5,
            commentCount = 1,
        ),
    )

    private fun dateOf(year: Int, month: Int, day: Int): Long =
        LocalDate.of(year, month, day)
            .atStartOfDay(ZoneId.systemDefault())
            .toInstant()
            .toEpochMilli()

    private companion object {
        const val FAKE_NETWORK_DELAY_MILLIS = 300L

        /** FakeArchiveRepository 의 강릉 여행 그룹 id 와 맞춘다. */
        const val DEMO_ARCHIVE_ID = "archive-gangneung"
    }
}
