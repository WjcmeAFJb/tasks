package org.tasks.data.sql

import org.junit.Assert.assertEquals
import org.junit.Test

class UnaryCriterionTest {
    private val f = Field("col")

    @Test fun eq() = assertEquals("(col=42)", UnaryCriterion.eq(f, 42).toString())
    @Test fun eqString() = assertEquals("(col='hello')", UnaryCriterion.eq(f, "hello").toString())
    @Test fun gt() = assertEquals("(col>10)", UnaryCriterion.gt(f, 10).toString())
    @Test fun lt() = assertEquals("(col<10)", UnaryCriterion.lt(f, 10).toString())
    @Test fun lte() = assertEquals("(col<=10)", UnaryCriterion.lte(f, 10).toString())
    @Test fun isNull() = assertEquals("(col IS NULL)", UnaryCriterion.isNull(f).toString())
    @Test fun isNotNull() = assertEquals("(col IS NOT NULL)", UnaryCriterion.isNotNull(f).toString())
    @Test fun like() = assertEquals("(col LIKE 'pat%')", UnaryCriterion.like(f, "pat%").toString())
    @Test fun sanitize() = assertEquals("it''s", UnaryCriterion.sanitize("it's"))
    @Test fun sanitizeMultiple() = assertEquals("it''s a ''test''", UnaryCriterion.sanitize("it's a 'test'"))
    @Test fun sanitizeNoQuotes() = assertEquals("hello", UnaryCriterion.sanitize("hello"))
}
