package org.tasks.filters

import com.todoroo.astrid.api.BooleanCriterion
import com.todoroo.astrid.api.MultipleSelectCriterion
import com.todoroo.astrid.api.TextInputCriterion
import com.todoroo.astrid.core.CriterionInstance
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class CriterionInstanceTest {

    // --- titleFromCriterion ---

    @Test
    fun titleFromMultipleSelectCriterionWithSelectedIndex() {
        val criterion = MultipleSelectCriterion(
            "test",
            "Priority: ?",
            "SELECT 1",
            null,
            arrayOf("High", "Medium", "Low"),
            arrayOf("0", "1", "2"),
            "Priority"
        )
        val instance = CriterionInstance().apply {
            this.criterion = criterion
            selectedIndex = 1
        }

        assertEquals("Priority: Medium", instance.titleFromCriterion)
    }

    @Test
    fun titleFromMultipleSelectCriterionWithNoEntryTitles() {
        val criterion = MultipleSelectCriterion(
            "test",
            "All active",
            null,
            null,
            null,
            null,
            null
        )
        val instance = CriterionInstance().apply {
            this.criterion = criterion
            selectedIndex = -1
        }

        assertEquals("All active", instance.titleFromCriterion)
    }

    @Test
    fun titleFromMultipleSelectCriterionWithNegativeIndex() {
        val criterion = MultipleSelectCriterion(
            "test",
            "Due: ?",
            "SELECT 1",
            null,
            arrayOf("Today", "Tomorrow"),
            arrayOf("0", "1"),
            "Due"
        )
        val instance = CriterionInstance().apply {
            this.criterion = criterion
            selectedIndex = -1
        }

        assertEquals("Due: ?", instance.titleFromCriterion)
    }

    @Test
    fun titleFromMultipleSelectCriterionWithOutOfBoundsIndex() {
        val criterion = MultipleSelectCriterion(
            "test",
            "Due: ?",
            "SELECT 1",
            null,
            arrayOf("Today"),
            arrayOf("0"),
            "Due"
        )
        val instance = CriterionInstance().apply {
            this.criterion = criterion
            selectedIndex = 5
        }

        assertEquals("Due: ?", instance.titleFromCriterion)
    }

    @Test
    fun titleFromTextInputCriterionWithSelectedText() {
        val criterion = TextInputCriterion(
            "title",
            "Title contains ?",
            "SELECT 1",
            "Enter title",
            "",
            "Title"
        )
        val instance = CriterionInstance().apply {
            this.criterion = criterion
            selectedText = "groceries"
        }

        assertEquals("Title contains groceries", instance.titleFromCriterion)
    }

    @Test
    fun titleFromTextInputCriterionWithNullText() {
        val criterion = TextInputCriterion(
            "title",
            "Title contains ?",
            "SELECT 1",
            "Enter title",
            "",
            "Title"
        )
        val instance = CriterionInstance().apply {
            this.criterion = criterion
            selectedText = null
        }

        assertEquals("Title contains ?", instance.titleFromCriterion)
    }

    @Test
    fun titleFromBooleanCriterion() {
        val criterion = BooleanCriterion("recur", "Repeats", "SELECT 1")
        val instance = CriterionInstance().apply {
            this.criterion = criterion
        }

        assertEquals("Repeats", instance.titleFromCriterion)
    }

    // --- valueFromCriterion ---

    @Test
    fun valueFromMultipleSelectCriterionWithValidIndex() {
        val criterion = MultipleSelectCriterion(
            "importance",
            "Priority: ?",
            "SELECT 1",
            null,
            arrayOf("High", "Medium", "Low"),
            arrayOf("0", "1", "2"),
            "Priority"
        )
        val instance = CriterionInstance().apply {
            this.criterion = criterion
            selectedIndex = 2
        }

        assertEquals("2", instance.valueFromCriterion)
    }

    @Test
    fun valueFromMultipleSelectCriterionWithNullEntryValues() {
        val criterion = MultipleSelectCriterion(
            "test",
            "All active",
            null,
            null,
            null,
            null,
            null
        )
        val instance = CriterionInstance().apply {
            this.criterion = criterion
            selectedIndex = 0
        }

        assertEquals("All active", instance.valueFromCriterion)
    }

    @Test
    fun valueFromMultipleSelectCriterionWithNegativeIndex() {
        val criterion = MultipleSelectCriterion(
            "test",
            "text",
            "sql",
            null,
            arrayOf("A"),
            arrayOf("a"),
            "name"
        )
        val instance = CriterionInstance().apply {
            this.criterion = criterion
            selectedIndex = -1
        }

        assertEquals("text", instance.valueFromCriterion)
    }

    @Test
    fun valueFromTextInputCriterion() {
        val criterion = TextInputCriterion(
            "title",
            "Title contains ?",
            "SELECT 1",
            "Enter",
            "",
            "Title"
        )
        val instance = CriterionInstance().apply {
            this.criterion = criterion
            selectedText = "hello"
        }

        assertEquals("hello", instance.valueFromCriterion)
    }

    @Test
    fun valueFromBooleanCriterion() {
        val criterion = BooleanCriterion("recur", "Repeats", "SELECT 1")
        val instance = CriterionInstance().apply {
            this.criterion = criterion
        }

        assertEquals("Repeats", instance.valueFromCriterion)
    }

    @Test
    fun valueFromCriterionReturnsNullForUniverse() {
        val criterion = MultipleSelectCriterion(
            "active",
            "All",
            null,
            null,
            null,
            null,
            null
        )
        val instance = CriterionInstance().apply {
            this.criterion = criterion
            type = CriterionInstance.TYPE_UNIVERSE
        }

        assertNull(instance.valueFromCriterion)
    }

    // --- serialize / deserialize ---

    @Test
    fun serializeSingleBooleanCriterion() {
        val criterion = BooleanCriterion("recur", "Repeats", "SELECT _id FROM tasks")
        val instance = CriterionInstance().apply {
            this.criterion = criterion
            type = CriterionInstance.TYPE_INTERSECT
        }

        val serialized = CriterionInstance.serialize(listOf(instance))

        // Format: identifier|value|text|type|sql
        val parts = serialized.split("|")
        assertEquals(5, parts.size)
        assertEquals("recur", parts[0])
        assertEquals("Repeats", parts[1])  // value from BooleanCriterion
        assertEquals("Repeats", parts[2])  // text
        assertEquals("2", parts[3])        // TYPE_INTERSECT
        assertEquals("SELECT _id FROM tasks", parts[4])
    }

    @Test
    fun serializeMultipleCriteria() {
        val c1 = BooleanCriterion("recur", "Repeats", "sql1")
        val c2 = BooleanCriterion("completed", "Completed", "sql2")
        val i1 = CriterionInstance().apply {
            criterion = c1
            type = CriterionInstance.TYPE_INTERSECT
        }
        val i2 = CriterionInstance().apply {
            criterion = c2
            type = CriterionInstance.TYPE_ADD
        }

        val serialized = CriterionInstance.serialize(listOf(i1, i2))

        val lines = serialized.split("\n")
        assertEquals(2, lines.size)
    }

    @Test
    fun serializeEmptyList() {
        val serialized = CriterionInstance.serialize(emptyList())

        assertEquals("", serialized)
    }

    @Test
    fun serializeEscapesPipeInValues() {
        val criterion = TextInputCriterion(
            "title",
            "Title: ?",
            "SELECT 1",
            "Enter",
            "",
            "Title"
        )
        val instance = CriterionInstance().apply {
            this.criterion = criterion
            selectedText = "has|pipe"
            type = CriterionInstance.TYPE_INTERSECT
        }

        val serialized = CriterionInstance.serialize(listOf(instance))

        // The pipe in "has|pipe" should be escaped to "has!PIPE!pipe"
        assertTrue(serialized.contains("has!PIPE!pipe"))
    }

    @Test
    fun serializeTextInputCriterionSelectedText() {
        val criterion = TextInputCriterion(
            "title",
            "Contains ?",
            "SELECT 1",
            "Enter",
            "",
            "Title"
        )
        val instance = CriterionInstance().apply {
            this.criterion = criterion
            selectedText = "search term"
            type = CriterionInstance.TYPE_INTERSECT
        }

        val serialized = CriterionInstance.serialize(listOf(instance))
        val parts = serialized.split("|")
        assertEquals("search term", parts[1])
    }

    @Test
    fun serializeMultipleSelectCriterionSelectedValue() {
        val criterion = MultipleSelectCriterion(
            "importance",
            "Priority: ?",
            "SELECT 1",
            null,
            arrayOf("High", "Medium", "Low"),
            arrayOf("0", "1", "2"),
            "Priority"
        )
        val instance = CriterionInstance().apply {
            this.criterion = criterion
            selectedIndex = 0
            type = CriterionInstance.TYPE_INTERSECT
        }

        val serialized = CriterionInstance.serialize(listOf(instance))
        val parts = serialized.split("|")
        assertEquals("0", parts[1])  // entryValues[0]
    }

    // --- copy constructor ---

    @Test
    fun copyConstructorCopiesAllFields() {
        val criterion = BooleanCriterion("test", "Test", "sql")
        val original = CriterionInstance().apply {
            this.criterion = criterion
            selectedIndex = 3
            selectedText = "text"
            type = CriterionInstance.TYPE_SUBTRACT
            end = 10
            start = 5
            max = 20
        }

        val copy = CriterionInstance(original)

        assertEquals(original.id, copy.id)
        assertEquals(original.selectedIndex, copy.selectedIndex)
        assertEquals(original.selectedText, copy.selectedText)
        assertEquals(original.type, copy.type)
        assertEquals(original.end, copy.end)
        assertEquals(original.start, copy.start)
        assertEquals(original.max, copy.max)
    }

    // --- type constants ---

    @Test
    fun typeConstants() {
        assertEquals(0, CriterionInstance.TYPE_ADD)
        assertEquals(1, CriterionInstance.TYPE_SUBTRACT)
        assertEquals(2, CriterionInstance.TYPE_INTERSECT)
        assertEquals(3, CriterionInstance.TYPE_UNIVERSE)
    }

    private fun assertTrue(condition: Boolean) {
        org.junit.Assert.assertTrue(condition)
    }
}
