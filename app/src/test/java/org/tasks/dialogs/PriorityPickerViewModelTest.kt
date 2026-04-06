package org.tasks.dialogs

import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.tasks.data.entity.Task

class PriorityPickerViewModelTest {

    private lateinit var viewModel: PriorityPickerViewModel

    @Before
    fun setUp() {
        viewModel = PriorityPickerViewModel()
    }

    @Test
    fun initialPriorityIsNone() {
        assertEquals(Task.Priority.NONE, viewModel.priority.value)
    }

    @Test
    fun setPriorityToHigh() {
        viewModel.setPriority(Task.Priority.HIGH)
        assertEquals(Task.Priority.HIGH, viewModel.priority.value)
    }

    @Test
    fun setPriorityToMedium() {
        viewModel.setPriority(Task.Priority.MEDIUM)
        assertEquals(Task.Priority.MEDIUM, viewModel.priority.value)
    }

    @Test
    fun setPriorityToLow() {
        viewModel.setPriority(Task.Priority.LOW)
        assertEquals(Task.Priority.LOW, viewModel.priority.value)
    }

    @Test
    fun setPriorityToNone() {
        viewModel.setPriority(Task.Priority.HIGH)
        viewModel.setPriority(Task.Priority.NONE)
        assertEquals(Task.Priority.NONE, viewModel.priority.value)
    }

    @Test
    fun setPriorityMultipleTimes() {
        viewModel.setPriority(Task.Priority.HIGH)
        viewModel.setPriority(Task.Priority.LOW)
        viewModel.setPriority(Task.Priority.MEDIUM)
        assertEquals(Task.Priority.MEDIUM, viewModel.priority.value)
    }

    @Test
    fun setPrioritySameValueTwice() {
        viewModel.setPriority(Task.Priority.HIGH)
        viewModel.setPriority(Task.Priority.HIGH)
        assertEquals(Task.Priority.HIGH, viewModel.priority.value)
    }
}
