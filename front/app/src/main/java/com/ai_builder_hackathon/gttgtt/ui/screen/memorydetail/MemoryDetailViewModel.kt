package com.ai_builder_hackathon.gttgtt.ui.screen.memorydetail

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ai_builder_hackathon.gttgtt.domain.repository.ChatRepository
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
class MemoryDetailViewModel @Inject constructor(
    private val memoryRepository: MemoryRepository,
    // 좋아요/채팅공유는 각각 GroupFeedViewModel 이 이미 쓰는 PostRepository.toggleLike(),
    // ChatRepository.sendSharedMemory() 를 그대로 재사용한다 — 피드의 게시물과 이 화면의
    // 기억은 같은 memories/reactions 행을 가리키므로 로직을 두 번 만들 이유가 없다.
    private val postRepository: PostRepository,
    private val chatRepository: ChatRepository,
    savedStateHandle: SavedStateHandle,
) : ViewModel() {

    // toRoute() 는 Bundle 을 요구해 JVM 테스트에서 터지므로 키로 읽는다.
    private val memoryId: String = requireNotNull(savedStateHandle["memoryId"]) {
        "memoryId 인자가 없다. Route.MemoryDetail 로 진입했는지 확인할 것."
    }

    // 피드 카드의 댓글 버튼으로 들어왔을 때만 true (Route.MemoryDetail.focusComment 참고).
    private val focusCommentOnEntry: Boolean = savedStateHandle["focusComment"] ?: false

    private val _uiState = MutableStateFlow(MemoryDetailUiState(shouldFocusCommentInput = focusCommentOnEntry))
    val uiState: StateFlow<MemoryDetailUiState> = _uiState.asStateFlow()

    init {
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

    /**
     * 좋아요 토글. GroupFeedViewModel.onLikeClick() 과 같은 낙관적 업데이트 패턴 —
     * 서버 응답을 기다리지 않고 화면을 먼저 바꾸고, 실패하면 되돌린다.
     */
    fun onLikeClick() {
        val before = _uiState.value.memory ?: return

        _uiState.update { state ->
            state.copy(
                memory = before.copy(
                    likedByMe = !before.likedByMe,
                    likeCount = before.likeCount + if (before.likedByMe) -1 else 1,
                )
            )
        }

        viewModelScope.launch {
            postRepository.toggleLike(memoryId)
                .onSuccess { post ->
                    _uiState.update { state ->
                        state.copy(
                            memory = state.memory?.copy(
                                likedByMe = post.likedByMe,
                                likeCount = post.likeCount,
                            )
                        )
                    }
                }
                .onFailure { throwable ->
                    _uiState.update {
                        it.copy(
                            memory = before,
                            errorMessage = throwable.message ?: "좋아요를 반영하지 못했습니다.",
                        )
                    }
                }
        }
    }

    /** 채팅방 공유 — 사진 아래 오른쪽 보내기 버튼. GroupFeedViewModel.onShareClick() 과 같은 패턴. */
    fun onShareClick() {
        if (_uiState.value.isSharing) return
        val archiveId = _uiState.value.memory?.archiveId ?: return
        _uiState.update { it.copy(isSharing = true, shareError = null) }

        viewModelScope.launch {
            chatRepository.sendSharedMemory(archiveId, memoryId)
                .onSuccess {
                    _uiState.update {
                        it.copy(isSharing = false, shareSuccessCount = it.shareSuccessCount + 1)
                    }
                }
                .onFailure { throwable ->
                    _uiState.update {
                        it.copy(
                            isSharing = false,
                            shareError = throwable.message ?: "채팅방에 공유하지 못했습니다.",
                        )
                    }
                }
        }
    }

    /**
     * 공유 성공 후 채팅방 이동을 처리했다고 표시한다.
     * GroupFeedViewModel.onShareHandled() 와 같은 이유 — 안 하면 채팅방에서 뒤로가기로
     * 돌아왔을 때 이 화면이 재조립되며 shareSuccessCount 가 그대로인 걸 보고 또 튕겨버린다.
     */
    fun onShareHandled() {
        _uiState.update { it.copy(shareSuccessCount = 0) }
    }

    /**
     * 댓글 입력창 자동 포커스를 처리했다고 표시한다. GroupFeedViewModel.onShareHandled() 와
     * 같은 이유 — 안 하면 이 화면이 재조립될 때마다(예: 수정 화면 다녀오기) 키보드가 다시 뜬다.
     */
    fun onCommentFocusHandled() {
        _uiState.update { it.copy(shouldFocusCommentInput = false) }
    }

    fun onMoreClick() {
        _uiState.update { it.copy(isOptionsSheetOpen = true) }
    }

    fun onDismissOptionsSheet() {
        _uiState.update { it.copy(isOptionsSheetOpen = false) }
    }

    fun onDeleteClick() {
        _uiState.update { it.copy(isOptionsSheetOpen = false, isDeleteConfirmOpen = true) }
    }

    fun onDismissDeleteConfirm() {
        _uiState.update { it.copy(isDeleteConfirmOpen = false, deleteError = null) }
    }

    fun onConfirmDelete() {
        if (_uiState.value.isDeleting) return
        _uiState.update { it.copy(isDeleting = true, deleteError = null) }

        viewModelScope.launch {
            memoryRepository.deleteMemory(memoryId)
                .onSuccess {
                    _uiState.update { it.copy(isDeleting = false, isDeleteConfirmOpen = false, isDeleted = true) }
                }
                .onFailure { throwable ->
                    _uiState.update {
                        it.copy(
                            isDeleting = false,
                            deleteError = throwable.message ?: "삭제하지 못했습니다.",
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
