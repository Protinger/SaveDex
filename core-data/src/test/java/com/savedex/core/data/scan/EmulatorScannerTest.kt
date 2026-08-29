package com.savedex.core.data.scan

import com.savedex.core.domain.Emulator
import com.savedex.core.domain.EmulatorProfile
import com.savedex.core.domain.EmulatorProfiles
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class EmulatorScannerTest {

    private val melonDsProfile = EmulatorProfile(
        emulator = Emulator.MELONDS,
        packageNames = listOf("me.magnum.melonds"),
        saveDirectories = listOf("Android/data/me.magnum.melonds/files/saves"),
        saveFileExtensions = listOf(".sav"),
    )

    @Test
    fun `finds a save file matching the profile extension inside its known directory`() {
        val fileSystem = FakeEmulatorFileSystem().apply {
            addFile(
                "Android/data/me.magnum.melonds/files/saves/Pokemon Platinum.sav",
                sizeBytes = 512,
                lastModifiedEpochMillis = 1000,
            )
            addFile("Android/data/me.magnum.melonds/files/saves/readme.txt")
        }

        val result = EmulatorScanner(fileSystem, profiles = listOf(melonDsProfile)).scan()

        val save = result.single()
        assertEquals(Emulator.MELONDS, save.emulator)
        assertEquals("Pokemon Platinum.sav", save.fileName)
        assertEquals(512L, save.sizeBytes)
        assertEquals(1000L, save.lastModifiedEpochMillis)
    }

    @Test
    fun `recurses into subdirectories under the known save directory`() {
        val fileSystem = FakeEmulatorFileSystem().apply {
            addFile("Android/data/me.magnum.melonds/files/saves/subdir/Pokemon Diamond.sav")
        }

        val result = EmulatorScanner(fileSystem, profiles = listOf(melonDsProfile)).scan()

        assertEquals("Pokemon Diamond.sav", result.single().fileName)
    }

    @Test
    fun `returns nothing for a profile whose save directory does not exist`() {
        val result = EmulatorScanner(FakeEmulatorFileSystem(), profiles = listOf(melonDsProfile)).scan()

        assertTrue(result.isEmpty())
    }

    @Test
    fun `scans every candidate directory for a profile`() {
        val retroArchProfile = EmulatorProfile(
            emulator = Emulator.RETROARCH,
            packageNames = listOf("com.retroarch"),
            saveDirectories = listOf("RetroArch/saves", "Android/data/com.retroarch/files/RetroArch/saves"),
            saveFileExtensions = listOf(".srm"),
        )
        val fileSystem = FakeEmulatorFileSystem().apply {
            addFile("RetroArch/saves/game1.srm")
            addFile("Android/data/com.retroarch/files/RetroArch/saves/game2.srm")
        }

        val result = EmulatorScanner(fileSystem, profiles = listOf(retroArchProfile)).scan()

        assertEquals(setOf("game1.srm", "game2.srm"), result.map { it.fileName }.toSet())
    }

    @Test
    fun `matches extensions case-insensitively`() {
        val fileSystem = FakeEmulatorFileSystem().apply {
            addFile("Android/data/me.magnum.melonds/files/saves/Pokemon.SAV")
        }

        val result = EmulatorScanner(fileSystem, profiles = listOf(melonDsProfile)).scan()

        assertEquals(1, result.size)
    }

    @Test
    fun `does not descend into a sibling directory outside the profile's own path`() {
        val fileSystem = FakeEmulatorFileSystem().apply {
            addFile("Android/data/me.magnum.melonds/files/saves/Pokemon.sav")
            addFile("Android/data/some.other.app/files/saves/Pokemon.sav")
        }

        val result = EmulatorScanner(fileSystem, profiles = listOf(melonDsProfile)).scan()

        assertEquals(1, result.size)
    }

    @Test
    fun `scans every default profile without crashing on an empty filesystem`() {
        val result = EmulatorScanner(FakeEmulatorFileSystem(), profiles = EmulatorProfiles.all).scan()

        assertTrue(result.isEmpty())
    }
}
