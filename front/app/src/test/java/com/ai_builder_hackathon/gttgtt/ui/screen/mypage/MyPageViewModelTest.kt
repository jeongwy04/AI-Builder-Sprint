package com.ai_builder_hackathon.gttgtt.ui.screen.mypage

import com.ai_builder_hackathon.gttgtt.domain.model.UserProfile
import com.ai_builder_hackathon.gttgtt.domain.repository.ProfileRepository
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
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class MyPageViewModelTest {

    private val dispatcher = StandardTestDispatcher()
    private val repository = mockk<ProfileRepository>()

    @Before
    fun setUp() {
        Dispatchers.setMain(dispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `프로필을 불러오면 로딩이 끝난다`() = runTest(dispatcher) {
        coEvery { repository.getMyProfile() } returns Result.success(profile)

        val vm = MyPageViewModel(repository)
        dispatcher.scheduler.advanceUntilIdle()

        val state = vm.uiState.value
        assertEquals(false, state.isLoading)
        assertEquals("김그때", state.profile?.name)
        assertEquals(128, state.profile?.memoryCount)
    }

    @Test
    fun `조회 실패 시 프로필은 비고 에러만 남는다`() = runTest(dispatcher) {
        coEvery { repository.getMyProfile() } returns
            Result.failure(IllegalStateException("네트워크 오류"))

        val vm = MyPageViewModel(repository)
        dispatcher.scheduler.advanceUntilIdle()

        assertNull(vm.uiState.value.profile)
        assertEquals("네트워크 오류", vm.uiState.value.errorMessage)
        assertEquals(false, vm.uiState.value.isLoading)
    }

    @Test
    fun `로그아웃에 성공하면 화면을 나갈 신호를 준다`() = runTest(dispatcher) {
        coEvery { repository.getMyProfile() } returns Result.success(profile)
        coEvery { repository.signOut() } returns Result.success(Unit)

        val vm = MyPageViewModel(repository)
        dispatcher.scheduler.advanceUntilIdle()

        vm.onSignOutClick()
        dispatcher.scheduler.advanceUntilIdle()

        assertEquals(true, vm.uiState.value.isSignedOut)
    }

    @Test
    fun `로그아웃에 실패하면 화면에 머문다`() = runTest(dispatcher) {
        coEvery { repository.getMyProfile() } returns Result.success(profile)
        coEvery { repository.signOut() } returns
            Result.failure(IllegalStateException("로그아웃하지 못했습니다."))

        val vm = MyPageViewModel(repository)
        dispatcher.scheduler.advanceUntilIdle()

        vm.onSignOutClick()
        dispatcher.scheduler.advanceUntilIdle()

        assertEquals(false, vm.uiState.value.isSignedOut)
        assertEquals("로그아웃하지 못했습니다.", vm.uiState.value.errorMessage)
    }

    private val profile = UserProfile(
        id = "u-me",
        name = "김그때",
        streakDays = 120,
        statusMessage = "추억을 모으는 중 ✨",
        memoryCount = 128,
        mediaCount = 342,
    )
}
