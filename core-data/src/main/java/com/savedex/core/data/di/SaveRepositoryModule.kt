package com.savedex.core.data.di

import com.savedex.core.data.db.RoomSaveRepository
import com.savedex.core.data.save.DefaultRealSaveAccess
import com.savedex.core.data.save.DefaultSaveBackupStore
import com.savedex.core.domain.RealSaveAccess
import com.savedex.core.domain.SaveBackupStore
import com.savedex.core.domain.SaveRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

@Module
@InstallIn(SingletonComponent::class)
abstract class SaveRepositoryModule {

    @Binds
    abstract fun bindSaveRepository(impl: RoomSaveRepository): SaveRepository

    @Binds
    abstract fun bindRealSaveAccess(impl: DefaultRealSaveAccess): RealSaveAccess

    @Binds
    abstract fun bindSaveBackupStore(impl: DefaultSaveBackupStore): SaveBackupStore
}
