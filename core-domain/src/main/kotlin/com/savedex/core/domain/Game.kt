package com.savedex.core.domain

/**
 * Identity of a game a save belongs to. [id] is derived from the ROM/save
 * itself (e.g. a hash of the save's header) rather than assigned
 * sequentially, so the same game re-discovered on a later scan resolves to
 * the same [Game] row instead of creating a duplicate.
 */
data class Game(
    val id: String,
    val title: String,
    val generation: Int,
    val sourceEmulator: Emulator,
)
