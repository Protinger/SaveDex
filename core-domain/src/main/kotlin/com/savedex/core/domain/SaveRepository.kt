package com.savedex.core.domain

import kotlinx.coroutines.flow.Flow

/**
 * Local-only persistence for the Game → SaveSlot → SaveVersion hierarchy.
 * Every member here is expected to work fully offline — no network call on
 * any path.
 */
interface SaveRepository {
    fun observeGames(): Flow<List<Game>>
    fun observeSlots(gameId: String): Flow<List<SaveSlot>>
    fun observeVersions(slotId: String): Flow<List<SaveVersion>>

    suspend fun getSlot(slotId: String): SaveSlot?
    suspend fun getActiveSlot(gameId: String): SaveSlot?
    suspend fun getVersion(versionId: String): SaveVersion?
    suspend fun latestVersion(slotId: String): SaveVersion?

    suspend fun upsertGame(game: Game)
    suspend fun upsertSlot(slot: SaveSlot)
    suspend fun upsertVersion(version: SaveVersion)

    /** Activates [slotId] within [gameId] and deactivates every other slot of that game, atomically. */
    suspend fun setActiveSlot(gameId: String, slotId: String)
}
