package com.todoroo.astrid.subtasks

import com.todoroo.astrid.subtasks.SubtasksFilterUpdater.Companion.buildOrderString
import com.todoroo.astrid.subtasks.SubtasksFilterUpdater.Node
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.mockito.Mockito.mock
import org.mockito.Mockito.verify
import org.mockito.Mockito.verifyNoInteractions
import org.tasks.data.dao.TaskDao
import org.tasks.data.entity.TaskListMetadata
import org.tasks.data.dao.TaskListMetadataDao

class SubtasksFilterUpdaterDeepTest {

    private lateinit var taskListMetadataDao: TaskListMetadataDao
    private lateinit var taskDao: TaskDao
    private lateinit var updater: SubtasksFilterUpdater

    @Before
    fun setUp() {
        taskListMetadataDao = mock()
        taskDao = mock()
        updater = SubtasksFilterUpdater(taskListMetadataDao, taskDao)
    }

    // ===== buildOrderString =====

    @Test
    fun buildOrderStringEmpty() {
        assertEquals("(1)", buildOrderString(emptyList()))
    }

    @Test
    fun buildOrderStringSingleItem() {
        val result = buildOrderString(listOf("uuid1"))
        assertTrue(result.contains("uuid1"))
    }

    @Test
    fun buildOrderStringMultipleItems() {
        val result = buildOrderString(listOf("a", "b", "c"))
        assertTrue(result.contains("a"))
        assertTrue(result.contains("b"))
        assertTrue(result.contains("c"))
    }

    @Test
    fun buildOrderStringReversesOrder() {
        val result = buildOrderString(listOf("first", "second"))
        val secondIdx = result.indexOf("second")
        val firstIdx = result.indexOf("first")
        assertTrue("second should come before first in reversed order", secondIdx < firstIdx)
    }

    @Test
    fun buildOrderStringContainsSeparator() {
        val result = buildOrderString(listOf("a", "b"))
        assertTrue(result.contains(", "))
    }

    @Test
    fun buildOrderStringWith900Items() {
        val ids = (1..900).map { "uuid-$it" }
        val result = buildOrderString(ids)
        assertTrue(result.contains("uuid-1"))
        assertTrue(result.contains("uuid-900"))
    }

    @Test
    fun buildOrderStringWith1000ItemsTruncates() {
        val ids = (1..1000).map { "uuid-$it" }
        val result = buildOrderString(ids)
        // Last element (index 999) should always be included (reversed)
        assertTrue(result.contains("uuid-1000"))
    }

    @Test
    fun buildOrderStringThreeItemsReversed() {
        val result = buildOrderString(listOf("x", "y", "z"))
        val zIdx = result.indexOf("'z'")
        val yIdx = result.indexOf("'y'")
        val xIdx = result.indexOf("'x'")
        assertTrue("z before y", zIdx < yIdx)
        assertTrue("y before x", yIdx < xIdx)
    }

    @Test
    fun buildOrderStringFourItemsReversed() {
        val result = buildOrderString(listOf("a", "b", "c", "d"))
        val dIdx = result.indexOf("'d'")
        val cIdx = result.indexOf("'c'")
        val bIdx = result.indexOf("'b'")
        val aIdx = result.indexOf("'a'")
        assertTrue(dIdx < cIdx)
        assertTrue(cIdx < bIdx)
        assertTrue(bIdx < aIdx)
    }

    @Test
    fun buildOrderStringWithSpecialCharacters() {
        val result = buildOrderString(listOf("uuid-with-dash", "uuid.with.dot"))
        assertTrue(result.contains("uuid-with-dash"))
        assertTrue(result.contains("uuid.with.dot"))
    }

    @Test
    fun buildOrderStringWithLongUuids() {
        val longUuid = "a".repeat(100)
        val result = buildOrderString(listOf(longUuid))
        assertTrue(result.contains(longUuid))
    }

    // ===== getIndentForTask =====

    @Test
    fun getIndentForTaskUnknown() {
        assertEquals(0, updater.getIndentForTask("unknown"))
    }

    @Test
    fun getIndentForTaskNull() {
        assertEquals(0, updater.getIndentForTask(null))
    }

    @Test
    fun getIndentForTaskEmptyString() {
        assertEquals(0, updater.getIndentForTask(""))
    }

    // ===== findNodeForTask =====

    @Test
    fun findNodeForTaskUnknown() {
        assertNull(updater.findNodeForTask("unknown"))
    }

    @Test
    fun findNodeForTaskNull() {
        assertNull(updater.findNodeForTask(null))
    }

    @Test
    fun findNodeForTaskEmptyString() {
        assertNull(updater.findNodeForTask(""))
    }

    // ===== isDescendantOf =====

    @Test
    fun isDescendantOfUnknownNodes() {
        assertFalse(updater.isDescendantOf("a", "b"))
    }

    @Test
    fun isDescendantOfNullNodes() {
        assertFalse(updater.isDescendantOf(null, null))
    }

    @Test
    fun isDescendantOfOneNullOneUnknown() {
        assertFalse(updater.isDescendantOf(null, "a"))
        assertFalse(updater.isDescendantOf("a", null))
    }

    // ===== writeSerialization =====

    @Test
    fun writeSerializationUpdatesListAndDao() = runTest {
        val list = TaskListMetadata().apply { taskIds = "[]" }

        updater.writeSerialization(list, """["-1","uuid1"]""")

        assertEquals("""["-1","uuid1"]""", list.taskIds)
        verify(taskListMetadataDao).update(list)
    }

    @Test
    fun writeSerializationDoesNothingForNullList() = runTest {
        updater.writeSerialization(null, """["-1"]""")

        verifyNoInteractions(taskListMetadataDao)
    }

    @Test
    fun writeSerializationSetsValue() = runTest {
        val list = TaskListMetadata().apply { taskIds = "old" }

        updater.writeSerialization(list, "new-value")

        assertEquals("new-value", list.taskIds)
    }

    @Test
    fun writeSerializationWithEmptyString() = runTest {
        val list = TaskListMetadata().apply { taskIds = "old" }

        updater.writeSerialization(list, "")

        assertEquals("", list.taskIds)
    }

    @Test
    fun writeSerializationWithNullSerialized() = runTest {
        val list = TaskListMetadata().apply { taskIds = "old" }

        updater.writeSerialization(list, null)

        assertNull(list.taskIds)
        verify(taskListMetadataDao).update(list)
    }

    // ===== Constants =====

    @Test
    fun activeTasksOrderConstant() {
        assertEquals("active_tasks_order", SubtasksFilterUpdater.ACTIVE_TASKS_ORDER)
    }

    @Test
    fun todayTasksOrderConstant() {
        assertEquals("today_tasks_order", SubtasksFilterUpdater.TODAY_TASKS_ORDER)
    }

    // ===== Node properties =====

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
    fun nodeChildrenMutable() {
        val root = Node("root", null, -1)
        val child = Node("c", root, 0)
        root.children.add(child)
        assertEquals(1, root.children.size)
        assertEquals("c", root.children[0].uuid)
    }

    @Test
    fun nodeParentMutable() {
        val p1 = Node("p1", null, -1)
        val p2 = Node("p2", null, -1)
        val child = Node("c", p1, 0)
        child.parent = p2
        assertEquals(p2, child.parent)
    }

    @Test
    fun nodeIndentMutable() {
        val node = Node("n", null, 0)
        node.indent = 5
        assertEquals(5, node.indent)
    }

    @Test
    fun nodeUuidMutable() {
        val node = Node("old", null, 0)
        node.uuid = "new"
        assertEquals("new", node.uuid)
    }

    @Test
    fun nodeChildrenCanBeCleared() {
        val root = Node("root", null, -1)
        root.children.add(Node("c1", root, 0))
        root.children.add(Node("c2", root, 0))
        root.children.clear()
        assertEquals(0, root.children.size)
    }

    @Test
    fun nodeChildrenCanBeRemoved() {
        val root = Node("root", null, -1)
        val c1 = Node("c1", root, 0)
        val c2 = Node("c2", root, 0)
        root.children.add(c1)
        root.children.add(c2)
        root.children.remove(c1)
        assertEquals(1, root.children.size)
        assertEquals("c2", root.children[0].uuid)
    }

    @Test
    fun nodeChildrenCanBeInsertedAtIndex() {
        val root = Node("root", null, -1)
        val c1 = Node("c1", root, 0)
        val c2 = Node("c2", root, 0)
        root.children.add(c1)
        root.children.add(0, c2)
        assertEquals("c2", root.children[0].uuid)
        assertEquals("c1", root.children[1].uuid)
    }

    @Test
    fun nodeChildIndexOf() {
        val root = Node("root", null, -1)
        val c1 = Node("c1", root, 0)
        val c2 = Node("c2", root, 0)
        root.children.add(c1)
        root.children.add(c2)
        assertEquals(0, root.children.indexOf(c1))
        assertEquals(1, root.children.indexOf(c2))
    }

    @Test
    fun nodeChildRemoveAt() {
        val root = Node("root", null, -1)
        val c1 = Node("c1", root, 0)
        val c2 = Node("c2", root, 0)
        root.children.add(c1)
        root.children.add(c2)
        root.children.removeAt(0)
        assertEquals(1, root.children.size)
        assertEquals("c2", root.children[0].uuid)
    }

    @Test
    fun nodeWithMultipleChildLevels() {
        val root = Node("root", null, -1)
        val child = Node("child", root, 0)
        val grandchild = Node("grandchild", child, 1)
        val greatGrandchild = Node("greatGrandchild", grandchild, 2)
        grandchild.children.add(greatGrandchild)
        child.children.add(grandchild)
        root.children.add(child)
        assertEquals(1, root.children.size)
        assertEquals(1, root.children[0].children.size)
        assertEquals(1, root.children[0].children[0].children.size)
        assertEquals("greatGrandchild", root.children[0].children[0].children[0].uuid)
    }

    @Test
    fun nodeNullParent() {
        val node = Node("orphan", null, 0)
        assertNull(node.parent)
    }

    @Test
    fun nodeNegativeIndent() {
        val root = Node("-1", null, -1)
        assertEquals(-1, root.indent)
    }

    // ===== moveToParentOf uses idToNode =====

    @Test
    fun moveToParentOfUnknownNodesDoesNotCrash() {
        // Before initialization, idToNode is empty
        updater.moveToParentOf("unknown1", "unknown2")
        // No crash, no change
    }

    // ===== Tree building manually (simulating what buildTreeModel does) =====

    @Test
    fun manualTreeRoundTrip() {
        val root = Node("-1", null, -1)
        val a = Node("a", root, 0)
        val b = Node("b", root, 0)
        val c = Node("c", a, 1)
        a.children.add(c)
        root.children.add(a)
        root.children.add(b)

        assertEquals(2, root.children.size)
        assertEquals("a", root.children[0].uuid)
        assertEquals("b", root.children[1].uuid)
        assertEquals(1, root.children[0].children.size)
        assertEquals("c", root.children[0].children[0].uuid)
    }

    @Test
    fun manualTreeRemoveNode() {
        val root = Node("-1", null, -1)
        val a = Node("a", root, 0)
        val b = Node("b", root, 0)
        val child = Node("child", a, 1)
        a.children.add(child)
        root.children.add(a)
        root.children.add(b)

        // Simulate removing 'a' and promoting children
        val parent = a.parent!!
        parent.children.remove(a)
        for (c in a.children) {
            c.parent = parent
            parent.children.add(c)
        }

        assertEquals(2, root.children.size)
        assertEquals("b", root.children[0].uuid)
        assertEquals("child", root.children[1].uuid)
        assertEquals(root, root.children[1].parent)
    }

    @Test
    fun manualTreeIndentNode() {
        val root = Node("-1", null, -1)
        val a = Node("a", root, 0)
        val b = Node("b", root, 0)
        root.children.add(a)
        root.children.add(b)

        // Simulate indent: move b under a
        val siblings = root.children
        val index = siblings.indexOf(b)
        val newParent = siblings[index - 1]
        siblings.removeAt(index)
        b.parent = newParent
        newParent.children.add(b)
        b.indent = newParent.indent + 1

        assertEquals(1, root.children.size)
        assertEquals("a", root.children[0].uuid)
        assertEquals(1, a.children.size)
        assertEquals("b", a.children[0].uuid)
        assertEquals(1, b.indent)
    }

    @Test
    fun manualTreeDeindentNode() {
        val root = Node("-1", null, -1)
        val a = Node("a", root, 0)
        val b = Node("b", a, 1)
        a.children.add(b)
        root.children.add(a)

        // Simulate deindent: move b from under a to root level
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
        assertEquals(0, a.children.size)
    }

    @Test
    fun manualTreeMoveNode() {
        val root = Node("-1", null, -1)
        val a = Node("a", root, 0)
        val b = Node("b", root, 0)
        val c = Node("c", root, 0)
        root.children.add(a)
        root.children.add(b)
        root.children.add(c)

        // Move c before a
        val oldParent = c.parent!!
        oldParent.children.remove(c)
        c.parent = root
        root.children.add(0, c)

        assertEquals(3, root.children.size)
        assertEquals("c", root.children[0].uuid)
        assertEquals("a", root.children[1].uuid)
        assertEquals("b", root.children[2].uuid)
    }

    @Test
    fun isDescendantManual() {
        val root = Node("-1", null, -1)
        val a = Node("a", root, 0)
        val b = Node("b", a, 1)
        val c = Node("c", b, 2)
        b.children.add(c)
        a.children.add(b)
        root.children.add(a)

        // c is descendant of a
        var curr: Node? = c
        var isDesc = false
        while (curr !== root) {
            if (curr === a) {
                isDesc = true
                break
            }
            curr = curr!!.parent
        }
        assertTrue(isDesc)

        // a is NOT descendant of c
        curr = a
        isDesc = false
        while (curr !== root) {
            if (curr === c) {
                isDesc = true
                break
            }
            curr = curr!!.parent
        }
        assertFalse(isDesc)
    }

    // ===== Ordered IDs simulation =====

    @Test
    fun orderedIdHelperPreOrder() {
        val root = Node("-1", null, -1)
        val a = Node("a", root, 0)
        val b = Node("b", root, 0)
        val a1 = Node("a1", a, 1)
        a.children.add(a1)
        root.children.add(a)
        root.children.add(b)

        val ids = mutableListOf<String>()
        collectIds(root, ids)

        assertEquals(listOf("a", "a1", "b"), ids)
    }

    private fun collectIds(node: Node, ids: MutableList<String>) {
        if (node.uuid != "-1") {
            ids.add(node.uuid)
        }
        for (child in node.children) {
            collectIds(child, ids)
        }
    }

    @Test
    fun applyToDescendantsManual() {
        val root = Node("-1", null, -1)
        val a = Node("a", root, 0)
        val a1 = Node("a1", a, 1)
        val a2 = Node("a2", a, 1)
        a.children.add(a1)
        a.children.add(a2)
        root.children.add(a)

        val visited = mutableListOf<String>()
        visitDescendants(a) { visited.add(it.uuid) }

        assertEquals(2, visited.size)
        assertTrue(visited.contains("a1"))
        assertTrue(visited.contains("a2"))
    }

    private fun visitDescendants(node: Node, visitor: (Node) -> Unit) {
        for (child in node.children) {
            visitor(child)
            visitDescendants(child, visitor)
        }
    }

    @Test
    fun applyToDescendantsDeepManual() {
        val root = Node("-1", null, -1)
        val a = Node("a", root, 0)
        val b = Node("b", a, 1)
        val c = Node("c", b, 2)
        b.children.add(c)
        a.children.add(b)
        root.children.add(a)

        val visited = mutableListOf<String>()
        visitDescendants(a) { visited.add(it.uuid) }

        assertEquals(listOf("b", "c"), visited)
    }
}
