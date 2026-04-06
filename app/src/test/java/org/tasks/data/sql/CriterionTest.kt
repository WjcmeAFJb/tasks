package org.tasks.data.sql

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class CriterionTest {
    private val f = Field("col")

    @Test fun andTwoCriteria() {
        val result = Criterion.and(f.eq(1), f.eq(2)).toString()
        assertTrue(result.contains("AND"))
        assertTrue(result.contains("col=1"))
        assertTrue(result.contains("col=2"))
    }

    @Test fun andThreeCriteria() {
        val result = Criterion.and(f.eq(1), f.eq(2), f.eq(3)).toString()
        assertEquals("((col=1) AND (col=2) AND (col=3))", result)
    }

    @Test fun orTwoCriteria() {
        val result = Criterion.or(f.eq(1), f.eq(2)).toString()
        assertEquals("((col=1) OR (col=2))", result)
    }

    @Test fun existsQuery() {
        val q = Query.select(f).from(org.tasks.data.db.Table("t")).where(f.eq(1))
        val result = Criterion.exists(q).toString()
        assertTrue(result.startsWith("(EXISTS ("))
    }
}
