package org.tasks.data.entity

import org.junit.Assert.assertEquals
import org.junit.Test

class PrincipalTest {

    // --- name computed property ---

    @Test
    fun nameReturnsDisplayNameWhenSet() {
        val principal = Principal(account = 1, href = "/principals/user1", displayName = "Alice")
        assertEquals("Alice", principal.name)
    }

    @Test
    fun nameStripsMailtoFromHrefWhenNoDisplayName() {
        val principal = Principal(account = 1, href = "mailto:user@example.com")
        assertEquals("user@example.com", principal.name)
    }

    @Test
    fun nameExtractsLastSegmentFromHrefWhenNoDisplayName() {
        val principal = Principal(account = 1, href = "/principals/users/alice")
        assertEquals("alice", principal.name)
    }

    @Test
    fun nameExtractsLastSegmentWithTrailingSlash() {
        val principal = Principal(account = 1, href = "/principals/users/bob/")
        // The regex captures the last segment before trailing content
        // ".*/([^/]+).*" with "/principals/users/bob/" -> checks for last non-slash segment
        val name = principal.name
        // With trailing slash, the regex ".*/([^/]+).*" should still match "bob"
        assertEquals("bob", name)
    }

    @Test
    fun nameReturnsFullHrefWhenNoSegments() {
        val principal = Principal(account = 1, href = "user1")
        // No slash, so the regex doesn't match, falls back to href
        assertEquals("user1", principal.name)
    }

    @Test
    fun nameReturnsHrefWhenDisplayNameIsNull() {
        val principal = Principal(account = 1, href = "simple-href", displayName = null)
        assertEquals("simple-href", principal.name)
    }

    @Test
    fun namePrefersDisplayNameOverHref() {
        val principal = Principal(
            account = 1,
            href = "/principals/users/alice",
            displayName = "Alice Smith"
        )
        assertEquals("Alice Smith", principal.name)
    }

    // --- Default values ---

    @Test
    fun defaultIdIsZero() {
        assertEquals(0L, Principal(account = 1, href = "h").id)
    }

    @Test
    fun defaultEmailIsNull() {
        assertEquals(null, Principal(account = 1, href = "h").email)
    }

    @Test
    fun defaultDisplayNameIsNull() {
        assertEquals(null, Principal(account = 1, href = "h").displayName)
    }

    // --- Setters ---

    @Test
    fun setEmail() {
        val principal = Principal(account = 1, href = "h")
        principal.email = "test@example.com"
        assertEquals("test@example.com", principal.email)
    }

    @Test
    fun setDisplayName() {
        val principal = Principal(account = 1, href = "h")
        principal.displayName = "Bob"
        assertEquals("Bob", principal.displayName)
    }

    // --- Copy / Equality ---

    @Test
    fun copyChangesDisplayName() {
        val original = Principal(account = 1, href = "h", displayName = "Old")
        val copy = original.copy(displayName = "New")
        assertEquals("New", copy.displayName)
    }

    @Test
    fun equalityOnSameValues() {
        val a = Principal(id = 1, account = 2, href = "h", email = "e", displayName = "d")
        val b = Principal(id = 1, account = 2, href = "h", email = "e", displayName = "d")
        assertEquals(a, b)
    }

    @Test
    fun hashCodeConsistentWithEquals() {
        val a = Principal(id = 1, account = 2, href = "h")
        val b = Principal(id = 1, account = 2, href = "h")
        assertEquals(a.hashCode(), b.hashCode())
    }

    @Test
    fun nameWithMailtoAndDisplayName() {
        val principal = Principal(
            account = 1,
            href = "mailto:user@example.com",
            displayName = "User Name"
        )
        assertEquals("User Name", principal.name)
    }
}
