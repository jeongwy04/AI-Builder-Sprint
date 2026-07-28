package com.ai_builder_hackathon.gttgtt.ui.screen.groupchat

import androidx.lifecycle.SavedStateHandle
import com.ai_builder_hackathon.gttgtt.domain.model.ArchiveSummary
import com.ai_builder_hackathon.gttgtt.domain.model.ChatListItem
import com.ai_builder_hackathon.gttgtt.domain.model.ChatMessage
import com.ai_builder_hackathon.gttgtt.domain.model.GradientTheme
import com.ai_builder_hackathon.gttgtt.domain.repository.ArchiveRepository
import com.ai_builder_hackathon.gttgtt.domain.repository.ChatRepository
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
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId

@OptIn(ExperimentalCoroutinesApi::class)
class GroupChatViewModelTest {

    private val dispatcher = StandardTestDispatcher()
    private val chatRepository = mockk<ChatRepository>()
    private val archiveRepository = mockk<ArchiveRepository>()

    @Before
    fun setUp() {
        Dispatchers.setMain(dispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun viewModel() = GroupChatViewModel(
        chatRepository,
        archiveRepository,
        SavedStateHandle(mapOf("archiveId" to ARCHIVE_ID)),
    )

    @Test
    fun `날짜가 바뀌는 지점마다 구분선이 하나씩 들어간다`() {
        val items = listOf(
            message("1", at(2025, 12, 22, 18, 31)),
            message("2", at(2025, 12, 22, 18, 33)),
            message("3", at(2025, 12, 23, 9, 10)),
        ).toListItems()

        // 날짜 2개 → 구분선 2개, 메시지 3개
        assertEquals(2, items.count { it is ChatListItem.DateHeader })
        assertEquals(3, items.count { it is ChatListItem.Message })
        // 같은 날 메시지 사이에는 구분선이 끼지 않는다
        assertTrue(items[0] is ChatListItem.DateHeader)
        assertTrue(items[1] is ChatListItem.Message)
        assertTrue(items[2] is ChatListItem.Message)
        assertTrue(items[3] is ChatListItem.DateHeader)
    }

    @Test
    fun `전송하면 입력창이 비고 목록에 추가된다`() = runTest(dispatcher) {
        coEvery { archiveRepository.getMyArchives() } returns Result.success(listOf(archive))
        coEvery { chatRepository.getMessages(ARCHIVE_ID) } returns Result.success(emptyList())
        coEvery { chatRepository.sendMessage(ARCHIVE_ID, "안녕") } returns
            Result.success(message("new", at(2025, 12, 23, 10, 0), text = "안녕", mine = true))

        val vm = viewModel()
        dispatcher.scheduler.advanceUntilIdle()

        vm.onInputChange("안녕")
        assertTrue(vm.uiState.value.canSend)

        vm.onSendClick()
        dispatcher.scheduler.advanceUntilIdle()

        assertEquals("", vm.uiState.value.input)
        assertEquals(1, vm.uiState.value.items.count { it is ChatListItem.Message })
    }

    @Test
    fun `전송 실패 시 입력 내용을 되돌려 준다`() = runTest(dispatcher) {
        coEvery { archiveRepository.getMyArchives() } returns Result.success(listOf(archive))
        coEvery { chatRepository.getMessages(ARCHIVE_ID) } returns Result.success(emptyList())
        coEvery { chatRepository.sendMessage(ARCHIVE_ID, "안녕") } returns
            Result.failure(IllegalStateException("네트워크 오류"))

        val vm = viewModel()
        dispatcher.scheduler.advanceUntilIdle()

        vm.onInputChange("안녕")
        vm.onSendClick()
        dispatcher.scheduler.advanceUntilIdle()

        // 애써 쓴 메시지가 사라지면 안 된다
        assertEquals("안녕", vm.uiState.value.input)
        assertEquals("네트워크 오류", vm.uiState.value.errorMessage)
    }

    @Test
    fun `공백만 있으면 보낼 수 없다`() = runTest(dispatcher) {
        coEvery { archiveRepository.getMyArchives() } returns Result.success(listOf(archive))
        coEvery { chatRepository.getMessages(ARCHIVE_ID) } returns Result.success(emptyList())

        val vm = viewModel()
        dispatcher.scheduler.advanceUntilIdle()

        vm.onInputChange("   ")
        assertEquals(false, vm.uiState.value.canSend)
    }

    private fun message(
        id: String,
        at: Long,
        text: String = "테스트 메시지",
        mine: Boolean = false,
    ) = ChatMessage(
        id = id,
        archiveId = ARCHIVE_ID,
        senderId = if (mine) "u-me" else "u-minji",
        senderName = if (mine) "나" else "민지",
        sentAtMillis = at,
        text = text,
        isMine = mine,
    )

    private fun at(y: Int, m: Int, d: Int, hour: Int, minute: Int): Long =
        LocalDate.of(y, m, d)
            .atTime(LocalTime.of(hour, minute))
            .atZone(ZoneId.systemDefault())
            .toInstant()
            .toEpochMilli()

    private val archive = ArchiveSummary(
        id = ARCHIVE_ID,
        name = "강릉 여행",
        lastMessagePreview = "민지: 바다 너무 예뻤어",
        lastActivityAtMillis = 0L,
        theme = GradientTheme.BEACH,
        memberIds = listOf("u-minji"),
        totalMemberCount = 5,
    )

    private companion object {
        const val ARCHIVE_ID = "archive-gangneung"
    }
}
