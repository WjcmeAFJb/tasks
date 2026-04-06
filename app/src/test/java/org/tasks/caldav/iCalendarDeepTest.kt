@file:Suppress("ClassName")

package org.tasks.caldav

import com.natpryce.makeiteasy.MakeItEasy.with
import com.todoroo.astrid.alarms.AlarmService
import kotlinx.coroutines.test.runTest
import net.fortuna.ical4j.model.property.Completed
import net.fortuna.ical4j.model.property.RelatedTo
import net.fortuna.ical4j.model.property.Status
import net.fortuna.ical4j.model.property.XProperty
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.mockito.ArgumentMatchers.anyLong
import org.mockito.ArgumentMatchers.anySet
import org.mockito.ArgumentMatchers.eq
import org.mockito.Mockito.mock
import org.mockito.Mockito.never
import org.mockito.Mockito.verify
import org.mockito.Mockito.`when`
import org.tasks.caldav.iCalendar.Companion.IS_APPLE_SORT_ORDER
import org.tasks.caldav.iCalendar.Companion.collapsed
import org.tasks.caldav.iCalendar.Companion.fromVtodo
import org.tasks.caldav.iCalendar.Companion.getDateTime
import org.tasks.caldav.iCalendar.Companion.order
import org.tasks.caldav.iCalendar.Companion.parent
import org.tasks.caldav.iCalendar.Companion.reminders
import org.tasks.caldav.iCalendar.Companion.toMillis
import org.tasks.data.TaskSaver
import org.tasks.data.dao.AlarmDao
import org.tasks.data.dao.CaldavDao
import org.tasks.data.dao.LocationDao
import org.tasks.data.dao.TagDao
import org.tasks.data.dao.TagDataDao
import org.tasks.data.dao.TaskDao
import org.tasks.data.entity.CaldavAccount
import org.tasks.data.entity.CaldavCalendar
import org.tasks.data.entity.CaldavTask
import org.tasks.data.entity.TagData
import org.tasks.data.entity.Task
import org.tasks.data.entity.Task.Priority.Companion.HIGH
import org.tasks.data.entity.Task.Priority.Companion.LOW
import org.tasks.data.entity.Task.Priority.Companion.MEDIUM
import org.tasks.data.entity.Task.Priority.Companion.NONE
import org.tasks.date.DateTimeUtils.newDateTime
import org.tasks.location.Geocoder
import org.tasks.location.LocationService
import org.tasks.makers.CaldavTaskMaker.newCaldavTask
import org.tasks.makers.TaskMaker
import org.tasks.makers.TaskMaker.newTask
import org.tasks.makers.iCalMaker.COLLAPSED
import org.tasks.makers.iCalMaker.COMPLETED_AT
import org.tasks.makers.iCalMaker.CREATED_AT
import org.tasks.makers.iCalMaker.DESCRIPTION
import org.tasks.makers.iCalMaker.ORDER
import org.tasks.makers.iCalMaker.PARENT
import org.tasks.makers.iCalMaker.PRIORITY
import org.tasks.makers.iCalMaker.RRULE
import org.tasks.makers.iCalMaker.STATUS
import org.tasks.makers.iCalMaker.TITLE
import org.tasks.makers.iCalMaker.newIcal
import org.tasks.notifications.Notifier
import org.tasks.preferences.AppPreferences

class iCalendarDeepTest {

    private lateinit var tagDataDao: TagDataDao
    private lateinit var preferences: AppPreferences
    private lateinit var locationDao: LocationDao
    private lateinit var geocoder: Geocoder
    private lateinit var locationService: LocationService
    private lateinit var tagDao: TagDao
    private lateinit var taskDao: TaskDao
    private lateinit var taskSaver: TaskSaver
    private lateinit var caldavDao: CaldavDao
    private lateinit var alarmDao: AlarmDao
    private lateinit var alarmService: AlarmService
    private lateinit var vtodoCache: VtodoCache
    private lateinit var notifier: Notifier
    private lateinit var ical: iCalendar

    @Before
    fun setUp() {
        tagDataDao = mock(TagDataDao::class.java)
        preferences = mock(AppPreferences::class.java)
        locationDao = mock(LocationDao::class.java)
        geocoder = mock(Geocoder::class.java)
        locationService = mock(LocationService::class.java)
        tagDao = mock(TagDao::class.java)
        taskDao = mock(TaskDao::class.java)
        taskSaver = mock(TaskSaver::class.java)
        caldavDao = mock(CaldavDao::class.java)
        alarmDao = mock(AlarmDao::class.java)
        alarmService = mock(AlarmService::class.java)
        vtodoCache = mock(VtodoCache::class.java)
        notifier = mock(Notifier::class.java)
        ical = iCalendar(
            tagDataDao = tagDataDao, preferences = preferences, locationDao = locationDao,
            geocoder = geocoder, locationService = locationService, tagDao = tagDao,
            taskDao = taskDao, taskSaver = taskSaver, caldavDao = caldavDao,
            alarmDao = alarmDao, alarmService = alarmService, vtodoCache = vtodoCache,
            notifier = notifier,
        )
    }

    // ===== getTags =====

    @Test fun getTagsEmptyReturnsEmpty() = runTest { assertTrue(ical.getTags(emptyList()).isEmpty()) }

    @Test
    fun getTagsAllExisting() = runTest {
        val existing = listOf(TagData(name = "Work"), TagData(name = "Home"))
        `when`(tagDataDao.getTags(listOf("Work", "Home"))).thenReturn(existing)
        val result = ical.getTags(listOf("Work", "Home"))
        assertEquals(2, result.size)
    }

    @Test
    fun getTagsCreatesNew() = runTest {
        `when`(tagDataDao.getTags(listOf("Existing", "NewTag"))).thenReturn(listOf(TagData(name = "Existing")))
        `when`(tagDataDao.insert(org.mockito.ArgumentMatchers.any(TagData::class.java) ?: TagData())).thenReturn(99L)
        val result = ical.getTags(listOf("Existing", "NewTag"))
        assertEquals(2, result.size)
    }

    @Test
    fun getTagsCreatesMultipleNew() = runTest {
        `when`(tagDataDao.getTags(listOf("A", "B", "C"))).thenReturn(emptyList())
        `when`(tagDataDao.insert(org.mockito.ArgumentMatchers.any(TagData::class.java) ?: TagData())).thenReturn(1L, 2L, 3L)
        assertEquals(3, ical.getTags(listOf("A", "B", "C")).size)
    }

    // ===== fromVtodo() companion =====

    @Test fun fromVtodoValid() { val r = fromVtodo("BEGIN:VCALENDAR\nVERSION:2.0\nPRODID:-//T//T//EN\nBEGIN:VTODO\nUID:test\nSUMMARY:My Task\nPRIORITY:5\nDESCRIPTION:A note\nEND:VTODO\nEND:VCALENDAR"); assertNotNull(r); assertEquals("test", r!!.uid); assertEquals("My Task", r.summary); assertEquals(5, r.priority); assertEquals("A note", r.description) }
    @Test fun fromVtodoInvalid() = assertNull(fromVtodo("not valid"))
    @Test fun fromVtodoEmpty() = assertNull(fromVtodo(""))
    @Test fun fromVtodoMultiple() = assertNull(fromVtodo("BEGIN:VCALENDAR\nVERSION:2.0\nPRODID:-//T//T//EN\nBEGIN:VTODO\nUID:a\nEND:VTODO\nBEGIN:VTODO\nUID:b\nEND:VTODO\nEND:VCALENDAR"))

    @Test fun fromVtodoCompleted() { val r = fromVtodo("BEGIN:VCALENDAR\nVERSION:2.0\nPRODID:-//T//T//EN\nBEGIN:VTODO\nUID:t\nSTATUS:COMPLETED\nCOMPLETED:20240315T100000Z\nPERCENT-COMPLETE:100\nEND:VTODO\nEND:VCALENDAR"); assertNotNull(r); assertEquals(Status.VTODO_COMPLETED, r!!.status); assertEquals(100, r.percentComplete) }
    @Test fun fromVtodoCategories() { val r = fromVtodo("BEGIN:VCALENDAR\nVERSION:2.0\nPRODID:-//T//T//EN\nBEGIN:VTODO\nUID:t\nCATEGORIES:Work,Personal,Urgent\nEND:VTODO\nEND:VCALENDAR"); assertEquals(3, r!!.categories.size) }
    @Test fun fromVtodoGeo() { assertNotNull(fromVtodo("BEGIN:VCALENDAR\nVERSION:2.0\nPRODID:-//T//T//EN\nBEGIN:VTODO\nUID:t\nGEO:37.386;-122.082\nEND:VTODO\nEND:VCALENDAR")!!.geoPosition) }
    @Test fun fromVtodoRelatedTo() { assertEquals("parent-uid", fromVtodo("BEGIN:VCALENDAR\nVERSION:2.0\nPRODID:-//T//T//EN\nBEGIN:VTODO\nUID:child\nRELATED-TO:parent-uid\nEND:VTODO\nEND:VCALENDAR")!!.parent) }
    @Test fun fromVtodoRecurrence() { assertNotNull(fromVtodo("BEGIN:VCALENDAR\nVERSION:2.0\nPRODID:-//T//T//EN\nBEGIN:VTODO\nUID:t\nRRULE:FREQ=WEEKLY;BYDAY=MO,WE,FR\nEND:VTODO\nEND:VCALENDAR")!!.rRule) }
    @Test fun fromVtodoDueDate() { assertNotNull(fromVtodo("BEGIN:VCALENDAR\nVERSION:2.0\nPRODID:-//T//T//EN\nBEGIN:VTODO\nUID:t\nDUE;VALUE=DATE:20240315\nEND:VTODO\nEND:VCALENDAR")!!.due) }
    @Test fun fromVtodoDueDateTime() { assertNotNull(fromVtodo("BEGIN:VCALENDAR\nVERSION:2.0\nPRODID:-//T//T//EN\nBEGIN:VTODO\nUID:t\nDUE:20240315T100000Z\nEND:VTODO\nEND:VCALENDAR")!!.due) }
    @Test fun fromVtodoDtStart() { val r = fromVtodo("BEGIN:VCALENDAR\nVERSION:2.0\nPRODID:-//T//T//EN\nBEGIN:VTODO\nUID:t\nDTSTART;VALUE=DATE:20240310\nDUE;VALUE=DATE:20240315\nEND:VTODO\nEND:VCALENDAR"); assertNotNull(r!!.dtStart); assertNotNull(r.due) }
    @Test fun fromVtodoComment() { assertEquals("This is a comment", fromVtodo("BEGIN:VCALENDAR\nVERSION:2.0\nPRODID:-//T//T//EN\nBEGIN:VTODO\nUID:t\nCOMMENT:This is a comment\nEND:VTODO\nEND:VCALENDAR")!!.comment) }

    // ===== prodId / supportsReminders =====

    @Test fun prodIdValid() { assertEquals("-//tasks.org//EN", with(iCalendar.Companion) { "BEGIN:VCALENDAR\nPRODID:-//tasks.org//EN\nEND:VCALENDAR\n".prodId() }) }
    @Test fun prodIdMissing() { assertNull(with(iCalendar.Companion) { "BEGIN:VCALENDAR\nVERSION:2.0\nEND:VCALENDAR\n".prodId() }) }
    @Test fun tasksOrgReminders() { assertTrue(with(iCalendar.Companion) { "tasks.org/client".supportsReminders() }) }
    @Test fun mozillaReminders() { assertTrue(with(iCalendar.Companion) { "Mozilla.org/Calendar".supportsReminders() }) }
    @Test fun appleReminders() { assertTrue(with(iCalendar.Companion) { "Apple Inc./iCal".supportsReminders() }) }
    @Test fun unknownNoReminders() { assertFalse(with(iCalendar.Companion) { "random".supportsReminders() }) }
    @Test fun emptyNoReminders() { assertFalse(with(iCalendar.Companion) { "".supportsReminders() }) }

    // ===== VTodoTask extensions: parent =====

    @Test fun parentNull() = assertNull(org.tasks.caldav.Task(uid = "c").parent)
    @Test fun parentValue() { val t = org.tasks.caldav.Task(uid = "c"); t.relatedTo.add(RelatedTo("p")); assertEquals("p", t.parent) }
    @Test fun parentSet() { val t = org.tasks.caldav.Task(uid = "c"); t.parent = "np"; assertEquals("np", t.parent) }
    @Test fun parentSetNull() { val t = org.tasks.caldav.Task(uid = "c"); t.relatedTo.add(RelatedTo("old")); t.parent = null; assertNull(t.parent) }
    @Test fun parentSetBlank() { val t = org.tasks.caldav.Task(uid = "c"); t.relatedTo.add(RelatedTo("old")); t.parent = ""; assertNull(t.parent) }
    @Test fun parentUpdate() { val t = org.tasks.caldav.Task(uid = "c"); t.relatedTo.add(RelatedTo("old")); t.parent = "new"; assertEquals("new", t.parent); assertEquals(1, t.relatedTo.size) }

    // ===== VTodoTask extensions: order =====

    @Test fun orderNull() = assertNull(org.tasks.caldav.Task(uid = "u").order)
    @Test fun orderSet() { val t = org.tasks.caldav.Task(uid = "u"); t.order = 42L; assertEquals(42L, t.order) }
    @Test fun orderSetNull() { val t = org.tasks.caldav.Task(uid = "u"); t.order = 42L; t.order = null; assertNull(t.order) }
    @Test fun orderUpdate() { val t = org.tasks.caldav.Task(uid = "u"); t.order = 10L; t.order = 20L; assertEquals(20L, t.order) }

    // ===== VTodoTask extensions: collapsed =====

    @Test fun collapsedFalse() = assertFalse(org.tasks.caldav.Task(uid = "u").collapsed)
    @Test fun collapsedTrue() { val t = org.tasks.caldav.Task(uid = "u"); t.collapsed = true; assertTrue(t.collapsed) }
    @Test fun collapsedReset() { val t = org.tasks.caldav.Task(uid = "u"); t.collapsed = true; t.collapsed = false; assertFalse(t.collapsed) }
    @Test fun collapsedTwice() { val t = org.tasks.caldav.Task(uid = "u"); t.collapsed = true; t.collapsed = true; assertEquals(1, t.unknownProperties.count { it.name.equals("X-OC-HIDESUBTASKS", true) }) }

    // ===== IS_APPLE_SORT_ORDER =====

    @Test fun appleSortOrderTrue() = assertTrue(IS_APPLE_SORT_ORDER(XProperty("X-APPLE-SORT-ORDER", "123")))
    @Test fun appleSortOrderCaseInsensitive() = assertTrue(IS_APPLE_SORT_ORDER(XProperty("x-apple-sort-order", "123")))
    @Test fun appleSortOrderFalse() = assertFalse(IS_APPLE_SORT_ORDER(XProperty("X-OTHER", "123")))
    @Test fun appleSortOrderNull() = assertFalse(IS_APPLE_SORT_ORDER(null))

    // ===== Due/DtStart toMillis =====

    @Test fun nullDueZero() { val d: net.fortuna.ical4j.model.property.Due? = null; assertEquals(0L, d.toMillis()) }
    @Test fun nullDtStartZero() { val d: net.fortuna.ical4j.model.property.DtStart? = null; assertEquals(0L, with(iCalendar.Companion) { d.toMillis(Task()) }) }
    @Test fun getDateTimeNonNull() = assertNotNull(getDateTime(newDateTime().millis))

    // ===== applyRemote: title =====

    @Test fun titleNewTask() { val t = newTask(); t.applyRemote(newIcal(with(TITLE, "Remote")), null); assertEquals("Remote", t.title) }
    @Test fun titleRemoteWins() { val t = newTask(with(TaskMaker.TITLE, "Old")); t.applyRemote(newIcal(with(TITLE, "New")), newIcal(with(TITLE, "Old"))); assertEquals("New", t.title) }
    @Test fun titleLocalWins() { val t = newTask(with(TaskMaker.TITLE, "Local")); t.applyRemote(newIcal(with(TITLE, "Remote")), newIcal(with(TITLE, "Orig"))); assertEquals("Local", t.title) }

    // ===== applyRemote: description =====

    @Test fun descNewTask() { val t = newTask(); t.applyRemote(newIcal(with(DESCRIPTION, "Desc")), null); assertEquals("Desc", t.notes) }
    @Test fun descRemoteWins() { val t = newTask(with(TaskMaker.DESCRIPTION, "Old")); t.applyRemote(newIcal(with(DESCRIPTION, "New")), newIcal(with(DESCRIPTION, "Old"))); assertEquals("New", t.notes) }
    @Test fun descLocalWins() { val t = newTask(with(TaskMaker.DESCRIPTION, "Local")); t.applyRemote(newIcal(with(DESCRIPTION, "Remote")), newIcal(with(DESCRIPTION, "Orig"))); assertEquals("Local", t.notes) }

    // ===== applyRemote: priority =====

    @Test fun priority0None() { val t = newTask(); t.applyRemote(newIcal(with(PRIORITY, 0)), null); assertEquals(NONE, t.priority) }
    @Test fun priority1High() { val t = newTask(); t.applyRemote(newIcal(with(PRIORITY, 1)), null); assertEquals(HIGH, t.priority) }
    @Test fun priority4High() { val t = newTask(); t.applyRemote(newIcal(with(PRIORITY, 4)), null); assertEquals(HIGH, t.priority) }
    @Test fun priority5Medium() { val t = newTask(); t.applyRemote(newIcal(with(PRIORITY, 5)), null); assertEquals(MEDIUM, t.priority) }
    @Test fun priority6Low() { val t = newTask(); t.applyRemote(newIcal(with(PRIORITY, 6)), null); assertEquals(LOW, t.priority) }
    @Test fun priority9Low() { val t = newTask(); t.applyRemote(newIcal(with(PRIORITY, 9)), null); assertEquals(LOW, t.priority) }

    // ===== applyRemote: completion =====

    @Test fun completedNew() { val t = newTask(); t.applyRemote(newIcal(with(COMPLETED_AT, newDateTime().toUTC())), null); assertTrue(t.isCompleted) }
    @Test fun completedStatus() { val t = newTask(); t.applyRemote(newIcal(with(STATUS, Status.VTODO_COMPLETED)), null); assertTrue(t.isCompleted) }
    @Test fun notCompletedClears() { val t = newTask(with(TaskMaker.COMPLETION_TIME, newDateTime())); t.applyRemote(newIcal(), null); assertFalse(t.isCompleted) }

    // ===== applyRemote: recurrence =====

    @Test fun recurrenceNew() { val t = newTask(); t.applyRemote(newIcal(with(RRULE, "FREQ=DAILY")), null); assertTrue(t.recurrence!!.contains("DAILY")) }
    @Test fun recurrenceNull() { val t = newTask(); t.applyRemote(newIcal(), null); assertNull(t.recurrence) }

    // ===== applyRemote: collapsed =====

    @Test fun collapsedFromRemote() { val t = newTask(); t.applyRemote(newIcal(with(COLLAPSED, true)), null); assertTrue(t.isCollapsed) }
    @Test fun collapsedDefaultFalseRemote() { val t = newTask(); t.applyRemote(newIcal(), null); assertFalse(t.isCollapsed) }

    // ===== applyRemote: order =====

    @Test fun orderFromRemote() { val t = newTask(); t.applyRemote(newIcal(with(ORDER, 42L)), null); assertEquals(42L, t.order) }
    @Test fun orderNullFromRemote() { val t = newTask(); t.applyRemote(newIcal(), null); assertNull(t.order) }

    // ===== CaldavTask.applyRemote: parent =====

    @Test fun parentFromRemote() { val ct = newCaldavTask(); ct.applyRemote(newIcal(with(PARENT, "pid")), null); assertEquals("pid", ct.remoteParent) }
    @Test fun parentNullFromRemote() { val ct = newCaldavTask(); ct.applyRemote(newIcal(), null); assertNull(ct.remoteParent) }

    // ===== applyRemote: createdAt =====

    @Test fun createdAtNew() { val c = newDateTime(); val t = newTask(with(TaskMaker.CREATION_TIME, c)); t.applyRemote(newIcal(with(CREATED_AT, c.plusHours(1).toUTC())), null); assertEquals(c.plusHours(1).millis, t.creationDate) }
    @Test fun createdAtNull() { val c = newDateTime(); val t = newTask(with(TaskMaker.CREATION_TIME, c)); t.applyRemote(newIcal(), null); assertEquals(c.millis, t.creationDate) }

    // ===== fromVtodo() instance: new task =====

    @Test
    fun fromVtodoCreatesNew() = runTest {
        val account = CaldavAccount(uuid = "a", password = "pw")
        val calendar = CaldavCalendar(uuid = "c", account = "a")
        val remote = newIcal(with(TITLE, "New Task"), with(PRIORITY, 5))
        remote.uid = "rid"
        `when`(preferences.defaultPriority()).thenReturn(0)
        `when`(taskDao.createNew(org.mockito.ArgumentMatchers.any(Task::class.java) ?: Task())).thenAnswer { (it.arguments[0] as Task).id = 100L }
        `when`(caldavDao.insert(org.mockito.ArgumentMatchers.any(CaldavTask::class.java) ?: CaldavTask(calendar = "c"))).thenReturn(1L)
        `when`(alarmDao.getAlarms(100L)).thenReturn(emptyList())
        `when`(alarmService.synchronizeAlarms(anyLong(), org.mockito.ArgumentMatchers.any() ?: mutableSetOf())).thenReturn(false)
        `when`(locationDao.getPlaceForTask(100L)).thenReturn(null)
        `when`(locationDao.getActiveGeofences(100L)).thenReturn(emptyList())
        `when`(tagDataDao.getTagDataForTask(100L)).thenReturn(emptyList())
        `when`(tagDataDao.getTags(emptyList())).thenReturn(emptyList())
        ical.fromVtodo(account, calendar, null, remote, null, "rid.ics", "etag")
        // Verify that task was created and saved
        verify(alarmDao).getAlarms(100L)
    }

    @Test
    fun fromVtodoUpdatesExisting() = runTest {
        val account = CaldavAccount(uuid = "a", password = "pw")
        val calendar = CaldavCalendar(uuid = "c", account = "a")
        val existing = CaldavTask(id = 50L, task = 200L, calendar = "c", remoteId = "rid", obj = "rid.ics", lastSync = 1000L)
        val existingTask = Task(id = 200L, title = "Old", modificationDate = 999L)
        val remote = newIcal(with(TITLE, "Updated"))
        remote.uid = "rid"
        `when`(taskDao.fetch(200L)).thenReturn(existingTask)
        `when`(vtodoCache.getVtodo(calendar, existing)).thenReturn(null)
        `when`(alarmDao.getAlarms(200L)).thenReturn(emptyList())
        `when`(alarmService.synchronizeAlarms(anyLong(), org.mockito.ArgumentMatchers.any() ?: mutableSetOf())).thenReturn(false)
        `when`(locationDao.getPlaceForTask(200L)).thenReturn(null)
        `when`(locationDao.getActiveGeofences(200L)).thenReturn(emptyList())
        `when`(tagDataDao.getTagDataForTask(200L)).thenReturn(emptyList())
        `when`(tagDataDao.getTags(emptyList())).thenReturn(emptyList())
        ical.fromVtodo(account, calendar, existing, remote, null, "rid.ics", "new-etag")
        verify(alarmDao).getAlarms(200L)
    }

    @Test
    fun fromVtodoSkipsDeleted() = runTest {
        val account = CaldavAccount(uuid = "a", password = "pw")
        val calendar = CaldavCalendar(uuid = "c", account = "a")
        val deleted = CaldavTask(id = 50L, task = 200L, calendar = "c", remoteId = "rid", deleted = 1000L)
        val remote = newIcal(with(TITLE, "Should Not Process"))
        remote.uid = "rid"
        ical.fromVtodo(account, calendar, deleted, remote, null)
        verify(taskDao, never()).fetch(anyLong())
    }

    @Test
    fun fromVtodoCancelsNotifierOnLastAck() = runTest {
        val account = CaldavAccount(uuid = "a", password = "pw")
        val calendar = CaldavCalendar(uuid = "c", account = "a")
        val remote = newIcal(with(TITLE, "Task"))
        remote.uid = "rid"
        remote.unknownProperties.add(XProperty("X-MOZ-LASTACK", "20400101T000000Z"))
        val existingTask = Task(id = 200L, reminderLast = 100L, modificationDate = 999L)
        val existing = CaldavTask(id = 50L, task = 200L, calendar = "c", remoteId = "rid", lastSync = 1000L)
        `when`(taskDao.fetch(200L)).thenReturn(existingTask)
        `when`(vtodoCache.getVtodo(calendar, existing)).thenReturn(null)
        `when`(alarmDao.getAlarms(200L)).thenReturn(emptyList())
        `when`(alarmService.synchronizeAlarms(anyLong(), org.mockito.ArgumentMatchers.any() ?: mutableSetOf())).thenReturn(false)
        `when`(locationDao.getPlaceForTask(200L)).thenReturn(null)
        `when`(locationDao.getActiveGeofences(200L)).thenReturn(emptyList())
        `when`(tagDataDao.getTagDataForTask(200L)).thenReturn(emptyList())
        `when`(tagDataDao.getTags(emptyList())).thenReturn(emptyList())
        ical.fromVtodo(account, calendar, existing, remote, null)
        verify(notifier).cancel(200L)
    }

    // ===== toVtodo serialization =====

    @Test
    fun toVtodoBasic() = runTest {
        val account = CaldavAccount(uuid = "a", password = "pw")
        val calendar = CaldavCalendar(uuid = "c", account = "a")
        val ct = CaldavTask(id = 1L, task = 100L, calendar = "c", remoteId = "rid")
        val task = Task(id = 100L, title = "My Task", creationDate = newDateTime().millis, modificationDate = newDateTime().millis)
        `when`(vtodoCache.getVtodo(calendar, ct)).thenReturn(null)
        `when`(tagDataDao.getTagDataForTask(100L)).thenReturn(emptyList())
        `when`(locationDao.getGeofences(100L)).thenReturn(null)
        `when`(alarmDao.getAlarms(100L)).thenReturn(emptyList())
        val output = String(ical.toVtodo(account, calendar, ct, task))
        assertTrue(output.contains("BEGIN:VCALENDAR"))
        assertTrue(output.contains("BEGIN:VTODO"))
        assertTrue(output.contains("rid"))
    }

    @Test
    fun toVtodoWithTags() = runTest {
        val account = CaldavAccount(uuid = "a", password = "pw")
        val calendar = CaldavCalendar(uuid = "c", account = "a")
        val ct = CaldavTask(id = 1L, task = 100L, calendar = "c", remoteId = "rid")
        val task = Task(id = 100L, title = "Tagged", creationDate = newDateTime().millis, modificationDate = newDateTime().millis)
        `when`(vtodoCache.getVtodo(calendar, ct)).thenReturn(null)
        `when`(tagDataDao.getTagDataForTask(100L)).thenReturn(listOf(TagData(name = "Work"), TagData(name = "Urgent")))
        `when`(locationDao.getGeofences(100L)).thenReturn(null)
        `when`(alarmDao.getAlarms(100L)).thenReturn(emptyList())
        val output = String(ical.toVtodo(account, calendar, ct, task))
        assertTrue(output.contains("CATEGORIES"))
        assertTrue(output.contains("Work"))
    }

    @Test
    fun toVtodoCompleted() = runTest {
        val account = CaldavAccount(uuid = "a", password = "pw")
        val calendar = CaldavCalendar(uuid = "c", account = "a")
        val ct = CaldavTask(id = 1L, task = 100L, calendar = "c", remoteId = "rid")
        val task = Task(id = 100L, title = "Done", completionDate = newDateTime().millis, creationDate = newDateTime().millis, modificationDate = newDateTime().millis)
        `when`(vtodoCache.getVtodo(calendar, ct)).thenReturn(null)
        `when`(tagDataDao.getTagDataForTask(100L)).thenReturn(emptyList())
        `when`(locationDao.getGeofences(100L)).thenReturn(null)
        `when`(alarmDao.getAlarms(100L)).thenReturn(emptyList())
        val output = String(ical.toVtodo(account, calendar, ct, task))
        assertTrue(output.contains("STATUS:COMPLETED"))
        assertTrue(output.contains("PERCENT-COMPLETE:100"))
    }

    @Test
    fun toVtodoWithParent() = runTest {
        val account = CaldavAccount(uuid = "a", password = "pw")
        val calendar = CaldavCalendar(uuid = "c", account = "a")
        val ct = CaldavTask(id = 1L, task = 100L, calendar = "c", remoteId = "rid", remoteParent = "parent-rid")
        val task = Task(id = 100L, title = "Child", parent = 50L, creationDate = newDateTime().millis, modificationDate = newDateTime().millis)
        `when`(vtodoCache.getVtodo(calendar, ct)).thenReturn(null)
        `when`(tagDataDao.getTagDataForTask(100L)).thenReturn(emptyList())
        `when`(locationDao.getGeofences(100L)).thenReturn(null)
        `when`(alarmDao.getAlarms(100L)).thenReturn(emptyList())
        val output = String(ical.toVtodo(account, calendar, ct, task))
        assertTrue(output.contains("RELATED-TO"))
        assertTrue(output.contains("parent-rid"))
    }

    // ===== applyLocal =====

    @Test
    fun applyLocalSets() {
        val vt = org.tasks.caldav.Task()
        val ct = CaldavTask(task = 100L, calendar = "c", remoteId = "rid")
        val t = Task(id = 100L, title = "Title", notes = "Notes", creationDate = 1000L, modificationDate = 2000L, priority = Task.Priority.HIGH)
        with(iCalendar.Companion) { vt.applyLocal(ct, t) }
        assertEquals("Title", vt.summary)
        assertEquals("Notes", vt.description)
        assertEquals(1, vt.priority) // HIGH -> 1
    }

    @Test
    fun applyLocalCompleted() {
        val vt = org.tasks.caldav.Task()
        val ct = CaldavTask(task = 100L, calendar = "c", remoteId = "id")
        val t = Task(id = 100L, completionDate = 1000L, creationDate = 500L, modificationDate = 1500L)
        with(iCalendar.Companion) { vt.applyLocal(ct, t) }
        assertEquals(Status.VTODO_COMPLETED, vt.status)
        assertNotNull(vt.completedAt)
        assertEquals(100, vt.percentComplete)
    }

    @Test
    fun applyLocalUncomplete() {
        val vt = org.tasks.caldav.Task(completedAt = Completed(net.fortuna.ical4j.model.DateTime()), status = Status.VTODO_COMPLETED, percentComplete = 100)
        val ct = CaldavTask(task = 100L, calendar = "c", remoteId = "id")
        val t = Task(id = 100L, completionDate = 0L, creationDate = 500L, modificationDate = 1500L)
        with(iCalendar.Companion) { vt.applyLocal(ct, t) }
        assertNull(vt.completedAt)
        assertNull(vt.status)
        assertNull(vt.percentComplete)
    }

    @Test fun applyLocalNonePriority() { val vt = org.tasks.caldav.Task(); with(iCalendar.Companion) { vt.applyLocal(CaldavTask(task = 1, calendar = "c", remoteId = "id"), Task(priority = Task.Priority.NONE, creationDate = 1, modificationDate = 1)) }; assertEquals(0, vt.priority) }
    @Test fun applyLocalMediumPriority() { val vt = org.tasks.caldav.Task(); with(iCalendar.Companion) { vt.applyLocal(CaldavTask(task = 1, calendar = "c", remoteId = "id"), Task(priority = Task.Priority.MEDIUM, creationDate = 1, modificationDate = 1)) }; assertEquals(5, vt.priority) }
    @Test fun applyLocalHighPriority() { val vt = org.tasks.caldav.Task(priority = 0); with(iCalendar.Companion) { vt.applyLocal(CaldavTask(task = 1, calendar = "c", remoteId = "id"), Task(priority = Task.Priority.HIGH, creationDate = 1, modificationDate = 1)) }; assertEquals(1, vt.priority) }
    @Test fun applyLocalLowPriority() { val vt = org.tasks.caldav.Task(priority = 0); with(iCalendar.Companion) { vt.applyLocal(CaldavTask(task = 1, calendar = "c", remoteId = "id"), Task(priority = Task.Priority.LOW, creationDate = 1, modificationDate = 1)) }; assertEquals(9, vt.priority) }
    @Test fun applyLocalHighPreserves() { val vt = org.tasks.caldav.Task(priority = 3); with(iCalendar.Companion) { vt.applyLocal(CaldavTask(task = 1, calendar = "c", remoteId = "id"), Task(priority = Task.Priority.HIGH, creationDate = 1, modificationDate = 1)) }; assertEquals(3, vt.priority) }
    @Test fun applyLocalHighFromHighRange() { val vt = org.tasks.caldav.Task(priority = 7); with(iCalendar.Companion) { vt.applyLocal(CaldavTask(task = 1, calendar = "c", remoteId = "id"), Task(priority = Task.Priority.HIGH, creationDate = 1, modificationDate = 1)) }; assertEquals(1, vt.priority) }
    @Test fun applyLocalLowPreserves() { val vt = org.tasks.caldav.Task(priority = 7); with(iCalendar.Companion) { vt.applyLocal(CaldavTask(task = 1, calendar = "c", remoteId = "id"), Task(priority = Task.Priority.LOW, creationDate = 1, modificationDate = 1)) }; assertEquals(7, vt.priority) }
    @Test fun applyLocalLowFromLow() { val vt = org.tasks.caldav.Task(priority = 2); with(iCalendar.Companion) { vt.applyLocal(CaldavTask(task = 1, calendar = "c", remoteId = "id"), Task(priority = Task.Priority.LOW, creationDate = 1, modificationDate = 1)) }; assertEquals(9, vt.priority) }

    @Test fun applyLocalOrder() { val vt = org.tasks.caldav.Task(); with(iCalendar.Companion) { vt.applyLocal(CaldavTask(task = 1, calendar = "c", remoteId = "id"), Task(order = 42L, creationDate = 1, modificationDate = 1)) }; assertEquals(42L, vt.order) }
    @Test fun applyLocalCollapsed() { val vt = org.tasks.caldav.Task(); with(iCalendar.Companion) { vt.applyLocal(CaldavTask(task = 1, calendar = "c", remoteId = "id"), Task(isCollapsed = true, creationDate = 1, modificationDate = 1)) }; assertTrue(vt.collapsed) }
    @Test fun applyLocalParent() { val vt = org.tasks.caldav.Task(); with(iCalendar.Companion) { vt.applyLocal(CaldavTask(task = 1, calendar = "c", remoteId = "id", remoteParent = "p"), Task(parent = 50L, creationDate = 1, modificationDate = 1)) }; assertEquals("p", vt.parent) }
    @Test fun applyLocalNoParent() { val vt = org.tasks.caldav.Task(); with(iCalendar.Companion) { vt.applyLocal(CaldavTask(task = 1, calendar = "c", remoteId = "id", remoteParent = "old"), Task(parent = 0L, creationDate = 1, modificationDate = 1)) }; assertNull(vt.parent) }

    // ===== reminders =====

    @Test fun remindersEmpty() = assertTrue(org.tasks.caldav.Task().reminders.isEmpty())

    // ===== Task parsing edge cases =====

    @Test fun dtStartDateDueDateTimeFixes() { val r = fromVtodo("BEGIN:VCALENDAR\nVERSION:2.0\nPRODID:-//T//T//EN\nBEGIN:VTODO\nUID:t\nDTSTART;VALUE=DATE:20240315\nDUE:20240320T100000Z\nEND:VTODO\nEND:VCALENDAR"); assertNotNull(r!!.dtStart); assertNotNull(r.due) }
    @Test fun dtStartDateTimeDueDateFixes() { val r = fromVtodo("BEGIN:VCALENDAR\nVERSION:2.0\nPRODID:-//T//T//EN\nBEGIN:VTODO\nUID:t\nDTSTART:20240315T100000Z\nDUE;VALUE=DATE:20240320\nEND:VTODO\nEND:VCALENDAR"); assertNotNull(r!!.dtStart); assertNotNull(r.due) }
    @Test fun dueBeforeDtStartDrops() { val r = fromVtodo("BEGIN:VCALENDAR\nVERSION:2.0\nPRODID:-//T//T//EN\nBEGIN:VTODO\nUID:t\nDTSTART;VALUE=DATE:20240320\nDUE;VALUE=DATE:20240315\nEND:VTODO\nEND:VCALENDAR"); assertNull(r!!.dtStart); assertNotNull(r.due) }
    @Test fun durationWithoutDtStartDrops() { assertNull(fromVtodo("BEGIN:VCALENDAR\nVERSION:2.0\nPRODID:-//T//T//EN\nBEGIN:VTODO\nUID:t\nDURATION:PT1H\nEND:VTODO\nEND:VCALENDAR")!!.duration) }
    @Test fun durationWithDtStartKept() { val r = fromVtodo("BEGIN:VCALENDAR\nVERSION:2.0\nPRODID:-//T//T//EN\nBEGIN:VTODO\nUID:t\nDTSTART;VALUE=DATE:20240315\nDURATION:P1D\nEND:VTODO\nEND:VCALENDAR"); assertNotNull(r!!.dtStart); assertNotNull(r.duration) }

    // ===== Round trips =====

    @Test fun roundTripUid() { val o = org.tasks.caldav.Task(uid = "rt-uid"); val os = java.io.ByteArrayOutputStream(); o.write(os); assertEquals("rt-uid", org.tasks.caldav.Task.tasksFromReader(java.io.StringReader(os.toString("UTF-8")))[0].uid) }
    @Test fun roundTripSummary() { val o = org.tasks.caldav.Task(uid = "u", summary = "RT Title"); val os = java.io.ByteArrayOutputStream(); o.write(os); assertEquals("RT Title", org.tasks.caldav.Task.tasksFromReader(java.io.StringReader(os.toString("UTF-8")))[0].summary) }
    @Test fun roundTripPriority() { val o = org.tasks.caldav.Task(uid = "u", priority = 7); val os = java.io.ByteArrayOutputStream(); o.write(os); assertEquals(7, org.tasks.caldav.Task.tasksFromReader(java.io.StringReader(os.toString("UTF-8")))[0].priority) }
    @Test fun roundTripDesc() { val o = org.tasks.caldav.Task(uid = "u", description = "A desc"); val os = java.io.ByteArrayOutputStream(); o.write(os); assertEquals("A desc", org.tasks.caldav.Task.tasksFromReader(java.io.StringReader(os.toString("UTF-8")))[0].description) }
    @Test fun roundTripCategories() { val o = org.tasks.caldav.Task(uid = "u"); o.categories.addAll(listOf("C1", "C2")); val os = java.io.ByteArrayOutputStream(); o.write(os); val p = org.tasks.caldav.Task.tasksFromReader(java.io.StringReader(os.toString("UTF-8")))[0]; assertTrue(p.categories.contains("C1")); assertTrue(p.categories.contains("C2")) }
    @Test fun roundTripStatus() { val o = org.tasks.caldav.Task(uid = "u", status = Status.VTODO_IN_PROCESS); val os = java.io.ByteArrayOutputStream(); o.write(os); assertEquals(Status.VTODO_IN_PROCESS, org.tasks.caldav.Task.tasksFromReader(java.io.StringReader(os.toString("UTF-8")))[0].status) }
    @Test fun roundTripRelatedTo() { val o = org.tasks.caldav.Task(uid = "c"); o.relatedTo.add(RelatedTo("p")); val os = java.io.ByteArrayOutputStream(); o.write(os); assertEquals("p", org.tasks.caldav.Task.tasksFromReader(java.io.StringReader(os.toString("UTF-8")))[0].relatedTo[0].value) }
    @Test fun roundTripComment() { val o = org.tasks.caldav.Task(uid = "u", comment = "A comment"); val os = java.io.ByteArrayOutputStream(); o.write(os); assertEquals("A comment", org.tasks.caldav.Task.tasksFromReader(java.io.StringReader(os.toString("UTF-8")))[0].comment) }
    @Test fun roundTripPercent() { val o = org.tasks.caldav.Task(uid = "u", percentComplete = 75); val os = java.io.ByteArrayOutputStream(); o.write(os); assertEquals(75, org.tasks.caldav.Task.tasksFromReader(java.io.StringReader(os.toString("UTF-8")))[0].percentComplete) }
}
