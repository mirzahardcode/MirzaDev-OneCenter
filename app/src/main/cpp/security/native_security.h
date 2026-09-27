#ifndef NATIVE_SECURITY_H
#define NATIVE_SECURITY_H

#include "native_security_core.h"
#include <jni.h>

// JNI glue only. The actual algorithms live in native_security_core.h/.cpp
// (see that header for the full tamper-evidence-vs-prevention disclaimer
// and the secureClear limitations). Keeping this split means:
//   1. The core logic has zero JNI/Android dependency and is host-testable
//      (see security/tests/native_security_core_test.cpp).
//   2. These JNI exports and their Java_... symbol names are UNCHANGED
//      from the original implementation, so no Kotlin-side call site needs
//      to change.

#ifdef __cplusplus
extern "C" {
#endif

JNIEXPORT jboolean JNICALL
Java_com_mirzadev_onecenter_NativeSecurity_constantTimeEquals(
        JNIEnv* env,
        jobject thiz,
        jstring a,
        jstring b
);

JNIEXPORT jboolean JNICALL
Java_com_mirzadev_onecenter_NativeSecurity_verifySigningDigest(
        JNIEnv* env,
        jobject thiz,
        jstring actual_digest,
        jstring expected_digest
);

#ifdef __cplusplus
}
#endif

#endif // NATIVE_SECURITY_H
