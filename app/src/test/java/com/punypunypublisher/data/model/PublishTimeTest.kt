package com.punypunypublisher.data.model

import org.junit.Assert.assertEquals
import org.junit.Test

class PublishTimeTest {

    @Test
    fun cron_mapsIsoWeekdayToCronDayOfWeek() {
        // Friday 14:00 UTC -> cron DOW 5
        assertEquals("0 14 * * 5", PublishTime(weekday = 5, utcHour = 14).cron())
        // Sunday (ISO 7) -> cron DOW 0
        assertEquals("0 0 * * 0", PublishTime(weekday = 7, utcHour = 0).cron())
        // Monday (ISO 1) -> cron DOW 1
        assertEquals("0 9 * * 1", PublishTime(weekday = 1, utcHour = 9).cron())
    }

    @Test
    fun humanLabel_isReadable() {
        assertEquals("Every Friday 14:00 UTC", PublishTime(weekday = 5, utcHour = 14).humanLabel())
        assertEquals("Every Sunday 00:00 UTC", PublishTime(weekday = 7, utcHour = 0).humanLabel())
    }

    @Test(expected = IllegalArgumentException::class)
    fun rejectsInvalidWeekday() {
        PublishTime(weekday = 8, utcHour = 0)
    }

    @Test(expected = IllegalArgumentException::class)
    fun rejectsInvalidHour() {
        PublishTime(weekday = 1, utcHour = 24)
    }
}
