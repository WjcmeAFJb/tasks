@file:Suppress("ClassName")

package org.tasks.caldav

import net.fortuna.ical4j.model.DateTime
import net.fortuna.ical4j.model.property.Completed
import net.fortuna.ical4j.model.property.DtStart
import net.fortuna.ical4j.model.property.Due
import net.fortuna.ical4j.model.property.RelatedTo
import net.fortuna.ical4j.model.property.XProperty
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.tasks.caldav.iCalendar.Companion.IS_APPLE_SORT_ORDER
import org.tasks.caldav.iCalendar.Companion.collapsed
import org.tasks.caldav.iCalendar.Companion.fromVtodo
import org.tasks.caldav.iCalendar.Companion.getDateTime
import org.tasks.caldav.iCalendar.Companion.order
import org.tasks.caldav.iCalendar.Companion.parent
import org.tasks.caldav.iCalendar.Companion.toMillis
import org.tasks.data.entity.Task
import org.tasks.date.DateTimeUtils.newDateTime
import java.io.StringReader

class iCalendarCompanionTest {

    // ===== prodId / supportsReminders =====

    @Test
    fun tasksOrgSupportsReminders() {
        assertTrue("tasks.org".supportsReminders())
    }

    @Test
    fun mozillaSupportsReminders() {
        assertTrue("Mozilla.org/Calendar".supportsReminders())
    }

    @Test
    fun appleSupportsReminders() {
        assertTrue("Apple Inc.".supportsReminders())
    }

    @Test
    fun unknownClientDoesNotSupportReminders() {
        assertFalse("random-client".supportsReminders())
    }

    @Test
    fun emptyStringDoesNotSupportReminders() {
        assertFalse("".supportsReminders())
    }

    private fun String.supportsReminders(): Boolean {
        // Call the actual companion method
        return with(iCalendar.Companion) {
            this@supportsReminders.supportsReminders()
        }
    }

    // ===== prodId extraction =====

    @Test
    fun extractProdIdFromVtodo() {
        val vtodo = "BEGIN:VCALENDAR\nPRODID:-//tasks.org//Test//EN\nEND:VCALENDAR\n"
        val prodId = with(iCalendar.Companion) { vtodo.prodId() }
        assertEquals("-//tasks.org//Test//EN", prodId)
    }

    @Test
    fun extractProdIdReturnsNullWhenMissing() {
        val vtodo = "BEGIN:VCALENDAR\nVERSION:2.0\nEND:VCALENDAR\n"
        val prodId = with(iCalendar.Companion) { vtodo.prodId() }
        assertNull(prodId)
    }

    // ===== fromVtodo =====

    @Test
    fun fromVtodoReturnsTaskForValidInput() {
        val vtodo = """BEGIN:VCALENDAR
VERSION:2.0
PRODID:-//Test//Test//EN
BEGIN:VTODO
UID:test-uid
SUMMARY:Test
END:VTODO
END:VCALENDAR"""
        val result = fromVtodo(vtodo)
        assertNotNull(result)
        assertEquals("test-uid", result!!.uid)
    }

    @Test
    fun fromVtodoReturnsNullForInvalidInput() {
        val result = fromVtodo("not valid ical")
        assertNull(result)
    }

    @Test
    fun fromVtodoReturnsNullForEmptyInput() {
        val result = fromVtodo("")
        assertNull(result)
    }

    @Test
    fun fromVtodoReturnsNullForMultipleVtodos() {
        val vtodo = """BEGIN:VCALENDAR
VERSION:2.0
PRODID:-//Test//Test//EN
BEGIN:VTODO
UID:uid1
END:VTODO
BEGIN:VTODO
UID:uid2
END:VTODO
END:VCALENDAR"""
        val result = fromVtodo(vtodo)
        // When there are multiple VTODOs, size != 1, so returns null
        assertNull(result)
    }

    // ===== parent extension =====

    @Test
    fun getParentReturnsRelatedToValue() {
        val task = Task(uid = "child")
        task.relatedTo.add(RelatedTo("parent-uid"))
        assertEquals("parent-uid", task.parent)
    }

    @Test
    fun getParentReturnsNullWhenNoRelatedTo() {
        val task = Task(uid = "child")
        assertNull(task.parent)
    }

    @Test
    fun setParentAddsRelatedTo() {
        val task = Task(uid = "child")
        task.parent = "new-parent"
        assertEquals("new-parent", task.parent)
        assertEquals(1, task.relatedTo.size)
    }

    @Test
    fun setParentNullRemovesRelatedTo() {
        val task = Task(uid = "child")
        task.relatedTo.add(RelatedTo("parent"))
        task.parent = null
        assertNull(task.parent)
        assertTrue(task.relatedTo.isEmpty())
    }

    @Test
    fun setParentBlankRemovesRelatedTo() {
        val task = Task(uid = "child")
        task.relatedTo.add(RelatedTo("parent"))
        task.parent = ""
        assertNull(task.parent)
        assertTrue(task.relatedTo.isEmpty())
    }

    @Test
    fun setParentUpdatesExistingRelatedTo() {
        val task = Task(uid = "child")
        task.relatedTo.add(RelatedTo("old-parent"))
        task.parent = "new-parent"
        assertEquals("new-parent", task.parent)
        assertEquals(1, task.relatedTo.size)
    }

    // ===== order extension =====

    @Test
    fun getOrderReturnsNullWhenNoProperty() {
        val task = Task(uid = "uid")
        assertNull(task.order)
    }

    @Test
    fun setOrderAddsProperty() {
        val task = Task(uid = "uid")
        task.order = 42L
        assertEquals(42L, task.order)
    }

    @Test
    fun setOrderNullRemovesProperty() {
        val task = Task(uid = "uid")
        task.order = 42L
        task.order = null
        assertNull(task.order)
    }

    @Test
    fun setOrderUpdatesExistingProperty() {
        val task = Task(uid = "uid")
        task.order = 10L
        task.order = 20L
        assertEquals(20L, task.order)
    }

    @Test
    fun isAppleSortOrderMatchesCaseInsensitive() {
        val prop = XProperty("x-apple-sort-order", "123")
        assertTrue(IS_APPLE_SORT_ORDER(prop))
    }

    @Test
    fun isAppleSortOrderMatchesExact() {
        val prop = XProperty("X-APPLE-SORT-ORDER", "123")
        assertTrue(IS_APPLE_SORT_ORDER(prop))
    }

    @Test
    fun isAppleSortOrderDoesNotMatchOther() {
        val prop = XProperty("X-OTHER-PROP", "123")
        assertFalse(IS_APPLE_SORT_ORDER(prop))
    }

    // ===== collapsed extension =====

    @Test
    fun collapsedFalseByDefault() {
        val task = Task(uid = "uid")
        assertFalse(task.collapsed)
    }

    @Test
    fun collapsedTrueWhenSet() {
        val task = Task(uid = "uid")
        task.collapsed = true
        assertTrue(task.collapsed)
    }

    @Test
    fun collapsedCanBeReset() {
        val task = Task(uid = "uid")
        task.collapsed = true
        task.collapsed = false
        assertFalse(task.collapsed)
    }

    // ===== Due.toMillis / DtStart.toMillis =====

    @Test
    fun nullDueReturnsZero() {
        val due: Due? = null
        assertEquals(0L, due.toMillis())
    }

    @Test
    fun nullDtStartReturnsZero() {
        val dtStart: DtStart? = null
        val task = org.tasks.data.entity.Task()
        assertEquals(0L, with(iCalendar.Companion) { dtStart.toMillis(task) })
    }

    // ===== getDateTime =====

    @Test
    fun getDateTimeReturnsDateTime() {
        val now = newDateTime().millis
        val result = getDateTime(now)
        assertNotNull(result)
        assertTrue(result is DateTime)
    }
}
