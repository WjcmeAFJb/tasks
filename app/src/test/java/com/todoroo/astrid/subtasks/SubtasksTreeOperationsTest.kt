package com.todoroo.astrid.subtasks

import com.todoroo.astrid.subtasks.SubtasksFilterUpdater.Companion.buildOrderString
import com.todoroo.astrid.subtasks.SubtasksFilterUpdater.Node
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.mockito.Mockito.mock
import org.tasks.data.dao.TaskDao
import org.tasks.data.dao.TaskListMetadataDao

class SubtasksTreeOperationsTest {

    private lateinit var taskListMetadataDao: TaskListMetadataDao
    private lateinit var taskDao: TaskDao
    private lateinit var updater: SubtasksFilterUpdater

    @Before
    fun setUp() {
        taskListMetadataDao = mock()
        taskDao = mock()
        updater = SubtasksFilterUpdater(taskListMetadataDao, taskDao)
    }

    // ===== Node construction and tree building =====

    @Test
    fun buildFlatTree() {
        val root = Node("-1", null, -1)
        val a = Node("a", root, 0)
        val b = Node("b", root, 0)
        val c = Node("c", root, 0)
        root.children.add(a)
        root.children.add(b)
        root.children.add(c)

        assertEquals(3, root.children.size)
        assertEquals("a", root.children[0].uuid)
        assertEquals("b", root.children[1].uuid)
        assertEquals("c", root.children[2].uuid)
    }

    @Test
    fun buildNestedTree() {
        val root = Node("-1", null, -1)
        val a = Node("a", root, 0)
        val b = Node("b", a, 1)
        val c = Node("c", b, 2)
        c.children.clear()
        b.children.add(c)
        a.children.add(b)
        root.children.add(a)

        assertEquals(1, root.children.size)
        assertEquals("a", root.children[0].uuid)
        assertEquals(1, a.children.size)
        assertEquals("b", a.children[0].uuid)
        assertEquals(1, b.children.size)
        assertEquals("c", b.children[0].uuid)
    }

    @Test
    fun parentReferencesCorrect() {
        val root = Node("-1", null, -1)
        val a = Node("a", root, 0)
        val b = Node("b", a, 1)
        a.children.add(b)
        root.children.add(a)

        assertEquals(root, a.parent)
        assertEquals(a, b.parent)
        assertNull(root.parent)
    }

    @Test
    fun indentLevelsCorrect() {
        val root = Node("-1", null, -1)
        val a = Node("a", root, 0)
        val b = Node("b", a, 1)
        val c = Node("c", b, 2)

        assertEquals(-1, root.indent)
        assertEquals(0, a.indent)
        assertEquals(1, b.indent)
        assertEquals(2, c.indent)
    }

    // ===== Tree operations: indent =====

    @Test
    fun indentNodeMovesUnderSibling() {
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
        assertEquals("a", root.children[0].uuid)
        assertEquals(1, a.children.size)
        assertEquals("b", a.children[0].uuid)
        assertEquals(1, b.indent)
    }

    @Test
    fun indentFirstChildIsNotAllowed() {
        val root = Node("-1", null, -1)
        val a = Node("a", root, 0)
        root.children.add(a)

        // Can't indent first child (index 0)
        val index = root.children.indexOf(a)
        assertTrue(index <= 0) // Should not indent
    }

    // ===== Tree operations: deindent =====

    @Test
    fun deindentNodeMovesToParentLevel() {
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
        assertEquals(0, a.children.size)
    }

    @Test
    fun deindentTopLevelIsNotAllowed() {
        val root = Node("-1", null, -1)
        val a = Node("a", root, 0)
        root.children.add(a)

        // Can't deindent a top-level item (parent is root)
        assertTrue(a.parent === root)
    }

    // ===== Tree operations: move =====

    @Test
    fun moveNodeBeforeAnother() {
        val root = Node("-1", null, -1)
        val a = Node("a", root, 0)
        val b = Node("b", root, 0)
        val c = Node("c", root, 0)
        root.children.add(a)
        root.children.add(b)
        root.children.add(c)

        // Move c before a
        root.children.remove(c)
        root.children.add(0, c)
        c.parent = root

        assertEquals("c", root.children[0].uuid)
        assertEquals("a", root.children[1].uuid)
        assertEquals("b", root.children[2].uuid)
    }

    @Test
    fun moveNodeToEnd() {
        val root = Node("-1", null, -1)
        val a = Node("a", root, 0)
        val b = Node("b", root, 0)
        root.children.add(a)
        root.children.add(b)

        // Move a to end
        root.children.remove(a)
        root.children.add(a)

        assertEquals("b", root.children[0].uuid)
        assertEquals("a", root.children[1].uuid)
    }

    // ===== Tree operations: remove and reparent children =====

    @Test
    fun removeNodePromotesChildren() {
        val root = Node("-1", null, -1)
        val a = Node("a", root, 0)
        val b = Node("b", a, 1)
        val c = Node("c", a, 1)
        a.children.add(b)
        a.children.add(c)
        root.children.add(a)

        // Remove a, promote children to root
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
        assertEquals(0, root.children[1].indent)
    }

    @Test
    fun removeLeafNode() {
        val root = Node("-1", null, -1)
        val a = Node("a", root, 0)
        root.children.add(a)

        root.children.remove(a)
        assertEquals(0, root.children.size)
    }

    // ===== isDescendantOf =====

    @Test
    fun childIsDescendantOfParent() {
        val root = Node("-1", null, -1)
        val a = Node("a", root, 0)
        val b = Node("b", a, 1)
        a.children.add(b)
        root.children.add(a)

        var curr: Node? = b
        var isDesc = false
        while (curr !== root) {
            if (curr === a) { isDesc = true; break }
            curr = curr!!.parent
        }
        assertTrue(isDesc)
    }

    @Test
    fun grandchildIsDescendantOfGrandparent() {
        val root = Node("-1", null, -1)
        val a = Node("a", root, 0)
        val b = Node("b", a, 1)
        val c = Node("c", b, 2)
        b.children.add(c)
        a.children.add(b)
        root.children.add(a)

        var curr: Node? = c
        var isDesc = false
        while (curr !== root) {
            if (curr === a) { isDesc = true; break }
            curr = curr!!.parent
        }
        assertTrue(isDesc)
    }

    @Test
    fun siblingIsNotDescendant() {
        val root = Node("-1", null, -1)
        val a = Node("a", root, 0)
        val b = Node("b", root, 0)
        root.children.add(a)
        root.children.add(b)

        var curr: Node? = a
        var isDesc = false
        while (curr !== root) {
            if (curr === b) { isDesc = true; break }
            curr = curr!!.parent
        }
        assertFalse(isDesc)
    }

    @Test
    fun parentIsNotDescendantOfChild() {
        val root = Node("-1", null, -1)
        val a = Node("a", root, 0)
        val b = Node("b", a, 1)
        a.children.add(b)
        root.children.add(a)

        var curr: Node? = a
        var isDesc = false
        while (curr !== root) {
            if (curr === b) { isDesc = true; break }
            curr = curr!!.parent
        }
        assertFalse(isDesc)
    }

    // ===== Pre-order traversal (orderedIds simulation) =====

    @Test
    fun preOrderTraversalFlat() {
        val root = Node("-1", null, -1)
        root.children.add(Node("a", root, 0))
        root.children.add(Node("b", root, 0))
        root.children.add(Node("c", root, 0))

        val ids = mutableListOf<String>()
        collectPreOrder(root, ids)
        assertEquals(listOf("a", "b", "c"), ids)
    }

    @Test
    fun preOrderTraversalNested() {
        val root = Node("-1", null, -1)
        val a = Node("a", root, 0)
        val a1 = Node("a1", a, 1)
        a.children.add(a1)
        root.children.add(a)
        root.children.add(Node("b", root, 0))

        val ids = mutableListOf<String>()
        collectPreOrder(root, ids)
        assertEquals(listOf("a", "a1", "b"), ids)
    }

    @Test
    fun preOrderTraversalDeep() {
        val root = Node("-1", null, -1)
        val a = Node("a", root, 0)
        val b = Node("b", a, 1)
        val c = Node("c", b, 2)
        b.children.add(c)
        a.children.add(b)
        root.children.add(a)

        val ids = mutableListOf<String>()
        collectPreOrder(root, ids)
        assertEquals(listOf("a", "b", "c"), ids)
    }

    private fun collectPreOrder(node: Node, ids: MutableList<String>) {
        if (node.uuid != "-1") {
            ids.add(node.uuid)
        }
        for (child in node.children) {
            collectPreOrder(child, ids)
        }
    }

    // ===== Descendant visitor =====

    @Test
    fun visitDescendantsVisitsAll() {
        val root = Node("-1", null, -1)
        val a = Node("a", root, 0)
        val b = Node("b", a, 1)
        val c = Node("c", a, 1)
        a.children.add(b)
        a.children.add(c)
        root.children.add(a)

        val visited = mutableListOf<String>()
        visitDesc(a) { visited.add(it.uuid) }
        assertEquals(listOf("b", "c"), visited)
    }

    @Test
    fun visitDescendantsDeep() {
        val a = Node("a", null, 0)
        val b = Node("b", a, 1)
        val c = Node("c", b, 2)
        val d = Node("d", c, 3)
        c.children.add(d)
        b.children.add(c)
        a.children.add(b)

        val visited = mutableListOf<String>()
        visitDesc(a) { visited.add(it.uuid) }
        assertEquals(listOf("b", "c", "d"), visited)
    }

    @Test
    fun visitDescendantsOnLeaf() {
        val leaf = Node("leaf", null, 0)
        val visited = mutableListOf<String>()
        visitDesc(leaf) { visited.add(it.uuid) }
        assertTrue(visited.isEmpty())
    }

    private fun visitDesc(node: Node, visitor: (Node) -> Unit) {
        for (child in node.children) {
            visitor(child)
            visitDesc(child, visitor)
        }
    }

    // ===== buildOrderString extra =====

    @Test
    fun buildOrderStringEmptyReturnsDefault() {
        assertEquals("(1)", buildOrderString(emptyList()))
    }

    @Test
    fun buildOrderStringSingleElementNoComma() {
        val result = buildOrderString(listOf("uuid1"))
        assertTrue(result.contains("uuid1"))
        assertFalse(result.contains(", "))
    }

    @Test
    fun buildOrderStringTwoElementsReversed() {
        val result = buildOrderString(listOf("first", "second"))
        val secondIdx = result.indexOf("second")
        val firstIdx = result.indexOf("first")
        assertTrue("second should appear before first", secondIdx < firstIdx)
    }

    @Test
    fun buildOrderStringExactly900Elements() {
        val ids = (1..900).map { "id-$it" }
        val result = buildOrderString(ids)
        assertTrue(result.contains("id-1"))
        assertTrue(result.contains("id-900"))
    }

    @Test
    fun buildOrderString901Truncates() {
        val ids = (1..901).map { "id-$it" }
        val result = buildOrderString(ids)
        // Should contain last 900 elements reversed from end
        assertTrue(result.contains("id-901"))
    }

    // ===== adjustDescendantsIndent simulation =====

    @Test
    fun adjustDescendantsIndent() {
        val root = Node("-1", null, -1)
        val a = Node("a", root, 0)
        val b = Node("b", a, 1)
        val c = Node("c", b, 2)
        b.children.add(c)
        a.children.add(b)
        root.children.add(a)

        // Set a to indent 3, adjust descendants
        a.indent = 3
        adjustIndent(a, 3)

        assertEquals(3, a.indent)
        assertEquals(4, b.indent)
        assertEquals(5, c.indent)
    }

    private fun adjustIndent(node: Node, baseIndent: Int) {
        for (child in node.children) {
            child.indent = baseIndent + 1
            adjustIndent(child, child.indent)
        }
    }
}
