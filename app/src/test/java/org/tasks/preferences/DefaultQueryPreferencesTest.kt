package org.tasks.preferences

import com.todoroo.astrid.core.SortHelper
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class DefaultQueryPreferencesTest {

    private lateinit var prefs: DefaultQueryPreferences

    @Before
    fun setUp() {
        prefs = DefaultQueryPreferences()
    }

    // --- default values ---

    @Test
    fun defaultSortModeIsSortDue() {
        assertEquals(SortHelper.SORT_DUE, prefs.sortMode)
    }

    @Test
    fun defaultGroupModeIsSortDue() {
        assertEquals(SortHelper.SORT_DUE, prefs.groupMode)
    }

    @Test
    fun defaultCompletedModeIsSortAuto() {
        assertEquals(SortHelper.SORT_AUTO, prefs.completedMode)
    }

    @Test
    fun defaultSubtaskModeIsSortManual() {
        assertEquals(SortHelper.SORT_MANUAL, prefs.subtaskMode)
    }

    @Test
    fun defaultIsManualSortFalse() {
        assertFalse(prefs.isManualSort)
    }

    @Test
    fun defaultIsAstridSortFalse() {
        assertFalse(prefs.isAstridSort)
    }

    @Test
    fun defaultSortAscendingTrue() {
        assertTrue(prefs.sortAscending)
    }

    @Test
    fun defaultGroupAscendingTrue() {
        assertTrue(prefs.groupAscending)
    }

    @Test
    fun defaultCompletedAscendingFalse() {
        assertFalse(prefs.completedAscending)
    }

    @Test
    fun defaultSubtaskAscendingFalse() {
        assertFalse(prefs.subtaskAscending)
    }

    @Test
    fun defaultShowHiddenFalse() {
        assertFalse(prefs.showHidden)
    }

    @Test
    fun defaultShowCompletedTrue() {
        assertTrue(prefs.showCompleted)
    }

    @Test
    fun defaultAlwaysDisplayFullDateFalse() {
        assertFalse(prefs.alwaysDisplayFullDate)
    }

    @Test
    fun defaultCompletedTasksAtBottomTrue() {
        assertTrue(prefs.completedTasksAtBottom)
    }

    // --- setters ---

    @Test
    fun setSortMode() {
        prefs.sortMode = SortHelper.SORT_ALPHA
        assertEquals(SortHelper.SORT_ALPHA, prefs.sortMode)
    }

    @Test
    fun setGroupMode() {
        prefs.groupMode = SortHelper.SORT_IMPORTANCE
        assertEquals(SortHelper.SORT_IMPORTANCE, prefs.groupMode)
    }

    @Test
    fun setCompletedMode() {
        prefs.completedMode = SortHelper.SORT_COMPLETED
        assertEquals(SortHelper.SORT_COMPLETED, prefs.completedMode)
    }

    @Test
    fun setSubtaskMode() {
        prefs.subtaskMode = SortHelper.SORT_DUE
        assertEquals(SortHelper.SORT_DUE, prefs.subtaskMode)
    }

    @Test
    fun setIsManualSort() {
        prefs.isManualSort = true
        assertTrue(prefs.isManualSort)
    }

    @Test
    fun setIsAstridSort() {
        prefs.isAstridSort = true
        assertTrue(prefs.isAstridSort)
    }

    @Test
    fun setSortAscending() {
        prefs.sortAscending = false
        assertFalse(prefs.sortAscending)
    }

    @Test
    fun setGroupAscending() {
        prefs.groupAscending = false
        assertFalse(prefs.groupAscending)
    }

    @Test
    fun setCompletedAscending() {
        prefs.completedAscending = true
        assertTrue(prefs.completedAscending)
    }

    @Test
    fun setSubtaskAscending() {
        prefs.subtaskAscending = true
        assertTrue(prefs.subtaskAscending)
    }

    @Test
    fun setCompletedTasksAtBottom() {
        prefs.completedTasksAtBottom = false
        assertFalse(prefs.completedTasksAtBottom)
    }

    // --- multiple set operations ---

    @Test
    fun setAndResetSortMode() {
        prefs.sortMode = SortHelper.SORT_ALPHA
        assertEquals(SortHelper.SORT_ALPHA, prefs.sortMode)

        prefs.sortMode = SortHelper.SORT_CREATED
        assertEquals(SortHelper.SORT_CREATED, prefs.sortMode)
    }

    @Test
    fun setMultipleProperties() {
        prefs.sortMode = SortHelper.SORT_ALPHA
        prefs.groupMode = SortHelper.SORT_IMPORTANCE
        prefs.sortAscending = false
        prefs.isManualSort = true

        assertEquals(SortHelper.SORT_ALPHA, prefs.sortMode)
        assertEquals(SortHelper.SORT_IMPORTANCE, prefs.groupMode)
        assertFalse(prefs.sortAscending)
        assertTrue(prefs.isManualSort)
    }

    // --- implements QueryPreferences ---

    @Test
    fun implementsQueryPreferencesInterface() {
        val queryPrefs: QueryPreferences = prefs
        assertEquals(SortHelper.SORT_DUE, queryPrefs.sortMode)
        assertTrue(queryPrefs.showCompleted)
    }

    @Test
    fun sortModeAcceptsSortAutoValue() {
        prefs.sortMode = SortHelper.SORT_AUTO
        assertEquals(SortHelper.SORT_AUTO, prefs.sortMode)
    }

    @Test
    fun sortModeAcceptsSortCreatedValue() {
        prefs.sortMode = SortHelper.SORT_CREATED
        assertEquals(SortHelper.SORT_CREATED, prefs.sortMode)
    }

    @Test
    fun sortModeAcceptsSortCompletedValue() {
        prefs.sortMode = SortHelper.SORT_COMPLETED
        assertEquals(SortHelper.SORT_COMPLETED, prefs.sortMode)
    }

    // --- independent instances ---

    @Test
    fun separateInstancesAreIndependent() {
        val prefs1 = DefaultQueryPreferences()
        val prefs2 = DefaultQueryPreferences()

        prefs1.sortMode = SortHelper.SORT_ALPHA
        prefs2.sortMode = SortHelper.SORT_IMPORTANCE

        assertEquals(SortHelper.SORT_ALPHA, prefs1.sortMode)
        assertEquals(SortHelper.SORT_IMPORTANCE, prefs2.sortMode)
    }

    @Test
    fun separateInstancesBooleanIndependence() {
        val prefs1 = DefaultQueryPreferences()
        val prefs2 = DefaultQueryPreferences()

        prefs1.isManualSort = true
        assertFalse(prefs2.isManualSort)
    }
}
