package com.ai_builder_hackathon.gttgtt.ui.screen.chat

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ai_builder_hackathon.gttgtt.domain.model.AiMessage
import com.ai_builder_hackathon.gttgtt.domain.repository.AiChatRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.UUID
import javax.inject.Inject

/**
 * AI 추억 찾기 바텀시트의 상태.
 *
 * 시트는 별도 네비게이션 목적지가 아니라 그룹 피드 위에 뜨는 오버레이라서,
 * 이 ViewModel 은 GroupFeed 목적지에 스코프된다. archiveId 도 거기서 가져온다.
 */
@HiltViewModel
class AiChatViewModel @Inject constructor(
    private val aiChatRepository: AiChatRepository,
    savedStateHandle: SavedStateHandle,
) : ViewModel() {

    // GroupFeed 목적지에 스코프되므로 피드의 archiveId 인자를 그대로 읽는다.
    // toRoute() 는 Bundle 을 요구해 JVM 테스트에서 터지므로 키로 읽는다.
    private val archiveId: String = requireNotNull(savedStateHandle["archiveId"]) {
        "archiveId 인자가 없다. Route.GroupFeed 로 진입했는지 확인할 것."
    }

    private val _uiState = MutableStateFlow(
        // 그룹에 들어오면 AI 가 먼저 묻는다 (CLAUDE.md §6.4).
        AiChatUiState(messages = listOf(aiChatRepository.greeting()))
    )
    val uiState: StateFlow<AiChatUiState> = _uiState.asStateFlow()

    fun onInputChange(value: String) {
        _uiState.update { it.copy(input = value) }
    }

    fun onSendClick() {
        val text = _uiState.value.input.trim()
        if (text.isEmpty() || _uiState.value.isThinking) return

        val userMessage = AiMessage(
            id = UUID.randomUUID().toString(),
            role = AiMessage.Role.USER,
            text = text,
        )

        // 내 말풍선은 바로 붙이고, 답을 기다리는 동안 입력을 잠근다.
        _uiState.update {
            it.copy(
                messages = it.messages + userMessage,
                input = "",
                isThinking = true,
                errorMessage = null,
            )
        }

        viewModelScope.launch {
            aiChatRepository.send(archiveId, text)
                .onSuccess { reply ->
                    _uiState.update {
                        it.copy(messages = it.messages + reply, isThinking = false)
                    }
                }
                .onFailure { throwable ->
                    // 실패해도 빈 화면을 주지 않는다 (CLAUDE.md §9).
                    _uiState.update {
                        it.copy(
                            isThinking = false,
                            errorMessage = throwable.message ?: "답변을 가져오지 못했어요.",
                        )
                    }
                }
        }
    }
}
