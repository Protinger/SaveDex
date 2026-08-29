package com.savedex.core.domain.usecase

import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ActivateSaveSlotUseCaseTest {

    @Test
    fun `activating a slot never loses the previously active slot's unbacked state`() = runTest {
        val repository = FakeSaveRepository()
        val realSaveAccess = FakeRealSaveAccess()
        val backupStore = FakeSaveBackupStore()
        val create = CreateSaveSlotUseCase(repository, realSaveAccess, backupStore, clock = { 0L })

        val slotA = create("game-1", "Slot A")
        val slotB = create("game-1", "Slot B")
        repository.setActiveSlot("game-1", slotA.id)

        // Slot A is active and the emulator has since written state that was never explicitly backed up.
        realSaveAccess.seed("/real/save.sav", byteArrayOf(9, 9, 9))

        val activate = ActivateSaveSlotUseCase(repository, realSaveAccess, backupStore, clock = { 5000L })
        activate("game-1", slotB.id, "/real/save.sav")

        val slotAVersions = repository.snapshotVersions(slotA.id)
        assertEquals(1, slotAVersions.size)
        assertArrayEquals(byteArrayOf(9, 9, 9), backupStore.read(slotAVersions.single().backupPath))
    }

    @Test
    fun `does not create a duplicate backup when the real path already matches the slot's latest version`() = runTest {
        val repository = FakeSaveRepository()
        val realSaveAccess = FakeRealSaveAccess().apply { seed("/real/save.sav", byteArrayOf(1, 2, 3)) }
        val backupStore = FakeSaveBackupStore()
        val create = CreateSaveSlotUseCase(repository, realSaveAccess, backupStore, clock = { 0L })

        val slotA = create("game-1", "Slot A", seedFromRealPath = "/real/save.sav")
        val slotB = create("game-1", "Slot B")
        repository.setActiveSlot("game-1", slotA.id)

        val activate = ActivateSaveSlotUseCase(repository, realSaveAccess, backupStore, clock = { 9999L })
        activate("game-1", slotB.id, "/real/save.sav")

        assertEquals(1, repository.snapshotVersions(slotA.id).size)
    }

    @Test
    fun `copies the target slot's latest version to the real path and flips isActive`() = runTest {
        val repository = FakeSaveRepository()
        val realSaveAccess = FakeRealSaveAccess().apply { seed("/imports/slotB-seed.sav", byteArrayOf(7, 7, 7)) }
        val backupStore = FakeSaveBackupStore()
        val create = CreateSaveSlotUseCase(repository, realSaveAccess, backupStore, clock = { 0L })

        val slotA = create("game-1", "Slot A")
        repository.setActiveSlot("game-1", slotA.id)
        val slotB = create("game-1", "Slot B", seedFromRealPath = "/imports/slotB-seed.sav")

        val activate = ActivateSaveSlotUseCase(repository, realSaveAccess, backupStore, clock = { 1234L })
        activate("game-1", slotB.id, "/real/save.sav")

        assertArrayEquals(byteArrayOf(7, 7, 7), realSaveAccess.read("/real/save.sav"))
        val slots = repository.snapshotSlots("game-1").associateBy { it.id }
        assertTrue(slots.getValue(slotB.id).isActive)
        assertFalse(slots.getValue(slotA.id).isActive)
    }
}
