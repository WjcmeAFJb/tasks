package org.tasks.taskedit

import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.persistentSetOf
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.tasks.data.entity.CaldavAccount
import org.tasks.data.entity.CaldavCalendar
import org.tasks.data.entity.Task
import org.tasks.filters.CaldavFilter
import org.tasks.kmp.org.tasks.taskedit.TaskEditViewState

class TaskEditViewStateTest {
    private fun state(task: Task = Task()) = TaskEditViewState(
        task = task,
        displayOrder = persistentListOf(),
        showBeastModeHint = false,
        showComments = false,
        showKeyboard = false,
        backButtonSavesTask = true,
        isReadOnly = false,
        linkify = false,
        alwaysDisplayFullDate = false,
        showEditScreenWithoutUnlock = false,
        list = CaldavFilter(CaldavCalendar(), CaldavAccount()),
        location = null,
        tags = persistentSetOf(),
        calendar = null,
        alarms = persistentSetOf(),
        multilineTitle = false,
    )

    @Test fun isNewWhenTaskIsNew() = assertTrue(state(Task()).isNew)
    @Test fun isNotNewWhenSaved() = assertFalse(state(Task(id = 1)).isNew)
    @Test fun isCompletedWhenHasCompletionDate() = assertTrue(state(Task(completionDate = 1000)).isCompleted)
    @Test fun isNotCompletedWhenNoCompletionDate() = assertFalse(state(Task(completionDate = 0)).isCompleted)
    @Test fun hasParentWhenParentIsSet() = assertTrue(state(Task(parent = 5)).hasParent)
    @Test fun noParentWhenZero() = assertFalse(state(Task(parent = 0)).hasParent)
}
