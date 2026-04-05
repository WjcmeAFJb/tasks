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
}
