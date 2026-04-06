package org.tasks.data

import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.tasks.data.db.DbUtils
import org.tasks.data.db.DbUtils.dbchunk
import org.tasks.data.db.SuspendDbUtils.chunkedMap
import org.tasks.data.db.SuspendDbUtils.eachChunk
import org.tasks.data.db.Property
import org.tasks.data.db.Table

/**
 * Deep tests for DbUtils, SuspendDbUtils, Table, and Property.
 * These cover the SQLite arg chunking, table/property construction,
 * and the suspend utility functions.
 */
class DbUtilsDeepTest {

    // =============================================
    // DbUtils.MAX_SQLITE_ARGS constant
    // =============================================

    @Test
    fun maxSqliteArgsIs990() {
        assertEquals(990, DbUtils.MAX_SQLITE_ARGS)
    }

    // =============================================
    // dbchunk — chunking iterable into lists of MAX_SQLITE_ARGS
    // =============================================

    @Test
    fun dbchunkEmptyList() {
        val result = emptyList<Int>().dbchunk()
        assertTrue(result.isEmpty())
    }

    @Test
    fun dbchunkSingleElement() {
        val result = listOf(1).dbchunk()
        assertEquals(1, result.size)
        assertEquals(listOf(1), result[0])
    }

    @Test
    fun dbchunkUnderLimit() {
        val items = (1..100).toList()
        val result = items.dbchunk()
        assertEquals(1, result.size)
        assertEquals(100, result[0].size)
    }

    @Test
    fun dbchunkExactlyAtLimit() {
        val items = (1..990).toList()
        val result = items.dbchunk()
        assertEquals(1, result.size)
        assertEquals(990, result[0].size)
    }

    @Test
    fun dbchunkOneOverLimit() {
        val items = (1..991).toList()
        val result = items.dbchunk()
        assertEquals(2, result.size)
        assertEquals(990, result[0].size)
        assertEquals(1, result[1].size)
    }

    @Test
    fun dbchunkTwoChunks() {
        val items = (1..1980).toList()
        val result = items.dbchunk()
        assertEquals(2, result.size)
        assertEquals(990, result[0].size)
        assertEquals(990, result[1].size)
    }

    @Test
    fun dbchunkThreeChunks() {
        val items = (1..2000).toList()
        val result = items.dbchunk()
        assertEquals(3, result.size)
        assertEquals(990, result[0].size)
        assertEquals(990, result[1].size)
        assertEquals(20, result[2].size)
    }

    @Test
    fun dbchunkPreservesOrder() {
        val items = (1..2000).toList()
        val result = items.dbchunk().flatten()
        assertEquals(items, result)
    }

    @Test
    fun dbchunkWithStrings() {
        val items = (1..1000).map { "item_$it" }
        val result = items.dbchunk()
        assertEquals(2, result.size)
        assertEquals(990, result[0].size)
        assertEquals(10, result[1].size)
    }

    // =============================================
    // SuspendDbUtils.eachChunk — with default size
    // =============================================

    @Test
    fun eachChunkProcessesAllElements() = runTest {
        val items = (1..2000).toList()
        val processed = mutableListOf<Int>()

        items.eachChunk { chunk ->
            processed.addAll(chunk)
        }

        assertEquals(2000, processed.size)
        assertEquals(items, processed)
    }

    @Test
    fun eachChunkCallsActionCorrectNumberOfTimes() = runTest {
        val items = (1..2000).toList()
        var callCount = 0

        items.eachChunk { callCount++ }

        assertEquals(3, callCount) // 990 + 990 + 20
    }

    @Test
    fun eachChunkOnEmptyList() = runTest {
        var callCount = 0
        emptyList<Int>().eachChunk { callCount++ }
        assertEquals(0, callCount)
    }

    @Test
    fun eachChunkOnSingleElement() = runTest {
        val results = mutableListOf<List<Int>>()
        listOf(42).eachChunk { results.add(it) }

        assertEquals(1, results.size)
        assertEquals(listOf(42), results[0])
    }

    // =============================================
    // SuspendDbUtils.eachChunk — with custom size
    // =============================================

    @Test
    fun eachChunkWithCustomSize() = runTest {
        val items = (1..10).toList()
        val chunks = mutableListOf<List<Int>>()

        items.eachChunk(3) { chunks.add(it) }

        assertEquals(4, chunks.size) // 3 + 3 + 3 + 1
        assertEquals(listOf(1, 2, 3), chunks[0])
        assertEquals(listOf(4, 5, 6), chunks[1])
        assertEquals(listOf(7, 8, 9), chunks[2])
        assertEquals(listOf(10), chunks[3])
    }

    @Test
    fun eachChunkWithSizeOne() = runTest {
        val items = listOf(1, 2, 3)
        var callCount = 0

        items.eachChunk(1) { callCount++ }

        assertEquals(3, callCount)
    }

    @Test
    fun eachChunkWithSizeLargerThanList() = runTest {
        val items = listOf(1, 2, 3)
        val chunks = mutableListOf<List<Int>>()

        items.eachChunk(100) { chunks.add(it) }

        assertEquals(1, chunks.size)
        assertEquals(listOf(1, 2, 3), chunks[0])
    }

    // =============================================
    // SuspendDbUtils.chunkedMap
    // =============================================

    @Test
    fun chunkedMapTransformsAllElements() = runTest {
        val items = (1..2000).toList()

        val result = items.chunkedMap { chunk ->
            chunk.map { it * 2 }
        }

        assertEquals(2000, result.size)
        assertEquals(2, result[0])
        assertEquals(4000, result[1999])
    }

    @Test
    fun chunkedMapPreservesOrder() = runTest {
        val items = (1..2000).toList()

        val result = items.chunkedMap { chunk ->
            chunk.map { it.toString() }
        }

        assertEquals("1", result[0])
        assertEquals("990", result[989])
        assertEquals("991", result[990])
        assertEquals("2000", result[1999])
    }

    @Test
    fun chunkedMapEmptyList() = runTest {
        val result = emptyList<Int>().chunkedMap { it.map { x -> x * 2 } }
        assertTrue(result.isEmpty())
    }

    @Test
    fun chunkedMapSingleElement() = runTest {
        val result = listOf(5).chunkedMap { chunk ->
            chunk.map { it + 10 }
        }

        assertEquals(listOf(15), result)
    }

    @Test
    fun chunkedMapWithFiltering() = runTest {
        val items = (1..2000).toList()

        val result = items.chunkedMap { chunk ->
            chunk.filter { it % 2 == 0 }
        }

        assertEquals(1000, result.size)
        assertTrue(result.all { it % 2 == 0 })
    }

    @Test
    fun chunkedMapReturnsEmptyFromTransform() = runTest {
        val items = (1..100).toList()

        val result = items.chunkedMap { emptyList<Int>() }

        assertTrue(result.isEmpty())
    }

    // =============================================
    // Table
    // =============================================

    @Test
    fun tableConstructor() {
        val table = Table("tasks")
        assertEquals("tasks", table.toString())
    }

    @Test
    fun tableAlias() {
        val table = Table("tasks")
        val aliased = table.`as`("t")
        assertEquals("tasks AS t", aliased.toString())
    }

    @Test
    fun tableName() {
        val table = Table("tasks")
        assertEquals("tasks", table.name())
    }

    @Test
    fun tableNameWithAlias() {
        val table = Table("tasks")
        val aliased = table.`as`("t")
        assertEquals("t", aliased.name())
    }

    @Test
    fun tableColumn() {
        val table = Table("tasks")
        val column = table.column("title")
        assertEquals("title", column.name)
        assertEquals("tasks.title", column.toString())
    }

    @Test
    fun tableColumnWithAlias() {
        val table = Table("tasks")
        val aliased = table.`as`("t")
        val column = aliased.column("title")
        assertEquals("title", column.name)
        assertEquals("t.title", column.toString())
    }

    @Test
    fun tableColumnMultiple() {
        val table = Table("tasks")
        val idCol = table.column("_id")
        val titleCol = table.column("title")
        val dueCol = table.column("dueDate")

        assertEquals("tasks._id", idCol.toString())
        assertEquals("tasks.title", titleCol.toString())
        assertEquals("tasks.dueDate", dueCol.toString())
    }

    // =============================================
    // Property
    // =============================================

    @Test
    fun propertyFromTable() {
        val table = Table("tasks")
        val property = Property(table, "title")

        assertEquals("title", property.name)
        assertEquals("tasks.title", property.toString())
    }

    @Test
    fun propertyFromAliasedTable() {
        val table = Table("tasks").`as`("t")
        val property = Property(table, "completed")

        assertEquals("completed", property.name)
        assertEquals("t.completed", property.toString())
    }

    @Test
    fun propertyFromDifferentTables() {
        val tasks = Table("tasks")
        val tags = Table("tags")

        val taskTitle = tasks.column("title")
        val tagName = tags.column("name")

        assertEquals("tasks.title", taskTitle.toString())
        assertEquals("tags.name", tagName.toString())
    }
}
