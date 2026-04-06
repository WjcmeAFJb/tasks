package org.tasks.data.db

import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test
import org.tasks.data.db.SuspendDbUtils.chunkedMap
import org.tasks.data.db.SuspendDbUtils.eachChunk

class SuspendDbUtilsTest {
    @Test fun eachChunkSmall() = runTest {
        val collected = mutableListOf<List<Int>>()
        (1..5).toList().eachChunk { collected.add(it) }
        assertEquals(1, collected.size)
        assertEquals(5, collected[0].size)
    }

    @Test fun eachChunkCustomSize() = runTest {
        val collected = mutableListOf<List<Int>>()
        (1..10).toList().eachChunk(3) { collected.add(it) }
        assertEquals(4, collected.size)
        assertEquals(listOf(1, 2, 3), collected[0])
        assertEquals(listOf(4, 5, 6), collected[1])
        assertEquals(listOf(7, 8, 9), collected[2])
        assertEquals(listOf(10), collected[3])
    }

    @Test fun eachChunkEmpty() = runTest {
        val collected = mutableListOf<List<Int>>()
        emptyList<Int>().eachChunk { collected.add(it) }
        assertEquals(0, collected.size)
    }

    @Test fun chunkedMapSmall() = runTest {
        val result = (1..5).toList().chunkedMap { it.map { x -> x * 2 } }
        assertEquals(listOf(2, 4, 6, 8, 10), result)
    }

    @Test fun chunkedMapEmpty() = runTest {
        val result = emptyList<Int>().chunkedMap { it.map { x -> x * 2 } }
        assertEquals(emptyList<Int>(), result)
    }

    @Test fun chunkedMapLarge() = runTest {
        val input = (1..2000).toList()
        val result = input.chunkedMap { it }
        assertEquals(2000, result.size)
        assertEquals(input, result)
    }
}
