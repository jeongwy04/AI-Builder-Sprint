package com.ai_builder_hackathon.gttgtt.ui.screen.grouplist

import com.ai_builder_hackathon.gttgtt.domain.model.ArchiveSummary
import com.ai_builder_hackathon.gttgtt.domain.model.GradientTheme
import com.ai_builder_hackathon.gttgtt.domain.model.GroupType
import com.ai_builder_hackathon.gttgtt.domain.repository.ArchiveRepository
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
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class GroupListViewModelTest {

    private val dispatcher = StandardTestDispatcher()
    private val repository = mockk<ArchiveRepository>()

    @Before
    fun setUp() {
        Dispatchers.setMain(dispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `목록은 최근 활동순으로 정렬된다`() = runTest(dispatcher) {
        coEvery { repository.getMyArchives() } returns Result.success(
            listOf(olderGroup, newerGroup)
        )

        val viewModel = GroupListViewModel(repository)
        dispatcher.scheduler.advanceUntilIdle()

        val state = viewModel.uiState.value
        assertEquals(listOf("강릉 여행", "가족"), state.groups.map { it.name })
        assertEquals(false, state.isLoading)
    }

    @Test
    fun `검색어는 방 이름과 마지막 메시지 모두에 적용된다`() = runTest(dispatcher) {
        coEvery { repository.getMyArchives() } returns Result.success(
            listOf(olderGroup, newerGroup)
        )
        val viewModel = GroupListViewModel(repository)
        dispatcher.scheduler.advanceUntilIdle()

        // 방 이름으로 검색
        viewModel.onQueryChange("가족")
        assertEquals(listOf("가족"), viewModel.uiState.value.groups.map { it.name })

        // 마지막 메시지 내용으로 검색
        viewModel.onQueryChange("바다")
        assertEquals(listOf("강릉 여행"), viewModel.uiState.value.groups.map { it.name })

        // 없는 검색어면 빈 상태
        viewModel.onQueryChange("존재하지않는방")
        assertTrue(viewModel.uiState.value.isEmpty)
    }

    @Test
    fun `조회 실패 시 에러 메시지를 담고 로딩을 끝낸다`() = runTest(dispatcher) {
        coEvery { repository.getMyArchives() } returns
            Result.failure(IllegalStateException("네트워크 오류"))

        val viewModel = GroupListViewModel(repository)
        dispatcher.scheduler.advanceUntilIdle()

        val state = viewModel.uiState.value
        assertEquals("네트워크 오류", state.errorMessage)
        assertEquals(false, state.isLoading)
    }

    @Test
    fun `이름 없이 만들기를 누르면 에러만 뜨고 요청하지 않는다`() = runTest(dispatcher) {
        coEvery { repository.getMyArchives() } returns Result.success(emptyList())
        val viewModel = GroupListViewModel(repository)
        dispatcher.scheduler.advanceUntilIdle()

        viewModel.onCreateGroupClick()
        viewModel.onConfirmCreateGroup()

        val state = viewModel.uiState.value
        assertEquals("그룹 이름을 입력해주세요.", state.createError)
        assertFalse(state.isCreating)
    }

    @Test
    fun `그룹을 만들면 목록 맨 위에 추가되고 다이얼로그가 닫힌다`() = runTest(dispatcher) {
        coEvery { repository.getMyArchives() } returns Result.success(listOf(olderGroup))
        coEvery { repository.createArchive("개발팀", GroupType.CLUB) } returns Result.success(newGroup)

        val viewModel = GroupListViewModel(repository)
        dispatcher.scheduler.advanceUntilIdle()

        viewModel.onCreateGroupClick()
        viewModel.onCreateNameChange("개발팀")
        viewModel.onCreateGroupTypeSelect(GroupType.CLUB)
        viewModel.onConfirmCreateGroup()
        dispatcher.scheduler.advanceUntilIdle()

        val state = viewModel.uiState.value
        assertFalse(state.isCreateDialogOpen)
        assertFalse(state.isCreating)
        assertEquals(listOf("개발팀", "가족"), state.groups.map { it.name })
    }

    @Test
    fun `그룹 생성이 실패하면 에러 메시지를 담고 다이얼로그는 그대로 열려 있다`() = runTest(dispatcher) {
        coEvery { repository.getMyArchives() } returns Result.success(emptyList())
        coEvery { repository.createArchive(any(), any()) } returns
            Result.failure(IllegalStateException("네트워크 오류"))

        val viewModel = GroupListViewModel(repository)
        dispatcher.scheduler.advanceUntilIdle()

        viewModel.onCreateGroupClick()
        viewModel.onCreateNameChange("개발팀")
        viewModel.onConfirmCreateGroup()
        dispatcher.scheduler.advanceUntilIdle()

        val state = viewModel.uiState.value
        assertTrue(state.isCreateDialogOpen)
        assertFalse(state.isCreating)
        assertEquals("네트워크 오류", state.createError)
    }

    @Test
    fun `취소하면 다이얼로그가 닫힌다`() = runTest(dispatcher) {
        coEvery { repository.getMyArchives() } returns Result.success(emptyList())
        val viewModel = GroupListViewModel(repository)
        dispatcher.scheduler.advanceUntilIdle()

        viewModel.onCreateGroupClick()
        assertTrue(viewModel.uiState.value.isCreateDialogOpen)

        viewModel.onDismissCreateDialog()
        assertFalse(viewModel.uiState.value.isCreateDialogOpen)
    }

    private val newGroup = ArchiveSummary(
        id = "3",
        name = "개발팀",
        lastMessagePreview = "그룹을 만들었어요! 첫 기억을 남겨보세요.",
        lastActivityAtMillis = 3_000L,
        theme = GradientTheme.LAPTOP,
        memberIds = listOf("u-me"),
        totalMemberCount = 1,
    )

    private val newerGroup = ArchiveSummary(
        id = "1",
        name = "강릉 여행",
        lastMessagePreview = "민지: 바다 너무 예뻤어",
        lastActivityAtMillis = 2_000L,
        theme = GradientTheme.BEACH,
        memberIds = listOf("u-minji"),
        totalMemberCount = 6,
    )

    private val olderGroup = ArchiveSummary(
        id = "2",
        name = "가족",
        lastMessagePreview = "엄마: 주말에 만나자~",
        lastActivityAtMillis = 1_000L,
        theme = GradientTheme.FAMILY,
        memberIds = listOf("u-mom"),
        totalMemberCount = 4,
    )
}
