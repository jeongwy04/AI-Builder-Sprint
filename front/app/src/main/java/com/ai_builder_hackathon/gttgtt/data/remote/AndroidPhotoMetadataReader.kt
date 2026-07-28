package com.ai_builder_hackathon.gttgtt.data.remote

import android.content.Context
import androidx.core.net.toUri
import androidx.exifinterface.media.ExifInterface
import com.ai_builder_hackathon.gttgtt.domain.repository.PhotoMetadataReader
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import javax.inject.Inject

/**
 * EXIF 에서 촬영일을 읽는다.
 *
 * 갤러리 사진은 content:// URI 라 파일 경로로 접근할 수 없어 InputStream 으로 읽는다.
 * (framework 의 android.media.ExifInterface 는 스트림 입력을 제대로 못 다뤄 androidx 판을 쓴다.)
 */
class AndroidPhotoMetadataReader @Inject constructor(
    @ApplicationContext private val context: Context,
) : PhotoMetadataReader {

    override suspend fun readCapturedAtMillis(uri: String): Long? = withContext(Dispatchers.IO) {
        runCatching {
            context.contentResolver.openInputStream(uri.toUri())?.use { stream ->
                val exif = ExifInterface(stream)
                val raw = exif.getAttribute(ExifInterface.TAG_DATETIME_ORIGINAL)
                    ?: exif.getAttribute(ExifInterface.TAG_DATETIME)
                    ?: return@use null

                LocalDateTime.parse(raw, EXIF_FORMATTER)
                    .atZone(ZoneId.systemDefault())
                    .toInstant()
                    .toEpochMilli()
            }
        }.getOrNull() // EXIF 가 없거나 형식이 깨진 사진은 흔하다. 실패는 null 로 흘린다.
    }

    private companion object {
        /** EXIF 표준 표기: "2025:12:22 18:31:04" */
        val EXIF_FORMATTER: DateTimeFormatter = DateTimeFormatter.ofPattern("yyyy:MM:dd HH:mm:ss")
    }
}
