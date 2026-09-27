package com.mirzadev.onecenter.data

import android.content.Context
import android.content.SharedPreferences
import com.mirzadev.onecenter.AppConfig
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
    }
}
