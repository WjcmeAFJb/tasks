package org.tasks.taskedit

import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.persistentSetOf
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.tasks.data.entity.Alarm
import org.tasks.data.entity.CaldavAccount
import org.tasks.data.entity.CaldavCalendar
import org.tasks.data.entity.TagData
import org.tasks.data.entity.Task
import org.tasks.data.entity.TaskAttachment
import org.tasks.filters.CaldavFilter
import org.tasks.kmp.org.tasks.taskedit.TaskEditViewState

class TaskEditViewStateTest {
    private fun state(
        task: Task = Task(),
        isReadOnly: Boolean = false,
        location: org.tasks.data.Location? = null,
        tags: Set<TagData> = emptySet(),
        calendar: String? = null,
        attachments: Set<TaskAttachment> = emptySet(),
        alarms: Set<Alarm> = emptySet(),
        newSubtasks: List<Task> = emptyList(),
        backButtonSavesTask: Boolean = true,
        linkify: Boolean = false,
        showComments: Boolean = false,
        showKeyboard: Boolean = false,
        showBeastModeHint: Boolean = false,
        multilineTitle: Boolean = false,
        alwaysDisplayFullDate: Boolean = false,
        showEditScreenWithoutUnlock: Boolean = false,
    ) = TaskEditViewState(
        task = task,
        displayOrder = persistentListOf(),
        showBeastModeHint = showBeastModeHint,
        showComments = showComments,
        showKeyboard = showKeyboard,
        backButtonSavesTask = backButtonSavesTask,
        isReadOnly = isReadOnly,
        linkify = linkify,
        alwaysDisplayFullDate = alwaysDisplayFullDate,
        showEditScreenWithoutUnlock = showEditScreenWithoutUnlock,
        list = CaldavFilter(CaldavCalendar(), CaldavAccount()),
        location = location,
        tags = persistentSetOf<TagData>().addAll(tags),
        calendar = calendar,
        attachments = persistentSetOf<TaskAttachment>().addAll(attachments),
        alarms = persistentSetOf<Alarm>().addAll(alarms),
        newSubtasks = persistentListOf<Task>().addAll(newSubtasks),
        multilineTitle = multilineTitle,
    )

    // ===== isNew =====

    @Test fun isNewWhenTaskIsNew() = assertTrue(state(Task()).isNew)
    @Test fun isNotNewWhenSaved() = assertFalse(state(Task(id = 1)).isNew)
    @Test fun isNewWhenIdIsZero() = assertTrue(state(Task(id = 0)).isNew)

    // ===== isCompleted =====

    @Test fun isCompletedWhenHasCompletionDate() = assertTrue(state(Task(completionDate = 1000)).isCompleted)
    @Test fun isNotCompletedWhenNoCompletionDate() = assertFalse(state(Task(completionDate = 0)).isCompleted)
    @Test fun isCompletedWithSmallPositiveValue() = assertTrue(state(Task(completionDate = 1)).isCompleted)

    // ===== hasParent =====

    @Test fun hasParentWhenParentIsSet() = assertTrue(state(Task(parent = 5)).hasParent)
    @Test fun noParentWhenZero() = assertFalse(state(Task(parent = 0)).hasParent)
    @Test fun hasParentWithLargeValue() = assertTrue(state(Task(parent = Long.MAX_VALUE)).hasParent)

    // ===== Properties pass through =====

    @Test fun taskPropertyPassesThrough() {
        val task = Task(id = 42, title = "Test task")
        val s = state(task)
        assertEquals(42L, s.task.id)
        assertEquals("Test task", s.task.title)
    }

    @Test fun isReadOnlyPassesThrough() {
        assertTrue(state(isReadOnly = true).isReadOnly)
        assertFalse(state(isReadOnly = false).isReadOnly)
    }

    @Test fun locationNullByDefault() {
        assertNull(state().location)
    }

    @Test fun calendarNullByDefault() {
        assertNull(state().calendar)
    }

    @Test fun calendarPassesThrough() {
        assertEquals("myCalendar", state(calendar = "myCalendar").calendar)
    }

    @Test fun backButtonSavesTaskDefault() {
        assertTrue(state().backButtonSavesTask)
    }

    @Test fun backButtonSavesTaskFalse() {
        assertFalse(state(backButtonSavesTask = false).backButtonSavesTask)
    }

    @Test fun linkifyDefault() {
        assertFalse(state().linkify)
    }

    @Test fun linkifyTrue() {
        assertTrue(state(linkify = true).linkify)
    }

    @Test fun showCommentsDefault() {
        assertFalse(state().showComments)
    }

    @Test fun showCommentsTrue() {
        assertTrue(state(showComments = true).showComments)
    }

    @Test fun showKeyboardDefault() {
        assertFalse(state().showKeyboard)
    }

    @Test fun showKeyboardTrue() {
        assertTrue(state(showKeyboard = true).showKeyboard)
    }

    @Test fun showBeastModeHintDefault() {
        assertFalse(state().showBeastModeHint)
    }

    @Test fun showBeastModeHintTrue() {
        assertTrue(state(showBeastModeHint = true).showBeastModeHint)
    }

    @Test fun multilineTitleDefault() {
        assertFalse(state().multilineTitle)
    }

    @Test fun multilineTitleTrue() {
        assertTrue(state(multilineTitle = true).multilineTitle)
    }

    @Test fun alwaysDisplayFullDateDefault() {
        assertFalse(state().alwaysDisplayFullDate)
    }

    @Test fun alwaysDisplayFullDateTrue() {
        assertTrue(state(alwaysDisplayFullDate = true).alwaysDisplayFullDate)
    }

    @Test fun showEditScreenWithoutUnlockDefault() {
        assertFalse(state().showEditScreenWithoutUnlock)
    }

    @Test fun showEditScreenWithoutUnlockTrue() {
        assertTrue(state(showEditScreenWithoutUnlock = true).showEditScreenWithoutUnlock)
    }

    // ===== Collections =====

    @Test fun emptyTagsByDefault() {
        assertTrue(state().tags.isEmpty())
    }

    @Test fun tagsPassThrough() {
        val tag = TagData(name = "Important")
        val s = state(tags = setOf(tag))
        assertEquals(1, s.tags.size)
    }

    @Test fun emptyAlarmsByDefault() {
        assertTrue(state().alarms.isEmpty())
    }

    @Test fun alarmsPassThrough() {
        val alarm = Alarm(task = 1, time = 12345L)
        val s = state(alarms = setOf(alarm))
        assertEquals(1, s.alarms.size)
    }

    @Test fun emptyAttachmentsByDefault() {
        assertTrue(state().attachments.isEmpty())
    }

    @Test fun emptyNewSubtasksByDefault() {
        assertTrue(state().newSubtasks.isEmpty())
    }

    @Test fun newSubtasksPassThrough() {
        val subtask = Task(title = "Subtask 1")
        val s = state(newSubtasks = listOf(subtask))
        assertEquals(1, s.newSubtasks.size)
    }
}
