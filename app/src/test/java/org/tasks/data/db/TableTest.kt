package org.tasks.data.db

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Test
import org.tasks.data.db.DbUtils.dbchunk

class TableTest {
    @Test fun name() = assertEquals("tasks", Table("tasks").name())
    @Test fun toStringNoAlias() = assertEquals("tasks", Table("tasks").toString())
    @Test fun withAlias() {
        val t = Table("tasks").`as`("t")
        assertEquals("tasks AS t", t.toString())
    }
    @Test fun nameWithAlias() = assertEquals("t", Table("tasks").`as`("t").name())
    @Test fun column() {
        val col = Table("tasks").column("_id")
        assertEquals("_id", col.name)
        assertEquals("tasks._id", col.expression)
    }
    @Test fun columnWithAlias() {
        val col = Table("tasks").`as`("t").column("_id")
        assertEquals("t._id", col.expression)
    }
    @Test fun equals() = assertEquals(Table("tasks"), Table("tasks"))
    @Test fun notEquals() = assertNotEquals(Table("a"), Table("b"))
    @Test fun hashCodeConsistent() = assertEquals(Table("x").hashCode(), Table("x").hashCode())
}

class PropertyTest {
    @Test fun fromTable() {
        val table = Table("tasks")
        val p = Property(table, "title")
        assertEquals("title", p.name)
        assertEquals("tasks.title", p.expression)
    }
    @Test fun propertyToString() {
        val table = Table("tasks")
        val p = Property(table, "title")
        assertEquals("tasks.title", p.toString())
    }
}

class DbUtilsTest {
    @Test fun maxArgs() = assertEquals(990, DbUtils.MAX_SQLITE_ARGS)

    @Test fun dbchunkSmallList() {
        val chunks = (1..10).toList().dbchunk()
        assertEquals(1, chunks.size)
        assertEquals(10, chunks[0].size)
    }

    @Test fun dbchunkLargeList() {
        val chunks = (1..2000).toList().dbchunk()
        assertEquals(3, chunks.size)
        assertEquals(990, chunks[0].size)
        assertEquals(990, chunks[1].size)
        assertEquals(20, chunks[2].size)
    }

    @Test fun dbchunkEmpty() = assertEquals(0, emptyList<Int>().dbchunk().size)

    @Test fun dbchunkExactSize() = assertEquals(1, (1..990).toList().dbchunk().size)

    @Test fun dbchunkOnePastMax() {
        val chunks = (1..991).toList().dbchunk()
        assertEquals(2, chunks.size)
        assertEquals(990, chunks[0].size)
        assertEquals(1, chunks[1].size)
    }
}
