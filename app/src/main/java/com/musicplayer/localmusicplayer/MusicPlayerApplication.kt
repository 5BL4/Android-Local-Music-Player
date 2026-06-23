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

        // 注册全局未捕获异常处理器，记录崩溃日志后交给系统默认处理
        val previousHandler = Thread.getDefaultUncaughtExceptionHandler()
        Thread.setDefaultUncaughtExceptionHandler { thread, throwable ->
            android.util.Log.e(
                "AppCrash",
                "Uncaught exception on thread ${thread.name}",
                throwable
            )
            // 交给系统默认 handler 处理（弹出崩溃对话框/终止进程）
            previousHandler?.uncaughtException(thread, throwable)
        }

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
