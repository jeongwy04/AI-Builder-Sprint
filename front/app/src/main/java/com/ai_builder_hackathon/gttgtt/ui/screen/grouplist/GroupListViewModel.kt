package com.ai_builder_hackathon.gttgtt.ui.screen.grouplist

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ai_builder_hackathon.gttgtt.domain.model.ArchiveSummary
import com.ai_builder_hackathon.gttgtt.domain.model.GroupType
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

    /** 우측 하단 + 버튼. */
    fun onCreateGroupClick() {
        _uiState.update {
            it.copy(
                isCreateDialogOpen = true,
                createName = "",
                createGroupType = GroupType.FRIENDS,
                createError = null,
            )
        }
    }

    fun onDismissCreateDialog() {
        // 생성 요청이 나가 있는 동안엔 닫히면 안 된다 (중복 탭 방지).
        if (_uiState.value.isCreating) return
        _uiState.update { it.copy(isCreateDialogOpen = false) }
    }

    fun onCreateNameChange(value: String) {
        _uiState.update { it.copy(createName = value, createError = null) }
    }

    fun onCreateGroupTypeSelect(type: GroupType) {
        _uiState.update { it.copy(createGroupType = type) }
    }

    fun onConfirmCreateGroup() {
        val state = _uiState.value
        if (!state.canConfirmCreate) {
            _uiState.update { it.copy(createError = "그룹 이름을 입력해주세요.") }
            return
        }

        _uiState.update { it.copy(isCreating = true, createError = null) }
        viewModelScope.launch {
            archiveRepository.createArchive(state.createName, state.createGroupType)
                .onSuccess { created ->
                    // 새로 만든 그룹이 최근 활동순 맨 위에 오도록 앞에 붙인다.
                    allGroups = listOf(created) + allGroups
                    _uiState.update { it.copy(isCreating = false, isCreateDialogOpen = false) }
                    applyFilter()
                }
                .onFailure { throwable ->
                    _uiState.update {
                        it.copy(
                            isCreating = false,
                            createError = throwable.message ?: "그룹을 만들지 못했습니다.",
                        )
                    }
                }
        }
    }

    // ── 코드로 참여하기 ──

    fun onJoinByCodeClick() {
        _uiState.update { it.copy(isJoinDialogOpen = true, joinCode = "", joinError = null) }
    }

    fun onDismissJoinDialog() {
        if (_uiState.value.isJoining) return
        _uiState.update { it.copy(isJoinDialogOpen = false) }
    }

    fun onJoinCodeChange(value: String) {
        _uiState.update { it.copy(joinCode = value, joinError = null) }
    }

    fun onConfirmJoin() {
        val state = _uiState.value
        if (!state.canConfirmJoin) {
            _uiState.update { it.copy(joinError = "초대 코드를 입력해주세요.") }
            return
        }

        _uiState.update { it.copy(isJoining = true, joinError = null) }
        viewModelScope.launch {
            archiveRepository.joinArchiveByToken(state.joinCode)
                .onSuccess {
                    _uiState.update { it.copy(isJoining = false, isJoinDialogOpen = false) }
                    // 새로 들어간 그룹이 목록에 보이도록 다시 불러온다.
                    loadGroups()
                }
                .onFailure { throwable ->
                    _uiState.update {
                        it.copy(
                            isJoining = false,
                            joinError = throwable.message ?: "유효하지 않은 코드예요.",
                        )
                    }
                }
        }
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
