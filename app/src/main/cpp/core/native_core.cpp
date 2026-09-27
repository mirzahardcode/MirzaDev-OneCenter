#include "native_core.h"
#include "native_logger.h"

#include <string>
#include <cstdint>
#include <sstream>
#include <iomanip>
#include <android/api-level.h>
#include <sys/system_properties.h>
#include <dlfcn.h>

extern "C"
JNIEXPORT void JNICALL
Java_com_mirzadev_onecenter_NativeCore_logDiagnostics(
        JNIEnv* env,
        jobject /* thiz */
) {
    native_log_info(DEFAULT_NATIVE_TAG, "=== NativeCore Diagnostics ===");

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

    int length = __system_property_get(
            "ro.build.version.sdk",
            sdkVersion
    );

    if (length > 0) {
        native_log_info_fmt(DEFAULT_NATIVE_TAG, "Android API: %s", sdkVersion);
    } else {
        native_log_error(DEFAULT_NATIVE_TAG, "Android API: failed to read");
    }

    native_log_info(DEFAULT_NATIVE_TAG, "NativeCore version: 1.1.0");
    native_log_info(DEFAULT_NATIVE_TAG, "=== End Diagnostics ===");
}

extern "C"
JNIEXPORT jstring JNICALL
Java_com_mirzadev_onecenter_NativeCore_getNativeLibraryPath(
        JNIEnv* env,
        jobject /* thiz */
) {
    Dl_info info{};

    if (dladdr(
            reinterpret_cast<void*>(
                    &Java_com_mirzadev_onecenter_NativeCore_getNativeLibraryPath
            ),
            &info
    ) == 0) {
        return env->NewStringUTF("unknown");
    }

    if (info.dli_fname == nullptr) {
        return env->NewStringUTF("unknown");
    }

    return env->NewStringUTF(info.dli_fname);
}

extern "C"
JNIEXPORT jstring JNICALL
Java_com_mirzadev_onecenter_NativeCore_getDiagnostics(
        JNIEnv* env,
        jobject /* thiz */
) {
    std::stringstream output;

    output << "=== NativeCore Diagnostics ===\n";

#if defined(__aarch64__)
    output << "ABI: arm64-v8a\n";
#elif defined(__arm__)
    output << "ABI: armeabi-v7a\n";
#elif defined(__x86_64__)
    output << "ABI: x86_64\n";
#elif defined(__i386__)
    output << "ABI: x86\n";
#else
    output << "ABI: unknown\n";
#endif

#if defined(__aarch64__) || defined(__x86_64__)
    output << "64-bit: true\n";
#else
    output << "64-bit: false\n";
#endif

    char sdkVersion[PROP_VALUE_MAX] = {};

    int length = __system_property_get(
            "ro.build.version.sdk",
            sdkVersion
    );

    if (length > 0) {
        output << "Android API: " << sdkVersion << "\n";
    } else {
        output << "Android API: unknown\n";
    }

    output << "NativeCore version: 1.1.0\n";
    output << "=== End Diagnostics ===";

    const std::string result = output.str();

    return env->NewStringUTF(result.c_str());
}

extern "C"
JNIEXPORT jstring JNICALL
Java_com_mirzadev_onecenter_NativeCore_getDeviceAbis(
        JNIEnv* env,
        jobject /* thiz */
) {
    char abiList[PROP_VALUE_MAX] = {};

    int length = __system_property_get(
            "ro.product.cpu.abilist",
            abiList
    );

    if (length <= 0) {
        length = __system_property_get(
                "ro.product.cpu.abi",
                abiList
        );
    }

    if (length <= 0) {
        return env->NewStringUTF("unknown");
    }

    return env->NewStringUTF(abiList);
}

extern "C"
JNIEXPORT jboolean JNICALL
Java_com_mirzadev_onecenter_NativeCore_checkAbiCompatibility(
        JNIEnv* env,
        jobject /* thiz */
) {
    char abiList[PROP_VALUE_MAX] = {};

    int length = __system_property_get(
            "ro.product.cpu.abilist",
            abiList
    );

    if (length <= 0) {
        return JNI_FALSE;
    }

#if defined(__aarch64__)
    const std::string nativeAbi = "arm64-v8a";
#elif defined(__arm__)
    const std::string nativeAbi = "armeabi-v7a";
#elif defined(__x86_64__)
    const std::string nativeAbi = "x86_64";
#elif defined(__i386__)
    const std::string nativeAbi = "x86";
#else
    const std::string nativeAbi = "";
#endif

    if (nativeAbi.empty()) {
        return JNI_FALSE;
    }

    const std::string supportedAbis(abiList);

    return supportedAbis.find(nativeAbi) != std::string::npos
           ? JNI_TRUE
           : JNI_FALSE;
}

extern "C"
JNIEXPORT jstring JNICALL
Java_com_mirzadev_onecenter_NativeCore_getNativeVersion(
        JNIEnv* env,
        jobject /* thiz */
) {
    const std::string version = "1.0.0";
    return env->NewStringUTF(version.c_str());
}

extern "C"
JNIEXPORT jstring JNICALL
Java_com_mirzadev_onecenter_NativeCore_calculateChecksum(
        JNIEnv* env,
        jobject /* thiz */,
        jstring input
) {
    if (input == nullptr) {
        return env->NewStringUTF("");
    }

    const char* text = env->GetStringUTFChars(input, nullptr);
    const jsize length = env->GetStringUTFLength(input);

    uint32_t crc = 0xFFFFFFFF;

    for (jsize i = 0; i < length; ++i) {
        crc ^= static_cast<uint8_t>(text[i]);

        for (int j = 0; j < 8; ++j) {
            if (crc & 1U) {
                crc = (crc >> 1U) ^ 0xEDB88320U;
            } else {
                crc >>= 1U;
            }
        }
    }

    crc ^= 0xFFFFFFFFU;

    env->ReleaseStringUTFChars(input, text);

    std::stringstream result;
    result << std::uppercase
           << std::hex
           << std::setw(8)
           << std::setfill('0')
           << crc;

    const std::string checksum = result.str();
    return env->NewStringUTF(checksum.c_str());
}

extern "C"
JNIEXPORT jboolean JNICALL
Java_com_mirzadev_onecenter_NativeCore_validateInput(
        JNIEnv* env,
        jobject /* thiz */,
        jstring input
) {
    if (input == nullptr) {
        return JNI_FALSE;
    }

    const jsize length = env->GetStringUTFLength(input);

    return length >= 4 ? JNI_TRUE : JNI_FALSE;
}

extern "C"
JNIEXPORT jstring JNICALL
Java_com_mirzadev_onecenter_NativeCore_getNativePlatform(
        JNIEnv* env,
        jobject /* thiz */
) {
#if defined(__aarch64__)
    return env->NewStringUTF("arm64-v8a");
#elif defined(__arm__)
    return env->NewStringUTF("armeabi-v7a");
#elif defined(__x86_64__)
    return env->NewStringUTF("x86_64");
#elif defined(__i386__)
    return env->NewStringUTF("x86");
#else
    return env->NewStringUTF("unknown");
#endif
}

extern "C"
JNIEXPORT jint JNICALL
Java_com_mirzadev_onecenter_NativeCore_compareVersions(
        JNIEnv* env,
        jobject /* thiz */,
        jstring versionA,
        jstring versionB
) {
    if (versionA == nullptr || versionB == nullptr) {
        return 0;
    }

    const char* a = env->GetStringUTFChars(versionA, nullptr);
    const char* b = env->GetStringUTFChars(versionB, nullptr);

    std::string strA(a);
    std::string strB(b);

    env->ReleaseStringUTFChars(versionA, a);
    env->ReleaseStringUTFChars(versionB, b);

    if (!strA.empty() && strA[0] == 'v') {
        strA.erase(0, 1);
    }

    if (!strB.empty() && strB[0] == 'v') {
        strB.erase(0, 1);
    }

    std::stringstream streamA(strA);
    std::stringstream streamB(strB);

    int a1 = 0, a2 = 0, a3 = 0;
    int b1 = 0, b2 = 0, b3 = 0;

    char dot;

    streamA >> a1 >> dot >> a2 >> dot >> a3;
    streamB >> b1 >> dot >> b2 >> dot >> b3;

    if (a1 != b1) {
        return a1 > b1 ? 1 : -1;
    }

    if (a2 != b2) {
        return a2 > b2 ? 1 : -1;
    }

    if (a3 != b3) {
        return a3 > b3 ? 1 : -1;
    }

    return 0;
}

extern "C"
JNIEXPORT jboolean JNICALL
Java_com_mirzadev_onecenter_NativeCore_isNative64Bit(
        JNIEnv* env,
        jobject /* thiz */
) {
#if defined(__aarch64__) || defined(__x86_64__)
    return JNI_TRUE;
#else
    return JNI_FALSE;
#endif
}

extern "C"
JNIEXPORT jint JNICALL
Java_com_mirzadev_onecenter_NativeCore_getAndroidApiLevel(
        JNIEnv* env,
        jobject /* thiz */
) {
    char sdkVersion[PROP_VALUE_MAX] = {};

    int length = __system_property_get(
            "ro.build.version.sdk",
            sdkVersion
    );

    if (length <= 0) {
        return -1;
    }

    try {
        return std::stoi(sdkVersion);
    } catch (...) {
        return -1;
    }
}
