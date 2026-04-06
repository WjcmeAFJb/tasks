package org.tasks.dialogs

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Before
import org.junit.Test
import org.tasks.data.entity.Task

class PriorityPickerViewModelDeepTest {

    private lateinit var viewModel: PriorityPickerViewModel

    @Before
    fun setUp() {
        viewModel = PriorityPickerViewModel()
    }

    // ===== Initial state =====

    @Test
    fun initialPriorityIsNone() {
        assertEquals(Task.Priority.NONE, viewModel.priority.value)
    }

    @Test
    fun initialPriorityFlowEmitsNone() {
        assertEquals(Task.Priority.NONE, viewModel.priority.value)
    }

    // ===== setPriority transitions =====

    @Test
    fun setPriorityNoneToHigh() {
        viewModel.setPriority(Task.Priority.HIGH)
        assertEquals(Task.Priority.HIGH, viewModel.priority.value)
    }

    @Test
    fun setPriorityHighToMedium() {
        viewModel.setPriority(Task.Priority.HIGH)
        viewModel.setPriority(Task.Priority.MEDIUM)
        assertEquals(Task.Priority.MEDIUM, viewModel.priority.value)
    }

    @Test
    fun setPriorityMediumToLow() {
        viewModel.setPriority(Task.Priority.MEDIUM)
        viewModel.setPriority(Task.Priority.LOW)
        assertEquals(Task.Priority.LOW, viewModel.priority.value)
    }

    @Test
    fun setPriorityLowToNone() {
        viewModel.setPriority(Task.Priority.LOW)
        viewModel.setPriority(Task.Priority.NONE)
        assertEquals(Task.Priority.NONE, viewModel.priority.value)
    }

    @Test
    fun setPriorityNoneToNone() {
        viewModel.setPriority(Task.Priority.NONE)
        assertEquals(Task.Priority.NONE, viewModel.priority.value)
    }

    @Test
    fun setPriorityHighToHigh() {
        viewModel.setPriority(Task.Priority.HIGH)
        viewModel.setPriority(Task.Priority.HIGH)
        assertEquals(Task.Priority.HIGH, viewModel.priority.value)
    }

    // ===== All priority values =====

    @Test
    fun allPriorityValuesCanBeSet() {
        val priorities = listOf(
            Task.Priority.HIGH,
            Task.Priority.MEDIUM,
            Task.Priority.LOW,
            Task.Priority.NONE,
        )
        for (p in priorities) {
            viewModel.setPriority(p)
            assertEquals(p, viewModel.priority.value)
        }
    }

    // ===== Priority constants =====

    @Test
    fun highPriorityValue() {
        assertEquals(0, Task.Priority.HIGH)
    }

    @Test
    fun mediumPriorityValue() {
        assertEquals(1, Task.Priority.MEDIUM)
    }

    @Test
    fun lowPriorityValue() {
        assertEquals(2, Task.Priority.LOW)
    }

    @Test
    fun nonePriorityValue() {
        assertEquals(3, Task.Priority.NONE)
    }

    // ===== Priority ordering =====

    @Test
    fun highIsLowestNumericValue() {
        assert(Task.Priority.HIGH < Task.Priority.MEDIUM)
        assert(Task.Priority.MEDIUM < Task.Priority.LOW)
        assert(Task.Priority.LOW < Task.Priority.NONE)
    }

    // ===== Arbitrary int values =====

    @Test
    fun setPriorityWithArbitraryValue() {
        viewModel.setPriority(99)
        assertEquals(99, viewModel.priority.value)
    }

    @Test
    fun setPriorityWithNegativeValue() {
        viewModel.setPriority(-1)
        assertEquals(-1, viewModel.priority.value)
    }

    @Test
    fun setPriorityWithZero() {
        viewModel.setPriority(0)
        assertEquals(0, viewModel.priority.value)
    }

    // ===== Rapid changes =====

    @Test
    fun rapidPriorityChangesSettleToLast() {
        for (i in 0..100) {
            viewModel.setPriority(i % 4)
        }
        assertEquals(0, viewModel.priority.value) // 100 % 4 = 0
    }
}
