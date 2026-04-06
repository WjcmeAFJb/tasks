package org.tasks.location

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test
import org.tasks.location.GeocoderMapbox.Companion.asCoordinates
import org.tasks.location.GeocoderMapbox.Companion.jsonToPlace
import org.tasks.location.GeocoderMapbox.Companion.toPlace

class GeocoderMapboxDeepTest {

    // ===== jsonToPlace =====

    @Test
    fun jsonToPlaceWithEmptyFeatures() {
        val json = """{"features":[]}"""
        assertNull(jsonToPlace(json))
    }

    @Test
    fun jsonToPlaceWithNoFeatures() {
        val json = """{}"""
        assertNull(jsonToPlace(json))
    }

    @Test
    fun jsonToPlaceWithNonAddressType() {
        val json = """{
            "features": [{
                "text": "Some Place",
                "place_name": "Some Place, Somewhere",
                "place_type": ["poi"],
                "center": [-73.9857, 40.7484]
            }]
        }"""
        val place = jsonToPlace(json)!!
        assertEquals("Some Place", place.name)
        assertEquals("Some Place, Somewhere", place.address)
        assertEquals(-73.9857, place.longitude, 0.0001)
        assertEquals(40.7484, place.latitude, 0.0001)
    }

    @Test
    fun jsonToPlaceWithAddressTypeAndAddressNumber() {
        val json = """{
            "features": [{
                "text": "Main Street",
                "address": "123",
                "place_name": "123 Main Street, City",
                "place_type": ["address"],
                "center": [-73.9857, 40.7484]
            }]
        }"""
        val place = jsonToPlace(json)!!
        assertEquals("123 Main Street", place.name)
    }

    @Test
    fun jsonToPlaceWithAddressTypeButNoAddressNumber() {
        val json = """{
            "features": [{
                "text": "Main Street",
                "place_name": "Main Street, City",
                "place_type": ["address"],
                "center": [-73.9857, 40.7484]
            }]
        }"""
        val place = jsonToPlace(json)!!
        assertEquals("Main Street", place.name)
    }

    @Test
    fun jsonToPlaceWithMultipleFeaturesUsesFirst() {
        val json = """{
            "features": [
                {
                    "text": "First",
                    "place_name": "First Place",
                    "place_type": ["poi"],
                    "center": [1.0, 2.0]
                },
                {
                    "text": "Second",
                    "place_name": "Second Place",
                    "place_type": ["poi"],
                    "center": [3.0, 4.0]
                }
            ]
        }"""
        val place = jsonToPlace(json)!!
        assertEquals("First", place.name)
    }

    @Test
    fun jsonToPlaceWithMixedPlaceTypes() {
        val json = """{
            "features": [{
                "text": "Broadway",
                "place_name": "Broadway, New York",
                "place_type": ["address", "poi"],
                "address": "42",
                "center": [-73.9857, 40.7484]
            }]
        }"""
        val place = jsonToPlace(json)!!
        assertEquals("42 Broadway", place.name)
    }

    @Test
    fun jsonToPlaceWithRegionType() {
        val json = """{
            "features": [{
                "text": "California",
                "place_name": "California, United States",
                "place_type": ["region"],
                "center": [-119.4179, 36.7783]
            }]
        }"""
        val place = jsonToPlace(json)!!
        assertEquals("California", place.name)
    }

    // ===== asCoordinates =====

    @Test
    fun asCoordinatesParsesPair() {
        val element = Json.parseToJsonElement("[1.5, 2.5]")
        val (lng, lat) = element.asCoordinates
        assertEquals(1.5, lng, 0.0)
        assertEquals(2.5, lat, 0.0)
    }

    @Test
    fun asCoordinatesWithNegativeValues() {
        val element = Json.parseToJsonElement("[-122.4194, 37.7749]")
        val (lng, lat) = element.asCoordinates
        assertEquals(-122.4194, lng, 0.0001)
        assertEquals(37.7749, lat, 0.0001)
    }

    @Test
    fun asCoordinatesWithZeros() {
        val element = Json.parseToJsonElement("[0.0, 0.0]")
        val (lng, lat) = element.asCoordinates
        assertEquals(0.0, lng, 0.0)
        assertEquals(0.0, lat, 0.0)
    }

    @Test
    fun asCoordinatesWithIntegers() {
        val element = Json.parseToJsonElement("[10, 20]")
        val (lng, lat) = element.asCoordinates
        assertEquals(10.0, lng, 0.0)
        assertEquals(20.0, lat, 0.0)
    }

    // ===== toPlace =====

    @Test
    fun toPlaceExtractsAllFields() {
        val json = """{
            "text": "Empire State Building",
            "place_name": "Empire State Building, New York, NY",
            "place_type": ["poi"],
            "center": [-73.9857, 40.7484]
        }"""
        val element = Json.parseToJsonElement(json)
        val place = element.toPlace()
        assertEquals("Empire State Building", place.name)
        assertEquals("Empire State Building, New York, NY", place.address)
        assertEquals(-73.9857, place.longitude, 0.0001)
        assertEquals(40.7484, place.latitude, 0.0001)
    }

    @Test
    fun toPlaceWithAddressTypeFormatsNameWithNumber() {
        val json = """{
            "text": "Oak Avenue",
            "address": "456",
            "place_name": "456 Oak Avenue, Springfield",
            "place_type": ["address"],
            "center": [-89.6501, 39.7817]
        }"""
        val element = Json.parseToJsonElement(json)
        val place = element.toPlace()
        assertEquals("456 Oak Avenue", place.name)
    }

    // ===== asStringList (tested indirectly through place_type) =====

    @Test
    fun placeTypeArrayWithSingleElement() {
        val json = """{
            "text": "Test",
            "place_name": "Test, Somewhere",
            "place_type": ["poi"],
            "center": [0.0, 0.0]
        }"""
        val place = jsonToPlace("""{"features":[$json]}""")!!
        assertEquals("Test", place.name) // Not address type, so name = text
    }

    @Test
    fun placeTypeArrayWithMultipleElements() {
        val json = """{
            "text": "Test",
            "address": "1",
            "place_name": "1 Test, Somewhere",
            "place_type": ["address", "poi"],
            "center": [0.0, 0.0]
        }"""
        val place = jsonToPlace("""{"features":[$json]}""")!!
        assertEquals("1 Test", place.name) // Contains "address" type
    }
}
