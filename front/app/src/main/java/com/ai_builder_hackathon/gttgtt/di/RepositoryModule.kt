package com.ai_builder_hackathon.gttgtt.di

import com.ai_builder_hackathon.gttgtt.data.remote.AndroidPhotoMetadataReader
import com.ai_builder_hackathon.gttgtt.data.repository.SupabaseAiChatRepository
import com.ai_builder_hackathon.gttgtt.data.repository.SupabaseArchiveRepository
import com.ai_builder_hackathon.gttgtt.data.repository.SupabaseChatRepository
import com.ai_builder_hackathon.gttgtt.data.repository.SupabaseMemoryRepository
import com.ai_builder_hackathon.gttgtt.data.repository.SupabasePostRepository
import com.ai_builder_hackathon.gttgtt.data.repository.SupabaseProfileRepository
import com.ai_builder_hackathon.gttgtt.domain.repository.AiChatRepository
import com.ai_builder_hackathon.gttgtt.domain.repository.ArchiveRepository
import com.ai_builder_hackathon.gttgtt.domain.repository.ChatRepository
import com.ai_builder_hackathon.gttgtt.domain.repository.MemoryRepository
import com.ai_builder_hackathon.gttgtt.domain.repository.PhotoMetadataReader
import com.ai_builder_hackathon.gttgtt.domain.repository.PostRepository
import com.ai_builder_hackathon.gttgtt.domain.repository.ProfileRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * Repository 구현체 바인딩. 한 줄씩 바꿔 Fake ↔ Supabase 를 스위칭한다.
 *
 * 로그인이 실제로 붙어서(구글 네이티브 로그인) RLS 가 걸리는 5개는 Supabase 로 전환했다.
 *
 * ⚠️ AiChatRepository 만 아직 Fake 다 — `chat`/`embed-memory` Edge Function 배포 여부가
 * 확인되지 않았다. 배포가 확인되면 SupabaseAiChatRepository 로 바꾼다 (작성은 이미 끝남).
 */
@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {

    @Binds
    @Singleton
    abstract fun bindArchiveRepository(impl: SupabaseArchiveRepository): ArchiveRepository

    @Binds
    @Singleton
    abstract fun bindPostRepository(impl: SupabasePostRepository): PostRepository

    @Binds
    @Singleton
    abstract fun bindChatRepository(impl: SupabaseChatRepository): ChatRepository

    // chat/embed-memory Edge Function 배포 확인됨 → 실제 Solar Pro 3 연동으로 전환.
    @Binds
    @Singleton
    abstract fun bindAiChatRepository(impl: SupabaseAiChatRepository): AiChatRepository

    @Binds
    @Singleton
    abstract fun bindProfileRepository(impl: SupabaseProfileRepository): ProfileRepository

    @Binds
    @Singleton
    abstract fun bindMemoryRepository(impl: SupabaseMemoryRepository): MemoryRepository

    @Binds
    @Singleton
    abstract fun bindPhotoMetadataReader(impl: AndroidPhotoMetadataReader): PhotoMetadataReader
}
