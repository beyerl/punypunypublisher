package com.punypunypublisher.data.model

import kotlinx.serialization.Serializable
import java.time.DayOfWeek
import java.time.format.TextStyle
import java.util.Locale

/**
 * A weekly publishing slot, at [weekday] (ISO-8601: 1 = Monday .. 7 = Sunday) and [utcHour]
 * (0..23, UTC). Resolution is one hour, matching the README ("Weekday, UTC hour").
 */
@Serializable
data class PublishTime(
    val weekday: Int,
    val utcHour: Int,
) {
    init {
        require(weekday in 1..7) { "weekday must be 1..7 (Mon..Sun), was $weekday" }
        require(utcHour in 0..23) { "utcHour must be 0..23, was $utcHour" }
    }

    /** Cron expression for this slot, e.g. `0 14 * * 5` (cron DOW: 0 = Sunday .. 6 = Saturday). */
    fun cron(): String = "0 $utcHour * * ${weekday % 7}"

    /** Human-readable label, e.g. `Every Friday 14:00 UTC`. */
    fun humanLabel(): String {
        val day = DayOfWeek.of(weekday).getDisplayName(TextStyle.FULL, Locale.ENGLISH)
        return "Every %s %02d:00 UTC".format(day, utcHour)
    }
}
