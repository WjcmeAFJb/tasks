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
import org.mockito.Mockito
import org.mockito.Mockito.mock
import org.mockito.Mockito.never
import org.mockito.Mockito.verify
import org.mockito.Mockito.`when`
import org.tasks.R
import org.tasks.data.TaskSaver
import org.tasks.data.GoogleTask
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
import org.tasks.data.entity.Tag
import org.tasks.data.entity.TagData
import org.tasks.data.entity.Task
import org.tasks.filters.CaldavFilter
import org.tasks.preferences.DefaultFilterProvider
import org.tasks.preferences.Preferences

class TaskCreatorUnitTest {
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
        gcalHelper = mock(GCalHelper::class.java)
        preferences = mock(Preferences::class.java)
        tagDataDao = mock(TagDataDao::class.java)
        taskDao = mock(TaskDao::class.java)
        taskSaver = mock(TaskSaver::class.java)
        tagDao = mock(TagDao::class.java)
        googleTaskDao = mock(GoogleTaskDao::class.java)
        defaultFilterProvider = mock(DefaultFilterProvider::class.java)
        caldavDao = mock(CaldavDao::class.java)
        locationDao = mock(LocationDao::class.java)
        alarmDao = mock(AlarmDao::class.java)
        `when`(preferences.getStringValue(Mockito.anyInt())).thenReturn(null)
        `when`(preferences.getIntegerFromString(Mockito.anyInt(), Mockito.anyInt())).thenAnswer { it.getArgument<Int>(1) }
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
        taskCreator = TaskCreator(gcalHelper = gcalHelper, preferences = preferences, tagDataDao = tagDataDao, taskDao = taskDao, taskSaver = taskSaver, tagDao = tagDao, googleTaskDao = googleTaskDao, defaultFilterProvider = defaultFilterProvider, caldavDao = caldavDao, locationDao = locationDao, alarmDao = alarmDao)
    }

    @Test fun createWithValuesSetsTitle() = runTest { assertEquals("Buy groceries", taskCreator.createWithValues("Buy groceries").title) }
    @Test fun createWithValuesTrimsTitle() = runTest { assertEquals("Buy groceries", taskCreator.createWithValues("  Buy groceries  ").title) }
    @Test fun createWithValuesNullTitle() = runTest { assertNull(taskCreator.createWithValues(null as String?).title) }
    @Test fun createWithValuesSetsCreationDate() = runTest { assertTrue(taskCreator.createWithValues("Test").creationDate > 0) }
    @Test fun createWithValuesSetsModificationDate() = runTest { assertTrue(taskCreator.createWithValues("Test").modificationDate > 0) }
    @Test fun createWithValuesSetsRemoteId() = runTest { val t = taskCreator.createWithValues("Test"); assertNotNull(t.remoteId); assertTrue(t.remoteId!!.isNotEmpty()) }
    @Test fun createWithValuesDefaultPriority() = runTest { `when`(preferences.defaultPriority()).thenReturn(Task.Priority.MEDIUM); assertEquals(Task.Priority.MEDIUM, taskCreator.createWithValues("Test").priority) }
    @Test fun createWithValuesDefaultUrgencyNone() = runTest { `when`(preferences.getIntegerFromString(Mockito.anyInt(), Mockito.eq(Task.URGENCY_NONE))).thenReturn(Task.URGENCY_NONE); assertEquals(0L, taskCreator.createWithValues("Test").dueDate) }
    @Test fun createWithValuesDefaultHideUntilNone() = runTest { `when`(preferences.getIntegerFromString(Mockito.anyInt(), Mockito.eq(Task.HIDE_UNTIL_NONE))).thenReturn(Task.HIDE_UNTIL_NONE); assertEquals(0L, taskCreator.createWithValues("Test").hideUntil) }
    @Test fun createWithValuesDefaultRecurrence() = runTest { `when`(preferences.getStringValue(Mockito.anyInt())).thenReturn(null); `when`(preferences.getStringValue(R.string.p_default_recurrence)).thenReturn("FREQ=WEEKLY"); `when`(preferences.getIntegerFromString(R.string.p_default_recurrence_from, 0)).thenReturn(0); val t = taskCreator.createWithValues("Test"); assertEquals("FREQ=WEEKLY", t.recurrence); assertEquals(Task.RepeatFrom.DUE_DATE, t.repeatFrom) }
    @Test fun createWithValuesRecurrenceFromCompletion() = runTest { `when`(preferences.getStringValue(Mockito.anyInt())).thenReturn(null); `when`(preferences.getStringValue(R.string.p_default_recurrence)).thenReturn("FREQ=DAILY"); `when`(preferences.getIntegerFromString(R.string.p_default_recurrence_from, 0)).thenReturn(1); val t = taskCreator.createWithValues("Test"); assertEquals("FREQ=DAILY", t.recurrence); assertEquals(Task.RepeatFrom.COMPLETION_DATE, t.repeatFrom) }
    @Test fun createWithValuesNoRecurrenceWhenBlank() = runTest { `when`(preferences.getStringValue(R.string.p_default_recurrence)).thenReturn("   "); assertNull(taskCreator.createWithValues("Test").recurrence) }
    @Test fun createWithValuesDefaultTags() = runTest { `when`(preferences.getStringValue(R.string.p_default_tags)).thenReturn("tag-uuid-1"); `when`(tagDataDao.getByUuid("tag-uuid-1")).thenReturn(TagData(remoteId = "tag-uuid-1", name = "Work")); assertTrue(taskCreator.createWithValues("Test").tags.contains("Work")) }
    @Test fun createWithValuesNoDefaultTagsWhenPrefNull() = runTest { `when`(preferences.getStringValue(R.string.p_default_tags)).thenReturn(null); assertTrue(taskCreator.createWithValues("Test").tags.isEmpty()) }
    @Test fun basicQuickAddTaskCreatesTaskWithTitle() = runTest { setupDefaultListMock(); val t = taskCreator.basicQuickAddTask("Buy groceries"); assertEquals("Buy groceries", t.title); verify(taskDao).createNew(any()) }
    @Test fun basicQuickAddTaskTrimsTitle() = runTest { setupDefaultListMock(); assertEquals("Buy groceries", taskCreator.basicQuickAddTask("  Buy groceries  ").title) }
    @Test fun basicQuickAddTaskSavesTask() = runTest { setupDefaultListMock(); taskCreator.basicQuickAddTask("Test task"); verify(taskSaver).save(any(), Mockito.isNull()) }
    @Test fun basicQuickAddTaskInsertsCaldavTask() = runTest { `when`(defaultFilterProvider.getDefaultList()).thenReturn(CaldavFilter(calendar = CaldavCalendar(uuid = "cal-uuid", account = "acc-uuid"), account = CaldavAccount(uuid = "acc-uuid"))); taskCreator.basicQuickAddTask("Test task"); verify(caldavDao).insert(any(), any(), Mockito.anyBoolean()) }
    @Test fun basicQuickAddTaskInsertsAlarms() = runTest { setupDefaultListMock(); taskCreator.basicQuickAddTask("Test task"); verify(alarmDao).insert(any<List<org.tasks.data.entity.Alarm>>()) }
    @Test fun filterContextSetsCaldavList() = runTest { val t = taskCreator.createWithValues(CaldavFilter(calendar = CaldavCalendar(uuid = "cal-uuid-123", account = "acc-uuid"), account = CaldavAccount(uuid = "acc-uuid")), "Test"); assertTrue(t.hasTransitory(CaldavTask.KEY)); assertEquals("cal-uuid-123", t.getTransitory<String>(CaldavTask.KEY)) }
    @Test fun filterContextWithNullFilter() = runTest { assertFalse(taskCreator.createWithValues(null, "Test").hasTransitory(CaldavTask.KEY)) }
    @Test fun createTagsInsertsNewTag() = runTest { val t = Task(id = 1, remoteId = "uuid-1"); t.putTransitory(Tag.KEY, arrayListOf("NewTag")); `when`(tagDataDao.getTagByName("NewTag")).thenReturn(null); taskCreator.createTags(t); verify(tagDataDao).insert(any<TagData>()); verify(tagDao).insert(any<Tag>()) }
    @Test fun createTagsUsesExistingTagData() = runTest { val t = Task(id = 1, remoteId = "uuid-1"); t.putTransitory(Tag.KEY, arrayListOf("ExistingTag")); `when`(tagDataDao.getTagByName("ExistingTag")).thenReturn(TagData(name = "ExistingTag", remoteId = "r")); taskCreator.createTags(t); verify(tagDataDao, never()).insert(any<TagData>()); verify(tagDao).insert(any<Tag>()) }
    @Test fun createTagsMultipleTags() = runTest { val t = Task(id = 1, remoteId = "uuid-1"); t.putTransitory(Tag.KEY, arrayListOf("Tag1", "Tag2")); `when`(tagDataDao.getTagByName("Tag1")).thenReturn(TagData(name = "Tag1", remoteId = "r1")); `when`(tagDataDao.getTagByName("Tag2")).thenReturn(TagData(name = "Tag2", remoteId = "r2")); taskCreator.createTags(t); verify(tagDao, Mockito.times(2)).insert(any<Tag>()) }
    @Test fun createTagsNoTags() = runTest { taskCreator.createTags(Task(id = 1, remoteId = "uuid-1")); verify(tagDao, never()).insert(any<Tag>()) }
    @Test fun createWithValuesSetsImportanceFromMap() = runTest { assertEquals(1, taskCreator.create(mapOf(Task.IMPORTANCE.name to "1"), "Test").priority) }
    @Test fun createWithValuesSetsGoogleTaskTransitory() = runTest { val t = taskCreator.create(mapOf(GoogleTask.KEY to "list-id"), "Test"); assertTrue(t.hasTransitory(GoogleTask.KEY)); assertEquals("list-id", t.getTransitory<String>(GoogleTask.KEY)) }
    @Test fun createWithValuesSetsCaldavTransitory() = runTest { val t = taskCreator.create(mapOf(CaldavTask.KEY to "cal-id"), "Test"); assertTrue(t.hasTransitory(CaldavTask.KEY)); assertEquals("cal-id", t.getTransitory<String>(CaldavTask.KEY)) }
    private suspend fun setupDefaultListMock() { `when`(defaultFilterProvider.getDefaultList()).thenReturn(CaldavFilter(calendar = CaldavCalendar(uuid = "default-cal", account = "default-acc"), account = CaldavAccount(uuid = "default-acc"))) }
}
