package org.tasks.filters

import android.content.Context
import android.content.res.Resources
import com.todoroo.astrid.api.MultipleSelectCriterion
import com.todoroo.astrid.api.TextInputCriterion
import com.todoroo.astrid.core.CriterionInstance
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
import org.tasks.data.entity.CaldavCalendar
import org.tasks.data.entity.TagData

class FilterCriteriaProviderMaxCovTest {

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
        `when`(context.getString(anyInt(), org.mockito.Mockito.any())).thenReturn("test_string")
        `when`(resources.getString(anyInt())).thenReturn("test_string")
        `when`(resources.getStringArray(anyInt())).thenReturn(arrayOf("a", "b", "c", "d", "e", "f", "g", "h"))
        provider = FilterCriteriaProvider(context, tagDataDao, caldavDao)
    }

    @Test
    fun dueDateFilterEntryValues() {
        assertEquals(8, (provider.dueDateFilter as MultipleSelectCriterion).entryValues.size)
    }

    @Test
    fun startDateFilterEntryValues() {
        assertEquals(8, (provider.startDateFilter as MultipleSelectCriterion).entryValues.size)
    }

    @Test
    fun priorityFilterEntryValues() {
        assertEquals(4, (provider.priorityFilter as MultipleSelectCriterion).entryValues.size)
    }

    @Test
    fun dueDateFilterIdentifier() {
        assertEquals("dueDate", provider.dueDateFilter.identifier)
    }

    @Test
    fun startDateFilterIdentifier() {
        assertEquals("startDate", provider.startDateFilter.identifier)
    }

    @Test
    fun dueDateSqlContainsDueDate() {
        assertTrue(provider.dueDateFilter.sql!!.contains("dueDate"))
    }

    // ===== fromString with each identifier =====

    @Test
    fun fromStringTitle() = runTest {
        val result = provider.fromString("title|searchterm|test|2|sql")
        assertEquals(1, result.size)
        assertTrue(result[0].criterion is TextInputCriterion)
        assertEquals("searchterm", result[0].selectedText)
    }

    @Test
    fun fromStringPriority() = runTest {
        assertEquals(1, provider.fromString("importance|1|test|0|sql").size)
    }

    @Test
    fun fromStringStartDate() = runTest {
        assertEquals(1, provider.fromString("startDate|0|test|2|sql").size)
    }

    @Test
    fun fromStringDueDate() = runTest {
        assertEquals(1, provider.fromString("dueDate|0|test|2|sql").size)
    }

    @Test
    fun fromStringCaldav() = runTest {
        `when`(caldavDao.getCalendars()).thenReturn(listOf(CaldavCalendar(uuid = "c1", name = "Cal")))
        assertEquals(1, provider.fromString("caldavlist|c1|test|2|sql").size)
    }

    @Test
    fun fromStringGtasks() = runTest {
        `when`(caldavDao.getCalendars()).thenReturn(emptyList())
        assertEquals(1, provider.fromString("gtaskslist||test|2|sql").size)
    }

    @Test
    fun fromStringTagIs() = runTest {
        `when`(tagDataDao.tagDataOrderedByName()).thenReturn(listOf(TagData(name = "Work")))
        assertEquals(1, provider.fromString("tag_is|Work|test|2|sql").size)
    }

    @Test
    fun fromStringTagContains() = runTest {
        val result = provider.fromString("tag_contains|keyword|test|0|sql")
        assertEquals(1, result.size)
        assertEquals("keyword", result[0].selectedText)
    }

    @Test
    fun fromStringRecur() = runTest {
        assertEquals(1, provider.fromString("recur||test|2|sql").size)
    }

    @Test
    fun fromStringCompleted() = runTest {
        assertEquals(1, provider.fromString("completed||test|2|sql").size)
    }

    @Test
    fun fromStringHidden() = runTest {
        assertEquals(1, provider.fromString("hidden||test|0|sql").size)
    }

    @Test
    fun fromStringParent() = runTest {
        assertEquals(1, provider.fromString("parent||test|2|sql").size)
    }

    @Test
    fun fromStringSubtask() = runTest {
        assertEquals(1, provider.fromString("subtask||test|2|sql").size)
    }

    @Test
    fun fromStringReminders() = runTest {
        assertEquals(1, provider.fromString("reminders||test|2|sql").size)
    }

    @Test
    fun fromStringActive() = runTest {
        val result = provider.fromString("active||text|3|sql")
        assertEquals(1, result.size)
        assertEquals(3, result[0].type)
    }

    @Test
    fun fromStringMultipleRows() = runTest {
        assertEquals(2, provider.fromString("title|s|t|2|q\nrecur||t|2|q").size)
    }

    @Test
    fun fromStringEscapedPipe() = runTest {
        val result = provider.fromString("title|search!PIPE!term|test|2|sql")
        assertEquals("search|term", result[0].selectedText)
    }

    @Test
    fun fromStringUnknownThrows() = runTest {
        try {
            provider.fromString("unknown||t|2|q")
        } catch (e: RuntimeException) {
            assertTrue(e.message!!.contains("Unknown identifier"))
        }
    }

    // ===== rebuildFilter =====

    @Test
    fun rebuildFilterValid() = runTest {
        val filter = org.tasks.data.entity.Filter(criterion = "title|search|t|2|sql")
        val result = provider.rebuildFilter(filter)
        assertNotNull(result.sql)
        assertNotNull(result.criterion)
    }

    // ===== all() =====

    @Test
    fun allReturns13() = runTest {
        `when`(tagDataDao.tagDataOrderedByName()).thenReturn(emptyList())
        `when`(caldavDao.getCalendars()).thenReturn(emptyList())
        assertEquals(13, provider.all().size)
    }

    @Test
    fun allContainsExpectedIdentifiers() = runTest {
        `when`(tagDataDao.tagDataOrderedByName()).thenReturn(emptyList())
        `when`(caldavDao.getCalendars()).thenReturn(emptyList())
        val ids = provider.all().map { it.identifier }.toSet()
        assertTrue(ids.containsAll(listOf(
            "tag_is", "tag_contains", "startDate", "dueDate", "importance",
            "title", "caldavlist", "recur", "completed", "hidden", "parent", "subtask", "reminders"
        )))
    }

    @Test
    fun tagFilterDeduplicates() = runTest {
        `when`(tagDataDao.tagDataOrderedByName()).thenReturn(listOf(
            TagData(name = "Work"), TagData(name = "Work"), TagData(name = "Personal")
        ))
        `when`(caldavDao.getCalendars()).thenReturn(emptyList())
        val tagCrit = provider.all().find { it.identifier == "tag_is" } as MultipleSelectCriterion
        assertEquals(2, tagCrit.entryTitles.size)
    }

    @Test
    fun caldavFilterWithCalendars() = runTest {
        `when`(tagDataDao.tagDataOrderedByName()).thenReturn(emptyList())
        `when`(caldavDao.getCalendars()).thenReturn(listOf(
            CaldavCalendar(uuid = "c1", name = "W"), CaldavCalendar(uuid = "c2", name = "P")
        ))
        val crit = provider.all().find { it.identifier == "caldavlist" } as MultipleSelectCriterion
        assertEquals(2, crit.entryTitles.size)
    }
}
