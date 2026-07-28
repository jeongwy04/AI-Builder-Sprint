package com.ai_builder_hackathon.gttgtt.di

import com.ai_builder_hackathon.gttgtt.data.repository.FakeArchiveRepository
import com.ai_builder_hackathon.gttgtt.domain.repository.ArchiveRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * Repository 구현체 바인딩.
 * 실제 Supabase 구현이 생기면 여기 한 줄만 바꾸면 된다 — ViewModel은 손대지 않는다.
 */
@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {

    @Binds
    @Singleton
    abstract fun bindArchiveRepository(impl: FakeArchiveRepository): ArchiveRepository
}
