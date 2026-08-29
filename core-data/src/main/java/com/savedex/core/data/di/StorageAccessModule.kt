package com.savedex.core.data.di

import com.savedex.core.data.access.DefaultStorageAccessManager
import com.savedex.core.data.access.StorageAccessManager
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

@Module
@InstallIn(SingletonComponent::class)
abstract class StorageAccessModule {

    @Binds
    abstract fun bindStorageAccessManager(impl: DefaultStorageAccessManager): StorageAccessManager
}
