package org.tasks.location

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.tasks.location.PlaceSearchGoogle.Companion.toJson
import org.tasks.location.PlaceSearchGoogle.Companion.toPlace
import org.tasks.location.PlaceSearchGoogle.Companion.toSearchResults

class PlaceSearchGoogleDeepTest {

    // ===== toJson =====

    @Test
    fun toJsonParsesValidJson() {
        val json = """{"key": "value"}""".toJson()
        assertEquals("value", json["key"]?.toString()?.trim('"'))
    }

    @Test
    fun toJsonParsesComplexObject() {
        val json = """{"nested": {"inner": "data"}}""".toJson()
        assertEquals("data", json["nested"]?.jsonObject?.get("inner")?.toString()?.trim('"'))
    }

    // ===== toSearchResults =====

    @Test
    fun toSearchResultsEmptyPredictions() {
        val json = """{"predictions": [], "status": "OK"}""".toJson()
        val results = toSearchResults(json)
        assertTrue(results.isEmpty())
    }

    @Test
    fun toSearchResultsFiltersMissingPlaceId() {
        val json = """{
            "predictions": [
                {
                    "description": "No place id entry",
                    "structured_formatting": {
                        "main_text": "Test",
                        "secondary_text": "No ID"
                    }
                }
            ],
            "status": "OK"
        }""".toJson()
        val results = toSearchResults(json)
        assertTrue(results.isEmpty())
    }

    @Test
    fun toSearchResultsWithValidPrediction() {
        val json = """{
            "predictions": [
                {
                    "place_id": "ChIJ123",
                    "structured_formatting": {
                        "main_text": "Test Place",
                        "secondary_text": "123 Test St"
                    }
                }
            ],
            "status": "OK"
        }""".toJson()
        val results = toSearchResults(json)
        assertEquals(1, results.size)
        assertEquals("ChIJ123", results[0].id)
        assertEquals("Test Place", results[0].name)
        assertEquals("123 Test St", results[0].address)
    }

    @Test
    fun toSearchResultsWithMultiplePredictions() {
        val json = """{
            "predictions": [
                {
                    "place_id": "A",
                    "structured_formatting": {
                        "main_text": "Place A",
                        "secondary_text": "Addr A"
                    }
                },
                {
                    "place_id": "B",
                    "structured_formatting": {
                        "main_text": "Place B",
                        "secondary_text": "Addr B"
                    }
                }
            ],
            "status": "OK"
        }""".toJson()
        val results = toSearchResults(json)
        assertEquals(2, results.size)
        assertEquals("A", results[0].id)
        assertEquals("B", results[1].id)
    }

    @Test
    fun toSearchResultsMixedWithAndWithoutPlaceId() {
        val json = """{
            "predictions": [
                {
                    "place_id": "valid",
                    "structured_formatting": {
                        "main_text": "Valid",
                        "secondary_text": "Has ID"
                    }
                },
                {
                    "description": "Invalid - no place_id",
                    "structured_formatting": {
                        "main_text": "Invalid",
                        "secondary_text": "No ID"
                    }
                },
                {
                    "place_id": "another_valid",
                    "structured_formatting": {
                        "main_text": "Another Valid",
                        "secondary_text": "Has ID Too"
                    }
                }
            ],
            "status": "OK"
        }""".toJson()
        val results = toSearchResults(json)
        assertEquals(2, results.size)
        assertEquals("valid", results[0].id)
        assertEquals("another_valid", results[1].id)
    }

    @Test
    fun toSearchResultsNoPlaceInResult() {
        val json = """{
            "predictions": [
                {
                    "place_id": "test",
                    "structured_formatting": {
                        "main_text": "Test",
                        "secondary_text": "Address"
                    }
                }
            ],
            "status": "OK"
        }""".toJson()
        val results = toSearchResults(json)
        assertNull(results[0].place)
    }

    // ===== toPlace =====

    @Test
    fun toPlaceWithAllFields() {
        val json = """{
            "result": {
                "name": "Test Place",
                "formatted_address": "123 Test St, City",
                "international_phone_number": "+1 555-1234",
                "website": "https://example.com",
                "geometry": {
                    "location": {
                        "lat": 40.7484,
                        "lng": -73.9857
                    }
                }
            },
            "status": "OK"
        }""".toJson()
        val place = toPlace(json)
        assertEquals("Test Place", place.name)
        assertEquals("123 Test St, City", place.address)
        assertEquals("+1 555-1234", place.phone)
        assertEquals("https://example.com", place.url)
        assertEquals(40.7484, place.latitude, 0.0001)
        assertEquals(-73.9857, place.longitude, 0.0001)
    }

    @Test
    fun toPlaceWithMinimalFields() {
        val json = """{
            "result": {
                "name": "Minimal Place",
                "geometry": {
                    "location": {
                        "lat": 0.0,
                        "lng": 0.0
                    }
                }
            },
            "status": "OK"
        }""".toJson()
        val place = toPlace(json)
        assertEquals("Minimal Place", place.name)
        assertNull(place.address)
        assertNull(place.phone)
        assertNull(place.url)
        assertEquals(0.0, place.latitude, 0.0)
        assertEquals(0.0, place.longitude, 0.0)
    }

    @Test
    fun toPlaceWithNoPhone() {
        val json = """{
            "result": {
                "name": "No Phone",
                "formatted_address": "Address",
                "website": "https://example.com",
                "geometry": {
                    "location": {
                        "lat": 10.0,
                        "lng": 20.0
                    }
                }
            },
            "status": "OK"
        }""".toJson()
        val place = toPlace(json)
        assertNull(place.phone)
        assertEquals("https://example.com", place.url)
    }

    @Test
    fun toPlaceWithNoWebsite() {
        val json = """{
            "result": {
                "name": "No Website",
                "formatted_address": "Address",
                "international_phone_number": "+1 555-0000",
                "geometry": {
                    "location": {
                        "lat": 10.0,
                        "lng": 20.0
                    }
                }
            },
            "status": "OK"
        }""".toJson()
        val place = toPlace(json)
        assertEquals("+1 555-0000", place.phone)
        assertNull(place.url)
    }

    @Test
    fun toPlaceWithNegativeCoordinates() {
        val json = """{
            "result": {
                "name": "Southern Hemisphere",
                "geometry": {
                    "location": {
                        "lat": -33.8688,
                        "lng": 151.2093
                    }
                }
            },
            "status": "OK"
        }""".toJson()
        val place = toPlace(json)
        assertEquals(-33.8688, place.latitude, 0.0001)
        assertEquals(151.2093, place.longitude, 0.0001)
    }
}
