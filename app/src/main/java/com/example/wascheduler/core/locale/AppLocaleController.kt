package com.example.wascheduler.core.locale

import android.app.LocaleManager
import android.content.Context
import android.content.ContextWrapper
import android.content.res.Configuration
import android.os.Build
import android.os.LocaleList
import com.example.wascheduler.data.repository.AppLanguage
import java.util.Locale

object AppLocaleController {
    private const val PREFS_NAME = "wascheduler_locale"
    private const val KEY_LANGUAGE = "language"

    fun apply(context: Context, language: AppLanguage) {
        persist(context, language)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            context.getSystemService(LocaleManager::class.java).applicationLocales =
                language.languageTag?.let(LocaleList::forLanguageTags) ?: LocaleList.getEmptyLocaleList()
        }
    }

    fun applyPersistedPlatformLocale(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val language = persistedLanguage(context)
            context.getSystemService(LocaleManager::class.java).applicationLocales =
                language.languageTag?.let(LocaleList::forLanguageTags) ?: LocaleList.getEmptyLocaleList()
        }
    }

    fun wrapForStoredLanguage(base: Context): Context {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) return base
        val language = persistedLanguage(base)
        val tag = language.languageTag ?: return base
        val locale = Locale.forLanguageTag(tag)
        val configuration = Configuration(base.resources.configuration)
        configuration.setLocale(locale)
        return ContextWrapper(base.createConfigurationContext(configuration))
    }

    private fun persist(context: Context, language: AppLanguage) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .putString(KEY_LANGUAGE, language.name)
            .apply()
    }

    private fun persistedLanguage(context: Context): AppLanguage {
        val value = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .getString(KEY_LANGUAGE, AppLanguage.SYSTEM.name)
        return value?.let { runCatching { AppLanguage.valueOf(it) }.getOrNull() } ?: AppLanguage.SYSTEM
    }
}
