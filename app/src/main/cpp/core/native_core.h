#ifndef NATIVE_CORE_H
#define NATIVE_CORE_H

#include <jni.h>

#ifdef __cplusplus
extern "C" {
#endif

JNIEXPORT void JNICALL
Java_com_mirzadev_onecenter_NativeCore_logDiagnostics(
        JNIEnv* env,
        jobject thiz
);

JNIEXPORT jstring JNICALL
Java_com_mirzadev_onecenter_NativeCore_getNativeLibraryPath(
        JNIEnv* env,
        jobject thiz
);

JNIEXPORT jstring JNICALL
Java_com_mirzadev_onecenter_NativeCore_getDiagnostics(
        JNIEnv* env,
        jobject thiz
);

JNIEXPORT jstring JNICALL
Java_com_mirzadev_onecenter_NativeCore_getDeviceAbis(
        JNIEnv* env,
        jobject thiz
);

JNIEXPORT jboolean JNICALL
Java_com_mirzadev_onecenter_NativeCore_checkAbiCompatibility(
        JNIEnv* env,
        jobject thiz
);

JNIEXPORT jstring JNICALL
Java_com_mirzadev_onecenter_NativeCore_getNativeVersion(
        JNIEnv* env,
        jobject thiz
);

JNIEXPORT jstring JNICALL
Java_com_mirzadev_onecenter_NativeCore_calculateChecksum(
        JNIEnv* env,
        jobject thiz,
        jstring input
);

JNIEXPORT jboolean JNICALL
Java_com_mirzadev_onecenter_NativeCore_validateInput(
        JNIEnv* env,
        jobject thiz,
        jstring input
);

JNIEXPORT jstring JNICALL
Java_com_mirzadev_onecenter_NativeCore_getNativePlatform(
        JNIEnv* env,
        jobject thiz
);

JNIEXPORT jint JNICALL
Java_com_mirzadev_onecenter_NativeCore_compareVersions(
        JNIEnv* env,
        jobject thiz,
        jstring versionA,
        jstring versionB
);

JNIEXPORT jboolean JNICALL
Java_com_mirzadev_onecenter_NativeCore_isNative64Bit(
        JNIEnv* env,
        jobject thiz
);

JNIEXPORT jint JNICALL
Java_com_mirzadev_onecenter_NativeCore_getAndroidApiLevel(
        JNIEnv* env,
        jobject thiz
);

#ifdef __cplusplus
}
#endif

#endif // NATIVE_CORE_H
