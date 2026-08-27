using System.Runtime.InteropServices;

namespace SaveDex.PkhexBridge.Native;

/// <summary>
/// Hand-written JNI export, following the JNI naming convention
/// (Java_&lt;package&gt;_&lt;Class&gt;_&lt;method&gt;) so the JVM's dynamic
/// linker resolves it via `System.loadLibrary` + `external fun` without any
/// explicit `RegisterNatives` call. No JNIEnv access needed for this proof
/// (no strings/arrays crossing the boundary), so the JNIEnv*/jclass
/// parameters are accepted but unused.
/// </summary>
public static class NativeExports
{
    [UnmanagedCallersOnly(EntryPoint = "Java_com_savedex_core_pkhexbridge_NativeBridge_add")]
    public static int Add(nint env, nint clazz, int a, int b) => a + b;
}
