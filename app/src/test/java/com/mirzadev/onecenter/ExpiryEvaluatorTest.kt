package com.mirzadev.onecenter

import com.mirzadev.onecenter.state.ExpiryEvaluator
import com.mirzadev.onecenter.state.ExpiryEvaluator.ExpiryStatus
import org.junit.Assert.assertEquals
import org.junit.Test

class ExpiryEvaluatorTest {

    private val deviceId = "test-device-123"
    private val now = 1000000000000L

    @Test
    fun userNotFound_returnsUserNotFound() {
        val status = ExpiryEvaluator.evaluate(
            userExists = false,
            active = true,
            registeredDeviceId = deviceId,
            currentDeviceId = deviceId,
            expiresAtMs = now + 10000,
            currentTimeMs = now
        )
        assertEquals(ExpiryStatus.USER_NOT_FOUND, status)
    }

    @Test
    fun inactiveUser_returnsInactive() {
        val status = ExpiryEvaluator.evaluate(
            userExists = true,
            active = false,
            registeredDeviceId = deviceId,
            currentDeviceId = deviceId,
            expiresAtMs = now + 10000,
            currentTimeMs = now
        )
        assertEquals(ExpiryStatus.INACTIVE, status)
    }

    @Test
    fun deviceMismatch_returnsDeviceMismatch() {
        val status = ExpiryEvaluator.evaluate(
            userExists = true,
            active = true,
            registeredDeviceId = "other-device-456",
            currentDeviceId = deviceId,
            expiresAtMs = now + 10000,
            currentTimeMs = now
        )
        assertEquals(ExpiryStatus.DEVICE_MISMATCH, status)
    }

    @Test
    fun missingExpiresAt_returnsMissingExpiry() {
        val status = ExpiryEvaluator.evaluate(
            userExists = true,
            active = true,
            registeredDeviceId = deviceId,
            currentDeviceId = deviceId,
            expiresAtMs = null,
            currentTimeMs = now
        )
        assertEquals(ExpiryStatus.MISSING_EXPIRY, status)
    }

    @Test
    fun expiresAtInPast_returnsExpired() {
        val status = ExpiryEvaluator.evaluate(
            userExists = true,
            active = true,
            registeredDeviceId = deviceId,
            currentDeviceId = deviceId,
            expiresAtMs = now - 1000,
            currentTimeMs = now
        )
        assertEquals(ExpiryStatus.EXPIRED, status)
    }

    @Test
    fun expiresAtExact_returnsExpired() {
        val status = ExpiryEvaluator.evaluate(
            userExists = true,
            active = true,
            registeredDeviceId = deviceId,
            currentDeviceId = deviceId,
            expiresAtMs = now,
            currentTimeMs = now
        )
        assertEquals(ExpiryStatus.EXPIRED, status)
    }

    @Test
    fun expiresAtInFuture_returnsValid() {
        val status = ExpiryEvaluator.evaluate(
            userExists = true,
            active = true,
            registeredDeviceId = deviceId,
            currentDeviceId = deviceId,
            expiresAtMs = now + 50000,
            currentTimeMs = now
        )
        assertEquals(ExpiryStatus.VALID, status)
    }

    @Test
    fun expiresAtInSecondsFormat_automaticallyNormalizedToMillis() {
        // 1790592000 is in seconds (10 digits). Current time ~1740000000000 (ms).
        val currentMs = 1740000000000L
        val secondsExpiry = 1790592000L
        val status = ExpiryEvaluator.evaluate(
            userExists = true,
            active = true,
            registeredDeviceId = deviceId,
            currentDeviceId = deviceId,
            expiresAtMs = secondsExpiry,
            currentTimeMs = currentMs
        )
        assertEquals(ExpiryStatus.VALID, status)
    }
}
