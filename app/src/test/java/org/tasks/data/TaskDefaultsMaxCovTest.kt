package org.tasks.data

import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever
import org.tasks.data.entity.Alarm
import org.tasks.data.entity.Alarm.Companion.TYPE_RANDOM
import org.tasks.data.entity.Alarm.Companion.TYPE_REL_END
import org.tasks.data.entity.Alarm.Companion.TYPE_REL_START
import org.tasks.data.entity.Task
import org.tasks.preferences.AppPreferences
import org.tasks.time.ONE_HOUR

class TaskDefaultsMaxCovTest {

    // Helper: create a hideUntil value that has a "time" component
    // hasDueTime(x) => x > 0 && x % 60000 > 0
    private fun hideUntilWithTime(): Long {
        // Use withSecondOfMinute(1) to make it have time component
        val base = System.currentTimeMillis()
        // Ensure modulo 60000 > 0 by adding 1 second (1000ms)
        return (base / 60000) * 60000 + 1000
    }

    // Helper: create a hideUntil value that does NOT have a time
    // hasDueTime(x) => false when x > 0 && x % 60000 == 0
    private fun hideUntilAllDay(): Long {
        val base = System.currentTimeMillis()
        return (base / 60000) * 60000  // exact minute boundary
    }

    // Helper: create a dueDate that has time component
    private fun dueDateWithTime(): Long {
        val base = System.currentTimeMillis()
        return (base / 60000) * 60000 + 1000
    }

    // Helper: create a dueDate that is all-day (no time)
    private fun dueDateAllDay(): Long {
        val base = System.currentTimeMillis()
        return (base / 60000) * 60000
    }

    // ===== setDefaultReminders =====

    @Test
    fun setDefaultRemindersBasic() = runTest {
        val preferences: AppPreferences = mock()
        whenever(preferences.defaultRandomHours()).thenReturn(2)
        whenever(preferences.defaultAlarms()).thenReturn(listOf(
            Alarm(time = 0, type = TYPE_REL_START),
            Alarm(time = 0, type = TYPE_REL_END),
        ))
        whenever(preferences.defaultRingMode()).thenReturn(1)

        val task = Task()
        task.setDefaultReminders(preferences)

        assertEquals(2 * ONE_HOUR, task.randomReminder)
        assertEquals(1, task.ringFlags)
    }

    @Test
    fun setDefaultRemindersZeroRandomHours() = runTest {
        val preferences: AppPreferences = mock()
        whenever(preferences.defaultRandomHours()).thenReturn(0)
        whenever(preferences.defaultAlarms()).thenReturn(emptyList())
        whenever(preferences.defaultRingMode()).thenReturn(0)

        val task = Task()
        task.setDefaultReminders(preferences)

        assertEquals(0L, task.randomReminder)
        assertEquals(0, task.ringFlags)
    }

    // ===== getDefaultAlarms =====

    @Test
    fun getDefaultAlarmsEmpty() {
        val task = Task(id = 1L)
        val result = task.getDefaultAlarms(false)
        assertTrue(result.isEmpty())
    }

    @Test
    fun getDefaultAlarmsWithRelStartAndStartTime() = runTest {
        val preferences: AppPreferences = mock()
        whenever(preferences.defaultRandomHours()).thenReturn(0)
        whenever(preferences.defaultAlarms()).thenReturn(listOf(
            Alarm(time = 0, type = TYPE_REL_START),
        ))
        whenever(preferences.defaultRingMode()).thenReturn(0)

        val task = Task(id = 1L, hideUntil = hideUntilWithTime())
        task.setDefaultReminders(preferences)

        val alarms = task.getDefaultAlarms(false)
        assertEquals(1, alarms.size)
        assertEquals(TYPE_REL_START, alarms[0].type)
        assertEquals(1L, alarms[0].task)
    }

    @Test
    fun getDefaultAlarmsWithRelEndAndDueTime() = runTest {
        val preferences: AppPreferences = mock()
        whenever(preferences.defaultRandomHours()).thenReturn(0)
        whenever(preferences.defaultAlarms()).thenReturn(listOf(
            Alarm(time = 0, type = TYPE_REL_END),
        ))
        whenever(preferences.defaultRingMode()).thenReturn(0)

        val task = Task(id = 2L, dueDate = dueDateWithTime())
        task.setDefaultReminders(preferences)

        val alarms = task.getDefaultAlarms(false)
        assertEquals(1, alarms.size)
        assertEquals(TYPE_REL_END, alarms[0].type)
        assertEquals(2L, alarms[0].task)
    }

    @Test
    fun getDefaultAlarmsRelStartWithoutStartDateExcluded() = runTest {
        val preferences: AppPreferences = mock()
        whenever(preferences.defaultRandomHours()).thenReturn(0)
        whenever(preferences.defaultAlarms()).thenReturn(listOf(
            Alarm(time = 0, type = TYPE_REL_START),
        ))
        whenever(preferences.defaultRingMode()).thenReturn(0)

        val task = Task(id = 3L, hideUntil = 0L)
        task.setDefaultReminders(preferences)

        val alarms = task.getDefaultAlarms(false)
        assertTrue(alarms.isEmpty())
    }

    @Test
    fun getDefaultAlarmsRelEndWithoutDueDateExcluded() = runTest {
        val preferences: AppPreferences = mock()
        whenever(preferences.defaultRandomHours()).thenReturn(0)
        whenever(preferences.defaultAlarms()).thenReturn(listOf(
            Alarm(time = 0, type = TYPE_REL_END),
        ))
        whenever(preferences.defaultRingMode()).thenReturn(0)

        val task = Task(id = 4L, dueDate = 0L)
        task.setDefaultReminders(preferences)

        val alarms = task.getDefaultAlarms(false)
        assertTrue(alarms.isEmpty())
    }

    @Test
    fun getDefaultAlarmsWithRandomReminder() = runTest {
        val preferences: AppPreferences = mock()
        whenever(preferences.defaultRandomHours()).thenReturn(3)
        whenever(preferences.defaultAlarms()).thenReturn(emptyList())
        whenever(preferences.defaultRingMode()).thenReturn(0)

        val task = Task(id = 5L)
        task.setDefaultReminders(preferences)

        val alarms = task.getDefaultAlarms(false)
        assertEquals(1, alarms.size)
        assertEquals(TYPE_RANDOM, alarms[0].type)
        assertEquals(3 * ONE_HOUR, alarms[0].time)
        assertEquals(5L, alarms[0].task)
    }

    @Test
    fun getDefaultAlarmsRelStartWithAllDayAndDefaultRemindersEnabled() = runTest {
        val preferences: AppPreferences = mock()
        whenever(preferences.defaultRandomHours()).thenReturn(0)
        whenever(preferences.defaultAlarms()).thenReturn(listOf(
            Alarm(time = 0, type = TYPE_REL_START),
        ))
        whenever(preferences.defaultRingMode()).thenReturn(0)

        // All-day start date (no time)
        val task = Task(id = 6L, hideUntil = hideUntilAllDay())
        task.setDefaultReminders(preferences)

        // defaultRemindersEnabled=true should include even without time
        val alarms = task.getDefaultAlarms(true)
        assertEquals(1, alarms.size)
    }

    @Test
    fun getDefaultAlarmsRelStartWithAllDayAndDefaultRemindersDisabled() = runTest {
        val preferences: AppPreferences = mock()
        whenever(preferences.defaultRandomHours()).thenReturn(0)
        whenever(preferences.defaultAlarms()).thenReturn(listOf(
            Alarm(time = 0, type = TYPE_REL_START),
        ))
        whenever(preferences.defaultRingMode()).thenReturn(0)

        // All-day start date (no time)
        val task = Task(id = 7L, hideUntil = hideUntilAllDay())
        task.setDefaultReminders(preferences)

        // defaultRemindersEnabled=false should NOT include
        val alarms = task.getDefaultAlarms(false)
        assertTrue(alarms.isEmpty())
    }

    @Test
    fun getDefaultAlarmsRelEndWithAllDayDueAndDefaultRemindersEnabled() = runTest {
        val preferences: AppPreferences = mock()
        whenever(preferences.defaultRandomHours()).thenReturn(0)
        whenever(preferences.defaultAlarms()).thenReturn(listOf(
            Alarm(time = 0, type = TYPE_REL_END),
        ))
        whenever(preferences.defaultRingMode()).thenReturn(0)

        // All-day due date (no time)
        val task = Task(id = 8L, dueDate = dueDateAllDay())
        task.setDefaultReminders(preferences)

        val alarms = task.getDefaultAlarms(true)
        assertEquals(1, alarms.size)
    }

    @Test
    fun getDefaultAlarmsRelEndWithAllDayDueAndDefaultRemindersDisabled() = runTest {
        val preferences: AppPreferences = mock()
        whenever(preferences.defaultRandomHours()).thenReturn(0)
        whenever(preferences.defaultAlarms()).thenReturn(listOf(
            Alarm(time = 0, type = TYPE_REL_END),
        ))
        whenever(preferences.defaultRingMode()).thenReturn(0)

        val task = Task(id = 9L, dueDate = dueDateAllDay())
        task.setDefaultReminders(preferences)

        val alarms = task.getDefaultAlarms(false)
        assertTrue(alarms.isEmpty())
    }

    @Test
    fun getDefaultAlarmsMixed() = runTest {
        val preferences: AppPreferences = mock()
        whenever(preferences.defaultRandomHours()).thenReturn(1)
        whenever(preferences.defaultAlarms()).thenReturn(listOf(
            Alarm(time = 0, type = TYPE_REL_START),
            Alarm(time = 0, type = TYPE_REL_END),
        ))
        whenever(preferences.defaultRingMode()).thenReturn(0)

        val task = Task(
            id = 10L,
            dueDate = dueDateWithTime(),
            hideUntil = hideUntilWithTime(),
        )
        task.setDefaultReminders(preferences)

        val alarms = task.getDefaultAlarms(false)
        // 2 alarms (start + end) + 1 random = 3
        assertEquals(3, alarms.size)
    }
}
