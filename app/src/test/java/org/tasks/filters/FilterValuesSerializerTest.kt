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
        // Odd number of segments -- last key has no value
        val map = mapFromSerializedString("orphankey")
        // Should not crash, may be empty
        assertTrue(map.isEmpty() || map.isNotEmpty())
    }

    @Test fun separatorConstant() = assertEquals("|", SERIALIZATION_SEPARATOR)
    @Test fun escapeConstant() = assertEquals("!PIPE!", SEPARATOR_ESCAPE)

    // --- Additional tests for expanded coverage ---

    @Test fun serializeZeroInt() {
        val result = mapToSerializedString(mapOf("z" to 0))
        assertEquals("z|i0|", result)
    }

    @Test fun serializeNegativeInt() {
        val result = mapToSerializedString(mapOf("n" to -42))
        assertEquals("n|i-42|", result)
    }

    @Test fun serializeMaxInt() {
        val result = mapToSerializedString(mapOf("m" to Int.MAX_VALUE))
        assertEquals("m|i${Int.MAX_VALUE}|", result)
    }

    @Test fun serializeMinInt() {
        val result = mapToSerializedString(mapOf("m" to Int.MIN_VALUE))
        assertEquals("m|i${Int.MIN_VALUE}|", result)
    }

    @Test fun serializeZeroLong() {
        val result = mapToSerializedString(mapOf("z" to 0L))
        assertEquals("z|l0|", result)
    }

    @Test fun serializeNegativeLong() {
        val result = mapToSerializedString(mapOf("n" to -999L))
        assertEquals("n|l-999|", result)
    }

    @Test fun serializeMinLong() {
        val result = mapToSerializedString(mapOf("m" to Long.MIN_VALUE))
        assertEquals("m|l${Long.MIN_VALUE}|", result)
    }

    @Test fun serializeZeroDouble() {
        val result = mapToSerializedString(mapOf("z" to 0.0))
        assertEquals("z|d0.0|", result)
    }

    @Test fun serializeNegativeDouble() {
        val result = mapToSerializedString(mapOf("n" to -1.5))
        assertEquals("n|d-1.5|", result)
    }

    @Test fun serializeEmptyString() {
        val result = mapToSerializedString(mapOf("e" to ""))
        assertEquals("e|s|", result)
    }

    @Test fun deserializeEmptyString() {
        val map = mapFromSerializedString("e|s|")
        assertEquals("", map["e"])
    }

    @Test fun roundtripEmptyString() {
        val original = mapOf("e" to "")
        assertEquals(original, mapFromSerializedString(mapToSerializedString(original)))
    }

    @Test fun roundtripNegativeInt() {
        val original = mapOf("n" to -42)
        assertEquals(original, mapFromSerializedString(mapToSerializedString(original)))
    }

    @Test fun roundtripNegativeDouble() {
        val original = mapOf("n" to -3.14)
        assertEquals(original, mapFromSerializedString(mapToSerializedString(original)))
    }

    @Test fun roundtripNegativeLong() {
        val original = mapOf("n" to -999L)
        assertEquals(original, mapFromSerializedString(mapToSerializedString(original)))
    }

    @Test fun roundtripBooleanFalse() {
        val original = mapOf("f" to false)
        assertEquals(original, mapFromSerializedString(mapToSerializedString(original)))
    }

    @Test fun deserializeNegativeInt() {
        val map = mapFromSerializedString("n|i-42|")
        assertEquals(-42, map["n"])
    }

    @Test fun deserializeNegativeLong() {
        val map = mapFromSerializedString("n|l-999|")
        assertEquals(-999L, map["n"])
    }

    @Test fun deserializeNegativeDouble() {
        val map = mapFromSerializedString("n|d-1.5|")
        assertEquals(-1.5, map["n"])
    }

    @Test fun multipleEntriesSerialization() {
        val map = mapOf<String, Any>("a" to 1, "b" to 2)
        val result = mapToSerializedString(map)
        // Both entries should be in the result
        assertTrue(result.contains("a|i1|"))
        assertTrue(result.contains("b|i2|"))
    }

    @Test fun multiplePipesInKey() {
        val result = mapToSerializedString(mapOf("a|b|c" to "val"))
        assertTrue(result.contains("a!PIPE!b!PIPE!c"))
    }

    @Test fun multiplePipesInValue() {
        val result = mapToSerializedString(mapOf("k" to "a|b|c"))
        assertTrue(result.contains("sa!PIPE!b!PIPE!c"))
    }

    @Test fun roundtripMultiplePipesInKeyAndValue() {
        val original = mapOf<String, Any>("a|b|c" to "d|e|f")
        val serialized = mapToSerializedString(original)
        val deserialized = mapFromSerializedString(serialized)
        assertEquals("d|e|f", deserialized["a|b|c"])
    }

    @Test fun serializeStringWithSpecialCharacters() {
        val result = mapToSerializedString(mapOf("k" to "hello\nworld"))
        assertEquals("k|shello\nworld|", result)
    }

    @Test fun roundtripStringWithSpecialCharacters() {
        val original = mapOf("k" to "hello\nworld\ttab")
        assertEquals(original, mapFromSerializedString(mapToSerializedString(original)))
    }

    @Test fun serializeLargeDouble() {
        val result = mapToSerializedString(mapOf("l" to 1.7976931348623157E308))
        assertTrue(result.startsWith("l|d"))
    }

    @Test fun deserializeMultipleEntries() {
        val map = mapFromSerializedString("a|i1|b|stwo|c|btrue|")
        assertEquals(1, map["a"])
        assertEquals("two", map["b"])
        assertEquals(true, map["c"])
    }

    @Test(expected = UnsupportedOperationException::class)
    fun unsupportedTypeFloatThrows() {
        mapToSerializedString(mapOf("k" to 1.0f))
    }

    @Test(expected = UnsupportedOperationException::class)
    fun unsupportedTypeMapThrows() {
        mapToSerializedString(mapOf("k" to mapOf("nested" to "value")))
    }

    @Test fun invalidNumberFallsBackToString() {
        // When the type says integer but value is not parseable as int,
        // it falls back to storing as string
        val map = mapFromSerializedString("k|inotanumber|")
        // The code catches NumberFormatException and re-stores as string
        assertEquals("notanumber", map["k"])
    }
}
