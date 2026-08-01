package com.punypunypublisher.data

import com.punypunypublisher.data.model.PublishedEntry
import java.time.Instant

/**
 * Pure (filesystem-free) parsing/serialization for the two markdown files.
 *
 * - `posts.md`  : each non-blank line is one queued post; the first non-blank line is next out.
 * - `published.md` : append-only, one line per post: `text ||| <ISO-8601 instant> ||| <url>`.
 */
object MarkdownFormat {

    private const val SEP = " ||| "

    /** Queued posts, in order, ignoring blank lines. */
    fun parsePosts(text: String): List<String> =
        text.lineSequence().map { it.trim() }.filter { it.isNotEmpty() }.toList()

    /** The next post to publish, or null when the queue is empty. */
    fun firstPost(text: String): String? = parsePosts(text).firstOrNull()

    /**
     * Remove the first non-blank line from [text], preserving the remaining lines verbatim.
     * Used after a successful publish to drip exactly one row off the top.
     */
    fun removeFirstPost(text: String): String {
        val lines = text.split("\n")
        val idx = lines.indexOfFirst { it.trim().isNotEmpty() }
        if (idx < 0) return text
        return lines.filterIndexed { i, _ -> i != idx }
            .joinToString("\n")
            .trim('\n')
    }

    fun publishedLine(entry: PublishedEntry): String =
        listOf(entry.text, entry.publishedAt.toString(), entry.statusUrl).joinToString(SEP)

    /** Parse published.md; lenient — malformed lines are skipped. Newest entries appear last. */
    fun parsePublished(text: String): List<PublishedEntry> =
        text.lineSequence()
            .map { it.trim() }
            .filter { it.isNotEmpty() }
            .mapNotNull { line ->
                val parts = line.split(SEP)
                if (parts.size < 3) return@mapNotNull null
                val instant = runCatching { Instant.parse(parts[1]) }.getOrNull() ?: return@mapNotNull null
                PublishedEntry(text = parts[0], publishedAt = instant, statusUrl = parts[2])
            }
            .toList()
}
