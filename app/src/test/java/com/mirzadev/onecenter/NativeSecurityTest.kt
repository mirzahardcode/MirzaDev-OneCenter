package com.mirzadev.onecenter

import org.junit.Assert.assertEquals
import org.junit.Test

class NativeSecurityTest {

    @Test
    fun getExpectedDigest_returnsBuildConfigValue() {
        val expected = NativeSecurity.getExpectedDigest()
        assertEquals(BuildConfig.EXPECTED_SIGNING_DIGEST, expected)
    }

    @Test
    fun getSignatureStatus_whenExpectedDigestIsBlank_returnsNotConfigured() {
        val status = NativeSecurity.getSignatureStatus(null, "")
        assertEquals(SignatureStatus.NOT_CONFIGURED, status)
    }

    @Test
    fun getSignatureStatus_whenExpectedDigestIsNotBlankAndContextIsNull_returnsMismatch() {
        val status = NativeSecurity.getSignatureStatus(null, "AA:BB:CC")
        assertEquals(SignatureStatus.MISMATCH, status)
    }

    @Test
    fun signatureStatus_enumValues() {
        assertEquals("MATCH", SignatureStatus.MATCH.name)
        assertEquals("MISMATCH", SignatureStatus.MISMATCH.name)
        assertEquals("NOT_CONFIGURED", SignatureStatus.NOT_CONFIGURED.name)
    }

    @Test
    fun securityInitState_enumValues() {
        assertEquals("ACTIVE", SecurityInitState.ACTIVE.name)
        assertEquals("AUDIT_ONLY", SecurityInitState.AUDIT_ONLY.name)
        assertEquals("BLOCKED", SecurityInitState.BLOCKED.name)
        assertEquals("NOT_CONFIGURED", SecurityInitState.NOT_CONFIGURED.name)
    }

    @Test
    fun evaluateSecurityInitState_debugMode() {
        assertEquals(
            SecurityInitState.ACTIVE,
            NativeSecurity.evaluateSecurityInitState(SignatureStatus.MATCH, isDebug = true)
        )
        assertEquals(
            SecurityInitState.AUDIT_ONLY,
            NativeSecurity.evaluateSecurityInitState(SignatureStatus.MISMATCH, isDebug = true)
        )
        assertEquals(
            SecurityInitState.AUDIT_ONLY,
            NativeSecurity.evaluateSecurityInitState(SignatureStatus.NOT_CONFIGURED, isDebug = true)
        )
    }

    @Test
    fun evaluateSecurityInitState_releaseMode() {
        assertEquals(
            SecurityInitState.ACTIVE,
            NativeSecurity.evaluateSecurityInitState(SignatureStatus.MATCH, isDebug = false)
        )
        assertEquals(
            SecurityInitState.BLOCKED,
            NativeSecurity.evaluateSecurityInitState(SignatureStatus.MISMATCH, isDebug = false)
        )
        assertEquals(
            SecurityInitState.NOT_CONFIGURED,
            NativeSecurity.evaluateSecurityInitState(SignatureStatus.NOT_CONFIGURED, isDebug = false)
        )
    }
}
