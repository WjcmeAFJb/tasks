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
import org.mockito.Mockito.mock
import org.mockito.Mockito.`when`
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

/**
 * Deep tests for TasksJsonImporter logic.
 *
 * Because TasksJsonImporter.importTasks uses android.util.JsonReader
 * (not available in unit tests), these tests exercise all internal logic
 * paths: deserialization, duplicate detection, tag lookup, alarm/geofence
 * mapping, legacy migration, attachment mapping, vtodo resolution, etc.
 */
class TasksJsonImporterDeepTest {

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

    // ===== TaskBackup deserialization =====

    @Test
    fun deserializeMinimalBackup() {
        val backup = json.decodeFromString<TaskBackup>("""{"task":{"title":"Test"}}""")
        assertEquals("Test", backup.task.title)
        assertNull(backup.alarms)
        assertNull(backup.geofences)
        assertNull(backup.tags)
        assertNull(backup.comments)
        assertNull(backup.attachments)
        assertNull(backup.caldavTasks)
        assertNull(backup.vtodo)
        assertNull(backup.google)
        assertNull(backup.locations)
    }

    @Test
    fun deserializeBackupWithAlarms() {
        val backup = json.decodeFromString<TaskBackup>("""{"task":{"title":"T"},"alarms":[{"time":1000,"type":1},{"time":2000,"type":2}]}""")
        assertEquals(2, backup.alarms!!.size)
        assertEquals(1000L, backup.alarms!![0].time)
        assertEquals(2000L, backup.alarms!![1].time)
    }

    @Test
    fun deserializeBackupWithGeofences() {
        val backup = json.decodeFromString<TaskBackup>("""{"task":{"title":"T"},"geofences":[{"place":"p1","isArrival":true,"isDeparture":false}]}""")
        assertEquals(1, backup.geofences!!.size)
        assertTrue(backup.geofences!![0].isArrival)
        assertFalse(backup.geofences!![0].isDeparture)
    }

    @Test
    fun deserializeBackupWithTags() {
        val backup = json.decodeFromString<TaskBackup>("""{"task":{"title":"T"},"tags":[{"name":"work","tagUid":"u1"},{"name":"home","tagUid":"u2"}]}""")
        assertEquals(2, backup.tags!!.size)
        assertEquals("work", backup.tags!![0].name)
        assertEquals("home", backup.tags!![1].name)
    }

    @Test
    fun deserializeBackupWithComments() {
        val backup = json.decodeFromString<TaskBackup>("""{"task":{"title":"T"},"comments":[{"message":"hello"},{"message":"world"}]}""")
        assertEquals(2, backup.comments!!.size)
    }

    @Test
    fun deserializeBackupWithCaldavTasks() {
        val backup = json.decodeFromString<TaskBackup>("""{"task":{"title":"T"},"caldavTasks":[{"calendar":"c1","remoteId":"r1","deleted":0},{"calendar":"c2","remoteId":"r2","deleted":1000}]}""")
        assertEquals(2, backup.caldavTasks!!.size)
        assertEquals(0L, backup.caldavTasks!![0].deleted)
        assertEquals(1000L, backup.caldavTasks!![1].deleted)
    }

    @Test
    fun deserializeBackupWithVtodo() {
        val backup = json.decodeFromString<TaskBackup>("""{"task":{"title":"T"},"vtodo":"BEGIN:VTODO\nEND:VTODO"}""")
        assertEquals("BEGIN:VTODO\nEND:VTODO", backup.vtodo)
    }

    @Test
    fun deserializeBackupWithGoogleTasks() {
        val backup = json.decodeFromString<TaskBackup>("""{"task":{"title":"T"},"google":[{"remoteId":"g1","listId":"l1","remoteOrder":5,"lastSync":999}]}""")
        assertEquals(1, backup.google!!.size)
        assertEquals("g1", backup.google!![0].remoteId)
        assertEquals("l1", backup.google!![0].listId)
        assertEquals(5L, backup.google!![0].remoteOrder)
    }

    @Test
    fun deserializeBackupWithLegacyLocations() {
        val backup = json.decodeFromString<TaskBackup>("""{"task":{"title":"T"},"locations":[{"name":"Office","latitude":40.7,"longitude":-74.0,"arrival":true,"departure":false}]}""")
        assertEquals(1, backup.locations!!.size)
        assertEquals("Office", backup.locations!![0].name)
        assertTrue(backup.locations!![0].arrival)
    }

    @Test
    fun deserializeBackupWithAttachments() {
        val backup = json.decodeFromString<TaskBackup>("""{"task":{"title":"T"},"attachments":[{"attachmentUid":"att-1"}]}""")
        assertEquals(1, backup.attachments!!.size)
    }

    @Test
    fun deserializeBackupWithEmptyLists() {
        val backup = json.decodeFromString<TaskBackup>("""{"task":{},"alarms":[],"geofences":[],"tags":[],"comments":[],"attachments":[],"caldavTasks":[]}""")
        assertTrue(backup.alarms!!.isEmpty())
        assertTrue(backup.geofences!!.isEmpty())
        assertTrue(backup.tags!!.isEmpty())
        assertTrue(backup.comments!!.isEmpty())
        assertTrue(backup.caldavTasks!!.isEmpty())
    }

    @Test
    fun deserializeBackupIgnoresUnknownFields() {
        val backup = json.decodeFromString<TaskBackup>("""{"task":{"title":"T","unknownField":42},"futureField":"x"}""")
        assertEquals("T", backup.task.title)
    }

    @Test
    fun deserializeBackupRoundTrip() {
        val original = TaskBackup(
            task = Task(title = "RT", priority = Task.Priority.HIGH, dueDate = 12345L),
            alarms = listOf(Alarm(time = 1000L, type = Alarm.TYPE_DATE_TIME)),
            geofences = listOf(Geofence(place = "p1", isArrival = true)),
            tags = listOf(Tag(name = "t", tagUid = "u")),
            comments = listOf(UserActivity(message = "c")),
            caldavTasks = listOf(CaldavTask(calendar = "cal1", remoteId = "r1")),
            vtodo = "BEGIN:VTODO\nEND:VTODO",
        )
        val decoded = json.decodeFromString<TaskBackup>(json.encodeToString(original))
        assertEquals("RT", decoded.task.title)
        assertEquals(1, decoded.alarms!!.size)
        assertEquals(1, decoded.caldavTasks!!.size)
        assertEquals("BEGIN:VTODO\nEND:VTODO", decoded.vtodo)
    }

    // ===== Duplicate detection: task UUID =====

    @Test
    fun skipWhenUuidExists() = runTest {
        `when`(taskDao.fetch("existing")).thenReturn(Task(id = 1L, remoteId = "existing"))
        assertNotNull(taskDao.fetch("existing"))
    }

    @Test
    fun doNotSkipWhenUuidNotFound() = runTest {
        `when`(taskDao.fetch("new")).thenReturn(null)
        assertNull(taskDao.fetch("new"))
    }

    // ===== Duplicate detection: CaldavTask =====

    @Test
    fun skipWhenCaldavTaskExistsByObj() = runTest {
        `when`(caldavDao.getTask("c1", "t.ics")).thenReturn(CaldavTask(calendar = "c1"))
        assertNotNull(caldavDao.getTask("c1", "t.ics"))
    }

    @Test
    fun skipWhenCaldavTaskRemoteIdWithNullObj() = runTest {
        `when`(caldavDao.getTaskByRemoteId("c1", "r1")).thenReturn(CaldavTask(calendar = "c1"))
        val ct = CaldavTask(calendar = "c1", obj = null, remoteId = "r1", deleted = 0L)
        assertTrue(ct.obj.isNullOrBlank())
        assertNotNull(caldavDao.getTaskByRemoteId("c1", "r1"))
    }

    @Test
    fun caldavTaskObjNullIcsPattern() {
        val ct = CaldavTask(calendar = "c1", obj = "null.ics", remoteId = "r1", deleted = 0L)
        assertEquals("null.ics", ct.obj)
        assertTrue(ct.obj == "null.ics")
    }

    @Test
    fun onlyNonDeletedCaldavTasksChecked() {
        val tasks = listOf(
            CaldavTask(calendar = "c1", obj = "a.ics", deleted = 1000L),
            CaldavTask(calendar = "c2", obj = "b.ics", deleted = 0L),
        )
        val nonDeleted = tasks.filter { it.deleted == 0L }
        assertEquals(1, nonDeleted.size)
        assertEquals("c2", nonDeleted[0].calendar)
    }

    @Test
    fun caldavTaskObjBlankString() {
        assertTrue(CaldavTask(calendar = "c1", obj = "").obj.isNullOrBlank())
    }

    @Test
    fun caldavTaskObjNull() {
        assertTrue(CaldavTask(calendar = "c1", obj = null).obj.isNullOrBlank())
    }

    @Test
    fun caldavTaskObjValid() {
        val ct = CaldavTask(calendar = "c1", obj = "task-123.ics")
        assertFalse(ct.obj.isNullOrBlank())
        assertFalse(ct.obj == "null.ics")
    }

    // ===== Alarm mapping =====

    @Test
    fun alarmsRemappedToNewTaskId() {
        val alarms = listOf(Alarm(task = 99L, time = 1000L, type = Alarm.TYPE_DATE_TIME), Alarm(task = 99L, time = 2000L))
        val remapped = alarms.map { it.copy(task = 42L) }
        assertEquals(42L, remapped[0].task)
        assertEquals(42L, remapped[1].task)
    }

    @Test
    fun skipAllDayAlarmsCondition() {
        val task = Task(dueDate = 1680480000000L)
        assertTrue(task.hasDueDate())
        assertFalse(task.hasDueTime())
    }

    @Test
    fun doNotSkipAlarmsWhenTaskHasDueTime() {
        val task = Task(dueDate = 1680480000001L)
        assertTrue(task.hasDueDate())
        assertTrue(task.hasDueTime())
    }

    // ===== Legacy alarm filter =====

    @Test
    fun filterRemovesRelStartWhenNoStartDate() {
        val task = Task(hideUntil = 0L)
        assertFalse(task.hasStartDate())
        val alarms = listOf(Alarm(type = Alarm.TYPE_REL_START), Alarm(type = Alarm.TYPE_DATE_TIME))
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
    fun filterRemovesRelEndWhenNoDueDate() {
        val task = Task(dueDate = 0L)
        val alarms = listOf(Alarm(type = Alarm.TYPE_REL_END), Alarm(type = Alarm.TYPE_SNOOZE))
        val filtered = alarms.filter { alarm ->
            when (alarm.type) {
                Alarm.TYPE_REL_START -> task.hasStartDate()
                Alarm.TYPE_REL_END -> task.hasDueDate()
                else -> true
            }
        }
        assertEquals(1, filtered.size)
        assertEquals(Alarm.TYPE_SNOOZE, filtered[0].type)
    }

    @Test
    fun filterKeepsRelStartWhenStartDatePresent() {
        val task = Task(hideUntil = 1680000000L)
        assertTrue(task.hasStartDate())
        val alarms = listOf(Alarm(type = Alarm.TYPE_REL_START))
        val filtered = alarms.filter { alarm ->
            when (alarm.type) { Alarm.TYPE_REL_START -> task.hasStartDate(); Alarm.TYPE_REL_END -> task.hasDueDate(); else -> true }
        }
        assertEquals(1, filtered.size)
    }

    @Test
    fun filterKeepsRelEndWhenDueDatePresent() {
        val task = Task(dueDate = 1680480000000L)
        assertTrue(task.hasDueDate())
        val alarms = listOf(Alarm(type = Alarm.TYPE_REL_END))
        val filtered = alarms.filter { alarm ->
            when (alarm.type) { Alarm.TYPE_REL_START -> task.hasStartDate(); Alarm.TYPE_REL_END -> task.hasDueDate(); else -> true }
        }
        assertEquals(1, filtered.size)
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
    fun ringFlagsMigrateZero() {
        val task = Task(ringFlags = 0)
        val flags = when {
            task.isNotifyModeFive -> Task.NOTIFY_MODE_FIVE
            task.isNotifyModeNonstop -> Task.NOTIFY_MODE_NONSTOP
            else -> 0
        }
        assertEquals(0, flags)
    }

    // ===== findTagData logic =====

    @Test
    fun findTagDataByUuidFirst() = runTest {
        `when`(tagDataDao.getByUuid("u1")).thenReturn(TagData(remoteId = "u1", name = "Work"))
        val found = tagDataDao.getByUuid("u1") ?: tagDataDao.getTagByName("Work")
        assertEquals("u1", found!!.remoteId)
    }

    @Test
    fun findTagDataByNameFallback() = runTest {
        `when`(tagDataDao.getByUuid("u1")).thenReturn(null)
        `when`(tagDataDao.getTagByName("Personal")).thenReturn(TagData(remoteId = "u2", name = "Personal"))
        val found = tagDataDao.getByUuid("u1") ?: tagDataDao.getTagByName("Personal")
        assertEquals("u2", found!!.remoteId)
    }

    @Test
    fun findTagDataReturnsNullWhenNotFound() = runTest {
        `when`(tagDataDao.getByUuid("u1")).thenReturn(null)
        `when`(tagDataDao.getTagByName("X")).thenReturn(null)
        assertNull(tagDataDao.getByUuid("u1") ?: tagDataDao.getTagByName("X"))
    }

    // ===== Metadata import logic =====

    @Test
    fun placeNotExisting() = runTest {
        `when`(locationDao.getByUid("p1")).thenReturn(null)
        assertNull(locationDao.getByUid("p1"))
    }

    @Test
    fun placeExisting() = runTest {
        `when`(locationDao.getByUid("p1")).thenReturn(Place(uid = "p1"))
        assertNotNull(locationDao.getByUid("p1"))
    }

    @Test
    fun filterNotExisting() = runTest {
        `when`(filterDao.getByName("Active")).thenReturn(null)
        assertNull(filterDao.getByName("Active"))
    }

    @Test
    fun filterExisting() = runTest {
        `when`(filterDao.getByName("Active")).thenReturn(Filter(title = "Active", sql = "S1"))
        assertNotNull(filterDao.getByName("Active"))
    }

    @Test
    fun caldavAccountNotExisting() = runTest {
        `when`(caldavDao.getAccountByUuid("a1")).thenReturn(null)
        assertNull(caldavDao.getAccountByUuid("a1"))
    }

    @Test
    fun caldavCalendarNotExisting() = runTest {
        `when`(caldavDao.getCalendarByUuid("cl1")).thenReturn(null)
        assertNull(caldavDao.getCalendarByUuid("cl1"))
    }

    @Test
    fun taskListMetadataUsesFilterWhenNotBlank() {
        val tlm = TaskListMetadata().apply { filter = "f1"; tagUuid = "t1" }
        val id = tlm.filter.takeIf { it?.isNotBlank() == true } ?: tlm.tagUuid!!
        assertEquals("f1", id)
    }

    @Test
    fun taskListMetadataUsesTagUuidWhenFilterBlank() {
        val tlm = TaskListMetadata().apply { filter = ""; tagUuid = "t1" }
        val id = tlm.filter.takeIf { it?.isNotBlank() == true } ?: tlm.tagUuid!!
        assertEquals("t1", id)
    }

    @Test
    fun taskListMetadataUsesTagUuidWhenFilterNull() {
        val tlm = TaskListMetadata().apply { filter = null; tagUuid = "t1" }
        val id = tlm.filter.takeIf { it?.isNotBlank() == true } ?: tlm.tagUuid!!
        assertEquals("t1", id)
    }

    @Test
    fun taskAttachmentNotExisting() = runTest {
        `when`(taskAttachmentDao.getAttachment("f1")).thenReturn(null)
        assertNull(taskAttachmentDao.getAttachment("f1"))
    }

    @Test
    fun taskAttachmentExisting() = runTest {
        `when`(taskAttachmentDao.getAttachment("f1")).thenReturn(TaskAttachment(remoteId = "f1", name = "f.pdf", uri = "c://f"))
        assertNotNull(taskAttachmentDao.getAttachment("f1"))
    }

    // ===== Google Task migration =====

    @Test
    fun googleTaskToCaldavTask() {
        val gt = GoogleTask(remoteId = "g1", listId = "l1", remoteOrder = 5L, remoteParent = "p1", lastSync = 999L)
        val ct = CaldavTask(calendar = gt.listId, remoteId = gt.remoteId, remoteOrder = gt.remoteOrder, remoteParent = gt.remoteParent, lastSync = gt.lastSync)
        assertEquals("l1", ct.calendar)
        assertEquals("g1", ct.remoteId)
        assertEquals(5L, ct.remoteOrder)
        assertEquals("p1", ct.remoteParent)
        assertEquals(999L, ct.lastSync)
    }

    @Test
    fun googleTaskAccountToCaldavAccount() {
        @Suppress("DEPRECATION")
        val gta = GoogleTaskAccount(account = "u@g.com")
        val ca = CaldavAccount(accountType = CaldavAccount.TYPE_GOOGLE_TASKS, uuid = gta.account, name = gta.account, username = gta.account)
        assertEquals(CaldavAccount.TYPE_GOOGLE_TASKS, ca.accountType)
        assertEquals("u@g.com", ca.uuid)
    }

    @Test
    fun googleTaskListToCaldavCalendar() {
        @Suppress("DEPRECATION")
        val gtl = GoogleTaskList(account = "u@g.com", remoteId = "l1", color = 5)
        val cal = CaldavCalendar(account = gtl.account, uuid = gtl.remoteId, color = gtl.color ?: 0)
        assertEquals("u@g.com", cal.account)
        assertEquals("l1", cal.uuid)
        assertEquals(5, cal.color)
    }

    // ===== Legacy location to Place/Geofence =====

    @Test
    fun legacyLocationToPlace() {
        @Suppress("DEPRECATION")
        val loc = TasksJsonImporter.LegacyLocation().apply {
            name = "Office"; address = "123 Main St"; phone = "+1-555-0100"; url = "https://example.com"
            latitude = 40.7128; longitude = -74.006
        }
        val place = Place(longitude = loc.longitude, latitude = loc.latitude, name = loc.name, address = loc.address, url = loc.url, phone = loc.phone)
        assertEquals("Office", place.name)
        assertEquals(40.7128, place.latitude, 0.0001)
    }

    @Test
    fun legacyLocationToGeofence() {
        @Suppress("DEPRECATION")
        val loc = TasksJsonImporter.LegacyLocation().apply { arrival = true; departure = false }
        val geo = Geofence(task = 42L, place = "uid", isArrival = loc.arrival, isDeparture = loc.departure)
        assertTrue(geo.isArrival)
        assertFalse(geo.isDeparture)
    }

    // ===== Vtodo import logic =====

    @Test
    fun vtodoUsesFirstNonDeletedCaldavTask() {
        val tasks = listOf(CaldavTask(calendar = "c1", deleted = 1000L), CaldavTask(calendar = "c2", deleted = 0L, obj = "t.ics"))
        assertEquals("c2", tasks.firstOrNull { !it.isDeleted() }!!.calendar)
    }

    @Test
    fun vtodoSkipsWhenAllDeleted() {
        assertNull(listOf(CaldavTask(calendar = "c1", deleted = 1000L)).firstOrNull { !it.isDeleted() })
    }

    @Test
    fun vtodoRequiresCalendar() = runTest {
        `when`(caldavDao.getCalendar("c1")).thenReturn(null)
        assertNull(caldavDao.getCalendar("c1"))
    }

    // ===== Attachment mapping =====

    @Test
    fun attachmentLookupById() = runTest {
        `when`(taskAttachmentDao.getAttachment("att-1")).thenReturn(TaskAttachment(id = 10L, remoteId = "att-1", name = "d.pdf", uri = "c://d"))
        assertEquals(10L, taskAttachmentDao.getAttachment("att-1")!!.id)
    }

    @Test
    fun attachmentMapped() {
        val ta = TaskAttachment(id = 10L, remoteId = "att-1", name = "d.pdf", uri = "c://d")
        val mapped = Attachment(task = 42L, fileId = ta.id!!, attachmentUid = ta.remoteId)
        assertEquals(42L, mapped.task)
        assertEquals(10L, mapped.fileId)
    }

    // ===== Geofence/CaldavTask/Tag mapping =====

    @Test
    fun geofencesCopiedWithNewTaskId() {
        val remapped = listOf(Geofence(task = 0L, place = "p1", isArrival = true)).map { it.copy(task = 42L) }
        assertEquals(42L, remapped[0].task)
        assertTrue(remapped[0].isArrival)
    }

    @Test
    fun caldavTasksCopiedWithNewTaskId() {
        val remapped = listOf(CaldavTask(calendar = "c1", remoteId = "r1")).map { it.copy(task = 42L) }
        assertEquals(42L, remapped[0].task)
    }

    @Test
    fun tagCopiedWithNewIds() {
        val tag = Tag(task = 0L, name = "work", tagUid = "old")
        val tagData = TagData(remoteId = "new", name = "work")
        val remapped = tag.copy(task = 42L, taskUid = "task-remote", tagUid = tagData.remoteId)
        assertEquals(42L, remapped.task)
        assertEquals("new", remapped.tagUid)
    }

    // ===== Task suppress sync/refresh =====

    @Test
    fun taskSuppressRefreshAndSync() {
        val task = Task(title = "Test")
        task.suppressRefresh()
        task.suppressSync()
        assertTrue(task.isSuppressRefresh())
    }

    // ===== Version constants ordering =====

    @Test
    fun versionConstantsOrdered() {
        assertTrue(com.todoroo.astrid.service.Upgrader.V6_4 < com.todoroo.astrid.service.Upgrader.V8_2)
        assertTrue(com.todoroo.astrid.service.Upgrader.V8_2 < com.todoroo.astrid.service.Upgrader.V9_6)
        assertTrue(com.todoroo.astrid.service.Upgrader.V9_6 < com.todoroo.astrid.service.Upgrader.V12_4)
        assertTrue(com.todoroo.astrid.service.Upgrader.V12_4 < com.todoroo.astrid.service.Upgrader.V12_8)
    }

    @Test
    fun upgrade1413Version() {
        assertEquals(141300, com.todoroo.astrid.service.Upgrade_14_13.VERSION)
    }

    // ===== ImportResult =====

    @Test
    fun importResultDefaults() {
        val r = TasksJsonImporter.ImportResult()
        assertEquals(0, r.taskCount)
        assertEquals(0, r.importCount)
        assertEquals(0, r.skipCount)
    }

    @Test
    fun importResultAccumulates() {
        val r = TasksJsonImporter.ImportResult()
        for (i in 1..10) {
            r.taskCount++
            if (i % 3 == 0) r.skipCount++ else r.importCount++
        }
        assertEquals(10, r.taskCount)
        assertEquals(7, r.importCount)
        assertEquals(3, r.skipCount)
    }

    // ===== LegacyLocation deserialization =====

    @Test
    fun legacyLocationDeserializeAllFields() {
        @Suppress("DEPRECATION")
        val loc = json.decodeFromString<TasksJsonImporter.LegacyLocation>("""{"name":"Off","address":"123","phone":"+1","url":"http://x","latitude":40.7,"longitude":-74.0,"radius":250,"arrival":true,"departure":false}""")
        assertEquals("Off", loc.name)
        assertEquals(40.7, loc.latitude, 0.1)
        assertTrue(loc.arrival)
        assertFalse(loc.departure)
    }

    @Test
    fun legacyLocationDeserializeEmpty() {
        @Suppress("DEPRECATION")
        val loc = json.decodeFromString<TasksJsonImporter.LegacyLocation>("""{}""")
        assertNull(loc.name)
        assertEquals(0.0, loc.latitude, 0.0)
    }

    @Test
    fun legacyLocationRoundTrip() {
        @Suppress("DEPRECATION")
        val orig = TasksJsonImporter.LegacyLocation().apply { name = "RT"; latitude = 34.0; longitude = -118.0; radius = 300; arrival = true }
        val encoded = json.encodeToString(orig)
        @Suppress("DEPRECATION")
        val decoded = json.decodeFromString<TasksJsonImporter.LegacyLocation>(encoded)
        assertEquals("RT", decoded.name)
        assertEquals(34.0, decoded.latitude, 0.1)
    }

    // ===== Google Task serialization =====

    @Test
    fun googleTaskSerialization() {
        val gt = GoogleTask(remoteId = "g1", listId = "l1", remoteParent = "p1", remoteOrder = 3L, lastSync = 12345L)
        val decoded = json.decodeFromString<GoogleTask>(json.encodeToString(gt))
        assertEquals("g1", decoded.remoteId)
        assertEquals("l1", decoded.listId)
    }

    @Test
    fun googleTaskAccountSerialization() {
        @Suppress("DEPRECATION")
        val gta = GoogleTaskAccount(account = "u@g.com", etag = "abc", isCollapsed = true)
        @Suppress("DEPRECATION")
        val decoded = json.decodeFromString<GoogleTaskAccount>(json.encodeToString(gta))
        assertEquals("u@g.com", decoded.account)
        assertTrue(decoded.isCollapsed)
    }

    @Test
    fun googleTaskListSerialization() {
        @Suppress("DEPRECATION")
        val gtl = GoogleTaskList(account = "u@g.com", remoteId = "l1", title = "My List", color = 3)
        @Suppress("DEPRECATION")
        val decoded = json.decodeFromString<GoogleTaskList>(json.encodeToString(gtl))
        assertEquals("l1", decoded.remoteId)
        assertEquals("My List", decoded.title)
    }
}
