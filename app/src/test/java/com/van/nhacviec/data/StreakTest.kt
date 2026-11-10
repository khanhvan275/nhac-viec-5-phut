package com.van.nhacviec.data

import org.junit.Assert.assertEquals
import org.junit.Test

class StreakTest {
    private val td = 20000L

    @Test
    fun countsBackFromToday() {
        val d = AppData(tasks = listOf(Task(title = "TA", quadrant = Quadrant.Q2, daily = true, doneDays = listOf(td, td - 1, td - 2))))
        assertEquals(3, d.streak(td))
    }

    @Test
    fun todayNotYetKeepsYesterdayStreak() {
        val d = AppData(tasks = listOf(Task(title = "TA", quadrant = Quadrant.Q2, daily = true, doneDays = listOf(td - 1, td - 2))))
        assertEquals(2, d.streak(td))
    }

    @Test
    fun brokenStreak() {
        val d = AppData(tasks = listOf(Task(title = "TA", quadrant = Quadrant.Q2, daily = true, doneDays = listOf(td - 2, td - 3))))
        assertEquals(0, d.streak(td))
    }

    @Test
    fun fiveMinutesOfWorkCounts() {
        val d = AppData(
            sessions = listOf(Session("x", 0, 3, td), Session("y", 0, 2, td), Session("x", 0, 4, td - 1)),
        )
        // Hôm nay 3+2 = 5 phút: đạt. Hôm qua 4 phút: chưa đạt.
        assertEquals(1, d.streak(td))
    }
}
