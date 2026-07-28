package com.ai_builder_hackathon.gttgtt.di

import com.ai_builder_hackathon.gttgtt.data.repository.FakeAiChatRepository
import com.ai_builder_hackathon.gttgtt.data.repository.FakeArchiveRepository
import com.ai_builder_hackathon.gttgtt.data.repository.FakeChatRepository
import com.ai_builder_hackathon.gttgtt.data.remote.AndroidPhotoMetadataReader
import com.ai_builder_hackathon.gttgtt.data.repository.FakeMemoryRepository
import com.ai_builder_hackathon.gttgtt.data.repository.FakePostRepository
import com.ai_builder_hackathon.gttgtt.data.repository.FakeProfileRepository
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
 * ⚠️ Supabase 구현은 로그인 세션이 있어야 동작한다 (RLS).
 * 로그인 완성 전까지 Fake 를 유지하고, 완성되면 아래 주석의 구현으로 교체:
 *   - ArchiveRepository → SupabaseArchiveRepository  (작성 완료)
 *   - AiChatRepository  → SupabaseAiChatRepository   (작성 완료)
 *   - MemoryRepository  → SupabaseMemoryRepository   (작성 완료, MediaUploader 필요)
 *   - PostRepository    → SupabasePostRepository     (작성 완료, MediaUploader 필요)
 *   - ChatRepository    → SupabaseChatRepository      (작성 완료, MediaUploader 필요)
 *   - ProfileRepository → SupabaseProfileRepository   (작성 완료)
 * 6개 전부 작성 끝. 로그인 붙으면 이 파일 6줄만 바꾸면 된다.
 */
@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {

    @Binds
    @Singleton
    abstract fun bindArchiveRepository(impl: FakeArchiveRepository): ArchiveRepository

    @Binds
    @Singleton
    abstract fun bindPostRepository(impl: FakePostRepository): PostRepository

    @Binds
    @Singleton
    abstract fun bindChatRepository(impl: FakeChatRepository): ChatRepository

    @Binds
    @Singleton
    abstract fun bindAiChatRepository(impl: FakeAiChatRepository): AiChatRepository

    @Binds
    @Singleton
    abstract fun bindProfileRepository(impl: FakeProfileRepository): ProfileRepository

    @Binds
    @Singleton
    abstract fun bindMemoryRepository(impl: FakeMemoryRepository): MemoryRepository

    @Binds
    @Singleton
    abstract fun bindPhotoMetadataReader(impl: AndroidPhotoMetadataReader): PhotoMetadataReader
}
