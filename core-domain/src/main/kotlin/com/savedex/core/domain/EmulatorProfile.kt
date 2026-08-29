package com.savedex.core.domain

/**
 * Describes where a given [emulator] is known to keep its save files on
 * Android, so [EmulatorScanner]-style code (in `:core-data`) knows where to
 * look without hardcoding paths itself.
 *
 * @property packageNames Android application IDs known to belong to this
 *   emulator. Empty when there is no single official build to key off of.
 * @property saveDirectories Candidate directories, relative to the primary
 *   external storage root (e.g. `Android/data/<pkg>/files/saves`,
 *   `RetroArch/saves`), that are known or commonly documented to hold save
 *   files for this emulator. A scanner should treat every entry as
 *   "look here if it exists", not "this exists".
 * @property saveFileExtensions Case-insensitive file extensions (including
 *   the leading dot) recognized as save files for this emulator.
 */
data class EmulatorProfile(
    val emulator: Emulator,
    val packageNames: List<String>,
    val saveDirectories: List<String>,
    val saveFileExtensions: List<String>,
)

/**
 * Best-effort defaults sourced from each emulator's own docs/wiki/forums
 * where one exists. **Not verified against a real device for every entry**
 * — several of these emulators have no single official Android build, so
 * treat this as a starting point to confirm during manual QA, not ground
 * truth:
 *
 * - [Emulator.AZAHAR]: official Android build, but it stores saves through a
 *   user-chosen Storage Access Framework tree rather than a fixed path, so
 *   [EmulatorProfile.saveDirectories] here is only a fallback guess.
 * - [Emulator.MGBA]: upstream mGBA ships no official Android build at all;
 *   the package/paths below match a commonly seen community port and may not
 *   match the build a given user has installed.
 * - [Emulator.MELONDS]: covers both the original rafaelvcaetano/melonDS-android
 *   port and its MelonDualDS fork (by SapphireRhodonite, since renamed to
 *   WatermelonDS) — the fork shares the upstream codebase and save-directory
 *   convention, just under its own package IDs. The rename to WatermelonDS
 *   may have changed the package ID again after this was written; re-check
 *   against a real install before trusting it.
 * - [Emulator.RYUJINX]: the original project was discontinued; Android
 *   builds are unofficial community forks with inconsistent package IDs.
 */
object EmulatorProfiles {

    val AZAHAR = EmulatorProfile(
        emulator = Emulator.AZAHAR,
        packageNames = listOf("io.github.azahar_emu.azahar"),
        saveDirectories = listOf(
            "Android/data/io.github.azahar_emu.azahar/files/userdata/saves",
        ),
        saveFileExtensions = listOf(".sav"),
    )

    val MGBA = EmulatorProfile(
        emulator = Emulator.MGBA,
        packageNames = emptyList(),
        saveDirectories = listOf(
            "Android/data/com.github.mgba/files",
        ),
        saveFileExtensions = listOf(".sav"),
    )

    val MELONDS = EmulatorProfile(
        emulator = Emulator.MELONDS,
        packageNames = listOf(
            "me.magnum.melonds",
            "me.magnum.melondualds",
            "me.magnum.melondualds.nightly",
        ),
        saveDirectories = listOf(
            "Android/data/me.magnum.melonds/files/saves",
            "Android/data/me.magnum.melondualds/files/saves",
            "Android/data/me.magnum.melondualds.nightly/files/saves",
        ),
        saveFileExtensions = listOf(".sav"),
    )

    // Switch saves are a directory tree per title (bis/user/save/<id>/0/...),
    // not a single flat file with a recognizable extension, so an
    // extension-matching scanner can locate the directory above but won't
    // pick out individual "save files" the way it does for the other
    // profiles here.
    val RYUJINX = EmulatorProfile(
        emulator = Emulator.RYUJINX,
        packageNames = emptyList(),
        saveDirectories = listOf(
            "Android/data/org.ryujinx.android/files/bis/user/save",
        ),
        saveFileExtensions = emptyList(),
    )

    val RETROARCH = EmulatorProfile(
        emulator = Emulator.RETROARCH,
        packageNames = listOf("com.retroarch", "com.retroarch.aarch64", "com.retroarch.ra32"),
        saveDirectories = listOf(
            "RetroArch/saves",
            "Android/data/com.retroarch/files/RetroArch/saves",
        ),
        saveFileExtensions = listOf(".srm", ".sav"),
    )

    val all: List<EmulatorProfile> = listOf(AZAHAR, MGBA, MELONDS, RYUJINX, RETROARCH)
}
