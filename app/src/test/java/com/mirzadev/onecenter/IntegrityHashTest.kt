package com.mirzadev.onecenter

import org.junit.Assert.assertEquals
import org.junit.Test
import java.io.ByteArrayInputStream
import java.io.File
import kotlin.io.path.createTempFile

/**
 * Tests [ResourceIntegrity], including SHA-256 output against well-known
 * NIST test vectors. Runs on the host JVM (`./gradlew :app:testDebugUnitTest`)
 * — no device needed, since hashing uses `java.security.MessageDigest` and
 * the final comparison falls back to a plain String comparison when the
 * native library isn't loaded (see ResourceIntegrity.compare's KDoc for
 * why that fallback is a safe, deliberate, non-production code path).
 */
class IntegrityHashTest {

    @Test
    fun sha256_emptyInput_matchesKnownVector() {
        val hash = ResourceIntegrity.sha256Hex(ByteArray(0))
        assertEquals(
            "e3b0c44298fc1c149afbf4c8996fb92427ae41e4649b934ca495991b7852b855",
            hash,
        )
    }

    @Test
    fun sha256_abc_matchesKnownVector() {
        val hash = ResourceIntegrity.sha256Hex("abc".toByteArray(Charsets.UTF_8))
        assertEquals(
            "ba7816bf8f01cfea414140de5dae2223b00361a396177a9cb410ff61f20015ad",
            hash,
        )
    }

    @Test
    fun sha256_streamOverload_matchesByteArrayOverload() {
        val data = "The quick brown fox jumps over the lazy dog".toByteArray(Charsets.UTF_8)
        val fromBytes = ResourceIntegrity.sha256Hex(data)
        val fromStream = ResourceIntegrity.sha256Hex(ByteArrayInputStream(data))
        assertEquals(fromBytes, fromStream)
    }

    @Test
    fun sha256_streamLargerThanBufferSize_isConsistent() {
        // Exercise the streaming loop across multiple internal 8KB reads.
        val data = ByteArray(20_000) { (it % 251).toByte() }
        val fromBytes = ResourceIntegrity.sha256Hex(data)
        val fromStream = ResourceIntegrity.sha256Hex(ByteArrayInputStream(data))
        assertEquals(fromBytes, fromStream)
    }

    @Test
    fun verifyFile_blankExpectedDigest_isNotConfigured() {
        val tempFile = tempFileWithContent("abc")
        val status = ResourceIntegrity.verifyFile(tempFile, expectedSha256Hex = "")
        assertEquals(IntegrityStatus.NOT_CONFIGURED, status)
    }

    @Test
    fun verifyFile_missingFile_isUnavailable() {
        val missing = File("/definitely/does/not/exist/native_lib.so")
        val status = ResourceIntegrity.verifyFile(missing, expectedSha256Hex = "deadbeef")
        assertEquals(IntegrityStatus.UNAVAILABLE, status)
    }

    @Test
    fun verifyFile_matchingDigest_isMatch() {
        val tempFile = tempFileWithContent("abc")
        val expected = "ba7816bf8f01cfea414140de5dae2223b00361a396177a9cb410ff61f20015ad"
        val status = ResourceIntegrity.verifyFile(tempFile, expectedSha256Hex = expected)
        assertEquals(IntegrityStatus.MATCH, status)
    }

    @Test
    fun verifyFile_matchingDigest_isCaseInsensitive() {
        val tempFile = tempFileWithContent("abc")
        val expectedUpper = "BA7816BF8F01CFEA414140DE5DAE2223B00361A396177A9CB410FF61F20015AD"
        val status = ResourceIntegrity.verifyFile(tempFile, expectedSha256Hex = expectedUpper)
        assertEquals(IntegrityStatus.MATCH, status)
    }

    @Test
    fun verifyFile_wrongDigest_isMismatch() {
        val tempFile = tempFileWithContent("custom content")
        val wrong = "0".repeat(64)
        val status = ResourceIntegrity.verifyFile(tempFile, expectedSha256Hex = wrong)
        assertEquals(IntegrityStatus.MISMATCH, status)
    }

    @Test
    fun parseExpectedDigests_blank_isEmpty() {
        assertEquals(emptyMap<String, String>(), NativeLibraryIntegrity.parseExpectedDigests(""))
    }

    @Test
    fun parseExpectedDigests_wellFormed_parsesAllEntries() {
        val parsed = NativeLibraryIntegrity.parseExpectedDigests(
            "onecentercore:aa11,onecenterauth:bb22",
        )
        assertEquals(
            mapOf("onecentercore" to "aa11", "onecenterauth" to "bb22"),
            parsed,
        )
    }

    @Test
    fun parseExpectedDigests_malformedEntry_isSkipped() {
        val parsed = NativeLibraryIntegrity.parseExpectedDigests(
            "onecentercore:aa11,garbage-without-colon,onecenterauth:bb22",
        )
        assertEquals(
            mapOf("onecentercore" to "aa11", "onecenterauth" to "bb22"),
            parsed,
        )
    }

    private fun tempFileWithContent(content: String): File {
        val file = createTempFile().toFile()
        file.deleteOnExit()
        file.writeBytes(content.toByteArray(Charsets.UTF_8))
        return file
    }
}
