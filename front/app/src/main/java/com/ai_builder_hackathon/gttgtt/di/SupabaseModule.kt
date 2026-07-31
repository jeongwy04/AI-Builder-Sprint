package com.ai_builder_hackathon.gttgtt.di

import com.ai_builder_hackathon.gttgtt.BuildConfig
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.annotations.SupabaseInternal
import io.github.jan.supabase.createSupabaseClient
import io.github.jan.supabase.auth.Auth
import io.github.jan.supabase.compose.auth.ComposeAuth
import io.github.jan.supabase.compose.auth.googleNativeLogin
import io.github.jan.supabase.functions.Functions
import io.github.jan.supabase.postgrest.Postgrest
import io.github.jan.supabase.storage.Storage
import io.ktor.client.plugins.HttpTimeout
import javax.inject.Singleton
import kotlin.time.Duration.Companion.seconds

/**
 * SupabaseClient는 이 모듈에서만 생성한다.
 * Composable이 이 타입을 주입받는 일은 없어야 한다 (CLAUDE.md §5.3).
 *
 * anon 키만 사용한다. service_role 키가 앱에 들어가는 순간 RLS가 무의미해진다 (§5.1, §14).
 */
@Module
@InstallIn(SingletonComponent::class)
object SupabaseModule {

    @Provides
    @Singleton
    @OptIn(SupabaseInternal::class)
    fun provideSupabaseClient(): SupabaseClient {
        // local.properties 누락 시 조용히 빈 문자열로 도는 것보다 즉시 실패하는 편이 낫다.
        require(BuildConfig.SUPABASE_URL.isNotBlank()) {
            "SUPABASE_URL 이 비어 있다. front/local.properties 를 확인할 것."
        }
        require(BuildConfig.SUPABASE_ANON_KEY.isNotBlank()) {
            "SUPABASE_ANON_KEY 가 비어 있다. front/local.properties 를 확인할 것."
        }

        return createSupabaseClient(
            supabaseUrl = BuildConfig.SUPABASE_URL,
            supabaseKey = BuildConfig.SUPABASE_ANON_KEY,
        ) {
            // supabase-kt 기본 요청 타임아웃(requestTimeout)은 10초다 — `chat` Edge Function 은
            // Solar Pro 3 를 function calling 으로 호출해(검색 도구까지 왕복하면) 10초를 넘기기
            // 쉬워서 "request timeout has expired" 로 자주 실패했다. CLAUDE.md §8.2 가 명시한
            // 20초로 늘린다 (Postgrest/Storage 등 다른 호출에도 같이 적용되지만, 그쪽은
            // 원래도 10초 안에 끝나니 체감 영향은 없다).
            requestTimeout = 20.seconds
            // ⚠️ requestTimeout 만으로는 부족했다 — 스트리밍 없이(§8.2) 서버가 응답 바이트를
            // 하나도 안 보내는 동안, 실제로 걸리는 건 requestTimeoutMillis(총 시간 상한)가 아니라
            // OkHttp 엔진 자체의 기본 read timeout(10초, "아무 데이터도 안 오면 끊는다")이다.
            // requestTimeout 을 늘려도 "Socket timeout has expired" 로 여전히 끊겼던 이유.
            // socketTimeoutMillis 를 명시적으로 늘려야 실제로 해결된다.
            httpConfig {
                install(HttpTimeout) {
                    socketTimeoutMillis = 20_000L
                    connectTimeoutMillis = 15_000L
                }
            }
            install(Auth)
            install(Postgrest)
            install(Storage)
            install(Functions)
            install(ComposeAuth) {
                // Web 클라이언트 ID를 넣는다 (Android 클라이언트 ID 아님) — Credential Manager 가
                // 이 값을 audience 로 하는 ID 토큰을 요구한다. Google Cloud Console에 등록된
                // Android OAuth 클라이언트(SHA-1)는 "이 앱+서명이 맞는지" 검증에만 쓰이고 코드엔 안 들어간다.
                googleNativeLogin(serverClientId = BuildConfig.GOOGLE_WEB_CLIENT_ID)
            }
        }
    }
}
