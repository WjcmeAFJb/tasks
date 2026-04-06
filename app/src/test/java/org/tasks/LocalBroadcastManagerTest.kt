package org.tasks

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Test

class LocalBroadcastManagerTest {

    // ===== Companion constants =====

    @Test
    fun refreshConstant() {
        assertEquals("${BuildConfig.APPLICATION_ID}.REFRESH", LocalBroadcastManager.REFRESH)
    }

    @Test
    fun refreshConstantContainsAppId() {
        assert(LocalBroadcastManager.REFRESH.contains(BuildConfig.APPLICATION_ID))
    }

    @Test
    fun refreshConstantEndsWithRefresh() {
        assert(LocalBroadcastManager.REFRESH.endsWith(".REFRESH"))
    }

    @Test
    fun refreshConstantNotEmpty() {
        assert(LocalBroadcastManager.REFRESH.isNotEmpty())
    }
}
