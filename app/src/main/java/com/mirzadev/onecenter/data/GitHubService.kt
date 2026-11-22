package com.mirzadev.onecenter.data

import com.google.gson.Gson
import com.google.gson.annotations.SerializedName
import com.mirzadev.onecenter.AppConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONArray
import java.util.Locale
import java.util.concurrent.TimeUnit

data class LogoManifestResponse(
    @field:SerializedName("logos") val logos: Map<String, String>? = null
)

/**
 * Handles public GitHub Releases API fetches for APK downloads and dynamic logo resolution
 * via repository-hosted `logo-manifest.json` with TTL caching, Mutex locking, and safe filename validation.
 */
object GitHubService {

    private val client: OkHttpClient by lazy {
        OkHttpClient.Builder()
            .connectTimeout(15, TimeUnit.SECONDS)
            .readTimeout(15, TimeUnit.SECONDS)
            .retryOnConnectionFailure(true)
            .build()
    }

    private val gson = Gson()
    private val manifestMutex = Mutex()
    private var cachedManifestMap: Map<String, String>? = null
    private var manifestCachedAt: Long = 0L
    private const val MANIFEST_TTL_MILLIS = 10 * 60 * 1000L

    var timeProvider: () -> Long = { System.currentTimeMillis() }

    private val SAFE_FILENAME_REGEX = Regex("^[A-Za-z0-9_-]+\\.png$")

    private fun isSafeFilename(filename: String): Boolean {
        return SAFE_FILENAME_REGEX.matches(filename)
    }

    private suspend fun fetchLogoManifest(
        forceRefresh: Boolean = false,
        baseUrl: String = "https://raw.githubusercontent.com/mirzahardcode/cracking-exam-app/main",
        httpClient: OkHttpClient = client
    ): Map<String, String> = manifestMutex.withLock {
        val now = timeProvider()
        if (!forceRefresh && cachedManifestMap != null && (now - manifestCachedAt < MANIFEST_TTL_MILLIS)) {
            return@withLock cachedManifestMap!!
        }

        val request = Request.Builder()
            .url("$baseUrl/assets/logo-manifest.json")
            .header("Accept", "application/json")
            .build()

        try {
            httpClient.newCall(request).execute().use { response ->
                if (response.isSuccessful) {
                    val body = response.body?.string()
                    if (!body.isNullOrBlank()) {
                        val parsed = gson.fromJson(body, LogoManifestResponse::class.java)
                        val validMap = mutableMapOf<String, String>()
                        parsed?.logos?.forEach { (tag, filename) ->
                            if (!tag.isNullOrBlank() && !filename.isNullOrBlank() && isSafeFilename(filename)) {
                                validMap[tag] = filename
                            }
                        }
                        cachedManifestMap = validMap
                        manifestCachedAt = now
                        return@withLock validMap
                    }
                }
            }
        } catch (_: Exception) {
            // Graceful degradation: retain old cache if fetch fails
        }

        return@withLock cachedManifestMap ?: emptyMap()
    }

    // ─────────────────────────────────────────────
    // Releases API (public unauthenticated)
    // ─────────────────────────────────────────────

    suspend fun fetchApks(forceRefresh: Boolean = false): List<GitHubApk> = withContext(Dispatchers.IO) {
        val manifest = fetchLogoManifest(forceRefresh)

        val request = Request.Builder()
            .url("https://api.github.com/repos/${AppConfig.RELEASES_REPO}/releases")
            .header("Accept", "application/vnd.github+json")
            .build()

        val body = client.newCall(request).execute().use { response ->
            if (response.code == 403 || response.code == 429) {
                throw GitHubException("Batas kueri publik GitHub tercapai. Silakan coba lagi nanti.")
            }
            if (!response.isSuccessful) {
                throw GitHubException("GitHub HTTP ${response.code}: ${response.message}")
            }
            response.body?.string() ?: throw GitHubException("Response kosong.")
        }

        val releases = JSONArray(body)
        val apks = ArrayList<GitHubApk>(releases.length())

        for (i in 0 until releases.length()) {
            val release = releases.getJSONObject(i)
            val releaseName = release.optString("name")
                .ifBlank { release.optString("tag_name") }
            val tag = release.optString("tag_name")
            val assets = release.optJSONArray("assets") ?: continue

            val logoFilename = manifest[tag]
            val logoUrl = if (!logoFilename.isNullOrBlank()) {
                "https://raw.githubusercontent.com/mirzahardcode/cracking-exam-app/main/assets/$logoFilename"
            } else {
                null
            }

            for (j in 0 until assets.length()) {
                val asset = assets.getJSONObject(j)
                val fileName = asset.optString("name")
                if (!fileName.endsWith(".apk", ignoreCase = true)) continue

                val sizeMb = asset.optLong("size") / (1024.0 * 1024.0)

                apks += GitHubApk(
                    name = releaseName,
                    version = tag,
                    // Locale.US so the decimal separator is stable regardless of device locale.
                    size = String.format(Locale.US, "%.1f MB", sizeMb),
                    downloadUrl = asset.optString("browser_download_url"),
                    logoUrl = logoUrl
                )
            }
        }

        apks
    }
}

class GitHubException(message: String) : Exception(message)
