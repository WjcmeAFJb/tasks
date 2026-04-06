package org.tasks.data.sql

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.tasks.data.db.Table

class QueryTemplateTest {
    private val table = Table("tasks")
    private val id = Field("_id")

    @Test fun emptyTemplate() {
        assertEquals("", QueryTemplate().toString())
    }

    @Test fun whereOnly() {
        val t = QueryTemplate().where(id.eq(1)).toString()
        assertTrue(t.contains("WHERE"))
        assertTrue(t.contains("_id=1"))
    }

    @Test fun joinOnly() {
        val t = QueryTemplate()
            .join(Join.inner(table, id.eq(Field("other.id"))))
            .toString()
        assertTrue(t.contains("INNER JOIN tasks"))
    }

    @Test fun orderByOnly() {
        val t = QueryTemplate().orderBy(Order.asc("_id")).toString()
        assertTrue(t.contains("ORDER BY _id ASC"))
    }

    @Test fun fullTemplate() {
        val t = QueryTemplate()
            .join(Join.left(table, id.eq(Field("t2.id"))))
            .where(id.gt(0))
            .orderBy(Order.desc("_id"))
            .toString()
        assertTrue(t.contains("LEFT JOIN tasks"))
        assertTrue(t.contains("WHERE"))
        assertTrue(t.contains("ORDER BY _id DESC"))
    }
}
