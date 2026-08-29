package com.savedex.core.domain.usecase

import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertArrayEquals
import org.junit.Test

class RestoreSaveVersionUseCaseTest {

    @Test
    fun `restoring a version leaves the real file identical to the backup`() = runTest {
        val repository = FakeSaveRepository()
        val realSaveAccess = FakeRealSaveAccess().apply { seed("/real/save.sav", byteArrayOf(4, 5, 6)) }
        val backupStore = FakeSaveBackupStore()
        val backup = BackupSaveUseCase(repository, realSaveAccess, backupStore, clock = { 42L })
        val version = backup("slot-1", "/real/save.sav")

        // Real path now diverges from what was backed up.
        realSaveAccess.write("/real/save.sav", byteArrayOf(0, 0, 0))

        val restore = RestoreSaveVersionUseCase(repository, realSaveAccess, backupStore)
        restore(version.id, "/real/save.sav")

        val restored = realSaveAccess.read("/real/save.sav")
        assertArrayEquals(backupStore.read(version.backupPath), restored)
        assertArrayEquals(byteArrayOf(4, 5, 6), restored)
    }

    @Test(expected = IllegalStateException::class)
    fun `fails fast when the requested version does not exist`() = runTest {
        val restore = RestoreSaveVersionUseCase(FakeSaveRepository(), FakeRealSaveAccess(), FakeSaveBackupStore())

        restore("missing-version", "/real/save.sav")
    }
}
