package org.tasks.filters

import android.content.Context
import android.content.res.Resources
import com.todoroo.astrid.api.BooleanCriterion
import com.todoroo.astrid.api.MultipleSelectCriterion
import com.todoroo.astrid.api.PermaSql
import com.todoroo.astrid.api.TextInputCriterion
import com.todoroo.astrid.core.CriterionInstance
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.mockito.Mockito.anyInt
import org.mockito.Mockito.anyString
import org.mockito.Mockito.mock
import org.mockito.Mockito.`when`
import org.tasks.R
import org.tasks.data.dao.CaldavDao
import org.tasks.data.dao.TagDataDao
import org.tasks.data.entity.CaldavCalendar
import org.tasks.data.entity.TagData

class FilterCriteriaProviderTest {

    private lateinit var context: Context
    private lateinit var resources: Resources
    private lateinit var tagDataDao: TagDataDao
    private lateinit var caldavDao: CaldavDao
    private lateinit var provider: FilterCriteriaProvider

    @Before
    fun setUp() {
        context = mock(Context::class.java)
        resources = mock(Resources::class.java)
        tagDataDao = mock(TagDataDao::class.java)
        caldavDao = mock(CaldavDao::class.java)
        `when`(context.resources).thenReturn(resources)
        `when`(context.getString(anyInt())).thenAnswer { "string_${it.getArgument<Int>(0)}" }
        `when`(context.getString(anyInt(), anyString())).thenAnswer {
            "string_${it.getArgument<Int>(0)}_${it.getArgument<String>(1)}"
        }
        `when`(resources.getString(anyInt())).thenAnswer { "string_${it.getArgument<Int>(0)}" }
        `when`(resources.getStringArray(anyInt())).thenReturn(
            arrayOf("entry1", "entry2", "entry3", "entry4", "entry5", "entry6", "entry7", "entry8")
        )
        provider = FilterCriteriaProvider(context, tagDataDao, caldavDao)
    }

    // --- startingUniverse ---

    @Test
    fun startingUniverseIsMultipleSelectCriterion() {
        val criterion = provider.startingUniverse
        assertTrue(criterion is MultipleSelectCriterion)
    }

    @Test
    fun startingUniverseHasCorrectIdentifier() {
        val criterion = provider.startingUniverse
        assertEquals("active", criterion.identifier)
    }

    @Test
    fun startingUniverseHasNullSql() {
        val criterion = provider.startingUniverse
        assertNull(criterion.sql)
    }

    @Test
    fun startingUniverseHasNullEntryValues() {
        val criterion = provider.startingUniverse as MultipleSelectCriterion
        assertNull(criterion.entryValues)
    }

    @Test
    fun startingUniverseHasNullEntryTitles() {
        val criterion = provider.startingUniverse as MultipleSelectCriterion
        assertNull(criterion.entryTitles)
    }

    // --- tagNameContainsFilter ---

    @Test
    fun tagNameContainsFilterIsTextInputCriterion() {
        val criterion = provider.tagNameContainsFilter
        assertTrue(criterion is TextInputCriterion)
    }

    @Test
    fun tagNameContainsFilterHasCorrectIdentifier() {
        val criterion = provider.tagNameContainsFilter
        assertEquals("tag_contains", criterion.identifier)
    }

    @Test
    fun tagNameContainsFilterSqlContainsLike() {
        val criterion = provider.tagNameContainsFilter
        assertTrue(criterion.sql.contains("LIKE"))
    }

    @Test
    fun tagNameContainsFilterSqlContainsTagTable() {
        val criterion = provider.tagNameContainsFilter
        assertTrue(criterion.sql.contains("tags"))
    }

    // --- dueDateFilter ---

    @Test
    fun dueDateFilterIsMultipleSelectCriterion() {
        val criterion = provider.dueDateFilter
        assertTrue(criterion is MultipleSelectCriterion)
    }

    @Test
    fun dueDateFilterHasCorrectIdentifier() {
        val criterion = provider.dueDateFilter
        assertEquals("dueDate", criterion.identifier)
    }

    @Test
    fun dueDateFilterHasEightEntryValues() {
        val criterion = provider.dueDateFilter as MultipleSelectCriterion
        assertEquals(8, criterion.entryValues.size)
    }

    @Test
    fun dueDateFilterEntryValuesContainPermaSqlConstants() {
        val criterion = provider.dueDateFilter as MultipleSelectCriterion
        val values = criterion.entryValues.toList()
        assertTrue(values.contains("0"))
        assertTrue(values.contains(PermaSql.VALUE_EOD_YESTERDAY))
        assertTrue(values.contains(PermaSql.VALUE_EOD))
        assertTrue(values.contains(PermaSql.VALUE_EOD_TOMORROW))
        assertTrue(values.contains(PermaSql.VALUE_EOD_DAY_AFTER))
        assertTrue(values.contains(PermaSql.VALUE_EOD_NEXT_WEEK))
        assertTrue(values.contains(PermaSql.VALUE_EOD_NEXT_MONTH))
        assertTrue(values.contains(PermaSql.VALUE_NOW))
    }

    @Test
    fun dueDateFilterSqlContainsDueDate() {
        val criterion = provider.dueDateFilter
        assertTrue(criterion.sql.contains("dueDate"))
    }

    @Test
    fun dueDateFilterValuesForNewTasksContainsDueDate() {
        val criterion = provider.dueDateFilter
        assertTrue(criterion.valuesForNewTasks.containsKey("dueDate"))
        assertEquals("?", criterion.valuesForNewTasks["dueDate"])
    }

    // --- startDateFilter ---

    @Test
    fun startDateFilterIsMultipleSelectCriterion() {
        val criterion = provider.startDateFilter
        assertTrue(criterion is MultipleSelectCriterion)
    }

    @Test
    fun startDateFilterHasCorrectIdentifier() {
        val criterion = provider.startDateFilter
        assertEquals("startDate", criterion.identifier)
    }

    @Test
    fun startDateFilterHasEightEntryValues() {
        val criterion = provider.startDateFilter as MultipleSelectCriterion
        assertEquals(8, criterion.entryValues.size)
    }

    @Test
    fun startDateFilterSqlContainsHideUntil() {
        val criterion = provider.startDateFilter
        assertTrue(criterion.sql.contains("hideUntil"))
    }

    @Test
    fun startDateFilterValuesForNewTasksContainsHideUntil() {
        val criterion = provider.startDateFilter
        assertTrue(criterion.valuesForNewTasks.containsKey("hideUntil"))
        assertEquals("?", criterion.valuesForNewTasks["hideUntil"])
    }

    // --- priorityFilter ---

    @Test
    fun priorityFilterIsMultipleSelectCriterion() {
        val criterion = provider.priorityFilter
        assertTrue(criterion is MultipleSelectCriterion)
    }

    @Test
    fun priorityFilterHasCorrectIdentifier() {
        val criterion = provider.priorityFilter
        assertEquals("importance", criterion.identifier)
    }

    @Test
    fun priorityFilterHasFourEntryValues() {
        val criterion = provider.priorityFilter as MultipleSelectCriterion
        assertEquals(4, criterion.entryValues.size)
    }

    @Test
    fun priorityFilterEntryValuesAreNumericPriorities() {
        val criterion = provider.priorityFilter as MultipleSelectCriterion
        val values = criterion.entryValues.toList()
        assertTrue(values.contains("0"))  // HIGH
        assertTrue(values.contains("1"))  // MEDIUM
        assertTrue(values.contains("2"))  // LOW
        assertTrue(values.contains("3"))  // NONE
    }

    @Test
    fun priorityFilterEntryTitlesAreExclamationMarks() {
        val criterion = provider.priorityFilter as MultipleSelectCriterion
        val titles = criterion.entryTitles.toList()
        assertEquals(listOf("!!!", "!!", "!", "o"), titles)
    }

    @Test
    fun priorityFilterSqlContainsImportance() {
        val criterion = provider.priorityFilter
        assertTrue(criterion.sql.contains("importance"))
    }

    @Test
    fun priorityFilterValuesForNewTasksContainsImportance() {
        val criterion = provider.priorityFilter
        assertTrue(criterion.valuesForNewTasks.containsKey("importance"))
        assertEquals("?", criterion.valuesForNewTasks["importance"])
    }

    // --- taskTitleContainsFilter (accessed via all()) ---

    @Test
    fun allContainsTaskTitleFilter() = runTest {
        `when`(tagDataDao.tagDataOrderedByName()).thenReturn(emptyList())
        `when`(caldavDao.getCalendars()).thenReturn(emptyList())
        val criteria = provider.all()
        val titleFilter = criteria.find { it.identifier == "title" }
        assertNotNull(titleFilter)
        assertTrue(titleFilter is TextInputCriterion)
    }

    @Test
    fun taskTitleFilterSqlContainsLike() = runTest {
        `when`(tagDataDao.tagDataOrderedByName()).thenReturn(emptyList())
        `when`(caldavDao.getCalendars()).thenReturn(emptyList())
        val criteria = provider.all()
        val titleFilter = criteria.find { it.identifier == "title" }!!
        assertTrue(titleFilter.sql.contains("LIKE"))
    }

    // --- all() ---

    @Test
    fun allReturnsThirteenCriteria() = runTest {
        `when`(tagDataDao.tagDataOrderedByName()).thenReturn(emptyList())
        `when`(caldavDao.getCalendars()).thenReturn(emptyList())
        val criteria = provider.all()
        assertEquals(13, criteria.size)
    }

    @Test
    fun allContainsAllExpectedIdentifiers() = runTest {
        `when`(tagDataDao.tagDataOrderedByName()).thenReturn(emptyList())
        `when`(caldavDao.getCalendars()).thenReturn(emptyList())
        val criteria = provider.all()
        val identifiers = criteria.map { it.identifier }.toSet()
        assertTrue(identifiers.contains("tag_is"))
        assertTrue(identifiers.contains("tag_contains"))
        assertTrue(identifiers.contains("startDate"))
        assertTrue(identifiers.contains("dueDate"))
        assertTrue(identifiers.contains("importance"))
        assertTrue(identifiers.contains("title"))
        assertTrue(identifiers.contains("caldavlist"))
        assertTrue(identifiers.contains("recur"))
        assertTrue(identifiers.contains("completed"))
        assertTrue(identifiers.contains("hidden"))
        assertTrue(identifiers.contains("parent"))
        assertTrue(identifiers.contains("subtask"))
        assertTrue(identifiers.contains("reminders"))
    }

    // --- tagFilter (via all) ---

    @Test
    fun tagFilterPopulatesTagNames() = runTest {
        `when`(tagDataDao.tagDataOrderedByName()).thenReturn(
            listOf(
                TagData(name = "Work"),
                TagData(name = "Home"),
                TagData(name = "Errands"),
            )
        )
        `when`(caldavDao.getCalendars()).thenReturn(emptyList())
        val criteria = provider.all()
        val tagFilter = criteria.find { it.identifier == "tag_is" } as MultipleSelectCriterion
        assertEquals(3, tagFilter.entryTitles.size)
        assertEquals("Work", tagFilter.entryTitles[0])
        assertEquals("Home", tagFilter.entryTitles[1])
        assertEquals("Errands", tagFilter.entryTitles[2])
    }

    @Test
    fun tagFilterDeduplicatesNames() = runTest {
        `when`(tagDataDao.tagDataOrderedByName()).thenReturn(
            listOf(
                TagData(name = "Work"),
                TagData(name = "Work"),
                TagData(name = "Home"),
            )
        )
        `when`(caldavDao.getCalendars()).thenReturn(emptyList())
        val criteria = provider.all()
        val tagFilter = criteria.find { it.identifier == "tag_is" } as MultipleSelectCriterion
        assertEquals(2, tagFilter.entryTitles.size)
    }

    @Test
    fun tagFilterWithEmptyTagList() = runTest {
        `when`(tagDataDao.tagDataOrderedByName()).thenReturn(emptyList())
        `when`(caldavDao.getCalendars()).thenReturn(emptyList())
        val criteria = provider.all()
        val tagFilter = criteria.find { it.identifier == "tag_is" } as MultipleSelectCriterion
        assertEquals(0, tagFilter.entryTitles.size)
    }

    @Test
    fun tagFilterSqlContainsTagsTable() = runTest {
        `when`(tagDataDao.tagDataOrderedByName()).thenReturn(emptyList())
        `when`(caldavDao.getCalendars()).thenReturn(emptyList())
        val criteria = provider.all()
        val tagFilter = criteria.find { it.identifier == "tag_is" }!!
        assertTrue(tagFilter.sql.contains("tags"))
    }

    @Test
    fun tagFilterIsPresent() = runTest {
        `when`(tagDataDao.tagDataOrderedByName()).thenReturn(emptyList())
        `when`(caldavDao.getCalendars()).thenReturn(emptyList())
        val criteria = provider.all()
        assertNotNull(criteria.find { it.identifier == "tag_is" })
    }

    // --- caldavFilterCriteria (via all) ---

    @Test
    fun caldavFilterPopulatesCalendarNames() = runTest {
        `when`(tagDataDao.tagDataOrderedByName()).thenReturn(emptyList())
        `when`(caldavDao.getCalendars()).thenReturn(
            listOf(
                CaldavCalendar().apply { uuid = "uuid1"; name = "Calendar A" },
                CaldavCalendar().apply { uuid = "uuid2"; name = "Calendar B" },
            )
        )
        val criteria = provider.all()
        val caldavFilter = criteria.find { it.identifier == "caldavlist" } as MultipleSelectCriterion
        assertEquals(2, caldavFilter.entryTitles.size)
        assertEquals("Calendar A", caldavFilter.entryTitles[0])
        assertEquals("Calendar B", caldavFilter.entryTitles[1])
    }

    @Test
    fun caldavFilterEntryValuesAreUuids() = runTest {
        `when`(tagDataDao.tagDataOrderedByName()).thenReturn(emptyList())
        `when`(caldavDao.getCalendars()).thenReturn(
            listOf(
                CaldavCalendar().apply { uuid = "uuid-abc"; name = "Cal" },
            )
        )
        val criteria = provider.all()
        val caldavFilter = criteria.find { it.identifier == "caldavlist" } as MultipleSelectCriterion
        assertEquals("uuid-abc", caldavFilter.entryValues[0])
    }

    @Test
    fun caldavFilterSqlContainsCaldavTasks() = runTest {
        `when`(tagDataDao.tagDataOrderedByName()).thenReturn(emptyList())
        `when`(caldavDao.getCalendars()).thenReturn(emptyList())
        val criteria = provider.all()
        val caldavFilter = criteria.find { it.identifier == "caldavlist" }!!
        assertTrue(caldavFilter.sql.contains("caldav_tasks"))
    }

    @Test
    fun caldavFilterWithEmptyCalendarList() = runTest {
        `when`(tagDataDao.tagDataOrderedByName()).thenReturn(emptyList())
        `when`(caldavDao.getCalendars()).thenReturn(emptyList())
        val criteria = provider.all()
        val caldavFilter = criteria.find { it.identifier == "caldavlist" } as MultipleSelectCriterion
        assertEquals(0, caldavFilter.entryTitles.size)
    }

    // --- boolean criteria (recurringFilter, completedFilter, etc.) ---

    @Test
    fun recurringFilterIsBooleanCriterion() = runTest {
        `when`(tagDataDao.tagDataOrderedByName()).thenReturn(emptyList())
        `when`(caldavDao.getCalendars()).thenReturn(emptyList())
        val criteria = provider.all()
        val recurFilter = criteria.find { it.identifier == "recur" }
        assertTrue(recurFilter is BooleanCriterion)
    }

    @Test
    fun recurringFilterSqlContainsRecurrence() = runTest {
        `when`(tagDataDao.tagDataOrderedByName()).thenReturn(emptyList())
        `when`(caldavDao.getCalendars()).thenReturn(emptyList())
        val criteria = provider.all()
        val recurFilter = criteria.find { it.identifier == "recur" }!!
        assertTrue(recurFilter.sql.contains("recurrence"))
    }

    @Test
    fun completedFilterIsBooleanCriterion() = runTest {
        `when`(tagDataDao.tagDataOrderedByName()).thenReturn(emptyList())
        `when`(caldavDao.getCalendars()).thenReturn(emptyList())
        val criteria = provider.all()
        val completed = criteria.find { it.identifier == "completed" }
        assertTrue(completed is BooleanCriterion)
    }

    @Test
    fun completedFilterSqlContainsCompletionDate() = runTest {
        `when`(tagDataDao.tagDataOrderedByName()).thenReturn(emptyList())
        `when`(caldavDao.getCalendars()).thenReturn(emptyList())
        val criteria = provider.all()
        val completed = criteria.find { it.identifier == "completed" }!!
        assertTrue(completed.sql.contains("completed"))
    }

    @Test
    fun hiddenFilterIsBooleanCriterion() = runTest {
        `when`(tagDataDao.tagDataOrderedByName()).thenReturn(emptyList())
        `when`(caldavDao.getCalendars()).thenReturn(emptyList())
        val criteria = provider.all()
        val hidden = criteria.find { it.identifier == "hidden" }
        assertTrue(hidden is BooleanCriterion)
    }

    @Test
    fun hiddenFilterSqlContainsHideUntil() = runTest {
        `when`(tagDataDao.tagDataOrderedByName()).thenReturn(emptyList())
        `when`(caldavDao.getCalendars()).thenReturn(emptyList())
        val criteria = provider.all()
        val hidden = criteria.find { it.identifier == "hidden" }!!
        assertTrue(hidden.sql.contains("hideUntil"))
    }

    @Test
    fun parentFilterIsBooleanCriterion() = runTest {
        `when`(tagDataDao.tagDataOrderedByName()).thenReturn(emptyList())
        `when`(caldavDao.getCalendars()).thenReturn(emptyList())
        val criteria = provider.all()
        val parent = criteria.find { it.identifier == "parent" }
        assertTrue(parent is BooleanCriterion)
    }

    @Test
    fun parentFilterSqlContainsChildren() = runTest {
        `when`(tagDataDao.tagDataOrderedByName()).thenReturn(emptyList())
        `when`(caldavDao.getCalendars()).thenReturn(emptyList())
        val criteria = provider.all()
        val parent = criteria.find { it.identifier == "parent" }!!
        assertTrue(parent.sql.contains("children"))
    }

    @Test
    fun subtaskFilterIsBooleanCriterion() = runTest {
        `when`(tagDataDao.tagDataOrderedByName()).thenReturn(emptyList())
        `when`(caldavDao.getCalendars()).thenReturn(emptyList())
        val criteria = provider.all()
        val subtask = criteria.find { it.identifier == "subtask" }
        assertTrue(subtask is BooleanCriterion)
    }

    @Test
    fun subtaskFilterSqlContainsParent() = runTest {
        `when`(tagDataDao.tagDataOrderedByName()).thenReturn(emptyList())
        `when`(caldavDao.getCalendars()).thenReturn(emptyList())
        val criteria = provider.all()
        val subtask = criteria.find { it.identifier == "subtask" }!!
        assertTrue(subtask.sql.contains("parent"))
    }

    @Test
    fun reminderFilterIsBooleanCriterion() = runTest {
        `when`(tagDataDao.tagDataOrderedByName()).thenReturn(emptyList())
        `when`(caldavDao.getCalendars()).thenReturn(emptyList())
        val criteria = provider.all()
        val reminder = criteria.find { it.identifier == "reminders" }
        assertTrue(reminder is BooleanCriterion)
    }

    @Test
    fun reminderFilterSqlContainsAlarms() = runTest {
        `when`(tagDataDao.tagDataOrderedByName()).thenReturn(emptyList())
        `when`(caldavDao.getCalendars()).thenReturn(emptyList())
        val criteria = provider.all()
        val reminder = criteria.find { it.identifier == "reminders" }!!
        assertTrue(reminder.sql.contains("alarms"))
    }

    // --- fromString ---

    @Test
    fun fromStringReturnsEmptyForNull() = runTest {
        val result = provider.fromString(null)
        assertTrue(result.isEmpty())
    }

    @Test
    fun fromStringReturnsEmptyForBlank() = runTest {
        val result = provider.fromString("")
        assertTrue(result.isEmpty())
    }

    @Test
    fun fromStringReturnsEmptyForWhitespace() = runTest {
        val result = provider.fromString("   ")
        assertTrue(result.isEmpty())
    }

    @Test
    fun fromStringReturnsEmptyForInvalidRow() = runTest {
        val result = provider.fromString("only|two")
        assertTrue(result.isEmpty())
    }

    @Test
    fun fromStringReturnsEmptyForThreeFields() = runTest {
        val result = provider.fromString("one|two|three")
        assertTrue(result.isEmpty())
    }

    @Test
    fun fromStringParsesBooleanCriterion() = runTest {
        // Format: identifier|value|text|type  (4 fields)
        val serialized = "recur|Repeats|Repeats|2"
        val result = provider.fromString(serialized)
        assertEquals(1, result.size)
        assertEquals(CriterionInstance.TYPE_INTERSECT, result[0].type)
        assertTrue(result[0].criterion is BooleanCriterion)
    }

    @Test
    fun fromStringParsesFiveFieldRow() = runTest {
        // Format: identifier|value|text|type|sql  (5 fields)
        val serialized = "recur|Repeats|Repeats|2|SELECT _id FROM tasks"
        val result = provider.fromString(serialized)
        assertEquals(1, result.size)
        assertEquals(CriterionInstance.TYPE_INTERSECT, result[0].type)
    }

    @Test
    fun fromStringParsesMultipleRows() = runTest {
        val serialized = "active|All active|All active|3\nrecur|Repeats|Repeats|2"
        val result = provider.fromString(serialized)
        assertEquals(2, result.size)
        assertEquals(CriterionInstance.TYPE_UNIVERSE, result[0].type)
        assertEquals(CriterionInstance.TYPE_INTERSECT, result[1].type)
    }

    @Test
    fun fromStringParsesTextInputCriterion() = runTest {
        val serialized = "title|search text|Title contains ?|2"
        val result = provider.fromString(serialized)
        assertEquals(1, result.size)
        assertTrue(result[0].criterion is TextInputCriterion)
        assertEquals("search text", result[0].selectedText)
    }

    @Test
    fun fromStringParsesMultipleSelectCriterion() = runTest {
        val serialized = "importance|0|Priority: ?|2"
        val result = provider.fromString(serialized)
        assertEquals(1, result.size)
        assertTrue(result[0].criterion is MultipleSelectCriterion)
        assertEquals(0, result[0].selectedIndex)
    }

    @Test
    fun fromStringParsesTypeAdd() = runTest {
        val serialized = "recur|Repeats|Repeats|0"
        val result = provider.fromString(serialized)
        assertEquals(CriterionInstance.TYPE_ADD, result[0].type)
    }

    @Test
    fun fromStringParsesTypeSubtract() = runTest {
        val serialized = "recur|Repeats|Repeats|1"
        val result = provider.fromString(serialized)
        assertEquals(CriterionInstance.TYPE_SUBTRACT, result[0].type)
    }

    @Test
    fun fromStringUnescapesPipeInFields() = runTest {
        // The value field has an escaped pipe: "has!PIPE!pipe"
        val serialized = "title|has!PIPE!pipe|Title contains ?|2"
        val result = provider.fromString(serialized)
        assertEquals(1, result.size)
        assertEquals("has|pipe", result[0].selectedText)
    }

    @Test
    fun fromStringHandlesGtasksIdentifier() = runTest {
        `when`(caldavDao.getCalendars()).thenReturn(emptyList())
        val serialized = "gtaskslist|value|text|2"
        val result = provider.fromString(serialized)
        assertEquals(1, result.size)
        assertEquals("caldavlist", result[0].criterion.identifier)
    }

    @Test
    fun fromStringHandlesCaldavIdentifier() = runTest {
        `when`(caldavDao.getCalendars()).thenReturn(emptyList())
        val serialized = "caldavlist|value|text|2"
        val result = provider.fromString(serialized)
        assertEquals(1, result.size)
        assertEquals("caldavlist", result[0].criterion.identifier)
    }

    @Test(expected = RuntimeException::class)
    fun fromStringThrowsForUnknownIdentifier() = runTest {
        provider.fromString("unknown_identifier|value|text|2")
    }

    // --- rebuildFilter ---

    @Test
    fun rebuildFilterWithNullCriterionReturnsSameFilter() = runTest {
        val filter = org.tasks.data.entity.Filter(
            id = 1,
            title = "Test",
            criterion = null,
        )
        val rebuilt = provider.rebuildFilter(filter)
        assertEquals("", rebuilt.criterion)
    }

    @Test
    fun rebuildFilterWithBlankCriterionReturnsSameFilter() = runTest {
        val filter = org.tasks.data.entity.Filter(
            id = 1,
            title = "Test",
            criterion = "   ",
        )
        val rebuilt = provider.rebuildFilter(filter)
        assertEquals("", rebuilt.criterion)
    }

    @Test
    fun rebuildFilterWithValidCriterionUpdatesSql() = runTest {
        val serialized = "active|All active|All active|3"
        val filter = org.tasks.data.entity.Filter(
            id = 1,
            title = "My Filter",
            criterion = serialized,
        )
        val rebuilt = provider.rebuildFilter(filter)
        assertNotNull(rebuilt.sql)
        assertTrue(rebuilt.sql!!.contains("WHERE"))
    }

    @Test
    fun rebuildFilterPreservesFilterId() = runTest {
        val filter = org.tasks.data.entity.Filter(
            id = 42,
            title = "Test",
            criterion = null,
        )
        val rebuilt = provider.rebuildFilter(filter)
        assertEquals(42, rebuilt.id)
    }

    @Test
    fun rebuildFilterPreservesFilterTitle() = runTest {
        val filter = org.tasks.data.entity.Filter(
            id = 1,
            title = "My Filter",
            criterion = null,
        )
        val rebuilt = provider.rebuildFilter(filter)
        assertEquals("My Filter", rebuilt.title)
    }

    // --- SQL structure of specific filters ---

    @Test
    fun dueDateFilterSqlContainsActiveAndVisible() {
        val criterion = provider.dueDateFilter
        assertTrue(criterion.sql.contains("deleted"))
    }

    @Test
    fun startDateFilterEntryValuesMatchDueDate() {
        val startCriterion = provider.startDateFilter as MultipleSelectCriterion
        val dueCriterion = provider.dueDateFilter as MultipleSelectCriterion
        // Both should have the same PermaSql entry values
        assertEquals(startCriterion.entryValues.size, dueCriterion.entryValues.size)
        for (i in startCriterion.entryValues.indices) {
            assertEquals(startCriterion.entryValues[i], dueCriterion.entryValues[i])
        }
    }

    @Test
    fun reminderFilterSqlUsesExists() = runTest {
        `when`(tagDataDao.tagDataOrderedByName()).thenReturn(emptyList())
        `when`(caldavDao.getCalendars()).thenReturn(emptyList())
        val criteria = provider.all()
        val reminder = criteria.find { it.identifier == "reminders" }!!
        assertTrue(reminder.sql.contains("EXISTS"))
    }

    @Test
    fun parentFilterSqlUsesJoinOnChildren() = runTest {
        `when`(tagDataDao.tagDataOrderedByName()).thenReturn(emptyList())
        `when`(caldavDao.getCalendars()).thenReturn(emptyList())
        val criteria = provider.all()
        val parent = criteria.find { it.identifier == "parent" }!!
        assertTrue(parent.sql.contains("children.parent"))
    }

    @Test
    fun completedFilterSqlChecksCompletionDateIsPositive() = runTest {
        `when`(tagDataDao.tagDataOrderedByName()).thenReturn(emptyList())
        `when`(caldavDao.getCalendars()).thenReturn(emptyList())
        val criteria = provider.all()
        val completed = criteria.find { it.identifier == "completed" }!!
        // completed tasks have completed > 0, so the SQL checks completed < 1 = 0
        assertTrue(completed.sql.contains("completed"))
    }

    @Test
    fun hiddenFilterSqlReferencesNow() = runTest {
        `when`(tagDataDao.tagDataOrderedByName()).thenReturn(emptyList())
        `when`(caldavDao.getCalendars()).thenReturn(emptyList())
        val criteria = provider.all()
        val hidden = criteria.find { it.identifier == "hidden" }!!
        assertTrue(hidden.sql.contains(PermaSql.VALUE_NOW))
    }

    @Test
    fun caldavFilterSqlChecksCaldavDeleted() = runTest {
        `when`(tagDataDao.tagDataOrderedByName()).thenReturn(emptyList())
        `when`(caldavDao.getCalendars()).thenReturn(emptyList())
        val criteria = provider.all()
        val caldav = criteria.find { it.identifier == "caldavlist" }!!
        assertTrue(caldav.sql.contains("cd_deleted"))
    }

    @Test
    fun tagFilterSqlJoinsTaskTable() = runTest {
        `when`(tagDataDao.tagDataOrderedByName()).thenReturn(emptyList())
        `when`(caldavDao.getCalendars()).thenReturn(emptyList())
        val criteria = provider.all()
        val tagFilter = criteria.find { it.identifier == "tag_is" }!!
        assertTrue(tagFilter.sql.contains("INNER JOIN"))
    }
}
