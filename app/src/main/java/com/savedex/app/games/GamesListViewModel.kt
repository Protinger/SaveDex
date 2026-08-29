package com.savedex.app.games

import android.content.Intent
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.savedex.core.data.access.StorageAccess
import com.savedex.core.data.access.StorageAccessManager
import com.savedex.core.data.scan.EmulatorSaveRepository
import com.savedex.core.domain.Emulator
import com.savedex.core.domain.Game
import com.savedex.core.domain.Hashing
import com.savedex.core.domain.RealSaveAccess
import com.savedex.core.domain.SaveBackupStore
import com.savedex.core.domain.SaveFile
import com.savedex.core.domain.SaveRepository
import com.savedex.core.domain.usecase.CreateSaveSlotUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import java.util.UUID
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/** State of the "add a game by scanning/importing a save" flow surfaced by [GamesListViewModel.scanState]. */
sealed interface ScanState {
    data object Idle : ScanState
    data object NeedsStorageAccess : ScanState
    data object Scanning : ScanState
    data class Results(val saveFiles: List<SaveFile>) : ScanState
    data class Error(val message: String) : ScanState
}

@HiltViewModel
class GamesListViewModel @Inject constructor(
    private val saveRepository: SaveRepository,
    private val emulatorSaveRepository: EmulatorSaveRepository,
    private val storageAccessManager: StorageAccessManager,
    private val realSaveAccess: RealSaveAccess,
    saveBackupStore: SaveBackupStore,
) : ViewModel() {

    val games: StateFlow<List<Game>> = saveRepository.observeGames()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    private val _scanState = MutableStateFlow<ScanState>(ScanState.Idle)
    val scanState: StateFlow<ScanState> = _scanState.asStateFlow()

    private val createSaveSlotUseCase = CreateSaveSlotUseCase(saveRepository, realSaveAccess, saveBackupStore)

    fun createManageAllFilesIntent(): Intent = storageAccessManager.createManageAllFilesIntent()

    fun createOpenDocumentTreeIntent(): Intent = storageAccessManager.createOpenDocumentTreeIntent()

    fun createManualSavePickerIntent(): Intent = emulatorSaveRepository.createManualSavePickerIntent()

    fun onDocumentTreeGranted(uri: Uri) {
        storageAccessManager.onDocumentTreeGranted(uri)
    }

    fun scanForSaveFiles() {
        if (storageAccessManager.currentAccess() == StorageAccess.Unavailable) {
            _scanState.value = ScanState.NeedsStorageAccess
            return
        }
        viewModelScope.launch {
            _scanState.value = ScanState.Scanning
            val results = emulatorSaveRepository.scan().mapNotNull { saveFile ->
                emulatorSaveRepository.resolveRealPath(saveFile)?.let { realPath -> saveFile.copy(path = realPath) }
            }
            _scanState.value = ScanState.Results(results)
        }
    }

    fun importManualSave(uri: Uri, emulator: Emulator) {
        val saveFile = emulatorSaveRepository.importManualSave(uri, emulator)
        _scanState.value = if (saveFile != null) {
            ScanState.Results(listOf(saveFile))
        } else {
            ScanState.Error("Couldn't read the selected file.")
        }
    }

    fun resetScan() {
        _scanState.value = ScanState.Idle
    }

    fun addGame(saveFile: SaveFile, title: String, generation: Int) {
        viewModelScope.launch {
            val bytes = realSaveAccess.read(saveFile.path)
            val gameId = if (bytes != null) Hashing.sha256(bytes) else UUID.randomUUID().toString()
            saveRepository.upsertGame(
                Game(id = gameId, title = title, generation = generation, sourceEmulator = saveFile.emulator),
            )
            val slot = createSaveSlotUseCase(gameId = gameId, name = "Main", seedFromRealPath = saveFile.path)
            saveRepository.setActiveSlot(gameId, slot.id)
            _scanState.value = ScanState.Idle
        }
    }
}
