package org.tasks.data.entity

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import org.tasks.data.NO_ORDER

class FilterEntityTest {

    // --- Default values ---

    @Test
    fun defaultId() = assertEquals(0L, Filter().id)

    @Test
    fun defaultTitle() = assertNull(Filter().title)

    @Test
    fun defaultSql() = assertNull(Filter().sql)

    @Test
    fun defaultValues() = assertNull(Filter().values)

    @Test
    fun defaultCriterion() = assertNull(Filter().criterion)

    @Test
    fun defaultColor() = assertEquals(0, Filter().color)

    @Test
    fun defaultIcon() = assertNull(Filter().icon)

    @Test
    fun defaultOrder() = assertEquals(NO_ORDER, Filter().order)

    // --- Construction with values ---

    @Test
    fun constructWithTitle() {
        val filter = Filter(title = "My Filter")
        assertEquals("My Filter", filter.title)
    }

    @Test
    fun constructWithSql() {
        val filter = Filter(sql = "SELECT * FROM tasks WHERE 1")
        assertEquals("SELECT * FROM tasks WHERE 1", filter.sql)
    }

    @Test
    fun constructWithValues() {
        val filter = Filter(values = "{\"key\": \"value\"}")
        assertEquals("{\"key\": \"value\"}", filter.values)
    }

    @Test
    fun constructWithCriterion() {
        val filter = Filter(criterion = "priority > 0")
        assertEquals("priority > 0", filter.criterion)
    }

    @Test
    fun constructWithColor() {
        val filter = Filter(color = 42)
        assertEquals(42, filter.color)
    }

    @Test
    fun constructWithIcon() {
        val filter = Filter(icon = "ic_star")
        assertEquals("ic_star", filter.icon)
    }

    @Test
    fun constructWithOrder() {
        val filter = Filter(order = 5)
        assertEquals(5, filter.order)
    }

    // --- Data class features ---

    @Test
    fun equality() {
        val a = Filter(title = "Test", sql = "SELECT 1", color = 3)
        val b = Filter(title = "Test", sql = "SELECT 1", color = 3)
        assertEquals(a, b)
    }

    @Test
    fun hashCodeConsistentWithEquals() {
        val a = Filter(title = "Test", sql = "SELECT 1")
        val b = Filter(title = "Test", sql = "SELECT 1")
        assertEquals(a.hashCode(), b.hashCode())
    }

    @Test
    fun copy() {
        val original = Filter(title = "Original", color = 1)
        val copied = original.copy(title = "Copied", color = 2)
        assertEquals("Copied", copied.title)
        assertEquals(2, copied.color)
        assertEquals(original.sql, copied.sql)
    }

    @Test
    fun copyPreservesUnchangedFields() {
        val original = Filter(
            title = "Test",
            sql = "SELECT 1",
            values = "vals",
            criterion = "crit",
            color = 5,
            icon = "ic",
            order = 3
        )
        val copied = original.copy(title = "New")
        assertEquals("New", copied.title)
        assertEquals("SELECT 1", copied.sql)
        assertEquals("vals", copied.values)
        assertEquals("crit", copied.criterion)
        assertEquals(5, copied.color)
        assertEquals("ic", copied.icon)
        assertEquals(3, copied.order)
    }

    @Test
    fun noOrderConstant() = assertEquals(-1, NO_ORDER)
}
