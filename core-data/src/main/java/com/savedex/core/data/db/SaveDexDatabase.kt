package com.savedex.core.data.db

import androidx.room.Database
import androidx.room.RoomDatabase

// No exported schema directory is wired up yet (see other modules for the same
// tradeoff) — revisit once a real migration is needed.
@Database(
    entities = [GameEntity::class, SaveSlotEntity::class, SaveVersionEntity::class],
    version = 1,
    exportSchema = false,
)
abstract class SaveDexDatabase : RoomDatabase() {
    abstract fun gameDao(): GameDao
    abstract fun saveSlotDao(): SaveSlotDao
    abstract fun saveVersionDao(): SaveVersionDao
}
