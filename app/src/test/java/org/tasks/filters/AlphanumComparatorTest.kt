package org.tasks.filters

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.tasks.data.entity.CaldavAccount
import org.tasks.data.entity.CaldavCalendar
import org.tasks.data.entity.TagData

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

    // --- Additional tests for expanded coverage ---

    @Test fun singleCharacterStrings() = assertTrue(cmp.compare("a", "b") < 0)

    @Test fun singleDigitVsMultiDigit() = assertTrue(cmp.compare("1", "10") < 0)

    @Test fun multipleNumericChunks() {
        // "1.2" vs "1.10" - should compare first chunk, then second
        assertTrue(cmp.compare("v1.2", "v1.10") < 0)
    }

    @Test fun multipleNumericChunksVersionSorting() {
        val versions = listOf("v2.0", "v1.10", "v1.2", "v1.1")
        val sorted = versions.sortedWith(cmp)
        assertEquals(listOf("v1.1", "v1.2", "v1.10", "v2.0"), sorted)
    }

    @Test fun allDigitStrings() = assertTrue(cmp.compare("100", "200") < 0)

    @Test fun allDigitStringsDifferentLength() = assertTrue(cmp.compare("99", "100") < 0)

    @Test fun allDigitStringsEqual() = assertEquals(0, cmp.compare("42", "42"))

    @Test fun nonEmptyVsEmpty() = assertTrue(cmp.compare("a", "") > 0)

    @Test fun leadingDigitsVsLetters() {
        // digit chunk vs letter chunk: digits come before letters in ASCII
        assertTrue(cmp.compare("1abc", "abc") < 0)
    }

    @Test fun mixedCaseAlphabeticSort() {
        val input = listOf("Banana", "apple", "Cherry")
        val sorted = input.sortedWith(cmp)
        assertEquals(listOf("apple", "Banana", "Cherry"), sorted)
    }

    @Test fun identicalPrefixDifferentLength() = assertTrue(cmp.compare("abc", "abcd") < 0)

    @Test fun identicalPrefixShorterSecond() = assertTrue(cmp.compare("abcd", "abc") > 0)

    @Test fun specialCharacters() {
        // Non-digit, non-alpha characters are compared case-insensitively as text chunks
        val result = cmp.compare("file-a", "file-b")
        assertTrue(result < 0)
    }

    @Test fun numbersAtEnd() {
        val input = listOf("task3", "task1", "task2")
        val sorted = input.sortedWith(cmp)
        assertEquals(listOf("task1", "task2", "task3"), sorted)
    }

    @Test fun numbersAtBeginning() {
        val input = listOf("3task", "1task", "2task")
        val sorted = input.sortedWith(cmp)
        assertEquals(listOf("1task", "2task", "3task"), sorted)
    }

    @Test fun largeNumbers() = assertTrue(cmp.compare("file999999", "file1000000") < 0)

    @Test fun sameTextDifferentNumbers() {
        assertTrue(cmp.compare("chapter1", "chapter2") < 0)
        assertTrue(cmp.compare("chapter2", "chapter10") < 0)
        assertTrue(cmp.compare("chapter10", "chapter20") < 0)
    }

    @Test fun onlyWhitespace() {
        // Spaces are non-digit text, compared case-insensitively
        assertEquals(0, cmp.compare(" ", " "))
    }

    @Test fun textWithSpaces() = assertTrue(cmp.compare("item 1", "item 2") < 0)

    // --- FILTER companion object ---

    @Test fun filterComparatorSortsFilters() {
        val f1 = FilterImpl(title = "Groceries")
        val f2 = FilterImpl(title = "Work")
        val f3 = FilterImpl(title = "Appointments")
        val sorted = listOf(f1, f2, f3).sortedWith(AlphanumComparator.FILTER)
        assertEquals(listOf("Appointments", "Groceries", "Work"), sorted.map { it.title })
    }

    @Test fun filterComparatorNumericSort() {
        val f1 = FilterImpl(title = "List 10")
        val f2 = FilterImpl(title = "List 2")
        val f3 = FilterImpl(title = "List 1")
        val sorted = listOf(f1, f2, f3).sortedWith(AlphanumComparator.FILTER)
        assertEquals(listOf("List 1", "List 2", "List 10"), sorted.map { it.title })
    }

    @Test fun filterComparatorHandlesNullTitle() {
        // FilterImpl defaults to "" for title, but custom implementations might return null
        // The comparator returns 0 for null
        val nullCmp = AlphanumComparator<String?> { it }
        assertEquals(0, nullCmp.compare(null, null))
    }

    // --- TAGDATA companion object ---

    @Test fun tagDataComparatorSortsByName() {
        val t1 = TagData(name = "Work")
        val t2 = TagData(name = "Home")
        val t3 = TagData(name = "Errands")
        val sorted = listOf(t1, t2, t3).sortedWith(AlphanumComparator.TAGDATA)
        assertEquals(listOf("Errands", "Home", "Work"), sorted.map { it.name })
    }

    @Test fun tagDataComparatorHandlesNumericNames() {
        val t1 = TagData(name = "Tag 10")
        val t2 = TagData(name = "Tag 2")
        val t3 = TagData(name = "Tag 1")
        val sorted = listOf(t1, t2, t3).sortedWith(AlphanumComparator.TAGDATA)
        assertEquals(listOf("Tag 1", "Tag 2", "Tag 10"), sorted.map { it.name })
    }

    // --- Custom getTitle function ---

    @Test fun customTitleExtractor() {
        data class Item(val label: String, val id: Int)
        val cmpItems = AlphanumComparator<Item> { it.label }
        val items = listOf(Item("b2", 1), Item("a10", 2), Item("a1", 3))
        val sorted = items.sortedWith(cmpItems)
        assertEquals(listOf(3, 2, 1), sorted.map { it.id })
    }

    @Test fun customTitleExtractorReturningNull() {
        data class Item(val label: String?)
        val cmpItems = AlphanumComparator<Item> { it.label }
        // null title returns 0, so relative order is preserved by stable sort
        assertEquals(0, cmpItems.compare(Item(null), Item("abc")))
    }

    // --- Edge cases for getChunk ---

    @Test fun singleDigitChunk() = assertTrue(cmp.compare("a1", "a2") < 0)

    @Test fun singleLetterChunk() = assertTrue(cmp.compare("1a", "1b") < 0)

    @Test fun alternatingChunks() {
        // "a1b2" vs "a1b3"
        assertTrue(cmp.compare("a1b2", "a1b3") < 0)
    }

    @Test fun alternatingChunksDifferentTextChunk() {
        // "a1b" vs "a1c"
        assertTrue(cmp.compare("a1b", "a1c") < 0)
    }

    @Test fun sameNumericChunkDifferentNextChunk() {
        // "a1x" vs "a1y"
        assertTrue(cmp.compare("a1x", "a1y") < 0)
    }

    // --- Sorting with CaldavFilter (uses Filter::title) ---

    @Test fun filterComparatorWithCaldavFilters() {
        val f1 = CaldavFilter(
            calendar = CaldavCalendar(name = "List 10"),
            account = CaldavAccount()
        )
        val f2 = CaldavFilter(
            calendar = CaldavCalendar(name = "List 2"),
            account = CaldavAccount()
        )
        val f3 = CaldavFilter(
            calendar = CaldavCalendar(name = "List 1"),
            account = CaldavAccount()
        )
        val sorted = listOf(f1, f2, f3).sortedWith(AlphanumComparator.FILTER)
        assertEquals(listOf("List 1", "List 2", "List 10"), sorted.map { it.title })
    }
}
