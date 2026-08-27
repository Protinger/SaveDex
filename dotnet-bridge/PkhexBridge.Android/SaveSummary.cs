namespace SaveDex.PkhexBridge;

/// <summary>
/// Minimal Java-callable result of a save-file load. Must derive from
/// <see cref="Java.Lang.Object"/> (not be a plain C# POCO) for the
/// .NET-for-Android build to emit a Java Callable Wrapper for it in the AAR.
/// </summary>
public sealed class SaveSummary : Java.Lang.Object
{
    public string GameName { get; set; } = string.Empty;
    public int PartyCount { get; set; }
    public string FirstPartySpecies { get; set; } = string.Empty;
    public int FirstPartyLevel { get; set; }
}
