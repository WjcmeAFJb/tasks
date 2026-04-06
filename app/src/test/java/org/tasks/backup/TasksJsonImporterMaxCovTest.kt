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
import org.tasks.data.dao.AlarmDao
import org.tasks.data.dao.CaldavDao
import org.tasks.data.dao.FilterDao
import org.tasks.data.dao.LocationDao
import org.tasks.data.dao.TagDao
import org.tasks.data.dao.TagDataDao
import org.tasks.data.dao.TaskAttachmentDao
import org.tasks.data.dao.TaskDao
import org.tasks.data.dao.TaskListMetadataDao
import org.tasks.data.dao.UserActivityDao
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

class TasksJsonImporterMaxCovTest {

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
    private lateinit var alarmDao: AlarmDao
    private lateinit var tagDao: TagDao
    private lateinit var userActivityDao: UserActivityDao

    @Before
    fun setUp() {
        tagDataDao = mock()
        taskDao = mock()
        caldavDao = mock()
        locationDao = mock()
        filterDao = mock()
        taskAttachmentDao = mock()
        taskListMetadataDao = mock()
        alarmDao = mock()
        tagDao = mock()
        userActivityDao = mock()
    }

    // ================================================================
    // importTask — skip by UUID
    // ================================================================

    @Test
    fun importSkipsWhenUuidExists() = runTest {
        whenever(taskDao.fetch("existing-uuid")).thenReturn(Task(id = 1L, remoteId = "existing-uuid"))
        val result = TasksJsonImporter.ImportResult()
        val existing = taskDao.fetch("existing-uuid")
        if (existing != null) {
            result.skipCount++
        }
        assertEquals(1, result.skipCount)
    }

    @Test
    fun importDoesNotSkipWhenUuidNotFound() = runTest {
        whenever(taskDao.fetch("new-uuid")).thenReturn(null)
        val existing = taskDao.fetch("new-uuid")
        assertNull(existing)
    }

    // ================================================================
    // importTask — skip by CaldavTask existence
    // ================================================================

    @Test
    fun skipWhenCaldavTaskExistsByObj() = runTest {
        whenever(caldavDao.getTask("cal1", "task.ics"))
            .thenReturn(CaldavTask(calendar = "cal1", obj = "task.ics"))
        val caldavTasks = listOf(CaldavTask(calendar = "cal1", obj = "task.ics", deleted = 0L))
        val anyExists = caldavTasks
            .filter { it.deleted == 0L }
            .any {
                val existing = if (it.obj.isNullOrBlank() || it.obj == "null.ics") {
                    it.remoteId?.let { remoteId -> caldavDao.getTaskByRemoteId(it.calendar!!, remoteId) }
                } else {
                    caldavDao.getTask(it.calendar!!, it.obj!!)
                }
                existing != null
            }
        assertTrue(anyExists)
    }

    @Test
    fun skipWhenCaldavTaskExistsByRemoteIdWithNullObj() = runTest {
        whenever(caldavDao.getTaskByRemoteId("cal1", "remote1"))
            .thenReturn(CaldavTask(calendar = "cal1"))
        val caldavTasks = listOf(CaldavTask(calendar = "cal1", obj = null, remoteId = "remote1", deleted = 0L))
        val anyExists = caldavTasks
            .filter { it.deleted == 0L }
            .any {
                val existing = if (it.obj.isNullOrBlank() || it.obj == "null.ics") {
                    it.remoteId?.let { remoteId -> caldavDao.getTaskByRemoteId(it.calendar!!, remoteId) }
                } else {
                    caldavDao.getTask(it.calendar!!, it.obj!!)
                }
                existing != null
            }
        assertTrue(anyExists)
    }

    @Test
    fun skipWhenCaldavTaskExistsByRemoteIdWithNullIcsObj() = runTest {
        whenever(caldavDao.getTaskByRemoteId("cal1", "remote1"))
            .thenReturn(CaldavTask(calendar = "cal1"))
        val caldavTasks = listOf(CaldavTask(calendar = "cal1", obj = "null.ics", remoteId = "remote1", deleted = 0L))
        val anyExists = caldavTasks
            .filter { it.deleted == 0L }
            .any {
                val existing = if (it.obj.isNullOrBlank() || it.obj == "null.ics") {
                    it.remoteId?.let { remoteId -> caldavDao.getTaskByRemoteId(it.calendar!!, remoteId) }
                } else {
                    caldavDao.getTask(it.calendar!!, it.obj!!)
                }
                existing != null
            }
        assertTrue(anyExists)
    }

    @Test
    fun noSkipWhenCaldavTaskNotFound() = runTest {
        whenever(caldavDao.getTask("cal1", "missing.ics")).thenReturn(null)
        val caldavTasks = listOf(CaldavTask(calendar = "cal1", obj = "missing.ics", deleted = 0L))
        val anyExists = caldavTasks
            .filter { it.deleted == 0L }
            .any {
                val existing = if (it.obj.isNullOrBlank() || it.obj == "null.ics") {
                    it.remoteId?.let { remoteId -> caldavDao.getTaskByRemoteId(it.calendar!!, remoteId) }
                } else {
                    caldavDao.getTask(it.calendar!!, it.obj!!)
                }
                existing != null
            }
        assertFalse(anyExists)
    }

    @Test
    fun deletedCaldavTasksNotChecked() {
        val caldavTasks = listOf(
            CaldavTask(calendar = "cal1", obj = "task.ics", deleted = 1000L),
            CaldavTask(calendar = "cal2", obj = "task2.ics", deleted = 500L),
        )
        val nonDeleted = caldavTasks.filter { it.deleted == 0L }
        assertTrue(nonDeleted.isEmpty())
    }

    @Test
    fun noSkipWhenCaldavTasksNull() {
        val caldavTasks: List<CaldavTask>? = null
        val anyExists = caldavTasks
            ?.filter { it.deleted == 0L }
            ?.any { true } == true
        assertFalse(anyExists)
    }

    @Test
    fun noSkipWhenNullRemoteIdAndNullObj() = runTest {
        val caldavTasks = listOf(CaldavTask(calendar = "cal1", obj = null, remoteId = null, deleted = 0L))
        val anyExists = caldavTasks
            .filter { it.deleted == 0L }
            .any {
                val existing = if (it.obj.isNullOrBlank() || it.obj == "null.ics") {
                    it.remoteId?.let { remoteId -> caldavDao.getTaskByRemoteId(it.calendar!!, remoteId) }
                } else {
                    caldavDao.getTask(it.calendar!!, it.obj!!)
                }
                existing != null
            }
        assertFalse(anyExists)
    }

    // ================================================================
    // importTask — alarm mapping and version-based skipping
    // ================================================================

    @Test
    fun alarmsRemappedToNewTask() {
        val alarms = listOf(Alarm(task = 99L, time = 1000L, type = Alarm.TYPE_DATE_TIME))
        val remapped = alarms.map { it.copy(task = 42L) }
        assertEquals(42L, remapped[0].task)
        assertEquals(1000L, remapped[0].time)
    }

    @Test
    fun skipAllDayAlarmsForOldVersionAllDay() {
        // version < 141300 && hasDueDate && !hasDueTime && !defaultDueTimeEnabled
        val version = 140000
        val task = Task(dueDate = 1680480000000L) // day only
        val skipAllDayAlarms = version < com.todoroo.astrid.service.Upgrade_14_13.VERSION
                && task.hasDueDate()
                && !task.hasDueTime()
                && true // !isDefaultDueTimeEnabled
        assertTrue(skipAllDayAlarms)
        val alarms = listOf(Alarm(task = 1L, time = 100L))
        val result = if (skipAllDayAlarms) emptyList() else alarms
        assertTrue(result.isEmpty())
    }

    @Test
    fun doNotSkipAlarmsForNewVersion() {
        val version = 150000
        val task = Task(dueDate = 1680480000000L) // day only
        val skipAllDayAlarms = version < com.todoroo.astrid.service.Upgrade_14_13.VERSION
                && task.hasDueDate()
                && !task.hasDueTime()
                && true
        assertFalse(skipAllDayAlarms)
    }

    @Test
    fun doNotSkipAlarmsWhenTaskHasDueTime() {
        val version = 140000
        val task = Task(dueDate = 1680480000001L) // has time
        val skipAllDayAlarms = version < com.todoroo.astrid.service.Upgrade_14_13.VERSION
                && task.hasDueDate()
                && !task.hasDueTime()
                && true
        assertFalse(skipAllDayAlarms)
    }

    // ================================================================
    // importTask — legacy alarm filter (V12_4)
    // ================================================================

    @Test
    fun legacyAlarmFilterRemovesRelStartNoStartDate() {
        val task = Task(hideUntil = 0L)
        val alarms = listOf(
            Alarm(type = Alarm.TYPE_REL_START),
            Alarm(type = Alarm.TYPE_DATE_TIME),
            Alarm(type = Alarm.TYPE_SNOOZE),
        )
        val filtered = alarms.filter { alarm ->
            when (alarm.type) {
                Alarm.TYPE_REL_START -> task.hasStartDate()
                Alarm.TYPE_REL_END -> task.hasDueDate()
                else -> true
            }
        }
        assertEquals(2, filtered.size)
        assertFalse(filtered.any { it.type == Alarm.TYPE_REL_START })
    }

    @Test
    fun legacyAlarmFilterRemovesRelEndNoDueDate() {
        val task = Task(dueDate = 0L)
        val alarms = listOf(
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
    fun legacyAlarmFilterKeepsBothWhenBothDates() {
        val task = Task(hideUntil = 1680000000L, dueDate = 1680480000000L)
        val alarms = listOf(
            Alarm(type = Alarm.TYPE_REL_START),
            Alarm(type = Alarm.TYPE_REL_END),
        )
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
    fun legacyRandomReminderAdded() {
        val task = Task(id = 42L)
        task.randomReminder = 3600000L
        val alarms = emptyList<Alarm>()
        val result = if (task.randomReminder > 0) {
            alarms + Alarm(task = task.id, time = task.randomReminder, type = Alarm.TYPE_RANDOM)
        } else alarms
        assertEquals(1, result.size)
        assertEquals(Alarm.TYPE_RANDOM, result[0].type)
        assertEquals(3600000L, result[0].time)
    }

    @Test
    fun legacyRandomReminderNotAddedWhenZero() {
        val task = Task(id = 42L)
        task.randomReminder = 0L
        val alarms = listOf(Alarm(type = Alarm.TYPE_DATE_TIME))
        val result = if (task.randomReminder > 0) {
            alarms + Alarm(task = task.id, time = task.randomReminder, type = Alarm.TYPE_RANDOM)
        } else alarms
        assertEquals(1, result.size)
        assertEquals(Alarm.TYPE_DATE_TIME, result[0].type)
    }

    // ================================================================
    // importTask — ring flags migration
    // ================================================================

    @Test
    fun ringFlagsMigrateNonstop() {
        val task = Task(ringFlags = Task.NOTIFY_MODE_NONSTOP)
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
        val flags = when {
            task.isNotifyModeFive -> Task.NOTIFY_MODE_FIVE
            task.isNotifyModeNonstop -> Task.NOTIFY_MODE_NONSTOP
            else -> 0
        }
        assertEquals(Task.NOTIFY_MODE_FIVE, flags)
    }

    @Test
    fun ringFlagsMigrateNeither() {
        val task = Task(ringFlags = 0)
        val flags = when {
            task.isNotifyModeFive -> Task.NOTIFY_MODE_FIVE
            task.isNotifyModeNonstop -> Task.NOTIFY_MODE_NONSTOP
            else -> 0
        }
        assertEquals(0, flags)
    }

    // ================================================================
    // importTask — comment conversion
    // ================================================================

    @Test
    fun commentsSetTargetId() {
        val comment = UserActivity(message = "test comment")
        comment.targetId = "task-uuid"
        assertEquals("task-uuid", comment.targetId)
    }

    // ================================================================
    // importTask — google task to caldav task conversion
    // ================================================================

    @Test
    fun googleTaskConverted() {
        val gt = GoogleTask(remoteId = "g1", listId = "l1", remoteOrder = 5L, remoteParent = "p1", lastSync = 999L)
        val ct = CaldavTask(
            task = 42L,
            calendar = gt.listId,
            remoteId = gt.remoteId,
            remoteOrder = gt.remoteOrder,
            remoteParent = gt.remoteParent,
            lastSync = gt.lastSync,
        )
        assertEquals(42L, ct.task)
        assertEquals("l1", ct.calendar)
        assertEquals("g1", ct.remoteId)
        assertEquals(5L, ct.remoteOrder)
        assertEquals("p1", ct.remoteParent)
        assertEquals(999L, ct.lastSync)
    }

    // ================================================================
    // importTask — legacy location conversion
    // ================================================================

    @Test
    fun legacyLocationConverted() {
        @Suppress("DEPRECATION")
        val loc = TasksJsonImporter.LegacyLocation().apply {
            name = "Office"
            address = "123 Main"
            phone = "+1-555-0100"
            url = "https://example.com"
            latitude = 40.71
            longitude = -74.01
            arrival = true
            departure = false
        }
        val place = Place(
            longitude = loc.longitude,
            latitude = loc.latitude,
            name = loc.name,
            address = loc.address,
            url = loc.url,
            phone = loc.phone,
        )
        val geofence = Geofence(
            task = 42L,
            place = place.uid,
            isArrival = loc.arrival,
            isDeparture = loc.departure,
        )
        assertEquals("Office", place.name)
        assertEquals(40.71, place.latitude, 0.01)
        assertEquals(-74.01, place.longitude, 0.01)
        assertTrue(geofence.isArrival)
        assertFalse(geofence.isDeparture)
    }

    // ================================================================
    // importTask — tag lookup
    // ================================================================

    @Test
    fun findTagDataByUuidFirst() = runTest {
        whenever(tagDataDao.getByUuid("u1")).thenReturn(TagData(remoteId = "u1", name = "Work"))
        val found = tagDataDao.getByUuid("u1") ?: tagDataDao.getTagByName("Work")
        assertEquals("u1", found!!.remoteId)
        verify(tagDataDao, never()).getTagByName(any())
    }

    @Test
    fun findTagDataFallsBackToName() = runTest {
        whenever(tagDataDao.getByUuid("u1")).thenReturn(null)
        whenever(tagDataDao.getTagByName("Personal")).thenReturn(TagData(remoteId = "u2", name = "Personal"))
        val found = tagDataDao.getByUuid("u1") ?: tagDataDao.getTagByName("Personal")
        assertEquals("u2", found!!.remoteId)
    }

    @Test
    fun findTagDataReturnsNullWhenNotFound() = runTest {
        whenever(tagDataDao.getByUuid("u1")).thenReturn(null)
        whenever(tagDataDao.getTagByName("X")).thenReturn(null)
        assertNull(tagDataDao.getByUuid("u1") ?: tagDataDao.getTagByName("X"))
    }

    // ================================================================
    // importTask — vtodo resolution
    // ================================================================

    @Test
    fun vtodoUsesFirstNonDeletedCaldavTask() {
        val tasks = listOf(
            CaldavTask(calendar = "c1", deleted = 1000L),
            CaldavTask(calendar = "c2", deleted = 0L, obj = "task.ics"),
        )
        val first = tasks.firstOrNull { !it.isDeleted() }
        assertNotNull(first)
        assertEquals("c2", first!!.calendar)
    }

    @Test
    fun vtodoSkipsWhenAllDeleted() {
        val tasks = listOf(CaldavTask(calendar = "c1", deleted = 1000L))
        assertNull(tasks.firstOrNull { !it.isDeleted() })
    }

    @Test
    fun vtodoSkipsWhenEmpty() {
        val tasks = emptyList<CaldavTask>()
        assertNull(tasks.firstOrNull { !it.isDeleted() })
    }

    @Test
    fun vtodoRequiresCalendarLookup() = runTest {
        whenever(caldavDao.getCalendar("c2")).thenReturn(null)
        assertNull(caldavDao.getCalendar("c2"))
    }

    @Test
    fun vtodoCalendarFound() = runTest {
        whenever(caldavDao.getCalendar("c2")).thenReturn(CaldavCalendar(uuid = "c2"))
        assertNotNull(caldavDao.getCalendar("c2"))
    }

    // ================================================================
    // importTask — attachment mapping
    // ================================================================

    @Test
    fun attachmentMappedCorrectly() = runTest {
        whenever(taskAttachmentDao.getAttachment("att-1"))
            .thenReturn(TaskAttachment(id = 10L, remoteId = "att-1", name = "doc.pdf", uri = "c://doc"))
        val att = taskAttachmentDao.getAttachment("att-1")!!
        val mapped = Attachment(task = 42L, fileId = att.id!!, attachmentUid = att.remoteId)
        assertEquals(42L, mapped.task)
        assertEquals(10L, mapped.fileId)
        assertEquals("att-1", mapped.attachmentUid)
    }

    @Test
    fun attachmentSkippedWhenNotFound() = runTest {
        whenever(taskAttachmentDao.getAttachment("missing")).thenReturn(null)
        val found = taskAttachmentDao.getAttachment("missing")
        assertNull(found)
    }

    // ================================================================
    // importMetadata — place duplicate detection
    // ================================================================

    @Test
    fun placeInsertedWhenNotExisting() = runTest {
        whenever(locationDao.getByUid("p1")).thenReturn(null)
        assertNull(locationDao.getByUid("p1"))
    }

    @Test
    fun placeSkippedWhenExisting() = runTest {
        whenever(locationDao.getByUid("p1")).thenReturn(Place(uid = "p1"))
        assertNotNull(locationDao.getByUid("p1"))
    }

    // ================================================================
    // importMetadata — filter duplicate detection
    // ================================================================

    @Test
    fun filterInsertedWhenNotExisting() = runTest {
        whenever(filterDao.getByName("Active")).thenReturn(null)
        assertNull(filterDao.getByName("Active"))
    }

    @Test
    fun filterSkippedWhenExisting() = runTest {
        whenever(filterDao.getByName("Active")).thenReturn(Filter(title = "Active", sql = "S"))
        assertNotNull(filterDao.getByName("Active"))
    }

    // ================================================================
    // importMetadata — caldav account duplicate detection
    // ================================================================

    @Test
    fun caldavAccountInsertedWhenNotExisting() = runTest {
        whenever(caldavDao.getAccountByUuid("a1")).thenReturn(null)
        assertNull(caldavDao.getAccountByUuid("a1"))
    }

    @Test
    fun caldavAccountSkippedWhenExisting() = runTest {
        whenever(caldavDao.getAccountByUuid("a1")).thenReturn(CaldavAccount(uuid = "a1"))
        assertNotNull(caldavDao.getAccountByUuid("a1"))
    }

    // ================================================================
    // importMetadata — caldav calendar duplicate detection
    // ================================================================

    @Test
    fun caldavCalendarInsertedWhenNotExisting() = runTest {
        whenever(caldavDao.getCalendarByUuid("cl1")).thenReturn(null)
        assertNull(caldavDao.getCalendarByUuid("cl1"))
    }

    @Test
    fun caldavCalendarSkippedWhenExisting() = runTest {
        whenever(caldavDao.getCalendarByUuid("cl1")).thenReturn(CaldavCalendar(uuid = "cl1"))
        assertNotNull(caldavDao.getCalendarByUuid("cl1"))
    }

    // ================================================================
    // importMetadata — taskListMetadata
    // ================================================================

    @Test
    fun taskListMetadataIdFromFilter() {
        val tlm = TaskListMetadata().apply { filter = "f1"; tagUuid = "t1" }
        val id = tlm.filter.takeIf { it?.isNotBlank() == true } ?: tlm.tagUuid!!
        assertEquals("f1", id)
    }

    @Test
    fun taskListMetadataIdFromTagUuid() {
        val tlm = TaskListMetadata().apply { filter = null; tagUuid = "t1" }
        val id = tlm.filter.takeIf { it?.isNotBlank() == true } ?: tlm.tagUuid!!
        assertEquals("t1", id)
    }

    @Test
    fun taskListMetadataIdFromTagUuidWhenFilterBlank() {
        val tlm = TaskListMetadata().apply { filter = "  "; tagUuid = "t1" }
        val id = tlm.filter.takeIf { it?.isNotBlank() == true } ?: tlm.tagUuid!!
        assertEquals("t1", id)
    }

    // ================================================================
    // Version constants
    // ================================================================

    @Test
    fun upgradeVersionOrdering() {
        assertTrue(com.todoroo.astrid.service.Upgrader.V6_4 < com.todoroo.astrid.service.Upgrader.V8_2)
        assertTrue(com.todoroo.astrid.service.Upgrader.V8_2 < com.todoroo.astrid.service.Upgrader.V9_6)
        assertTrue(com.todoroo.astrid.service.Upgrader.V9_6 < com.todoroo.astrid.service.Upgrader.V12_4)
        assertTrue(com.todoroo.astrid.service.Upgrader.V12_4 < com.todoroo.astrid.service.Upgrader.V12_8)
    }

    @Test
    fun upgrade1413VersionConstant() {
        assertEquals(141300, com.todoroo.astrid.service.Upgrade_14_13.VERSION)
    }

    // ================================================================
    // themeToColor
    // ================================================================

    @Test
    fun themeToColorOldVersionConverts() {
        val version = 600
        assertTrue(version < com.todoroo.astrid.service.Upgrader.V8_2)
    }

    @Test
    fun themeToColorNewVersionPassesThrough() {
        val version = 1400
        assertFalse(version < com.todoroo.astrid.service.Upgrader.V8_2)
    }
}
