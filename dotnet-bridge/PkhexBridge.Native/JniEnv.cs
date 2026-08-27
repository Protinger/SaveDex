namespace SaveDex.PkhexBridge.Native;

/// <summary>
/// Minimal hand-rolled JNI interop: just the four functions this bridge
/// needs (read a jbyteArray, allocate a jstring, throw). No general-purpose
/// JNI wrapper library — deliberately narrow, so every offset here is
/// checked against the ABI, not trusted blind.
///
/// <c>JNIEnv*</c> (what <c>[UnmanagedCallersOnly]</c> exports receive as
/// their first parameter) is, per the JNI spec, a pointer to a pointer to a
/// <c>JNINativeInterface</c> function table: <c>struct { const
/// JNINativeInterface* functions; } *env;</c>. Function pointers are laid
/// out in that struct in a fixed, ABI-stable order (unchanged since JDK
/// 1.2) — the indices below were computed by parsing the actual
/// <c>jni.h</c> shipped in Android NDK 27.3.13750724
/// (<c>toolchains/llvm/prebuilt/windows-x86_64/sysroot/usr/include/jni.h</c>),
/// not copied from a third party:
/// <c>ThrowNew</c>=14, <c>NewStringUTF</c>=167, <c>GetArrayLength</c>=171,
/// <c>GetByteArrayRegion</c>=200 (0-based, counting the 4 leading
/// <c>reserved0..3</c> slots).
/// </summary>
internal static unsafe class JniEnv
{
    private static IntPtr GetFunction(nint env, int vtableIndex)
    {
        // env -> JNINativeInterface** -> JNINativeInterface* -> functions[index]
        IntPtr functions = *(IntPtr*)env;
        return ((IntPtr*)functions)[vtableIndex];
    }

    public static int GetArrayLength(nint env, nint array)
    {
        var fn = (delegate* unmanaged<nint, nint, int>)GetFunction(env, 171);
        return fn(env, array);
    }

    public static void GetByteArrayRegion(nint env, nint array, int start, int length, byte* buffer)
    {
        var fn = (delegate* unmanaged<nint, nint, int, int, byte*, void>)GetFunction(env, 200);
        fn(env, array, start, length, buffer);
    }

    public static nint NewStringUtf(nint env, byte* utf8NullTerminated)
    {
        var fn = (delegate* unmanaged<nint, byte*, nint>)GetFunction(env, 167);
        return fn(env, utf8NullTerminated);
    }

    /// <summary>Reads the full contents of a Java byte[] into a managed array.</summary>
    public static byte[] ReadByteArray(nint env, nint array)
    {
        int length = GetArrayLength(env, array);
        byte[] data = new byte[length];
        fixed (byte* buffer = data)
        {
            GetByteArrayRegion(env, array, 0, length, buffer);
        }
        return data;
    }

    /// <summary>Allocates a Java String from a UTF-8 managed string.</summary>
    public static nint NewString(nint env, string value)
    {
        byte[] utf8 = System.Text.Encoding.UTF8.GetBytes(value + "\0");
        fixed (byte* p = utf8)
        {
            return NewStringUtf(env, p);
        }
    }
}
