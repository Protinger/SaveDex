package com.savedex.core.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class EmulatorProfilesTest {

    @Test
    fun `has exactly one profile per emulator`() {
        assertEquals(Emulator.entries.toSet(), EmulatorProfiles.all.map { it.emulator }.toSet())
        assertEquals(Emulator.entries.size, EmulatorProfiles.all.size)
    }

    @Test
    fun `every profile declares at least one candidate save directory`() {
        EmulatorProfiles.all.forEach { profile ->
            assertTrue("${profile.emulator} has no save directories", profile.saveDirectories.isNotEmpty())
        }
    }
}
