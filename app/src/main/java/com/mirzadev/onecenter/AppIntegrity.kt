package com.mirzadev.onecenter

import android.content.Context
import java.io.File
import java.io.FileInputStream
import java.io.InputStream
import java.security.MessageDigest

/**
 * Result of an integrity comparison against an expected SHA-256 digest.
 *
 * - [NOT_CONFIGURED] — no expected digest was supplied for this build
 *   (e.g. `EXPECTED_LIBRARY_DIGESTS_*` was left blank). This is the
 *   default, inert state until you deliberately opt in.
 * - [UNAVAILABLE] — an expected digest WAS supplied, but the check could
 *   not be performed (file missing, I/O error, asset not found). This is
 *   kept distinct from [MISMATCH] on purpose: treating "couldn't check"
 *   the same as "checked and it's wrong" would produce false tamper
 *   reports on perfectly legitimate configurations — see
 *   [NativeLibraryIntegrity]'s class doc for a concrete example.
 * - [MATCH] / [MISMATCH] — the check ran, and the digest did or did not
 *   match.
 */
enum class IntegrityStatus {
    MATCH,
    MISMATCH,
    NOT_CONFIGURED,
    UNAVAILABLE
}

/**
 * Application/resource integrity — "tamper EVIDENCE, not tamper
 * PREVENTION" (see [NativeSecurity]'s class doc for the full disclaimer;
 * the same disclaimer applies here without qualification).
 *
 * Verifies that a file or bundled asset's SHA-256 digest matches an
 * expected value. This is the mechanism for security-scope item #2
 * (application integrity): pin the expected hash of an application-owned
 * resource (a config file, a bundled asset, an extracted native library —
 * see [NativeLibraryIntegrity]) and detect at runtime if it no longer
 * matches.
 *
 * Hashing is done with `java.security.MessageDigest` — the JVM's own
 * audited SHA-256 implementation — rather than a hand-rolled native
 * implementation. There is no reason to reimplement a standard hash
 * algorithm in C++ when a correct, standard implementation is one call
 * away; per the security scope, this project does not roll its own
 * cryptographic primitives. The final digest *comparison* reuses
 * [NativeSecurity.constantTimeEquals], so the same reviewed comparison
 * primitive backs every integrity check in this app, not just signing.
 */
object ResourceIntegrity {

    private const val STREAM_BUFFER_SIZE = 8192

    fun sha256Hex(bytes: ByteArray): String {
        val digest = MessageDigest.getInstance("SHA-256").digest(bytes)
        return digest.joinToString("") { "%02x".format(it) }
    }

    fun sha256Hex(stream: InputStream): String {
        val digest = MessageDigest.getInstance("SHA-256")
        val buffer = ByteArray(STREAM_BUFFER_SIZE)
        while (true) {
            val read = stream.read(buffer)
            if (read <= 0) break
            digest.update(buffer, 0, read)
        }
        return digest.digest().joinToString("") { "%02x".format(it) }
    }

    fun verifyFile(file: File, expectedSha256Hex: String): IntegrityStatus {
        if (expectedSha256Hex.isBlank()) return IntegrityStatus.NOT_CONFIGURED
        if (!file.exists() || !file.isFile) return IntegrityStatus.UNAVAILABLE
        return try {
            FileInputStream(file).use { stream ->
                compare(sha256Hex(stream), expectedSha256Hex)
            }
        } catch (e: Exception) {
            IntegrityStatus.UNAVAILABLE
        }
    }

    fun verifyAsset(context: Context, assetPath: String, expectedSha256Hex: String): IntegrityStatus {
        if (expectedSha256Hex.isBlank()) return IntegrityStatus.NOT_CONFIGURED
        return try {
            context.assets.open(assetPath).use { stream ->
                compare(sha256Hex(stream), expectedSha256Hex)
            }
        } catch (e: Exception) {
            IntegrityStatus.UNAVAILABLE
        }
    }

    private fun compare(actualHex: String, expectedHex: String): IntegrityStatus {
        // On-device, NativeSecurity's JNI library is loaded and this always
        // takes the constant-time native comparison path. The String
        // .equals fallback exists solely so this logic is unit-testable on
        // a plain host JVM (no NDK/device — see IntegrityHashTest), where
        // System.loadLibrary fails and the `external fun` throws
        // UnsatisfiedLinkError instead of running.
        //
        // This is a deliberate, low-risk trade-off: the values compared
        // here are SHA-256 hex digests, not secrets — the same reasoning
        // that makes the APK signing certificate digest a non-secret (see
        // NativeSecurity's class doc) applies to any hash digest compared
        // here. A non-constant-time comparison in this narrow fallback
        // path is therefore not considered a meaningful timing-attack
        // surface, and this path is never taken on a real device/build.
        val matches = try {
            NativeSecurity.constantTimeEquals(actualHex.lowercase(), expectedHex.lowercase())
        } catch (e: UnsatisfiedLinkError) {
            actualHex.equals(expectedHex, ignoreCase = true)
        }
        return if (matches) IntegrityStatus.MATCH else IntegrityStatus.MISMATCH
    }
}

/**
 * Best-effort native (`.so`) library integrity checking — security-scope
 * item #3.
 *
 * RELIABILITY EVALUATION (read before enabling):
 * On Android, a loaded native library is not guaranteed to exist as a
 * standalone file you can open and hash:
 *
 *  - If the APK was installed with `extractNativeLibs="true"` (or the OS
 *    decided to extract for another reason), each `.so` is extracted to
 *    `ApplicationInfo.nativeLibraryDir` as a normal file, and hashing it
 *    here is reliable.
 *  - If `extractNativeLibs="false"` (the modern AGP default for
 *    page-aligned, uncompressed native libs), the library is mapped
 *    directly out of the APK's zip archive and is generally NOT present
 *    as a separate file in `nativeLibraryDir`. In that configuration this
 *    check correctly reports [IntegrityStatus.UNAVAILABLE] for every
 *    library — that is the honest answer, not a false [IntegrityStatus.MISMATCH]
 *    and not a fabricated [IntegrityStatus.MATCH].
 *
 * Even when it does run, this is DEFENSE IN DEPTH, not a primary control:
 * modifying a `.so` inside the APK almost always requires resigning the
 * APK, which [NativeSecurity]'s signing-digest check already catches
 * (assuming the attacker doesn't hold your signing key — if they do, no
 * on-device check can stop them). The marginal value of this specific
 * check is catching a narrower scenario: a `.so` file swapped on disk
 * *after* install, while the rest of the APK's signature still validates.
 *
 * To use this for real, supply expected digests via the
 * `EXPECTED_LIBRARY_DIGESTS_DEBUG` / `EXPECTED_LIBRARY_DIGESTS_RELEASE`
 * Gradle properties (see build.gradle.kts), formatted as
 * `name1:hexdigest1,name2:hexdigest2`. Because the libraries being hashed
 * are produced by the same build that would embed their digests,
 * populating real values requires a two-pass release process: build once,
 * compute the SHA-256 of each generated `.so`, then feed those digests
 * into the properties for a second, final build. Left unset (the
 * default), every library reports [IntegrityStatus.NOT_CONFIGURED], which
 * is a safe, inert default — nothing in this app is gated on it.
 */
object NativeLibraryIntegrity {

    data class LibraryCheck(
        val libraryName: String,
        val status: IntegrityStatus,
        val path: String?
    )

    /** Matches the four libraries this project builds — see CMakeLists.txt. */
    private val TRACKED_LIBRARIES = listOf(
        "onecentercore",
        "onecenterlogger",
        "onecenterauth",
        "onecentersecurity"
    )

    fun getExpectedDigests(): Map<String, String> = try {
        parseExpectedDigests(BuildConfig.EXPECTED_LIBRARY_DIGESTS)
    } catch (e: Exception) {
        emptyMap()
    }

    fun parseExpectedDigests(raw: String): Map<String, String> {
        if (raw.isBlank()) return emptyMap()
        return raw.split(",")
            .mapNotNull { entry ->
                val parts = entry.split(":", limit = 2)
                val name = parts.getOrNull(0)?.trim()
                val digest = parts.getOrNull(1)?.trim()
                if (parts.size == 2 && !name.isNullOrBlank() && !digest.isNullOrBlank()) {
                    name to digest
                } else {
                    null
                }
            }
            .toMap()
    }

    fun verifyAll(
        context: Context,
        expectedDigests: Map<String, String> = getExpectedDigests()
    ): List<LibraryCheck> {
        val nativeLibDir = context.applicationInfo.nativeLibraryDir
        return TRACKED_LIBRARIES.map { name ->
            val expected = expectedDigests[name]
            when {
                expected.isNullOrBlank() ->
                    LibraryCheck(name, IntegrityStatus.NOT_CONFIGURED, null)
                nativeLibDir.isNullOrBlank() ->
                    LibraryCheck(name, IntegrityStatus.UNAVAILABLE, null)
                else -> {
                    val file = File(nativeLibDir, "lib$name.so")
                    LibraryCheck(name, ResourceIntegrity.verifyFile(file, expected), file.absolutePath)
                }
            }
        }
    }
}
