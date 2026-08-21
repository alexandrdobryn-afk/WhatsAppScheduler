package com.example.wascheduler.core.device

import android.app.Activity
import android.app.KeyguardManager
import android.content.Context
import android.content.Intent
import android.os.Bundle
import com.example.wascheduler.core.logging.LogComponent
import com.example.wascheduler.core.logging.Logger
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.withTimeoutOrNull

enum class WakeUnlockResult {
    READY,
    SECURE_LOCKED,
    DISMISS_FAILED,
    TIMEOUT,
    START_FAILED
}

class WakeUnlockActivity : Activity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        setShowWhenLocked(true)
        setTurnScreenOn(true)
        super.onCreate(savedInstanceState)

        val keyguardManager = getSystemService(Context.KEYGUARD_SERVICE) as KeyguardManager
        when {
            !keyguardManager.isKeyguardLocked -> finishWith(WakeUnlockResult.READY)
            keyguardManager.isDeviceSecure && keyguardManager.isDeviceLocked -> finishWith(WakeUnlockResult.SECURE_LOCKED)
            else -> keyguardManager.requestDismissKeyguard(
                this,
                object : KeyguardManager.KeyguardDismissCallback() {
                    override fun onDismissSucceeded() {
                        finishWith(WakeUnlockResult.READY)
                    }

                    override fun onDismissCancelled() {
                        finishWith(WakeUnlockResult.DISMISS_FAILED)
                    }

                    override fun onDismissError() {
                        finishWith(WakeUnlockResult.DISMISS_FAILED)
                    }
                }
            )
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        WakeUnlockRequestRegistry.completeIfPending(WakeUnlockResult.DISMISS_FAILED)
    }

    private fun finishWith(result: WakeUnlockResult) {
        WakeUnlockRequestRegistry.completeIfPending(result)
        finish()
    }

    companion object {
        suspend fun request(context: Context, timeoutMs: Long): WakeUnlockResult {
            val deferred = WakeUnlockRequestRegistry.begin()
            val intent = Intent(context, WakeUnlockActivity::class.java)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                .addFlags(Intent.FLAG_ACTIVITY_NO_ANIMATION)
                .addFlags(Intent.FLAG_ACTIVITY_EXCLUDE_FROM_RECENTS)
            return try {
                context.startActivity(intent)
                withTimeoutOrNull(timeoutMs) { deferred.await() } ?: WakeUnlockResult.TIMEOUT
            } catch (exception: RuntimeException) {
                Logger.e(LogComponent.EXECUTION, "Could not start WakeUnlockActivity", exception)
                WakeUnlockResult.START_FAILED
            } finally {
                WakeUnlockRequestRegistry.clear(deferred)
            }
        }
    }
}

private object WakeUnlockRequestRegistry {
    private val monitor = Any()
    private var pending: CompletableDeferred<WakeUnlockResult>? = null

    fun begin(): CompletableDeferred<WakeUnlockResult> = synchronized(monitor) {
        pending?.complete(WakeUnlockResult.DISMISS_FAILED)
        CompletableDeferred<WakeUnlockResult>().also { pending = it }
    }

    fun completeIfPending(result: WakeUnlockResult) {
        synchronized(monitor) {
            pending?.complete(result)
        }
    }

    fun clear(deferred: CompletableDeferred<WakeUnlockResult>) {
        synchronized(monitor) {
            if (pending === deferred) pending = null
        }
    }
}
