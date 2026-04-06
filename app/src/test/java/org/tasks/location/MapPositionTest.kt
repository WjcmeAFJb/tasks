package org.tasks.location

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Test

class MapPositionTest {

    // --- Construction ---

    @Test
    fun constructWithLatitudeAndLongitude() {
        val pos = MapPosition(latitude = 37.7749, longitude = -122.4194)
        assertEquals(37.7749, pos.latitude, 0.0001)
        assertEquals(-122.4194, pos.longitude, 0.0001)
    }

    @Test
    fun defaultZoomIs15() {
        val pos = MapPosition(latitude = 0.0, longitude = 0.0)
        assertEquals(15.0f, pos.zoom, 0.001f)
    }

    @Test
    fun customZoom() {
        val pos = MapPosition(latitude = 0.0, longitude = 0.0, zoom = 10.5f)
        assertEquals(10.5f, pos.zoom, 0.001f)
    }

    @Test
    fun zeroCoordinates() {
        val pos = MapPosition(latitude = 0.0, longitude = 0.0)
        assertEquals(0.0, pos.latitude, 0.0)
        assertEquals(0.0, pos.longitude, 0.0)
    }

    @Test
    fun negativeCoordinates() {
        val pos = MapPosition(latitude = -33.8688, longitude = -151.2093)
        assertEquals(-33.8688, pos.latitude, 0.0001)
        assertEquals(-151.2093, pos.longitude, 0.0001)
    }

    @Test
    fun maxLatitude() {
        val pos = MapPosition(latitude = 90.0, longitude = 0.0)
        assertEquals(90.0, pos.latitude, 0.0)
    }

    @Test
    fun minLatitude() {
        val pos = MapPosition(latitude = -90.0, longitude = 0.0)
        assertEquals(-90.0, pos.latitude, 0.0)
    }

    @Test
    fun maxLongitude() {
        val pos = MapPosition(latitude = 0.0, longitude = 180.0)
        assertEquals(180.0, pos.longitude, 0.0)
    }

    @Test
    fun minLongitude() {
        val pos = MapPosition(latitude = 0.0, longitude = -180.0)
        assertEquals(-180.0, pos.longitude, 0.0)
    }

    // --- Equality ---

    @Test
    fun equalPositionsAreEqual() {
        val pos1 = MapPosition(latitude = 37.7749, longitude = -122.4194, zoom = 12.0f)
        val pos2 = MapPosition(latitude = 37.7749, longitude = -122.4194, zoom = 12.0f)
        assertEquals(pos1, pos2)
    }

    @Test
    fun differentLatitudeNotEqual() {
        val pos1 = MapPosition(latitude = 37.7749, longitude = -122.4194)
        val pos2 = MapPosition(latitude = 37.7750, longitude = -122.4194)
        assertNotEquals(pos1, pos2)
    }

    @Test
    fun differentLongitudeNotEqual() {
        val pos1 = MapPosition(latitude = 37.7749, longitude = -122.4194)
        val pos2 = MapPosition(latitude = 37.7749, longitude = -122.4195)
        assertNotEquals(pos1, pos2)
    }

    @Test
    fun differentZoomNotEqual() {
        val pos1 = MapPosition(latitude = 37.7749, longitude = -122.4194, zoom = 10.0f)
        val pos2 = MapPosition(latitude = 37.7749, longitude = -122.4194, zoom = 15.0f)
        assertNotEquals(pos1, pos2)
    }

    @Test
    fun sameDefaultZoomEquals() {
        val pos1 = MapPosition(latitude = 1.0, longitude = 2.0)
        val pos2 = MapPosition(latitude = 1.0, longitude = 2.0, zoom = 15.0f)
        assertEquals(pos1, pos2)
    }

    // --- hashCode ---

    @Test
    fun equalObjectsHaveSameHashCode() {
        val pos1 = MapPosition(latitude = 37.7749, longitude = -122.4194, zoom = 12.0f)
        val pos2 = MapPosition(latitude = 37.7749, longitude = -122.4194, zoom = 12.0f)
        assertEquals(pos1.hashCode(), pos2.hashCode())
    }

    // --- copy ---

    @Test
    fun copyWithDifferentLatitude() {
        val pos = MapPosition(latitude = 37.7749, longitude = -122.4194, zoom = 12.0f)
        val copy = pos.copy(latitude = 40.7128)
        assertEquals(40.7128, copy.latitude, 0.0001)
        assertEquals(-122.4194, copy.longitude, 0.0001)
        assertEquals(12.0f, copy.zoom, 0.001f)
    }

    @Test
    fun copyWithDifferentLongitude() {
        val pos = MapPosition(latitude = 37.7749, longitude = -122.4194, zoom = 12.0f)
        val copy = pos.copy(longitude = -74.0060)
        assertEquals(37.7749, copy.latitude, 0.0001)
        assertEquals(-74.0060, copy.longitude, 0.0001)
    }

    @Test
    fun copyWithDifferentZoom() {
        val pos = MapPosition(latitude = 37.7749, longitude = -122.4194, zoom = 12.0f)
        val copy = pos.copy(zoom = 18.0f)
        assertEquals(18.0f, copy.zoom, 0.001f)
    }

    @Test
    fun copyPreservesOriginal() {
        val pos = MapPosition(latitude = 37.7749, longitude = -122.4194, zoom = 12.0f)
        pos.copy(latitude = 0.0)
        assertEquals(37.7749, pos.latitude, 0.0001)
    }

    // --- toString ---

    @Test
    fun toStringContainsFieldValues() {
        val pos = MapPosition(latitude = 1.5, longitude = 2.5, zoom = 3.0f)
        val str = pos.toString()
        assert(str.contains("1.5")) { "Expected toString to contain latitude" }
        assert(str.contains("2.5")) { "Expected toString to contain longitude" }
        assert(str.contains("3.0")) { "Expected toString to contain zoom" }
    }

    // --- Destructuring ---

    @Test
    fun destructuringComponents() {
        val pos = MapPosition(latitude = 37.7749, longitude = -122.4194, zoom = 10.0f)
        val (lat, lng, zm) = pos
        assertEquals(37.7749, lat, 0.0001)
        assertEquals(-122.4194, lng, 0.0001)
        assertEquals(10.0f, zm, 0.001f)
    }
}
