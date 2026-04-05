package org.tasks.sync.microsoft

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.tasks.TestUtilities.withTZ
import org.tasks.data.entity.Task
import org.tasks.sync.microsoft.MicrosoftConverter.applySubtask
import org.tasks.sync.microsoft.MicrosoftConverter.toChecklistItem
import org.tasks.time.DateTime

class MicrosoftConverterSubtaskTest {

    @Test
    fun applySubtaskSetsParent() {
        withTZ("America/Chicago") {
            val task = Task()
            task.applySubtask(
                parent = 42,
                parentCompletionDate = 0,
                checklistItem = Tasks.Task.ChecklistItem(
                    id = "abc",
                    displayName = "Buy milk",
                    isChecked = false,
                ),
            )
            assertEquals(42L, task.parent)
        }
    }

    @Test
    fun applySubtaskSetsTitle() {
        withTZ("UTC") {
            val task = Task()
            task.applySubtask(0, 0, Tasks.Task.ChecklistItem(
                displayName = "Subtask title",
                isChecked = false,
            ))
            assertEquals("Subtask title", task.title)
        }
    }

    @Test
    fun applySubtaskSetsCompletionFromCheckedDateTime() {
        withTZ("UTC") {
            val task = Task()
            task.applySubtask(0, 0, Tasks.Task.ChecklistItem(
                displayName = "Done",
                isChecked = true,
                checkedDateTime = "2023-09-18T04:43:10.117Z",
            ))
            assertTrue(task.completionDate > 0)
        }
    }

    @Test
    fun applyUncheckedSubtaskUsesParentCompletion() {
        withTZ("UTC") {
            val task = Task()
            task.applySubtask(0, 12345L, Tasks.Task.ChecklistItem(
                displayName = "Not done",
                isChecked = false,
            ))
            assertEquals(12345L, task.completionDate)
        }
    }

    @Test
    fun toChecklistItemSetsDisplayName() {
        withTZ("UTC") {
            val task = Task(title = "My subtask")
            val item = task.toChecklistItem("item-id")
            assertEquals("My subtask", item.displayName)
        }
    }

    @Test
    fun toChecklistItemSetsId() {
        withTZ("UTC") {
            val task = Task()
            val item = task.toChecklistItem("item-123")
            assertEquals("item-123", item.id)
        }
    }

    @Test
    fun toChecklistItemNullIdForNewItem() {
        withTZ("UTC") {
            val task = Task()
            val item = task.toChecklistItem(null)
            assertNull(item.id)
        }
    }

    @Test
    fun toChecklistItemCompletedTask() {
        withTZ("UTC") {
            val task = Task(completionDate = DateTime(2023, 9, 18).millis)
            val item = task.toChecklistItem("id")
            assertTrue(item.isChecked)
            assertTrue(item.checkedDateTime != null)
        }
    }

    @Test
    fun toChecklistItemIncompleteTask() {
        withTZ("UTC") {
            val task = Task(completionDate = 0)
            val item = task.toChecklistItem("id")
            assertFalse(item.isChecked)
            assertNull(item.checkedDateTime)
        }
    }

    @Test
    fun toChecklistItemEmptyTitleWhenNull() {
        withTZ("UTC") {
            val task = Task(title = null)
            val item = task.toChecklistItem(null)
            assertEquals("", item.displayName)
        }
    }

    // --- Mutation-killing tests ---

    @Test
    fun applySubtaskSetsCreationDate() {
        withTZ("UTC") {
            val task = Task()
            task.applySubtask(0, 0, Tasks.Task.ChecklistItem(
                displayName = "x",
                isChecked = false,
                createdDateTime = "2023-09-18T04:43:10.117Z",
            ))
            assertTrue(task.creationDate > 0)
        }
    }

    @Test
    fun applySubtaskCheckedUsesCheckedDateTime() {
        withTZ("UTC") {
            val task = Task()
            task.applySubtask(0, 0, Tasks.Task.ChecklistItem(
                displayName = "x",
                isChecked = true,
                checkedDateTime = "2023-09-18T04:43:10.117Z",
            ))
            // completionDate should come from checkedDateTime, not parentCompletionDate
            assertTrue(task.completionDate > 0)
            assertNotEquals(99999L, task.completionDate)
        }
    }

    @Test
    fun applySubtaskUncheckedUsesParentCompletionExactly() {
        withTZ("UTC") {
            val task = Task()
            task.applySubtask(0, 77777L, Tasks.Task.ChecklistItem(
                displayName = "x",
                isChecked = false,
            ))
            assertEquals(77777L, task.completionDate)
        }
    }

    @Test
    fun toChecklistItemHasCreatedDateTime() {
        withTZ("UTC") {
            val task = Task(creationDate = DateTime(2023, 9, 1).millis)
            val item = task.toChecklistItem("id")
            assertNotNull(item.createdDateTime)
            assertTrue(item.createdDateTime!!.contains("2023"))
        }
    }

    @Test
    fun toChecklistItemCompletedHasCheckedDateTime() {
        withTZ("UTC") {
            val task = Task(completionDate = DateTime(2023, 9, 1, 10, 30).millis)
            val item = task.toChecklistItem("id")
            assertTrue(item.isChecked)
            assertNotNull(item.checkedDateTime)
        }
    }

    @Test
    fun toChecklistItemIncompleteNoCheckedDateTime() {
        withTZ("UTC") {
            val task = Task(completionDate = 0)
            val item = task.toChecklistItem("id")
            assertFalse(item.isChecked)
            assertNull(item.checkedDateTime)
        }
    }
}
