package com.punypunypublisher.mastodon

import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.engine.okhttp.OkHttp
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.request.forms.submitForm
import io.ktor.client.request.header
import io.ktor.client.statement.bodyAsText
import io.ktor.http.HttpHeaders
import io.ktor.http.isSuccess
import io.ktor.http.parameters
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import java.io.Closeable
import java.io.IOException

/** Minimal Mastodon REST client: posts a single status via `POST /api/v1/statuses`. */
class MastodonClient(
    private val httpClient: HttpClient = defaultClient(),
) : Closeable {

    /**
     * Publish [text] to the [instanceUrl] account authenticated by [token].
     * [idempotencyKey] (the slot id) makes retries safe: the server collapses duplicate posts.
     * Returns the public status URL on success.
     */
    suspend fun postStatus(
        instanceUrl: String,
        token: String,
        text: String,
        idempotencyKey: String,
    ): Result<String> = runCatching {
        val url = "${normalizeBaseUrl(instanceUrl)}/api/v1/statuses"
        val response = httpClient.submitForm(
            url = url,
            formParameters = parameters { append("status", text) },
        ) {
            header(HttpHeaders.Authorization, "Bearer $token")
            header("Idempotency-Key", idempotencyKey)
        }
        if (!response.status.isSuccess()) {
            throw IOException("Mastodon API ${response.status}: ${response.bodyAsText()}")
        }
        val status: StatusResponse = response.body()
        status.url ?: status.uri ?: error("Mastodon response had no status URL")
    }

    override fun close() = httpClient.close()

    @Serializable
    private data class StatusResponse(val url: String? = null, val uri: String? = null)

    companion object {
        /** Add a scheme if the user typed a bare host, and drop any trailing slash. */
        fun normalizeBaseUrl(raw: String): String {
            val trimmed = raw.trim().trimEnd('/')
            val withScheme = if (trimmed.startsWith("http://") || trimmed.startsWith("https://")) {
                trimmed
            } else {
                "https://$trimmed"
            }
            return withScheme
        }

        private fun defaultClient(): HttpClient = HttpClient(OkHttp) {
            install(ContentNegotiation) {
                json(Json { ignoreUnknownKeys = true })
            }
        }
    }
}
