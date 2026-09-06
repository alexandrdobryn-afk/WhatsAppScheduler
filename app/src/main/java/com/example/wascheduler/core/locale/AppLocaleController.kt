package com.example.wascheduler.core.locale

import android.content.Context
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.os.LocaleListCompat
import com.example.wascheduler.data.repository.AppLanguage

object AppLocaleController {
    fun localeListFor(language: AppLanguage): LocaleListCompat =
        language.languageTag
            ?.let(LocaleListCompat::forLanguageTags)
            ?: LocaleListCompat.getEmptyLocaleList()

    fun apply(@Suppress("UNUSED_PARAMETER") context: Context, language: AppLanguage) {
        AppCompatDelegate.setApplicationLocales(localeListFor(language))
    }
}
