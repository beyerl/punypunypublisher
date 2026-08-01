package com.punypunypublisher.scheduler

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.punypunypublisher.data.MarkdownFormat
import com.punypunypublisher.data.MarkdownStore
import com.punypunypublisher.data.SettingsStore
import com.punypunypublisher.data.model.PublishedEntry
import com.punypunypublisher.mastodon.MastodonClient
import com.punypunypublisher.publish.PublishPlanner
import java.time.Instant

/**
 * Periodically checks the schedule and drips exactly one queued post per slot.
 *
 * Flow: load settings → bail if unconfigured → bail if this slot was already consumed →
 * (scheduled only) bail if the current hour is not a configured slot → take the top row →
 * post it (idempotency key = slot id) → on success move it to published.md and mark the slot
 * consumed; on network failure leave the queue untouched and ask WorkManager to retry.
 */
class PublishWorker(
    context: Context,
    params: WorkerParameters,
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        val settingsStore = SettingsStore(applicationContext)
        val settings = settingsStore.snapshot()
        if (!settings.isConfigured) return Result.success()

        val force = inputData.getBoolean(KEY_FORCE, false)
        val now = Instant.now()
        val slot = PublishPlanner.slotId(now)

        // One row per slot — covers both scheduled and manual ("Publish next now") runs.
        if (slot == settingsStore.lastPublishedSlot()) return Result.success()
        if (!force && !PublishPlanner.matchesSlotTime(now, settings.publishTimes)) {
            return Result.success()
        }

        val store = MarkdownStore(applicationContext.filesDir)
        val postsText = store.readPostsText()
        val next = MarkdownFormat.firstPost(postsText)
        if (next == null) {
            // Nothing queued: consume the slot so we don't re-check every poll this hour.
            settingsStore.setLastPublishedSlot(slot)
            return Result.success()
        }

        val client = MastodonClient()
        val posted = try {
            client.postStatus(settings.instanceUrl, settings.accessToken, next, slot)
        } finally {
            client.close()
        }

        return posted.fold(
            onSuccess = { url ->
                store.writePostsText(MarkdownFormat.removeFirstPost(postsText))
                store.appendPublished(PublishedEntry(text = next, publishedAt = now, statusUrl = url))
                settingsStore.setLastPublishedSlot(slot)
                Result.success()
            },
            onFailure = { Result.retry() },
        )
    }

    companion object {
        const val KEY_FORCE = "force"
    }
}
