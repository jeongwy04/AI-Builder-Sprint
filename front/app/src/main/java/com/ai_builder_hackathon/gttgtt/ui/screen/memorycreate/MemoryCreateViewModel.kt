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

    private val _uiState = MutableStateFlow(MemoryCreateUiState())
    val uiState: StateFlow<MemoryCreateUiState> = _uiState.asStateFlow()

    init {
        loadMembers()
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

    /**
     * 갤러리에서 사진을 고른 직후.
     * 첫 사진의 EXIF 촬영일을 읽어 추억 날짜 기본값으로 채운다 (CLAUDE.md §6.2).
     */
    fun onPhotosPicked(uris: List<String>) {
        if (uris.isEmpty()) return

        val isFirstPick = _uiState.value.photoUris.isEmpty()
        _uiState.update { it.copy(photoUris = (it.photoUris + uris).distinct()) }

        // 이미 사용자가 날짜를 정했으면 덮어쓰지 않는다.
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

        viewModelScope.launch {
            memoryRepository.createMemory(
                NewMemory(
                    archiveId = archiveId,
                    // 제목을 비워두면 본문 첫 줄을 제목으로 삼는다.
                    title = state.title.ifBlank { state.body.lineSequence().first().take(TITLE_MAX) },
                    body = state.body,
                    memoryDateMillis = state.memoryDateMillis,
                    photoUris = state.photoUris,
                    participantIds = state.selectedParticipantIds.toList(),
                )
            )
                .onSuccess { memoryId ->
                    _uiState.update { it.copy(isSaving = false, savedMemoryId = memoryId) }
                }
                .onFailure { throwable ->
                    _uiState.update {
                        it.copy(
                            isSaving = false,
                            errorMessage = throwable.message ?: "기억을 저장하지 못했습니다.",
                        )
                    }
                }
        }
    }

    private fun loadMembers() {
        viewModelScope.launch {
            val archive = archiveRepository.getMyArchives()
                .getOrNull()
                ?.firstOrNull { it.id == archiveId }
                ?: return@launch

            _uiState.update { state ->
                state.copy(
                    members = archive.memberIds.map { id ->
                        Participant(id = id, name = id.removePrefix("u-"))
                    }
                )
            }
        }
    }

    private companion object {
        const val TITLE_MAX = 30
    }
}
