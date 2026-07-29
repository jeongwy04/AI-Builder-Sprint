package com.ai_builder_hackathon.gttgtt.ui.screen.memorycreate

import androidx.lifecycle.SavedStateHandle
import com.ai_builder_hackathon.gttgtt.domain.model.ArchiveSummary
import com.ai_builder_hackathon.gttgtt.domain.model.GradientTheme
import com.ai_builder_hackathon.gttgtt.domain.model.MemoryDetail
import com.ai_builder_hackathon.gttgtt.domain.model.NewMemory
import com.ai_builder_hackathon.gttgtt.domain.model.Participant
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
        coEvery { archiveRepository.getMemberNames(any()) } returns
            Result.success(mapOf("u-minji" to "민지", "u-hyunwoo" to "현우"))
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun viewModel(memoryId: String? = null) = MemoryCreateViewModel(
        memoryRepository,
        archiveRepository,
        photoMetadataReader,
        SavedStateHandle(
            buildMap {
                put("archiveId", ARCHIVE_ID)
                if (memoryId != null) put("memoryId", memoryId)
            }
        ),
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

    @Test
    fun `memoryId 가 있으면 편집 모드로 기존 값을 불러온다`() = runTest(dispatcher) {
        coEvery { memoryRepository.getDetail(MEMORY_ID) } returns Result.success(existingMemory)

        val vm = viewModel(memoryId = MEMORY_ID)
        dispatcher.scheduler.advanceUntilIdle()

        val state = vm.uiState.value
        assertEquals(true, state.isEditMode)
        assertEquals(existingMemory.body, state.body)
        assertEquals(existingMemory.memoryDateMillis, state.memoryDateMillis)
        assertEquals(setOf("u-minji"), state.selectedParticipantIds)
    }

    @Test
    fun `편집 모드에서 저장하면 createMemory 대신 updateMemory 를 호출한다`() = runTest(dispatcher) {
        coEvery { memoryRepository.getDetail(MEMORY_ID) } returns Result.success(existingMemory)
        coEvery {
            memoryRepository.updateMemory(
                memoryId = MEMORY_ID,
                archiveId = ARCHIVE_ID,
                title = any(),
                body = any(),
                memoryDateMillis = any(),
                participantIds = any(),
            )
        } returns Result.success(Unit)

        val vm = viewModel(memoryId = MEMORY_ID)
        dispatcher.scheduler.advanceUntilIdle()

        vm.onBodyChange("고친 본문")
        vm.onSaveClick()
        dispatcher.scheduler.advanceUntilIdle()

        assertEquals(MEMORY_ID, vm.uiState.value.savedMemoryId)
        io.mockk.coVerify(exactly = 0) { memoryRepository.createMemory(any()) }
    }

    @Test
    fun `제목만 바꿔 저장하면 입력한 그대로 title body 를 넘긴다`() = runTest(dispatcher) {
        // title 을 본문 첫 줄로 접어 넣는 건 Supabase 스키마 제약 때문에 리포지토리(구현체)가 할 일이다.
        // ViewModel 은 그대로 전달만 해야 한다 — 여기서 손대면 편집을 반복할 때 제목 줄이 쌓인다.
        coEvery { memoryRepository.getDetail(MEMORY_ID) } returns Result.success(existingMemory)
        val capturedTitle = slot<String>()
        val capturedBody = slot<String>()
        coEvery {
            memoryRepository.updateMemory(
                memoryId = MEMORY_ID,
                archiveId = ARCHIVE_ID,
                title = capture(capturedTitle),
                body = capture(capturedBody),
                memoryDateMillis = any(),
                participantIds = any(),
            )
        } returns Result.success(Unit)

        val vm = viewModel(memoryId = MEMORY_ID)
        dispatcher.scheduler.advanceUntilIdle()

        assertEquals(existingMemory.body, vm.uiState.value.body)

        vm.onTitleChange("새 제목")
        vm.onSaveClick()
        dispatcher.scheduler.advanceUntilIdle()

        assertEquals("새 제목", capturedTitle.captured)
        assertEquals(existingMemory.body, capturedBody.captured)
    }

    private val existingMemory = MemoryDetail(
        id = MEMORY_ID,
        archiveId = ARCHIVE_ID,
        memoryDateMillis = 1_700_000_000_000L,
        title = "강릉 여행 둘째 날",
        body = "바다 보고 회 먹었다",
        photos = emptyList(),
        participants = listOf(Participant("u-minji", "민지")),
        relatedPhotos = emptyList(),
        comments = emptyList(),
    )

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
        const val MEMORY_ID = "mem-gangneung-2"
        const val PHOTO_URI = "content://photo/1"
        const val CAPTURED_AT = 1_766_361_600_000L
        const val MANUAL_DATE = 1_700_000_000_000L
    }
}
