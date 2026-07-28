package com.ai_builder_hackathon.gttgtt.ui.screen.auth

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class AuthViewModelTest {

    @Test
    fun `초기 상태는 로그인 유지 체크 · 제출 불가`() {
        val vm = AuthViewModel()
        val state = vm.uiState.value
        assertTrue(state.keepSignedIn)
        assertFalse(state.canSubmit)
        assertFalse(state.isSubmitting)
    }

    @Test
    fun `이메일과 비밀번호가 모두 채워지면 제출 가능`() {
        val vm = AuthViewModel()

        vm.onEmailChange("me@example.com")
        assertFalse(vm.uiState.value.canSubmit) // 비밀번호 아직 비어 있음

        vm.onPasswordChange("secret")
        assertTrue(vm.uiState.value.canSubmit)
    }

    @Test
    fun `비밀번호 표시 토글이 반전된다`() {
        val vm = AuthViewModel()
        assertFalse(vm.uiState.value.passwordVisible)

        vm.togglePasswordVisibility()
        assertTrue(vm.uiState.value.passwordVisible)

        vm.togglePasswordVisibility()
        assertFalse(vm.uiState.value.passwordVisible)
    }

    @Test
    fun `로그인 유지 토글이 반전된다`() {
        val vm = AuthViewModel()
        assertTrue(vm.uiState.value.keepSignedIn)

        vm.toggleKeepSignedIn()
        assertFalse(vm.uiState.value.keepSignedIn)
    }

    @Test
    fun `빈 입력으로 로그인하면 에러 메시지만 세우고 제출하지 않는다`() {
        val vm = AuthViewModel()

        vm.onLoginClick()

        val state = vm.uiState.value
        assertFalse(state.isSubmitting)
        assertEquals("이메일과 비밀번호를 입력해주세요.", state.errorMessage)
    }

    @Test
    fun `유효한 입력으로 로그인하면 제출 상태로 전환된다`() {
        val vm = AuthViewModel()
        vm.onEmailChange("me@example.com")
        vm.onPasswordChange("secret")

        vm.onLoginClick()

        val state = vm.uiState.value
        assertTrue(state.isSubmitting)
        assertNull(state.errorMessage)
    }

    @Test
    fun `입력이 바뀌면 기존 에러 메시지가 지워진다`() {
        val vm = AuthViewModel()
        vm.onLoginClick() // 에러 유발
        assertEquals("이메일과 비밀번호를 입력해주세요.", vm.uiState.value.errorMessage)

        vm.onEmailChange("a")
        assertNull(vm.uiState.value.errorMessage)
    }
}
