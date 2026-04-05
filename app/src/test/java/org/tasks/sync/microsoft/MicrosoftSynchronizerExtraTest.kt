package org.tasks.sync.microsoft

import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.tasks.data.entity.CaldavCalendar

class MicrosoftSynchronizerExtraTest {

    private val json = Json { ignoreUnknownKeys = true }

    // ===== JSON deserialization for Tasks with deltaLink / nextLink =====

    @Test
    fun deserializeTasksWithDeltaLink() {
        val input = """
            {
                "value": [],
                "@odata.deltaLink": "https://graph.microsoft.com/delta?token=abc123"
            }
        """.trimIndent()
        val tasks = json.decodeFromString<Tasks>(input)
        assertEquals("https://graph.microsoft.com/delta?token=abc123", tasks.nextDelta)
        assertNull(tasks.nextPage)
    }

    @Test
    fun deserializeTasksWithNextLink() {
        val input = """
            {
                "value": [],
                "@odata.nextLink": "https://graph.microsoft.com/next?page=2"
            }
        """.trimIndent()
        val tasks = json.decodeFromString<Tasks>(input)
        assertEquals("https://graph.microsoft.com/next?page=2", tasks.nextPage)
        assertNull(tasks.nextDelta)
    }

    @Test
    fun deserializeTasksWithBothLinks() {
        val input = """
            {
                "value": [],
                "@odata.nextLink": "https://next",
                "@odata.deltaLink": "https://delta"
            }
        """.trimIndent()
        val tasks = json.decodeFromString<Tasks>(input)
        assertEquals("https://next", tasks.nextPage)
        assertEquals("https://delta", tasks.nextDelta)
    }

    @Test
    fun deserializeTasksWithNoLinks() {
        val input = """{ "value": [] }"""
        val tasks = json.decodeFromString<Tasks>(input)
        assertNull(tasks.nextPage)
        assertNull(tasks.nextDelta)
    }

    // ===== JSON deserialization for Tasks.Task with @removed =====

    @Test
    fun deserializeTaskWithRemoved() {
        val input = """
            {
                "id": "task-1",
                "@removed": { "reason": "deleted" }
            }
        """.trimIndent()
        val task = json.decodeFromString<Tasks.Task>(input)
        assertNotNull(task.removed)
        assertEquals("deleted", task.removed!!.reason)
    }

    @Test
    fun deserializeTaskWithoutRemoved() {
        val input = """{ "id": "task-2", "title": "Test" }"""
        val task = json.decodeFromString<Tasks.Task>(input)
        assertNull(task.removed)
    }

    // ===== JSON deserialization for full task with checklist items =====

    @Test
    fun deserializeTaskWithChecklistItems() {
        val input = """
            {
                "id": "task-3",
                "title": "Parent Task",
                "checklistItems": [
                    { "id": "chk-1", "displayName": "Sub 1", "isChecked": false },
                    { "id": "chk-2", "displayName": "Sub 2", "isChecked": true, "checkedDateTime": "2024-01-01T00:00:00Z" }
                ]
            }
        """.trimIndent()
        val task = json.decodeFromString<Tasks.Task>(input)
        assertEquals(2, task.checklistItems!!.size)
        assertEquals("Sub 1", task.checklistItems!![0].displayName)
        assertEquals(false, task.checklistItems!![0].isChecked)
        assertEquals(true, task.checklistItems!![1].isChecked)
    }

    @Test
    fun deserializeTaskWithEmptyChecklistItems() {
        val input = """
            {
                "id": "task-4",
                "checklistItems": []
            }
        """.trimIndent()
        val task = json.decodeFromString<Tasks.Task>(input)
        assertNotNull(task.checklistItems)
        assertTrue(task.checklistItems!!.isEmpty())
    }

    @Test
    fun deserializeTaskWithNullChecklistItems() {
        val input = """{ "id": "task-5" }"""
        val task = json.decodeFromString<Tasks.Task>(input)
        assertNull(task.checklistItems)
    }

    // ===== JSON deserialization for task with recurrence =====

    @Test
    fun deserializeTaskWithRecurrence() {
        val input = """
            {
                "id": "task-6",
                "recurrence": {
                    "pattern": {
                        "type": "weekly",
                        "interval": 2,
                        "daysOfWeek": ["monday", "friday"]
                    }
                }
            }
        """.trimIndent()
        val task = json.decodeFromString<Tasks.Task>(input)
        assertNotNull(task.recurrence)
        assertEquals(Tasks.Task.RecurrenceType.weekly, task.recurrence!!.pattern.type)
        assertEquals(2, task.recurrence!!.pattern.interval)
        assertEquals(2, task.recurrence!!.pattern.daysOfWeek.size)
        assertEquals(Tasks.Task.RecurrenceDayOfWeek.monday, task.recurrence!!.pattern.daysOfWeek[0])
        assertEquals(Tasks.Task.RecurrenceDayOfWeek.friday, task.recurrence!!.pattern.daysOfWeek[1])
    }

    @Test
    fun deserializeTaskWithoutRecurrence() {
        val input = """{ "id": "task-7" }"""
        val task = json.decodeFromString<Tasks.Task>(input)
        assertNull(task.recurrence)
    }

    // ===== JSON deserialization for task with importance and status =====

    @Test
    fun deserializeTaskDefaultImportance() {
        val input = """{ "id": "task-8" }"""
        val task = json.decodeFromString<Tasks.Task>(input)
        assertEquals(Tasks.Task.Importance.low, task.importance)
    }

    @Test
    fun deserializeTaskHighImportance() {
        val input = """{ "id": "task-9", "importance": "high" }"""
        val task = json.decodeFromString<Tasks.Task>(input)
        assertEquals(Tasks.Task.Importance.high, task.importance)
    }

    @Test
    fun deserializeTaskDefaultStatus() {
        val input = """{ "id": "task-10" }"""
        val task = json.decodeFromString<Tasks.Task>(input)
        assertEquals(Tasks.Task.Status.notStarted, task.status)
    }

    @Test
    fun deserializeTaskCompletedStatus() {
        val input = """{ "id": "task-11", "status": "completed" }"""
        val task = json.decodeFromString<Tasks.Task>(input)
        assertEquals(Tasks.Task.Status.completed, task.status)
    }

    @Test
    fun deserializeTaskDeferredStatus() {
        val input = """{ "id": "task-12", "status": "deferred" }"""
        val task = json.decodeFromString<Tasks.Task>(input)
        assertEquals(Tasks.Task.Status.deferred, task.status)
    }

    // ===== JSON deserialization for task with body =====

    @Test
    fun deserializeTaskWithBody() {
        val input = """
            {
                "id": "task-13",
                "body": { "content": "Notes here", "contentType": "text" }
            }
        """.trimIndent()
        val task = json.decodeFromString<Tasks.Task>(input)
        assertNotNull(task.body)
        assertEquals("Notes here", task.body!!.content)
        assertEquals("text", task.body!!.contentType)
    }

    @Test
    fun deserializeTaskWithNullBody() {
        val input = """{ "id": "task-14" }"""
        val task = json.decodeFromString<Tasks.Task>(input)
        assertNull(task.body)
    }

    // ===== JSON deserialization for task with dueDateTime and completedDateTime =====

    @Test
    fun deserializeTaskWithDueDateTime() {
        val input = """
            {
                "id": "task-15",
                "dueDateTime": {
                    "dateTime": "2024-06-15T00:00:00.0000000",
                    "timeZone": "UTC"
                }
            }
        """.trimIndent()
        val task = json.decodeFromString<Tasks.Task>(input)
        assertNotNull(task.dueDateTime)
        assertEquals("2024-06-15T00:00:00.0000000", task.dueDateTime!!.dateTime)
        assertEquals("UTC", task.dueDateTime!!.timeZone)
    }

    @Test
    fun deserializeTaskWithCompletedDateTime() {
        val input = """
            {
                "id": "task-16",
                "completedDateTime": {
                    "dateTime": "2024-06-20T14:30:00.0000000",
                    "timeZone": "America/New_York"
                }
            }
        """.trimIndent()
        val task = json.decodeFromString<Tasks.Task>(input)
        assertNotNull(task.completedDateTime)
        assertEquals("2024-06-20T14:30:00.0000000", task.completedDateTime!!.dateTime)
        assertEquals("America/New_York", task.completedDateTime!!.timeZone)
    }

    // ===== JSON deserialization for task with categories =====

    @Test
    fun deserializeTaskWithCategories() {
        val input = """
            {
                "id": "task-17",
                "categories": ["work", "urgent"]
            }
        """.trimIndent()
        val task = json.decodeFromString<Tasks.Task>(input)
        assertEquals(listOf("work", "urgent"), task.categories)
    }

    @Test
    fun deserializeTaskWithEmptyCategories() {
        val input = """{ "id": "task-18", "categories": [] }"""
        val task = json.decodeFromString<Tasks.Task>(input)
        assertNotNull(task.categories)
        assertTrue(task.categories!!.isEmpty())
    }

    @Test
    fun deserializeTaskWithNullCategories() {
        val input = """{ "id": "task-19" }"""
        val task = json.decodeFromString<Tasks.Task>(input)
        assertNull(task.categories)
    }

    // ===== JSON deserialization ignores unknown keys =====

    @Test
    fun deserializeTaskIgnoresUnknownKeys() {
        val input = """
            {
                "id": "task-20",
                "title": "Test",
                "unknownField": "value",
                "anotherUnknown": 42
            }
        """.trimIndent()
        val task = json.decodeFromString<Tasks.Task>(input)
        assertEquals("task-20", task.id)
        assertEquals("Test", task.title)
    }

    // ===== JSON deserialization for TaskLists =====

    @Test
    fun deserializeTaskListsWithNextPage() {
        val input = """
            {
                "@odata.context": "https://graph.microsoft.com/v1.0/metadata",
                "value": [
                    { "id": "list-1", "displayName": "My Tasks" },
                    { "id": "list-2", "displayName": "Work" }
                ],
                "@odata.nextLink": "https://graph.microsoft.com/next"
            }
        """.trimIndent()
        val taskLists = json.decodeFromString<TaskLists>(input)
        assertEquals(2, taskLists.value.size)
        assertEquals("list-1", taskLists.value[0].id)
        assertEquals("My Tasks", taskLists.value[0].displayName)
        assertEquals("https://graph.microsoft.com/next", taskLists.nextPage)
    }

    @Test
    fun deserializeTaskListsWithoutNextPage() {
        val input = """
            {
                "@odata.context": "ctx",
                "value": []
            }
        """.trimIndent()
        val taskLists = json.decodeFromString<TaskLists>(input)
        assertTrue(taskLists.value.isEmpty())
        assertNull(taskLists.nextPage)
    }

    // ===== Error JSON deserialization =====

    @Test
    fun deserializeError() {
        val input = """
            {
                "error": {
                    "code": "syncStateNotFound",
                    "message": "The sync state was not found"
                }
            }
        """.trimIndent()
        val error = json.decodeFromString<Error>(input)
        assertEquals("syncStateNotFound", error.error.code)
        assertEquals("The sync state was not found", error.error.message)
    }

    @Test
    fun deserializeErrorResourceNotFound() {
        val input = """
            {
                "error": {
                    "code": "ResourceNotFound",
                    "message": "Resource not found"
                }
            }
        """.trimIndent()
        val error = json.decodeFromString<Error>(input)
        assertEquals("ResourceNotFound", error.error.code)
    }

    // ===== JSON round-trip for Tasks.Task encoding =====

    @Test
    fun encodeDecodeTaskWithAllFields() {
        val original = Tasks.Task(
            id = "round-trip-1",
            title = "Full Task",
            body = Tasks.Task.Body(content = "Description", contentType = "text"),
            importance = Tasks.Task.Importance.normal,
            status = Tasks.Task.Status.inProgress,
            categories = listOf("tag1", "tag2"),
            dueDateTime = Tasks.Task.DateTime(
                dateTime = "2024-12-25T00:00:00.0000000",
                timeZone = "America/Chicago",
            ),
            completedDateTime = null,
            recurrence = Tasks.Task.Recurrence(
                pattern = Tasks.Task.Pattern(
                    type = Tasks.Task.RecurrenceType.daily,
                    interval = 1,
                    daysOfWeek = emptyList(),
                )
            ),
            checklistItems = listOf(
                Tasks.Task.ChecklistItem(
                    id = "chk-rt-1",
                    displayName = "Sub",
                    isChecked = false,
                )
            ),
        )
        val encoded = json.encodeToString(original)
        val decoded = json.decodeFromString<Tasks.Task>(encoded)
        assertEquals(original.id, decoded.id)
        assertEquals(original.title, decoded.title)
        assertEquals(original.body?.content, decoded.body?.content)
        assertEquals(original.importance, decoded.importance)
        assertEquals(original.status, decoded.status)
        assertEquals(original.categories, decoded.categories)
        assertEquals(original.dueDateTime?.dateTime, decoded.dueDateTime?.dateTime)
        assertEquals(original.recurrence?.pattern?.type, decoded.recurrence?.pattern?.type)
        assertEquals(1, decoded.checklistItems!!.size)
    }

    // ===== TaskLists.TaskList.applyTo edge cases =====

    @Test
    fun applyToPreservesAccountField() {
        val list = CaldavCalendar(account = "my-account")
        val remote = TaskLists.TaskList(
            id = "new-id",
            displayName = "New List",
            isOwner = true,
        )
        remote.applyTo(list)
        assertEquals("my-account", list.account)
        assertEquals("new-id", list.uuid)
        assertEquals("New List", list.name)
    }

    @Test
    fun applyToDoesNotResetCtag() {
        val list = CaldavCalendar(ctag = "some-ctag")
        val remote = TaskLists.TaskList(id = "id-1")
        remote.applyTo(list)
        assertEquals("some-ctag", list.ctag)
    }

    @Test
    fun applyToWithEmptyStringId() {
        val list = CaldavCalendar()
        val remote = TaskLists.TaskList(id = "")
        remote.applyTo(list)
        assertEquals("", list.url)
        assertEquals("", list.uuid)
    }

    // ===== Recurrence patterns edge cases =====

    @Test
    fun relativeMonthlyRecurrenceType() {
        val pattern = Tasks.Task.Pattern(
            type = Tasks.Task.RecurrenceType.relativeMonthly,
            interval = 1,
            daysOfWeek = listOf(Tasks.Task.RecurrenceDayOfWeek.tuesday),
            index = Tasks.Task.RecurrenceIndex.second,
        )
        assertEquals(Tasks.Task.RecurrenceType.relativeMonthly, pattern.type)
        assertEquals(Tasks.Task.RecurrenceIndex.second, pattern.index)
        assertEquals(1, pattern.daysOfWeek.size)
    }

    @Test
    fun relativeYearlyRecurrenceType() {
        val pattern = Tasks.Task.Pattern(
            type = Tasks.Task.RecurrenceType.relativeYearly,
            interval = 1,
            daysOfWeek = listOf(Tasks.Task.RecurrenceDayOfWeek.thursday),
            index = Tasks.Task.RecurrenceIndex.last,
            month = 11,
        )
        assertEquals(Tasks.Task.RecurrenceType.relativeYearly, pattern.type)
        assertEquals(Tasks.Task.RecurrenceIndex.last, pattern.index)
        assertEquals(11, pattern.month)
    }

    @Test
    fun allDaysOfWeekInPattern() {
        val pattern = Tasks.Task.Pattern(
            type = Tasks.Task.RecurrenceType.weekly,
            interval = 1,
            daysOfWeek = Tasks.Task.RecurrenceDayOfWeek.entries.toList(),
        )
        assertEquals(7, pattern.daysOfWeek.size)
    }

    // ===== JSON deserialization for reminderDateTime =====

    @Test
    fun deserializeTaskWithReminderDateTime() {
        val input = """
            {
                "id": "task-reminder",
                "isReminderOn": true,
                "reminderDateTime": {
                    "dateTime": "2024-06-15T09:00:00.0000000",
                    "timeZone": "UTC"
                }
            }
        """.trimIndent()
        val task = json.decodeFromString<Tasks.Task>(input)
        assertTrue(task.isReminderOn)
        assertNotNull(task.reminderDateTime)
        assertEquals("2024-06-15T09:00:00.0000000", task.reminderDateTime!!.dateTime)
    }

    @Test
    fun deserializeTaskReminderOffByDefault() {
        val input = """{ "id": "task-no-reminder" }"""
        val task = json.decodeFromString<Tasks.Task>(input)
        assertEquals(false, task.isReminderOn)
        assertNull(task.reminderDateTime)
    }

    // ===== JSON deserialization for TaskList with wellknownListName =====

    @Test
    fun deserializeTaskListWithWellknown() {
        val input = """
            {
                "@odata.context": "ctx",
                "value": [
                    { "id": "default-1", "displayName": "Tasks", "wellknownListName": "defaultList", "isOwner": true },
                    { "id": "flagged-1", "displayName": "Flagged", "wellknownListName": "flaggedEmails" }
                ]
            }
        """.trimIndent()
        val taskLists = json.decodeFromString<TaskLists>(input)
        assertEquals("defaultList", taskLists.value[0].wellknownListName)
        assertEquals("flaggedEmails", taskLists.value[1].wellknownListName)
    }

    // ===== JSON deserialization for full task list response =====

    @Test
    fun deserializeFullTaskListResponse() {
        val input = """
            {
                "value": [
                    {
                        "@odata.etag": "etag-1",
                        "id": "task-full",
                        "title": "Full Task",
                        "body": { "content": "Notes", "contentType": "text" },
                        "importance": "high",
                        "status": "completed",
                        "categories": ["work"],
                        "isReminderOn": false,
                        "createdDateTime": "2024-01-01T00:00:00.0000000Z",
                        "lastModifiedDateTime": "2024-06-01T12:00:00.0000000Z",
                        "completedDateTime": { "dateTime": "2024-05-01T00:00:00.0000000", "timeZone": "UTC" },
                        "dueDateTime": { "dateTime": "2024-04-01T00:00:00.0000000", "timeZone": "UTC" }
                    }
                ]
            }
        """.trimIndent()
        val tasks = json.decodeFromString<Tasks>(input)
        assertEquals(1, tasks.value.size)
        val task = tasks.value[0]
        assertEquals("etag-1", task.etag)
        assertEquals("task-full", task.id)
        assertEquals("Full Task", task.title)
        assertEquals("Notes", task.body!!.content)
        assertEquals(Tasks.Task.Importance.high, task.importance)
        assertEquals(Tasks.Task.Status.completed, task.status)
        assertEquals(listOf("work"), task.categories)
        assertEquals("2024-01-01T00:00:00.0000000Z", task.createdDateTime)
        assertEquals("2024-06-01T12:00:00.0000000Z", task.lastModifiedDateTime)
        assertNotNull(task.completedDateTime)
        assertNotNull(task.dueDateTime)
    }
}
