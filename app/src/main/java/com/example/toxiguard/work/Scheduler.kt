// file: com/example/toxiguard/work/Scheduler.kt
package com.example.toxiguard.work

import android.content.Context
import androidx.work.*
import java.util.Calendar
import java.util.concurrent.TimeUnit

fun scheduleDailyToxicitySummary(context: Context, hourOfDay: Int = 21, minute: Int = 0) {
    // Compute initial delay to the next hourOfDay:minute local time
    val now = Calendar.getInstance()
    val next = Calendar.getInstance().apply {
        set(Calendar.HOUR_OF_DAY, hourOfDay)
        set(Calendar.MINUTE, minute)
        set(Calendar.SECOND, 0)
        set(Calendar.MILLISECOND, 0)
        if (before(now)) add(Calendar.DATE, 1)
    }

    val initialDelay = next.timeInMillis - now.timeInMillis
    val periodicRequest = PeriodicWorkRequestBuilder<ToxicitySummaryWorker>(
        24, TimeUnit.HOURS
    )
        .setInitialDelay(initialDelay, TimeUnit.MILLISECONDS)
        .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, 30, TimeUnit.SECONDS)
        .build()

    WorkManager.getInstance(context).enqueueUniquePeriodicWork(
        "toxicity_daily_summary",
        ExistingPeriodicWorkPolicy.REPLACE,
        periodicRequest
    )
}
