package org.tasks.data.sql

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Test

class JoinTypeTest {

    @Test
    fun innerExists() {
        assertNotNull(JoinType.INNER)
    }

    @Test
    fun leftExists() {
        assertNotNull(JoinType.LEFT)
    }

    @Test
    fun valuesContainsBothTypes() {
        val values = JoinType.entries
        assertEquals(2, values.size)
    }

    @Test
    fun innerName() {
        assertEquals("INNER", JoinType.INNER.name)
    }

    @Test
    fun leftName() {
        assertEquals("LEFT", JoinType.LEFT.name)
    }

    @Test
    fun valueOfInner() {
        assertEquals(JoinType.INNER, JoinType.valueOf("INNER"))
    }

    @Test
    fun valueOfLeft() {
        assertEquals(JoinType.LEFT, JoinType.valueOf("LEFT"))
    }

    @Test
    fun ordinals() {
        assertEquals(0, JoinType.INNER.ordinal)
        assertEquals(1, JoinType.LEFT.ordinal)
    }
}
