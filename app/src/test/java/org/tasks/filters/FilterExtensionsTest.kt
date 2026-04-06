package org.tasks.filters

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import org.tasks.billing.PurchaseState
import org.tasks.data.entity.CaldavAccount
import org.tasks.data.entity.CaldavCalendar
import org.tasks.data.entity.Place
import org.tasks.data.entity.TagData
import org.tasks.themes.TasksIcons

class FilterExtensionsTest {

    private val pro = object : PurchaseState { override val hasPro = true }
    private val free = object : PurchaseState { override val hasPro = false }

    @Test fun tagFilterIconWithPro() {
        val filter = TagFilter(tagData = TagData(name = "t", icon = "star"), count = 0)
        assertEquals("star", filter.getIcon(pro))
    }

    @Test fun tagFilterIconWithoutPro() {
        val filter = TagFilter(tagData = TagData(name = "t", icon = "star"), count = 0)
        assertEquals(TasksIcons.LABEL, filter.getIcon(free))
    }

    @Test fun tagFilterDefaultIcon() {
        val filter = TagFilter(tagData = TagData(name = "t"), count = 0)
        assertEquals(TasksIcons.LABEL, filter.getIcon(free))
    }

    @Test fun caldavFilterIconWithPro() {
        val filter = CaldavFilter(
            calendar = CaldavCalendar(icon = "work"),
            account = CaldavAccount()
        )
        assertEquals("work", filter.getIcon(pro))
    }

    @Test fun caldavFilterDefaultIcon() {
        val filter = CaldavFilter(
            calendar = CaldavCalendar(),
            account = CaldavAccount()
        )
        assertEquals(TasksIcons.LIST, filter.getIcon(free))
    }

    @Test fun placeFilterIconWithPro() {
        val filter = PlaceFilter(place = Place(icon = "home"), count = 0)
        assertEquals("home", filter.getIcon(pro))
    }

    @Test fun placeFilterDefaultIcon() {
        val filter = PlaceFilter(place = Place(), count = 0)
        assertEquals(TasksIcons.PLACE, filter.getIcon(free))
    }

    @Test fun customFilterIconWithPro() {
        val filter = CustomFilter(
            filter = org.tasks.data.entity.Filter(icon = "filter_alt")
        )
        assertEquals("filter_alt", filter.getIcon(pro))
    }

    @Test fun customFilterDefaultIcon() {
        val filter = CustomFilter(
            filter = org.tasks.data.entity.Filter()
        )
        assertEquals(TasksIcons.FILTER_LIST, filter.getIcon(free))
    }

    @Test fun proWithBlankIconFallsBack() {
        val filter = TagFilter(tagData = TagData(name = "t", icon = ""), count = 0)
        assertEquals(TasksIcons.LABEL, filter.getIcon(pro))
    }

    @Test fun proWithNullIconFallsBack() {
        val filter = TagFilter(tagData = TagData(name = "t", icon = null), count = 0)
        assertEquals(TasksIcons.LABEL, filter.getIcon(pro))
    }

    // --- Additional tests for expanded coverage ---

    @Test fun caldavFilterWithBlankIconFallsBackWithPro() {
        val filter = CaldavFilter(
            calendar = CaldavCalendar(icon = ""),
            account = CaldavAccount()
        )
        assertEquals(TasksIcons.LIST, filter.getIcon(pro))
    }

    @Test fun caldavFilterWithNullIconFallsBackWithPro() {
        val filter = CaldavFilter(
            calendar = CaldavCalendar(icon = null),
            account = CaldavAccount()
        )
        assertEquals(TasksIcons.LIST, filter.getIcon(pro))
    }

    @Test fun placeFilterWithBlankIconFallsBackWithPro() {
        val filter = PlaceFilter(place = Place(icon = ""), count = 0)
        assertEquals(TasksIcons.PLACE, filter.getIcon(pro))
    }

    @Test fun placeFilterWithNullIconFallsBackWithPro() {
        val filter = PlaceFilter(place = Place(icon = null), count = 0)
        assertEquals(TasksIcons.PLACE, filter.getIcon(pro))
    }

    @Test fun customFilterWithBlankIconFallsBackWithPro() {
        val filter = CustomFilter(
            filter = org.tasks.data.entity.Filter(icon = "")
        )
        assertEquals(TasksIcons.FILTER_LIST, filter.getIcon(pro))
    }

    @Test fun customFilterWithNullIconFallsBackWithPro() {
        val filter = CustomFilter(
            filter = org.tasks.data.entity.Filter(icon = null)
        )
        assertEquals(TasksIcons.FILTER_LIST, filter.getIcon(pro))
    }

    // FilterImpl and other non-specific filter types use the else branch
    @Test fun filterImplWithProReturnsCustomIcon() {
        val filter = FilterImpl(icon = "star")
        assertEquals("star", filter.getIcon(pro))
    }

    @Test fun filterImplWithProAndNullIconReturnsNull() {
        val filter = FilterImpl(icon = null)
        // else branch returns icon, which is null
        assertNull(filter.getIcon(pro))
    }

    @Test fun filterImplWithFreeReturnsIcon() {
        // Not a TagFilter/CaldavFilter/CustomFilter/PlaceFilter, so else branch -> icon
        val filter = FilterImpl(icon = "star")
        assertEquals("star", filter.getIcon(free))
    }

    @Test fun filterImplWithFreeAndNullIconReturnsNull() {
        val filter = FilterImpl(icon = null)
        assertNull(filter.getIcon(free))
    }

    @Test fun emptyFilterWithFreeReturnsIcon() {
        // EmptyFilter.icon is null (default from Filter)
        val filter = EmptyFilter()
        // Not one of the specific types, so else branch returns icon (null)
        assertNull(filter.getIcon(free))
    }

    @Test fun notificationsFilterWithProReturnsCustomIcon() {
        // NotificationsFilter has icon = TasksIcons.NOTIFICATIONS
        val filter = NotificationsFilter(title = "N")
        assertEquals(TasksIcons.NOTIFICATIONS, filter.getIcon(pro))
    }

    @Test fun notificationsFilterWithFreeReturnsIcon() {
        val filter = NotificationsFilter(title = "N")
        // Not a specific type in the when, so falls to else -> icon
        assertEquals(TasksIcons.NOTIFICATIONS, filter.getIcon(free))
    }

    @Test fun timerFilterWithProReturnsTimerIcon() {
        val filter = TimerFilter(title = "T")
        assertEquals(TasksIcons.TIMER, filter.getIcon(pro))
    }

    @Test fun timerFilterWithFreeReturnsTimerIcon() {
        val filter = TimerFilter(title = "T")
        assertEquals(TasksIcons.TIMER, filter.getIcon(free))
    }

    @Test fun snoozedFilterWithProReturnsSnoozedIcon() {
        val filter = SnoozedFilter(title = "S")
        assertEquals(TasksIcons.SNOOZE, filter.getIcon(pro))
    }

    @Test fun debugFilterWithProAndIconReturnsIcon() {
        val filter = DebugFilter(title = "D", sql = null, icon = "debug_icon")
        assertEquals("debug_icon", filter.getIcon(pro))
    }

    @Test fun debugFilterWithFreeReturnsIcon() {
        val filter = DebugFilter(title = "D", sql = null, icon = "debug_icon")
        // Not a specific type, so else branch returns icon
        assertEquals("debug_icon", filter.getIcon(free))
    }

    @Test fun debugFilterWithFreeAndNullIconReturnsNull() {
        val filter = DebugFilter(title = "D", sql = null, icon = null)
        assertNull(filter.getIcon(free))
    }
}
