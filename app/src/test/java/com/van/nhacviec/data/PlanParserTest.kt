package com.van.nhacviec.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class PlanParserTest {

    @Test
    fun standardFormat() {
        val p = PlanParser.parse(
            """
            LỘ TRÌNH: Fashion English
            LOẠI: sách
            - Chương 1: Fashion basics | 45 phút | từ vựng cơ bản
            - Chương 2: Colors & Patterns | 30 phút
            - Chương 3: Fabrics | 60 phút
            """.trimIndent()
        )
        assertEquals("Fashion English", p.name)
        assertEquals(PlanKind.BOOK, p.kind)
        assertEquals(3, p.steps.size)
        assertEquals("Chương 1: Fashion basics", p.steps[0].title)
        assertEquals(45, p.steps[0].minutes)
        assertEquals("từ vựng cơ bản", p.steps[0].detail)
        assertEquals(60, p.steps[2].minutes)
    }

    @Test
    fun chatGptMarkdown() {
        val p = PlanParser.parse(
            """
            # Lộ trình giảm mỡ 4 tuần

            Dưới đây là lộ trình cho bạn:

            1. **Tay** (20 phút)
               - Hít đất 3 hiệp
               - Plank 1 phút
            2. **Chân** (25 phút)
            3. **Bụng** (15 phút)
            """.trimIndent()
        )
        assertEquals("Lộ trình giảm mỡ 4 tuần", p.name)
        assertEquals(PlanKind.EXERCISE, p.kind)
        assertEquals(listOf("Tay", "Chân", "Bụng"), p.steps.map { it.title })
        assertEquals(20, p.steps[0].minutes)
        assertTrue(p.steps[0].detail.contains("Hít đất"))
    }

    @Test
    fun courseLessons() {
        val p = PlanParser.parse(
            """
            Khóa CLO3D online
            Bài 1: Giao diện
            Bài 2: Avatar — chỉnh số đo
            Bài 3: Grading
            """.trimIndent()
        )
        assertEquals("Khóa CLO3D online", p.name)
        assertEquals(PlanKind.COURSE, p.kind)
        assertEquals(3, p.steps.size)
        assertEquals("Bài 2: Avatar", p.steps[1].title)
        assertEquals("chỉnh số đo", p.steps[1].detail)
    }

    @Test
    fun mergeKeepsProgress() {
        val old = Plan(
            name = "Fashion English",
            steps = listOf(
                PlanStep(title = "Chương 1: Basics", progress = 100, reviewStage = 2, nextReview = 5),
                PlanStep(title = "Chương 2: Colors", progress = 50),
            ),
        )
        val parsed = PlanParser.parse(
            """
            LỘ TRÌNH: Fashion English
            - Chương 1: Basics
            - Chương 1.5: Extra
            - Chương 2: Colors | 40 phút
            """.trimIndent()
        )
        val merged = PlanParser.merge(old, parsed)
        assertEquals(3, merged.steps.size)
        assertEquals(100, merged.steps[0].progress)
        assertEquals(2, merged.steps[0].reviewStage)
        assertEquals(0, merged.steps[1].progress)
        assertEquals(50, merged.steps[2].progress)
        assertEquals(40, merged.steps[2].minutes)
        assertEquals(old.taskId, merged.taskId)
    }

    @Test
    fun currentStepAndText() {
        val td = today()
        val p = Plan(
            name = "Book",
            steps = listOf(
                PlanStep(title = "C1", progress = 100),
                PlanStep(title = "C2", progress = 50, progressDay = td - 1),
                PlanStep(title = "C3"),
            ),
        )
        assertEquals("C2", p.currentStep(td)?.title)
        assertTrue(p.todayText(td).contains("50%"))
        assertEquals(50, p.overallPercent())
    }

    @Test
    fun exerciseRotation() {
        val td = today()
        val p = Plan(
            name = "Gym", kind = PlanKind.EXERCISE,
            steps = listOf(PlanStep(title = "Tay"), PlanStep(title = "Chân"), PlanStep(title = "Bụng")),
            rotationIndex = 1, lastPassDay = td, lastPassIndex = 0,
        )
        // Hôm nay đã đạt buổi Tay → hôm nay vẫn hiện Tay, mai là Chân.
        assertEquals("Tay", p.currentStep(td)?.title)
        assertEquals("Chân", p.currentStep(td + 1)?.title)
    }
}
