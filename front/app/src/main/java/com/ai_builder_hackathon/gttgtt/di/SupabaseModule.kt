package com.ai_builder_hackathon.gttgtt.di

import com.ai_builder_hackathon.gttgtt.BuildConfig
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.createSupabaseClient
import io.github.jan.supabase.auth.Auth
import io.github.jan.supabase.functions.Functions
import io.github.jan.supabase.postgrest.Postgrest
import io.github.jan.supabase.storage.Storage
import javax.inject.Singleton

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
            install(Auth)
            install(Postgrest)
            install(Storage)
            install(Functions)
        }
    }
}
