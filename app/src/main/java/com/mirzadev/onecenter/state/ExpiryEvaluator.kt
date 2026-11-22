package com.mirzadev.onecenter.state

/**
 * Pure Kotlin evaluator for Private session subscription validity.
 *
 * Evaluates subscription status based on user existence, active flag,
 * device binding, and absolute expiration timestamp (expiresAt) in milliseconds.
 */
object ExpiryEvaluator {

    enum class ExpiryStatus {
        VALID,
        EXPIRED,
        MISSING_EXPIRY,
        INACTIVE,
        DEVICE_MISMATCH,
        USER_NOT_FOUND
    }

    /**
     * Evaluates subscription validity.
     *
     * @param userExists Whether the user node exists in Firebase Realtime Database
     * @param active The value of the `active` field (defaults to true if null/omitted)
     * @param registeredDeviceId The registered deviceId in the database
     * @param currentDeviceId The current device's hardware/secure ID
     * @param expiresAtMs The absolute expiration timestamp in milliseconds from epoch
     * @param currentTimeMs Current timestamp (preferably including server time offset)
     */
    fun evaluate(
        userExists: Boolean,
        active: Boolean?,
        registeredDeviceId: String?,
        currentDeviceId: String,
        expiresAtMs: Long?,
        currentTimeMs: Long
    ): ExpiryStatus {
        if (!userExists) return ExpiryStatus.USER_NOT_FOUND
        if (active == false) return ExpiryStatus.INACTIVE
        if (!registeredDeviceId.isNullOrBlank() && registeredDeviceId != currentDeviceId) {
            return ExpiryStatus.DEVICE_MISMATCH
        }
        if (expiresAtMs == null) return ExpiryStatus.MISSING_EXPIRY

        // Normalize timestamps given in seconds (e.g. 10 digits like 1790592000) to milliseconds
        val normalizedExpiresAt = if (expiresAtMs < 100_000_000_000L) {
            expiresAtMs * 1000L
        } else {
            expiresAtMs
        }

        if (currentTimeMs >= normalizedExpiresAt) return ExpiryStatus.EXPIRED
        return ExpiryStatus.VALID
    }

    /**
     * Returns a user-friendly error message for a non-VALID [ExpiryStatus].
     */
    fun getErrorMessage(status: ExpiryStatus): String? {
        return when (status) {
            ExpiryStatus.VALID -> null
            ExpiryStatus.EXPIRED -> "Akses Private telah berakhir. Silakan perbarui akses Anda."
            ExpiryStatus.MISSING_EXPIRY -> "Akses Private telah berakhir atau belum diatur. Silakan perbarui akses Anda."
            ExpiryStatus.INACTIVE -> "Akses akun telah dinonaktifkan."
            ExpiryStatus.DEVICE_MISMATCH -> "Device tidak diizinkan untuk akun ini."
            ExpiryStatus.USER_NOT_FOUND -> "Akun belum terdaftar di sistem."
        }
    }
}
