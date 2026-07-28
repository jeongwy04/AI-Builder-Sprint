package com.ai_builder_hackathon.gttgtt.domain.repository

import com.ai_builder_hackathon.gttgtt.domain.model.Comment
import com.ai_builder_hackathon.gttgtt.domain.model.MemoryDetail

interface MemoryRepository {
    suspend fun getDetail(memoryId: String): Result<MemoryDetail>

    /**
     * 댓글 추가. 등록된 댓글을 돌려준다.
     *
     * 그룹 멤버는 전원 동일 권한이라 다른 사람의 기억에도 누구나 댓글을 달 수 있다 (CLAUDE.md §6.1).
     */
    suspend fun addComment(memoryId: String, text: String): Result<Comment>
}
