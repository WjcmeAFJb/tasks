package org.tasks.data.entity

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test
import org.tasks.data.NO_ORDER

class TagDataTest {

    // --- Default values ---

    @Test
    fun defaultIdIsNull() {
        assertNull(TagData().id)
    }

    @Test
    fun defaultRemoteIdIsGenerated() {
        val tag = TagData()
        assertNotNull(tag.remoteId)
        assert(tag.remoteId!!.isNotEmpty())
    }

    @Test
    fun defaultNameIsEmpty() {
        assertEquals("", TagData().name)
    }

    @Test
    fun defaultColorIsZero() {
        assertEquals(0, TagData().color)
    }

    @Test
    fun defaultTagOrderingIsEmptyJsonArray() {
        assertEquals("[]", TagData().tagOrdering)
    }

    @Test
    fun defaultIconIsNull() {
        assertNull(TagData().icon)
    }

    @Test
    fun defaultOrderIsNoOrder() {
        assertEquals(NO_ORDER, TagData().order)
    }

    // --- Copy ---

    @Test
    fun copyChangesName() {
        val original = TagData(name = "Original")
        val copy = original.copy(name = "Updated")
        assertEquals("Updated", copy.name)
    }

    @Test
    fun copyChangesColor() {
        val original = TagData(color = 0)
        val copy = original.copy(color = 0xFF0000)
        assertEquals(0xFF0000, copy.color)
    }

    @Test
    fun copyChangesOrder() {
        val original = TagData(order = NO_ORDER)
        val copy = original.copy(order = 5)
        assertEquals(5, copy.order)
    }

    @Test
    fun copyPreservesRemoteId() {
        val original = TagData(remoteId = "abc-123")
        val copy = original.copy(name = "New Name")
        assertEquals("abc-123", copy.remoteId)
    }

    // --- Equality ---

    @Test
    fun equalityOnSameValues() {
        val a = TagData(remoteId = "abc", name = "Tag1", color = 5)
        val b = TagData(remoteId = "abc", name = "Tag1", color = 5)
        assertEquals(a, b)
    }

    @Test
    fun hashCodeConsistentWithEquals() {
        val a = TagData(remoteId = "abc", name = "Tag1", color = 5)
        val b = TagData(remoteId = "abc", name = "Tag1", color = 5)
        assertEquals(a.hashCode(), b.hashCode())
    }

    // --- Explicit values ---

    @Test
    fun setNameExplicitly() {
        val tag = TagData(name = "Work")
        assertEquals("Work", tag.name)
    }

    @Test
    fun setColorExplicitly() {
        val tag = TagData(color = 42)
        assertEquals(42, tag.color)
    }

    @Test
    fun setIconExplicitly() {
        val tag = TagData(icon = "ic_label")
        assertEquals("ic_label", tag.icon)
    }

    @Test
    fun setTagOrderingExplicitly() {
        val tag = TagData(tagOrdering = "[1,2,3]")
        assertEquals("[1,2,3]", tag.tagOrdering)
    }

    @Test
    fun setOrderExplicitly() {
        val tag = TagData(order = 7)
        assertEquals(7, tag.order)
    }

    @Test
    fun twoTagDataWithDifferentRemoteIdsAreNotEqual() {
        val a = TagData(remoteId = "aaa", name = "Tag")
        val b = TagData(remoteId = "bbb", name = "Tag")
        assert(a != b)
    }
}
