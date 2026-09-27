package com.mirzadev.onecenter

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

/**
 * These tests run on a device/emulator (`./gradlew :app:connectedDebugAndroidTest`)
 * because they exercise the real libonecentersecurity.so via JNI. Pure
 * state-machine logic and hashing are covered separately on the host JVM
 * by SecurityStateTest and IntegrityHashTest.
 */
@RunWith(AndroidJUnit4::class)
class NativeSecurityInstrumentedTest {

    private val context = InstrumentationRegistry.getInstrumentation().targetContext

    // ---- constantTimeEquals (native, JNI) ----

    @Test
    fun constantTimeEquals_equalValues_returnsTrue() {
        assertTrue(NativeSecurity.constantTimeEquals("abc123", "abc123"))
    }

    @Test
    fun constantTimeEquals_differentValues_returnsFalse() {
        assertFalse(NativeSecurity.constantTimeEquals("abc123", "abc124"))
    }

    @Test
    fun constantTimeEquals_differentLengths_returnsFalse() {
        assertFalse(NativeSecurity.constantTimeEquals("abc", "abcd"))
    }

    @Test
    fun constantTimeEquals_emptyValues_returnsTrue() {
        assertTrue(NativeSecurity.constantTimeEquals("", ""))
    }

    @Test
    fun constantTimeEquals_oneEmptyOneNot_returnsFalse() {
        assertFalse(NativeSecurity.constantTimeEquals("", "a"))
    }

    // ---- verifySigningDigest / signature status, through the real APK ----

    @Test
    fun signatureStatus_blankExpectedDigest_isNotConfigured() {
        val status = NativeSecurity.getSignatureStatus(context, expectedDigest = "")
        assertEquals(SignatureStatus.NOT_CONFIGURED, status)
    }

    @Test
    fun signatureStatus_matchingDigest_isMatch() {
        val actual = NativeSecurity.getCurrentSigningDigest(context)
        assertNotNull("Test APK must be signed to run this test", actual)
        val status = NativeSecurity.getSignatureStatus(context, expectedDigest = actual!!)
        assertEquals(SignatureStatus.MATCH, status)
    }

    @Test
    fun signatureStatus_wrongDigest_isMismatch() {
        val bogus = "00:00:00:00:00:00:00:00:00:00:00:00:00:00:00:00:" +
            "00:00:00:00:00:00:00:00:00:00:00:00:00:00:00:00"
        val status = NativeSecurity.getSignatureStatus(context, expectedDigest = bogus)
        assertEquals(SignatureStatus.MISMATCH, status)
    }

    @Test
    fun verifyAppSignature_matchesGetSignatureStatus() {
        val actual = NativeSecurity.getCurrentSigningDigest(context)
        assertNotNull(actual)
        assertTrue(NativeSecurity.verifyAppSignature(context, actual!!))
        assertFalse(NativeSecurity.verifyAppSignature(context, "not-a-real-digest"))
    }

    // ---- initialization: idempotency and re-entrancy ----

    @Test
    fun initializeSecurity_calledTwice_returnsSameState() {
        NativeSecurity.resetForTesting()
        val first = NativeSecurity.initializeSecurity(context)
        val second = NativeSecurity.initializeSecurity(context)
        assertEquals(first, second)
    }

    @Test
    fun getSecurityInitState_lazyInitThenCached_isConsistent() {
        NativeSecurity.resetForTesting()
        val lazy = NativeSecurity.getSecurityInitState(context) // triggers initializeSecurity
        val cached = NativeSecurity.getSecurityInitState(context) // reads the cache
        assertEquals(lazy, cached)
    }

    @Test
    fun getSecurityInitState_matchesExplicitEvaluation() {
        NativeSecurity.resetForTesting()
        val status = NativeSecurity.getSignatureStatus(context)
        val expected = NativeSecurity.evaluateSecurityInitState(status)
        val actual = NativeSecurity.getSecurityInitState(context)
        assertEquals(expected, actual)
    }

    // ---- gate behavior ----

    @Test
    fun executeGatedSecurityOperation_runsIffCanExecuteSecurityOperations() {
        NativeSecurity.resetForTesting()
        val allowed = NativeSecurity.canExecuteSecurityOperations(context)

        var ran = false
        val result = NativeSecurity.executeGatedSecurityOperation(context, "instrumented-test-op") {
            ran = true
            true
        }

        // The gate's contract: the block runs exactly when the security
        // state is ACTIVE or AUDIT_ONLY, never otherwise — and it never
        // throws, kills the process, or has any side effect beyond
        // (not) running `block` and logging.
        assertEquals(allowed, ran)
        assertEquals(allowed, result)
    }

    @Test
    fun executeGatedSecurityOperation_neverThrows() {
        NativeSecurity.resetForTesting()
        // Should complete normally regardless of this build's signing
        // state (debug-signed test APKs are the common case in CI).
        NativeSecurity.executeGatedSecurityOperation(context, "instrumented-test-op-2") { true }
    }
}
