package com.ai_builder_hackathon.gttgtt.ui.screen.groupchat

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ai_builder_hackathon.gttgtt.domain.model.ChatListItem
import com.ai_builder_hackathon.gttgtt.domain.model.ChatMessage
import com.ai_builder_hackathon.gttgtt.domain.repository.ArchiveRepository
import com.ai_builder_hackathon.gttgtt.domain.repository.ChatRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import javax.inject.Inject

@HiltViewModel
class GroupChatViewModel @Inject constructor(
    private val chatRepository: ChatRepository,
    private val archiveRepository: ArchiveRepository,
    savedStateHandle: SavedStateHandle,
) : ViewModel() {

    // toRoute() 는 Bundle 을 요구해 JVM 테스트에서 터지므로 키로 읽는다.
    private val archiveId: String = requireNotNull(savedStateHandle["archiveId"]) {
        "archiveId 인자가 없다. Route.GroupChat 으로 진입했는지 확인할 것."
    }

    private val _uiState = MutableStateFlow(GroupChatUiState())
    val uiState: StateFlow<GroupChatUiState> = _uiState.asStateFlow()

    private var messages: List<ChatMessage> = emptyList()

    init {
        loadMessages()
    }

    fun onInputChange(value: String) {
        _uiState.update { it.copy(input = value) }
    }

    fun onSendClick() {
        val text = _uiState.value.input
        if (text.isBlank()) return

        // 입력창은 즉시 비운다. 전송이 실패하면 되돌려 준다.
        _uiState.update { it.copy(input = "") }

        viewModelScope.launch {
            chatRepository.sendMessage(archiveId, text)
                .onSuccess { sent ->
                    messages = messages + sent
                    _uiState.update { it.copy(items = messages.toListItems()) }
                }
                .onFailure { throwable ->
                    _uiState.update {
                        it.copy(
                            input = text,
                            errorMessage = throwable.message ?: "메시지를 보내지 못했습니다.",
                        )
                    }
                }
        }
    }

    private fun loadMessages() {
        _uiState.update { it.copy(isLoading = true, errorMessage = null) }
        viewModelScope.launch {
            val group = archiveRepository.getMyArchives()
                .getOrNull()
                ?.firstOrNull { it.id == archiveId }

            chatRepository.getMessages(archiveId)
                .onSuccess { loaded ->
                    messages = loaded
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            groupName = group?.name.orEmpty(),
                            memberCount = group?.totalMemberCount ?: 0,
                            items = loaded.toListItems(),
                        )
                    }
                    // 방을 열었으니 안 읽음 배지 기준(chat_reads)을 갱신한다. 실패해도 채팅 자체는
                    // 이미 보여준 뒤라 화면에 영향 없다 — 다음에 열 때 다시 시도된다.
                    chatRepository.markRead(archiveId)
                }
                .onFailure { throwable ->
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            errorMessage = throwable.message ?: "대화를 불러오지 못했습니다.",
                        )
                    }
                }
        }
    }
}

/**
 * 메시지 목록에 날짜가 바뀌는 지점마다 구분선을 끼워 넣는다.
 * 화면이 아니라 여기서 조립해야 Composable 이 분기 없이 단순해진다.
 */
internal fun List<ChatMessage>.toListItems(): List<ChatListItem> {
    val items = mutableListOf<ChatListItem>()
    var lastDate: LocalDate? = null

    forEach { message ->
        val date = message.sentAtMillis.toLocalDate()
        if (date != lastDate) {
            items += ChatListItem.DateHeader(message.sentAtMillis)
            lastDate = date
        }
        items += ChatListItem.Message(message)
    }
    return items
}

private fun Long.toLocalDate(): LocalDate =
    Instant.ofEpochMilli(this).atZone(ZoneId.systemDefault()).toLocalDate()
