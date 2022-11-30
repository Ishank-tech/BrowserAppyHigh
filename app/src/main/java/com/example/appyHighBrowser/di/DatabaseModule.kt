package com.example.appyHighBrowser.di

import android.content.Context
import com.example.appyHighBrowser.room.AppDatabase
import com.example.appyHighBrowser.room.DownloadDAO
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@InstallIn(SingletonComponent::class)
@Module
class DatabaseModule {
    @Singleton
    @Provides
    fun provideChannelDao(appDatabase: AppDatabase): DownloadDAO {
        return appDatabase.channelDao()
    }

    @Provides
    @Singleton
    fun provideAppDatabase(@ApplicationContext appContext: Context): AppDatabase {
        return AppDatabase(appContext)
    }

}