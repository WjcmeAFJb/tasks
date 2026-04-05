package org.tasks.backup

import kotlinx.serialization.Serializable
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.tasks.backup.TasksJsonExporter.Companion.JsonWriter
import org.tasks.backup.TasksJsonExporter.ExportType
import java.io.StringWriter

class TasksJsonExporterTest {

    @Test fun writeRawString() {
        val sw = StringWriter()
        JsonWriter(sw).write("{}")
        assertEquals("{}", sw.toString())
    }

    @Test fun writeStringValue() {
        val sw = StringWriter()
        JsonWriter(sw).write("name", "hello")
        assertEquals("\"name\":\"hello\",", sw.toString())
    }

    @Test fun writeIntValue() {
        val sw = StringWriter()
        JsonWriter(sw).write("count", 42)
        assertEquals("\"count\":42,", sw.toString())
    }

    @Test fun writeBooleanValue() {
        val sw = StringWriter()
        JsonWriter(sw).write("flag", true)
        assertEquals("\"flag\":true,", sw.toString())
    }

    @Test fun writeLastItemNoTrailingComma() {
        val sw = StringWriter()
        JsonWriter(sw).write("last", "value", lastItem = true)
        assertEquals("\"last\":\"value\"", sw.toString())
    }

    @Test fun writeListValue() {
        val sw = StringWriter()
        JsonWriter(sw).write("items", listOf(1, 2, 3))
        assertEquals("\"items\":[1,2,3],", sw.toString())
    }

    @Test fun writeMapValue() {
        val sw = StringWriter()
        JsonWriter(sw).write("map", mapOf("a" to 1))
        assertTrue(sw.toString().contains("\"a\":1"))
    }

    @Test fun writeMultipleFields() {
        val sw = StringWriter()
        val jw = JsonWriter(sw)
        jw.write("{")
        jw.write("a", 1)
        jw.write("b", "two")
        jw.write("c", true, lastItem = true)
        jw.write("}")
        assertEquals("{\"a\":1,\"b\":\"two\",\"c\":true}", sw.toString())
    }

    @Test fun exportTypeService() = assertEquals("EXPORT_TYPE_SERVICE", ExportType.EXPORT_TYPE_SERVICE.name)
    @Test fun exportTypeManual() = assertEquals("EXPORT_TYPE_MANUAL", ExportType.EXPORT_TYPE_MANUAL.name)
    @Test fun exportTypeValues() = assertEquals(2, ExportType.values().size)

    @Serializable
    data class TestData(val x: Int, val y: String)

    @Test fun writeSerializableObject() {
        val sw = StringWriter()
        JsonWriter(sw).write("obj", TestData(1, "hello"))
        assertTrue(sw.toString().contains("\"x\":1"))
        assertTrue(sw.toString().contains("\"y\":\"hello\""))
    }

    @Test fun writeLongValue() {
        val sw = StringWriter()
        JsonWriter(sw).write("ts", 1680000000000L)
        assertEquals("\"ts\":1680000000000,", sw.toString())
    }

    @Test fun writeEmptyList() {
        val sw = StringWriter()
        JsonWriter(sw).write("items", emptyList<String>())
        assertEquals("\"items\":[],", sw.toString())
    }
}
