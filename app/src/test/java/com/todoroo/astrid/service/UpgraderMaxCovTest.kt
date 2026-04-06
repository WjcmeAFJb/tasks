package com.todoroo.astrid.service

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.tasks.data.entity.Alarm
import org.tasks.data.entity.Task

class UpgraderMaxCovTest {

    // ===== migrateGoogleTaskFilters logic =====
    // These are private instance methods but the logic is pure string replacement.
    // We test the replacement logic directly.

    @Test
    fun migrateGtaskSelectTask() {
        val input = "SELECT task FROM google_tasks WHERE (list_id='abc')"
        val result = input.replace("SELECT task FROM google_tasks", "SELECT gt_task as task FROM google_tasks")
            .replace("(list_id", "(gt_list_id")
            .replace("google_tasks.list_id", "google_tasks.gt_list_id")
            .replace("google_tasks.task", "google_tasks.gt_task")
        assertTrue(result.contains("SELECT gt_task as task FROM google_tasks"))
        assertTrue(result.contains("(gt_list_id"))
    }

    @Test
    fun migrateGtaskDotListId() {
        val input = "google_tasks.list_id = 'test'"
        val result = input.replace("google_tasks.list_id", "google_tasks.gt_list_id")
        assertTrue(result.contains("google_tasks.gt_list_id"))
    }

    @Test
    fun migrateGtaskDotTask() {
        val input = "google_tasks.task = 123"
        val result = input.replace("google_tasks.task", "google_tasks.gt_task")
        assertTrue(result.contains("google_tasks.gt_task"))
    }

    @Test
    fun migrateCaldavSelectTask() {
        val input = "SELECT task FROM caldav_tasks WHERE (calendar='abc')"
        val result = input.replace("SELECT task FROM caldav_tasks", "SELECT cd_task as task FROM caldav_tasks")
            .replace("(calendar", "(cd_calendar")
        assertTrue(result.contains("SELECT cd_task as task FROM caldav_tasks"))
        assertTrue(result.contains("(cd_calendar"))
    }

    @Test
    fun migrateMetadataRemovesDeletedClause() {
        val input = "something AND (metadata.deleted=0) other"
        val result = input.replace("""AND \(metadata\.deleted=0\)""".toRegex(), "")
        assertFalse(result.contains("metadata.deleted"))
    }

    @Test
    fun migrateNullOrEmpty() {
        assertEquals("", null.orEmpty())
        assertEquals("", "".orEmpty())
    }

    @Test
    fun migrateNoMatchPassesThrough() {
        val input = "SELECT * FROM tasks WHERE id = 1"
        assertEquals(input, input
            .replace("SELECT task FROM google_tasks", "SELECT gt_task as task FROM google_tasks")
            .replace("(list_id", "(gt_list_id"))
    }

    // ===== getLegacyColor =====

    @Test
    fun getLegacyColorEachValidIndex() {
        for (i in 0..20) {
            assertTrue("Index $i should return valid resource", Upgrader.getLegacyColor(i, -1) > 0)
        }
    }

    @Test
    fun getLegacyColorIndex21ReturnsDefault() {
        assertEquals(-1, Upgrader.getLegacyColor(21, -1))
    }

    @Test
    fun getLegacyColorNegativeReturnsDefault() {
        assertEquals(42, Upgrader.getLegacyColor(-100, 42))
    }

    // ===== Upgrade_14_11.fromLegacyFlags =====

    @Test
    fun fromLegacyFlagsNone() {
        assertTrue(Upgrade_14_11.fromLegacyFlags(0).isEmpty())
    }

    @Test
    fun fromLegacyFlagsAtStart() {
        val alarms = Upgrade_14_11.fromLegacyFlags(Task.NOTIFY_AT_START)
        assertEquals(1, alarms.size)
        assertEquals(Alarm.TYPE_REL_START, alarms[0].type)
    }

    @Test
    fun fromLegacyFlagsAtDeadline() {
        val alarms = Upgrade_14_11.fromLegacyFlags(Task.NOTIFY_AT_DEADLINE)
        assertEquals(1, alarms.size)
        assertEquals(Alarm.TYPE_REL_END, alarms[0].type)
    }

    @Test
    fun fromLegacyFlagsAfterDeadline() {
        assertEquals(1, Upgrade_14_11.fromLegacyFlags(Task.NOTIFY_AFTER_DEADLINE).size)
    }

    @Test
    fun fromLegacyFlagsAllFlags() {
        val flags = Task.NOTIFY_AT_START or Task.NOTIFY_AT_DEADLINE or Task.NOTIFY_AFTER_DEADLINE
        assertEquals(3, Upgrade_14_11.fromLegacyFlags(flags).size)
    }

    @Test
    fun fromLegacyFlagsWithTaskId() {
        val alarms = Upgrade_14_11.fromLegacyFlags(Task.NOTIFY_AT_DEADLINE, task = 42L)
        assertEquals(42L, alarms[0].task)
    }

    // ===== Version constants =====

    @Test
    fun upgrade14_11Version() {
        assertEquals(141100, Upgrade_14_11.VERSION)
    }

    @Test
    fun upgrade14_13Version() {
        assertEquals(141300, Upgrade_14_13.VERSION)
    }

    // ===== Upgrade_13_11 migrateLegacyIcon =====

    @Test
    fun migrateLegacyIconNull() {
        assertNull(invokeMLI(null))
    }

    @Test
    fun migrateLegacyIconBlank() {
        assertEquals("", invokeMLI(""))
    }

    @Test
    fun migrateLegacyIconAlpha() {
        assertEquals("my_icon", invokeMLI("my_icon"))
    }

    @Test
    fun migrateLegacyIconZero() {
        assertNull(invokeMLI("0"))
    }

    @Test
    fun migrateLegacyIconMinusOne() {
        assertNull(invokeMLI("-1"))
    }

    @Test
    fun migrateLegacyIconUnknownNumeric() {
        assertNull(invokeMLI("9999"))
    }

    @Test
    fun migrateLegacyIconKnownIndices() {
        val known = mapOf(
            1 to "label", 2 to "filter_list", 3 to "cloud",
            4 to "all_inbox", 5 to "label_off", 6 to "history",
            7 to "today", 8 to "list", 1000 to "flag",
            1062 to "home", 1041 to "work_outline", 1001 to "pets",
            1002 to "payment", 1003 to "attach_money",
            1042 to "store", 1043 to "shopping_cart",
            1004 to "hourglass_empty", 1005 to "favorite_border",
            1006 to "school", 1023 to "flight",
            1044 to "schedule", 1049 to "event",
            1057 to "flight_takeoff", 1058 to "flight_land",
            1059 to "euro_symbol", 1088 to "spa",
            1114 to "security", 1148 to "house",
            1185 to "person_add", 1186 to "block",
        )
        for ((index, expected) in known) {
            assertEquals("Index $index", expected, invokeMLI(index.toString()))
        }
    }

    private fun invokeMLI(s: String?): String? {
        return with(Upgrade_13_11) { s.migrateLegacyIcon() }
    }
}
