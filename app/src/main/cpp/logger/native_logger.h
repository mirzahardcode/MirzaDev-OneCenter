#ifndef NATIVE_LOGGER_H
#define NATIVE_LOGGER_H

#include <jni.h>
#include <android/log.h>

#define DEFAULT_NATIVE_TAG "MirzaDevNative"

#ifdef __cplusplus
extern "C" {
#endif

void native_log_info(const char* tag, const char* message);
void native_log_error(const char* tag, const char* message);
void native_log_info_fmt(const char* tag, const char* fmt, ...);
void native_log_error_fmt(const char* tag, const char* fmt, ...);

JNIEXPORT void JNICALL
Java_com_mirzadev_onecenter_NativeLogger_info(
        JNIEnv* env,
        jobject thiz,
        jstring tag,
        jstring message
);

JNIEXPORT void JNICALL
Java_com_mirzadev_onecenter_NativeLogger_error(
        JNIEnv* env,
        jobject thiz,
        jstring tag,
        jstring message
);

JNIEXPORT void JNICALL
Java_com_mirzadev_onecenter_NativeLogger_logDiagnostics(
        JNIEnv* env,
        jobject thiz
);

#ifdef __cplusplus
}
#endif

#endif // NATIVE_LOGGER_H
