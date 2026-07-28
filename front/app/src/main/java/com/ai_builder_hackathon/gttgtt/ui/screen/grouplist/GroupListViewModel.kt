package com.ai_builder_hackathon.gttgtt.ui.screen.grouplist

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ai_builder_hackathon.gttgtt.domain.model.ArchiveSummary
import com.ai_builder_hackathon.gttgtt.domain.repository.ArchiveRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class GroupListViewModel @Inject constructor(
    private val archiveRepository: ArchiveRepository,
) : ViewModel() {

    // 외부에는 StateFlow 하나만 노출한다 (CLAUDE.md §5.3).
    private val _uiState = MutableStateFlow(GroupListUiState())
    val uiState: StateFlow<GroupListUiState> = _uiState.asStateFlow()

    /** 검색어가 바뀌어도 재조회하지 않도록 원본을 들고 있는다. */
    private var allGroups: List<ArchiveSummary> = emptyList()

    init {
        loadGroups()
    }

    fun onQueryChange(query: String) {
        _uiState.update { it.copy(query = query) }
        applyFilter()
    }

    fun retry() {
        loadGroups()
    }

    private fun loadGroups() {
        _uiState.update { it.copy(isLoading = true, errorMessage = null) }
        viewModelScope.launch {
            archiveRepository.getMyArchives()
                .onSuccess { groups ->
                    allGroups = groups
                    _uiState.update { it.copy(isLoading = false) }
                    applyFilter()
                }
                .onFailure { throwable ->
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            errorMessage = throwable.message ?: "목록을 불러오지 못했습니다.",
                        )
                    }
                }
        }
    }

    private fun applyFilter() {
        val query = _uiState.value.query.trim()
        val matched = if (query.isEmpty()) {
            allGroups
        } else {
            // 방 이름과 마지막 메시지 양쪽에서 찾는다 — 사용자는 둘 중 기억나는 쪽으로 검색한다.
            allGroups.filter {
                it.name.contains(query, ignoreCase = true) ||
                    it.lastMessagePreview.contains(query, ignoreCase = true)
            }
        }

        _uiState.update {
            it.copy(groups = matched.sortedByDescending { group -> group.lastActivityAtMillis })
        }
    }
}
