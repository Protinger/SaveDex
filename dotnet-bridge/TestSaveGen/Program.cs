using PKHeX.Core;

// Gen8+ (SWSH) saves are SCBlock-based key/value databases; Write() alone
// doesn't reproduce a byte-exact file SaveUtil's detector recognizes.
// Emerald is a simple flat, checksummed blob - much likelier to round-trip.
var sav = BlankSaveFile.Get(GameVersion.RD, "SaveDex", LanguageID.English);
if (sav is SAV1 sav1)
    sav1.BoxesInitialized = true;

byte[] bytes = sav.Write().ToArray();
File.WriteAllBytes(args[0], bytes);
Console.WriteLine($"Wrote {bytes.Length} bytes to {args[0]} (Version={sav.Version}, PartyCount={sav.PartyCount})");

// Round-trip check: does SaveUtil recognize the exact bytes we just wrote?
byte[] readBack = File.ReadAllBytes(args[0]);
bool recognized = SaveUtil.TryGetSaveFile(readBack, out var sav2);
Console.WriteLine($"Round-trip TryGetSaveFile: recognized={recognized}, Version={sav2?.Version}, PartyCount={sav2?.PartyCount}");
