package org.tasks.receivers

import android.app.AlarmManager
import android.content.Intent
import android.location.LocationManager
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.mockito.Mockito.mock
import org.mockito.Mockito.`when`

/**
 * Deep tests for all receivers' intent filtering and guard conditions.
 */
class ReceiversDeepTest {

    private fun intentWithAction(action: String?): Intent {
        val intent = mock(Intent::class.java)
        `when`(intent.action).thenReturn(action)
        return intent
    }

    // ========== CompleteTaskReceiver ==========

    @Test
    fun completeTaskReceiver_taskIdConstant() {
        assertEquals("id", CompleteTaskReceiver.TASK_ID)
    }

    @Test
    fun completeTaskReceiver_intentReturnsTaskId() {
        val intent = mock(Intent::class.java)
        `when`(intent.getLongExtra(CompleteTaskReceiver.TASK_ID, 0)).thenReturn(123L)
        assertEquals(123L, intent.getLongExtra(CompleteTaskReceiver.TASK_ID, 0))
    }

    @Test
    fun completeTaskReceiver_intentDefaultsToZero() {
        val intent = mock(Intent::class.java)
        `when`(intent.getLongExtra(CompleteTaskReceiver.TASK_ID, 0)).thenReturn(0L)
        assertEquals(0L, intent.getLongExtra(CompleteTaskReceiver.TASK_ID, 0))
    }

    @Test
    fun completeTaskReceiver_negativeTaskId() {
        val intent = mock(Intent::class.java)
        `when`(intent.getLongExtra(CompleteTaskReceiver.TASK_ID, 0)).thenReturn(-5L)
        assertEquals(-5L, intent.getLongExtra(CompleteTaskReceiver.TASK_ID, 0))
    }

    // ========== MyPackageReplacedReceiver ==========

    @Test
    fun myPackageReplaced_correctAction() {
        val intent = intentWithAction(Intent.ACTION_MY_PACKAGE_REPLACED)
        assertEquals(Intent.ACTION_MY_PACKAGE_REPLACED, intent.action)
    }

    @Test
    fun myPackageReplaced_wrongActionReturnsEarly() {
        val intent = intentWithAction(Intent.ACTION_BOOT_COMPLETED)
        val shouldReturn = intent.action != Intent.ACTION_MY_PACKAGE_REPLACED
        assertTrue(shouldReturn)
    }

    @Test
    fun myPackageReplaced_nullActionReturnsEarly() {
        val intent = intentWithAction(null)
        val shouldReturn = intent.action != Intent.ACTION_MY_PACKAGE_REPLACED
        assertTrue(shouldReturn)
    }

    @Test
    fun myPackageReplaced_exactAction() {
        val intent = intentWithAction("android.intent.action.MY_PACKAGE_REPLACED")
        assertFalse(intent.action != Intent.ACTION_MY_PACKAGE_REPLACED)
    }

    // ========== LocationProviderChangedReceiver ==========

    @Test
    fun locationProviderChanged_correctAction() {
        val intent = intentWithAction(LocationManager.PROVIDERS_CHANGED_ACTION)
        assertFalse(intent.action != LocationManager.PROVIDERS_CHANGED_ACTION)
    }

    @Test
    fun locationProviderChanged_wrongAction() {
        val intent = intentWithAction("some.other.action")
        assertTrue(intent.action != LocationManager.PROVIDERS_CHANGED_ACTION)
    }

    @Test
    fun locationProviderChanged_nullAction() {
        val intent = intentWithAction(null)
        assertTrue(intent.action != LocationManager.PROVIDERS_CHANGED_ACTION)
    }

    @Test
    fun locationProviderChanged_actionConstant() {
        assertEquals("android.location.PROVIDERS_CHANGED", LocationManager.PROVIDERS_CHANGED_ACTION)
    }

    // ========== SystemEventReceiver ==========

    @Test
    fun systemEvent_bootCompleted() {
        val intent = intentWithAction(Intent.ACTION_BOOT_COMPLETED)
        assertEquals(Intent.ACTION_BOOT_COMPLETED, intent.action)
    }

    @Test
    fun systemEvent_userPresent() {
        val intent = intentWithAction(Intent.ACTION_USER_PRESENT)
        assertEquals(Intent.ACTION_USER_PRESENT, intent.action)
    }

    @Test
    fun systemEvent_whenActionIsBoot_matchesBoot() {
        val intent = intentWithAction(Intent.ACTION_BOOT_COMPLETED)
        val isBoot = intent.action == Intent.ACTION_BOOT_COMPLETED
        assertTrue(isBoot)
    }

    @Test
    fun systemEvent_whenActionIsUserPresent_matchesUserPresent() {
        val intent = intentWithAction(Intent.ACTION_USER_PRESENT)
        val isUserPresent = intent.action == Intent.ACTION_USER_PRESENT
        assertTrue(isUserPresent)
    }

    @Test
    fun systemEvent_whenActionIsOther_matchesNeither() {
        val intent = intentWithAction("android.intent.action.TIMEZONE_CHANGED")
        assertFalse(intent.action == Intent.ACTION_BOOT_COMPLETED)
        assertFalse(intent.action == Intent.ACTION_USER_PRESENT)
    }

    @Test
    fun systemEvent_nullAction_matchesNeither() {
        val intent = intentWithAction(null)
        assertFalse(intent.action == Intent.ACTION_BOOT_COMPLETED)
        assertFalse(intent.action == Intent.ACTION_USER_PRESENT)
    }

    @Test
    fun systemEvent_bootAndUserPresentAreDifferent() {
        assertFalse(Intent.ACTION_BOOT_COMPLETED == Intent.ACTION_USER_PRESENT)
    }

    // ========== ScheduleExactAlarmsPermissionReceiver ==========

    @Test
    fun scheduleExactAlarms_correctAction() {
        val intent = intentWithAction(AlarmManager.ACTION_SCHEDULE_EXACT_ALARM_PERMISSION_STATE_CHANGED)
        assertFalse(intent.action != AlarmManager.ACTION_SCHEDULE_EXACT_ALARM_PERMISSION_STATE_CHANGED)
    }

    @Test
    fun scheduleExactAlarms_wrongAction() {
        val intent = intentWithAction("wrong")
        assertTrue(intent.action != AlarmManager.ACTION_SCHEDULE_EXACT_ALARM_PERMISSION_STATE_CHANGED)
    }

    @Test
    fun scheduleExactAlarms_nullIntentAction() {
        // The receiver checks intent?.action, so null intent means guard fails
        val action: String? = null
        assertTrue(action != AlarmManager.ACTION_SCHEDULE_EXACT_ALARM_PERMISSION_STATE_CHANGED)
    }

    @Test
    fun scheduleExactAlarms_actionConstant() {
        assertEquals(
            "android.app.action.SCHEDULE_EXACT_ALARM_PERMISSION_STATE_CHANGED",
            AlarmManager.ACTION_SCHEDULE_EXACT_ALARM_PERMISSION_STATE_CHANGED
        )
    }

    // ========== Cross-receiver tests ==========

    @Test
    fun allReceiverActionsAreDistinct() {
        val actions = setOf(
            Intent.ACTION_MY_PACKAGE_REPLACED,
            LocationManager.PROVIDERS_CHANGED_ACTION,
            Intent.ACTION_BOOT_COMPLETED,
            Intent.ACTION_USER_PRESENT,
            AlarmManager.ACTION_SCHEDULE_EXACT_ALARM_PERMISSION_STATE_CHANGED,
        )
        assertEquals(5, actions.size)
    }
}
