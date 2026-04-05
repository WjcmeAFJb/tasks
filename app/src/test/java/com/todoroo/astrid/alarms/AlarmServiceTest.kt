package com.todoroo.astrid.alarms

import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.mockito.Mockito
import org.mockito.Mockito.mock
import org.mockito.Mockito.never
import org.mockito.Mockito.verify
import org.mockito.Mockito.`when`
import org.tasks.broadcast.RefreshBroadcaster
import org.tasks.data.dao.AlarmDao
import org.tasks.data.dao.TaskDao
import org.tasks.data.entity.Alarm
import org.tasks.data.entity.Alarm.Companion.TYPE_DATE_TIME
import org.tasks.data.entity.Alarm.Companion.TYPE_REL_END
import org.tasks.data.entity.Alarm.Companion.TYPE_REL_START
import org.tasks.data.entity.Alarm.Companion.TYPE_SNOOZE
import org.tasks.notifications.Notifier
import org.tasks.preferences.AppPreferences

class AlarmServiceTest {

    private lateinit var alarmDao: AlarmDao
    private lateinit var taskDao: TaskDao
    private lateinit var refreshBroadcaster: RefreshBroadcaster
    private lateinit var notifier: Notifier
    private lateinit var alarmCalculator: AlarmCalculator
    private lateinit var preferences: AppPreferences
    private lateinit var alarmService: AlarmService

    @Suppress("UNCHECKED_CAST")
    private fun <T> anyNonNull(): T = Mockito.any<T>() as T

    @Before
    fun setUp() {
        alarmDao = mock()
        taskDao = mock()
        refreshBroadcaster = mock()
        notifier = mock()
        alarmCalculator = mock()
        preferences = mock()
        alarmService = AlarmService(
            alarmDao = alarmDao,
            taskDao = taskDao,
            refreshBroadcaster = refreshBroadcaster,
            notifier = notifier,
            alarmCalculator = alarmCalculator,
            preferences = preferences,
        )
    }

    @Test
    fun getAlarmsReturnsAlarmsFromDao() = runTest {
        val alarms = listOf(
            Alarm(id = 1, task = 5, type = TYPE_DATE_TIME, time = 1000),
            Alarm(id = 2, task = 5, type = TYPE_REL_END, time = 2000),
        )
        `when`(alarmDao.getAlarms(5L)).thenReturn(alarms)
        val result = alarmService.getAlarms(5L)
        assertEquals(alarms, result)
    }

    @Test
    fun getAlarmsReturnsEmptyListWhenNoAlarms() = runTest {
        `when`(alarmDao.getAlarms(5L)).thenReturn(emptyList())
        val result = alarmService.getAlarms(5L)
        assertTrue(result.isEmpty())
    }

    @Test
    fun synchronizeAlarmsDeletesRemovedAlarms() = runTest {
        val existing = Alarm(id = 1, task = 5, type = TYPE_DATE_TIME, time = 1000)
        `when`(alarmDao.getAlarms(5L)).thenReturn(listOf(existing))
        val result = alarmService.synchronizeAlarms(5L, mutableSetOf())
        verify(alarmDao).delete(existing)
        assertTrue(result)
    }

    @Test
    fun synchronizeAlarmsInsertsNewAlarms() = runTest {
        `when`(alarmDao.getAlarms(5L)).thenReturn(emptyList())
        val newAlarm = Alarm(type = TYPE_REL_START, time = 500)
        val result = alarmService.synchronizeAlarms(5L, mutableSetOf(newAlarm))
        verify(alarmDao).insert(listOf(newAlarm.copy(task = 5)))
        assertTrue(result)
    }

    @Test
    fun synchronizeAlarmsKeepsMatchingAlarms() = runTest {
        val existing = Alarm(id = 1, task = 5, type = TYPE_DATE_TIME, time = 1000)
        val matching = Alarm(type = TYPE_DATE_TIME, time = 1000)
        `when`(alarmDao.getAlarms(5L)).thenReturn(listOf(existing))
        val result = alarmService.synchronizeAlarms(5L, mutableSetOf(matching))
        verify(alarmDao, never()).delete(anyNonNull<Alarm>())
        assertFalse(result)
    }

    @Test
    fun synchronizeAlarmsBroadcastsRefreshWhenChanged() = runTest {
        `when`(alarmDao.getAlarms(5L)).thenReturn(emptyList())
        val newAlarm = Alarm(type = TYPE_DATE_TIME, time = 1000)
        alarmService.synchronizeAlarms(5L, mutableSetOf(newAlarm))
        verify(refreshBroadcaster).broadcastRefresh()
    }

    @Test
    fun synchronizeAlarmsDoesNotBroadcastRefreshWhenUnchanged() = runTest {
        val existing = Alarm(id = 1, task = 5, type = TYPE_DATE_TIME, time = 1000)
        val matching = Alarm(type = TYPE_DATE_TIME, time = 1000)
        `when`(alarmDao.getAlarms(5L)).thenReturn(listOf(existing))
        alarmService.synchronizeAlarms(5L, mutableSetOf(matching))
        verify(refreshBroadcaster, never()).broadcastRefresh()
    }

    @Test
    fun synchronizeAlarmsReturnsFalseWhenNothingChanged() = runTest {
        `when`(alarmDao.getAlarms(5L)).thenReturn(emptyList())
        val result = alarmService.synchronizeAlarms(5L, mutableSetOf())
        assertFalse(result)
    }

    @Test
    fun synchronizeAlarmsDeletesOldAndInsertsNew() = runTest {
        val existing = Alarm(id = 1, task = 5, type = TYPE_DATE_TIME, time = 1000)
        val newAlarm = Alarm(type = TYPE_REL_END, time = 2000)
        `when`(alarmDao.getAlarms(5L)).thenReturn(listOf(existing))
        val result = alarmService.synchronizeAlarms(5L, mutableSetOf(newAlarm))
        verify(alarmDao).delete(existing)
        verify(alarmDao).insert(listOf(newAlarm.copy(task = 5)))
        assertTrue(result)
    }

    @Test
    fun synchronizeAlarmsWithMultipleExistingAndNew() = runTest {
        val existing1 = Alarm(id = 1, task = 5, type = TYPE_DATE_TIME, time = 1000)
        val existing2 = Alarm(id = 2, task = 5, type = TYPE_REL_START, time = 500)
        val keep = Alarm(type = TYPE_DATE_TIME, time = 1000)
        val add = Alarm(type = TYPE_REL_END, time = 3000)
        `when`(alarmDao.getAlarms(5L)).thenReturn(listOf(existing1, existing2))
        val result = alarmService.synchronizeAlarms(5L, mutableSetOf(keep, add))
        verify(alarmDao).delete(existing2)
        verify(alarmDao, never()).delete(existing1)
        assertTrue(result)
    }

    @Test
    fun snoozeCancelsNotifications() = runTest {
        val taskIds = listOf(1L, 2L, 3L)
        alarmService.snooze(5000L, taskIds)
        verify(notifier).cancel(taskIds)
    }

    @Test
    fun snoozeDeletesExistingSnoozed() = runTest {
        val taskIds = listOf(1L, 2L)
        alarmService.snooze(5000L, taskIds)
        verify(alarmDao).deleteSnoozed(taskIds)
    }

    @Test
    fun snoozeInsertsNewSnoozeAlarms() = runTest {
        val taskIds = listOf(10L, 20L)
        alarmService.snooze(5000L, taskIds)
        verify(alarmDao).insert(
            listOf(
                Alarm(task = 10, time = 5000, type = TYPE_SNOOZE),
                Alarm(task = 20, time = 5000, type = TYPE_SNOOZE),
            )
        )
    }

    @Test
    fun snoozeTouchesTasksOnDao() = runTest {
        alarmService.snooze(5000L, listOf(1L))
        verify(taskDao).touch(Mockito.anyList(), Mockito.anyLong())
    }

    @Test
    fun snoozeTriggersNotifications() = runTest {
        val taskIds = listOf(1L)
        alarmService.snooze(5000L, taskIds)
        verify(notifier).triggerNotifications()
    }

    @Test
    fun snoozeWithEmptyListStillExecutes() = runTest {
        alarmService.snooze(5000L, emptyList())
        verify(notifier).cancel(emptyList<Long>())
        verify(alarmDao).deleteSnoozed(emptyList())
        verify(alarmDao).insert(emptyList<Alarm>())
    }
}
