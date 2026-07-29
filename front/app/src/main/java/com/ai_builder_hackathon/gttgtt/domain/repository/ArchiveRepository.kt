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
     * 그룹 하나만 필요한 화면(그룹 피드/채팅 상단바)에서 쓴다.
     * [getMyArchives] 를 통째로 다시 불러와 그중 하나만 골라 쓰면, 화면 하나 열 때마다
     * 내 모든 그룹의 마지막 메시지를 전부 다시 조회하는 낭비가 생긴다 — 그래서 따로 둔다.
     */
    suspend fun getArchive(archiveId: String): Result<ArchiveSummary>

    /**
     * 새 그룹을 만들고 나를 첫 멤버로 넣는다.
     * ⚠️ Supabase 구현은 `archives` 에 직접 insert 하지 않고 `create_archive` RPC 로만 만든다
     * (직접 insert 시 RLS readback 함정에 걸린다 — 백엔드 계약).
     */
    suspend fun createArchive(name: String, groupType: GroupType): Result<ArchiveSummary>

    /**
     * 그룹 이름 변경. `archives_update_member` 정책이 `is_member(id)` 라서
     * 역할 구분 없이 멤버 누구나 바꿀 수 있다 (CLAUDE.md §6.1 — 역할 개념 없음).
     */
    suspend fun renameArchive(archiveId: String, newName: String): Result<Unit>

    /**
     * 그룹(보관소) 삭제. `archives_delete_member` 정책도 `is_member(id)` 라 멤버 누구나 지울 수 있다.
     * memberships/memories/notes/… 는 전부 `archive_id` FK 에 `on delete cascade` 가 걸려 있어
     * archives 한 행만 지우면 하위 데이터가 함께 정리된다 (마이그레이션 §2~8 참고).
     */
    suspend fun deleteArchive(archiveId: String): Result<Unit>

    /**
     * 초대 토큰을 새로 만든다. 반환값은 공유할 토큰 문자열.
     * 토큰 자체는 `invitations` 테이블의 DB 기본값(`encode(gen_random_bytes(16),'hex')`)이 채운다.
     */
    suspend fun createInvitation(archiveId: String): Result<String>

    /**
     * 초대 토큰으로 멤버십을 등록한다. `accept_invitation` RPC 를 그대로 감싼 것.
     * 성공 시 가입한 archiveId 를 반환한다.
     */
    suspend fun joinArchiveByToken(token: String): Result<String>

    /**
     * 멤버 id → 표시 이름. "함께한 사람" 선택 등, 이름이 필요한데 목록 화면처럼
     * 마지막 메시지 미리보기까지는 필요 없는 곳에서 쓴다.
     */
    suspend fun getMemberNames(memberIds: List<String>): Result<Map<String, String>>
}
