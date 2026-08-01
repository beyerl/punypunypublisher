package com.punypunypublisher.publish

import com.punypunypublisher.data.model.PublishTime
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.Instant

class PublishPlannerTest {

    // 2024-01-01T09:30Z is a Monday at 09:00 UTC slot.
    private val monday0930 = Instant.parse("2024-01-01T09:30:00Z")
    private val mondaySlot = PublishTime(weekday = 1, utcHour = 9)

    @Test
    fun slotId_isHourGranularityInUtc() {
        assertEquals("2024-01-01T09", PublishPlanner.slotId(monday0930))
    }

    @Test
    fun weekdayAndHour_areUtc() {
        assertEquals(1, PublishPlanner.weekday(monday0930))
        assertEquals(9, PublishPlanner.hour(monday0930))
    }

    @Test
    fun isDue_trueInsideUnconsumedSlot() {
        assertTrue(PublishPlanner.isDue(monday0930, listOf(mondaySlot), lastSlot = null))
    }

    @Test
    fun isDue_falseOutsideConfiguredSlot() {
        val tuesdaySlot = PublishTime(weekday = 2, utcHour = 9)
        assertFalse(PublishPlanner.isDue(monday0930, listOf(tuesdaySlot), lastSlot = null))
    }

    @Test
    fun isDue_falseWhenSlotAlreadyConsumed() {
        assertFalse(PublishPlanner.isDue(monday0930, listOf(mondaySlot), lastSlot = "2024-01-01T09"))
    }

    @Test
    fun matchesSlotTime_ignoresDedup() {
        assertTrue(PublishPlanner.matchesSlotTime(monday0930, listOf(mondaySlot)))
        assertFalse(PublishPlanner.matchesSlotTime(monday0930, emptyList()))
    }
}
