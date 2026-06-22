package com.musicplayer.localmusicplayer

import android.app.Application
import com.musicplayer.localmusicplayer.data.preference.AppPreferences
import com.musicplayer.localmusicplayer.domain.model.Language
import com.musicplayer.localmusicplayer.util.LocaleHelper
import dagger.hilt.android.HiltAndroidApp
import kotlinx.coroutines.runBlocking
import javax.inject.Inject

@HiltAndroidApp
class MusicPlayerApplication : Application() {

    @Inject
    lateinit var appPreferences: AppPreferences

    @Inject
    lateinit var themeRepository: com.musicplayer.localmusicplayer.domain.repository.ThemeRepository

    override fun onCreate() {
        super.onCreate()

        // Load saved language synchronously before Activity starts
        savedLanguage = try {
            runBlocking { appPreferences.getLanguage() }
        } catch (e: Exception) {
            Language.Chinese // Default to Chinese
        }

        // Apply locale
        LocaleHelper.applyLocale(this, savedLanguage)
    }

    companion object {
        var savedLanguage: Language = Language.Chinese
    }
}
