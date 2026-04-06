package org.tasks.extensions

import org.junit.Assert.assertEquals
import org.junit.Test

class StringExtensionsTest {

    // --- htmlEscape: ampersand ---

    @Test
    fun escapesAmpersand() {
        assertEquals("a &amp; b", "a & b".htmlEscape())
    }

    @Test
    fun escapesMultipleAmpersands() {
        assertEquals("&amp;&amp;&amp;", "&&&".htmlEscape())
    }

    // --- htmlEscape: less than ---

    @Test
    fun escapesLessThan() {
        assertEquals("a &lt; b", "a < b".htmlEscape())
    }

    @Test
    fun escapesMultipleLessThan() {
        assertEquals("&lt;&lt;", "<<".htmlEscape())
    }

    // --- htmlEscape: greater than ---

    @Test
    fun escapesGreaterThan() {
        assertEquals("a &gt; b", "a > b".htmlEscape())
    }

    @Test
    fun escapesMultipleGreaterThan() {
        assertEquals("&gt;&gt;", ">>".htmlEscape())
    }

    // --- htmlEscape: double quote ---

    @Test
    fun escapesDoubleQuote() {
        assertEquals("say &quot;hello&quot;", "say \"hello\"".htmlEscape())
    }

    @Test
    fun escapesMultipleDoubleQuotes() {
        assertEquals("&quot;&quot;&quot;", "\"\"\"".htmlEscape())
    }

    // --- htmlEscape: single quote ---

    @Test
    fun escapesSingleQuote() {
        assertEquals("it&#x27;s", "it's".htmlEscape())
    }

    @Test
    fun escapesMultipleSingleQuotes() {
        assertEquals("&#x27;&#x27;", "''".htmlEscape())
    }

    // --- htmlEscape: mixed ---

    @Test
    fun escapesAllSpecialCharacters() {
        assertEquals(
            "&amp;&lt;&gt;&quot;&#x27;",
            "&<>\"'".htmlEscape()
        )
    }

    @Test
    fun escapesHtmlTag() {
        assertEquals(
            "&lt;script&gt;alert(&#x27;xss&#x27;)&lt;/script&gt;",
            "<script>alert('xss')</script>".htmlEscape()
        )
    }

    @Test
    fun escapesHtmlAttribute() {
        assertEquals(
            "&lt;div class=&quot;test&quot;&gt;",
            "<div class=\"test\">".htmlEscape()
        )
    }

    @Test
    fun escapesAmpersandBeforeOtherEntities() {
        // Verifies that & is escaped first, so subsequent replacements
        // don't double-escape
        assertEquals("&amp;lt;", "&lt;".htmlEscape())
    }

    // --- htmlEscape: no special characters ---

    @Test
    fun noEscapingNeededForPlainText() {
        assertEquals("hello world", "hello world".htmlEscape())
    }

    @Test
    fun noEscapingNeededForNumbers() {
        assertEquals("12345", "12345".htmlEscape())
    }

    @Test
    fun noEscapingNeededForLetters() {
        assertEquals("abcdefghijklmnopqrstuvwxyz", "abcdefghijklmnopqrstuvwxyz".htmlEscape())
    }

    // --- htmlEscape: empty and whitespace ---

    @Test
    fun emptyStringReturnsEmpty() {
        assertEquals("", "".htmlEscape())
    }

    @Test
    fun whitespacePreserved() {
        assertEquals("  \t\n  ", "  \t\n  ".htmlEscape())
    }

    // --- htmlEscape: unicode ---

    @Test
    fun unicodeCharactersPreserved() {
        assertEquals("\u00e9\u00e8\u00ea", "\u00e9\u00e8\u00ea".htmlEscape())
    }

    @Test
    fun emojiPreserved() {
        assertEquals("\uD83D\uDE00", "\uD83D\uDE00".htmlEscape())
    }

    @Test
    fun unicodeWithSpecialCharsMixed() {
        assertEquals("caf\u00e9 &amp; cr\u00e8me", "caf\u00e9 & cr\u00e8me".htmlEscape())
    }

    // --- htmlEscape: order of replacement ---

    @Test
    fun ampersandEscapedBeforeOtherReplacements() {
        // This tests that & is replaced first so &quot; in input becomes &amp;quot;
        assertEquals("&amp;quot;", "&quot;".htmlEscape())
    }

    @Test
    fun ampersandInEntityLikeStringEscapedCorrectly() {
        assertEquals("&amp;amp;", "&amp;".htmlEscape())
    }

    // --- htmlEscape: long string ---

    @Test
    fun longStringWithMixedContent() {
        val input = "Hello <world> & \"friends\" it's a 'nice' day"
        val expected = "Hello &lt;world&gt; &amp; &quot;friends&quot; it&#x27;s a &#x27;nice&#x27; day"
        assertEquals(expected, input.htmlEscape())
    }

    // --- htmlEscape: only special characters ---

    @Test
    fun stringOfOnlySpecialCharacters() {
        assertEquals(
            "&amp;&amp;&lt;&lt;&gt;&gt;&quot;&quot;&#x27;&#x27;",
            "&&<<>>\"\"''".htmlEscape()
        )
    }

    @Test
    fun singleAmpersand() {
        assertEquals("&amp;", "&".htmlEscape())
    }

    @Test
    fun singleLessThan() {
        assertEquals("&lt;", "<".htmlEscape())
    }

    @Test
    fun singleGreaterThan() {
        assertEquals("&gt;", ">".htmlEscape())
    }

    @Test
    fun singleDoubleQuote() {
        assertEquals("&quot;", "\"".htmlEscape())
    }

    @Test
    fun singleSingleQuote() {
        assertEquals("&#x27;", "'".htmlEscape())
    }
}
