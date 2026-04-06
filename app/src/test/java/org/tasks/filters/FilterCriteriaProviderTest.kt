package org.tasks.filters

import android.content.Context
import android.content.res.Resources
import com.todoroo.astrid.api.MultipleSelectCriterion
import com.todoroo.astrid.api.TextInputCriterion
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.mockito.Mock
import org.mockito.Mockito.anyInt
import org.mockito.Mockito.`when`
import org.mockito.MockitoAnnotations
import org.tasks.data.dao.CaldavDao
import org.tasks.data.dao.TagDataDao

class FilterCriteriaProviderTest {

    @Mock lateinit var context: Context
    @Mock lateinit var resources: Resources
    @Mock lateinit var tagDataDao: TagDataDao
    @Mock lateinit var caldavDao: CaldavDao

    private lateinit var provider: FilterCriteriaProvider

    @Before
    fun setUp() {
        MockitoAnnotations.openMocks(this)
        `when`(context.resources).thenReturn(resources)
        `when`(context.getString(anyInt())).thenReturn("test_string")
        `when`(resources.getString(anyInt())).thenReturn("test_string")
        `when`(resources.getStringArray(anyInt())).thenReturn(arrayOf("a", "b", "c", "d", "e", "f", "g", "h"))
        provider = FilterCriteriaProvider(context, tagDataDao, caldavDao)
    }

    @Test fun startingUniverseIsMultipleSelect() {
        assertTrue(provider.startingUniverse is MultipleSelectCriterion)
    }

    @Test fun startingUniverseIdentifier() {
        assertEquals("active", provider.startingUniverse.identifier)
    }

    @Test fun priorityFilterIsMultipleSelect() {
        assertTrue(provider.priorityFilter is MultipleSelectCriterion)
    }

    @Test fun priorityFilterIdentifier() {
        assertEquals("importance", provider.priorityFilter.identifier)
    }

    @Test fun priorityFilterHasFourValues() {
        val c = provider.priorityFilter as MultipleSelectCriterion
        assertEquals(4, c.entryValues.size)
    }

    @Test fun priorityFilterSqlContainsImportance() {
        assertTrue(provider.priorityFilter.sql!!.contains("importance"))
    }

    @Test fun dueDateFilterIsMultipleSelect() {
        assertTrue(provider.dueDateFilter is MultipleSelectCriterion)
    }

    @Test fun dueDateFilterHasEightValues() {
        val c = provider.dueDateFilter as MultipleSelectCriterion
        assertEquals(8, c.entryValues.size)
    }

    @Test fun startDateFilterIsMultipleSelect() {
        assertTrue(provider.startDateFilter is MultipleSelectCriterion)
    }

    @Test fun startDateFilterSql() {
        assertTrue(provider.startDateFilter.sql!!.contains("hideUntil"))
    }

    @Test fun tagNameContainsIsTextInput() {
        assertTrue(provider.tagNameContainsFilter is TextInputCriterion)
    }

    @Test fun tagNameContainsSqlHasLike() {
        assertTrue(provider.tagNameContainsFilter.sql!!.contains("LIKE"))
    }

    @Test fun fromStringNull() = runTest {
        assertEquals(emptyList<Any>(), provider.fromString(null))
    }

    @Test fun fromStringBlank() = runTest {
        assertEquals(emptyList<Any>(), provider.fromString(""))
    }

    @Test fun fromStringWhitespace() = runTest {
        assertEquals(emptyList<Any>(), provider.fromString("   "))
    }

    @Test fun fromStringInvalidRow() = runTest {
        assertEquals(emptyList<Any>(), provider.fromString("only_one_part"))
    }

    @Test fun rebuildFilterWithNullCriterion() = runTest {
        val filter = org.tasks.data.entity.Filter(criterion = null)
        val result = provider.rebuildFilter(filter)
        assertNotNull(result)
    }

    @Test fun rebuildFilterWithBlankCriterion() = runTest {
        val filter = org.tasks.data.entity.Filter(criterion = "  ")
        val result = provider.rebuildFilter(filter)
        assertNotNull(result)
    }
}
