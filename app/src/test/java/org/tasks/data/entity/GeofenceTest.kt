package org.tasks.data.entity

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class GeofenceTest {
    @Test fun defaultIdIsZero() = assertEquals(0L, Geofence().id)
    @Test fun defaultTaskIsZero() = assertEquals(0L, Geofence().task)
    @Test fun defaultPlaceIsNull() = assertNull(Geofence().place)
    @Test fun defaultArrivalIsFalse() = assertFalse(Geofence().isArrival)
    @Test fun defaultDepartureIsFalse() = assertFalse(Geofence().isDeparture)

    @Test fun setArrival() = assertTrue(Geofence(isArrival = true).isArrival)
    @Test fun setDeparture() = assertTrue(Geofence(isDeparture = true).isDeparture)
    @Test fun setPlace() = assertEquals("place-uid", Geofence(place = "place-uid").place)
    @Test fun setTask() = assertEquals(42L, Geofence(task = 42).task)

    @Test fun tableNameConstant() = assertEquals("geofences", Geofence.TABLE_NAME)

    @Test fun copyChangesArrival() {
        val original = Geofence(isArrival = false)
        assertTrue(original.copy(isArrival = true).isArrival)
    }

    @Test fun equality() {
        val a = Geofence(place = "p1", isArrival = true, isDeparture = false)
        val b = Geofence(place = "p1", isArrival = true, isDeparture = false)
        assertEquals(a, b)
    }
}
