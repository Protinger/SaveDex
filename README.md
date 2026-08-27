# SaveDex

SaveDex is an Android app for inspecting and editing Pokémon save files.

Copyright (C) 2026 Álvaro. Licensed under the **GNU General Public License
v3.0 or later** — see [`LICENSE`](LICENSE) for the full text. In short: you
are free to use, study, modify, and redistribute this software, provided
derivative works are also distributed under the GPLv3 and their source is
made available.

## Status

Phase 1 (this commit): module scaffolding, build configuration, and a
placeholder Compose screen. No save-file logic yet.

Phase 2 (next): `core-pkhex-bridge` will wrap a PKHeX.Core-based .NET Android
library, built separately as a .NET for Android project and consumed here as
a compiled AAR.

## Module structure

| Module | Type | Purpose |
|---|---|---|
| `:app` | Android application, Compose + Hilt | UI shell, DI graph wiring, entry point |
| `:core-domain` | Kotlin/JVM (no Android deps) | Domain models and use cases, unit-testable on the JVM |
| `:core-data` | Android library | Room database, WorkManager jobs, repositories backing `core-domain` |
| `:core-pkhex-bridge` | Android library (placeholder) | Will wrap the .NET/PKHeX.Core AAR in phase 2 |

`:core-domain` intentionally has zero Android dependencies so its business
logic stays pure Kotlin and fast to unit test.

## Toolchain

Versions pinned in [`gradle/libs.versions.toml`](gradle/libs.versions.toml),
each verified against Google Maven / Maven Central at the time of writing
(August 2026):

- Kotlin 2.4.10, AGP 9.3.2, Gradle 9.5.0
- Compose BOM 2026.08.00
- Hilt 2.60.1, KSP 2.3.11
- Room 2.8.4, WorkManager 2.11.2, androidx.window 1.5.1
- kotlinx.coroutines 1.11.0
- `compileSdk` / `targetSdk` 37 (Android 17), `minSdk` 26

`compileSdk` started at 36 (Android 16) but the current stable releases of
`androidx.core`, `androidx.lifecycle`, and `androidx.compose.ui` all declare
a minimum `compileSdk` of 37 in their AAR metadata, so the build fails below
that even though 36 was "latest stable" a page ago. This moves fast; re-check
before assuming 37 is still current.

AGP 9.0 also switched two defaults that this project opts back out of, in
[`gradle.properties`](gradle.properties):

- `android.builtInKotlin=false` — AGP 9 now bundles its own Kotlin support
  and rejects the explicit `org.jetbrains.kotlin.android` plugin unless this
  is disabled. Built-in Kotlin is new enough (see the open issues linked
  from its own release notes) that pinning to the classic, independently
  versioned Kotlin Gradle plugin seemed the sturdier default for a
  foundation others will build on.
- `android.newDsl=false` — the classic `android { ... }` extension type
  (`BaseAppModuleExtension` etc.) that `kotlin-android` expects is
  incompatible with AGP 9's new DSL types unless this is set.

Both are easy to flip later; nothing else in this project depends on the
legacy DSL beyond that plugin interop.

R8 with resource shrinking is enabled on the `release` build type of `:app`
from the start (`isMinifyEnabled = true`, `isShrinkResources = true`), so
shrinking rules get exercised as code is added rather than bolted on later.

## Building

```bash
./gradlew assembleDebug
```

The Gradle wrapper (`gradlew` / `gradlew.bat` / `gradle/wrapper/*`) targets
Gradle 9.5.0 and was fetched directly from Gradle's own release artifacts —
no local Gradle install is required, just a JDK 17+ on `PATH` or `JAVA_HOME`
(Android Studio's bundled JBR works).

**Verified end to end:** `:app:assembleDebug` and `:app:assembleRelease`
(R8 + resource shrinking both ran, via `minifyReleaseWithR8` /
`optimizeReleaseResources`) build clean, and the debug APK was installed
and launched on a real emulator, confirming the placeholder screen renders
("SaveDex" / "Module scaffolding ready...") with no crashes in logcat.

## Emulator

An AVD is already set up on this machine for exactly this kind of
verification in future phases:

- Name: `SaveDex_API36` (Pixel 6 device profile, Android 16 / API 36,
  `google_apis` x86_64 image)
- Android SDK cmdline-tools (`sdkmanager` / `avdmanager`) were installed
  under `<sdk>/cmdline-tools/latest` to manage this — they weren't there
  before.
- Windows Hypervisor Platform (WHPX) accelerates it; boots in well under a
  minute headless.

To boot it headless from PowerShell:

```powershell
& "$env:LOCALAPPDATA\Android\Sdk\emulator\emulator.exe" -avd SaveDex_API36 -no-window -no-audio -no-boot-anim -gpu swiftshader_indirect -accel auto
```

**Use the Windows short (8.3) path for the SDK root when launching the
emulator, not the plain one** — e.g. `C:\Users\LVARO~1\AppData\Local\Android\Sdk`
instead of `C:\Users\Álvaro\AppData\Local\Android\Sdk`. The accented
username breaks QEMU's native BIOS-file loading (`could not load PC BIOS
'bios-256k.bin'`) if you don't. Set `ANDROID_SDK_ROOT` / `ANDROID_HOME` to
the short path before invoking `emulator.exe`, `adb.exe`, `sdkmanager.bat`,
etc. Get the short path for any folder from PowerShell if you ever need it
again:

```powershell
(New-Object -ComObject Scripting.FileSystemObject).GetFolder("C:\Users\Álvaro").ShortPath
```

Once booted, `adb devices` should show `emulator-5554  device`; then
`adb install -r app\build\outputs\apk\debug\app-debug.apk` and
`adb shell am start -n com.savedex.app/com.savedex.app.MainActivity` to
run it, or just open the project in Android Studio and hit Run — Android
Studio doesn't need the short-path workaround since it launches the
emulator itself.

## .NET SDK (needed for phase 2, not yet installed on this machine)

`core-pkhex-bridge` will eventually be backed by a .NET for Android project.
That workload was not found on this machine (no `dotnet` command). To set
it up ahead of phase 2:

```bash
winget install Microsoft.DotNet.SDK.9
dotnet workload install android
```

(Use whichever current .NET SDK feature band `winget` resolves; check
`dotnet --list-sdks` afterwards.) Verify the Android workload landed with:

```bash
dotnet workload list
```

This step was left as documentation rather than performed automatically,
since installing an SDK is a machine-wide change outside this repository.

## Notes for review

- `local.properties` (with `sdk.dir`) was created locally to point at the
  installed Android SDK; it is gitignored and was not committed.
- No commits were made — everything here is unstaged/untracked in a fresh
  `git init` for review before anything is pushed.
- The shell this project was scaffolded in blocks the loopback (AF_UNIX)
  socket Gradle's build process needs on Windows — a sandbox limitation,
  not a project issue, but it meant `gradlew` could not run there beyond
  `--version`. The actual `assembleDebug` / `assembleRelease` builds above
  were verified from WSL (Ubuntu) instead, pointed at the same Android SDK
  via `/mnt/c/...`, with a temporary OpenJDK 17 installed there for the
  purpose. `local.properties` is back to the Windows-style path now; WSL
  isn't required for you to build this — a normal Windows terminal (or
  Android Studio) should work fine outside that sandbox.
- Two real (not sandbox-related) compatibility issues turned up during that
  verification and are already fixed in the files here: AGP 9's built-in
  Kotlin/new-DSL defaults conflicting with the `kotlin-android` plugin, and
  `compileSdk 36` being too low for current AndroidX releases. Both are
  explained above under "Toolchain".
