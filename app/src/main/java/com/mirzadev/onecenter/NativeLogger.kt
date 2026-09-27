package com.mirzadev.onecenter

object NativeLogger {

    init {
        System.loadLibrary("onecenterlogger")
    }

    external fun info(tag: String, message: String)

    external fun error(tag: String, message: String)

    external fun logDiagnostics()
}
