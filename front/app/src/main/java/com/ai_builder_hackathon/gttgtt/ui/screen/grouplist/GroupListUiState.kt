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
    /** 그룹 카드 우측 상단 점 세개를 누르면 뜨는 "그룹 설정" 시트 — 이름 변경 · 친구 초대 · 삭제. */
    val settingsArchiveId: String? = null,
    val settingsArchiveName: String = "",
    /** 이름 변경 다이얼로그 상태. */
    val renamingArchiveId: String? = null,
    val renameText: String = "",
    val isRenaming: Boolean = false,
    val renameError: String? = null,
    /** 친구 초대 다이얼로그 상태. */
    val invitingArchiveId: String? = null,
    val isInviteLoading: Boolean = false,
    val inviteToken: String? = null,
    val inviteError: String? = null,
    /** 그룹 삭제 확인 다이얼로그 상태. */
    val deletingArchiveId: String? = null,
    val deletingArchiveName: String = "",
    val isDeleting: Boolean = false,
    val deleteError: String? = null,
    /** 그룹 사진 변경 — Photo Picker 로 고른 뒤 업로드가 끝날 때까지 그 그룹 id 가 담긴다. */
    val updatingCoverImageArchiveId: String? = null,
    val coverImageError: String? = null,
) {
    val isEmpty: Boolean
        get() = !isLoading && groups.isEmpty()

    val canConfirmCreate: Boolean
        get() = !isCreating && createName.isNotBlank()

    val canConfirmJoin: Boolean
        get() = !isJoining && joinCode.isNotBlank()

    val isSettingsSheetOpen: Boolean
        get() = settingsArchiveId != null

    val isRenameDialogOpen: Boolean
        get() = renamingArchiveId != null

    val canConfirmRename: Boolean
        get() = !isRenaming && renameText.isNotBlank()

    val isInviteDialogOpen: Boolean
        get() = invitingArchiveId != null

    val isDeleteConfirmOpen: Boolean
        get() = deletingArchiveId != null
}
