package org.tasks.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Test
import org.tasks.data.entity.Geofence
import org.tasks.data.entity.Place
import org.tasks.data.entity.TagData
import org.tasks.filters.PlaceFilter
import org.tasks.filters.TagFilter

class DataExtensionsTest {

    // --- TagFiltersExtensions ---

    @Test
    fun toTagFilterMapsTagData() {
        val tagData = TagData(name = "My Tag", remoteId = "remote-123")
        val tagFilters = TagFilters(tagData = tagData, count = 5)
        val result = tagFilters.toTagFilter()
        assertEquals("My Tag", result.tagData.name)
        assertEquals("remote-123", result.tagData.remoteId)
        assertEquals(5, result.count)
    }

    @Test
    fun toTagFilterWithZeroCount() {
        val tagData = TagData(name = "Empty Tag")
        val tagFilters = TagFilters(tagData = tagData, count = 0)
        val result = tagFilters.toTagFilter()
        assertEquals(0, result.count)
    }

    @Test
    fun toTagFilterPreservesAllTagDataFields() {
        val tagData = TagData(
            name = "Test",
            remoteId = "uuid-456",
            tagOrdering = "some-ordering"
        )
        val tagFilters = TagFilters(tagData = tagData, count = 10)
        val result = tagFilters.toTagFilter()
        assertNotNull(result)
        assertEquals("uuid-456", result.tagData.remoteId)
    }

    // --- LocationFiltersExtensions ---

    @Test
    fun toLocationFilterMapsPlace() {
        val place = Place(uid = "place-uid", name = "Office", latitude = 40.7, longitude = -74.0)
        val locationFilters = LocationFilters(place = place, count = 3)
        val result = locationFilters.toLocationFilter()
        assertEquals("Office", result.place.name)
        assertEquals("place-uid", result.place.uid)
        assertEquals(3, result.count)
    }

    @Test
    fun toLocationFilterWithZeroCount() {
        val place = Place(uid = "uid", name = "Nowhere")
        val locationFilters = LocationFilters(place = place, count = 0)
        val result = locationFilters.toLocationFilter()
        assertEquals(0, result.count)
    }

    @Test
    fun toLocationFilterPreservesPlaceCoordinates() {
        val place = Place(
            uid = "uid-789",
            name = "Home",
            latitude = 51.5,
            longitude = -0.12
        )
        val locationFilters = LocationFilters(place = place, count = 7)
        val result = locationFilters.toLocationFilter()
        assertEquals(51.5, result.place.latitude, 0.001)
        assertEquals(-0.12, result.place.longitude, 0.001)
    }

    @Test
    fun toLocationFilterWithLargeCount() {
        val place = Place(uid = "uid", name = "Busy Place")
        val locationFilters = LocationFilters(place = place, count = 9999)
        val result = locationFilters.toLocationFilter()
        assertEquals(9999, result.count)
    }
}
