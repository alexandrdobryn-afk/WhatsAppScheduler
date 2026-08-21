package com.example.wascheduler.service

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import com.example.wascheduler.core.automation.ExecutionWorker
import com.example.wascheduler.core.logging.LogComponent
import com.example.wascheduler.core.logging.Logger
import dagger.hilt.android.AndroidEntryPoint

/**
 * A BroadcastReceiver's onReceive() window is far too short for the full
 * automation pipeline (WhatsApp launch + node polling can take many seconds),
 * so every receiver here does the minimum possible amount of work — enqueue a
 * guaranteed WorkManager job — and returns immediately.
 */
@AndroidEntryPoint
class AlarmReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        Logger.i(LogComponent.SCHEDULER, "Alarm fired — enqueueing ExecutionWorker")
        enqueueExecutionWorker(context)
    }
}

@AndroidEntryPoint
class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        Logger.i(LogComponent.SCHEDULER, "Received ${intent.action} — restoring schedule")
        // BOOT_COMPLETED and MY_PACKAGE_REPLACED both mean any previously-armed
        // AlarmManager alarm is gone (spec sections 19 and 72); ExecutionWorker
        // recomputes due occurrences AND re-arms the next alarm from the DB,
        // which remains the single source of truth (spec section 100).
        enqueueExecutionWorker(context)
    }
}

@AndroidEntryPoint
class TimeChangeReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        Logger.i(LogComponent.SCHEDULER, "Received ${intent.action} — rescheduling from current time")
        // A manual clock/timezone/date change invalidates whatever the previously
        // scheduled alarm instant assumed (spec section 20); recompute fully.
        enqueueExecutionWorker(context)
    }
}

private fun enqueueExecutionWorker(context: Context) {
    val request = OneTimeWorkRequestBuilder<ExecutionWorker>().build()
    WorkManager.getInstance(context)
        .enqueueUniqueWork(ExecutionWorker.UNIQUE_WORK_NAME, ExistingWorkPolicy.APPEND_OR_REPLACE, request)
}
