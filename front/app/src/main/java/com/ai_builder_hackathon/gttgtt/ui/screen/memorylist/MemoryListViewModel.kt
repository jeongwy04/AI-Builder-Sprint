package com.ai_builder_hackathon.gttgtt.ui.screen.memorylist

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
import com.ai_builder_hackathon.gttgtt.ui.util.toUserMessage

/**
 * 내 추억 / 좋아요한 추억 목록을 로드한다. 종류는 화면에서 [load] 로 한 번 넘겨준다.
 */
@HiltViewModel
class MemoryListViewModel @Inject constructor(
    private val memoryRepository: MemoryRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(MemoryListUiState())
    val uiState: StateFlow<MemoryListUiState> = _uiState.asStateFlow()

    // LaunchedEffect 가 재구성으로 다시 불려도 한 번만 로드한다.
    private var started = false

    fun load(kind: MemoryListKind) {
        if (started) return
        started = true
        _uiState.update { it.copy(isLoading = true, errorMessage = null) }
        viewModelScope.launch {
            val result = when (kind) {
                MemoryListKind.MINE -> memoryRepository.getMyMemories()
                MemoryListKind.LIKED -> memoryRepository.getLikedMemories()
            }
            result
                .onSuccess { list ->
                    _uiState.update { it.copy(isLoading = false, memories = list) }
                }
                .onFailure { throwable ->
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            errorMessage = throwable.toUserMessage("목록을 불러오지 못했습니다."),
                        )
                    }
                }
        }
    }
}
