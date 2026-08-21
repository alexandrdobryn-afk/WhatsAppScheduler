package com.example.wascheduler.core.device

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class DeviceWakeSnapshotTest {

    @Test
    fun `unlocked screen is ready and cannot auto dismiss anything`() {
        val snapshot = DeviceWakeSnapshot(
            screenInteractive = true,
            keyguardLocked = false,
            deviceLocked = false,
            deviceSecure = false
        )

        assertEquals("ON", snapshot.screenLabel)
        assertEquals("UNLOCKED", snapshot.keyguardLabel)
        assertFalse(snapshot.autoDismissAvailable)
        assertFalse(snapshot.secureDeviceLocked)
    }

    @Test
    fun `non secure keyguard can be dismissed automatically`() {
        val snapshot = DeviceWakeSnapshot(
            screenInteractive = false,
            keyguardLocked = true,
            deviceLocked = true,
            deviceSecure = false
        )

        assertEquals("OFF", snapshot.screenLabel)
        assertEquals("LOCKED", snapshot.keyguardLabel)
        assertEquals("YES", snapshot.autoDismissAvailableLabel)
        assertTrue(snapshot.autoDismissAvailable)
        assertFalse(snapshot.secureDeviceLocked)
    }

    @Test
    fun `secure locked device is blocked before automation`() {
        val snapshot = DeviceWakeSnapshot(
            screenInteractive = false,
            keyguardLocked = true,
            deviceLocked = true,
            deviceSecure = true
        )

        assertEquals("YES", snapshot.secureLockLabel)
        assertEquals("NO", snapshot.autoDismissAvailableLabel)
        assertTrue(snapshot.secureDeviceLocked)
    }
}
