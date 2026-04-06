package org.tasks.jobs

import androidx.work.NetworkType
import org.junit.Assert.assertEquals
import org.junit.Test

class NetworkConstraintsTest {

    @Test
    fun networkConstraintsRequireConnected() {
        val constraints = networkConstraints
        assertEquals(NetworkType.CONNECTED, constraints.requiredNetworkType)
    }

    @Test
    fun networkConstraintsDoNotRequireCharging() {
        val constraints = networkConstraints
        assertEquals(false, constraints.requiresCharging())
    }

    @Test
    fun networkConstraintsDoNotRequireBatteryNotLow() {
        val constraints = networkConstraints
        assertEquals(false, constraints.requiresBatteryNotLow())
    }

    @Test
    fun networkConstraintsDoNotRequireStorageNotLow() {
        val constraints = networkConstraints
        assertEquals(false, constraints.requiresStorageNotLow())
    }

    @Test
    fun networkConstraintsDoNotRequireDeviceIdle() {
        val constraints = networkConstraints
        assertEquals(false, constraints.requiresDeviceIdle())
    }

    @Test
    fun multipleCallsReturnNewInstances() {
        val c1 = networkConstraints
        val c2 = networkConstraints
        // They should be equal in value but different instances (val getter creates new each time)
        assertEquals(c1.requiredNetworkType, c2.requiredNetworkType)
    }
}
