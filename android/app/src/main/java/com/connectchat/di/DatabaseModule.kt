package com.connectchat.di

import android.content.Context
import androidx.room.Room
import com.connectchat.data.local.AppDatabase
import com.connectchat.data.local.ConversationDao
import com.connectchat.data.local.MessageDao
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
    fun provideAppDatabase(@ApplicationContext context: Context): AppDatabase =
        Room.databaseBuilder(
            context,
            AppDatabase::class.java,
            "connectchat.db"
        ).fallbackToDestructiveMigration().build()

    @Provides
    @Singleton
    fun provideMessageDao(database: AppDatabase): MessageDao = database.messageDao()

    @Provides
    @Singleton
    fun provideConversationDao(database: AppDatabase): ConversationDao = database.conversationDao()
}
