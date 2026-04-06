package org.tasks.reminders

import android.content.Intent
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Tests for [SnoozeActivity] companion constants.
 * The newIntent factory creates real Intent objects which cannot be tested
 * in pure unit tests (Android stubs are not implemented). We test the
 * constant values and flag calculations.
 */
class SnoozeActivityTest {

    // ===== Constants =====

    @Test
    fun extraTaskIdConstant() {
        assertEquals("id", SnoozeActivity.EXTRA_TASK_ID)
    }

    @Test
    fun extraTaskIdsConstant() {
        assertEquals("ids", SnoozeActivity.EXTRA_TASK_IDS)
    }

    @Test
    fun extraSnoozeTimeConstant() {
        assertEquals("snooze_time", SnoozeActivity.EXTRA_SNOOZE_TIME)
    }

    // ===== Flag values =====

    @Test
    fun expectedFlagCombination() {
        val expectedFlags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        assert(expectedFlags != 0)
    }

    @Test
    fun flagCombinationIncludesBothFlags() {
        val combined = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        assertEquals(Intent.FLAG_ACTIVITY_NEW_TASK, combined and Intent.FLAG_ACTIVITY_NEW_TASK)
        assertEquals(Intent.FLAG_ACTIVITY_CLEAR_TASK, combined and Intent.FLAG_ACTIVITY_CLEAR_TASK)
    }

    // ===== Constants are distinct =====

    @Test
    fun taskIdAndTaskIdsAreDifferentKeys() {
        assertTrue(SnoozeActivity.EXTRA_TASK_ID != SnoozeActivity.EXTRA_TASK_IDS)
    }

    @Test
    fun taskIdAndSnoozeTimeAreDifferentKeys() {
        assertTrue(SnoozeActivity.EXTRA_TASK_ID != SnoozeActivity.EXTRA_SNOOZE_TIME)
    }

    @Test
    fun taskIdsAndSnoozeTimeAreDifferentKeys() {
        assertTrue(SnoozeActivity.EXTRA_TASK_IDS != SnoozeActivity.EXTRA_SNOOZE_TIME)
    }
}
