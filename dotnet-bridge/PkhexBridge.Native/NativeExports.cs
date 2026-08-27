using System.Runtime.InteropServices;
using PKHeX.Core;

namespace SaveDex.PkhexBridge.Native;

/// <summary>
/// Raw JNI export, hand-marshaled (see <see cref="JniEnv"/>) — no Java
/// Callable Wrapper, no Mono, no Android SDK. Loaded from Kotlin via
/// <c>System.loadLibrary("PkhexBridgeNative")</c> +
/// <c>external fun loadSave(data: ByteArray): String?</c>
/// (com.savedex.core.pkhexbridge.NativeBridge).
/// </summary>
public static unsafe class NativeExports
{
    /// <summary>Kept from the round-trip PoC as a lightweight sanity check
    /// that doesn't touch PKHeX or any JNIEnv marshaling.</summary>
    [UnmanagedCallersOnly(EntryPoint = "Java_com_savedex_core_pkhexbridge_NativeBridge_add")]
    public static int Add(nint env, nint clazz, int a, int b) => a + b;

    /// <summary>
    /// Detects the save type and summarizes the lead party Pokémon, as a
    /// minimal hand-built JSON string. Returns null (not a thrown Java
    /// exception — that would need FindClass+ThrowNew, deliberately not
    /// added here) on any failure, including an unrecognized save; the
    /// Kotlin side turns null into an IllegalArgumentException so the
    /// public contract still matches the original (Mono/JCW spike)
    /// SaveBridge.LoadSave design.
    /// </summary>
    [UnmanagedCallersOnly(EntryPoint = "Java_com_savedex_core_pkhexbridge_NativeBridge_loadSaveNative")]
    public static nint LoadSave(nint env, nint thiz, nint jByteArray)
    {
        try
        {
            byte[] data = JniEnv.ReadByteArray(env, jByteArray);

            if (!SaveUtil.TryGetSaveFile(data, out SaveFile? sav))
                return 0;

            var party = sav.PartyData;
            PKM? first = party.Count > 0 ? party[0] : null;
            string species = first is null ? "" : ((Species)first.Species).ToString();
            int level = first?.CurrentLevel ?? 0;

            string json =
                "{\"gameName\":\"" + JsonEscape(sav.Version.ToString()) + "\"," +
                "\"partyCount\":" + sav.PartyCount + "," +
                "\"firstPartySpecies\":\"" + JsonEscape(species) + "\"," +
                "\"firstPartyLevel\":" + level + "}";

            return JniEnv.NewString(env, json);
        }
        catch
        {
            return 0;
        }
    }

    private static string JsonEscape(string s) => s.Replace("\\", "\\\\").Replace("\"", "\\\"");
}
