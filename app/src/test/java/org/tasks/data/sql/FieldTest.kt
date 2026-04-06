package org.tasks.data.sql

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class FieldTest {
    @Test fun fieldExpression() = assertEquals("name", Field("name").expression)
    @Test fun fieldToString() = assertEquals("name", Field("name").toString())
    @Test fun fieldWithAlias() = assertEquals("alias", Field("name").`as`("alias").toString())
    @Test fun toStringInSelectNoAlias() = assertEquals("name", Field("name").toStringInSelect())
    @Test fun toStringInSelectWithAlias() = assertEquals("name AS alias", Field("name").`as`("alias").toStringInSelect())
    @Test fun toStringInSelectDotNotation() = assertEquals("table.col AS col", Field("table.col").toStringInSelect())
    @Test fun toStringInSelectStar() = assertEquals("table.*", Field("table.*").toStringInSelect())

    @Test fun eqInt() = assertEquals("(name=42)", Field("name").eq(42).toString())
    @Test fun eqString() = assertEquals("(name='hello')", Field("name").eq("hello").toString())
    @Test fun eqNull() = assertEquals("(name IS NULL)", Field("name").eq(null).toString())
    @Test fun gt() = assertEquals("(name>5)", Field("name").gt(5).toString())
    @Test fun lt() = assertEquals("(name<5)", Field("name").lt(5).toString())
    @Test fun lte() = assertEquals("(name<=5)", Field("name").lte(5).toString())
    @Test fun like() = assertEquals("(name LIKE 'pattern')", Field("name").like("pattern").toString())

    @Test fun inList() = assertEquals("(name IN (1,2,3))", Field("name").`in`(listOf(1, 2, 3)).toString())
    @Test fun inEmptyList() = assertEquals("(name IN ())", Field("name").`in`(emptyList<Int>()).toString())

    @Test fun fieldCompanion() = assertEquals("expr", Field.field("expr").expression)
    @Test fun countField() = assertEquals("COUNT(*)", Field.COUNT.expression)

    @Test fun equalsAndHashCode() {
        val f1 = Field("x")
        val f2 = Field("x")
        assertEquals(f1, f2)
        assertEquals(f1.hashCode(), f2.hashCode())
    }

    @Test fun notEqualsDifferentExpression() {
        assertFalse(Field("x") == Field("y"))
    }

    @Test fun equalsDifferentAlias() {
        assertFalse(Field("x").`as`("a") == Field("x").`as`("b"))
    }

    @Test fun sanitizeSingleQuote() {
        assertEquals("(name='it''s')", Field("name").eq("it's").toString())
    }
}
