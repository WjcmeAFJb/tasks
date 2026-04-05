package org.tasks.sync

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class DebouncerTest {

    @Test
    fun singleCallExecutesAfterDelay() = runTest {
        val results = mutableListOf<Int>()
        val debouncer = Debouncer<Int>(
            tag = "test",
            default = 0,
            block = { results.add(it) }
        )

        launch { debouncer.sync(42) }
        advanceTimeBy(999)
        assertTrue(results.isEmpty())
        advanceTimeBy(2)
        assertEquals(listOf(42), results)
    }

    @Test
    fun rapidCallsDebouncesToLastValue() = runTest {
        val results = mutableListOf<String>()
        val debouncer = Debouncer<String>(
            tag = "test",
            default = "",
            block = { results.add(it) }
        )

        launch { debouncer.sync("first") }
        advanceTimeBy(500)
        launch { debouncer.sync("second") }
        advanceTimeBy(500)
        launch { debouncer.sync("third") }
        advanceTimeBy(1001)
        assertEquals(listOf("third"), results)
    }

    @Test
    fun noExecutionBeforeDelayExpires() = runTest {
        val results = mutableListOf<Int>()
        val debouncer = Debouncer<Int>(
            tag = "test",
            default = 0,
            block = { results.add(it) }
        )

        launch { debouncer.sync(1) }
        advanceTimeBy(500)
        assertTrue(results.isEmpty())
    }

    @Test
    fun twoSeparateCallsBothExecuteIfSpacedApart() = runTest {
        val results = mutableListOf<Int>()
        val debouncer = Debouncer<Int>(
            tag = "test",
            default = 0,
            block = { results.add(it) }
        )

        launch { debouncer.sync(1) }
        advanceTimeBy(1001)
        assertEquals(listOf(1), results)

        launch { debouncer.sync(2) }
        advanceTimeBy(1001)
        assertEquals(listOf(1, 2), results)
    }

    @Test
    fun defaultValueUsedForMergeInitialization() = runTest {
        val results = mutableListOf<Int>()
        val debouncer = Debouncer<Int>(
            tag = "test",
            default = 0,
            merge = { a, b -> a + b },
            block = { results.add(it) }
        )

        launch { debouncer.sync(5) }
        advanceTimeBy(1001)
        // merge(0, 5) = 5
        assertEquals(listOf(5), results)
    }

    @Test
    fun mergeAccumulatesMultipleValues() = runTest {
        val results = mutableListOf<Int>()
        val debouncer = Debouncer<Int>(
            tag = "test",
            default = 0,
            merge = { a, b -> a + b },
            block = { results.add(it) }
        )

        launch { debouncer.sync(1) }
        advanceTimeBy(500)
        launch { debouncer.sync(2) }
        advanceTimeBy(500)
        launch { debouncer.sync(3) }
        advanceTimeBy(1001)
        // merge(merge(merge(0, 1), 2), 3) = 6
        assertEquals(listOf(6), results)
    }

    @Test
    fun defaultMergeUsesLastValue() = runTest {
        val results = mutableListOf<String>()
        val debouncer = Debouncer<String>(
            tag = "test",
            default = "initial",
            block = { results.add(it) }
        )

        launch { debouncer.sync("a") }
        advanceTimeBy(500)
        launch { debouncer.sync("b") }
        advanceTimeBy(1001)
        assertEquals(listOf("b"), results)
    }

    @Test
    fun pendingResetAfterExecution() = runTest {
        val results = mutableListOf<Int>()
        val debouncer = Debouncer<Int>(
            tag = "test",
            default = 100,
            merge = { a, b -> a + b },
            block = { results.add(it) }
        )

        // First batch
        launch { debouncer.sync(5) }
        advanceTimeBy(1001)
        assertEquals(listOf(105), results) // merge(100, 5) = 105

        // Second batch: pending was reset to default (100)
        launch { debouncer.sync(3) }
        advanceTimeBy(1001)
        assertEquals(listOf(105, 103), results) // merge(100, 3) = 103
    }

    @Test
    fun mergeWithSetUnion() = runTest {
        val results = mutableListOf<Set<String>>()
        val debouncer = Debouncer<Set<String>>(
            tag = "test",
            default = emptySet(),
            merge = { a, b -> a + b },
            block = { results.add(it) }
        )

        launch { debouncer.sync(setOf("a", "b")) }
        advanceTimeBy(500)
        launch { debouncer.sync(setOf("b", "c")) }
        advanceTimeBy(1001)
        assertEquals(listOf(setOf("a", "b", "c")), results)
    }

    @Test
    fun tripleRapidCallsOnlyLastFires() = runTest {
        var callCount = 0
        val debouncer = Debouncer<Unit>(
            tag = "test",
            default = Unit,
            block = { callCount++ }
        )

        launch { debouncer.sync(Unit) }
        advanceTimeBy(300)
        launch { debouncer.sync(Unit) }
        advanceTimeBy(300)
        launch { debouncer.sync(Unit) }
        advanceTimeBy(1001)
        assertEquals(1, callCount)
    }

    @Test
    fun debouncerResetsAfterSuccessfulExecution() = runTest {
        val results = mutableListOf<Int>()
        val debouncer = Debouncer<Int>(
            tag = "test",
            default = 0,
            block = { results.add(it) }
        )

        launch { debouncer.sync(10) }
        advanceTimeBy(1001)

        launch { debouncer.sync(20) }
        advanceTimeBy(1001)

        launch { debouncer.sync(30) }
        advanceTimeBy(1001)

        assertEquals(listOf(10, 20, 30), results)
    }

    @Test
    fun onlyFinalValueInBurstIsEmitted() = runTest {
        val results = mutableListOf<Int>()
        val debouncer = Debouncer<Int>(
            tag = "test",
            default = 0,
            block = { results.add(it) }
        )

        // Fire 5 times rapidly
        for (i in 1..5) {
            launch { debouncer.sync(i) }
            advanceTimeBy(100)
        }
        advanceTimeBy(1001)
        // default merge keeps last, so result is 5
        assertEquals(listOf(5), results)
    }
}
