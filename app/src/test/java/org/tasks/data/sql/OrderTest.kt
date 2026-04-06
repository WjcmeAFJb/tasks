package org.tasks.data.sql

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class OrderTest {
    @Test fun ascOrder() = assertEquals("name ASC", Order.asc("name").toString())
    @Test fun descOrder() = assertEquals("name DESC", Order.desc("name").toString())
    @Test fun reverseAsc() = assertEquals("name DESC", Order.asc("name").reverse().toString())
    @Test fun reverseDesc() = assertEquals("name ASC", Order.desc("name").reverse().toString())
    @Test fun ascType() = assertEquals(OrderType.ASC, Order.asc("x").orderType)
    @Test fun descType() = assertEquals(OrderType.DESC, Order.desc("x").orderType)

    @Test fun secondaryExpression() {
        val o = Order.asc("a").addSecondaryExpression(Order.desc("b"))
        val result = o.toString()
        assertTrue(result.contains("a ASC"))
        assertTrue(result.contains("b DESC"))
    }

    @Test fun multipleSecondary() {
        val o = Order.asc("a")
            .addSecondaryExpression(Order.desc("b"))
            .addSecondaryExpression(Order.asc("c"))
        val result = o.toString()
        assertTrue(result.contains("b DESC"))
        assertTrue(result.contains("c ASC"))
    }
}
