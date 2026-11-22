package com.mirzadev.onecenter.data

import com.mirzadev.onecenter.AppConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONArray
import java.util.Locale
import java.util.concurrent.TimeUnit

/**
 * Handles public GitHub Releases API fetches for APK downloads.
 * Unauthenticated public GET requests are used since the releases repository is public.
 */
object GitHubService {

    private val client: OkHttpClient by lazy {
        OkHttpClient.Builder()
            .connectTimeout(15, TimeUnit.SECONDS)
            .readTimeout(15, TimeUnit.SECONDS)
            .retryOnConnectionFailure(true)
            .build()
    }

    // ─────────────────────────────────────────────
    // Releases API (public unauthenticated)
    // ─────────────────────────────────────────────

    suspend fun fetchApks(): List<GitHubApk> = withContext(Dispatchers.IO) {

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
                    downloadUrl = asset.optString("browser_download_url")
                )
            }
        }

        apks
    }
}

class GitHubException(message: String) : Exception(message)
