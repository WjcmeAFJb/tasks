package org.tasks.backup

import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class TasksJsonImporterExtraTest {

    private val json = Json {
        isLenient = true
        ignoreUnknownKeys = true
    }

    // ===== ImportResult boundary conditions =====

    @Test
    fun importResultMaxValues() {
        val result = TasksJsonImporter.ImportResult()
        result.taskCount = Int.MAX_VALUE
        result.importCount = Int.MAX_VALUE
        result.skipCount = Int.MAX_VALUE
        assertEquals(Int.MAX_VALUE, result.taskCount)
        assertEquals(Int.MAX_VALUE, result.importCount)
        assertEquals(Int.MAX_VALUE, result.skipCount)
    }

    @Test
    fun importResultZeroAfterReset() {
        val result = TasksJsonImporter.ImportResult()
        result.taskCount = 100
        result.importCount = 80
        result.skipCount = 20
        result.taskCount = 0
        result.importCount = 0
        result.skipCount = 0
        assertEquals(0, result.taskCount)
        assertEquals(0, result.importCount)
        assertEquals(0, result.skipCount)
    }

    @Test
    fun importResultCountsAreIndependentlyMutable() {
        val result = TasksJsonImporter.ImportResult()
        result.taskCount = 10
        assertEquals(0, result.importCount)
        assertEquals(0, result.skipCount)
        result.importCount = 5
        assertEquals(10, result.taskCount)
        assertEquals(0, result.skipCount)
        result.skipCount = 3
        assertEquals(10, result.taskCount)
        assertEquals(5, result.importCount)
    }

    @Test
    fun importResultMultipleIncrementDecrement() {
        val result = TasksJsonImporter.ImportResult()
        repeat(100) { result.taskCount++ }
        repeat(50) { result.importCount++ }
        repeat(50) { result.skipCount++ }
        assertEquals(100, result.taskCount)
        assertEquals(50, result.importCount)
        assertEquals(50, result.skipCount)
    }

    // ===== LegacyLocation JSON deserialization =====

    @Test
    fun deserializeLegacyLocationAllFields() {
        val input = """
            {
                "name": "Office",
                "address": "123 Main St",
                "phone": "+1-555-0100",
                "url": "https://example.com",
                "latitude": 40.7128,
                "longitude": -74.006,
                "radius": 250,
                "arrival": true,
                "departure": false
            }
        """.trimIndent()
        @Suppress("DEPRECATION")
        val location = json.decodeFromString<TasksJsonImporter.LegacyLocation>(input)
        assertEquals("Office", location.name)
        assertEquals("123 Main St", location.address)
        assertEquals("+1-555-0100", location.phone)
        assertEquals("https://example.com", location.url)
        assertEquals(40.7128, location.latitude, 0.0001)
        assertEquals(-74.006, location.longitude, 0.0001)
        assertEquals(250, location.radius)
        assertTrue(location.arrival)
        assertFalse(location.departure)
    }

    @Test
    fun deserializeLegacyLocationMinimalFields() {
        val input = """{}"""
        @Suppress("DEPRECATION")
        val location = json.decodeFromString<TasksJsonImporter.LegacyLocation>(input)
        assertNull(location.name)
        assertNull(location.address)
        assertNull(location.phone)
        assertNull(location.url)
        assertEquals(0.0, location.latitude, 0.0)
        assertEquals(0.0, location.longitude, 0.0)
        assertEquals(0, location.radius)
        assertFalse(location.arrival)
        assertFalse(location.departure)
    }

    @Test
    fun deserializeLegacyLocationNegativeCoordinates() {
        val input = """
            {
                "latitude": -33.8688,
                "longitude": 151.2093
            }
        """.trimIndent()
        @Suppress("DEPRECATION")
        val location = json.decodeFromString<TasksJsonImporter.LegacyLocation>(input)
        assertEquals(-33.8688, location.latitude, 0.0001)
        assertEquals(151.2093, location.longitude, 0.0001)
    }

    @Test
    fun deserializeLegacyLocationBothArrivalAndDeparture() {
        val input = """
            {
                "arrival": true,
                "departure": true
            }
        """.trimIndent()
        @Suppress("DEPRECATION")
        val location = json.decodeFromString<TasksJsonImporter.LegacyLocation>(input)
        assertTrue(location.arrival)
        assertTrue(location.departure)
    }

    @Test
    fun deserializeLegacyLocationIgnoresUnknownFields() {
        val input = """
            {
                "name": "Test",
                "unknownField": "ignored",
                "anotherUnknown": 42
            }
        """.trimIndent()
        @Suppress("DEPRECATION")
        val location = json.decodeFromString<TasksJsonImporter.LegacyLocation>(input)
        assertEquals("Test", location.name)
    }

    @Test
    fun deserializeLegacyLocationZeroRadius() {
        val input = """{ "radius": 0 }"""
        @Suppress("DEPRECATION")
        val location = json.decodeFromString<TasksJsonImporter.LegacyLocation>(input)
        assertEquals(0, location.radius)
    }

    @Test
    fun deserializeLegacyLocationLargeRadius() {
        val input = """{ "radius": 50000 }"""
        @Suppress("DEPRECATION")
        val location = json.decodeFromString<TasksJsonImporter.LegacyLocation>(input)
        assertEquals(50000, location.radius)
    }

    // ===== LegacyLocation JSON round-trip =====

    @Test
    fun legacyLocationRoundTrip() {
        @Suppress("DEPRECATION")
        val original = TasksJsonImporter.LegacyLocation().apply {
            name = "Round Trip"
            address = "456 Oak Ave"
            phone = "+1-555-0200"
            url = "https://roundtrip.example.com"
            latitude = 34.0522
            longitude = -118.2437
            radius = 300
            arrival = true
            departure = false
        }
        val encoded = json.encodeToString(original)
        @Suppress("DEPRECATION")
        val decoded = json.decodeFromString<TasksJsonImporter.LegacyLocation>(encoded)
        assertEquals(original.name, decoded.name)
        assertEquals(original.address, decoded.address)
        assertEquals(original.phone, decoded.phone)
        assertEquals(original.url, decoded.url)
        assertEquals(original.latitude, decoded.latitude, 0.0001)
        assertEquals(original.longitude, decoded.longitude, 0.0001)
        assertEquals(original.radius, decoded.radius)
        assertEquals(original.arrival, decoded.arrival)
        assertEquals(original.departure, decoded.departure)
    }

    // ===== LegacyLocation edge cases =====

    @Test
    fun legacyLocationEquator() {
        @Suppress("DEPRECATION")
        val location = TasksJsonImporter.LegacyLocation()
        location.latitude = 0.0
        location.longitude = 0.0
        assertEquals(0.0, location.latitude, 0.0)
        assertEquals(0.0, location.longitude, 0.0)
    }

    @Test
    fun legacyLocationInternationalDateLine() {
        @Suppress("DEPRECATION")
        val location = TasksJsonImporter.LegacyLocation()
        location.longitude = 180.0
        assertEquals(180.0, location.longitude, 0.0)
    }

    @Test
    fun legacyLocationNegativeLongitude() {
        @Suppress("DEPRECATION")
        val location = TasksJsonImporter.LegacyLocation()
        location.longitude = -180.0
        assertEquals(-180.0, location.longitude, 0.0)
    }

    @Test
    fun legacyLocationExtremeLatitude() {
        @Suppress("DEPRECATION")
        val location = TasksJsonImporter.LegacyLocation()
        location.latitude = 90.0
        assertEquals(90.0, location.latitude, 0.0)
        location.latitude = -90.0
        assertEquals(-90.0, location.latitude, 0.0)
    }

    // ===== ImportResult used as accumulator =====

    @Test
    fun importResultAccumulatesOverMultipleBatches() {
        val result = TasksJsonImporter.ImportResult()
        // Simulate batch 1
        for (i in 1..10) {
            result.taskCount++
            if (i % 3 == 0) result.skipCount++ else result.importCount++
        }
        assertEquals(10, result.taskCount)
        assertEquals(7, result.importCount)
        assertEquals(3, result.skipCount)
        // Simulate batch 2
        for (i in 1..5) {
            result.taskCount++
            result.importCount++
        }
        assertEquals(15, result.taskCount)
        assertEquals(12, result.importCount)
        assertEquals(3, result.skipCount)
    }

    // ===== LegacyLocation serialization with null string fields =====

    @Test
    fun legacyLocationSerializesNullStrings() {
        @Suppress("DEPRECATION")
        val location = TasksJsonImporter.LegacyLocation()
        // All string fields are null by default
        val encoded = json.encodeToString(location)
        @Suppress("DEPRECATION")
        val decoded = json.decodeFromString<TasksJsonImporter.LegacyLocation>(encoded)
        assertNull(decoded.name)
        assertNull(decoded.address)
        assertNull(decoded.phone)
        assertNull(decoded.url)
    }

    // ===== LegacyLocation with unicode strings =====

    @Test
    fun legacyLocationUnicodeStrings() {
        @Suppress("DEPRECATION")
        val location = TasksJsonImporter.LegacyLocation()
        location.name = "\u6771\u4eac\u30bf\u30ef\u30fc" // Tokyo Tower in Japanese
        location.address = "4 Chome-2-8 Shibakoen, Minato City, \u6771\u4eac\u90fd"
        val encoded = json.encodeToString(location)
        @Suppress("DEPRECATION")
        val decoded = json.decodeFromString<TasksJsonImporter.LegacyLocation>(encoded)
        assertEquals("\u6771\u4eac\u30bf\u30ef\u30fc", decoded.name)
        assertEquals("4 Chome-2-8 Shibakoen, Minato City, \u6771\u4eac\u90fd", decoded.address)
    }

    // ===== LegacyLocation with special characters in URL =====

    @Test
    fun legacyLocationSpecialCharactersInUrl() {
        @Suppress("DEPRECATION")
        val location = TasksJsonImporter.LegacyLocation()
        location.url = "https://maps.google.com/?q=40.7128,-74.0060&z=15"
        val encoded = json.encodeToString(location)
        @Suppress("DEPRECATION")
        val decoded = json.decodeFromString<TasksJsonImporter.LegacyLocation>(encoded)
        assertEquals("https://maps.google.com/?q=40.7128,-74.0060&z=15", decoded.url)
    }
}
