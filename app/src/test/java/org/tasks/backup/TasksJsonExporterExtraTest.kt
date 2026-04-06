package org.tasks.backup

import kotlinx.coroutines.test.runTest
import kotlinx.serialization.Serializable
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.mockito.Mockito.mock
import org.mockito.Mockito.verify
import org.mockito.Mockito.`when`
import org.tasks.backup.TasksJsonExporter.Companion.JsonWriter
import org.tasks.backup.TasksJsonExporter.ExportType
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
import org.tasks.data.entity.UserActivity
import org.tasks.jobs.WorkManager
import org.tasks.preferences.Preferences
import java.io.ByteArrayOutputStream
import java.io.StringWriter

class TasksJsonExporterExtraTest {

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

    // ===== JsonWriter =====

    @Test fun jsonWriterUsesProvidedJsonInstance() {
        val customJson = kotlinx.serialization.json.Json { prettyPrint = true }
        assertEquals(customJson, JsonWriter(StringWriter(), customJson).json)
    }

    @Test fun jsonWriterUsesDefaultJsonInstance() {
        assertEquals(kotlinx.serialization.json.Json, JsonWriter(StringWriter()).json)
    }

    @Test fun jsonWriterWriterProperty() {
        val sw = StringWriter()
        assertEquals(sw, JsonWriter(sw).writer)
    }

    @Test fun writeMapWithMultipleEntries() {
        val sw = StringWriter()
        JsonWriter(sw).write("prefs", mapOf("a" to 1, "b" to 2))
        assertTrue(sw.toString().contains("\"a\":1"))
        assertTrue(sw.toString().contains("\"b\":2"))
    }

    @Test fun writeMapAsLastItem() {
        val sw = StringWriter()
        JsonWriter(sw).write("prefs", mapOf("a" to 1), lastItem = true)
        assertFalse(sw.toString().endsWith(","))
    }

    @Test fun writeEmptyMap() {
        val sw = StringWriter()
        JsonWriter(sw).write("empty", emptyMap<String, Int>())
        assertEquals("\"empty\":{},", sw.toString())
    }

    @Test fun writeEmptyMapAsLastItem() {
        val sw = StringWriter()
        JsonWriter(sw).write("empty", emptyMap<String, Int>(), lastItem = true)
        assertEquals("\"empty\":{}", sw.toString())
    }

    @Test fun writeNestedObject() {
        val sw = StringWriter()
        JsonWriter(sw).write("nested", mapOf("inner" to mapOf("deep" to "value")))
        assertTrue(sw.toString().contains("\"deep\":\"value\""))
    }

    @Test fun writeSetOfStrings() {
        val sw = StringWriter()
        JsonWriter(sw).write("tags", setOf("a", "b", "c"))
        assertTrue(sw.toString().startsWith("\"tags\":["))
    }

    @Test fun writeSetAsLastItem() {
        val sw = StringWriter()
        JsonWriter(sw).write("tags", setOf("x"), lastItem = true)
        assertFalse(sw.toString().endsWith(","))
    }

    @Test fun writeMapOfSets() {
        val sw = StringWriter()
        JsonWriter(sw).write("setPrefs", mapOf("group" to setOf("item1", "item2")), lastItem = true)
        assertFalse(sw.toString().endsWith(","))
    }

    @Serializable data class Nested(val list: List<Int>, val map: Map<String, String>)

    @Test fun writeComplexSerializableObject() {
        val sw = StringWriter()
        JsonWriter(sw).write("data", Nested(listOf(1, 2), mapOf("k" to "v")))
        assertTrue(sw.toString().contains("\"list\":[1,2]"))
    }

    @Test fun writeDoubleValue() {
        val sw = StringWriter()
        JsonWriter(sw).write("pi", 3.14159)
        assertEquals("\"pi\":3.14159,", sw.toString())
    }

    // ===== doSettingsExport =====

    @Test fun doSettingsExportWritesValidJson() = runTest {
        setupEmptyPreferences()
        val os = ByteArrayOutputStream()
        exporter.doSettingsExport(os)
        val json = os.toString(Charsets.UTF_8.name())
        assertTrue(json.startsWith("{"))
        assertTrue(json.endsWith("}"))
        assertTrue(json.contains("\"version\":"))
        assertTrue(json.contains("\"timestamp\":"))
        assertTrue(json.contains("\"data\":"))
    }

    @Test fun doSettingsExportIncludesIntPrefs() = runTest {
        @Suppress("UNCHECKED_CAST")
        `when`(preferences.getPrefs(Integer::class.java)).thenReturn(mapOf("pref_int" to 42 as Integer) as Map<String, Integer>)
        `when`(preferences.getPrefs(java.lang.Long::class.java)).thenReturn(emptyMap())
        `when`(preferences.getPrefs(String::class.java)).thenReturn(emptyMap())
        `when`(preferences.getPrefs(java.lang.Boolean::class.java)).thenReturn(emptyMap())
        `when`(preferences.getPrefs(Set::class.java)).thenReturn(emptyMap())
        val os = ByteArrayOutputStream()
        exporter.doSettingsExport(os)
        assertTrue(os.toString(Charsets.UTF_8.name()).contains("\"pref_int\":42"))
    }

    @Test fun doSettingsExportIncludesLongPrefs() = runTest {
        `when`(preferences.getPrefs(Integer::class.java)).thenReturn(emptyMap())
        @Suppress("UNCHECKED_CAST")
        `when`(preferences.getPrefs(java.lang.Long::class.java)).thenReturn(mapOf("pref_long" to java.lang.Long.valueOf(9999999L)) as Map<String, java.lang.Long>)
        `when`(preferences.getPrefs(String::class.java)).thenReturn(emptyMap())
        `when`(preferences.getPrefs(java.lang.Boolean::class.java)).thenReturn(emptyMap())
        `when`(preferences.getPrefs(Set::class.java)).thenReturn(emptyMap())
        val os = ByteArrayOutputStream()
        exporter.doSettingsExport(os)
        assertTrue(os.toString(Charsets.UTF_8.name()).contains("\"pref_long\":9999999"))
    }

    @Test fun doSettingsExportIncludesStringPrefs() = runTest {
        `when`(preferences.getPrefs(Integer::class.java)).thenReturn(emptyMap())
        `when`(preferences.getPrefs(java.lang.Long::class.java)).thenReturn(emptyMap())
        @Suppress("UNCHECKED_CAST")
        `when`(preferences.getPrefs(String::class.java)).thenReturn(mapOf("pref_str" to "hello") as Map<String, String>)
        `when`(preferences.getPrefs(java.lang.Boolean::class.java)).thenReturn(emptyMap())
        `when`(preferences.getPrefs(Set::class.java)).thenReturn(emptyMap())
        val os = ByteArrayOutputStream()
        exporter.doSettingsExport(os)
        assertTrue(os.toString(Charsets.UTF_8.name()).contains("\"pref_str\":\"hello\""))
    }

    @Test fun doSettingsExportIncludesBoolPrefs() = runTest {
        `when`(preferences.getPrefs(Integer::class.java)).thenReturn(emptyMap())
        `when`(preferences.getPrefs(java.lang.Long::class.java)).thenReturn(emptyMap())
        `when`(preferences.getPrefs(String::class.java)).thenReturn(emptyMap())
        @Suppress("UNCHECKED_CAST")
        `when`(preferences.getPrefs(java.lang.Boolean::class.java)).thenReturn(mapOf("pref_bool" to java.lang.Boolean.TRUE) as Map<String, java.lang.Boolean>)
        `when`(preferences.getPrefs(Set::class.java)).thenReturn(emptyMap())
        val os = ByteArrayOutputStream()
        exporter.doSettingsExport(os)
        assertTrue(os.toString(Charsets.UTF_8.name()).contains("\"pref_bool\":true"))
    }

    @Suppress("UNCHECKED_CAST")
    @Test fun doSettingsExportIncludesSetPrefs() = runTest {
        `when`(preferences.getPrefs(Integer::class.java)).thenReturn(emptyMap())
        `when`(preferences.getPrefs(java.lang.Long::class.java)).thenReturn(emptyMap())
        `when`(preferences.getPrefs(String::class.java)).thenReturn(emptyMap())
        `when`(preferences.getPrefs(java.lang.Boolean::class.java)).thenReturn(emptyMap())
        `when`(preferences.getPrefs(Set::class.java)).thenReturn(mapOf("pref_set" to setOf("a", "b") as Set<*>) as Map<String, Set<*>>)
        val os = ByteArrayOutputStream()
        exporter.doSettingsExport(os)
        assertTrue(os.toString(Charsets.UTF_8.name()).contains("\"pref_set\""))
    }

    @Test fun doSettingsExportReadsAllPrefsTypes() = runTest {
        setupEmptyPreferences()
        val os = ByteArrayOutputStream()
        exporter.doSettingsExport(os)
        verify(preferences).getPrefs(Integer::class.java)
        verify(preferences).getPrefs(java.lang.Long::class.java)
        verify(preferences).getPrefs(String::class.java)
        verify(preferences).getPrefs(java.lang.Boolean::class.java)
        verify(preferences).getPrefs(Set::class.java)
    }

    @Test fun doSettingsExportHasDataWrapper() = runTest {
        setupEmptyPreferences()
        val os = ByteArrayOutputStream()
        exporter.doSettingsExport(os)
        val json = os.toString(Charsets.UTF_8.name())
        assertTrue(json.contains("\"data\":{"))
        assertTrue(json.contains("\"intPrefs\":"))
        assertTrue(json.contains("\"longPrefs\":"))
        assertTrue(json.contains("\"stringPrefs\":"))
        assertTrue(json.contains("\"boolPrefs\":"))
        assertTrue(json.contains("\"setPrefs\":"))
    }

    @Suppress("UNCHECKED_CAST")
    @Test fun doSettingsExportWithMixedPreferences() = runTest {
        `when`(preferences.getPrefs(Integer::class.java)).thenReturn(mapOf("theme" to 2 as Integer) as Map<String, Integer>)
        `when`(preferences.getPrefs(java.lang.Long::class.java)).thenReturn(mapOf("lastSync" to java.lang.Long.valueOf(5000L)) as Map<String, java.lang.Long>)
        `when`(preferences.getPrefs(String::class.java)).thenReturn(mapOf("username" to "alice") as Map<String, String>)
        `when`(preferences.getPrefs(java.lang.Boolean::class.java)).thenReturn(mapOf("darkMode" to java.lang.Boolean.TRUE) as Map<String, java.lang.Boolean>)
        `when`(preferences.getPrefs(Set::class.java)).thenReturn(mapOf("tags" to setOf("work") as Set<*>) as Map<String, Set<*>>)
        val os = ByteArrayOutputStream()
        exporter.doSettingsExport(os)
        val json = os.toString(Charsets.UTF_8.name())
        assertTrue(json.contains("\"theme\":2"))
        assertTrue(json.contains("\"username\":\"alice\""))
        assertTrue(json.contains("\"darkMode\":true"))
    }

    // ===== Export DAO wiring =====

    @Test fun daoReturnsSingleTask() = runTest {
        val task = Task(id = 1L, title = "Test Task", remoteId = "uuid-1")
        `when`(taskDao.fetch(1L)).thenReturn(task)
        assertEquals(task, taskDao.fetch(1L))
    }

    @Test fun daoReturnsAlarms() = runTest {
        `when`(alarmDao.getAlarms(2L)).thenReturn(listOf(Alarm(task = 2L, time = 1000L, type = Alarm.TYPE_DATE_TIME)))
        assertEquals(1, alarmDao.getAlarms(2L).size)
    }

    @Test fun daoReturnsGeofences() = runTest {
        `when`(locationDao.getGeofencesForTask(3L)).thenReturn(listOf(Geofence(task = 3L, place = "p1", isArrival = true)))
        assertTrue(locationDao.getGeofencesForTask(3L)[0].isArrival)
    }

    @Test fun daoReturnsTags() = runTest {
        `when`(tagDao.getTagsForTask(4L)).thenReturn(listOf(Tag(task = 4L, name = "work", tagUid = "uid-1")))
        assertEquals("work", tagDao.getTagsForTask(4L)[0].name)
    }

    @Test fun daoReturnsComments() = runTest {
        `when`(userActivityDao.getComments(5L)).thenReturn(listOf(UserActivity(message = "First"), UserActivity(message = "Second")))
        assertEquals(2, userActivityDao.getComments(5L).size)
    }

    @Test fun vtodoCacheReturnsVtodo() = runTest {
        val caldavTask = CaldavTask(calendar = "cal-1", remoteId = "r-1", deleted = 0L)
        `when`(vtodoCache.getVtodo(caldavTask)).thenReturn("BEGIN:VTODO\nEND:VTODO")
        assertEquals("BEGIN:VTODO\nEND:VTODO", vtodoCache.getVtodo(caldavTask))
    }

    @Test fun daoReturnsPlaces() = runTest {
        `when`(locationDao.getPlaces()).thenReturn(listOf(Place(uid = "p1", name = "Office")))
        assertEquals(1, locationDao.getPlaces().size)
    }

    @Test fun daoReturnsTagData() = runTest {
        `when`(tagDataDao.getAll()).thenReturn(listOf(TagData(name = "work", remoteId = "td-1")))
        assertEquals(1, tagDataDao.getAll().size)
    }

    @Test fun daoReturnsFilters() = runTest {
        `when`(filterDao.getFilters()).thenReturn(listOf(Filter(title = "Active", sql = "SELECT 1")))
        assertEquals("Active", filterDao.getFilters()[0].title)
    }

    @Test fun daoReturnsCaldavAccounts() = runTest {
        `when`(caldavDao.getAccounts()).thenReturn(listOf(CaldavAccount(uuid = "acc-1", name = "Acc")))
        assertEquals(1, caldavDao.getAccounts().size)
    }

    @Test fun daoReturnsCaldavCalendars() = runTest {
        `when`(caldavDao.getCalendars()).thenReturn(listOf(CaldavCalendar(uuid = "cal-1", name = "Cal")))
        assertEquals(1, caldavDao.getCalendars().size)
    }

    @Test fun daoReturnsTaskAttachments() = runTest {
        `when`(taskAttachmentDao.getAttachments()).thenReturn(listOf(TaskAttachment(remoteId = "f1", name = "pic.jpg", uri = "content://pic")))
        assertEquals(1, taskAttachmentDao.getAttachments().size)
    }

    // ===== ExportType =====

    @Test fun exportTypeValueOf() {
        assertEquals(ExportType.EXPORT_TYPE_SERVICE, ExportType.valueOf("EXPORT_TYPE_SERVICE"))
        assertEquals(ExportType.EXPORT_TYPE_MANUAL, ExportType.valueOf("EXPORT_TYPE_MANUAL"))
    }

    @Test fun exportTypeOrdinals() {
        assertEquals(0, ExportType.EXPORT_TYPE_SERVICE.ordinal)
        assertEquals(1, ExportType.EXPORT_TYPE_MANUAL.ordinal)
    }

    // ===== JsonWriter: serializable entities =====

    @Test fun jsonWriterSerializesTask() {
        val sw = StringWriter()
        JsonWriter(sw).write("task", Task(title = "Buy milk", priority = Task.Priority.HIGH))
        assertTrue(sw.toString().contains("\"title\":\"Buy milk\""))
    }

    @Test fun jsonWriterSerializesAlarm() {
        val sw = StringWriter()
        JsonWriter(sw).write("alarm", Alarm(time = 5000L, type = Alarm.TYPE_SNOOZE))
        assertTrue(sw.toString().contains("\"type\":${Alarm.TYPE_SNOOZE}"))
    }

    @Test fun jsonWriterSerializesGeofence() {
        val sw = StringWriter()
        JsonWriter(sw).write("geo", Geofence(place = "p1", isArrival = true))
        assertTrue(sw.toString().contains("\"place\":\"p1\""))
    }

    @Test fun jsonWriterSerializesTag() {
        val sw = StringWriter()
        JsonWriter(sw).write("tag", Tag(name = "urgent", tagUid = "uid-1"))
        assertTrue(sw.toString().contains("\"name\":\"urgent\""))
    }

    @Test fun jsonWriterSerializesCaldavTask() {
        val sw = StringWriter()
        JsonWriter(sw).write("ct", CaldavTask(calendar = "cal-1", remoteId = "r-1"), lastItem = true)
        assertTrue(sw.toString().contains("\"calendar\":\"cal-1\""))
        assertFalse(sw.toString().endsWith(","))
    }

    @Test fun jsonWriterSerializesCaldavAccount() {
        val sw = StringWriter()
        JsonWriter(sw).write("acc", CaldavAccount(uuid = "acc-1", name = "My Cal"))
        assertTrue(sw.toString().contains("\"uuid\":\"acc-1\""))
    }

    @Test fun jsonWriterSerializesCaldavCalendar() {
        val sw = StringWriter()
        JsonWriter(sw).write("cal", CaldavCalendar(uuid = "cal-1", name = "Personal"))
        assertTrue(sw.toString().contains("\"uuid\":\"cal-1\""))
    }

    @Test fun jsonWriterSerializesPlace() {
        val sw = StringWriter()
        JsonWriter(sw).write("place", Place(uid = "p1", name = "Office"))
        assertTrue(sw.toString().contains("\"name\":\"Office\""))
    }

    @Test fun jsonWriterSerializesFilter() {
        val sw = StringWriter()
        JsonWriter(sw).write("filter", Filter(title = "Active", sql = "SELECT 1"))
        assertTrue(sw.toString().contains("\"title\":\"Active\""))
    }

    // ===== CaldavTask.isDeleted =====

    @Test fun nonDeletedCaldavTaskIsNotDeleted() {
        assertFalse(CaldavTask(calendar = "cal", deleted = 0L).isDeleted())
    }

    @Test fun deletedCaldavTaskIsDeleted() {
        assertTrue(CaldavTask(calendar = "cal", deleted = 1000L).isDeleted())
    }

    @Test fun firstNonDeletedCaldavTaskSelected() {
        val tasks = listOf(CaldavTask(calendar = "cal-1", deleted = 1000L), CaldavTask(calendar = "cal-2", deleted = 0L))
        assertEquals("cal-2", tasks.firstOrNull { !it.isDeleted() }?.calendar)
    }

    // ===== JsonWriter: special characters =====

    @Test fun jsonWriterHandlesUnicode() {
        val sw = StringWriter()
        JsonWriter(sw).write("name", "\u6771\u4eac")
        assertTrue(sw.toString().contains("\u6771\u4eac"))
    }

    @Test fun jsonWriterHandlesEmptyString() {
        val sw = StringWriter()
        JsonWriter(sw).write("empty", "")
        assertEquals("\"empty\":\"\",", sw.toString())
    }

    @Test fun jsonWriterHandlesQuotes() {
        val sw = StringWriter()
        JsonWriter(sw).write("quoted", "say \"hello\"")
        assertTrue(sw.toString().contains("\\\"hello\\\""))
    }

    @Test fun jsonWriterHandlesNewline() {
        val sw = StringWriter()
        JsonWriter(sw).write("multi", "line1\nline2")
        assertTrue(sw.toString().contains("\\n"))
    }

    // ===== BackupConstants format strings =====

    @Test fun backupFileNameFormat() {
        assertEquals("auto.20230101T1200.json", String.format(BackupConstants.BACKUP_FILE_NAME, "20230101T1200"))
    }

    @Test fun exportFileNameFormat() {
        assertEquals("user.20230101T1200.json", String.format(BackupConstants.EXPORT_FILE_NAME, "20230101T1200"))
    }

    // ===== JsonWriter zero/negative values =====

    @Test fun writeZeroInt() {
        val sw = StringWriter()
        JsonWriter(sw).write("zero", 0)
        assertEquals("\"zero\":0,", sw.toString())
    }

    @Test fun writeNegativeInt() {
        val sw = StringWriter()
        JsonWriter(sw).write("neg", -1)
        assertEquals("\"neg\":-1,", sw.toString())
    }

    @Test fun writeMaxLong() {
        val sw = StringWriter()
        JsonWriter(sw).write("max", Long.MAX_VALUE)
        assertEquals("\"max\":${Long.MAX_VALUE},", sw.toString())
    }

    // ===== Helpers =====

    private fun setupEmptyPreferences() {
        `when`(preferences.getPrefs(Integer::class.java)).thenReturn(emptyMap())
        `when`(preferences.getPrefs(java.lang.Long::class.java)).thenReturn(emptyMap())
        `when`(preferences.getPrefs(String::class.java)).thenReturn(emptyMap())
        `when`(preferences.getPrefs(java.lang.Boolean::class.java)).thenReturn(emptyMap())
        `when`(preferences.getPrefs(Set::class.java)).thenReturn(emptyMap())
    }
}
