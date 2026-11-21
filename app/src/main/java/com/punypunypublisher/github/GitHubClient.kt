package com.punypunypublisher.github

import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.engine.okhttp.OkHttp
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.request.HttpRequestBuilder
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.request.parameter
import io.ktor.client.request.put
import io.ktor.client.request.setBody
import io.ktor.client.statement.HttpResponse
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.contentType
import io.ktor.http.encodeURLPath
import io.ktor.http.isSuccess
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import java.io.Closeable
import java.io.IOException
import java.util.Base64

/** Where `posts.json` lives and how to authenticate. */
data class RepoRef(
    val owner: String,
    val repo: String,
    val branch: String,
    val path: String,
    val token: String,
)

/** File text plus the blob sha needed for the next write (null when the file doesn't exist). */
data class FileSnapshot(val text: String, val sha: String?)

/** The file changed on GitHub since it was read (HTTP 409). Re-fetch and re-apply. */
class ConflictException : IOException("posts.json changed on GitHub since it was loaded.")

class GitHubException(val status: Int, message: String) : IOException(message)

/**
 * Reads and writes one file through the GitHub Contents API. Uses `api.github.com` rather than
 * `raw.githubusercontent.com` because the latter is CDN-cached and would show stale data.
 */
class GitHubClient(
    private val http: HttpClient = defaultClient(),
) : Closeable {

    suspend fun read(ref: RepoRef): FileSnapshot {
        val response = http.get(contentsUrl(ref)) {
            auth(ref)
            parameter("ref", ref.branch)
        }
        if (response.status == HttpStatusCode.NotFound) {
            // Either the file doesn't exist yet (fine: first save creates it) or the repo/branch
            // isn't reachable with this token. Tell the two apart.
            ensureRepoReachable(ref)
            return FileSnapshot(text = "", sha = null)
        }
        response.throwIfFailed("load")
        val body: ContentsResponse = response.body()
        val text = body.content?.let { String(Base64.getMimeDecoder().decode(it), Charsets.UTF_8) }
            ?: throw GitHubException(response.status.value, "GitHub returned no file content (file too large?).")
        return FileSnapshot(text = text, sha = body.sha)
    }

    /** Writes [text]; returns the new blob sha. Throws [ConflictException] if [sha] is stale. */
    suspend fun write(ref: RepoRef, text: String, sha: String?, message: String): String {
        val response = http.put(contentsUrl(ref)) {
            auth(ref)
            contentType(ContentType.Application.Json)
            setBody(
                PutRequest(
                    message = message,
                    content = Base64.getEncoder().encodeToString(text.toByteArray(Charsets.UTF_8)),
                    sha = sha,
                    branch = ref.branch,
                ),
            )
        }
        // 409: sha mismatch. 422 without a sha: someone created the file in the meantime.
        if (response.status == HttpStatusCode.Conflict ||
            (response.status == HttpStatusCode.UnprocessableEntity && sha == null)
        ) {
            throw ConflictException()
        }
        response.throwIfFailed("save")
        val body: PutResponse = response.body()
        return body.content?.sha ?: throw GitHubException(response.status.value, "GitHub returned no sha.")
    }

    private suspend fun ensureRepoReachable(ref: RepoRef) {
        val response = http.get("$API/repos/${ref.owner.enc()}/${ref.repo.enc()}/branches/${ref.branch.enc()}") {
            auth(ref)
        }
        if (response.status == HttpStatusCode.NotFound) {
            throw GitHubException(
                404,
                "Repository ${ref.owner}/${ref.repo} (branch ${ref.branch}) not found, " +
                    "or the token has no access to it.",
            )
        }
        response.throwIfFailed("load")
    }

    private suspend fun HttpResponse.throwIfFailed(action: String) {
        if (status.isSuccess()) return
        val hint = when (status) {
            HttpStatusCode.Unauthorized -> "The GitHub token is invalid or expired."
            HttpStatusCode.Forbidden, HttpStatusCode.NotFound ->
                "The GitHub token lacks access (it needs Contents: Read and write on this repo)."
            else -> "GitHub API error."
        }
        throw GitHubException(status.value, "Couldn't $action posts: $hint (HTTP ${status.value}: ${bodyAsText().take(200)})")
    }

    private fun HttpRequestBuilder.auth(ref: RepoRef) {
        header(HttpHeaders.Authorization, "Bearer ${ref.token}")
        header(HttpHeaders.Accept, "application/vnd.github+json")
        header("X-GitHub-Api-Version", "2022-11-28")
        header(HttpHeaders.CacheControl, "no-cache")
    }

    private fun contentsUrl(ref: RepoRef) =
        "$API/repos/${ref.owner.enc()}/${ref.repo.enc()}/contents/${ref.path.trim('/').encodeURLPath()}"

    private fun String.enc() = trim().encodeURLPath()

    override fun close() = http.close()

    @Serializable
    private data class ContentsResponse(val sha: String, val content: String? = null)

    @Serializable
    private data class PutRequest(
        val message: String,
        val content: String,
        val sha: String? = null,
        val branch: String,
    )

    @Serializable
    private data class PutResponse(val content: PutContent? = null)

    @Serializable
    private data class PutContent(val sha: String)

    companion object {
        const val API = "https://api.github.com"

        fun defaultClient(): HttpClient = HttpClient(OkHttp) {
            install(ContentNegotiation) { json(Json { ignoreUnknownKeys = true }) }
        }
    }
}
