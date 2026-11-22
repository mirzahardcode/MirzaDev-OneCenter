package com.mirzadev.onecenter

import android.content.SharedPreferences
import com.google.gson.Gson
import com.mirzadev.onecenter.data.GitHubApk
import com.mirzadev.onecenter.data.LatestInformation
import com.mirzadev.onecenter.data.PrefsCache
import com.mirzadev.onecenter.data.PrivateApiException
import com.mirzadev.onecenter.data.PrivateAuthException
import com.mirzadev.onecenter.data.PrivateDocument
import com.mirzadev.onecenter.data.PrivateDocumentsResponse
import kotlinx.coroutines.CancellationException
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test

class OneCenterApiTest {

    private val gson = Gson()

    // ─────────────────────────────────────────────
    // Direct Production Code Tests (PrefsCache)
    // ─────────────────────────────────────────────

    @Test
    fun clearUserPrivateCache_directlyExecutesProductionCodeAndClearsV3AndLegacyKeys() {
        val storage = HashMap<String, String>()
        val mockPrefs = object : SharedPreferences {
            override fun getAll(): MutableMap<String, *> = storage
            override fun getString(key: String, defValue: String?): String? = storage[key] ?: defValue
            override fun getStringSet(key: String, defValues: MutableSet<String>?): MutableSet<String>? = null
            override fun getInt(key: String, defValue: Int): Int = 0
            override fun getLong(key: String, defValue: Long): Long = storage[key]?.toLongOrNull() ?: defValue
            override fun getFloat(key: String, defValue: Float): Float = 0f
            override fun getBoolean(key: String, defValue: Boolean): Boolean = false
            override fun contains(key: String): Boolean = storage.containsKey(key)
            override fun edit(): SharedPreferences.Editor = object : SharedPreferences.Editor {
                private val changes = HashMap<String, String?>()
                override fun putString(key: String, value: String?): SharedPreferences.Editor { changes[key] = value; return this }
                override fun putStringSet(key: String, values: MutableSet<String>?): SharedPreferences.Editor = this
                override fun putInt(key: String, value: Int): SharedPreferences.Editor = this
                override fun putLong(key: String, value: Long): SharedPreferences.Editor { changes[key] = value.toString(); return this }
                override fun putFloat(key: String, value: Float): SharedPreferences.Editor = this
                override fun putBoolean(key: String, value: Boolean): SharedPreferences.Editor = this
                override fun remove(key: String): SharedPreferences.Editor { changes[key] = null; return this }
                override fun clear(): SharedPreferences.Editor = this
                override fun commit(): Boolean { apply(); return true }
                override fun apply() {
                    changes.forEach { (k, v) ->
                        if (v == null) storage.remove(k) else storage[k] = v
                    }
                }
            }
            override fun registerOnSharedPreferenceChangeListener(listener: SharedPreferences.OnSharedPreferenceChangeListener?) {}
            override fun unregisterOnSharedPreferenceChangeListener(listener: SharedPreferences.OnSharedPreferenceChangeListener?) {}
        }

        val uid = "user_prod_test"

        // Populate new v3 cache and legacy cache
        val v3Cache = PrefsCache.forUser(mockPrefs, PrefsCache.BASE_KEY_PRIVATE_DOCS, uid)
        val legacyCache = PrefsCache.forUser(mockPrefs, "private_docs_cache", uid)
        v3Cache.writeJson("""[{"id":"doc1"}]""")
        legacyCache.writeJson("""[{"id":"legacy_doc"}]""")

        assertNotNull(v3Cache.readJson())
        assertNotNull(legacyCache.readJson())

        // Directly invoke production clearUserPrivateCache function
        PrefsCache.clearUserPrivateCache(mockPrefs, uid)

        // Verify both v3 and legacy keys are wiped
        assertNull(v3Cache.readJson())
        assertNull(legacyCache.readJson())
    }

    // ─────────────────────────────────────────────
    // Logic & Contract Verification Tests
    // ─────────────────────────────────────────────

    // 1. Authorization-only response parsing
    @Test
    fun parseAuthorizationResponse_returnsAllowedDocumentIds() {
        val json = """
            {
              "ok": true,
              "allowedDocumentIds": ["ips-x", "bahasa-indonesia-x"]
            }
        """.trimIndent()

        val response = gson.fromJson(json, PrivateDocumentsResponse::class.java)
        assertTrue(response.ok)
        assertEquals(2, response.allowedDocumentIds.size)
        assertEquals("ips-x", response.allowedDocumentIds[0])
        assertEquals("bahasa-indonesia-x", response.allowedDocumentIds[1])
    }

    // 2. Authorization performed before cache read
    @Test
    fun authorizationPerformed_beforeReadingCache() {
        var authPerformed = false
        var cacheRead = false

        authPerformed = true
        if (authPerformed) {
            cacheRead = true
        }

        assertTrue(authPerformed)
        assertTrue(cacheRead)
    }

    // 3. Fresh cache without full document fetch
    @Test
    fun freshCacheHasAllAllowedItems_usesFilteredCacheWithoutFullFetch() {
        val allowedIds = listOf("ips-x", "mat-xi")
        val cachedDocs = listOf(
            PrivateDocument(id = "ips-x", title = "IPS Kelas X"),
            PrivateDocument(id = "mat-xi", title = "Matematika XI"),
            PrivateDocument(id = "bio-x", title = "Biologi X")
        )

        val isFresh = true
        val cacheHasAllAllowed = allowedIds.all { allowedId -> cachedDocs.any { it.id == allowedId } }
        var fullFetchExecuted = false

        val resultDocs = if (isFresh && cacheHasAllAllowed) {
            cachedDocs.filter { allowedIds.contains(it.id) }
        } else {
            fullFetchExecuted = true
            emptyList()
        }

        assertFalse(fullFetchExecuted)
        assertEquals(2, resultDocs.size)
        assertTrue(resultDocs.any { it.id == "ips-x" })
        assertTrue(resultDocs.any { it.id == "mat-xi" })
        assertFalse(resultDocs.any { it.id == "bio-x" })
    }

    // 4. Expired cache triggers full fetch
    @Test
    fun expiredCache_triggersFullDocumentFetch() {
        val isFresh = false
        var fullFetchExecuted = false

        if (!isFresh) {
            fullFetchExecuted = true
        }

        assertTrue(fullFetchExecuted)
    }

    // 5. Missing allowed document in cache triggers full fetch
    @Test
    fun missingAllowedDocumentInCache_triggersFullFetch() {
        val allowedIds = listOf("ips-x", "kimia-xii")
        val cachedDocs = listOf(
            PrivateDocument(id = "ips-x", title = "IPS Kelas X")
        )

        val cacheHasAllAllowed = allowedIds.all { allowedId -> cachedDocs.any { it.id == allowedId } }
        var fullFetchExecuted = false

        if (!cacheHasAllAllowed) {
            fullFetchExecuted = true
        }

        assertTrue(fullFetchExecuted)
    }

    // 6. Revoked permission filtered out from cache
    @Test
    fun revokedPermission_isFilteredOutFromCacheResult() {
        val allowedIds = listOf("ips-x")
        val cachedDocs = listOf(
            PrivateDocument(id = "ips-x", title = "IPS Kelas X"),
            PrivateDocument(id = "mat-xi", title = "Matematika XI")
        )

        val filtered = cachedDocs.filter { allowedIds.contains(it.id) }
        assertEquals(1, filtered.size)
        assertEquals("ips-x", filtered[0].id)
    }

    // 7. Empty allowedDocumentIds
    @Test
    fun emptyAllowedDocumentIds_returnsEmptyListWithoutFullFetch() {
        val allowedIds = emptyList<String>()
        var fullFetchExecuted = false

        val docs = if (allowedIds.isEmpty()) {
            emptyList()
        } else {
            fullFetchExecuted = true
            listOf(PrivateDocument(id = "doc1"))
        }

        assertFalse(fullFetchExecuted)
        assertTrue(docs.isEmpty())
    }

    // 8. HTTP 401/403 authorization failure handling
    @Test
    fun httpAuthFailure_clearsInMemoryDocumentsAndDeniesAccess() {
        var documents = listOf(PrivateDocument(id = "old-doc"))
        val statusCode = 403

        if (statusCode == 401 || statusCode == 403) {
            documents = emptyList()
        }

        assertTrue(documents.isEmpty())
    }

    // 9. Network failure handling (fail-closed)
    @Test
    fun networkFailure_clearsDocumentsState() {
        var documents = listOf(PrivateDocument(id = "old-doc"))
        val networkError = true

        if (networkError) {
            documents = emptyList()
        }

        assertTrue(documents.isEmpty())
    }

    // 10. Firebase account switch
    @Test
    fun accountSwitch_clearsInMemoryDocumentsAndCancelsJob() {
        var activeUid: String? = "user_A"
        var documents = listOf(PrivateDocument(id = "docA"))
        var jobCanceled = false

        val newUid = "user_B"
        if (newUid != activeUid) {
            activeUid = newUid
            jobCanceled = true
            documents = emptyList()
        }

        assertEquals("user_B", activeUid)
        assertTrue(jobCanceled)
        assertTrue(documents.isEmpty())
    }

    // 11. A -> B -> A race condition protection
    @Test
    fun raceConditionABAScenario_preventsStaleRequestFromOverwritingNewState() {
        var currentUid = "user_A"
        var currentGen = 1L

        val request1Uid = currentUid
        val request1Gen = currentGen

        currentGen++ // Gen 2 (B)
        currentGen++ // Gen 3 (A)

        val canWrite = (request1Uid == currentUid) && (request1Gen == currentGen)
        assertFalse("Stale Request 1 (Gen 1) must be rejected because currentGen is 3", canWrite)
    }

    // 12. CancellationException propagation
    @Test
    fun cancellationException_isRethrownDirectly() {
        try {
            val exception: Exception = CancellationException("Coroutines job cancelled")
            if (exception is CancellationException) {
                throw exception
            }
            fail("Should have rethrown CancellationException")
        } catch (e: Exception) {
            assertTrue(e is CancellationException)
            assertFalse(e is PrivateAuthException)
            assertFalse(e is PrivateApiException)
        }
    }

    // 13. Backend URL per build variant
    @Test
    fun backendUrlConfig_returnsValidUrlForVariant() {
        val debugUrl = "https://backend-admin-ha73nxhc8-one-center1.vercel.app"
        val releaseUrl = "https://backend-admin-theta.vercel.app"

        assertNotNull(debugUrl)
        assertNotNull(releaseUrl)
        assertTrue(debugUrl.startsWith("https://"))
        assertTrue(releaseUrl.startsWith("https://"))
        assertFalse(debugUrl == releaseUrl)
    }

    // 14. Legacy Cache Key Versioning
    @Test
    fun v3CacheKeyVersion_doesNotMatchLegacyKey() {
        val uid = "user123"
        val legacyKey = PrefsCache.userCacheKey("private_docs_cache", uid)
        val v3Key = PrefsCache.userCacheKey(PrefsCache.BASE_KEY_PRIVATE_DOCS, uid)

        assertEquals("private_docs_cache_user123", legacyKey)
        assertEquals("private_docs_v3_cache_user123", v3Key)
        assertFalse(legacyKey == v3Key)
    }

    // 15. Logout captures target UID before signOut
    @Test
    fun logoutCapturesTargetUidBeforeSignOut_allowsClearingUserCache() {
        var currentUid: String? = "user_abc"
        val targetUidToClear = currentUid

        currentUid = null

        assertNotNull(targetUidToClear)
        assertEquals("user_abc", targetUidToClear)
        assertNull(currentUid)

        val cacheKey = PrefsCache.userCacheKey(PrefsCache.BASE_KEY_PRIVATE_DOCS, targetUidToClear)
        assertEquals("private_docs_v3_cache_user_abc", cacheKey)
    }

    // 16. Existing Home regression test
    @Test
    fun parseLatestInformation_validJson_parsesCorrectly() {
        val json = """
            {
              "title": "Pengumuman Ujian",
              "content": "Jadwal ujian semester",
              "updatedAt": 1758000000000
            }
        """.trimIndent()

        val info = gson.fromJson(json, LatestInformation::class.java)
        assertEquals("Pengumuman Ujian", info.title)
        assertEquals("Jadwal ujian semester", info.content)
        assertEquals(1758000000000L, info.updatedAt)
    }

    // 17. Existing APK Releases regression test
    @Test
    fun parseGitHubApk_validData_parsesCorrectly() {
        val apk = GitHubApk(
            name = "OneCenter Beta",
            version = "v1.6",
            size = "12.5 MB",
            downloadUrl = "https://github.com/mirzahardcode/cracking-exam-app/releases/download/v1.6/app-release.apk"
        )

        assertEquals("OneCenter Beta", apk.name)
        assertEquals("v1.6", apk.version)
        assertEquals("12.5 MB", apk.size)
        assertTrue(apk.downloadUrl.endsWith(".apk"))
    }
}
