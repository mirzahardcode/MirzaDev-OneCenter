#include "native_logger.h"
#include <cstdio>
#include <cstdarg>
#include <sys/system_properties.h>

void native_log_info(const char* tag, const char* message) {
    const char* logTag = (tag != nullptr && tag[0] != '\0') ? tag : DEFAULT_NATIVE_TAG;
    __android_log_print(ANDROID_LOG_INFO, logTag, "%s", message ? message : "");
}

void native_log_error(const char* tag, const char* message) {
    const char* logTag = (tag != nullptr && tag[0] != '\0') ? tag : DEFAULT_NATIVE_TAG;
    __android_log_print(ANDROID_LOG_ERROR, logTag, "%s", message ? message : "");
}

void native_log_info_fmt(const char* tag, const char* fmt, ...) {
    const char* logTag = (tag != nullptr && tag[0] != '\0') ? tag : DEFAULT_NATIVE_TAG;
    va_list args;
    va_start(args, fmt);
    __android_log_vprint(ANDROID_LOG_INFO, logTag, fmt, args);
    va_end(args);
}

void native_log_error_fmt(const char* tag, const char* fmt, ...) {
    const char* logTag = (tag != nullptr && tag[0] != '\0') ? tag : DEFAULT_NATIVE_TAG;
    va_list args;
    va_start(args, fmt);
    __android_log_vprint(ANDROID_LOG_ERROR, logTag, fmt, args);
    va_end(args);
}

extern "C"
JNIEXPORT void JNICALL
Java_com_mirzadev_onecenter_NativeLogger_info(
        JNIEnv* env,
        jobject /* thiz */,
        jstring tag,
        jstring message
) {
    if (message == nullptr) return;

    const char* nativeMessage = env->GetStringUTFChars(message, nullptr);
    const char* nativeTag = nullptr;

    if (tag != nullptr) {
        nativeTag = env->GetStringUTFChars(tag, nullptr);
    }

    native_log_info(nativeTag, nativeMessage);

    if (nativeTag != nullptr) {
        env->ReleaseStringUTFChars(tag, nativeTag);
    }
    env->ReleaseStringUTFChars(message, nativeMessage);
}

extern "C"
JNIEXPORT void JNICALL
Java_com_mirzadev_onecenter_NativeLogger_error(
        JNIEnv* env,
        jobject /* thiz */,
        jstring tag,
        jstring message
) {
    if (message == nullptr) return;

    const char* nativeMessage = env->GetStringUTFChars(message, nullptr);
    const char* nativeTag = nullptr;

    if (tag != nullptr) {
        nativeTag = env->GetStringUTFChars(tag, nullptr);
    }

    native_log_error(nativeTag, nativeMessage);

    if (nativeTag != nullptr) {
        env->ReleaseStringUTFChars(tag, nativeTag);
    }
    env->ReleaseStringUTFChars(message, nativeMessage);
}

extern "C"
JNIEXPORT void JNICALL
Java_com_mirzadev_onecenter_NativeLogger_logDiagnostics(
        JNIEnv* env,
        jobject /* thiz */
) {
    native_log_info(DEFAULT_NATIVE_TAG, "=== NativeLogger Diagnostics ===");

#if defined(__aarch64__)
    native_log_info(DEFAULT_NATIVE_TAG, "ABI: arm64-v8a");
#elif defined(__arm__)
    native_log_info(DEFAULT_NATIVE_TAG, "ABI: armeabi-v7a");
#elif defined(__x86_64__)
    native_log_info(DEFAULT_NATIVE_TAG, "ABI: x86_64");
#elif defined(__i386__)
    native_log_info(DEFAULT_NATIVE_TAG, "ABI: x86");
#else
    native_log_info(DEFAULT_NATIVE_TAG, "ABI: unknown");
#endif

#if defined(__aarch64__) || defined(__x86_64__)
    native_log_info(DEFAULT_NATIVE_TAG, "64-bit: true");
#else
    native_log_info(DEFAULT_NATIVE_TAG, "64-bit: false");
#endif

    char sdkVersion[PROP_VALUE_MAX] = {};
    int length = __system_property_get("ro.build.version.sdk", sdkVersion);

    if (length > 0) {
        native_log_info_fmt(DEFAULT_NATIVE_TAG, "Android API: %s", sdkVersion);
    } else {
        native_log_error(DEFAULT_NATIVE_TAG, "Android API: failed to read");
    }

    native_log_info(DEFAULT_NATIVE_TAG, "NativeLogger version: 1.0.0");
    native_log_info(DEFAULT_NATIVE_TAG, "=== End Diagnostics ===");
}
