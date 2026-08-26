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
        QwDatabase.getInstance(context)
}
