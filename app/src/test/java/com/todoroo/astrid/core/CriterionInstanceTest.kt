package com.todoroo.astrid.core

import com.todoroo.astrid.api.BooleanCriterion
import com.todoroo.astrid.api.MultipleSelectCriterion
import com.todoroo.astrid.api.TextInputCriterion
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class CriterionInstanceTest {

    // --- titleFromCriterion with MultipleSelectCriterion ---

    @Test
    fun titleFromMultipleSelectWithValidIndex() {
        val criterion = MultipleSelectCriterion(
            "id", "Due: ?", "sql", null,
            arrayOf("Today", "Tomorrow"),
            arrayOf("0", "1"),
            "Due Date"
        )
        val instance = CriterionInstance()
        instance.criterion = criterion
        instance.selectedIndex = 0
        assertEquals("Due: Today", instance.titleFromCriterion)
    }

    @Test
    fun titleFromMultipleSelectWithSecondIndex() {
        val criterion = MultipleSelectCriterion(
            "id", "Due: ?", "sql", null,
            arrayOf("Today", "Tomorrow"),
            arrayOf("0", "1"),
            "Due Date"
        )
        val instance = CriterionInstance()
        instance.criterion = criterion
        instance.selectedIndex = 1
        assertEquals("Due: Tomorrow", instance.titleFromCriterion)
    }

    @Test
    fun titleFromMultipleSelectWithNegativeIndex() {
        val criterion = MultipleSelectCriterion(
            "id", "Due: ?", "sql", null,
            arrayOf("Today"),
            arrayOf("0"),
            "Due Date"
        )
        val instance = CriterionInstance()
        instance.criterion = criterion
        instance.selectedIndex = -1
        assertEquals("Due: ?", instance.titleFromCriterion)
    }

    @Test
    fun titleFromMultipleSelectWithIndexOutOfBounds() {
        val criterion = MultipleSelectCriterion(
            "id", "Due: ?", "sql", null,
            arrayOf("Today"),
            arrayOf("0"),
            "Due Date"
        )
        val instance = CriterionInstance()
        instance.criterion = criterion
        instance.selectedIndex = 5
        assertEquals("Due: ?", instance.titleFromCriterion)
    }

    @Test
    fun titleFromMultipleSelectWithNullTitles() {
        val criterion = MultipleSelectCriterion(
            "id", "Due: ?", "sql", null,
            null,
            null,
            "Due Date"
        )
        val instance = CriterionInstance()
        instance.criterion = criterion
        instance.selectedIndex = 0
        assertEquals("Due: ?", instance.titleFromCriterion)
    }

    // --- titleFromCriterion with TextInputCriterion ---

    @Test
    fun titleFromTextInputWithSelectedText() {
        val criterion = TextInputCriterion(
            "id", "Contains: ?", "sql", "Enter text", "hint", "Text Search"
        )
        val instance = CriterionInstance()
        instance.criterion = criterion
        instance.selectedText = "hello"
        assertEquals("Contains: hello", instance.titleFromCriterion)
    }

    @Test
    fun titleFromTextInputWithNullSelectedText() {
        val criterion = TextInputCriterion(
            "id", "Contains: ?", "sql", "Enter text", "hint", "Text Search"
        )
        val instance = CriterionInstance()
        instance.criterion = criterion
        instance.selectedText = null
        assertEquals("Contains: ?", instance.titleFromCriterion)
    }

    // --- titleFromCriterion with BooleanCriterion ---

    @Test
    fun titleFromBooleanCriterion() {
        val criterion = BooleanCriterion("id", "Has Attachments", "sql")
        val instance = CriterionInstance()
        instance.criterion = criterion
        assertEquals("Has Attachments", instance.titleFromCriterion)
    }

    // --- valueFromCriterion ---

    @Test
    fun valueFromMultipleSelectWithValidIndex() {
        val criterion = MultipleSelectCriterion(
            "id", "Due: ?", "sql", null,
            arrayOf("Today", "Tomorrow"),
            arrayOf("0", "1"),
            "Due Date"
        )
        val instance = CriterionInstance()
        instance.criterion = criterion
        instance.selectedIndex = 1
        instance.type = CriterionInstance.TYPE_INTERSECT
        assertEquals("1", instance.valueFromCriterion)
    }

    @Test
    fun valueFromMultipleSelectWithNegativeIndex() {
        val criterion = MultipleSelectCriterion(
            "id", "Due: ?", "sql", null,
            arrayOf("Today"),
            arrayOf("0"),
            "Due Date"
        )
        val instance = CriterionInstance()
        instance.criterion = criterion
        instance.selectedIndex = -1
        instance.type = CriterionInstance.TYPE_INTERSECT
        assertEquals("Due: ?", instance.valueFromCriterion)
    }

    @Test
    fun valueFromMultipleSelectWithIndexOutOfBounds() {
        val criterion = MultipleSelectCriterion(
            "id", "Due: ?", "sql", null,
            arrayOf("Today"),
            arrayOf("0"),
            "Due Date"
        )
        val instance = CriterionInstance()
        instance.criterion = criterion
        instance.selectedIndex = 10
        instance.type = CriterionInstance.TYPE_ADD
        assertEquals("Due: ?", instance.valueFromCriterion)
    }

    @Test
    fun valueFromMultipleSelectWithNullValues() {
        val criterion = MultipleSelectCriterion(
            "id", "Due: ?", "sql", null,
            arrayOf("Today"),
            null,
            "Due Date"
        )
        val instance = CriterionInstance()
        instance.criterion = criterion
        instance.selectedIndex = 0
        instance.type = CriterionInstance.TYPE_INTERSECT
        assertEquals("Due: ?", instance.valueFromCriterion)
    }

    @Test
    fun valueFromCriterionReturnsNullForUniverse() {
        val criterion = BooleanCriterion("id", "All", "sql")
        val instance = CriterionInstance()
        instance.criterion = criterion
        instance.type = CriterionInstance.TYPE_UNIVERSE
        assertNull(instance.valueFromCriterion)
    }

    @Test
    fun valueFromTextInputCriterion() {
        val criterion = TextInputCriterion(
            "id", "Contains: ?", "sql", "Enter text", "hint", "Text Search"
        )
        val instance = CriterionInstance()
        instance.criterion = criterion
        instance.selectedText = "hello"
        instance.type = CriterionInstance.TYPE_ADD
        assertEquals("hello", instance.valueFromCriterion)
    }

    @Test
    fun valueFromTextInputCriterionWithNullText() {
        val criterion = TextInputCriterion(
            "id", "Contains: ?", "sql", "Enter text", "hint", "Text Search"
        )
        val instance = CriterionInstance()
        instance.criterion = criterion
        instance.selectedText = null
        instance.type = CriterionInstance.TYPE_ADD
        assertNull(instance.valueFromCriterion)
    }

    @Test
    fun valueFromBooleanCriterion() {
        val criterion = BooleanCriterion("id", "Has Attachments", "sql")
        val instance = CriterionInstance()
        instance.criterion = criterion
        instance.type = CriterionInstance.TYPE_INTERSECT
        assertEquals("Has Attachments", instance.valueFromCriterion)
    }

    // --- serialize ---

    @Test
    fun serializeEmptyList() {
        assertEquals("", CriterionInstance.serialize(emptyList()))
    }

    @Test
    fun serializeSingleItem() {
        val criterion = BooleanCriterion("bool-id", "Has Tags", "sql-query")
        val instance = CriterionInstance()
        instance.criterion = criterion
        instance.type = CriterionInstance.TYPE_INTERSECT
        val result = CriterionInstance.serialize(listOf(instance))
        assertTrue(result.contains("bool-id"))
        assertTrue(result.contains("Has Tags"))
        assertTrue(result.contains("sql-query"))
    }

    @Test
    fun serializeMultipleItems() {
        val criterion1 = BooleanCriterion("id1", "Criterion 1", "sql1")
        val criterion2 = BooleanCriterion("id2", "Criterion 2", "sql2")
        val i1 = CriterionInstance().apply {
            criterion = criterion1
            type = CriterionInstance.TYPE_ADD
        }
        val i2 = CriterionInstance().apply {
            criterion = criterion2
            type = CriterionInstance.TYPE_SUBTRACT
        }
        val result = CriterionInstance.serialize(listOf(i1, i2))
        assertTrue(result.contains("\n"))
        assertTrue(result.contains("id1"))
        assertTrue(result.contains("id2"))
    }

    @Test
    fun serializeEscapesPipeCharacters() {
        val criterion = BooleanCriterion("id|with|pipes", "Name|With|Pipes", "sql|pipe")
        val instance = CriterionInstance()
        instance.criterion = criterion
        instance.type = CriterionInstance.TYPE_ADD
        val result = CriterionInstance.serialize(listOf(instance))
        assertTrue(result.contains("!PIPE!"))
    }

    @Test
    fun serializeWithNullSql() {
        val criterion = BooleanCriterion("id", "Name", "sql")
        criterion.sql = null
        val instance = CriterionInstance()
        instance.criterion = criterion
        instance.type = CriterionInstance.TYPE_ADD
        val result = CriterionInstance.serialize(listOf(instance))
        assertFalse(result.isEmpty())
    }

    // --- copy constructor ---

    @Test
    fun copyConstructorCopiesAllFields() {
        val criterion = BooleanCriterion("id", "Name", "sql")
        val original = CriterionInstance()
        original.criterion = criterion
        original.selectedIndex = 3
        original.selectedText = "text"
        original.type = CriterionInstance.TYPE_SUBTRACT
        original.end = 10
        original.start = 5
        original.max = 100

        val copy = CriterionInstance(original)

        assertEquals(original.id, copy.id)
        assertEquals(original.criterion, copy.criterion)
        assertEquals(original.selectedIndex, copy.selectedIndex)
        assertEquals(original.selectedText, copy.selectedText)
        assertEquals(original.type, copy.type)
        assertEquals(original.end, copy.end)
        assertEquals(original.start, copy.start)
        assertEquals(original.max, copy.max)
    }

    // --- equals and hashCode ---

    @Test
    fun equalsReturnsTrueForSameInstance() {
        val criterion = BooleanCriterion("id", "Name", "sql")
        val instance = CriterionInstance()
        instance.criterion = criterion
        assertTrue(instance == instance)
    }

    @Test
    fun equalsReturnsTrueForCopy() {
        val criterion = BooleanCriterion("id", "Name", "sql")
        val original = CriterionInstance()
        original.criterion = criterion
        original.selectedIndex = 2
        original.selectedText = "text"
        original.type = CriterionInstance.TYPE_ADD
        val copy = CriterionInstance(original)
        assertEquals(original, copy)
    }

    @Test
    fun equalsReturnsFalseForDifferentType() {
        val criterion = BooleanCriterion("id", "Name", "sql")
        val a = CriterionInstance()
        a.criterion = criterion
        a.type = CriterionInstance.TYPE_ADD
        val b = CriterionInstance(a)
        b.type = CriterionInstance.TYPE_SUBTRACT
        assertNotEquals(a, b)
    }

    @Test
    fun equalsReturnsFalseForDifferentSelectedIndex() {
        val criterion = BooleanCriterion("id", "Name", "sql")
        val a = CriterionInstance()
        a.criterion = criterion
        a.selectedIndex = 0
        val b = CriterionInstance(a)
        b.selectedIndex = 1
        assertNotEquals(a, b)
    }

    @Test
    fun equalsReturnsFalseForDifferentSelectedText() {
        val criterion = BooleanCriterion("id", "Name", "sql")
        val a = CriterionInstance()
        a.criterion = criterion
        a.selectedText = "text1"
        val b = CriterionInstance(a)
        b.selectedText = "text2"
        assertNotEquals(a, b)
    }

    @Test
    fun equalsReturnsFalseForNull() {
        val criterion = BooleanCriterion("id", "Name", "sql")
        val a = CriterionInstance()
        a.criterion = criterion
        assertFalse(a.equals(null))
    }

    @Test
    fun equalsReturnsFalseForDifferentClass() {
        val criterion = BooleanCriterion("id", "Name", "sql")
        val a = CriterionInstance()
        a.criterion = criterion
        assertFalse(a.equals("string"))
    }

    @Test
    fun equalsReturnsFalseForDifferentEnd() {
        val criterion = BooleanCriterion("id", "Name", "sql")
        val a = CriterionInstance()
        a.criterion = criterion
        a.end = 10
        val b = CriterionInstance(a)
        b.end = 20
        assertNotEquals(a, b)
    }

    @Test
    fun equalsReturnsFalseForDifferentStart() {
        val criterion = BooleanCriterion("id", "Name", "sql")
        val a = CriterionInstance()
        a.criterion = criterion
        a.start = 1
        val b = CriterionInstance(a)
        b.start = 2
        assertNotEquals(a, b)
    }

    @Test
    fun equalsReturnsFalseForDifferentMax() {
        val criterion = BooleanCriterion("id", "Name", "sql")
        val a = CriterionInstance()
        a.criterion = criterion
        a.max = 50
        val b = CriterionInstance(a)
        b.max = 100
        assertNotEquals(a, b)
    }

    @Test
    fun hashCodeIsSameForEqualInstances() {
        val criterion = BooleanCriterion("id", "Name", "sql")
        val a = CriterionInstance()
        a.criterion = criterion
        a.selectedIndex = 2
        a.type = CriterionInstance.TYPE_ADD
        val b = CriterionInstance(a)
        assertEquals(a.hashCode(), b.hashCode())
    }

    @Test
    fun hashCodeDiffersForDifferentInstances() {
        val c1 = BooleanCriterion("id1", "Name1", "sql1")
        val c2 = BooleanCriterion("id2", "Name2", "sql2")
        val a = CriterionInstance()
        a.criterion = c1
        val b = CriterionInstance()
        b.criterion = c2
        assertNotEquals(a.hashCode(), b.hashCode())
    }

    // --- toString ---

    @Test
    fun toStringContainsRelevantFields() {
        val criterion = BooleanCriterion("id", "Name", "sql")
        val instance = CriterionInstance()
        instance.criterion = criterion
        instance.selectedIndex = 3
        instance.type = CriterionInstance.TYPE_INTERSECT
        val str = instance.toString()
        assertTrue(str.contains("CriterionInstance"))
        assertTrue(str.contains("selectedIndex=3"))
        assertTrue(str.contains("type=2"))
    }

    // --- type constants ---

    @Test
    fun typeConstants() {
        assertEquals(0, CriterionInstance.TYPE_ADD)
        assertEquals(1, CriterionInstance.TYPE_SUBTRACT)
        assertEquals(2, CriterionInstance.TYPE_INTERSECT)
        assertEquals(3, CriterionInstance.TYPE_UNIVERSE)
    }

    // --- default type ---

    @Test
    fun defaultTypeIsIntersect() {
        val criterion = BooleanCriterion("id", "Name", "sql")
        val instance = CriterionInstance()
        instance.criterion = criterion
        assertEquals(CriterionInstance.TYPE_INTERSECT, instance.type)
    }

    // --- id is unique ---

    @Test
    fun idIsNotEmpty() {
        val criterion = BooleanCriterion("id", "Name", "sql")
        val instance = CriterionInstance()
        instance.criterion = criterion
        assertTrue(instance.id.isNotEmpty())
    }

    @Test
    fun differentInstancesHaveDifferentIds() {
        val criterion = BooleanCriterion("id", "Name", "sql")
        val a = CriterionInstance()
        a.criterion = criterion
        val b = CriterionInstance()
        b.criterion = criterion
        assertNotEquals(a.id, b.id)
    }

    // --- serialize with different types ---

    @Test
    fun serializeContainsType() {
        val criterion = BooleanCriterion("id", "Name", "sql")
        val instance = CriterionInstance()
        instance.criterion = criterion
        instance.type = CriterionInstance.TYPE_SUBTRACT
        val result = CriterionInstance.serialize(listOf(instance))
        assertTrue(result.contains("|1|"))
    }

    @Test
    fun serializeWithTypeUniverse() {
        val criterion = BooleanCriterion("id", "Name", "sql")
        val instance = CriterionInstance()
        instance.criterion = criterion
        instance.type = CriterionInstance.TYPE_UNIVERSE
        val result = CriterionInstance.serialize(listOf(instance))
        // TYPE_UNIVERSE makes value null, which becomes empty
        assertTrue(result.contains("|3|"))
    }

    // --- equals with different criterion ---

    @Test
    fun equalsReturnsFalseForDifferentCriterion() {
        val c1 = BooleanCriterion("id1", "Name1", "sql1")
        val c2 = BooleanCriterion("id2", "Name2", "sql2")
        val a = CriterionInstance()
        a.criterion = c1
        val b = CriterionInstance()
        b.criterion = c2
        assertNotEquals(a, b)
    }
}
