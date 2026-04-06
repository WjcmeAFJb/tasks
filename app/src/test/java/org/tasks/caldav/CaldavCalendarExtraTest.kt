package org.tasks.caldav

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.tasks.data.entity.CaldavCalendar
import org.tasks.data.entity.CaldavCalendar.Companion.ACCESS_OWNER
import org.tasks.data.entity.CaldavCalendar.Companion.ACCESS_READ_ONLY
import org.tasks.data.entity.CaldavCalendar.Companion.ACCESS_READ_WRITE

class CaldavCalendarExtraTest {

    @Test
    fun readOnlyWhenAccessReadOnly() {
        assertTrue(CaldavCalendar(access = ACCESS_READ_ONLY).readOnly())
    }

    @Test
    fun notReadOnlyWhenAccessOwner() {
        assertFalse(CaldavCalendar(access = ACCESS_OWNER).readOnly())
    }

    @Test
    fun notReadOnlyWhenAccessReadWrite() {
        assertFalse(CaldavCalendar(access = ACCESS_READ_WRITE).readOnly())
    }

    @Test
    fun calendarUriExtractsLastSegment() {
        val calendar = CaldavCalendar(url = "https://example.com/calendars/user/cal-uuid/")
        assertEquals("cal-uuid", calendar.calendarUri)
    }

    @Test
    fun calendarUriWithoutTrailingSlash() {
        val calendar = CaldavCalendar(url = "https://example.com/calendars/user/cal-uuid")
        assertEquals("cal-uuid", calendar.calendarUri)
    }

    @Test
    fun calendarUriNullWhenUrlNull() {
        val calendar = CaldavCalendar(url = null)
        assertNull(calendar.calendarUri)
    }

    @Test
    fun calendarUriNullWhenUrlEmpty() {
        val calendar = CaldavCalendar(url = "")
        assertNull(calendar.calendarUri)
    }

    @Test
    fun calendarUriHandlesRootPath() {
        val calendar = CaldavCalendar(url = "https://example.com/")
        // After trimEnd('/'), url is "https://example.com" -> last segment is "example.com"
        assertEquals("example.com", calendar.calendarUri)
    }

    @Test
    fun calendarUriDeepPath() {
        val calendar = CaldavCalendar(url = "https://example.com/a/b/c/d/")
        assertEquals("d", calendar.calendarUri)
    }

    @Test
    fun defaultAccessIsOwner() {
        assertEquals(ACCESS_OWNER, CaldavCalendar().access)
    }

    @Test
    fun defaultColorIsZero() {
        assertEquals(0, CaldavCalendar().color)
    }

    @Test
    fun defaultCtagIsNull() {
        assertNull(CaldavCalendar().ctag)
    }

    @Test
    fun defaultIconIsNull() {
        assertNull(CaldavCalendar().icon)
    }

    @Test
    fun defaultLastSyncIsZero() {
        assertEquals(0L, CaldavCalendar().lastSync)
    }

    // ===== Data class =====

    @Test
    fun calendarCopy() {
        val original = CaldavCalendar(
            name = "My Calendar",
            url = "https://example.com/cal/",
            color = 0xFF0000,
            access = ACCESS_OWNER,
        )
        val copy = original.copy(name = "Renamed")
        assertEquals("Renamed", copy.name)
        assertEquals(original.url, copy.url)
        assertEquals(original.color, copy.color)
        assertEquals(original.access, copy.access)
    }
}
