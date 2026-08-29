package com.savedex.core.data.di

import android.content.Context
import androidx.room.Room
import com.savedex.core.data.db.GameDao
import com.savedex.core.data.db.SaveDexDatabase
import com.savedex.core.data.db.SaveSlotDao
import com.savedex.core.data.db.SaveVersionDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object SaveDatabaseModule {

    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): SaveDexDatabase =
        Room.databaseBuilder(context, SaveDexDatabase::class.java, "savedex.db").build()

    @Provides
    fun provideGameDao(database: SaveDexDatabase): GameDao = database.gameDao()

    @Provides
    fun provideSaveSlotDao(database: SaveDexDatabase): SaveSlotDao = database.saveSlotDao()

    @Provides
    fun provideSaveVersionDao(database: SaveDexDatabase): SaveVersionDao = database.saveVersionDao()
}
