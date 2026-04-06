package org.tasks

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class StringsTest {

    @Test
    fun nullReturnsTrue() {
        assertTrue(Strings.isNullOrEmpty(null))
    }

    @Test
    fun emptyStringReturnsTrue() {
        assertTrue(Strings.isNullOrEmpty(""))
    }

    @Test
    fun nonEmptyStringReturnsFalse() {
        assertFalse(Strings.isNullOrEmpty("hello"))
    }

    @Test
    fun whitespaceOnlyReturnsFalse() {
        assertFalse(Strings.isNullOrEmpty(" "))
    }

    @Test
    fun singleCharReturnsFalse() {
        assertFalse(Strings.isNullOrEmpty("a"))
    }

    @Test
    fun tabCharReturnsFalse() {
        assertFalse(Strings.isNullOrEmpty("\t"))
    }

    @Test
    fun newlineReturnsFalse() {
        assertFalse(Strings.isNullOrEmpty("\n"))
    }

    @Test
    fun multipleSpacesReturnsFalse() {
        assertFalse(Strings.isNullOrEmpty("   "))
    }

    @Test
    fun longStringReturnsFalse() {
        assertFalse(Strings.isNullOrEmpty("this is a longer string with spaces"))
    }

    @Test
    fun unicodeStringReturnsFalse() {
        assertFalse(Strings.isNullOrEmpty("\u00e9"))
    }
}
