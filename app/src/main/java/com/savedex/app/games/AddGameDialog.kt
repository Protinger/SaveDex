package com.savedex.app.games

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.savedex.core.domain.Emulator
import com.savedex.core.domain.SaveFile

private sealed interface DialogStep {
    data object Choices : DialogStep
    data object PickEmulatorForManualImport : DialogStep
    data class NameGame(val saveFile: SaveFile) : DialogStep
}

@Composable
fun AddGameDialog(viewModel: GamesListViewModel, onDismiss: () -> Unit) {
    val scanState by viewModel.scanState.collectAsState()
    var step by remember { mutableStateOf<DialogStep>(DialogStep.Choices) }
    var pendingManualImportEmulator by remember { mutableStateOf<Emulator?>(null) }

    val manualImportLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult(),
    ) { result ->
        val uri = result.data?.data
        val emulator = pendingManualImportEmulator
        if (uri != null && emulator != null) {
            viewModel.importManualSave(uri, emulator)
            step = DialogStep.Choices
        }
    }

    val manageAllFilesLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult(),
    ) {
        viewModel.scanForSaveFiles()
    }

    val openDocumentTreeLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult(),
    ) { result ->
        val uri = result.data?.data
        if (uri != null) {
            viewModel.onDocumentTreeGranted(uri)
            viewModel.scanForSaveFiles()
        }
    }

    Dialog(onDismissRequest = onDismiss) {
        Surface(shape = MaterialTheme.shapes.large) {
            Column(modifier = Modifier.padding(24.dp)) {
                when (val currentStep = step) {
                    is DialogStep.Choices -> ChoicesStep(
                        scanState = scanState,
                        onScanStorage = viewModel::scanForSaveFiles,
                        onImportManually = { step = DialogStep.PickEmulatorForManualImport },
                        onGrantFullAccess = { manageAllFilesLauncher.launch(viewModel.createManageAllFilesIntent()) },
                        onPickFolder = { openDocumentTreeLauncher.launch(viewModel.createOpenDocumentTreeIntent()) },
                        onSelectSaveFile = { saveFile -> step = DialogStep.NameGame(saveFile) },
                        onCancel = onDismiss,
                    )

                    is DialogStep.PickEmulatorForManualImport -> PickEmulatorStep(
                        onSelect = { emulator ->
                            pendingManualImportEmulator = emulator
                            manualImportLauncher.launch(viewModel.createManualSavePickerIntent())
                        },
                        onCancel = { step = DialogStep.Choices },
                    )

                    is DialogStep.NameGame -> NameGameStep(
                        saveFile = currentStep.saveFile,
                        onConfirm = { title, generation ->
                            viewModel.addGame(currentStep.saveFile, title, generation)
                            onDismiss()
                        },
                        onCancel = { step = DialogStep.Choices },
                    )
                }
            }
        }
    }
}

@Composable
private fun ChoicesStep(
    scanState: ScanState,
    onScanStorage: () -> Unit,
    onImportManually: () -> Unit,
    onGrantFullAccess: () -> Unit,
    onPickFolder: () -> Unit,
    onSelectSaveFile: (SaveFile) -> Unit,
    onCancel: () -> Unit,
) {
    Text("Add a game", style = MaterialTheme.typography.titleLarge)

    when (scanState) {
        is ScanState.Idle -> Column(
            modifier = Modifier.padding(top = 16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Button(onClick = onScanStorage, modifier = Modifier.fillMaxWidth()) {
                Text("Scan storage")
            }
            OutlinedButton(onClick = onImportManually, modifier = Modifier.fillMaxWidth()) {
                Text("Import save file manually")
            }
            TextButton(onClick = onCancel, modifier = Modifier.fillMaxWidth()) {
                Text("Cancel")
            }
        }

        is ScanState.NeedsStorageAccess -> Column(
            modifier = Modifier.padding(top = 16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text(
                "SaveDex needs access to shared storage to find save files.",
                style = MaterialTheme.typography.bodyMedium,
            )
            Button(onClick = onGrantFullAccess, modifier = Modifier.fillMaxWidth()) {
                Text("Grant storage access")
            }
            OutlinedButton(onClick = onPickFolder, modifier = Modifier.fillMaxWidth()) {
                Text("Choose a folder instead")
            }
            TextButton(onClick = onCancel, modifier = Modifier.fillMaxWidth()) {
                Text("Cancel")
            }
        }

        is ScanState.Scanning -> Column(
            modifier = Modifier.padding(top = 24.dp, bottom = 8.dp),
            horizontalAlignment = androidx.compose.ui.Alignment.CenterHorizontally,
        ) {
            CircularProgressIndicator()
        }

        is ScanState.Results -> if (scanState.saveFiles.isEmpty()) {
            Column(
                modifier = Modifier.padding(top = 16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Text("No save files found.", style = MaterialTheme.typography.bodyMedium)
                OutlinedButton(onClick = onImportManually, modifier = Modifier.fillMaxWidth()) {
                    Text("Import save file manually")
                }
                TextButton(onClick = onCancel, modifier = Modifier.fillMaxWidth()) {
                    Text("Cancel")
                }
            }
        } else {
            LazyColumn(modifier = Modifier.padding(top = 8.dp)) {
                items(scanState.saveFiles, key = SaveFile::id) { saveFile ->
                    ListItem(
                        headlineContent = { Text(saveFile.fileName) },
                        supportingContent = { Text(saveFile.emulator.displayName) },
                        modifier = Modifier.fillMaxWidth(),
                    )
                    TextButton(onClick = { onSelectSaveFile(saveFile) }, modifier = Modifier.fillMaxWidth()) {
                        Text("Use this save")
                    }
                }
            }
        }

        is ScanState.Error -> Column(
            modifier = Modifier.padding(top = 16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text(scanState.message, style = MaterialTheme.typography.bodyMedium)
            Button(onClick = onScanStorage, modifier = Modifier.fillMaxWidth()) {
                Text("Retry scan")
            }
            TextButton(onClick = onCancel, modifier = Modifier.fillMaxWidth()) {
                Text("Cancel")
            }
        }
    }
}

@Composable
private fun PickEmulatorStep(onSelect: (Emulator) -> Unit, onCancel: () -> Unit) {
    Text("Which emulator is this save from?", style = MaterialTheme.typography.titleLarge)
    Column(modifier = Modifier.padding(top = 16.dp)) {
        Emulator.entries.forEach { emulator ->
            TextButton(onClick = { onSelect(emulator) }, modifier = Modifier.fillMaxWidth()) {
                Text(emulator.displayName)
            }
        }
        TextButton(onClick = onCancel, modifier = Modifier.fillMaxWidth()) {
            Text("Cancel")
        }
    }
}

@Composable
private fun NameGameStep(saveFile: SaveFile, onConfirm: (title: String, generation: Int) -> Unit, onCancel: () -> Unit) {
    var title by remember { mutableStateOf("") }
    var generationText by remember { mutableStateOf("") }

    Text("Name this game", style = MaterialTheme.typography.titleLarge)
    Column(
        modifier = Modifier.padding(top = 16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Text(saveFile.fileName, style = MaterialTheme.typography.bodySmall)
        OutlinedTextField(
            value = title,
            onValueChange = { title = it },
            label = { Text("Game title") },
            modifier = Modifier.fillMaxWidth(),
        )
        OutlinedTextField(
            value = generationText,
            onValueChange = { generationText = it.filter(Char::isDigit) },
            label = { Text("Generation") },
            modifier = Modifier.fillMaxWidth(),
        )
        Button(
            onClick = { onConfirm(title, generationText.toIntOrNull() ?: 0) },
            enabled = title.isNotBlank(),
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text("Add game")
        }
        TextButton(onClick = onCancel, modifier = Modifier.fillMaxWidth()) {
            Text("Back")
        }
    }
}
