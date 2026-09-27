#ifndef NATIVE_SECURITY_CORE_H
#define NATIVE_SECURITY_CORE_H

#include <string>

// Pure, JNI-free security primitives.
//
// This header intentionally has ZERO Android/JNI dependencies so it can be
// compiled and unit-tested with a normal desktop toolchain (g++/clang), no
// Android NDK required. The JNI-facing glue lives in native_security.h /
// native_security.cpp, which simply forwards into this class. See
// security/tests/native_security_core_test.cpp for the host test suite.
//
// SECURITY NOTE (tamper evidence vs. tamper prevention):
// Nothing here, nor anywhere else in this native library, makes the app
// "impossible to modify". A capable attacker with device access can patch
// the APK, resign it with their own key, or patch this library in memory
// at runtime. What this class provides is a correctly-implemented
// constant-time comparison primitive used elsewhere to build tamper
// EVIDENCE checks — mechanisms that can detect and report a mismatch
// without leaking timing information about where the mismatch occurred.
// Detection is not prevention.
class NativeSecurity {
public:
    // Constant-time comparison of two byte sequences (std::string is used
    // purely as a byte buffer here, not as text). Returns true only if
    // both sequences have equal length and equal content. Total execution
    // time depends on max(len(a), len(b)), not on where the first
    // differing byte is.
    static bool constantTimeEquals(
        const std::string& a,
        const std::string& b
    );

    // Best-effort in-place zeroing of a std::string's backing buffer
    // before it is cleared.
    //
    // LIMITATIONS (documented, not hidden):
    //  - This only touches the one buffer passed in. Any prior copies made
    //    by std::string's copy constructor/assignment, by small-string
    //    optimization, or by the JNI layer converting a jstring to UTF-8,
    //    are NOT retroactively wiped.
    //  - `volatile` stops the compiler from optimizing the write loop
    //    away, but it does not control the OS page cache, swap, or
    //    hibernation images, and it cannot un-page memory already
    //    swapped to disk.
    //  - A JVM/Kotlin `String` is immutable and freely copied/interned by
    //    the runtime; this function cannot and does not claim to erase
    //    Kotlin-side String contents. Callers needing best-effort secure
    //    erasure on the Kotlin side should use a mutable buffer
    //    (CharArray/ByteArray) and overwrite it directly in Kotlin.
    static void secureClear(
        std::string& value
    );
};

#endif // NATIVE_SECURITY_CORE_H
