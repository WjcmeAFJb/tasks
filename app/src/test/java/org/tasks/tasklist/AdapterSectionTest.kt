package org.tasks.tasklist

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class AdapterSectionTest {

    // --- Construction ---

    @Test
    fun constructWithRequiredFields() {
        val section = AdapterSection(firstPosition = 0, value = 100L)
        assertEquals(0, section.firstPosition)
        assertEquals(100L, section.value)
    }

    @Test
    fun defaultSectionedPositionIsZero() {
        val section = AdapterSection(firstPosition = 5, value = 200L)
        assertEquals(0, section.sectionedPosition)
    }

    @Test
    fun defaultCollapsedIsFalse() {
        val section = AdapterSection(firstPosition = 0, value = 100L)
        assertFalse(section.collapsed)
    }

    @Test
    fun constructWithAllFields() {
        val section = AdapterSection(
            firstPosition = 3,
            value = 42L,
            sectionedPosition = 7,
            collapsed = true,
        )
        assertEquals(3, section.firstPosition)
        assertEquals(42L, section.value)
        assertEquals(7, section.sectionedPosition)
        assertTrue(section.collapsed)
    }

    // --- Mutable fields ---

    @Test
    fun firstPositionIsMutable() {
        val section = AdapterSection(firstPosition = 0, value = 1L)
        section.firstPosition = 10
        assertEquals(10, section.firstPosition)
    }

    @Test
    fun sectionedPositionIsMutable() {
        val section = AdapterSection(firstPosition = 0, value = 1L)
        section.sectionedPosition = 5
        assertEquals(5, section.sectionedPosition)
    }

    @Test
    fun collapsedIsMutable() {
        val section = AdapterSection(firstPosition = 0, value = 1L)
        section.collapsed = true
        assertTrue(section.collapsed)
    }

    // --- Equality ---

    @Test
    fun equalSectionsAreEqual() {
        val s1 = AdapterSection(firstPosition = 1, value = 10L, sectionedPosition = 2, collapsed = false)
        val s2 = AdapterSection(firstPosition = 1, value = 10L, sectionedPosition = 2, collapsed = false)
        assertEquals(s1, s2)
    }

    @Test
    fun differentFirstPositionNotEqual() {
        val s1 = AdapterSection(firstPosition = 1, value = 10L)
        val s2 = AdapterSection(firstPosition = 2, value = 10L)
        assertNotEquals(s1, s2)
    }

    @Test
    fun differentValueNotEqual() {
        val s1 = AdapterSection(firstPosition = 1, value = 10L)
        val s2 = AdapterSection(firstPosition = 1, value = 20L)
        assertNotEquals(s1, s2)
    }

    @Test
    fun differentSectionedPositionNotEqual() {
        val s1 = AdapterSection(firstPosition = 1, value = 10L, sectionedPosition = 0)
        val s2 = AdapterSection(firstPosition = 1, value = 10L, sectionedPosition = 5)
        assertNotEquals(s1, s2)
    }

    @Test
    fun differentCollapsedNotEqual() {
        val s1 = AdapterSection(firstPosition = 1, value = 10L, collapsed = false)
        val s2 = AdapterSection(firstPosition = 1, value = 10L, collapsed = true)
        assertNotEquals(s1, s2)
    }

    // --- hashCode ---

    @Test
    fun equalObjectsHaveSameHashCode() {
        val s1 = AdapterSection(firstPosition = 5, value = 99L, sectionedPosition = 3, collapsed = true)
        val s2 = AdapterSection(firstPosition = 5, value = 99L, sectionedPosition = 3, collapsed = true)
        assertEquals(s1.hashCode(), s2.hashCode())
    }

    // --- copy ---

    @Test
    fun copyWithDifferentFirstPosition() {
        val section = AdapterSection(firstPosition = 1, value = 10L, sectionedPosition = 2, collapsed = true)
        val copy = section.copy(firstPosition = 99)
        assertEquals(99, copy.firstPosition)
        assertEquals(10L, copy.value)
        assertEquals(2, copy.sectionedPosition)
        assertTrue(copy.collapsed)
    }

    @Test
    fun copyWithDifferentValue() {
        val section = AdapterSection(firstPosition = 1, value = 10L)
        val copy = section.copy(value = 42L)
        assertEquals(42L, copy.value)
        assertEquals(1, copy.firstPosition)
    }

    @Test
    fun copyWithDifferentCollapsed() {
        val section = AdapterSection(firstPosition = 1, value = 10L, collapsed = false)
        val copy = section.copy(collapsed = true)
        assertTrue(copy.collapsed)
        assertFalse(section.collapsed)
    }

    @Test
    fun copyPreservesOriginal() {
        val section = AdapterSection(firstPosition = 1, value = 10L, sectionedPosition = 5)
        section.copy(firstPosition = 99)
        assertEquals(1, section.firstPosition)
    }

    // --- Edge cases ---

    @Test
    fun zeroValue() {
        val section = AdapterSection(firstPosition = 0, value = 0L)
        assertEquals(0L, section.value)
    }

    @Test
    fun negativeValue() {
        val section = AdapterSection(firstPosition = 0, value = -1L)
        assertEquals(-1L, section.value)
    }

    @Test
    fun largeValue() {
        val section = AdapterSection(firstPosition = 0, value = Long.MAX_VALUE)
        assertEquals(Long.MAX_VALUE, section.value)
    }

    @Test
    fun negativeFirstPosition() {
        // While not semantically meaningful, the data class allows it
        val section = AdapterSection(firstPosition = -1, value = 0L)
        assertEquals(-1, section.firstPosition)
    }

    // --- toString ---

    @Test
    fun toStringContainsFieldValues() {
        val section = AdapterSection(firstPosition = 3, value = 42L, sectionedPosition = 7, collapsed = true)
        val str = section.toString()
        assert(str.contains("3")) { "Expected toString to contain firstPosition" }
        assert(str.contains("42")) { "Expected toString to contain value" }
        assert(str.contains("7")) { "Expected toString to contain sectionedPosition" }
        assert(str.contains("true")) { "Expected toString to contain collapsed" }
    }

    // --- Destructuring ---

    @Test
    fun destructuringComponents() {
        val section = AdapterSection(firstPosition = 1, value = 2L, sectionedPosition = 3, collapsed = true)
        val (fp, v, sp, c) = section
        assertEquals(1, fp)
        assertEquals(2L, v)
        assertEquals(3, sp)
        assertTrue(c)
    }
}
