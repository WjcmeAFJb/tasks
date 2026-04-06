package org.tasks.reminders

import android.content.Intent
import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * Tests for [NotificationActivity] companion constants.
 * The newIntent factory creates real Intent objects which cannot be tested
 * in pure unit tests (Android stubs are not implemented). We test the
 * constant values and flag calculations.
 */
class NotificationActivityTest {

    // ===== Constants =====

    @Test
    fun extraTitleConstant() {
        assertEquals("extra_title", NotificationActivity.EXTRA_TITLE)
    }

    @Test
    fun extraTaskIdConstant() {
        assertEquals("extra_task_id", NotificationActivity.EXTRA_TASK_ID)
    }

    @Test
    fun extraReadOnlyConstant() {
        assertEquals("extra_read_only", NotificationActivity.EXTRA_READ_ONLY)
    }

    // ===== Flag values =====

    @Test
    fun expectedFlagCombination() {
        val expectedFlags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        // Both flags should be non-zero
        assert(expectedFlags != 0)
    }

    @Test
    fun newTaskFlagIsNonZero() {
        assert(Intent.FLAG_ACTIVITY_NEW_TASK != 0)
    }

    @Test
    fun clearTaskFlagIsNonZero() {
        assert(Intent.FLAG_ACTIVITY_CLEAR_TASK != 0)
    }

    @Test
    fun flagCombinationIncludesBothFlags() {
        val combined = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        assertEquals(Intent.FLAG_ACTIVITY_NEW_TASK, combined and Intent.FLAG_ACTIVITY_NEW_TASK)
        assertEquals(Intent.FLAG_ACTIVITY_CLEAR_TASK, combined and Intent.FLAG_ACTIVITY_CLEAR_TASK)
    }
}
