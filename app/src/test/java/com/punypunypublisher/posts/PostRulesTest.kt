package com.punypunypublisher.posts

import com.punypunypublisher.data.PostsJson
import com.punypunypublisher.data.model.Post
import com.punypunypublisher.data.model.PublishedEntry
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.Instant
import java.time.LocalDate

class PostRulesTest {

    private val today = LocalDate.parse("2026-11-21")
    private val posts = listOf(
        Post(1, "2026-11-07", "published", published = true),
        Post(3, "2026-11-28", "upcoming"),
    )

    @Test
    fun add_assignsNextIdAndLatestDatePlusAWeek() {
        val result = PostRules.apply(posts, Edit.Add("new", today), 500)
        assertEquals(Post(4, "2026-12-05", "new"), result.last())
    }

    @Test
    fun add_countsPublishedPostsForTheLatestDate() {
        val onlyPublished = listOf(Post(1, "2026-11-20", "p", published = true))
        assertEquals("2026-11-27", PostRules.apply(onlyPublished, Edit.Add("x", today), 500).last().date)
    }

    @Test
    fun nextDate_isNeverInThePast() {
        val stale = listOf(Post(1, "2026-01-01", "p", published = true))
        assertEquals(today, PostRules.nextDate(stale, today))
        assertEquals(today, PostRules.nextDate(emptyList(), today))
    }

    @Test
    fun update_changesTextButKeepsDate() {
        val result = PostRules.apply(posts, Edit.Update(3, "changed"), 500)
        assertEquals(Post(3, "2026-11-28", "changed"), result.first { it.id == 3 })
    }

    @Test(expected = EditException::class)
    fun update_refusesPublishedPosts() {
        PostRules.apply(posts, Edit.Update(1, "nope"), 500)
    }

    @Test(expected = EditException::class)
    fun delete_refusesPublishedPosts() {
        PostRules.apply(posts, Edit.Delete(1), 500)
    }

    @Test
    fun delete_removesUpcomingPostAndIgnoresMissing() {
        assertEquals(listOf(1), PostRules.apply(posts, Edit.Delete(3), 500).map { it.id })
        assertEquals(posts, PostRules.apply(posts, Edit.Delete(42), 500))
    }

    @Test(expected = EditException::class)
    fun add_rejectsTooLongText() {
        PostRules.apply(posts, Edit.Add("x".repeat(11), today), 10)
    }

    @Test(expected = EditException::class)
    fun add_rejectsBlankText() {
        PostRules.apply(posts, Edit.Add("  \n", today), 500)
    }

    @Test
    fun problems_flagsDuplicateDates() {
        assertTrue(PostRules.problems("2026-11-28", "x", posts, 500).isNotEmpty())
        assertTrue(PostRules.problems("2026-11-29", "x", posts, 500).isEmpty())
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
