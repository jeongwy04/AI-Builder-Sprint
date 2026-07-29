package com.ai_builder_hackathon.gttgtt.domain.repository

import com.ai_builder_hackathon.gttgtt.domain.model.UserProfile

interface ProfileRepository {
    suspend fun getMyProfile(): Result<UserProfile>

    /** 한 줄 상태 메시지를 저장한다. 빈 문자열이면 기본 문구로 되돌린다. */
    suspend fun updateStatus(status: String): Result<Unit>

    suspend fun signOut(): Result<Unit>

    /**
     * 회원가입 때 받은 닉네임을 `profiles.display_name` 에 반영한다.
     * 구글 로그인 자체는 이름을 안 물어보므로(트리거가 구글 계정 이름/이메일 앞부분으로
     * 기본값만 채운다), 로그인 성공 직후 이 메서드로 사용자가 정한 닉네임으로 덮어쓴다.
     *
     * ⚠️ 닉네임은 `profiles_display_name_unique_idx` 로 대소문자 구분 없이 유니크하다.
     * 이미 다른 사람이 쓰고 있으면 실패(Result.failure)하고, 메시지는 이미
     * "이미 사용 중인 닉네임이에요." 처럼 사용자에게 그대로 보여줄 수 있게 다듬어져 있다.
     */
    suspend fun updateNickname(nickname: String): Result<Unit>

    /**
     * 이 닉네임을 이미 다른 사람이 쓰고 있는지 확인한다.
     * 회원가입 화면이 계정을 만들기 *전에* 먼저 물어보는 용도라 로그인 전(anon) 상태에서도
     * 호출할 수 있어야 한다 — `is_nickname_taken` RPC(security definer)가 이를 지원한다.
     * 이 확인 자체가 실패해도(네트워크 등) 가입을 막을 필요는 없다 — 최종 방어선은
     * [updateNickname] 의 유니크 제약이다.
     */
    suspend fun isNicknameTaken(nickname: String): Result<Boolean>
}
