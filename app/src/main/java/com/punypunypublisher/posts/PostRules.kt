package com.punypunypublisher.posts

import com.punypunypublisher.data.model.Post
import com.punypunypublisher.data.model.PublishedEntry
import java.time.Clock
import java.time.LocalDate
import java.time.ZoneId
import java.time.ZoneOffset
import java.time.format.DateTimeParseException

/** Thrown when an edit can't be applied; the message is shown to the user. */
class EditException(message: String) : Exception(message)

/**
 * A change to `posts.json`. Edits are re-applied to a freshly fetched file when a save hits a
 * 409 conflict, so they describe intent rather than a whole new file.
 */
sealed interface Edit {
    val commitMessage: String

    /**
     * Save the edited queue: [lines] are the upcoming posts in publishing order (one per line).
     * [baseIds] are the ids of the upcoming posts the editor was loaded with; if one of them has
     * been published in the meantime, its line is dropped instead of being queued again.
     */
    data class ReplaceQueue(
        val lines: List<String>,
        val baseIds: Set<Int>,
        val today: LocalDate,
    ) : Edit {
        override val commitMessage get() = "Update posts"
    }

    /** One-time import of the old on-device `posts.md` / `published.md`. */
    data class ImportLegacy(
        val queued: List<String>,
        val published: List<PublishedEntry>,
        val today: LocalDate,
    ) : Edit {
        override val commitMessage get() = "Import posts from the old on-device queue"
    }
}

/** Pure rules for `posts.json` (see HANDOFF: IDs, dates, uniqueness, read-only published posts). */
object PostRules {

    const val DEFAULT_MAX_CHARS = 500
    const val SPACING_DAYS = 7L

    /** Reference time zone shared with the workflow (`PUBLISH_TZ` in publish.yml of the posts repo). */
    val ZONE: ZoneId = ZoneOffset.UTC

    fun today(clock: Clock = Clock.systemUTC()): LocalDate = LocalDate.now(clock.withZone(ZONE))

    fun parseDate(raw: String): LocalDate? =
        try {
            LocalDate.parse(raw)
        } catch (_: DateTimeParseException) {
            null
        }

    fun nextId(posts: List<Post>): Int = (posts.maxOfOrNull { it.id } ?: 0) + 1

    /**
     * Date for a new post: the latest date in [posts] (published posts included) plus seven days,
     * but never before [today].
     */
    fun nextDate(posts: List<Post>, today: LocalDate): LocalDate {
        val latest = posts.mapNotNull { parseDate(it.date) }.maxOrNull() ?: return today
        val candidate = latest.plusDays(SPACING_DAYS)
        return if (candidate.isBefore(today)) today else candidate
    }

    /** Mastodon counts characters as code points. */
    fun charCount(text: String): Int = text.codePointCount(0, text.length)

    fun upcoming(posts: List<Post>): List<Post> =
        posts.filter { !it.published }.sortedWith(compareBy({ it.date }, { it.id }))

    /** The editor text: one upcoming post per line, next one first. */
    fun queueText(posts: List<Post>): String =
        upcoming(posts).joinToString("\n") { it.text.replace('\n', ' ') }

    /** Non-blank, trimmed lines of the editor text; each is one post. */
    fun parseLines(text: String): List<String> =
        text.lineSequence().map { it.trim() }.filter { it.isNotEmpty() }.toList()

    /** Apply [edit] to [posts], returning the new list. Throws [EditException] if not allowed. */
    fun apply(posts: List<Post>, edit: Edit, maxChars: Int): List<Post> = when (edit) {
        is Edit.ReplaceQueue -> replaceQueue(posts, edit, maxChars)
        is Edit.ImportLegacy -> LegacyImport.merge(posts, edit.queued, edit.published, edit.today)
    }

    /**
     * Rebuild the upcoming part of the file from the editor lines. Published posts are never
     * touched.
     * - Line i takes the i-th existing upcoming date, so removing a line moves later posts up
     *   (no gap) and reordering lines reorders publishing.
     * - Extra lines get the next weekly date (latest date + 7 days, never in the past).
     * - A line whose text equals an existing upcoming post keeps that post's id; other lines get
     *   new ids. Upcoming posts with no line left are removed, except ones added elsewhere since
     *   the editor was loaded (not in baseIds), which are appended.
     */
    private fun replaceQueue(posts: List<Post>, edit: Edit.ReplaceQueue, maxChars: Int): List<Post> {
        val published = posts.filter { it.published }
        val upcoming = upcoming(posts)

        // Posts the workflow published since the editor was loaded: drop their line, don't re-queue.
        val lines = edit.lines.toMutableList()
        published.filter { it.id in edit.baseIds }.forEach { lines.remove(it.text) }
        // Posts added elsewhere (another device) since the editor was loaded: keep them, at the end.
        upcoming.filter { it.id !in edit.baseIds && it.text !in lines }.forEach { lines += it.text }

        lines.forEachIndexed { i, line ->
            val count = charCount(line)
            if (count > maxChars) {
                throw EditException("Line ${i + 1} has $count characters; the limit is $maxChars.")
            }
        }

        val unmatched = upcoming.toMutableList()
        var newId = nextId(posts)
        val slots = upcoming.map { it.date }
        val result = published.toMutableList()
        lines.forEachIndexed { i, line ->
            val id = unmatched.firstOrNull { it.text == line }?.also { unmatched.remove(it) }?.id
                ?: newId++
            val date = slots.getOrNull(i) ?: nextDate(result, edit.today).toString()
            result += Post(id = id, date = date, text = line)
        }
        return result
    }
}
