package com.example.wascheduler.core.device

import android.app.KeyguardManager
import android.content.Context
import android.os.PowerManager
import com.example.wascheduler.core.logging.LogComponent
import com.example.wascheduler.core.logging.Logger
import com.example.wascheduler.domain.model.ErrorCode
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import javax.inject.Inject
import javax.inject.Singleton

data class DeviceWakeSnapshot(
    val screenInteractive: Boolean,
    val keyguardLocked: Boolean,
    val deviceLocked: Boolean,
    val deviceSecure: Boolean
) {
    val screenLabel: String get() = if (screenInteractive) "ON" else "OFF"
    val keyguardLabel: String get() = if (keyguardLocked) "LOCKED" else "UNLOCKED"
    val secureLockLabel: String get() = if (deviceSecure) "YES" else "NO"
    val autoDismissAvailableLabel: String get() = if (autoDismissAvailable) "YES" else "NO"
    val autoDismissAvailable: Boolean get() = keyguardLocked && !deviceSecure
    val secureDeviceLocked: Boolean get() = deviceSecure && deviceLocked
}

data class DevicePreparation(
    val errorCode: ErrorCode?,
    val session: DeviceWakeSession,
    val before: DeviceWakeSnapshot,
    val after: DeviceWakeSnapshot
)

class DeviceWakeSession internal constructor(
    private val wakeLock: PowerManager.WakeLock?
) : AutoCloseable {
    override fun close() {
        if (wakeLock?.isHeld == true) {
            wakeLock.release()
            Logger.i(LogComponent.EXECUTION, "Released scheduled-send wake lock")
        }
    }
}

@Singleton
class DeviceWakeController @Inject constructor(
    @ApplicationContext private val context: Context
) {
    fun snapshot(): DeviceWakeSnapshot {
        val powerManager = context.getSystemService(Context.POWER_SERVICE) as PowerManager
        val keyguardManager = context.getSystemService(Context.KEYGUARD_SERVICE) as KeyguardManager
        return DeviceWakeSnapshot(
            screenInteractive = powerManager.isInteractive,
            keyguardLocked = keyguardManager.isKeyguardLocked,
            deviceLocked = keyguardManager.isDeviceLocked,
            deviceSecure = keyguardManager.isDeviceSecure
        )
    }

    suspend fun prepareForScheduledSend(): DevicePreparation = withContext(Dispatchers.Main.immediate) {
        val before = snapshot()
        Logger.i(
            LogComponent.EXECUTION,
            "Device before scheduled send: screen=${before.screenLabel} keyguard=${before.keyguardLabel} secureLock=${before.secureLockLabel} autoDismiss=${before.autoDismissAvailableLabel}"
        )

        val session = DeviceWakeSession(acquireWakeLockIfNeeded(before))
        val error = when {
            before.secureDeviceLocked -> ErrorCode.DEVICE_SECURE_LOCKED
            before.screenInteractive && !before.keyguardLocked -> null
            else -> requestWakeAndDismiss()
        }
        val after = snapshot()
        Logger.i(
            LogComponent.EXECUTION,
            "Device after wake preparation: screen=${after.screenLabel} keyguard=${after.keyguardLabel} secureLock=${after.secureLockLabel} autoDismiss=${after.autoDismissAvailableLabel} result=${error?.name ?: "READY"}"
        )
        DevicePreparation(error, session, before, after)
    }

    private fun acquireWakeLockIfNeeded(snapshot: DeviceWakeSnapshot): PowerManager.WakeLock? {
        if (snapshot.screenInteractive) return null
        val powerManager = context.getSystemService(Context.POWER_SERVICE) as PowerManager
        return powerManager.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, WAKE_LOCK_TAG).apply {
            acquire(WAKE_LOCK_TIMEOUT_MS)
            Logger.i(LogComponent.EXECUTION, "Acquired scheduled-send wake lock for ${WAKE_LOCK_TIMEOUT_MS}ms")
        }
    }

    private suspend fun requestWakeAndDismiss(): ErrorCode? {
        return when (WakeUnlockActivity.request(context, WAKE_DISMISS_TIMEOUT_MS)) {
            WakeUnlockResult.READY -> {
                val ready = withTimeoutOrNull(SCREEN_READY_TIMEOUT_MS) {
                    while (true) {
                        val current = snapshot()
                        if (current.screenInteractive && !current.keyguardLocked) return@withTimeoutOrNull true
                        delay(100L)
                    }
                } == true
                if (ready) null else ErrorCode.DEVICE_LOCKED
            }
            WakeUnlockResult.SECURE_LOCKED -> ErrorCode.DEVICE_SECURE_LOCKED
            WakeUnlockResult.DISMISS_FAILED,
            WakeUnlockResult.TIMEOUT,
            WakeUnlockResult.START_FAILED -> ErrorCode.DEVICE_LOCKED
        }
    }

    companion object {
        private const val WAKE_LOCK_TAG = "WaScheduler:ScheduledSend"
        private const val WAKE_LOCK_TIMEOUT_MS = 90_000L
        private const val WAKE_DISMISS_TIMEOUT_MS = 12_000L
        private const val SCREEN_READY_TIMEOUT_MS = 3_000L
    }
}
