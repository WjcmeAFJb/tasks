package org.tasks.data.entity

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.tasks.data.entity.Task.Companion.NO_ID
import org.tasks.data.entity.Task.Companion.NO_UUID
import org.tasks.data.entity.Task.Companion.sanitizeRecur

class TaskEntityTest {

    // --- Default values ---

    @Test
    fun defaultIdIsNoId() {
        val task = Task()
        assertEquals(NO_ID, task.id)
    }

    @Test
    fun defaultPriorityIsNone() {
        val task = Task()
        assertEquals(Task.Priority.NONE, task.priority)
    }

    @Test
    fun defaultDueDateIsZero() {
        val task = Task()
        assertEquals(0L, task.dueDate)
    }

    @Test
    fun defaultCompletionDateIsZero() {
        val task = Task()
        assertEquals(0L, task.completionDate)
    }

    @Test
    fun defaultDeletionDateIsZero() {
        val task = Task()
        assertEquals(0L, task.deletionDate)
    }

    @Test
    fun defaultRepeatFromIsDueDate() {
        val task = Task()
        assertEquals(Task.RepeatFrom.DUE_DATE, task.repeatFrom)
    }

    // --- Priority constants ---

    @Test
    fun priorityHighIsZero() {
        assertEquals(0, Task.Priority.HIGH)
    }

    @Test
    fun priorityMediumIsOne() {
        assertEquals(1, Task.Priority.MEDIUM)
    }

    @Test
    fun priorityLowIsTwo() {
        assertEquals(2, Task.Priority.LOW)
    }

    @Test
    fun priorityNoneIsThree() {
        assertEquals(3, Task.Priority.NONE)
    }

    // --- isCompleted ---

    @Test
    fun isCompletedWhenCompletionDatePositive() {
        val task = Task(completionDate = 1000L)
        assertTrue(task.isCompleted)
    }

    @Test
    fun isNotCompletedWhenCompletionDateZero() {
        val task = Task(completionDate = 0L)
        assertFalse(task.isCompleted)
    }

    // --- isDeleted ---

    @Test
    fun isDeletedWhenDeletionDatePositive() {
        val task = Task(deletionDate = 1000L)
        assertTrue(task.isDeleted)
    }

    @Test
    fun isNotDeletedWhenDeletionDateZero() {
        val task = Task(deletionDate = 0L)
        assertFalse(task.isDeleted)
    }

    // --- isRecurring ---

    @Test
    fun isRecurringWhenRecurrenceNotBlank() {
        val task = Task(recurrence = "FREQ=DAILY")
        assertTrue(task.isRecurring)
    }

    @Test
    fun isNotRecurringWhenRecurrenceNull() {
        val task = Task(recurrence = null)
        assertFalse(task.isRecurring)
    }

    @Test
    fun isNotRecurringWhenRecurrenceBlank() {
        val task = Task(recurrence = "   ")
        assertFalse(task.isRecurring)
    }

    @Test
    fun isNotRecurringWhenRecurrenceEmpty() {
        val task = Task(recurrence = "")
        assertFalse(task.isRecurring)
    }

    // --- hasDueDate ---

    @Test
    fun hasDueDateWhenPositive() {
        val task = Task(dueDate = 1000L)
        assertTrue(task.hasDueDate())
    }

    @Test
    fun hasNoDueDateWhenZero() {
        val task = Task(dueDate = 0L)
        assertFalse(task.hasDueDate())
    }

    // --- hasDueTime ---

    @Test
    fun hasDueTimeWhenNotDivisibleBy60000() {
        val task = Task(dueDate = 60001L)
        assertTrue(task.hasDueTime())
    }

    @Test
    fun hasNoDueTimeWhenDivisibleBy60000() {
        val task = Task(dueDate = 120000L)
        assertFalse(task.hasDueTime())
    }

    @Test
    fun hasNoDueTimeWhenZero() {
        val task = Task(dueDate = 0L)
        assertFalse(task.hasDueTime())
    }

    // --- static hasDueTime ---

    @Test
    fun staticHasDueTimeReturnsTrueForNonMultipleOf60000() {
        assertTrue(Task.hasDueTime(60001L))
    }

    @Test
    fun staticHasDueTimeReturnsFalseForMultipleOf60000() {
        assertFalse(Task.hasDueTime(120000L))
    }

    @Test
    fun staticHasDueTimeReturnsFalseForZero() {
        assertFalse(Task.hasDueTime(0L))
    }

    @Test
    fun staticHasDueTimeReturnsFalseForNegative() {
        assertFalse(Task.hasDueTime(-1L))
    }

    // --- hasStartDate / hasStartTime ---

    @Test
    fun hasStartDateWhenHideUntilPositive() {
        val task = Task(hideUntil = 1000L)
        assertTrue(task.hasStartDate())
    }

    @Test
    fun hasNoStartDateWhenHideUntilZero() {
        val task = Task(hideUntil = 0L)
        assertFalse(task.hasStartDate())
    }

    @Test
    fun hasStartTimeWhenHideUntilNotDivisibleBy60000() {
        val task = Task(hideUntil = 60001L)
        assertTrue(task.hasStartTime())
    }

    @Test
    fun hasNoStartTimeWhenHideUntilDivisibleBy60000() {
        val task = Task(hideUntil = 120000L)
        assertFalse(task.hasStartTime())
    }

    // --- isSaved ---

    @Test
    fun isSavedWhenIdIsNotNoId() {
        val task = Task(id = 42L)
        assertTrue(task.isSaved)
    }

    @Test
    fun isNotSavedWhenIdIsNoId() {
        val task = Task(id = NO_ID)
        assertFalse(task.isSaved)
    }

    // --- isNew ---

    @Test
    fun isNewWhenIdIsNoId() {
        val task = Task(id = NO_ID)
        assertTrue(task.isNew)
    }

    @Test
    fun isNotNewWhenIdIsSet() {
        val task = Task(id = 1L)
        assertFalse(task.isNew)
    }

    // --- uuid ---

    @Test
    fun uuidReturnsRemoteIdWhenSet() {
        val task = Task(remoteId = "abc-123")
        assertEquals("abc-123", task.uuid)
    }

    @Test
    fun uuidReturnsNoUuidWhenRemoteIdNull() {
        val task = Task(remoteId = null)
        assertEquals(NO_UUID, task.uuid)
    }

    @Test
    fun uuidReturnsNoUuidWhenRemoteIdEmpty() {
        val task = Task(remoteId = "")
        assertEquals(NO_UUID, task.uuid)
    }

    @Test
    fun setUuidUpdatesRemoteId() {
        val task = Task()
        task.uuid = "new-uuid"
        assertEquals("new-uuid", task.remoteId)
    }

    // --- insignificantChange ---

    @Test
    fun insignificantChangeWithSameReference() {
        val task = Task()
        assertTrue(task.insignificantChange(task))
    }

    @Test
    fun insignificantChangeWithNull() {
        val task = Task()
        assertFalse(task.insignificantChange(null))
    }

    @Test
    fun insignificantChangeWithIdenticalTasks() {
        val remoteId = "shared-id"
        val task1 = Task(id = 1, title = "Test", priority = 1, dueDate = 100, remoteId = remoteId)
        val task2 = Task(id = 1, title = "Test", priority = 1, dueDate = 100, remoteId = remoteId)
        assertTrue(task1.insignificantChange(task2))
    }

    @Test
    fun insignificantChangeDetectsTitleDifference() {
        val remoteId = "shared-id"
        val task1 = Task(id = 1, title = "Test A", remoteId = remoteId)
        val task2 = Task(id = 1, title = "Test B", remoteId = remoteId)
        assertFalse(task1.insignificantChange(task2))
    }

    @Test
    fun insignificantChangeDetectsPriorityDifference() {
        val remoteId = "shared-id"
        val task1 = Task(id = 1, priority = Task.Priority.HIGH, remoteId = remoteId)
        val task2 = Task(id = 1, priority = Task.Priority.LOW, remoteId = remoteId)
        assertFalse(task1.insignificantChange(task2))
    }

    @Test
    fun insignificantChangeIgnoresTimerStart() {
        val remoteId = "shared-id"
        val task1 = Task(id = 1, timerStart = 100L, remoteId = remoteId)
        val task2 = Task(id = 1, timerStart = 200L, remoteId = remoteId)
        assertTrue(task1.insignificantChange(task2))
    }

    @Test
    fun insignificantChangeIgnoresReminderLast() {
        val remoteId = "shared-id"
        val task1 = Task(id = 1, reminderLast = 100L, remoteId = remoteId)
        val task2 = Task(id = 1, reminderLast = 200L, remoteId = remoteId)
        assertTrue(task1.insignificantChange(task2))
    }

    // --- googleTaskUpToDate ---

    @Test
    fun googleTaskUpToDateWithSameReference() {
        val task = Task()
        assertTrue(task.googleTaskUpToDate(task))
    }

    @Test
    fun googleTaskUpToDateWithNull() {
        val task = Task()
        assertFalse(task.googleTaskUpToDate(null))
    }

    @Test
    fun googleTaskUpToDateWithIdenticalRelevantFields() {
        val task1 = Task(title = "Test", dueDate = 100, completionDate = 0, deletionDate = 0, parent = 0, notes = "note", order = 1)
        val task2 = Task(title = "Test", dueDate = 100, completionDate = 0, deletionDate = 0, parent = 0, notes = "note", order = 1)
        assertTrue(task1.googleTaskUpToDate(task2))
    }

    @Test
    fun googleTaskUpToDateIgnoresPriority() {
        val task1 = Task(title = "Test", priority = Task.Priority.HIGH)
        val task2 = Task(title = "Test", priority = Task.Priority.LOW)
        assertTrue(task1.googleTaskUpToDate(task2))
    }

    @Test
    fun googleTaskUpToDateDetectsTitleChange() {
        val task1 = Task(title = "A")
        val task2 = Task(title = "B")
        assertFalse(task1.googleTaskUpToDate(task2))
    }

    @Test
    fun googleTaskUpToDateDetectsOrderChange() {
        val task1 = Task(title = "Test", order = 1)
        val task2 = Task(title = "Test", order = 2)
        assertFalse(task1.googleTaskUpToDate(task2))
    }

    // --- caldavUpToDate ---

    @Test
    fun caldavUpToDateWithSameReference() {
        val task = Task()
        assertTrue(task.caldavUpToDate(task))
    }

    @Test
    fun caldavUpToDateWithNull() {
        val task = Task()
        assertFalse(task.caldavUpToDate(null))
    }

    @Test
    fun caldavUpToDateDetectsPriorityChange() {
        val task1 = Task(title = "Test", priority = Task.Priority.HIGH)
        val task2 = Task(title = "Test", priority = Task.Priority.LOW)
        assertFalse(task1.caldavUpToDate(task2))
    }

    @Test
    fun caldavUpToDateDetectsHideUntilChange() {
        val task1 = Task(title = "Test", hideUntil = 100)
        val task2 = Task(title = "Test", hideUntil = 200)
        assertFalse(task1.caldavUpToDate(task2))
    }

    @Test
    fun caldavUpToDateDetectsCollapsedChange() {
        val task1 = Task(title = "Test", isCollapsed = false)
        val task2 = Task(title = "Test", isCollapsed = true)
        assertFalse(task1.caldavUpToDate(task2))
    }

    @Test
    fun caldavUpToDateDetectsRecurrenceChange() {
        val task1 = Task(title = "Test", recurrence = "FREQ=DAILY")
        val task2 = Task(title = "Test", recurrence = "FREQ=WEEKLY")
        assertFalse(task1.caldavUpToDate(task2))
    }

    // --- microsoftUpToDate ---

    @Test
    fun microsoftUpToDateWithSameReference() {
        val task = Task()
        assertTrue(task.microsoftUpToDate(task))
    }

    @Test
    fun microsoftUpToDateWithNull() {
        val task = Task()
        assertFalse(task.microsoftUpToDate(null))
    }

    @Test
    fun microsoftUpToDateDetectsPriorityChange() {
        val task1 = Task(title = "Test", priority = Task.Priority.HIGH)
        val task2 = Task(title = "Test", priority = Task.Priority.LOW)
        assertFalse(task1.microsoftUpToDate(task2))
    }

    @Test
    fun microsoftUpToDateDetectsRecurrenceChange() {
        val task1 = Task(title = "Test", recurrence = "FREQ=DAILY")
        val task2 = Task(title = "Test", recurrence = null)
        assertFalse(task1.microsoftUpToDate(task2))
    }

    @Test
    fun microsoftUpToDateIgnoresHideUntil() {
        val task1 = Task(title = "Test", hideUntil = 100)
        val task2 = Task(title = "Test", hideUntil = 200)
        assertTrue(task1.microsoftUpToDate(task2))
    }

    @Test
    fun microsoftUpToDateIgnoresParent() {
        val task1 = Task(title = "Test", parent = 1)
        val task2 = Task(title = "Test", parent = 2)
        assertTrue(task1.microsoftUpToDate(task2))
    }

    // --- Transitory data ---

    @Test
    fun putAndGetTransitory() {
        val task = Task()
        task.putTransitory("key", "value")
        assertEquals("value", task.getTransitory<String>("key"))
    }

    @Test
    fun hasTransitoryReturnsTrueWhenKeyExists() {
        val task = Task()
        task.putTransitory("key", "value")
        assertTrue(task.hasTransitory("key"))
    }

    @Test
    fun hasTransitoryReturnsFalseWhenKeyMissing() {
        val task = Task()
        assertFalse(task.hasTransitory("key"))
    }

    @Test
    fun hasTransitoryReturnsFalseWhenNoTransitoryData() {
        val task = Task()
        assertFalse(task.hasTransitory("key"))
    }

    @Test
    fun checkTransitoryReturnsTrueWhenKeyExists() {
        val task = Task()
        task.putTransitory("flag", true)
        assertTrue(task.checkTransitory("flag"))
    }

    @Test
    fun checkTransitoryReturnsFalseWhenKeyMissing() {
        val task = Task()
        assertFalse(task.checkTransitory("flag"))
    }

    @Test
    fun getTransitoryReturnsNullWhenNoData() {
        val task = Task()
        assertNull(task.getTransitory<String>("missing"))
    }

    @Test
    fun suppressSyncSetsTransitory() {
        val task = Task()
        task.suppressSync()
        assertTrue(task.hasTransitory(SUPPRESS_SYNC))
    }

    @Test
    fun suppressRefreshSetsTransitory() {
        val task = Task()
        task.suppressRefresh()
        assertTrue(task.isSuppressRefresh())
    }

    @Test
    fun isSuppressRefreshReturnsFalseByDefault() {
        val task = Task()
        assertFalse(task.isSuppressRefresh())
    }

    @Test
    fun randomReminderDefaultsToZero() {
        val task = Task()
        assertEquals(0L, task.randomReminder)
    }

    @Test
    fun setAndGetRandomReminder() {
        val task = Task()
        task.randomReminder = 42L
        assertEquals(42L, task.randomReminder)
    }

    @Test
    fun tagsDefaultsToEmptyList() {
        val task = Task()
        assertTrue(task.tags.isEmpty())
    }

    // --- Notify modes ---

    @Test
    fun isNotifyModeNonstop() {
        val task = Task(ringFlags = Task.NOTIFY_MODE_NONSTOP)
        assertTrue(task.isNotifyModeNonstop)
    }

    @Test
    fun isNotNotifyModeNonstopByDefault() {
        val task = Task()
        assertFalse(task.isNotifyModeNonstop)
    }

    @Test
    fun isNotifyModeFive() {
        val task = Task(ringFlags = Task.NOTIFY_MODE_FIVE)
        assertTrue(task.isNotifyModeFive)
    }

    @Test
    fun isNotNotifyModeFiveByDefault() {
        val task = Task()
        assertFalse(task.isNotifyModeFive)
    }

    // --- Urgency constants ---

    @Test
    fun urgencyConstants() {
        assertEquals(0, Task.URGENCY_NONE)
        assertEquals(7, Task.URGENCY_SPECIFIC_DAY)
        assertEquals(8, Task.URGENCY_SPECIFIC_DAY_TIME)
    }

    // --- Hide until constants ---

    @Test
    fun hideUntilConstants() {
        assertEquals(0, Task.HIDE_UNTIL_NONE)
        assertEquals(1, Task.HIDE_UNTIL_DUE)
        assertEquals(2, Task.HIDE_UNTIL_DAY_BEFORE)
        assertEquals(3, Task.HIDE_UNTIL_WEEK_BEFORE)
        assertEquals(4, Task.HIDE_UNTIL_SPECIFIC_DAY)
        assertEquals(5, Task.HIDE_UNTIL_SPECIFIC_DAY_TIME)
        assertEquals(6, Task.HIDE_UNTIL_DUE_TIME)
    }

    // --- setDueDateAdjustingHideUntil ---

    @Test
    fun setDueDateAdjustsHideUntil() {
        val task = Task(dueDate = 1000L, hideUntil = 500L)
        task.setDueDateAdjustingHideUntil(2000L)
        assertEquals(2000L, task.dueDate)
        assertEquals(1500L, task.hideUntil)
    }

    @Test
    fun setDueDateToZeroClearsHideUntil() {
        val task = Task(dueDate = 1000L, hideUntil = 500L)
        task.setDueDateAdjustingHideUntil(0L)
        assertEquals(0L, task.dueDate)
        assertEquals(0L, task.hideUntil)
    }

    @Test
    fun setDueDateDoesNotAdjustWhenNoPreviousDueDate() {
        val task = Task(dueDate = 0L, hideUntil = 500L)
        task.setDueDateAdjustingHideUntil(2000L)
        assertEquals(2000L, task.dueDate)
        assertEquals(500L, task.hideUntil)
    }

    @Test
    fun setDueDateDoesNotAdjustWhenNoHideUntil() {
        val task = Task(dueDate = 1000L, hideUntil = 0L)
        task.setDueDateAdjustingHideUntil(2000L)
        assertEquals(2000L, task.dueDate)
        assertEquals(0L, task.hideUntil)
    }

    // --- isUuidEmpty ---

    @Test
    fun isUuidEmptyForNoUuid() {
        assertTrue(Task.isUuidEmpty(NO_UUID))
    }

    @Test
    fun isUuidEmptyForNull() {
        assertTrue(Task.isUuidEmpty(null))
    }

    @Test
    fun isUuidEmptyForEmptyString() {
        assertTrue(Task.isUuidEmpty(""))
    }

    @Test
    fun isUuidNotEmptyForValidUuid() {
        assertFalse(Task.isUuidEmpty("abc-123"))
    }

    // --- sanitizeRecur ---

    @Test
    fun sanitizeRecurRemovesBydayWithSemicolon() {
        assertEquals(
            "FREQ=WEEKLY",
            "FREQ=WEEKLYBYDAY=;".sanitizeRecur()
        )
    }

    @Test
    fun sanitizeRecurRemovesInvalidCountMinusOne() {
        assertEquals(
            "FREQ=DAILY",
            "FREQ=DAILY;COUNT=-1".sanitizeRecur()
        )
    }

    @Test
    fun sanitizeRecurRemovesInvalidCountZero() {
        assertEquals(
            "FREQ=DAILY",
            "FREQ=DAILY;COUNT=0".sanitizeRecur()
        )
    }

    @Test
    fun sanitizeRecurReturnsNullForNull() {
        assertNull((null as String?).sanitizeRecur())
    }

    @Test
    fun sanitizeRecurPreservesValidCount() {
        assertEquals(
            "FREQ=DAILY;COUNT=5",
            "FREQ=DAILY;COUNT=5".sanitizeRecur()
        )
    }

    // --- readOnly flag ---

    @Test
    fun readOnlyDefaultIsFalse() {
        val task = Task()
        assertFalse(task.readOnly)
    }

    @Test
    fun readOnlyCanBeSetTrue() {
        val task = Task(readOnly = true)
        assertTrue(task.readOnly)
    }

    // --- RepeatFrom constants ---

    @Test
    fun repeatFromDueDateIsZero() {
        assertEquals(0, Task.RepeatFrom.DUE_DATE)
    }

    @Test
    fun repeatFromCompletionDateIsOne() {
        assertEquals(1, Task.RepeatFrom.COMPLETION_DATE)
    }

    // --- Notification flag constants ---

    @Test
    fun notifyAtDeadlineConstant() {
        assertEquals(1 shl 1, Task.NOTIFY_AT_DEADLINE)
    }

    @Test
    fun notifyAfterDeadlineConstant() {
        assertEquals(1 shl 2, Task.NOTIFY_AFTER_DEADLINE)
    }

    @Test
    fun notifyModeNonstopConstant() {
        assertEquals(1 shl 3, Task.NOTIFY_MODE_NONSTOP)
    }

    @Test
    fun notifyModeFiveConstant() {
        assertEquals(1 shl 4, Task.NOTIFY_MODE_FIVE)
    }

    @Test
    fun notifyAtStartConstant() {
        assertEquals(1 shl 5, Task.NOTIFY_AT_START)
    }

    // --- Additional urgency constants ---

    @Test
    fun urgencyTodayIsOne() {
        assertEquals(1, Task.URGENCY_TODAY)
    }

    @Test
    fun urgencyTomorrowIsTwo() {
        assertEquals(2, Task.URGENCY_TOMORROW)
    }

    @Test
    fun urgencyDayAfterIsThree() {
        assertEquals(3, Task.URGENCY_DAY_AFTER)
    }

    @Test
    fun urgencyNextWeekIsFour() {
        assertEquals(4, Task.URGENCY_NEXT_WEEK)
    }

    @Test
    fun urgencyInTwoWeeksIsFive() {
        assertEquals(5, Task.URGENCY_IN_TWO_WEEKS)
    }

    // --- TABLE_NAME ---

    @Test
    fun tableNameIsTasks() {
        assertEquals("tasks", Task.TABLE_NAME)
    }

    // --- NO_ID ---

    @Test
    fun noIdIsZero() {
        assertEquals(0L, NO_ID)
    }

    // --- NO_UUID ---

    @Test
    fun noUuidIsZeroString() {
        assertEquals("0", NO_UUID)
    }

    // --- isValidUuid ---

    @Test
    fun isValidUuidForPositiveNumber() {
        assertTrue(Task.isValidUuid("12345"))
    }

    @Test
    fun isValidUuidForNonNumericString() {
        // Non-numeric UUIDs trigger NumberFormatException, then fall through to isUuidEmpty
        // A non-empty non-numeric string is not empty, so isUuidEmpty returns false
        assertFalse(Task.isValidUuid("abc-def"))
    }

    @Test
    fun isValidUuidReturnsFalseForNoUuid() {
        // "0" parses as 0L which is not > 0, so returns false
        assertFalse(Task.isValidUuid(NO_UUID))
    }

    // --- isCollapsed ---

    @Test
    fun defaultIsCollapsedIsFalse() {
        val task = Task()
        assertFalse(task.isCollapsed)
    }

    @Test
    fun isCollapsedCanBeSetTrue() {
        val task = Task(isCollapsed = true)
        assertTrue(task.isCollapsed)
    }

    // --- default notes and title ---

    @Test
    fun defaultTitleIsNull() {
        val task = Task()
        assertNull(task.title)
    }

    @Test
    fun defaultNotesIsNull() {
        val task = Task()
        assertNull(task.notes)
    }

    @Test
    fun defaultEstimatedSecondsIsZero() {
        val task = Task()
        assertEquals(0, task.estimatedSeconds)
    }

    @Test
    fun defaultElapsedSecondsIsZero() {
        val task = Task()
        assertEquals(0, task.elapsedSeconds)
    }

    @Test
    fun defaultTimerStartIsZero() {
        val task = Task()
        assertEquals(0L, task.timerStart)
    }

    @Test
    fun defaultRingFlagsIsZero() {
        val task = Task()
        assertEquals(0, task.ringFlags)
    }

    @Test
    fun defaultReminderLastIsZero() {
        val task = Task()
        assertEquals(0L, task.reminderLast)
    }

    @Test
    fun defaultRecurrenceIsNull() {
        val task = Task()
        assertNull(task.recurrence)
    }

    @Test
    fun defaultCalendarURIIsNull() {
        val task = Task()
        assertNull(task.calendarURI)
    }

    @Test
    fun defaultParentIsZero() {
        val task = Task()
        assertEquals(0L, task.parent)
    }

    @Test
    fun defaultOrderIsNull() {
        val task = Task()
        assertNull(task.order)
    }

    @Test
    fun defaultHideUntilIsZero() {
        val task = Task()
        assertEquals(0L, task.hideUntil)
    }

    @Test
    fun defaultCreationDateIsZero() {
        val task = Task()
        assertEquals(0L, task.creationDate)
    }

    @Test
    fun defaultModificationDateIsZero() {
        val task = Task()
        assertEquals(0L, task.modificationDate)
    }

    // --- Data class equality/hashCode ---

    @Test
    fun dataClassEquality() {
        val remoteId = "shared-uuid"
        val a = Task(id = 1, title = "Test", priority = 2, dueDate = 100L, remoteId = remoteId)
        val b = Task(id = 1, title = "Test", priority = 2, dueDate = 100L, remoteId = remoteId)
        assertEquals(a, b)
    }

    @Test
    fun dataClassHashCodeConsistency() {
        val remoteId = "shared-uuid"
        val a = Task(id = 1, title = "Test", priority = 2, remoteId = remoteId)
        val b = Task(id = 1, title = "Test", priority = 2, remoteId = remoteId)
        assertEquals(a.hashCode(), b.hashCode())
    }

    @Test
    fun dataClassInequalityOnDifferentTitle() {
        val remoteId = "shared-uuid"
        val a = Task(id = 1, title = "A", remoteId = remoteId)
        val b = Task(id = 1, title = "B", remoteId = remoteId)
        assertNotEquals(a, b)
    }

    @Test
    fun dataClassInequalityOnDifferentId() {
        val remoteId = "shared-uuid"
        val a = Task(id = 1, title = "Test", remoteId = remoteId)
        val b = Task(id = 2, title = "Test", remoteId = remoteId)
        assertNotEquals(a, b)
    }

    // --- copy preserves values ---

    @Test
    fun copyPreservesFields() {
        val remoteId = "test-uuid"
        val original = Task(id = 1, title = "Original", priority = Task.Priority.HIGH, remoteId = remoteId)
        val copy = original.copy(title = "Modified")
        assertEquals("Modified", copy.title)
        assertEquals(1L, copy.id)
        assertEquals(Task.Priority.HIGH, copy.priority)
        assertEquals(remoteId, copy.remoteId)
    }

    // --- tags transitory data ---

    @Test
    fun tagsTransitoryCanBeSet() {
        val task = Task()
        val tagsList = arrayListOf("tag1", "tag2")
        task.putTransitory(Tag.KEY, tagsList)
        assertEquals(tagsList, task.tags)
    }

    // --- caldavUpToDate detects more field changes ---

    @Test
    fun caldavUpToDateDetectsOrderChange() {
        val task1 = Task(title = "Test", order = 1)
        val task2 = Task(title = "Test", order = 2)
        assertFalse(task1.caldavUpToDate(task2))
    }

    @Test
    fun caldavUpToDateDetectsParentChange() {
        val task1 = Task(title = "Test", parent = 1)
        val task2 = Task(title = "Test", parent = 2)
        assertFalse(task1.caldavUpToDate(task2))
    }

    @Test
    fun caldavUpToDateDetectsNotesChange() {
        val task1 = Task(title = "Test", notes = "A")
        val task2 = Task(title = "Test", notes = "B")
        assertFalse(task1.caldavUpToDate(task2))
    }

    @Test
    fun caldavUpToDateIgnoresRingFlags() {
        val task1 = Task(title = "Test", ringFlags = 0)
        val task2 = Task(title = "Test", ringFlags = Task.NOTIFY_MODE_NONSTOP)
        assertTrue(task1.caldavUpToDate(task2))
    }

    // --- microsoftUpToDate detects more field changes ---

    @Test
    fun microsoftUpToDateDetectsTitleChange() {
        val task1 = Task(title = "A")
        val task2 = Task(title = "B")
        assertFalse(task1.microsoftUpToDate(task2))
    }

    @Test
    fun microsoftUpToDateDetectsDueDateChange() {
        val task1 = Task(title = "Test", dueDate = 100)
        val task2 = Task(title = "Test", dueDate = 200)
        assertFalse(task1.microsoftUpToDate(task2))
    }

    @Test
    fun microsoftUpToDateDetectsCompletionDateChange() {
        val task1 = Task(title = "Test", completionDate = 0)
        val task2 = Task(title = "Test", completionDate = 100)
        assertFalse(task1.microsoftUpToDate(task2))
    }

    @Test
    fun microsoftUpToDateDetectsDeletionDateChange() {
        val task1 = Task(title = "Test", deletionDate = 0)
        val task2 = Task(title = "Test", deletionDate = 100)
        assertFalse(task1.microsoftUpToDate(task2))
    }

    @Test
    fun microsoftUpToDateDetectsNotesChange() {
        val task1 = Task(title = "Test", notes = "A")
        val task2 = Task(title = "Test", notes = "B")
        assertFalse(task1.microsoftUpToDate(task2))
    }

    @Test
    fun microsoftUpToDateIgnoresCollapsed() {
        val task1 = Task(title = "Test", isCollapsed = false)
        val task2 = Task(title = "Test", isCollapsed = true)
        assertTrue(task1.microsoftUpToDate(task2))
    }

    @Test
    fun microsoftUpToDateIgnoresOrder() {
        val task1 = Task(title = "Test", order = 1)
        val task2 = Task(title = "Test", order = 2)
        assertTrue(task1.microsoftUpToDate(task2))
    }

    // --- googleTaskUpToDate detects more field changes ---

    @Test
    fun googleTaskUpToDateDetectsNotesChange() {
        val task1 = Task(title = "Test", notes = "A")
        val task2 = Task(title = "Test", notes = "B")
        assertFalse(task1.googleTaskUpToDate(task2))
    }

    @Test
    fun googleTaskUpToDateDetectsParentChange() {
        val task1 = Task(title = "Test", parent = 1)
        val task2 = Task(title = "Test", parent = 2)
        assertFalse(task1.googleTaskUpToDate(task2))
    }

    @Test
    fun googleTaskUpToDateDetectsCompletionDateChange() {
        val task1 = Task(title = "Test", completionDate = 0)
        val task2 = Task(title = "Test", completionDate = 100)
        assertFalse(task1.googleTaskUpToDate(task2))
    }

    @Test
    fun googleTaskUpToDateDetectsDeletionDateChange() {
        val task1 = Task(title = "Test", deletionDate = 0)
        val task2 = Task(title = "Test", deletionDate = 100)
        assertFalse(task1.googleTaskUpToDate(task2))
    }

    @Test
    fun googleTaskUpToDateDetectsDueDateChange() {
        val task1 = Task(title = "Test", dueDate = 100)
        val task2 = Task(title = "Test", dueDate = 200)
        assertFalse(task1.googleTaskUpToDate(task2))
    }

    @Test
    fun googleTaskUpToDateIgnoresRecurrence() {
        val task1 = Task(title = "Test", recurrence = "FREQ=DAILY")
        val task2 = Task(title = "Test", recurrence = "FREQ=WEEKLY")
        assertTrue(task1.googleTaskUpToDate(task2))
    }

    @Test
    fun googleTaskUpToDateIgnoresHideUntil() {
        val task1 = Task(title = "Test", hideUntil = 100)
        val task2 = Task(title = "Test", hideUntil = 200)
        assertTrue(task1.googleTaskUpToDate(task2))
    }

    // --- insignificantChange detects more field differences ---

    @Test
    fun insignificantChangeDetectsCompletionDateDifference() {
        val remoteId = "shared-id"
        val task1 = Task(id = 1, completionDate = 0, remoteId = remoteId)
        val task2 = Task(id = 1, completionDate = 100, remoteId = remoteId)
        assertFalse(task1.insignificantChange(task2))
    }

    @Test
    fun insignificantChangeDetectsDeletionDateDifference() {
        val remoteId = "shared-id"
        val task1 = Task(id = 1, deletionDate = 0, remoteId = remoteId)
        val task2 = Task(id = 1, deletionDate = 100, remoteId = remoteId)
        assertFalse(task1.insignificantChange(task2))
    }

    @Test
    fun insignificantChangeDetectsNotesDifference() {
        val remoteId = "shared-id"
        val task1 = Task(id = 1, notes = "A", remoteId = remoteId)
        val task2 = Task(id = 1, notes = "B", remoteId = remoteId)
        assertFalse(task1.insignificantChange(task2))
    }

    @Test
    fun insignificantChangeDetectsEstimatedSecondsDifference() {
        val remoteId = "shared-id"
        val task1 = Task(id = 1, estimatedSeconds = 100, remoteId = remoteId)
        val task2 = Task(id = 1, estimatedSeconds = 200, remoteId = remoteId)
        assertFalse(task1.insignificantChange(task2))
    }

    @Test
    fun insignificantChangeDetectsElapsedSecondsDifference() {
        val remoteId = "shared-id"
        val task1 = Task(id = 1, elapsedSeconds = 100, remoteId = remoteId)
        val task2 = Task(id = 1, elapsedSeconds = 200, remoteId = remoteId)
        assertFalse(task1.insignificantChange(task2))
    }

    @Test
    fun insignificantChangeDetectsRingFlagsDifference() {
        val remoteId = "shared-id"
        val task1 = Task(id = 1, ringFlags = 0, remoteId = remoteId)
        val task2 = Task(id = 1, ringFlags = Task.NOTIFY_MODE_NONSTOP, remoteId = remoteId)
        assertFalse(task1.insignificantChange(task2))
    }

    @Test
    fun insignificantChangeDetectsRecurrenceDifference() {
        val remoteId = "shared-id"
        val task1 = Task(id = 1, recurrence = "FREQ=DAILY", remoteId = remoteId)
        val task2 = Task(id = 1, recurrence = "FREQ=WEEKLY", remoteId = remoteId)
        assertFalse(task1.insignificantChange(task2))
    }

    @Test
    fun insignificantChangeDetectsRemoteIdDifference() {
        val task1 = Task(id = 1, remoteId = "aaa")
        val task2 = Task(id = 1, remoteId = "bbb")
        assertFalse(task1.insignificantChange(task2))
    }

    @Test
    fun insignificantChangeDetectsOrderDifference() {
        val remoteId = "shared-id"
        val task1 = Task(id = 1, order = 1, remoteId = remoteId)
        val task2 = Task(id = 1, order = 2, remoteId = remoteId)
        assertFalse(task1.insignificantChange(task2))
    }

    @Test
    fun insignificantChangeDetectsCalendarURIDifference() {
        val remoteId = "shared-id"
        val task1 = Task(id = 1, calendarURI = "cal://a", remoteId = remoteId)
        val task2 = Task(id = 1, calendarURI = "cal://b", remoteId = remoteId)
        assertFalse(task1.insignificantChange(task2))
    }

    @Test
    fun insignificantChangeDetectsParentDifference() {
        val remoteId = "shared-id"
        val task1 = Task(id = 1, parent = 1, remoteId = remoteId)
        val task2 = Task(id = 1, parent = 2, remoteId = remoteId)
        assertFalse(task1.insignificantChange(task2))
    }

    @Test
    fun insignificantChangeDetectsDueDateDifference() {
        val remoteId = "shared-id"
        val task1 = Task(id = 1, dueDate = 100, remoteId = remoteId)
        val task2 = Task(id = 1, dueDate = 200, remoteId = remoteId)
        assertFalse(task1.insignificantChange(task2))
    }

    @Test
    fun insignificantChangeDetectsHideUntilDifference() {
        val remoteId = "shared-id"
        val task1 = Task(id = 1, hideUntil = 100, remoteId = remoteId)
        val task2 = Task(id = 1, hideUntil = 200, remoteId = remoteId)
        assertFalse(task1.insignificantChange(task2))
    }

    @Test
    fun insignificantChangeDetectsCreationDateDifference() {
        val remoteId = "shared-id"
        val task1 = Task(id = 1, creationDate = 100, remoteId = remoteId)
        val task2 = Task(id = 1, creationDate = 200, remoteId = remoteId)
        assertFalse(task1.insignificantChange(task2))
    }

    @Test
    fun insignificantChangeDetectsModificationDateDifference() {
        val remoteId = "shared-id"
        val task1 = Task(id = 1, modificationDate = 100, remoteId = remoteId)
        val task2 = Task(id = 1, modificationDate = 200, remoteId = remoteId)
        assertFalse(task1.insignificantChange(task2))
    }

    // --- sanitizeRecur edge cases ---

    @Test
    fun sanitizeRecurHandlesEmptyString() {
        assertEquals("", "".sanitizeRecur())
    }

    @Test
    fun sanitizeRecurRemovesMultipleInvalidPatterns() {
        assertEquals(
            "FREQ=WEEKLY",
            "FREQ=WEEKLYBYDAY=;;COUNT=-1".sanitizeRecur()
        )
    }

    // --- isUuidEmpty edge cases ---

    @Test
    fun isUuidEmptyForWhitespaceIsNotEmpty() {
        assertFalse(Task.isUuidEmpty("  "))
    }

    // --- TRANS constants ---

    @Test
    fun transDefaultAlarmsConstant() {
        assertEquals("default_alarms", Task.TRANS_DEFAULT_ALARMS)
    }

    @Test
    fun transRandomConstant() {
        assertEquals("random", Task.TRANS_RANDOM)
    }
}
