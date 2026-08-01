package com.punypunypublisher.scheduler

import android.content.Context
import androidx.work.BackoffPolicy
import androidx.work.Constraints
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkRequest
import androidx.work.workDataOf
import java.util.concurrent.TimeUnit

/** Enqueues the background publishing work. */
object Scheduler {

    private const val PERIODIC_WORK = "punypunypublisher-periodic-publish"
    private const val PUBLISH_NOW_WORK = "punypunypublisher-publish-now"

    private val networkConstraint = Constraints.Builder()
        .setRequiredNetworkType(NetworkType.CONNECTED)
        .build()

    /** Idempotent: keeps the existing periodic chain if already scheduled. Call on app start. */
    fun ensurePeriodicWork(context: Context) {
        val request = PeriodicWorkRequestBuilder<PublishWorker>(15, TimeUnit.MINUTES)
            .setConstraints(networkConstraint)
            .setBackoffCriteria(
                BackoffPolicy.LINEAR,
                WorkRequest.MIN_BACKOFF_MILLIS,
                TimeUnit.MILLISECONDS,
            )
            .build()
        WorkManager.getInstance(context).enqueueUniquePeriodicWork(
            PERIODIC_WORK,
            ExistingPeriodicWorkPolicy.KEEP,
            request,
        )
    }

    /** Manual "Publish next now": runs immediately, bypassing the configured-time check. */
    fun publishNow(context: Context) {
        val request = OneTimeWorkRequestBuilder<PublishWorker>()
            .setInputData(workDataOf(PublishWorker.KEY_FORCE to true))
            .setConstraints(networkConstraint)
            .build()
        WorkManager.getInstance(context).enqueueUniqueWork(
            PUBLISH_NOW_WORK,
            ExistingWorkPolicy.REPLACE,
            request,
        )
    }
}
