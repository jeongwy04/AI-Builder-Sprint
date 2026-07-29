package com.ai_builder_hackathon.gttgtt.ui.screen.memorycreate

import com.ai_builder_hackathon.gttgtt.domain.model.Participant
import com.ai_builder_hackathon.gttgtt.domain.model.Photo

data class MemoryCreateUiState(
    val title: String = "",
    val body: String = "",
    /** 새로 고른(아직 업로드 전) 사진의 content:// URI 목록 */
    val photoUris: List<String> = emptyList(),
    /**
     * 수정 모드에서 서버에 이미 저장돼 있는 사진들. 새 기억 작성에서는 항상 비어 있다.
     * [removedPhotoIds] 에 담긴 id 는 화면에서 걸러서 안 보여준다 — 실제 삭제는 저장 시점에 한다.
     */
    val existingPhotos: List<Photo> = emptyList(),
    /** existingPhotos 중 사용자가 지우기로 표시한 사진의 id ([Photo.id]) */
    val removedPhotoIds: Set<String> = emptySet(),
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
    /** memoryId 가 있는 라우트로 들어왔는지 — 켜지면 제목/본문/날짜/함께한 사람 외에 사진도 고칠 수 있다. */
    val isEditMode: Boolean = false,
    /** 수정 대상 기억을 불러오는 중 (편집 모드 진입 직후) */
    val isLoadingExisting: Boolean = false,
) {
    /** 화면에 지금 보이는(=저장하면 남을) 기존 사진 목록. 삭제 표시된 건 뺀다. */
    val visibleExistingPhotos: List<Photo>
        get() = existingPhotos.filterNot { it.id in removedPhotoIds }

    /** 새 사진 + 남은 기존 사진을 합친, 저장하면 실제로 남게 될 사진 수 */
    val remainingPhotoCount: Int
        get() = photoUris.size + visibleExistingPhotos.size

    /**
     * 사진과 본문 중 하나는 있어야 저장할 수 있다.
     * 사진만 있고 메모가 없으면 검색 근거(search_text)가 빈약해지므로
     * 화면에서 메모 작성을 권유한다 (CLAUDE.md §6.3~6.4).
     */
    val canSave: Boolean
        get() = !isSaving && !isLoadingExisting && (remainingPhotoCount > 0 || body.isNotBlank())

    /** 사진은 있는데 메모가 비었을 때 안내를 띄울지 */
    val shouldSuggestNote: Boolean
        get() = remainingPhotoCount > 0 && body.isBlank()
}
