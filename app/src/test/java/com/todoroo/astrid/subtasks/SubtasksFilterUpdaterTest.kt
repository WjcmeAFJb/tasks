package com.todoroo.astrid.subtasks

import com.todoroo.astrid.subtasks.SubtasksFilterUpdater.Companion.buildOrderString
import com.todoroo.astrid.subtasks.SubtasksFilterUpdater.Node
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.mockito.Mockito.mock
import org.tasks.data.dao.TaskDao
import org.tasks.data.dao.TaskListMetadataDao

class SubtasksFilterUpdaterTest {

    private lateinit var taskListMetadataDao: TaskListMetadataDao
    private lateinit var taskDao: TaskDao
    private lateinit var updater: SubtasksFilterUpdater

    @Before
    fun setUp() {
        taskListMetadataDao = mock(TaskListMetadataDao::class.java)
        taskDao = mock(TaskDao::class.java)
        updater = SubtasksFilterUpdater(taskListMetadataDao, taskDao)
    }

    // --- buildOrderString ---

    @Test
    fun buildOrderStringWithEmptyList() {
        assertEquals("(1)", buildOrderString(emptyList()))
    }

    @Test
    fun buildOrderStringWithSingleItem() {
        val result = buildOrderString(listOf("uuid1"))
        assertTrue(result.contains("uuid1"))
    }

    @Test
    fun buildOrderStringWithMultipleItems() {
        val result = buildOrderString(listOf("uuid1", "uuid2", "uuid3"))
        assertTrue(result.contains("uuid1"))
        assertTrue(result.contains("uuid2"))
        assertTrue(result.contains("uuid3"))
    }

    @Test
    fun buildOrderStringReversesOrder() {
        val result = buildOrderString(listOf("first", "second"))
        val firstIdx = result.indexOf("second")
        val secondIdx = result.indexOf("first")
        assertTrue(firstIdx < secondIdx)
    }

    @Test
    fun buildOrderStringContainsComma() {
        val result = buildOrderString(listOf("a", "b"))
        assertTrue(result.contains(", "))
    }

    @Test
    fun buildOrderStringWithLargeList() {
        val ids = (1..100).map { "uuid-$it" }
        val result = buildOrderString(ids)
        assertTrue(result.contains("uuid-1"))
        assertTrue(result.contains("uuid-100"))
    }

    @Test
    fun buildOrderStringWithTwoItems() {
        val result = buildOrderString(listOf("aaa", "bbb"))
        val bIdx = result.indexOf("'bbb'")
        val aIdx = result.indexOf("'aaa'")
        assertTrue("bbb should be before aaa in reversed order", bIdx < aIdx)
    }

    @Test
    fun buildOrderStringWithThreeItems() {
        val result = buildOrderString(listOf("xxx", "yyy", "zzz"))
        val zIdx = result.indexOf("'zzz'")
        val yIdx = result.indexOf("'yyy'")
        val xIdx = result.indexOf("'xxx'")
        assertTrue("zzz before yyy", zIdx < yIdx)
        assertTrue("yyy before xxx", yIdx < xIdx)
    }

    // --- getIndentForTask (before initialization) ---

    @Test
    fun getIndentForTaskReturnsZeroForUnknownTask() {
        assertEquals(0, updater.getIndentForTask("unknown"))
    }

    @Test
    fun getIndentForTaskReturnsZeroForNull() {
        assertEquals(0, updater.getIndentForTask(null))
    }

    // --- findNodeForTask (before initialization) ---

    @Test
    fun findNodeForTaskReturnsNullForUnknownTask() {
        assertNull(updater.findNodeForTask("unknown"))
    }

    @Test
    fun findNodeForTaskReturnsNullForNull() {
        assertNull(updater.findNodeForTask(null))
    }

    // --- Node ---

    @Test
    fun nodeInitialization() {
        val parent = Node("parent-uuid", null, 0)
        val child = Node("child-uuid", parent, 1)
        assertEquals("child-uuid", child.uuid)
        assertEquals(parent, child.parent)
        assertEquals(1, child.indent)
        assertEquals(0, child.children.size)
    }

    @Test
    fun nodeChildrenIsMutable() {
        val root = Node("root", null, -1)
        val child = Node("child", root, 0)
        root.children.add(child)
        assertEquals(1, root.children.size)
        assertEquals("child", root.children[0].uuid)
    }

    @Test
    fun nodeUuidIsMutable() {
        val node = Node("original", null, 0)
        node.uuid = "changed"
        assertEquals("changed", node.uuid)
    }

    @Test
    fun nodeIndentIsMutable() {
        val node = Node("uuid", null, 0)
        node.indent = 5
        assertEquals(5, node.indent)
    }

    @Test
    fun nodeParentIsMutable() {
        val parent1 = Node("p1", null, -1)
        val parent2 = Node("p2", null, -1)
        val child = Node("child", parent1, 0)
        assertEquals(parent1, child.parent)
        child.parent = parent2
        assertEquals(parent2, child.parent)
    }

    @Test
    fun nodeChildrenCanBeRemoved() {
        val root = Node("root", null, -1)
        val child1 = Node("c1", root, 0)
        val child2 = Node("c2", root, 0)
        root.children.add(child1)
        root.children.add(child2)
        root.children.remove(child1)
        assertEquals(1, root.children.size)
        assertEquals("c2", root.children[0].uuid)
    }

    @Test
    fun nodeChildrenCanBeInserted() {
        val root = Node("root", null, -1)
        val child1 = Node("c1", root, 0)
        val child2 = Node("c2", root, 0)
        root.children.add(child1)
        root.children.add(0, child2)
        assertEquals("c2", root.children[0].uuid)
        assertEquals("c1", root.children[1].uuid)
    }

    // --- isDescendantOf (before initialization) ---

    @Test
    fun isDescendantOfReturnsFalseForNullNodes() {
        assertFalse(updater.isDescendantOf("unknown1", "unknown2"))
    }

    @Test
    fun isDescendantOfReturnsFalseWhenBothNull() {
        assertFalse(updater.isDescendantOf(null, null))
    }

    // --- Constants ---

    @Test
    fun activeTasksOrderConstant() {
        assertEquals("active_tasks_order", SubtasksFilterUpdater.ACTIVE_TASKS_ORDER)
    }

    @Test
    fun todayTasksOrderConstant() {
        assertEquals("today_tasks_order", SubtasksFilterUpdater.TODAY_TASKS_ORDER)
    }
}
