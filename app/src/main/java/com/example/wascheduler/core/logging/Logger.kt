package com.example.wascheduler.core.logging

import android.util.Log
import com.example.wascheduler.BuildConfig

/** Fixed set of components so log output can be filtered meaningfully (spec section 83). */
enum class LogComponent { SCHEDULER, EXECUTION, ACCESSIBILITY, WHATSAPP_ADAPTER, DATABASE, PERMISSIONS }

object Logger {
    private const val BASE_TAG = "WaScheduler"

    fun d(component: LogComponent, message: String) {
        if (BuildConfig.DEBUG) Log.d(tag(component), message)
    }

    fun i(component: LogComponent, message: String) {
        Log.i(tag(component), message)
    }

    fun w(component: LogComponent, message: String, throwable: Throwable? = null) {
        Log.w(tag(component), message, throwable)
    }

    fun e(component: LogComponent, message: String, throwable: Throwable? = null) {
        Log.e(tag(component), message, throwable)
    }

    private fun tag(component: LogComponent) = "$BASE_TAG:${component.name}"
}
