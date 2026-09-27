package com.mirzadev.onecenter

import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import java.security.MessageDigest

enum class SignatureStatus {
    MATCH,
    MISMATCH,
    NOT_CONFIGURED
}

enum class SecurityInitState {
    ACTIVE,
    AUDIT_ONLY,
    BLOCKED,
    NOT_CONFIGURED
}

object NativeSecurity {

    private var isInitialized = false
    private var cachedStatus: SignatureStatus? = null
    private var cachedInitState: SecurityInitState? = null

    init {
        try {
            System.loadLibrary("onecentersecurity")
        } catch (e: UnsatisfiedLinkError) {
            // Ignored on host JVM unit tests
        }
    }

    external fun constantTimeEquals(
        a: String,
        b: String
    ): Boolean

    external fun verifySigningDigest(
        actualDigest: String,
        expectedDigest: String
    ): Boolean

    fun getCurrentSigningDigest(context: Context): String? {
        return try {
            val packageName = context.packageName
            val signatures = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                val packageInfo = context.packageManager.getPackageInfo(
                    packageName,
                    PackageManager.GET_SIGNING_CERTIFICATES
                )
                packageInfo.signingInfo?.let { signingInfo ->
                    if (signingInfo.hasMultipleSigners()) {
                        signingInfo.apkContentsSigners
                    } else {
                        signingInfo.signingCertificateHistory
                    }
                }
            } else {
                @Suppress("DEPRECATION")
                val packageInfo = context.packageManager.getPackageInfo(
                    packageName,
                    PackageManager.GET_SIGNATURES
                )
                @Suppress("DEPRECATION")
                packageInfo.signatures
            }

            if (signatures.isNullOrEmpty()) return null
            val certBytes = signatures[0].toByteArray()
            val md = MessageDigest.getInstance("SHA-256")
            val digest = md.digest(certBytes)
            digest.joinToString(":") { "%02X".format(it) }
        } catch (e: Exception) {
            null
        }
    }

    fun getExpectedDigest(): String {
        return try {
            BuildConfig.EXPECTED_SIGNING_DIGEST
        } catch (e: Exception) {
            ""
        }
    }

    fun getSignatureStatus(context: Context?, expectedDigest: String = getExpectedDigest()): SignatureStatus {
        if (expectedDigest.isBlank()) {
            return SignatureStatus.NOT_CONFIGURED
        }
        if (context == null) return SignatureStatus.MISMATCH
        val actualDigest = getCurrentSigningDigest(context) ?: return SignatureStatus.MISMATCH
        val matches = verifySigningDigest(actualDigest, expectedDigest)
        return if (matches) SignatureStatus.MATCH else SignatureStatus.MISMATCH
    }

    fun evaluateSecurityInitState(
        status: SignatureStatus,
        isDebug: Boolean = BuildConfig.DEBUG
    ): SecurityInitState {
        return if (isDebug) {
            when (status) {
                SignatureStatus.MATCH -> SecurityInitState.ACTIVE
                SignatureStatus.MISMATCH -> SecurityInitState.AUDIT_ONLY
                SignatureStatus.NOT_CONFIGURED -> SecurityInitState.AUDIT_ONLY
            }
        } else {
            when (status) {
                SignatureStatus.MATCH -> SecurityInitState.ACTIVE
                SignatureStatus.MISMATCH -> SecurityInitState.BLOCKED
                SignatureStatus.NOT_CONFIGURED -> SecurityInitState.NOT_CONFIGURED
            }
        }
    }

    fun initializeSecurity(context: Context?): SecurityInitState {
        val status = getSignatureStatus(context)
        val state = evaluateSecurityInitState(status)

        cachedStatus = status
        cachedInitState = state
        isInitialized = true

        try {
            val logMessage = if (state == SecurityInitState.BLOCKED) {
                "Security initialization: BLOCKED, Reason: SIGNATURE_MISMATCH"
            } else if (state == SecurityInitState.NOT_CONFIGURED) {
                "Security initialization: NOT_CONFIGURED"
            } else {
                "Security initialization: $state (Signature status: $status)"
            }
            NativeLogger.info("NativeSecurity", logMessage)
        } catch (_: Exception) {}

        return state
    }

    fun getSecurityInitState(context: Context?): SecurityInitState {
        if (!isInitialized || cachedInitState == null) {
            if (context != null) {
                return initializeSecurity(context)
            }
            val status = getSignatureStatus(null)
            return evaluateSecurityInitState(status)
        }
        return cachedInitState ?: SecurityInitState.NOT_CONFIGURED
    }

    fun canExecuteSecurityOperations(context: Context?): Boolean {
        val state = getSecurityInitState(context)
        return state == SecurityInitState.ACTIVE || state == SecurityInitState.AUDIT_ONLY
    }

    fun executeGatedSecurityOperation(context: Context?, operationName: String, block: () -> Boolean): Boolean {
        if (!canExecuteSecurityOperations(context)) {
            val state = getSecurityInitState(context)
            try {
                NativeLogger.error(
                    "NativeSecurity",
                    "Security operation '$operationName' BLOCKED. Security initialization: $state"
                )
            } catch (_: Exception) {}
            return false
        }
        return block()
    }

    fun verifyAppSignature(
        context: Context,
        expectedDigest: String
    ): Boolean {
        if (expectedDigest.isBlank()) return false
        val actualDigest = getCurrentSigningDigest(context) ?: return false
        return verifySigningDigest(actualDigest, expectedDigest)
    }
}
