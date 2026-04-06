package org.tasks.data.sql

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.tasks.data.db.Table

class QueryTest {
    private val table = Table("tasks")
    private val id = Field("_id")
    private val title = Field("title")

    @Test fun selectAll() {
        val q = Query.select().from(table).toString()
        assertTrue(q.contains("SELECT *"))
        assertTrue(q.contains("FROM tasks"))
    }

    @Test fun selectFields() {
        val q = Query.select(id, title).from(table).toString()
        assertTrue(q.contains("SELECT _id, title"))
    }

    @Test fun selectWithWhere() {
        val q = Query.select(id).from(table).where(id.eq(1)).toString()
        assertTrue(q.contains("WHERE"))
        assertTrue(q.contains("_id=1"))
    }

    @Test fun selectWithJoin() {
        val other = Table("tags")
        val q = Query.select(id).from(table)
            .join(Join.left(other, id.eq(Field("tags.task"))))
            .toString()
        assertTrue(q.contains("LEFT JOIN tags"))
    }

    @Test fun withQueryTemplate() {
        val q = Query.select(id).from(table)
            .withQueryTemplate("WHERE _id > 0 ORDER BY _id")
            .toString()
        assertTrue(q.contains("WHERE _id > 0"))
        assertTrue(q.contains("ORDER BY _id"))
    }

    @Test fun equalsOnSameQuery() {
        val q1 = Query.select(id).from(table)
        val q2 = Query.select(id).from(table)
        assertEquals(q1, q2)
    }

    @Test fun notEqualsDifferentQuery() {
        val q1 = Query.select(id).from(table)
        val q2 = Query.select(title).from(table)
        assertNotEquals(q1, q2)
    }

    @Test fun hashCodeConsistent() {
        val q1 = Query.select(id).from(table)
        val q2 = Query.select(id).from(table)
        assertEquals(q1.hashCode(), q2.hashCode())
    }
}
