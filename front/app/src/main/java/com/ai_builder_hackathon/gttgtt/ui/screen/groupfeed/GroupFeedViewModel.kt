package com.ai_builder_hackathon.gttgtt.ui.screen.groupfeed

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ai_builder_hackathon.gttgtt.domain.repository.ArchiveRepository
import com.ai_builder_hackathon.gttgtt.domain.repository.MemoryRepository
import com.ai_builder_hackathon.gttgtt.domain.repository.PostRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class GroupFeedViewModel @Inject constructor(
    private val postRepository: PostRepository,
    private val archiveRepository: ArchiveRepository,
    private val memoryRepository: MemoryRepository,
    savedStateHandle: SavedStateHandle,
) : ViewModel() {

    /**
     * 어떤 그룹의 피드인지는 네비게이션 인자에서 온다.
     * toRoute() 는 내부에서 Bundle 을 만들어 JVM 단위 테스트에서 터진다.
     * type-safe 라우트도 인자를 이름으로 저장하므로 키로 읽어도 동작은 같다.
     */
    val archiveId: String = requireNotNull(savedStateHandle["archiveId"]) {
        "archiveId 인자가 없다. Route.GroupFeed 로 진입했는지 확인할 것."
    }

    private val _uiState = MutableStateFlow(GroupFeedUiState())
    val uiState: StateFlow<GroupFeedUiState> = _uiState.asStateFlow()

    init {
        loadFeed()
    }

    fun retry() {
        loadFeed()
    }

    /**
     * 좋아요 토글.
     * 서버 응답을 기다리지 않고 화면을 먼저 바꾼다(낙관적 업데이트) — 탭 반응이 즉각적이어야 하기 때문.
     * 실패하면 이전 상태로 되돌린다.
     */
    fun onLikeClick(postId: String) {
        val before = _uiState.value.posts
        _uiState.update { state ->
            state.copy(
                posts = state.posts.map { post ->
                    if (post.id != postId) {
                        post
                    } else {
                        post.copy(
                            likedByMe = !post.likedByMe,
                            likeCount = post.likeCount + if (post.likedByMe) -1 else 1,
                        )
                    }
                }
            )
        }

        viewModelScope.launch {
            postRepository.toggleLike(postId)
                .onSuccess { updated ->
                    _uiState.update { state ->
                        state.copy(posts = state.posts.map { if (it.id == updated.id) updated else it })
                    }
                }
                .onFailure { throwable ->
                    _uiState.update {
                        it.copy(
                            posts = before,
                            errorMessage = throwable.message ?: "좋아요를 반영하지 못했습니다.",
                        )
                    }
                }
        }
    }

    // ── 그룹 설정 시트 ──

    fun onSettingsClick() {
        _uiState.update { it.copy(isSettingsSheetOpen = true) }
    }

    fun onDismissSettingsSheet() {
        _uiState.update { it.copy(isSettingsSheetOpen = false) }
    }

    // ── 이름 변경 ──

    fun onRenameClick() {
        _uiState.update {
            it.copy(
                isSettingsSheetOpen = false,
                isRenameDialogOpen = true,
                renameText = it.groupName,
                renameError = null,
            )
        }
    }

    fun onRenameTextChange(value: String) {
        _uiState.update { it.copy(renameText = value, renameError = null) }
    }

    fun onDismissRenameDialog() {
        if (_uiState.value.isRenaming) return
        _uiState.update { it.copy(isRenameDialogOpen = false) }
    }

    fun onConfirmRename() {
        val newName = _uiState.value.renameText
        _uiState.update { it.copy(isRenaming = true, renameError = null) }
        viewModelScope.launch {
            archiveRepository.renameArchive(archiveId, newName)
                .onSuccess {
                    _uiState.update {
                        it.copy(
                            isRenaming = false,
                            isRenameDialogOpen = false,
                            groupName = newName.trim(),
                        )
                    }
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

    // ── 삭제 ──

    fun onDeleteClick() {
        _uiState.update { it.copy(isSettingsSheetOpen = false, isDeleteConfirmOpen = true, deleteError = null) }
    }

    fun onDismissDeleteConfirm() {
        if (_uiState.value.isDeleting) return
        _uiState.update { it.copy(isDeleteConfirmOpen = false) }
    }

    fun onConfirmDelete() {
        _uiState.update { it.copy(isDeleting = true, deleteError = null) }
        viewModelScope.launch {
            archiveRepository.deleteArchive(archiveId)
                .onSuccess {
                    _uiState.update {
                        it.copy(isDeleting = false, isDeleteConfirmOpen = false, isGroupDeleted = true)
                    }
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

    // ── 친구 초대 ──

    fun onInviteClick() {
        _uiState.update {
            it.copy(
                isSettingsSheetOpen = false,
                isInviteDialogOpen = true,
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
        _uiState.update { it.copy(isInviteDialogOpen = false) }
    }

    // ── 게시물(기억) 삭제 — 카드 우상단 점 세개 버튼 ──

    fun onPostDeleteClick(postId: String) {
        _uiState.update { it.copy(postPendingDeleteId = postId, deletePostError = null) }
    }

    fun onDismissPostDeleteConfirm() {
        if (_uiState.value.isDeletingPost) return
        _uiState.update { it.copy(postPendingDeleteId = null, deletePostError = null) }
    }

    fun onConfirmPostDelete() {
        val postId = _uiState.value.postPendingDeleteId ?: return
        _uiState.update { it.copy(isDeletingPost = true, deletePostError = null) }

        viewModelScope.launch {
            memoryRepository.deleteMemory(postId)
                .onSuccess {
                    _uiState.update { state ->
                        state.copy(
                            isDeletingPost = false,
                            postPendingDeleteId = null,
                            posts = state.posts.filterNot { it.id == postId },
                        )
                    }
                }
                .onFailure { throwable ->
                    _uiState.update {
                        it.copy(
                            isDeletingPost = false,
                            deletePostError = throwable.message ?: "게시물을 삭제하지 못했습니다.",
                        )
                    }
                }
        }
    }

    private fun loadFeed() {
        _uiState.update { it.copy(isLoading = true, errorMessage = null) }
        viewModelScope.launch {
            val group = archiveRepository.getMyArchives()
                .getOrNull()
                ?.firstOrNull { it.id == archiveId }

            postRepository.getFeed(archiveId)
                .onSuccess { posts ->
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            groupName = group?.name.orEmpty(),
                            memberCount = group?.totalMemberCount ?: 0,
                            posts = posts,
                        )
                    }
                }
                .onFailure { throwable ->
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            errorMessage = throwable.message ?: "게시물을 불러오지 못했습니다.",
                        )
                    }
                }
        }
    }
}
