package org.tasks.caldav

import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class TasksAccountResponseTest {

    private val json = Json { ignoreUnknownKeys = true }

    @Test
    fun parseMinimalResponse() {
        val raw = """{}"""
        val response = json.decodeFromString<TasksAccountResponse>(raw)
        assertNull(response.createdAt)
        assertNull(response.subscription)
        assertNull(response.inboundEmail)
        assertEquals(emptyList<TasksAccountResponse.AppPassword>(), response.appPasswords)
        assertFalse(response.guest)
        assertEquals(emptyList<String>(), response.sharedWithMe)
        assertEquals(emptyList<TasksAccountResponse.Guest>(), response.guests)
        assertEquals(5, response.maxGuests)
    }

    @Test
    fun parseFullResponse() {
        val raw = """{
            "createdAt": 1700000000,
            "subscription": {"free": false, "provider": "google", "expiration": 1700100000},
            "inbound_email": {"email": "test@tasks.org", "calendar": "cal-uuid"},
            "app_passwords": [{"description": "My Phone", "session_id": 42, "created_at": 1700000001, "last_access": 1700000002}],
            "guest": true,
            "shared_with_me": ["list1", "list2"],
            "guests": [{"display_name": "Alice", "email": "alice@example.com"}],
            "max_guests": 10
        }"""
        val response = json.decodeFromString<TasksAccountResponse>(raw)
        assertEquals(1700000000L, response.createdAt)
        assertFalse(response.subscription!!.free)
        assertEquals("google", response.subscription!!.provider)
        assertEquals(1700100000L, response.subscription!!.expiration)
        assertEquals("test@tasks.org", response.inboundEmail!!.email)
        assertEquals("cal-uuid", response.inboundEmail!!.calendar)
        assertEquals(1, response.appPasswords.size)
        assertEquals("My Phone", response.appPasswords[0].description)
        assertEquals(42, response.appPasswords[0].sessionId)
        assertEquals(1700000001L, response.appPasswords[0].createdAt)
        assertEquals(1700000002L, response.appPasswords[0].lastAccess)
        assertTrue(response.guest)
        assertEquals(listOf("list1", "list2"), response.sharedWithMe)
        assertEquals(1, response.guests.size)
        assertEquals("Alice", response.guests[0].displayName)
        assertEquals("alice@example.com", response.guests[0].email)
        assertEquals(10, response.maxGuests)
    }

    @Test
    fun parseWithUnknownKeys() {
        val raw = """{"unknown_field": "value", "guest": true}"""
        val response = json.decodeFromString<TasksAccountResponse>(raw)
        assertTrue(response.guest)
    }

    @Test
    fun subscriptionDefaultValues() {
        val sub = TasksAccountResponse.Subscription()
        assertTrue(sub.free)
        assertNull(sub.provider)
        assertNull(sub.expiration)
    }

    @Test
    fun inboundEmailDefaultValues() {
        val email = TasksAccountResponse.InboundEmail()
        assertNull(email.email)
        assertNull(email.calendar)
    }

    @Test
    fun appPasswordDefaultValues() {
        val pw = TasksAccountResponse.AppPassword()
        assertNull(pw.description)
        assertEquals(-1, pw.sessionId)
        assertNull(pw.createdAt)
        assertNull(pw.lastAccess)
    }

    @Test
    fun guestDefaultValues() {
        val guest = TasksAccountResponse.Guest()
        assertNull(guest.displayName)
        assertNull(guest.email)
    }

    @Test
    fun defaultGuestIsFalse() {
        val response = TasksAccountResponse()
        assertFalse(response.guest)
    }

    @Test
    fun defaultMaxGuestsIsFive() {
        val response = TasksAccountResponse()
        assertEquals(5, response.maxGuests)
    }

    @Test
    fun parseSubscriptionFree() {
        val raw = """{"subscription": {"free": true}}"""
        val response = json.decodeFromString<TasksAccountResponse>(raw)
        assertTrue(response.subscription!!.free)
    }

    @Test
    fun parseEmptyAppPasswords() {
        val raw = """{"app_passwords": []}"""
        val response = json.decodeFromString<TasksAccountResponse>(raw)
        assertTrue(response.appPasswords.isEmpty())
    }

    @Test
    fun parseMultipleGuests() {
        val raw = """{
            "guests": [
                {"display_name": "Alice"},
                {"email": "bob@example.com"},
                {"display_name": "Charlie", "email": "charlie@example.com"}
            ]
        }"""
        val response = json.decodeFromString<TasksAccountResponse>(raw)
        assertEquals(3, response.guests.size)
        assertEquals("Alice", response.guests[0].displayName)
        assertNull(response.guests[0].email)
        assertNull(response.guests[1].displayName)
        assertEquals("bob@example.com", response.guests[1].email)
        assertEquals("Charlie", response.guests[2].displayName)
        assertEquals("charlie@example.com", response.guests[2].email)
    }
}
