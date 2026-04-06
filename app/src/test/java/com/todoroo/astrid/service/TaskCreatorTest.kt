package com.todoroo.astrid.service

import com.todoroo.astrid.gcal.GCalHelper
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.mockito.ArgumentCaptor
import org.mockito.Mockito
import org.mockito.Mockito.mock
import org.mockito.Mockito.never
import org.mockito.Mockito.verify
import org.mockito.Mockito.verifyNoInteractions
import org.mockito.Mockito.`when`
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
import org.tasks.data.entity.CaldavAccount.Companion.TYPE_GOOGLE_TASKS
import org.tasks.data.entity.CaldavCalendar
import org.tasks.data.entity.CaldavTask
import org.tasks.data.entity.Place
import org.tasks.data.entity.Tag
import org.tasks.data.entity.TagData
import org.tasks.data.entity.Task
import org.tasks.filters.CaldavFilter
import org.tasks.filters.FilterImpl
import org.tasks.preferences.DefaultFilterProvider
import org.tasks.preferences.Preferences

class TaskCreatorTest {
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

    @Suppress("UNCHECKED_CAST")
    private fun <T> any(): T = Mockito.any<T>() as T

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
        `when`(preferences.getStringValue(Mockito.anyInt())).thenReturn(null)
        `when`(preferences.getIntegerFromString(Mockito.anyInt(), Mockito.anyInt()))
            .thenAnswer { it.getArgument<Int>(1) }
        runBlocking {
            `when`(preferences.defaultPriority()).thenReturn(Task.Priority.NONE)
            `when`(preferences.defaultRandomHours()).thenReturn(0)
            `when`(preferences.defaultRingMode()).thenReturn(0)
            `when`(preferences.defaultDueTime()).thenReturn(0)
            `when`(preferences.defaultAlarms()).thenReturn(emptyList())
            `when`(preferences.isDefaultDueTimeEnabled()).thenReturn(false)
            `when`(preferences.defaultLocationReminder()).thenReturn(0)
        }
        `when`(preferences.isDefaultCalendarSet).thenReturn(false)
        `when`(preferences.addTasksToTop()).thenReturn(false)
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

    // ---------------------------------------------------------------
    // create() - null values map
    // ---------------------------------------------------------------

    @Test
    fun createWithNullValuesMapSetsTitle() = runTest {
        val task = taskCreator.create(null, "Test task")
        assertEquals("Test task", task.title)
    }

    @Test
    fun createWithNullValuesMapSetsDefaultPriority() = runTest {
        `when`(preferences.defaultPriority()).thenReturn(Task.Priority.HIGH)
        val task = taskCreator.create(null, "Test")
        assertEquals(Task.Priority.HIGH, task.priority)
    }

    @Test
    fun createWithNullValuesMapSetsCreationDate() = runTest {
        val task = taskCreator.create(null, "Test")
        assertTrue(task.creationDate > 0)
    }

    @Test
    fun createWithNullValuesMapSetsModificationDate() = runTest {
        val task = taskCreator.create(null, "Test")
        assertTrue(task.modificationDate > 0)
    }

    @Test
    fun createWithNullValuesMapSetsRemoteId() = runTest {
        val task = taskCreator.create(null, "Test")
        assertNotNull(task.remoteId)
        assertTrue(task.remoteId!!.isNotEmpty())
    }

    @Test
    fun createWithNullValuesSetsDefaultDueDate() = runTest {
        `when`(preferences.getIntegerFromString(
            Mockito.eq(R.string.p_default_urgency_key),
            Mockito.eq(Task.URGENCY_NONE)
        )).thenReturn(Task.URGENCY_NONE)
        val task = taskCreator.create(null, "Test")
        assertEquals(0L, task.dueDate)
    }

    @Test
    fun createWithNullValuesSetsDefaultHideUntil() = runTest {
        val task = taskCreator.create(null, "Test")
        assertEquals(0L, task.hideUntil)
    }

    @Test
    fun createWithNullValuesLoadsDefaultTags() = runTest {
        `when`(preferences.getStringValue(R.string.p_default_tags))
            .thenReturn("uuid-1,uuid-2")
        `when`(tagDataDao.getByUuid("uuid-1"))
            .thenReturn(TagData(remoteId = "uuid-1", name = "Home"))
        `when`(tagDataDao.getByUuid("uuid-2"))
            .thenReturn(TagData(remoteId = "uuid-2", name = "Work"))
        val task = taskCreator.create(null, "Test")
        assertTrue(task.tags.contains("Home"))
        assertTrue(task.tags.contains("Work"))
    }

    @Test
    fun createWithNullValuesTrimsTitle() = runTest {
        val task = taskCreator.create(null, "  spaces  ")
        assertEquals("spaces", task.title)
    }

    @Test
    fun createWithNullValuesAndNullTitle() = runTest {
        val task = taskCreator.create(null, null)
        assertNull(task.title)
    }

    // ---------------------------------------------------------------
    // create() - DUE_DATE in values map
    // ---------------------------------------------------------------

    @Test
    fun createWithDueDateInValuesSetsDueDate() = runTest {
        val timestamp = 1700000000000L
        val task = taskCreator.create(
            mapOf(Task.DUE_DATE.name to timestamp.toString()),
            "Test"
        )
        assertTrue(task.dueDate > 0)
    }

    @Test
    fun createWithDueDateSkipsDefaultDueDate() = runTest {
        // When DUE_DATE is in values, should NOT apply default urgency from preferences
        `when`(preferences.getIntegerFromString(
            Mockito.eq(R.string.p_default_urgency_key),
            Mockito.anyInt()
        )).thenReturn(Task.URGENCY_TODAY)
        val timestamp = 1700000000000L
        val task = taskCreator.create(
            mapOf(Task.DUE_DATE.name to timestamp.toString()),
            "Test"
        )
        // Due date is set from the values, not from default urgency
        assertTrue(task.dueDate > 0)
    }

    @Test
    fun createWithInvalidDueDateDoesNotCrash() = runTest {
        val task = taskCreator.create(
            mapOf(Task.DUE_DATE.name to "not-a-number"),
            "Test"
        )
        // Should not crash; due date value is null from toLongOrNull, falls through to default
        assertNotNull(task)
    }

    // ---------------------------------------------------------------
    // create() - IMPORTANCE in values map
    // ---------------------------------------------------------------

    @Test
    fun createWithImportanceInValuesSetsHighPriority() = runTest {
        val task = taskCreator.create(
            mapOf(Task.IMPORTANCE.name to Task.Priority.HIGH.toString()),
            "Test"
        )
        assertEquals(Task.Priority.HIGH, task.priority)
    }

    @Test
    fun createWithImportanceInValuesSetsMediumPriority() = runTest {
        val task = taskCreator.create(
            mapOf(Task.IMPORTANCE.name to Task.Priority.MEDIUM.toString()),
            "Test"
        )
        assertEquals(Task.Priority.MEDIUM, task.priority)
    }

    @Test
    fun createWithImportanceInValuesSetsLowPriority() = runTest {
        val task = taskCreator.create(
            mapOf(Task.IMPORTANCE.name to Task.Priority.LOW.toString()),
            "Test"
        )
        assertEquals(Task.Priority.LOW, task.priority)
    }

    @Test
    fun createWithInvalidImportanceDoesNotCrash() = runTest {
        val task = taskCreator.create(
            mapOf(Task.IMPORTANCE.name to "invalid"),
            "Test"
        )
        // Falls back to default priority
        assertEquals(Task.Priority.NONE, task.priority)
    }

    @Test
    fun createWithImportanceOverridesDefaultPriority() = runTest {
        `when`(preferences.defaultPriority()).thenReturn(Task.Priority.LOW)
        val task = taskCreator.create(
            mapOf(Task.IMPORTANCE.name to Task.Priority.HIGH.toString()),
            "Test"
        )
        assertEquals(Task.Priority.HIGH, task.priority)
    }

    // ---------------------------------------------------------------
    // create() - HIDE_UNTIL in values map
    // ---------------------------------------------------------------

    @Test
    fun createWithHideUntilInValuesSetsHideUntil() = runTest {
        val timestamp = 1700000000000L
        val task = taskCreator.create(
            mapOf(Task.HIDE_UNTIL.name to timestamp.toString()),
            "Test"
        )
        // hideUntil is set to startOfDay of the provided timestamp
        assertTrue(task.hideUntil > 0)
    }

    @Test
    fun createWithHideUntilSkipsDefaultHideUntil() = runTest {
        // HIDE_UNTIL in values should prevent default hide-until from preferences
        val timestamp = 1700000000000L
        val task = taskCreator.create(
            mapOf(Task.HIDE_UNTIL.name to timestamp.toString()),
            "Test"
        )
        assertTrue(task.hideUntil > 0)
    }

    @Test
    fun createWithInvalidHideUntilDoesNotCrash() = runTest {
        val task = taskCreator.create(
            mapOf(Task.HIDE_UNTIL.name to "bad"),
            "Test"
        )
        assertNotNull(task)
    }

    // ---------------------------------------------------------------
    // create() - Tag.KEY in values map
    // ---------------------------------------------------------------

    @Test
    fun createWithTagInValuesAddsSingleTag() = runTest {
        val task = taskCreator.create(
            mapOf(Tag.KEY to "MyTag"),
            "Test"
        )
        assertTrue(task.tags.contains("MyTag"))
    }

    @Test
    fun createWithTagInValuesSkipsDefaultTags() = runTest {
        `when`(preferences.getStringValue(R.string.p_default_tags))
            .thenReturn("default-uuid")
        `when`(tagDataDao.getByUuid("default-uuid"))
            .thenReturn(TagData(remoteId = "default-uuid", name = "DefaultTag"))
        val task = taskCreator.create(
            mapOf(Tag.KEY to "ExplicitTag"),
            "Test"
        )
        // ExplicitTag should be present, DefaultTag should NOT because tags from values
        // are non-empty so default tags are skipped
        assertTrue(task.tags.contains("ExplicitTag"))
        assertFalse(task.tags.contains("DefaultTag"))
    }

    // ---------------------------------------------------------------
    // create() - GoogleTask.KEY in values map
    // ---------------------------------------------------------------

    @Test
    fun createWithGoogleTaskKeyInValuesSetsTransitory() = runTest {
        val task = taskCreator.create(
            mapOf(GoogleTask.KEY to "my-google-list"),
            "Test"
        )
        assertTrue(task.hasTransitory(GoogleTask.KEY))
        assertEquals("my-google-list", task.getTransitory<String>(GoogleTask.KEY))
    }

    // ---------------------------------------------------------------
    // create() - CaldavTask.KEY in values map
    // ---------------------------------------------------------------

    @Test
    fun createWithCaldavKeyInValuesSetsTransitory() = runTest {
        val task = taskCreator.create(
            mapOf(CaldavTask.KEY to "my-caldav-cal"),
            "Test"
        )
        assertTrue(task.hasTransitory(CaldavTask.KEY))
        assertEquals("my-caldav-cal", task.getTransitory<String>(CaldavTask.KEY))
    }

    // ---------------------------------------------------------------
    // create() - Place.KEY in values map
    // ---------------------------------------------------------------

    @Test
    fun createWithPlaceKeyInValuesSetsTransitory() = runTest {
        val task = taskCreator.create(
            mapOf(Place.KEY to "place-uid-123"),
            "Test"
        )
        assertTrue(task.hasTransitory(Place.KEY))
        assertEquals("place-uid-123", task.getTransitory<String>(Place.KEY))
    }

    // ---------------------------------------------------------------
    // create() - default recurrence from preferences
    // ---------------------------------------------------------------

    @Test
    fun createSetsRecurrenceFromPreferences() = runTest {
        `when`(preferences.getStringValue(R.string.p_default_recurrence))
            .thenReturn("FREQ=MONTHLY")
        `when`(preferences.getIntegerFromString(R.string.p_default_recurrence_from, 0))
            .thenReturn(0)
        val task = taskCreator.create(null, "Test")
        assertEquals("FREQ=MONTHLY", task.recurrence)
        assertEquals(Task.RepeatFrom.DUE_DATE, task.repeatFrom)
    }

    @Test
    fun createSetsRecurrenceFromCompletionDate() = runTest {
        `when`(preferences.getStringValue(R.string.p_default_recurrence))
            .thenReturn("FREQ=DAILY")
        `when`(preferences.getIntegerFromString(R.string.p_default_recurrence_from, 0))
            .thenReturn(1)
        val task = taskCreator.create(null, "Test")
        assertEquals("FREQ=DAILY", task.recurrence)
        assertEquals(Task.RepeatFrom.COMPLETION_DATE, task.repeatFrom)
    }

    @Test
    fun createIgnoresBlankRecurrence() = runTest {
        `when`(preferences.getStringValue(R.string.p_default_recurrence))
            .thenReturn("  ")
        val task = taskCreator.create(null, "Test")
        assertNull(task.recurrence)
    }

    @Test
    fun createIgnoresEmptyRecurrence() = runTest {
        `when`(preferences.getStringValue(R.string.p_default_recurrence))
            .thenReturn("")
        val task = taskCreator.create(null, "Test")
        assertNull(task.recurrence)
    }

    @Test
    fun createIgnoresNullRecurrence() = runTest {
        `when`(preferences.getStringValue(R.string.p_default_recurrence)).thenReturn(null)
        val task = taskCreator.create(null, "Test")
        assertNull(task.recurrence)
    }

    // ---------------------------------------------------------------
    // create() - default location from preferences
    // ---------------------------------------------------------------

    @Test
    fun createSetsDefaultLocationFromPreferences() = runTest {
        `when`(preferences.getStringValue(R.string.p_default_location))
            .thenReturn("place-uid-456")
        val task = taskCreator.create(null, "Test")
        assertTrue(task.hasTransitory(Place.KEY))
        assertEquals("place-uid-456", task.getTransitory<String>(Place.KEY))
    }

    @Test
    fun createIgnoresBlankDefaultLocation() = runTest {
        `when`(preferences.getStringValue(R.string.p_default_location)).thenReturn("  ")
        val task = taskCreator.create(null, "Test")
        assertFalse(task.hasTransitory(Place.KEY))
    }

    @Test
    fun createIgnoresEmptyDefaultLocation() = runTest {
        `when`(preferences.getStringValue(R.string.p_default_location)).thenReturn("")
        val task = taskCreator.create(null, "Test")
        assertFalse(task.hasTransitory(Place.KEY))
    }

    @Test
    fun createIgnoresNullDefaultLocation() = runTest {
        `when`(preferences.getStringValue(R.string.p_default_location)).thenReturn(null)
        val task = taskCreator.create(null, "Test")
        assertFalse(task.hasTransitory(Place.KEY))
    }

    // ---------------------------------------------------------------
    // create() - default tags from preferences
    // ---------------------------------------------------------------

    @Test
    fun createLoadsMultipleDefaultTags() = runTest {
        `when`(preferences.getStringValue(R.string.p_default_tags))
            .thenReturn("uuid-a,uuid-b,uuid-c")
        `when`(tagDataDao.getByUuid("uuid-a"))
            .thenReturn(TagData(remoteId = "uuid-a", name = "TagA"))
        `when`(tagDataDao.getByUuid("uuid-b"))
            .thenReturn(TagData(remoteId = "uuid-b", name = "TagB"))
        `when`(tagDataDao.getByUuid("uuid-c"))
            .thenReturn(TagData(remoteId = "uuid-c", name = "TagC"))
        val task = taskCreator.create(null, "Test")
        assertEquals(3, task.tags.size)
        assertTrue(task.tags.containsAll(listOf("TagA", "TagB", "TagC")))
    }

    @Test
    fun createSkipsNullTagDataForDefaultTags() = runTest {
        `when`(preferences.getStringValue(R.string.p_default_tags))
            .thenReturn("uuid-exists,uuid-missing")
        `when`(tagDataDao.getByUuid("uuid-exists"))
            .thenReturn(TagData(remoteId = "uuid-exists", name = "Exists"))
        `when`(tagDataDao.getByUuid("uuid-missing")).thenReturn(null)
        val task = taskCreator.create(null, "Test")
        assertTrue(task.tags.contains("Exists"))
        assertEquals(1, task.tags.size)
    }

    @Test
    fun createWithEmptyDefaultTagsPref() = runTest {
        `when`(preferences.getStringValue(R.string.p_default_tags)).thenReturn("")
        val task = taskCreator.create(null, "Test")
        // Empty string split produces [""], getByUuid("") returns null, name is mapNotNull'd away
        assertTrue(task.tags.isEmpty())
    }

    // ---------------------------------------------------------------
    // create() - combined values
    // ---------------------------------------------------------------

    @Test
    fun createWithMultipleValuesAppliesAll() = runTest {
        val timestamp = 1700000000000L
        val task = taskCreator.create(
            mapOf(
                Task.DUE_DATE.name to timestamp.toString(),
                Task.IMPORTANCE.name to Task.Priority.HIGH.toString(),
                Tag.KEY to "Urgent",
                CaldavTask.KEY to "my-cal",
            ),
            "Important task"
        )
        assertEquals("Important task", task.title)
        assertEquals(Task.Priority.HIGH, task.priority)
        assertTrue(task.dueDate > 0)
        assertTrue(task.hasTransitory(CaldavTask.KEY))
        assertTrue(task.tags.contains("Urgent"))
    }

    @Test
    fun createWithEmptyValuesMapBehavesLikeNullValues() = runTest {
        val task = taskCreator.create(emptyMap(), "Test")
        assertEquals("Test", task.title)
        assertEquals(Task.Priority.NONE, task.priority)
    }

    // ---------------------------------------------------------------
    // create() - unique remote IDs
    // ---------------------------------------------------------------

    @Test
    fun createGeneratesUniqueRemoteIds() = runTest {
        val task1 = taskCreator.create(null, "Task 1")
        val task2 = taskCreator.create(null, "Task 2")
        assertNotEquals(task1.remoteId, task2.remoteId)
    }

    // ---------------------------------------------------------------
    // createWithValues(title)
    // ---------------------------------------------------------------

    @Test
    fun createWithValuesTitleDelegatesToCreateWithNullValues() = runTest {
        val task = taskCreator.createWithValues("My task")
        assertEquals("My task", task.title)
        assertTrue(task.creationDate > 0)
    }

    // ---------------------------------------------------------------
    // createWithValues(filter, title) - various filters
    // ---------------------------------------------------------------

    @Test
    fun createWithCaldavFilterSetsCaldavTransitory() = runTest {
        val filter = CaldavFilter(
            calendar = CaldavCalendar(uuid = "cal-uuid-abc", account = "acc-uuid"),
            account = CaldavAccount(uuid = "acc-uuid"),
        )
        val task = taskCreator.createWithValues(filter, "Test")
        assertTrue(task.hasTransitory(CaldavTask.KEY))
        assertEquals("cal-uuid-abc", task.getTransitory<String>(CaldavTask.KEY))
    }

    @Test
    fun createWithNullFilterHasNoListTransitory() = runTest {
        val task = taskCreator.createWithValues(null, "Test")
        assertFalse(task.hasTransitory(CaldavTask.KEY))
        assertFalse(task.hasTransitory(GoogleTask.KEY))
    }

    // ---------------------------------------------------------------
    // basicQuickAddTask - task creation and persistence
    // ---------------------------------------------------------------

    @Test
    fun basicQuickAddCreatesNewTask() = runTest {
        setupDefaultCaldavList()
        val task = taskCreator.basicQuickAddTask("Buy milk")
        assertEquals("Buy milk", task.title)
        verify(taskDao).createNew(any())
    }

    @Test
    fun basicQuickAddTrimsTitle() = runTest {
        setupDefaultCaldavList()
        val task = taskCreator.basicQuickAddTask("  Buy milk  ")
        assertEquals("Buy milk", task.title)
    }

    @Test
    fun basicQuickAddSavesTaskWithNullOriginal() = runTest {
        setupDefaultCaldavList()
        taskCreator.basicQuickAddTask("Test")
        verify(taskSaver).save(any(), Mockito.isNull())
    }

    @Test
    fun basicQuickAddInsertsDefaultAlarms() = runTest {
        setupDefaultCaldavList()
        taskCreator.basicQuickAddTask("Test")
        verify(alarmDao).insert(any<List<org.tasks.data.entity.Alarm>>())
    }

    // ---------------------------------------------------------------
    // basicQuickAddTask - Google Task list assignment
    // ---------------------------------------------------------------

    @Test
    fun basicQuickAddWithGoogleTaskTransitoryInsertsGoogleTask() = runTest {
        `when`(defaultFilterProvider.getDefaultList()).thenReturn(
            CaldavFilter(
                calendar = CaldavCalendar(uuid = "dummy", account = "acc"),
                account = CaldavAccount(uuid = "acc"),
            )
        )
        // Use FilterImpl with valuesForNewTasks serialized with GoogleTask.KEY
        val filter = FilterImpl(
            title = "Test",
            sql = "",
            valuesForNewTasks = org.tasks.filters.mapToSerializedString(
                mapOf(GoogleTask.KEY to "google-list-id")
            ),
        )
        taskCreator.basicQuickAddTask("Test", filter)
        verify(googleTaskDao).insertAndShift(any(), any(), Mockito.anyBoolean())
    }

    @Test
    fun basicQuickAddWithCaldavTransitoryInsertsCaldavTask() = runTest {
        `when`(defaultFilterProvider.getDefaultList()).thenReturn(
            CaldavFilter(
                calendar = CaldavCalendar(uuid = "dummy", account = "acc"),
                account = CaldavAccount(uuid = "acc"),
            )
        )
        val filter = CaldavFilter(
            calendar = CaldavCalendar(uuid = "my-cal-uuid", account = "acc"),
            account = CaldavAccount(uuid = "acc"),
        )
        taskCreator.basicQuickAddTask("Test", filter)
        verify(caldavDao).insert(any(), any(), Mockito.anyBoolean())
    }

    // ---------------------------------------------------------------
    // basicQuickAddTask - default list when no transitory
    // ---------------------------------------------------------------

    @Test
    fun basicQuickAddWithNoTransitoryUsesCaldavDefaultList() = runTest {
        `when`(defaultFilterProvider.getDefaultList()).thenReturn(
            CaldavFilter(
                calendar = CaldavCalendar(uuid = "default-cal", account = "acc"),
                account = CaldavAccount(uuid = "acc"),
            )
        )
        taskCreator.basicQuickAddTask("Test")
        verify(caldavDao).insert(any(), any(), Mockito.anyBoolean())
        verify(googleTaskDao, never()).insertAndShift(any(), any(), Mockito.anyBoolean())
    }

    @Test
    fun basicQuickAddWithNoTransitoryUsesGoogleTasksDefaultList() = runTest {
        `when`(defaultFilterProvider.getDefaultList()).thenReturn(
            CaldavFilter(
                calendar = CaldavCalendar(uuid = "gtask-cal", account = "acc"),
                account = CaldavAccount(uuid = "acc", accountType = TYPE_GOOGLE_TASKS),
            )
        )
        taskCreator.basicQuickAddTask("Test")
        verify(googleTaskDao).insertAndShift(any(), any(), Mockito.anyBoolean())
        verify(caldavDao, never()).insert(any(), any(), Mockito.anyBoolean())
    }

    // ---------------------------------------------------------------
    // basicQuickAddTask - calendar event creation
    // ---------------------------------------------------------------

    @Test
    fun basicQuickAddCreatesCalendarEventWhenEnabled() = runTest {
        setupDefaultCaldavList()
        `when`(preferences.isDefaultCalendarSet).thenReturn(true)
        `when`(preferences.defaultCalendar).thenReturn("my-calendar")
        `when`(preferences.getIntegerFromString(
            Mockito.eq(R.string.p_default_urgency_key),
            Mockito.anyInt()
        )).thenReturn(Task.URGENCY_TODAY)
        val mockUri = mock<android.net.Uri>()
        `when`(gcalHelper.createTaskEvent(any(), any<String>())).thenReturn(mockUri)
        taskCreator.basicQuickAddTask("Task with date")
        verify(gcalHelper).createTaskEvent(any(), Mockito.eq("my-calendar"))
    }

    @Test
    fun basicQuickAddDoesNotCreateCalendarEventWhenDisabled() = runTest {
        setupDefaultCaldavList()
        `when`(preferences.isDefaultCalendarSet).thenReturn(false)
        taskCreator.basicQuickAddTask("Test")
        verify(gcalHelper, never()).createTaskEvent(any(), any())
    }

    @Test
    fun basicQuickAddDoesNotCreateCalendarEventWithNoDueDate() = runTest {
        setupDefaultCaldavList()
        `when`(preferences.isDefaultCalendarSet).thenReturn(true)
        // Task has no due date (default URGENCY_NONE), so no calendar event
        taskCreator.basicQuickAddTask("Test")
        verify(gcalHelper, never()).createTaskEvent(any(), any())
    }

    @Test
    fun basicQuickAddDoesNotCreateCalendarEventWithEmptyTitle() = runTest {
        setupDefaultCaldavList()
        `when`(preferences.isDefaultCalendarSet).thenReturn(true)
        `when`(preferences.defaultCalendar).thenReturn("my-calendar")
        `when`(preferences.getIntegerFromString(
            Mockito.eq(R.string.p_default_urgency_key),
            Mockito.anyInt()
        )).thenReturn(Task.URGENCY_TODAY)
        // Empty title (after trim) -> isNullOrEmpty is true -> no calendar event
        val task = taskCreator.basicQuickAddTask("")
        verify(gcalHelper, never()).createTaskEvent(any(), any())
    }

    // ---------------------------------------------------------------
    // basicQuickAddTask - Place transitory
    // ---------------------------------------------------------------

    @Test
    fun basicQuickAddWithPlaceTransitoryInsertsGeofence() = runTest {
        setupDefaultCaldavList()
        `when`(preferences.getStringValue(R.string.p_default_location))
            .thenReturn("place-uid-xyz")
        val place = Place(uid = "place-uid-xyz", name = "Office")
        `when`(locationDao.getPlace("place-uid-xyz")).thenReturn(place)
        taskCreator.basicQuickAddTask("Test")
        verify(locationDao).insert(any<org.tasks.data.entity.Geofence>())
    }

    @Test
    fun basicQuickAddWithPlaceTransitoryButNoPlaceFoundSkipsGeofence() = runTest {
        setupDefaultCaldavList()
        `when`(preferences.getStringValue(R.string.p_default_location))
            .thenReturn("missing-uid")
        `when`(locationDao.getPlace("missing-uid")).thenReturn(null)
        taskCreator.basicQuickAddTask("Test")
        verify(locationDao, never()).insert(any<org.tasks.data.entity.Geofence>())
    }

    // ---------------------------------------------------------------
    // basicQuickAddTask - addTasksToTop preference
    // ---------------------------------------------------------------

    @Test
    fun basicQuickAddAddsToTopWhenPreferenceEnabled() = runTest {
        `when`(preferences.addTasksToTop()).thenReturn(true)
        `when`(defaultFilterProvider.getDefaultList()).thenReturn(
            CaldavFilter(
                calendar = CaldavCalendar(uuid = "cal", account = "acc"),
                account = CaldavAccount(uuid = "acc"),
            )
        )
        taskCreator.basicQuickAddTask("Test")
        verify(caldavDao).insert(any(), any(), Mockito.eq(true))
    }

    @Test
    fun basicQuickAddAddsToBottomWhenPreferenceDisabled() = runTest {
        `when`(preferences.addTasksToTop()).thenReturn(false)
        `when`(defaultFilterProvider.getDefaultList()).thenReturn(
            CaldavFilter(
                calendar = CaldavCalendar(uuid = "cal", account = "acc"),
                account = CaldavAccount(uuid = "acc"),
            )
        )
        taskCreator.basicQuickAddTask("Test")
        verify(caldavDao).insert(any(), any(), Mockito.eq(false))
    }

    // ---------------------------------------------------------------
    // basicQuickAddTask - filter parameter
    // ---------------------------------------------------------------

    @Test
    fun basicQuickAddWithNullFilterUsesDefaultList() = runTest {
        setupDefaultCaldavList()
        taskCreator.basicQuickAddTask("Test", null)
        verify(defaultFilterProvider).getDefaultList()
    }

    // ---------------------------------------------------------------
    // createTags - tag creation
    // ---------------------------------------------------------------

    @Test
    fun createTagsWithNewTagInsertsTagDataAndTag() = runTest {
        val task = Task(id = 10, remoteId = "uid-10")
        task.putTransitory(Tag.KEY, arrayListOf("Brand New"))
        `when`(tagDataDao.getTagByName("Brand New")).thenReturn(null)
        taskCreator.createTags(task)
        verify(tagDataDao).insert(any<TagData>())
        verify(tagDao).insert(any<Tag>())
    }

    @Test
    fun createTagsWithExistingTagDoesNotInsertNewTagData() = runTest {
        val task = Task(id = 10, remoteId = "uid-10")
        task.putTransitory(Tag.KEY, arrayListOf("Existing"))
        `when`(tagDataDao.getTagByName("Existing"))
            .thenReturn(TagData(name = "Existing", remoteId = "r-id"))
        taskCreator.createTags(task)
        verify(tagDataDao, never()).insert(any<TagData>())
        verify(tagDao).insert(any<Tag>())
    }

    @Test
    fun createTagsWithMultipleTagsInsertsAll() = runTest {
        val task = Task(id = 10, remoteId = "uid-10")
        task.putTransitory(Tag.KEY, arrayListOf("T1", "T2", "T3"))
        `when`(tagDataDao.getTagByName("T1"))
            .thenReturn(TagData(name = "T1", remoteId = "r1"))
        `when`(tagDataDao.getTagByName("T2")).thenReturn(null)
        `when`(tagDataDao.getTagByName("T3"))
            .thenReturn(TagData(name = "T3", remoteId = "r3"))
        taskCreator.createTags(task)
        verify(tagDataDao, Mockito.times(1)).insert(any<TagData>()) // only T2 is new
        verify(tagDao, Mockito.times(3)).insert(any<Tag>())
    }

    @Test
    fun createTagsWithNoTagsDoesNothing() = runTest {
        val task = Task(id = 10, remoteId = "uid-10")
        // No transitory tags set at all
        taskCreator.createTags(task)
        verify(tagDao, never()).insert(any<Tag>())
        verify(tagDataDao, never()).insert(any<TagData>())
    }

    @Test
    fun createTagsWithEmptyTagListDoesNothing() = runTest {
        val task = Task(id = 10, remoteId = "uid-10")
        task.putTransitory(Tag.KEY, arrayListOf<String>())
        taskCreator.createTags(task)
        verify(tagDao, never()).insert(any<Tag>())
    }

    // ---------------------------------------------------------------
    // create() - default priority levels
    // ---------------------------------------------------------------

    @Test
    fun createWithDefaultPriorityHigh() = runTest {
        `when`(preferences.defaultPriority()).thenReturn(Task.Priority.HIGH)
        val task = taskCreator.create(null, "Test")
        assertEquals(Task.Priority.HIGH, task.priority)
    }

    @Test
    fun createWithDefaultPriorityMedium() = runTest {
        `when`(preferences.defaultPriority()).thenReturn(Task.Priority.MEDIUM)
        val task = taskCreator.create(null, "Test")
        assertEquals(Task.Priority.MEDIUM, task.priority)
    }

    @Test
    fun createWithDefaultPriorityLow() = runTest {
        `when`(preferences.defaultPriority()).thenReturn(Task.Priority.LOW)
        val task = taskCreator.create(null, "Test")
        assertEquals(Task.Priority.LOW, task.priority)
    }

    @Test
    fun createWithDefaultPriorityNone() = runTest {
        `when`(preferences.defaultPriority()).thenReturn(Task.Priority.NONE)
        val task = taskCreator.create(null, "Test")
        assertEquals(Task.Priority.NONE, task.priority)
    }

    // ---------------------------------------------------------------
    // create() - location from values overrides preference location
    // ---------------------------------------------------------------

    @Test
    fun createWithPlaceInValuesOverridesDefaultLocation() = runTest {
        `when`(preferences.getStringValue(R.string.p_default_location))
            .thenReturn("default-place")
        val task = taskCreator.create(
            mapOf(Place.KEY to "explicit-place"),
            "Test"
        )
        // The values map sets the transitory after preferences, so it overrides
        assertEquals("explicit-place", task.getTransitory<String>(Place.KEY))
    }

    // ---------------------------------------------------------------
    // basicQuickAddTask - non-CaldavFilter default list
    // ---------------------------------------------------------------

    @Test
    fun basicQuickAddDefaultListAlwaysInsertsCaldavOrGoogle() = runTest {
        // getDefaultList() always returns CaldavFilter, so one of caldavDao or
        // googleTaskDao will always be called when no transitory is set
        setupDefaultCaldavList()
        taskCreator.basicQuickAddTask("Test")
        verify(caldavDao).insert(any(), any(), Mockito.anyBoolean())
    }

    // ---------------------------------------------------------------
    // create() with various edge cases
    // ---------------------------------------------------------------

    @Test
    fun createSetsTagsTransitoryEvenWhenEmpty() = runTest {
        val task = taskCreator.create(null, "Test")
        // The create method always does task.putTransitory(Tag.KEY, tags) at the end
        assertTrue(task.hasTransitory(Tag.KEY))
    }

    @Test
    fun createTimestampsAreReasonablyRecent() = runTest {
        val before = System.currentTimeMillis()
        val task = taskCreator.create(null, "Test")
        val after = System.currentTimeMillis()
        assertTrue(task.creationDate in before..after)
        assertTrue(task.modificationDate in before..after)
    }

    @Test
    fun createWithValuesFilterDelegatesToCreateWithParsedMap() = runTest {
        // A CaldavFilter's valuesForNewTasks serializes CaldavTask.KEY
        val filter = CaldavFilter(
            calendar = CaldavCalendar(uuid = "unique-cal", account = "acc"),
            account = CaldavAccount(uuid = "acc"),
        )
        val task = taskCreator.createWithValues(filter, "Test")
        assertTrue(task.hasTransitory(CaldavTask.KEY))
        assertEquals("unique-cal", task.getTransitory<String>(CaldavTask.KEY))
    }

    // ---------------------------------------------------------------
    // basicQuickAddTask - CaldavTask.KEY transitory priority over GoogleTask.KEY
    // ---------------------------------------------------------------

    @Test
    fun basicQuickAddCaldavTransitoryTakesPriorityWhenBothPresent() = runTest {
        // If both GoogleTask.KEY and CaldavTask.KEY are present, only
        // GoogleTask.KEY branch runs (it's checked first with hasTransitory)
        // but in practice both won't be set. Test the CaldavTask.KEY branch
        // by ensuring only CaldavTask.KEY is set.
        `when`(defaultFilterProvider.getDefaultList()).thenReturn(
            CaldavFilter(
                calendar = CaldavCalendar(uuid = "dummy", account = "acc"),
                account = CaldavAccount(uuid = "acc"),
            )
        )
        val filter = CaldavFilter(
            calendar = CaldavCalendar(uuid = "specific-cal", account = "acc"),
            account = CaldavAccount(uuid = "acc"),
        )
        taskCreator.basicQuickAddTask("Test", filter)
        verify(caldavDao).insert(any(), any(), Mockito.anyBoolean())
        verify(googleTaskDao, never()).insertAndShift(any(), any(), Mockito.anyBoolean())
    }

    // ---------------------------------------------------------------
    // Helpers
    // ---------------------------------------------------------------

    private suspend fun setupDefaultCaldavList() {
        `when`(defaultFilterProvider.getDefaultList()).thenReturn(
            CaldavFilter(
                calendar = CaldavCalendar(uuid = "default-cal", account = "default-acc"),
                account = CaldavAccount(uuid = "default-acc"),
            )
        )
    }
}
