package com.musicplayer.localmusicplayer.util

import android.content.Context
import android.content.res.Configuration
import com.musicplayer.localmusicplayer.domain.model.Language
import java.util.Locale

object LocaleHelper {

    fun applyLocale(context: Context, language: Language): Context {
        val locale = when (language) {
            Language.English -> Locale("en")
            Language.Chinese -> Locale("zh")
        }
        Locale.setDefault(locale)

        val config = Configuration(context.resources.configuration)
        config.setLocale(locale)
        return context.createConfigurationContext(config)
    }
}
