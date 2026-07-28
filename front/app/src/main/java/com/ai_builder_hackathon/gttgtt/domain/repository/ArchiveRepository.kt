package com.ai_builder_hackathon.gttgtt.domain.repository

import com.ai_builder_hackathon.gttgtt.domain.model.ArchiveSummary

interface ArchiveRepository {
    /**
     * 내가 속한 그룹 목록.
     * 예외를 UI까지 던지지 않는다 — 실패는 Result 로 표현한다 (CLAUDE.md §5.3).
     */
    suspend fun getMyArchives(): Result<List<ArchiveSummary>>
}
