package com.ai_builder_hackathon.gttgtt.domain.repository

import com.ai_builder_hackathon.gttgtt.domain.model.Comment
import com.ai_builder_hackathon.gttgtt.domain.model.MemoryDetail
import com.ai_builder_hackathon.gttgtt.domain.model.MemorySummary
import com.ai_builder_hackathon.gttgtt.domain.model.NewMemory

interface MemoryRepository {
    suspend fun getDetail(memoryId: String): Result<MemoryDetail>

    /** 내가 작성한 기억 목록 (memory_date 최근순). 여러 그룹에 걸쳐 모은다. */
    suspend fun getMyMemories(): Result<List<MemorySummary>>

    /** 내가 좋아요(reaction)한 기억 목록 (memory_date 최근순). */
    suspend fun getLikedMemories(): Result<List<MemorySummary>>

    /**
     * 기억을 새로 만든다. 생성된 기억 id 를 돌려준다.
     *
     * 구현체는 사진 업로드 → memories insert 까지 마친 뒤
     * 반드시 embed-memory Edge Function 을 호출해야 한다.
     * 빠뜨리면 AI 검색에 영영 안 잡힌다 (CLAUDE.md §5.6, §15).
     */
    suspend fun createMemory(memory: NewMemory): Result<String>

    /**
     * 댓글 추가. 등록된 댓글을 돌려준다.
     *
     * 그룹 멤버는 전원 동일 권한이라 다른 사람의 기억에도 누구나 댓글을 달 수 있다 (CLAUDE.md §6.1).
     */
    suspend fun addComment(memoryId: String, text: String): Result<Comment>

    /**
     * 기억 수정. 제목/본문/날짜/함께한 사람과 함께 사진도 바꿀 수 있다.
     *
     * @param newPhotoUris 새로 추가할 사진의 기기 content:// URI 목록.
     * @param removedPhotoIds 지울 기존 사진의 id 목록 (media_assets 행 id — [Photo.id]).
     *
     * ⚠️ title 은 실제로는 저장되지 않는다 (getDetail() 이 매번 본문 첫 줄에서 다시 계산한다 —
     * createMemory() 와 같은 이유). Fake 구현만 title 을 따로 들고 있어서 값을 반영한다.
     * 본문이 바뀌면 검색 재료가 바뀌므로 구현체는 embed-memory 를 다시 호출해야 한다 (CLAUDE.md §5.6).
     */
    suspend fun updateMemory(
        memoryId: String,
        archiveId: String,
        title: String,
        body: String,
        memoryDateMillis: Long,
        participantIds: List<String>,
        newPhotoUris: List<String> = emptyList(),
        removedPhotoIds: List<String> = emptyList(),
    ): Result<Unit>

    /**
     * 기억 삭제. media_assets/notes/memory_people 은 memory_id FK 에 on delete cascade 라
     * memories 한 행만 지우면 하위 데이터가 함께 정리된다 (마이그레이션 참고).
     *
     * 그룹 멤버는 전원 동일 권한이라 다른 사람이 남긴 기억도 지울 수 있다 (CLAUDE.md §6.1).
     */
    suspend fun deleteMemory(memoryId: String): Result<Unit>
}
