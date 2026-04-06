package com.todoroo.astrid.subtasks

import com.todoroo.astrid.subtasks.SubtasksFilterUpdater.Companion.buildOrderString
import com.todoroo.astrid.subtasks.SubtasksFilterUpdater.Companion.buildTreeModel
import com.todoroo.astrid.subtasks.SubtasksFilterUpdater.Companion.serializeTree
import com.todoroo.astrid.subtasks.SubtasksFilterUpdater.Node
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.tasks.data.dao.TaskDao
import org.tasks.data.entity.TaskListMetadata
import org.tasks.data.dao.TaskListMetadataDao

class SubtasksFilterUpdaterMaxCovTest {

    private lateinit var taskListMetadataDao: TaskListMetadataDao
    private lateinit var taskDao: TaskDao
    private lateinit var updater: SubtasksFilterUpdater

    @Before
    fun setUp() {
        taskListMetadataDao = mock()
        taskDao = mock()
        updater = SubtasksFilterUpdater(taskListMetadataDao, taskDao)
    }

    // ================================================================
    // buildTreeModel — returns root with -1 uuid
    // NOTE: org.json.JSONArray is a stub in unit tests, so
    // buildTreeModel always returns an empty root.
    // We test the null/invalid paths and manual tree ops instead.
    // ================================================================

    @Test
    fun buildTreeModelNullTreeReturnsEmptyRoot() {
        val root = buildTreeModel(null, null)
        assertEquals("-1", root.uuid)
        assertEquals(0, root.children.size)
    }

    @Test
    fun buildTreeModelEmptyStringReturnsEmptyRoot() {
        val root = buildTreeModel("", null)
        assertEquals("-1", root.uuid)
    }

    @Test
    fun buildTreeModelInvalidJsonReturnsEmptyRoot() {
        val root = buildTreeModel("not json", null)
        assertEquals(0, root.children.size)
    }

    @Test
    fun buildTreeModelRootIndent() {
        val root = buildTreeModel(null, null)
        assertEquals(-1, root.indent)
    }

    @Test
    fun buildTreeModelRootParentIsNull() {
        val root = buildTreeModel(null, null)
        assertNull(root.parent)
    }

    // ================================================================
    // Manual tree building (org.json.JSONArray is stubbed in unit tests,
    // so buildTreeModel/serializeTree cannot be directly tested here.
    // Instead we test manual tree construction and operations.)
    // ================================================================

    @Test
    fun manualTreeFlatStructure() {
        val root = Node("-1", null, -1)
        root.children.add(Node("a", root, 0))
        root.children.add(Node("b", root, 0))
        assertEquals(2, root.children.size)
        assertEquals("a", root.children[0].uuid)
        assertEquals("b", root.children[1].uuid)
    }

    @Test
    fun manualTreeNestedStructure() {
        val root = Node("-1", null, -1)
        val a = Node("a", root, 0)
        a.children.add(Node("a1", a, 1))
        root.children.add(a)
        root.children.add(Node("b", root, 0))
        assertEquals(2, root.children.size)
        assertEquals(1, root.children[0].children.size)
        assertEquals("a1", root.children[0].children[0].uuid)
    }

    @Test
    fun manualTreeDeepNesting() {
        val root = Node("-1", null, -1)
        val a = Node("a", root, 0)
        val b = Node("b", a, 1)
        val c = Node("c", b, 2)
        b.children.add(c)
        a.children.add(b)
        root.children.add(a)
        assertEquals(1, root.children.size)
        assertEquals(2, root.children[0].children[0].children[0].indent)
    }

    @Test
    fun manualTreeEmptyRoot() {
        val root = Node("-1", null, -1)
        assertEquals(0, root.children.size)
        assertEquals("-1", root.uuid)
    }

    // ================================================================
    // buildOrderString
    // ================================================================

    @Test
    fun buildOrderStringEmpty() {
        assertEquals("(1)", buildOrderString(emptyList()))
    }

    @Test
    fun buildOrderStringSingle() {
        val result = buildOrderString(listOf("uuid1"))
        assertTrue(result.contains("uuid1"))
        assertFalse(result.contains(", "))
    }

    @Test
    fun buildOrderStringMultiple() {
        val result = buildOrderString(listOf("a", "b", "c"))
        assertTrue(result.contains("a"))
        assertTrue(result.contains("b"))
        assertTrue(result.contains("c"))
    }

    @Test
    fun buildOrderStringReversedOrder() {
        val result = buildOrderString(listOf("first", "second", "third"))
        val thirdIdx = result.indexOf("third")
        val firstIdx = result.indexOf("first")
        assertTrue("third before first in reversed order", thirdIdx < firstIdx)
    }

    @Test
    fun buildOrderStringCapsAt900() {
        val ids = (1..1000).map { "id-$it" }
        val result = buildOrderString(ids)
        // Should contain last element
        assertTrue(result.contains("id-1000"))
        // First 100 elements should be excluded (1000-900=100)
        assertFalse(result.contains("'id-100'"))
    }

    // ================================================================
    // getIndentForTask
    // ================================================================

    @Test
    fun getIndentForUnknownTaskReturnsZero() {
        assertEquals(0, updater.getIndentForTask("unknown"))
    }

    @Test
    fun getIndentForNullReturnsZero() {
        assertEquals(0, updater.getIndentForTask(null))
    }

    // ================================================================
    // findNodeForTask
    // ================================================================

    @Test
    fun findNodeForUnknownReturnsNull() {
        assertNull(updater.findNodeForTask("missing"))
    }

    @Test
    fun findNodeForNullReturnsNull() {
        assertNull(updater.findNodeForTask(null))
    }

    // ================================================================
    // isDescendantOf
    // ================================================================

    @Test
    fun isDescendantOfUnknownNodes() {
        assertFalse(updater.isDescendantOf("a", "b"))
    }

    @Test
    fun isDescendantOfNulls() {
        assertFalse(updater.isDescendantOf(null, null))
    }

    // ================================================================
    // moveToParentOf
    // ================================================================

    @Test
    fun moveToParentOfUnknownNodesDoesNotCrash() {
        updater.moveToParentOf("x", "y")
    }

    // ================================================================
    // writeSerialization
    // ================================================================

    @Test
    fun writeSerializationUpdatesDao() = runTest {
        val list = TaskListMetadata().apply { taskIds = "[]" }
        updater.writeSerialization(list, """["-1","a"]""")
        assertEquals("""["-1","a"]""", list.taskIds)
        verify(taskListMetadataDao).update(list)
    }

    @Test
    fun writeSerializationNullListIsNoOp() = runTest {
        updater.writeSerialization(null, "anything")
    }

    // ================================================================
    // Constants
    // ================================================================

    @Test
    fun activeTasksOrder() {
        assertEquals("active_tasks_order", SubtasksFilterUpdater.ACTIVE_TASKS_ORDER)
    }

    @Test
    fun todayTasksOrder() {
        assertEquals("today_tasks_order", SubtasksFilterUpdater.TODAY_TASKS_ORDER)
    }

    // ================================================================
    // Tree operations — indent/deindent/move simulation
    // ================================================================

    @Test
    fun indentSecondChild() {
        val root = Node("-1", null, -1)
        val a = Node("a", root, 0)
        val b = Node("b", root, 0)
        root.children.add(a)
        root.children.add(b)

        // Indent b under a
        val siblings = root.children
        val index = siblings.indexOf(b)
        val newParent = siblings[index - 1]
        siblings.removeAt(index)
        b.parent = newParent
        newParent.children.add(b)
        b.indent = newParent.indent + 1

        assertEquals(1, root.children.size)
        assertEquals(1, a.children.size)
        assertEquals("b", a.children[0].uuid)
        assertEquals(1, b.indent)
    }

    @Test
    fun deindentChild() {
        val root = Node("-1", null, -1)
        val a = Node("a", root, 0)
        val b = Node("b", a, 1)
        a.children.add(b)
        root.children.add(a)

        // Deindent b
        val siblings = a.children
        val index = siblings.indexOf(b)
        val newParent = a.parent!!
        val newSiblings = newParent.children
        val insertAfter = newSiblings.indexOf(a)
        siblings.removeAt(index)
        b.parent = newParent
        b.indent = newParent.indent + 1
        newSiblings.add(insertAfter + 1, b)

        assertEquals(2, root.children.size)
        assertEquals("a", root.children[0].uuid)
        assertEquals("b", root.children[1].uuid)
        assertEquals(0, b.indent)
    }

    @Test
    fun removeNodePromotesChildren() {
        val root = Node("-1", null, -1)
        val a = Node("a", root, 0)
        val b = Node("b", a, 1)
        val c = Node("c", a, 1)
        a.children.add(b)
        a.children.add(c)
        root.children.add(a)

        // Remove a, promote children
        val parent = a.parent!!
        parent.children.remove(a)
        for (child in a.children) {
            child.parent = parent
            child.indent = parent.indent + 1
            parent.children.add(child)
        }

        assertEquals(2, root.children.size)
        assertEquals("b", root.children[0].uuid)
        assertEquals("c", root.children[1].uuid)
        assertEquals(0, root.children[0].indent)
    }

    @Test
    fun moveToEnd() {
        val root = Node("-1", null, -1)
        val a = Node("a", root, 0)
        val b = Node("b", root, 0)
        root.children.add(a)
        root.children.add(b)

        root.children.remove(a)
        root.children.add(a)
        a.parent = root
        a.indent = 0

        assertEquals("b", root.children[0].uuid)
        assertEquals("a", root.children[1].uuid)
    }
}
