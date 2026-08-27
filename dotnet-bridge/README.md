# `dotnet-bridge` — PKHeX.Core interop spike (phase 2)

**Status: phase 2 is done. The literal ask (C# class → `dotnet build` →
AAR with automatic Kotlin bindings) is not viable — see Part 1. The
realistic alternative — raw JNI exports from a NativeAOT-compiled native
library — is viable and fully verified: a real Pokémon Black 2 save
(user-supplied, DeSmuME `.sav`, 512 KB) was picked through the app's real
file picker, passed across the JNI boundary, and correctly parsed by
PKHeX.Core running as a native `.so` — the app displayed `Game: B2, Party
count: 2, First party species: Volcarona, First party level: 70`, which
matches the actual save.** Cold-start and memory were measured on that
same run — see Part 4.

## Part 1 — why the automatic-AAR-bindings approach doesn't work

`PkhexBridge.Android/` is a `dotnet new androidlib` class library targeting
`net10.0-android`, referencing `PKHeX.Core` from NuGet, with
[`SaveBridge.cs`](PkhexBridge.Android/SaveBridge.cs) /
[`SaveSummary.cs`](PkhexBridge.Android/SaveSummary.cs) — both deriving from
`Java.Lang.Object`, the usual prerequisite for .NET-for-Android's Java
Callable Wrapper (JCW) generator. This compiles cleanly against
`net10.0-android` (proof PKHeX.Core's public API — `SaveUtil`, `SaveFile`,
`PKM`, the `Species` enum — works fine on this target), but produces no
callable artifact — Java-interop codegen, Mono/CoreCLR runtime embedding,
DEX compilation, and APK assembly are all gated on `AndroidApplication ==
'True'` in the SDK's own MSBuild targets, and only run for *application*
projects, never libraries. Confirmed three separate ways:

1. **Reading the installed SDK's actual targets.**
   `Microsoft.Android.Sdk.BuildOrder.targets` (at
   `C:\Program Files\dotnet\packs\Microsoft.Android.Sdk.Windows\36.1.69\targets\`)
   gates `_GenerateJavaStubs` (JCW generation), `_CompileJava`,
   `_LinkNativeRuntime`, `_CompileDex`, and `_CreateBaseApk` all behind
   `Condition=" '$(AndroidApplication)' == 'True' "`. The AAR a *library*
   can produce (`_CreateAar` in `Microsoft.Android.Sdk.AndroidLibraries.targets`)
   only bundles jars/resources/assets/manifest — never the compiled
   assembly, never a runtime.
2. **`dotnet new list android`** offers no template for "expose C# to
   plain Kotlin" — only `androidlib` (what was built here — C# reused by
   *other* .NET Android projects) and `android-bindinglib` (the opposite
   direction: wraps an *existing* Java/Kotlin AAR for use *from* C#).
3. **Trying NativeAOT as an alternate runtime backend for the same class
   library didn't change the outcome.** The Android workload ships
   `Microsoft.Android.Runtime.NativeAOT.36.android-{arm64,x64}` packs, and
   `Microsoft.Android.Sdk.NativeAOT.targets` even sets
   `_AndroidJcwCodegenTarget=JavaInterop1` unconditionally, which looked
   promising. But `dotnet publish -r android-x64 -p:PublishAot=true` on the
   *library* project (`AndroidApplication=false`) never schedules the
   `IlcCompile` target at all (verified at `-v:normal` — zero mentions of
   `Ilc` anywhere in the log, before *and* after installing the Android
   NDK, which ruled out that as the blocker). The generic .NET SDK's
   `CopyNativeBinary` step still runs and fails trying to copy a `.so`
   that was never produced. NativeAOT-for-Android, like Mono-for-Android,
   is application-scoped tooling.

## Part 2 — the raw-JNI alternative: proven viable, on-device

The technically real way to call .NET code from a *pure* Kotlin app is
**NativeAOT compiled to a `linux-bionic` shared library, loaded via
`System.loadLibrary`, called through hand-written JNI export functions**
(`[UnmanagedCallersOnly(EntryPoint = "Java_...")]`, no automatic bindings
of any kind — every JNI signature is hand-matched to the C JNI ABI). This
is real: [josephmoresena/NativeAOT-AndroidHelloJniLib](https://github.com/josephmoresena/NativeAOT-AndroidHelloJniLib)
is a working reference implementation, and its `BionicNativeAot.targets`
(copied into [`PkhexBridge.Native.Poc/`](PkhexBridge.Native.Poc/) here)
is what actually made this build on this machine.

**What was proven, concretely:** a `net10.0` (no `-android` suffix, no
Android SDK involved at all) project, `RuntimeIdentifier=linux-bionic-x64`,
`PublishAot=true`, `NativeLib=Shared`, exporting one function:

```csharp
[UnmanagedCallersOnly(EntryPoint = "Java_com_savedex_core_pkhexbridge_NativeBridge_add")]
public static int Add(nint env, nint clazz, int a, int b) => a + b;
```

publishes to a real 924 KB Android x86_64 ELF shared object
(`libPkhexBridgeNative.so`, confirmed via `llvm-nm -D` that the exported
symbol is present), which was copied into
[`core-pkhex-bridge/src/main/jniLibs/x86_64/`](../core-pkhex-bridge/src/main/jniLibs/x86_64/),
loaded from [`NativeBridge.kt`](../core-pkhex-bridge/src/main/java/com/savedex/core/pkhexbridge/NativeBridge.kt)
(`System.loadLibrary("PkhexBridgeNative")` + `external fun add`), called
from [`MainActivity.kt`](../app/src/main/java/com/savedex/app/MainActivity.kt),
and **run on the `SaveDex_API36` emulator** — the screen shows
`NativeAOT JNI spike: add(2, 3) = 5`, no crash, no `UnsatisfiedLinkError`.
This is currently wired into `:app`/`:core-pkhex-bridge` (uncommitted, like
everything else in this session) purely as evidence the round trip works;
it has nothing to do with PKHeX yet.

### Exact repro (all flags matter)

```bash
# from PkhexBridge.Native.Poc/, after `sdkmanager "ndk;27.3.13750724"`
export ANDROID_NDK_ROOT="C:\Users\<8.3-short-name>\AppData\Local\Android\Sdk\ndk\27.3.13750724"
dotnet publish -c Release -p:DisableUnsupportedError=true -p:PublishAotUsingRuntimePack=true
```

Three non-obvious things had to line up:
- **`-p:DisableUnsupportedError=true`** — without it, ILCompiler refuses
  with `Cross-OS native compilation is not supported` on a Windows host
  targeting `linux-bionic-*`. This is a real, intentional check that the
  Android-application NativeAOT path (Part 1, point 3) silently disables
  for itself but which a plain `net10.0` project has to opt out of
  explicitly.
- **The 8.3 short path for `ANDROID_NDK_ROOT`.** Same root cause as the
  emulator BIOS-loading issue already documented in the top-level
  [`README.md`](../README.md#emulator): the accented username
  (`C:\Users\Álvaro\...`) breaks the NDK toolchain invocation here too.
  Use the short path (`C:\Users\LVARO~1\...`), not the display one.
- **The project directory needs to be on `PATH`.** `BionicNativeAot.targets`
  writes `android_clang.cmd`/`android_llvm-objcopy.cmd` wrapper batch files
  into the current directory so NDK paths with spaces/unusual characters
  survive being passed as `CppCompilerAndLinker`; without the project
  directory on `PATH`, the linker step fails with `9009` (command not
  found) trying to invoke them.

## Part 3 — the real bridge: PKHeX.Core behind hand-marshaled JNI

[`PkhexBridge.Native/`](PkhexBridge.Native/) replaces the `add`-only PoC
with the actual bridge: `NativeExports.LoadSave` takes a `jbyteArray`,
calls `PKHeX.Core.SaveUtil.TryGetSaveFile`, and returns a hand-built JSON
string (or `0`/null on any failure — no Java exception thrown natively,
to avoid also needing `FindClass`+`ThrowNew`; [`NativeBridge.kt`](../core-pkhex-bridge/src/main/java/com/savedex/core/pkhexbridge/NativeBridge.kt)
turns a null result into an `IllegalArgumentException` on the Kotlin side,
preserving the contract the original Mono/JCW spike had).

No general-purpose JNI wrapper library was pulled in for this — just the
four `JNIEnv` functions actually needed
(`GetArrayLength`, `GetByteArrayRegion`, `NewStringUTF`, in
[`JniEnv.cs`](PkhexBridge.Native/JniEnv.cs)). Their vtable offsets (171,
200, 167) were **computed by parsing the real `jni.h` shipped in Android
NDK 27.3.13750724** (`sysroot/usr/include/jni.h` — `JNINativeInterface` is
a fixed-order, ABI-stable function-pointer table; a small Python script
counted the `(*Name)` declarations in order, 4 reserved slots then 233
functions total, and cross-checked several well-known ones like
`GetObjectClass`=31), not copied from a third party — a wrong offset here
would mean calling into arbitrary native code, so this was verified
against the authoritative source rather than trusted.

**Proven on-device:** the debug screen in `MainActivity.kt` now uses a
real system file picker (`ActivityResultContracts.OpenDocument()`), reads
the picked file's bytes via `ContentResolver`, and calls
`NativeBridge.loadSave`. Driven end-to-end on the `SaveDex_API36` emulator
(button tap → picker → Downloads → file selection, via `adb shell input
tap` + screenshots) with a 1.58 MB file that is *not* a save PKHeX
recognizes: no crash, the screen correctly shows
`Load failed: Unrecognized save file format.` — proving the full pipeline
(SAF picker → JNI `byte[]` marshaling → `SaveUtil.TryGetSaveFile` → error
path → Kotlin exception → UI) end to end. (An earlier attempt crashed with
`UnsatisfiedLinkError` because the C# `EntryPoint` name didn't match
Kotlin's `external fun` name exactly — JNI symbol resolution is
string-matched, no compile-time check catches a typo like that; fixed and
re-verified.)

**The happy path, verified with a real save.** Before a real save was
available, generating one synthetically with PKHeX itself
(`PKHeX.Core.BlankSaveFile.Get(...)` → `sav.Write()`, see the throwaway
[`TestSaveGen/`](TestSaveGen/) console app) was tried and didn't pan out —
worth keeping as a documented finding even though it's no longer the
blocker: `BlankSaveFile` produces an in-memory starting point meant for
PKHeX's own UI to finish initializing, not a disk-realistic dump —
`SaveUtil.TryGetSaveFile` correctly declined to recognize it, identically
on both a plain in-process .NET round-trip test *and* the on-device JNI
path (good independent confirmation the byte marshaling itself was
already bit-exact, even before a real save closed the loop). Gen8+ (SWSH)
blank saves failed outright with `ArgumentOutOfRangeException` during
`Write()` (SCBlock key/value databases, not flat blobs); Gen3/Gen4 blank
saves threw similarly; Gen1 at least serialized to a real 32 KB file but
still wasn't detected (`SaveUtil`'s `IsG1` checks box/party list
terminators a never-booted save doesn't have populated).

The user then supplied a real save
(`Pokémon Negro 2.sav`, DeSmuME battery file, 512 KB, Pokémon Black 2).
Pushed to the emulator, picked through the app's real system file picker,
and passed through the full pipeline — the app correctly showed:

```
Game: B2
Party count: 2
First party species: Volcarona
First party level: 70
```

— matching the actual save. No crash, no exceptions in logcat. This is
the phase-2 deliverable working end to end, not simulated.

## Part 4 — cold-start and memory (measured, not estimated)

Instrumented [`NativeBridge.kt`](../core-pkhex-bridge/src/main/java/com/savedex/core/pkhexbridge/NativeBridge.kt)
with `System.nanoTime()` around `System.loadLibrary` and around the
`loadSaveNative` call, then measured a genuinely cold run: `am force-stop`
→ fresh `am start` (nothing touches `NativeBridge` during composition —
it's a Kotlin `object`, only initialized on first *reference*, which here
is the button's `onClick`, so the app already demonstrates the phase-2
lazy-init requirement by construction, not just by convention) → tap
through the file picker → select the real Black 2 save. `adb logcat`:

```
System.loadLibrary took 12.29ms
loadSaveNative call took 130.39ms (524288 bytes in)
```

`adb shell dumpsys meminfo com.savedex.app`, before vs. after that same
cold run:

| | PSS | RSS |
|---|---|---|
| Before (app open, bridge untouched) | 88,570 KB | 168,140 KB |
| After (bridge loaded + save parsed) | 108,991 KB | 191,416 KB |
| **Delta** | **+20.4 MB** | **+23.3 MB** |

(`.so mmap`: 11.3 MB, `Native Heap`: 9.5 MB — roughly accounts for the
~17 MB `.so` plus PKHeX.Core's per-generation data tables materializing on
first touch.)

Two things worth taking into phase 7:
- **The library-load cost is trivial (12 ms)** — this is the practical
  payoff of NativeAOT over the ruled-out Mono/JCW path: there's no
  separate managed-runtime bootstrap, no JIT warm-up, just a shared
  library the OS dynamic linker relocates once. A Mono-hosted alternative
  (had it been viable at all — see Part 1) would very plausibly have cost
  a low-single-digit number of *seconds* on first touch, not
  milliseconds.
- **The memory delta (+20 MB PSS) is real and not free**, even with
  NativeAOT's lean startup. This is what makes the phase-2 lazy-init
  requirement matter in practice, not just in principle: +20 MB at app
  launch, paid by every user whether or not they ever touch save-file
  features, would be a real regression; +20 MB only when someone enters
  transfer/advanced mode is a reasonable cost for what it buys.

## What's left

Nothing blocking. Everything the phase-2 brief asked for is done: real
PKHeX.Core dependency, a working bridge (not the originally-imagined
AAR-with-bindings, but the real viable equivalent — see Parts 1/2), Gradle
integration (`:core-pkhex-bridge` ships the `.so` via `jniLibs/`), a debug
screen in `:app` with a real file picker, verified against a real save,
and cold-start/memory both measured. Fair follow-ups for later phases,
not now:
- `arm64` alongside the `x86_64` build tested here (real devices, not just
  the emulator) — same `dotnet publish` command, different
  `RuntimeIdentifier`/`jniLibs/` folder, untested in this session.
- The negative-path error surface is a bare `IllegalArgumentException`
  string; worth richer error types once there's a real UI to design
  around them.

## Toolchain notes / problems hit along the way

- **PKHeX.Core is on NuGet** (`26.8.26`, GPL-3.0-or-later, matches this
  repo's license), no git submodule needed — but that version only ships a
  `net10.0` asset group, forcing the **.NET 10 SDK** rather than .NET 9.
  `PKHeX.Core.dll` itself is **~18 MB** uncompressed.
- **.NET 10 SDK** (`winget install Microsoft.DotNet.SDK.10`) and
  **`dotnet workload install android`** both installed cleanly via winget,
  the latter taking substantially longer (10+ minutes; pulls
  cross-compilation toolchains for all four Android ABIs plus some
  apparently-unrelated emscripten manifests as part of the same workload
  manifest update).
- **JDK mismatch:** Android Studio's bundled JBR is JDK 25; the Android
  SDK tooling (`Microsoft.Android.Sdk.Windows` 36.1.69) hard-requires
  **JDK 21** (`error XA0030`).
- **The JDK 21 MSI installer (`winget install Microsoft.OpenJDK.21`) hung
  indefinitely** waiting on a UAC prompt a non-interactive shell can never
  answer. Worked around with Microsoft's portable JDK 21 **zip**
  (`https://aka.ms/download-jdk/microsoft-jdk-21.0.12-windows-x64.zip`,
  ~200 MB), extracted under `%LOCALAPPDATA%`, passed to builds via
  `-p:JavaSdkDirectory=<path>` — no admin rights needed. Don't use the MSI
  installer non-interactively on Windows.
- **`android_clang.cmd`/NDK cross-compile gotchas** — see the repro section
  above (`DisableUnsupportedError`, 8.3 short path, `PATH`).
- **`./gradlew` can't run directly in this shell** — `java.io.IOException:
  Unable to establish loopback connection`, the same sandbox limitation
  already noted in the top-level README from phase 1 scaffolding (confirmed
  it's not this session's sandbox specifically: still fails with
  `dangerouslyDisableSandbox`). Worked around the same way phase 1 did:
  ran Gradle from WSL Ubuntu (`wsl -d Ubuntu -- bash -lc '...'`), pointing
  `local.properties` at the SDK via `/mnt/c/...` for the duration of the
  build, then restoring the Windows-style path afterward.

## Repository state

`dotnet-bridge/` (all of it, including this README) plus the following are
new/modified and **untracked** — nothing was committed or pushed:

- [`core-pkhex-bridge/src/main/java/com/savedex/core/pkhexbridge/NativeBridge.kt`](../core-pkhex-bridge/src/main/java/com/savedex/core/pkhexbridge/NativeBridge.kt) (new — `add` sanity check + `loadSave`/`SaveSummary`)
- [`core-pkhex-bridge/src/main/jniLibs/x86_64/libPkhexBridgeNative.so`](../core-pkhex-bridge/src/main/jniLibs/x86_64/) (new, binary — built from `PkhexBridge.Native/`, the real bridge, not the PoC)
- [`app/src/main/java/com/savedex/app/MainActivity.kt`](../app/src/main/java/com/savedex/app/MainActivity.kt) (modified — real debug screen: file picker + `NativeBridge.loadSave` + result/error display)

`PkhexBridge.Native.Poc/` and `TestSaveGen/` are throwaways kept for the
record (the PoC proved the harness before the real bridge was built on top
of it; `TestSaveGen` is the blank-save experiment described in Part 3) —
neither is referenced by the shipped `.so`.

The real save file used for verification (Part 3/4) was pushed straight
from its original location on disk to the emulator's `/sdcard/Download/`
and was never copied into this repo or committed anywhere.

System-wide, this session installed (all reversible via their own
uninstallers): .NET SDK 10.0.400, the .NET `android` workload
(36.1.69/10.0.100), Android NDK 27.3.13750724 (via `sdkmanager`), and a
portable JDK 21.0.12 under `%LOCALAPPDATA%\jdk21` (no PATH/env changes
made outside individual build invocations). The `SaveDex_API36` emulator
was left running at the end of this session for further testing.
