package com.savedex.core.data.save

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.uiautomator.UiDevice
import com.savedex.core.data.db.RoomSaveRepository
import com.savedex.core.data.db.SaveDexDatabase
import com.savedex.core.domain.Emulator
import com.savedex.core.domain.Game
import com.savedex.core.domain.Hashing
import com.savedex.core.domain.usecase.ActivateSaveSlotUseCase
import com.savedex.core.domain.usecase.BackupSaveUseCase
import com.savedex.core.domain.usecase.CreateSaveSlotUseCase
import com.savedex.core.domain.usecase.RestoreSaveVersionUseCase
import java.io.File
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Runs the whole slot lifecycle — create, activate (with auto-backup of the
 * outgoing slot), manual backup, restore — with the device's radio actually
 * off, to verify none of it secretly depends on network access.
 *
 * Airplane mode is toggled via shell commands run through [UiDevice], the
 * same mechanism `adb shell` uses — the app itself has no permission to
 * flip system settings, and never needs to for this feature to work.
 */
@RunWith(AndroidJUnit4::class)
class AirplaneModeSaveFlowTest {

    private val context = ApplicationProvider.getApplicationContext<Context>()
    private val device: UiDevice = UiDevice.getInstance(InstrumentationRegistry.getInstrumentation())

    private lateinit var database: SaveDexDatabase
    private lateinit var repository: RoomSaveRepository
    private lateinit var realSaveAccess: DefaultRealSaveAccess
    private lateinit var backupStore: DefaultSaveBackupStore
    private lateinit var realSaveDir: File
    private lateinit var realPath: String

    @Before
    fun setUp() {
        database = Room.inMemoryDatabaseBuilder(context, SaveDexDatabase::class.java).build()
        repository = RoomSaveRepository(database.gameDao(), database.saveSlotDao(), database.saveVersionDao())
        realSaveAccess = DefaultRealSaveAccess(context)
        backupStore = DefaultSaveBackupStore(context)
        realSaveDir = File(context.cacheDir, "airplane-mode-test").apply { mkdirs() }
        realPath = File(realSaveDir, "emulator_real_save.sav").absolutePath

        setAirplaneMode(enabled = true)
        awaitNetworkAvailable(expected = false)
    }

    @After
    fun tearDown() {
        setAirplaneMode(enabled = false)
        database.close()
        realSaveDir.deleteRecursively()
    }

    @Test
    fun fullSlotLifecycleCompletesWithNoNetworkAvailable() = runBlocking {
        assertFalse("Test setup failed to actually disable networking", isNetworkAvailable())

        repository.upsertGame(
            Game(id = "hash-abc", title = "Pokemon Platinum", generation = 4, sourceEmulator = Emulator.MELONDS),
        )

        val create = CreateSaveSlotUseCase(repository, realSaveAccess, backupStore, clock = { 1_000L })
        val slotA = create("hash-abc", "Partida principal")

        File(realPath).writeBytes(byteArrayOf(1, 2, 3))
        val backup = BackupSaveUseCase(repository, realSaveAccess, backupStore, clock = { 2_000L })
        val versionA1 = backup(slotA.id, realPath)

        val slotB = create("hash-abc", "Randomizer")
        repository.setActiveSlot("hash-abc", slotA.id)

        // The emulator writes new, never-backed-up state before we switch slots.
        File(realPath).writeBytes(byteArrayOf(9, 9, 9))
        val activate = ActivateSaveSlotUseCase(repository, realSaveAccess, backupStore, clock = { 3_000L })
        activate("hash-abc", slotB.id, realPath)

        val slotAVersions = repository.observeVersions(slotA.id).first()
        assertTrue(slotAVersions.any { it.hash == Hashing.sha256(byteArrayOf(9, 9, 9)) })

        val restore = RestoreSaveVersionUseCase(repository, realSaveAccess, backupStore)
        restore(versionA1.id, realPath)

        assertArrayEquals(byteArrayOf(1, 2, 3), File(realPath).readBytes())
        assertFalse("Networking came back on during the test", isNetworkAvailable())
    }

    private fun isNetworkAvailable(): Boolean {
        val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val network = connectivityManager.activeNetwork ?: return false
        val capabilities = connectivityManager.getNetworkCapabilities(network) ?: return false
        return capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
    }

    private fun setAirplaneMode(enabled: Boolean) {
        device.executeShellCommand("settings put global airplane_mode_on ${if (enabled) 1 else 0}")
        device.executeShellCommand("am broadcast -a android.intent.action.AIRPLANE_MODE --ez state $enabled")
    }

    private fun awaitNetworkAvailable(expected: Boolean, timeoutMillis: Long = 10_000L) {
        val deadline = System.currentTimeMillis() + timeoutMillis
        while (System.currentTimeMillis() < deadline && isNetworkAvailable() != expected) {
            Thread.sleep(200)
        }
    }
}
