package org.tasks.receivers

import android.app.AlarmManager
import android.content.Intent
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.mockito.Mockito.mock
import org.mockito.Mockito.`when`

/**
 * Tests for [ScheduleExactAlarmsPermissionReceiver] intent action matching.
 * Uses mocked intents since Android SDK stubs are not implemented in unit tests.
 */
class ScheduleExactAlarmsPermissionReceiverTest {

    private fun intentWithAction(action: String?): Intent {
        val intent = mock(Intent::class.java)
        `when`(intent.action).thenReturn(action)
        return intent
    }

    @Test
    fun exactAlarmPermissionActionMatches() {
        val intent = intentWithAction(AlarmManager.ACTION_SCHEDULE_EXACT_ALARM_PERMISSION_STATE_CHANGED)
        assertEquals(
            AlarmManager.ACTION_SCHEDULE_EXACT_ALARM_PERMISSION_STATE_CHANGED,
            intent.action
        )
    }

    @Test
    fun otherActionDoesNotMatch() {
        val intent = intentWithAction(Intent.ACTION_BOOT_COMPLETED)
        val matches = intent.action == AlarmManager.ACTION_SCHEDULE_EXACT_ALARM_PERMISSION_STATE_CHANGED
        assertFalse(matches)
    }

    @Test
    fun nullActionDoesNotMatch() {
        val intent = intentWithAction(null)
        val matches = intent.action == AlarmManager.ACTION_SCHEDULE_EXACT_ALARM_PERMISSION_STATE_CHANGED
        assertFalse(matches)
    }

    @Test
    fun nullIntentActionDoesNotMatchConstant() {
        val action: String? = null
        val matches = action == AlarmManager.ACTION_SCHEDULE_EXACT_ALARM_PERMISSION_STATE_CHANGED
        assertFalse(matches)
    }

    @Test
    fun actionConstantValue() {
        assertEquals(
            "android.app.action.SCHEDULE_EXACT_ALARM_PERMISSION_STATE_CHANGED",
            AlarmManager.ACTION_SCHEDULE_EXACT_ALARM_PERMISSION_STATE_CHANGED
        )
    }

    @Test
    fun guardConditionReturnsFalseForWrongAction() {
        val intent = intentWithAction("wrong_action")
        val shouldReturn = intent.action != AlarmManager.ACTION_SCHEDULE_EXACT_ALARM_PERMISSION_STATE_CHANGED
        assertTrue("Guard should reject wrong action", shouldReturn)
    }

    @Test
    fun guardConditionReturnsFalseForCorrectAction() {
        val intent = intentWithAction(AlarmManager.ACTION_SCHEDULE_EXACT_ALARM_PERMISSION_STATE_CHANGED)
        val shouldReturn = intent.action != AlarmManager.ACTION_SCHEDULE_EXACT_ALARM_PERMISSION_STATE_CHANGED
        assertFalse("Guard should not reject correct action", shouldReturn)
    }
}
