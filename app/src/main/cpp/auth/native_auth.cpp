#include "native_auth.h"
#include "native_logger.h"

#include <string>
#include <algorithm>

bool NativeAuth::validateEmail(const std::string& email) {
    if (email.empty()) {
        return false;
    }

    // Check for whitespace
    if (email.find(' ') != std::string::npos || email.find('\t') != std::string::npos) {
        return false;
    }

    // Must have exactly one '@'
    size_t atCount = 0;
    size_t atPos = 0;
    for (size_t i = 0; i < email.length(); ++i) {
        if (email[i] == '@') {
            atCount++;
            atPos = i;
        }
    }

    if (atCount != 1) {
        return false;
    }

    // Username (before '@') must not be empty
    std::string username = email.substr(0, atPos);
    if (username.empty()) {
        return false;
    }

    // Domain (after '@') must not be empty
    std::string domain = email.substr(atPos + 1);
    if (domain.empty()) {
        return false;
    }

    // Domain must contain a dot, and cannot start or end with a dot
    if (domain.front() == '.' || domain.back() == '.') {
        return false;
    }

    size_t dotPos = domain.find('.');
    if (dotPos == std::string::npos) {
        return false;
    }

    return true;
}

bool NativeAuth::validatePassword(const std::string& password) {
    if (password.empty() || password.length() < 6) {
        return false;
    }
    return true;
}

int NativeAuth::validateCredentials(const std::string& email, const std::string& password) {
    bool validEmail = validateEmail(email);
    bool validPassword = validatePassword(password);

    int result = INVALID_BOTH;
    if (validEmail && validPassword) {
        result = VALID;
    } else if (!validEmail && validPassword) {
        result = INVALID_EMAIL;
    } else if (validEmail && !validPassword) {
        result = INVALID_PASSWORD;
    } else {
        result = INVALID_BOTH;
    }

    native_log_info_fmt(DEFAULT_NATIVE_TAG, "NativeAuth validation result code: %d", result);
    return result;
}

std::string NativeAuth::sanitizeEmail(const std::string& email) {
    if (email.empty()) {
        return "";
    }

    size_t start = 0;
    while (start < email.length() && (email[start] == ' ' || email[start] == '\t' || email[start] == '\n' || email[start] == '\r')) {
        start++;
    }

    if (start == email.length()) {
        return "";
    }

    size_t end = email.length() - 1;
    while (end > start && (email[end] == ' ' || email[end] == '\t' || email[end] == '\n' || email[end] == '\r')) {
        end--;
    }

    return email.substr(start, end - start + 1);
}

extern "C"
JNIEXPORT jboolean JNICALL
Java_com_mirzadev_onecenter_NativeAuth_validateEmail(
        JNIEnv* env,
        jobject /* thiz */,
        jstring email
) {
    if (email == nullptr) {
        return JNI_FALSE;
    }

    const char* nativeEmail = env->GetStringUTFChars(email, nullptr);
    if (nativeEmail == nullptr) {
        return JNI_FALSE;
    }

    std::string emailStr(nativeEmail);
    env->ReleaseStringUTFChars(email, nativeEmail);

    bool result = NativeAuth::validateEmail(emailStr);
    return result ? JNI_TRUE : JNI_FALSE;
}

extern "C"
JNIEXPORT jboolean JNICALL
Java_com_mirzadev_onecenter_NativeAuth_validatePassword(
        JNIEnv* env,
        jobject /* thiz */,
        jstring password
) {
    if (password == nullptr) {
        return JNI_FALSE;
    }

    const char* nativePassword = env->GetStringUTFChars(password, nullptr);
    if (nativePassword == nullptr) {
        return JNI_FALSE;
    }

    std::string passwordStr(nativePassword);
    env->ReleaseStringUTFChars(password, nativePassword);

    bool result = NativeAuth::validatePassword(passwordStr);
    return result ? JNI_TRUE : JNI_FALSE;
}

extern "C"
JNIEXPORT jint JNICALL
Java_com_mirzadev_onecenter_NativeAuth_validateCredentials(
        JNIEnv* env,
        jobject /* thiz */,
        jstring email,
        jstring password
) {
    if (email == nullptr || password == nullptr) {
        return INVALID_BOTH;
    }

    const char* nativeEmail = env->GetStringUTFChars(email, nullptr);
    const char* nativePassword = env->GetStringUTFChars(password, nullptr);

    if (nativeEmail == nullptr || nativePassword == nullptr) {
        if (nativeEmail != nullptr) env->ReleaseStringUTFChars(email, nativeEmail);
        if (nativePassword != nullptr) env->ReleaseStringUTFChars(password, nativePassword);
        return INVALID_BOTH;
    }

    std::string emailStr(nativeEmail);
    std::string passwordStr(nativePassword);

    env->ReleaseStringUTFChars(email, nativeEmail);
    env->ReleaseStringUTFChars(password, nativePassword);

    return NativeAuth::validateCredentials(emailStr, passwordStr);
}

extern "C"
JNIEXPORT jstring JNICALL
Java_com_mirzadev_onecenter_NativeAuth_sanitizeEmail(
        JNIEnv* env,
        jobject /* thiz */,
        jstring email
) {
    if (email == nullptr) {
        return env->NewStringUTF("");
    }

    const char* nativeEmail = env->GetStringUTFChars(email, nullptr);
    if (nativeEmail == nullptr) {
        return env->NewStringUTF("");
    }

    std::string emailStr(nativeEmail);
    env->ReleaseStringUTFChars(email, nativeEmail);

    std::string sanitized = NativeAuth::sanitizeEmail(emailStr);
    return env->NewStringUTF(sanitized.c_str());
}
