package org.tasks.data.sql

import org.junit.Assert.assertTrue
import org.junit.Test
import org.tasks.data.db.Table

class JoinTest {
    private val table = Table("tags")
    private val id = Field("_id")

    @Test fun innerJoin() {
        val j = Join.inner(table, id.eq(Field("tags.task")))
        val result = j.toString()
        assertTrue(result.contains("INNER JOIN tags"))
        assertTrue(result.contains("ON"))
    }

    @Test fun leftJoin() {
        val j = Join.left(table, id.eq(Field("tags.task")))
        val result = j.toString()
        assertTrue(result.contains("LEFT JOIN tags"))
    }

    @Test fun multipleCriteria() {
        val j = Join.inner(table, id.eq(Field("tags.task")), Field("deleted").eq(0))
        val result = j.toString()
        assertTrue(result.contains("AND"))
    }
}
