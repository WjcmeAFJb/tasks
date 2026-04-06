package org.tasks.data.sql

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class DBObjectTest {

    // Use Field as a concrete subclass of DBObject for testing
    private fun dbObject(expr: String) = Field(expr)

    // --- toString ---

    @Test
    fun toStringReturnsExpression() {
        assertEquals("tasks.id", dbObject("tasks.id").toString())
    }

    @Test
    fun toStringReturnsAliasWhenSet() {
        val obj = dbObject("tasks.id").`as`("tid")
        assertEquals("tid", obj.toString())
    }

    @Test
    fun toStringReturnsExpressionWhenNoAlias() {
        val obj = dbObject("COUNT(*)")
        assertEquals("COUNT(*)", obj.toString())
    }

    // --- hasAlias (tested indirectly via `as` and behavior) ---

    @Test
    fun noAliasInitially() {
        val obj = dbObject("x")
        assertNull(obj.alias)
    }

    @Test
    fun aliasSetAfterAs() {
        val obj = dbObject("x").`as`("y")
        assertEquals("y", obj.alias)
    }

    // --- equals ---

    @Test
    fun equalsSameObject() {
        val obj = dbObject("x")
        @Suppress("ReplaceCallWithBinaryOperator")
        assertTrue(obj.equals(obj))
    }

    @Test
    fun equalsSameExpressionNoAlias() {
        val a = dbObject("tasks.id")
        val b = dbObject("tasks.id")
        assertEquals(a, b)
    }

    @Test
    fun equalsSameExpressionSameAlias() {
        val a = dbObject("tasks.id").`as`("tid")
        val b = dbObject("tasks.id").`as`("tid")
        assertEquals(a, b)
    }

    @Test
    fun notEqualsDifferentExpression() {
        val a = dbObject("tasks.id")
        val b = dbObject("tasks.title")
        assertNotEquals(a, b)
    }

    @Test
    fun notEqualsDifferentAlias() {
        val a = dbObject("tasks.id").`as`("a")
        val b = dbObject("tasks.id").`as`("b")
        assertNotEquals(a, b)
    }

    @Test
    fun notEqualsOneHasAlias() {
        val a = dbObject("tasks.id")
        val b = dbObject("tasks.id").`as`("tid")
        assertNotEquals(a, b)
    }

    @Test
    fun notEqualsNull() {
        val obj = dbObject("x")
        assertFalse(obj.equals(null))
    }

    @Test
    fun notEqualsNonDBObject() {
        val obj = dbObject("x")
        assertFalse(obj.equals("x"))
    }

    // --- hashCode ---

    @Test
    fun hashCodeConsistentWithEquals() {
        val a = dbObject("tasks.id")
        val b = dbObject("tasks.id")
        assertEquals(a.hashCode(), b.hashCode())
    }

    @Test
    fun hashCodeWithAlias() {
        val a = dbObject("tasks.id").`as`("tid")
        val b = dbObject("tasks.id").`as`("tid")
        assertEquals(a.hashCode(), b.hashCode())
    }

    @Test
    fun hashCodeDifferentForDifferentExpressions() {
        val a = dbObject("tasks.id")
        val b = dbObject("tasks.title")
        assertNotEquals(a.hashCode(), b.hashCode())
    }

    @Test
    fun hashCodeDifferentForDifferentAliases() {
        val a = dbObject("x").`as`("a")
        val b = dbObject("x").`as`("b")
        assertNotEquals(a.hashCode(), b.hashCode())
    }

    @Test
    fun hashCodeNoAliasVsWithAlias() {
        val a = dbObject("x")
        val b = dbObject("x").`as`("a")
        // They're unequal, so hashCodes *may* differ (not guaranteed but likely)
        assertNotEquals(a, b)
    }

    // --- expression field ---

    @Test
    fun expressionIsStored() {
        assertEquals("my_expression", dbObject("my_expression").expression)
    }

    @Test
    fun expressionPreservedAfterAlias() {
        val obj = dbObject("my_expression").`as`("alias")
        assertEquals("my_expression", obj.expression)
    }
}
