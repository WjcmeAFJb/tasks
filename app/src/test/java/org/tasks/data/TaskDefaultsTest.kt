package org.tasks.data

import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.tasks.data.entity.Alarm
import org.tasks.data.entity.Alarm.Companion.TYPE_RANDOM
import org.tasks.data.entity.Alarm.Companion.TYPE_REL_END
import org.tasks.data.entity.Alarm.Companion.TYPE_REL_START
import org.tasks.data.entity.Task
import org.tasks.preferences.AppPreferences
import org.tasks.time.ONE_HOUR

class TaskDefaultsTest {

    private fun prefs(
        randomHours: Int = 0,
        alarms: List<Alarm> = emptyList(),
        ringMode: Int = 0,
    ): AppPreferences = object : AppPreferences {
        override suspend fun isDefaultDueTimeEnabled() = false
        override suspend fun defaultLocationReminder() = 0
        override suspend fun defaultAlarms() = alarms
        override suspend fun defaultRandomHours() = randomHours
        override suspend fun defaultRingMode() = ringMode
        override suspend fun defaultDueTime() = 0
        override suspend fun defaultPriority() = Task.Priority.NONE
        override suspend fun isCurrentlyQuietHours() = false
        override suspend fun adjustForQuietHours(time: Long) = time
    }

    // --- setDefaultReminders tests ---

    @Test
    fun setDefaultReminders_setsRandomReminderFromPreferences() = runTest {
        val task = Task()
        task.setDefaultReminders(prefs(randomHours = 3))
        assertEquals(3 * ONE_HOUR, task.randomReminder)
    }

    @Test
    fun setDefaultReminders_zeroRandomHoursResultsInZeroReminder() = runTest {
        val task = Task()
        task.setDefaultReminders(prefs(randomHours = 0))
        assertEquals(0L, task.randomReminder)
    }

    @Test
    fun setDefaultReminders_setsRingFlags() = runTest {
        val task = Task()
        task.setDefaultReminders(prefs(ringMode = 5))
        assertEquals(5, task.ringFlags)
    }

    @Test
    fun setDefaultReminders_storesDefaultAlarmsInTransitory() = runTest {
        val alarms = listOf(
            Alarm(type = TYPE_REL_START, time = 0),
            Alarm(type = TYPE_REL_END, time = 0),
        )
        val task = Task()
        task.setDefaultReminders(prefs(alarms = alarms))
        val stored = task.getTransitory<List<Alarm>>(Task.TRANS_DEFAULT_ALARMS)
        assertEquals(alarms, stored)
    }

    @Test
    fun setDefaultReminders_emptyAlarmsListStored() = runTest {
        val task = Task()
        task.setDefaultReminders(prefs(alarms = emptyList()))
        val stored = task.getTransitory<List<Alarm>>(Task.TRANS_DEFAULT_ALARMS)
        assertEquals(emptyList<Alarm>(), stored)
    }

    // --- getDefaultAlarms tests ---

    @Test
    fun getDefaultAlarms_relStartIncludedWhenHasStartDateAndStartTime() {
        // hideUntil % 60000 > 0 means it has start time
        val task = Task(id = 1, hideUntil = 1000001L)
        val alarm = Alarm(type = TYPE_REL_START, time = 0)
        task.putTransitory(Task.TRANS_DEFAULT_ALARMS, listOf(alarm))
        val result = task.getDefaultAlarms(defaultRemindersEnabled = false)
        assertEquals(1, result.size)
        assertEquals(alarm.copy(task = 1), result[0])
    }

    @Test
    fun getDefaultAlarms_relStartIncludedWhenHasStartDateAndDefaultEnabled() {
        // hideUntil divisible by 60000 = date only, no time
        val task = Task(id = 2, hideUntil = 120000L)
        val alarm = Alarm(type = TYPE_REL_START, time = 0)
        task.putTransitory(Task.TRANS_DEFAULT_ALARMS, listOf(alarm))
        val result = task.getDefaultAlarms(defaultRemindersEnabled = true)
        assertEquals(1, result.size)
        assertEquals(alarm.copy(task = 2), result[0])
    }

    @Test
    fun getDefaultAlarms_relStartExcludedWhenNoStartDate() {
        val task = Task(id = 3, hideUntil = 0)
        val alarm = Alarm(type = TYPE_REL_START, time = 0)
        task.putTransitory(Task.TRANS_DEFAULT_ALARMS, listOf(alarm))
        val result = task.getDefaultAlarms(defaultRemindersEnabled = true)
        assertTrue(result.isEmpty())
    }

    @Test
    fun getDefaultAlarms_relStartExcludedWhenNoStartTimeAndDefaultDisabled() {
        // hideUntil > 0 but divisible by 60000 (date only), defaultReminders=false
        val task = Task(id = 4, hideUntil = 60000L)
        val alarm = Alarm(type = TYPE_REL_START, time = 0)
        task.putTransitory(Task.TRANS_DEFAULT_ALARMS, listOf(alarm))
        val result = task.getDefaultAlarms(defaultRemindersEnabled = false)
        assertTrue(result.isEmpty())
    }

    @Test
    fun getDefaultAlarms_relEndIncludedWhenHasDueDateAndDueTime() {
        val task = Task(id = 5, dueDate = 1000001L)
        val alarm = Alarm(type = TYPE_REL_END, time = 0)
        task.putTransitory(Task.TRANS_DEFAULT_ALARMS, listOf(alarm))
        val result = task.getDefaultAlarms(defaultRemindersEnabled = false)
        assertEquals(1, result.size)
        assertEquals(alarm.copy(task = 5), result[0])
    }

    @Test
    fun getDefaultAlarms_relEndIncludedWhenHasDueDateAndDefaultEnabled() {
        val task = Task(id = 6, dueDate = 60000L)
        val alarm = Alarm(type = TYPE_REL_END, time = 0)
        task.putTransitory(Task.TRANS_DEFAULT_ALARMS, listOf(alarm))
        val result = task.getDefaultAlarms(defaultRemindersEnabled = true)
        assertEquals(1, result.size)
        assertEquals(alarm.copy(task = 6), result[0])
    }

    @Test
    fun getDefaultAlarms_relEndExcludedWhenNoDueDate() {
        val task = Task(id = 7, dueDate = 0)
        val alarm = Alarm(type = TYPE_REL_END, time = 0)
        task.putTransitory(Task.TRANS_DEFAULT_ALARMS, listOf(alarm))
        val result = task.getDefaultAlarms(defaultRemindersEnabled = true)
        assertTrue(result.isEmpty())
    }

    @Test
    fun getDefaultAlarms_relEndExcludedWhenNoDueTimeAndDefaultDisabled() {
        val task = Task(id = 8, dueDate = 60000L)
        val alarm = Alarm(type = TYPE_REL_END, time = 0)
        task.putTransitory(Task.TRANS_DEFAULT_ALARMS, listOf(alarm))
        val result = task.getDefaultAlarms(defaultRemindersEnabled = false)
        assertTrue(result.isEmpty())
    }

    @Test
    fun getDefaultAlarms_randomReminderAddedWhenPositive() {
        val task = Task(id = 9)
        task.randomReminder = 3600000L
        val result = task.getDefaultAlarms(defaultRemindersEnabled = false)
        assertEquals(1, result.size)
        assertEquals(Alarm(task = 9, time = 3600000L, type = TYPE_RANDOM), result[0])
    }

    @Test
    fun getDefaultAlarms_randomReminderNotAddedWhenZero() {
        val task = Task(id = 10)
        task.randomReminder = 0L
        val result = task.getDefaultAlarms(defaultRemindersEnabled = false)
        assertTrue(result.isEmpty())
    }

    @Test
    fun getDefaultAlarms_noTransitoryReturnsEmptyList() {
        val task = Task(id = 11)
        val result = task.getDefaultAlarms(defaultRemindersEnabled = true)
        assertTrue(result.isEmpty())
    }

    @Test
    fun getDefaultAlarms_mixedAlarmsFilteredCorrectly() {
        val task = Task(id = 12, dueDate = 1000001L, hideUntil = 0)
        task.randomReminder = 5000L
        val startAlarm = Alarm(type = TYPE_REL_START, time = 0)
        val endAlarm = Alarm(type = TYPE_REL_END, time = 0)
        task.putTransitory(Task.TRANS_DEFAULT_ALARMS, listOf(startAlarm, endAlarm))
        val result = task.getDefaultAlarms(defaultRemindersEnabled = false)
        // start excluded (no start date), end included, random included
        assertEquals(2, result.size)
        assertEquals(endAlarm.copy(task = 12), result[0])
        assertEquals(Alarm(task = 12, time = 5000L, type = TYPE_RANDOM), result[1])
    }

    @Test
    fun getDefaultAlarms_bothAlarmsIncludedWhenBothDatesWithTime() {
        val task = Task(id = 13, dueDate = 1000001L, hideUntil = 1000001L)
        val startAlarm = Alarm(type = TYPE_REL_START, time = 0)
        val endAlarm = Alarm(type = TYPE_REL_END, time = 0)
        task.putTransitory(Task.TRANS_DEFAULT_ALARMS, listOf(startAlarm, endAlarm))
        val result = task.getDefaultAlarms(defaultRemindersEnabled = false)
        assertEquals(2, result.size)
    }

    @Test
    fun getDefaultAlarms_alarmCopiedWithCorrectTaskId() {
        val task = Task(id = 99, hideUntil = 1000001L)
        val alarm = Alarm(type = TYPE_REL_START, time = 5000L)
        task.putTransitory(Task.TRANS_DEFAULT_ALARMS, listOf(alarm))
        val result = task.getDefaultAlarms(defaultRemindersEnabled = false)
        assertEquals(99L, result[0].task)
        assertEquals(5000L, result[0].time)
        assertEquals(TYPE_REL_START, result[0].type)
    }

    @Test
    fun getDefaultAlarms_preservesAlarmTimeInCopy() {
        val task = Task(id = 1, dueDate = 1000001L)
        val alarm = Alarm(type = TYPE_REL_END, time = 300000L)
        task.putTransitory(Task.TRANS_DEFAULT_ALARMS, listOf(alarm))
        val result = task.getDefaultAlarms(defaultRemindersEnabled = false)
        assertEquals(300000L, result[0].time)
    }
}
