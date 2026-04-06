package org.tasks.data.sql

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Test

class OrderTypeTest {

    @Test
    fun descExists() {
        assertNotNull(OrderType.DESC)
    }

    @Test
    fun ascExists() {
        assertNotNull(OrderType.ASC)
    }

    @Test
    fun valuesContainsBothTypes() {
        val values = OrderType.entries
        assertEquals(2, values.size)
    }

    @Test
    fun descName() {
        assertEquals("DESC", OrderType.DESC.name)
    }

    @Test
    fun ascName() {
        assertEquals("ASC", OrderType.ASC.name)
    }

    @Test
    fun valueOfDesc() {
        assertEquals(OrderType.DESC, OrderType.valueOf("DESC"))
    }

    @Test
    fun valueOfAsc() {
        assertEquals(OrderType.ASC, OrderType.valueOf("ASC"))
    }

    @Test
    fun ordinals() {
        assertEquals(0, OrderType.DESC.ordinal)
        assertEquals(1, OrderType.ASC.ordinal)
    }

    @Test
    fun toStringMatchesName() {
        assertEquals("DESC", OrderType.DESC.toString())
        assertEquals("ASC", OrderType.ASC.toString())
    }
}
