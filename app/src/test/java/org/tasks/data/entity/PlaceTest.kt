package org.tasks.data.entity

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

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
}
