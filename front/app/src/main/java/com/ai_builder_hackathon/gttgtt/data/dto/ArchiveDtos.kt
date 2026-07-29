package com.ai_builder_hackathon.gttgtt.data.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

// 스키마 원천: docs/SCHEMA_REFERENCE.md (마이그레이션 SQL이 진짜 원천)

/**
 * archives + 내장 memberships 조인.
 * RLS 덕에 내가 멤버인 보관소만 내려온다.
 *
 * select: id,name,group_type,created_at,memberships(user_id)
 */
@Serializable
data class ArchiveWithMembersDto(
    @SerialName("id") val id: String,
    @SerialName("name") val name: String,
    @SerialName("group_type") val groupType: String? = null,
    @SerialName("created_at") val createdAt: String,
    @SerialName("memberships") val memberships: List<MembershipUserDto> = emptyList(),
)

@Serializable
data class MembershipUserDto(
    @SerialName("user_id") val userId: String,
)

/** 그룹 목록 미리보기용 마지막 메시지 1건 */
@Serializable
data class MessagePreviewDto(
    @SerialName("body") val body: String? = null,
    @SerialName("image_path") val imagePath: String? = null,
    @SerialName("sender_id") val senderId: String,
    @SerialName("created_at") val createdAt: String,
)

@Serializable
data class ProfileDto(
    @SerialName("id") val id: String,
    @SerialName("display_name") val displayName: String? = null,
    @SerialName("avatar_url") val avatarUrl: String? = null,
)

/** 회원가입 시 닉네임 반영 — profiles.update() 요청 바디 (display_name 한 컬럼만) */
@Serializable
data class ProfileNicknameUpdate(
    @SerialName("display_name") val displayName: String,
)

/** unread_counts() RPC 응답 행 */
@Serializable
data class UnreadCountDto(
    @SerialName("archive_id") val archiveId: String,
    @SerialName("unread") val unread: Int,
)

/**
 * create_archive RPC 가 돌려주는 archives 1행.
 *
 * ⚠️ archives 에 직접 insert 하지 않는다 — RLS readback 함정이 있어 이 RPC 로만 그룹을 만든다.
 * 요청 파라미터는 이 postgrest-kt 버전이 `rpc(function, JsonObject)` 만 지원해서
 * DTO 대신 `buildJsonObject { }` 로 바로 조립한다 (SupabaseArchiveRepository 참고).
 */
@Serializable
data class ArchiveRowDto(
    @SerialName("id") val id: String,
    @SerialName("name") val name: String,
    @SerialName("group_type") val groupType: String? = null,
    @SerialName("cover_image_path") val coverImagePath: String? = null,
    @SerialName("created_by") val createdBy: String,
    @SerialName("created_at") val createdAt: String,
)

/** 그룹 이름 변경 — archives.update() 요청 바디 (name 한 컬럼만) */
@Serializable
data class ArchiveRenameDto(
    @SerialName("name") val name: String,
)

/** invitations insert 요청 바디. token/expires_at 은 DB 기본값이 채운다. */
@Serializable
data class InvitationInsert(
    @SerialName("archive_id") val archiveId: String,
)

/** invitations insert 응답에서 토큰만 꺼내 쓴다. */
@Serializable
data class InvitationRowDto(
    @SerialName("token") val token: String,
)
