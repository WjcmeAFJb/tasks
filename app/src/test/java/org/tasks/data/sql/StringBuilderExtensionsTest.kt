package org.tasks.data.sql

import org.junit.Assert.assertEquals
import org.junit.Test
import org.tasks.data.db.Table
import org.tasks.data.sql.StringBuilderExtensions.from
import org.tasks.data.sql.StringBuilderExtensions.join
import org.tasks.data.sql.StringBuilderExtensions.orderBy
import org.tasks.data.sql.StringBuilderExtensions.select
import org.tasks.data.sql.StringBuilderExtensions.where

class StringBuilderExtensionsTest {

    private val tasks = Table("tasks")
    private val tags = Table("tags")
    private val id = Field.field("tasks.id")
    private val tagId = Field.field("tags.task_id")

    // --- join ---

    @Test
    fun joinWithEmptyList() {
        val sb = StringBuilder()
        sb.join(emptyList())
        assertEquals("", sb.toString())
    }

    @Test
    fun joinWithSingleJoin() {
        val sb = StringBuilder()
        val j = Join.inner(tags, id.eq(tagId))
        sb.join(listOf(j))
        assertEquals("INNER JOIN tags ON ((tasks.id=tags.task_id)) ", sb.toString())
    }

    @Test
    fun joinWithMultipleJoins() {
        val sb = StringBuilder()
        val other = Table("other")
        val j1 = Join.inner(tags, id.eq(tagId))
        val j2 = Join.left(other, id.eq(Field.field("other.task_id")))
        sb.join(listOf(j1, j2))
        assertEquals(
            "INNER JOIN tags ON ((tasks.id=tags.task_id)) LEFT JOIN other ON ((tasks.id=other.task_id)) ",
            sb.toString()
        )
    }

    @Test
    fun joinReturnsSameStringBuilder() {
        val sb = StringBuilder()
        val result = sb.join(emptyList())
        assert(sb === result)
    }

    // --- where ---

    @Test
    fun whereWithEmptyList() {
        val sb = StringBuilder()
        sb.where(emptyList())
        assertEquals("", sb.toString())
    }

    @Test
    fun whereWithSingleCriterion() {
        val sb = StringBuilder()
        sb.where(listOf(id.eq(1)))
        assertEquals("WHERE (tasks.id=1) ", sb.toString())
    }

    @Test
    fun whereWithMultipleCriteria() {
        val sb = StringBuilder()
        sb.where(listOf(id.eq(1), id.gt(0)))
        assertEquals("WHERE (tasks.id=1) (tasks.id>0) ", sb.toString())
    }

    @Test
    fun whereReturnsSameStringBuilder() {
        val sb = StringBuilder()
        val result = sb.where(emptyList())
        assert(sb === result)
    }

    // --- orderBy ---

    @Test
    fun orderByWithEmptyList() {
        val sb = StringBuilder()
        sb.orderBy(emptyList())
        assertEquals("", sb.toString())
    }

    @Test
    fun orderByWithSingleOrder() {
        val sb = StringBuilder()
        sb.orderBy(listOf(Order.asc("tasks.title")))
        assertEquals("ORDER BY tasks.title ASC ", sb.toString())
    }

    @Test
    fun orderByWithMultipleOrders() {
        val sb = StringBuilder()
        sb.orderBy(listOf(Order.asc("tasks.title"), Order.desc("tasks.priority")))
        assertEquals("ORDER BY tasks.title ASC,tasks.priority DESC ", sb.toString())
    }

    @Test
    fun orderByReturnsSameStringBuilder() {
        val sb = StringBuilder()
        val result = sb.orderBy(emptyList())
        assert(sb === result)
    }

    // --- from ---

    @Test
    fun fromWithNullTable() {
        val sb = StringBuilder()
        sb.from(null)
        assertEquals("", sb.toString())
    }

    @Test
    fun fromWithTable() {
        val sb = StringBuilder()
        sb.from(tasks)
        assertEquals("FROM tasks ", sb.toString())
    }

    @Test
    fun fromWithAliasedTable() {
        val sb = StringBuilder()
        sb.from(tasks.`as`("t"))
        assertEquals("FROM tasks AS t ", sb.toString())
    }

    @Test
    fun fromReturnsSameStringBuilder() {
        val sb = StringBuilder()
        val result = sb.from(null)
        assert(sb === result)
    }

    // --- select ---

    @Test
    fun selectWithEmptyFields() {
        val sb = StringBuilder()
        sb.select(emptyList())
        assertEquals("SELECT * ", sb.toString())
    }

    @Test
    fun selectWithSingleField() {
        val sb = StringBuilder()
        sb.select(listOf(Field.field("tasks.id")))
        assertEquals("SELECT tasks.id AS id ", sb.toString())
    }

    @Test
    fun selectWithMultipleFields() {
        val sb = StringBuilder()
        sb.select(listOf(Field.field("tasks.id"), Field.field("tasks.title")))
        assertEquals("SELECT tasks.id AS id, tasks.title AS title ", sb.toString())
    }

    @Test
    fun selectWithAliasedField() {
        val sb = StringBuilder()
        sb.select(listOf(Field.field("tasks.id").`as`("task_id")))
        assertEquals("SELECT tasks.id AS task_id ", sb.toString())
    }

    @Test
    fun selectReturnsSameStringBuilder() {
        val sb = StringBuilder()
        val result = sb.select(emptyList())
        assert(sb === result)
    }

    // --- chaining ---

    @Test
    fun chainingAllExtensions() {
        val sb = StringBuilder()
        sb.select(listOf(id))
            .from(tasks)
            .join(listOf(Join.inner(tags, id.eq(tagId))))
            .where(listOf(id.gt(0)))
            .orderBy(listOf(Order.asc("tasks.id")))
        assertEquals(
            "SELECT tasks.id AS id FROM tasks " +
                "INNER JOIN tags ON ((tasks.id=tags.task_id)) " +
                "WHERE (tasks.id>0) " +
                "ORDER BY tasks.id ASC ",
            sb.toString()
        )
    }

    @Test
    fun selectStarWhenFieldsHaveEmptyToStringInSelect() {
        // COUNT(*) has no dot, so toStringInSelect returns "COUNT(*)"
        val sb = StringBuilder()
        sb.select(listOf(Field.COUNT))
        assertEquals("SELECT COUNT(*) ", sb.toString())
    }
}
