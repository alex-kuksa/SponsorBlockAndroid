@file:Suppress("DEPRECATION")

package com.sponsorblock

import android.content.Context
import android.content.SharedPreferences
import android.preference.PreferenceManager
import androidx.core.content.edit
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.json.JSONObject
import java.util.ArrayList

class PreferencesManager(context: Context) {
    private val sharedPreferences: SharedPreferences =
        PreferenceManager.getDefaultSharedPreferences(context)

    fun saveData(key: String, value: Boolean) {
        val editor = sharedPreferences.edit()
        editor.putBoolean(key, value)
        editor.apply()
    }

    fun saveData(key: String, value: String) {
        val editor = sharedPreferences.edit()
        editor.putString(key, value)
        editor.apply()
    }

    fun getData(key: String, defaultValue: Boolean = false): Boolean {
        return sharedPreferences.getBoolean(key, defaultValue)
    }

    fun getData(key: String, defaultValue: String): String {
        return sharedPreferences.getString(key, defaultValue) ?: defaultValue
    }

    fun saveYTMetadata(metadata: Triple<String, String, String?>) {
        sharedPreferences.edit {
            putString("yt_metadata_cache_key", Json.encodeToString(metadata))
        }
    }

    fun saveSBMetadata(sbMetadata: ArrayList<JSONObject>) {
        sharedPreferences.edit {
            putStringSet("sb_metadata_cache_key", sbMetadata.map { it.toString() }.toSet())
        }
    }

    fun restoreYTMetadata(): Triple<String, String, String?>? {
        return sharedPreferences.getString("yt_metadata_cache_key", null)?.run {
            Json.decodeFromString(this)
        }
    }

    fun restoreSBMetadata(): ArrayList<JSONObject>? {
        return sharedPreferences.getStringSet("sb_metadata_cache_key", null)?.map {
            JSONObject(it)
        }?.let {
            ArrayList(it)
        }
    }
}

class PreferencesKeys {
    companion object {
        const val SHOW_SKIPPED_TOAST = "show_skipped_toast_key"
        const val SHOW_SPONSOR_TOAST = "show_exclusive_access_toast_key"
        const val SHOW_SELF_PROMOTION_TOAST = "show_exclusive_access_toast_key"
        const val SHOW_EXCLUSIVE_ACCESS_TOAST = "show_exclusive_access_toast_key"

        const val SKIP_SPONSOR = "skip_sponsor_key"
        const val SKIP_SELF_PROMOTION = "skip_self_promotion_key"
        const val SKIP_INTERACTION = "skip_interaction_key"
        const val SKIP_INTRO = "skip_intro_key"
        const val SKIP_OUTRO = "skip_outro_key"
        const val SKIP_PREVIEW = "skip_preview_key"
        const val SKIP_MUSIC_OFFTOPIC = "skip_music_offtopic_key"

        const val USE_YT_API = "use_yt_api_key"
        const val YT_API_KEY = "yt_api_key_key"
    }
}