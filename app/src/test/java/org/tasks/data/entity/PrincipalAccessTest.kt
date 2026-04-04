package org.tasks.data.entity

import org.junit.Assert.assertEquals
import org.junit.Test

class PrincipalAccessTest {

    // --- Default values ---

    @Test
    fun defaultIdIsZero() {
        assertEquals(0L, PrincipalAccess(list = 1).id)
    }

    @Test
    fun defaultPrincipalIsZero() {
        assertEquals(0L, PrincipalAccess(list = 1).principal)
    }

    @Test
    fun defaultInviteIsUnknown() {
        assertEquals(CaldavCalendar.INVITE_UNKNOWN, PrincipalAccess(list = 1).invite)
    }

    @Test
    fun defaultAccessIsUnknown() {
        assertEquals(CaldavCalendar.ACCESS_UNKNOWN, PrincipalAccess(list = 1).access)
    }

    // --- Explicit values ---

    @Test
    fun setListExplicitly() {
        val pa = PrincipalAccess(list = 42)
        assertEquals(42L, pa.list)
    }

    @Test
    fun setPrincipalExplicitly() {
        val pa = PrincipalAccess(principal = 7, list = 1)
        assertEquals(7L, pa.principal)
    }

    @Test
    fun setInviteExplicitly() {
        val pa = PrincipalAccess(list = 1, invite = CaldavCalendar.INVITE_ACCEPTED)
        assertEquals(CaldavCalendar.INVITE_ACCEPTED, pa.invite)
    }

    @Test
    fun setAccessExplicitly() {
        val pa = PrincipalAccess(list = 1, access = CaldavCalendar.ACCESS_READ_WRITE)
        assertEquals(CaldavCalendar.ACCESS_READ_WRITE, pa.access)
    }

    // --- Mutable setters ---

    @Test
    fun setInviteViaSetter() {
        val pa = PrincipalAccess(list = 1)
        pa.invite = CaldavCalendar.INVITE_DECLINED
        assertEquals(CaldavCalendar.INVITE_DECLINED, pa.invite)
    }

    @Test
    fun setAccessViaSetter() {
        val pa = PrincipalAccess(list = 1)
        pa.access = CaldavCalendar.ACCESS_READ_ONLY
        assertEquals(CaldavCalendar.ACCESS_READ_ONLY, pa.access)
    }

    @Test
    fun setIdViaSetter() {
        val pa = PrincipalAccess(list = 1)
        pa.id = 99
        assertEquals(99L, pa.id)
    }

    // --- Copy / Equality ---

    @Test
    fun copyChangesAccess() {
        val original = PrincipalAccess(list = 1, access = CaldavCalendar.ACCESS_OWNER)
        val copy = original.copy(access = CaldavCalendar.ACCESS_READ_ONLY)
        assertEquals(CaldavCalendar.ACCESS_READ_ONLY, copy.access)
    }

    @Test
    fun equalityOnSameValues() {
        val a = PrincipalAccess(id = 1, principal = 2, list = 3, invite = 0, access = 1)
        val b = PrincipalAccess(id = 1, principal = 2, list = 3, invite = 0, access = 1)
        assertEquals(a, b)
    }

    @Test
    fun hashCodeConsistentWithEquals() {
        val a = PrincipalAccess(id = 1, principal = 2, list = 3)
        val b = PrincipalAccess(id = 1, principal = 2, list = 3)
        assertEquals(a.hashCode(), b.hashCode())
    }
}
