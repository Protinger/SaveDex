package com.savedex.core.domain

/**
 * A named save state for a [Game] — e.g. "Partida principal" or
 * "Randomizer". At most one slot per game should have [isActive] set at a
 * time: that's the slot whose latest [SaveVersion] currently lives at the
 * emulator's real save path. See `ActivateSaveSlotUseCase` for how that
 * invariant is maintained.
 */
data class SaveSlot(
    val id: String,
    val gameId: String,
    val name: String,
    val isActive: Boolean,
)
