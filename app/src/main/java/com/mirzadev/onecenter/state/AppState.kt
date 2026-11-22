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
import com.mirzadev.onecenter.data.OneCenterApi
import com.mirzadev.onecenter.data.PrefsCache
import com.mirzadev.onecenter.data.PrivateAuthException
import com.mirzadev.onecenter.data.PrivateDocument
import com.mirzadev.onecenter.ui.design.BackgroundType
import com.mirzadev.onecenter.ui.design.GlassMode
import kotlinx.coroutines.CancellationException
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
                val latest = OneCenterApi.fetchLatestInformation()
                information = latest
                cache.writeJson(gson.toJson(latest))
                notice = null
                isOnline = true
            } catch (e: Exception) {
                if (e is CancellationException) throw e
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
                val result = GitHubService.fetchApks(forceRefresh)
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

    var expiresAt by mutableStateOf<Long?>(null)
        private set

    var documents by mutableStateOf(emptyList<PrivateDocument>())
        private set

    private var job: Job? = null
    private var checkJob: Job? = null
    private var serverTimeOffset: Long = 0L

    private var activeUid: String? = auth.currentUser?.uid
    private var requestGeneration: Long = 0L

    init {
        auth.addAuthStateListener { firebaseAuth ->
            val current = firebaseAuth.currentUser?.uid
            if (current != activeUid) {
                activeUid = current
                requestGeneration++
                job?.cancel()
                documents = emptyList()
                errorMessage = null
                isRefreshing = false
                if (current == null) {
                    isLoggedIn = false
                    expiresAt = null
                }
            }
        }
    }

    private fun getUserCache(): PrefsCache? {
        val uid = auth.currentUser?.uid ?: return null
        val prefs = PrefsCache.preferences(context)
        return PrefsCache.forUser(prefs, PrefsCache.BASE_KEY_PRIVATE_DOCS, uid)
    }

    private fun readCache(): List<PrivateDocument> = try {
        getUserCache()?.readJson()
            ?.let { gson.fromJson(it, Array<PrivateDocument>::class.java).toList() }
            ?: emptyList()
    } catch (_: Exception) {
        emptyList()
    }

    private fun writeUserCache(list: List<PrivateDocument>) {
        try {
            getUserCache()?.writeJson(gson.toJson(list))
        } catch (_: Exception) {}
    }

    private fun clearUserCache(targetUid: String? = auth.currentUser?.uid) {
        try {
            val prefs = PrefsCache.preferences(context)
            PrefsCache.clearUserPrivateCache(prefs, targetUid)
        } catch (_: Exception) {}
        cache.clear()
    }

    fun dismissError() {
        errorMessage = null
    }

    private suspend fun updateServerTimeOffset() {
        try {
            val snapshot = database.child(".info/serverTimeOffset").get().await()
            serverTimeOffset = snapshot.getValue(Long::class.java) ?: 0L
        } catch (e: Exception) {
            if (e is CancellationException) throw e
            // Keep current offset on network error
        }
    }

    fun performLogout(message: String? = null) {
        val targetUid = auth.currentUser?.uid
        job?.cancel()
        checkJob?.cancel()
        requestGeneration++
        clearUserCache(targetUid)
        try {
            auth.signOut()
        } catch (_: Exception) {}
        isLoggedIn = false
        expiresAt = null
        username = ""
        password = ""
        documents = emptyList()
        errorMessage = message
    }

    fun lock() {
        performLogout(message = null)
    }

    fun checkSession(scope: CoroutineScope) {
        if (!isLoggedIn) return
        val currentUser = auth.currentUser
        if (currentUser == null) {
            performLogout("Sesi tidak valid atau akun tidak ditemukan.")
            return
        }

        // Immediate local check if expiresAt is known
        expiresAt?.let { exp ->
            val now = System.currentTimeMillis() + serverTimeOffset
            if (now >= exp) {
                performLogout("Akses Private telah berakhir. Silakan perbarui akses Anda.")
                return
            }
        }

        if (checkJob?.isActive == true) return

        checkJob = scope.launch {
            try {
                updateServerTimeOffset()
                val now = System.currentTimeMillis() + serverTimeOffset

                val userRef = database.child("users").child(currentUser.uid)
                val snapshot = userRef.get().await()

                val userExists = snapshot.exists()
                val active = snapshot.child("active").getValue(Boolean::class.java)
                val registeredDeviceId = snapshot.child("deviceId").getValue(String::class.java)
                val expiresAtVal = snapshot.child("expiresAt").getValue(Long::class.java)

                val status = ExpiryEvaluator.evaluate(
                    userExists = userExists,
                    active = active,
                    registeredDeviceId = registeredDeviceId,
                    currentDeviceId = deviceId(context),
                    expiresAtMs = expiresAtVal,
                    currentTimeMs = now
                )

                val errorMsg = ExpiryEvaluator.getErrorMessage(status)
                if (errorMsg != null) {
                    performLogout(errorMsg)
                } else {
                    expiresAt = expiresAtVal
                }
            } catch (e: Exception) {
                if (e is CancellationException) throw e
                // On network error, fall back to local comparison if expiresAt is known
                expiresAt?.let { exp ->
                    val now = System.currentTimeMillis() + serverTimeOffset
                    if (now >= exp) {
                        performLogout("Akses Private telah berakhir. Silakan perbarui akses Anda.")
                    }
                }
            }
        }
    }

    // Unchanged security flow: Firebase sign-in, UID lookup, Android ID
    // device binding + expiry validation.
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

                updateServerTimeOffset()
                val now = System.currentTimeMillis() + serverTimeOffset

                val devId = deviceId(context)
                val userRef = database.child("users").child(user.uid)
                val snapshot = userRef.get().await()

                if (!snapshot.exists()) {
                    auth.signOut()
                    throw Exception("Akun belum terdaftar di sistem.")
                }

                var registeredDeviceId = snapshot
                    .child("deviceId")
                    .getValue(String::class.java)

                if (registeredDeviceId.isNullOrBlank()) {
                    userRef.updateChildren(
                        mapOf<String, Any>(
                            "deviceId" to devId,
                            "deviceBound" to true,
                            "deviceBoundAt" to System.currentTimeMillis()
                        )
                    ).await()
                    registeredDeviceId = devId
                }

                val active = snapshot.child("active").getValue(Boolean::class.java)
                val expiresAtVal = snapshot.child("expiresAt").getValue(Long::class.java)

                val status = ExpiryEvaluator.evaluate(
                    userExists = true,
                    active = active,
                    registeredDeviceId = registeredDeviceId,
                    currentDeviceId = devId,
                    expiresAtMs = expiresAtVal,
                    currentTimeMs = now
                )

                val errorMsg = ExpiryEvaluator.getErrorMessage(status)
                if (errorMsg != null) {
                    auth.signOut()
                    throw Exception(errorMsg)
                }

                expiresAt = expiresAtVal
                password = ""
                isLoggedIn = true
            } catch (e: Exception) {
                if (e is CancellationException) throw e
                isLoggedIn = false
                errorMessage = e.message ?: "Login gagal."
            } finally {
                isAuthenticating = false
            }
        }
    }

    fun loadDocuments(scope: CoroutineScope, forceRefresh: Boolean = false) {
        if (!isLoggedIn) return
        checkSession(scope)
        if (!isLoggedIn) return
        if (job?.isActive == true) return

        val currentUid = auth.currentUser?.uid ?: return
        val currentGen = requestGeneration

        job = scope.launch {
            isRefreshing = true
            errorMessage = null

            try {
                // Step 1: Authorization-Only endpoint check
                val authResult = OneCenterApi.fetchDocumentAuthorization(context, auth)

                if (auth.currentUser?.uid != currentUid || requestGeneration != currentGen) {
                    return@launch
                }

                val allowedIds = authResult.allowedDocumentIds

                if (allowedIds.isEmpty()) {
                    documents = emptyList()
                    clearUserCache(currentUid)
                    return@launch
                }

                val userCache = getUserCache()
                val cachedDocs = readCache()

                val cacheHasAllAllowed = cachedDocs.isNotEmpty() &&
                        allowedIds.all { allowedId -> cachedDocs.any { it.id == allowedId } }

                if (!forceRefresh && userCache?.isFresh() == true && cacheHasAllAllowed) {
                    // Cache fresh and contains all allowed items -> filter and render!
                    val filteredCached = cachedDocs.filter { allowedIds.contains(it.id) }
                    documents = filteredCached
                } else {
                    // Step 2: Full Document Fetch
                    val fullResult = OneCenterApi.fetchPrivateDocuments(context, auth)

                    if (auth.currentUser?.uid != currentUid || requestGeneration != currentGen) {
                        return@launch
                    }

                    val docs = fullResult.documents
                    val filteredDocs = docs.filter { allowedIds.contains(it.id) }
                    documents = filteredDocs
                    writeUserCache(docs)
                }
            } catch (e: Exception) {
                if (e is CancellationException) throw e

                if (auth.currentUser?.uid != currentUid || requestGeneration != currentGen) {
                    return@launch
                }

                documents = emptyList()
                clearUserCache(currentUid)

                errorMessage = if (e is PrivateAuthException) {
                    e.message ?: "Akses ditolak atau sesi tidak valid."
                } else {
                    "Gagal mengambil dokumen: ${e.message}"
                }
            } finally {
                if (auth.currentUser?.uid == currentUid && requestGeneration == currentGen) {
                    isRefreshing = false
                }
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

    var backgroundType by mutableStateOf(
        PrefsCache.getBackgroundType(context)
    )
        private set

    fun updateGlassMode(mode: GlassMode) {
        glassMode = mode
        PrefsCache.setGlassMode(context, mode)
    }

    fun updateBackgroundType(type: BackgroundType) {
        backgroundType = type
        PrefsCache.setBackgroundType(context, type)
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
fun rememberAppState(): AppState {
    val context = LocalContext.current.applicationContext
    val prefs = PrefsCache.preferences(context)

    return remember {
        AppState(
            home = HomeState(
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
