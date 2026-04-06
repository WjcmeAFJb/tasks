package org.tasks.filters

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.tasks.data.entity.CaldavAccount
import org.tasks.data.entity.CaldavCalendar
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

    // ===== EmptyFilter tests =====

    @Test
    fun emptyFilterDefaultSql() {
        assertEquals("WHERE 0", EmptyFilter().sql)
    }

    @Test
    fun emptyFilterDefaultTitle() {
        assertEquals("", EmptyFilter().title)
    }

    @Test
    fun emptyFilterCustomSql() {
        val filter = EmptyFilter(sql = "WHERE 1=0")
        assertEquals("WHERE 1=0", filter.sql)
    }

    @Test
    fun emptyFilterCustomTitle() {
        val filter = EmptyFilter(title = "Empty")
        assertEquals("Empty", filter.title)
    }

    @Test
    fun emptyFilterAreItemsTheSameAlwaysFalse() {
        val a = EmptyFilter()
        val b = EmptyFilter()
        assertFalse(a.areItemsTheSame(b))
    }

    @Test
    fun emptyFilterAreItemsTheSameWithOtherTypes() {
        val empty = EmptyFilter()
        val tag = tagFilter()
        assertFalse(empty.areItemsTheSame(tag))
    }

    @Test
    fun emptyFilterItemType() {
        assertEquals(FilterListItem.Type.ITEM, EmptyFilter().itemType)
    }

    @Test
    fun emptyFilterKey() {
        assertEquals("builtin_empty", EmptyFilter().key())
    }

    // ===== FilterImpl tests =====

    @Test
    fun filterImplDefaultValues() {
        val f = FilterImpl()
        assertEquals("", f.title)
        assertNull(f.sql)
        assertNull(f.valuesForNewTasks)
        assertNull(f.icon)
        assertEquals(0, f.tint)
    }

    @Test
    fun filterImplCustomValues() {
        val f = FilterImpl(
            title = "My List",
            sql = "SELECT 1",
            valuesForNewTasks = "key|val|",
            icon = "star",
            tint = 42,
        )
        assertEquals("My List", f.title)
        assertEquals("SELECT 1", f.sql)
        assertEquals("key|val|", f.valuesForNewTasks)
        assertEquals("star", f.icon)
        assertEquals(42, f.tint)
    }

    @Test
    fun filterImplAreItemsTheSameMatchesBySql() {
        val a = FilterImpl(title = "A", sql = "SELECT 1")
        val b = FilterImpl(title = "B", sql = "SELECT 1")
        assertTrue(a.areItemsTheSame(b))
    }

    @Test
    fun filterImplAreItemsTheSameDifferentSql() {
        val a = FilterImpl(title = "A", sql = "SELECT 1")
        val b = FilterImpl(title = "A", sql = "SELECT 2")
        assertFalse(a.areItemsTheSame(b))
    }

    @Test
    fun filterImplAreItemsTheSameWrongType() {
        val impl = FilterImpl(sql = "SELECT 1")
        val tag = tagFilter()
        // TagFilter is a Filter, so areItemsTheSame checks sql equality
        // Unless sql matches it will be false
        assertFalse(impl.areItemsTheSame(tag))
    }

    @Test
    fun filterImplItemType() {
        assertEquals(FilterListItem.Type.ITEM, FilterImpl().itemType)
    }

    // ===== SubtaskFilter tests =====

    @Test
    fun subtaskFilterTitle() {
        assertEquals("subtasks", SubtaskFilter(parent = 1L).title)
    }

    @Test
    fun subtaskFilterSqlContainsParent() {
        val filter = SubtaskFilter(parent = 42L)
        assertTrue(filter.sql.contains("42"))
    }

    @Test
    fun subtaskFilterDisablesHeaders() {
        assertTrue(SubtaskFilter(parent = 1L).disableHeaders())
    }

    @Test
    fun subtaskFilterAreItemsTheSameSameParent() {
        val a = SubtaskFilter(parent = 1L)
        val b = SubtaskFilter(parent = 1L)
        assertTrue(a.areItemsTheSame(b))
    }

    @Test
    fun subtaskFilterAreItemsTheSameDifferentParent() {
        val a = SubtaskFilter(parent = 1L)
        val b = SubtaskFilter(parent = 2L)
        assertFalse(a.areItemsTheSame(b))
    }

    @Test
    fun subtaskFilterAreItemsTheSameWrongType() {
        val subtask = SubtaskFilter(parent = 1L)
        val empty = EmptyFilter()
        assertFalse(subtask.areItemsTheSame(empty))
    }

    @Test
    fun subtaskFilterItemType() {
        assertEquals(FilterListItem.Type.ITEM, SubtaskFilter(parent = 1L).itemType)
    }

    // ===== SearchFilter tests =====

    @Test
    fun searchFilterTitle() {
        assertEquals("Search", SearchFilter(title = "Search", query = "test").title)
    }

    @Test
    fun searchFilterQuery() {
        assertEquals("test", SearchFilter(title = "Search", query = "test").query)
    }

    @Test
    fun searchFilterSqlContainsQuery() {
        val sql = SearchFilter(title = "Search", query = "hello").sql
        assertTrue(sql.contains("%hello%"))
    }

    @Test
    fun searchFilterSqlContainsDeletionDateCheck() {
        val sql = SearchFilter(title = "Search", query = "x").sql
        assertTrue(sql.contains("deleted"))
    }

    @Test
    fun searchFilterSqlContainsTitleSearch() {
        val sql = SearchFilter(title = "Search", query = "x").sql
        assertTrue(sql.contains("title"))
    }

    @Test
    fun searchFilterSqlContainsNotesSearch() {
        val sql = SearchFilter(title = "Search", query = "x").sql
        assertTrue(sql.contains("notes"))
    }

    @Test
    fun searchFilterSqlContainsTagSearch() {
        val sql = SearchFilter(title = "Search", query = "x").sql
        assertTrue(sql.contains("tags"))
    }

    @Test
    fun searchFilterDoesNotSupportHiddenTasks() {
        assertFalse(SearchFilter(title = "Search", query = "x").supportsHiddenTasks())
    }

    @Test
    fun searchFilterAreItemsTheSame() {
        val a = SearchFilter(title = "A", query = "x")
        val b = SearchFilter(title = "B", query = "y")
        assertTrue(a.areItemsTheSame(b))
    }

    @Test
    fun searchFilterAreItemsTheSameWrongType() {
        val search = SearchFilter(title = "S", query = "x")
        val empty = EmptyFilter()
        assertFalse(search.areItemsTheSame(empty))
    }

    @Test
    fun searchFilterKey() {
        assertEquals("builtin_search", SearchFilter(title = "S", query = "x").key())
    }

    // ===== NotificationsFilter tests =====

    @Test
    fun notificationsFilterTitle() {
        assertEquals("Notifications", NotificationsFilter(title = "Notifications").title)
    }

    @Test
    fun notificationsFilterIcon() {
        assertEquals(TasksIcons.NOTIFICATIONS, NotificationsFilter(title = "N").icon)
    }

    @Test
    fun notificationsFilterSqlContainsJoin() {
        assertTrue(NotificationsFilter(title = "N").sql.contains("JOIN"))
    }

    @Test
    fun notificationsFilterSqlContainsNotification() {
        assertTrue(NotificationsFilter(title = "N").sql.contains("notification"))
    }

    @Test
    fun notificationsFilterDoesNotSupportHiddenTasks() {
        assertFalse(NotificationsFilter(title = "N").supportsHiddenTasks())
    }

    @Test
    fun notificationsFilterAreItemsTheSame() {
        val a = NotificationsFilter(title = "A")
        val b = NotificationsFilter(title = "B")
        assertTrue(a.areItemsTheSame(b))
    }

    @Test
    fun notificationsFilterAreItemsTheSameWrongType() {
        assertFalse(NotificationsFilter(title = "N").areItemsTheSame(EmptyFilter()))
    }

    @Test
    fun notificationsFilterKey() {
        assertEquals("builtin_notifications", NotificationsFilter(title = "N").key())
    }

    @Test
    fun notificationsFilterItemType() {
        assertEquals(FilterListItem.Type.ITEM, NotificationsFilter(title = "N").itemType)
    }

    // ===== TimerFilter tests =====

    @Test
    fun timerFilterTitle() {
        assertEquals("Timer", TimerFilter(title = "Timer").title)
    }

    @Test
    fun timerFilterIcon() {
        assertEquals(TasksIcons.TIMER, TimerFilter(title = "T").icon)
    }

    @Test
    fun timerFilterSqlContainsTimerStart() {
        assertTrue(TimerFilter(title = "T").sql.contains("timerStart"))
    }

    @Test
    fun timerFilterSqlContainsDeletionCheck() {
        assertTrue(TimerFilter(title = "T").sql.contains("deleted"))
    }

    @Test
    fun timerFilterAreItemsTheSame() {
        val a = TimerFilter(title = "A")
        val b = TimerFilter(title = "B")
        assertTrue(a.areItemsTheSame(b))
    }

    @Test
    fun timerFilterAreItemsTheSameWrongType() {
        assertFalse(TimerFilter(title = "T").areItemsTheSame(EmptyFilter()))
    }

    @Test
    fun timerFilterKey() {
        assertEquals("builtin_timer", TimerFilter(title = "T").key())
    }

    @Test
    fun timerFilterItemType() {
        assertEquals(FilterListItem.Type.ITEM, TimerFilter(title = "T").itemType)
    }

    // ===== SnoozedFilter tests =====

    @Test
    fun snoozedFilterTitle() {
        assertEquals("Snoozed", SnoozedFilter(title = "Snoozed").title)
    }

    @Test
    fun snoozedFilterIcon() {
        assertEquals(TasksIcons.SNOOZE, SnoozedFilter(title = "S").icon)
    }

    @Test
    fun snoozedFilterSqlContainsAlarms() {
        assertTrue(SnoozedFilter(title = "S").sql.contains("alarms"))
    }

    @Test
    fun snoozedFilterSqlContainsSnoozeType() {
        // Alarm.TYPE_SNOOZE = 4
        assertTrue(SnoozedFilter(title = "S").sql.contains("4"))
    }

    @Test
    fun snoozedFilterDoesNotSupportHiddenTasks() {
        assertFalse(SnoozedFilter(title = "S").supportsHiddenTasks())
    }

    @Test
    fun snoozedFilterAreItemsTheSame() {
        val a = SnoozedFilter(title = "A")
        val b = SnoozedFilter(title = "B")
        assertTrue(a.areItemsTheSame(b))
    }

    @Test
    fun snoozedFilterAreItemsTheSameWrongType() {
        assertFalse(SnoozedFilter(title = "S").areItemsTheSame(EmptyFilter()))
    }

    @Test
    fun snoozedFilterKey() {
        assertEquals("builtin_snoozed", SnoozedFilter(title = "S").key())
    }

    // ===== RecentlyModifiedFilter tests =====

    @Test
    fun recentlyModifiedFilterTitle() {
        assertEquals("Recent", RecentlyModifiedFilter(title = "Recent").title)
    }

    @Test
    fun recentlyModifiedFilterIcon() {
        assertEquals(TasksIcons.HISTORY, RecentlyModifiedFilter(title = "R").icon)
    }

    @Test
    fun recentlyModifiedFilterSqlContainsDeletionCheck() {
        assertTrue(RecentlyModifiedFilter(title = "R").sql.contains("deleted"))
    }

    @Test
    fun recentlyModifiedFilterSqlContainsModified() {
        assertTrue(RecentlyModifiedFilter(title = "R").sql.contains("modified"))
    }

    @Test
    fun recentlyModifiedFilterSqlContainsOrderBy() {
        assertTrue(RecentlyModifiedFilter(title = "R").sql.contains("ORDER BY"))
    }

    @Test
    fun recentlyModifiedFilterDoesNotSupportHiddenTasks() {
        assertFalse(RecentlyModifiedFilter(title = "R").supportsHiddenTasks())
    }

    @Test
    fun recentlyModifiedFilterDoesNotSupportSubtasks() {
        assertFalse(RecentlyModifiedFilter(title = "R").supportsSubtasks())
    }

    @Test
    fun recentlyModifiedFilterDoesNotSupportSorting() {
        assertFalse(RecentlyModifiedFilter(title = "R").supportsSorting())
    }

    @Test
    fun recentlyModifiedFilterDisablesHeaders() {
        assertTrue(RecentlyModifiedFilter(title = "R").disableHeaders())
    }

    @Test
    fun recentlyModifiedFilterAreItemsTheSame() {
        val a = RecentlyModifiedFilter(title = "A")
        val b = RecentlyModifiedFilter(title = "B")
        assertTrue(a.areItemsTheSame(b))
    }

    @Test
    fun recentlyModifiedFilterAreItemsTheSameWrongType() {
        assertFalse(RecentlyModifiedFilter(title = "R").areItemsTheSame(EmptyFilter()))
    }

    @Test
    fun recentlyModifiedFilterKey() {
        assertEquals("builtin_recently_modified", RecentlyModifiedFilter(title = "R").key())
    }

    // ===== MyTasksFilter tests =====

    @Test
    fun myTasksFilterTitle() {
        assertEquals("My Tasks", MyTasksFilter(title = "My Tasks").title)
    }

    @Test
    fun myTasksFilterIcon() {
        assertEquals(TasksIcons.ALL_INBOX, MyTasksFilter(title = "M").icon)
    }

    @Test
    fun myTasksFilterSqlContainsParentZero() {
        assertTrue(MyTasksFilter(title = "M").sql.contains("parent"))
    }

    @Test
    fun myTasksFilterAreItemsTheSame() {
        val a = MyTasksFilter(title = "A")
        val b = MyTasksFilter(title = "B")
        assertTrue(a.areItemsTheSame(b))
    }

    @Test
    fun myTasksFilterAreItemsTheSameWrongType() {
        assertFalse(MyTasksFilter(title = "M").areItemsTheSame(EmptyFilter()))
    }

    @Test
    fun myTasksFilterKey() {
        assertEquals("builtin_my_tasks", MyTasksFilter(title = "M").key())
    }

    @Test
    fun myTasksFilterFilterOverrideDefault() {
        assertNull(MyTasksFilter(title = "M").filterOverride)
    }

    @Test
    fun myTasksFilterGetSqlQueryUsesOverride() {
        val filter = MyTasksFilter(title = "M")
        filter.filterOverride = "override SQL"
        assertEquals("override SQL", filter.getSqlQuery())
    }

    @Test
    fun myTasksFilterGetSqlQueryFallsBackToSql() {
        val filter = MyTasksFilter(title = "M")
        filter.filterOverride = null
        assertEquals(filter.sql, filter.getSqlQuery())
    }

    // ===== TodayFilter tests =====

    @Test
    fun todayFilterTitle() {
        assertEquals("Today", TodayFilter(title = "Today").title)
    }

    @Test
    fun todayFilterIcon() {
        assertEquals(TasksIcons.TODAY, TodayFilter(title = "T").icon)
    }

    @Test
    fun todayFilterSqlContainsDueDate() {
        assertTrue(TodayFilter(title = "T").sql.contains("dueDate"))
    }

    @Test
    fun todayFilterValuesForNewTasksContainsDueDate() {
        val values = TodayFilter(title = "T").valuesForNewTasks
        assertTrue(values.contains("dueDate"))
    }

    @Test
    fun todayFilterAreItemsTheSame() {
        val a = TodayFilter(title = "A")
        val b = TodayFilter(title = "B")
        assertTrue(a.areItemsTheSame(b))
    }

    @Test
    fun todayFilterAreItemsTheSameWrongType() {
        assertFalse(TodayFilter(title = "T").areItemsTheSame(EmptyFilter()))
    }

    @Test
    fun todayFilterKey() {
        assertEquals("builtin_today", TodayFilter(title = "T").key())
    }

    @Test
    fun todayFilterFilterOverrideDefault() {
        assertNull(TodayFilter(title = "T").filterOverride)
    }

    @Test
    fun todayFilterGetSqlQueryUsesOverride() {
        val filter = TodayFilter(title = "T")
        filter.filterOverride = "override"
        assertEquals("override", filter.getSqlQuery())
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

    @Test
    fun caldavFilterKey() {
        val filter = CaldavFilter(
            calendar = CaldavCalendar(account = "acc1", uuid = "uuid1"),
            account = CaldavAccount()
        )
        assertEquals("list_acc1_uuid1", filter.key())
    }

    @Test
    fun debugFilterKey() {
        val filter = DebugFilter(title = "MyDebugTitle", sql = "SELECT 1", icon = null)
        assertEquals("MyDebugTitle", filter.key())
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
    fun filterDisableHeadersWhenSortingNotSupported() {
        // RecentlyModifiedFilter does not support sorting, so disableHeaders is true
        assertTrue(RecentlyModifiedFilter(title = "R").disableHeaders())
    }

    @Test
    fun filterSupportsManualSortDefault() {
        // Default is false
        assertFalse(tagFilter().supportsManualSort())
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

    // ===== equals/hashCode behavior =====
    // Note: These are data classes, so equals compares all constructor properties.
    // The Filter base class equals uses areItemsTheSame, but data class equals
    // overrides it to compare constructor fields.

    @Test
    fun dataClassEqualsWithSameProperties() {
        val a = tagFilter(id = 1L, name = "Work")
        val b = tagFilter(id = 1L, name = "Work")
        assertEquals(a, b)
    }

    @Test
    fun dataClassNotEqualsWithDifferentProperties() {
        val a = tagFilter(id = 1L, name = "Work")
        val b = tagFilter(id = 1L, name = "Different")
        assertNotEquals(a, b)
    }

    @Test
    fun filterEqualsWithSameObject() {
        val a = tagFilter()
        assertEquals(a, a)
    }

    @Test
    fun filterNotEqualsWithNull() {
        val a = tagFilter()
        assertFalse(a.equals(null))
    }

    @Test
    fun filterNotEqualsWithNonFilter() {
        val a = tagFilter()
        assertFalse(a.equals("not a filter"))
    }

    @Test
    fun emptyFilterDataClassEquals() {
        val a = EmptyFilter()
        val b = EmptyFilter()
        assertEquals(a, b)
    }

    @Test
    fun myTasksFilterDataClassEqualsSameTitle() {
        val a = MyTasksFilter(title = "My Tasks")
        val b = MyTasksFilter(title = "My Tasks")
        assertEquals(a, b)
    }

    @Test
    fun myTasksFilterDataClassNotEqualsDifferentTitle() {
        val a = MyTasksFilter(title = "A")
        val b = MyTasksFilter(title = "B")
        // Data class compares title field
        assertNotEquals(a, b)
    }

    @Test
    fun timerFilterDataClassEquals() {
        val a = TimerFilter(title = "Timer")
        val b = TimerFilter(title = "Timer")
        assertEquals(a, b)
    }

    @Test
    fun notificationsFilterDataClassEquals() {
        val a = NotificationsFilter(title = "Notifications")
        val b = NotificationsFilter(title = "Notifications")
        assertEquals(a, b)
    }

    @Test
    fun snoozedFilterDataClassEquals() {
        val a = SnoozedFilter(title = "Snoozed")
        val b = SnoozedFilter(title = "Snoozed")
        assertEquals(a, b)
    }

    @Test
    fun searchFilterDataClassEquals() {
        val a = SearchFilter(title = "Search", query = "test")
        val b = SearchFilter(title = "Search", query = "test")
        assertEquals(a, b)
    }

    @Test
    fun searchFilterDataClassNotEqualsDifferentQuery() {
        val a = SearchFilter(title = "S", query = "x")
        val b = SearchFilter(title = "S", query = "y")
        assertNotEquals(a, b)
    }

    @Test
    fun differentFilterTypesNotEqual() {
        val timer = TimerFilter(title = "T")
        val notif = NotificationsFilter(title = "N")
        assertNotEquals(timer, notif)
    }

    @Test
    fun filterImplHashCode() {
        val a = FilterImpl(title = "A", sql = "SELECT 1")
        val b = FilterImpl(title = "A", sql = "SELECT 1")
        assertEquals(a.hashCode(), b.hashCode())
    }

    @Test
    fun filterImplDifferentHashCode() {
        val a = FilterImpl(title = "A", sql = "SELECT 1")
        val b = FilterImpl(title = "B", sql = "SELECT 2")
        assertNotEquals(a.hashCode(), b.hashCode())
    }

    // ===== NavigationDrawerSubheader tests =====

    @Test
    fun subheaderItemType() {
        val subheader = NavigationDrawerSubheader(
            title = "Header",
            error = false,
            isCollapsed = false,
            subheaderType = NavigationDrawerSubheader.SubheaderType.PREFERENCE,
            id = "1"
        )
        assertEquals(FilterListItem.Type.SUBHEADER, subheader.itemType)
    }

    @Test
    fun subheaderAreItemsTheSameSameTypeAndId() {
        val a = NavigationDrawerSubheader(
            title = "A", error = false, isCollapsed = false,
            subheaderType = NavigationDrawerSubheader.SubheaderType.PREFERENCE, id = "1"
        )
        val b = NavigationDrawerSubheader(
            title = "B", error = true, isCollapsed = true,
            subheaderType = NavigationDrawerSubheader.SubheaderType.PREFERENCE, id = "1"
        )
        assertTrue(a.areItemsTheSame(b))
    }

    @Test
    fun subheaderAreItemsTheSameDifferentId() {
        val a = NavigationDrawerSubheader(
            title = "A", error = false, isCollapsed = false,
            subheaderType = NavigationDrawerSubheader.SubheaderType.PREFERENCE, id = "1"
        )
        val b = NavigationDrawerSubheader(
            title = "A", error = false, isCollapsed = false,
            subheaderType = NavigationDrawerSubheader.SubheaderType.PREFERENCE, id = "2"
        )
        assertFalse(a.areItemsTheSame(b))
    }

    @Test
    fun subheaderAreItemsTheSameDifferentType() {
        val a = NavigationDrawerSubheader(
            title = "A", error = false, isCollapsed = false,
            subheaderType = NavigationDrawerSubheader.SubheaderType.PREFERENCE, id = "1"
        )
        val b = NavigationDrawerSubheader(
            title = "A", error = false, isCollapsed = false,
            subheaderType = NavigationDrawerSubheader.SubheaderType.CALDAV, id = "1"
        )
        assertFalse(a.areItemsTheSame(b))
    }

    @Test
    fun subheaderAreItemsTheSameWrongType() {
        val subheader = NavigationDrawerSubheader(
            title = "A", error = false, isCollapsed = false,
            subheaderType = NavigationDrawerSubheader.SubheaderType.PREFERENCE, id = "1"
        )
        assertFalse(subheader.areItemsTheSame(EmptyFilter()))
    }

    @Test
    fun subheaderProperties() {
        val subheader = NavigationDrawerSubheader(
            title = "My Header",
            error = true,
            isCollapsed = true,
            subheaderType = NavigationDrawerSubheader.SubheaderType.TASKS,
            id = "42",
            addIntentRc = 100,
            icon = "cloud",
            childCount = 5,
        )
        assertEquals("My Header", subheader.title)
        assertTrue(subheader.error)
        assertTrue(subheader.isCollapsed)
        assertEquals(NavigationDrawerSubheader.SubheaderType.TASKS, subheader.subheaderType)
        assertEquals("42", subheader.id)
        assertEquals(100, subheader.addIntentRc)
        assertEquals("cloud", subheader.icon)
        assertEquals(5, subheader.childCount)
    }

    @Test
    fun subheaderDefaultProperties() {
        val subheader = NavigationDrawerSubheader(
            title = null,
            error = false,
            isCollapsed = false,
            subheaderType = NavigationDrawerSubheader.SubheaderType.CALDAV,
            id = "0"
        )
        assertNull(subheader.title)
        assertFalse(subheader.error)
        assertFalse(subheader.isCollapsed)
        assertEquals(0, subheader.addIntentRc)
        assertNull(subheader.icon)
        assertNull(subheader.accountIcon)
        assertEquals(0, subheader.childCount)
        assertNull(subheader.openTaskApp)
    }

    // ===== DebugFilter tests =====

    @Test
    fun debugFilterTitle() {
        assertEquals("Test", DebugFilter(title = "Test", sql = null, icon = null).title)
    }

    @Test
    fun debugFilterSql() {
        assertEquals("SELECT 1", DebugFilter(title = "T", sql = "SELECT 1", icon = null).sql)
    }

    @Test
    fun debugFilterNullSql() {
        assertNull(DebugFilter(title = "T", sql = null, icon = null).sql)
    }

    @Test
    fun debugFilterIcon() {
        assertEquals("star", DebugFilter(title = "T", sql = null, icon = "star").icon)
    }

    @Test
    fun debugFilterNullIcon() {
        assertNull(DebugFilter(title = "T", sql = null, icon = null).icon)
    }

    @Test
    fun debugFilterAreItemsTheSameMatchesBySql() {
        val a = DebugFilter(title = "A", sql = "SELECT 1", icon = null)
        val b = DebugFilter(title = "B", sql = "SELECT 1", icon = "star")
        assertTrue(a.areItemsTheSame(b))
    }

    @Test
    fun debugFilterAreItemsTheSameDifferentSql() {
        val a = DebugFilter(title = "A", sql = "SELECT 1", icon = null)
        val b = DebugFilter(title = "A", sql = "SELECT 2", icon = null)
        assertFalse(a.areItemsTheSame(b))
    }

    // ===== CaldavFilter.filterPreferencesKey =====

    @Test
    fun filterPreferencesKeyFormat() {
        val cal = CaldavCalendar(account = "myAccount", uuid = "myUuid")
        assertEquals("list_myAccount_myUuid", cal.filterPreferencesKey())
    }

    @Test
    fun filterPreferencesKeyWithDefaultValues() {
        val cal = CaldavCalendar()
        val key = cal.filterPreferencesKey()
        assertTrue(key.startsWith("list_"))
    }

    // ===== isReadOnly / isWritable =====

    @Test
    fun readOnlyFilterIsNotWritable() {
        val filter = CaldavFilter(
            calendar = CaldavCalendar(access = CaldavCalendar.ACCESS_READ_ONLY),
            account = CaldavAccount()
        )
        assertTrue(filter.isReadOnly)
        assertFalse(filter.isWritable)
    }

    @Test
    fun writableFilterIsNotReadOnly() {
        val filter = CaldavFilter(
            calendar = CaldavCalendar(access = CaldavCalendar.ACCESS_OWNER),
            account = CaldavAccount()
        )
        assertFalse(filter.isReadOnly)
        assertTrue(filter.isWritable)
    }

    // ===== ValuesForNewTasks across types =====

    @Test
    fun emptyFilterValuesForNewTasksIsNull() {
        assertNull(EmptyFilter().valuesForNewTasks)
    }

    @Test
    fun filterImplValuesForNewTasksIsNull() {
        assertNull(FilterImpl().valuesForNewTasks)
    }

    @Test
    fun filterImplValuesForNewTasksCanBeSet() {
        assertEquals("val", FilterImpl(valuesForNewTasks = "val").valuesForNewTasks)
    }

    @Test
    fun notificationsFilterValuesForNewTasksIsNull() {
        assertNull(NotificationsFilter(title = "N").valuesForNewTasks)
    }

    @Test
    fun timerFilterValuesForNewTasksIsNull() {
        assertNull(TimerFilter(title = "T").valuesForNewTasks)
    }
}
