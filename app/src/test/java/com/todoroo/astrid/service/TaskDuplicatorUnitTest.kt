package com.todoroo.astrid.service

import com.todoroo.astrid.gcal.GCalHelper
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.mockito.ArgumentCaptor
import org.mockito.Mockito
import org.mockito.Mockito.mock
import org.mockito.Mockito.never
import org.mockito.Mockito.verify
import org.mockito.Mockito.`when`
import org.tasks.broadcast.RefreshBroadcaster
import org.tasks.data.TaskSaver
import org.tasks.data.dao.AlarmDao
import org.tasks.data.dao.CaldavDao
import org.tasks.data.dao.GoogleTaskDao
import org.tasks.data.dao.LocationDao
import org.tasks.data.dao.TagDao
import org.tasks.data.dao.TagDataDao
import org.tasks.data.dao.TaskAttachmentDao
import org.tasks.data.dao.TaskDao
import org.tasks.data.entity.Alarm
import org.tasks.data.entity.Attachment
import org.tasks.data.entity.CaldavTask
import org.tasks.data.entity.Geofence
import org.tasks.data.entity.Tag
import org.tasks.data.entity.TagData
import org.tasks.data.entity.Task
import org.tasks.preferences.Preferences

class TaskDuplicatorUnitTest {
    private lateinit var gcalHelper: GCalHelper
    private lateinit var taskDao: TaskDao
    private lateinit var taskSaver: TaskSaver
    private lateinit var refreshBroadcaster: RefreshBroadcaster
    private lateinit var tagDao: TagDao
    private lateinit var tagDataDao: TagDataDao
    private lateinit var googleTaskDao: GoogleTaskDao
    private lateinit var caldavDao: CaldavDao
    private lateinit var locationDao: LocationDao
    private lateinit var alarmDao: AlarmDao
    private lateinit var preferences: Preferences
    private lateinit var taskAttachmentDao: TaskAttachmentDao
    private lateinit var taskDuplicator: TaskDuplicator
    @Suppress("UNCHECKED_CAST") private fun <T> any(): T = Mockito.any<T>() as T
    @Suppress("UNCHECKED_CAST") private fun <T> capture(captor: ArgumentCaptor<T>): T = captor.capture() as T
    @Before fun setUp() {
        gcalHelper = mock(GCalHelper::class.java); taskDao = mock(TaskDao::class.java); taskSaver = mock(TaskSaver::class.java); refreshBroadcaster = mock(RefreshBroadcaster::class.java); tagDao = mock(TagDao::class.java); tagDataDao = mock(TagDataDao::class.java); googleTaskDao = mock(GoogleTaskDao::class.java); caldavDao = mock(CaldavDao::class.java); locationDao = mock(LocationDao::class.java); alarmDao = mock(AlarmDao::class.java); preferences = mock(Preferences::class.java); taskAttachmentDao = mock(TaskAttachmentDao::class.java)
        taskDuplicator = TaskDuplicator(gcalHelper = gcalHelper, taskDao = taskDao, taskSaver = taskSaver, refreshBroadcaster = refreshBroadcaster, tagDao = tagDao, tagDataDao = tagDataDao, googleTaskDao = googleTaskDao, caldavDao = caldavDao, locationDao = locationDao, alarmDao = alarmDao, preferences = preferences, taskAttachmentDao = taskAttachmentDao)
    }
    private suspend fun setupSingleTask(task: Task, newId: Long = 100L) {
        `when`(taskDao.getChildren(listOf(task.id))).thenReturn(emptyList()); `when`(taskDao.fetch(listOf(task.id))).thenReturn(listOf(task)); `when`(taskDao.createNew(any())).thenReturn(newId); `when`(tagDataDao.getTagDataForTask(task.id)).thenReturn(emptyList()); `when`(googleTaskDao.getByTaskId(task.id)).thenReturn(null); `when`(caldavDao.getTask(task.id)).thenReturn(null); `when`(locationDao.getGeofencesForTask(task.id)).thenReturn(emptyList()); `when`(alarmDao.getAlarms(task.id)).thenReturn(emptyList()); `when`(taskAttachmentDao.getAttachmentsForTask(task.id)).thenReturn(emptyList()); `when`(taskDao.getChildren(task.id)).thenReturn(emptyList()); `when`(taskDao.getChildren(newId)).thenReturn(emptyList()); `when`(taskDao.fetch(emptyList())).thenReturn(emptyList())
    }
    @Test fun duplicatedTaskHasClearedCompletionDate() = runTest { val t = Task(id = 1, title = "Done", completionDate = 1000L, remoteId = "u1"); setupSingleTask(t); assertEquals(0L, taskDuplicator.duplicate(listOf(1L))[0].completionDate) }
    @Test fun duplicatedCompletedTaskIsNotCompleted() = runTest { val t = Task(id = 1, title = "Done", completionDate = System.currentTimeMillis(), remoteId = "u1"); setupSingleTask(t); assertEquals(false, taskDuplicator.duplicate(listOf(1L))[0].isCompleted) }
    @Test fun duplicatedTaskHasNoUUID() = runTest { val t = Task(id = 1, title = "T", remoteId = "orig-uuid"); setupSingleTask(t); assertEquals(Task.NO_UUID, taskDuplicator.duplicate(listOf(1L))[0].remoteId) }
    @Test fun duplicatedTaskUuidDiffersFromOriginal() = runTest { val t = Task(id = 1, title = "T", remoteId = "orig-uuid"); setupSingleTask(t); assertNotEquals("orig-uuid", taskDuplicator.duplicate(listOf(1L))[0].remoteId) }
    @Test fun duplicatedTaskCreatedViaDao() = runTest { val t = Task(id = 42, title = "T", remoteId = "u1"); setupSingleTask(t); taskDuplicator.duplicate(listOf(42L)); verify(taskDao).createNew(any()) }
    @Test fun duplicatedTaskPreservesTitle() = runTest { val t = Task(id = 1, title = "Important", remoteId = "u1"); setupSingleTask(t); assertEquals("Important", taskDuplicator.duplicate(listOf(1L))[0].title) }
    @Test fun duplicatedTaskClearsReminderLast() = runTest { val t = Task(id = 1, title = "T", reminderLast = 999L, remoteId = "u1"); setupSingleTask(t); assertEquals(0L, taskDuplicator.duplicate(listOf(1L))[0].reminderLast) }
    @Test fun duplicatedTaskClearsCalendarURI() = runTest { val t = Task(id = 1, title = "T", calendarURI = "content://cal/1", remoteId = "u1"); setupSingleTask(t); assertEquals("", taskDuplicator.duplicate(listOf(1L))[0].calendarURI) }
    @Test fun tagsCopiedForDuplicatedTask() = runTest { val t = Task(id = 1, title = "T", remoteId = "u1"); setupSingleTask(t); `when`(tagDataDao.getTagDataForTask(1L)).thenReturn(listOf(TagData(id = 10, name = "Work", remoteId = "r1"), TagData(id = 11, name = "Urgent", remoteId = "r2"))); taskDuplicator.duplicate(listOf(1L)); @Suppress("UNCHECKED_CAST") val c = ArgumentCaptor.forClass(List::class.java) as ArgumentCaptor<List<Tag>>; verify(tagDao).insert(capture(c)); assertEquals(2, c.value.size); assertEquals("Work", c.value[0].name); assertEquals("Urgent", c.value[1].name) }
    @Test fun noTagsInsertedWhenOriginalHasNone() = runTest { val t = Task(id = 1, title = "T", remoteId = "u1"); setupSingleTask(t); taskDuplicator.duplicate(listOf(1L)); verify(tagDao, never()).insert(any<List<Tag>>()) }
    @Test fun caldavTaskCreatedForDuplicatedTask() = runTest { val t = Task(id = 1, title = "T", remoteId = "u1"); setupSingleTask(t, newId = 200L); `when`(caldavDao.getTask(1L)).thenReturn(CaldavTask(id = 5, task = 1, calendar = "cal-abc")); taskDuplicator.duplicate(listOf(1L)); verify(caldavDao).insert(any(), any(), Mockito.anyBoolean()) }
    @Test fun googleTaskCreatedForDuplicatedGoogleTask() = runTest { val t = Task(id = 1, title = "T", remoteId = "u1"); setupSingleTask(t, newId = 200L); `when`(googleTaskDao.getByTaskId(1L)).thenReturn(CaldavTask(id = 5, task = 1, calendar = "gt-list", remoteId = "gt-r")); taskDuplicator.duplicate(listOf(1L)); verify(googleTaskDao).insertAndShift(any(), any(), Mockito.anyBoolean()) }
    @Test fun noCaldavOrGoogleTaskWhenOriginalHasNeither() = runTest { val t = Task(id = 1, title = "T", remoteId = "u1"); setupSingleTask(t); taskDuplicator.duplicate(listOf(1L)); verify(caldavDao, never()).insert(any(), any(), Mockito.anyBoolean()); verify(googleTaskDao, never()).insertAndShift(any(), any(), Mockito.anyBoolean()) }
    @Test fun geofencesCopiedForDuplicatedTask() = runTest { val t = Task(id = 1, title = "T", remoteId = "u1"); setupSingleTask(t, newId = 200L); `when`(locationDao.getGeofencesForTask(1L)).thenReturn(listOf(Geofence(task = 1, place = "p1", isArrival = true, isDeparture = false))); taskDuplicator.duplicate(listOf(1L)); verify(locationDao).insert(any<Geofence>()) }
    @Test fun alarmsCopiedForDuplicatedTask() = runTest { val t = Task(id = 1, title = "T", remoteId = "u1"); setupSingleTask(t, newId = 200L); `when`(alarmDao.getAlarms(1L)).thenReturn(listOf(Alarm(id = 10, task = 1, time = 5000L))); taskDuplicator.duplicate(listOf(1L)); @Suppress("UNCHECKED_CAST") val c = ArgumentCaptor.forClass(List::class.java) as ArgumentCaptor<List<Alarm>>; verify(alarmDao).insert(capture(c)); assertEquals(1, c.value.size); assertEquals(0L, c.value[0].id); assertEquals(5000L, c.value[0].time) }
    @Test fun noAlarmsInsertedWhenOriginalHasNone() = runTest { val t = Task(id = 1, title = "T", remoteId = "u1"); setupSingleTask(t); taskDuplicator.duplicate(listOf(1L)); verify(alarmDao, never()).insert(any<List<Alarm>>()) }
    @Test fun attachmentsCopiedForDuplicatedTask() = runTest { val t = Task(id = 1, title = "T", remoteId = "u1"); setupSingleTask(t, newId = 200L); `when`(taskAttachmentDao.getAttachmentsForTask(1L)).thenReturn(listOf(Attachment(task = 1, fileId = 42, attachmentUid = "att-1"))); taskDuplicator.duplicate(listOf(1L)); @Suppress("UNCHECKED_CAST") val c = ArgumentCaptor.forClass(List::class.java) as ArgumentCaptor<List<Attachment>>; verify(taskAttachmentDao).insert(capture(c)); assertEquals("att-1", c.value[0].attachmentUid); assertEquals(42L, c.value[0].fileId) }
    @Test fun readOnlyTasksNotDuplicated() = runTest { val t = Task(id = 1, title = "RO", readOnly = true, remoteId = "u1"); `when`(taskDao.getChildren(listOf(1L))).thenReturn(emptyList()); `when`(taskDao.fetch(listOf(1L))).thenReturn(listOf(t)); assertTrue(taskDuplicator.duplicate(listOf(1L)).isEmpty()); verify(taskDao, never()).createNew(any()) }
    @Test fun duplicatedTaskIsSaved() = runTest { val t = Task(id = 1, title = "T", remoteId = "u1"); setupSingleTask(t); taskDuplicator.duplicate(listOf(1L)); verify(taskSaver).save(any(), Mockito.isNull()) }
    @Test fun broadcastRefreshCalledAfterDuplicate() = runTest { val t = Task(id = 1, title = "T", remoteId = "u1"); setupSingleTask(t); taskDuplicator.duplicate(listOf(1L)); verify(refreshBroadcaster).broadcastRefresh() }
    @Test fun duplicatedTaskHasNewCreationDate() = runTest { val t = Task(id = 1, title = "T", creationDate = 1000L, remoteId = "u1"); setupSingleTask(t); assertTrue(taskDuplicator.duplicate(listOf(1L))[0].creationDate > 1000L) }
    @Test fun duplicatedTaskHasNewModificationDate() = runTest { val t = Task(id = 1, title = "T", modificationDate = 1000L, remoteId = "u1"); setupSingleTask(t); assertTrue(taskDuplicator.duplicate(listOf(1L))[0].modificationDate > 1000L) }
    @Test fun duplicatedTaskPreservesPriority() = runTest { val t = Task(id = 1, title = "T", priority = Task.Priority.HIGH, remoteId = "u1"); setupSingleTask(t); assertEquals(Task.Priority.HIGH, taskDuplicator.duplicate(listOf(1L))[0].priority) }
    @Test fun duplicatedTaskPreservesDueDate() = runTest { val t = Task(id = 1, title = "T", dueDate = 1234567890L, remoteId = "u1"); setupSingleTask(t); assertEquals(1234567890L, taskDuplicator.duplicate(listOf(1L))[0].dueDate) }
}
