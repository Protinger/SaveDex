package com.savedex.core.domain.usecase

import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Test

class BackupSaveUseCaseTest {

    @Test
    fun `creates a new version from whatever is currently at the real path`() = runTest {
        val repository = FakeSaveRepository()
        val realSaveAccess = FakeRealSaveAccess().apply { seed("/real/save.sav", byteArrayOf(1, 1, 1)) }
        val useCase = BackupSaveUseCase(repository, realSaveAccess, FakeSaveBackupStore(), clock = { 100L })

        val version = useCase("slot-1", "/real/save.sav")

        assertEquals("slot-1", version.slotId)
        assertEquals(100L, version.timestampEpochMillis)
        assertNotNull(repository.getVersion(version.id))
    }

    @Test(expected = IllegalStateException::class)
    fun `fails fast when there is nothing at the real path yet`() = runTest {
        val useCase = BackupSaveUseCase(FakeSaveRepository(), FakeRealSaveAccess(), FakeSaveBackupStore())

        useCase("slot-1", "/nowhere.sav")
    }
}
