package com.savedex.core.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface SaveVersionDao {
    @Query("SELECT * FROM save_versions WHERE slotId = :slotId ORDER BY timestampEpochMillis DESC")
    fun observeForSlot(slotId: String): Flow<List<SaveVersionEntity>>

    @Query("SELECT * FROM save_versions WHERE id = :versionId")
    suspend fun get(versionId: String): SaveVersionEntity?

    @Query("SELECT * FROM save_versions WHERE slotId = :slotId ORDER BY timestampEpochMillis DESC LIMIT 1")
    suspend fun getLatest(slotId: String): SaveVersionEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(version: SaveVersionEntity)
}
