using System.Runtime.InteropServices;

namespace SaveDex.PkhexBridge;

/// <summary>
/// Raw-JNI spike: only used under `dotnet publish -p:PublishAot=true`,
/// alongside &lt;NativeLib&gt;Shared&lt;/NativeLib&gt; in the csproj. Exists
/// to test whether the Android SDK's NativeAOT NDK cross-compilation setup
/// (see dotnet-bridge/README.md) actually produces a loadable .so, before
/// investing in real JNI marshaling for <see cref="SaveBridge"/>. Unrelated
/// to <see cref="SaveBridge"/>/<see cref="SaveSummary"/>, which are the
/// (ruled-out) automatic-JCW/Mono spike.
/// </summary>
public static class NativeExports
{
    [UnmanagedCallersOnly(EntryPoint = "Java_com_savedex_core_pkhexbridge_NativeBridge_add")]
    public static int Add(nint env, nint clazz, int a, int b) => a + b;
}
