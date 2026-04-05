package org.tasks.markdown

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Test

class MarkdownTest {

    @Test
    fun disabledIsNotEnabled() {
        val md = MarkdownDisabled()
        assertFalse(md.enabled)
    }

    @Test
    fun disabledToMarkdownReturnsInput() {
        val md = MarkdownDisabled()
        assertEquals("hello world", md.toMarkdown("hello world"))
    }

    @Test
    fun disabledToMarkdownWithNull() {
        val md = MarkdownDisabled()
        assertNull(md.toMarkdown(null))
    }

    @Test
    fun disabledToMarkdownWithEmptyString() {
        val md = MarkdownDisabled()
        assertEquals("", md.toMarkdown(""))
    }

    @Test
    fun disabledToMarkdownPreservesMarkdownSyntax() {
        val md = MarkdownDisabled()
        assertEquals("**bold**", md.toMarkdown("**bold**"))
    }

    @Test
    fun disabledToMarkdownPreservesStrikethrough() {
        val md = MarkdownDisabled()
        assertEquals("~~strikethrough~~", md.toMarkdown("~~strikethrough~~"))
    }

    @Test
    fun disabledToMarkdownPreservesLinks() {
        val md = MarkdownDisabled()
        assertEquals("[link](https://example.com)", md.toMarkdown("[link](https://example.com)"))
    }

    @Test
    fun disabledToMarkdownPreservesCheckboxes() {
        val md = MarkdownDisabled()
        assertEquals("- [ ] todo item", md.toMarkdown("- [ ] todo item"))
    }

    @Test
    fun disabledToMarkdownPreservesHeaders() {
        val md = MarkdownDisabled()
        assertEquals("# Header", md.toMarkdown("# Header"))
    }

    @Test
    fun disabledToMarkdownPreservesMultiline() {
        val md = MarkdownDisabled()
        val input = "line 1\nline 2\nline 3"
        assertEquals(input, md.toMarkdown(input))
    }

    @Test
    fun disabledToMarkdownPreservesWhitespace() {
        val md = MarkdownDisabled()
        assertEquals("  leading spaces", md.toMarkdown("  leading spaces"))
    }

    @Test
    fun disabledToMarkdownPreservesSpecialCharacters() {
        val md = MarkdownDisabled()
        assertEquals("<html>&amp;</html>", md.toMarkdown("<html>&amp;</html>"))
    }

    @Test
    fun disabledTextWatcherReturnsNull() {
        val md = MarkdownDisabled()
        assertNull(md.textWatcher(org.mockito.Mockito.mock(android.widget.EditText::class.java)))
    }
}
