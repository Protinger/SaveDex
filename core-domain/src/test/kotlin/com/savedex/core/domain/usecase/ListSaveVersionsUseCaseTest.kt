package com.savedex.core.domain.usecase

import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test

class ListSaveVersionsUseCaseTest {

    @Test
    fun `exposes only the versions belonging to the requested slot`() = runTest {
        val repository = FakeSaveRepository()
        val realSaveAccess = FakeRealSaveAccess().apply { seed("/real/save.sav", byteArrayOf(1)) }
        val backupStore = FakeSaveBackupStore()
        val backup = BackupSaveUseCase(repository, realSaveAccess, backupStore, clock = { 1L })

        val versionA = backup("slot-A", "/real/save.sav")
        backup("slot-B", "/real/save.sav")

        val versions = ListSaveVersionsUseCase(repository)("slot-A").first()

        assertEquals(listOf(versionA), versions)
    }
}
