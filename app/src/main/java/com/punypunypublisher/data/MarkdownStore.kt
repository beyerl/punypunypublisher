package com.punypunypublisher.data

import com.punypunypublisher.data.model.PublishedEntry
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

/**
 * File-backed store for the two markdown files of the old on-device publisher (≤ 0.1) in the app
 * folder ([dir], typically `filesDir`). Kept only so their contents can be imported into posts.json.
 * Parsing is delegated to [MarkdownFormat]; this class only does IO.
 */
class MarkdownStore(private val dir: File) {

    val postsFile: File get() = File(dir, POSTS_FILE)
    val publishedFile: File get() = File(dir, PUBLISHED_FILE)

    suspend fun readPostsText(): String = withContext(Dispatchers.IO) {
        if (postsFile.exists()) postsFile.readText() else ""
    }

    suspend fun writePostsText(text: String): Unit = withContext(Dispatchers.IO) {
        ensureDir()
        postsFile.writeText(text)
    }

    suspend fun readPublishedText(): String = withContext(Dispatchers.IO) {
        if (publishedFile.exists()) publishedFile.readText() else ""
    }

    suspend fun readPublished(): List<PublishedEntry> =
        MarkdownFormat.parsePublished(readPublishedText())

    /** Append one published row to published.md (newest last). */
    suspend fun appendPublished(entry: PublishedEntry): Unit = withContext(Dispatchers.IO) {
        ensureDir()
        val prefix = if (publishedFile.exists() && publishedFile.length() > 0L) "\n" else ""
        publishedFile.appendText(prefix + MarkdownFormat.publishedLine(entry))
    }

    /** True if the pre-0.2 on-device queue or history has anything to import. */
    suspend fun hasLegacyData(): Boolean =
        readPostsText().isNotBlank() || readPublishedText().isNotBlank()

    /** Rename both files after a successful import so they aren't offered again. */
    suspend fun markImported(): Unit = withContext(Dispatchers.IO) {
        listOf(postsFile, publishedFile).filter { it.exists() }.forEach {
            it.renameTo(File(dir, it.name + ".imported"))
        }
    }

    private fun ensureDir() {
        if (!dir.exists()) dir.mkdirs()
    }

    companion object {
        const val POSTS_FILE = "posts.md"
        const val PUBLISHED_FILE = "published.md"
    }
}
