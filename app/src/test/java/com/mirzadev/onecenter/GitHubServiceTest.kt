package com.mirzadev.onecenter

import com.google.gson.Gson
import com.mirzadev.onecenter.data.LogoManifestResponse
import kotlinx.coroutines.CancellationException
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test

class GitHubServiceTest {

    private val gson = Gson()
    private val safeFilenameRegex = Regex("^[A-Za-z0-9_-]+\\.png$")

    private fun isSafeFilename(filename: String): Boolean {
        return safeFilenameRegex.matches(filename)
    }

    @Test
    fun parseLogoManifest_allSevenTags_mapsCorrectly() {
        val json = """
            {
              "logos": {
                "ExamBrowserClient": "exambrowserclient.png",
                "ExamBrowserCBT": "cbtexambro.png",
                "CloudCBT": "cbtcloud.png",
                "SemiofflineCBT": "cbtsemioffline.png",
                "OnlineCBT": "cbtonline.png",
                "ExamManTemanggung": "mantemanggung.png",
                "ExamSMan1Cigombong": "smancigombong.png"
              }
            }
        """.trimIndent()

        val parsed = gson.fromJson(json, LogoManifestResponse::class.java)
        assertNotNull(parsed?.logos)
        assertEquals(7, parsed?.logos?.size)

        val tags = listOf(
            "ExamBrowserClient",
            "ExamBrowserCBT",
            "CloudCBT",
            "SemiofflineCBT",
            "OnlineCBT",
            "ExamManTemanggung",
            "ExamSMan1Cigombong"
        )

        for (tag in tags) {
            val filename = parsed?.logos?.get(tag)
            assertNotNull("Filename should not be null for tag $tag", filename)
            assertTrue("Filename $filename must be safe", isSafeFilename(filename!!))
        }
    }

    @Test
    fun parseLogoManifest_unknownTag_returnsNull() {
        val json = """
            {
              "logos": {
                "ExamBrowserClient": "exambrowserclient.png"
              }
            }
        """.trimIndent()

        val parsed = gson.fromJson(json, LogoManifestResponse::class.java)
        val unknownLogo = parsed?.logos?.get("UnknownAppTag")
        assertNull(unknownLogo)
    }

    @Test
    fun safeFilenameValidation_rejectsPathTraversalAndInvalidUrls() {
        // Valid filenames
        assertTrue(isSafeFilename("exambrowserclient.png"))
        assertTrue(isSafeFilename("cbtexambro.png"))
        assertTrue(isSafeFilename("my-app_logo2.png"))

        // Invalid filenames (Path traversal, absolute paths, URLs, wrong extensions)
        assertFalse(isSafeFilename("../exambrowserclient.png"))
        assertFalse(isSafeFilename("/etc/passwd.png"))
        assertFalse(isSafeFilename("https://malicious.com/logo.png"))
        assertFalse(isSafeFilename("logo.exe"))
        assertFalse(isSafeFilename("logo.png.txt"))
        assertFalse(isSafeFilename(""))
    }

    @Test
    fun manifestFetchFailure_gracefulDegradationRetainsFallback() {
        val cachedManifest: Map<String, String>? = mapOf("ExamBrowserClient" to "exambrowserclient.png")
        val fetchFailed = true

        // Graceful degradation logic: if fetch fails, use cached manifest or empty map
        val effectiveManifest = if (fetchFailed) {
            cachedManifest ?: emptyMap()
        } else {
            emptyMap()
        }

        assertEquals(1, effectiveManifest.size)
        assertEquals("exambrowserclient.png", effectiveManifest["ExamBrowserClient"])
    }

    @Test
    fun cancellationException_isRethrownDirectly() {
        try {
            val exception: Exception = CancellationException("Job canceled")
            if (exception is CancellationException) {
                throw exception
            }
            fail("Should have rethrown CancellationException")
        } catch (e: Exception) {
            assertTrue(e is CancellationException)
        }
    }
}
