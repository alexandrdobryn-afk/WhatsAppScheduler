package com.example.wascheduler.core.intents

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.Settings

object ExternalLinks {
    const val PRIVACY_POLICY_URL = "https://alexandrdobryn-afk.github.io/WhatsAppScheduler/privacy/"
    const val WEBSITE_URL = "https://alexandrdobryn-afk.github.io/WhatsAppScheduler/"
    const val SUPPORT_EMAIL = "alexapp.support@gmail.com"
    const val SUPPORT_EMAIL_SUBJECT = "WA Schedule Support"
}

object ExternalIntents {
    fun privacyPolicy(): Intent =
        Intent(Intent.ACTION_VIEW, Uri.parse(ExternalLinks.PRIVACY_POLICY_URL))

    fun website(): Intent =
        Intent(Intent.ACTION_VIEW, Uri.parse(ExternalLinks.WEBSITE_URL))

    fun supportEmail(): Intent =
        Intent(Intent.ACTION_SENDTO, Uri.parse("mailto:${ExternalLinks.SUPPORT_EMAIL}"))
            .putExtra(Intent.EXTRA_SUBJECT, ExternalLinks.SUPPORT_EMAIL_SUBJECT)

    fun accessibilitySettings(): Intent =
        Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)
}

fun Context.startActivityIfAvailable(intent: Intent): Boolean {
    val launchIntent = if (this is Activity) intent else Intent(intent).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    return runCatching {
        startActivity(launchIntent)
        true
    }.getOrDefault(false)
}
