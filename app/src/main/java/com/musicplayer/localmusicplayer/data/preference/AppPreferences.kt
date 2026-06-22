package com.musicplayer.localmusicplayer.data.preference

import android.content.Context
import android.util.Log
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.musicplayer.localmusicplayer.domain.model.Language
import com.musicplayer.localmusicplayer.domain.model.SortOption
import com.musicplayer.localmusicplayer.domain.model.ThemeColor
import com.musicplayer.localmusicplayer.domain.model.ThemeMode
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "settings")

@Singleton
class AppPreferences @Inject constructor(
    @ApplicationContext private val context: Context
) {
    companion object {
        val TAG = "AppPreferences"
        private val KEY_THEME_MODE = stringPreferencesKey("theme_mode")
        private val KEY_USE_DYNAMIC_COLOR = booleanPreferencesKey("use_dynamic_color")
        private val KEY_DEFAULT_SORT = stringPreferencesKey("default_sort")
        private val KEY_LANGUAGE = stringPreferencesKey("language")
        private val KEY_THEME_COLOR = stringPreferencesKey("theme_color")
    }

    val themeMode: Flow<ThemeMode> = context.dataStore.data.map { prefs ->
        try {
            ThemeMode.valueOf(prefs[KEY_THEME_MODE] ?: ThemeMode.System.name)
        } catch (_: IllegalArgumentException) {
            ThemeMode.System
        }
    }

    val useDynamicColor: Flow<Boolean> = context.dataStore.data.map { prefs ->
        prefs[KEY_USE_DYNAMIC_COLOR] ?: true
    }

    val defaultSortOption: Flow<SortOption> = context.dataStore.data.map { prefs ->
        try {
            SortOption.valueOf(prefs[KEY_DEFAULT_SORT] ?: SortOption.Title.name)
        } catch (_: IllegalArgumentException) {
            SortOption.Title
        }
    }

    val language: Flow<Language> = context.dataStore.data.map { prefs ->
        try {
            val langStr = prefs[KEY_LANGUAGE] ?: Language.Chinese.name
            Language.valueOf(langStr)
        } catch (_: IllegalArgumentException) {
            Language.Chinese
        }
    }

    suspend fun getLanguage(): Language {
        return try {
            val prefs = context.dataStore.data.firstOrNull()
            val langStr = prefs?.get(KEY_LANGUAGE) ?: Language.Chinese.name
            try { Language.valueOf(langStr) } catch (_: Exception) { Language.Chinese }
        } catch (e: Exception) {
            Log.e(TAG, "Error reading language preference", e)
            Language.Chinese
        }
    }

    suspend fun setThemeMode(mode: ThemeMode) {
        context.dataStore.edit { prefs ->
            prefs[KEY_THEME_MODE] = mode.name
        }
    }

    suspend fun setUseDynamicColor(useDynamic: Boolean) {
        context.dataStore.edit { prefs ->
            prefs[KEY_USE_DYNAMIC_COLOR] = useDynamic
        }
    }

    suspend fun setDefaultSortOption(option: SortOption) {
        context.dataStore.edit { prefs ->
            prefs[KEY_DEFAULT_SORT] = option.name
        }
    }

    val themeColor: Flow<ThemeColor> = context.dataStore.data.map { prefs ->
        try { ThemeColor.valueOf(prefs[KEY_THEME_COLOR] ?: ThemeColor.Blue.name) }
        catch (_: Exception) { ThemeColor.Blue }
    }

    suspend fun setThemeColor(color: ThemeColor) {
        context.dataStore.edit { prefs -> prefs[KEY_THEME_COLOR] = color.name }
    }

    suspend fun setLanguage(language: Language) {
        context.dataStore.edit { prefs ->
            prefs[KEY_LANGUAGE] = language.name
        }
    }

    fun logAllPrefs() {
        kotlinx.coroutines.MainScope().launch {
            try {
                val prefs = context.dataStore.data.firstOrNull()
                Log.d(TAG, "=== Current Preferences ===")
                prefs?.asMap()?.forEach { (key, value) ->
                    Log.d(TAG, "  ${key.name} = $value")
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error reading prefs", e)
            }
        }
    }
}
