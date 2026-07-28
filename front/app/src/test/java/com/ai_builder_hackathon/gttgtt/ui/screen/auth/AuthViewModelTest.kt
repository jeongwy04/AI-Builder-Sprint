package com.ai_builder_hackathon.gttgtt.ui.screen.auth

import io.github.jan.supabase.SupabaseClient
import io.mockk.mockk
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class AuthViewModelTest {

    /** compose-auth 트리거용으로만 노출되는 값이라, 테스트에선 내용 없이 relaxed mock 이면 충분하다. */
    private val supabase = mockk<SupabaseClient>(relaxed = true)

    private fun viewModel() = AuthViewModel(supabase)

    @Test
    fun `초기 상태는 로그인 유지 체크 · 제출 불가 · 미인증`() {
        val vm = viewModel()
        val state = vm.uiState.value
        assertTrue(state.keepSignedIn)
        assertFalse(state.canSubmit)
        assertFalse(state.isSubmitting)
        assertFalse(state.isAuthenticated)
    }

    @Test
    fun `이메일과 비밀번호가 모두 채워지면 제출 가능`() {
        val vm = viewModel()

        vm.onEmailChange("me@example.com")
        assertFalse(vm.uiState.value.canSubmit) // 비밀번호 아직 비어 있음

        vm.onPasswordChange("secret")
        assertTrue(vm.uiState.value.canSubmit)
    }

    @Test
    fun `비밀번호 표시 토글이 반전된다`() {
        val vm = viewModel()
        assertFalse(vm.uiState.value.passwordVisible)

        vm.togglePasswordVisibility()
        assertTrue(vm.uiState.value.passwordVisible)

        vm.togglePasswordVisibility()
        assertFalse(vm.uiState.value.passwordVisible)
    }

    @Test
    fun `로그인 유지 토글이 반전된다`() {
        val vm = viewModel()
        assertTrue(vm.uiState.value.keepSignedIn)

        vm.toggleKeepSignedIn()
        assertFalse(vm.uiState.value.keepSignedIn)
    }

    @Test
    fun `빈 입력으로 로그인하면 에러 메시지만 세우고 제출하지 않는다`() {
        val vm = viewModel()

        vm.onLoginClick()

        val state = vm.uiState.value
        assertFalse(state.isSubmitting)
        assertEquals("이메일과 비밀번호를 입력해주세요.", state.errorMessage)
    }

    @Test
    fun `이메일 로그인은 아직 스코프 밖이라 구글 안내만 뜬다`() {
        val vm = viewModel()
        vm.onEmailChange("me@example.com")
        vm.onPasswordChange("secret")

        vm.onLoginClick()

        val state = vm.uiState.value
        // 이메일/비밀번호 로그인은 실제로 붙지 않았다 — 제출 상태로 넘어가면 안 된다.
        assertFalse(state.isSubmitting)
        assertEquals("지금은 구글 로그인만 지원해요. 아래 구글 버튼을 이용해주세요.", state.errorMessage)
    }

    @Test
    fun `입력이 바뀌면 기존 에러 메시지가 지워진다`() {
        val vm = viewModel()
        vm.onLoginClick() // 에러 유발
        assertEquals("이메일과 비밀번호를 입력해주세요.", vm.uiState.value.errorMessage)

        vm.onEmailChange("a")
        assertNull(vm.uiState.value.errorMessage)
    }

    @Test
    fun `구글 외 소셜 버튼은 아직 지원하지 않는다는 안내만 띄운다`() {
        val vm = viewModel()

        vm.onSocialClick("kakao")

        assertEquals("지금은 구글 로그인만 지원해요.", vm.uiState.value.errorMessage)
    }

    @Test
    fun `구글 로그인 흐름을 시작하면 제출 중 상태가 된다`() {
        val vm = viewModel()

        vm.onGoogleFlowStarted()

        val state = vm.uiState.value
        assertTrue(state.isSubmitting)
        assertNull(state.errorMessage)
    }

    @Test
    fun `구글 로그인 성공 결과를 받으면 인증됨으로 바뀐다`() {
        val vm = viewModel()
        vm.onGoogleFlowStarted()

        vm.onGoogleResult(GoogleSignInOutcome.SUCCESS)

        val state = vm.uiState.value
        assertTrue(state.isAuthenticated)
        assertFalse(state.isSubmitting)
        assertNull(state.errorMessage)
    }

    @Test
    fun `구글 로그인을 사용자가 닫으면 제출 상태만 풀리고 인증되지 않는다`() {
        val vm = viewModel()
        vm.onGoogleFlowStarted()

        vm.onGoogleResult(GoogleSignInOutcome.CANCELLED)

        val state = vm.uiState.value
        assertFalse(state.isSubmitting)
        assertFalse(state.isAuthenticated)
    }
}
