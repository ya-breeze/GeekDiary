package com.example.geekdiary.di

import android.content.Context
import android.content.SharedPreferences
import androidx.room.Room
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import com.example.geekdiary.data.local.DiaryDatabase
import com.example.geekdiary.data.local.dao.AssetDao
import com.example.geekdiary.data.local.dao.DiaryEntryDao
import com.example.geekdiary.data.local.dao.PendingAssetDownloadDao
import com.example.geekdiary.data.local.dao.PendingAssetUploadDao
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

    @Provides
    fun provideAssetDao(database: DiaryDatabase): AssetDao {
        return database.assetDao()
    }

    @Provides
    fun providePendingAssetDownloadDao(database: DiaryDatabase): PendingAssetDownloadDao {
        return database.pendingAssetDownloadDao()
    }

    @Provides
    fun providePendingAssetUploadDao(database: DiaryDatabase): PendingAssetUploadDao {
        return database.pendingAssetUploadDao()
    }

    @Provides
    @Singleton
    fun provideSharedPreferences(@ApplicationContext context: Context): SharedPreferences {
        val masterKey = MasterKey.Builder(context)
            .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
            .build()

        return EncryptedSharedPreferences.create(
            context,
            "diary_settings",
            masterKey,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
        )
    }
}
