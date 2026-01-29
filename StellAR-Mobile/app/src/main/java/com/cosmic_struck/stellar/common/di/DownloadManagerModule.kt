package com.cosmic_struck.stellar.common.di

import android.app.DownloadManager
import android.content.Context
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DownloadManagerModule {
    @Provides
    @Singleton
    fun provideDownloadManager(@ApplicationContext context: Context) : DownloadManager{
        return context.getSystemService(DownloadManager::class.java)
    }
}