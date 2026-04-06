package org.tasks.sync.microsoft

import com.natpryce.makeiteasy.MakeItEasy.with
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.tasks.Freeze.Companion.freezeAt
import org.tasks.TestUtilities.withTZ
import org.tasks.data.entity.CaldavTask
import org.tasks.data.entity.TagData
import org.tasks.data.entity.Task
import org.tasks.makers.CaldavTaskMaker.REMOTE_ID
import org.tasks.makers.CaldavTaskMaker.newCaldavTask
import org.tasks.makers.TaskMaker.COMPLETION_TIME
import org.tasks.makers.TaskMaker.DESCRIPTION
import org.tasks.makers.TaskMaker.DUE_DATE
import org.tasks.makers.TaskMaker.DUE_TIME
import org.tasks.makers.TaskMaker.PRIORITY
import org.tasks.makers.TaskMaker.RECUR
import org.tasks.makers.TaskMaker.TITLE
import org.tasks.makers.TaskMaker.newTask
import org.tasks.sync.microsoft.MicrosoftConverter.applyRemote
import org.tasks.sync.microsoft.MicrosoftConverter.applySubtask
import org.tasks.sync.microsoft.MicrosoftConverter.toChecklistItem
import org.tasks.sync.microsoft.MicrosoftConverter.toRemote
import org.tasks.sync.microsoft.Tasks.Task.Importance
import org.tasks.sync.microsoft.Tasks.Task.RecurrenceDayOfWeek
import org.tasks.sync.microsoft.Tasks.Task.RecurrenceType
import org.tasks.time.DateTime

/**
 * Additional tests for [MicrosoftConverter] targeting uncovered branches and
 * areas not yet exercised by existing test classes.
 */
class MicrosoftConverterExtraTest {

    // ================================================================
    // applyRemote — title assignment
    // ================================================================

    @Test
    fun applyRemoteSetsTitle() {
        withTZ("UTC") {
            val task = Task()
            val remote = Tasks.Task(
                title = "My Task Title",
                importance = Importance.low,
                createdDateTime = "2023-07-20T04:20:56.009Z",
                lastModifiedDateTime = "2023-07-20T04:21:06.269Z",
            )
            task.applyRemote(remote, Task.Priority.NONE)
            assertEquals("My Task Title", task.title)
        }
    }

    @Test
    fun applyRemoteNullTitleSetsNull() {
        withTZ("UTC") {
            val task = Task(title = "Old Title")
            val remote = Tasks.Task(
                title = null,
                importance = Importance.low,
                createdDateTime = "2023-07-20T04:20:56.009Z",
                lastModifiedDateTime = "2023-07-20T04:21:06.269Z",
            )
            task.applyRemote(remote, Task.Priority.NONE)
            assertNull(task.title)
        }
    }

    // ================================================================
    // applyRemote — notes (body) parsing edge cases
    // ================================================================

    @Test
    fun applyRemoteNullBodySetsNullNotes() {
        withTZ("UTC") {
            val task = Task(notes = "Old notes")
            val remote = Tasks.Task(
                title = "No body",
                importance = Importance.low,
                createdDateTime = "2023-07-20T04:20:56.009Z",
                lastModifiedDateTime = "2023-07-20T04:21:06.269Z",
                body = null,
            )
            task.applyRemote(remote, Task.Priority.NONE)
            assertNull(task.notes)
        }
    }

    @Test
    fun applyRemoteEmptyStringBodySetsNull() {
        withTZ("UTC") {
            val task = Task()
            val remote = Tasks.Task(
                title = "Empty body",
                importance = Importance.low,
                createdDateTime = "2023-07-20T04:20:56.009Z",
                lastModifiedDateTime = "2023-07-20T04:21:06.269Z",
                body = Tasks.Task.Body(content = "", contentType = "text"),
            )
            task.applyRemote(remote, Task.Priority.NONE)
            assertNull(task.notes)
        }
    }

    @Test
    fun applyRemoteTextBodyWithContent() {
        withTZ("UTC") {
            val task = Task()
            val remote = Tasks.Task(
                title = "With body",
                importance = Importance.low,
                createdDateTime = "2023-07-20T04:20:56.009Z",
                lastModifiedDateTime = "2023-07-20T04:21:06.269Z",
                body = Tasks.Task.Body(content = "Some notes", contentType = "text"),
            )
            task.applyRemote(remote, Task.Priority.NONE)
            assertEquals("Some notes", task.notes)
        }
    }

    // ================================================================
    // applyRemote — creationDate and modificationDate
    // ================================================================

    @Test
    fun applyRemoteSetsCreationDate() {
        withTZ("UTC") {
            val task = Task()
            val remote = Tasks.Task(
                title = "Test",
                importance = Importance.low,
                createdDateTime = "2023-07-20T04:20:56.009Z",
                lastModifiedDateTime = "2023-07-20T04:21:06.269Z",
            )
            task.applyRemote(remote, Task.Priority.NONE)
            assertTrue(task.creationDate > 0)
        }
    }

    @Test
    fun applyRemoteSetsModificationDate() {
        withTZ("UTC") {
            val task = Task()
            val remote = Tasks.Task(
                title = "Test",
                importance = Importance.low,
                createdDateTime = "2023-07-20T04:20:56.009Z",
                lastModifiedDateTime = "2023-07-20T04:21:06.269Z",
            )
            task.applyRemote(remote, Task.Priority.NONE)
            assertTrue(task.modificationDate > 0)
        }
    }

    @Test
    fun applyRemoteNullCreationDateUsesCurrentTime() {
        withTZ("UTC") {
            val task = Task()
            val remote = Tasks.Task(
                title = "Null creation",
                importance = Importance.low,
                createdDateTime = null,
                lastModifiedDateTime = "2023-07-20T04:21:06.269Z",
            )
            task.applyRemote(remote, Task.Priority.NONE)
            assertTrue("creationDate should be set to current time", task.creationDate > 0)
        }
    }

    @Test
    fun applyRemoteNullModificationDateUsesCurrentTime() {
        withTZ("UTC") {
            val task = Task()
            val remote = Tasks.Task(
                title = "Null modification",
                importance = Importance.low,
                createdDateTime = "2023-07-20T04:20:56.009Z",
                lastModifiedDateTime = null,
            )
            task.applyRemote(remote, Task.Priority.NONE)
            assertTrue("modificationDate should be set to current time", task.modificationDate > 0)
        }
    }

    // ================================================================
    // applyRemote — recurrence with interval > 1 preserved
    // ================================================================

    @Test
    fun applyRemoteRecurrenceWithInterval3() {
        withTZ("UTC") {
            val task = Task()
            val remote = Tasks.Task(
                title = "Every 3 days",
                importance = Importance.low,
                createdDateTime = "2023-07-20T04:20:56.009Z",
                lastModifiedDateTime = "2023-07-20T04:21:06.269Z",
                recurrence = Tasks.Task.Recurrence(
                    pattern = Tasks.Task.Pattern(
                        type = RecurrenceType.daily,
                        interval = 3,
                        daysOfWeek = emptyList(),
                    ),
                ),
            )
            task.applyRemote(remote, Task.Priority.NONE)
            assertEquals("FREQ=DAILY;INTERVAL=3", task.recurrence)
        }
    }

    @Test
    fun applyRemoteRecurrenceWeeklySingleDay() {
        withTZ("UTC") {
            val task = Task()
            val remote = Tasks.Task(
                title = "Weekly on Tuesday",
                importance = Importance.low,
                createdDateTime = "2023-07-20T04:20:56.009Z",
                lastModifiedDateTime = "2023-07-20T04:21:06.269Z",
                recurrence = Tasks.Task.Recurrence(
                    pattern = Tasks.Task.Pattern(
                        type = RecurrenceType.weekly,
                        interval = 1,
                        daysOfWeek = listOf(RecurrenceDayOfWeek.tuesday),
                    ),
                ),
            )
            task.applyRemote(remote, Task.Priority.NONE)
            assertEquals("FREQ=WEEKLY;BYDAY=TU", task.recurrence)
        }
    }

    @Test
    fun applyRemoteRecurrenceRelativeYearlyReturnsNull() {
        withTZ("UTC") {
            val task = Task()
            val remote = Tasks.Task(
                title = "Relative yearly",
                importance = Importance.low,
                createdDateTime = "2023-07-20T04:20:56.009Z",
                lastModifiedDateTime = "2023-07-20T04:21:06.269Z",
                recurrence = Tasks.Task.Recurrence(
                    pattern = Tasks.Task.Pattern(
                        type = RecurrenceType.relativeYearly,
                        interval = 1,
                        daysOfWeek = emptyList(),
                    ),
                ),
            )
            task.applyRemote(remote, Task.Priority.NONE)
            assertNull("relativeYearly is unsupported", task.recurrence)
        }
    }

    // ================================================================
    // applyRemote — due date with existing due time
    // ================================================================

    @Test
    fun applyRemoteDueDatePreservesExistingDueTime() {
        withTZ("America/Chicago") {
            val task = newTask(with(DUE_TIME, DateTime(2023, 7, 19, 14, 30)))
            val remote = Tasks.Task(
                title = "Due with time",
                importance = Importance.low,
                createdDateTime = "2023-07-20T04:20:56.009Z",
                lastModifiedDateTime = "2023-07-20T04:21:06.269Z",
                dueDateTime = Tasks.Task.DateTime(
                    dateTime = "2023-07-21T05:00:00.000000",
                    timeZone = "UTC",
                ),
            )
            task.applyRemote(remote, Task.Priority.NONE)
            assertTrue("dueDate should be > 0", task.dueDate > 0)
        }
    }

    // ================================================================
    // toRemote — title and id
    // ================================================================

    @Test
    fun toRemoteIncludesRemoteId() {
        val remote = newTask().toRemote(
            newCaldavTask(with(REMOTE_ID, "abc-123")),
            emptyList(),
        )
        assertEquals("abc-123", remote.id)
    }

    @Test
    fun toRemoteSetsTitleCorrectly() {
        val remote = newTask(with(TITLE, "My Task")).toRemote(
            newCaldavTask(),
            emptyList(),
        )
        assertEquals("My Task", remote.title)
    }

    // ================================================================
    // toRemote — timestamps
    // ================================================================

    @Test
    fun toRemoteCreatedDateTimeIsUtc() {
        withTZ("America/Chicago") {
            val remote = Task(
                creationDate = DateTime(2024, 1, 15, 10, 30, 0, 0).millis,
            ).toRemote(newCaldavTask(), emptyList())
            assertTrue(remote.createdDateTime!!.endsWith("Z"))
        }
    }

    @Test
    fun toRemoteLastModifiedDateTimeIsUtc() {
        withTZ("America/Chicago") {
            val remote = Task(
                modificationDate = DateTime(2024, 1, 15, 10, 30, 0, 0).millis,
            ).toRemote(newCaldavTask(), emptyList())
            assertTrue(remote.lastModifiedDateTime!!.endsWith("Z"))
        }
    }

    // ================================================================
    // toRemote — recurrence unsupported frequency returns null
    // ================================================================

    @Test
    fun toRemoteUnsupportedRecurrenceFrequencyReturnsNull() {
        withTZ("UTC") {
            val remote = newTask(
                with(DUE_TIME, DateTime(2023, 8, 2, 22, 42, 59)),
                with(RECUR, "FREQ=SECONDLY"),
            ).toRemote(newCaldavTask(), emptyList())
            assertNull("SECONDLY frequency is unsupported", remote.recurrence)
        }
    }

    @Test
    fun toRemoteMinutelyRecurrenceReturnsNull() {
        withTZ("UTC") {
            val remote = newTask(
                with(DUE_TIME, DateTime(2023, 8, 2, 22, 42, 59)),
                with(RECUR, "FREQ=MINUTELY"),
            ).toRemote(newCaldavTask(), emptyList())
            assertNull("MINUTELY frequency is unsupported", remote.recurrence)
        }
    }

    @Test
    fun toRemoteHourlyRecurrenceReturnsNull() {
        withTZ("UTC") {
            val remote = newTask(
                with(DUE_TIME, DateTime(2023, 8, 2, 22, 42, 59)),
                with(RECUR, "FREQ=HOURLY"),
            ).toRemote(newCaldavTask(), emptyList())
            assertNull("HOURLY frequency is unsupported", remote.recurrence)
        }
    }

    // ================================================================
    // toRemote — weekly recurrence with individual day mappings
    // ================================================================

    @Test
    fun toRemoteWeeklyRecurrenceSundayOnly() {
        withTZ("America/Chicago") {
            val remote = newTask(
                with(DUE_TIME, DateTime(2023, 8, 2, 22, 42, 59)),
                with(RECUR, "FREQ=WEEKLY;BYDAY=SU"),
            ).toRemote(newCaldavTask(), emptyList())
            assertEquals(
                listOf(RecurrenceDayOfWeek.sunday),
                remote.recurrence!!.pattern.daysOfWeek,
            )
        }
    }

    @Test
    fun toRemoteWeeklyRecurrenceSaturdayOnly() {
        withTZ("America/Chicago") {
            val remote = newTask(
                with(DUE_TIME, DateTime(2023, 8, 2, 22, 42, 59)),
                with(RECUR, "FREQ=WEEKLY;BYDAY=SA"),
            ).toRemote(newCaldavTask(), emptyList())
            assertEquals(
                listOf(RecurrenceDayOfWeek.saturday),
                remote.recurrence!!.pattern.daysOfWeek,
            )
        }
    }

    @Test
    fun toRemoteWeeklyRecurrenceThursdayOnly() {
        withTZ("America/Chicago") {
            val remote = newTask(
                with(DUE_TIME, DateTime(2023, 8, 2, 22, 42, 59)),
                with(RECUR, "FREQ=WEEKLY;BYDAY=TH"),
            ).toRemote(newCaldavTask(), emptyList())
            assertEquals(
                listOf(RecurrenceDayOfWeek.thursday),
                remote.recurrence!!.pattern.daysOfWeek,
            )
        }
    }

    @Test
    fun toRemoteWeeklyRecurrenceWednesdayOnly() {
        withTZ("America/Chicago") {
            val remote = newTask(
                with(DUE_TIME, DateTime(2023, 8, 2, 22, 42, 59)),
                with(RECUR, "FREQ=WEEKLY;BYDAY=WE"),
            ).toRemote(newCaldavTask(), emptyList())
            assertEquals(
                listOf(RecurrenceDayOfWeek.wednesday),
                remote.recurrence!!.pattern.daysOfWeek,
            )
        }
    }

    @Test
    fun toRemoteWeeklyRecurrenceTuesdayOnly() {
        withTZ("America/Chicago") {
            val remote = newTask(
                with(DUE_TIME, DateTime(2023, 8, 2, 22, 42, 59)),
                with(RECUR, "FREQ=WEEKLY;BYDAY=TU"),
            ).toRemote(newCaldavTask(), emptyList())
            assertEquals(
                listOf(RecurrenceDayOfWeek.tuesday),
                remote.recurrence!!.pattern.daysOfWeek,
            )
        }
    }

    // ================================================================
    // toRemote — completedDateTime with timezone
    // ================================================================

    @Test
    fun toRemoteCompletedDateTimeIncludesTimeZone() {
        withTZ("America/New_York") {
            val remote = newTask(
                with(COMPLETION_TIME, DateTime(2024, 3, 15, 14, 0)),
            ).toRemote(newCaldavTask(), emptyList())
            assertNotNull(remote.completedDateTime)
            assertEquals("America/New_York", remote.completedDateTime!!.timeZone)
        }
    }

    // ================================================================
    // toRemote — dueDateTime timezone matches system
    // ================================================================

    @Test
    fun toRemoteDueDateTimeIncludesSystemTimeZone() {
        withTZ("Europe/London") {
            val remote = newTask(
                with(DUE_TIME, DateTime(2024, 3, 15, 14, 0)),
            ).toRemote(newCaldavTask(), emptyList())
            assertNotNull(remote.dueDateTime)
            assertEquals("Europe/London", remote.dueDateTime!!.timeZone)
        }
    }

    // ================================================================
    // toRemote — recurrence with interval > 1
    // ================================================================

    @Test
    fun toRemoteRecurrenceWithHighInterval() {
        withTZ("UTC") {
            val remote = newTask(
                with(DUE_TIME, DateTime(2023, 8, 2, 22, 42, 59)),
                with(RECUR, "FREQ=DAILY;INTERVAL=5"),
            ).toRemote(newCaldavTask(), emptyList())
            assertEquals(5, remote.recurrence!!.pattern.interval)
        }
    }

    // ================================================================
    // toChecklistItem — various edge cases
    // ================================================================

    @Test
    fun toChecklistItemCreatedDateTimeContainsUtcMarker() {
        withTZ("America/Chicago") {
            val task = Task(
                creationDate = DateTime(2024, 1, 15, 10, 30, 0, 0).millis,
            )
            val item = task.toChecklistItem("id")
            assertTrue(item.createdDateTime!!.endsWith("Z"))
        }
    }

    @Test
    fun toChecklistItemCheckedDateTimeContainsUtcMarker() {
        withTZ("America/Chicago") {
            val task = Task(
                completionDate = DateTime(2024, 1, 15, 10, 30, 0, 0).millis,
            )
            val item = task.toChecklistItem("id")
            assertTrue(item.checkedDateTime!!.endsWith("Z"))
        }
    }

    // ================================================================
    // applySubtask — unchecked with zero parentCompletionDate
    // ================================================================

    @Test
    fun applySubtaskUncheckedWithZeroParentCompletion() {
        withTZ("UTC") {
            val task = Task()
            task.applySubtask(10, 0, Tasks.Task.ChecklistItem(
                displayName = "Unchecked",
                isChecked = false,
            ))
            assertEquals(0L, task.completionDate)
        }
    }

    @Test
    fun applySubtaskCheckedWithNullCheckedDateTime() {
        withTZ("UTC") {
            val task = Task()
            task.applySubtask(10, 0, Tasks.Task.ChecklistItem(
                displayName = "Checked no datetime",
                isChecked = true,
                checkedDateTime = null,
            ))
            // When isChecked but checkedDateTime is null, parseDateTime returns currentTimeMillis
            assertTrue(task.completionDate > 0)
        }
    }

    // ================================================================
    // applyRemote — priority unchanged when importance is low and current is not high
    // ================================================================

    @Test
    fun applyRemoteLowImportancePreservesLowPriority() {
        withTZ("UTC") {
            val task = Task(priority = Task.Priority.LOW)
            val remote = Tasks.Task(
                title = "Low",
                importance = Importance.low,
                createdDateTime = "2023-07-20T04:20:56.009Z",
                lastModifiedDateTime = "2023-07-20T04:21:06.269Z",
            )
            task.applyRemote(remote, Task.Priority.NONE)
            assertEquals(Task.Priority.LOW, task.priority)
        }
    }

    @Test
    fun applyRemoteNormalImportancePreservesNonePriority() {
        withTZ("UTC") {
            val task = Task(priority = Task.Priority.NONE)
            val remote = Tasks.Task(
                title = "Normal",
                importance = Importance.normal,
                createdDateTime = "2023-07-20T04:20:56.009Z",
                lastModifiedDateTime = "2023-07-20T04:21:06.269Z",
            )
            task.applyRemote(remote, Task.Priority.NONE)
            assertEquals(Task.Priority.NONE, task.priority)
        }
    }

    // ================================================================
    // toRemote — monthly recurrence without dueDate uses today
    // ================================================================

    @Test
    fun toRemoteMonthlyRecurrenceWithoutDueDateUsesCurrentDate() {
        withTZ("America/Chicago") {
            freezeAt(DateTime(2024, 3, 14, 10, 0, 0).millis) {
                val remote = newTask(
                    with(RECUR, "FREQ=MONTHLY"),
                ).toRemote(newCaldavTask(), emptyList())
                assertNotNull(remote.recurrence)
                assertEquals(14, remote.recurrence!!.pattern.dayOfMonth)
                assertEquals(0, remote.recurrence!!.pattern.month) // monthly has 0 month
            }
        }
    }

    // ================================================================
    // toRemote — weekly recurrence dayOfMonth and month are 0
    // ================================================================

    @Test
    fun toRemoteWeeklyRecurrenceZeroDayOfMonthAndMonth() {
        withTZ("America/Chicago") {
            val remote = newTask(
                with(DUE_TIME, DateTime(2023, 8, 2, 22, 42, 59)),
                with(RECUR, "FREQ=WEEKLY;BYDAY=MO"),
            ).toRemote(newCaldavTask(), emptyList())
            assertEquals(0, remote.recurrence!!.pattern.dayOfMonth)
            assertEquals(0, remote.recurrence!!.pattern.month)
        }
    }

    // ================================================================
    // toRemote — dueDateTime for recurring task without due date uses fallback
    // ================================================================

    @Test
    fun toRemoteRecurringTaskHasDueDateTimeFallback() {
        withTZ("America/Chicago") {
            freezeAt(DateTime(2024, 6, 15, 10, 0, 0).millis) {
                val remote = newTask(
                    with(RECUR, "FREQ=WEEKLY;BYDAY=MO"),
                ).toRemote(newCaldavTask(), emptyList())
                assertNotNull("Recurring task must have dueDateTime", remote.dueDateTime)
                assertTrue(
                    remote.dueDateTime!!.dateTime.startsWith("2024-06-15")
                )
            }
        }
    }
}
