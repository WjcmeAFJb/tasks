package com.todoroo.astrid.subtasks

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class SubtasksHelperTest {

    // --- getStringIdArray ---

    @Test
    fun getStringIdArrayWithFlatIds() {
        val result = SubtasksHelper.getStringIdArray("[\"uuid1\",\"uuid2\",\"uuid3\"]")
        assertEquals(3, result.size)
        assertEquals("uuid1", result[0])
        assertEquals("uuid2", result[1])
        assertEquals("uuid3", result[2])
    }

    @Test
    fun getStringIdArrayWithNestedIds() {
        val result = SubtasksHelper.getStringIdArray("[[\"parent\",\"child1\",\"child2\"]]")
        assertTrue(result.contains("parent"))
        assertTrue(result.contains("child1"))
        assertTrue(result.contains("child2"))
    }

    @Test
    fun getStringIdArrayWithEmptyArray() {
        val result = SubtasksHelper.getStringIdArray("[]")
        assertEquals(0, result.size)
    }

    @Test
    fun getStringIdArrayWithWhitespace() {
        val result = SubtasksHelper.getStringIdArray("[ \"uuid1\" , \"uuid2\" ]")
        assertEquals(2, result.size)
        assertEquals("uuid1", result[0])
        assertEquals("uuid2", result[1])
    }

    @Test
    fun getStringIdArrayFiltersEmptyStrings() {
        val result = SubtasksHelper.getStringIdArray("[,,,]")
        assertEquals(0, result.size)
    }

    @Test
    fun getStringIdArrayWithMixedContent() {
        val result = SubtasksHelper.getStringIdArray("[\"root\",[\"parent\",\"child\"]]")
        assertTrue(result.contains("root"))
        assertTrue(result.contains("parent"))
        assertTrue(result.contains("child"))
    }

    @Test
    fun getStringIdArrayWithNumericIds() {
        val result = SubtasksHelper.getStringIdArray("[\"123\",\"456\"]")
        assertEquals(2, result.size)
        assertEquals("123", result[0])
        assertEquals("456", result[1])
    }

    @Test
    fun getStringIdArrayWithSingleItem() {
        val result = SubtasksHelper.getStringIdArray("[\"only\"]")
        assertEquals(1, result.size)
        assertEquals("only", result[0])
    }
}
