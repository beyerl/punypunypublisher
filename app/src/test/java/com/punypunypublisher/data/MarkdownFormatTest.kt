package com.punypunypublisher.data

import com.punypunypublisher.data.model.PublishedEntry
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.time.Instant

class MarkdownFormatTest {

    @Test
    fun parsePosts_ignoresBlankLinesAndTrims() {
        val text = "first post\n\n  second post  \n\n"
        assertEquals(listOf("first post", "second post"), MarkdownFormat.parsePosts(text))
    }

    @Test
    fun firstPost_returnsTopNonBlankLine() {
        assertEquals("alpha", MarkdownFormat.firstPost("\n\nalpha\nbeta"))
        assertNull(MarkdownFormat.firstPost("\n  \n"))
    }

    @Test
    fun removeFirstPost_dripsExactlyOneRow() {
        assertEquals("b\nc", MarkdownFormat.removeFirstPost("a\nb\nc"))
        assertEquals("", MarkdownFormat.removeFirstPost("only"))
        assertEquals("b", MarkdownFormat.removeFirstPost("a\nb\n"))
    }

    @Test
    fun publishedLine_andParse_roundTrip() {
        val entry = PublishedEntry(
            text = "hello world",
            publishedAt = Instant.parse("2024-01-01T09:00:00Z"),
            statusUrl = "https://mastodon.social/@me/123",
        )
        val line = MarkdownFormat.publishedLine(entry)
        val parsed = MarkdownFormat.parsePublished(line)
        assertEquals(listOf(entry), parsed)
    }

    @Test
    fun parsePublished_skipsMalformedLines() {
        val text = "good ||| 2024-01-01T09:00:00Z ||| https://x/1\nnonsense line\nbad ||| not-a-date ||| u"
        val parsed = MarkdownFormat.parsePublished(text)
        assertEquals(1, parsed.size)
        assertEquals("good", parsed.first().text)
    }
}
