package com.mirzadev.onecenter

object NativeAuth {

    init {
        System.loadLibrary("onecenterauth")
    }

    external fun validateEmail(email: String): Boolean

    external fun validatePassword(password: String): Boolean

    external fun validateCredentials(email: String, password: String): Int

    external fun sanitizeEmail(email: String): String
}
