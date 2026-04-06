package org.tasks.sync.microsoft

import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.tasks.data.entity.CaldavCalendar
import org.tasks.data.entity.CaldavCalendar.Companion.ACCESS_OWNER
import org.tasks.data.entity.CaldavCalendar.Companion.ACCESS_READ_WRITE
import org.tasks.data.entity.CaldavCalendar.Companion.ACCESS_UNKNOWN

/**
 * Tests for [MicrosoftListSettingsActivityViewModel.ViewState] and additional
 * data class / serialization coverage for the Microsoft sync package.
 */
class MicrosoftViewModelTest {

    private val json = Json { ignoreUnknownKeys = true }

    // ===== ViewState tests =====

    @Test
    fun viewStateDefaults() {
        val state = MicrosoftListSettingsActivityViewModel.ViewState()
        assertFalse(state.requestInFlight)
        assertNull(state.result)
        assertNull(state.error)
        assertFalse(state.deleted)
    }

    @Test
    fun viewStateRequestInFlight() {
        val state = MicrosoftListSettingsActivityViewModel.ViewState(requestInFlight = true)
        assertTrue(state.requestInFlight)
    }

    @Test
    fun viewStateWithResult() {
        val calendar = CaldavCalendar(name = "Test")
        val state = MicrosoftListSettingsActivityViewModel.ViewState(result = calendar)
        assertNotNull(state.result)
        assertEquals("Test", state.result!!.name)
    }

    @Test
    fun viewStateWithError() {
        val error = RuntimeException("fail")
        val state = MicrosoftListSettingsActivityViewModel.ViewState(error = error)
        assertNotNull(state.error)
        assertEquals("fail", state.error!!.message)
    }

    @Test
    fun viewStateDeleted() {
        val state = MicrosoftListSettingsActivityViewModel.ViewState(deleted = true)
        assertTrue(state.deleted)
    }

    @Test
    fun viewStateCopy() {
        val state = MicrosoftListSettingsActivityViewModel.ViewState()
        val updated = state.copy(requestInFlight = true, deleted = true)
        assertTrue(updated.requestInFlight)
        assertTrue(updated.deleted)
        assertFalse(state.requestInFlight)
        assertFalse(state.deleted)
    }

    // ===== TaskLists.TaskList.applyTo — additional mutation-killing =====

    @Test
    fun applyToNotOwnerNotSharedSetsUnknown() {
        val list = CaldavCalendar()
        val remote = TaskLists.TaskList(isOwner = false, isShared = false)
        remote.applyTo(list)
        assertEquals(ACCESS_UNKNOWN, list.access)
    }

    @Test
    fun applyToOwnerFalseMeansNotOwner() {
        val list = CaldavCalendar()
        val remote = TaskLists.TaskList(isOwner = false, isShared = true)
        remote.applyTo(list)
        assertEquals(ACCESS_READ_WRITE, list.access)
    }

    @Test
    fun applyToNullOwnerNullSharedMeansUnknown() {
        val list = CaldavCalendar()
        val remote = TaskLists.TaskList(isOwner = null, isShared = null)
        remote.applyTo(list)
        assertEquals(ACCESS_UNKNOWN, list.access)
    }

    @Test
    fun applyToTrueTrueMeansOwner() {
        val list = CaldavCalendar()
        val remote = TaskLists.TaskList(isOwner = true, isShared = true)
        remote.applyTo(list)
        assertEquals(ACCESS_OWNER, list.access)
    }

    // ===== JSON encoding covers all fields =====

    @Test
    fun encodeTaskWithBody() {
        val task = Tasks.Task(
            id = "task-1",
            title = "Test",
            body = Tasks.Task.Body(content = "notes", contentType = "text"),
        )
        val encoded = json.encodeToString(task)
        assertTrue(encoded.contains("\"notes\""))
        assertTrue(encoded.contains("\"text\""))
    }

    @Test
    fun encodeTaskWithNullBody() {
        val task = Tasks.Task(
            id = "task-2",
            title = "Test",
            body = null,
        )
        val encoded = json.encodeToString(task)
        // body should be encoded as null
        assertTrue(encoded.contains("\"body\":null"))
    }

    @Test
    fun encodeTaskWithRecurrenceAndDecodeMatches() {
        val task = Tasks.Task(
            id = "task-rec",
            recurrence = Tasks.Task.Recurrence(
                pattern = Tasks.Task.Pattern(
                    type = Tasks.Task.RecurrenceType.absoluteYearly,
                    interval = 1,
                    daysOfWeek = emptyList(),
                    month = 12,
                    dayOfMonth = 25,
                )
            )
        )
        val encoded = json.encodeToString(task)
        val decoded = json.decodeFromString<Tasks.Task>(encoded)
        assertEquals(Tasks.Task.RecurrenceType.absoluteYearly, decoded.recurrence!!.pattern.type)
        assertEquals(12, decoded.recurrence!!.pattern.month)
        assertEquals(25, decoded.recurrence!!.pattern.dayOfMonth)
    }

    @Test
    fun encodeTaskWithCompletedDateTime() {
        val task = Tasks.Task(
            id = "task-comp",
            status = Tasks.Task.Status.completed,
            completedDateTime = Tasks.Task.DateTime(
                dateTime = "2024-01-15T10:30:00.0000000",
                timeZone = "UTC",
            ),
        )
        val encoded = json.encodeToString(task)
        val decoded = json.decodeFromString<Tasks.Task>(encoded)
        assertEquals(Tasks.Task.Status.completed, decoded.status)
        assertNotNull(decoded.completedDateTime)
        assertEquals("UTC", decoded.completedDateTime!!.timeZone)
    }

    @Test
    fun encodeTaskWithNullCompletedDateTime() {
        val task = Tasks.Task(
            id = "task-nocomp",
            completedDateTime = null,
        )
        val encoded = json.encodeToString(task)
        val decoded = json.decodeFromString<Tasks.Task>(encoded)
        assertNull(decoded.completedDateTime)
    }

    @Test
    fun encodeTaskWithDueDateTime() {
        val task = Tasks.Task(
            id = "task-due",
            dueDateTime = Tasks.Task.DateTime(
                dateTime = "2024-07-15T00:00:00.0000000",
                timeZone = "America/Chicago",
            ),
        )
        val encoded = json.encodeToString(task)
        val decoded = json.decodeFromString<Tasks.Task>(encoded)
        assertNotNull(decoded.dueDateTime)
        assertEquals("America/Chicago", decoded.dueDateTime!!.timeZone)
    }

    @Test
    fun encodeTaskWithNullDueDateTime() {
        val task = Tasks.Task(
            id = "task-nodue",
            dueDateTime = null,
        )
        val encoded = json.encodeToString(task)
        val decoded = json.decodeFromString<Tasks.Task>(encoded)
        assertNull(decoded.dueDateTime)
    }

    @Test
    fun encodeTaskWithNullRecurrence() {
        val task = Tasks.Task(
            id = "task-norec",
            recurrence = null,
        )
        val encoded = json.encodeToString(task)
        val decoded = json.decodeFromString<Tasks.Task>(encoded)
        assertNull(decoded.recurrence)
    }

    // ===== Tasks data class equality =====

    @Test
    fun tasksDataClassEquality() {
        val t1 = Tasks(value = emptyList(), nextPage = "page1")
        val t2 = Tasks(value = emptyList(), nextPage = "page1")
        assertEquals(t1, t2)
    }

    @Test
    fun taskDataClassEquality() {
        val t1 = Tasks.Task(id = "1", title = "A")
        val t2 = Tasks.Task(id = "1", title = "A")
        assertEquals(t1, t2)
    }

    @Test
    fun taskBodyEquality() {
        val b1 = Tasks.Task.Body(content = "x", contentType = "text")
        val b2 = Tasks.Task.Body(content = "x", contentType = "text")
        assertEquals(b1, b2)
    }

    @Test
    fun taskDateTimeEquality() {
        val d1 = Tasks.Task.DateTime(dateTime = "2024-01-01", timeZone = "UTC")
        val d2 = Tasks.Task.DateTime(dateTime = "2024-01-01", timeZone = "UTC")
        assertEquals(d1, d2)
    }

    @Test
    fun checklistItemEquality() {
        val c1 = Tasks.Task.ChecklistItem(displayName = "x", isChecked = true)
        val c2 = Tasks.Task.ChecklistItem(displayName = "x", isChecked = true)
        assertEquals(c1, c2)
    }

    @Test
    fun removedEquality() {
        val r1 = Tasks.Task.Removed(reason = "deleted")
        val r2 = Tasks.Task.Removed(reason = "deleted")
        assertEquals(r1, r2)
    }

    @Test
    fun patternEquality() {
        val p1 = Tasks.Task.Pattern(
            type = Tasks.Task.RecurrenceType.daily,
            interval = 1,
            daysOfWeek = emptyList(),
        )
        val p2 = Tasks.Task.Pattern(
            type = Tasks.Task.RecurrenceType.daily,
            interval = 1,
            daysOfWeek = emptyList(),
        )
        assertEquals(p1, p2)
    }

    @Test
    fun recurrenceEquality() {
        val r1 = Tasks.Task.Recurrence(
            pattern = Tasks.Task.Pattern(
                type = Tasks.Task.RecurrenceType.daily,
                interval = 1,
                daysOfWeek = emptyList(),
            )
        )
        val r2 = Tasks.Task.Recurrence(
            pattern = Tasks.Task.Pattern(
                type = Tasks.Task.RecurrenceType.daily,
                interval = 1,
                daysOfWeek = emptyList(),
            )
        )
        assertEquals(r1, r2)
    }

    // ===== Error data class =====

    @Test
    fun errorEquality() {
        val e1 = Error(error = Error.ErrorBody(code = "a", message = "b"))
        val e2 = Error(error = Error.ErrorBody(code = "a", message = "b"))
        assertEquals(e1, e2)
    }

    @Test
    fun errorHashCode() {
        val e1 = Error(error = Error.ErrorBody(code = "a", message = "b"))
        val e2 = Error(error = Error.ErrorBody(code = "a", message = "b"))
        assertEquals(e1.hashCode(), e2.hashCode())
    }

    // ===== TaskLists data class equality =====

    @Test
    fun taskListsEquality() {
        val tl1 = TaskLists(context = "ctx", value = emptyList())
        val tl2 = TaskLists(context = "ctx", value = emptyList())
        assertEquals(tl1, tl2)
    }

    @Test
    fun taskListEquality() {
        val t1 = TaskLists.TaskList(id = "1", displayName = "A")
        val t2 = TaskLists.TaskList(id = "1", displayName = "A")
        assertEquals(t1, t2)
    }

    // ===== JSON round-trip for TaskLists =====

    @Test
    fun taskListsWithAllFieldsRoundTrip() {
        val original = TaskLists(
            context = "https://graph.microsoft.com",
            value = listOf(
                TaskLists.TaskList(
                    etag = "etag-1",
                    displayName = "My List",
                    isOwner = true,
                    isShared = false,
                    wellknownListName = "defaultList",
                    id = "list-1",
                )
            ),
            nextPage = "https://next",
        )
        val encoded = json.encodeToString(original)
        val decoded = json.decodeFromString<TaskLists>(encoded)
        assertEquals(original.context, decoded.context)
        assertEquals(original.value.size, decoded.value.size)
        assertEquals(original.nextPage, decoded.nextPage)
        assertEquals(original.value[0].id, decoded.value[0].id)
        assertEquals(original.value[0].displayName, decoded.value[0].displayName)
    }

    // ===== Recurrence index and firstDayOfWeek =====

    @Test
    fun patternWithCustomIndex() {
        val pattern = Tasks.Task.Pattern(
            type = Tasks.Task.RecurrenceType.relativeMonthly,
            interval = 1,
            daysOfWeek = listOf(Tasks.Task.RecurrenceDayOfWeek.monday),
            index = Tasks.Task.RecurrenceIndex.third,
        )
        assertEquals(Tasks.Task.RecurrenceIndex.third, pattern.index)
    }

    @Test
    fun patternWithCustomFirstDayOfWeek() {
        val pattern = Tasks.Task.Pattern(
            type = Tasks.Task.RecurrenceType.weekly,
            interval = 1,
            daysOfWeek = emptyList(),
            firstDayOfWeek = Tasks.Task.RecurrenceDayOfWeek.monday,
        )
        assertEquals(Tasks.Task.RecurrenceDayOfWeek.monday, pattern.firstDayOfWeek)
    }

    @Test
    fun allRecurrenceIndexValues() {
        val values = Tasks.Task.RecurrenceIndex.entries
        assertEquals(5, values.size)
        assertTrue(values.contains(Tasks.Task.RecurrenceIndex.first))
        assertTrue(values.contains(Tasks.Task.RecurrenceIndex.second))
        assertTrue(values.contains(Tasks.Task.RecurrenceIndex.third))
        assertTrue(values.contains(Tasks.Task.RecurrenceIndex.fourth))
        assertTrue(values.contains(Tasks.Task.RecurrenceIndex.last))
    }

    // ===== Encode/decode with @odata fields =====

    @Test
    fun deserializeTaskWithEtag() {
        val input = """{ "@odata.etag": "W/\"123\"", "id": "task-etag" }"""
        val task = json.decodeFromString<Tasks.Task>(input)
        assertEquals("W/\"123\"", task.etag)
        assertEquals("task-etag", task.id)
    }

    @Test
    fun encodeDecodeTaskPreservesEtag() {
        val task = Tasks.Task(etag = "etag-value", id = "task-1")
        val encoded = json.encodeToString(task)
        val decoded = json.decodeFromString<Tasks.Task>(encoded)
        assertEquals("etag-value", decoded.etag)
    }

    // ===== Status enum =====

    @Test
    fun inProgressStatus() {
        val task = Tasks.Task(status = Tasks.Task.Status.inProgress)
        assertEquals(Tasks.Task.Status.inProgress, task.status)
    }

    @Test
    fun waitingOnOthersStatus() {
        val task = Tasks.Task(status = Tasks.Task.Status.waitingOnOthers)
        assertEquals(Tasks.Task.Status.waitingOnOthers, task.status)
    }

    @Test
    fun deferredStatus() {
        val task = Tasks.Task(status = Tasks.Task.Status.deferred)
        assertEquals(Tasks.Task.Status.deferred, task.status)
    }
}
