package com.savedex.core.domain

/**
 * Emulators SaveDex knows how to locate save files for. Android build/package
 * availability varies a lot between these — see [EmulatorProfile] for the
 * per-emulator caveats before trusting any path baked into a profile.
 */
enum class Emulator(val displayName: String) {
    AZAHAR("Azahar"),
    MGBA("mGBA"),
    MELONDS("melonDS"),
    RYUJINX("Ryujinx"),
    RETROARCH("RetroArch"),
}
