package org.tasks.tasklist

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.tasks.data.TaskContainer
import org.tasks.data.entity.Task

class UiItemTest {
    @Test fun headerKey() = assertEquals("header_42", UiItem.Header(42, false).key)
    @Test fun headerKeyNegative() = assertEquals("header_-1", UiItem.Header(-1, false).key)
    @Test fun taskKey() {
        val container = TaskContainer(task = Task(id = 99))
        assertEquals("99", UiItem.Task(container).key)
    }
    @Test fun headerCollapsed() = assertTrue(UiItem.Header(1, true).collapsed)
    @Test fun headerNotCollapsed() = assertFalse(UiItem.Header(1, false).collapsed)
    @Test fun headerValue() = assertEquals(100L, UiItem.Header(100, false).value)

    @Test fun adapterSectionDefaults() {
        val section = AdapterSection(firstPosition = 5, value = 10)
        assertEquals(5, section.firstPosition)
        assertEquals(10L, section.value)
        assertEquals(0, section.sectionedPosition)
        assertFalse(section.collapsed)
    }

    @Test fun adapterSectionCollapsed() {
        val section = AdapterSection(firstPosition = 0, value = 0, collapsed = true)
        assertTrue(section.collapsed)
    }

    @Test fun adapterSectionSectionedPosition() {
        val section = AdapterSection(firstPosition = 3, value = 7, sectionedPosition = 5)
        assertEquals(5, section.sectionedPosition)
    }

    @Test fun headerKeyZero() = assertEquals("header_0", UiItem.Header(0, false).key)

    @Test fun headerKeyMaxLong() =
        assertEquals("header_${Long.MAX_VALUE}", UiItem.Header(Long.MAX_VALUE, false).key)

    @Test fun taskKeyZero() {
        val container = TaskContainer(task = Task(id = 0))
        assertEquals("0", UiItem.Task(container).key)
    }

    @Test fun adapterSectionMutableFirstPosition() {
        val section = AdapterSection(firstPosition = 0, value = 1)
        section.firstPosition = 10
        assertEquals(10, section.firstPosition)
    }

    @Test fun adapterSectionMutableSectionedPosition() {
        val section = AdapterSection(firstPosition = 0, value = 1)
        section.sectionedPosition = 42
        assertEquals(42, section.sectionedPosition)
    }

    @Test fun adapterSectionMutableCollapsed() {
        val section = AdapterSection(firstPosition = 0, value = 1)
        assertFalse(section.collapsed)
        section.collapsed = true
        assertTrue(section.collapsed)
    }

    @Test fun headerEquality() {
        assertEquals(UiItem.Header(5, true), UiItem.Header(5, true))
    }

    @Test fun headerInequality() {
        assertNotEquals(UiItem.Header(5, true), UiItem.Header(5, false))
    }

    @Test fun adapterSectionCopy() {
        val section = AdapterSection(firstPosition = 2, value = 3, sectionedPosition = 4, collapsed = true)
        val copy = section.copy(collapsed = false)
        assertFalse(copy.collapsed)
        assertEquals(2, copy.firstPosition)
    }
}
