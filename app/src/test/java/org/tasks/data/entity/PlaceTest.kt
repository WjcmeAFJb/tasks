package org.tasks.data.entity

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.tasks.data.NO_ORDER

class PlaceTest {
    @Test fun defaultIdIsZero() = assertEquals(0L, Place().id)
    @Test fun defaultRadiusIs250() = assertEquals(250, Place().radius)
    @Test fun defaultColorIsZero() = assertEquals(0, Place().color)
    @Test fun defaultLatitudeIsZero() = assertEquals(0.0, Place().latitude, 0.001)
    @Test fun defaultLongitudeIsZero() = assertEquals(0.0, Place().longitude, 0.001)
    @Test fun defaultUidGenerated() = assertNotNull(Place().uid)
    @Test fun defaultNameIsNull() = assertNull(Place().name)
    @Test fun defaultAddressIsNull() = assertNull(Place().address)

    @Test fun displayNameUsesName() =
        assertEquals("Home", Place(name = "Home").displayName)

    @Test fun displayNameFallsBackToAddress() =
        assertEquals("123 Main St", Place(address = "123 Main St").displayName)

    @Test fun displayNamePrefersNameOverAddress() =
        assertEquals("Office", Place(name = "Office", address = "456 Broadway").displayName)

    @Test fun displayAddressRemovesNamePrefix() {
        val place = Place(name = "Home", address = "Home, 123 Main St, NYC")
        assertEquals("123 Main St, NYC", place.displayAddress)
    }

    @Test fun displayAddressNullWhenEmpty() = assertNull(Place(address = "").displayAddress)
    @Test fun displayAddressNullWhenNull() = assertNull(Place(address = null).displayAddress)

    @Test fun displayAddressReturnsAsIsWhenNoNamePrefix() {
        val place = Place(name = "Home", address = "123 Main St")
        assertEquals("123 Main St", place.displayAddress)
    }

    @Test fun distanceBetweenSamePointIsZero() {
        val d = Place.distanceBetween(40.0, -74.0, 40.0, -74.0)
        assertEquals(0.0, d, 0.01)
    }

    @Test fun distanceBetweenNewYorkAndLondon() {
        // NYC: 40.7128, -74.0060
        // London: 51.5074, -0.1278
        val d = Place.distanceBetween(40.7128, -74.006, 51.5074, -0.1278)
        // ~5,570 km
        assertTrue(d > 5_500_000 && d < 5_650_000)
    }

    @Test fun distanceToUsesHaversine() {
        val nyc = Place(latitude = 40.7128, longitude = -74.006)
        val la = Place(latitude = 34.0522, longitude = -118.2437)
        val d = nyc.distanceTo(la)
        // ~3,940 km
        assertTrue(d > 3_900_000 && d < 4_000_000)
    }

    @Test fun distanceBetweenIsSymmetric() {
        val d1 = Place.distanceBetween(40.0, -74.0, 51.0, -0.1)
        val d2 = Place.distanceBetween(51.0, -0.1, 40.0, -74.0)
        assertEquals(d1, d2, 0.01)
    }

    @Test fun keyConstant() = assertEquals("place", Place.KEY)
    @Test fun tableNameConstant() = assertEquals("places", Place.TABLE_NAME)

    @Test fun copyChangesName() {
        val original = Place(name = "Old")
        val copy = original.copy(name = "New")
        assertEquals("New", copy.name)
    }

    // coordinateNameNotUsedAsDisplayName requires Android Location.convert()

    // --- Additional default values ---

    @Test fun defaultPhoneIsNull() = assertNull(Place().phone)
    @Test fun defaultUrlIsNull() = assertNull(Place().url)
    @Test fun defaultIconIsNull() = assertNull(Place().icon)
    @Test fun defaultOrderIsNoOrder() = assertEquals(NO_ORDER, Place().order)

    // --- displayName edge cases ---

    // displayNameUsesCoordinatesWhenNameAndAddressNull - requires android.location.Location

    @Test fun displayNameUsesAddressWhenNameNull() {
        val place = Place(name = null, address = "123 Main St")
        assertEquals("123 Main St", place.displayName)
    }

    @Test fun displayNameUsesCoordinatesWhenNameIsCoordinatePattern() {
        // A name matching the coordinate regex pattern like "40°42'46.1"N 74°0'21.6"W"
        val coordName = "40°42'46.1\"N 74°0'21.6\"W"
        val place = Place(name = coordName, address = "NYC")
        assertEquals("NYC", place.displayName)
    }

    @Test fun displayNameUsesNameWhenNotCoordinatePattern() {
        val place = Place(name = "Home sweet home", address = "123 Main St")
        assertEquals("Home sweet home", place.displayName)
    }

    // --- displayAddress when name not in address ---

    @Test fun displayAddressReturnsFullAddressWhenNameNotPrefix() {
        val place = Place(name = "Office", address = "456 Broadway, NYC")
        assertEquals("456 Broadway, NYC", place.displayAddress)
    }

    // --- distanceBetween edge cases ---

    @Test fun distanceBetweenAntipodalPoints() {
        // North pole to South pole: ~20,000 km
        val d = Place.distanceBetween(90.0, 0.0, -90.0, 0.0)
        assertTrue(d > 19_900_000 && d < 20_100_000)
    }

    @Test fun distanceBetweenSamePointIsZeroForNonZeroCoords() {
        val d = Place.distanceBetween(51.5074, -0.1278, 51.5074, -0.1278)
        assertEquals(0.0, d, 0.01)
    }

    // --- distanceTo ---

    @Test fun distanceToSamePlace() {
        val place = Place(latitude = 40.0, longitude = -74.0)
        assertEquals(0.0, place.distanceTo(place), 0.01)
    }

    @Test fun distanceToIsSymmetric() {
        val a = Place(latitude = 40.7128, longitude = -74.006)
        val b = Place(latitude = 34.0522, longitude = -118.2437)
        assertEquals(a.distanceTo(b), b.distanceTo(a), 0.01)
    }

    // --- Data class equality ---

    @Test fun dataClassEquality() {
        val a = Place(id = 1, uid = "abc", name = "Home", latitude = 40.0, longitude = -74.0)
        val b = Place(id = 1, uid = "abc", name = "Home", latitude = 40.0, longitude = -74.0)
        assertEquals(a, b)
    }

    @Test fun dataClassHashCodeConsistency() {
        val a = Place(id = 1, uid = "abc", name = "Home")
        val b = Place(id = 1, uid = "abc", name = "Home")
        assertEquals(a.hashCode(), b.hashCode())
    }

    @Test fun dataClassInequalityOnDifferentName() {
        val a = Place(id = 1, uid = "abc", name = "Home")
        val b = Place(id = 1, uid = "abc", name = "Work")
        assertNotEquals(a, b)
    }

    @Test fun dataClassInequalityOnDifferentLatitude() {
        val a = Place(id = 1, uid = "abc", latitude = 40.0)
        val b = Place(id = 1, uid = "abc", latitude = 41.0)
        assertNotEquals(a, b)
    }

    // --- Explicit construction ---

    @Test fun explicitConstruction() {
        val place = Place(
            id = 5,
            uid = "test-uid",
            name = "Test Place",
            address = "123 Test St",
            phone = "555-1234",
            url = "https://example.com",
            latitude = 51.5074,
            longitude = -0.1278,
            color = 0xFF0000,
            icon = "ic_place",
            order = 3,
            radius = 500,
        )
        assertEquals(5L, place.id)
        assertEquals("test-uid", place.uid)
        assertEquals("Test Place", place.name)
        assertEquals("123 Test St", place.address)
        assertEquals("555-1234", place.phone)
        assertEquals("https://example.com", place.url)
        assertEquals(51.5074, place.latitude, 0.0001)
        assertEquals(-0.1278, place.longitude, 0.0001)
        assertEquals(0xFF0000, place.color)
        assertEquals("ic_place", place.icon)
        assertEquals(3, place.order)
        assertEquals(500, place.radius)
    }

    // --- copy preserves values ---

    @Test fun copyPreservesAllFields() {
        val original = Place(
            id = 1, uid = "uid", name = "Home", address = "123 St",
            phone = "555", url = "http://x", latitude = 40.0, longitude = -74.0,
            color = 100, icon = "ic", order = 2, radius = 300
        )
        val copy = original.copy(name = "Office")
        assertEquals("Office", copy.name)
        assertEquals(1L, copy.id)
        assertEquals("uid", copy.uid)
        assertEquals("123 St", copy.address)
        assertEquals("555", copy.phone)
        assertEquals("http://x", copy.url)
        assertEquals(40.0, copy.latitude, 0.001)
        assertEquals(-74.0, copy.longitude, 0.001)
        assertEquals(100, copy.color)
        assertEquals("ic", copy.icon)
        assertEquals(2, copy.order)
        assertEquals(300, copy.radius)
    }
}
