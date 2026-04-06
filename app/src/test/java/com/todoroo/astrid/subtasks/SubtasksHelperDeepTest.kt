package com.todoroo.astrid.subtasks

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class SubtasksHelperDeepTest {

    // ===== getStringIdArray =====

    @Test
    fun getStringIdArrayFromEmptyArray() {
        val result = SubtasksHelper.getStringIdArray("[]")
        assertEquals(0, result.size)
    }

    @Test
    fun getStringIdArrayFromSingleId() {
        val result = SubtasksHelper.getStringIdArray("[\"abc\"]")
        assertEquals(1, result.size)
        assertEquals("abc", result[0])
    }

    @Test
    fun getStringIdArrayFromMultipleIds() {
        val result = SubtasksHelper.getStringIdArray("[\"a\",\"b\",\"c\"]")
        assertEquals(3, result.size)
        assertEquals("a", result[0])
        assertEquals("b", result[1])
        assertEquals("c", result[2])
    }

    @Test
    fun getStringIdArrayFromNestedArray() {
        val result = SubtasksHelper.getStringIdArray("[[\"p\",\"c1\"],\"sibling\"]")
        assertTrue(result.contains("p"))
        assertTrue(result.contains("c1"))
        assertTrue(result.contains("sibling"))
    }

    @Test
    fun getStringIdArrayFiltersEmptyStrings() {
        val result = SubtasksHelper.getStringIdArray("[,,,]")
        assertEquals(0, result.size)
    }

    @Test
    fun getStringIdArrayWithWhitespace() {
        val result = SubtasksHelper.getStringIdArray("[ \"x\" , \"y\" ]")
        assertEquals(2, result.size)
        assertEquals("x", result[0])
        assertEquals("y", result[1])
    }

    @Test
    fun getStringIdArrayWithNumericStrings() {
        val result = SubtasksHelper.getStringIdArray("[\"123\",\"456\",\"789\"]")
        assertEquals(3, result.size)
        assertEquals("123", result[0])
    }

    @Test
    fun getStringIdArrayWithDeeplyNested() {
        val result = SubtasksHelper.getStringIdArray("[[\"a\",[\"b\",\"c\"]]]")
        assertTrue(result.contains("a"))
        assertTrue(result.contains("b"))
        assertTrue(result.contains("c"))
    }

    @Test
    fun getStringIdArrayWithSpecialChars() {
        val result = SubtasksHelper.getStringIdArray("[\"uuid-with-dashes\",\"uuid.with.dots\"]")
        assertEquals(2, result.size)
        assertTrue(result.contains("uuid-with-dashes"))
        assertTrue(result.contains("uuid.with.dots"))
    }

    @Test
    fun getStringIdArrayWithLongUuids() {
        val longId = "a".repeat(50)
        val result = SubtasksHelper.getStringIdArray("[\"$longId\"]")
        assertEquals(1, result.size)
        assertEquals(longId, result[0])
    }

    @Test
    fun getStringIdArrayWithMixedNesting() {
        val result = SubtasksHelper.getStringIdArray("[\"root\",[\"parent\",\"child\"]]")
        assertTrue(result.contains("root"))
        assertTrue(result.contains("parent"))
        assertTrue(result.contains("child"))
    }

    @Test
    fun getStringIdArrayOrderPreserved() {
        val result = SubtasksHelper.getStringIdArray("[\"z\",\"a\",\"m\"]")
        assertEquals("z", result[0])
        assertEquals("a", result[1])
        assertEquals("m", result[2])
    }

    @Test
    fun getStringIdArrayWithOnlyWhitespace() {
        val result = SubtasksHelper.getStringIdArray("[  ]")
        assertEquals(0, result.size)
    }

    @Test
    fun getStringIdArrayWithUuidFormat() {
        val uuid = "550e8400-e29b-41d4-a716-446655440000"
        val result = SubtasksHelper.getStringIdArray("[\"$uuid\"]")
        assertEquals(1, result.size)
        assertEquals(uuid, result[0])
    }
}
