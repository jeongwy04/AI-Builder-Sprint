package com.ai_builder_hackathon.gttgtt.domain.repository

import com.ai_builder_hackathon.gttgtt.domain.model.Comment
import com.ai_builder_hackathon.gttgtt.domain.model.MemoryDetail
import com.ai_builder_hackathon.gttgtt.domain.model.NewMemory

interface MemoryRepository {
    suspend fun getDetail(memoryId: String): Result<MemoryDetail>

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
}
