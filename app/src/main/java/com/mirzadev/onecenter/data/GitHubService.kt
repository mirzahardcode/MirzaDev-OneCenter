package com.mirzadev.onecenter.data

import android.util.Base64
import com.google.gson.Gson
import com.mirzadev.onecenter.AppConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONArray
import java.util.Locale
import java.util.concurrent.TimeUnit

/**
 * All GitHub access in one place.
 *
 * Previously every call allocated a fresh OkHttpClient (throwing away the
 * connection pool and thread pool each time) and the releases endpoint used a
 * raw HttpURLConnection. One lazily-created client now serves everything.
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

    // ─────────────────────────────────────────────
    // Contents API (private repo, base64 payloads)
    // ─────────────────────────────────────────────

    private suspend fun fetchContentFile(
        repo: String,
        path: String,
        token: String
    ): String = withContext(Dispatchers.IO) {

        val request = Request.Builder()
            .url("https://api.github.com/repos/$repo/contents/$path")
            .header("Authorization", "Bearer $token")
            .header("Accept", "application/vnd.github+json")
            .build()

        client.newCall(request).execute().use { response ->

            if (!response.isSuccessful) {
                throw GitHubException("GitHub HTTP ${response.code}: ${response.message}")
            }

            val body = response.body?.string()
                ?: throw GitHubException("Response kosong.")

            val encoded = gson.fromJson(body, Map::class.java)["content"]?.toString()
                ?: throw GitHubException("Field content tidak ditemukan.")

            val decoded = Base64.decode(
                encoded.replace("\\s".toRegex(), ""),
                Base64.DEFAULT
            )

            String(decoded, Charsets.UTF_8)
        }
    }

    suspend fun fetchLatestInformation(token: String): LatestInformation {
        val json = fetchContentFile(
            repo = AppConfig.PRIVATE_REPO,
            path = "latest-information.json",
            token = token
        )
        // Parsing off the main thread too — Default, not IO.
        return withContext(Dispatchers.Default) {
            gson.fromJson(json, LatestInformation::class.java)
                ?: throw GitHubException("Informasi tidak dapat dibaca.")
        }
    }

    suspend fun fetchPrivateDocuments(token: String): List<PrivateDocument> {
        val json = fetchContentFile(
            repo = AppConfig.PRIVATE_REPO,
            path = "private-docs.json",
            token = token
        )
        return withContext(Dispatchers.Default) {
            gson.fromJson(json, PrivateDocumentsResponse::class.java)
                ?.documents
                ?: throw GitHubException("Dokumen tidak dapat dibaca.")
        }
    }

    // ─────────────────────────────────────────────
    // Releases API (public)
    // ─────────────────────────────────────────────

    suspend fun fetchApks(): List<GitHubApk> = withContext(Dispatchers.IO) {

        val request = Request.Builder()
            .url("https://api.github.com/repos/${AppConfig.RELEASES_REPO}/releases")
            .header("Accept", "application/vnd.github+json")
            .build()

        val body = client.newCall(request).execute().use { response ->
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
                    // Locale.US so the decimal separator is stable regardless
                    // of device locale.
                    size = String.format(Locale.US, "%.1f MB", sizeMb),
                    downloadUrl = asset.optString("browser_download_url")
                )
            }
        }

        apks
    }
}

class GitHubException(message: String) : Exception(message)
