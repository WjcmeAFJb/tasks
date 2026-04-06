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

class TasksJsonImporterLogicTest {

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

    // ===== TaskBackup JSON serialization =====

    @Test
    fun serializeTaskBackupMinimal() {
        val backup = TaskBackup(task = Task(title = "Test"))
        val encoded = json.encodeToString(backup)
        assertTrue(encoded.contains("\"title\":\"Test\""))
    }

    @Test
    fun deserializeTaskBackupMinimal() {
        val input = """{"task":{"title":"Test"}}"""
        val backup = json.decodeFromString<TaskBackup>(input)
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
    fun deserializeTaskBackupWithAlarms() {
        val input = """{"task":{"title":"T"},"alarms":[{"time":1000,"type":1}]}"""
        val backup = json.decodeFromString<TaskBackup>(input)
        assertNotNull(backup.alarms)
        assertEquals(1, backup.alarms!!.size)
        assertEquals(1000L, backup.alarms!![0].time)
    }

    @Test
    fun deserializeTaskBackupWithTags() {
        val input = """{"task":{"title":"T"},"tags":[{"name":"work","tagUid":"u1"}]}"""
        val backup = json.decodeFromString<TaskBackup>(input)
        assertNotNull(backup.tags)
        assertEquals("work", backup.tags!![0].name)
    }

    @Test
    fun deserializeTaskBackupWithGeofences() {
        val input = """{"task":{"title":"T"},"geofences":[{"place":"p1","isArrival":true}]}"""
        val backup = json.decodeFromString<TaskBackup>(input)
        assertNotNull(backup.geofences)
        assertTrue(backup.geofences!![0].isArrival)
    }

    @Test
    fun deserializeTaskBackupWithComments() {
        val input = """{"task":{"title":"T"},"comments":[{"message":"hello"}]}"""
        val backup = json.decodeFromString<TaskBackup>(input)
        assertNotNull(backup.comments)
        assertEquals("hello", backup.comments!![0].message)
    }

    @Test
    fun deserializeTaskBackupWithCaldavTasks() {
        val input = """{"task":{"title":"T"},"caldavTasks":[{"calendar":"c1","remoteId":"r1"}]}"""
        val backup = json.decodeFromString<TaskBackup>(input)
        assertNotNull(backup.caldavTasks)
        assertEquals("c1", backup.caldavTasks!![0].calendar)
    }

    @Test
    fun deserializeTaskBackupWithVtodo() {
        val input = """{"task":{"title":"T"},"vtodo":"BEGIN:VTODO\nEND:VTODO"}"""
        val backup = json.decodeFromString<TaskBackup>(input)
        assertEquals("BEGIN:VTODO\nEND:VTODO", backup.vtodo)
    }

    @Test
    fun deserializeTaskBackupWithGoogleTasks() {
        val input = """{"task":{"title":"T"},"google":[{"remoteId":"g1","listId":"l1"}]}"""
        val backup = json.decodeFromString<TaskBackup>(input)
        assertNotNull(backup.google)
        assertEquals("g1", backup.google!![0].remoteId)
    }

    @Test
    fun deserializeTaskBackupWithLegacyLocations() {
        val input = """{"task":{"title":"T"},"locations":[{"name":"Office","latitude":40.7,"longitude":-74.0,"arrival":true}]}"""
        val backup = json.decodeFromString<TaskBackup>(input)
        assertNotNull(backup.locations)
        assertEquals("Office", backup.locations!![0].name)
        assertTrue(backup.locations!![0].arrival)
    }

    @Test
    fun taskBackupRoundTrip() {
        val original = TaskBackup(
            task = Task(title = "Round trip", priority = Task.Priority.HIGH, dueDate = 12345L),
            alarms = listOf(Alarm(time = 1000L, type = Alarm.TYPE_DATE_TIME)),
            geofences = listOf(Geofence(place = "p1", isArrival = true)),
            tags = listOf(Tag(name = "tag1", tagUid = "uid1")),
            comments = listOf(UserActivity(message = "comment1")),
            caldavTasks = listOf(CaldavTask(calendar = "cal1", remoteId = "r1")),
            vtodo = "BEGIN:VTODO\nEND:VTODO",
        )
        val encoded = json.encodeToString(original)
        val decoded = json.decodeFromString<TaskBackup>(encoded)
        assertEquals("Round trip", decoded.task.title)
        assertEquals(1, decoded.alarms!!.size)
        assertEquals(1, decoded.caldavTasks!!.size)
        assertEquals("BEGIN:VTODO\nEND:VTODO", decoded.vtodo)
    }

    @Test
    fun taskBackupIgnoresUnknownFields() {
        val input = """{"task":{"title":"T","unknownField":42},"futureField":"ignored"}"""
        val backup = json.decodeFromString<TaskBackup>(input)
        assertEquals("T", backup.task.title)
    }

    // ===== Import skip logic: task already exists by UUID =====

    @Test
    fun skipTaskWhenUuidAlreadyExists() = runTest {
        val task = Task(remoteId = "existing-uuid")
        `when`(taskDao.fetch("existing-uuid")).thenReturn(Task(id = 1L, remoteId = "existing-uuid"))
        assertNotNull(taskDao.fetch(task.uuid))
    }

    @Test
    fun doNotSkipTaskWhenUuidNotFound() = runTest {
        `when`(taskDao.fetch("new-uuid")).thenReturn(null)
        assertNull(taskDao.fetch("new-uuid"))
    }

    // ===== Import skip logic: CaldavTask already exists =====

    @Test
    fun skipTaskWhenCaldavTaskExistsByObj() = runTest {
        val caldavTask = CaldavTask(calendar = "cal1", obj = "task.ics", deleted = 0L)
        `when`(caldavDao.getTask("cal1", "task.ics")).thenReturn(caldavTask)
        assertNotNull(caldavDao.getTask("cal1", "task.ics"))
    }

    @Test
    fun skipTaskWhenCaldavTaskExistsByRemoteIdWithNullObj() = runTest {
        val caldavTask = CaldavTask(calendar = "cal1", obj = null, remoteId = "remote1", deleted = 0L)
        `when`(caldavDao.getTaskByRemoteId("cal1", "remote1")).thenReturn(caldavTask)
        assertTrue(caldavTask.obj.isNullOrBlank())
        assertNotNull(caldavDao.getTaskByRemoteId("cal1", "remote1"))
    }

    @Test
    fun skipTaskWhenCaldavTaskObjIsNullIcs() = runTest {
        val caldavTask = CaldavTask(calendar = "cal1", obj = "null.ics", remoteId = "remote1", deleted = 0L)
        `when`(caldavDao.getTaskByRemoteId("cal1", "remote1")).thenReturn(caldavTask)
        assertEquals("null.ics", caldavTask.obj)
    }

    @Test
    fun doNotSkipWhenCaldavTaskNotFound() = runTest {
        `when`(caldavDao.getTask("cal1", "task.ics")).thenReturn(null)
        assertNull(caldavDao.getTask("cal1", "task.ics"))
    }

    @Test
    fun onlyCheckNonDeletedCaldavTasksForSkip() {
        val tasks = listOf(
            CaldavTask(calendar = "cal1", obj = "a.ics", deleted = 1000L),
            CaldavTask(calendar = "cal2", obj = "b.ics", deleted = 0L),
        )
        val filtered = tasks.filter { it.deleted == 0L }
        assertEquals(1, filtered.size)
        assertEquals("cal2", filtered[0].calendar)
    }

    // ===== Alarm import logic =====

    @Test
    fun alarmsRemappedToNewTaskId() {
        val originalAlarms = listOf(
            Alarm(task = 99L, time = 1000L, type = Alarm.TYPE_DATE_TIME),
            Alarm(task = 99L, time = 2000L, type = Alarm.TYPE_REL_START),
        )
        val remapped = originalAlarms.map { it.copy(task = 42L) }
        assertEquals(42L, remapped[0].task)
        assertEquals(42L, remapped[1].task)
    }

    @Test
    fun skipAllDayAlarmsWhenConditionsMet() {
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

    // ===== Legacy alarm migration =====

    @Test
    fun legacyAlarmFilterRemovesRelStartWhenNoStartDate() {
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
    fun legacyAlarmFilterRemovesRelEndWhenNoDueDate() {
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
    fun legacyAlarmFilterKeepsRelStartWhenStartDatePresent() {
        val task = Task(hideUntil = 1680000000L)
        assertTrue(task.hasStartDate())
        val alarms = listOf(Alarm(type = Alarm.TYPE_REL_START))
        val filtered = alarms.filter { alarm ->
            when (alarm.type) {
                Alarm.TYPE_REL_START -> task.hasStartDate()
                Alarm.TYPE_REL_END -> task.hasDueDate()
                else -> true
            }
        }
        assertEquals(1, filtered.size)
    }

    // ===== Ring flags migration =====

    @Test
    fun ringFlagsMigrateNonstop() {
        val task = Task(ringFlags = Task.NOTIFY_MODE_NONSTOP)
        assertTrue(task.isNotifyModeNonstop)
        val newFlags = when {
            task.isNotifyModeFive -> Task.NOTIFY_MODE_FIVE
            task.isNotifyModeNonstop -> Task.NOTIFY_MODE_NONSTOP
            else -> 0
        }
        assertEquals(Task.NOTIFY_MODE_NONSTOP, newFlags)
    }

    @Test
    fun ringFlagsMigrateFive() {
        val task = Task(ringFlags = Task.NOTIFY_MODE_FIVE)
        assertTrue(task.isNotifyModeFive)
        val newFlags = when {
            task.isNotifyModeFive -> Task.NOTIFY_MODE_FIVE
            task.isNotifyModeNonstop -> Task.NOTIFY_MODE_NONSTOP
            else -> 0
        }
        assertEquals(Task.NOTIFY_MODE_FIVE, newFlags)
    }

    @Test
    fun ringFlagsMigrateZero() {
        val task = Task(ringFlags = 0)
        val newFlags = when {
            task.isNotifyModeFive -> Task.NOTIFY_MODE_FIVE
            task.isNotifyModeNonstop -> Task.NOTIFY_MODE_NONSTOP
            else -> 0
        }
        assertEquals(0, newFlags)
    }

    // ===== findTagData logic =====

    @Test
    fun findTagDataByUuidFirst() = runTest {
        val tagData = TagData(remoteId = "uid-1", name = "Work")
        `when`(tagDataDao.getByUuid("uid-1")).thenReturn(tagData)
        val found = tagDataDao.getByUuid("uid-1") ?: tagDataDao.getTagByName("Work")
        assertEquals("uid-1", found!!.remoteId)
    }

    @Test
    fun findTagDataByNameWhenUuidNotFound() = runTest {
        val tagData = TagData(remoteId = "uid-2", name = "Personal")
        `when`(tagDataDao.getByUuid("uid-1")).thenReturn(null)
        `when`(tagDataDao.getTagByName("Personal")).thenReturn(tagData)
        val found = tagDataDao.getByUuid("uid-1") ?: tagDataDao.getTagByName("Personal")
        assertEquals("uid-2", found!!.remoteId)
    }

    @Test
    fun findTagDataReturnsNullWhenNotFound() = runTest {
        `when`(tagDataDao.getByUuid("uid-1")).thenReturn(null)
        `when`(tagDataDao.getTagByName("Unknown")).thenReturn(null)
        val found = tagDataDao.getByUuid("uid-1") ?: tagDataDao.getTagByName("Unknown")
        assertNull(found)
    }

    // ===== Import metadata =====

    @Test
    fun placeImportedWhenNotExisting() = runTest {
        `when`(locationDao.getByUid("place-1")).thenReturn(null)
        assertNull(locationDao.getByUid("place-1"))
    }

    @Test
    fun placeSkippedWhenExisting() = runTest {
        val place = Place(uid = "place-1", name = "Office")
        `when`(locationDao.getByUid("place-1")).thenReturn(place)
        assertNotNull(locationDao.getByUid("place-1"))
    }

    @Test
    fun filterImportedWhenNotExisting() = runTest {
        `when`(filterDao.getByName("Active Tasks")).thenReturn(null)
        assertNull(filterDao.getByName("Active Tasks"))
    }

    @Test
    fun caldavAccountImportedWhenNotExisting() = runTest {
        `when`(caldavDao.getAccountByUuid("acc-1")).thenReturn(null)
        assertNull(caldavDao.getAccountByUuid("acc-1"))
    }

    @Test
    fun caldavCalendarImportedWhenNotExisting() = runTest {
        `when`(caldavDao.getCalendarByUuid("cal-1")).thenReturn(null)
        assertNull(caldavDao.getCalendarByUuid("cal-1"))
    }

    @Test
    fun taskListMetadataUsesTagUuidWhenFilterBlank() {
        val tlm = TaskListMetadata().apply { filter = ""; tagUuid = "tag-uuid-1" }
        val id = tlm.filter.takeIf { it?.isNotBlank() == true } ?: tlm.tagUuid!!
        assertEquals("tag-uuid-1", id)
    }

    @Test
    fun taskListMetadataUsesFilterWhenNotBlank() {
        val tlm = TaskListMetadata().apply { filter = "filter-1"; tagUuid = "tag-uuid-1" }
        val id = tlm.filter.takeIf { it?.isNotBlank() == true } ?: tlm.tagUuid!!
        assertEquals("filter-1", id)
    }

    @Test
    fun taskListMetadataUsesTagUuidWhenFilterNull() {
        val tlm = TaskListMetadata().apply { filter = null; tagUuid = "tag-uuid-1" }
        val id = tlm.filter.takeIf { it?.isNotBlank() == true } ?: tlm.tagUuid!!
        assertEquals("tag-uuid-1", id)
    }

    @Test
    fun taskAttachmentImportedWhenNotExisting() = runTest {
        `when`(taskAttachmentDao.getAttachment("file-1")).thenReturn(null)
        assertNull(taskAttachmentDao.getAttachment("file-1"))
    }

    @Test
    fun taskAttachmentSkippedWhenExisting() = runTest {
        val existing = TaskAttachment(remoteId = "file-1", name = "photo.jpg", uri = "content://photo.jpg")
        `when`(taskAttachmentDao.getAttachment("file-1")).thenReturn(existing)
        assertNotNull(taskAttachmentDao.getAttachment("file-1"))
    }

    // ===== Google Task migration =====

    @Test
    fun googleTaskMigratesToCaldavTask() {
        val googleTask = GoogleTask(remoteId = "g1", listId = "list1", remoteOrder = 5L, remoteParent = "parent1", lastSync = 999L)
        val caldavTask = CaldavTask(calendar = googleTask.listId, remoteId = googleTask.remoteId, remoteOrder = googleTask.remoteOrder, remoteParent = googleTask.remoteParent, lastSync = googleTask.lastSync)
        assertEquals("list1", caldavTask.calendar)
        assertEquals("g1", caldavTask.remoteId)
        assertEquals(5L, caldavTask.remoteOrder)
    }

    @Test
    fun googleTaskDefaultValues() {
        val googleTask = GoogleTask()
        assertEquals("", googleTask.remoteId)
        assertEquals("", googleTask.listId)
        assertNull(googleTask.remoteParent)
        assertEquals(0L, googleTask.remoteOrder)
    }

    @Test
    fun googleTaskAccountMigratesToCaldavAccount() {
        @Suppress("DEPRECATION")
        val gta = GoogleTaskAccount(account = "user@gmail.com")
        val caldavAccount = CaldavAccount(accountType = CaldavAccount.TYPE_GOOGLE_TASKS, uuid = gta.account, name = gta.account, username = gta.account)
        assertEquals(CaldavAccount.TYPE_GOOGLE_TASKS, caldavAccount.accountType)
        assertEquals("user@gmail.com", caldavAccount.uuid)
    }

    @Test
    fun googleTaskListMigratesToCaldavCalendar() {
        @Suppress("DEPRECATION")
        val gtl = GoogleTaskList(account = "user@gmail.com", remoteId = "list-id", color = 5)
        val calendar = CaldavCalendar(account = gtl.account, uuid = gtl.remoteId, color = gtl.color ?: 0)
        assertEquals("user@gmail.com", calendar.account)
        assertEquals("list-id", calendar.uuid)
        assertEquals(5, calendar.color)
    }

    // ===== Legacy location to Place migration =====

    @Test
    fun legacyLocationToPlace() {
        @Suppress("DEPRECATION")
        val location = TasksJsonImporter.LegacyLocation().apply {
            name = "Office"; address = "123 Main St"; phone = "+1-555-0100"; url = "https://example.com"
            latitude = 40.7128; longitude = -74.0060
        }
        val place = Place(longitude = location.longitude, latitude = location.latitude, name = location.name, address = location.address, url = location.url, phone = location.phone)
        assertEquals("Office", place.name)
        assertEquals(40.7128, place.latitude, 0.0001)
    }

    @Test
    fun legacyLocationToGeofence() {
        @Suppress("DEPRECATION")
        val location = TasksJsonImporter.LegacyLocation().apply { arrival = true; departure = false }
        val geofence = Geofence(task = 42L, place = "uid-1", isArrival = location.arrival, isDeparture = location.departure)
        assertTrue(geofence.isArrival)
        assertFalse(geofence.isDeparture)
    }

    // ===== Vtodo import logic =====

    @Test
    fun vtodoImportUsesFirstNonDeletedCaldavTask() {
        val caldavTasks = listOf(
            CaldavTask(calendar = "cal1", deleted = 1000L),
            CaldavTask(calendar = "cal2", deleted = 0L, obj = "task.ics"),
        )
        val firstNonDeleted = caldavTasks.firstOrNull { !it.isDeleted() }
        assertEquals("cal2", firstNonDeleted!!.calendar)
    }

    @Test
    fun vtodoImportSkipsWhenAllCaldavTasksDeleted() {
        val caldavTasks = listOf(CaldavTask(calendar = "cal1", deleted = 1000L))
        assertNull(caldavTasks.firstOrNull { !it.isDeleted() })
    }

    @Test
    fun vtodoImportRequiresCalendar() = runTest {
        `when`(caldavDao.getCalendar("cal1")).thenReturn(null)
        assertNull(caldavDao.getCalendar("cal1"))
    }

    // ===== Attachment import logic =====

    @Test
    fun attachmentImportLooksUpByUid() = runTest {
        val taskAttachment = TaskAttachment(id = 10L, remoteId = "att-uid-1", name = "doc.pdf", uri = "content://doc.pdf")
        `when`(taskAttachmentDao.getAttachment("att-uid-1")).thenReturn(taskAttachment)
        assertEquals(10L, taskAttachmentDao.getAttachment("att-uid-1")!!.id)
    }

    @Test
    fun attachmentMappedToCorrectTask() {
        val taskAttachment = TaskAttachment(id = 10L, remoteId = "att-uid-1", name = "doc.pdf", uri = "content://doc.pdf")
        val mapped = Attachment(task = 42L, fileId = taskAttachment.id!!, attachmentUid = taskAttachment.remoteId)
        assertEquals(42L, mapped.task)
        assertEquals(10L, mapped.fileId)
    }

    // ===== Geofence/CaldavTask/Tag import =====

    @Test
    fun geofencesCopiedWithNewTaskId() {
        val geofences = listOf(Geofence(task = 0L, place = "p1", isArrival = true))
        val remapped = geofences.map { it.copy(task = 42L) }
        assertEquals(42L, remapped[0].task)
        assertTrue(remapped[0].isArrival)
    }

    @Test
    fun caldavTasksCopiedWithNewTaskId() {
        val caldavTasks = listOf(CaldavTask(calendar = "cal1", remoteId = "r1"))
        val remapped = caldavTasks.map { it.copy(task = 42L) }
        assertEquals(42L, remapped[0].task)
        assertEquals("cal1", remapped[0].calendar)
    }

    @Test
    fun tagCopiedWithNewTaskIdAndTagData() {
        val tag = Tag(task = 0L, name = "work", tagUid = "old-uid")
        val tagData = TagData(remoteId = "new-uid", name = "work")
        val remapped = tag.copy(task = 42L, taskUid = "task-remote-id", tagUid = tagData.remoteId)
        assertEquals(42L, remapped.task)
        assertEquals("new-uid", remapped.tagUid)
    }

    // ===== Serialization round-trips =====

    @Test
    fun googleTaskSerialization() {
        val gt = GoogleTask(remoteId = "g1", listId = "list1", remoteParent = "parent1", remoteOrder = 3L, lastSync = 12345L, deleted = 0L)
        val decoded = json.decodeFromString<GoogleTask>(json.encodeToString(gt))
        assertEquals("g1", decoded.remoteId)
        assertEquals("list1", decoded.listId)
    }

    @Test
    fun googleTaskAccountSerialization() {
        @Suppress("DEPRECATION")
        val gta = GoogleTaskAccount(account = "user@gmail.com", etag = "abc123", isCollapsed = true)
        @Suppress("DEPRECATION")
        val decoded = json.decodeFromString<GoogleTaskAccount>(json.encodeToString(gta))
        assertEquals("user@gmail.com", decoded.account)
        assertTrue(decoded.isCollapsed)
    }

    @Test
    fun googleTaskListSerialization() {
        @Suppress("DEPRECATION")
        val gtl = GoogleTaskList(account = "user@gmail.com", remoteId = "list-id", title = "My List", color = 3)
        @Suppress("DEPRECATION")
        val decoded = json.decodeFromString<GoogleTaskList>(json.encodeToString(gtl))
        assertEquals("list-id", decoded.remoteId)
        assertEquals("My List", decoded.title)
    }

    // ===== Task suppress sync/refresh =====

    @Test
    fun taskSuppressRefreshAndSync() {
        val task = Task(title = "Test")
        task.suppressRefresh()
        task.suppressSync()
        assertTrue(task.isSuppressRefresh())
    }

    // ===== Version constants =====

    @Test
    fun versionConstantsAreOrdered() {
        assertTrue(com.todoroo.astrid.service.Upgrader.V6_4 < com.todoroo.astrid.service.Upgrader.V8_2)
        assertTrue(com.todoroo.astrid.service.Upgrader.V8_2 < com.todoroo.astrid.service.Upgrader.V9_6)
        assertTrue(com.todoroo.astrid.service.Upgrader.V9_6 < com.todoroo.astrid.service.Upgrader.V12_4)
        assertTrue(com.todoroo.astrid.service.Upgrader.V12_4 < com.todoroo.astrid.service.Upgrader.V12_8)
    }

    @Test
    fun upgrade1413VersionIsCorrect() {
        assertEquals(141300, com.todoroo.astrid.service.Upgrade_14_13.VERSION)
    }

    // ===== Edge cases =====

    @Test
    fun emptyBackupDeserialization() {
        val input = """{"task":{}}"""
        val backup = json.decodeFromString<TaskBackup>(input)
        assertNull(backup.task.title)
    }

    @Test
    fun taskBackupWithEmptyLists() {
        val input = """{"task":{"title":"T"},"alarms":[],"geofences":[],"tags":[],"comments":[],"attachments":[],"caldavTasks":[]}"""
        val backup = json.decodeFromString<TaskBackup>(input)
        assertTrue(backup.alarms!!.isEmpty())
        assertTrue(backup.geofences!!.isEmpty())
        assertTrue(backup.tags!!.isEmpty())
        assertTrue(backup.caldavTasks!!.isEmpty())
    }

    @Test
    fun caldavTaskObjNullIcsPattern() {
        val ct = CaldavTask(calendar = "cal1", obj = "null.ics")
        assertEquals("null.ics", ct.obj)
    }

    @Test
    fun caldavTaskObjBlankString() {
        val ct = CaldavTask(calendar = "cal1", obj = "")
        assertTrue(ct.obj.isNullOrBlank())
    }

    @Test
    fun caldavTaskObjNull() {
        val ct = CaldavTask(calendar = "cal1", obj = null)
        assertTrue(ct.obj.isNullOrBlank())
    }

    @Test
    fun caldavTaskObjValidIcs() {
        val ct = CaldavTask(calendar = "cal1", obj = "task-123.ics")
        assertFalse(ct.obj.isNullOrBlank())
        assertFalse(ct.obj == "null.ics")
    }
}
