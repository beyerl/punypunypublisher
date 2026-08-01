package com.punypunypublisher.data.model

import java.time.Instant

/** A post that has been published, as rendered in the Published view and stored in published.md. */
data class PublishedEntry(
    val text: String,
    val publishedAt: Instant,
    val statusUrl: String,
)
