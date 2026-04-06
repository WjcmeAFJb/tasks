package org.tasks

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class EventTest {

    @Test
    fun getValueReturnsValue() {
        val event = Event("hello")
        assertEquals("hello", event.value)
    }

    @Test
    fun getIfUnhandledReturnsValueFirstTime() {
        val event = Event("data")
        assertEquals("data", event.getIfUnhandled())
    }

    @Test
    fun getIfUnhandledReturnsNullSecondTime() {
        val event = Event("data")
        event.getIfUnhandled()
        assertNull(event.getIfUnhandled())
    }

    @Test
    fun getIfUnhandledReturnsNullThirdTime() {
        val event = Event("data")
        event.getIfUnhandled()
        event.getIfUnhandled()
        assertNull(event.getIfUnhandled())
    }

    @Test
    fun getValueAlwaysReturnsValueRegardlessOfHandled() {
        val event = Event("persistent")
        event.getIfUnhandled()
        assertEquals("persistent", event.value)
    }

    @Test
    fun eventWithNullValue() {
        val event = Event<String?>(null)
        assertNull(event.value)
        assertNull(event.getIfUnhandled())
    }

    @Test
    fun eventWithIntegerValue() {
        val event = Event(42)
        assertEquals(42, event.value)
        assertEquals(42, event.getIfUnhandled())
        assertNull(event.getIfUnhandled())
    }

    @Test
    fun eventWithBooleanValue() {
        val event = Event(true)
        assertEquals(true, event.value)
        assertEquals(true, event.getIfUnhandled())
        assertNull(event.getIfUnhandled())
    }

    @Test
    fun eventWithEmptyString() {
        val event = Event("")
        assertEquals("", event.value)
        assertEquals("", event.getIfUnhandled())
        assertNull(event.getIfUnhandled())
    }
}
