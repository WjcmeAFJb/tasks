package org.tasks.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.tasks.data.entity.CaldavAccount
import org.tasks.data.entity.CaldavTask
import org.tasks.data.entity.Task

class TaskContainerTest {
    private fun container(
        task: Task = Task(),
        caldavTask: CaldavTask? = null,
        accountType: Int = CaldavAccount.TYPE_LOCAL,
        children: Int = 0,
        indent: Int = 0,
        primarySort: Long = 0,
        secondarySort: Long = 0,
    ) = TaskContainer(
        task = task,
        caldavTask = caldavTask,
        accountType = accountType,
        children = children,
        indent = indent,
        primarySort = primarySort,
        secondarySort = secondarySort,
    )

    @Test fun isGoogleTaskTrue() =
        assertTrue(container(accountType = CaldavAccount.TYPE_GOOGLE_TASKS).isGoogleTask)

    @Test fun isGoogleTaskFalse() =
        assertFalse(container(accountType = CaldavAccount.TYPE_CALDAV).isGoogleTask)

    @Test fun isSingleLevelSubtaskForGoogleTasks() =
        assertTrue(container(accountType = CaldavAccount.TYPE_GOOGLE_TASKS).isSingleLevelSubtask)

    @Test fun isSingleLevelSubtaskForMicrosoft() =
        assertTrue(container(accountType = CaldavAccount.TYPE_MICROSOFT).isSingleLevelSubtask)

    @Test fun isNotSingleLevelSubtaskForCaldav() =
        assertFalse(container(accountType = CaldavAccount.TYPE_CALDAV).isSingleLevelSubtask)

    @Test fun caldavReturnsCalendar() {
        val ct = CaldavTask(calendar = "my-cal")
        assertEquals("my-cal", container(caldavTask = ct).caldav)
    }

    @Test fun caldavNullWhenNoCaldavTask() =
        assertNull(container(caldavTask = null).caldav)

    @Test fun isCaldavTaskTrue() =
        assertTrue(container(caldavTask = CaldavTask(calendar = "c")).isCaldavTask())

    @Test fun isCaldavTaskFalse() =
        assertFalse(container(caldavTask = null).isCaldavTask())

    @Test fun titleFromTask() =
        assertEquals("Test", container(task = Task(title = "Test")).title)

    @Test fun notesFromTask() =
        assertEquals("Notes", container(task = Task(notes = "Notes")).notes)

    @Test fun isCompletedFromTask() =
        assertTrue(container(task = Task(completionDate = 1000)).isCompleted)

    @Test fun hasDueDateFromTask() =
        assertTrue(container(task = Task(dueDate = 1000)).hasDueDate())

    @Test fun noDueDateFromTask() =
        assertFalse(container(task = Task(dueDate = 0)).hasDueDate())

    @Test fun idFromTask() =
        assertEquals(42L, container(task = Task(id = 42)).id)

    @Test fun uuidFromTask() {
        val task = Task()
        task.remoteId = "uuid-123"
        assertEquals("uuid-123", container(task = task).uuid)
    }

    @Test fun hasParentTrue() =
        assertTrue(container(task = Task(parent = 5)).hasParent())

    @Test fun hasParentFalse() =
        assertFalse(container(task = Task(parent = 0)).hasParent())

    @Test fun hasChildrenTrue() = assertTrue(container(children = 3).hasChildren())
    @Test fun hasChildrenFalse() = assertFalse(container(children = 0).hasChildren())
    @Test fun hasLocationFalse() = assertFalse(container().hasLocation())

    @Test fun isCollapsedFromTask() =
        assertTrue(container(task = Task(isCollapsed = true)).isCollapsed)

    @Test fun caldavSortOrderUsePrimaryAtIndent0() =
        assertEquals(100L, container(indent = 0, primarySort = 100, secondarySort = 200).caldavSortOrder)

    @Test fun caldavSortOrderUseSecondaryAtIndent1() =
        assertEquals(200L, container(indent = 1, primarySort = 100, secondarySort = 200).caldavSortOrder)

    @Test fun priorityFromTask() =
        assertEquals(Task.Priority.HIGH, container(task = Task(priority = Task.Priority.HIGH)).priority)

    @Test fun isReadOnlyFromTask() =
        assertTrue(container(task = Task(readOnly = true)).isReadOnly)

    @Test fun parentSetterUpdatesTask() {
        val tc = container(task = Task())
        tc.parent = 99
        assertEquals(99L, tc.task.parent)
    }
}
