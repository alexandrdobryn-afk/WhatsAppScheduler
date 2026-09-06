package com.example.wascheduler.core.locale

import com.example.wascheduler.data.repository.AppLanguage
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class AppLocaleControllerTest {
    @Test
    fun `system maps to empty locale list`() {
        assertTrue(AppLocaleController.localeListFor(AppLanguage.SYSTEM).isEmpty)
    }

    @Test
    fun `english maps to en language tag`() {
        assertEquals("en", AppLocaleController.localeListFor(AppLanguage.EN).toLanguageTags())
    }

    @Test
    fun `russian maps to ru language tag`() {
        assertEquals("ru", AppLocaleController.localeListFor(AppLanguage.RU).toLanguageTags())
    }

    @Test
    fun `ukrainian maps to uk language tag`() {
        assertEquals("uk", AppLocaleController.localeListFor(AppLanguage.UK).toLanguageTags())
    }
}
