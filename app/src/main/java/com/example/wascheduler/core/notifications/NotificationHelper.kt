package com.example.wascheduler.core.notifications

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.example.wascheduler.App
import com.example.wascheduler.R
import com.example.wascheduler.domain.model.ErrorCode
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class NotificationHelper @Inject constructor(
    @ApplicationContext private val context: Context
) {
    fun notifySent(chatName: String, timeLabel: String) {
        show(
            id = chatName.hashCode(),
            title = context.getString(R.string.notification_sent_title),
            text = "$timeLabel · $chatName"
        )
    }

    fun notifyFailed(chatName: String, timeLabel: String, errorCode: ErrorCode) {
        val reasonResId = context.resources.getIdentifier(errorCode.stringResKey, "string", context.packageName)
        val reason = if (reasonResId != 0) context.getString(reasonResId) else errorCode.name
        show(
            id = chatName.hashCode(),
            title = context.getString(R.string.notification_failed_title),
            text = "$timeLabel · $chatName — $reason"
        )
    }

    private fun show(id: Int, title: String, text: String) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ActivityCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
        ) {
            return
        }
        val notification = NotificationCompat.Builder(context, App.NOTIFICATION_CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_dialog_email)
            .setContentTitle(title)
            .setContentText(text)
            .setAutoCancel(true)
            .build()
        NotificationManagerCompat.from(context).notify(id, notification)
    }
}
