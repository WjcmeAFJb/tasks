package org.tasks.viewmodel

import com.todoroo.astrid.core.SortHelper
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.mockito.Mockito.mock
import org.mockito.Mockito.verify
import org.mockito.Mockito.`when`
import org.tasks.analytics.Reporting
import org.tasks.broadcast.RefreshBroadcaster
import org.tasks.preferences.QueryPreferences
import org.tasks.viewmodel.SortSettingsViewModel.Companion.toSortName

class SortSettingsViewModelTest {

    private lateinit var preferences: QueryPreferences
    private lateinit var reporting: Reporting
    private lateinit var refreshBroadcaster: RefreshBroadcaster
    private lateinit var viewModel: SortSettingsViewModel

    @Before
    fun setUp() {
        preferences = mock()
        reporting = mock()
        refreshBroadcaster = mock()

        `when`(preferences.isManualSort).thenReturn(false)
        `when`(preferences.isAstridSort).thenReturn(false)
        `when`(preferences.groupMode).thenReturn(SortHelper.GROUP_NONE)
        `when`(preferences.groupAscending).thenReturn(true)
        `when`(preferences.completedMode).thenReturn(SortHelper.SORT_COMPLETED)
        `when`(preferences.completedAscending).thenReturn(false)
        `when`(preferences.completedTasksAtBottom).thenReturn(true)
        `when`(preferences.sortMode).thenReturn(SortHelper.SORT_AUTO)
        `when`(preferences.sortAscending).thenReturn(true)
        `when`(preferences.subtaskMode).thenReturn(SortHelper.SORT_MANUAL)
        `when`(preferences.subtaskAscending).thenReturn(true)

        viewModel = SortSettingsViewModel(preferences, reporting, refreshBroadcaster)
    }

    // --- Initial state ---

    @Test
    fun initialStateMatchesPreferences() {
        val state = viewModel.state.value
        assertFalse(state.manualSort)
        assertFalse(state.astridSort)
        assertEquals(SortHelper.GROUP_NONE, state.groupMode)
        assertTrue(state.groupAscending)
        assertEquals(SortHelper.SORT_COMPLETED, state.completedMode)
        assertFalse(state.completedAscending)
        assertTrue(state.completedAtBottom)
        assertEquals(SortHelper.SORT_AUTO, state.sortMode)
        assertTrue(state.sortAscending)
        assertEquals(SortHelper.SORT_MANUAL, state.subtaskMode)
        assertTrue(state.subtaskAscending)
    }

    // --- setSortAscending ---

    @Test
    fun setSortAscendingUpdatesPrefAndState() {
        viewModel.setSortAscending(false)

        verify(preferences).sortAscending = false
        assertFalse(viewModel.state.value.sortAscending)
        verify(refreshBroadcaster).broadcastRefresh()
    }

    @Test
    fun setSortAscendingToTrue() {
        viewModel.setSortAscending(true)

        verify(preferences).sortAscending = true
        assertTrue(viewModel.state.value.sortAscending)
    }

    // --- setGroupAscending ---

    @Test
    fun setGroupAscendingUpdatesState() {
        viewModel.setGroupAscending(false)

        verify(preferences).groupAscending = false
        assertFalse(viewModel.state.value.groupAscending)
        verify(refreshBroadcaster).broadcastRefresh()
    }

    // --- setCompletedAscending ---

    @Test
    fun setCompletedAscendingUpdatesState() {
        viewModel.setCompletedAscending(true)

        verify(preferences).completedAscending = true
        assertTrue(viewModel.state.value.completedAscending)
    }

    // --- setSubtaskAscending ---

    @Test
    fun setSubtaskAscendingUpdatesState() {
        viewModel.setSubtaskAscending(false)

        verify(preferences).subtaskAscending = false
        assertFalse(viewModel.state.value.subtaskAscending)
    }

    // --- setCompletedAtBottom ---

    @Test
    fun setCompletedAtBottomUpdatesState() {
        viewModel.setCompletedAtBottom(false)

        verify(preferences).completedTasksAtBottom = false
        assertFalse(viewModel.state.value.completedAtBottom)
    }

    // --- setGroupMode ---

    @Test
    fun setGroupModeNoop_WhenSameMode() {
        // groupMode is initially GROUP_NONE
        viewModel.setGroupMode(SortHelper.GROUP_NONE)

        // Should not change state since it's the same
        assertEquals(SortHelper.GROUP_NONE, viewModel.state.value.groupMode)
    }

    @Test
    fun setGroupModeToDueDisablesManualSort() {
        viewModel.setGroupMode(SortHelper.SORT_DUE)

        verify(preferences).isManualSort = false
        verify(preferences).isAstridSort = false
        verify(preferences).groupMode = SortHelper.SORT_DUE
        assertEquals(SortHelper.SORT_DUE, viewModel.state.value.groupMode)
        assertTrue(viewModel.state.value.groupAscending)
    }

    @Test
    fun setGroupModeToModifiedSetsDescending() {
        viewModel.setGroupMode(SortHelper.SORT_MODIFIED)

        verify(preferences).groupAscending = false
        assertFalse(viewModel.state.value.groupAscending)
    }

    @Test
    fun setGroupModeToCreatedSetsDescending() {
        viewModel.setGroupMode(SortHelper.SORT_CREATED)

        verify(preferences).groupAscending = false
        assertFalse(viewModel.state.value.groupAscending)
    }

    @Test
    fun setGroupModeToAlphaSetsAscending() {
        viewModel.setGroupMode(SortHelper.SORT_ALPHA)

        verify(preferences).groupAscending = true
        assertTrue(viewModel.state.value.groupAscending)
    }

    @Test
    fun changedGroupFalseInitially() {
        assertFalse(viewModel.changedGroup)
    }

    @Test
    fun changedGroupTrueAfterChangingGroupMode() {
        viewModel.setGroupMode(SortHelper.SORT_DUE)

        assertTrue(viewModel.changedGroup)
    }

    // --- setSortMode ---

    @Test
    fun setSortModeDisablesManualAndAstrid() {
        viewModel.setSortMode(SortHelper.SORT_ALPHA)

        verify(preferences).isManualSort = false
        verify(preferences).isAstridSort = false
        verify(preferences).sortMode = SortHelper.SORT_ALPHA
        assertEquals(SortHelper.SORT_ALPHA, viewModel.state.value.sortMode)
        assertFalse(viewModel.state.value.manualSort)
        assertFalse(viewModel.state.value.astridSort)
    }

    @Test
    fun setSortModeToModifiedSetsDescending() {
        viewModel.setSortMode(SortHelper.SORT_MODIFIED)

        assertFalse(viewModel.state.value.sortAscending)
    }

    @Test
    fun setSortModeToCreatedSetsDescending() {
        viewModel.setSortMode(SortHelper.SORT_CREATED)

        assertFalse(viewModel.state.value.sortAscending)
    }

    @Test
    fun setSortModeToDueSetsAscending() {
        viewModel.setSortMode(SortHelper.SORT_DUE)

        assertTrue(viewModel.state.value.sortAscending)
    }

    // --- setCompletedMode ---

    @Test
    fun setCompletedModeToCompletedSetsDescending() {
        viewModel.setCompletedMode(SortHelper.SORT_COMPLETED)

        assertFalse(viewModel.state.value.completedAscending)
    }

    @Test
    fun setCompletedModeToModifiedSetsDescending() {
        viewModel.setCompletedMode(SortHelper.SORT_MODIFIED)

        assertFalse(viewModel.state.value.completedAscending)
    }

    @Test
    fun setCompletedModeToCreatedSetsDescending() {
        viewModel.setCompletedMode(SortHelper.SORT_CREATED)

        assertFalse(viewModel.state.value.completedAscending)
    }

    @Test
    fun setCompletedModeToAlphaSetsAscending() {
        viewModel.setCompletedMode(SortHelper.SORT_ALPHA)

        assertTrue(viewModel.state.value.completedAscending)
    }

    // --- setSubtaskMode ---

    @Test
    fun setSubtaskModeToModifiedSetsDescending() {
        viewModel.setSubtaskMode(SortHelper.SORT_MODIFIED)

        assertFalse(viewModel.state.value.subtaskAscending)
    }

    @Test
    fun setSubtaskModeToCreatedSetsDescending() {
        viewModel.setSubtaskMode(SortHelper.SORT_CREATED)

        assertFalse(viewModel.state.value.subtaskAscending)
    }

    @Test
    fun setSubtaskModeToDueSetsAscending() {
        viewModel.setSubtaskMode(SortHelper.SORT_DUE)

        assertTrue(viewModel.state.value.subtaskAscending)
    }

    // --- setManual ---

    @Test
    fun setManualTrueDisablesGrouping() {
        viewModel.setManual(true)

        verify(preferences).isManualSort = true
        verify(preferences).groupMode = SortHelper.GROUP_NONE
        assertTrue(viewModel.state.value.manualSort)
        assertEquals(SortHelper.GROUP_NONE, viewModel.state.value.groupMode)
    }

    @Test
    fun setManualFalsePreservesGroupMode() {
        viewModel.setManual(false)

        verify(preferences).isManualSort = false
        assertFalse(viewModel.state.value.manualSort)
    }

    // --- setAstrid ---

    @Test
    fun setAstridTrueDisablesGrouping() {
        viewModel.setAstrid(true)

        verify(preferences).isAstridSort = true
        verify(preferences).groupMode = SortHelper.GROUP_NONE
        assertTrue(viewModel.state.value.astridSort)
        assertEquals(SortHelper.GROUP_NONE, viewModel.state.value.groupMode)
    }

    @Test
    fun setAstridFalsePreservesGroupMode() {
        viewModel.setAstrid(false)

        verify(preferences).isAstridSort = false
        assertFalse(viewModel.state.value.astridSort)
    }

    // --- forceReload ---

    @Test
    fun forceReloadFalseInitially() {
        assertFalse(viewModel.forceReload)
    }

    @Test
    fun forceReloadTrueAfterManualSortChange() {
        viewModel.setManual(true)

        assertTrue(viewModel.forceReload)
    }

    @Test
    fun forceReloadTrueAfterAstridSortChange() {
        viewModel.setAstrid(true)

        assertTrue(viewModel.forceReload)
    }

    // --- toSortName ---

    @Test
    fun sortNameNone() {
        assertEquals("none", SortHelper.GROUP_NONE.toSortName())
    }

    @Test
    fun sortNameAlpha() {
        assertEquals("alpha", SortHelper.SORT_ALPHA.toSortName())
    }

    @Test
    fun sortNameDue() {
        assertEquals("due", SortHelper.SORT_DUE.toSortName())
    }

    @Test
    fun sortNameImportance() {
        assertEquals("importance", SortHelper.SORT_IMPORTANCE.toSortName())
    }

    @Test
    fun sortNameModified() {
        assertEquals("modified", SortHelper.SORT_MODIFIED.toSortName())
    }

    @Test
    fun sortNameCreated() {
        assertEquals("created", SortHelper.SORT_CREATED.toSortName())
    }

    @Test
    fun sortNameStart() {
        assertEquals("start", SortHelper.SORT_START.toSortName())
    }

    @Test
    fun sortNameList() {
        assertEquals("list", SortHelper.SORT_LIST.toSortName())
    }

    @Test
    fun sortNameCompleted() {
        assertEquals("completed", SortHelper.SORT_COMPLETED.toSortName())
    }

    @Test
    fun sortNameManual() {
        assertEquals("manual", SortHelper.SORT_MANUAL.toSortName())
    }

    @Test
    fun sortNameAuto() {
        assertEquals("auto", SortHelper.SORT_AUTO.toSortName())
    }

    @Test
    fun sortNameUnknown() {
        assertEquals("unknown", 999.toSortName())
    }
}
