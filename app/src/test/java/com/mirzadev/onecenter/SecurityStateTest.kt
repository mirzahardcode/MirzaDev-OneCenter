package com.mirzadev.onecenter

import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * Tests [NativeSecurity.evaluateSecurityInitState], which is pure Kotlin
 * (no I/O, no JNI) and therefore runs on the plain host JVM via
 * `./gradlew :app:testDebugUnitTest` — no device/emulator needed.
 *
 * Covers all six (SignatureStatus × build type) combinations named in the
 * spec:
 *   Debug   MATCH          -> ACTIVE
 *   Debug   MISMATCH       -> AUDIT_ONLY
 *   Debug   NOT_CONFIGURED -> AUDIT_ONLY
 *   Release MATCH          -> ACTIVE
 *   Release MISMATCH       -> BLOCKED
 *   Release NOT_CONFIGURED -> NOT_CONFIGURED
 */
class SecurityStateTest {

    @Test
    fun debug_match_isActive() {
        assertEquals(
            SecurityInitState.ACTIVE,
            NativeSecurity.evaluateSecurityInitState(SignatureStatus.MATCH, isDebug = true)
        )
    }

    @Test
    fun debug_mismatch_isAuditOnly() {
        assertEquals(
            SecurityInitState.AUDIT_ONLY,
            NativeSecurity.evaluateSecurityInitState(SignatureStatus.MISMATCH, isDebug = true)
        )
    }

    @Test
    fun debug_notConfigured_isAuditOnly() {
        assertEquals(
            SecurityInitState.AUDIT_ONLY,
            NativeSecurity.evaluateSecurityInitState(SignatureStatus.NOT_CONFIGURED, isDebug = true)
        )
    }

    @Test
    fun release_match_isActive() {
        assertEquals(
            SecurityInitState.ACTIVE,
            NativeSecurity.evaluateSecurityInitState(SignatureStatus.MATCH, isDebug = false)
        )
    }

    @Test
    fun release_mismatch_isBlocked() {
        assertEquals(
            SecurityInitState.BLOCKED,
            NativeSecurity.evaluateSecurityInitState(SignatureStatus.MISMATCH, isDebug = false)
        )
    }

    @Test
    fun release_notConfigured_isNotConfigured() {
        assertEquals(
            SecurityInitState.NOT_CONFIGURED,
            NativeSecurity.evaluateSecurityInitState(SignatureStatus.NOT_CONFIGURED, isDebug = false)
        )
    }

    @Test
    fun getSignatureStatus_blankExpectedDigest_isNotConfigured_evenWithNullContext() {
        // getSignatureStatus checks expectedDigest.isBlank() before ever
        // touching context, so this is also a pure/host-safe test.
        val status = NativeSecurity.getSignatureStatus(context = null, expectedDigest = "")
        assertEquals(SignatureStatus.NOT_CONFIGURED, status)
    }

    @Test
    fun getSignatureStatus_nonBlankDigestWithNullContext_isMismatch() {
        // No Context means we can't read the actual signing digest, so the
        // documented fail-safe behavior is MISMATCH, not a crash and not a
        // false MATCH.
        val status = NativeSecurity.getSignatureStatus(context = null, expectedDigest = "AA:BB")
        assertEquals(SignatureStatus.MISMATCH, status)
    }
}
