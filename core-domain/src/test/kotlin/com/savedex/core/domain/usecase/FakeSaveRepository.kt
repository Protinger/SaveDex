package com.savedex.core.domain.usecase

import com.savedex.core.domain.Game
import com.savedex.core.domain.SaveRepository
import com.savedex.core.domain.SaveSlot
import com.savedex.core.domain.SaveVersion
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map

/** In-memory [SaveRepository] for use case tests — no Room, no device involved. */
class FakeSaveRepository : SaveRepository {
    private val games = MutableStateFlow<Map<String, Game>>(emptyMap())
    private val slots = MutableStateFlow<Map<String, SaveSlot>>(emptyMap())
    private val versions = MutableStateFlow<List<SaveVersion>>(emptyList())

    override fun observeGames() = games.map { it.values.toList() }

    override fun observeSlots(gameId: String) = slots.map { map -> map.values.filter { it.gameId == gameId } }

    override fun observeVersions(slotId: String) = versions.map { list -> list.filter { it.slotId == slotId } }

    override suspend fun getSlot(slotId: String): SaveSlot? = slots.value[slotId]

    override suspend fun getActiveSlot(gameId: String): SaveSlot? =
        slots.value.values.firstOrNull { it.gameId == gameId && it.isActive }

    override suspend fun getVersion(versionId: String): SaveVersion? =
        versions.value.firstOrNull { it.id == versionId }

    override suspend fun latestVersion(slotId: String): SaveVersion? =
        versions.value.filter { it.slotId == slotId }.maxByOrNull { it.timestampEpochMillis }

    override suspend fun upsertGame(game: Game) {
        games.value = games.value + (game.id to game)
    }

    override suspend fun upsertSlot(slot: SaveSlot) {
        slots.value = slots.value + (slot.id to slot)
    }

    override suspend fun upsertVersion(version: SaveVersion) {
        versions.value = versions.value + version
    }

    override suspend fun setActiveSlot(gameId: String, slotId: String) {
        slots.value = slots.value.mapValues { (_, slot) ->
            if (slot.gameId != gameId) slot else slot.copy(isActive = slot.id == slotId)
        }
    }

    fun snapshotSlots(gameId: String): List<SaveSlot> = slots.value.values.filter { it.gameId == gameId }

    fun snapshotVersions(slotId: String): List<SaveVersion> = versions.value.filter { it.slotId == slotId }
}
