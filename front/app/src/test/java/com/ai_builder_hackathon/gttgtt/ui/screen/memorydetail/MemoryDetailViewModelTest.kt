package com.ai_builder_hackathon.gttgtt.ui.screen.memorydetail

import androidx.lifecycle.SavedStateHandle
import com.ai_builder_hackathon.gttgtt.domain.model.Comment
import com.ai_builder_hackathon.gttgtt.domain.model.GradientTheme
import com.ai_builder_hackathon.gttgtt.domain.model.MemoryDetail
import com.ai_builder_hackathon.gttgtt.domain.model.Participant
import com.ai_builder_hackathon.gttgtt.domain.repository.MemoryRepository
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
class MemoryDetailViewModelTest {

    private val dispatcher = StandardTestDispatcher()
    private val repository = mockk<MemoryRepository>()

    @Before
    fun setUp() {
        Dispatchers.setMain(dispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun viewModel() = MemoryDetailViewModel(
        repository,
        SavedStateHandle(mapOf("memoryId" to MEMORY_ID)),
    )

    @Test
    fun `상세를 불러오면 사진과 댓글이 채워진다`() = runTest(dispatcher) {
        coEvery { repository.getDetail(MEMORY_ID) } returns Result.success(memory)

        val vm = viewModel()
        dispatcher.scheduler.advanceUntilIdle()

        val state = vm.uiState.value
        assertEquals(false, state.isLoading)
        assertEquals(3, state.memory?.photoCount)
        assertEquals(1, state.memory?.comments?.size)
    }

    @Test
    fun `댓글을 등록하면 목록에 붙고 입력창이 비워진다`() = runTest(dispatcher) {
        coEvery { repository.getDetail(MEMORY_ID) } returns Result.success(memory)
        coEvery { repository.addComment(MEMORY_ID, "나도 기억나 ㅋㅋ") } returns
            Result.success(newComment)

        val vm = viewModel()
        dispatcher.scheduler.advanceUntilIdle()

        vm.onCommentInputChange("나도 기억나 ㅋㅋ")
        assertEquals(true, vm.uiState.value.canSubmitComment)

        vm.onSubmitComment()
        dispatcher.scheduler.advanceUntilIdle()

        assertEquals("", vm.uiState.value.commentInput)
        assertEquals(2, vm.uiState.value.memory?.comments?.size)
    }

    @Test
    fun `댓글 등록에 실패하면 쓰던 내용을 되돌려 준다`() = runTest(dispatcher) {
        coEvery { repository.getDetail(MEMORY_ID) } returns Result.success(memory)
        coEvery { repository.addComment(MEMORY_ID, any()) } returns
            Result.failure(IllegalStateException("네트워크 오류"))

        val vm = viewModel()
        dispatcher.scheduler.advanceUntilIdle()

        vm.onCommentInputChange("나도 기억나 ㅋㅋ")
        vm.onSubmitComment()
        dispatcher.scheduler.advanceUntilIdle()

        assertEquals("나도 기억나 ㅋㅋ", vm.uiState.value.commentInput)
        assertEquals("네트워크 오류", vm.uiState.value.errorMessage)
        assertEquals(1, vm.uiState.value.memory?.comments?.size)
    }

    @Test
    fun `공백만 있으면 등록할 수 없다`() = runTest(dispatcher) {
        coEvery { repository.getDetail(MEMORY_ID) } returns Result.success(memory)

        val vm = viewModel()
        dispatcher.scheduler.advanceUntilIdle()

        vm.onCommentInputChange("   ")
        assertEquals(false, vm.uiState.value.canSubmitComment)
    }

    @Test
    fun `사진을 넘기면 현재 번호가 따라온다`() = runTest(dispatcher) {
        coEvery { repository.getDetail(MEMORY_ID) } returns Result.success(memory)

        val vm = viewModel()
        dispatcher.scheduler.advanceUntilIdle()

        vm.onPhotoIndexChange(2)
        assertEquals(2, vm.uiState.value.currentPhotoIndex)
    }

    private val memory = MemoryDetail(
        id = MEMORY_ID,
        archiveId = "archive-gangneung",
        memoryDateMillis = 0L,
        title = "시험 끝나고 치킨 먹다 울었던 날",
        body = "정말 잊지 못할 추억",
        photos = listOf(GradientTheme.FOOD, GradientTheme.NIGHT, GradientTheme.BEACH),
        participants = listOf(Participant("u-me", "나")),
        relatedPhotos = listOf(GradientTheme.SEA),
        comments = listOf(
            Comment("c1", "u-minji", "민지", "진짜 그때 생각하면 아직도 울컥 😢", 0L, 2)
        ),
    )

    private val newComment =
        Comment("c-new", "u-me", "나", "나도 기억나 ㅋㅋ", 1_000L, 0)

    private companion object {
        const val MEMORY_ID = "mem-chicken"
    }
}
