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
 * 409 conflict, so they describe intent ("set post 7 to …") rather than a whole new file.
 */
sealed interface Edit {
    val commitMessage: String

    /** New post; its id and date are assigned when applied (see [PostRules.nextDate]). */
    data class Add(val text: String, val today: LocalDate) : Edit {
        override val commitMessage get() = "Add post"
    }

    /** Change the text of an unpublished post; its date stays as is. */
    data class Update(val id: Int, val text: String) : Edit {
        override val commitMessage get() = "Update post #$id"
    }

    data class Delete(val id: Int) : Edit {
        override val commitMessage get() = "Delete post #$id"
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
     * Default date for a new post: the latest date in the file (published posts included) plus
     * seven days, but never before [today].
     */
    fun nextDate(posts: List<Post>, today: LocalDate): LocalDate {
        val latest = posts.mapNotNull { parseDate(it.date) }.maxOrNull() ?: return today
        val candidate = latest.plusDays(SPACING_DAYS)
        return if (candidate.isBefore(today)) today else candidate
    }

    /** Mastodon counts characters as code points. */
    fun charCount(text: String): Int = text.codePointCount(0, text.length)

    /** Problems with a post given the [others] in the file; empty when valid. */
    fun problems(date: String, text: String, others: List<Post>, maxChars: Int): List<String> {
        val out = mutableListOf<String>()
        if (parseDate(date) == null) out += "Invalid date $date in posts.json."
        if (others.any { it.date == date }) out += "Two posts would share the date $date."
        if (text.isBlank()) out += "Post text is empty."
        val count = charCount(text)
        if (count > maxChars) out += "Post is $count characters; the limit is $maxChars."
        return out
    }

    /** Apply [edit] to [posts], returning the new list. Throws [EditException] if not allowed. */
    fun apply(posts: List<Post>, edit: Edit, maxChars: Int): List<Post> = when (edit) {
        is Edit.Add -> add(posts, edit, maxChars)
        is Edit.Update -> update(posts, edit, maxChars)
        is Edit.Delete -> delete(posts, edit)
        is Edit.ImportLegacy -> LegacyImport.merge(posts, edit.queued, edit.published, edit.today)
    }

    private fun add(posts: List<Post>, edit: Edit.Add, maxChars: Int): List<Post> {
        val text = edit.text.trimEnd()
        val date = nextDate(posts, edit.today).toString()
        problems(date, text, posts, maxChars).firstOrNull()?.let { throw EditException(it) }
        return posts + Post(id = nextId(posts), date = date, text = text)
    }

    private fun update(posts: List<Post>, edit: Edit.Update, maxChars: Int): List<Post> {
        val text = edit.text.trimEnd()
        val existing = posts.firstOrNull { it.id == edit.id }
            ?: throw EditException("This post no longer exists on GitHub.")
        if (existing.published) throw EditException("This post has already been published.")
        val others = posts.filter { it.id != edit.id }
        problems(existing.date, text, others, maxChars).firstOrNull()?.let { throw EditException(it) }
        return posts.map { if (it.id == edit.id) it.copy(text = text) else it }
    }

    private fun delete(posts: List<Post>, edit: Edit.Delete): List<Post> {
        val existing = posts.firstOrNull { it.id == edit.id } ?: return posts
        if (existing.published) throw EditException("This post has already been published.")
        return posts.filter { it.id != edit.id }
    }
}
