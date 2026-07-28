package com.ai_builder_hackathon.gttgtt.domain.repository

import com.ai_builder_hackathon.gttgtt.domain.model.ArchiveSummary
import com.ai_builder_hackathon.gttgtt.domain.model.GroupType

interface ArchiveRepository {
    /**
     * 내가 속한 그룹 목록.
     * 예외를 UI까지 던지지 않는다 — 실패는 Result 로 표현한다 (CLAUDE.md §5.3).
     */
    suspend fun getMyArchives(): Result<List<ArchiveSummary>>

    /**
     * 새 그룹을 만들고 나를 첫 멤버로 넣는다.
     * ⚠️ Supabase 구현은 `archives` 에 직접 insert 하지 않고 `create_archive` RPC 로만 만든다
     * (직접 insert 시 RLS readback 함정에 걸린다 — 백엔드 계약).
     */
    suspend fun createArchive(name: String, groupType: GroupType): Result<ArchiveSummary>
}
