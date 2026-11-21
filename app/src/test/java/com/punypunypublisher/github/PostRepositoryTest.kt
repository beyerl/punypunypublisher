package com.punypunypublisher.github

import com.punypunypublisher.data.PostsJson
import com.punypunypublisher.data.model.Post
import com.punypunypublisher.posts.Edit
import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpMethod
import io.ktor.http.HttpStatusCode
import io.ktor.http.content.TextContent
import io.ktor.http.headersOf
import io.ktor.serialization.kotlinx.json.json
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.LocalDate
import java.util.Base64

/** Simulates the Contents API with an in-memory file whose sha changes on every write. */
class PostRepositoryTest {

    private val ref = RepoRef("o", "r", "main", "posts.json", "t")
    private val jsonHeaders = headersOf(HttpHeaders.ContentType, "application/json")

    private var fileText = PostsJson.encode(listOf(Post(1, "2026-11-28", "first")))
    private var fileSha = "sha1"
    private var putCount = 0
    private var authHeaders = mutableListOf<String?>()

    /** Called before each PUT is processed, to simulate another device writing first. */
    private var beforePut: () -> Unit = {}

    private val engine = MockEngine { request ->
        authHeaders += request.headers[HttpHeaders.Authorization]
        when (request.method) {
            HttpMethod.Get -> respond(
                """{"sha":"$fileSha","content":"${Base64.getMimeEncoder().encodeToString(fileText.toByteArray()).replace("\r\n", "\\n")}"}""",
                HttpStatusCode.OK,
                jsonHeaders,
            )
            HttpMethod.Put -> {
                beforePut()
                beforePut = {}
                val body = Json.parseToJsonElement((request.body as TextContent).text).jsonObject
                if (body["sha"]?.jsonPrimitive?.content != fileSha) {
                    respond("""{"message":"does not match"}""", HttpStatusCode.Conflict, jsonHeaders)
                } else {
                    putCount++
                    fileText = String(Base64.getDecoder().decode(body["content"]!!.jsonPrimitive.content))
                    fileSha = "sha${putCount + 1}"
                    respond("""{"content":{"sha":"$fileSha"}}""", HttpStatusCode.OK, jsonHeaders)
                }
            }
            else -> error("unexpected ${request.method}")
        }
    }

    private val repository = PostRepository(
        GitHubClient(HttpClient(engine) { install(ContentNegotiation) { json(Json { ignoreUnknownKeys = true }) } }),
    )

    @Test
    fun load_decodesBase64AndKeepsSha() = runTest {
        val snapshot = repository.load(ref)
        assertEquals(listOf(Post(1, "2026-11-28", "first")), snapshot.posts)
        assertEquals("sha1", snapshot.sha)
        assertEquals("Bearer t", authHeaders.first())
    }

    @Test
    fun apply_onConflictRefetchesAndReappliesEdit() = runTest {
        val stale = repository.load(ref)
        // Another device adds post 2 before our write lands.
        beforePut = {
            fileText = PostsJson.encode(listOf(Post(1, "2026-11-28", "first"), Post(2, "2026-12-05", "other device")))
            fileSha = "sha-other"
        }
        val result = repository.apply(ref, Edit.ReplaceQueue(listOf("first", "mine"), setOf(1), LocalDate.parse("2026-11-21")), 500, stale)

        val expected = listOf(
            Post(1, "2026-11-28", "first"),
            Post(3, "2026-12-05", "mine"),
            Post(2, "2026-12-12", "other device"),
        )
        assertEquals(expected, result.posts.sortedBy { it.date })
        assertEquals(expected, PostsJson.decode(fileText).sortedBy { it.date })
        assertEquals(fileSha, result.sha)
        assertEquals(1, putCount)
    }
}
