package com.ai_builder_hackathon.gttgtt.data.remote

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.net.Uri
import androidx.core.graphics.scale
import androidx.core.net.toUri
import androidx.exifinterface.media.ExifInterface
import com.ai_builder_hackathon.gttgtt.domain.model.Photo
import dagger.hilt.android.qualifiers.ApplicationContext
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.storage.storage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
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
 *
 * ⚠️ 업로드 전 항상 [compressToJpeg] 로 리사이즈·압축한다 — 갤러리 원본을 그대로 올리면
 * 한 장에 몇 MB씩이라, 92~170dp 썸네일 하나 그리는 데도 그 원본을 통째로 내려받게 되어
 * 피드가 느려진다. 압축 결과는 항상 JPEG 라 경로 확장자도 항상 `.jpg` 로 고정한다.
 */
@Singleton
class MediaUploader @Inject constructor(
    private val supabase: SupabaseClient,
    @ApplicationContext private val context: Context,
) {

    /**
     * 기억 사진 업로드. 경로: `{archive_id}/{memory_id}/{uuid}.jpg`
     * @return 업로드된 storage path 목록 (실패한 건은 제외)
     */
    suspend fun uploadMemoryPhotos(
        archiveId: String,
        memoryId: String,
        uris: List<String>,
    ): List<String> = withContext(Dispatchers.IO) {
        uris.mapNotNull { uri ->
            runCatching {
                val path = "$archiveId/$memoryId/${UUID.randomUUID()}.jpg"
                supabase.storage.from(BUCKET).upload(path, compressToJpeg(uri, PHOTO_MAX_DIMENSION))
                path
            }.getOrNull()
        }
    }

    /** 채팅 사진 업로드. 경로: `{archive_id}/chat/{uuid}.jpg` */
    suspend fun uploadChatPhoto(archiveId: String, uri: String): String? =
        withContext(Dispatchers.IO) {
            runCatching {
                val path = "$archiveId/chat/${UUID.randomUUID()}.jpg"
                supabase.storage.from(BUCKET).upload(path, compressToJpeg(uri, PHOTO_MAX_DIMENSION))
                path
            }.getOrNull()
        }

    /** 그룹 대표 사진 업로드. 경로: `{archive_id}/cover/{uuid}.jpg` */
    suspend fun uploadGroupCoverImage(archiveId: String, uri: String): String? =
        withContext(Dispatchers.IO) {
            runCatching {
                val path = "$archiveId/cover/${UUID.randomUUID()}.jpg"
                supabase.storage.from(BUCKET).upload(path, compressToJpeg(uri, COVER_MAX_DIMENSION))
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
        storagePaths.map { path -> async { toPhoto(path) } }.awaitAll()
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

    // ── 프로필 아바타 (별도 버킷) ──
    // 'memories' 버킷은 경로 1단계가 archive_id 라 RLS 가 is_member(archive_id) 로 격리한다.
    // 아바타는 archive 소속이 아니라 사용자 전역 소지물이라 같은 버킷을 못 쓴다 —
    // 대신 'avatars' 버킷에 {user_id}/{uuid}.{ext} 로 올리고, 소유자만 쓰기 허용한다
    // (마이그레이션 20260730150000_avatar_storage.sql).

    /** 프로필 사진 업로드. 경로: `{user_id}/{uuid}.jpg` */
    suspend fun uploadAvatar(userId: String, uri: String): String? =
        withContext(Dispatchers.IO) {
            runCatching {
                val path = "$userId/${UUID.randomUUID()}.jpg"
                supabase.storage.from(AVATAR_BUCKET).upload(path, compressToJpeg(uri, AVATAR_MAX_DIMENSION))
                path
            }.getOrNull()
        }

    /**
     * 아바타 signed URL. 프로필 조회·업로드 직후 화면에 바로 띄울 때 쓴다.
     *
     * ⚠️ 캐싱 없이 매번 새로 발급하면 같은 아바타인데도 토큰이 계속 바뀌어, URL 자체를
     * 캐시 키로 쓰는 Coil이 화면을 오갈 때마다 매번 새로 받는다 — 피드를 스크롤할 때마다
     * 멤버 아바타가 다시 로딩되는 게 가장 크게 체감된다. [signedUrlOrNull] 과 같은 TTL 캐시를 쓴다.
     */
    private val avatarSignedUrlCache = ConcurrentHashMap<String, CachedSignedUrl>()

    suspend fun avatarSignedUrl(storagePath: String): String? {
        val now = System.currentTimeMillis()
        val cached = avatarSignedUrlCache[storagePath]
        if (cached != null && cached.expiresAtMillis - now > SIGNED_URL_REFRESH_MARGIN.inWholeMilliseconds) {
            return cached.url
        }

        return withContext(Dispatchers.IO) {
            runCatching {
                supabase.storage.from(AVATAR_BUCKET).createSignedUrl(storagePath, SIGNED_URL_TTL)
            }.onSuccess { url ->
                avatarSignedUrlCache[storagePath] = CachedSignedUrl(url, now + SIGNED_URL_TTL.inWholeMilliseconds)
            }.getOrNull()
        }
    }

    /** 이전 아바타 오브젝트를 지운다 — 갱신 뒤 orphan 정리용. 실패해도 예외를 던지지 않는다. */
    suspend fun deleteAvatarObject(storagePath: String) = withContext(Dispatchers.IO) {
        runCatching { supabase.storage.from(AVATAR_BUCKET).delete(storagePath) }
    }

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

    /**
     * 갤러리 원본을 [maxDimension] 이하로 줄이고 JPEG 로 다시 인코딩한다.
     *
     * 1) `inJustDecodeBounds` 로 크기만 먼저 읽는다 — 12MP짜리 원본을 무작정 통째로
     *    비트맵에 올리면 그 자체로 느리고 메모리도 많이 먹는다.
     * 2) 그 크기에 맞춰 `inSampleSize` 를 계산해 축소된 크기로 디코드한다(정확히 맞지
     *    않을 수 있어 3)에서 한 번 더 다듬는다).
     * 3) EXIF 회전을 픽셀에 직접 반영한다 — 재인코딩하면 EXIF 태그가 사라지므로,
     *    안 그러면 카메라로 세로로 찍은 사진이 서버에는 옆으로 누워 올라간다.
     * 4) 그래도 [maxDimension] 을 넘으면 정확히 그 크기로 한 번 더 줄인다.
     */
    private fun compressToJpeg(uri: String, maxDimension: Int): ByteArray {
        val contentUri = uri.toUri()

        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        context.contentResolver.openInputStream(contentUri)?.use { input ->
            BitmapFactory.decodeStream(input, null, bounds)
        } ?: error("사진을 읽을 수 없습니다: $uri")

        val sampleSize = calculateInSampleSize(bounds.outWidth, bounds.outHeight, maxDimension)
        val sampleOptions = BitmapFactory.Options().apply { inSampleSize = sampleSize }
        val sampled = context.contentResolver.openInputStream(contentUri)?.use { input ->
            BitmapFactory.decodeStream(input, null, sampleOptions)
        } ?: error("사진을 읽을 수 없습니다: $uri")

        val rotated = rotateToUpright(sampled, contentUri)
        val resized = downscaleIfNeeded(rotated, maxDimension)

        return ByteArrayOutputStream().use { output ->
            resized.compress(Bitmap.CompressFormat.JPEG, JPEG_QUALITY, output)
            // 중간 비트맵들은 재사용되지 않으면 바로 회수한다 — 사진 여러 장을 연달아
            // 압축할 때(uploadMemoryPhotos) 하나씩 정리해야 메모리가 안 쌓인다.
            if (resized !== rotated) rotated.recycle()
            if (rotated !== sampled) sampled.recycle()
            resized.recycle()
            output.toByteArray()
        }
    }

    /** 긴 변이 [maxDimension] 의 절반 이하가 될 때까지 2배씩 줄인다 — BitmapFactory 는 2의 거듭제곱만 받는다. */
    private fun calculateInSampleSize(width: Int, height: Int, maxDimension: Int): Int {
        var sampleSize = 1
        var w = width
        var h = height
        while (w / 2 >= maxDimension || h / 2 >= maxDimension) {
            w /= 2
            h /= 2
            sampleSize *= 2
        }
        return sampleSize
    }

    /** EXIF 회전 태그가 있으면 그만큼 픽셀 자체를 돌려서 원본 없이도 똑바로 보이게 만든다. */
    private fun rotateToUpright(bitmap: Bitmap, uri: Uri): Bitmap {
        val degrees = runCatching {
            context.contentResolver.openInputStream(uri)?.use { ExifInterface(it).rotationDegrees } ?: 0
        }.getOrDefault(0)

        if (degrees == 0) return bitmap

        val matrix = Matrix().apply { postRotate(degrees.toFloat()) }
        return Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
    }

    /** inSampleSize 는 2의 거듭제곱 단위라 정확히 안 맞을 수 있다 — 여기서 목표 크기로 딱 맞춘다. */
    private fun downscaleIfNeeded(bitmap: Bitmap, maxDimension: Int): Bitmap {
        val largerSide = maxOf(bitmap.width, bitmap.height)
        if (largerSide <= maxDimension) return bitmap

        val scale = maxDimension.toFloat() / largerSide
        val targetWidth = (bitmap.width * scale).toInt().coerceAtLeast(1)
        val targetHeight = (bitmap.height * scale).toInt().coerceAtLeast(1)
        return bitmap.scale(targetWidth, targetHeight)
    }

    /** 경로 해시로 고정 배정 — 같은 사진은 항상 같은 fallback 색을 갖는다. */
    private fun fallbackFor(path: String) =
        com.ai_builder_hackathon.gttgtt.domain.model.GradientTheme.entries[
            kotlin.math.abs(path.hashCode()) %
                com.ai_builder_hackathon.gttgtt.domain.model.GradientTheme.entries.size
        ]

    private companion object {
        const val BUCKET = "memories"
        const val AVATAR_BUCKET = "avatars"

        /** signed URL 유효기간. 화면을 오래 열어둬도 사진이 깨지지 않을 만큼. */
        val SIGNED_URL_TTL = 2.hours

        /** 만료 10분 전부터는 캐시를 못 미더워하고 새로 발급받는다. */
        val SIGNED_URL_REFRESH_MARGIN = 10.minutes

        /** JPEG 압축 품질. 85 정도면 육안으로는 원본과 거의 구분이 안 되면서 용량은 크게 준다. */
        const val JPEG_QUALITY = 85

        /** 기억/채팅 사진 — 전체화면 뷰어(PhotoViewerDialog)에서 봐도 흐리지 않을 정도. */
        const val PHOTO_MAX_DIMENSION = 1600

        /** 그룹 대표 사진 — 목록/헤더 썸네일로만 쓰여서 사진보다 작아도 된다. */
        const val COVER_MAX_DIMENSION = 1200

        /** 아바타 — 가장 크게 써도 마이페이지 헤더(92dp) 수준이라 훨씬 작아도 충분하다. */
        const val AVATAR_MAX_DIMENSION = 512
    }
}
