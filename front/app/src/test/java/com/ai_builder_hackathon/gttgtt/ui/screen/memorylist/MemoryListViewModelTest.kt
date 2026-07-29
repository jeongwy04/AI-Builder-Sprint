package com.ai_builder_hackathon.gttgtt.ui.screen.memorylist

import com.ai_builder_hackathon.gttgtt.domain.model.MemorySummary
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
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class MemoryListViewModelTest {

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

    @Test
    fun `MINE 로드 성공 시 내 기억이 채워진다`() = runTest(dispatcher) {
        coEvery { repository.getMyMemories() } returns Result.success(listOf(sample))

        val vm = MemoryListViewModel(repository)
        vm.load(MemoryListKind.MINE)
        dispatcher.scheduler.advanceUntilIdle()

        val state = vm.uiState.value
        assertEquals(false, state.isLoading)
        assertEquals(listOf("1"), state.memories.map { it.id })
    }

    @Test
    fun `LIKED 로드는 getLikedMemories 를 호출한다`() = runTest(dispatcher) {
        coEvery { repository.getLikedMemories() } returns Result.success(emptyList())

        val vm = MemoryListViewModel(repository)
        vm.load(MemoryListKind.LIKED)
        dispatcher.scheduler.advanceUntilIdle()

        assertTrue(vm.uiState.value.isEmpty)
    }

    @Test
    fun `실패 시 에러 메시지를 담고 로딩을 끝낸다`() = runTest(dispatcher) {
        coEvery { repository.getMyMemories() } returns
            Result.failure(IllegalStateException("네트워크 오류"))

        val vm = MemoryListViewModel(repository)
        vm.load(MemoryListKind.MINE)
        dispatcher.scheduler.advanceUntilIdle()

        val state = vm.uiState.value
        assertEquals("네트워크 오류", state.errorMessage)
        assertEquals(false, state.isLoading)
    }

    private val sample = MemorySummary(
        id = "1",
        archiveId = "a",
        memoryDateMillis = 0L,
        placeName = "강릉 경포해변",
        preview = "경포 바다 진짜 미쳤다",
    )
}
