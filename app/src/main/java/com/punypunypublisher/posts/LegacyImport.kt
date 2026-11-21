package com.punypunypublisher.posts

import com.punypunypublisher.data.model.Post
import com.punypunypublisher.data.model.PublishedEntry
import java.time.LocalDate

/**
 * Merges the pre-0.2 on-device files into `posts.json`:
 * - `published.md` rows become `published: true` posts dated on their publish day,
 * - `posts.md` lines become upcoming posts, one week apart after the latest date.
 * Entries whose text is already in the file are skipped, so importing twice is harmless.
 * Dates are bumped forward a day at a time where needed to keep them unique.
 */
object LegacyImport {

    fun merge(
        existing: List<Post>,
        queued: List<String>,
        published: List<PublishedEntry>,
        today: LocalDate,
    ): List<Post> {
        val result = existing.toMutableList()
        val taken = existing.map { it.date }.toMutableSet()

        fun freeDate(start: LocalDate): String {
            var d = start
            while (d.toString() in taken) d = d.plusDays(1)
            return d.toString().also { taken += it }
        }

        val knownPublished = existing.filter { it.published }.map { it.text }.toMutableSet()
        for (entry in published.sortedBy { it.publishedAt }) {
            if (!knownPublished.add(entry.text)) continue
            val day = entry.publishedAt.atZone(PostRules.ZONE).toLocalDate()
            result += Post(
                id = PostRules.nextId(result),
                date = freeDate(day),
                text = entry.text,
                published = true,
                statusUrl = entry.statusUrl.ifBlank { null },
            )
        }

        val knownQueued = existing.filter { !it.published }.map { it.text }.toMutableSet()
        for (text in queued) {
            if (!knownQueued.add(text)) continue
            result += Post(
                id = PostRules.nextId(result),
                date = freeDate(PostRules.nextDate(result, today)),
                text = text,
            )
        }
        return result
    }
}
