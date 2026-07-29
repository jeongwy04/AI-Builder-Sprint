package com.ai_builder_hackathon.gttgtt.data.remote

import android.content.Context
import androidx.core.net.toUri
import com.ai_builder_hackathon.gttgtt.domain.model.Photo
import dagger.hilt.android.qualifiers.ApplicationContext
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.storage.storage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.withContext
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.time.Duration.Companion.hours
import kotlin.time.Duration.Companion.minutes

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

    /** 그룹 대표 사진 업로드. 경로: `{archive_id}/cover/{uuid}.{ext}` */
    suspend fun uploadGroupCoverImage(archiveId: String, uri: String): String? =
        withContext(Dispatchers.IO) {
            runCatching {
                val path = "$archiveId/cover/${UUID.randomUUID()}.${extensionOf(uri)}"
                supabase.storage.from(BUCKET).upload(path, readBytes(uri))
                path
            }.getOrNull()
        }

    /**
     * storage path → 화면에 쓸 [Photo].
     * URL 발급에 실패해도 화면이 깨지지 않도록 fallback 그라디언트가 남는다.
     */
    suspend fun toPhoto(storagePath: String): Photo = Photo(
        storagePath = storagePath,
        url = signedUrlOrNull(storagePath),
        fallback = fallbackFor(storagePath),
    )

    /**
     * 여러 장을 한꺼번에 변환한다 — 사진마다 signed URL 발급을 순차로 기다리면
     * 사진이 5장이면 네트워크 왕복이 5번 이어져 화면이 그만큼 늦게 뜬다. 병렬로 발급받는다.
     */
    suspend fun toPhotos(storagePaths: List<String>): List<Photo> = coroutineScope {
        storagePaths.map { path -> async { toPhoto(path) } }.map { it.await() }
    }

    /**
     * media_assets 행 id 까지 아는 경우(기억 상세/수정)에 쓴다.
     * id 가 있어야 나중에 "이 사진을 지워줘" 요청에서 어떤 media_assets 행인지 식별할 수 있다.
     */
    suspend fun toPhoto(id: String, storagePath: String): Photo = Photo(
        id = id,
        storagePath = storagePath,
        url = signedUrlOrNull(storagePath),
        fallback = fallbackFor(storagePath),
    )

    /**
     * storage 오브젝트를 지운다 — 기억 사진, 그룹 표지 사진 등 이 버킷의 어떤 경로든 쓸 수 있다.
     * DB 행(media_assets 등) 자체는 호출한 쪽(Repository)이 지운다 — 여기선 Storage 만 담당한다.
     * 실패해도(이미 지워졌거나 네트워크 문제) 예외를 던지지 않는다 — DB 쪽 정리가 더 중요하다.
     */
    suspend fun deleteStorageObjects(storagePaths: List<String>) = withContext(Dispatchers.IO) {
        if (storagePaths.isEmpty()) return@withContext
        // supabase-kt storage 의 delete() 는 vararg String 이라 List 를 그대로 넘길 수 없다 — 스프레드로 푼다.
        runCatching { supabase.storage.from(BUCKET).delete(*storagePaths.toTypedArray()) }
    }

    /**
     * signed URL 문자열만 필요할 때 쓴다 (그룹 표지 사진처럼 [Photo] 전체가 필요 없는 경우).
     * 캐싱/발급 로직은 [toPhoto] 와 동일하게 [signedUrlOrNull] 을 공유한다.
     */
    suspend fun signedUrl(storagePath: String): String? = signedUrlOrNull(storagePath)

    /**
     * signed URL 을 메모리에 캐싱한다. 캐시가 없으면 화면을 나갔다 돌아올 때마다(ON_RESUME 재조회)
     * 이미 유효한 URL도 매번 새로 발급받게 되어, 그만큼 사진이 늦게 뜬다.
     * 만료 [SIGNED_URL_REFRESH_MARGIN] 전에 미리 새로 받아 화면을 오래 띄워놔도 깨지지 않게 한다.
     * 앱 프로세스가 살아있는 동안만 유효한 캐시라 재시작하면 비워진다 — 그 정도로 충분하다.
     */
    private val signedUrlCache = ConcurrentHashMap<String, CachedSignedUrl>()

    private suspend fun signedUrlOrNull(path: String): String? {
        val now = System.currentTimeMillis()
        val cached = signedUrlCache[path]
        if (cached != null && cached.expiresAtMillis - now > SIGNED_URL_REFRESH_MARGIN.inWholeMilliseconds) {
            return cached.url
        }

        return runCatching {
            supabase.storage.from(BUCKET).createSignedUrl(path, SIGNED_URL_TTL)
        }.onSuccess { url ->
            signedUrlCache[path] = CachedSignedUrl(url, now + SIGNED_URL_TTL.inWholeMilliseconds)
        }.getOrNull()
    }

    private data class CachedSignedUrl(val url: String, val expiresAtMillis: Long)

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

        /** 만료 10분 전부터는 캐시를 못 미더워하고 새로 발급받는다. */
        val SIGNED_URL_REFRESH_MARGIN = 10.minutes
    }
}
