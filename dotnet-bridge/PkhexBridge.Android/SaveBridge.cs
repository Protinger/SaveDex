using PKHeX.Core;

namespace SaveDex.PkhexBridge;

/// <summary>
/// Entry point exposed to Kotlin/Java. Must derive from
/// <see cref="Java.Lang.Object"/> so the .NET-for-Android build emits a Java
/// Callable Wrapper for it.
/// </summary>
public sealed class SaveBridge : Java.Lang.Object
{
    /// <summary>
    /// Detects the save type and summarizes the lead party Pokémon.
    /// Throws <see cref="Java.Lang.IllegalArgumentException"/> (a real Java
    /// exception, catchable from Kotlin) if <paramref name="data"/> isn't a
    /// save PKHeX recognizes.
    /// </summary>
    public SaveSummary LoadSave(byte[] data)
    {
        if (!SaveUtil.TryGetSaveFile(data, out var sav))
            throw new Java.Lang.IllegalArgumentException("Unrecognized save file format.");

        var party = sav.PartyData;
        var first = party.Count > 0 ? party[0] : null;

        return new SaveSummary
        {
            GameName = sav.Version.ToString(),
            PartyCount = sav.PartyCount,
            FirstPartySpecies = first is null ? string.Empty : ((Species)first.Species).ToString(),
            FirstPartyLevel = first?.CurrentLevel ?? 0,
        };
    }
}
