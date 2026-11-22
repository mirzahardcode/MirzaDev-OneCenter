package com.mirzadev.onecenter.data

import android.content.Context
import android.content.SharedPreferences
import com.mirzadev.onecenter.AppConfig
import com.mirzadev.onecenter.ui.design.BackgroundType
import com.mirzadev.onecenter.ui.design.GlassMode

/**
 * One small TTL-aware JSON cache, replacing InformationCacheManager,
 * PrivateCacheManager and the two mismatched APK cache helpers
 * (`saveApksToCache` wrote key "apks" while ApksScreen read key "data",
 * so the APK cache never actually hit).
 */
class PrefsCache(
    private val prefs: SharedPreferences,
    private val key: String,
    private val ttlMillis: Long = AppConfig.CACHE_TTL_MILLIS
) {

    private val timeKey = "${key}_cached_at"

    fun readJson(): String? =
        prefs.getString(key, null)

    fun writeJson(json: String) {
        prefs.edit()
            .putString(key, json)
            .putLong(timeKey, System.currentTimeMillis())
            .apply()
    }

    fun isFresh(): Boolean {
        val cachedAt = prefs.getLong(timeKey, 0L)
        if (cachedAt == 0L) return false

        return System.currentTimeMillis() - cachedAt < ttlMillis
    }

    fun clear() {
        prefs.edit()
            .remove(key)
            .remove(timeKey)
            .apply()
    }

    companion object {

        const val KEY_GLASS_MODE = "glass_mode_v2"
        const val KEY_BACKGROUND_TYPE = "background_type_v1"
        const val BASE_KEY_PRIVATE_DOCS = "private_docs_v3_cache"

        fun userCacheKey(baseKey: String, uid: String?): String {
            return if (!uid.isNullOrBlank()) {
                "${baseKey}_${uid}"
            } else {
                baseKey
            }
        }

        fun forUser(
            prefs: SharedPreferences,
            baseKey: String,
            uid: String?,
            ttlMillis: Long = AppConfig.CACHE_TTL_MILLIS
        ): PrefsCache {
            return PrefsCache(
                prefs = prefs,
                key = userCacheKey(baseKey, uid),
                ttlMillis = ttlMillis
            )
        }

        fun clearUserPrivateCache(prefs: SharedPreferences, uid: String?) {
            if (!uid.isNullOrBlank()) {
                forUser(prefs, BASE_KEY_PRIVATE_DOCS, uid).clear()
                forUser(prefs, "private_docs_cache", uid).clear()
            }
            PrefsCache(prefs, "private_docs_cache").clear()
        }

        fun preferences(context: Context): SharedPreferences {
            return context.getSharedPreferences(
                "mirzadev_center",
                Context.MODE_PRIVATE
            )
        }

        fun getGlassMode(context: Context): GlassMode {
            val ordinal = preferences(context).getInt(KEY_GLASS_MODE, GlassMode.OFF.ordinal)
            return GlassMode.entries.getOrElse(ordinal) { GlassMode.OFF }
        }

        fun setGlassMode(context: Context, mode: GlassMode) {
            preferences(context)
                .edit()
                .putInt(KEY_GLASS_MODE, mode.ordinal)
                .apply()
        }

        fun getBackgroundType(context: Context): BackgroundType {
            val ordinal = preferences(context).getInt(KEY_BACKGROUND_TYPE, BackgroundType.Default.ordinal)
            return BackgroundType.entries.getOrElse(ordinal) { BackgroundType.Default }
        }

        fun setBackgroundType(context: Context, type: BackgroundType) {
            preferences(context)
                .edit()
                .putInt(KEY_BACKGROUND_TYPE, type.ordinal)
                .apply()
        }
    }
}
