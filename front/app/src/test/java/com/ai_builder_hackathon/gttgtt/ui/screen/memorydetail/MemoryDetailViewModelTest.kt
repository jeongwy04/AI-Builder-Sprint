package com.ai_builder_hackathon.gttgtt.ui.screen.memorydetail

import androidx.lifecycle.SavedStateHandle
import com.ai_builder_hackathon.gttgtt.domain.model.ChatMessage
import com.ai_builder_hackathon.gttgtt.domain.model.Comment
import com.ai_builder_hackathon.gttgtt.domain.model.GradientTheme
import com.ai_builder_hackathon.gttgtt.domain.model.asPhoto
import com.ai_builder_hackathon.gttgtt.domain.model.MemoryDetail
import com.ai_builder_hackathon.gttgtt.domain.model.Participant
import com.ai_builder_hackathon.gttgtt.domain.model.Post
import com.ai_builder_hackathon.gttgtt.domain.model.SharedMemoryPreview
import com.ai_builder_hackathon.gttgtt.domain.repository.ChatRepository
import com.ai_builder_hackathon.gttgtt.domain.repository.MemoryRepository
import com.ai_builder_hackathon.gttgtt.domain.repository.PostRepository
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class MemoryDetailViewModelTest {

    private val dispatcher = StandardTestDispatcher()
    private val repository = mockk<MemoryRepository>()
    private val postRepository = mockk<PostRepository>()
    private val chatRepository = mockk<ChatRepository>()

    @Before
    fun setUp() {
        Dispatchers.setMain(dispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun viewModel(focusComment: Boolean = false) = MemoryDetailViewModel(
        repository,
        postRepository,
        chatRepository,
        SavedStateHandle(mapOf("memoryId" to MEMORY_ID, "focusComment" to focusComment)),
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

    @Test
    fun `더보기를 누르면 메뉴가 열리고 삭제를 누르면 확인창으로 넘어간다`() = runTest(dispatcher) {
        coEvery { repository.getDetail(MEMORY_ID) } returns Result.success(memory)

        val vm = viewModel()
        dispatcher.scheduler.advanceUntilIdle()

        vm.onMoreClick()
        assertEquals(true, vm.uiState.value.isOptionsSheetOpen)

        vm.onDeleteClick()
        assertEquals(false, vm.uiState.value.isOptionsSheetOpen)
        assertEquals(true, vm.uiState.value.isDeleteConfirmOpen)
    }

    @Test
    fun `삭제를 확인하면 성공 시 화면을 나가는 신호가 켜진다`() = runTest(dispatcher) {
        coEvery { repository.getDetail(MEMORY_ID) } returns Result.success(memory)
        coEvery { repository.deleteMemory(MEMORY_ID) } returns Result.success(Unit)

        val vm = viewModel()
        dispatcher.scheduler.advanceUntilIdle()

        vm.onDeleteClick()
        vm.onConfirmDelete()
        dispatcher.scheduler.advanceUntilIdle()

        assertEquals(true, vm.uiState.value.isDeleted)
        assertEquals(false, vm.uiState.value.isDeleting)
    }

    @Test
    fun `삭제에 실패하면 화면에 머물고 에러를 알린다`() = runTest(dispatcher) {
        coEvery { repository.getDetail(MEMORY_ID) } returns Result.success(memory)
        coEvery { repository.deleteMemory(MEMORY_ID) } returns
            Result.failure(IllegalStateException("삭제 실패"))

        val vm = viewModel()
        dispatcher.scheduler.advanceUntilIdle()

        vm.onDeleteClick()
        vm.onConfirmDelete()
        dispatcher.scheduler.advanceUntilIdle()

        assertEquals(false, vm.uiState.value.isDeleted)
        assertEquals("삭제 실패", vm.uiState.value.deleteError)
    }

    @Test
    fun `focusComment 로 진입하면 댓글 자동 포커스 신호가 켜져 있다`() = runTest(dispatcher) {
        coEvery { repository.getDetail(MEMORY_ID) } returns Result.success(memory)

        val vm = viewModel(focusComment = true)
        dispatcher.scheduler.advanceUntilIdle()

        assertEquals(true, vm.uiState.value.shouldFocusCommentInput)
    }

    @Test
    fun `기본 진입에서는 댓글 자동 포커스 신호가 꺼져 있다`() = runTest(dispatcher) {
        coEvery { repository.getDetail(MEMORY_ID) } returns Result.success(memory)

        val vm = viewModel()
        dispatcher.scheduler.advanceUntilIdle()

        assertEquals(false, vm.uiState.value.shouldFocusCommentInput)
    }

    @Test
    fun `댓글 포커스를 처리하면 신호가 꺼진다`() = runTest(dispatcher) {
        coEvery { repository.getDetail(MEMORY_ID) } returns Result.success(memory)

        val vm = viewModel(focusComment = true)
        dispatcher.scheduler.advanceUntilIdle()
        assertEquals(true, vm.uiState.value.shouldFocusCommentInput)

        vm.onCommentFocusHandled()

        assertEquals(false, vm.uiState.value.shouldFocusCommentInput)
    }

    @Test
    fun `좋아요를 누르면 서버 응답 전에 화면이 먼저 바뀐다`() = runTest(dispatcher) {
        coEvery { repository.getDetail(MEMORY_ID) } returns Result.success(memory)
        coEvery { postRepository.toggleLike(MEMORY_ID) } returns
            Result.success(likedPost)

        val vm = viewModel()
        dispatcher.scheduler.advanceUntilIdle()

        vm.onLikeClick()

        // 코루틴을 아직 돌리지 않았는데도 이미 반영돼 있어야 한다 (낙관적 업데이트)
        assertEquals(true, vm.uiState.value.memory?.likedByMe)
        assertEquals(3, vm.uiState.value.memory?.likeCount)
    }

    @Test
    fun `좋아요 실패 시 이전 상태로 되돌린다`() = runTest(dispatcher) {
        coEvery { repository.getDetail(MEMORY_ID) } returns Result.success(memory)
        coEvery { postRepository.toggleLike(MEMORY_ID) } returns
            Result.failure(IllegalStateException("네트워크 오류"))

        val vm = viewModel()
        dispatcher.scheduler.advanceUntilIdle()

        vm.onLikeClick()
        dispatcher.scheduler.advanceUntilIdle()

        assertEquals(false, vm.uiState.value.memory?.likedByMe)
        assertEquals(2, vm.uiState.value.memory?.likeCount)
        assertEquals("네트워크 오류", vm.uiState.value.errorMessage)
    }

    @Test
    fun `채팅방 공유에 성공하면 성공 카운트가 늘고 공유 상태가 풀린다`() = runTest(dispatcher) {
        coEvery { repository.getDetail(MEMORY_ID) } returns Result.success(memory)
        coEvery { chatRepository.sendSharedMemory(memory.archiveId, MEMORY_ID) } returns
            Result.success(sharedChatMessage)

        val vm = viewModel()
        dispatcher.scheduler.advanceUntilIdle()

        vm.onShareClick()
        dispatcher.scheduler.advanceUntilIdle()

        val state = vm.uiState.value
        assertEquals(1, state.shareSuccessCount)
        assertEquals(false, state.isSharing)
        assertNull(state.shareError)
    }

    @Test
    fun `공유 중에는 다시 눌러도 두 번 보내지 않는다`() = runTest(dispatcher) {
        coEvery { repository.getDetail(MEMORY_ID) } returns Result.success(memory)
        coEvery { chatRepository.sendSharedMemory(memory.archiveId, MEMORY_ID) } returns
            Result.success(sharedChatMessage)

        val vm = viewModel()
        dispatcher.scheduler.advanceUntilIdle()

        vm.onShareClick()
        assertEquals(true, vm.uiState.value.isSharing)
        vm.onShareClick()
        dispatcher.scheduler.advanceUntilIdle()

        coVerify(exactly = 1) { chatRepository.sendSharedMemory(memory.archiveId, MEMORY_ID) }
    }

    @Test
    fun `채팅방 공유 실패시 에러를 담는다`() = runTest(dispatcher) {
        coEvery { repository.getDetail(MEMORY_ID) } returns Result.success(memory)
        coEvery { chatRepository.sendSharedMemory(memory.archiveId, MEMORY_ID) } returns
            Result.failure(IllegalStateException("네트워크 오류"))

        val vm = viewModel()
        dispatcher.scheduler.advanceUntilIdle()

        vm.onShareClick()
        dispatcher.scheduler.advanceUntilIdle()

        val state = vm.uiState.value
        assertEquals(0, state.shareSuccessCount)
        assertEquals("네트워크 오류", state.shareError)
        assertEquals(false, state.isSharing)
    }

    @Test
    fun `공유 처리 완료를 알리면 성공 카운트가 0으로 되돌아간다`() = runTest(dispatcher) {
        coEvery { repository.getDetail(MEMORY_ID) } returns Result.success(memory)
        coEvery { chatRepository.sendSharedMemory(memory.archiveId, MEMORY_ID) } returns
            Result.success(sharedChatMessage)

        val vm = viewModel()
        dispatcher.scheduler.advanceUntilIdle()

        vm.onShareClick()
        dispatcher.scheduler.advanceUntilIdle()
        assertEquals(1, vm.uiState.value.shareSuccessCount)

        vm.onShareHandled()

        assertEquals(0, vm.uiState.value.shareSuccessCount)
    }

    private val likedPost = Post(
        id = MEMORY_ID,
        archiveId = "archive-gangneung",
        authorId = "u-minji",
        authorName = "민지",
        memoryDateMillis = 0L,
        photos = listOf(GradientTheme.FOOD.asPhoto()),
        caption = "시험 끝나고 치킨 먹다 울었던 날",
        likeCount = 3,
        commentCount = 1,
        likedByMe = true,
    )

    private val sharedChatMessage = ChatMessage(
        id = "msg-1",
        archiveId = "archive-gangneung",
        senderId = "u-me",
        senderName = "나",
        sentAtMillis = 0L,
        sharedMemory = SharedMemoryPreview(
            memoryId = MEMORY_ID,
            title = "시험 끝나고 치킨 먹다 울었던 날",
            photo = null,
            memoryDateMillis = 0L,
        ),
        isMine = true,
    )

    private val memory = MemoryDetail(
        id = MEMORY_ID,
        archiveId = "archive-gangneung",
        memoryDateMillis = 0L,
        title = "시험 끝나고 치킨 먹다 울었던 날",
        body = "정말 잊지 못할 추억",
        photos = listOf(GradientTheme.FOOD.asPhoto(), GradientTheme.NIGHT.asPhoto(), GradientTheme.BEACH.asPhoto()),
        participants = listOf(Participant("u-me", "나")),
        relatedPhotos = listOf(GradientTheme.SEA.asPhoto()),
        comments = listOf(
            Comment("c1", "u-minji", "민지", "진짜 그때 생각하면 아직도 울컥 😢", 0L, 2)
        ),
        likeCount = 2,
        likedByMe = false,
    )

    private val newComment =
        Comment("c-new", "u-me", "나", "나도 기억나 ㅋㅋ", 1_000L, 0)

    private companion object {
        const val MEMORY_ID = "mem-chicken"
    }
}
