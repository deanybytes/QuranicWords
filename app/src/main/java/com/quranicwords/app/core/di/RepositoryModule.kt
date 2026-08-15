package com.quranicwords.app.core.di

import com.quranicwords.app.core.data.repository.BackupRepositoryImpl
import com.quranicwords.app.core.data.repository.ContentRepositoryImpl
import com.quranicwords.app.core.data.repository.ProgressRepositoryImpl
import com.quranicwords.app.core.data.repository.WordAudioRepositoryImpl
import com.quranicwords.app.core.domain.repository.BackupRepository
import com.quranicwords.app.core.domain.repository.ContentRepository
import com.quranicwords.app.core.domain.repository.ProgressRepository
import com.quranicwords.app.core.domain.repository.WordAudioRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {
    @Binds
    abstract fun bindContentRepository(impl: ContentRepositoryImpl): ContentRepository

    @Binds
    abstract fun bindProgressRepository(impl: ProgressRepositoryImpl): ProgressRepository

    @Binds
    abstract fun bindBackupRepository(impl: BackupRepositoryImpl): BackupRepository

    @Binds
    abstract fun bindWordAudioRepository(impl: WordAudioRepositoryImpl): WordAudioRepository
}
