package org.tasks.backup

import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.mockito.Mockito.mock
import org.mockito.Mockito.verify
import org.mockito.Mockito.`when`
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

class TasksJsonExporterDeepTest {

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
            tagDataDao = tagDataDao,
            taskDao = taskDao,
            userActivityDao = userActivityDao,
            preferences = preferences,
            alarmDao = alarmDao,
            locationDao = locationDao,
            tagDao = tagDao,
            filterDao = filterDao,
            taskAttachmentDao = taskAttachmentDao,
            caldavDao = caldavDao,
            workManager = workManager,
            taskListMetadataDao = taskListMetadataDao,
            vtodoCache = vtodoCache,
        )
    }

    @Suppress("UNCHECKED_CAST")
    private fun setupEmptyPreferences() {
        `when`(preferences.getPrefs(Integer::class.java)).thenReturn(emptyMap())
        `when`(preferences.getPrefs(java.lang.Long::class.java)).thenReturn(emptyMap())
        `when`(preferences.getPrefs(String::class.java)).thenReturn(emptyMap())
        `when`(preferences.getPrefs(java.lang.Boolean::class.java)).thenReturn(emptyMap())
        `when`(preferences.getPrefs(java.util.Set::class.java)).thenReturn(emptyMap<String, java.util.Set<*>>())
    }

    // ===== doSettingsExport =====

    @Test
    fun settingsExportProducesValidJson() = runTest {
        setupEmptyPreferences()
        val os = ByteArrayOutputStream()
        exporter.doSettingsExport(os)
        val json = os.toString(Charsets.UTF_8.name())
        assertTrue(json.startsWith("{"))
        assertTrue(json.endsWith("}"))
    }

    @Test
    fun settingsExportContainsVersion() = runTest {
        setupEmptyPreferences()
        val os = ByteArrayOutputStream()
        exporter.doSettingsExport(os)
        assertTrue(os.toString(Charsets.UTF_8.name()).contains("\"version\":"))
    }

    @Test
    fun settingsExportContainsTimestamp() = runTest {
        setupEmptyPreferences()
        val os = ByteArrayOutputStream()
        exporter.doSettingsExport(os)
        assertTrue(os.toString(Charsets.UTF_8.name()).contains("\"timestamp\":"))
    }

    @Test
    fun settingsExportContainsDataWrapper() = runTest {
        setupEmptyPreferences()
        val os = ByteArrayOutputStream()
        exporter.doSettingsExport(os)
        assertTrue(os.toString(Charsets.UTF_8.name()).contains("\"data\":{"))
    }

    @Test
    fun settingsExportContainsAllPrefTypes() = runTest {
        setupEmptyPreferences()
        val os = ByteArrayOutputStream()
        exporter.doSettingsExport(os)
        val json = os.toString(Charsets.UTF_8.name())
        assertTrue(json.contains("\"intPrefs\":"))
        assertTrue(json.contains("\"longPrefs\":"))
        assertTrue(json.contains("\"stringPrefs\":"))
        assertTrue(json.contains("\"boolPrefs\":"))
        assertTrue(json.contains("\"setPrefs\":"))
    }

    @Suppress("UNCHECKED_CAST")
    @Test
    fun settingsExportWithAllPrefTypes() = runTest {
        `when`(preferences.getPrefs(Integer::class.java)).thenReturn(mapOf("int_key" to 42 as Integer) as Map<String, Integer>)
        `when`(preferences.getPrefs(java.lang.Long::class.java)).thenReturn(mapOf("long_key" to java.lang.Long.valueOf(999L)) as Map<String, java.lang.Long>)
        `when`(preferences.getPrefs(String::class.java)).thenReturn(mapOf("str_key" to "value") as Map<String, String>)
        `when`(preferences.getPrefs(java.lang.Boolean::class.java)).thenReturn(mapOf("bool_key" to java.lang.Boolean.TRUE) as Map<String, java.lang.Boolean>)
        `when`(preferences.getPrefs(java.util.Set::class.java)).thenReturn(mapOf("set_key" to java.util.HashSet(setOf("a"))) as Map<String, java.util.Set<*>>)

        val os = ByteArrayOutputStream()
        exporter.doSettingsExport(os)

        val json = os.toString(Charsets.UTF_8.name())
        assertTrue(json.contains("\"int_key\":42"))
        assertTrue(json.contains("\"long_key\":999"))
        assertTrue(json.contains("\"str_key\":\"value\""))
        assertTrue(json.contains("\"bool_key\":true"))
        assertTrue(json.contains("\"set_key\""))
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

    // ===== JsonWriter: write(key, value) with various types =====

    @Test
    fun writeTaskEntity() {
        val sw = StringWriter()
        val task = Task(title = "Buy groceries", priority = Task.Priority.HIGH)
        JsonWriter(sw).write("task", task)
        assertTrue(sw.toString().contains("\"title\":\"Buy groceries\""))
    }

    @Test
    fun writeListOfAlarms() {
        val sw = StringWriter()
        val alarms = listOf(Alarm(time = 1000L, type = Alarm.TYPE_DATE_TIME), Alarm(time = 2000L, type = Alarm.TYPE_REL_START))
        JsonWriter(sw).write("alarms", alarms)
        assertTrue(sw.toString().contains("\"alarms\":["))
    }

    @Test
    fun writeListOfGeofences() {
        val sw = StringWriter()
        val geos = listOf(Geofence(place = "p1", isArrival = true), Geofence(place = "p2", isDeparture = true))
        JsonWriter(sw).write("geofences", geos)
        assertTrue(sw.toString().contains("\"place\":\"p1\""))
        assertTrue(sw.toString().contains("\"place\":\"p2\""))
    }

    @Test
    fun writeListOfTags() {
        val sw = StringWriter()
        val tags = listOf(Tag(name = "work", tagUid = "uid-1"), Tag(name = "personal", tagUid = "uid-2"))
        JsonWriter(sw).write("tags", tags)
        assertTrue(sw.toString().contains("\"name\":\"work\""))
        assertTrue(sw.toString().contains("\"name\":\"personal\""))
    }

    @Test
    fun writeListOfComments() {
        val sw = StringWriter()
        val comments = listOf(UserActivity(message = "First"), UserActivity(message = "Second"))
        JsonWriter(sw).write("comments", comments)
        assertTrue(sw.toString().contains("\"message\":\"First\""))
    }

    @Test
    fun writeListOfCaldavTasks() {
        val sw = StringWriter()
        val tasks = listOf(CaldavTask(calendar = "cal-1", remoteId = "r-1"))
        JsonWriter(sw).write("caldavTasks", tasks, lastItem = true)
        assertTrue(sw.toString().contains("\"calendar\":\"cal-1\""))
        assertFalse(sw.toString().endsWith(","))
    }

    @Test
    fun writeVtodoString() {
        val sw = StringWriter()
        JsonWriter(sw).write("vtodo", "BEGIN:VTODO\nEND:VTODO")
        assertTrue(sw.toString().contains("BEGIN:VTODO"))
    }

    @Test
    fun writeListOfPlaces() {
        val sw = StringWriter()
        val places = listOf(Place(uid = "p1", name = "Office"), Place(uid = "p2", name = "Home"))
        JsonWriter(sw).write("places", places)
        assertTrue(sw.toString().contains("\"name\":\"Office\""))
        assertTrue(sw.toString().contains("\"name\":\"Home\""))
    }

    @Test
    fun writeListOfTagData() {
        val sw = StringWriter()
        val tagData = listOf(TagData(name = "work", remoteId = "td-1"))
        JsonWriter(sw).write("tags", tagData)
        assertTrue(sw.toString().contains("\"name\":\"work\""))
    }

    @Test
    fun writeListOfFilters() {
        val sw = StringWriter()
        val filters = listOf(Filter(title = "Active", sql = "SELECT 1"))
        JsonWriter(sw).write("filters", filters)
        assertTrue(sw.toString().contains("\"title\":\"Active\""))
    }

    @Test
    fun writeListOfCaldavAccounts() {
        val sw = StringWriter()
        val accounts = listOf(CaldavAccount(uuid = "acc-1", name = "My Cal"))
        JsonWriter(sw).write("caldavAccounts", accounts)
        assertTrue(sw.toString().contains("\"uuid\":\"acc-1\""))
    }

    @Test
    fun writeListOfCaldavCalendars() {
        val sw = StringWriter()
        val calendars = listOf(CaldavCalendar(uuid = "cal-1", name = "Personal"))
        JsonWriter(sw).write("caldavCalendars", calendars)
        assertTrue(sw.toString().contains("\"uuid\":\"cal-1\""))
    }

    @Test
    fun writeListOfTaskListMetadata() {
        val sw = StringWriter()
        val meta = listOf(TaskListMetadata())
        JsonWriter(sw).write("taskListMetadata", meta)
        assertTrue(sw.toString().contains("\"taskListMetadata\":"))
    }

    @Test
    fun writeListOfTaskAttachments() {
        val sw = StringWriter()
        val attachments = listOf(TaskAttachment(remoteId = "f1", name = "pic.jpg", uri = "content://pic"))
        JsonWriter(sw).write("taskAttachments", attachments)
        assertTrue(sw.toString().contains("\"name\":\"pic.jpg\""))
    }

    // ===== JsonWriter: edge cases =====

    @Test
    fun writeEmptyListAsLastItem() {
        val sw = StringWriter()
        JsonWriter(sw).write("empty", emptyList<String>(), lastItem = true)
        assertEquals("\"empty\":[]", sw.toString())
    }

    @Test
    fun writeMultipleFieldsInSequence() {
        val sw = StringWriter()
        val jw = JsonWriter(sw)
        jw.write("{")
        jw.write("version", 100)
        jw.write("timestamp", 1680000000000L)
        jw.write("\"data\":{")
        jw.write("tasks", emptyList<String>(), lastItem = true)
        jw.write("}")
        jw.write("}")
        val result = sw.toString()
        assertTrue(result.startsWith("{"))
        assertTrue(result.endsWith("}"))
        assertTrue(result.contains("\"version\":100"))
        assertTrue(result.contains("\"tasks\":[]"))
    }

    @Test
    fun writeNegativeLong() {
        val sw = StringWriter()
        JsonWriter(sw).write("neg", -999L)
        assertEquals("\"neg\":-999,", sw.toString())
    }

    @Test
    fun writeSpecialCharsInString() {
        val sw = StringWriter()
        JsonWriter(sw).write("special", "line1\nline2\ttab")
        assertTrue(sw.toString().contains("\\n"))
        assertTrue(sw.toString().contains("\\t"))
    }

    @Test
    fun writeUnicodeInString() {
        val sw = StringWriter()
        JsonWriter(sw).write("jp", "\u6771\u4eac")
        assertTrue(sw.toString().contains("\u6771\u4eac"))
    }

    @Test
    fun writeMapOfIntPrefs() {
        val sw = StringWriter()
        JsonWriter(sw).write("intPrefs", mapOf("theme" to 2))
        assertTrue(sw.toString().contains("\"theme\":2"))
    }

    @Test
    fun writeMapOfLongPrefs() {
        val sw = StringWriter()
        JsonWriter(sw).write("longPrefs", mapOf("sync" to 5000L))
        assertTrue(sw.toString().contains("\"sync\":5000"))
    }

    @Test
    fun writeMapOfStringPrefs() {
        val sw = StringWriter()
        JsonWriter(sw).write("stringPrefs", mapOf("user" to "alice"))
        assertTrue(sw.toString().contains("\"user\":\"alice\""))
    }

    @Test
    fun writeMapOfBoolPrefs() {
        val sw = StringWriter()
        JsonWriter(sw).write("boolPrefs", mapOf("dark" to true))
        assertTrue(sw.toString().contains("\"dark\":true"))
    }

    @Test
    fun writeMapOfSetPrefs() {
        val sw = StringWriter()
        JsonWriter(sw).write("setPrefs", mapOf("group" to setOf("a", "b")), lastItem = true)
        assertFalse(sw.toString().endsWith(","))
        assertTrue(sw.toString().contains("\"group\""))
    }

    // ===== ExportType enum =====

    @Test
    fun exportTypeServiceName() {
        assertEquals("EXPORT_TYPE_SERVICE", TasksJsonExporter.ExportType.EXPORT_TYPE_SERVICE.name)
    }

    @Test
    fun exportTypeManualName() {
        assertEquals("EXPORT_TYPE_MANUAL", TasksJsonExporter.ExportType.EXPORT_TYPE_MANUAL.name)
    }

    @Test
    fun exportTypeServiceOrdinal() {
        assertEquals(0, TasksJsonExporter.ExportType.EXPORT_TYPE_SERVICE.ordinal)
    }

    @Test
    fun exportTypeManualOrdinal() {
        assertEquals(1, TasksJsonExporter.ExportType.EXPORT_TYPE_MANUAL.ordinal)
    }

    @Test
    fun exportTypeValuesLength() {
        assertEquals(2, TasksJsonExporter.ExportType.values().size)
    }

    @Test
    fun exportTypeValueOf() {
        assertEquals(TasksJsonExporter.ExportType.EXPORT_TYPE_SERVICE, TasksJsonExporter.ExportType.valueOf("EXPORT_TYPE_SERVICE"))
        assertEquals(TasksJsonExporter.ExportType.EXPORT_TYPE_MANUAL, TasksJsonExporter.ExportType.valueOf("EXPORT_TYPE_MANUAL"))
    }

    // ===== BackupConstants =====

    @Test
    fun backupFileNameContainsAuto() {
        assertTrue(String.format(BackupConstants.BACKUP_FILE_NAME, "20230101T1200").startsWith("auto."))
    }

    @Test
    fun exportFileNameContainsUser() {
        assertTrue(String.format(BackupConstants.EXPORT_FILE_NAME, "20230101T1200").startsWith("user."))
    }

    @Test
    fun backupFileNameEndsWithJson() {
        assertTrue(String.format(BackupConstants.BACKUP_FILE_NAME, "20230101T1200").endsWith(".json"))
    }

    @Test
    fun exportFileNameEndsWithJson() {
        assertTrue(String.format(BackupConstants.EXPORT_FILE_NAME, "20230101T1200").endsWith(".json"))
    }

    // ===== CaldavTask.isDeleted =====

    @Test
    fun caldavTaskNotDeletedWhenZero() {
        assertFalse(CaldavTask(calendar = "c", deleted = 0L).isDeleted())
    }

    @Test
    fun caldavTaskDeletedWhenPositive() {
        assertTrue(CaldavTask(calendar = "c", deleted = 1000L).isDeleted())
    }

    @Test
    fun firstNonDeletedCaldavTask() {
        val tasks = listOf(
            CaldavTask(calendar = "c1", deleted = 1000L),
            CaldavTask(calendar = "c2", deleted = 0L),
            CaldavTask(calendar = "c3", deleted = 0L),
        )
        assertEquals("c2", tasks.firstOrNull { !it.isDeleted() }?.calendar)
    }

    // ===== Vtodo resolution for export =====

    @Test
    fun vtodoCacheReturnsNullForDeletedCaldavTask() = runTest {
        val caldavTask = CaldavTask(calendar = "cal-1", deleted = 1000L)
        `when`(vtodoCache.getVtodo(caldavTask)).thenReturn(null)
        assertEquals(null, vtodoCache.getVtodo(caldavTask))
    }

    @Test
    fun vtodoCacheReturnsVtodoForNonDeletedTask() = runTest {
        val caldavTask = CaldavTask(calendar = "cal-1", deleted = 0L)
        `when`(vtodoCache.getVtodo(caldavTask)).thenReturn("BEGIN:VTODO\nUID:123\nEND:VTODO")
        assertEquals("BEGIN:VTODO\nUID:123\nEND:VTODO", vtodoCache.getVtodo(caldavTask))
    }
}
