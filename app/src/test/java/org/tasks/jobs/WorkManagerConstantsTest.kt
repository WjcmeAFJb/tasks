package org.tasks.jobs

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class WorkManagerConstantsTest {

    @Test
    fun tagBackup() {
        assertEquals("tag_backup", WorkManager.TAG_BACKUP)
    }

    @Test
    fun tagRefresh() {
        assertEquals("tag_refresh", WorkManager.TAG_REFRESH)
    }

    @Test
    fun tagSync() {
        assertEquals("tag_sync", WorkManager.TAG_SYNC)
    }

    @Test
    fun tagBackgroundSync() {
        assertEquals("tag_background_sync", WorkManager.TAG_BACKGROUND_SYNC)
    }

    @Test
    fun tagRemoteConfig() {
        assertEquals("tag_remote_config", WorkManager.TAG_REMOTE_CONFIG)
    }

    @Test
    fun tagMigrateLocal() {
        assertEquals("tag_migrate_local", WorkManager.TAG_MIGRATE_LOCAL)
    }

    @Test
    fun tagUpdatePurchases() {
        assertEquals("tag_update_purchases", WorkManager.TAG_UPDATE_PURCHASES)
    }

    @Test
    fun tagNotifications() {
        assertEquals("tag_notifications", WorkManager.TAG_NOTIFICATIONS)
    }

    @Test
    fun remoteConfigIntervalIsPositive() {
        assertTrue(WorkManager.REMOTE_CONFIG_INTERVAL_HOURS > 0)
    }

    @Test
    fun allTagsAreDistinct() {
        val tags = setOf(
            WorkManager.TAG_BACKUP,
            WorkManager.TAG_REFRESH,
            WorkManager.TAG_SYNC,
            WorkManager.TAG_BACKGROUND_SYNC,
            WorkManager.TAG_REMOTE_CONFIG,
            WorkManager.TAG_MIGRATE_LOCAL,
            WorkManager.TAG_UPDATE_PURCHASES,
            WorkManager.TAG_NOTIFICATIONS,
        )
        assertEquals(8, tags.size)
    }
}
