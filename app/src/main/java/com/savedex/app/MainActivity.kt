package com.savedex.app

import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.savedex.app.games.GamesListScreen
import com.savedex.app.ui.theme.SaveDexTheme
import com.savedex.core.pkhexbridge.NativeBridge
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            SaveDexTheme {
                var showBridgeDebugScreen by remember { mutableStateOf(false) }
                if (showBridgeDebugScreen) {
                    Scaffold { innerPadding ->
                        Column(modifier = Modifier.padding(innerPadding)) {
                            TextButton(onClick = { showBridgeDebugScreen = false }) {
                                Text("Back to games")
                            }
                            PkhexBridgeDebugScreen()
                        }
                    }
                } else {
                    Column(modifier = Modifier.fillMaxSize()) {
                        TextButton(onClick = { showBridgeDebugScreen = true }) {
                            Text("PKHeX bridge debug")
                        }
                        GamesListScreen(modifier = Modifier.weight(1f))
                    }
                }
            }
        }
    }
}

/**
 * Phase-2 debug screen: pick a real save file via the system file picker,
 * pass its bytes to [NativeBridge.loadSave] (PKHeX.Core running behind a
 * NativeAOT/JNI bridge — see `dotnet-bridge/README.md`), and show the
 * result. This is the debug entry point the phase-2 brief asked for, not a
 * permanent app screen.
 */
@Composable
private fun PkhexBridgeDebugScreen(modifier: Modifier = Modifier) {
    val context = LocalContext.current
    var resultText by remember { mutableStateOf("Pick a save file to test the PKHeX bridge.") }

    val pickFileLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument(),
    ) { uri: Uri? ->
        if (uri == null) return@rememberLauncherForActivityResult
        resultText = runCatching {
            val bytes = context.contentResolver.openInputStream(uri)?.use { it.readBytes() }
                ?: throw IllegalStateException("Could not open the selected file.")
            NativeBridge.loadSave(bytes)
        }.fold(
            onSuccess = { summary ->
                "Game: ${summary.gameName}\n" +
                    "Party count: ${summary.partyCount}\n" +
                    "First party species: ${summary.firstPartySpecies}\n" +
                    "First party level: ${summary.firstPartyLevel}"
            },
            onFailure = { error -> "Load failed: ${error.message}" },
        )
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(text = stringResource(id = R.string.placeholder_title), style = MaterialTheme.typography.headlineMedium)
        Text(text = stringResource(id = R.string.placeholder_subtitle), style = MaterialTheme.typography.bodyMedium)
        Button(onClick = { pickFileLauncher.launch(arrayOf("*/*")) }) {
            Text("Pick save file (PKHeX bridge test)")
        }
        Text(text = resultText, style = MaterialTheme.typography.bodySmall)
    }
}

@Preview(showBackground = true)
@Composable
private fun PkhexBridgeDebugScreenPreview() {
    SaveDexTheme {
        PkhexBridgeDebugScreen()
    }
}
