package com.punypunypublisher.publish

import com.punypunypublisher.data.model.PublishTime
import java.time.Instant
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter

/**
 * Pure scheduling logic. A "slot" is one UTC hour (`yyyy-MM-dd'T'HH`); at most one row is
 * published per slot. The worker polls more often than hourly, so [isDue] also gates on the
 * last-consumed slot id to guarantee exactly one drip per slot.
 */
object PublishPlanner {

    private val SLOT_FORMAT: DateTimeFormatter =
        DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH").withZone(ZoneOffset.UTC)

    fun slotId(now: Instant): String = SLOT_FORMAT.format(now)

    /** ISO-8601 weekday: 1 = Monday .. 7 = Sunday, in UTC. */
    fun weekday(now: Instant): Int = now.atZone(ZoneOffset.UTC).dayOfWeek.value

    fun hour(now: Instant): Int = now.atZone(ZoneOffset.UTC).hour

    /** True when [now]'s (weekday, hour) matches any configured slot, ignoring dedup. */
    fun matchesSlotTime(now: Instant, times: List<PublishTime>): Boolean {
        val wd = weekday(now)
        val h = hour(now)
        return times.any { it.weekday == wd && it.utcHour == h }
    }

    /**
     * True when [now] falls inside a configured [times] slot that has not yet been consumed
     * (i.e. its slot id differs from [lastSlot]). This is the scheduled-publish gate.
     */
    fun isDue(now: Instant, times: List<PublishTime>, lastSlot: String?): Boolean =
        slotId(now) != lastSlot && matchesSlotTime(now, times)
}
