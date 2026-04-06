package org.tasks.location

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test
import org.tasks.data.entity.Place

class PlaceSearchResultTest {

    @Test
    fun constructionWithAllFields() {
        val place = Place(name = "Test", latitude = 1.0, longitude = 2.0)
        val result = PlaceSearchResult("id-1", "Name", "Address", place)
        assertEquals("id-1", result.id)
        assertEquals("Name", result.name)
        assertEquals("Address", result.address)
        assertEquals(place, result.place)
    }

    @Test
    fun constructionWithDefaultPlace() {
        val result = PlaceSearchResult("id-2", "Name", "Address")
        assertEquals("id-2", result.id)
        assertNull(result.place)
    }

    @Test
    fun constructionWithNullNameAndAddress() {
        val result = PlaceSearchResult("id-3", null, null)
        assertNull(result.name)
        assertNull(result.address)
    }

    @Test
    fun dataClassEquality() {
        val r1 = PlaceSearchResult("id", "N", "A")
        val r2 = PlaceSearchResult("id", "N", "A")
        assertEquals(r1, r2)
    }

    @Test
    fun dataClassInequality() {
        val r1 = PlaceSearchResult("id1", "N", "A")
        val r2 = PlaceSearchResult("id2", "N", "A")
        assertNotEquals(r1, r2)
    }

    @Test
    fun dataClassHashCodeConsistent() {
        val r1 = PlaceSearchResult("id", "N", "A")
        val r2 = PlaceSearchResult("id", "N", "A")
        assertEquals(r1.hashCode(), r2.hashCode())
    }

    @Test
    fun copyChangesId() {
        val original = PlaceSearchResult("id1", "N", "A")
        val copy = original.copy(id = "id2")
        assertEquals("id2", copy.id)
        assertEquals("N", copy.name)
    }

    @Test
    fun toStringContainsFields() {
        val result = PlaceSearchResult("myId", "myName", "myAddr")
        val str = result.toString()
        assert(str.contains("myId"))
        assert(str.contains("myName"))
    }
}
