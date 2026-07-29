package com.ai_builder_hackathon.gttgtt.domain.repository

import com.ai_builder_hackathon.gttgtt.domain.model.UserProfile

interface ProfileRepository {
    suspend fun getMyProfile(): Result<UserProfile>

    suspend fun signOut(): Result<Unit>

    /**
     * 회원가입 때 받은 닉네임을 `profiles.display_name` 에 반영한다.
     * 구글 로그인 자체는 이름을 안 물어보므로(트리거가 구글 계정 이름/이메일 앞부분으로
     * 기본값만 채운다), 로그인 성공 직후 이 메서드로 사용자가 정한 닉네임으로 덮어쓴다.
     */
    suspend fun updateNickname(nickname: String): Result<Unit>
}
