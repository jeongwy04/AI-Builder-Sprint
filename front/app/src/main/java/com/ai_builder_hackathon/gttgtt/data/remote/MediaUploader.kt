package com.ai_builder_hackathon.gttgtt.data.remote

import android.content.Context
import androidx.core.net.toUri
import com.ai_builder_hackathon.gttgtt.domain.model.Photo
import dagger.hilt.android.qualifiers.ApplicationContext
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.storage.storage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.time.Duration.Companion.hours

/**
 * Storage 업로드와 signed URL 발급을 한곳에 모은다.
 *
 * 버킷은 private 이라 조회는 반드시 signed URL 로만 한다 (CLAUDE.md §5.2).
 * 경로 규칙도 여기서만 만든다 — 여러 Repository 가 각자 조립하면 규칙이 어긋난다.
 */
@Singleton
class MediaUploader @Inject constructor(
    private val supabase: SupabaseClient,
    @ApplicationContext private val context: Context,
) {

    /**
     * 기억 사진 업로드. 경로: `{archive_id}/{memory_id}/{uuid}.{ext}`
     * @return 업로드된 storage path 목록 (실패한 건은 제외)
     */
    suspend fun uploadMemoryPhotos(
        archiveId: String,
        memoryId: String,
        uris: List<String>,
    ): List<String> = withContext(Dispatchers.IO) {
        uris.mapNotNull { uri ->
            runCatching {
                val path = "$archiveId/$memoryId/${UUID.randomUUID()}.${extensionOf(uri)}"
                supabase.storage.from(BUCKET).upload(path, readBytes(uri))
                path
            }.getOrNull()
        }
    }

    /** 채팅 사진 업로드. 경로: `{archive_id}/chat/{uuid}.{ext}` */
    suspend fun uploadChatPhoto(archiveId: String, uri: String): String? =
        withContext(Dispatchers.IO) {
            runCatching {
                val path = "$archiveId/chat/${UUID.randomUUID()}.${extensionOf(uri)}"
                supabase.storage.from(BUCKET).upload(path, readBytes(uri))
                path
            }.getOrNull()
        }

    /**
     * storage path → 화면에 쓸 [Photo].
     * URL 발급에 실패해도 화면이 깨지지 않도록 fallback 그라디언트가 남는다.
     */
    suspend fun toPhoto(storagePath: String): Photo = Photo(
        url = signedUrlOrNull(storagePath),
        fallback = fallbackFor(storagePath),
    )

    suspend fun toPhotos(storagePaths: List<String>): List<Photo> =
        storagePaths.map { toPhoto(it) }

    private suspend fun signedUrlOrNull(path: String): String? =
        runCatching {
            supabase.storage.from(BUCKET).createSignedUrl(path, SIGNED_URL_TTL)
        }.getOrNull()

    private fun readBytes(uri: String): ByteArray =
        context.contentResolver.openInputStream(uri.toUri())?.use { it.readBytes() }
            ?: error("사진을 읽을 수 없습니다: $uri")

    private fun extensionOf(uri: String): String =
        context.contentResolver.getType(uri.toUri())
            ?.substringAfterLast('/')
            ?.takeIf { it.isNotBlank() }
            ?: "jpg"

    /** 경로 해시로 고정 배정 — 같은 사진은 항상 같은 fallback 색을 갖는다. */
    private fun fallbackFor(path: String) =
        com.ai_builder_hackathon.gttgtt.domain.model.GradientTheme.entries[
            kotlin.math.abs(path.hashCode()) %
                com.ai_builder_hackathon.gttgtt.domain.model.GradientTheme.entries.size
        ]

    private companion object {
        const val BUCKET = "memories"

        /** signed URL 유효기간. 화면을 오래 열어둬도 사진이 깨지지 않을 만큼. */
        val SIGNED_URL_TTL = 2.hours
    }
}
