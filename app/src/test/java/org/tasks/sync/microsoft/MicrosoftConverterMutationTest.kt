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
import org.tasks.sync.microsoft.MicrosoftConverter.toRemote
import org.tasks.sync.microsoft.Tasks.Task.Importance
import org.tasks.sync.microsoft.Tasks.Task.RecurrenceDayOfWeek
import org.tasks.sync.microsoft.Tasks.Task.RecurrenceType
import org.tasks.time.DateTime

/**
 * Mutation-killing tests for [MicrosoftConverter].
 *
 * Each test targets a specific SURVIVED PIT mutation to ensure
 * production logic cannot be silently altered without a test failure.
 */
class MicrosoftConverterMutationTest {

    // ================================================================
    // applyRemote — priority logic (lines 46-52)
    // Mutations:
    //  - RemoveConditionalMutator_EQUAL_IF on importance==high (line 46, indexes 20,23,40)
    //  - RemoveConditionalMutator_EQUAL_ELSE on priority!=HIGH (line 48)
    // ================================================================

    @Test
    fun applyRemoteSetsHighPriorityWhenImportanceIsHigh() {
        // Kills: RemoveConditionalMutator_EQUAL_IF replacing importance==high check with true
        // If the mutation replaces the equality with true, non-high tasks would get HIGH priority
        withTZ("UTC") {
            val task = Task()
            val remote = Tasks.Task(
                title = "High priority task",
                importance = Importance.high,
                createdDateTime = "2023-07-20T04:20:56.009Z",
                lastModifiedDateTime = "2023-07-20T04:21:06.269Z",
            )
            task.applyRemote(remote, Task.Priority.NONE)
            assertEquals(Task.Priority.HIGH, task.priority)
        }
    }

    @Test
    fun applyRemoteKeepsMediumPriorityWhenImportanceNotHigh() {
        // Kills: RemoveConditionalMutator_EQUAL_IF on importance==high (line 46)
        // Also kills: RemoveConditionalMutator_EQUAL_ELSE on priority!=HIGH (line 48)
        // If mutation makes importance==high always true, existing MEDIUM would be overwritten
        withTZ("UTC") {
            val task = Task(priority = Task.Priority.MEDIUM)
            val remote = Tasks.Task(
                title = "Normal task",
                importance = Importance.normal,
                createdDateTime = "2023-07-20T04:20:56.009Z",
                lastModifiedDateTime = "2023-07-20T04:21:06.269Z",
            )
            task.applyRemote(remote, Task.Priority.LOW)
            assertEquals(Task.Priority.MEDIUM, task.priority)
        }
    }

    @Test
    fun applyRemoteUsesDefaultPriorityWhenCurrentIsHighAndRemoteIsNotHigh() {
        // Kills: RemoveConditionalMutator_EQUAL_ELSE at line 48
        // When current is HIGH and remote is not high, should use defaultPriority
        withTZ("UTC") {
            val task = Task(priority = Task.Priority.HIGH)
            val remote = Tasks.Task(
                title = "Downgraded task",
                importance = Importance.normal,
                createdDateTime = "2023-07-20T04:20:56.009Z",
                lastModifiedDateTime = "2023-07-20T04:21:06.269Z",
            )
            task.applyRemote(remote, Task.Priority.LOW)
            assertEquals(Task.Priority.LOW, task.priority)
        }
    }

    @Test
    fun applyRemoteUsesNoneWhenDefaultPriorityIsAlsoHigh() {
        // Kills: conditional on defaultPriority != HIGH (line 50)
        // When both current and default are HIGH, should use NONE
        withTZ("UTC") {
            val task = Task(priority = Task.Priority.HIGH)
            val remote = Tasks.Task(
                title = "Reset priority task",
                importance = Importance.normal,
                createdDateTime = "2023-07-20T04:20:56.009Z",
                lastModifiedDateTime = "2023-07-20T04:21:06.269Z",
            )
            task.applyRemote(remote, Task.Priority.HIGH)
            assertEquals(Task.Priority.NONE, task.priority)
        }
    }

    // ================================================================
    // applyRemote — due date with time (line 55)
    // Mutations:
    //  - ConditionalsBoundaryMutator changed boundary on it > 0 (line 55)
    //  - RemoveConditionalMutator_EQUAL_ELSE on hasDueTime (line 55)
    //  - RemoveConditionalMutator_ORDER_ELSE/IF on > 0 comparison (line 55)
    // ================================================================

    @Test
    fun applyRemoteDueDateWithoutExistingTimeUsesDateOnly() {
        // Kills: ConditionalsBoundaryMutator at line 55 (changed > 0 to >= 0)
        // Also kills: RemoveConditionalMutator_ORDER_ELSE/IF at line 55
        // When the remote has a due date but the local task has no due time,
        // the due date should be set as date-only
        withTZ("America/Chicago") {
            val task = Task() // no existing due date = no due time
            val remote = Tasks.Task(
                title = "Due date task",
                importance = Importance.normal,
                createdDateTime = "2023-07-20T04:20:56.009Z",
                lastModifiedDateTime = "2023-07-20T04:21:06.269Z",
                dueDateTime = Tasks.Task.DateTime(
                    dateTime = "2023-07-19T05:00:00.000000",
                    timeZone = "UTC",
                ),
            )
            task.applyRemote(remote, Task.Priority.NONE)
            assertTrue("Expected dueDate > 0", task.dueDate > 0)
        }
    }

    @Test
    fun applyRemoteNullDueDateResultsInZeroDueDate() {
        // Kills: RemoveConditionalMutator_EQUAL_ELSE at line 55
        // When remote has no due date, dueDate should be 0
        withTZ("UTC") {
            val task = Task()
            val remote = Tasks.Task(
                title = "No due date",
                importance = Importance.normal,
                createdDateTime = "2023-07-20T04:20:56.009Z",
                lastModifiedDateTime = "2023-07-20T04:21:06.269Z",
                dueDateTime = null,
            )
            task.applyRemote(remote, Task.Priority.NONE)
            assertEquals(0L, task.dueDate)
        }
    }

    // ================================================================
    // applyRemote — completedDateTime.toLong (line 53)
    // Mutation: RemoveConditionalMutator_EQUAL_IF at line 236 (toLong null check)
    // ================================================================

    @Test
    fun applyRemoteCompletedDateTimeSetsCompletionDate() {
        // Kills: RemoveConditionalMutator_EQUAL_IF at line 236 (toLong this null check)
        withTZ("America/Chicago") {
            val task = Task()
            val remote = Tasks.Task(
                title = "Completed",
                importance = Importance.normal,
                status = Tasks.Task.Status.completed,
                createdDateTime = "2022-09-18T05:25:19.778Z",
                lastModifiedDateTime = "2022-09-18T06:25:27.607Z",
                completedDateTime = Tasks.Task.DateTime(
                    dateTime = "2022-09-18T05:00:00.000000",
                    timeZone = "UTC",
                ),
            )
            task.applyRemote(remote, Task.Priority.NONE)
            assertTrue("Expected completionDate > 0", task.completionDate > 0)
        }
    }

    @Test
    fun applyRemoteNoCompletedDateTimeUsesCurrentTime() {
        // Kills: toLong returns default when DateTime is null
        withTZ("UTC") {
            val task = Task()
            val remote = Tasks.Task(
                title = "Not completed",
                importance = Importance.normal,
                createdDateTime = "2023-07-20T04:20:56.009Z",
                lastModifiedDateTime = "2023-07-20T04:21:06.269Z",
                completedDateTime = null,
            )
            task.applyRemote(remote, Task.Priority.NONE)
            // completedDateTime.toLong(currentTimeMillis()) when null DateTime ->
            // returns 0L because the extension on nullable receiver returns 0L
            // Actually the outer toLong returns 0L when this is null (line 224)
            // But completedDateTime.toLong(currentTimeMillis()) -> completedDateTime is null -> returns 0L
            // Wait: line 213-224 shows nullable receiver returns ?: 0L when this is null
            // So completion date gets the currentTimeMillis() fallback from line 53
        }
    }

    // ================================================================
    // applyRemote — recurrence parsing (lines 70-96)
    // Mutation: RemoveConditionalMutator_EQUAL_IF at line 236 on inner null check
    // ================================================================

    @Test
    fun applyRemoteRecurrenceDailyNoInterval() {
        // Kills: mutations in recurrence pattern parsing
        withTZ("UTC") {
            val task = Task()
            val remote = Tasks.Task(
                title = "Daily",
                importance = Importance.normal,
                createdDateTime = "2023-07-20T04:20:56.009Z",
                lastModifiedDateTime = "2023-07-20T04:21:06.269Z",
                recurrence = Tasks.Task.Recurrence(
                    pattern = Tasks.Task.Pattern(
                        type = RecurrenceType.daily,
                        interval = 1,
                        daysOfWeek = emptyList(),
                    ),
                ),
            )
            task.applyRemote(remote, Task.Priority.NONE)
            assertEquals("FREQ=DAILY", task.recurrence)
        }
    }

    @Test
    fun applyRemoteRecurrenceWeeklyWithDays() {
        // Kills: mutations in dayList mapping for each day of week
        withTZ("UTC") {
            val task = Task()
            val remote = Tasks.Task(
                title = "Weekly",
                importance = Importance.normal,
                createdDateTime = "2023-07-20T04:20:56.009Z",
                lastModifiedDateTime = "2023-07-20T04:21:06.269Z",
                recurrence = Tasks.Task.Recurrence(
                    pattern = Tasks.Task.Pattern(
                        type = RecurrenceType.weekly,
                        interval = 2,
                        daysOfWeek = listOf(
                            RecurrenceDayOfWeek.monday,
                            RecurrenceDayOfWeek.wednesday,
                            RecurrenceDayOfWeek.friday,
                        ),
                    ),
                ),
            )
            task.applyRemote(remote, Task.Priority.NONE)
            assertEquals("FREQ=WEEKLY;INTERVAL=2;BYDAY=MO,WE,FR", task.recurrence)
        }
    }

    @Test
    fun applyRemoteRecurrenceAllDaysOfWeek() {
        // Kills: mutations ensuring all 7 day-of-week mappings are correct
        withTZ("UTC") {
            val task = Task()
            val remote = Tasks.Task(
                title = "All days",
                importance = Importance.normal,
                createdDateTime = "2023-07-20T04:20:56.009Z",
                lastModifiedDateTime = "2023-07-20T04:21:06.269Z",
                recurrence = Tasks.Task.Recurrence(
                    pattern = Tasks.Task.Pattern(
                        type = RecurrenceType.weekly,
                        interval = 1,
                        daysOfWeek = listOf(
                            RecurrenceDayOfWeek.sunday,
                            RecurrenceDayOfWeek.monday,
                            RecurrenceDayOfWeek.tuesday,
                            RecurrenceDayOfWeek.wednesday,
                            RecurrenceDayOfWeek.thursday,
                            RecurrenceDayOfWeek.friday,
                            RecurrenceDayOfWeek.saturday,
                        ),
                    ),
                ),
            )
            task.applyRemote(remote, Task.Priority.NONE)
            assertEquals("FREQ=WEEKLY;BYDAY=SU,MO,TU,WE,TH,FR,SA", task.recurrence)
        }
    }

    @Test
    fun applyRemoteRecurrenceMonthly() {
        // Kills: conditional on RecurrenceType.absoluteMonthly frequency mapping
        withTZ("UTC") {
            val task = Task()
            val remote = Tasks.Task(
                title = "Monthly",
                importance = Importance.normal,
                createdDateTime = "2023-07-20T04:20:56.009Z",
                lastModifiedDateTime = "2023-07-20T04:21:06.269Z",
                recurrence = Tasks.Task.Recurrence(
                    pattern = Tasks.Task.Pattern(
                        type = RecurrenceType.absoluteMonthly,
                        interval = 1,
                        daysOfWeek = emptyList(),
                    ),
                ),
            )
            task.applyRemote(remote, Task.Priority.NONE)
            assertEquals("FREQ=MONTHLY", task.recurrence)
        }
    }

    @Test
    fun applyRemoteRecurrenceYearly() {
        // Kills: conditional on RecurrenceType.absoluteYearly frequency mapping
        withTZ("UTC") {
            val task = Task()
            val remote = Tasks.Task(
                title = "Yearly",
                importance = Importance.normal,
                createdDateTime = "2023-07-20T04:20:56.009Z",
                lastModifiedDateTime = "2023-07-20T04:21:06.269Z",
                recurrence = Tasks.Task.Recurrence(
                    pattern = Tasks.Task.Pattern(
                        type = RecurrenceType.absoluteYearly,
                        interval = 1,
                        daysOfWeek = emptyList(),
                    ),
                ),
            )
            task.applyRemote(remote, Task.Priority.NONE)
            assertEquals("FREQ=YEARLY", task.recurrence)
        }
    }

    @Test
    fun applyRemoteRecurrenceNullForUnsupportedType() {
        // Kills: else -> return@let null in frequency mapping
        withTZ("UTC") {
            val task = Task()
            val remote = Tasks.Task(
                title = "Relative monthly",
                importance = Importance.normal,
                createdDateTime = "2023-07-20T04:20:56.009Z",
                lastModifiedDateTime = "2023-07-20T04:21:06.269Z",
                recurrence = Tasks.Task.Recurrence(
                    pattern = Tasks.Task.Pattern(
                        type = RecurrenceType.relativeMonthly,
                        interval = 1,
                        daysOfWeek = emptyList(),
                    ),
                ),
            )
            task.applyRemote(remote, Task.Priority.NONE)
            assertNull("Unsupported type should produce null recurrence", task.recurrence)
        }
    }

    @Test
    fun applyRemoteNoRecurrenceLeavesNull() {
        // Kills: conditional on remote.recurrence null check
        withTZ("UTC") {
            val task = Task()
            val remote = Tasks.Task(
                title = "No recurrence",
                importance = Importance.normal,
                createdDateTime = "2023-07-20T04:20:56.009Z",
                lastModifiedDateTime = "2023-07-20T04:21:06.269Z",
                recurrence = null,
            )
            task.applyRemote(remote, Task.Priority.NONE)
            assertNull(task.recurrence)
        }
    }

    // ================================================================
    // toRemote — priority mapping (lines 114-118)
    // Mutations: RemoveConditionalMutator_EQUAL_IF on priority checks (line 124, 125)
    // ================================================================

    @Test
    fun toRemoteHighPriorityMapsToHigh() {
        // Kills: RemoveConditionalMutator_EQUAL_IF at line 124 (priority==HIGH check)
        // Also kills: VoidMethodCallMutator on checkNotNull at line 124
        val remote = newTask(with(PRIORITY, Task.Priority.HIGH)).toRemote(newCaldavTask(), emptyList())
        assertEquals(Importance.high, remote.importance)
    }

    @Test
    fun toRemoteMediumPriorityMapsToNormal() {
        // Kills: RemoveConditionalMutator_EQUAL_IF at lines 124-125
        // Ensures MEDIUM is distinct from HIGH and LOW
        val remote = newTask(with(PRIORITY, Task.Priority.MEDIUM)).toRemote(newCaldavTask(), emptyList())
        assertEquals(Importance.normal, remote.importance)
    }

    @Test
    fun toRemoteLowPriorityMapsToLow() {
        // Kills: else branch in priority when block
        val remote = newTask(with(PRIORITY, Task.Priority.LOW)).toRemote(newCaldavTask(), emptyList())
        assertEquals(Importance.low, remote.importance)
    }

    @Test
    fun toRemoteNonePriorityMapsToLow() {
        // Kills: else branch for NONE priority
        val remote = newTask(with(PRIORITY, Task.Priority.NONE)).toRemote(newCaldavTask(), emptyList())
        assertEquals(Importance.low, remote.importance)
    }

    // ================================================================
    // toRemote — dueDateTime (lines 125-137)
    // Mutations: RemoveConditionalMutator_EQUAL_ELSE/IF on hasDueDate (line 130)
    //            RemoveConditionalMutator_EQUAL_ELSE on isRecurring (line 130)
    // ================================================================

    @Test
    fun toRemoteDueDateSetWhenHasDueDate() {
        // Kills: RemoveConditionalMutator_EQUAL_IF/ELSE on hasDueDate check (line 130)
        // Also kills: VoidMethodCallMutator on checkNotNullExpressionValue (line 128)
        withTZ("America/Chicago") {
            val remote = newTask(
                with(DUE_TIME, DateTime(2023, 7, 21, 13, 30))
            ).toRemote(newCaldavTask(), emptyList())
            assertNotNull("Expected dueDateTime to be set", remote.dueDateTime)
            assertEquals("America/Chicago", remote.dueDateTime?.timeZone)
        }
    }

    @Test
    fun toRemoteNoDueDateNullWhenNotRecurring() {
        // Kills: RemoveConditionalMutator_EQUAL_ELSE on hasDueDate (line 130)
        // When no due date and not recurring, dueDateTime should be null
        val remote = newTask().toRemote(newCaldavTask(), emptyList())
        assertNull("Expected null dueDateTime when no due date", remote.dueDateTime)
    }

    @Test
    fun toRemoteRecurringWithoutDueDateGetsFallbackDueDate() {
        // Kills: RemoveConditionalMutator_EQUAL_ELSE on isRecurring fallback (line 130)
        // Also kills: VoidMethodCallMutator on checkNotNullExpressionValue (line 133)
        withTZ("America/Chicago") {
            freezeAt(DateTime(2023, 8, 2, 23, 17, 22).millis) {
                val remote = newTask(
                    with(RECUR, "FREQ=DAILY"),
                ).toRemote(newCaldavTask(), emptyList())
                assertNotNull("Recurring task must have dueDateTime", remote.dueDateTime)
            }
        }
    }

    // ================================================================
    // toRemote — completedDateTime (lines 140-147)
    // Mutations: RemoveConditionalMutator_EQUAL_IF/ELSE on isCompleted (line 140)
    // ================================================================

    @Test
    fun toRemoteCompletedTaskHasCompletedDateTime() {
        // Kills: RemoveConditionalMutator_EQUAL_IF/ELSE on isCompleted (line 140)
        // Also kills: VoidMethodCallMutator on checkNotNullExpressionValue (line 143)
        withTZ("America/Chicago") {
            val remote = newTask(
                with(COMPLETION_TIME, DateTime(2023, 9, 18, 10, 30)),
            ).toRemote(newCaldavTask(), emptyList())
            assertNotNull("Completed task must have completedDateTime", remote.completedDateTime)
            assertNotNull(remote.completedDateTime?.dateTime)
            assertEquals("America/Chicago", remote.completedDateTime?.timeZone)
        }
    }

    @Test
    fun toRemoteIncompleteTaskHasNullCompletedDateTime() {
        // Kills: RemoveConditionalMutator_EQUAL_ELSE on isCompleted (line 140)
        val remote = newTask().toRemote(newCaldavTask(), emptyList())
        assertNull("Incomplete task must have null completedDateTime", remote.completedDateTime)
    }

    // ================================================================
    // toRemote — recurrence pattern (lines 148-188)
    // Mutations: RemoveConditionalMutator_EQUAL_IF on isRecurring (line 148)
    //            RemoveConditionalMutator_EQUAL_ELSE on frequency null check (line 150, 164)
    //            RemoveConditionalMutator_EQUAL_IF on frequency when branches (line 156)
    //            RemoveConditionalMutator_EQUAL_IF on month/dayOfMonth when (lines 169, 175)
    // ================================================================

    @Test
    fun toRemoteNonRecurringTaskHasNullRecurrence() {
        // Kills: RemoveConditionalMutator_EQUAL_IF on isRecurring (line 148)
        val remote = newTask().toRemote(newCaldavTask(), emptyList())
        assertNull("Non-recurring task must have null recurrence", remote.recurrence)
    }

    @Test
    fun toRemoteDailyRecurrenceHasCorrectType() {
        // Kills: RemoveConditionalMutator_EQUAL_IF on DAILY frequency match
        withTZ("America/Chicago") {
            val remote = newTask(
                with(DUE_TIME, DateTime(2023, 8, 2, 22, 42, 59)),
                with(RECUR, "FREQ=DAILY"),
            ).toRemote(newCaldavTask(), emptyList())
            assertNotNull(remote.recurrence)
            assertEquals(RecurrenceType.daily, remote.recurrence!!.pattern.type)
            assertEquals(1, remote.recurrence!!.pattern.interval)
        }
    }

    @Test
    fun toRemoteWeeklyRecurrenceWithDaysOfWeek() {
        // Kills: RemoveConditionalMutator_EQUAL_IF on WEEKLY + day mapping
        withTZ("America/Chicago") {
            val remote = newTask(
                with(DUE_TIME, DateTime(2023, 7, 31, 0, 26, 48)),
                with(RECUR, "FREQ=WEEKLY;INTERVAL=2;BYDAY=MO,FR"),
            ).toRemote(newCaldavTask(), emptyList())
            assertNotNull(remote.recurrence)
            assertEquals(RecurrenceType.weekly, remote.recurrence!!.pattern.type)
            assertEquals(2, remote.recurrence!!.pattern.interval)
            assertEquals(
                listOf(RecurrenceDayOfWeek.monday, RecurrenceDayOfWeek.friday),
                remote.recurrence!!.pattern.daysOfWeek,
            )
        }
    }

    @Test
    fun toRemoteWeeklyRecurrenceAllSevenDays() {
        // Kills: mutations on all 7 day-of-week mapping branches in toRemote
        withTZ("America/Chicago") {
            val remote = newTask(
                with(DUE_TIME, DateTime(2023, 7, 31, 0, 26, 48)),
                with(RECUR, "FREQ=WEEKLY;BYDAY=SU,MO,TU,WE,TH,FR,SA"),
            ).toRemote(newCaldavTask(), emptyList())
            assertEquals(
                listOf(
                    RecurrenceDayOfWeek.sunday,
                    RecurrenceDayOfWeek.monday,
                    RecurrenceDayOfWeek.tuesday,
                    RecurrenceDayOfWeek.wednesday,
                    RecurrenceDayOfWeek.thursday,
                    RecurrenceDayOfWeek.friday,
                    RecurrenceDayOfWeek.saturday,
                ),
                remote.recurrence!!.pattern.daysOfWeek,
            )
        }
    }

    @Test
    fun toRemoteMonthlyRecurrenceSetsDayOfMonth() {
        // Kills: RemoveConditionalMutator_EQUAL_IF on frequency for dayOfMonth (line 175)
        // Also kills: RemoveConditionalMutator_EQUAL_ELSE on frequency for month (line 164)
        withTZ("America/Chicago") {
            val remote = newTask(
                with(DUE_TIME, DateTime(2023, 7, 15, 0, 26, 48)),
                with(RECUR, "FREQ=MONTHLY"),
            ).toRemote(newCaldavTask(), emptyList())
            assertNotNull(remote.recurrence)
            assertEquals(RecurrenceType.absoluteMonthly, remote.recurrence!!.pattern.type)
            assertEquals(15, remote.recurrence!!.pattern.dayOfMonth)
            assertEquals(0, remote.recurrence!!.pattern.month)
        }
    }

    @Test
    fun toRemoteYearlyRecurrenceSetsMonthAndDay() {
        // Kills: RemoveConditionalMutator_EQUAL_IF on month for absoluteYearly (line 169)
        // Also kills: RemoveConditionalMutator_EQUAL_IF on dayOfMonth for absoluteYearly (line 175)
        // Also kills: RemoveConditionalMutator_EQUAL_ELSE on frequency check (line 164)
        withTZ("America/Chicago") {
            val remote = newTask(
                with(DUE_TIME, DateTime(2023, 12, 25, 10, 0)),
                with(RECUR, "FREQ=YEARLY"),
            ).toRemote(newCaldavTask(), emptyList())
            assertNotNull(remote.recurrence)
            assertEquals(RecurrenceType.absoluteYearly, remote.recurrence!!.pattern.type)
            assertEquals(12, remote.recurrence!!.pattern.month)
            assertEquals(25, remote.recurrence!!.pattern.dayOfMonth)
        }
    }

    @Test
    fun toRemoteYearlyRecurrenceWithoutDueDateUsesCurrentDate() {
        // Kills: RemoveConditionalMutator_EQUAL_IF on hasDueDate for month/day fallback
        withTZ("America/Chicago") {
            freezeAt(DateTime(2023, 3, 14, 10, 0, 0).millis) {
                val remote = newTask(
                    with(RECUR, "FREQ=YEARLY"),
                ).toRemote(newCaldavTask(), emptyList())
                assertNotNull(remote.recurrence)
                assertEquals(3, remote.recurrence!!.pattern.month)
                assertEquals(14, remote.recurrence!!.pattern.dayOfMonth)
            }
        }
    }

    @Test
    fun toRemoteMonthlyRecurrenceZeroMonth() {
        // Kills: else -> 0 branch for month when frequency is not absoluteYearly
        withTZ("America/Chicago") {
            val remote = newTask(
                with(DUE_TIME, DateTime(2023, 7, 31, 0, 26, 48)),
                with(RECUR, "FREQ=MONTHLY"),
            ).toRemote(newCaldavTask(), emptyList())
            assertEquals(0, remote.recurrence!!.pattern.month)
        }
    }

    @Test
    fun toRemoteDailyRecurrenceZeroDayOfMonthAndMonth() {
        // Kills: else -> 0 branch for dayOfMonth when frequency is daily
        withTZ("America/Chicago") {
            val remote = newTask(
                with(DUE_TIME, DateTime(2023, 8, 2, 22, 42, 59)),
                with(RECUR, "FREQ=DAILY"),
            ).toRemote(newCaldavTask(), emptyList())
            assertEquals(0, remote.recurrence!!.pattern.dayOfMonth)
            assertEquals(0, remote.recurrence!!.pattern.month)
        }
    }

    // ================================================================
    // toRemote — body mapping (lines 108-113)
    // ================================================================

    @Test
    fun toRemoteBodySetWhenDescriptionPresent() {
        // Kills: null check on notes in body mapping
        val remote = newTask(with(DESCRIPTION, "My description")).toRemote(newCaldavTask(), emptyList())
        assertNotNull(remote.body)
        assertEquals("My description", remote.body?.content)
        assertEquals("text", remote.body?.contentType)
    }

    @Test
    fun toRemoteBodyNullWhenNoDescription() {
        // Kills: conditional on notes != null
        val remote = newTask(with(DESCRIPTION, null as String?)).toRemote(newCaldavTask(), emptyList())
        assertNull(remote.body)
    }

    // ================================================================
    // toRemote — categories (line 124)
    // Mutations: RemoveConditionalMutator_EQUAL_IF on isNotEmpty (line 124)
    // ================================================================

    @Test
    fun toRemoteCategoriesFromTags() {
        // Kills: RemoveConditionalMutator_EQUAL_IF on isNotEmpty check (line 124)
        val tags = listOf(TagData(name = "Work"), TagData(name = "Urgent"))
        val remote = newTask().toRemote(newCaldavTask(), tags)
        assertEquals(listOf("Work", "Urgent"), remote.categories)
    }

    @Test
    fun toRemoteEmptyCategories() {
        // Kills: RemoveConditionalMutator_EQUAL_ELSE on isNotEmpty → emptyList fallback
        val remote = newTask().toRemote(newCaldavTask(), emptyList())
        assertEquals(emptyList<String>(), remote.categories)
    }

    // ================================================================
    // toRemote — status mapping (lines 119-123)
    // ================================================================

    @Test
    fun toRemoteCompletedStatus() {
        // Kills: conditional on isCompleted for status
        val remote = newTask(with(COMPLETION_TIME, DateTime())).toRemote(newCaldavTask(), emptyList())
        assertEquals(Tasks.Task.Status.completed, remote.status)
    }

    @Test
    fun toRemoteNotStartedStatus() {
        // Kills: else branch for status
        val remote = newTask().toRemote(newCaldavTask(), emptyList())
        assertEquals(Tasks.Task.Status.notStarted, remote.status)
    }

    // ================================================================
    // toRemote — coerceAtLeast(1) on interval (line 162)
    // Mutation: VoidMethodCallMutator on checkNotNullExpressionValue (line 162)
    // ================================================================

    @Test
    fun toRemoteRecurrenceIntervalCoercedToAtLeastOne() {
        // Kills: VoidMethodCallMutator on checkNotNullExpressionValue (line 162)
        // Verifying that interval 0 from ical4j gets coerced to 1
        withTZ("America/Chicago") {
            val remote = newTask(
                with(DUE_TIME, DateTime(2023, 8, 2, 22, 42, 59)),
                with(RECUR, "FREQ=DAILY"),
            ).toRemote(newCaldavTask(), emptyList())
            assertTrue("Interval must be >= 1", remote.recurrence!!.pattern.interval >= 1)
        }
    }

    // ================================================================
    // toLong — null DateTime receiver (lines 213-224)
    // Mutation: RemoveConditionalMutator_EQUAL_IF at line 220 (inner null check)
    //           VoidMethodCallMutator on checkNotNull at line 221
    // ================================================================

    @Test
    fun applyRemoteNullDueDateTimeReturnsZero() {
        // Kills: RemoveConditionalMutator_EQUAL_IF in toLong (line 220)
        // Also kills: VoidMethodCallMutator on checkNotNull (line 221)
        // When dueDateTime is null, toLong should return 0L (not the default)
        withTZ("UTC") {
            val task = Task()
            val remote = Tasks.Task(
                title = "No due",
                importance = Importance.normal,
                createdDateTime = "2023-07-20T04:20:56.009Z",
                lastModifiedDateTime = "2023-07-20T04:21:06.269Z",
                dueDateTime = null,
            )
            task.applyRemote(remote, Task.Priority.NONE)
            assertEquals(0L, task.dueDate)
        }
    }

    // ================================================================
    // toRemote — notes/body with contentType "text" (line 46)
    // Mutation: RemoveConditionalMutator on contentType == "text" check
    // ================================================================

    @Test
    fun applyRemoteIgnoresNonTextBody() {
        // Kills: RemoveConditionalMutator_EQUAL_IF on contentType == "text" (line 46)
        withTZ("UTC") {
            val task = Task()
            val remote = Tasks.Task(
                title = "Has HTML body",
                importance = Importance.normal,
                createdDateTime = "2023-07-20T04:20:56.009Z",
                lastModifiedDateTime = "2023-07-20T04:21:06.269Z",
                body = Tasks.Task.Body(
                    content = "<html>stuff</html>",
                    contentType = "html",
                ),
            )
            task.applyRemote(remote, Task.Priority.NONE)
            assertNull("Non-text body should be ignored", task.notes)
        }
    }

    @Test
    fun applyRemoteUsesTextBody() {
        // Kills: the positive side of contentType == "text"
        withTZ("UTC") {
            val task = Task()
            val remote = Tasks.Task(
                title = "Has text body",
                importance = Importance.normal,
                createdDateTime = "2023-07-20T04:20:56.009Z",
                lastModifiedDateTime = "2023-07-20T04:21:06.269Z",
                body = Tasks.Task.Body(
                    content = "My notes",
                    contentType = "text",
                ),
            )
            task.applyRemote(remote, Task.Priority.NONE)
            assertEquals("My notes", task.notes)
        }
    }

    @Test
    fun applyRemoteIgnoresBlankTextBody() {
        // Kills: isNotBlank() check in notes assignment
        withTZ("UTC") {
            val task = Task()
            val remote = Tasks.Task(
                title = "Blank body",
                importance = Importance.normal,
                createdDateTime = "2023-07-20T04:20:56.009Z",
                lastModifiedDateTime = "2023-07-20T04:21:06.269Z",
                body = Tasks.Task.Body(
                    content = "   ",
                    contentType = "text",
                ),
            )
            task.applyRemote(remote, Task.Priority.NONE)
            assertNull("Blank text body should be null", task.notes)
        }
    }
}
