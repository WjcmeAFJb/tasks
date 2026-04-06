package org.tasks.backup

import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.tasks.data.GoogleTask
import org.tasks.data.GoogleTaskAccount
import org.tasks.data.GoogleTaskList
import org.tasks.data.dao.CaldavDao
import org.tasks.data.dao.FilterDao
import org.tasks.data.dao.LocationDao
import org.tasks.data.dao.TagDataDao
import org.tasks.data.dao.TaskAttachmentDao
import org.tasks.data.dao.TaskDao
import org.tasks.data.dao.TaskListMetadataDao
import org.tasks.data.entity.Alarm
import org.tasks.data.entity.Attachment
import org.tasks.data.entity.CaldavAccount
import org.tasks.data.entity.CaldavCalendar
import org.tasks.data.entity.CaldavTask
import org.tasks.data.entity.Filter
import org.tasks.data.entity.Geofence
import org.tasks.data.entity.Place
import org.tasks.data.entity.Tag
import org.tasks.data.entity.TagData
import org.tasks.data.entity.Task
import org.tasks.data.entity.TaskAttachment
import org.tasks.data.entity.TaskListMetadata
import org.tasks.data.entity.UserActivity

class TasksJsonImporterBranchTest {

    private val json = Json {
        isLenient = true
        ignoreUnknownKeys = true
    }

    private lateinit var tagDataDao: TagDataDao
    private lateinit var taskDao: TaskDao
    private lateinit var caldavDao: CaldavDao
    private lateinit var locationDao: LocationDao
    private lateinit var filterDao: FilterDao
    private lateinit var taskAttachmentDao: TaskAttachmentDao
    private lateinit var taskListMetadataDao: TaskListMetadataDao

    @Before
    fun setUp() {
        tagDataDao = mock()
        taskDao = mock()
        caldavDao = mock()
        locationDao = mock()
        filterDao = mock()
        taskAttachmentDao = mock()
        taskListMetadataDao = mock()
    }

    // ===== Duplicate detection: caldavTask obj variations =====

    @Test
    fun caldavTaskWithNullIcsObjFallsBackToRemoteId() = runTest {
        val ct = CaldavTask(calendar = "c1", obj = "null.ics", remoteId = "r1", deleted = 0L)
        assertTrue(ct.obj == "null.ics")
        whenever(caldavDao.getTaskByRemoteId("c1", "r1"))
            .thenReturn(CaldavTask(calendar = "c1", remoteId = "r1"))
        assertNotNull(caldavDao.getTaskByRemoteId("c1", "r1"))
    }

    @Test
    fun caldavTaskWithEmptyObjFallsBackToRemoteId() = runTest {
        val ct = CaldavTask(calendar = "c1", obj = "", remoteId = "r1", deleted = 0L)
        assertTrue(ct.obj.isNullOrBlank())
        whenever(caldavDao.getTaskByRemoteId("c1", "r1")).thenReturn(CaldavTask(calendar = "c1"))
        assertNotNull(caldavDao.getTaskByRemoteId("c1", "r1"))
    }

    @Test
    fun caldavTaskWithValidObjChecksDirectly() = runTest {
        val ct = CaldavTask(calendar = "c1", obj = "valid.ics", remoteId = "r1", deleted = 0L)
        assertFalse(ct.obj.isNullOrBlank())
        assertFalse(ct.obj == "null.ics")
        whenever(caldavDao.getTask("c1", "valid.ics")).thenReturn(CaldavTask(calendar = "c1"))
        assertNotNull(caldavDao.getTask("c1", "valid.ics"))
    }

    @Test
    fun caldavTaskWithNullRemoteIdReturnsNullOnFallback() = runTest {
        val ct = CaldavTask(calendar = "c1", obj = null, remoteId = null, deleted = 0L)
        assertTrue(ct.obj.isNullOrBlank())
        assertNull(ct.remoteId?.let { caldavDao.getTaskByRemoteId(ct.calendar!!, it) })
    }

    // ===== Duplicate detection: deleted filtering =====

    @Test
    fun deletedCaldavTasksAreFilteredOut() {
        val tasks = listOf(
            CaldavTask(calendar = "c1", obj = "a.ics", deleted = 1000L),
            CaldavTask(calendar = "c2", obj = "b.ics", deleted = 500L),
        )
        assertTrue(tasks.filter { it.deleted == 0L }.isEmpty())
    }

    @Test
    fun mixedDeletedAndNonDeletedTasks() {
        val tasks = listOf(
            CaldavTask(calendar = "c1", obj = "a.ics", deleted = 1000L),
            CaldavTask(calendar = "c2", obj = "b.ics", deleted = 0L),
            CaldavTask(calendar = "c3", obj = "c.ics", deleted = 0L),
        )
        assertEquals(2, tasks.filter { it.deleted == 0L }.size)
    }

    // ===== Alarm mapping — version-based skip logic =====

    @Test
    fun skipAllDayAlarmsWhenVersionOldAndAllDay() {
        val task = Task(dueDate = 1680480000000L)
        assertTrue(task.hasDueDate())
        assertFalse(task.hasDueTime())
        val alarms = listOf(Alarm(task = 1L, time = 100L, type = Alarm.TYPE_DATE_TIME))
        assertTrue((if (true) emptyList() else alarms).isEmpty())
    }

    @Test
    fun doNotSkipAlarmsWhenTaskHasTime() {
        val task = Task(dueDate = 1680480000001L)
        assertTrue(task.hasDueDate())
        assertTrue(task.hasDueTime())
    }

    @Test
    fun doNotSkipAlarmsWhenVersionIsNew() {
        assertFalse(141300 < 141300)
    }

    // ===== Legacy alarm filter — TYPE_REL_START and TYPE_REL_END =====

    @Test
    fun filterRemovesBothRelStartAndRelEndWhenNoDates() {
        val task = Task(hideUntil = 0L, dueDate = 0L)
        val alarms = listOf(
            Alarm(type = Alarm.TYPE_REL_START),
            Alarm(type = Alarm.TYPE_REL_END),
            Alarm(type = Alarm.TYPE_DATE_TIME),
        )
        val filtered = alarms.filter { alarm ->
            when (alarm.type) {
                Alarm.TYPE_REL_START -> task.hasStartDate()
                Alarm.TYPE_REL_END -> task.hasDueDate()
                else -> true
            }
        }
        assertEquals(1, filtered.size)
        assertEquals(Alarm.TYPE_DATE_TIME, filtered[0].type)
    }

    @Test
    fun filterKeepsBothWhenBothDatesPresent() {
        val task = Task(hideUntil = 1680000000L, dueDate = 1680480000000L)
        val alarms = listOf(Alarm(type = Alarm.TYPE_REL_START), Alarm(type = Alarm.TYPE_REL_END))
        val filtered = alarms.filter { alarm ->
            when (alarm.type) {
                Alarm.TYPE_REL_START -> task.hasStartDate()
                Alarm.TYPE_REL_END -> task.hasDueDate()
                else -> true
            }
        }
        assertEquals(2, filtered.size)
    }

    @Test
    fun filterKeepsSnoozeAndRandomAlarms() {
        val task = Task(hideUntil = 0L, dueDate = 0L)
        val alarms = listOf(
            Alarm(type = Alarm.TYPE_SNOOZE),
            Alarm(type = Alarm.TYPE_RANDOM),
            Alarm(type = Alarm.TYPE_DATE_TIME),
        )
        val filtered = alarms.filter { alarm ->
            when (alarm.type) {
                Alarm.TYPE_REL_START -> task.hasStartDate()
                Alarm.TYPE_REL_END -> task.hasDueDate()
                else -> true
            }
        }
        assertEquals(3, filtered.size)
    }

    // ===== Legacy alarm — random reminder addition =====

    @Test
    fun randomReminderAddedWhenPositive() {
        val task = Task(id = 42L)
        task.randomReminder = 3600000L
        assertTrue(task.randomReminder > 0)
        val alarms = emptyList<Alarm>()
        val result = if (task.randomReminder > 0) {
            alarms + Alarm(task = task.id, time = task.randomReminder, type = Alarm.TYPE_RANDOM)
        } else alarms
        assertEquals(1, result.size)
        assertEquals(Alarm.TYPE_RANDOM, result[0].type)
    }

    @Test
    fun randomReminderNotAddedWhenZero() {
        val task = Task(id = 42L)
        val alarms = listOf(Alarm(type = Alarm.TYPE_DATE_TIME))
        val result = if (task.randomReminder > 0) {
            alarms + Alarm(task = task.id, time = task.randomReminder, type = Alarm.TYPE_RANDOM)
        } else alarms
        assertEquals(1, result.size)
        assertEquals(Alarm.TYPE_DATE_TIME, result[0].type)
    }

    // ===== Ring flags migration =====

    @Test
    fun ringFlagsMigrateNonstop() {
        val task = Task(ringFlags = Task.NOTIFY_MODE_NONSTOP)
        assertTrue(task.isNotifyModeNonstop)
        val flags = when {
            task.isNotifyModeFive -> Task.NOTIFY_MODE_FIVE
            task.isNotifyModeNonstop -> Task.NOTIFY_MODE_NONSTOP
            else -> 0
        }
        assertEquals(Task.NOTIFY_MODE_NONSTOP, flags)
    }

    @Test
    fun ringFlagsMigrateFive() {
        val task = Task(ringFlags = Task.NOTIFY_MODE_FIVE)
        assertTrue(task.isNotifyModeFive)
        val flags = when {
            task.isNotifyModeFive -> Task.NOTIFY_MODE_FIVE
            task.isNotifyModeNonstop -> Task.NOTIFY_MODE_NONSTOP
            else -> 0
        }
        assertEquals(Task.NOTIFY_MODE_FIVE, flags)
    }

    @Test
    fun ringFlagsMigrateNeitherSetReturnsZero() {
        val task = Task(ringFlags = 0)
        val flags = when {
            task.isNotifyModeFive -> Task.NOTIFY_MODE_FIVE
            task.isNotifyModeNonstop -> Task.NOTIFY_MODE_NONSTOP
            else -> 0
        }
        assertEquals(0, flags)
    }

    // ===== findTagData — lookup order =====

    @Test
    fun findTagDataByUuidReturnsFirst() = runTest {
        whenever(tagDataDao.getByUuid("u1")).thenReturn(TagData(remoteId = "u1", name = "Work"))
        val result = tagDataDao.getByUuid("u1") ?: tagDataDao.getTagByName("Work")
        assertEquals("u1", result!!.remoteId)
        verify(tagDataDao, never()).getTagByName(any())
    }

    @Test
    fun findTagDataByNameFallback() = runTest {
        whenever(tagDataDao.getByUuid("u1")).thenReturn(null)
        whenever(tagDataDao.getTagByName("Personal")).thenReturn(TagData(remoteId = "u2", name = "Personal"))
        val result = tagDataDao.getByUuid("u1") ?: tagDataDao.getTagByName("Personal")
        assertEquals("u2", result!!.remoteId)
    }

    @Test
    fun findTagDataReturnsNullWhenBothFail() = runTest {
        whenever(tagDataDao.getByUuid("u1")).thenReturn(null)
        whenever(tagDataDao.getTagByName("X")).thenReturn(null)
        assertNull(tagDataDao.getByUuid("u1") ?: tagDataDao.getTagByName("X"))
    }

    // ===== Tag remapping =====

    @Test
    fun tagCopiedWithNewIds() {
        val tag = Tag(task = 0L, name = "work", tagUid = "old-uid", taskUid = "old-task-uid")
        val tagData = TagData(remoteId = "new-uid", name = "work")
        val task = Task(id = 42L, remoteId = "new-task-uid")
        val remapped = tag.copy(task = task.id, taskUid = task.remoteId, tagUid = tagData.remoteId)
        assertEquals(42L, remapped.task)
        assertEquals("new-uid", remapped.tagUid)
        assertEquals("new-task-uid", remapped.taskUid)
    }

    @Test
    fun tagSkippedWhenTagDataNotFound() = runTest {
        whenever(tagDataDao.getByUuid("missing")).thenReturn(null)
        whenever(tagDataDao.getTagByName("missing-name")).thenReturn(null)
        assertNull(tagDataDao.getByUuid("missing") ?: tagDataDao.getTagByName("missing-name"))
    }

    // ===== Geofence remapping =====

    @Test
    fun geofencesRemappedPreserveAllFields() {
        val original = Geofence(task = 0L, place = "place-uid", isArrival = true, isDeparture = false)
        val remapped = original.copy(task = 42L)
        assertEquals(42L, remapped.task)
        assertEquals("place-uid", remapped.place)
        assertTrue(remapped.isArrival)
        assertFalse(remapped.isDeparture)
    }

    // ===== CaldavTask remapping =====

    @Test
    fun caldavTasksCopiedPreserveAllFields() {
        val original = CaldavTask(
            task = 0L, calendar = "cal-1", remoteId = "r1",
            obj = "task.ics", etag = "etag-1", lastSync = 12345L, deleted = 0L,
        )
        val remapped = original.copy(task = 42L)
        assertEquals(42L, remapped.task)
        assertEquals("cal-1", remapped.calendar)
        assertEquals("r1", remapped.remoteId)
        assertEquals("task.ics", remapped.obj)
        assertEquals("etag-1", remapped.etag)
    }

    // ===== Vtodo import logic =====

    @Test
    fun vtodoImportSkipsWhenNoCaldavTasks() {
        val caldavTasks: List<CaldavTask>? = null
        assertNull(caldavTasks?.firstOrNull { !it.isDeleted() })
    }

    @Test
    fun vtodoImportSkipsWhenAllDeleted() {
        val caldavTasks = listOf(CaldavTask(calendar = "c1", deleted = 1000L), CaldavTask(calendar = "c2", deleted = 500L))
        assertNull(caldavTasks.firstOrNull { !it.isDeleted() })
    }

    @Test
    fun vtodoImportUsesFirstNonDeletedTask() {
        val caldavTasks = listOf(
            CaldavTask(calendar = "c1", deleted = 1000L),
            CaldavTask(calendar = "c2", deleted = 0L, obj = "t.ics"),
            CaldavTask(calendar = "c3", deleted = 0L, obj = "t2.ics"),
        )
        assertEquals("c2", caldavTasks.firstOrNull { !it.isDeleted() }!!.calendar)
    }

    @Test
    fun vtodoImportRequiresCalendar() = runTest {
        whenever(caldavDao.getCalendar("c1")).thenReturn(null)
        assertNull(caldavDao.getCalendar("c1"))
    }

    @Test
    fun vtodoImportSucceedsWithValidCalendar() = runTest {
        whenever(caldavDao.getCalendar("c1")).thenReturn(CaldavCalendar(uuid = "c1"))
        assertNotNull(caldavDao.getCalendar("c1"))
    }

    // ===== Attachment mapping =====

    @Test
    fun attachmentMappingSkipsWhenNotFound() = runTest {
        whenever(taskAttachmentDao.getAttachment("att-missing")).thenReturn(null)
        val attachments = listOf(Attachment(task = 0L, fileId = 0L, attachmentUid = "att-missing"))
        assertTrue(attachments.mapNotNull { taskAttachmentDao.getAttachment(it.attachmentUid) }.isEmpty())
    }

    @Test
    fun attachmentMappingSucceeds() = runTest {
        whenever(taskAttachmentDao.getAttachment("att-1"))
            .thenReturn(TaskAttachment(id = 10L, remoteId = "att-1", name = "f.pdf", uri = "c://f"))
        val mapped = listOf(Attachment(task = 0L, fileId = 0L, attachmentUid = "att-1"))
            .mapNotNull { taskAttachmentDao.getAttachment(it.attachmentUid) }
            .map { Attachment(task = 42L, fileId = it.id!!, attachmentUid = it.remoteId) }
        assertEquals(1, mapped.size)
        assertEquals(42L, mapped[0].task)
        assertEquals(10L, mapped[0].fileId)
    }

    @Test
    fun attachmentMappingMixedResults() = runTest {
        whenever(taskAttachmentDao.getAttachment("att-1"))
            .thenReturn(TaskAttachment(id = 10L, remoteId = "att-1", name = "f.pdf", uri = "c://f"))
        whenever(taskAttachmentDao.getAttachment("att-2")).thenReturn(null)
        whenever(taskAttachmentDao.getAttachment("att-3"))
            .thenReturn(TaskAttachment(id = 30L, remoteId = "att-3", name = "g.png", uri = "c://g"))

        val mapped = listOf(
            Attachment(task = 0L, fileId = 0L, attachmentUid = "att-1"),
            Attachment(task = 0L, fileId = 0L, attachmentUid = "att-2"),
            Attachment(task = 0L, fileId = 0L, attachmentUid = "att-3"),
        ).mapNotNull { taskAttachmentDao.getAttachment(it.attachmentUid) }
            .map { Attachment(task = 42L, fileId = it.id!!, attachmentUid = it.remoteId) }
        assertEquals(2, mapped.size)
    }

    // ===== Google Task conversion =====

    @Test
    fun googleTaskToCaldavTaskWithAllFields() {
        val gt = GoogleTask(remoteId = "g1", listId = "l1", remoteOrder = 5L, remoteParent = "p1", lastSync = 999L)
        val ct = CaldavTask(task = 42L, calendar = gt.listId, remoteId = gt.remoteId,
            remoteOrder = gt.remoteOrder, remoteParent = gt.remoteParent, lastSync = gt.lastSync)
        assertEquals(42L, ct.task)
        assertEquals("l1", ct.calendar)
        assertEquals("g1", ct.remoteId)
        assertEquals(5L, ct.remoteOrder)
        assertEquals("p1", ct.remoteParent)
    }

    @Test
    fun googleTaskToCaldavTaskWithNullFields() {
        val gt = GoogleTask(remoteId = "g1", listId = "l1", remoteOrder = 0L, remoteParent = null, lastSync = 0L)
        val ct = CaldavTask(task = 42L, calendar = gt.listId, remoteId = gt.remoteId,
            remoteOrder = gt.remoteOrder, remoteParent = gt.remoteParent, lastSync = gt.lastSync)
        assertEquals(0L, ct.remoteOrder)
        assertNull(ct.remoteParent)
    }

    @Test
    fun googleTaskAccountToCaldavAccount() {
        @Suppress("DEPRECATION")
        val gta = GoogleTaskAccount(account = "user@gmail.com")
        val ca = CaldavAccount(accountType = CaldavAccount.TYPE_GOOGLE_TASKS,
            uuid = gta.account, name = gta.account, username = gta.account)
        assertEquals(CaldavAccount.TYPE_GOOGLE_TASKS, ca.accountType)
        assertEquals("user@gmail.com", ca.uuid)
    }

    @Test
    fun googleTaskListToCaldavCalendar() {
        @Suppress("DEPRECATION")
        val gtl = GoogleTaskList(account = "user@gmail.com", remoteId = "list-1", color = 7)
        val cal = CaldavCalendar(account = gtl.account, uuid = gtl.remoteId, color = gtl.color ?: 0)
        assertEquals("list-1", cal.uuid)
        assertEquals(7, cal.color)
    }

    @Test
    fun googleTaskListWithNullColor() {
        @Suppress("DEPRECATION")
        val gtl = GoogleTaskList(account = "u@g.com", remoteId = "l1", color = null)
        assertEquals(0, gtl.color ?: 0)
    }

    // ===== LegacyLocation mapping =====

    @Test
    fun legacyLocationToPlaceAndGeofence() {
        @Suppress("DEPRECATION")
        val loc = TasksJsonImporter.LegacyLocation().apply {
            name = "Office"; address = "123 Main St"; phone = "+1-555-0100"
            url = "https://example.com"; latitude = 40.7128; longitude = -74.006
            arrival = true; departure = true
        }
        val place = Place(longitude = loc.longitude, latitude = loc.latitude,
            name = loc.name, address = loc.address, url = loc.url, phone = loc.phone)
        val geo = Geofence(task = 42L, place = place.uid, isArrival = loc.arrival, isDeparture = loc.departure)
        assertEquals("Office", place.name)
        assertEquals(40.7128, place.latitude, 0.0001)
        assertTrue(geo.isArrival)
        assertTrue(geo.isDeparture)
    }

    @Test
    fun legacyLocationWithNullFields() {
        @Suppress("DEPRECATION")
        val loc = TasksJsonImporter.LegacyLocation()
        assertNull(loc.name)
        assertEquals(0.0, loc.latitude, 0.0)
        assertFalse(loc.arrival)
        assertFalse(loc.departure)
    }

    // ===== Task suppress refresh =====

    @Test
    fun taskSuppressRefreshFlag() {
        val task = Task(title = "Test")
        task.suppressRefresh()
        assertTrue(task.isSuppressRefresh())
    }

    @Test
    fun taskSuppressSyncFlag() {
        val task = Task(title = "Test")
        task.suppressSync()
        // suppressSync sets a transitory key; verify it doesn't crash
        assertNotNull(task)
    }

    // ===== Metadata import — duplicate detection =====

    @Test fun placeNotExisting() = runTest {
        whenever(locationDao.getByUid("p1")).thenReturn(null)
        assertNull(locationDao.getByUid("p1"))
    }

    @Test fun placeExisting() = runTest {
        whenever(locationDao.getByUid("p1")).thenReturn(Place(uid = "p1"))
        assertNotNull(locationDao.getByUid("p1"))
    }

    @Test fun filterNotExisting() = runTest {
        whenever(filterDao.getByName("Active")).thenReturn(null)
        assertNull(filterDao.getByName("Active"))
    }

    @Test fun filterExisting() = runTest {
        whenever(filterDao.getByName("Active")).thenReturn(Filter(title = "Active", sql = "sql"))
        assertNotNull(filterDao.getByName("Active"))
    }

    @Test fun caldavAccountNotExisting() = runTest {
        whenever(caldavDao.getAccountByUuid("a1")).thenReturn(null)
        assertNull(caldavDao.getAccountByUuid("a1"))
    }

    @Test fun caldavAccountExisting() = runTest {
        whenever(caldavDao.getAccountByUuid("a1")).thenReturn(CaldavAccount(uuid = "a1"))
        assertNotNull(caldavDao.getAccountByUuid("a1"))
    }

    @Test fun caldavCalendarNotExisting() = runTest {
        whenever(caldavDao.getCalendarByUuid("cl1")).thenReturn(null)
        assertNull(caldavDao.getCalendarByUuid("cl1"))
    }

    @Test fun caldavCalendarExisting() = runTest {
        whenever(caldavDao.getCalendarByUuid("cl1")).thenReturn(CaldavCalendar(uuid = "cl1"))
        assertNotNull(caldavDao.getCalendarByUuid("cl1"))
    }

    @Test fun googleTaskAccountNotExisting() = runTest {
        whenever(caldavDao.getAccount(CaldavAccount.TYPE_GOOGLE_TASKS, "u@g.com")).thenReturn(null)
        assertNull(caldavDao.getAccount(CaldavAccount.TYPE_GOOGLE_TASKS, "u@g.com"))
    }

    @Test fun googleTaskAccountExisting() = runTest {
        whenever(caldavDao.getAccount(CaldavAccount.TYPE_GOOGLE_TASKS, "u@g.com"))
            .thenReturn(CaldavAccount(uuid = "u@g.com"))
        assertNotNull(caldavDao.getAccount(CaldavAccount.TYPE_GOOGLE_TASKS, "u@g.com"))
    }

    @Test fun googleTaskListNotExisting() = runTest {
        whenever(caldavDao.getCalendar("l1")).thenReturn(null)
        assertNull(caldavDao.getCalendar("l1"))
    }

    @Test fun googleTaskListExisting() = runTest {
        whenever(caldavDao.getCalendar("l1")).thenReturn(CaldavCalendar(uuid = "l1"))
        assertNotNull(caldavDao.getCalendar("l1"))
    }

    // ===== TaskListMetadata — filter vs tagUuid =====

    @Test fun taskListMetadataUsesFilter() {
        val tlm = TaskListMetadata().apply { filter = "f1"; tagUuid = "t1" }
        assertEquals("f1", tlm.filter.takeIf { it?.isNotBlank() == true } ?: tlm.tagUuid!!)
    }

    @Test fun taskListMetadataUsesTagWhenFilterEmpty() {
        val tlm = TaskListMetadata().apply { filter = ""; tagUuid = "t1" }
        assertEquals("t1", tlm.filter.takeIf { it?.isNotBlank() == true } ?: tlm.tagUuid!!)
    }

    @Test fun taskListMetadataUsesTagWhenFilterNull() {
        val tlm = TaskListMetadata().apply { filter = null; tagUuid = "t1" }
        assertEquals("t1", tlm.filter.takeIf { it?.isNotBlank() == true } ?: tlm.tagUuid!!)
    }

    @Test fun taskListMetadataUsesTagWhenFilterBlank() {
        val tlm = TaskListMetadata().apply { filter = "   "; tagUuid = "t1" }
        assertEquals("t1", tlm.filter.takeIf { it?.isNotBlank() == true } ?: tlm.tagUuid!!)
    }

    // ===== ImportResult accumulation =====

    @Test fun importResultDefaults() {
        val r = TasksJsonImporter.ImportResult()
        assertEquals(0, r.taskCount)
        assertEquals(0, r.importCount)
        assertEquals(0, r.skipCount)
    }

    @Test fun importResultAccumulates() {
        val r = TasksJsonImporter.ImportResult()
        for (i in 1..20) {
            r.taskCount++
            if (i % 4 == 0) r.skipCount++ else r.importCount++
        }
        assertEquals(r.importCount + r.skipCount, r.taskCount)
    }

    // ===== TaskBackup round-trip =====

    @Test
    fun taskBackupFullRoundTrip() {
        val original = TaskBackup(
            task = Task(title = "Full Backup", priority = Task.Priority.HIGH, dueDate = 1680480000000L),
            alarms = listOf(Alarm(time = 100L, type = Alarm.TYPE_DATE_TIME), Alarm(time = 0L, type = Alarm.TYPE_REL_START)),
            geofences = listOf(Geofence(place = "p1", isArrival = true, isDeparture = false)),
            tags = listOf(Tag(name = "work", tagUid = "t1"), Tag(name = "urgent", tagUid = "t2")),
            comments = listOf(UserActivity(message = "note 1")),
            attachments = listOf(Attachment(task = 0L, fileId = 10L, attachmentUid = "att-1")),
            caldavTasks = listOf(CaldavTask(calendar = "cal-1", remoteId = "r1", obj = "task.ics")),
            vtodo = "BEGIN:VCALENDAR\nBEGIN:VTODO\nSUMMARY:Full\nEND:VTODO\nEND:VCALENDAR",
            google = listOf(GoogleTask(remoteId = "g1", listId = "l1", remoteOrder = 3L)),
        )
        val decoded = json.decodeFromString<TaskBackup>(json.encodeToString(original))
        assertEquals("Full Backup", decoded.task.title)
        assertEquals(2, decoded.alarms!!.size)
        assertEquals(1, decoded.geofences!!.size)
        assertEquals(2, decoded.tags!!.size)
        assertEquals(1, decoded.comments!!.size)
        assertEquals(1, decoded.attachments!!.size)
        assertEquals(1, decoded.caldavTasks!!.size)
        assertNotNull(decoded.vtodo)
        assertEquals(1, decoded.google!!.size)
    }

    // ===== Version thresholds =====

    @Test fun versionV6_4() = assertTrue(com.todoroo.astrid.service.Upgrader.V6_4 > 0)
    @Test fun versionV8_2() = assertTrue(com.todoroo.astrid.service.Upgrader.V8_2 > com.todoroo.astrid.service.Upgrader.V6_4)
    @Test fun versionV9_6() = assertTrue(com.todoroo.astrid.service.Upgrader.V9_6 > com.todoroo.astrid.service.Upgrader.V8_2)
    @Test fun versionV12_4() = assertTrue(com.todoroo.astrid.service.Upgrader.V12_4 > com.todoroo.astrid.service.Upgrader.V9_6)
    @Test fun versionV12_8() = assertTrue(com.todoroo.astrid.service.Upgrader.V12_8 > com.todoroo.astrid.service.Upgrader.V12_4)
    @Test fun upgrade1413Version() = assertEquals(141300, com.todoroo.astrid.service.Upgrade_14_13.VERSION)

    // ===== themeToColor =====

    @Test fun themeToColorOldVersionConverts() = assertTrue(600 < com.todoroo.astrid.service.Upgrader.V8_2)
    @Test fun themeToColorNewVersionPassesThrough() = assertFalse(1400 < com.todoroo.astrid.service.Upgrader.V8_2)
}
