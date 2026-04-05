package org.tasks.filters

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.tasks.themes.TasksIcons

class DebugFiltersTest {

    @Test fun noListFilterTitle() = assertEquals("No list", DebugFilters.getNoListFilter().title)
    @Test fun noListFilterSqlContainsJoin() { assertTrue(DebugFilters.getNoListFilter().sql!!.contains("JOIN")) }
    @Test fun noListFilterSqlContainsCaldavTasks() { assertTrue(DebugFilters.getNoListFilter().sql!!.contains("caldav_tasks")) }
    @Test fun noListFilterSqlChecksForNull() { assertTrue(DebugFilters.getNoListFilter().sql!!.contains("NULL")) }
    @Test fun noListFilterIcon() = assertEquals(TasksIcons.CLOUD_OFF, DebugFilters.getNoListFilter().icon)

    @Test fun deletedFilterTitle() = assertEquals("Deleted", DebugFilters.getDeleted().title)
    @Test fun deletedFilterSqlChecksDeletionDate() { assertTrue(DebugFilters.getDeleted().sql!!.contains("deleted")) }
    @Test fun deletedFilterSqlChecksGreaterThanZero() { assertTrue(DebugFilters.getDeleted().sql!!.contains(">")) }
    @Test fun deletedFilterIcon() = assertEquals(TasksIcons.DELETE, DebugFilters.getDeleted().icon)

    @Test fun missingListFilterTitle() = assertEquals("Missing list", DebugFilters.getMissingListFilter().title)
    @Test fun missingListFilterSqlContainsCaldavTasks() { assertTrue(DebugFilters.getMissingListFilter().sql!!.contains("caldav_tasks")) }
    @Test fun missingListFilterSqlContainsCaldavLists() { assertTrue(DebugFilters.getMissingListFilter().sql!!.contains("caldav_lists")) }
    @Test fun missingListFilterSqlHasTwoJoins() { assertEquals(2, Regex("JOIN").findAll(DebugFilters.getMissingListFilter().sql!!).count()) }
    @Test fun missingListFilterIcon() = assertEquals(TasksIcons.CLOUD_OFF, DebugFilters.getMissingListFilter().icon)

    @Test fun missingAccountFilterTitle() = assertEquals("Missing account", DebugFilters.getMissingAccountFilter().title)
    @Test fun missingAccountFilterSqlContainsCaldavAccounts() { assertTrue(DebugFilters.getMissingAccountFilter().sql!!.contains("caldav_accounts")) }
    @Test fun missingAccountFilterSqlHasThreeJoins() { assertEquals(3, Regex("JOIN").findAll(DebugFilters.getMissingAccountFilter().sql!!).count()) }
    @Test fun missingAccountFilterIcon() = assertEquals(TasksIcons.CLOUD_OFF, DebugFilters.getMissingAccountFilter().icon)

    @Test fun noTitleFilterTitle() = assertEquals("No title", DebugFilters.getNoTitleFilter().title)
    @Test fun noTitleFilterSqlContainsTitle() { assertTrue(DebugFilters.getNoTitleFilter().sql!!.contains("title")) }
    @Test fun noTitleFilterIcon() = assertEquals(TasksIcons.CLEAR, DebugFilters.getNoTitleFilter().icon)

    @Test fun noCreateDateFilterTitle() = assertEquals("No create time", DebugFilters.getNoCreateDateFilter().title)
    @Test fun noCreateDateFilterSqlContainsCreated() { assertTrue(DebugFilters.getNoCreateDateFilter().sql!!.contains("created")) }
    @Test fun noCreateDateFilterIcon() = assertEquals(TasksIcons.ADD, DebugFilters.getNoCreateDateFilter().icon)

    @Test fun noModificationDateFilterTitle() = assertEquals("No modify time", DebugFilters.getNoModificationDateFilter().title)
    @Test fun noModificationDateFilterSqlContainsModified() { assertTrue(DebugFilters.getNoModificationDateFilter().sql!!.contains("modified")) }
    @Test fun noModificationDateFilterIcon() = assertEquals(TasksIcons.EDIT, DebugFilters.getNoModificationDateFilter().icon)

    @Test fun areItemsTheSameMatchesBySql() {
        val a = DebugFilter(title = "A", sql = "SELECT 1", icon = null)
        val b = DebugFilter(title = "B", sql = "SELECT 1", icon = "star")
        assertTrue(a.areItemsTheSame(b))
    }

    @Test fun areItemsTheSameDoesNotMatchDifferentSql() {
        val a = DebugFilter(title = "A", sql = "SELECT 1", icon = null)
        val b = DebugFilter(title = "A", sql = "SELECT 2", icon = null)
        assertTrue(!a.areItemsTheSame(b))
    }

    @Test fun areItemsTheSameDoesNotMatchNonFilter() {
        val a = DebugFilter(title = "A", sql = "SELECT 1", icon = null)
        val b = NavigationDrawerSubheader(title = "A", error = false, isCollapsed = false, subheaderType = NavigationDrawerSubheader.SubheaderType.PREFERENCE, id = "0")
        assertTrue(!a.areItemsTheSame(b))
    }

    @Test fun allFiltersProduceNonNullSql() {
        assertNotNull(DebugFilters.getNoListFilter().sql)
        assertNotNull(DebugFilters.getDeleted().sql)
        assertNotNull(DebugFilters.getMissingListFilter().sql)
        assertNotNull(DebugFilters.getMissingAccountFilter().sql)
        assertNotNull(DebugFilters.getNoTitleFilter().sql)
        assertNotNull(DebugFilters.getNoCreateDateFilter().sql)
        assertNotNull(DebugFilters.getNoModificationDateFilter().sql)
    }

    @Test fun allFilterTitlesAreDistinct() {
        val titles = listOf(
            DebugFilters.getNoListFilter().title, DebugFilters.getDeleted().title,
            DebugFilters.getMissingListFilter().title, DebugFilters.getMissingAccountFilter().title,
            DebugFilters.getNoTitleFilter().title, DebugFilters.getNoCreateDateFilter().title,
            DebugFilters.getNoModificationDateFilter().title,
        )
        assertEquals(titles.size, titles.toSet().size)
    }
}
