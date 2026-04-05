@file:Suppress("ClassName")

package org.tasks.caldav

import kotlinx.coroutines.test.runTest
import net.fortuna.ical4j.model.property.Geo
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.mockito.Mockito.mock
import org.mockito.Mockito.never
import org.mockito.Mockito.verify
import org.mockito.Mockito.`when`
import org.mockito.ArgumentMatchers.anyString
import org.tasks.data.Location
import org.tasks.data.MergedGeofence
import org.tasks.data.entity.Geofence
import org.tasks.data.entity.Place
import org.tasks.data.dao.LocationDao
import org.tasks.location.Geocoder
import org.tasks.location.LocationService
import org.tasks.location.MapPosition
import org.tasks.preferences.AppPreferences
import com.todoroo.astrid.alarms.AlarmService
import org.tasks.data.TaskSaver
import org.tasks.data.dao.AlarmDao
import org.tasks.data.dao.CaldavDao
import org.tasks.data.dao.TagDao
import org.tasks.data.dao.TagDataDao
import org.tasks.data.dao.TaskDao
import org.tasks.notifications.Notifier
import java.math.BigDecimal

class iCalendarSetPlaceTest {

    private lateinit var preferences: AppPreferences
    private lateinit var locationDao: LocationDao

    /** Records interactions for verification. */
    private val insertedPlaces = mutableListOf<Place>()
    private val insertedGeofences = mutableListOf<Geofence>()
    private val updatedPlaces = mutableListOf<Place>()
    private val updatedGeofences = mutableListOf<Geofence>()
    private val updatedLocationPlaces = mutableListOf<Place?>()

    private lateinit var ical: iCalendar

    /** Minimal LocationService test double. */
    inner class TestLocationService : LocationService {
        override val locationDao: LocationDao get() = this@iCalendarSetPlaceTest.locationDao
        override val appPreferences: AppPreferences get() = this@iCalendarSetPlaceTest.preferences
        override suspend fun currentLocation(): MapPosition? = null
        override fun addGeofences(geofence: MergedGeofence) {}
        override fun removeGeofences(place: Place) {}
        override suspend fun updateGeofences(place: Place?) {
            updatedLocationPlaces.add(place)
        }
    }

    /** Simple Geocoder test double. */
    class TestGeocoder : Geocoder {
        var result: Place? = null
        var exception: RuntimeException? = null
        override suspend fun reverseGeocode(mapPosition: MapPosition): Place? {
            exception?.let { throw it }
            return result
        }
    }

    private lateinit var geocoder: TestGeocoder

    /** Custom LocationDao wrapper that records inserts/updates without delegating to mock. */
    inner class RecordingLocationDao(private val delegate: LocationDao) : LocationDao by delegate {
        var nextPlaceId = 100L
        override suspend fun insert(place: Place): Long {
            insertedPlaces.add(place)
            return nextPlaceId++
        }
        override suspend fun insert(location: Geofence): Long {
            insertedGeofences.add(location)
            return 1L
        }
        override suspend fun update(place: Place) {
            updatedPlaces.add(place)
        }
        override suspend fun update(geofence: Geofence) {
            updatedGeofences.add(geofence)
        }
    }

    private lateinit var recordingLocationDao: RecordingLocationDao

    @Before
    fun setUp() {
        val tagDataDao = mock(TagDataDao::class.java)
        preferences = mock(AppPreferences::class.java)
        locationDao = mock(LocationDao::class.java)
        geocoder = TestGeocoder()
        val tagDao = mock(TagDao::class.java)
        val taskDao = mock(TaskDao::class.java)
        val taskSaver = mock(TaskSaver::class.java)
        val caldavDao = mock(CaldavDao::class.java)
        val alarmDao = mock(AlarmDao::class.java)
        val alarmService = mock(AlarmService::class.java)
        val vtodoCache = mock(VtodoCache::class.java)
        val notifier = mock(Notifier::class.java)

        recordingLocationDao = RecordingLocationDao(locationDao)

        ical = iCalendar(
            tagDataDao = tagDataDao,
            preferences = preferences,
            locationDao = recordingLocationDao,
            geocoder = geocoder,
            locationService = TestLocationService(),
            tagDao = tagDao,
            taskDao = taskDao,
            taskSaver = taskSaver,
            caldavDao = caldavDao,
            alarmDao = alarmDao,
            alarmService = alarmService,
            vtodoCache = vtodoCache,
            notifier = notifier,
        )

        // Clear recording lists
        insertedPlaces.clear()
        insertedGeofences.clear()
        updatedPlaces.clear()
        updatedGeofences.clear()
        updatedLocationPlaces.clear()
    }

    // --- setPlace with null geo ---

    @Test
    fun setPlaceWithNullGeoDeletesGeofences() = runTest {
        val taskId = 42L
        val place = Place(id = 1, uid = "place-uid", latitude = 10.0, longitude = 20.0)
        val geofence = Geofence(id = 1, task = taskId, place = "place-uid")
        val location = Location(geofence = geofence, place = place)
        `when`(locationDao.getActiveGeofences(taskId)).thenReturn(listOf(location))

        ical.setPlace(taskId, null)

        verify(locationDao).delete(geofence)
        assertEquals(1, updatedLocationPlaces.size)
        assertEquals(place, updatedLocationPlaces[0])
    }

    @Test
    fun setPlaceWithNullGeoAndNoActiveGeofences() = runTest {
        val taskId = 42L
        `when`(locationDao.getActiveGeofences(taskId)).thenReturn(emptyList())

        ical.setPlace(taskId, null)

        // No geofences to delete
        assertTrue(updatedLocationPlaces.isEmpty())
        assertTrue(updatedLocationPlaces.isEmpty())
    }

    @Test
    fun setPlaceWithNullGeoDeletesMultipleGeofences() = runTest {
        val taskId = 42L
        val place1 = Place(id = 1, uid = "p1", latitude = 10.0, longitude = 20.0)
        val place2 = Place(id = 2, uid = "p2", latitude = 30.0, longitude = 40.0)
        val geofence1 = Geofence(id = 1, task = taskId, place = "p1")
        val geofence2 = Geofence(id = 2, task = taskId, place = "p2")
        val loc1 = Location(geofence = geofence1, place = place1)
        val loc2 = Location(geofence = geofence2, place = place2)
        `when`(locationDao.getActiveGeofences(taskId)).thenReturn(listOf(loc1, loc2))

        ical.setPlace(taskId, null)

        verify(locationDao).delete(geofence1)
        verify(locationDao).delete(geofence2)
        assertEquals(listOf(place1, place2), updatedLocationPlaces)
    }

    // --- setPlace with existing place ---

    @Test
    fun setPlaceWithExistingPlaceCreatesGeofence() = runTest {
        val taskId = 42L
        val geo = Geo(BigDecimal("40.7128"), BigDecimal("-74.0060"))
        val existingPlace = Place(
            id = 5, uid = "existing-uid",
            latitude = 40.7128, longitude = -74.006,
            name = "New York"
        )
        `when`(locationDao.findPlace(anyString(), anyString())).thenReturn(existingPlace)
        `when`(locationDao.getGeofences(taskId)).thenReturn(null)
        `when`(preferences.defaultLocationReminder()).thenReturn(1)

        ical.setPlace(taskId, geo)

        // Should not insert a new place
        assertTrue("Should not insert new place", insertedPlaces.isEmpty())
        // Should insert a geofence for the existing place
        assertEquals("Should insert one geofence", 1, insertedGeofences.size)
        assertEquals(taskId, insertedGeofences[0].task)
    }

    @Test
    fun setPlaceWithExistingPlaceAndSameGeofence() = runTest {
        val taskId = 42L
        val geo = Geo(BigDecimal("40.7128"), BigDecimal("-74.0060"))
        val existingPlace = Place(
            id = 5, uid = "existing-uid",
            latitude = 40.7128, longitude = -74.006,
            name = "New York"
        )
        val existingGeofence = Geofence(id = 10, task = taskId, place = "existing-uid")
        val existingLocation = Location(geofence = existingGeofence, place = existingPlace)
        `when`(locationDao.findPlace(anyString(), anyString())).thenReturn(existingPlace)
        `when`(locationDao.getGeofences(taskId)).thenReturn(existingLocation)

        ical.setPlace(taskId, geo)

        // Same place, no new geofences
        assertTrue("Should not insert new geofence", insertedGeofences.isEmpty())
        assertTrue("Should not update geofence", updatedGeofences.isEmpty())
    }

    @Test
    fun setPlaceWithExistingPlaceAndDifferentGeofence() = runTest {
        val taskId = 42L
        val geo = Geo(BigDecimal("40.7128"), BigDecimal("-74.0060"))
        val newPlace = Place(
            id = 5, uid = "new-uid",
            latitude = 40.7128, longitude = -74.006,
            name = "New York"
        )
        val oldPlace = Place(
            id = 3, uid = "old-uid",
            latitude = 51.5074, longitude = -0.1278,
            name = "London"
        )
        val existingGeofence = Geofence(id = 10, task = taskId, place = "old-uid")
        val existingLocation = Location(geofence = existingGeofence, place = oldPlace)
        `when`(locationDao.findPlace(anyString(), anyString())).thenReturn(newPlace)
        `when`(locationDao.getGeofences(taskId)).thenReturn(existingLocation)

        ical.setPlace(taskId, geo)

        // Should update the geofence's place uid
        assertEquals(1, updatedGeofences.size)
        assertEquals("new-uid", updatedGeofences[0].place)
        // Should update geofences for both places
        assertEquals(2, updatedLocationPlaces.size)
    }

    // --- setPlace with new place ---

    @Test
    fun setPlaceWithNewPlaceInsertsPlace() = runTest {
        val taskId = 42L
        val geo = Geo(BigDecimal("48.8566"), BigDecimal("2.3522"))
        `when`(locationDao.findPlace(anyString(), anyString())).thenReturn(null)
        geocoder.result = null
        `when`(locationDao.getGeofences(taskId)).thenReturn(null)
        `when`(preferences.defaultLocationReminder()).thenReturn(1)

        ical.setPlace(taskId, geo)

        assertEquals(1, insertedPlaces.size)
        assertEquals(1, insertedGeofences.size)
    }

    @Test
    fun setPlaceWithNewPlaceAndCloseGeocoderResult() = runTest {
        val taskId = 42L
        val geo = Geo(BigDecimal("48.8566"), BigDecimal("2.3522"))
        `when`(locationDao.findPlace(anyString(), anyString())).thenReturn(null)
        geocoder.result = Place(
            latitude = 48.8566, longitude = 2.3522,
            name = "Paris", address = "Paris, France",
            phone = "+33123456789", url = "https://paris.fr"
        )
        `when`(locationDao.getGeofences(taskId)).thenReturn(null)
        `when`(preferences.defaultLocationReminder()).thenReturn(1)

        ical.setPlace(taskId, geo)

        assertEquals(1, insertedPlaces.size)
        // The geocoded place is close enough, should update with details
        assertEquals(1, updatedPlaces.size)
    }

    @Test
    fun setPlaceWithNewPlaceAndFarGeocoderResult() = runTest {
        val taskId = 42L
        val geo = Geo(BigDecimal("48.8566"), BigDecimal("2.3522"))
        `when`(locationDao.findPlace(anyString(), anyString())).thenReturn(null)
        geocoder.result = Place(
            latitude = 49.0, longitude = 3.0,
            name = "Elsewhere", address = "Far Away"
        )
        `when`(locationDao.getGeofences(taskId)).thenReturn(null)
        `when`(preferences.defaultLocationReminder()).thenReturn(1)

        ical.setPlace(taskId, geo)

        // Geocoded place is too far, should NOT update with details
        assertTrue("Should not update place", updatedPlaces.isEmpty())
    }

    @Test
    fun setPlaceWithNewPlaceAndGeocoderException() = runTest {
        val taskId = 42L
        val geo = Geo(BigDecimal("48.8566"), BigDecimal("2.3522"))
        `when`(locationDao.findPlace(anyString(), anyString())).thenReturn(null)
        geocoder.exception = RuntimeException("network error")
        `when`(locationDao.getGeofences(taskId)).thenReturn(null)
        `when`(preferences.defaultLocationReminder()).thenReturn(1)

        // Should not throw; exception is caught internally
        ical.setPlace(taskId, geo)

        assertEquals(1, insertedPlaces.size)
        assertTrue("Should not update place on exception", updatedPlaces.isEmpty())
    }

    @Test
    fun setPlaceWithNewPlaceAndGeocoderReturnsNull() = runTest {
        val taskId = 42L
        val geo = Geo(BigDecimal("48.8566"), BigDecimal("2.3522"))
        `when`(locationDao.findPlace(anyString(), anyString())).thenReturn(null)
        geocoder.result = null
        `when`(locationDao.getGeofences(taskId)).thenReturn(null)
        `when`(preferences.defaultLocationReminder()).thenReturn(2)

        ical.setPlace(taskId, geo)

        assertTrue("Should not update place", updatedPlaces.isEmpty())
        assertEquals(1, insertedGeofences.size)
    }

    // --- Geofence reminder preferences ---

    @Test
    fun setPlaceCreatesArrivalGeofence() = runTest {
        val taskId = 42L
        val geo = Geo(BigDecimal("10.0"), BigDecimal("20.0"))
        val place = Place(id = 1, uid = "uid", latitude = 10.0, longitude = 20.0)
        `when`(locationDao.findPlace(anyString(), anyString())).thenReturn(place)
        `when`(locationDao.getGeofences(taskId)).thenReturn(null)
        `when`(preferences.defaultLocationReminder()).thenReturn(1)

        ical.setPlace(taskId, geo)

        assertEquals(1, insertedGeofences.size)
        assertEquals(true, insertedGeofences[0].isArrival)
        assertEquals(false, insertedGeofences[0].isDeparture)
        assertEquals(taskId, insertedGeofences[0].task)
    }

    @Test
    fun setPlaceCreatesDepartureGeofence() = runTest {
        val taskId = 42L
        val geo = Geo(BigDecimal("10.0"), BigDecimal("20.0"))
        val place = Place(id = 1, uid = "uid", latitude = 10.0, longitude = 20.0)
        `when`(locationDao.findPlace(anyString(), anyString())).thenReturn(place)
        `when`(locationDao.getGeofences(taskId)).thenReturn(null)
        `when`(preferences.defaultLocationReminder()).thenReturn(2)

        ical.setPlace(taskId, geo)

        assertEquals(1, insertedGeofences.size)
        assertEquals(false, insertedGeofences[0].isArrival)
        assertEquals(true, insertedGeofences[0].isDeparture)
    }

    @Test
    fun setPlaceCreatesBothArrivalAndDepartureGeofence() = runTest {
        val taskId = 42L
        val geo = Geo(BigDecimal("10.0"), BigDecimal("20.0"))
        val place = Place(id = 1, uid = "uid", latitude = 10.0, longitude = 20.0)
        `when`(locationDao.findPlace(anyString(), anyString())).thenReturn(place)
        `when`(locationDao.getGeofences(taskId)).thenReturn(null)
        `when`(preferences.defaultLocationReminder()).thenReturn(3)

        ical.setPlace(taskId, geo)

        assertEquals(1, insertedGeofences.size)
        assertEquals(true, insertedGeofences[0].isArrival)
        assertEquals(true, insertedGeofences[0].isDeparture)
    }
}
