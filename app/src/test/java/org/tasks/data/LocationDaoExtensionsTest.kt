package org.tasks.data

import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.mockito.Mockito.mock
import org.mockito.Mockito.`when`
import org.tasks.data.dao.LocationDao
import org.tasks.data.entity.Geofence
import org.tasks.data.entity.Place
import org.tasks.data.entity.Task
import org.tasks.preferences.AppPreferences

class LocationDaoExtensionsTest {

    private lateinit var locationDao: LocationDao
    private lateinit var preferences: AppPreferences

    @Before
    fun setUp() {
        locationDao = mock(LocationDao::class.java)
        preferences = mock(AppPreferences::class.java)
    }

    // --- createGeofence ---

    @Test
    fun createGeofenceWithArrivalOnly() = runTest {
        `when`(preferences.defaultLocationReminder()).thenReturn(1)

        val geofence = createGeofence("place-uid", preferences)

        assertEquals("place-uid", geofence.place)
        assertTrue(geofence.isArrival)
        assertFalse(geofence.isDeparture)
    }

    @Test
    fun createGeofenceWithDepartureOnly() = runTest {
        `when`(preferences.defaultLocationReminder()).thenReturn(2)

        val geofence = createGeofence("place-uid", preferences)

        assertEquals("place-uid", geofence.place)
        assertFalse(geofence.isArrival)
        assertTrue(geofence.isDeparture)
    }

    @Test
    fun createGeofenceWithBothArrivalAndDeparture() = runTest {
        `when`(preferences.defaultLocationReminder()).thenReturn(3)

        val geofence = createGeofence("place-uid", preferences)

        assertEquals("place-uid", geofence.place)
        assertTrue(geofence.isArrival)
        assertTrue(geofence.isDeparture)
    }

    @Test
    fun createGeofenceWithNoReminders() = runTest {
        `when`(preferences.defaultLocationReminder()).thenReturn(0)

        val geofence = createGeofence("place-uid", preferences)

        assertEquals("place-uid", geofence.place)
        assertFalse(geofence.isArrival)
        assertFalse(geofence.isDeparture)
    }

    @Test
    fun createGeofenceWithNullPlace() = runTest {
        `when`(preferences.defaultLocationReminder()).thenReturn(1)

        val geofence = createGeofence(null, preferences)

        assertNull(geofence.place)
        assertTrue(geofence.isArrival)
    }

    @Test
    fun createGeofenceWithUnknownReminderValue() = runTest {
        `when`(preferences.defaultLocationReminder()).thenReturn(99)

        val geofence = createGeofence("place-uid", preferences)

        assertFalse(geofence.isArrival)
        assertFalse(geofence.isDeparture)
    }

    // --- getLocation ---

    @Test
    fun getLocationForNewTaskWithPlaceTransitory() = runTest {
        val task = Task(id = Task.NO_ID)
        task.putTransitory(Place.KEY, "place-uid-123")
        val place = Place(uid = "place-uid-123", name = "Home")
        `when`(locationDao.getPlace("place-uid-123")).thenReturn(place)
        `when`(preferences.defaultLocationReminder()).thenReturn(1)

        val result = locationDao.getLocation(task, preferences)

        assertNotNull(result)
        assertEquals("Home", result!!.place.name)
        assertTrue(result.geofence.isArrival)
    }

    @Test
    fun getLocationForNewTaskWithNoPlaceTransitory() = runTest {
        val task = Task(id = Task.NO_ID)

        val result = locationDao.getLocation(task, preferences)

        assertNull(result)
    }

    @Test
    fun getLocationForNewTaskWithPlaceTransitoryButPlaceNotFound() = runTest {
        val task = Task(id = Task.NO_ID)
        task.putTransitory(Place.KEY, "nonexistent-uid")
        `when`(locationDao.getPlace("nonexistent-uid")).thenReturn(null)

        val result = locationDao.getLocation(task, preferences)

        assertNull(result)
    }

    @Test
    fun getLocationForExistingTask() = runTest {
        val task = Task(id = 42)
        val place = Place(uid = "place-uid", name = "Office")
        val geofence = Geofence(task = 42, place = "place-uid", isArrival = true)
        val location = Location(geofence = geofence, place = place)
        `when`(locationDao.getGeofences(42)).thenReturn(location)

        val result = locationDao.getLocation(task, preferences)

        assertNotNull(result)
        assertEquals("Office", result!!.place.name)
    }

    @Test
    fun getLocationForExistingTaskWithNoGeofence() = runTest {
        val task = Task(id = 42)
        `when`(locationDao.getGeofences(42)).thenReturn(null)

        val result = locationDao.getLocation(task, preferences)

        assertNull(result)
    }
}
