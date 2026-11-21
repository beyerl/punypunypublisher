package com.punypunypublisher.posts

import com.punypunypublisher.data.PostsJson
import com.punypunypublisher.data.model.Post
import com.punypunypublisher.data.model.PublishedEntry
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Test
import java.time.Instant
import java.time.LocalDate

class PostRulesTest {

    private val today = LocalDate.parse("2026-11-21")
    private fun save(posts: List<Post>, vararg lines: String, base: Set<Int>? = null, maxChars: Int = 500) =
        PostRules.apply(
            posts,
            Edit.ReplaceQueue(lines.toList(), base ?: PostRules.upcoming(posts).map { it.id }.toSet(), today),
            maxChars,
        ).sortedBy { it.date }

    private val queue = listOf(
        Post(1, "2026-11-14", "done", published = true),
        Post(2, "2026-11-21", "a"),
        Post(5, "2026-11-28", "b"),
        Post(3, "2026-12-05", "c"),
    )

    @Test
    fun queueText_isOnePostPerLineInDateOrder() {
        assertEquals("a\nb\nc", PostRules.queueText(queue))
        assertEquals(listOf("x", "y"), PostRules.parseLines("  x \n\n y\n"))
    }

    @Test
    fun unchangedQueue_keepsEverything() {
        assertEquals(queue, save(queue, "a", "b", "c"))
    }

    @Test
    fun appendedLine_getsNextIdAndLatestDatePlusAWeek() {
        assertEquals(Post(6, "2026-12-12", "d"), save(queue, "a", "b", "c", "d").last())
    }

    @Test
    fun firstPost_countsPublishedPostsAndIsNeverInThePast() {
        val onlyPublished = listOf(Post(1, "2026-11-20", "p", published = true))
        assertEquals("2026-11-27", save(onlyPublished, "x").last().date)
        val stale = listOf(Post(1, "2026-01-01", "p", published = true))
        assertEquals(today.toString(), save(stale, "x").last().date)
        assertEquals(today.toString(), save(emptyList(), "x").single().date)
    }

    @Test
    fun removedLine_movesLaterPostsUpWithoutGap() {
        assertEquals(
            listOf(
                Post(1, "2026-11-14", "done", published = true),
                Post(2, "2026-11-21", "a"),
                Post(3, "2026-11-28", "c"),
            ),
            save(queue, "a", "c"),
        )
    }

    @Test
    fun editedLine_keepsSlotAndGetsNewId() {
        assertEquals(Post(6, "2026-11-28", "b edited"), save(queue, "a", "b edited", "c")[2])
    }

    @Test
    fun reorderedLines_swapDatesAndKeepIds() {
        assertEquals(listOf(2, 3, 5), save(queue, "a", "c", "b").drop(1).map { it.id })
    }

    @Test
    fun lineOfPostPublishedMeanwhile_isNotQueuedAgain() {
        // Editor loaded with a, b, c; the workflow then published "a".
        val afterPublish = queue.map { if (it.id == 2) it.copy(published = true) else it }
        val result = save(afterPublish, "a", "b", "c", base = setOf(2, 5, 3))
        assertEquals(1, result.count { it.text == "a" })
        assertEquals(listOf("b", "c"), result.filter { !it.published }.map { it.text })
    }

    @Test
    fun postAddedElsewhereMeanwhile_isKeptAtTheEnd() {
        val withOther = queue + Post(9, "2026-12-12", "from other device")
        val result = save(withOther, "a", "c", base = setOf(2, 5, 3))
        assertEquals(listOf("a", "c", "from other device"), result.filter { !it.published }.map { it.text })
    }

    @Test
    fun publishedPosts_areNeverTouched() {
        assertEquals(listOf(Post(1, "2026-11-14", "done", published = true)), save(queue))
    }

    @Test(expected = EditException::class)
    fun tooLongLine_isRejected() {
        save(queue, "a", "x".repeat(11), maxChars = 10)
    }

    @Test
    fun datesStayUnique() {
        val result = save(queue, "1", "2", "3", "4", "5")
        assertEquals(result.size, result.map { it.date }.toSet().size)
    }

    @Test
    fun charCount_countsCodePoints() {
        assertEquals(2, PostRules.charCount("😀a"))
    }

    @Test
    fun legacyImport_mergesHistoryAndQueueWithUniqueDates() {
        val result = LegacyImport.merge(
            existing = listOf(Post(1, "2026-11-14", "already there", published = true)),
            queued = listOf("q1", "q2"),
            published = listOf(
                PublishedEntry("already there", Instant.parse("2026-11-14T09:00:00Z"), "u0"),
                PublishedEntry("h1", Instant.parse("2026-11-14T15:00:00Z"), "u1"),
            ),
            today = today,
        )
        assertEquals(
            listOf(
                Post(1, "2026-11-14", "already there", published = true),
                Post(2, "2026-11-15", "h1", published = true, statusUrl = "u1"),
                Post(3, "2026-11-22", "q1"),
                Post(4, "2026-11-29", "q2"),
            ),
            result,
        )
        assertEquals(result.size, result.map { it.date }.toSet().size)
    }

    @Test
    fun postsJson_roundTripsMultiLineTextAndMatchesPythonLayout() {
        val list = listOf(Post(2, "2026-12-05", "two\nlines"), Post(1, "2026-11-28", "a", true, "9", "u"))
        val encoded = PostsJson.encode(list)
        assertEquals(
            """
            [
              {
                "id": 1,
                "date": "2026-11-28",
                "text": "a",
                "published": true,
                "status_id": "9",
                "status_url": "u"
              },
              {
                "id": 2,
                "date": "2026-12-05",
                "text": "two\nlines",
                "published": false
              }
            ]
            """.trimIndent() + "\n",
            encoded,
        )
        assertEquals(list.sortedBy { it.id }, PostsJson.decode(encoded).sortedBy { it.id })
        assertFalse(PostsJson.decode("[]\n").any())
    }
}
