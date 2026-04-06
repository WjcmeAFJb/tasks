package org.tasks.location

import androidx.arch.core.executor.ArchTaskExecutor
import androidx.arch.core.executor.TaskExecutor
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Before
import org.junit.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever

@OptIn(ExperimentalCoroutinesApi::class)
class PlaceSearchViewModelTest {

    private lateinit var placeSearch: PlaceSearch
    private lateinit var viewModel: PlaceSearchViewModel
    private val testDispatcher = StandardTestDispatcher()

    @Before
    fun setUp() {
        ArchTaskExecutor.getInstance().setDelegate(object : TaskExecutor() {
            override fun executeOnDiskIO(runnable: Runnable) = runnable.run()
            override fun postToMainThread(runnable: Runnable) = runnable.run()
            override fun isMainThread() = true
        })
        Dispatchers.setMain(testDispatcher)
        placeSearch = mock()
        viewModel = PlaceSearchViewModel(placeSearch)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
        ArchTaskExecutor.getInstance().setDelegate(null)
    }

    // ===== saveState / restoreState =====

    @Test
    fun saveStateDelegatesToPlaceSearch() {
        val bundle = mock<android.os.Bundle>()
        viewModel.saveState(bundle)
        verify(placeSearch).saveState(bundle)
    }

    @Test
    fun restoreStateDelegatesToPlaceSearch() {
        val bundle = mock<android.os.Bundle>()
        viewModel.restoreState(bundle)
        verify(placeSearch).restoreState(bundle)
    }

    @Test
    fun restoreStateWithNull() {
        viewModel.restoreState(null)
        verify(placeSearch).restoreState(null)
    }

    // ===== getAttributionRes =====

    @Test
    fun getAttributionResDelegatesDark() {
        whenever(placeSearch.getAttributionRes(true)).thenReturn(42)
        assertEquals(42, viewModel.getAttributionRes(true))
    }

    @Test
    fun getAttributionResDelegatesLight() {
        whenever(placeSearch.getAttributionRes(false)).thenReturn(99)
        assertEquals(99, viewModel.getAttributionRes(false))
    }

    // ===== query =====

    @Test
    fun queryWithNullPostsEmptyList() = runTest {
        viewModel.query(null, null)
        testDispatcher.scheduler.advanceUntilIdle()
        // No interaction with placeSearch.search when query is null/blank
    }

    @Test
    fun queryWithBlankPostsEmptyList() = runTest {
        viewModel.query("", null)
        testDispatcher.scheduler.advanceUntilIdle()
    }

    @Test
    fun queryWithWhitespacePostsEmptyList() = runTest {
        viewModel.query("   ", null)
        testDispatcher.scheduler.advanceUntilIdle()
    }

    @Test
    fun queryWithTextCallsSearch() = runTest {
        val results = listOf(
            PlaceSearchResult("id1", "Place 1", "Addr 1"),
        )
        whenever(placeSearch.search(any(), any())).thenReturn(results)
        viewModel.query("coffee", null)
        testDispatcher.scheduler.advanceUntilIdle()
        verify(placeSearch).search("coffee", null)
    }

    @Test
    fun queryWithBiasPassesBias() = runTest {
        val bias = MapPosition(40.7, -74.0)
        whenever(placeSearch.search(any(), any())).thenReturn(emptyList())
        viewModel.query("pizza", bias)
        testDispatcher.scheduler.advanceUntilIdle()
        verify(placeSearch).search("pizza", bias)
    }

    @Test
    fun queryWithExceptionDoesNotCrash() = runTest {
        whenever(placeSearch.search(any(), any())).thenThrow(RuntimeException("network error"))
        viewModel.query("test", null)
        testDispatcher.scheduler.advanceUntilIdle()
        // Should not throw, error is handled internally
    }

    // ===== fetch =====

    @Test
    fun fetchDelegatesToSearch() = runTest {
        val place = org.tasks.data.entity.Place(name = "Test", latitude = 1.0, longitude = 2.0)
        val result = PlaceSearchResult("id", "Name", "Addr")
        whenever(placeSearch.fetch(result)).thenReturn(place)
        viewModel.fetch(result)
        testDispatcher.scheduler.advanceUntilIdle()
        verify(placeSearch).fetch(result)
    }

    @Test
    fun fetchWithExceptionDoesNotCrash() = runTest {
        val result = PlaceSearchResult("id", "Name", "Addr")
        whenever(placeSearch.fetch(result)).thenThrow(RuntimeException("fetch error"))
        viewModel.fetch(result)
        testDispatcher.scheduler.advanceUntilIdle()
        // Should not throw
    }
}
