package com.example.geekdiary.data.local

import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import android.content.Context
import com.example.geekdiary.data.local.dao.AssetDao
import com.example.geekdiary.data.local.dao.DiaryEntryDao
import com.example.geekdiary.data.local.dao.PendingAssetDownloadDao
import com.example.geekdiary.data.local.dao.PendingAssetUploadDao
import com.example.geekdiary.data.local.dao.SyncDao
import com.example.geekdiary.data.local.dao.UserDao
import com.example.geekdiary.data.local.entity.AssetEntity
import com.example.geekdiary.data.local.entity.DiaryEntryEntity
import com.example.geekdiary.data.local.entity.PendingAssetDownloadEntity
import com.example.geekdiary.data.local.entity.PendingAssetUploadEntity
import com.example.geekdiary.data.local.entity.PendingChangeEntity
import com.example.geekdiary.data.local.entity.SyncStateEntity
import com.example.geekdiary.data.local.entity.UserEntity

@Database(
    entities = [
        UserEntity::class,
        DiaryEntryEntity::class,
        SyncStateEntity::class,
        PendingChangeEntity::class,
        AssetEntity::class,
        PendingAssetDownloadEntity::class,
        PendingAssetUploadEntity::class
    ],
    version = 2,
    exportSchema = false
)
abstract class DiaryDatabase : RoomDatabase() {

    abstract fun userDao(): UserDao
    abstract fun diaryEntryDao(): DiaryEntryDao
    abstract fun syncDao(): SyncDao
    abstract fun assetDao(): AssetDao
    abstract fun pendingAssetDownloadDao(): PendingAssetDownloadDao
    abstract fun pendingAssetUploadDao(): PendingAssetUploadDao
    
    companion object {
        const val DATABASE_NAME = "diary_database"
        
        @Volatile
        private var INSTANCE: DiaryDatabase? = null
        
        fun getDatabase(context: Context): DiaryDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    DiaryDatabase::class.java,
                    DATABASE_NAME
                )
                .fallbackToDestructiveMigration() // For now, will implement proper migrations later
                .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
