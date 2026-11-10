package com.van.nhacviec.data

import kotlinx.serialization.Serializable
import java.time.LocalDate
import java.util.UUID

/** 4 mục ưu tiên. remindMinutes = bao lâu nhắc một lần, maxLevel = mức "gắt" cao nhất. */
@Serializable
enum class Quadrant(
    val no: Int,
    val title: String,
    val label: String,
    val remindMinutes: Int,
    val maxLevel: Int,
) {
    Q1(1, "Khẩn cấp + Quan trọng", "Làm ngay", 60, 3),
    Q2(2, "Quan trọng + Không khẩn cấp", "Lên lịch", 180, 3),
    Q3(3, "Khẩn cấp + Không quan trọng", "Xử lý nhanh", 24 * 60, 2),
    Q4(4, "Không khẩn cấp + Không quan trọng", "Để sau", 24 * 60, 1),
}

@Serializable
data class Task(
    val id: String = UUID.randomUUID().toString(),
    val title: String,
    /** Mô tả chung (việc một lần: chi tiết; việc hằng ngày: mô tả cố định). */
    val detail: String = "",
    val quadrant: Quadrant,
    val daily: Boolean = false,
    /** Hạn chót (epoch millis), chỉ cho việc một lần. */
    val dueAt: Long? = null,
    val doneAt: Long? = null,
    /** Các ngày (epochDay) đã xong, cho việc hằng ngày. */
    val doneDays: List<Long> = emptyList(),
    /** Chi tiết riêng của hôm nay cho việc hằng ngày. */
    val dayDetail: String = "",
    val dayDetailDate: Long = 0,
    val lastRemindAt: Long = 0,
    val nagLevel: Int = 0,
    val nagDay: Long = 0,
    val createdAt: Long = System.currentTimeMillis(),
    /** Việc này là việc hằng ngày của một lộ trình. */
    val planId: String? = null,
) {
    fun isDone(today: Long = today()): Boolean =
        if (daily) today in doneDays else doneAt != null

    fun todayDetail(today: Long = today()): String =
        if (daily && dayDetailDate == today) dayDetail else ""
}

@Serializable
data class Session(
    val taskId: String,
    val start: Long,
    val minutes: Int,
    val day: Long,
)

@Serializable
data class Countdown(
    val id: String = UUID.randomUUID().toString(),
    val name: String,
    val contest: Boolean = false,
    val endDay: Long,
    val startDay: Long = today(),
    val onWidget: Boolean = true,
    /** Các mốc đã thông báo (5, 1). */
    val notified: List<Int> = emptyList(),
) {
    fun daysLeft(today: Long = today()): Long = endDay - today
}

@Serializable
data class Settings(
    val sleepStart: Int = 22 * 60 + 30,
    val sleepEnd: Int = 6 * 60 + 30,
    val journalMinute: Int = 21 * 60 + 30,
    val name: String = "Vân",
)

@Serializable
data class TimerState(
    val taskId: String,
    /** null = đang tạm dừng. */
    val runningSince: Long? = null,
    val accumulatedMs: Long = 0,
    val targetMin: Int = 5,
    val startedAt: Long = System.currentTimeMillis(),
    /** Đang ôn tập lại một bài của lộ trình. */
    val reviewStepId: String? = null,
) {
    fun elapsedMs(now: Long = System.currentTimeMillis()): Long =
        accumulatedMs + (runningSince?.let { now - it } ?: 0)
}

@Serializable
data class AppData(
    val tasks: List<Task> = emptyList(),
    val sessions: List<Session> = emptyList(),
    val countdowns: List<Countdown> = emptyList(),
    val settings: Settings = Settings(),
    val timer: TimerState? = null,
    val plans: List<Plan> = emptyList(),
    val pendingLog: PendingLog? = null,
) {
    fun task(id: String?): Task? = tasks.firstOrNull { it.id == id }

    fun plan(id: String?): Plan? = plans.firstOrNull { it.id == id }

    fun planOf(t: Task?): Plan? = t?.planId?.let { plan(it) }

    /** Tên hiển thị: việc của lộ trình kèm bài hôm nay. */
    fun titleOf(t: Task): String = planOf(t)?.shortTitle() ?: t.title

    /** Chi tiết hôm nay: lộ trình tự tính, việc thường dùng ghi chú. */
    fun detailOf(t: Task, today: Long = today()): String =
        planOf(t)?.todayText(today) ?: t.todayDetail(today).ifBlank { t.detail }

    fun dueReviews(today: Long = today()): List<Pair<Plan, PlanStep>> =
        plans.flatMap { p -> p.dueReviews(today).map { p to it } }

    /** Ngày đạt streak: xong ít nhất 1 việc, hoặc bấm giờ làm được từ 5 phút. */
    fun passedDays(): Set<Long> {
        val zone = java.time.ZoneId.systemDefault()
        val days = HashSet<Long>()
        tasks.forEach { t ->
            days.addAll(t.doneDays)
            t.doneAt?.let { days.add(java.time.Instant.ofEpochMilli(it).atZone(zone).toLocalDate().toEpochDay()) }
        }
        sessions.groupBy { it.day }.forEach { (day, l) -> if (l.sumOf { it.minutes } >= 5) days.add(day) }
        return days
    }

    /** Số ngày liên tiếp đạt, tính đến hôm nay (hoặc hôm qua nếu hôm nay chưa đạt). */
    fun streak(today: Long = today()): Int {
        val days = passedDays()
        var day = if (today in days) today else today - 1
        var n = 0
        while (day in days) {
            n++; day--
        }
        return n
    }

    fun minutesToday(taskId: String, today: Long = today()): Int =
        sessions.filter { it.taskId == taskId && it.day == today }.sumOf { it.minutes }
}

fun today(): Long = LocalDate.now().toEpochDay()
