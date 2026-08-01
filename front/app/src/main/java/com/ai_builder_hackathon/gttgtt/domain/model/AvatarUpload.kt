package com.ai_builder_hackathon.gttgtt.domain.model

/**
 * 프로필 사진 업로드 결과.
 *
 * @param url  화면에 바로 띄울 수 있는 signed URL (조회 때마다 새로 발급되는 값).
 * @param path Storage 오브젝트 경로 (profiles.avatar_url 에 저장되는 값).
 */
data class AvatarUpload(
    val url: String,
    val path: String,
)
