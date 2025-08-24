package com.example.geekdiary.di

import com.example.geekdiary.data.repository.AuthRepositoryImpl
import com.example.geekdiary.data.repository.DiaryRepositoryImpl
import com.example.geekdiary.data.repository.SyncRepositoryImpl
import com.example.geekdiary.domain.repository.AuthRepository
import com.example.geekdiary.domain.repository.DiaryRepository
import com.example.geekdiary.domain.repository.SyncRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {
    
    @Binds
    @Singleton
    abstract fun bindAuthRepository(
        authRepositoryImpl: AuthRepositoryImpl
    ): AuthRepository
    
    @Binds
    @Singleton
    abstract fun bindDiaryRepository(
        diaryRepositoryImpl: DiaryRepositoryImpl
    ): DiaryRepository
    
    @Binds
    @Singleton
    abstract fun bindSyncRepository(
        syncRepositoryImpl: SyncRepositoryImpl
    ): SyncRepository
}
