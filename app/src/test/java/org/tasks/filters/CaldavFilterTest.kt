package org.tasks.filters

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.tasks.data.entity.CaldavAccount
import org.tasks.data.entity.CaldavCalendar

class CaldavFilterTest {
    private fun filter(
        accountType: Int = CaldavAccount.TYPE_CALDAV,
        access: Int = CaldavCalendar.ACCESS_OWNER,
        name: String = "Test",
        uuid: String = "uuid-1",
        color: Int = 0,
        icon: String? = null,
        order: Int = 0,
    ) = CaldavFilter(
        calendar = CaldavCalendar(uuid = uuid, name = name, color = color, icon = icon, order = order, access = access),
        account = CaldavAccount(accountType = accountType),
    )

    @Test fun titleFromCalendarName() = assertEquals("My List", filter(name = "My List").title)
    @Test fun titleEmptyWhenEmpty() = assertEquals("", filter(name = "").title)
    @Test fun tintFromCalendarColor() = assertEquals(42, filter(color = 42).tint)
    @Test fun iconFromCalendar() = assertEquals("star", filter(icon = "star").icon)
    @Test fun orderFromCalendar() = assertEquals(5, filter(order = 5).order)
    @Test fun uuidFromCalendar() = assertEquals("abc", filter(uuid = "abc").uuid)

    @Test fun readOnlyWhenAccessReadOnly() =
        assertTrue(filter(access = CaldavCalendar.ACCESS_READ_ONLY).isReadOnly)

    @Test fun notReadOnlyWhenOwner() =
        assertFalse(filter(access = CaldavCalendar.ACCESS_OWNER).isReadOnly)

    @Test fun isGoogleTasks() =
        assertTrue(filter(accountType = CaldavAccount.TYPE_GOOGLE_TASKS).isGoogleTasks)

    @Test fun isNotGoogleTasks() =
        assertFalse(filter(accountType = CaldavAccount.TYPE_CALDAV).isGoogleTasks)

    @Test fun isIcalendar() =
        assertTrue(filter(accountType = CaldavAccount.TYPE_CALDAV).isIcalendar)

    @Test fun microsoftIsNotIcalendar() =
        assertFalse(filter(accountType = CaldavAccount.TYPE_MICROSOFT).isIcalendar)

    @Test fun googleTasksIsNotIcalendar() =
        assertFalse(filter(accountType = CaldavAccount.TYPE_GOOGLE_TASKS).isIcalendar)

    @Test fun supportsManualSort() = assertTrue(filter().supportsManualSort())

    @Test fun sqlContainsCalendarUuid() {
        val sql = filter(uuid = "test-uuid").sql
        assertTrue(sql.contains("test-uuid"))
    }

    @Test fun valuesForNewTasks() {
        val values = filter(uuid = "cal-uuid").valuesForNewTasks
        assertTrue(values.contains("caldav"))
        assertTrue(values.contains("cal-uuid"))
    }

    @Test fun areItemsTheSameSameId() {
        val a = CaldavFilter(CaldavCalendar(id = 1), CaldavAccount())
        val b = CaldavFilter(CaldavCalendar(id = 1), CaldavAccount())
        assertTrue(a.areItemsTheSame(b))
    }

    @Test fun areItemsTheSameDifferentId() {
        val a = CaldavFilter(CaldavCalendar(id = 1), CaldavAccount())
        val b = CaldavFilter(CaldavCalendar(id = 2), CaldavAccount())
        assertFalse(a.areItemsTheSame(b))
    }
}
