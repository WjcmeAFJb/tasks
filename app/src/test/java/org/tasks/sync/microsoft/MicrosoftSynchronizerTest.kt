package org.tasks.sync.microsoft

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test
import org.tasks.data.entity.CaldavCalendar
import org.tasks.data.entity.CaldavCalendar.Companion.ACCESS_OWNER
import org.tasks.data.entity.CaldavCalendar.Companion.ACCESS_READ_WRITE
import org.tasks.data.entity.CaldavCalendar.Companion.ACCESS_UNKNOWN

class MicrosoftSynchronizerTest {

    // ===== TaskLists.TaskList.applyTo =====

    @Test
    fun applyToSetsDisplayName() {
        val list = CaldavCalendar()
        val remote = TaskLists.TaskList(displayName = "My List")
        remote.applyTo(list)
        assertEquals("My List", list.name)
    }

    @Test
    fun applyToSetsUrl() {
        val list = CaldavCalendar()
        val remote = TaskLists.TaskList(id = "list-123")
        remote.applyTo(list)
        assertEquals("list-123", list.url)
    }

    @Test
    fun applyToSetsUuid() {
        val list = CaldavCalendar()
        val remote = TaskLists.TaskList(id = "list-456")
        remote.applyTo(list)
        assertEquals("list-456", list.uuid)
    }

    @Test
    fun applyToSetsAccessOwner() {
        val list = CaldavCalendar()
        val remote = TaskLists.TaskList(isOwner = true)
        remote.applyTo(list)
        assertEquals(ACCESS_OWNER, list.access)
    }

    @Test
    fun applyToSetsAccessReadWriteWhenShared() {
        val list = CaldavCalendar()
        val remote = TaskLists.TaskList(isShared = true)
        remote.applyTo(list)
        assertEquals(ACCESS_READ_WRITE, list.access)
    }

    @Test
    fun applyToSetsAccessUnknownWhenNeitherOwnerNorShared() {
        val list = CaldavCalendar()
        val remote = TaskLists.TaskList()
        remote.applyTo(list)
        assertEquals(ACCESS_UNKNOWN, list.access)
    }

    @Test
    fun applyToOwnerTakesPrecedenceOverShared() {
        val list = CaldavCalendar()
        val remote = TaskLists.TaskList(isOwner = true, isShared = true)
        remote.applyTo(list)
        assertEquals(ACCESS_OWNER, list.access)
    }

    @Test
    fun applyToSetsAccessUnknownWithNullFlags() {
        val list = CaldavCalendar()
        val remote = TaskLists.TaskList(isOwner = null, isShared = null)
        remote.applyTo(list)
        assertEquals(ACCESS_UNKNOWN, list.access)
    }

    @Test
    fun applyToSetsAccessUnknownWithFalseOwnerFalseShared() {
        val list = CaldavCalendar()
        val remote = TaskLists.TaskList(isOwner = false, isShared = false)
        remote.applyTo(list)
        assertEquals(ACCESS_UNKNOWN, list.access)
    }

    @Test
    fun applyToSetsAccessReadWriteNotOwnerButShared() {
        val list = CaldavCalendar()
        val remote = TaskLists.TaskList(isOwner = false, isShared = true)
        remote.applyTo(list)
        assertEquals(ACCESS_READ_WRITE, list.access)
    }

    @Test
    fun applyToSetsAllFieldsAtOnce() {
        val list = CaldavCalendar()
        val remote = TaskLists.TaskList(
            displayName = "Work Tasks",
            id = "work-list-id",
            isOwner = true,
        )
        remote.applyTo(list)
        assertEquals("Work Tasks", list.name)
        assertEquals("work-list-id", list.url)
        assertEquals("work-list-id", list.uuid)
        assertEquals(ACCESS_OWNER, list.access)
    }

    @Test
    fun applyToOverwritesExistingValues() {
        val list = CaldavCalendar(
            name = "Old Name",
            url = "old-url",
            uuid = "old-uuid",
            access = ACCESS_READ_WRITE,
        )
        val remote = TaskLists.TaskList(
            displayName = "New Name",
            id = "new-id",
            isOwner = true,
        )
        remote.applyTo(list)
        assertEquals("New Name", list.name)
        assertEquals("new-id", list.url)
        assertEquals("new-id", list.uuid)
        assertEquals(ACCESS_OWNER, list.access)
    }

    @Test
    fun applyToWithNullDisplayName() {
        val list = CaldavCalendar()
        val remote = TaskLists.TaskList(displayName = null, id = "id-1")
        remote.applyTo(list)
        assertNull(list.name)
    }

    @Test
    fun applyToWithNullId() {
        val list = CaldavCalendar()
        val remote = TaskLists.TaskList(displayName = "List", id = null)
        remote.applyTo(list)
        assertNull(list.url)
        assertNull(list.uuid)
    }

    // ===== TaskLists data classes =====

    @Test
    fun taskListsValueIsList() {
        val taskLists = TaskLists(
            context = "ctx",
            value = listOf(
                TaskLists.TaskList(id = "1"),
                TaskLists.TaskList(id = "2"),
            )
        )
        assertEquals(2, taskLists.value.size)
    }

    @Test
    fun taskListsNextPageDefault() {
        val taskLists = TaskLists(context = "ctx", value = emptyList())
        assertNull(taskLists.nextPage)
    }

    @Test
    fun taskListsNextPageSet() {
        val taskLists = TaskLists(
            context = "ctx",
            value = emptyList(),
            nextPage = "http://next"
        )
        assertEquals("http://next", taskLists.nextPage)
    }

    @Test
    fun taskListWellknownListName() {
        val tl = TaskLists.TaskList(wellknownListName = "defaultList")
        assertEquals("defaultList", tl.wellknownListName)
    }

    @Test
    fun taskListWellknownListNameDefault() {
        val tl = TaskLists.TaskList()
        assertNull(tl.wellknownListName)
    }

    @Test
    fun taskListEtag() {
        val tl = TaskLists.TaskList(etag = "etag-abc")
        assertEquals("etag-abc", tl.etag)
    }

    // ===== Tasks data classes =====

    @Test
    fun tasksValueIsList() {
        val tasks = Tasks(value = listOf(Tasks.Task(id = "t1")))
        assertEquals(1, tasks.value.size)
        assertEquals("t1", tasks.value[0].id)
    }

    @Test
    fun tasksNextPageDefault() {
        val tasks = Tasks(value = emptyList())
        assertNull(tasks.nextPage)
    }

    @Test
    fun tasksNextDeltaDefault() {
        val tasks = Tasks(value = emptyList())
        assertNull(tasks.nextDelta)
    }

    @Test
    fun tasksNextPageSet() {
        val tasks = Tasks(value = emptyList(), nextPage = "http://page2")
        assertEquals("http://page2", tasks.nextPage)
    }

    @Test
    fun tasksNextDeltaSet() {
        val tasks = Tasks(value = emptyList(), nextDelta = "http://delta")
        assertEquals("http://delta", tasks.nextDelta)
    }

    @Test
    fun taskDefaults() {
        val task = Tasks.Task()
        assertNull(task.id)
        assertNull(task.title)
        assertNull(task.body)
        assertEquals(Tasks.Task.Importance.low, task.importance)
        assertEquals(Tasks.Task.Status.notStarted, task.status)
        assertNull(task.categories)
        assertNull(task.completedDateTime)
        assertNull(task.dueDateTime)
        assertNull(task.recurrence)
        assertNull(task.checklistItems)
        assertNull(task.removed)
    }

    @Test
    fun taskRemovedIndicatesDeletion() {
        val task = Tasks.Task(
            id = "deleted-task",
            removed = Tasks.Task.Removed(reason = "deleted"),
        )
        assertNotNull(task.removed)
        assertEquals("deleted", task.removed!!.reason)
    }

    @Test
    fun taskRemovedNullByDefault() {
        val task = Tasks.Task(id = "normal-task")
        assertNull(task.removed)
    }

    @Test
    fun taskBody() {
        val body = Tasks.Task.Body(content = "Hello", contentType = "text")
        assertEquals("Hello", body.content)
        assertEquals("text", body.contentType)
    }

    @Test
    fun taskDateTime() {
        val dt = Tasks.Task.DateTime(
            dateTime = "2023-07-21T00:00:00.0000000",
            timeZone = "America/Chicago",
        )
        assertEquals("2023-07-21T00:00:00.0000000", dt.dateTime)
        assertEquals("America/Chicago", dt.timeZone)
    }

    @Test
    fun checklistItemDefaults() {
        val item = Tasks.Task.ChecklistItem(displayName = "item", isChecked = false)
        assertNull(item.id)
        assertNull(item.createdDateTime)
        assertNull(item.checkedDateTime)
        assertEquals("item", item.displayName)
        assertEquals(false, item.isChecked)
    }

    @Test
    fun checklistItemChecked() {
        val item = Tasks.Task.ChecklistItem(
            id = "chk-1",
            displayName = "Buy milk",
            isChecked = true,
            checkedDateTime = "2023-07-21T05:00:00.0000000Z",
        )
        assertEquals(true, item.isChecked)
        assertEquals("2023-07-21T05:00:00.0000000Z", item.checkedDateTime)
    }

    @Test
    fun recurrencePattern() {
        val pattern = Tasks.Task.Pattern(
            type = Tasks.Task.RecurrenceType.weekly,
            interval = 2,
            daysOfWeek = listOf(
                Tasks.Task.RecurrenceDayOfWeek.monday,
                Tasks.Task.RecurrenceDayOfWeek.friday,
            ),
        )
        assertEquals(Tasks.Task.RecurrenceType.weekly, pattern.type)
        assertEquals(2, pattern.interval)
        assertEquals(2, pattern.daysOfWeek.size)
    }

    @Test
    fun recurrencePatternDefaultMonth() {
        val pattern = Tasks.Task.Pattern(
            type = Tasks.Task.RecurrenceType.daily,
            interval = 1,
            daysOfWeek = emptyList(),
        )
        assertEquals(0, pattern.month)
        assertEquals(0, pattern.dayOfMonth)
    }

    @Test
    fun recurrenceMonthlyPattern() {
        val pattern = Tasks.Task.Pattern(
            type = Tasks.Task.RecurrenceType.absoluteMonthly,
            interval = 1,
            daysOfWeek = emptyList(),
            dayOfMonth = 15,
        )
        assertEquals(15, pattern.dayOfMonth)
    }

    @Test
    fun recurrenceYearlyPattern() {
        val pattern = Tasks.Task.Pattern(
            type = Tasks.Task.RecurrenceType.absoluteYearly,
            interval = 1,
            daysOfWeek = emptyList(),
            month = 12,
            dayOfMonth = 25,
        )
        assertEquals(12, pattern.month)
        assertEquals(25, pattern.dayOfMonth)
    }

    @Test
    fun recurrenceIndexDefault() {
        val pattern = Tasks.Task.Pattern(
            type = Tasks.Task.RecurrenceType.daily,
            interval = 1,
            daysOfWeek = emptyList(),
        )
        assertEquals(Tasks.Task.RecurrenceIndex.first, pattern.index)
    }

    @Test
    fun recurrenceFirstDayOfWeekDefault() {
        val pattern = Tasks.Task.Pattern(
            type = Tasks.Task.RecurrenceType.daily,
            interval = 1,
            daysOfWeek = emptyList(),
        )
        assertEquals(Tasks.Task.RecurrenceDayOfWeek.sunday, pattern.firstDayOfWeek)
    }

    // ===== Error data classes =====

    @Test
    fun errorDataClass() {
        val error = Error(
            error = Error.ErrorBody(
                code = "ResourceNotFound",
                message = "The resource was not found",
            )
        )
        assertEquals("ResourceNotFound", error.error.code)
        assertEquals("The resource was not found", error.error.message)
    }

    @Test
    fun errorBodyEquality() {
        val body1 = Error.ErrorBody(code = "NotFound", message = "msg")
        val body2 = Error.ErrorBody(code = "NotFound", message = "msg")
        assertEquals(body1, body2)
    }

    @Test
    fun errorBodyInequality() {
        val body1 = Error.ErrorBody(code = "NotFound", message = "msg")
        val body2 = Error.ErrorBody(code = "BadRequest", message = "msg")
        assertNotNull(body1)
        assertNotNull(body2)
        assert(body1 != body2)
    }

    // ===== Importance/Status enum values =====

    @Test
    fun importanceValues() {
        assertEquals(3, Tasks.Task.Importance.entries.size)
        assertNotNull(Tasks.Task.Importance.valueOf("low"))
        assertNotNull(Tasks.Task.Importance.valueOf("normal"))
        assertNotNull(Tasks.Task.Importance.valueOf("high"))
    }

    @Test
    fun statusValues() {
        assertEquals(5, Tasks.Task.Status.entries.size)
        assertNotNull(Tasks.Task.Status.valueOf("notStarted"))
        assertNotNull(Tasks.Task.Status.valueOf("inProgress"))
        assertNotNull(Tasks.Task.Status.valueOf("completed"))
        assertNotNull(Tasks.Task.Status.valueOf("waitingOnOthers"))
        assertNotNull(Tasks.Task.Status.valueOf("deferred"))
    }

    @Test
    fun recurrenceTypeValues() {
        assertEquals(6, Tasks.Task.RecurrenceType.entries.size)
        assertNotNull(Tasks.Task.RecurrenceType.valueOf("daily"))
        assertNotNull(Tasks.Task.RecurrenceType.valueOf("weekly"))
        assertNotNull(Tasks.Task.RecurrenceType.valueOf("absoluteMonthly"))
        assertNotNull(Tasks.Task.RecurrenceType.valueOf("relativeMonthly"))
        assertNotNull(Tasks.Task.RecurrenceType.valueOf("absoluteYearly"))
        assertNotNull(Tasks.Task.RecurrenceType.valueOf("relativeYearly"))
    }

    @Test
    fun recurrenceDayOfWeekValues() {
        assertEquals(7, Tasks.Task.RecurrenceDayOfWeek.entries.size)
    }

    @Test
    fun recurrenceIndexValues() {
        assertEquals(5, Tasks.Task.RecurrenceIndex.entries.size)
    }

    // ===== JSON serialization round-trip for Tasks.Task =====

    @Test
    fun taskSerializationRoundTrip() {
        val json = kotlinx.serialization.json.Json {
            ignoreUnknownKeys = true
        }
        val task = Tasks.Task(
            id = "abc-123",
            title = "Test Task",
            importance = Tasks.Task.Importance.high,
            status = Tasks.Task.Status.completed,
        )
        val encoded = json.encodeToString(task)
        val decoded = json.decodeFromString<Tasks.Task>(encoded)
        assertEquals(task.id, decoded.id)
        assertEquals(task.title, decoded.title)
        assertEquals(task.importance, decoded.importance)
        assertEquals(task.status, decoded.status)
    }

    @Test
    fun checklistItemSerializationRoundTrip() {
        val json = kotlinx.serialization.json.Json {
            ignoreUnknownKeys = true
        }
        val item = Tasks.Task.ChecklistItem(
            id = "chk-1",
            displayName = "Sub item",
            isChecked = true,
            checkedDateTime = "2023-07-21T12:00:00.0000000Z",
        )
        val encoded = json.encodeToString(item)
        val decoded = json.decodeFromString<Tasks.Task.ChecklistItem>(encoded)
        assertEquals(item.id, decoded.id)
        assertEquals(item.displayName, decoded.displayName)
        assertEquals(item.isChecked, decoded.isChecked)
        assertEquals(item.checkedDateTime, decoded.checkedDateTime)
    }

    @Test
    fun taskListSerializationRoundTrip() {
        val json = kotlinx.serialization.json.Json {
            ignoreUnknownKeys = true
        }
        val taskList = TaskLists.TaskList(
            id = "list-1",
            displayName = "My List",
            isOwner = true,
            isShared = false,
            wellknownListName = "defaultList",
        )
        val encoded = json.encodeToString(taskList)
        val decoded = json.decodeFromString<TaskLists.TaskList>(encoded)
        assertEquals(taskList.id, decoded.id)
        assertEquals(taskList.displayName, decoded.displayName)
        assertEquals(taskList.isOwner, decoded.isOwner)
        assertEquals(taskList.isShared, decoded.isShared)
        assertEquals(taskList.wellknownListName, decoded.wellknownListName)
    }

    @Test
    fun errorSerializationRoundTrip() {
        val json = kotlinx.serialization.json.Json {
            ignoreUnknownKeys = true
        }
        val error = Error(
            error = Error.ErrorBody(code = "syncStateNotFound", message = "Sync state not found")
        )
        val encoded = json.encodeToString(error)
        val decoded = json.decodeFromString<Error>(encoded)
        assertEquals(error.error.code, decoded.error.code)
        assertEquals(error.error.message, decoded.error.message)
    }
}
