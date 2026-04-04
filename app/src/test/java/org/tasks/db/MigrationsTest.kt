package org.tasks.db

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import org.tasks.data.entity.Task
import org.tasks.db.Migrations.repeatFrom
import org.tasks.db.Migrations.withoutFrom

class MigrationsTest {

    // --- repeatFrom ---

    @Test
    fun repeatFromReturnsCompletionDateWhenContainsFromCompletion() {
        val recurrence = "FREQ=DAILY;FROM=COMPLETION"
        assertEquals(Task.RepeatFrom.COMPLETION_DATE, recurrence.repeatFrom())
    }

    @Test
    fun repeatFromReturnsDueDateWhenNoFromCompletion() {
        val recurrence = "FREQ=DAILY"
        assertEquals(Task.RepeatFrom.DUE_DATE, recurrence.repeatFrom())
    }

    @Test
    fun repeatFromReturnsDueDateForNull() {
        assertEquals(Task.RepeatFrom.DUE_DATE, (null as String?).repeatFrom())
    }

    @Test
    fun repeatFromReturnsDueDateForEmptyString() {
        assertEquals(Task.RepeatFrom.DUE_DATE, "".repeatFrom())
    }

    @Test
    fun repeatFromReturnsCompletionDateWithExtraParams() {
        val recurrence = "FREQ=WEEKLY;BYDAY=MO;FROM=COMPLETION;COUNT=5"
        assertEquals(Task.RepeatFrom.COMPLETION_DATE, recurrence.repeatFrom())
    }

    @Test
    fun repeatFromReturnsDueDateWhenFromDueDate() {
        val recurrence = "FREQ=DAILY;FROM=DUE_DATE"
        assertEquals(Task.RepeatFrom.DUE_DATE, recurrence.repeatFrom())
    }

    // --- withoutFrom ---

    @Test
    fun withoutFromRemovesFromCompletion() {
        assertEquals("FREQ=DAILY", "FREQ=DAILY;FROM=COMPLETION".withoutFrom())
    }

    @Test
    fun withoutFromRemovesFromDueDate() {
        assertEquals("FREQ=DAILY", "FREQ=DAILY;FROM=DUE_DATE".withoutFrom())
    }

    @Test
    fun withoutFromReturnsNullForNull() {
        assertNull((null as String?).withoutFrom())
    }

    @Test
    fun withoutFromPreservesStringWithoutFrom() {
        assertEquals("FREQ=WEEKLY;BYDAY=MO", "FREQ=WEEKLY;BYDAY=MO".withoutFrom())
    }

    @Test
    fun withoutFromRemovesFromInMiddle() {
        assertEquals(
            "FREQ=WEEKLY;BYDAY=MO;COUNT=5",
            "FREQ=WEEKLY;BYDAY=MO;FROM=COMPLETION;COUNT=5".withoutFrom()
        )
    }

    @Test
    fun withoutFromRemovesFromAtStart() {
        assertEquals(";FREQ=DAILY", "FROM=COMPLETION;FREQ=DAILY".withoutFrom())
    }

    @Test
    fun withoutFromHandlesEmptyString() {
        assertEquals("", "".withoutFrom())
    }
}
