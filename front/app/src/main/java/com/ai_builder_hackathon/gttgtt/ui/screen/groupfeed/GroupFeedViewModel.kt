package com.ai_builder_hackathon.gttgtt.ui.screen.groupfeed

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ai_builder_hackathon.gttgtt.domain.repository.ArchiveRepository
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
