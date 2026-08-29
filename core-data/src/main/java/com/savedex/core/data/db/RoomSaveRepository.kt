package com.savedex.core.data.db

import com.savedex.core.domain.Emulator
import com.savedex.core.domain.Game
import com.savedex.core.domain.SaveRepository
import com.savedex.core.domain.SaveSlot
import com.savedex.core.domain.SaveVersion
import javax.inject.Inject
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class RoomSaveRepository @Inject constructor(
    private val gameDao: GameDao,
    private val saveSlotDao: SaveSlotDao,
    private val saveVersionDao: SaveVersionDao,
) : SaveRepository {

    override fun observeGames(): Flow<List<Game>> = gameDao.observeAll().map { it.map(GameEntity::toDomain) }

    override fun observeSlots(gameId: String): Flow<List<SaveSlot>> =
        saveSlotDao.observeForGame(gameId).map { it.map(SaveSlotEntity::toDomain) }

    override fun observeVersions(slotId: String): Flow<List<SaveVersion>> =
        saveVersionDao.observeForSlot(slotId).map { it.map(SaveVersionEntity::toDomain) }

    override suspend fun getSlot(slotId: String): SaveSlot? = saveSlotDao.get(slotId)?.toDomain()

    override suspend fun getActiveSlot(gameId: String): SaveSlot? = saveSlotDao.getActive(gameId)?.toDomain()

    override suspend fun getVersion(versionId: String): SaveVersion? = saveVersionDao.get(versionId)?.toDomain()

    override suspend fun latestVersion(slotId: String): SaveVersion? = saveVersionDao.getLatest(slotId)?.toDomain()

    override suspend fun upsertGame(game: Game) = gameDao.upsert(game.toEntity())

    override suspend fun upsertSlot(slot: SaveSlot) = saveSlotDao.upsert(slot.toEntity())

    override suspend fun upsertVersion(version: SaveVersion) = saveVersionDao.upsert(version.toEntity())

    override suspend fun setActiveSlot(gameId: String, slotId: String) = saveSlotDao.setActive(gameId, slotId)
}

private fun GameEntity.toDomain() = Game(id, title, generation, Emulator.valueOf(sourceEmulator))
private fun Game.toEntity() = GameEntity(id, title, generation, sourceEmulator.name)

private fun SaveSlotEntity.toDomain() = SaveSlot(id, gameId, name, isActive)
private fun SaveSlot.toEntity() = SaveSlotEntity(id, gameId, name, isActive)

private fun SaveVersionEntity.toDomain() = SaveVersion(id, slotId, timestampEpochMillis, hash, backupPath)
private fun SaveVersion.toEntity() = SaveVersionEntity(id, slotId, timestampEpochMillis, hash, backupPath)
