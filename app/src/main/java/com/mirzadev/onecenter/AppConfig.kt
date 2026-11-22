package com.mirzadev.onecenter

/**
 * Single place where app-level configuration lives.
 */
object AppConfig {

    /**
     * Backend base URL for latest information and private documents endpoints.
     * Configured via BuildConfig per build variant (Staging vs Production).
     */
    val BACKEND_BASE_URL: String
        get() {
            val url = try {
                val field = BuildConfig::class.java.getField("BACKEND_BASE_URL")
                field.get(null) as? String
            } catch (_: Exception) {
                null
            }
            if (url.isNullOrBlank()) {
                throw IllegalStateException("Konfigurasi BACKEND_BASE_URL belum ditentukan pada build variant ini.")
            }
            return url
        }

    const val RELEASES_REPO = "mirzahardcode/cracking-exam-app"

    const val CACHE_TTL_MILLIS = 30 * 60 * 1000L
}
