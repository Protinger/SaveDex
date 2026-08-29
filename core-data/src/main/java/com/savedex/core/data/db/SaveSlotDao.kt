package com.savedex.core.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface SaveSlotDao {
    @Query("SELECT * FROM save_slots WHERE gameId = :gameId")
    fun observeForGame(gameId: String): Flow<List<SaveSlotEntity>>

    @Query("SELECT * FROM save_slots WHERE id = :slotId")
    suspend fun get(slotId: String): SaveSlotEntity?

    @Query("SELECT * FROM save_slots WHERE gameId = :gameId AND isActive = 1 LIMIT 1")
    suspend fun getActive(gameId: String): SaveSlotEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(slot: SaveSlotEntity)

    /** Single-statement flip so no other slot of [gameId] can observe itself as active mid-switch. */
    @Query("UPDATE save_slots SET isActive = (id = :slotId) WHERE gameId = :gameId")
    suspend fun setActive(gameId: String, slotId: String)
}
