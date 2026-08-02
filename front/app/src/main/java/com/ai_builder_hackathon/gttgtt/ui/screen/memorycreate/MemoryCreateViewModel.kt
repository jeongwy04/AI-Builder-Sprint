package com.ai_builder_hackathon.gttgtt.ui.screen.memorycreate

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ai_builder_hackathon.gttgtt.domain.model.NewMemory
import com.ai_builder_hackathon.gttgtt.domain.model.Participant
import com.ai_builder_hackathon.gttgtt.domain.repository.ArchiveRepository
import com.ai_builder_hackathon.gttgtt.domain.repository.MemoryRepository
import com.ai_builder_hackathon.gttgtt.domain.repository.PhotoMetadataReader
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject
import com.ai_builder_hackathon.gttgtt.ui.util.toUserMessage

@HiltViewModel
class MemoryCreateViewModel @Inject constructor(
    private val memoryRepository: MemoryRepository,
    private val archiveRepository: ArchiveRepository,
    private val photoMetadataReader: PhotoMetadataReader,
    savedStateHandle: SavedStateHandle,
) : ViewModel() {

    private val archiveId: String = requireNotNull(savedStateHandle["archiveId"]) {
        "archiveId 인자가 없다. Route.MemoryCreate 로 진입했는지 확인할 것."
    }

    /** null 이면 새 기억 작성, 있으면 해당 기억을 수정하는 모드. */
    private val editMemoryId: String? = savedStateHandle["memoryId"]

    /** 홈에서 카메라로 촬영해 들어온 경우 미리 담을 사진 URI. */
    private val initialPhotoUri: String? = savedStateHandle["initialPhotoUri"]

    private val _uiState = MutableStateFlow(MemoryCreateUiState(isEditMode = editMemoryId != null))
    val uiState: StateFlow<MemoryCreateUiState> = _uiState.asStateFlow()

    init {
        loadMembers()
        editMemoryId?.let(::loadExistingMemory)
        // 카메라로 찍은 사진을 미리 담는다. onPhotosPicked 가 EXIF 촬영일까지 채운다.
        initialPhotoUri?.let { onPhotosPicked(listOf(it)) }
    }

    private fun loadExistingMemory(memoryId: String) {
        _uiState.update { it.copy(isLoadingExisting = true) }
        viewModelScope.launch {
            memoryRepository.getDetail(memoryId)
                .onSuccess { memory ->
                    _uiState.update {
                        it.copy(
                            isLoadingExisting = false,
                            title = memory.title,
                            body = memory.body,
                            existingPhotos = memory.photos,
                            memoryDateMillis = memory.memoryDateMillis,
                            selectedParticipantIds = memory.participants.map { p -> p.id }.toSet(),
                        )
                    }
                }
                .onFailure { throwable ->
                    _uiState.update {
                        it.copy(
                            isLoadingExisting = false,
                            errorMessage = throwable.toUserMessage("기억을 불러오지 못했습니다."),
                        )
                    }
                }
        }
    }

    fun onTitleChange(value: String) {
        _uiState.update { it.copy(title = value) }
    }

    fun onBodyChange(value: String) {
        _uiState.update { it.copy(body = value) }
    }

    fun onDateChange(millis: Long) {
        // 사용자가 직접 고른 순간부터는 EXIF 표시를 끈다.
        _uiState.update { it.copy(memoryDateMillis = millis, isDateFromExif = false) }
    }

    fun onParticipantToggle(memberId: String) {
        _uiState.update { state ->
            val next = state.selectedParticipantIds.toMutableSet()
            if (!next.add(memberId)) next.remove(memberId)
            state.copy(selectedParticipantIds = next)
        }
    }

    fun onPhotoRemove(uri: String) {
        _uiState.update { it.copy(photoUris = it.photoUris - uri) }
    }

    /** 이미 서버에 저장된 사진을 지우기로 표시한다. 실제 삭제는 저장 시점에 한 번에 처리한다. */
    fun onExistingPhotoRemove(photoId: String) {
        _uiState.update { it.copy(removedPhotoIds = it.removedPhotoIds + photoId) }
    }

    /**
     * 갤러리에서 사진을 고른 직후.
     * 첫 사진의 EXIF 촬영일을 읽어 추억 날짜 기본값으로 채운다 (CLAUDE.md §6.2).
     * 수정 모드에서는 이미 날짜가 정해져 있으므로(원래 기억의 날짜) 자동으로 덮어쓰지 않는다.
     */
    fun onPhotosPicked(uris: List<String>) {
        if (uris.isEmpty()) return

        val isFirstPick = !_uiState.value.isEditMode && _uiState.value.photoUris.isEmpty()
        _uiState.update { it.copy(photoUris = (it.photoUris + uris).distinct()) }

        // 이미 사용자가 날짜를 정했거나(또는 수정 모드라 원래 날짜가 있으면) 덮어쓰지 않는다.
        if (!isFirstPick) return

        viewModelScope.launch {
            val capturedAt = photoMetadataReader.readCapturedAtMillis(uris.first())
            if (capturedAt != null) {
                _uiState.update { it.copy(memoryDateMillis = capturedAt, isDateFromExif = true) }
            }
        }
    }

    fun onSaveClick() {
        val state = _uiState.value
        if (!state.canSave) return

        _uiState.update { it.copy(isSaving = true, errorMessage = null) }

        val title = state.title.ifBlank {
            state.body.lineSequence().firstOrNull { it.isNotBlank() }?.take(TITLE_MAX) ?: ""
        }

        viewModelScope.launch {
            val memoryId = editMemoryId
            val result = if (memoryId != null) {
                memoryRepository.updateMemory(
                    memoryId = memoryId,
                    archiveId = archiveId,
                    title = title,
                    body = state.body,
                    memoryDateMillis = state.memoryDateMillis,
                    participantIds = state.selectedParticipantIds.toList(),
                    newPhotoUris = state.photoUris,
                    removedPhotoIds = state.removedPhotoIds.toList(),
                ).map { memoryId }
            } else {
                memoryRepository.createMemory(
                    NewMemory(
                        archiveId = archiveId,
                        title = title,
                        body = state.body,
                        memoryDateMillis = state.memoryDateMillis,
                        photoUris = state.photoUris,
                        participantIds = state.selectedParticipantIds.toList(),
                    )
                )
            }

            result
                .onSuccess { savedId ->
                    _uiState.update { it.copy(isSaving = false, savedMemoryId = savedId) }
                }
                .onFailure { throwable ->
                    _uiState.update {
                        it.copy(
                            isSaving = false,
                            errorMessage = throwable.toUserMessage("기억을 저장하지 못했습니다."),
                        )
                    }
                }
        }
    }

    private fun loadMembers() {
        viewModelScope.launch {
            // 그룹 하나만 필요한데 getMyArchives() 로 내 그룹 전체를 다시 훑으면(N+1) 이 화면을
            // 열 때마다 불필요하게 느려진다 — getArchive() 는 이 그룹만 조회한다.
            val archive = archiveRepository.getArchive(archiveId).getOrNull() ?: return@launch

            // ⚠️ archive.memberIds 는 실제 유저 UUID다. id.removePrefix("u-") 로는
            // 이름이 안 나온다 (Fake 시절 "u-minji" 같은 id 에나 맞던 방식) — profiles 를 조회해야 한다.
            val nameById = archiveRepository.getMemberNames(archive.memberIds).getOrDefault(emptyMap())

            _uiState.update { state ->
                state.copy(
                    members = archive.memberIds.map { id ->
                        Participant(id = id, name = nameById[id] ?: "멤버")
                    }
                )
            }
        }
    }

    private companion object {
        const val TITLE_MAX = 30
    }
}
