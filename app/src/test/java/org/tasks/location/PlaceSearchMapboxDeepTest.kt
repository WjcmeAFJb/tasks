package org.tasks.location

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.tasks.location.PlaceSearchMapbox.Companion.jsonToSearchResults

class PlaceSearchMapboxDeepTest {

    // ===== jsonToSearchResults =====

    @Test
    fun emptyFeaturesReturnsEmptyList() {
        val json = """{"features": []}"""
        assertTrue(jsonToSearchResults(json).isEmpty())
    }

    @Test
    fun singleFeatureResult() {
        val json = """{
            "features": [{
                "id": "poi.123",
                "text": "Test Place",
                "place_name": "Test Place, City, State",
                "place_type": ["poi"],
                "center": [-73.9857, 40.7484]
            }]
        }"""
        val results = jsonToSearchResults(json)
        assertEquals(1, results.size)
        assertEquals("poi.123", results[0].id)
        assertEquals("Test Place", results[0].name)
        assertNotNull(results[0].place)
    }

    @Test
    fun multipleFeatureResults() {
        val json = """{
            "features": [
                {
                    "id": "poi.1",
                    "text": "Place One",
                    "place_name": "Place One, City",
                    "place_type": ["poi"],
                    "center": [1.0, 2.0]
                },
                {
                    "id": "poi.2",
                    "text": "Place Two",
                    "place_name": "Place Two, City",
                    "place_type": ["poi"],
                    "center": [3.0, 4.0]
                },
                {
                    "id": "poi.3",
                    "text": "Place Three",
                    "place_name": "Place Three, City",
                    "place_type": ["poi"],
                    "center": [5.0, 6.0]
                }
            ]
        }"""
        val results = jsonToSearchResults(json)
        assertEquals(3, results.size)
        assertEquals("poi.1", results[0].id)
        assertEquals("poi.2", results[1].id)
        assertEquals("poi.3", results[2].id)
    }

    @Test
    fun searchResultHasPlaceObject() {
        val json = """{
            "features": [{
                "id": "poi.999",
                "text": "Central Park",
                "place_name": "Central Park, NYC",
                "place_type": ["poi"],
                "center": [-73.9654, 40.7829]
            }]
        }"""
        val results = jsonToSearchResults(json)
        val place = results[0].place!!
        assertEquals("Central Park", place.name)
        assertEquals("Central Park, NYC", place.address)
        assertEquals(-73.9654, place.longitude, 0.0001)
        assertEquals(40.7829, place.latitude, 0.0001)
    }

    @Test
    fun searchResultWithAddressType() {
        val json = """{
            "features": [{
                "id": "addr.1",
                "text": "Main Street",
                "address": "42",
                "place_name": "42 Main Street, Springfield",
                "place_type": ["address"],
                "center": [-89.6501, 39.7817]
            }]
        }"""
        val results = jsonToSearchResults(json)
        assertEquals("42 Main Street", results[0].place!!.name)
    }

    @Test
    fun searchResultAddressIsDisplayAddress() {
        val json = """{
            "features": [{
                "id": "poi.42",
                "text": "Coffee Shop",
                "place_name": "Coffee Shop, 123 Elm St, Town",
                "place_type": ["poi"],
                "center": [10.0, 20.0]
            }]
        }"""
        val results = jsonToSearchResults(json)
        // address comes from place.displayAddress
        assertNotNull(results[0].address)
    }
}
