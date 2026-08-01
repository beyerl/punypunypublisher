package com.punypunypublisher

import android.app.Application
import com.punypunypublisher.scheduler.Scheduler

class PunyApp : Application() {
    override fun onCreate() {
        super.onCreate()
        // WorkManager persists the periodic chain across reboots; re-asserting with KEEP is cheap.
        Scheduler.ensurePeriodicWork(this)
    }
}
