package org.tasks.jobs

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class WorkManagerCompanionTest {

    @Test
    fun tagBackupValue() {
        assertEquals("tag_backup", WorkManager.TAG_BACKUP)
    }

    @Test
    fun tagRefreshValue() {
        assertEquals("tag_refresh", WorkManager.TAG_REFRESH)
    }

    @Test
    fun tagSyncValue() {
        assertEquals("tag_sync", WorkManager.TAG_SYNC)
    }

    @Test
    fun tagBackgroundSyncValue() {
        assertEquals("tag_background_sync", WorkManager.TAG_BACKGROUND_SYNC)
    }

    @Test
    fun tagRemoteConfigValue() {
        assertEquals("tag_remote_config", WorkManager.TAG_REMOTE_CONFIG)
    }

    @Test
    fun tagMigrateLocalValue() {
        assertEquals("tag_migrate_local", WorkManager.TAG_MIGRATE_LOCAL)
    }

    @Test
    fun tagUpdatePurchasesValue() {
        assertEquals("tag_update_purchases", WorkManager.TAG_UPDATE_PURCHASES)
    }

    @Test
    fun tagNotificationsValue() {
        assertEquals("tag_notifications", WorkManager.TAG_NOTIFICATIONS)
    }

    @Test
    fun remoteConfigIntervalHoursIsPositive() {
        assertTrue(WorkManager.REMOTE_CONFIG_INTERVAL_HOURS > 0)
    }

    @Test
    fun remoteConfigIntervalHoursIsAtMostTwelve() {
        assertTrue(WorkManager.REMOTE_CONFIG_INTERVAL_HOURS <= 12)
    }

    @Test
    fun allTagConstantsAreDistinct() {
        val tags = listOf(
            WorkManager.TAG_BACKUP,
            WorkManager.TAG_REFRESH,
            WorkManager.TAG_SYNC,
            WorkManager.TAG_BACKGROUND_SYNC,
            WorkManager.TAG_REMOTE_CONFIG,
            WorkManager.TAG_MIGRATE_LOCAL,
            WorkManager.TAG_UPDATE_PURCHASES,
            WorkManager.TAG_NOTIFICATIONS,
        )
        assertEquals(tags.size, tags.toSet().size)
    }

    @Test
    fun allTagsStartWithTagPrefix() {
        val tags = listOf(
            WorkManager.TAG_BACKUP,
            WorkManager.TAG_REFRESH,
            WorkManager.TAG_SYNC,
            WorkManager.TAG_BACKGROUND_SYNC,
            WorkManager.TAG_REMOTE_CONFIG,
            WorkManager.TAG_MIGRATE_LOCAL,
            WorkManager.TAG_UPDATE_PURCHASES,
            WorkManager.TAG_NOTIFICATIONS,
        )
        tags.forEach { tag ->
            assertTrue("Tag '$tag' should start with 'tag_'", tag.startsWith("tag_"))
        }
    }

    @Test
    fun allTagsAreNonEmpty() {
        val tags = listOf(
            WorkManager.TAG_BACKUP,
            WorkManager.TAG_REFRESH,
            WorkManager.TAG_SYNC,
            WorkManager.TAG_BACKGROUND_SYNC,
            WorkManager.TAG_REMOTE_CONFIG,
            WorkManager.TAG_MIGRATE_LOCAL,
            WorkManager.TAG_UPDATE_PURCHASES,
            WorkManager.TAG_NOTIFICATIONS,
        )
        tags.forEach { tag ->
            assertTrue("Tag should not be empty", tag.isNotEmpty())
        }
    }

    @Test
    fun exactlyEightTags() {
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
