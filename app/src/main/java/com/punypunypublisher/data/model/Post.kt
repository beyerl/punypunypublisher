package com.punypunypublisher.data.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * One entry of `posts.json` in the GitHub repo.
 *
 * - [id] is the stable primary key (never reused).
 * - [date] is the scheduled day, `YYYY-MM-DD` in [com.punypunypublisher.posts.PostRules.ZONE];
 *   unique across the file.
 * - [published], [statusId] and [statusUrl] are written only by the GitHub Actions publisher.
 */
@Serializable
data class Post(
    val id: Int,
    val date: String,
    val text: String,
    val published: Boolean = false,
    @SerialName("status_id") val statusId: String? = null,
    @SerialName("status_url") val statusUrl: String? = null,
)
