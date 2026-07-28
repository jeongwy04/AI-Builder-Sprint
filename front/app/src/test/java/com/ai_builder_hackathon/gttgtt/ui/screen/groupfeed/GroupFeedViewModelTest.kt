package com.ai_builder_hackathon.gttgtt.ui.screen.groupfeed

import androidx.lifecycle.SavedStateHandle
import com.ai_builder_hackathon.gttgtt.domain.model.ArchiveSummary
import com.ai_builder_hackathon.gttgtt.domain.model.GradientTheme
import com.ai_builder_hackathon.gttgtt.domain.model.asPhoto
import com.ai_builder_hackathon.gttgtt.domain.model.Post
import com.ai_builder_hackathon.gttgtt.domain.repository.ArchiveRepository
import com.ai_builder_hackathon.gttgtt.domain.repository.PostRepository
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class GroupFeedViewModelTest {

    private val dispatcher = StandardTestDispatcher()
    private val postRepository = mockk<PostRepository>()
    private val archiveRepository = mockk<ArchiveRepository>()

    @Before
    fun setUp() {
        Dispatchers.setMain(dispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    /** SavedStateHandle 에서 archiveId 를 읽으므로 라우트 인자를 심어준다. */
    private fun savedState() = SavedStateHandle(mapOf("archiveId" to ARCHIVE_ID))

    private fun viewModel() = GroupFeedViewModel(postRepository, archiveRepository, savedState())

    @Test
    fun `그룹 이름과 멤버수를 함께 채운다`() = runTest(dispatcher) {
        coEvery { archiveRepository.getMyArchives() } returns Result.success(listOf(archive))
        coEvery { postRepository.getFeed(ARCHIVE_ID) } returns Result.success(listOf(post))

        val vm = viewModel()
        dispatcher.scheduler.advanceUntilIdle()

        val state = vm.uiState.value
        assertEquals("강릉 여행", state.groupName)
        assertEquals(5, state.memberCount)
        assertEquals(1, state.posts.size)
    }

    @Test
    fun `좋아요를 누르면 서버 응답 전에 화면이 먼저 바뀐다`() = runTest(dispatcher) {
        coEvery { archiveRepository.getMyArchives() } returns Result.success(listOf(archive))
        coEvery { postRepository.getFeed(ARCHIVE_ID) } returns Result.success(listOf(post))
        coEvery { postRepository.toggleLike(post.id) } returns
            Result.success(post.copy(likedByMe = true, likeCount = 13))

        val vm = viewModel()
        dispatcher.scheduler.advanceUntilIdle()

        vm.onLikeClick(post.id)

        // 코루틴을 아직 돌리지 않았는데도 이미 반영돼 있어야 한다 (낙관적 업데이트)
        assertEquals(true, vm.uiState.value.posts.first().likedByMe)
        assertEquals(13, vm.uiState.value.posts.first().likeCount)
    }

    @Test
    fun `좋아요 실패 시 이전 상태로 되돌린다`() = runTest(dispatcher) {
        coEvery { archiveRepository.getMyArchives() } returns Result.success(listOf(archive))
        coEvery { postRepository.getFeed(ARCHIVE_ID) } returns Result.success(listOf(post))
        coEvery { postRepository.toggleLike(post.id) } returns
            Result.failure(IllegalStateException("네트워크 오류"))

        val vm = viewModel()
        dispatcher.scheduler.advanceUntilIdle()

        vm.onLikeClick(post.id)
        dispatcher.scheduler.advanceUntilIdle()

        val first = vm.uiState.value.posts.first()
        assertEquals(false, first.likedByMe)
        assertEquals(12, first.likeCount)
        assertEquals("네트워크 오류", vm.uiState.value.errorMessage)
    }

    private val archive = ArchiveSummary(
        id = ARCHIVE_ID,
        name = "강릉 여행",
        lastMessagePreview = "민지: 바다 너무 예뻤어",
        lastActivityAtMillis = 0L,
        theme = GradientTheme.BEACH,
        memberIds = listOf("u-minji"),
        totalMemberCount = 5,
    )

    private val post = Post(
        id = "post-1",
        archiveId = ARCHIVE_ID,
        authorId = "u-minji",
        authorName = "민지",
        memoryDateMillis = 0L,
        photos = listOf(GradientTheme.BEACH.asPhoto()),
        caption = "시험 끝나고 치킨 먹다 다 같이 울었던 날 🥹",
        likeCount = 12,
        commentCount = 5,
    )

    private companion object {
        const val ARCHIVE_ID = "archive-gangneung"
    }
}
