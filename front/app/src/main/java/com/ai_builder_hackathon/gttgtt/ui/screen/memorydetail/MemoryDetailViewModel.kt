package com.ai_builder_hackathon.gttgtt.ui.screen.memorydetail

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ai_builder_hackathon.gttgtt.domain.repository.MemoryRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class MemoryDetailViewModel @Inject constructor(
    private val memoryRepository: MemoryRepository,
    savedStateHandle: SavedStateHandle,
) : ViewModel() {

    // toRoute() 는 Bundle 을 요구해 JVM 테스트에서 터지므로 키로 읽는다.
    private val memoryId: String = requireNotNull(savedStateHandle["memoryId"]) {
        "memoryId 인자가 없다. Route.MemoryDetail 로 진입했는지 확인할 것."
    }

    private val _uiState = MutableStateFlow(MemoryDetailUiState())
    val uiState: StateFlow<MemoryDetailUiState> = _uiState.asStateFlow()

    init {
        loadDetail()
    }

    fun retry() {
        loadDetail()
    }

    fun onPhotoIndexChange(index: Int) {
        _uiState.update { it.copy(currentPhotoIndex = index) }
    }

    fun onCommentInputChange(value: String) {
        _uiState.update { it.copy(commentInput = value) }
    }

    fun onSubmitComment() {
        val text = _uiState.value.commentInput
        if (text.isBlank() || _uiState.value.isSubmittingComment) return

        // 입력창은 즉시 비우되, 실패하면 되돌려 준다.
        _uiState.update { it.copy(commentInput = "", isSubmittingComment = true) }

        viewModelScope.launch {
            memoryRepository.addComment(memoryId, text)
                .onSuccess { comment ->
                    _uiState.update { state ->
                        val memory = state.memory
                        state.copy(
                            isSubmittingComment = false,
                            memory = memory?.copy(comments = memory.comments + comment),
                        )
                    }
                }
                .onFailure { throwable ->
                    _uiState.update {
                        it.copy(
                            isSubmittingComment = false,
                            commentInput = text,
                            errorMessage = throwable.message ?: "댓글을 등록하지 못했습니다.",
                        )
                    }
                }
        }
    }

    private fun loadDetail() {
        _uiState.update { it.copy(isLoading = true, errorMessage = null) }
        viewModelScope.launch {
            memoryRepository.getDetail(memoryId)
                .onSuccess { memory ->
                    _uiState.update { it.copy(isLoading = false, memory = memory) }
                }
                .onFailure { throwable ->
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            errorMessage = throwable.message ?: "기억을 불러오지 못했습니다.",
                        )
                    }
                }
        }
    }
}
