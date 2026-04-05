package org.tasks.backup

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class TasksJsonImporterTest {

    // ===== ImportResult =====

    @Test
    fun importResultDefaultValues() {
        val result = TasksJsonImporter.ImportResult()
        assertEquals(0, result.taskCount)
        assertEquals(0, result.importCount)
        assertEquals(0, result.skipCount)
    }

    @Test
    fun importResultTracksTasks() {
        val result = TasksJsonImporter.ImportResult()
        result.taskCount = 10
        assertEquals(10, result.taskCount)
    }

    @Test
    fun importResultTracksImportCount() {
        val result = TasksJsonImporter.ImportResult()
        result.importCount = 7
        assertEquals(7, result.importCount)
    }

    @Test
    fun importResultTracksSkipCount() {
        val result = TasksJsonImporter.ImportResult()
        result.skipCount = 3
        assertEquals(3, result.skipCount)
    }

    @Test
    fun importResultAllCountsIndependent() {
        val result = TasksJsonImporter.ImportResult()
        result.taskCount = 100
        result.importCount = 80
        result.skipCount = 20
        assertEquals(100, result.taskCount)
        assertEquals(80, result.importCount)
        assertEquals(20, result.skipCount)
    }

    @Test
    fun importResultCountsSumCorrectly() {
        val result = TasksJsonImporter.ImportResult()
        result.taskCount = 50
        result.importCount = 30
        result.skipCount = 20
        assertEquals(result.taskCount, result.importCount + result.skipCount)
    }

    @Test
    fun importResultIncrements() {
        val result = TasksJsonImporter.ImportResult()
        repeat(5) { result.taskCount++ }
        repeat(3) { result.importCount++ }
        repeat(2) { result.skipCount++ }
        assertEquals(5, result.taskCount)
        assertEquals(3, result.importCount)
        assertEquals(2, result.skipCount)
    }

    // ===== LegacyLocation =====

    @Test
    fun legacyLocationDefaultValues() {
        @Suppress("DEPRECATION")
        val location = TasksJsonImporter.LegacyLocation()
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
    fun legacyLocationSetName() {
        @Suppress("DEPRECATION")
        val location = TasksJsonImporter.LegacyLocation()
        location.name = "Office"
        assertEquals("Office", location.name)
    }

    @Test
    fun legacyLocationSetAddress() {
        @Suppress("DEPRECATION")
        val location = TasksJsonImporter.LegacyLocation()
        location.address = "123 Main St"
        assertEquals("123 Main St", location.address)
    }

    @Test
    fun legacyLocationSetCoordinates() {
        @Suppress("DEPRECATION")
        val location = TasksJsonImporter.LegacyLocation()
        location.latitude = 40.7128
        location.longitude = -74.0060
        assertEquals(40.7128, location.latitude, 0.0001)
        assertEquals(-74.0060, location.longitude, 0.0001)
    }

    @Test
    fun legacyLocationSetRadius() {
        @Suppress("DEPRECATION")
        val location = TasksJsonImporter.LegacyLocation()
        location.radius = 500
        assertEquals(500, location.radius)
    }

    @Test
    fun legacyLocationSetArrival() {
        @Suppress("DEPRECATION")
        val location = TasksJsonImporter.LegacyLocation()
        location.arrival = true
        assertTrue(location.arrival)
    }

    @Test
    fun legacyLocationSetDeparture() {
        @Suppress("DEPRECATION")
        val location = TasksJsonImporter.LegacyLocation()
        location.departure = true
        assertTrue(location.departure)
    }

    @Test
    fun legacyLocationArrivalAndDeparture() {
        @Suppress("DEPRECATION")
        val location = TasksJsonImporter.LegacyLocation()
        location.arrival = true
        location.departure = true
        assertTrue(location.arrival)
        assertTrue(location.departure)
    }

    @Test
    fun legacyLocationSetPhone() {
        @Suppress("DEPRECATION")
        val location = TasksJsonImporter.LegacyLocation()
        location.phone = "+1-555-0100"
        assertEquals("+1-555-0100", location.phone)
    }

    @Test
    fun legacyLocationSetUrl() {
        @Suppress("DEPRECATION")
        val location = TasksJsonImporter.LegacyLocation()
        location.url = "https://example.com"
        assertEquals("https://example.com", location.url)
    }

    @Test
    fun legacyLocationFullPopulation() {
        @Suppress("DEPRECATION")
        val location = TasksJsonImporter.LegacyLocation()
        location.name = "Home"
        location.address = "456 Oak Ave"
        location.phone = "+1-555-0200"
        location.url = "https://home.example.com"
        location.latitude = 34.0522
        location.longitude = -118.2437
        location.radius = 200
        location.arrival = true
        location.departure = false
        assertEquals("Home", location.name)
        assertEquals("456 Oak Ave", location.address)
        assertEquals("+1-555-0200", location.phone)
        assertEquals("https://home.example.com", location.url)
        assertEquals(34.0522, location.latitude, 0.0001)
        assertEquals(-118.2437, location.longitude, 0.0001)
        assertEquals(200, location.radius)
        assertTrue(location.arrival)
        assertFalse(location.departure)
    }
}
