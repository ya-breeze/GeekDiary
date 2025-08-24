package com.example.geekdiary.di

import android.content.Context
import androidx.room.Room
import com.example.geekdiary.data.local.DiaryDatabase
import com.example.geekdiary.data.local.dao.DiaryEntryDao
import com.example.geekdiary.data.local.dao.SyncDao
import com.example.geekdiary.data.local.dao.UserDao
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
    fun provideDatabase(@ApplicationContext context: Context): DiaryDatabase {
        return Room.databaseBuilder(
            context.applicationContext,
            DiaryDatabase::class.java,
            DiaryDatabase.DATABASE_NAME
        ).build()
    }
    
    @Provides
    fun provideUserDao(database: DiaryDatabase): UserDao {
        return database.userDao()
    }
    
    @Provides
    fun provideDiaryEntryDao(database: DiaryDatabase): DiaryEntryDao {
        return database.diaryEntryDao()
    }
    
    @Provides
    fun provideSyncDao(database: DiaryDatabase): SyncDao {
        return database.syncDao()
    }
}
