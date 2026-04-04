package org.tasks.data.sql

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.tasks.data.db.Table

class SqlTest {

    private val tasks = Table("tasks")
    private val tags = Table("tags")
    private val id = Field.field("tasks.id")
    private val title = Field.field("tasks.title")
    private val priority = Field.field("tasks.priority")
    private val tagId = Field.field("tags.task_id")

    // --- Field tests ---

    @Test
    fun fieldToString() {
        assertEquals("tasks.id", id.toString())
    }

    @Test
    fun fieldWithAlias() {
        val aliased = id.`as`("task_id")
        assertEquals("task_id", aliased.toString())
    }

    @Test
    fun fieldToStringInSelectWithAlias() {
        val aliased = title.`as`("task_title")
        assertEquals("tasks.title AS task_title", aliased.toStringInSelect())
    }

    @Test
    fun fieldToStringInSelectWithoutAlias() {
        // For dotted expressions without alias, auto-aliases to part after the dot
        assertEquals("tasks.title AS title", title.toStringInSelect())
    }

    @Test
    fun fieldToStringInSelectCountStar() {
        assertEquals("COUNT(*)", Field.COUNT.toStringInSelect())
    }

    @Test
    fun fieldEquality() {
        val f1 = Field.field("tasks.id")
        val f2 = Field.field("tasks.id")
        assertEquals(f1, f2)
    }

    @Test
    fun fieldInequalityDifferentExpression() {
        val f1 = Field.field("tasks.id")
        val f2 = Field.field("tasks.title")
        assertNotEquals(f1, f2)
    }

    // --- Criterion tests ---

    @Test
    fun eqCriterion() {
        val criterion = id.eq(42)
        assertEquals("(tasks.id=42)", criterion.toString())
    }

    @Test
    fun eqNullProducesIsNull() {
        val criterion = id.eq(null)
        assertEquals("(tasks.id IS NULL)", criterion.toString())
    }

    @Test
    fun eqStringValue() {
        val criterion = title.eq("hello")
        assertEquals("(tasks.title='hello')", criterion.toString())
    }

    @Test
    fun eqStringSanitizesSingleQuotes() {
        val criterion = title.eq("it's")
        assertEquals("(tasks.title='it''s')", criterion.toString())
    }

    @Test
    fun gtCriterion() {
        val criterion = priority.gt(3)
        assertEquals("(tasks.priority>3)", criterion.toString())
    }

    @Test
    fun ltCriterion() {
        val criterion = priority.lt(5)
        assertEquals("(tasks.priority<5)", criterion.toString())
    }

    @Test
    fun lteCriterion() {
        val criterion = priority.lte(5)
        assertEquals("(tasks.priority<=5)", criterion.toString())
    }

    @Test
    fun likeCriterion() {
        val criterion = title.like("%test%")
        assertEquals("(tasks.title LIKE '%test%')", criterion.toString())
    }

    @Test
    fun andCriterion() {
        val c1 = id.eq(1)
        val c2 = priority.gt(2)
        val and = Criterion.and(c1, c2)
        assertEquals("((tasks.id=1) AND (tasks.priority>2))", and.toString())
    }

    @Test
    fun orCriterion() {
        val c1 = id.eq(1)
        val c2 = id.eq(2)
        val or = Criterion.or(c1, c2)
        assertEquals("((tasks.id=1) OR (tasks.id=2))", or.toString())
    }

    @Test
    fun andMultipleCriterions() {
        val c1 = id.eq(1)
        val c2 = priority.gt(0)
        val c3 = title.like("%foo%")
        val and = Criterion.and(c1, c2, c3)
        assertEquals(
            "((tasks.id=1) AND (tasks.priority>0) AND (tasks.title LIKE '%foo%'))",
            and.toString()
        )
    }

    @Test
    fun fieldInList() {
        val criterion = id.`in`(listOf(1, 2, 3))
        assertEquals("(tasks.id IN (1,2,3))", criterion.toString())
    }

    // --- UnaryCriterion tests ---

    @Test
    fun isNotNull() {
        val criterion = UnaryCriterion.isNotNull(title)
        assertEquals("(tasks.title IS NOT NULL)", criterion.toString())
    }

    @Test
    fun isNull() {
        val criterion = UnaryCriterion.isNull(id)
        assertEquals("(tasks.id IS NULL)", criterion.toString())
    }

    @Test
    fun sanitizeSingleQuotes() {
        assertEquals("it''s", UnaryCriterion.sanitize("it's"))
    }

    @Test
    fun sanitizeNoQuotes() {
        assertEquals("hello", UnaryCriterion.sanitize("hello"))
    }

    // --- Order tests ---

    @Test
    fun ascOrder() {
        val order = Order.asc("tasks.title")
        assertEquals("tasks.title ASC", order.toString())
    }

    @Test
    fun descOrder() {
        val order = Order.desc("tasks.priority")
        assertEquals("tasks.priority DESC", order.toString())
    }

    @Test
    fun reverseAscToDesc() {
        val order = Order.asc("tasks.title")
        val reversed = order.reverse()
        assertEquals("tasks.title DESC", reversed.toString())
    }

    @Test
    fun reverseDescToAsc() {
        val order = Order.desc("tasks.title")
        val reversed = order.reverse()
        assertEquals("tasks.title ASC", reversed.toString())
    }

    @Test
    fun orderWithSecondaryExpression() {
        val primary = Order.asc("tasks.priority")
        val secondary = Order.desc("tasks.title")
        primary.addSecondaryExpression(secondary)
        assertEquals("tasks.priority ASC, tasks.title DESC", primary.toString())
    }

    // --- Join tests ---

    @Test
    fun innerJoin() {
        val join = Join.inner(tags, id.eq(tagId))
        assertEquals("INNER JOIN tags ON ((tasks.id=tags.task_id))", join.toString())
    }

    @Test
    fun leftJoin() {
        val join = Join.left(tags, id.eq(tagId))
        assertEquals("LEFT JOIN tags ON ((tasks.id=tags.task_id))", join.toString())
    }

    @Test
    fun joinWithMultipleCriterions() {
        val join = Join.inner(tags, id.eq(tagId), priority.gt(0))
        assertEquals(
            "INNER JOIN tags ON ((tasks.id=tags.task_id) AND (tasks.priority>0))",
            join.toString()
        )
    }

    // --- Table tests ---

    @Test
    fun tableToString() {
        assertEquals("tasks", tasks.toString())
    }

    @Test
    fun tableWithAlias() {
        val aliased = tasks.`as`("t")
        assertEquals("tasks AS t", aliased.toString())
    }

    @Test
    fun tableName() {
        assertEquals("tasks", tasks.name())
    }

    @Test
    fun tableAliasedName() {
        val aliased = tasks.`as`("t")
        assertEquals("t", aliased.name())
    }

    // --- Query tests ---

    @Test
    fun selectAllFromTable() {
        val sql = Query.select().from(tasks).toString()
        assertEquals("SELECT * FROM tasks ", sql)
    }

    @Test
    fun selectFieldsFromTable() {
        val sql = Query.select(id, title).from(tasks).toString()
        assertEquals("SELECT tasks.id AS id, tasks.title AS title FROM tasks ", sql)
    }

    @Test
    fun selectWithWhere() {
        val sql = Query.select(id).from(tasks).where(priority.gt(3)).toString()
        assertEquals("SELECT tasks.id AS id FROM tasks WHERE (tasks.priority>3) ", sql)
    }

    @Test
    fun selectWithJoin() {
        val join = Join.inner(tags, id.eq(tagId))
        val sql = Query.select(id).from(tasks).join(join).toString()
        assertEquals(
            "SELECT tasks.id AS id FROM tasks INNER JOIN tags ON ((tasks.id=tags.task_id)) ",
            sql
        )
    }

    @Test
    fun selectWithQueryTemplate() {
        val template = QueryTemplate()
            .where(priority.gt(0))
            .orderBy(Order.asc("tasks.title"))
            .toString()
        val sql = Query.select(id).from(tasks).withQueryTemplate(template).toString()
        assertTrue(sql.contains("WHERE"))
        assertTrue(sql.contains("ORDER BY"))
    }

    // --- QueryTemplate tests ---

    @Test
    fun queryTemplateWhere() {
        val template = QueryTemplate().where(id.eq(1)).toString()
        assertEquals("WHERE (tasks.id=1) ", template)
    }

    @Test
    fun queryTemplateOrderBy() {
        val template = QueryTemplate().orderBy(Order.desc("tasks.priority")).toString()
        assertEquals("ORDER BY tasks.priority DESC ", template)
    }

    @Test
    fun queryTemplateJoinAndWhere() {
        val join = Join.left(tags, id.eq(tagId))
        val template = QueryTemplate().join(join).where(priority.gt(0)).toString()
        assertEquals(
            "LEFT JOIN tags ON ((tasks.id=tags.task_id)) WHERE (tasks.priority>0) ",
            template
        )
    }

    // --- Functions tests ---

    @Test
    fun upperFunction() {
        val upper = Functions.upper(title)
        assertEquals("UPPER(tasks.title)", upper.toString())
    }

    @Test
    fun nowFunction() {
        val now = Functions.now()
        assertEquals("(strftime('%s','now')*1000)", now.toString())
    }

    // --- Exists tests ---

    @Test
    fun existsCriterion() {
        val subquery = Query.select(id).from(tags).where(tagId.eq(1))
        val exists = Criterion.exists(subquery)
        assertTrue(exists.toString().startsWith("(EXISTS ("))
    }

    // --- Field.in(Query) ---

    @Test
    fun fieldInQuery() {
        val subquery = Query.select(tagId).from(tags)
        val criterion = id.`in`(subquery)
        assertTrue(criterion.toString().contains("IN ("))
        assertTrue(criterion.toString().contains("SELECT"))
    }

    // --- Query equality ---

    @Test
    fun queryEquality() {
        val q1 = Query.select(id).from(tasks).where(priority.gt(0))
        val q2 = Query.select(id).from(tasks).where(priority.gt(0))
        assertEquals(q1, q2)
    }

    @Test
    fun queryInequality() {
        val q1 = Query.select(id).from(tasks).where(priority.gt(0))
        val q2 = Query.select(id).from(tasks).where(priority.lt(0))
        assertNotEquals(q1, q2)
    }

    @Test
    fun queryHashCodeConsistentWithEquals() {
        val q1 = Query.select(id).from(tasks).where(priority.gt(0))
        val q2 = Query.select(id).from(tasks).where(priority.gt(0))
        assertEquals(q1.hashCode(), q2.hashCode())
    }

    // --- Table column helper ---

    @Test
    fun tableColumnCreatesProperty() {
        val prop = tasks.column("title")
        assertEquals("tasks.title", prop.expression)
        assertEquals("title", prop.name)
    }
}
