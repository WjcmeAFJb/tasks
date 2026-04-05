package org.tasks.preferences

import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.mockito.Mockito.mock
import org.mockito.Mockito.verify
import org.mockito.Mockito.`when`
import org.tasks.preferences.FilterPreferences.Companion.delete

class FilterPreferencesTest {

    private lateinit var globalPreferences: QueryPreferences
    private lateinit var tasksPreferences: TasksPreferences

    @Before
    fun setUp() {
        globalPreferences = mock(QueryPreferences::class.java)
        tasksPreferences = mock(TasksPreferences::class.java)
    }

    private fun createFilterPreferences(filterKey: String = "myFilter") =
        FilterPreferences(globalPreferences, tasksPreferences, filterKey)

    // --- sortMode ---

    @Test
    fun sortModeReadsFromTasksPreferences() = runTest {
        `when`(globalPreferences.sortMode).thenReturn(5)
        `when`(tasksPreferences.get(
            intPreferencesKey("filter_sort_myFilter_sort_mode"), 5
        )).thenReturn(10)

        val prefs = createFilterPreferences()
        assertEquals(10, prefs.sortMode)
    }

    @Test
    fun sortModeFallsBackToGlobal() = runTest {
        `when`(globalPreferences.sortMode).thenReturn(5)
        `when`(tasksPreferences.get(
            intPreferencesKey("filter_sort_myFilter_sort_mode"), 5
        )).thenReturn(5)

        val prefs = createFilterPreferences()
        assertEquals(5, prefs.sortMode)
    }

    @Test
    fun sortModeSetWritesToTasksPreferences() = runTest {
        val prefs = createFilterPreferences()
        prefs.sortMode = 42

        verify(tasksPreferences).set(
            intPreferencesKey("filter_sort_myFilter_sort_mode"), 42
        )
    }

    // --- groupMode ---

    @Test
    fun groupModeReadsFromTasksPreferences() = runTest {
        `when`(globalPreferences.groupMode).thenReturn(1)
        `when`(tasksPreferences.get(
            intPreferencesKey("filter_sort_myFilter_group_mode"), 1
        )).thenReturn(3)

        assertEquals(3, createFilterPreferences().groupMode)
    }

    @Test
    fun groupModeSetWritesToTasksPreferences() = runTest {
        createFilterPreferences().groupMode = 7

        verify(tasksPreferences).set(
            intPreferencesKey("filter_sort_myFilter_group_mode"), 7
        )
    }

    // --- completedMode ---

    @Test
    fun completedModeReadsCorrectKey() = runTest {
        `when`(globalPreferences.completedMode).thenReturn(0)
        `when`(tasksPreferences.get(
            intPreferencesKey("filter_sort_myFilter_completed_mode"), 0
        )).thenReturn(2)

        assertEquals(2, createFilterPreferences().completedMode)
    }

    // --- subtaskMode ---

    @Test
    fun subtaskModeReadsCorrectKey() = runTest {
        `when`(globalPreferences.subtaskMode).thenReturn(0)
        `when`(tasksPreferences.get(
            intPreferencesKey("filter_sort_myFilter_subtask_mode"), 0
        )).thenReturn(1)

        assertEquals(1, createFilterPreferences().subtaskMode)
    }

    // --- isManualSort ---

    @Test
    fun isManualSortReadsFromTasksPreferences() = runTest {
        `when`(globalPreferences.isManualSort).thenReturn(false)
        `when`(tasksPreferences.get(
            booleanPreferencesKey("filter_sort_myFilter_manual_sort"), false
        )).thenReturn(true)

        assertTrue(createFilterPreferences().isManualSort)
    }

    @Test
    fun isManualSortSetWritesToTasksPreferences() = runTest {
        createFilterPreferences().isManualSort = true

        verify(tasksPreferences).set(
            booleanPreferencesKey("filter_sort_myFilter_manual_sort"), true
        )
    }

    // --- isAstridSort ---

    @Test
    fun isAstridSortReadsFromTasksPreferences() = runTest {
        `when`(globalPreferences.isAstridSort).thenReturn(true)
        `when`(tasksPreferences.get(
            booleanPreferencesKey("filter_sort_myFilter_astrid_sort"), true
        )).thenReturn(false)

        assertFalse(createFilterPreferences().isAstridSort)
    }

    // --- sortAscending ---

    @Test
    fun sortAscendingReadsFromTasksPreferences() = runTest {
        `when`(globalPreferences.sortAscending).thenReturn(true)
        `when`(tasksPreferences.get(
            booleanPreferencesKey("filter_sort_myFilter_sort_ascending"), true
        )).thenReturn(false)

        assertFalse(createFilterPreferences().sortAscending)
    }

    // --- groupAscending ---

    @Test
    fun groupAscendingReadsFromTasksPreferences() = runTest {
        `when`(globalPreferences.groupAscending).thenReturn(false)
        `when`(tasksPreferences.get(
            booleanPreferencesKey("filter_sort_myFilter_group_ascending"), false
        )).thenReturn(true)

        assertTrue(createFilterPreferences().groupAscending)
    }

    // --- completedAscending ---

    @Test
    fun completedAscendingReadsFromTasksPreferences() = runTest {
        `when`(globalPreferences.completedAscending).thenReturn(false)
        `when`(tasksPreferences.get(
            booleanPreferencesKey("filter_sort_myFilter_completed_ascending"), false
        )).thenReturn(true)

        assertTrue(createFilterPreferences().completedAscending)
    }

    // --- subtaskAscending ---

    @Test
    fun subtaskAscendingReadsFromTasksPreferences() = runTest {
        `when`(globalPreferences.subtaskAscending).thenReturn(true)
        `when`(tasksPreferences.get(
            booleanPreferencesKey("filter_sort_myFilter_subtask_ascending"), true
        )).thenReturn(false)

        assertFalse(createFilterPreferences().subtaskAscending)
    }

    // --- completedTasksAtBottom ---

    @Test
    fun completedTasksAtBottomReadsFromTasksPreferences() = runTest {
        `when`(globalPreferences.completedTasksAtBottom).thenReturn(true)
        `when`(tasksPreferences.get(
            booleanPreferencesKey("filter_sort_myFilter_completed_at_bottom"), true
        )).thenReturn(false)

        assertFalse(createFilterPreferences().completedTasksAtBottom)
    }

    @Test
    fun completedTasksAtBottomSetWritesToTasksPreferences() = runTest {
        createFilterPreferences().completedTasksAtBottom = true

        verify(tasksPreferences).set(
            booleanPreferencesKey("filter_sort_myFilter_completed_at_bottom"), true
        )
    }

    // --- key formatting with different filterKeys ---

    @Test
    fun keyFormattingWithSpecialCharacters() = runTest {
        `when`(globalPreferences.sortMode).thenReturn(0)
        `when`(tasksPreferences.get(
            intPreferencesKey("filter_sort_list_acc_cal_sort_mode"), 0
        )).thenReturn(99)

        val prefs = FilterPreferences(globalPreferences, tasksPreferences, "list_acc_cal")
        assertEquals(99, prefs.sortMode)
    }

    @Test
    fun keyFormattingWithEmptyFilterKey() = runTest {
        `when`(globalPreferences.sortMode).thenReturn(0)
        `when`(tasksPreferences.get(
            intPreferencesKey("filter_sort__sort_mode"), 0
        )).thenReturn(7)

        val prefs = FilterPreferences(globalPreferences, tasksPreferences, "")
        assertEquals(7, prefs.sortMode)
    }

    // --- companion delete extension ---

    @Test
    fun deleteRemovesByPrefix() = runTest {
        tasksPreferences.delete("myFilter")

        verify(tasksPreferences).removeByPrefix("filter_sort_myFilter_")
    }

    @Test
    fun deleteWithCalendarStyleKey() = runTest {
        tasksPreferences.delete("list_acc1_cal1")

        verify(tasksPreferences).removeByPrefix("filter_sort_list_acc1_cal1_")
    }

    // --- delegation of non-overridden properties ---

    @Test
    fun showHiddenDelegatesToGlobal() {
        `when`(globalPreferences.showHidden).thenReturn(true)

        assertTrue(createFilterPreferences().showHidden)
    }

    @Test
    fun showCompletedDelegatesToGlobal() {
        `when`(globalPreferences.showCompleted).thenReturn(false)

        assertFalse(createFilterPreferences().showCompleted)
    }

    @Test
    fun alwaysDisplayFullDateDelegatesToGlobal() {
        `when`(globalPreferences.alwaysDisplayFullDate).thenReturn(true)

        assertTrue(createFilterPreferences().alwaysDisplayFullDate)
    }
}
