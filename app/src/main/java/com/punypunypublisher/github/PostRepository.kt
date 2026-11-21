package com.punypunypublisher.github

import com.punypunypublisher.data.PostsJson
import com.punypunypublisher.data.model.Post
import com.punypunypublisher.posts.Edit
import com.punypunypublisher.posts.PostRules
import java.io.IOException

/** Parsed `posts.json` plus the sha it was read at. */
data class PostsSnapshot(val posts: List<Post>, val sha: String?)

/**
 * `posts.json` on GitHub as the single source of truth. Writes are sha-checked; on a 409 the
 * file is re-fetched and the [Edit] re-applied (re-running all validation) before retrying.
 * There's no offline queue: if the network is down, saving fails and the editor keeps the text.
 */
class PostRepository(private val client: GitHubClient) {

    suspend fun load(ref: RepoRef): PostsSnapshot {
        val file = client.read(ref)
        val posts = try {
            PostsJson.decode(file.text)
        } catch (e: IllegalArgumentException) {
            throw IOException("posts.json on GitHub isn't valid JSON: ${e.message}", e)
        }
        return PostsSnapshot(posts, file.sha)
    }

    suspend fun apply(
        ref: RepoRef,
        edit: Edit,
        maxChars: Int,
        current: PostsSnapshot? = null,
    ): PostsSnapshot {
        var snapshot = current ?: load(ref)
        repeat(MAX_ATTEMPTS) {
            val updated = PostRules.apply(snapshot.posts, edit, maxChars)
            val text = PostsJson.encode(updated)
            if (text == PostsJson.encode(snapshot.posts)) return snapshot   // nothing changed
            try {
                val sha = client.write(ref, text, snapshot.sha, edit.commitMessage)
                return PostsSnapshot(updated, sha)
            } catch (_: ConflictException) {
                snapshot = load(ref)
            }
        }
        throw IOException("posts.json kept changing on GitHub; please try again.")
    }

    private companion object {
        const val MAX_ATTEMPTS = 4
    }
}
