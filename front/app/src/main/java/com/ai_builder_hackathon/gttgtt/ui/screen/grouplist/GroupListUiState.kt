package com.ai_builder_hackathon.gttgtt.ui.screen.grouplist

import com.ai_builder_hackathon.gttgtt.domain.model.ArchiveSummary
import com.ai_builder_hackathon.gttgtt.domain.model.GroupType

data class GroupListUiState(
    val isLoading: Boolean = true,
    val query: String = "",
    /** 최근 활동순으로 정렬된 채팅방 목록 */
    val groups: List<ArchiveSummary> = emptyList(),
    val errorMessage: String? = null,
    /** 우측 하단 + 버튼으로 여는 "새 그룹 만들기" 다이얼로그 상태 */
    val isCreateDialogOpen: Boolean = false,
    val createName: String = "",
    val createGroupType: GroupType = GroupType.FRIENDS,
    val isCreating: Boolean = false,
    val createError: String? = null,
    /** "코드로 참여하기" 다이얼로그 상태 — 그룹 설정에서 발급한 초대 코드를 입력해 참여한다. */
    val isJoinDialogOpen: Boolean = false,
    val joinCode: String = "",
    val isJoining: Boolean = false,
    val joinError: String? = null,
) {
    val isEmpty: Boolean
        get() = !isLoading && groups.isEmpty()

    val canConfirmCreate: Boolean
        get() = !isCreating && createName.isNotBlank()

    val canConfirmJoin: Boolean
        get() = !isJoining && joinCode.isNotBlank()
}
