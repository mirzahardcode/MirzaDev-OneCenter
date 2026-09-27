#include "native_security.h"

namespace {

// Copies a jstring's modified-UTF-8 bytes into `out` using the JVM-reported
// byte length (GetStringUTFLength) rather than relying on the buffer being
// null-terminated where expected. This is strictly more robust than a
// strlen()-based copy and is the recommended JNI pattern for byte-accurate
// extraction. Returns false (leaving `out` untouched) if the string could
// not be read, including when a pending JNI exception is detected.
bool copyJString(JNIEnv* env, jstring src, std::string& out) {
    if (env == nullptr || src == nullptr) {
        return false;
    }

    const jsize byteLength = env->GetStringUTFLength(src);
    if (env->ExceptionCheck()) {
        env->ExceptionClear();
        return false;
    }

    const char* chars = env->GetStringUTFChars(src, nullptr);
    if (chars == nullptr) {
        // GetStringUTFChars failed (e.g. OOM). The JVM has already thrown;
        // let that exception propagate back to the caller instead of
        // masking it. Per the JNI spec, a native method may still return a
        // value here — the JVM will throw the pending exception right
        // after this native call returns, and the returned value is
        // discarded.
        return false;
    }

    out.assign(chars, static_cast<size_t>(byteLength));
    env->ReleaseStringUTFChars(src, chars);
    return true;
}

} // namespace

extern "C"
JNIEXPORT jboolean JNICALL
Java_com_mirzadev_onecenter_NativeSecurity_constantTimeEquals(
        JNIEnv* env,
        jobject /* thiz */,
        jstring a,
        jstring b
) {
    if (a == nullptr || b == nullptr) {
        // Treat "both null" as trivially equal, anything else as not
        // equal — matches the null-safety expectations of Kotlin callers.
        return (a == b) ? JNI_TRUE : JNI_FALSE;
    }

    std::string strA;
    std::string strB;

    if (!copyJString(env, a, strA) || !copyJString(env, b, strB)) {
        NativeSecurity::secureClear(strA);
        NativeSecurity::secureClear(strB);
        return JNI_FALSE;
    }

    const bool result = NativeSecurity::constantTimeEquals(strA, strB);

    NativeSecurity::secureClear(strA);
    NativeSecurity::secureClear(strB);

    return result ? JNI_TRUE : JNI_FALSE;
}

extern "C"
JNIEXPORT jboolean JNICALL
Java_com_mirzadev_onecenter_NativeSecurity_verifySigningDigest(
        JNIEnv* env,
        jobject /* thiz */,
        jstring actual_digest,
        jstring expected_digest
) {
    if (actual_digest == nullptr || expected_digest == nullptr) {
        return (actual_digest == expected_digest) ? JNI_TRUE : JNI_FALSE;
    }

    std::string strActual;
    std::string strExpected;

    if (!copyJString(env, actual_digest, strActual) || !copyJString(env, expected_digest, strExpected)) {
        NativeSecurity::secureClear(strActual);
        NativeSecurity::secureClear(strExpected);
        return JNI_FALSE;
    }

    const bool result = NativeSecurity::constantTimeEquals(strActual, strExpected);

    NativeSecurity::secureClear(strActual);
    NativeSecurity::secureClear(strExpected);

    return result ? JNI_TRUE : JNI_FALSE;
}
