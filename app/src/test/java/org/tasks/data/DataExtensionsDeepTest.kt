package org.tasks.data

import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.anyOrNull
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.tasks.broadcast.RefreshBroadcaster
import org.tasks.data.dao.TaskDao
import org.tasks.data.entity.Alarm
import org.tasks.data.entity.Alarm.Companion.TYPE_RANDOM
import org.tasks.data.entity.Alarm.Companion.TYPE_REL_END
import org.tasks.data.entity.Alarm.Companion.TYPE_REL_START
import org.tasks.data.entity.Geofence
import org.tasks.data.entity.Place
import org.tasks.data.entity.TagData
import org.tasks.data.entity.Task
import org.tasks.data.dao.LocationDao
import org.tasks.filters.PlaceFilter
import org.tasks.filters.TagFilter
import org.tasks.jobs.BackgroundWork
import org.tasks.location.LocationService
import org.tasks.notifications.Notifier
import org.tasks.preferences.AppPreferences
import org.tasks.sync.SyncAdapters
import org.tasks.sync.SyncSource
import com.todoroo.astrid.timers.TimerPlugin

class DataExtensionsDeepTest {

    // ========== TaskDefaults: getDefaultAlarms ==========

    @Test
    fun getDefaultAlarms_noTransitory_emptyList() {
        val task = Task(id = 5L)
        val result = task.getDefaultAlarms(false)
        assertTrue(result.isEmpty())
    }

    @Test
    fun getDefaultAlarms_relStart_withStartDate_noTime_defaultRemindersEnabled() {
        val task = Task(id = 5L, hideUntil = 1000L) // has start date
        task.putTransitory(
            Task.TRANS_DEFAULT_ALARMS,
            listOf(Alarm(task = 0, time = 0, type = TYPE_REL_START))
        )
        val result = task.getDefaultAlarms(true)
        assertEquals(1, result.size)
        assertEquals(TYPE_REL_START, result[0].type)
        assertEquals(5L, result[0].task)
    }

    @Test
    fun getDefaultAlarms_relStart_withStartDate_noTime_defaultRemindersDisabled() {
        // hideUntil must be > 0 but hideUntil % 60000 == 0 to have start date but no start time
        val task = Task(id = 5L, hideUntil = 60000L)
        task.putTransitory(
            Task.TRANS_DEFAULT_ALARMS,
            listOf(Alarm(task = 0, time = 0, type = TYPE_REL_START))
        )
        // No start time and defaultRemindersEnabled=false => alarm NOT added
        val result = task.getDefaultAlarms(false)
        assertTrue(result.isEmpty())
    }

    @Test
    fun getDefaultAlarms_relStart_noStartDate_notAdded() {
        val task = Task(id = 5L, hideUntil = 0L) // no start date
        task.putTransitory(
            Task.TRANS_DEFAULT_ALARMS,
            listOf(Alarm(task = 0, time = 0, type = TYPE_REL_START))
        )
        val result = task.getDefaultAlarms(true)
        assertTrue(result.isEmpty())
    }

    @Test
    fun getDefaultAlarms_relEnd_withDueDate_defaultRemindersEnabled() {
        val task = Task(id = 5L, dueDate = 2000L) // has due date
        task.putTransitory(
            Task.TRANS_DEFAULT_ALARMS,
            listOf(Alarm(task = 0, time = 0, type = TYPE_REL_END))
        )
        val result = task.getDefaultAlarms(true)
        assertEquals(1, result.size)
        assertEquals(TYPE_REL_END, result[0].type)
        assertEquals(5L, result[0].task)
    }

    @Test
    fun getDefaultAlarms_relEnd_withDueDate_defaultRemindersDisabled_noDueTime() {
        // dueDate must be > 0 but dueDate % 60000 == 0 to have due date but no due time
        val task = Task(id = 5L, dueDate = 120000L)
        task.putTransitory(
            Task.TRANS_DEFAULT_ALARMS,
            listOf(Alarm(task = 0, time = 0, type = TYPE_REL_END))
        )
        val result = task.getDefaultAlarms(false)
        // no due time and default reminders disabled => empty
        assertTrue(result.isEmpty())
    }

    @Test
    fun getDefaultAlarms_relEnd_noDueDate_notAdded() {
        val task = Task(id = 5L, dueDate = 0L)
        task.putTransitory(
            Task.TRANS_DEFAULT_ALARMS,
            listOf(Alarm(task = 0, time = 0, type = TYPE_REL_END))
        )
        val result = task.getDefaultAlarms(true)
        assertTrue(result.isEmpty())
    }

    @Test
    fun getDefaultAlarms_randomReminder() {
        val task = Task(id = 5L)
        task.randomReminder = 3600000L // 1 hour
        val result = task.getDefaultAlarms(false)
        assertEquals(1, result.size)
        assertEquals(TYPE_RANDOM, result[0].type)
        assertEquals(3600000L, result[0].time)
        assertEquals(5L, result[0].task)
    }

    @Test
    fun getDefaultAlarms_randomReminder_zero_notAdded() {
        val task = Task(id = 5L)
        task.randomReminder = 0L
        val result = task.getDefaultAlarms(false)
        assertTrue(result.isEmpty())
    }

    @Test
    fun getDefaultAlarms_combined_relStartAndRandom() {
        val task = Task(id = 5L, hideUntil = 1000L)
        task.randomReminder = 7200000L
        task.putTransitory(
            Task.TRANS_DEFAULT_ALARMS,
            listOf(Alarm(task = 0, time = 0, type = TYPE_REL_START))
        )
        val result = task.getDefaultAlarms(true)
        assertEquals(2, result.size)
        assertEquals(TYPE_REL_START, result[0].type)
        assertEquals(TYPE_RANDOM, result[1].type)
    }

    @Test
    fun getDefaultAlarms_combined_relStartAndRelEnd() {
        val task = Task(id = 5L, hideUntil = 1000L, dueDate = 2000L)
        task.putTransitory(
            Task.TRANS_DEFAULT_ALARMS,
            listOf(
                Alarm(task = 0, time = 0, type = TYPE_REL_START),
                Alarm(task = 0, time = 0, type = TYPE_REL_END),
            )
        )
        val result = task.getDefaultAlarms(true)
        assertEquals(2, result.size)
        assertEquals(TYPE_REL_START, result[0].type)
        assertEquals(TYPE_REL_END, result[1].type)
    }

    @Test
    fun getDefaultAlarms_unknownAlarmType_ignored() {
        val task = Task(id = 5L, hideUntil = 1000L)
        task.putTransitory(
            Task.TRANS_DEFAULT_ALARMS,
            listOf(Alarm(task = 0, time = 0, type = 99))
        )
        val result = task.getDefaultAlarms(true)
        assertTrue(result.isEmpty())
    }

    // ========== TagFiltersExtensions ==========

    @Test
    fun toTagFilter_basic() {
        val tagData = TagData(name = "Work", remoteId = "r-123")
        val tf = TagFilters(tagData = tagData, count = 10)
        val result = tf.toTagFilter()
        assertEquals("Work", result.tagData.name)
        assertEquals("r-123", result.tagData.remoteId)
        assertEquals(10, result.count)
    }

    @Test
    fun toTagFilter_zeroCount() {
        val tagData = TagData(name = "Empty")
        val tf = TagFilters(tagData = tagData, count = 0)
        val result = tf.toTagFilter()
        assertEquals(0, result.count)
    }

    @Test
    fun toTagFilter_largeCount() {
        val tagData = TagData(name = "Popular")
        val tf = TagFilters(tagData = tagData, count = 999999)
        val result = tf.toTagFilter()
        assertEquals(999999, result.count)
    }

    @Test
    fun toTagFilter_nullName() {
        val tagData = TagData(name = null)
        val tf = TagFilters(tagData = tagData, count = 1)
        val result = tf.toTagFilter()
        assertNull(result.tagData.name)
    }

    // ========== LocationFiltersExtensions ==========

    @Test
    fun toLocationFilter_basic() {
        val place = Place(uid = "p-1", name = "Office", latitude = 40.7, longitude = -74.0)
        val lf = LocationFilters(place = place, count = 5)
        val result = lf.toLocationFilter()
        assertEquals("Office", result.place.name)
        assertEquals("p-1", result.place.uid)
        assertEquals(5, result.count)
    }

    @Test
    fun toLocationFilter_zeroCount() {
        val place = Place(uid = "p-1", name = "Empty Place")
        val lf = LocationFilters(place = place, count = 0)
        val result = lf.toLocationFilter()
        assertEquals(0, result.count)
    }

    @Test
    fun toLocationFilter_preservesCoordinates() {
        val place = Place(uid = "p-1", name = "Pin", latitude = 51.5, longitude = -0.12)
        val lf = LocationFilters(place = place, count = 3)
        val result = lf.toLocationFilter()
        assertEquals(51.5, result.place.latitude, 0.001)
        assertEquals(-0.12, result.place.longitude, 0.001)
    }

    // ========== LocationDaoExtensions ==========

    @Test
    fun createGeofence_arrival() = runTest {
        val prefs = mock<AppPreferences>()
        whenever(prefs.defaultLocationReminder()).thenReturn(1)
        val g = createGeofence("uid", prefs)
        assertEquals("uid", g.place)
        assertTrue(g.isArrival)
        assertFalse(g.isDeparture)
    }

    @Test
    fun createGeofence_departure() = runTest {
        val prefs = mock<AppPreferences>()
        whenever(prefs.defaultLocationReminder()).thenReturn(2)
        val g = createGeofence("uid", prefs)
        assertFalse(g.isArrival)
        assertTrue(g.isDeparture)
    }

    @Test
    fun createGeofence_both() = runTest {
        val prefs = mock<AppPreferences>()
        whenever(prefs.defaultLocationReminder()).thenReturn(3)
        val g = createGeofence("uid", prefs)
        assertTrue(g.isArrival)
        assertTrue(g.isDeparture)
    }

    @Test
    fun createGeofence_none() = runTest {
        val prefs = mock<AppPreferences>()
        whenever(prefs.defaultLocationReminder()).thenReturn(0)
        val g = createGeofence("uid", prefs)
        assertFalse(g.isArrival)
        assertFalse(g.isDeparture)
    }

    @Test
    fun createGeofence_nullPlace() = runTest {
        val prefs = mock<AppPreferences>()
        whenever(prefs.defaultLocationReminder()).thenReturn(1)
        val g = createGeofence(null, prefs)
        assertNull(g.place)
    }

    @Test
    fun getLocation_newTask_withPlaceTransitory() = runTest {
        val locationDao = mock<LocationDao>()
        val prefs = mock<AppPreferences>()
        val task = Task(id = Task.NO_ID)
        task.putTransitory(Place.KEY, "place-uid")
        val place = Place(uid = "place-uid", name = "Home")
        whenever(locationDao.getPlace("place-uid")).thenReturn(place)
        whenever(prefs.defaultLocationReminder()).thenReturn(1)
        val result = locationDao.getLocation(task, prefs)
        assertNotNull(result)
        assertEquals("Home", result!!.place.name)
    }

    @Test
    fun getLocation_newTask_noTransitory() = runTest {
        val locationDao = mock<LocationDao>()
        val prefs = mock<AppPreferences>()
        val task = Task(id = Task.NO_ID)
        val result = locationDao.getLocation(task, prefs)
        assertNull(result)
    }

    @Test
    fun getLocation_newTask_placeNotFound() = runTest {
        val locationDao = mock<LocationDao>()
        val prefs = mock<AppPreferences>()
        val task = Task(id = Task.NO_ID)
        task.putTransitory(Place.KEY, "missing")
        whenever(locationDao.getPlace("missing")).thenReturn(null)
        val result = locationDao.getLocation(task, prefs)
        assertNull(result)
    }

    @Test
    fun getLocation_existingTask() = runTest {
        val locationDao = mock<LocationDao>()
        val prefs = mock<AppPreferences>()
        val task = Task(id = 42)
        val place = Place(uid = "p", name = "Work")
        val geofence = Geofence(task = 42, place = "p", isArrival = true)
        whenever(locationDao.getGeofences(42)).thenReturn(Location(geofence, place))
        val result = locationDao.getLocation(task, prefs)
        assertNotNull(result)
        assertEquals("Work", result!!.place.name)
    }

    @Test
    fun getLocation_existingTask_noGeofence() = runTest {
        val locationDao = mock<LocationDao>()
        val prefs = mock<AppPreferences>()
        val task = Task(id = 42)
        whenever(locationDao.getGeofences(42)).thenReturn(null)
        val result = locationDao.getLocation(task, prefs)
        assertNull(result)
    }

    // ========== TaskDaoExtensions: countCompletedSql ==========

    @Test
    fun countCompletedSql_replacesClause() = runTest {
        val taskDao = mock<TaskDao>()
        whenever(taskDao.count(any())).thenReturn(10)
        val result = taskDao.countCompletedSql("WHERE tasks.completed<=0 AND tasks.deleted<=0")
        assertEquals(10, result)
    }

    @Test
    fun countCompletedSql_noClause_returnsZero() = runTest {
        val taskDao = mock<TaskDao>()
        val result = taskDao.countCompletedSql("WHERE tasks.deleted<=0")
        assertEquals(0, result)
        verify(taskDao, never()).count(any())
    }

    @Test
    fun countCompletedSql_emptyString_returnsZero() = runTest {
        val taskDao = mock<TaskDao>()
        val result = taskDao.countCompletedSql("")
        assertEquals(0, result)
    }

    @Test
    fun countCompletedSql_multipleOccurrences() = runTest {
        val taskDao = mock<TaskDao>()
        whenever(taskDao.count(any())).thenReturn(7)
        val result = taskDao.countCompletedSql("WHERE tasks.completed<=0 AND (tasks.completed<=0)")
        assertEquals(7, result)
    }

    // ========== TaskSaver ==========

    @Test
    fun taskSaver_save_noUpdate_doesNotCallAfterSave() = runTest {
        val taskDao = mock<TaskDao>()
        val refreshBroadcaster = mock<RefreshBroadcaster>()
        val notifier = mock<Notifier>()
        val locationService = mock<LocationService>()
        val timerPlugin = mock<TimerPlugin>()
        val syncAdapters = mock<SyncAdapters>()
        val backgroundWork = mock<BackgroundWork>()
        whenever(taskDao.update(any<Task>(), anyOrNull())).thenReturn(false)
        val saver = TaskSaver(
            taskDao = taskDao,
            refreshBroadcaster = refreshBroadcaster,
            notifier = notifier,
            locationService = locationService,
            timerPlugin = timerPlugin,
            syncAdapters = syncAdapters,
            backgroundWork = backgroundWork,
        )
        val task = Task(id = 1)
        saver.save(task)
        verify(refreshBroadcaster, never()).broadcastRefresh()
        verify(syncAdapters, never()).sync(any<Task>(), any())
    }

    @Test
    fun taskSaver_save_withUpdate_callsAfterSave() = runTest {
        val taskDao = mock<TaskDao>()
        val refreshBroadcaster = mock<RefreshBroadcaster>()
        val notifier = mock<Notifier>()
        val locationService = mock<LocationService>()
        val timerPlugin = mock<TimerPlugin>()
        val syncAdapters = mock<SyncAdapters>()
        val backgroundWork = mock<BackgroundWork>()
        whenever(taskDao.update(any<Task>(), anyOrNull())).thenReturn(true)
        val saver = TaskSaver(
            taskDao = taskDao,
            refreshBroadcaster = refreshBroadcaster,
            notifier = notifier,
            locationService = locationService,
            timerPlugin = timerPlugin,
            syncAdapters = syncAdapters,
            backgroundWork = backgroundWork,
        )
        val task = Task(id = 1)
        saver.save(task)
        verify(syncAdapters).sync(any<Task>(), anyOrNull())
        verify(notifier).triggerNotifications()
        verify(backgroundWork).scheduleRefresh(any())
    }

    @Test
    fun taskSaver_afterSave_justCompleted_stopsTimer() = runTest {
        val taskDao = mock<TaskDao>()
        val refreshBroadcaster = mock<RefreshBroadcaster>()
        val notifier = mock<Notifier>()
        val locationService = mock<LocationService>()
        val timerPlugin = mock<TimerPlugin>()
        val syncAdapters = mock<SyncAdapters>()
        val backgroundWork = mock<BackgroundWork>()
        val saver = TaskSaver(
            taskDao = taskDao,
            refreshBroadcaster = refreshBroadcaster,
            notifier = notifier,
            locationService = locationService,
            timerPlugin = timerPlugin,
            syncAdapters = syncAdapters,
            backgroundWork = backgroundWork,
        )
        val task = Task(id = 1, completionDate = 1000L, timerStart = 500L)
        val original = Task(id = 1, completionDate = 0L, timerStart = 500L)
        saver.afterSave(task, original)
        verify(timerPlugin).stopTimer(task)
    }

    @Test
    fun taskSaver_afterSave_notJustCompleted_doesNotStopTimer() = runTest {
        val taskDao = mock<TaskDao>()
        val refreshBroadcaster = mock<RefreshBroadcaster>()
        val notifier = mock<Notifier>()
        val locationService = mock<LocationService>()
        val timerPlugin = mock<TimerPlugin>()
        val syncAdapters = mock<SyncAdapters>()
        val backgroundWork = mock<BackgroundWork>()
        val saver = TaskSaver(
            taskDao = taskDao,
            refreshBroadcaster = refreshBroadcaster,
            notifier = notifier,
            locationService = locationService,
            timerPlugin = timerPlugin,
            syncAdapters = syncAdapters,
            backgroundWork = backgroundWork,
        )
        val task = Task(id = 1, completionDate = 1000L, timerStart = 500L)
        val original = Task(id = 1, completionDate = 1000L, timerStart = 500L) // already completed
        saver.afterSave(task, original)
        verify(timerPlugin, never()).stopTimer(any())
    }

    @Test
    fun taskSaver_afterSave_completionChanged_updatesGeofences() = runTest {
        val taskDao = mock<TaskDao>()
        val refreshBroadcaster = mock<RefreshBroadcaster>()
        val notifier = mock<Notifier>()
        val locationService = mock<LocationService>()
        val timerPlugin = mock<TimerPlugin>()
        val syncAdapters = mock<SyncAdapters>()
        val backgroundWork = mock<BackgroundWork>()
        val saver = TaskSaver(
            taskDao = taskDao,
            refreshBroadcaster = refreshBroadcaster,
            notifier = notifier,
            locationService = locationService,
            timerPlugin = timerPlugin,
            syncAdapters = syncAdapters,
            backgroundWork = backgroundWork,
        )
        val task = Task(id = 1, completionDate = 1000L)
        val original = Task(id = 1, completionDate = 0L)
        saver.afterSave(task, original)
        verify(locationService).updateGeofences(1L)
    }

    @Test
    fun taskSaver_afterSave_deletionChanged_updatesGeofences() = runTest {
        val taskDao = mock<TaskDao>()
        val refreshBroadcaster = mock<RefreshBroadcaster>()
        val notifier = mock<Notifier>()
        val locationService = mock<LocationService>()
        val timerPlugin = mock<TimerPlugin>()
        val syncAdapters = mock<SyncAdapters>()
        val backgroundWork = mock<BackgroundWork>()
        val saver = TaskSaver(
            taskDao = taskDao,
            refreshBroadcaster = refreshBroadcaster,
            notifier = notifier,
            locationService = locationService,
            timerPlugin = timerPlugin,
            syncAdapters = syncAdapters,
            backgroundWork = backgroundWork,
        )
        val task = Task(id = 1, deletionDate = 1000L)
        val original = Task(id = 1, deletionDate = 0L)
        saver.afterSave(task, original)
        verify(locationService).updateGeofences(1L)
    }

    @Test
    fun taskSaver_afterSave_noCompletionOrDeletionChange_doesNotUpdateGeofences() = runTest {
        val taskDao = mock<TaskDao>()
        val refreshBroadcaster = mock<RefreshBroadcaster>()
        val notifier = mock<Notifier>()
        val locationService = mock<LocationService>()
        val timerPlugin = mock<TimerPlugin>()
        val syncAdapters = mock<SyncAdapters>()
        val backgroundWork = mock<BackgroundWork>()
        val saver = TaskSaver(
            taskDao = taskDao,
            refreshBroadcaster = refreshBroadcaster,
            notifier = notifier,
            locationService = locationService,
            timerPlugin = timerPlugin,
            syncAdapters = syncAdapters,
            backgroundWork = backgroundWork,
        )
        val task = Task(id = 1, title = "Updated")
        val original = Task(id = 1, title = "Original")
        saver.afterSave(task, original)
        verify(locationService, never()).updateGeofences(any<Long>())
    }

    @Test
    fun taskSaver_afterSave_suppressRefresh_doesNotBroadcast() = runTest {
        val taskDao = mock<TaskDao>()
        val refreshBroadcaster = mock<RefreshBroadcaster>()
        val notifier = mock<Notifier>()
        val locationService = mock<LocationService>()
        val timerPlugin = mock<TimerPlugin>()
        val syncAdapters = mock<SyncAdapters>()
        val backgroundWork = mock<BackgroundWork>()
        val saver = TaskSaver(
            taskDao = taskDao,
            refreshBroadcaster = refreshBroadcaster,
            notifier = notifier,
            locationService = locationService,
            timerPlugin = timerPlugin,
            syncAdapters = syncAdapters,
            backgroundWork = backgroundWork,
        )
        val task = Task(id = 1)
        task.suppressRefresh()
        saver.afterSave(task, null)
        verify(refreshBroadcaster, never()).broadcastRefresh()
    }

    @Test
    fun taskSaver_afterSave_calendarUri_updatesCalendar() = runTest {
        val taskDao = mock<TaskDao>()
        val refreshBroadcaster = mock<RefreshBroadcaster>()
        val notifier = mock<Notifier>()
        val locationService = mock<LocationService>()
        val timerPlugin = mock<TimerPlugin>()
        val syncAdapters = mock<SyncAdapters>()
        val backgroundWork = mock<BackgroundWork>()
        val saver = TaskSaver(
            taskDao = taskDao,
            refreshBroadcaster = refreshBroadcaster,
            notifier = notifier,
            locationService = locationService,
            timerPlugin = timerPlugin,
            syncAdapters = syncAdapters,
            backgroundWork = backgroundWork,
        )
        val task = Task(id = 1, calendarURI = "content://calendar/events/123")
        saver.afterSave(task, null)
        verify(backgroundWork).updateCalendar(task)
    }

    @Test
    fun taskSaver_afterSave_noCalendarUri_doesNotUpdateCalendar() = runTest {
        val taskDao = mock<TaskDao>()
        val refreshBroadcaster = mock<RefreshBroadcaster>()
        val notifier = mock<Notifier>()
        val locationService = mock<LocationService>()
        val timerPlugin = mock<TimerPlugin>()
        val syncAdapters = mock<SyncAdapters>()
        val backgroundWork = mock<BackgroundWork>()
        val saver = TaskSaver(
            taskDao = taskDao,
            refreshBroadcaster = refreshBroadcaster,
            notifier = notifier,
            locationService = locationService,
            timerPlugin = timerPlugin,
            syncAdapters = syncAdapters,
            backgroundWork = backgroundWork,
        )
        val task = Task(id = 1, calendarURI = null)
        saver.afterSave(task, null)
        verify(backgroundWork, never()).updateCalendar(any())
    }

    @Test
    fun taskSaver_touch_syncsAfterTouch() = runTest {
        val taskDao = mock<TaskDao>()
        val refreshBroadcaster = mock<RefreshBroadcaster>()
        val notifier = mock<Notifier>()
        val locationService = mock<LocationService>()
        val timerPlugin = mock<TimerPlugin>()
        val syncAdapters = mock<SyncAdapters>()
        val backgroundWork = mock<BackgroundWork>()
        val saver = TaskSaver(
            taskDao = taskDao,
            refreshBroadcaster = refreshBroadcaster,
            notifier = notifier,
            locationService = locationService,
            timerPlugin = timerPlugin,
            syncAdapters = syncAdapters,
            backgroundWork = backgroundWork,
        )
        saver.touch(listOf(1L, 2L))
        verify(taskDao).touch(any<List<Long>>(), any())
        verify(syncAdapters).sync(SyncSource.TASK_CHANGE)
    }

    @Test
    fun taskSaver_setCollapsed_syncsAndBroadcasts() = runTest {
        val taskDao = mock<TaskDao>()
        val refreshBroadcaster = mock<RefreshBroadcaster>()
        val notifier = mock<Notifier>()
        val locationService = mock<LocationService>()
        val timerPlugin = mock<TimerPlugin>()
        val syncAdapters = mock<SyncAdapters>()
        val backgroundWork = mock<BackgroundWork>()
        val saver = TaskSaver(
            taskDao = taskDao,
            refreshBroadcaster = refreshBroadcaster,
            notifier = notifier,
            locationService = locationService,
            timerPlugin = timerPlugin,
            syncAdapters = syncAdapters,
            backgroundWork = backgroundWork,
        )
        saver.setCollapsed(5L, true)
        verify(taskDao).setCollapsed(any(), any(), any())
        verify(syncAdapters).sync(SyncSource.TASK_CHANGE)
        verify(refreshBroadcaster).broadcastRefresh()
    }
}
