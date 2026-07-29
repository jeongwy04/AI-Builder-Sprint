package com.ai_builder_hackathon.gttgtt.invite

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 딥링크로 들어온 초대 토큰을 "로그인 경계 너머로" 잠시 보관한다.
 *
 * 왜 필요한가: 비로그인 상태에서 초대 링크를 열면 바로 참여할 수 없다. 로그인 화면으로 보낸 뒤,
 * 로그인이 끝나면 이 토큰으로 자동 참여시켜야 한다. 그 사이 토큰을 잃지 않도록 여기에 담아둔다
 * (CLAUDE.md §15 "초대 링크를 미로그인 상태로 열 때 토큰 유실" 방지).
 *
 * 프로세스 메모리에만 둔다. 딥링크는 앱 프로세스를 살린 채 로그인이 곧바로 이어지므로 충분하다.
 * MainActivity 는 [push] 만 하고, [InviteViewModel] 이 [token] 을 관찰해 처리한다 —
 * 액티비티가 ViewModel 을 직접 알 필요가 없다.
 */
@Singleton
class PendingInvite @Inject constructor() {
    private val _token = MutableStateFlow<String?>(null)
    val token: StateFlow<String?> = _token.asStateFlow()

    fun push(newToken: String) {
        _token.value = newToken
    }

    fun current(): String? = _token.value

    fun clear() {
        _token.value = null
    }
}
