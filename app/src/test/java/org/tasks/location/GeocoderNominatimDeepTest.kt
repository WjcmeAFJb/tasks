package org.tasks.location

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test

class GeocoderNominatimDeepTest {

    // ===== jsonToPlace =====

    @Test
    fun jsonToPlaceWithEmptyFeatures() {
        val json = """{"features":[]}"""
        assertNull(GeocoderNominatim.jsonToPlace(json))
    }

    @Test
    fun jsonToPlaceWithNoFeatures() {
        val json = """{}"""
        assertNull(GeocoderNominatim.jsonToPlace(json))
    }

    @Test
    fun jsonToPlaceWithNamedPlace() {
        val json = """{
            "features": [{
                "properties": {
                    "geocoding": {
                        "name": "Central Park",
                        "label": "Central Park, New York, NY"
                    }
                },
                "geometry": {
                    "coordinates": [-73.9654, 40.7829]
                }
            }]
        }"""
        val place = GeocoderNominatim.jsonToPlace(json)!!
        assertEquals("Central Park", place.name)
        assertEquals("Central Park, New York, NY", place.address)
        assertEquals(-73.9654, place.longitude, 0.0001)
        assertEquals(40.7829, place.latitude, 0.0001)
    }

    @Test
    fun jsonToPlaceWithHouseNumber() {
        val json = """{
            "features": [{
                "properties": {
                    "geocoding": {
                        "housenumber": "42",
                        "street": "Main Street",
                        "label": "42 Main Street, Springfield"
                    }
                },
                "geometry": {
                    "coordinates": [-89.6501, 39.7817]
                }
            }]
        }"""
        val place = GeocoderNominatim.jsonToPlace(json)!!
        assertEquals("42 Main Street", place.name)
    }

    @Test
    fun jsonToPlaceWithNoNameOrHouseNumber() {
        val json = """{
            "features": [{
                "properties": {
                    "geocoding": {
                        "label": "Some Road, Somewhere"
                    }
                },
                "geometry": {
                    "coordinates": [10.0, 20.0]
                }
            }]
        }"""
        val place = GeocoderNominatim.jsonToPlace(json)!!
        assertNull(place.name)
        assertEquals("Some Road, Somewhere", place.address)
    }

    @Test
    fun jsonToPlaceWithNoLabel() {
        val json = """{
            "features": [{
                "properties": {
                    "geocoding": {
                        "name": "Test"
                    }
                },
                "geometry": {
                    "coordinates": [5.0, 10.0]
                }
            }]
        }"""
        val place = GeocoderNominatim.jsonToPlace(json)!!
        assertEquals("Test", place.name)
        assertNull(place.address)
    }

    @Test
    fun jsonToPlaceWithMultipleFeaturesUsesFirst() {
        val json = """{
            "features": [
                {
                    "properties": {
                        "geocoding": {
                            "name": "First",
                            "label": "First Place"
                        }
                    },
                    "geometry": {
                        "coordinates": [1.0, 2.0]
                    }
                },
                {
                    "properties": {
                        "geocoding": {
                            "name": "Second",
                            "label": "Second Place"
                        }
                    },
                    "geometry": {
                        "coordinates": [3.0, 4.0]
                    }
                }
            ]
        }"""
        val place = GeocoderNominatim.jsonToPlace(json)!!
        assertEquals("First", place.name)
    }

    @Test
    fun jsonToPlaceWithHouseNumberButNoStreet() {
        val json = """{
            "features": [{
                "properties": {
                    "geocoding": {
                        "housenumber": "7",
                        "label": "7, Somewhere"
                    }
                },
                "geometry": {
                    "coordinates": [0.0, 0.0]
                }
            }]
        }"""
        val place = GeocoderNominatim.jsonToPlace(json)!!
        assertEquals("7 null", place.name) // no street -> null appended
    }

    @Test
    fun jsonToPlaceNameTakesPriorityOverHouseNumber() {
        val json = """{
            "features": [{
                "properties": {
                    "geocoding": {
                        "name": "Named Place",
                        "housenumber": "99",
                        "street": "Elm Street",
                        "label": "Named Place, Elm Street"
                    }
                },
                "geometry": {
                    "coordinates": [0.0, 0.0]
                }
            }]
        }"""
        val place = GeocoderNominatim.jsonToPlace(json)!!
        assertEquals("Named Place", place.name)
    }

    @Test
    fun jsonToPlaceCoordinatesCorrectOrder() {
        val json = """{
            "features": [{
                "properties": {
                    "geocoding": {
                        "name": "Test"
                    }
                },
                "geometry": {
                    "coordinates": [11.1658572, 60.2301296]
                }
            }]
        }"""
        val place = GeocoderNominatim.jsonToPlace(json)!!
        assertEquals(11.1658572, place.longitude, 0.0000001)
        assertEquals(60.2301296, place.latitude, 0.0000001)
    }

    @Test
    fun jsonToPlaceWithNegativeCoordinates() {
        val json = """{
            "features": [{
                "properties": {
                    "geocoding": {
                        "name": "Test"
                    }
                },
                "geometry": {
                    "coordinates": [-87.6338, 41.8299]
                }
            }]
        }"""
        val place = GeocoderNominatim.jsonToPlace(json)!!
        assertEquals(-87.6338, place.longitude, 0.0001)
        assertEquals(41.8299, place.latitude, 0.0001)
    }
}
