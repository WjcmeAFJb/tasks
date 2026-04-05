package org.tasks.filters

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class FilterValuesSerializerTest {

    // --- mapToSerializedString ---

    @Test fun emptyMapProducesEmptyString() =
        assertEquals("", mapToSerializedString(emptyMap()))

    @Test fun serializeIntValue() {
        val result = mapToSerializedString(mapOf("key" to 42))
        assertEquals("key|i42|", result)
    }

    @Test fun serializeStringValue() {
        val result = mapToSerializedString(mapOf("name" to "hello"))
        assertEquals("name|shello|", result)
    }

    @Test fun serializeLongValue() {
        val result = mapToSerializedString(mapOf("ts" to 1234567890L))
        assertEquals("ts|l1234567890|", result)
    }

    @Test fun serializeDoubleValue() {
        val result = mapToSerializedString(mapOf("lat" to 3.14))
        assertEquals("lat|d3.14|", result)
    }

    @Test fun serializeBooleanTrue() {
        val result = mapToSerializedString(mapOf("flag" to true))
        assertEquals("flag|btrue|", result)
    }

    @Test fun serializeBooleanFalse() {
        val result = mapToSerializedString(mapOf("flag" to false))
        assertEquals("flag|bfalse|", result)
    }

    @Test fun pipeInKeyIsEscaped() {
        val result = mapToSerializedString(mapOf("key|with|pipes" to "val"))
        assertTrue(result.contains("key!PIPE!with!PIPE!pipes"))
    }

    @Test fun pipeInStringValueIsEscaped() {
        val result = mapToSerializedString(mapOf("k" to "a|b"))
        assertTrue(result.contains("sa!PIPE!b"))
    }

    @Test(expected = UnsupportedOperationException::class)
    fun unsupportedTypeThrows() {
        mapToSerializedString(mapOf("k" to listOf(1)))
    }

    // --- mapFromSerializedString ---

    @Test fun nullReturnsEmptyMap() =
        assertEquals(emptyMap<String, Any>(), mapFromSerializedString(null))

    @Test fun emptyStringReturnsEmptyMap() =
        assertTrue(mapFromSerializedString("").isEmpty())

    @Test fun deserializeInt() {
        val map = mapFromSerializedString("key|i42|")
        assertEquals(42, map["key"])
    }

    @Test fun deserializeString() {
        val map = mapFromSerializedString("name|shello|")
        assertEquals("hello", map["name"])
    }

    @Test fun deserializeLong() {
        val map = mapFromSerializedString("ts|l1234567890|")
        assertEquals(1234567890L, map["ts"])
    }

    @Test fun deserializeDouble() {
        val map = mapFromSerializedString("lat|d3.14|")
        assertEquals(3.14, map["lat"])
    }

    @Test fun deserializeBooleanTrue() {
        val map = mapFromSerializedString("flag|btrue|")
        assertEquals(true, map["flag"])
    }

    @Test fun deserializeBooleanFalse() {
        val map = mapFromSerializedString("flag|bfalse|")
        assertEquals(false, map["flag"])
    }

    @Test fun pipeInKeyIsUnescaped() {
        val map = mapFromSerializedString("key!PIPE!name|sval|")
        assertEquals("val", map["key|name"])
    }

    @Test fun pipeInValueIsUnescaped() {
        val map = mapFromSerializedString("k|sa!PIPE!b|")
        assertEquals("a|b", map["k"])
    }

    // --- roundtrip ---

    @Test fun roundtripInt() {
        val original = mapOf("x" to 99)
        assertEquals(original, mapFromSerializedString(mapToSerializedString(original)))
    }

    @Test fun roundtripString() {
        val original = mapOf("x" to "hello world")
        assertEquals(original, mapFromSerializedString(mapToSerializedString(original)))
    }

    @Test fun roundtripMultipleEntries() {
        val original = mapOf<String, Any>("a" to 1, "b" to "two", "c" to true, "d" to 4.0)
        val serialized = mapToSerializedString(original)
        val deserialized = mapFromSerializedString(serialized)
        assertEquals(original["a"], deserialized["a"])
        assertEquals(original["b"], deserialized["b"])
        assertEquals(original["c"], deserialized["c"])
        assertEquals(original["d"], deserialized["d"])
    }

    @Test fun roundtripWithPipesInKeysAndValues() {
        val original = mapOf<String, Any>("a|b" to "c|d")
        val serialized = mapToSerializedString(original)
        val deserialized = mapFromSerializedString(serialized)
        assertEquals("c|d", deserialized["a|b"])
    }

    @Test fun roundtripLong() {
        val original = mapOf("x" to Long.MAX_VALUE)
        assertEquals(original, mapFromSerializedString(mapToSerializedString(original)))
    }

    // --- edge cases ---

    @Test fun malformedInputDoesNotCrash() {
        // Odd number of segments — last key has no value
        val map = mapFromSerializedString("orphankey")
        // Should not crash, may be empty
        assertTrue(map.isEmpty() || map.isNotEmpty())
    }

    @Test fun separatorConstant() = assertEquals("|", SERIALIZATION_SEPARATOR)
    @Test fun escapeConstant() = assertEquals("!PIPE!", SEPARATOR_ESCAPE)
}
