package com.todoroo.astrid.core

import com.todoroo.astrid.api.BooleanCriterion
import com.todoroo.astrid.api.MultipleSelectCriterion
import com.todoroo.astrid.api.TextInputCriterion
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class CriterionInstanceMaxCovTest {

    // ===== titleFromCriterion =====

    @Test
    fun titleMultipleSelectValidIndex() {
        val c = MultipleSelectCriterion("t", "Priority is ?", null, null, arrayOf("High", "Med"), arrayOf("1", "2"), "P")
        val i = CriterionInstance().apply { criterion = c; selectedIndex = 1 }
        assertEquals("Priority is Med", i.titleFromCriterion)
    }

    @Test
    fun titleMultipleSelectNegativeIndex() {
        val c = MultipleSelectCriterion("t", "Priority is ?", null, null, arrayOf("High"), arrayOf("1"), "P")
        val i = CriterionInstance().apply { criterion = c; selectedIndex = -1 }
        assertEquals("Priority is ?", i.titleFromCriterion)
    }

    @Test
    fun titleMultipleSelectNullTitles() {
        val c = MultipleSelectCriterion("t", "All items", null, null, null, null, "T")
        val i = CriterionInstance().apply { criterion = c; selectedIndex = 0 }
        assertEquals("All items", i.titleFromCriterion)
    }

    @Test
    fun titleMultipleSelectOutOfBounds() {
        val c = MultipleSelectCriterion("t", "Test ?", null, null, arrayOf("One"), arrayOf("1"), "T")
        val i = CriterionInstance().apply { criterion = c; selectedIndex = 5 }
        assertEquals("Test ?", i.titleFromCriterion)
    }

    @Test
    fun titleTextInputWithText() {
        val c = TextInputCriterion("t", "Title contains ?", null, "Title", "", "T")
        val i = CriterionInstance().apply { criterion = c; selectedText = "groceries" }
        assertEquals("Title contains groceries", i.titleFromCriterion)
    }

    @Test
    fun titleTextInputNullText() {
        val c = TextInputCriterion("t", "Title contains ?", null, "Title", "", "T")
        val i = CriterionInstance().apply { criterion = c; selectedText = null }
        assertEquals("Title contains ?", i.titleFromCriterion)
    }

    @Test
    fun titleBoolean() {
        val c = BooleanCriterion("r", "Recurring", "")
        val i = CriterionInstance().apply { criterion = c }
        assertEquals("Recurring", i.titleFromCriterion)
    }

    // ===== valueFromCriterion =====

    @Test
    fun valueUniverseNull() {
        val c = MultipleSelectCriterion("t", "All", null, null, null, null, null)
        val i = CriterionInstance().apply { criterion = c; type = CriterionInstance.TYPE_UNIVERSE }
        assertNull(i.valueFromCriterion)
    }

    @Test
    fun valueMultipleSelectValid() {
        val c = MultipleSelectCriterion("t", "P", null, null, arrayOf("H"), arrayOf("1"), "P")
        val i = CriterionInstance().apply { criterion = c; selectedIndex = 0; type = CriterionInstance.TYPE_INTERSECT }
        assertEquals("1", i.valueFromCriterion)
    }

    @Test
    fun valueMultipleSelectNullValues() {
        val c = MultipleSelectCriterion("t", "All items", null, null, null, null, "T")
        val i = CriterionInstance().apply { criterion = c; selectedIndex = 0; type = CriterionInstance.TYPE_ADD }
        assertEquals("All items", i.valueFromCriterion)
    }

    @Test
    fun valueMultipleSelectNegativeIndex() {
        val c = MultipleSelectCriterion("t", "Fallback", null, null, arrayOf("A"), arrayOf("a"), "T")
        val i = CriterionInstance().apply { criterion = c; selectedIndex = -1; type = CriterionInstance.TYPE_INTERSECT }
        assertEquals("Fallback", i.valueFromCriterion)
    }

    @Test
    fun valueTextInput() {
        val c = TextInputCriterion("t", "?", null, "T", "", "T")
        val i = CriterionInstance().apply { criterion = c; selectedText = "search"; type = CriterionInstance.TYPE_INTERSECT }
        assertEquals("search", i.valueFromCriterion)
    }

    @Test
    fun valueBoolean() {
        val c = BooleanCriterion("r", "Recurring", "")
        val i = CriterionInstance().apply { criterion = c; type = CriterionInstance.TYPE_INTERSECT }
        assertEquals("Recurring", i.valueFromCriterion)
    }

    // ===== copy constructor =====

    @Test
    fun copyConstructor() {
        val c = BooleanCriterion("t", "Test", "")
        val original = CriterionInstance().apply {
            criterion = c; selectedIndex = 3; selectedText = "hello"
            type = CriterionInstance.TYPE_SUBTRACT; end = 10; start = 5; max = 100
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

    // ===== equals and hashCode =====

    @Test
    fun equalsReflexive() {
        val c = BooleanCriterion("t", "Test", "")
        val i = CriterionInstance().apply { criterion = c }
        assertEquals(i, i)
    }

    @Test
    fun equalsWithCopy() {
        val c = BooleanCriterion("t", "Test", "")
        val o = CriterionInstance().apply { criterion = c }
        assertEquals(o, CriterionInstance(o))
    }

    @Test
    fun equalsNullFalse() {
        val c = BooleanCriterion("t", "Test", "")
        val i = CriterionInstance().apply { criterion = c }
        assertFalse(i.equals(null))
    }

    @Test
    fun equalsOtherTypeFalse() {
        val c = BooleanCriterion("t", "Test", "")
        val i = CriterionInstance().apply { criterion = c }
        assertFalse(i.equals("not a CriterionInstance"))
    }

    @Test
    fun equalsDifferentType() {
        val c = BooleanCriterion("t", "Test", "")
        val a = CriterionInstance().apply { criterion = c; type = CriterionInstance.TYPE_ADD }
        val b = CriterionInstance(a).apply { type = CriterionInstance.TYPE_SUBTRACT }
        assertNotEquals(a, b)
    }

    @Test
    fun hashCodeConsistent() {
        val c = BooleanCriterion("t", "Test", "")
        val a = CriterionInstance().apply { criterion = c }
        val b = CriterionInstance(a)
        assertEquals(a.hashCode(), b.hashCode())
    }

    // ===== serialize =====

    @Test
    fun serializeSingle() {
        val c = BooleanCriterion("recur", "Recurring", "SELECT id FROM tasks")
        val i = CriterionInstance().apply { criterion = c; type = CriterionInstance.TYPE_INTERSECT }
        val s = CriterionInstance.serialize(listOf(i))
        assertTrue(s.contains("recur"))
        assertTrue(s.contains("2"))
    }

    @Test
    fun serializeMultiple() {
        val c1 = BooleanCriterion("recur", "Recurring", "sql1")
        val c2 = TextInputCriterion("title", "Title ?", "sql2", "T", "", "T")
        val i1 = CriterionInstance().apply { criterion = c1; type = CriterionInstance.TYPE_UNIVERSE }
        val i2 = CriterionInstance().apply { criterion = c2; selectedText = "test"; type = CriterionInstance.TYPE_INTERSECT }
        val s = CriterionInstance.serialize(listOf(i1, i2))
        assertTrue(s.contains("\n"))
        assertTrue(s.contains("recur"))
        assertTrue(s.contains("title"))
    }

    @Test
    fun serializeEmpty() {
        assertEquals("", CriterionInstance.serialize(emptyList()))
    }

    @Test
    fun serializeEscapesPipe() {
        val c = TextInputCriterion("title", "Title ?", "sql", "T", "", "T")
        val i = CriterionInstance().apply { criterion = c; selectedText = "hello|world"; type = CriterionInstance.TYPE_INTERSECT }
        val s = CriterionInstance.serialize(listOf(i))
        assertTrue(s.contains("!PIPE!"))
    }

    // ===== toString =====

    @Test
    fun toStringContainsType() {
        val c = BooleanCriterion("t", "Test", "")
        val i = CriterionInstance().apply { criterion = c; type = CriterionInstance.TYPE_ADD }
        assertTrue(i.toString().contains("type=0"))
    }

    // ===== type constants =====

    @Test
    fun typeConstants() {
        assertEquals(0, CriterionInstance.TYPE_ADD)
        assertEquals(1, CriterionInstance.TYPE_SUBTRACT)
        assertEquals(2, CriterionInstance.TYPE_INTERSECT)
        assertEquals(3, CriterionInstance.TYPE_UNIVERSE)
    }
}
