package org.tasks.filters

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.tasks.data.entity.Place
import org.tasks.data.entity.TagData
import org.tasks.themes.TasksIcons

class FilterTypesTest {

    // ===== TagFilter tests =====

    private fun tagFilter(
        id: Long? = 1L,
        remoteId: String? = "tag-uuid-1",
        name: String? = "Work",
        color: Int? = 0,
        icon: String? = null,
        order: Int = 0,
    ) = TagFilter(
        tagData = TagData(
            id = id,
            remoteId = remoteId,
            name = name,
            color = color,
            icon = icon,
            order = order,
        )
    )

    @Test
    fun tagFilterTitleFromTagData() {
        assertEquals("Work", tagFilter(name = "Work").title)
    }

    @Test
    fun tagFilterTitleEmptyWhenNull() {
        assertEquals("", tagFilter(name = null).title)
    }

    @Test
    fun tagFilterSqlContainsTagUid() {
        val sql = tagFilter(remoteId = "my-tag-uid").sql
        assertTrue(sql.contains("my-tag-uid"))
    }

    @Test
    fun tagFilterSqlContainsJoin() {
        val sql = tagFilter().sql
        assertTrue(sql.contains("JOIN"))
    }

    @Test
    fun tagFilterValuesForNewTasksContainsTagName() {
        val values = tagFilter(name = "Shopping").valuesForNewTasks
        assertTrue(values.contains("Shopping"))
    }

    @Test
    fun tagFilterValuesForNewTasksContainsKey() {
        val values = tagFilter(name = "Shopping").valuesForNewTasks
        assertTrue(values.contains(org.tasks.data.entity.Tag.KEY))
    }

    @Test
    fun tagFilterUuidFromTagData() {
        assertEquals("tag-uuid-1", tagFilter(remoteId = "tag-uuid-1").uuid)
    }

    @Test
    fun tagFilterIconFromTagData() {
        assertEquals("star", tagFilter(icon = "star").icon)
    }

    @Test
    fun tagFilterIconNullWhenNotSet() {
        assertNull(tagFilter(icon = null).icon)
    }

    @Test
    fun tagFilterTintFromTagData() {
        assertEquals(42, tagFilter(color = 42).tint)
    }

    @Test
    fun tagFilterTintDefaultsToZero() {
        assertEquals(0, tagFilter(color = null).tint)
    }

    @Test
    fun tagFilterOrderFromTagData() {
        assertEquals(5, tagFilter(order = 5).order)
    }

    @Test
    fun tagFilterAreItemsTheSameSameId() {
        val a = tagFilter(id = 1L)
        val b = tagFilter(id = 1L, name = "Different")
        assertTrue(a.areItemsTheSame(b))
    }

    @Test
    fun tagFilterAreItemsTheSameDifferentId() {
        val a = tagFilter(id = 1L)
        val b = tagFilter(id = 2L)
        assertFalse(a.areItemsTheSame(b))
    }

    @Test
    fun tagFilterAreItemsTheSameWrongType() {
        val tag = tagFilter()
        val place = placeFilter()
        assertFalse(tag.areItemsTheSame(place))
    }

    @Test
    fun tagFilterItemType() {
        assertEquals(FilterListItem.Type.ITEM, tagFilter().itemType)
    }

    @Test
    fun tagFilterFilterOverrideDefault() {
        assertNull(tagFilter().filterOverride)
    }

    @Test
    fun tagFilterGetSqlQueryUsesOverride() {
        val filter = tagFilter()
        filter.filterOverride = "custom SQL"
        assertEquals("custom SQL", filter.getSqlQuery())
    }

    @Test
    fun tagFilterGetSqlQueryFallsBackToSql() {
        val filter = tagFilter()
        filter.filterOverride = null
        assertEquals(filter.sql, filter.getSqlQuery())
    }

    // ===== PlaceFilter tests =====

    private fun placeFilter(
        id: Long = 1L,
        uid: String? = "place-uid-1",
        name: String? = "Office",
        address: String? = "123 Main St",
        color: Int = 0,
        icon: String? = null,
        order: Int = 0,
    ) = PlaceFilter(
        place = Place(
            id = id,
            uid = uid,
            name = name,
            address = address,
            color = color,
            icon = icon,
            order = order,
        )
    )

    @Test
    fun placeFilterTitleFromPlace() {
        assertEquals("Office", placeFilter(name = "Office").title)
    }

    @Test
    fun placeFilterTitleFallsBackToAddress() {
        assertEquals("123 Main St", placeFilter(name = null, address = "123 Main St").title)
    }

    @Test
    fun placeFilterSqlContainsPlaceUid() {
        val sql = placeFilter(uid = "my-place-uid").sql
        assertTrue(sql.contains("my-place-uid"))
    }

    @Test
    fun placeFilterSqlContainsJoins() {
        val sql = placeFilter().sql
        // Two joins: G2 (geofence) and P2 (place)
        assertTrue(sql.contains("JOIN"))
    }

    @Test
    fun placeFilterValuesForNewTasksContainsUid() {
        val values = placeFilter(uid = "place-uid-1").valuesForNewTasks
        assertTrue(values.contains("place-uid-1"))
    }

    @Test
    fun placeFilterValuesForNewTasksContainsKey() {
        val values = placeFilter().valuesForNewTasks
        assertTrue(values.contains(Place.KEY))
    }

    @Test
    fun placeFilterUidFromPlace() {
        assertEquals("place-uid-1", placeFilter(uid = "place-uid-1").uid)
    }

    @Test
    fun placeFilterIconFromPlace() {
        assertEquals("star", placeFilter(icon = "star").icon)
    }

    @Test
    fun placeFilterIconDefaultsToLocationOn() {
        assertEquals(TasksIcons.PLACE, placeFilter(icon = null).icon)
    }

    @Test
    fun placeFilterIconDefaultsWhenBlank() {
        assertEquals(TasksIcons.PLACE, placeFilter(icon = "").icon)
    }

    @Test
    fun placeFilterTintFromPlace() {
        assertEquals(42, placeFilter(color = 42).tint)
    }

    @Test
    fun placeFilterOrderFromPlace() {
        assertEquals(7, placeFilter(order = 7).order)
    }

    @Test
    fun placeFilterAreItemsTheSameSameId() {
        val a = placeFilter(id = 1L)
        val b = placeFilter(id = 1L, name = "Different")
        assertTrue(a.areItemsTheSame(b))
    }

    @Test
    fun placeFilterAreItemsTheSameDifferentId() {
        val a = placeFilter(id = 1L)
        val b = placeFilter(id = 2L)
        assertFalse(a.areItemsTheSame(b))
    }

    @Test
    fun placeFilterAreItemsTheSameWrongType() {
        val place = placeFilter()
        val tag = tagFilter()
        assertFalse(place.areItemsTheSame(tag))
    }

    @Test
    fun placeFilterItemType() {
        assertEquals(FilterListItem.Type.ITEM, placeFilter().itemType)
    }

    // ===== CustomFilter tests =====

    private fun customFilter(
        id: Long = 1L,
        title: String? = "My Filter",
        sql: String? = "SELECT * FROM tasks",
        values: String? = null,
        criterion: String? = null,
        color: Int? = 0,
        icon: String? = null,
        order: Int = 0,
    ) = CustomFilter(
        filter = org.tasks.data.entity.Filter(
            id = id,
            title = title,
            sql = sql,
            values = values,
            criterion = criterion,
            color = color,
            icon = icon,
            order = order,
        )
    )

    @Test
    fun customFilterTitleFromFilter() {
        assertEquals("My Filter", customFilter(title = "My Filter").title)
    }

    @Test
    fun customFilterTitleEmptyWhenNull() {
        assertEquals("", customFilter(title = null).title)
    }

    @Test
    fun customFilterSqlFromFilter() {
        assertEquals("SELECT * FROM tasks", customFilter(sql = "SELECT * FROM tasks").sql)
    }

    @Test
    fun customFilterValuesFromFilter() {
        assertEquals("someValues", customFilter(values = "someValues").valuesForNewTasks)
    }

    @Test
    fun customFilterValuesNullWhenNotSet() {
        assertNull(customFilter(values = null).valuesForNewTasks)
    }

    @Test
    fun customFilterCriterionFromFilter() {
        assertEquals("myCriterion", customFilter(criterion = "myCriterion").criterion)
    }

    @Test
    fun customFilterCriterionNullWhenNotSet() {
        assertNull(customFilter(criterion = null).criterion)
    }

    @Test
    fun customFilterIdFromFilter() {
        assertEquals(42L, customFilter(id = 42L).id)
    }

    @Test
    fun customFilterOrderFromFilter() {
        assertEquals(3, customFilter(order = 3).order)
    }

    @Test
    fun customFilterIconFromFilter() {
        assertEquals("star", customFilter(icon = "star").icon)
    }

    @Test
    fun customFilterIconDefaultsToFilterList() {
        assertEquals(TasksIcons.FILTER_LIST, customFilter(icon = null).icon)
    }

    @Test
    fun customFilterTintFromFilter() {
        assertEquals(99, customFilter(color = 99).tint)
    }

    @Test
    fun customFilterTintDefaultsToZero() {
        assertEquals(0, customFilter(color = null).tint)
    }

    @Test
    fun customFilterAreItemsTheSameSameId() {
        val a = customFilter(id = 1L)
        val b = customFilter(id = 1L, title = "Different")
        assertTrue(a.areItemsTheSame(b))
    }

    @Test
    fun customFilterAreItemsTheSameDifferentId() {
        val a = customFilter(id = 1L)
        val b = customFilter(id = 2L)
        assertFalse(a.areItemsTheSame(b))
    }

    @Test
    fun customFilterAreItemsTheSameWrongType() {
        val custom = customFilter()
        val tag = tagFilter()
        assertFalse(custom.areItemsTheSame(tag))
    }

    @Test
    fun customFilterItemType() {
        assertEquals(FilterListItem.Type.ITEM, customFilter().itemType)
    }

    // ===== Cross-type tests =====

    @Test
    fun tagFilterNotEqualToPlaceFilter() {
        assertFalse(tagFilter().areItemsTheSame(placeFilter()))
    }

    @Test
    fun placeFilterNotEqualToCustomFilter() {
        assertFalse(placeFilter().areItemsTheSame(customFilter()))
    }

    @Test
    fun customFilterNotEqualToTagFilter() {
        assertFalse(customFilter().areItemsTheSame(tagFilter()))
    }

    // ===== key() extension function tests =====

    @Test
    fun tagFilterKey() {
        val filter = tagFilter(remoteId = "abc-123")
        assertEquals("tag_abc-123", filter.key())
    }

    @Test
    fun placeFilterKey() {
        val filter = placeFilter(uid = "place-xyz")
        assertEquals("place_place-xyz", filter.key())
    }

    @Test
    fun customFilterKey() {
        val filter = customFilter(id = 42L)
        assertEquals("custom_42", filter.key())
    }

    // ===== Filter base class behavior =====

    @Test
    fun filterIsWritableByDefault() {
        assertTrue(tagFilter().isWritable)
    }

    @Test
    fun filterSupportsSubtasksByDefault() {
        assertTrue(tagFilter().supportsSubtasks())
    }

    @Test
    fun filterSupportsHiddenTasksByDefault() {
        assertTrue(placeFilter().supportsHiddenTasks())
    }

    @Test
    fun filterSupportsSortingByDefault() {
        assertTrue(customFilter().supportsSorting())
    }

    @Test
    fun filterDoesNotDisableHeadersByDefault() {
        assertFalse(customFilter().disableHeaders())
    }

    @Test
    fun tagFilterCountDefaultsToNoCount() {
        assertEquals(org.tasks.data.NO_COUNT, tagFilter().count)
    }

    @Test
    fun placeFilterCountDefaultsToNoCount() {
        assertEquals(org.tasks.data.NO_COUNT, placeFilter().count)
    }

    @Test
    fun tagFilterCountCanBeSet() {
        val filter = TagFilter(
            tagData = TagData(id = 1L, remoteId = "x", name = "t"),
            count = 5,
        )
        assertEquals(5, filter.count)
    }

    @Test
    fun placeFilterCountCanBeSet() {
        val filter = PlaceFilter(
            place = Place(id = 1L, uid = "x", name = "p"),
            count = 10,
        )
        assertEquals(10, filter.count)
    }
}
