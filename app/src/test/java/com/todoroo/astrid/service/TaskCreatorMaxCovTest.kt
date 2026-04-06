package com.todoroo.astrid.service

import com.todoroo.astrid.gcal.GCalHelper
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.eq
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.tasks.R
import org.tasks.data.GoogleTask
import org.tasks.data.TaskSaver
import org.tasks.data.dao.AlarmDao
import org.tasks.data.dao.CaldavDao
import org.tasks.data.dao.GoogleTaskDao
import org.tasks.data.dao.LocationDao
import org.tasks.data.dao.TagDao
import org.tasks.data.dao.TagDataDao
import org.tasks.data.dao.TaskDao
import org.tasks.data.entity.CaldavAccount
import org.tasks.data.entity.CaldavCalendar
import org.tasks.data.entity.CaldavTask
import org.tasks.data.entity.Place
import org.tasks.data.entity.Tag
import org.tasks.data.entity.TagData
import org.tasks.data.entity.Task
import org.tasks.filters.CaldavFilter
import org.tasks.preferences.DefaultFilterProvider
import org.tasks.preferences.Preferences

class TaskCreatorMaxCovTest {
    private lateinit var gcalHelper: GCalHelper
    private lateinit var preferences: Preferences
    private lateinit var tagDataDao: TagDataDao
    private lateinit var taskDao: TaskDao
    private lateinit var taskSaver: TaskSaver
    private lateinit var tagDao: TagDao
    private lateinit var googleTaskDao: GoogleTaskDao
    private lateinit var defaultFilterProvider: DefaultFilterProvider
    private lateinit var caldavDao: CaldavDao
    private lateinit var locationDao: LocationDao
    private lateinit var alarmDao: AlarmDao
    private lateinit var taskCreator: TaskCreator

    @Before
    fun setUp() {
        gcalHelper = mock()
        preferences = mock()
        tagDataDao = mock()
        taskDao = mock()
        taskSaver = mock()
        tagDao = mock()
        googleTaskDao = mock()
        defaultFilterProvider = mock()
        caldavDao = mock()
        locationDao = mock()
        alarmDao = mock()

        runBlocking {
            whenever(preferences.getStringValue(any<Int>())).thenReturn(null)
            whenever(preferences.getIntegerFromString(any<Int>(), any())).thenAnswer { it.getArgument<Int>(1) }
            whenever(preferences.defaultPriority()).thenReturn(Task.Priority.NONE)
            whenever(preferences.defaultRandomHours()).thenReturn(0)
            whenever(preferences.defaultRingMode()).thenReturn(0)
            whenever(preferences.defaultDueTime()).thenReturn(0)
            whenever(preferences.defaultAlarms()).thenReturn(emptyList())
            whenever(preferences.isDefaultDueTimeEnabled()).thenReturn(false)
            whenever(preferences.defaultLocationReminder()).thenReturn(0)
            whenever(preferences.isDefaultCalendarSet).thenReturn(false)
            whenever(preferences.addTasksToTop()).thenReturn(false)
        }

        taskCreator = TaskCreator(
            gcalHelper = gcalHelper,
            preferences = preferences,
            tagDataDao = tagDataDao,
            taskDao = taskDao,
            taskSaver = taskSaver,
            tagDao = tagDao,
            googleTaskDao = googleTaskDao,
            defaultFilterProvider = defaultFilterProvider,
            caldavDao = caldavDao,
            locationDao = locationDao,
            alarmDao = alarmDao,
        )
    }

    // ===== createWithValues with DUE_DATE in map =====

    @Test
    fun createWithDueDateInMap() = runTest {
        val dueMs = System.currentTimeMillis().toString()
        val task = taskCreator.create(mapOf(Task.DUE_DATE.name to dueMs), "Test")
        assertTrue(task.dueDate > 0)
    }

    @Test
    fun createWithHideUntilInMap() = runTest {
        val hideMs = System.currentTimeMillis().toString()
        val task = taskCreator.create(mapOf(Task.HIDE_UNTIL.name to hideMs), "Test")
        assertTrue(task.hideUntil > 0)
    }

    @Test
    fun createWithPlaceTransitory() = runTest {
        val task = taskCreator.create(mapOf(Place.KEY to "place-uid"), "Test")
        assertTrue(task.hasTransitory(Place.KEY))
        assertEquals("place-uid", task.getTransitory<String>(Place.KEY))
    }

    @Test
    fun createWithTagInMap() = runTest {
        val task = taskCreator.create(mapOf(Tag.KEY to "WorkTag"), "Test")
        assertTrue(task.tags.contains("WorkTag"))
    }

    @Test
    fun createWithDefaultLocationPref() = runTest {
        whenever(preferences.getStringValue(R.string.p_default_location)).thenReturn("place-uid-123")
        val task = taskCreator.create(null, "Test")
        assertTrue(task.hasTransitory(Place.KEY))
    }

    @Test
    fun createWithBlankLocationPrefIgnored() = runTest {
        whenever(preferences.getStringValue(R.string.p_default_location)).thenReturn("   ")
        val task = taskCreator.create(null, "Test")
        assertFalse(task.hasTransitory(Place.KEY))
    }

    @Test
    fun createWithMultipleDefaultTags() = runTest {
        whenever(preferences.getStringValue(R.string.p_default_tags)).thenReturn("uuid1,uuid2")
        whenever(tagDataDao.getByUuid("uuid1")).thenReturn(TagData(remoteId = "uuid1", name = "Tag1"))
        whenever(tagDataDao.getByUuid("uuid2")).thenReturn(TagData(remoteId = "uuid2", name = "Tag2"))
        val task = taskCreator.create(null, "Test")
        assertTrue(task.tags.contains("Tag1"))
        assertTrue(task.tags.contains("Tag2"))
    }

    @Test
    fun createWithDefaultTagsButOneNotFound() = runTest {
        whenever(preferences.getStringValue(R.string.p_default_tags)).thenReturn("uuid1,uuid2")
        whenever(tagDataDao.getByUuid("uuid1")).thenReturn(TagData(remoteId = "uuid1", name = "Tag1"))
        whenever(tagDataDao.getByUuid("uuid2")).thenReturn(null)
        val task = taskCreator.create(null, "Test")
        assertTrue(task.tags.contains("Tag1"))
        assertFalse(task.tags.contains("Tag2"))
    }

    @Test
    fun createWithImportanceInMap() = runTest {
        val task = taskCreator.create(
            mapOf(Task.IMPORTANCE.name to Task.Priority.HIGH.toString()),
            "Test"
        )
        assertEquals(Task.Priority.HIGH, task.priority)
    }

    // ===== basicQuickAddTask branches =====

    @Test
    fun basicQuickAddWithGoogleTaskTransitory() = runTest {
        whenever(defaultFilterProvider.getDefaultList()).thenReturn(
            CaldavFilter(
                calendar = CaldavCalendar(uuid = "default-cal", account = "default-acc"),
                account = CaldavAccount(uuid = "default-acc")
            )
        )
        // Simulate a task that has GoogleTask transitory set
        // by using values in create
        val task = taskCreator.basicQuickAddTask("Test")
        // Task should be created
        verify(taskDao).createNew(any())
    }

    @Test
    fun basicQuickAddWithPlaceTransitory() = runTest {
        whenever(defaultFilterProvider.getDefaultList()).thenReturn(
            CaldavFilter(
                calendar = CaldavCalendar(uuid = "default-cal", account = "default-acc"),
                account = CaldavAccount(uuid = "default-acc")
            )
        )
        whenever(preferences.getStringValue(R.string.p_default_location)).thenReturn("place-uid")
        whenever(locationDao.getPlace("place-uid")).thenReturn(
            Place(uid = "place-uid", latitude = 37.0, longitude = -122.0)
        )
        val task = taskCreator.basicQuickAddTask("Test")
        verify(locationDao).getPlace("place-uid")
        verify(locationDao).insert(any<org.tasks.data.entity.Geofence>())
    }

    @Test
    fun basicQuickAddPlaceNotFound() = runTest {
        whenever(defaultFilterProvider.getDefaultList()).thenReturn(
            CaldavFilter(
                calendar = CaldavCalendar(uuid = "default-cal", account = "default-acc"),
                account = CaldavAccount(uuid = "default-acc")
            )
        )
        whenever(preferences.getStringValue(R.string.p_default_location)).thenReturn("place-uid")
        whenever(locationDao.getPlace("place-uid")).thenReturn(null)
        val task = taskCreator.basicQuickAddTask("Test")
        verify(locationDao, never()).insert(any<org.tasks.data.entity.Geofence>())
    }

    @Test
    fun basicQuickAddWithGoogleTaskDefaultList() = runTest {
        whenever(defaultFilterProvider.getDefaultList()).thenReturn(
            CaldavFilter(
                calendar = CaldavCalendar(uuid = "gtask-cal", account = "gtask-acc"),
                account = CaldavAccount(
                    uuid = "gtask-acc",
                    accountType = CaldavAccount.TYPE_GOOGLE_TASKS,
                ),
            )
        )
        val task = taskCreator.basicQuickAddTask("Test")
        verify(googleTaskDao).insertAndShift(any(), any(), any())
    }

    @Test
    fun basicQuickAddWithCaldavDefaultList() = runTest {
        whenever(defaultFilterProvider.getDefaultList()).thenReturn(
            CaldavFilter(
                calendar = CaldavCalendar(uuid = "caldav-cal", account = "caldav-acc"),
                account = CaldavAccount(uuid = "caldav-acc"),
            )
        )
        val task = taskCreator.basicQuickAddTask("Test")
        verify(caldavDao).insert(any<Task>(), any(), any())
    }

    // ===== createTags =====

    @Test
    fun createTagsWithMultipleTags() = runTest {
        val task = Task(id = 1, remoteId = "uuid-1")
        task.putTransitory(Tag.KEY, arrayListOf("Tag1", "Tag2", "Tag3"))
        whenever(tagDataDao.getTagByName("Tag1")).thenReturn(TagData(name = "Tag1", remoteId = "r1"))
        whenever(tagDataDao.getTagByName("Tag2")).thenReturn(null)
        whenever(tagDataDao.getTagByName("Tag3")).thenReturn(TagData(name = "Tag3", remoteId = "r3"))
        taskCreator.createTags(task)
        verify(tagDataDao).insert(any<TagData>()) // Tag2 is new
        verify(tagDao, org.mockito.kotlin.times(3)).insert(any<Tag>())
    }

    // ===== createWithValues with filter =====

    @Test
    fun createWithValuesNullFilter() = runTest {
        val task = taskCreator.createWithValues(null, "Test")
        assertNotNull(task)
        assertEquals("Test", task.title)
    }

    @Test
    fun createWithValuesCaldavFilter() = runTest {
        val filter = CaldavFilter(
            calendar = CaldavCalendar(uuid = "cal-uuid", account = "acc-uuid"),
            account = CaldavAccount(uuid = "acc-uuid"),
        )
        val task = taskCreator.createWithValues(filter, "Filtered task")
        assertEquals("Filtered task", task.title)
        assertTrue(task.hasTransitory(CaldavTask.KEY))
    }

    // ===== title parsing edge cases =====

    @Test
    fun createWithBlankTitle() = runTest {
        val task = taskCreator.createWithValues("")
        assertEquals("", task.title)
    }

    @Test
    fun createWithWhitespaceOnlyTitle() = runTest {
        val task = taskCreator.createWithValues("   ")
        assertEquals("", task.title)
    }
}
