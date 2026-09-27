package com.mirzadev.onecenter

object NativeCore {

    init {
        System.loadLibrary("onecenterlogger")
        System.loadLibrary("onecentercore")
    }

    external fun getNativeVersion(): String

    external fun calculateChecksum(input: String): String

    external fun validateInput(input: String): Boolean

    external fun getNativePlatform(): String

    external fun compareVersions(
        versionA: String,
        versionB: String
    ): Int

    external fun isNative64Bit(): Boolean

    external fun getAndroidApiLevel(): Int

    external fun logDiagnostics()

    external fun getDiagnostics(): String

    external fun getDeviceAbis(): String

    external fun checkAbiCompatibility(): Boolean

    external fun getNativeLibraryPath(): String
}
