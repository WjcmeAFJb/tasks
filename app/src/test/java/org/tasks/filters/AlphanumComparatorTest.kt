package org.tasks.filters

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class AlphanumComparatorTest {
    private val cmp = AlphanumComparator<String> { it }

    @Test fun equalStrings() = assertEquals(0, cmp.compare("abc", "abc"))
    @Test fun alphabeticOrder() = assertTrue(cmp.compare("apple", "banana") < 0)
    @Test fun reverseAlphabetic() = assertTrue(cmp.compare("banana", "apple") > 0)
    @Test fun caseInsensitive() = assertEquals(0, cmp.compare("ABC", "abc"))
    @Test fun numbersInOrder() = assertTrue(cmp.compare("file1", "file2") < 0)
    @Test fun numericSortNotAscii() = assertTrue(cmp.compare("file2", "file10") < 0)
    @Test fun numericSortAscii() = assertTrue(cmp.compare("file10", "file2") > 0)
    @Test fun mixedChunks() = assertTrue(cmp.compare("a1b", "a2b") < 0)
    @Test fun longerNumericChunk() = assertTrue(cmp.compare("file1", "file100") < 0)
    @Test fun sameNumberDifferentSuffix() = assertTrue(cmp.compare("file1a", "file1b") < 0)
    @Test fun emptyStrings() = assertEquals(0, cmp.compare("", ""))
    @Test fun emptyVsNonEmpty() = assertTrue(cmp.compare("", "a") < 0)
    @Test fun pureNumeric() = assertTrue(cmp.compare("1", "2") < 0)
    @Test fun pureNumericLarger() = assertTrue(cmp.compare("9", "10") < 0)

    @Test fun nullTitleReturnsZero() {
        val nullCmp = AlphanumComparator<String?> { it }
        assertEquals(0, nullCmp.compare(null, "abc"))
        assertEquals(0, nullCmp.compare("abc", null))
        assertEquals(0, nullCmp.compare(null, null))
    }

    @Test fun sortsList() {
        val input = listOf("item10", "item2", "item1", "item20")
        val sorted = input.sortedWith(cmp)
        assertEquals(listOf("item1", "item2", "item10", "item20"), sorted)
    }

    @Test fun sortsMixedAlphaNum() {
        val input = listOf("z1", "a10", "a2", "a1")
        val sorted = input.sortedWith(cmp)
        assertEquals(listOf("a1", "a2", "a10", "z1"), sorted)
    }

    @Test fun sortsWithLeadingZeros() {
        val input = listOf("file002", "file001", "file010")
        val sorted = input.sortedWith(cmp)
        assertEquals(listOf("file001", "file002", "file010"), sorted)
    }
}
