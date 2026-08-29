package com.savedex.core.domain.usecase

import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class CreateSaveSlotUseCaseTest {

    @Test
    fun `creates an empty slot when there is nothing to seed from`() = runTest {
        val repository = FakeSaveRepository()
        val useCase = CreateSaveSlotUseCase(repository, FakeRealSaveAccess(), FakeSaveBackupStore())

        val slot = useCase(gameId = "game-1", name = "Partida principal", seedFromRealPath = null)

        assertEquals("game-1", slot.gameId)
        assertEquals("Partida principal", slot.name)
        assertTrue(repository.snapshotVersions(slot.id).isEmpty())
    }

    @Test
    fun `seeds a first version from the real path when one is provided`() = runTest {
        val repository = FakeSaveRepository()
        val realSaveAccess = FakeRealSaveAccess().apply { seed("/real/save.sav", byteArrayOf(1, 2, 3)) }
        val useCase = CreateSaveSlotUseCase(repository, realSaveAccess, FakeSaveBackupStore(), clock = { 1000L })

        val slot = useCase(gameId = "game-1", name = "Randomizer", seedFromRealPath = "/real/save.sav")

        val versions = repository.snapshotVersions(slot.id)
        assertEquals(1, versions.size)
        assertEquals(1000L, versions.single().timestampEpochMillis)
    }

    @Test
    fun `skips seeding when nothing exists yet at the real path`() = runTest {
        val repository = FakeSaveRepository()
        val useCase = CreateSaveSlotUseCase(repository, FakeRealSaveAccess(), FakeSaveBackupStore())

        val slot = useCase(gameId = "game-1", name = "Empty slot", seedFromRealPath = "/nowhere.sav")

        assertTrue(repository.snapshotVersions(slot.id).isEmpty())
    }
}
