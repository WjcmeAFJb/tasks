package org.tasks.tags

import androidx.compose.ui.state.ToggleableState
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import androidx.arch.core.executor.ArchTaskExecutor
import androidx.arch.core.executor.TaskExecutor
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.mockito.ArgumentMatchers.anyString
import org.mockito.Mockito.mock
import org.mockito.Mockito.`when`
import org.tasks.data.dao.TagDataDao
import org.tasks.data.entity.TagData

@OptIn(ExperimentalCoroutinesApi::class)
class TagPickerViewModelDeepTest {

    private lateinit var tagDataDao: TagDataDao
    private lateinit var viewModel: TagPickerViewModel
    private val testDispatcher = StandardTestDispatcher()

    @Before
    fun setUp() {
        ArchTaskExecutor.getInstance().setDelegate(object : TaskExecutor() {
            override fun executeOnDiskIO(runnable: Runnable) = runnable.run()
            override fun postToMainThread(runnable: Runnable) = runnable.run()
            override fun isMainThread() = true
        })
        Dispatchers.setMain(testDispatcher)
        tagDataDao = mock()
        viewModel = TagPickerViewModel(tagDataDao)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
        ArchTaskExecutor.getInstance().setDelegate(null)
    }

    // ===== Initial state =====

    @Test
    fun initialSearchTextIsEmpty() {
        assertEquals("", viewModel.searchText.value)
    }

    @Test
    fun initialTagToCreateIsEmpty() {
        assertEquals("", viewModel.tagToCreate.value)
    }

    @Test
    fun initialSelectedIsEmpty() {
        assertTrue(viewModel.getSelected().isEmpty())
    }

    @Test
    fun initialPartiallySelectedIsEmpty() {
        assertTrue(viewModel.getPartiallySelected().isEmpty())
    }

    // ===== setSelected =====

    @Test
    fun setSelected_addsAllSelected() {
        val t1 = TagData(id = 1, name = "Work")
        val t2 = TagData(id = 2, name = "Home")
        val t3 = TagData(id = 3, name = "Urgent")
        viewModel.setSelected(listOf(t1, t2, t3), null)
        assertEquals(3, viewModel.getSelected().size)
    }

    @Test
    fun setSelected_addsPartial() {
        val selected = TagData(id = 1, name = "Work")
        val partial = TagData(id = 2, name = "Home")
        viewModel.setSelected(listOf(selected), listOf(partial))
        assertEquals(1, viewModel.getSelected().size)
        assertEquals(1, viewModel.getPartiallySelected().size)
    }

    @Test
    fun setSelected_nullPartial_doesNotCrash() {
        viewModel.setSelected(listOf(TagData(id = 1, name = "A")), null)
        assertEquals(0, viewModel.getPartiallySelected().size)
    }

    @Test
    fun setSelected_emptyPartial() {
        viewModel.setSelected(listOf(TagData(id = 1, name = "A")), emptyList())
        assertEquals(0, viewModel.getPartiallySelected().size)
    }

    @Test
    fun setSelected_accumulates() {
        viewModel.setSelected(listOf(TagData(id = 1, name = "A")), null)
        viewModel.setSelected(listOf(TagData(id = 2, name = "B")), null)
        assertEquals(2, viewModel.getSelected().size)
    }

    @Test
    fun setSelected_duplicateNotAdded() {
        val tag = TagData(id = 1, name = "A")
        viewModel.setSelected(listOf(tag), null)
        viewModel.setSelected(listOf(tag), null)
        assertEquals(1, viewModel.getSelected().size)
    }

    // ===== getState =====

    @Test
    fun getState_off_whenNotSelected() {
        assertEquals(ToggleableState.Off, viewModel.getState(TagData(id = 1, name = "A")))
    }

    @Test
    fun getState_on_whenSelected() {
        val tag = TagData(id = 1, name = "A")
        viewModel.setSelected(listOf(tag), null)
        assertEquals(ToggleableState.On, viewModel.getState(tag))
    }

    @Test
    fun getState_indeterminate_whenPartiallySelected() {
        val tag = TagData(id = 1, name = "A")
        viewModel.setSelected(emptyList(), listOf(tag))
        assertEquals(ToggleableState.Indeterminate, viewModel.getState(tag))
    }

    @Test
    fun getState_indeterminate_takesPreference() {
        val tag = TagData(id = 1, name = "A")
        viewModel.setSelected(listOf(tag), listOf(tag))
        assertEquals(ToggleableState.Indeterminate, viewModel.getState(tag))
    }

    // ===== toggle =====

    @Test
    fun toggle_onAddsToSelected() = runTest {
        val tag = TagData(id = 1, name = "Work")
        val result = viewModel.toggle(tag, true)
        assertEquals(ToggleableState.On, result)
        assertTrue(viewModel.getSelected().contains(tag))
    }

    @Test
    fun toggle_offRemovesFromSelected() = runTest {
        val tag = TagData(id = 1, name = "Work")
        viewModel.setSelected(listOf(tag), null)
        val result = viewModel.toggle(tag, false)
        assertEquals(ToggleableState.Off, result)
        assertFalse(viewModel.getSelected().contains(tag))
    }

    @Test
    fun toggle_removesFromPartiallySelected() = runTest {
        val tag = TagData(id = 1, name = "Work")
        viewModel.setSelected(emptyList(), listOf(tag))
        assertEquals(ToggleableState.Indeterminate, viewModel.getState(tag))
        viewModel.toggle(tag, true)
        assertFalse(viewModel.getPartiallySelected().contains(tag))
        assertTrue(viewModel.getSelected().contains(tag))
    }

    @Test
    fun toggle_existingTag_on() = runTest {
        val tag = TagData(id = 10, name = "Existing")
        val result = viewModel.toggle(tag, true)
        assertEquals(ToggleableState.On, result)
    }

    @Test
    fun toggle_existingTag_off() = runTest {
        val tag = TagData(id = 10, name = "Existing")
        viewModel.setSelected(listOf(tag), null)
        val result = viewModel.toggle(tag, false)
        assertEquals(ToggleableState.Off, result)
        assertFalse(viewModel.getSelected().contains(tag))
    }

    // ===== search =====

    @Test
    fun search_emptyString_updatesSearchText() = runTest {
        `when`(tagDataDao.searchTagsInternal(anyString())).thenReturn(emptyList())
        viewModel.search("")
        testDispatcher.scheduler.advanceUntilIdle()
        assertEquals("", viewModel.searchText.value)
    }

    @Test
    fun search_nonEmpty_updatesSearchText() = runTest {
        `when`(tagDataDao.searchTagsInternal(anyString())).thenReturn(emptyList())
        viewModel.search("work")
        testDispatcher.scheduler.advanceUntilIdle()
        assertEquals("work", viewModel.searchText.value)
    }

    @Test
    fun search_matchingTag_doesNotSetTagToCreate() = runTest {
        val existingTag = TagData(id = 1, name = "Work")
        `when`(tagDataDao.searchTagsInternal(anyString())).thenReturn(listOf(existingTag))
        viewModel.search("Work")
        testDispatcher.scheduler.advanceUntilIdle()
        assertEquals("", viewModel.tagToCreate.value)
    }

    @Test
    fun search_noMatchingTag_setsTagToCreate() = runTest {
        `when`(tagDataDao.searchTagsInternal(anyString())).thenReturn(emptyList())
        viewModel.search("NewTag")
        testDispatcher.scheduler.advanceUntilIdle()
        assertEquals("NewTag", viewModel.tagToCreate.value)
    }

    @Test
    fun search_caseInsensitiveMatch_doesNotSetTagToCreate() = runTest {
        val existingTag = TagData(id = 1, name = "work")
        `when`(tagDataDao.searchTagsInternal(anyString())).thenReturn(listOf(existingTag))
        viewModel.search("WORK")
        testDispatcher.scheduler.advanceUntilIdle()
        assertEquals("", viewModel.tagToCreate.value)
    }

    @Test
    fun search_resultsAreSorted_selectedFirst() = runTest {
        val selectedTag = TagData(id = 1, name = "Selected")
        val unselectedTag = TagData(id = 2, name = "Other")
        viewModel.setSelected(listOf(selectedTag), null)
        `when`(tagDataDao.searchTagsInternal(anyString())).thenReturn(listOf(unselectedTag, selectedTag))
        viewModel.search("")
        testDispatcher.scheduler.advanceUntilIdle()
        val tags = viewModel.tagsList.value
        assertNotNull(tags)
        if (tags != null && tags.size == 2) {
            assertEquals(selectedTag, tags[0])
        }
    }

    // ===== tagsList =====

    @Test
    fun tagsListLiveDataAccessible() {
        assertNotNull(viewModel.tagsList)
    }

    // ===== TagPickerActivity constants =====

    @Test
    fun extraSelectedConstant() {
        assertEquals("extra_tags", TagPickerActivity.EXTRA_SELECTED)
    }

    @Test
    fun extraPartiallySelectedConstant() {
        assertEquals("extra_partial", TagPickerActivity.EXTRA_PARTIALLY_SELECTED)
    }

    @Test
    fun extraTasksConstant() {
        assertEquals("extra_tasks", TagPickerActivity.EXTRA_TASKS)
    }

    @Test
    fun allConstantsAreDistinct() {
        val constants = setOf(
            TagPickerActivity.EXTRA_SELECTED,
            TagPickerActivity.EXTRA_PARTIALLY_SELECTED,
            TagPickerActivity.EXTRA_TASKS,
        )
        assertEquals(3, constants.size)
    }
}
