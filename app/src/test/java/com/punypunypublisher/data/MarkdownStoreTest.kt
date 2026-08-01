package com.punypunypublisher.data

import com.punypunypublisher.data.model.PublishedEntry
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test
import java.nio.file.Files
import java.time.Instant

class MarkdownStoreTest {

    private fun newStore() = MarkdownStore(Files.createTempDirectory("puny").toFile())

    @Test
    fun postsText_roundTrips() = runTest {
        val store = newStore()
        assertEquals("", store.readPostsText())
        store.writePostsText("one\ntwo")
        assertEquals("one\ntwo", store.readPostsText())
    }

    @Test
    fun appendPublished_accumulatesNewestLast() = runTest {
        val store = newStore()
        store.appendPublished(
            PublishedEntry("first", Instant.parse("2024-01-01T09:00:00Z"), "https://x/1"),
        )
        store.appendPublished(
            PublishedEntry("second", Instant.parse("2024-01-08T09:00:00Z"), "https://x/2"),
        )
        val published = store.readPublished()
        assertEquals(listOf("first", "second"), published.map { it.text })
    }
}
