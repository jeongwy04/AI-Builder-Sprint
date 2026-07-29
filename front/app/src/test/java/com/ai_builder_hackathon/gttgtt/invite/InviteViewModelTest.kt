package com.ai_builder_hackathon.gttgtt.invite

import com.ai_builder_hackathon.gttgtt.domain.repository.ArchiveRepository
import com.ai_builder_hackathon.gttgtt.domain.repository.AuthRepository
import io.mockk.coEvery
import io.mockk.coVerify
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
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class InviteViewModelTest {

    private val dispatcher = StandardTestDispatcher()
    private val auth = mockk<AuthRepository>()
    private val archive = mockk<ArchiveRepository>()
    private val pendingInvite = PendingInvite()

    @Before
    fun setUp() {
        Dispatchers.setMain(dispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun viewModel() = InviteViewModel(pendingInvite, auth, archive)

    @Test
    fun `로그인된 상태로 초대가 들어오면 자동 참여하고 그룹을 연다`() = runTest(dispatcher) {
        every { auth.hasActiveSession() } returns true
        coEvery { archive.joinArchiveByToken("tok") } returns Result.success("archive-1")

        pendingInvite.push("tok")
        val vm = viewModel()
        dispatcher.scheduler.advanceUntilIdle()

        assertEquals(InviteViewModel.Event.OpenGroup("archive-1"), vm.event.value)
        // 성공한 토큰은 비워져 재처리되지 않는다.
        assertEquals(null, pendingInvite.current())
    }

    @Test
    fun `세션이 없으면 참여하지 않고 로그인을 요구한다`() = runTest(dispatcher) {
        every { auth.hasActiveSession() } returns false

        pendingInvite.push("tok")
        val vm = viewModel()
        dispatcher.scheduler.advanceUntilIdle()

        assertEquals(InviteViewModel.Event.NeedLogin, vm.event.value)
        // 토큰은 로그인 이후 재시도를 위해 보존된다.
        assertEquals("tok", pendingInvite.current())
        coVerify(exactly = 0) { archive.joinArchiveByToken(any()) }
    }

    @Test
    fun `로그인 대기 후 인증되면 그때 자동 참여한다`() = runTest(dispatcher) {
        every { auth.hasActiveSession() } returns false
        pendingInvite.push("tok")
        val vm = viewModel()
        dispatcher.scheduler.advanceUntilIdle()
        assertEquals(InviteViewModel.Event.NeedLogin, vm.event.value)

        // 로그인이 끝나 세션이 생겼다.
        every { auth.hasActiveSession() } returns true
        coEvery { archive.joinArchiveByToken("tok") } returns Result.success("archive-9")

        vm.onAuthenticated()
        dispatcher.scheduler.advanceUntilIdle()

        assertEquals(InviteViewModel.Event.OpenGroup("archive-9"), vm.event.value)
    }

    @Test
    fun `참여에 실패하면 실패 이벤트를 내고 토큰을 비운다`() = runTest(dispatcher) {
        every { auth.hasActiveSession() } returns true
        coEvery { archive.joinArchiveByToken("bad") } returns
            Result.failure(IllegalStateException("초대가 만료됐어요."))

        pendingInvite.push("bad")
        val vm = viewModel()
        dispatcher.scheduler.advanceUntilIdle()

        assertEquals(InviteViewModel.Event.Failed("초대가 만료됐어요."), vm.event.value)
        assertEquals(null, pendingInvite.current())
    }

    @Test
    fun `초대가 없으면 아무 일도 일어나지 않는다`() = runTest(dispatcher) {
        val vm = viewModel()
        dispatcher.scheduler.advanceUntilIdle()

        vm.onAuthenticated()
        dispatcher.scheduler.advanceUntilIdle()

        assertEquals(InviteViewModel.Event.Idle, vm.event.value)
        coVerify(exactly = 0) { archive.joinArchiveByToken(any()) }
    }
}
