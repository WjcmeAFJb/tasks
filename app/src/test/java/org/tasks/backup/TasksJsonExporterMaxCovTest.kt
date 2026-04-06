package org.tasks.backup

import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.tasks.backup.TasksJsonExporter.Companion.JsonWriter
import org.tasks.caldav.VtodoCache
import org.tasks.data.dao.AlarmDao
import org.tasks.data.dao.CaldavDao
import org.tasks.data.dao.FilterDao
import org.tasks.data.dao.LocationDao
import org.tasks.data.dao.TagDao
import org.tasks.data.dao.TagDataDao
import org.tasks.data.dao.TaskAttachmentDao
import org.tasks.data.dao.TaskDao
import org.tasks.data.dao.TaskListMetadataDao
import org.tasks.data.dao.UserActivityDao
import org.tasks.data.entity.Alarm
import org.tasks.data.entity.CaldavAccount
import org.tasks.data.entity.CaldavCalendar
import org.tasks.data.entity.CaldavTask
import org.tasks.data.entity.Filter
import org.tasks.data.entity.Geofence
import org.tasks.data.entity.Place
import org.tasks.data.entity.Tag
import org.tasks.data.entity.TagData
import org.tasks.data.entity.Task
import org.tasks.data.entity.TaskAttachment
import org.tasks.data.entity.TaskListMetadata
import org.tasks.data.entity.UserActivity
import org.tasks.jobs.WorkManager
import org.tasks.preferences.Preferences
import java.io.ByteArrayOutputStream
import java.io.StringWriter

class TasksJsonExporterMaxCovTest {

    private lateinit var tagDataDao: TagDataDao
    private lateinit var taskDao: TaskDao
    private lateinit var userActivityDao: UserActivityDao
    private lateinit var preferences: Preferences
    private lateinit var alarmDao: AlarmDao
    private lateinit var locationDao: LocationDao
    private lateinit var tagDao: TagDao
    private lateinit var filterDao: FilterDao
    private lateinit var taskAttachmentDao: TaskAttachmentDao
    private lateinit var caldavDao: CaldavDao
    private lateinit var workManager: WorkManager
    private lateinit var taskListMetadataDao: TaskListMetadataDao
    private lateinit var vtodoCache: VtodoCache
    private lateinit var exporter: TasksJsonExporter

    @Before
    fun setUp() {
        tagDataDao = mock()
        taskDao = mock()
        userActivityDao = mock()
        preferences = mock()
        alarmDao = mock()
        locationDao = mock()
        tagDao = mock()
        filterDao = mock()
        taskAttachmentDao = mock()
        caldavDao = mock()
        workManager = mock()
        taskListMetadataDao = mock()
        vtodoCache = mock()
        exporter = TasksJsonExporter(
            tagDataDao, taskDao, userActivityDao, preferences,
            alarmDao, locationDao, tagDao, filterDao, taskAttachmentDao,
            caldavDao, workManager, taskListMetadataDao, vtodoCache,
        )
    }

    @Suppress("UNCHECKED_CAST")
    private fun setupEmptyPreferences() {
        whenever(preferences.getPrefs(Integer::class.java)).thenReturn(emptyMap())
        whenever(preferences.getPrefs(java.lang.Long::class.java)).thenReturn(emptyMap())
        whenever(preferences.getPrefs(String::class.java)).thenReturn(emptyMap())
        whenever(preferences.getPrefs(java.lang.Boolean::class.java)).thenReturn(emptyMap())
        whenever(preferences.getPrefs(java.util.Set::class.java)).thenReturn(emptyMap<String, java.util.Set<*>>())
    }

    // ================================================================
    // doSettingsExport — complete flow
    // ================================================================

    @Test
    fun settingsExportProducesValidStructure() = runTest {
        setupEmptyPreferences()
        val os = ByteArrayOutputStream()
        exporter.doSettingsExport(os)
        val json = os.toString(Charsets.UTF_8.name())
        assertTrue(json.startsWith("{"))
        assertTrue(json.endsWith("}"))
        assertTrue(json.contains("\"version\":"))
        assertTrue(json.contains("\"timestamp\":"))
        assertTrue(json.contains("\"data\":{"))
        assertTrue(json.contains("\"intPrefs\":"))
        assertTrue(json.contains("\"longPrefs\":"))
        assertTrue(json.contains("\"stringPrefs\":"))
        assertTrue(json.contains("\"boolPrefs\":"))
        assertTrue(json.contains("\"setPrefs\":"))
    }

    @Suppress("UNCHECKED_CAST")
    @Test
    fun settingsExportIncludesAllPrefValues() = runTest {
        whenever(preferences.getPrefs(Integer::class.java))
            .thenReturn(mapOf("k1" to 1 as Integer) as Map<String, Integer>)
        whenever(preferences.getPrefs(java.lang.Long::class.java))
            .thenReturn(mapOf("k2" to java.lang.Long.valueOf(2L)) as Map<String, java.lang.Long>)
        whenever(preferences.getPrefs(String::class.java))
            .thenReturn(mapOf("k3" to "val") as Map<String, String>)
        whenever(preferences.getPrefs(java.lang.Boolean::class.java))
            .thenReturn(mapOf("k4" to java.lang.Boolean.TRUE) as Map<String, java.lang.Boolean>)
        whenever(preferences.getPrefs(java.util.Set::class.java))
            .thenReturn(mapOf("k5" to java.util.HashSet(setOf("s1"))) as Map<String, java.util.Set<*>>)

        val os = ByteArrayOutputStream()
        exporter.doSettingsExport(os)
        val json = os.toString(Charsets.UTF_8.name())
        assertTrue(json.contains("\"k1\":1"))
        assertTrue(json.contains("\"k2\":2"))
        assertTrue(json.contains("\"k3\":\"val\""))
        assertTrue(json.contains("\"k4\":true"))
        assertTrue(json.contains("\"k5\":"))
    }

    @Test
    fun settingsExportCallsAllPrefGetters() = runTest {
        setupEmptyPreferences()
        val os = ByteArrayOutputStream()
        exporter.doSettingsExport(os)
        verify(preferences).getPrefs(Integer::class.java)
        verify(preferences).getPrefs(java.lang.Long::class.java)
        verify(preferences).getPrefs(String::class.java)
        verify(preferences).getPrefs(java.lang.Boolean::class.java)
        verify(preferences).getPrefs(java.util.Set::class.java)
    }

    // ================================================================
    // JsonWriter — edge cases and types
    // ================================================================

    @Test
    fun jsonWriterWriteInt() {
        val sw = StringWriter()
        JsonWriter(sw).write("count", 42)
        assertEquals("\"count\":42,", sw.toString())
    }

    @Test
    fun jsonWriterWriteLong() {
        val sw = StringWriter()
        JsonWriter(sw).write("ts", 1234567890L)
        assertEquals("\"ts\":1234567890,", sw.toString())
    }

    @Test
    fun jsonWriterWriteString() {
        val sw = StringWriter()
        JsonWriter(sw).write("name", "test")
        assertEquals("\"name\":\"test\",", sw.toString())
    }

    @Test
    fun jsonWriterWriteBoolean() {
        val sw = StringWriter()
        JsonWriter(sw).write("flag", true)
        assertEquals("\"flag\":true,", sw.toString())
    }

    @Test
    fun jsonWriterWriteEmptyListLastItem() {
        val sw = StringWriter()
        JsonWriter(sw).write("list", emptyList<String>(), lastItem = true)
        assertEquals("\"list\":[]", sw.toString())
    }

    @Test
    fun jsonWriterWriteEmptyListNotLast() {
        val sw = StringWriter()
        JsonWriter(sw).write("list", emptyList<String>(), lastItem = false)
        assertEquals("\"list\":[],", sw.toString())
    }

    @Test
    fun jsonWriterWriteTask() {
        val sw = StringWriter()
        JsonWriter(sw).write("task", Task(title = "Buy milk"))
        assertTrue(sw.toString().contains("\"title\":\"Buy milk\""))
    }

    @Test
    fun jsonWriterWriteAlarms() {
        val sw = StringWriter()
        JsonWriter(sw).write("alarms", listOf(Alarm(time = 5000L, type = Alarm.TYPE_DATE_TIME)))
        assertTrue(sw.toString().contains("5000"))
    }

    @Test
    fun jsonWriterWriteGeofences() {
        val sw = StringWriter()
        JsonWriter(sw).write("geo", listOf(Geofence(place = "p1", isArrival = true)))
        assertTrue(sw.toString().contains("\"place\":\"p1\""))
    }

    @Test
    fun jsonWriterWriteTags() {
        val sw = StringWriter()
        JsonWriter(sw).write("tags", listOf(Tag(name = "work", tagUid = "uid")))
        assertTrue(sw.toString().contains("\"name\":\"work\""))
    }

    @Test
    fun jsonWriterWriteCaldavTasks() {
        val sw = StringWriter()
        JsonWriter(sw).write("ct", listOf(CaldavTask(calendar = "c1")), lastItem = true)
        assertFalse(sw.toString().endsWith(","))
    }

    @Test
    fun jsonWriterWriteVtodo() {
        val sw = StringWriter()
        JsonWriter(sw).write("vtodo", "BEGIN:VTODO\nEND:VTODO")
        assertTrue(sw.toString().contains("BEGIN:VTODO"))
    }

    @Test
    fun jsonWriterWritePlaces() {
        val sw = StringWriter()
        JsonWriter(sw).write("places", listOf(Place(name = "Home")))
        assertTrue(sw.toString().contains("\"name\":\"Home\""))
    }

    @Test
    fun jsonWriterWriteFilters() {
        val sw = StringWriter()
        JsonWriter(sw).write("filters", listOf(Filter(title = "Active", sql = "SELECT 1")))
        assertTrue(sw.toString().contains("\"title\":\"Active\""))
    }

    @Test
    fun jsonWriterWriteCaldavAccounts() {
        val sw = StringWriter()
        JsonWriter(sw).write("accounts", listOf(CaldavAccount(uuid = "acc-1")))
        assertTrue(sw.toString().contains("\"uuid\":\"acc-1\""))
    }

    @Test
    fun jsonWriterWriteCaldavCalendars() {
        val sw = StringWriter()
        JsonWriter(sw).write("calendars", listOf(CaldavCalendar(uuid = "cal-1")))
        assertTrue(sw.toString().contains("\"uuid\":\"cal-1\""))
    }

    @Test
    fun jsonWriterWriteMap() {
        val sw = StringWriter()
        JsonWriter(sw).write("prefs", mapOf("theme" to 2, "font" to 14))
        assertTrue(sw.toString().contains("\"theme\":2"))
        assertTrue(sw.toString().contains("\"font\":14"))
    }

    @Test
    fun jsonWriterCompositeOutput() {
        val sw = StringWriter()
        val jw = JsonWriter(sw)
        jw.write("{")
        jw.write("version", 100)
        jw.write("timestamp", 999L)
        jw.write("\"data\":{")
        jw.write("tasks", emptyList<Task>(), lastItem = true)
        jw.write("}")
        jw.write("}")
        val result = sw.toString()
        assertTrue(result.startsWith("{"))
        assertTrue(result.endsWith("}"))
        assertTrue(result.contains("\"version\":100"))
    }

    // ================================================================
    // ExportType enum
    // ================================================================

    @Test
    fun exportTypeCount() {
        assertEquals(2, TasksJsonExporter.ExportType.values().size)
    }

    @Test
    fun exportTypeService() {
        assertEquals(0, TasksJsonExporter.ExportType.EXPORT_TYPE_SERVICE.ordinal)
    }

    @Test
    fun exportTypeManual() {
        assertEquals(1, TasksJsonExporter.ExportType.EXPORT_TYPE_MANUAL.ordinal)
    }
}
