package com.ai_builder_hackathon.gttgtt.ui.screen.memorycreate

import androidx.lifecycle.SavedStateHandle
import com.ai_builder_hackathon.gttgtt.domain.model.ArchiveSummary
import com.ai_builder_hackathon.gttgtt.domain.model.GradientTheme
import com.ai_builder_hackathon.gttgtt.domain.model.NewMemory
import com.ai_builder_hackathon.gttgtt.domain.repository.ArchiveRepository
import com.ai_builder_hackathon.gttgtt.domain.repository.MemoryRepository
import com.ai_builder_hackathon.gttgtt.domain.repository.PhotoMetadataReader
import io.mockk.coEvery
import io.mockk.mockk
import io.mockk.slot
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
class MemoryCreateViewModelTest {

    private val dispatcher = StandardTestDispatcher()
    private val memoryRepository = mockk<MemoryRepository>()
    private val archiveRepository = mockk<ArchiveRepository>()
    private val photoMetadataReader = mockk<PhotoMetadataReader>()

    @Before
    fun setUp() {
        Dispatchers.setMain(dispatcher)
        coEvery { archiveRepository.getMyArchives() } returns Result.success(listOf(archive))
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun viewModel() = MemoryCreateViewModel(
        memoryRepository,
        archiveRepository,
        photoMetadataReader,
        SavedStateHandle(mapOf("archiveId" to ARCHIVE_ID)),
    )

    @Test
    fun `사진을 고르면 EXIF 촬영일이 추억 날짜로 채워진다`() = runTest(dispatcher) {
        coEvery { photoMetadataReader.readCapturedAtMillis(PHOTO_URI) } returns CAPTURED_AT

        val vm = viewModel()
        dispatcher.scheduler.advanceUntilIdle()

        vm.onPhotosPicked(listOf(PHOTO_URI))
        dispatcher.scheduler.advanceUntilIdle()

        assertEquals(CAPTURED_AT, vm.uiState.value.memoryDateMillis)
        assertEquals(true, vm.uiState.value.isDateFromExif)
    }

    @Test
    fun `EXIF 가 없는 사진이면 날짜를 건드리지 않는다`() = runTest(dispatcher) {
        coEvery { photoMetadataReader.readCapturedAtMillis(PHOTO_URI) } returns null

        val vm = viewModel()
        dispatcher.scheduler.advanceUntilIdle()
        val before = vm.uiState.value.memoryDateMillis

        vm.onPhotosPicked(listOf(PHOTO_URI))
        dispatcher.scheduler.advanceUntilIdle()

        assertEquals(before, vm.uiState.value.memoryDateMillis)
        assertEquals(false, vm.uiState.value.isDateFromExif)
    }

    @Test
    fun `사용자가 고른 날짜는 EXIF 로 덮이지 않는다`() = runTest(dispatcher) {
        coEvery { photoMetadataReader.readCapturedAtMillis(any()) } returns CAPTURED_AT

        val vm = viewModel()
        dispatcher.scheduler.advanceUntilIdle()

        vm.onPhotosPicked(listOf(PHOTO_URI))
        dispatcher.scheduler.advanceUntilIdle()

        vm.onDateChange(MANUAL_DATE)
        // 두 번째 사진을 추가해도 이미 정한 날짜를 유지해야 한다
        vm.onPhotosPicked(listOf("content://photo/2"))
        dispatcher.scheduler.advanceUntilIdle()

        assertEquals(MANUAL_DATE, vm.uiState.value.memoryDateMillis)
        assertEquals(false, vm.uiState.value.isDateFromExif)
    }

    @Test
    fun `사진도 메모도 없으면 저장할 수 없다`() = runTest(dispatcher) {
        val vm = viewModel()
        dispatcher.scheduler.advanceUntilIdle()

        assertEquals(false, vm.uiState.value.canSave)

        vm.onBodyChange("치킨 먹다 울었던 날")
        assertEquals(true, vm.uiState.value.canSave)
    }

    @Test
    fun `제목을 비우면 본문 첫 줄이 제목이 된다`() = runTest(dispatcher) {
        val captured = slot<NewMemory>()
        coEvery { memoryRepository.createMemory(capture(captured)) } returns Result.success("mem-new")

        val vm = viewModel()
        dispatcher.scheduler.advanceUntilIdle()

        vm.onBodyChange("치킨 먹다 울었던 날\n둘째 줄은 제목에 안 들어간다")
        vm.onSaveClick()
        dispatcher.scheduler.advanceUntilIdle()

        assertEquals("치킨 먹다 울었던 날", captured.captured.title)
        assertEquals("mem-new", vm.uiState.value.savedMemoryId)
    }

    @Test
    fun `함께한 사람은 토글로 켜고 끌 수 있다`() = runTest(dispatcher) {
        val vm = viewModel()
        dispatcher.scheduler.advanceUntilIdle()

        vm.onParticipantToggle("u-minji")
        assertTrue("u-minji" in vm.uiState.value.selectedParticipantIds)

        vm.onParticipantToggle("u-minji")
        assertTrue("u-minji" !in vm.uiState.value.selectedParticipantIds)
    }

    @Test
    fun `저장에 실패하면 화면에 머물고 에러를 알린다`() = runTest(dispatcher) {
        coEvery { memoryRepository.createMemory(any()) } returns
            Result.failure(IllegalStateException("업로드 실패"))

        val vm = viewModel()
        dispatcher.scheduler.advanceUntilIdle()

        vm.onBodyChange("기록")
        vm.onSaveClick()
        dispatcher.scheduler.advanceUntilIdle()

        assertEquals(null, vm.uiState.value.savedMemoryId)
        assertEquals("업로드 실패", vm.uiState.value.errorMessage)
        assertEquals(false, vm.uiState.value.isSaving)
    }

    private val archive = ArchiveSummary(
        id = ARCHIVE_ID,
        name = "강릉 여행",
        lastMessagePreview = "",
        lastActivityAtMillis = 0L,
        theme = GradientTheme.BEACH,
        memberIds = listOf("u-minji", "u-hyunwoo"),
        totalMemberCount = 2,
    )

    private companion object {
        const val ARCHIVE_ID = "archive-gangneung"
        const val PHOTO_URI = "content://photo/1"
        const val CAPTURED_AT = 1_766_361_600_000L
        const val MANUAL_DATE = 1_700_000_000_000L
    }
}
