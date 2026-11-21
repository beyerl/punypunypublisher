package com.punypunypublisher.data

import com.punypunypublisher.data.model.Post
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.json.Json

/**
 * (De)serialization of `posts.json`. The output matches Python's `json.dump(indent=2)` used by
 * `scripts/publish.py` (in the posts repo), so commits from the app and from the workflow produce clean diffs.
 */
@OptIn(ExperimentalSerializationApi::class)
object PostsJson {

    private val json = Json {
        prettyPrint = true
        prettyPrintIndent = "  "
        encodeDefaults = true
        explicitNulls = false
        ignoreUnknownKeys = true
    }

    private val serializer = ListSerializer(Post.serializer())

    fun decode(text: String): List<Post> =
        if (text.isBlank()) emptyList() else json.decodeFromString(serializer, text)

    /** Posts are written sorted by date (then id) so the file reads chronologically. */
    fun encode(posts: List<Post>): String =
        json.encodeToString(serializer, posts.sortedWith(compareBy({ it.date }, { it.id }))) + "\n"
}
