package com.ai_builder_hackathon.gttgtt.domain.repository

/**
 * 사진 파일에서 촬영 정보를 읽는다.
 *
 * 인터페이스로 빼둔 이유: 실제 구현은 Android Context 가 필요한데,
 * ViewModel 이 Context 에 묶이면 테스트가 어려워진다 (CLAUDE.md §15).
 */
interface PhotoMetadataReader {
    /**
     * EXIF 촬영일(epoch millis). 없으면 null.
     * @param uri content:// 형태의 사진 위치
     */
    suspend fun readCapturedAtMillis(uri: String): Long?
}
