package com.punypunypublisher

import android.app.Application
import androidx.work.WorkManager

class PunyApp : Application() {
    override fun onCreate() {
        super.onCreate()
        // Publishing moved to GitHub Actions in 0.2. Cancel the old on-device periodic job,
        // which would otherwise keep waking up (and fail, as its worker class is gone).
        WorkManager.getInstance(this).cancelUniqueWork(LEGACY_PERIODIC_WORK)
        WorkManager.getInstance(this).cancelUniqueWork(LEGACY_PUBLISH_NOW_WORK)
    }

    private companion object {
        const val LEGACY_PERIODIC_WORK = "punypunypublisher-periodic-publish"
        const val LEGACY_PUBLISH_NOW_WORK = "punypunypublisher-publish-now"
    }
}
