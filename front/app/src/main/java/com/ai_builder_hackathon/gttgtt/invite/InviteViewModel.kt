package com.ai_builder_hackathon.gttgtt.invite

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ai_builder_hackathon.gttgtt.domain.repository.ArchiveRepository
import com.ai_builder_hackathon.gttgtt.domain.repository.AuthRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * 딥링크 초대 처리. NavHost 상단(액티비티 스코프)에 하나만 두고, [event] 를 관찰해 화면을 옮긴다.
 *
 * 흐름:
 *   링크 열림 → [PendingInvite.push] → 여기서 관찰 → 세션 있으면 바로 참여(OpenGroup),
 *   없으면 NeedLogin → 로그인 화면. 로그인이 끝나면 [onAuthenticated] 로 다시 시도해 자동 참여.
 *
 * 규칙 준수: Composable 이 SupabaseClient 를 직접 만지지 않도록(§5.3) 참여는 Repository 로만 한다.
 */
@HiltViewModel
class InviteViewModel @Inject constructor(
    private val pendingInvite: PendingInvite,
    private val authRepository: AuthRepository,
    private val archiveRepository: ArchiveRepository,
) : ViewModel() {

    sealed interface Event {
        data object Idle : Event

        /** 참여 진행 중 — 화면에서 스낵바/로딩을 띄우고 싶을 때 쓴다. */
        data object Joining : Event

        /** 로그인이 필요하다 → 로그인 화면으로. 토큰은 보류 상태로 남는다. */
        data object NeedLogin : Event

        /** 참여 성공 → 이 그룹으로 이동. */
        data class OpenGroup(val archiveId: String) : Event

        /** 참여 실패(만료·잘못된 토큰 등). 이미 로그인은 된 상태라 보통 그룹 목록에서 안내한다. */
        data class Failed(val message: String) : Event
    }

    private val _event = MutableStateFlow<Event>(Event.Idle)
    val event: StateFlow<Event> = _event.asStateFlow()

    init {
        // 딥링크로 토큰이 들어오면(=null→비null) 처리한다.
        viewModelScope.launch {
            pendingInvite.token.collect { token ->
                if (token != null) process()
            }
        }
    }

    /** 로그인 완료 직후 호출. 보류 중인 초대가 있으면 그때 다시 참여를 시도한다. */
    fun onAuthenticated() {
        if (pendingInvite.current() != null) process()
    }

    private fun process() {
        val token = pendingInvite.current() ?: return

        // 세션이 없으면 지금 참여할 수 없다 — 토큰은 그대로 두고 로그인으로 유도한다.
        if (!authRepository.hasActiveSession()) {
            _event.value = Event.NeedLogin
            return
        }

        _event.value = Event.Joining
        viewModelScope.launch {
            archiveRepository.joinArchiveByToken(token)
                .onSuccess { archiveId ->
                    pendingInvite.clear()
                    _event.value = Event.OpenGroup(archiveId)
                }
                .onFailure { throwable ->
                    // 재시도 루프를 막기 위해 실패한 토큰도 비운다.
                    pendingInvite.clear()
                    _event.value = Event.Failed(throwable.message ?: "초대 링크가 유효하지 않아요.")
                }
        }
    }

    /** 화면이 이벤트를 처리한 뒤 호출 — 같은 이벤트를 두 번 타지 않도록 Idle 로 되돌린다. */
    fun eventHandled() {
        _event.value = Event.Idle
    }
}
