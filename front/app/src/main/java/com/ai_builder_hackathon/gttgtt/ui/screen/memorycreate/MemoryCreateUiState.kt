package com.ai_builder_hackathon.gttgtt.ui.screen.memorycreate

import com.ai_builder_hackathon.gttgtt.domain.model.Participant

data class MemoryCreateUiState(
    val title: String = "",
    val body: String = "",
    /** 선택한 사진의 content:// URI 목록 */
    val photoUris: List<String> = emptyList(),
    /** 추억 날짜. 첫 사진의 EXIF 촬영일로 자동 채워지고, 사용자가 바꿀 수 있다. */
    val memoryDateMillis: Long = System.currentTimeMillis(),
    /** EXIF 에서 날짜를 읽어왔는지. 읽어왔으면 화면에 "촬영일 자동" 표시를 띄운다. */
    val isDateFromExif: Boolean = false,
    /** 그룹 멤버 전체 (태깅 후보) */
    val members: List<Participant> = emptyList(),
    val selectedParticipantIds: Set<String> = emptySet(),
    val isSaving: Boolean = false,
    /** 저장 성공 시 만들어진(또는 수정한) 기억 id. 화면을 나가는 신호로 쓴다. */
    val savedMemoryId: String? = null,
    val errorMessage: String? = null,
    /** memoryId 가 있는 라우트로 들어왔는지 — 켜지면 제목/본문/날짜/함께한 사람만 고치고 사진은 그대로 둔다. */
    val isEditMode: Boolean = false,
    /** 수정 대상 기억을 불러오는 중 (편집 모드 진입 직후) */
    val isLoadingExisting: Boolean = false,
) {
    /**
     * 사진과 본문 중 하나는 있어야 저장할 수 있다.
     * 사진만 있고 메모가 없으면 검색 근거(search_text)가 빈약해지므로
     * 화면에서 메모 작성을 권유한다 (CLAUDE.md §6.3~6.4).
     *
     * 수정 모드에서는 사진을 이 화면에서 건드리지 않으므로 사진 유무를 조건에서 뺀다.
     */
    val canSave: Boolean
        get() = !isSaving && !isLoadingExisting &&
            (isEditMode || photoUris.isNotEmpty() || body.isNotBlank())

    /** 사진은 골랐는데 메모가 비었을 때 안내를 띄울지 */
    val shouldSuggestNote: Boolean
        get() = photoUris.isNotEmpty() && body.isBlank()
}
