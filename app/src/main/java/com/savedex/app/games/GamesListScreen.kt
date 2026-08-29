package com.savedex.app.games

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.savedex.core.domain.Game

@Composable
fun GamesListScreen(modifier: Modifier = Modifier, viewModel: GamesListViewModel = hiltViewModel()) {
    val games by viewModel.games.collectAsState()
    var showAddGameDialog by remember { mutableStateOf(false) }

    Scaffold(
        modifier = modifier,
        floatingActionButton = {
            FloatingActionButton(onClick = { showAddGameDialog = true }) {
                Text("+", style = MaterialTheme.typography.headlineSmall)
            }
        },
    ) { innerPadding ->
        if (games.isEmpty()) {
            EmptyGamesList(modifier = Modifier.padding(innerPadding).fillMaxSize())
        } else {
            LazyColumn(modifier = Modifier.padding(innerPadding).fillMaxSize()) {
                items(games, key = Game::id) { game -> GameRow(game) }
            }
        }
    }

    if (showAddGameDialog) {
        AddGameDialog(
            viewModel = viewModel,
            onDismiss = {
                showAddGameDialog = false
                viewModel.resetScan()
            },
        )
    }
}

@Composable
private fun EmptyGamesList(modifier: Modifier = Modifier) {
    Column(
        modifier = modifier.padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Text("No games yet", style = MaterialTheme.typography.titleMedium)
        Text(
            "Tap + to scan your emulators' storage for save files, or import one manually.",
            style = MaterialTheme.typography.bodyMedium,
        )
    }
}

@Composable
private fun GameRow(game: Game, modifier: Modifier = Modifier) {
    ListItem(
        modifier = modifier,
        headlineContent = { Text(game.title) },
        supportingContent = { Text(game.sourceEmulator.displayName) },
    )
}
