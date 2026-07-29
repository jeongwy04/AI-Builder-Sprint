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
        // 사진 업로드가 진행 중일 때 화면 재진입(ON_RESUME)으로 목록을 통째로 다시 불러오면,
        // 그 새로고침 결과가 업로드 완료 후 반영보다 늦게 끝날 경우 방금 바꾼 사진이 다시
        // 덮어써질 수 있다 — 업로드가 끝날 때까지는 재조회를 미룬다.
        if (_uiState.value.updatingCoverImageArchiveId != null) return
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

    /** "새 그룹 만들기" 다이얼로그 안에서 "코드로 참여하기"로 전환할 때. */
    fun onSwitchToJoinByCode() {
        _uiState.update {
            it.copy(
                isCreateDialogOpen = false,
                isJoinDialogOpen = true,
                joinCode = "",
                joinError = null,
            )
        }
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

    // ── 그룹 카드 우측 상단 점 세개 → 그룹 설정 시트 (이름 변경 · 친구 초대 · 삭제) ──

    fun onGroupSettingsClick(archiveId: String) {
        val name = allGroups.firstOrNull { it.id == archiveId }?.name ?: return
        _uiState.update { it.copy(settingsArchiveId = archiveId, settingsArchiveName = name) }
    }

    fun onDismissSettingsSheet() {
        _uiState.update { it.copy(settingsArchiveId = null) }
    }

    // ── 이름 변경 ──

    fun onRenameClick() {
        val state = _uiState.value
        val archiveId = state.settingsArchiveId ?: return
        _uiState.update {
            it.copy(
                settingsArchiveId = null,
                renamingArchiveId = archiveId,
                renameText = state.settingsArchiveName,
                renameError = null,
            )
        }
    }

    fun onRenameTextChange(value: String) {
        _uiState.update { it.copy(renameText = value, renameError = null) }
    }

    fun onDismissRenameDialog() {
        if (_uiState.value.isRenaming) return
        _uiState.update { it.copy(renamingArchiveId = null) }
    }

    fun onConfirmRename() {
        val state = _uiState.value
        val archiveId = state.renamingArchiveId ?: return
        if (!state.canConfirmRename) {
            _uiState.update { it.copy(renameError = "그룹 이름을 입력해주세요.") }
            return
        }

        val newName = state.renameText
        _uiState.update { it.copy(isRenaming = true, renameError = null) }
        viewModelScope.launch {
            archiveRepository.renameArchive(archiveId, newName)
                .onSuccess {
                    allGroups = allGroups.map { group ->
                        if (group.id == archiveId) group.copy(name = newName.trim()) else group
                    }
                    _uiState.update { it.copy(isRenaming = false, renamingArchiveId = null) }
                    applyFilter()
                }
                .onFailure { throwable ->
                    _uiState.update {
                        it.copy(
                            isRenaming = false,
                            renameError = throwable.message ?: "이름을 변경하지 못했습니다.",
                        )
                    }
                }
        }
    }

    // ── 친구 초대 ──

    fun onInviteClick() {
        val archiveId = _uiState.value.settingsArchiveId ?: return
        _uiState.update {
            it.copy(
                settingsArchiveId = null,
                invitingArchiveId = archiveId,
                isInviteLoading = true,
                inviteToken = null,
                inviteError = null,
            )
        }
        viewModelScope.launch {
            archiveRepository.createInvitation(archiveId)
                .onSuccess { token ->
                    _uiState.update { it.copy(isInviteLoading = false, inviteToken = token) }
                }
                .onFailure { throwable ->
                    _uiState.update {
                        it.copy(
                            isInviteLoading = false,
                            inviteError = throwable.message ?: "초대 코드를 만들지 못했습니다.",
                        )
                    }
                }
        }
    }

    fun onDismissInviteDialog() {
        _uiState.update { it.copy(invitingArchiveId = null) }
    }

    // ── 그룹 사진 변경 ──

    /**
     * 설정 시트의 "그룹 사진 변경"을 누르면 화면이 Photo Picker 를 띄우고, 고른 결과를 이걸로 넘긴다.
     *
     * ⚠️ [archiveId] 는 화면이 Photo Picker 를 띄우기 "직전"에 캡처해서 넘겨받은 값이어야 한다.
     * 예전엔 이 함수 안에서 `_uiState.value.settingsArchiveId` 를 읽었는데, Photo Picker는
     * 시스템 화면(별도 액티비티)이라 그 사이 화면 회전/메모리 회수로 ViewModel이 재생성되면
     * `settingsArchiveId` 가 null 로 리셋돼 있어 사진을 골라도 조용히 아무 일도 안 일어났다
     * (그룹 피드 화면은 archiveId 가 네비게이션 인자라 이 문제가 없어서 거기선 항상 잘 됐다).
     */
    fun onCoverImagePicked(archiveId: String, imageUri: String) {
        _uiState.update {
            it.copy(settingsArchiveId = null, updatingCoverImageArchiveId = archiveId, coverImageError = null)
        }
        viewModelScope.launch {
            archiveRepository.updateCoverImage(archiveId, imageUri)
                .onSuccess {
                    // 로컬 content:// URI 는 신뢰할 수 없어 그대로 쓰지 않고, 서버가 발급한
                    // signed URL 을 다시 받아온다.
                    val refreshed = archiveRepository.getArchive(archiveId).getOrNull()
                    if (refreshed != null) {
                        allGroups = allGroups.map { group ->
                            if (group.id == archiveId) group.copy(coverImageUrl = refreshed.coverImageUrl) else group
                        }
                    }
                    _uiState.update { it.copy(updatingCoverImageArchiveId = null) }
                    applyFilter()
                }
                .onFailure { throwable ->
                    _uiState.update {
                        it.copy(
                            updatingCoverImageArchiveId = null,
                            coverImageError = throwable.message ?: "그룹 사진을 변경하지 못했습니다.",
                        )
                    }
                }
        }
    }

    fun onDismissCoverImageError() {
        _uiState.update { it.copy(coverImageError = null) }
    }

    // ── 그룹 삭제 ──

    fun onDeleteClick() {
        val state = _uiState.value
        val archiveId = state.settingsArchiveId ?: return
        _uiState.update {
            it.copy(
                settingsArchiveId = null,
                deletingArchiveId = archiveId,
                deletingArchiveName = state.settingsArchiveName,
                deleteError = null,
            )
        }
    }

    fun onDismissDeleteConfirm() {
        if (_uiState.value.isDeleting) return
        _uiState.update { it.copy(deletingArchiveId = null) }
    }

    fun onConfirmDelete() {
        val archiveId = _uiState.value.deletingArchiveId ?: return
        _uiState.update { it.copy(isDeleting = true, deleteError = null) }
        viewModelScope.launch {
            archiveRepository.deleteArchive(archiveId)
                .onSuccess {
                    allGroups = allGroups.filterNot { it.id == archiveId }
                    _uiState.update { it.copy(isDeleting = false, deletingArchiveId = null) }
                    applyFilter()
                }
                .onFailure { throwable ->
                    _uiState.update {
                        it.copy(
                            isDeleting = false,
                            deleteError = throwable.message ?: "그룹을 삭제하지 못했습니다.",
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
