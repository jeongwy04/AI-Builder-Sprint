package com.ai_builder_hackathon.gttgtt.ui.screen.signup

import com.ai_builder_hackathon.gttgtt.domain.repository.AuthRepository
import com.ai_builder_hackathon.gttgtt.domain.repository.ProfileRepository
import io.github.jan.supabase.SupabaseClient
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
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class SignUpViewModelTest {

    private val dispatcher = StandardTestDispatcher()

    /** compose-auth 트리거용으로만 노출되는 값이라, 테스트에선 내용 없이 relaxed mock 이면 충분하다 (AuthViewModelTest 와 동일). */
    private val supabase = mockk<SupabaseClient>(relaxed = true)
    private val profileRepository = mockk<ProfileRepository>()
    private val authRepository = mockk<AuthRepository>()

    @Before
    fun setUp() {
        Dispatchers.setMain(dispatcher)
        // signUpWithEmail 성공 뒤 hasActiveSession() 으로 세션 여부를 확인하므로,
        // 기본값은 "세션 있음"(정상 가입 완료)으로 둔다. Confirm email 케이스는 개별 테스트에서 덮어쓴다.
        every { authRepository.hasActiveSession() } returns true
        // applyNickname() 이 닉네임 저장 성공 직후 항상 호출한다 (가입 직후 로그인 화면으로
        // 돌려보내기 위한 세션 종료). 대부분의 성공 케이스 테스트에서 공통으로 필요해 기본 스텁으로 둔다.
        coEvery { profileRepository.signOut() } returns Result.success(Unit)
        // 이메일 가입/구글 가입 모두 인증 시작 전에 닉네임 중복부터 확인한다.
        // 기본값은 "사용 가능"으로 두고, 중복 케이스는 개별 테스트에서 덮어쓴다.
        coEvery { profileRepository.isNicknameTaken(any()) } returns Result.success(false)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun viewModel() = SignUpViewModel(supabase, profileRepository, authRepository)

    @Test
    fun `초기 상태는 닉네임 빈칸 · 제출 불가`() {
        val vm = viewModel()
        val state = vm.uiState.value
        assertFalse(state.canSubmitGoogle)
        assertFalse(state.canSubmitEmail)
        assertFalse(state.isSubmitting)
        assertFalse(state.isSignedUp)
    }

    @Test
    fun `닉네임을 입력하면 구글 가입은 제출 가능해진다`() {
        val vm = viewModel()

        vm.onNicknameChange("민지")

        assertTrue(vm.uiState.value.canSubmitGoogle)
        assertFalse(vm.uiState.value.canSubmitEmail)
    }

    @Test
    fun `닉네임·이메일·비밀번호를 모두 입력하면 이메일 가입이 제출 가능해진다`() {
        val vm = viewModel()

        vm.onNicknameChange("민지")
        vm.onEmailChange("me@example.com")
        vm.onPasswordChange("secret")

        assertTrue(vm.uiState.value.canSubmitEmail)
    }

    @Test
    fun `공백만 입력하면 제출할 수 없다`() {
        val vm = viewModel()

        vm.onNicknameChange("   ")

        assertFalse(vm.uiState.value.canSubmitGoogle)
    }

    @Test
    fun `구글 흐름을 시작하면 제출 중 상태가 된다`() {
        val vm = viewModel()
        vm.onNicknameChange("민지")

        vm.onGoogleFlowStarted()

        val state = vm.uiState.value
        assertTrue(state.isSubmitting)
        assertNull(state.errorMessage)
    }

    @Test
    fun `구글 인증 성공 후 닉네임 저장까지 성공하면 가입 완료 신호가 켜진다`() = runTest(dispatcher) {
        coEvery { profileRepository.updateNickname("민지") } returns Result.success(Unit)

        val vm = viewModel()
        vm.onNicknameChange("민지")
        vm.onGoogleFlowStarted()

        vm.onGoogleResult(SignUpGoogleOutcome.SUCCESS)
        dispatcher.scheduler.advanceUntilIdle()

        val state = vm.uiState.value
        assertTrue(state.isSignedUp)
        assertFalse(state.isSubmitting)
        assertNull(state.errorMessage)
    }

    @Test
    fun `닉네임 저장에 성공하면 가입 직후 로그인 화면으로 돌려보내려고 세션을 끊는다`() = runTest(dispatcher) {
        coEvery { profileRepository.updateNickname("민지") } returns Result.success(Unit)

        val vm = viewModel()
        vm.onNicknameChange("민지")
        vm.onGoogleFlowStarted()

        vm.onGoogleResult(SignUpGoogleOutcome.SUCCESS)
        dispatcher.scheduler.advanceUntilIdle()

        coVerify(exactly = 1) { profileRepository.signOut() }
    }

    @Test
    fun `구글 인증은 성공했지만 닉네임 저장에 실패하면 화면에 남아 에러를 보여준다`() = runTest(dispatcher) {
        coEvery { profileRepository.updateNickname("민지") } returns
            Result.failure(IllegalStateException("네트워크 오류"))

        val vm = viewModel()
        vm.onNicknameChange("민지")
        vm.onGoogleFlowStarted()

        vm.onGoogleResult(SignUpGoogleOutcome.SUCCESS)
        dispatcher.scheduler.advanceUntilIdle()

        val state = vm.uiState.value
        assertFalse(state.isSignedUp)
        assertFalse(state.isSubmitting)
        assertEquals("네트워크 오류", state.errorMessage)
        // 닉네임 저장이 실패했으면 세션을 끊을 이유가 없다 — 재시도할 수 있어야 하니까.
        coVerify(exactly = 0) { profileRepository.signOut() }
    }

    @Test
    fun `구글 회원가입을 사용자가 닫으면 제출 상태만 풀리고 가입되지 않는다`() {
        val vm = viewModel()
        vm.onNicknameChange("민지")
        vm.onGoogleFlowStarted()

        vm.onGoogleResult(SignUpGoogleOutcome.CANCELLED)

        val state = vm.uiState.value
        assertFalse(state.isSubmitting)
        assertFalse(state.isSignedUp)
    }

    @Test
    fun `구글 회원가입 실패시 에러 메시지를 담는다`() {
        val vm = viewModel()
        vm.onNicknameChange("민지")
        vm.onGoogleFlowStarted()

        vm.onGoogleResult(SignUpGoogleOutcome.ERROR, "네트워크 연결을 확인해주세요.")

        val state = vm.uiState.value
        assertFalse(state.isSubmitting)
        assertFalse(state.isSignedUp)
        assertEquals("네트워크 연결을 확인해주세요.", state.errorMessage)
    }

    @Test
    fun `닉네임을 바꾸면 기존 에러 메시지가 지워진다`() {
        val vm = viewModel()
        vm.onNicknameChange("민지")
        vm.onGoogleFlowStarted()
        vm.onGoogleResult(SignUpGoogleOutcome.ERROR, "실패")
        assertEquals("실패", vm.uiState.value.errorMessage)

        vm.onNicknameChange("민지2")

        assertNull(vm.uiState.value.errorMessage)
    }

    @Test
    fun `필드가 비어 있으면 이메일 가입은 에러 메시지만 세우고 제출하지 않는다`() {
        val vm = viewModel()
        vm.onNicknameChange("민지")

        vm.onEmailSignUpClick()

        val state = vm.uiState.value
        assertFalse(state.isSubmitting)
        assertEquals("닉네임, 이메일, 비밀번호를 모두 입력해주세요.", state.errorMessage)
    }

    @Test
    fun `이메일 가입과 닉네임 저장이 모두 성공하면 가입 완료 신호가 켜진다`() = runTest(dispatcher) {
        coEvery { authRepository.signUpWithEmail("me@example.com", "secret") } returns Result.success(Unit)
        coEvery { profileRepository.updateNickname("민지") } returns Result.success(Unit)

        val vm = viewModel()
        vm.onNicknameChange("민지")
        vm.onEmailChange("me@example.com")
        vm.onPasswordChange("secret")

        vm.onEmailSignUpClick()
        dispatcher.scheduler.advanceUntilIdle()

        val state = vm.uiState.value
        assertTrue(state.isSignedUp)
        assertFalse(state.isSubmitting)
        assertNull(state.errorMessage)
    }

    @Test
    fun `이메일 가입 자체가 실패하면 에러 메시지를 보여준다`() = runTest(dispatcher) {
        coEvery { authRepository.signUpWithEmail("me@example.com", "secret") } returns
            Result.failure(IllegalStateException("이미 가입된 이메일이에요."))

        val vm = viewModel()
        vm.onNicknameChange("민지")
        vm.onEmailChange("me@example.com")
        vm.onPasswordChange("secret")

        vm.onEmailSignUpClick()
        dispatcher.scheduler.advanceUntilIdle()

        val state = vm.uiState.value
        assertFalse(state.isSignedUp)
        assertFalse(state.isSubmitting)
        assertEquals("이미 가입된 이메일이에요.", state.errorMessage)
    }

    @Test
    fun `이메일 인증(Confirm email)이 필요하면 닉네임 저장 없이 안내 메시지만 띄운다`() = runTest(dispatcher) {
        every { authRepository.hasActiveSession() } returns false
        coEvery { authRepository.signUpWithEmail("me@example.com", "secret") } returns Result.success(Unit)

        val vm = viewModel()
        vm.onNicknameChange("민지")
        vm.onEmailChange("me@example.com")
        vm.onPasswordChange("secret")

        vm.onEmailSignUpClick()
        dispatcher.scheduler.advanceUntilIdle()

        val state = vm.uiState.value
        assertFalse(state.isSignedUp)
        assertFalse(state.isSubmitting)
        assertEquals("가입 메일함에서 인증을 완료한 뒤 로그인해주세요.", state.confirmEmailMessage)
    }

    @Test
    fun `닉네임이 이미 사용 중이면 이메일 가입은 인증을 시도하지 않는다`() = runTest(dispatcher) {
        coEvery { profileRepository.isNicknameTaken("민지") } returns Result.success(true)

        val vm = viewModel()
        vm.onNicknameChange("민지")
        vm.onEmailChange("me@example.com")
        vm.onPasswordChange("secret")

        vm.onEmailSignUpClick()
        dispatcher.scheduler.advanceUntilIdle()

        val state = vm.uiState.value
        assertFalse(state.isSignedUp)
        assertFalse(state.isSubmitting)
        assertEquals("이미 사용 중인 닉네임이에요.", state.errorMessage)
        coVerify(exactly = 0) { authRepository.signUpWithEmail(any(), any()) }
    }

    @Test
    fun `닉네임 중복 확인 자체가 실패해도 이메일 가입은 계속 진행한다`() = runTest(dispatcher) {
        coEvery { profileRepository.isNicknameTaken("민지") } returns Result.failure(IllegalStateException("네트워크 오류"))
        coEvery { authRepository.signUpWithEmail("me@example.com", "secret") } returns Result.success(Unit)
        coEvery { profileRepository.updateNickname("민지") } returns Result.success(Unit)

        val vm = viewModel()
        vm.onNicknameChange("민지")
        vm.onEmailChange("me@example.com")
        vm.onPasswordChange("secret")

        vm.onEmailSignUpClick()
        dispatcher.scheduler.advanceUntilIdle()

        assertTrue(vm.uiState.value.isSignedUp)
        coVerify(exactly = 1) { authRepository.signUpWithEmail("me@example.com", "secret") }
    }

    @Test
    fun `canStartGoogleSignUp은 닉네임이 비어 있으면 false를 반환하고 에러를 세운다`() = runTest(dispatcher) {
        val vm = viewModel()

        val result = vm.canStartGoogleSignUp()

        assertFalse(result)
        assertEquals("닉네임을 입력해주세요.", vm.uiState.value.errorMessage)
        coVerify(exactly = 0) { profileRepository.isNicknameTaken(any()) }
    }

    @Test
    fun `canStartGoogleSignUp은 닉네임이 이미 사용 중이면 false를 반환하고 에러를 세운다`() = runTest(dispatcher) {
        coEvery { profileRepository.isNicknameTaken("민지") } returns Result.success(true)

        val vm = viewModel()
        vm.onNicknameChange("민지")

        val result = vm.canStartGoogleSignUp()

        val state = vm.uiState.value
        assertFalse(result)
        assertFalse(state.isSubmitting)
        assertEquals("이미 사용 중인 닉네임이에요.", state.errorMessage)
    }

    @Test
    fun `canStartGoogleSignUp은 닉네임을 쓸 수 있으면 true를 반환하고 제출 중 상태가 된다`() = runTest(dispatcher) {
        val vm = viewModel()
        vm.onNicknameChange("민지")

        val result = vm.canStartGoogleSignUp()

        val state = vm.uiState.value
        assertTrue(result)
        assertTrue(state.isSubmitting)
        assertNull(state.errorMessage)
    }
}
