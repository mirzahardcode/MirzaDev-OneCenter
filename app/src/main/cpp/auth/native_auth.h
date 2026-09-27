#ifndef NATIVE_AUTH_H
#define NATIVE_AUTH_H

#include <jni.h>
#include <string>

enum AuthValidationResult {
    VALID = 0,
    INVALID_EMAIL = 1,
    INVALID_PASSWORD = 2,
    INVALID_BOTH = 3
};

class NativeAuth {
public:
    static bool validateEmail(const std::string& email);
    static bool validatePassword(const std::string& password);
    static int validateCredentials(const std::string& email, const std::string& password);
    static std::string sanitizeEmail(const std::string& email);
};

#ifdef __cplusplus
extern "C" {
#endif

JNIEXPORT jboolean JNICALL
Java_com_mirzadev_onecenter_NativeAuth_validateEmail(
        JNIEnv* env,
        jobject thiz,
        jstring email
);

JNIEXPORT jboolean JNICALL
Java_com_mirzadev_onecenter_NativeAuth_validatePassword(
        JNIEnv* env,
        jobject thiz,
        jstring password
);

JNIEXPORT jint JNICALL
Java_com_mirzadev_onecenter_NativeAuth_validateCredentials(
        JNIEnv* env,
        jobject thiz,
        jstring email,
        jstring password
);

JNIEXPORT jstring JNICALL
Java_com_mirzadev_onecenter_NativeAuth_sanitizeEmail(
        JNIEnv* env,
        jobject thiz,
        jstring email
);

#ifdef __cplusplus
}
#endif

#endif // NATIVE_AUTH_H
