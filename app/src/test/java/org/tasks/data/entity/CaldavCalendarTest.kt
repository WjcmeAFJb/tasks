package org.tasks.data.entity

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class CaldavCalendarTest {
    @Test fun defaultAccessIsOwner() = assertEquals(CaldavCalendar.ACCESS_OWNER, CaldavCalendar().access)
    @Test fun defaultColorIsZero() = assertEquals(0, CaldavCalendar().color)
    @Test fun defaultNameIsEmpty() = assertEquals("", CaldavCalendar().name)
    @Test fun defaultCtagIsNull() = assertNull(CaldavCalendar().ctag)
    @Test fun defaultIconIsNull() = assertNull(CaldavCalendar().icon)
    @Test fun defaultLastSyncIsZero() = assertEquals(0L, CaldavCalendar().lastSync)

    @Test fun readOnlyWhenAccessReadOnly() = assertTrue(CaldavCalendar(access = CaldavCalendar.ACCESS_READ_ONLY).readOnly())
    @Test fun notReadOnlyWhenOwner() = assertFalse(CaldavCalendar(access = CaldavCalendar.ACCESS_OWNER).readOnly())
    @Test fun notReadOnlyWhenReadWrite() = assertFalse(CaldavCalendar(access = CaldavCalendar.ACCESS_READ_WRITE).readOnly())
    @Test fun notReadOnlyWhenUnknown() = assertFalse(CaldavCalendar(access = CaldavCalendar.ACCESS_UNKNOWN).readOnly())

    @Test fun calendarUriExtractsLastSegment() {
        val cal = CaldavCalendar(url = "https://example.com/dav/calendars/my-cal/")
        assertEquals("my-cal", cal.calendarUri)
    }

    @Test fun calendarUriHandlesNoTrailingSlash() {
        val cal = CaldavCalendar(url = "https://example.com/dav/calendars/my-cal")
        assertEquals("my-cal", cal.calendarUri)
    }

    @Test fun calendarUriNullWhenUrlNull() = assertNull(CaldavCalendar(url = null).calendarUri)
    @Test fun calendarUriNullWhenUrlEmpty() = assertNull(CaldavCalendar(url = "").calendarUri)

    @Test fun calendarUriHandlesMultipleTrailingSlashes() {
        val cal = CaldavCalendar(url = "https://example.com/cal///")
        assertEquals("cal", cal.calendarUri)
    }

    @Test fun accessConstants() {
        assertEquals(-1, CaldavCalendar.ACCESS_UNKNOWN)
        assertEquals(0, CaldavCalendar.ACCESS_OWNER)
        assertEquals(1, CaldavCalendar.ACCESS_READ_WRITE)
        assertEquals(2, CaldavCalendar.ACCESS_READ_ONLY)
    }

    @Test fun inviteConstants() {
        assertEquals(-1, CaldavCalendar.INVITE_UNKNOWN)
        assertEquals(0, CaldavCalendar.INVITE_ACCEPTED)
        assertEquals(1, CaldavCalendar.INVITE_NO_RESPONSE)
        assertEquals(2, CaldavCalendar.INVITE_DECLINED)
        assertEquals(3, CaldavCalendar.INVITE_INVALID)
    }

    @Test fun copyChangesAccess() {
        val original = CaldavCalendar(access = CaldavCalendar.ACCESS_OWNER)
        val copy = original.copy(access = CaldavCalendar.ACCESS_READ_ONLY)
        assertTrue(copy.readOnly())
    }

    @Test fun equality() {
        val a = CaldavCalendar(uuid = "abc", account = "acc", name = "Test")
        val b = CaldavCalendar(uuid = "abc", account = "acc", name = "Test")
        assertEquals(a, b)
    }
}
