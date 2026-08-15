package com.quranicwords.app.core.di

import android.content.Context
import androidx.room.Room
import com.quranicwords.app.core.data.local.QwDatabase
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {
    @Provides
    @Singleton
    fun provideQwDatabase(@ApplicationContext context: Context): QwDatabase =
        Room.databaseBuilder(context, QwDatabase::class.java, QwDatabase.DATABASE_NAME)
            // Pre-launch schema, no installed base to preserve - destructive fallback is
            // deliberate here (see QwDatabase.kt's own comment).
            .fallbackToDestructiveMigration(dropAllTables = true)
            .build()
}
