package com.ai_builder_hackathon.gttgtt.ui.screen.chat

import androidx.lifecycle.SavedStateHandle
import com.ai_builder_hackathon.gttgtt.domain.model.AiMessage
import com.ai_builder_hackathon.gttgtt.domain.model.GradientTheme
import com.ai_builder_hackathon.gttgtt.domain.model.MemoryHit
import com.ai_builder_hackathon.gttgtt.domain.repository.AiChatRepository
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class AiChatViewModelTest {

    private val dispatcher = StandardTestDispatcher()
    private val repository = mockk<AiChatRepository>()

    @Before
    fun setUp() {
        Dispatchers.setMain(dispatcher)
        every { repository.greeting() } returns greeting
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun viewModel() = AiChatViewModel(
        repository,
        SavedStateHandle(mapOf("archiveId" to ARCHIVE_ID)),
    )

    @Test
    fun `시트를 열면 AI가 먼저 묻는다`() {
        val vm = viewModel()
        assertEquals(listOf("어떤 추억을 찾고 싶으신가요?"), vm.uiState.value.messages.map { it.text })
    }

    @Test
    fun `보내면 내 말풍선이 먼저 붙고 응답을 기다리는 동안 입력이 잠긴다`() = runTest(dispatcher) {
        coEvery { repository.send(ARCHIVE_ID, "작년 겨울 바다") } returns Result.success(answer)

        val vm = viewModel()
        vm.onInputChange("작년 겨울 바다")
        vm.onSendClick()

        // 아직 코루틴을 돌리지 않은 시점
        assertEquals("", vm.uiState.value.input)
        assertTrue(vm.uiState.value.isThinking)
        assertEquals(false, vm.uiState.value.canSend)
        assertEquals(2, vm.uiState.value.messages.size)

        dispatcher.scheduler.advanceUntilIdle()

        assertEquals(false, vm.uiState.value.isThinking)
        assertEquals(3, vm.uiState.value.messages.size)
        assertEquals(1, vm.uiState.value.messages.last().results.size)
    }

    @Test
    fun `실패해도 대화를 잃지 않고 에러만 표시한다`() = runTest(dispatcher) {
        coEvery { repository.send(ARCHIVE_ID, "바다") } returns
            Result.failure(IllegalStateException("답변을 가져오지 못했어요."))

        val vm = viewModel()
        vm.onInputChange("바다")
        vm.onSendClick()
        dispatcher.scheduler.advanceUntilIdle()

        // 인사 + 내 메시지는 남아 있어야 한다
        assertEquals(2, vm.uiState.value.messages.size)
        assertEquals("답변을 가져오지 못했어요.", vm.uiState.value.errorMessage)
        assertEquals(false, vm.uiState.value.isThinking)
    }

    @Test
    fun `응답을 기다리는 중에는 중복 전송되지 않는다`() = runTest(dispatcher) {
        coEvery { repository.send(ARCHIVE_ID, any()) } returns Result.success(answer)

        val vm = viewModel()
        vm.onInputChange("바다")
        vm.onSendClick()
        vm.onInputChange("또 보내기")
        vm.onSendClick() // isThinking 이라 무시돼야 한다

        assertEquals(2, vm.uiState.value.messages.size)
    }

    private val greeting = AiMessage("greeting", AiMessage.Role.ASSISTANT, "어떤 추억을 찾고 싶으신가요?")

    private val answer = AiMessage(
        id = "a1",
        role = AiMessage.Role.ASSISTANT,
        text = "이런 기억을 찾았어요.",
        results = listOf(
            MemoryHit("m1", "바다 진짜 예뻤던 강릉 첫날", 0L, GradientTheme.SEA)
        ),
    )

    private companion object {
        const val ARCHIVE_ID = "archive-gangneung"
    }
}
