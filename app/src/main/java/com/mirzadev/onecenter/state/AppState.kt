package com.mirzadev.onecenter.state

import android.content.Context
import android.provider.Settings
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase
import com.google.gson.Gson
import com.mirzadev.onecenter.NativeAuth
import com.mirzadev.onecenter.data.GitHubApk
import com.mirzadev.onecenter.data.GitHubService
import com.mirzadev.onecenter.data.LatestInformation
import com.mirzadev.onecenter.data.PrefsCache
import com.mirzadev.onecenter.data.PrivateDocument
import com.mirzadev.onecenter.ui.design.GlassMode
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

/**
 * Why state lives here instead of inside each screen:
 *
 * HorizontalPager disposes pages once they leave the viewport, so `remember`
 * inside HomeScreen / ApksScreen / PrivateScreen was thrown away on every
 * swipe — which meant a network round trip (and a visible loading flash) each
 * time the user came back to a tab. Hoisting to the app root makes the tabs
 * feel instant and cuts redundant requests.
 *
 * These are plain state holders rather than ViewModels so no new Gradle
 * dependency is needed. Swapping them to ViewModels later is mechanical.
 */

private val gson = Gson()

// ─────────────────────────────────────────────
// Home
// ─────────────────────────────────────────────

@Stable
class HomeState(
    private val token: String,
    private val cache: PrefsCache
) {
    var information by mutableStateOf(readCache())
        private set

    var isLoading by mutableStateOf(false)
        private set

    /** Non-fatal note shown under the section header, e.g. "showing cached data". */
    var notice by mutableStateOf<String?>(null)
        private set

    /** Drives the "System online" / "Offline" indicator. */
    var isOnline by mutableStateOf(true)
        private set

    private var job: Job? = null

    private fun readCache(): LatestInformation? = try {
        cache.readJson()?.let { gson.fromJson(it, LatestInformation::class.java) }
    } catch (_: Exception) {
        null
    }

    fun load(scope: CoroutineScope, forceRefresh: Boolean = false) {
        if (!forceRefresh && information != null && cache.isFresh()) return
        if (job?.isActive == true) return

        job = scope.launch {
            isLoading = true
            try {
                val latest = GitHubService.fetchLatestInformation(token)
                information = latest
                cache.writeJson(gson.toJson(latest))
                notice = null
                isOnline = true
            } catch (e: Exception) {
                isOnline = false
                val cached = readCache()
                if (cached != null) {
                    information = cached
                    notice = "Menggunakan informasi terakhir."
                } else {
                    notice = "Gagal mengambil informasi: ${e.message}"
                }
            } finally {
                isLoading = false
            }
        }
    }
}

// ─────────────────────────────────────────────
// APKs
// ─────────────────────────────────────────────

@Stable
class ApksState(
    private val cache: PrefsCache
) {
    var apks by mutableStateOf(readCache())
        private set

    var isLoading by mutableStateOf(false)
        private set

    var errorMessage by mutableStateOf<String?>(null)
        private set

    private var job: Job? = null

    private fun readCache(): List<GitHubApk> = try {
        cache.readJson()
            ?.let { gson.fromJson(it, Array<GitHubApk>::class.java).toList() }
            ?: emptyList()
    } catch (_: Exception) {
        emptyList()
    }

    fun load(scope: CoroutineScope, forceRefresh: Boolean = false) {
        if (!forceRefresh && apks.isNotEmpty() && cache.isFresh()) return
        if (job?.isActive == true) return

        job = scope.launch {
            isLoading = true
            errorMessage = null
            try {
                val result = GitHubService.fetchApks()
                apks = result
                cache.writeJson(gson.toJson(result))
            } catch (e: Exception) {
                errorMessage = if (apks.isNotEmpty()) {
                    "Gagal memperbarui. Menampilkan data terakhir."
                } else {
                    "Gagal mengambil data: ${e.message}"
                }
            } finally {
                isLoading = false
            }
        }
    }
}

// ─────────────────────────────────────────────
// Private
// ─────────────────────────────────────────────

@Stable
class PrivateState(
    private val context: Context,
    private val token: String,
    private val cache: PrefsCache
) {
    private val auth = FirebaseAuth.getInstance()
    private val database = FirebaseDatabase.getInstance().reference

    var username by mutableStateOf("")
    var password by mutableStateOf("")

    var isLoggedIn by mutableStateOf(auth.currentUser != null)
        private set

    var isAuthenticating by mutableStateOf(false)
        private set

    var isRefreshing by mutableStateOf(false)
        private set

    var errorMessage by mutableStateOf<String?>(null)
        private set

    var documents by mutableStateOf(
        if (auth.currentUser != null) readCache() else emptyList()
    )
        private set

    private var job: Job? = null

    private fun readCache(): List<PrivateDocument> = try {
        cache.readJson()
            ?.let { gson.fromJson(it, Array<PrivateDocument>::class.java).toList() }
            ?: emptyList()
    } catch (_: Exception) {
        emptyList()
    }

    fun dismissError() {
        errorMessage = null
    }

    // Unchanged security flow: Firebase sign-in, UID lookup, Android ID
    // device binding. Only the surrounding state plumbing moved.
    fun login(scope: CoroutineScope) {
        val sanitizedEmail = try {
            NativeAuth.sanitizeEmail(username)
        } catch (e: Exception) {
            username.trim()
        }
        val currentPassword = password

        val validationCode = try {
            NativeAuth.validateCredentials(sanitizedEmail, currentPassword)
        } catch (e: Exception) {
            // Fallback safely if JNI call fails
            if (sanitizedEmail.isBlank() || currentPassword.isBlank()) 3 else 0
        }

        if (validationCode != 0) {
            errorMessage = when (validationCode) {
                1 -> if (sanitizedEmail.isBlank()) "Username dan password wajib diisi." else "Format email/username tidak valid."
                2 -> if (currentPassword.isBlank()) "Username dan password wajib diisi." else "Password minimal 6 karakter."
                else -> "Username dan password wajib diisi."
            }
            return
        }

        if (isAuthenticating) return

        scope.launch {
            isAuthenticating = true
            errorMessage = null

            try {
                val result = auth
                    .signInWithEmailAndPassword(sanitizedEmail, password)
                    .await()

                val user = result.user
                    ?: throw Exception("User Firebase tidak ditemukan.")

                val deviceId = deviceId(context)
                val userRef = database.child("users").child(user.uid)
                val snapshot = userRef.get().await()

                if (!snapshot.exists()) {
                    auth.signOut()
                    throw Exception("Akun belum terdaftar di sistem.")
                }

                val registeredDeviceId = snapshot
                    .child("deviceId")
                    .getValue(String::class.java)

                if (registeredDeviceId.isNullOrBlank()) {
                    userRef.child("deviceId").setValue(deviceId).await()
                } else if (registeredDeviceId != deviceId) {
                    auth.signOut()
                    throw Exception("Device tidak diizinkan untuk akun ini.")
                }

                password = ""
                isLoggedIn = true
                documents = readCache()
                loadDocuments(scope)
            } catch (e: Exception) {
                isLoggedIn = false
                errorMessage = e.message ?: "Login gagal."
            } finally {
                isAuthenticating = false
            }
        }
    }

    fun lock() {
        auth.signOut()
        job?.cancel()
        isLoggedIn = false
        username = ""
        password = ""
        documents = emptyList()
        errorMessage = null
    }

    fun loadDocuments(scope: CoroutineScope, forceRefresh: Boolean = false) {
        if (!isLoggedIn) return
        if (!forceRefresh && documents.isNotEmpty() && cache.isFresh()) return
        if (job?.isActive == true) return

        job = scope.launch {
            isRefreshing = true
            errorMessage = null
            try {
                val result = GitHubService.fetchPrivateDocuments(token)
                documents = result
                cache.writeJson(gson.toJson(result))
            } catch (e: Exception) {
                val cached = readCache()
                if (cached.isNotEmpty()) {
                    documents = cached
                    errorMessage = "Gagal memperbarui. Menampilkan data terakhir."
                } else {
                    errorMessage = "Gagal mengambil dokumen: ${e.message}"
                }
            } finally {
                isRefreshing = false
            }
        }
    }
}

fun deviceId(context: Context): String =
    Settings.Secure.getString(
        context.contentResolver,
        Settings.Secure.ANDROID_ID
    ).orEmpty()

// ─────────────────────────────────────────────
// Settings
// ─────────────────────────────────────────────

@Stable
class SettingsState(
    private val context: Context
) {
    var glassMode by mutableStateOf(
        PrefsCache.getGlassMode(context)
    )
        private set

    fun updateGlassMode(mode: GlassMode) {
        glassMode = mode
        PrefsCache.setGlassMode(context, mode)
    }
}
// ─────────────────────────────────────────────
// Root holder
// ─────────────────────────────────────────────

@Stable
class AppState(
    val home: HomeState,
    val apks: ApksState,
    val private: PrivateState,
    val settings: SettingsState
)

@Composable
fun rememberAppState(token: String): AppState {
    val context = LocalContext.current.applicationContext
    val prefs = PrefsCache.preferences(context)

    return remember(token) {
        AppState(
            home = HomeState(
                token = token,
                cache = PrefsCache(
                    prefs = prefs,
                    key = "latest_information"
                )
            ),
            apks = ApksState(
                cache = PrefsCache(
                    prefs = prefs,
                    key = "apks_v2"
                )
            ),
            private = PrivateState(
                context = context,
                token = token,
                cache = PrefsCache(
                    prefs = prefs,
                    key = "private_docs_cache"
                )
            ),
            settings = SettingsState(
                context = context
            )
        )
    }
}
