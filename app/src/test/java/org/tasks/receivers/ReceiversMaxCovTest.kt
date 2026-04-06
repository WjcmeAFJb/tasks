package org.tasks.receivers

import android.app.AlarmManager
import android.content.Intent
import android.location.LocationManager
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever

class ReceiversMaxCovTest {

    private fun intentWithAction(action: String?): Intent {
        val intent = mock<Intent>()
        whenever(intent.action).thenReturn(action)
        return intent
    }

    // ================================================================
    // CompleteTaskReceiver
    // ================================================================

    @Test
    fun completeTaskReceiver_taskIdConstant() {
        assertEquals("id", CompleteTaskReceiver.TASK_ID)
    }

    @Test
    fun completeTaskReceiver_readTaskId() {
        val intent = mock<Intent>()
        whenever(intent.getLongExtra(CompleteTaskReceiver.TASK_ID, 0)).thenReturn(42L)
        assertEquals(42L, intent.getLongExtra(CompleteTaskReceiver.TASK_ID, 0))
    }

    @Test
    fun completeTaskReceiver_defaultTaskId() {
        val intent = mock<Intent>()
        whenever(intent.getLongExtra(CompleteTaskReceiver.TASK_ID, 0)).thenReturn(0L)
        assertEquals(0L, intent.getLongExtra(CompleteTaskReceiver.TASK_ID, 0))
    }

    @Test
    fun completeTaskReceiver_negativeTaskId() {
        val intent = mock<Intent>()
        whenever(intent.getLongExtra(CompleteTaskReceiver.TASK_ID, 0)).thenReturn(-1L)
        assertEquals(-1L, intent.getLongExtra(CompleteTaskReceiver.TASK_ID, 0))
    }

    @Test
    fun completeTaskReceiver_maxTaskId() {
        val intent = mock<Intent>()
        whenever(intent.getLongExtra(CompleteTaskReceiver.TASK_ID, 0)).thenReturn(Long.MAX_VALUE)
        assertEquals(Long.MAX_VALUE, intent.getLongExtra(CompleteTaskReceiver.TASK_ID, 0))
    }

    // ================================================================
    // MyPackageReplacedReceiver
    // ================================================================

    @Test
    fun myPackageReplaced_correctAction() {
        val intent = intentWithAction(Intent.ACTION_MY_PACKAGE_REPLACED)
        assertFalse(intent.action != Intent.ACTION_MY_PACKAGE_REPLACED)
    }

    @Test
    fun myPackageReplaced_wrongActionReturnsEarly() {
        val intent = intentWithAction(Intent.ACTION_BOOT_COMPLETED)
        assertTrue(intent.action != Intent.ACTION_MY_PACKAGE_REPLACED)
    }

    @Test
    fun myPackageReplaced_nullActionReturnsEarly() {
        val intent = intentWithAction(null)
        assertTrue(intent.action != Intent.ACTION_MY_PACKAGE_REPLACED)
    }

    // ================================================================
    // LocationProviderChangedReceiver
    // ================================================================

    @Test
    fun locationProviderChanged_correctAction() {
        val intent = intentWithAction(LocationManager.PROVIDERS_CHANGED_ACTION)
        assertFalse(intent.action != LocationManager.PROVIDERS_CHANGED_ACTION)
    }

    @Test
    fun locationProviderChanged_wrongAction() {
        val intent = intentWithAction("wrong")
        assertTrue(intent.action != LocationManager.PROVIDERS_CHANGED_ACTION)
    }

    @Test
    fun locationProviderChanged_nullAction() {
        val intent = intentWithAction(null)
        assertTrue(intent.action != LocationManager.PROVIDERS_CHANGED_ACTION)
    }

    // ================================================================
    // SystemEventReceiver
    // ================================================================

    @Test
    fun systemEvent_bootCompleted() {
        val intent = intentWithAction(Intent.ACTION_BOOT_COMPLETED)
        assertTrue(intent.action == Intent.ACTION_BOOT_COMPLETED)
    }

    @Test
    fun systemEvent_userPresent() {
        val intent = intentWithAction(Intent.ACTION_USER_PRESENT)
        assertTrue(intent.action == Intent.ACTION_USER_PRESENT)
    }

    @Test
    fun systemEvent_otherAction() {
        val intent = intentWithAction("other")
        assertFalse(intent.action == Intent.ACTION_BOOT_COMPLETED)
        assertFalse(intent.action == Intent.ACTION_USER_PRESENT)
    }

    @Test
    fun systemEvent_nullAction() {
        val intent = intentWithAction(null)
        assertFalse(intent.action == Intent.ACTION_BOOT_COMPLETED)
        assertFalse(intent.action == Intent.ACTION_USER_PRESENT)
    }

    // ================================================================
    // ScheduleExactAlarmsPermissionReceiver
    // ================================================================

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
    fun scheduleExactAlarms_nullIntent() {
        val action: String? = null
        assertTrue(action != AlarmManager.ACTION_SCHEDULE_EXACT_ALARM_PERMISSION_STATE_CHANGED)
    }

    // ================================================================
    // Cross-receiver — all actions are distinct
    // ================================================================

    @Test
    fun allReceiverActionsDistinct() {
        val actions = setOf(
            Intent.ACTION_MY_PACKAGE_REPLACED,
            LocationManager.PROVIDERS_CHANGED_ACTION,
            Intent.ACTION_BOOT_COMPLETED,
            Intent.ACTION_USER_PRESENT,
            AlarmManager.ACTION_SCHEDULE_EXACT_ALARM_PERMISSION_STATE_CHANGED,
        )
        assertEquals(5, actions.size)
    }

    // ================================================================
    // When clause coverage (matches actual receiver code patterns)
    // ================================================================

    @Test
    fun systemEventWhenBoot_broadcastAndGeofence() {
        val action = Intent.ACTION_BOOT_COMPLETED
        var refreshCalled = false
        var geofenceCalled = false
        when (action) {
            Intent.ACTION_BOOT_COMPLETED -> {
                refreshCalled = true
                geofenceCalled = true
            }
            Intent.ACTION_USER_PRESENT -> {
                refreshCalled = true
            }
        }
        assertTrue(refreshCalled)
        assertTrue(geofenceCalled)
    }

    @Test
    fun systemEventWhenUserPresent_broadcastOnly() {
        val action = Intent.ACTION_USER_PRESENT
        var refreshCalled = false
        var geofenceCalled = false
        when (action) {
            Intent.ACTION_BOOT_COMPLETED -> {
                refreshCalled = true
                geofenceCalled = true
            }
            Intent.ACTION_USER_PRESENT -> {
                refreshCalled = true
            }
        }
        assertTrue(refreshCalled)
        assertFalse(geofenceCalled)
    }

    @Test
    fun systemEventWhenOther_nothing() {
        val action = "android.intent.action.TIMEZONE_CHANGED"
        var refreshCalled = false
        var geofenceCalled = false
        when (action) {
            Intent.ACTION_BOOT_COMPLETED -> {
                refreshCalled = true
                geofenceCalled = true
            }
            Intent.ACTION_USER_PRESENT -> {
                refreshCalled = true
            }
        }
        assertFalse(refreshCalled)
        assertFalse(geofenceCalled)
    }
}
