package com.example.wascheduler.core.automation

import com.example.wascheduler.core.logging.LogComponent
import com.example.wascheduler.core.logging.Logger
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class WhatsAppOperationCoordinator @Inject constructor() {
    private val mutex = Mutex()
    private var logInfo: (String) -> Unit = { message ->
        Logger.i(LogComponent.ACCESSIBILITY, message)
    }

    suspend fun <T> runExclusive(operationName: String, block: suspend () -> T): T {
        logInfo("Waiting for WhatsApp operation lock: $operationName")
        return mutex.withLock {
            logInfo("WhatsApp operation lock acquired: $operationName")
            try {
                block()
            } finally {
                logInfo("WhatsApp operation lock released: $operationName")
            }
        }
    }

    internal fun replaceLoggerForTests(logger: (String) -> Unit) {
        logInfo = logger
    }
}
