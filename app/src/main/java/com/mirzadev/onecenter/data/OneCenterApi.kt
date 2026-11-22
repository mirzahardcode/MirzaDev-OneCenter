package com.mirzadev.onecenter.data

import android.content.Context
import com.google.firebase.auth.FirebaseAuth
import com.google.gson.Gson
import com.mirzadev.onecenter.AppConfig
import com.mirzadev.onecenter.state.deviceId
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.util.concurrent.TimeUnit

/**
 * API client for OneCenter backend endpoints (Latest Information & Private Documents).
 */
object OneCenterApi {

    private val client: OkHttpClient by lazy {
        OkHttpClient.Builder()
            .connectTimeout(15, TimeUnit.SECONDS)
            .readTimeout(15, TimeUnit.SECONDS)
            .retryOnConnectionFailure(true)
            .build()
    }

    private val gson = Gson()

    suspend fun fetchLatestInformation(
        baseUrl: String = AppConfig.BACKEND_BASE_URL,
        httpClient: OkHttpClient = client
    ): LatestInformation = withContext(Dispatchers.IO) {
        val request = Request.Builder()
            .url("$baseUrl/latestInformation")
            .header("Accept", "application/json")
            .build()

        try {
            httpClient.newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    throw PrivateApiException("HTTP ${response.code}: ${response.message}")
                }
                val body = response.body?.string()
                    ?: throw PrivateApiException("Response kosong.")

                gson.fromJson(body, LatestInformation::class.java)
                    ?: throw PrivateApiException("Informasi tidak dapat dibaca.")
            }
        } catch (e: Exception) {
            if (e is CancellationException) throw e
            if (e is PrivateApiException) throw e
            throw PrivateApiException("Gagal mengambil informasi: ${e.message}")
        }
    }

    /**
     * Authorization-only mode: GET /privateDocuments?mode=authorization
     * Returns allowedDocumentIds without fetching full document payload from GitHub.
     */
    suspend fun fetchDocumentAuthorization(
        context: Context,
        auth: FirebaseAuth = FirebaseAuth.getInstance(),
        baseUrl: String = AppConfig.BACKEND_BASE_URL,
        httpClient: OkHttpClient = client
    ): PrivateDocumentsResponse = withContext(Dispatchers.IO) {
        val currentUser = auth.currentUser
            ?: throw PrivateAuthException("User tidak terotentikasi.")

        val idToken = try {
            val result = currentUser.getIdToken(false).await()
            result.token.takeIf { !it.isNullOrBlank() }
                ?: throw PrivateAuthException("Token otentikasi tidak tersedia.")
        } catch (e: Exception) {
            if (e is CancellationException) throw e
            if (e is PrivateAuthException) throw e
            throw PrivateAuthException("Gagal memperoleh token otentikasi: ${e.message}")
        }

        val devId = deviceId(context)

        val request = Request.Builder()
            .url("$baseUrl/privateDocuments?mode=authorization")
            .header("Accept", "application/json")
            .header("Authorization", "Bearer $idToken")
            .header("X-Device-ID", devId)
            .build()

        try {
            httpClient.newCall(request).execute().use { response ->
                when (response.code) {
                    401 -> throw PrivateAuthException("Sesi tidak valid.")
                    403 -> throw PrivateAuthException("Akses ditolak atau perangkat tidak cocok.")
                    in 200..299 -> {
                        val body = response.body?.string()
                            ?: throw PrivateApiException("Response kosong.")

                        val parsed = try {
                            gson.fromJson(body, PrivateDocumentsResponse::class.java)
                        } catch (_: Exception) {
                            throw PrivateApiException("Format JSON tidak valid.")
                        } ?: throw PrivateApiException("Response otorisasi tidak dapat dibaca.")

                        if (!parsed.ok) {
                            throw PrivateAuthException("Akses dokumen tidak diizinkan.")
                        }

                        parsed
                    }
                    else -> throw PrivateApiException("HTTP ${response.code}: ${response.message}")
                }
            }
        } catch (e: Exception) {
            if (e is CancellationException) throw e
            if (e is PrivateAuthException || e is PrivateApiException) throw e
            throw PrivateApiException("Gagal terhubung ke server: ${e.message}")
        }
    }

    /**
     * Full document fetch mode: GET /privateDocuments
     * Returns full document content objects filtered by actual permissions.
     */
    suspend fun fetchPrivateDocuments(
        context: Context,
        auth: FirebaseAuth = FirebaseAuth.getInstance(),
        baseUrl: String = AppConfig.BACKEND_BASE_URL,
        httpClient: OkHttpClient = client
    ): PrivateDocumentsResponse = withContext(Dispatchers.IO) {
        val currentUser = auth.currentUser
            ?: throw PrivateAuthException("User tidak terotentikasi.")

        val idToken = try {
            val result = currentUser.getIdToken(false).await()
            result.token.takeIf { !it.isNullOrBlank() }
                ?: throw PrivateAuthException("Token otentikasi tidak tersedia.")
        } catch (e: Exception) {
            if (e is CancellationException) throw e
            if (e is PrivateAuthException) throw e
            throw PrivateAuthException("Gagal memperoleh token otentikasi: ${e.message}")
        }

        val devId = deviceId(context)

        val request = Request.Builder()
            .url("$baseUrl/privateDocuments")
            .header("Accept", "application/json")
            .header("Authorization", "Bearer $idToken")
            .header("X-Device-ID", devId)
            .build()

        try {
            httpClient.newCall(request).execute().use { response ->
                when (response.code) {
                    401 -> throw PrivateAuthException("Sesi tidak valid.")
                    403 -> throw PrivateAuthException("Akses ditolak atau perangkat tidak cocok.")
                    in 200..299 -> {
                        val body = response.body?.string()
                            ?: throw PrivateApiException("Response kosong.")

                        val parsed = try {
                            gson.fromJson(body, PrivateDocumentsResponse::class.java)
                        } catch (_: Exception) {
                            throw PrivateApiException("Format JSON tidak valid.")
                        } ?: throw PrivateApiException("Dokumen tidak dapat dibaca.")

                        if (!parsed.ok) {
                            throw PrivateAuthException("Akses dokumen tidak diizinkan.")
                        }

                        parsed
                    }
                    else -> throw PrivateApiException("HTTP ${response.code}: ${response.message}")
                }
            }
        } catch (e: Exception) {
            if (e is CancellationException) throw e
            if (e is PrivateAuthException || e is PrivateApiException) throw e
            throw PrivateApiException("Gagal terhubung ke server: ${e.message}")
        }
    }
}

class PrivateAuthException(message: String) : Exception(message)
class PrivateApiException(message: String) : Exception(message)
