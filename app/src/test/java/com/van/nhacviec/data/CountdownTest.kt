package com.van.nhacviec.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

class CountdownTest {
    private fun day(y: Int, m: Int, d: Int) = LocalDate.of(y, m, d).toEpochDay()

    @Test
    fun monthlyRenewKeepsDayOfMonth() {
        val c = Countdown(name = "CLO3D", startDay = day(2026, 10, 30), endDay = day(2026, 11, 30))
        val n = c.renewed()
        assertEquals(day(2026, 11, 30), n.startDay)
        assertEquals(day(2026, 12, 30), n.endDay)
        assertEquals(1, n.renewals)
        assertTrue(n.notified.isEmpty())
    }

    @Test
    fun sameCycleInDaysOrOneMonth() {
        val c = Countdown(name = "Trial", endDay = day(2026, 11, 30), cycleUnit = "day", cycleCount = 14)
        assertEquals(day(2026, 12, 14), c.renewed().endDay)
        assertEquals(day(2026, 12, 30), c.renewed(oneMonth = true).endDay)
        assertEquals("14 ngày", c.cycleLabel())
    }

    @Test
    fun defaultsSoftwareRenewsContestDoesNot() {
        assertTrue(Countdown(name = "Canva", endDay = 0).renews())
        assertFalse(Countdown(name = "Thi", contest = true, endDay = 0).renews())
        assertFalse(Countdown(name = "Canva", endDay = 0, autoRenew = false).renews())
    }
}
