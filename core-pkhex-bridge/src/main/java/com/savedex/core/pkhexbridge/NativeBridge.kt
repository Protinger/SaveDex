package com.savedex.core.pkhexbridge

import android.util.Log
import org.json.JSONObject

/**
 * Loads `libPkhexBridgeNative.so` — a NativeAOT-compiled .NET library
 * (PKHeX.Core behind hand-written JNI exports; see
 * `dotnet-bridge/PkhexBridge.Native/`) — and calls into it via raw JNI. No
 * Java Callable Wrapper, no Mono, no Android SDK on the .NET side: that
 * path was tried and ruled out (`dotnet-bridge/README.md`).
 *
 * This is a Kotlin `object`: the JVM only runs [init] the first time
 * [NativeBridge] is *referenced*, not at app startup — so as long as
 * nothing touches this class before the user reaches transfer/advanced
 * mode, the load cost measured here (logged as
 * "PkhexBridge cold start") is naturally deferred already. Timed here,
 * not just asserted, per the phase-2 lazy-init requirement.
 */
object NativeBridge {
    init {
        val start = System.nanoTime()
        System.loadLibrary("PkhexBridgeNative")
        val elapsedMs = (System.nanoTime() - start) / 1_000_000.0
        Log.i("PkhexBridge", "cold start: System.loadLibrary took ${elapsedMs}ms")
    }

    /** Sanity-check export with no PKHeX/JNIEnv marshaling involved. */
    external fun add(a: Int, b: Int): Int

    /**
     * Native side returns a JSON string on success, or null for any
     * failure (unrecognized save format, or an unexpected exception on the
     * .NET side) — there's no thrown Java exception from native code here,
     * deliberately (see NativeExports.cs); [loadSave] below turns null
     * into the same [IllegalArgumentException] contract the original
     * Mono/JCW spike (`PkhexBridge.Android/SaveBridge.cs`) had.
     */
    private external fun loadSaveNative(data: ByteArray): String?

    fun loadSave(data: ByteArray): SaveSummary {
        val start = System.nanoTime()
        val json = loadSaveNative(data)
        val elapsedMs = (System.nanoTime() - start) / 1_000_000.0
        Log.i("PkhexBridge", "loadSaveNative call took ${elapsedMs}ms (${data.size} bytes in)")
        if (json == null)
            throw IllegalArgumentException("Unrecognized save file format.")
        val obj = JSONObject(json)
        return SaveSummary(
            gameName = obj.getString("gameName"),
            partyCount = obj.getInt("partyCount"),
            firstPartySpecies = obj.getString("firstPartySpecies"),
            firstPartyLevel = obj.getInt("firstPartyLevel"),
        )
    }
}

data class SaveSummary(
    val gameName: String,
    val partyCount: Int,
    val firstPartySpecies: String,
    val firstPartyLevel: Int,
)
